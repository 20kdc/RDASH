/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * A class to hold the actual synchronization code.
 * This allows a theoretical GUI frontend to be built. I guess. ServerLayout and SyncFeedback help with that.
 */
public class Synchronizer {

    public boolean failedToUploadAFile = false;

    // Stuff that must always be held.
    public final ServerLayout layout;
    public Synchronizer(ServerLayout l) {
        layout = l;
    }

    public Operation prepareSync(final boolean noHost, final OperationLists preparedLists) {
        String critical = layout.getCriticalFlag();
        if (critical != null)
            throw new RuntimeException("SANITY CHECK : Pre-sync checks show that file " + critical + " is in an uncertain state due to sync failure.");

        // Ok, so, here's how it goes as of 0.4 dev, though it's been more or less the case since the beginning.
        // The old database contains *just* our old index, and exists to generate deletion records.
        // The new database contains all *other* indexes, and the current state of the working area,
        //  along with deletion records imported from the old database and created by comparison with it.
        // The Index code is really much more suited to the new database vs. the old one, but it's just simpler to share the code.

        final Index theOldDatabase = new Index();
        final String theOldHost = theOldDatabase.fillIndexFromFile(layout.getIndex(layout.hostname));

        final Index theDatabase = new Index();
        final HashSet<String> existingHosts = new HashSet<>();
        File id = layout.getIndexDirectory();
        if (!id.exists())
            throw new RuntimeException("SANITY CHECK : Index dir must exist");
        for (File f : id.listFiles())
            if (f.isFile()) {
                if (!f.getName().equals(layout.hostname)) {
                    System.out.println("Importing index '" + f.getName() + "'");
                    String res = theDatabase.fillIndexFromFile(f);
                    if (res != null)
                        existingHosts.add(res);
                }
            }
        theDatabase.fillIndexFromDir(layout.hostname, layout.getLocalDir());
        theDatabase.createDeletions(theOldDatabase, theOldHost, layout.hostname);
        existingHosts.add(layout.hostname);
        return new Operation() {
            @Override
            public void execute(OperationFeedback feedback) {
                LinkedList<String> lls = new LinkedList<String>(theDatabase.entries.keySet());
                Collections.sort(lls);
                String[] d = lls.toArray(new String[0]);
                for (int i = 0; i < d.length; i++) {
                    feedback.showFeedback("Checking " + d[i], i / (double) d.length);
                    try {
                        negotiateFile(theDatabase, theOldDatabase, existingHosts, d[i], preparedLists, noHost);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                preparedLists.cleanup.add(new Operation() {
                    @Override
                    public String toString() {
                        return "Update Remote Index";
                    }

                    @Override
                    public void execute(OperationFeedback feedback) {
                        try {
                            theDatabase.pourSubIndexToFile(layout.hostname, layout.getIndex(layout.hostname));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        };
    }

    // Note: the old index is used to indicate time/date of what's been uploaded already.
    private void negotiateFile(final Index newIndex, Index oldIndex, HashSet<String> existingHosts, final String path, OperationLists actuallyPerform, boolean noHost) throws IOException {
        // Note that only files in newIndex are negotiated.
        final HashMap<String, IndexEntry> hosts = newIndex.entries.get(path);
        // First, do we need to update? If so, we do NOT want to upload, no matter what.
        // Also note that baseGet is set to the winner if one exists,
        // so that deletion metadata sticks.
        IndexEntry baseGetPF = hosts.values().iterator().next();
        if (baseGetPF == null)
            throw new RuntimeException("bad index");
        long ourDate = -1;
        for (Map.Entry<String, IndexEntry> people : hosts.entrySet()) {
            if (people.getKey().equals(layout.hostname))
                ourDate = people.getValue().time;
            if (people.getValue().time > baseGetPF.time)
                baseGetPF = people.getValue();
        }
        final IndexEntry baseGet = baseGetPF;
        baseGetPF = null;
        final long bestDate = baseGet.time;
        // Find people hosting the latest version.
        // (so we don't end up with >1 person hosting a version, and we know where to look)
        // Note that bestHost cannot be us. This is used in an attempt to detect bad uploads.
        // (Unless there's a deletion record involved.)
        final HashSet<File> validHosts = new HashSet<>();
        String bestHost = null;
        for (Map.Entry<String, IndexEntry> people : hosts.entrySet()) {
            if (people.getValue().size == -1) {
                if (people.getValue().time >= bestDate) {
                    // Reliable deletion record always wins.
                    bestHost = people.getKey();
                    break;
                }
            } else {
                File shouldBe = layout.getFile(people.getKey(), people.getValue());
                // NOTE: Since our index is updated, in the case of an update before sync is complete... just... ugh.
                // What's going on here is that we need to compare the hosted data to what it's labelled as in the index *then*.
                // Not the index now.
                // This means we delete our own out of date files, because we're comparing to the index entry that uploaded that file.
                IndexEntry entryAtUpload = people.getValue();
                if (people.getKey().equals(layout.hostname))
                    entryAtUpload = oldIndex.ensureEntry(path).get(layout.hostname);
                if (entryAtUpload != null) {
                    if (shouldBe.exists()) {
                        if (shouldBe.isFile()) {
                            // It's assumed that server lastmodified is unreliable,
                            // and that the indexes ARE reliable.
                            if (entryAtUpload.time >= bestDate) {
                                if (entryAtUpload.size != baseGet.size) {
                                    System.err.println("WARN: incomplete file hosted by " + people.getKey() + ".");
                                } else {
                                    if (!people.getKey().equals(layout.hostname))
                                        bestHost = people.getKey();
                                    validHosts.add(shouldBe);
                                }
                            } else {
                                actuallyPerform.cleanup.add(new Operation.DeleteFileOperation("out-of-date remote file", shouldBe));
                            }
                        } else {
                            actuallyPerform.cleanup.add(new Operation.DeleteFileOperation("remote conflicting non-file", shouldBe));
                        }
                    }
                }
            }
        }
        // This set of if/elseifs determines which of the following is the case:
        // A. We are out of date
        // B. The file exists and we are up to date, so check if we have a responsibility to forward
        // C. If not A or B, the file is being deleted
        if (ourDate < bestDate) {
            // If this fails, we need the file but we can't get it
            if (bestHost != null) {
                final File res = layout.getLocalFile(baseGet);
                if (baseGet.size == -1) {
                    if (res.exists()) {
                        // We have the file and we've been told to delete the file.
                        final String text = "Delete " + res + " because of " + bestHost;
                        actuallyPerform.cleanup.add(new Operation() {
                            @Override
                            public String toString() {
                                return text;
                            }

                            @Override
                            public void execute(OperationFeedback feedback) {
                                res.delete();
                                hosts.remove(layout.hostname);
                            }
                        });
                    }
                } else  {
                    final File hostedFile = layout.getFile(bestHost, baseGet);
                    // Keep in mind that bestDate and bestGet are linked, but NOT bestHost.
                    // Technically this should all be correct anyway,
                    //  but let's try and keep consistency with the entry we say we're downloading,
                    //  even if it's wrong.
                    long correctSize = hosts.get(bestHost).size;
                    if (hostedFile.length() == correctSize) {
                        actuallyPerform.download.add(new Operation() {

                            @Override
                            public String toString() {
                                return "Download " + hostedFile;
                            }

                            @Override
                            public void execute(OperationFeedback feedback) {
                                if (failedToUploadAFile)
                                    return;
                                feedback.showFeedback("Downloading " + hostedFile, 0);
                                // We need to update to bestHost's version, if possible.
                                layout.ensureLocalFileParent(baseGet);
                                // Split this into two sections to reduce chance of accidental corruption.
                                File temp = layout.getLocalTemp();
                                try {
                                    Files.copy(hostedFile.toPath(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                    failedToUploadAFile = true;
                                    return;
                                }
                                feedback.showFeedback("Transferring " + path, 0.5);
                                layout.setCriticalFlag(baseGet);
                                try {
                                    Files.copy(temp.toPath(), res.toPath(), StandardCopyOption.REPLACE_EXISTING); // CRITICAL OPERATION
                                } catch (Exception ioe) {
                                    System.exit(1);
                                    return;
                                }
                                layout.setCriticalFlag(null);
                                temp.delete();
                                res.setLastModified(newIndex.convertStH(baseGet.time));
                                if (!hosts.containsKey(layout.hostname)) {
                                    hosts.put(layout.hostname, new IndexEntry(baseGet.base, baseGet.name, bestDate, res.length()));
                                } else {
                                    hosts.get(layout.hostname).time = baseGet.time;
                                }
                            }
                        });
                    }
                }
            }
        } else if (baseGet.size >= 0) {
            final String hostUpdate = getHostUpdate(existingHosts, hosts, bestDate, true);
            // We need to see if other people need to update
            if (validHosts.size() == 0) {
                // There are no valid hosts, which means by definition nobody has it.
                if (hostUpdate != null) {
                    if (!noHost) {
                        actuallyPerform.upload.add(new Operation() {
                            @Override
                            public String toString() {
                                return "Upload " + path + " for " + hostUpdate;
                            }

                            @Override
                            public void execute(OperationFeedback feedback) {
                                if (failedToUploadAFile)
                                    return;
                                feedback.showFeedback("Uploading " + path, 0);
                                try {
                                    layout.ensureFileParent(layout.hostname, baseGet);
                                    Files.copy(layout.getLocalFile(baseGet).toPath(), layout.getFile(layout.hostname, baseGet).toPath(), StandardCopyOption.REPLACE_EXISTING);
                                } catch (Exception ioe) {
                                    ioe.printStackTrace();
                                    feedback.showFeedback("Something went wrong - won't continue uploading files.", 0);
                                    failedToUploadAFile = true;
                                    try {
                                        layout.getFile(layout.hostname, baseGet).delete();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        System.exit(1);
                                    }
                                }
                            }
                        });
                    }
                    // if no valid hosts and no hostupdate, this file is not in flux
                }
            } else if (hostUpdate == null) {
                actuallyPerform.cleanup.add(new Operation() {
                    @Override
                    public String toString() {
                        return "Purge unnecessary copies of " + path;
                    }

                    @Override
                    public void execute(OperationFeedback feedback) {
                        for (File host : validHosts)
                            host.delete();
                    }
                });
            }
            // else: file still needs to propagate
        } else {
            // So, the file's been deleted, and we know it.
            // Does anyone else need to know?
            // (note - if they don't have it in their index, it doesn't matter.
            //  this also prevents circles of update-deletion-records, followed by purges.)
            String hostUpdate = getHostUpdate(existingHosts, hosts, bestDate, false);
            if (hostUpdate == null)
                hosts.remove(layout.hostname);
        }
    }

    // existingHosts is the global list of hosts in the world, hosts is the usual host->index for this file, bestDate == lastest version time.
    public static String getHostUpdate(HashSet<String> existingHosts, HashMap<String, IndexEntry> hosts, long bestDate, boolean mustExist) {
        LinkedList<String> hss = new LinkedList<String>();
        // If everybody is supposed to have this record, does somebody not have it?
        if (mustExist)
            for (String s : existingHosts)
                if (!hosts.containsKey(s))
                    if (!hss.contains(s))
                        hss.add(s);
        // Does anyone have an outdated record?
        for (Map.Entry<String, IndexEntry> people : hosts.entrySet())
            if (people.getValue().time < bestDate)
                if (!hss.contains(people.getKey()))
                    hss.add(people.getKey());
        if (hss.size() == 0)
            return null;
        Collections.sort(hss);
        StringBuilder sb = new StringBuilder();
        boolean bln = false;
        for (String st : hss) {
            if (bln)
                sb.append(' ');
            sb.append(st);
            bln = true;
        }
        return sb.toString();
    }
}
