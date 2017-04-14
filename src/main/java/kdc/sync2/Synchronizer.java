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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * A class to hold the actual synchronization code.
 * This allows a theoretical GUI frontend to be built. I guess. ServerLayout and SyncFeedback help with that.
 */
public class Synchronizer {
    // Stuff that has to be held because it's updated inside functions.
    private boolean doNotHost = false;
    private LinkedList<String> didNotHost = new LinkedList<>();

    // Stuff that must always be held.
    public final ServerLayout layout;
    public final SyncFeedback feedback;
    public Synchronizer(ServerLayout l, SyncFeedback sf) {
        feedback = sf;
        layout = l;
    }
    // example usage:
    // java -jar Sync2.jar iwakura
    // where "iwakura" is the folder to index
    public void runSynchronize(boolean noHost) throws IOException {
        feedback.handlingFile("init", 0d);
        doNotHost = noHost;
        didNotHost.clear();
        Index theOldDatabase = new Index();
        String theOldHost = theOldDatabase.fillIndexFromFile(layout.getIndex(layout.hostname));
        Index theDatabase = new Index();
        HashSet<String> existingHosts = new HashSet<>();
        File id = layout.getIndexDirectory();
        if (!id.exists())
            throw new RuntimeException("SANITY CHECK : Index dir must exist");
        for (File f : id.listFiles())
            if (f.isFile()) {
                if (!f.getName().equals(layout.hostname)) {
                    feedback.doingTask("Importing index '" + f.getName() + "'");
                    String res = theDatabase.fillIndexFromFile(f);
                    if (res != null)
                        existingHosts.add(res);
                }
            }
        feedback.doingTask("Creating local index");
        theDatabase.fillIndexFromDir(layout.hostname, layout.getLocalDir());
        feedback.doingTask("Starting negotiations");
        theDatabase.createDeletions(theOldDatabase, theOldHost, layout.hostname);
        existingHosts.add(layout.hostname);
        String[] d = theDatabase.entries.keySet().toArray(new String[0]);
        for (int i = 0; i < d.length; i++) {
            feedback.handlingFile(d[i], ((double) i) / d.length);
            negotiateFile(theDatabase, theOldDatabase, existingHosts, d[i]);
        }
        feedback.handlingFile("ENDING", 1.0d);
        feedback.doingTask("Ending negotiations");
        theDatabase.pourSubIndexToFile(layout.hostname, layout.getIndex(layout.hostname));
        if (didNotHost.size() > 0) {
            feedback.logNote("SOME FILES WERE NOT HOSTED, BUT THEY SHOULD BE (make a 'fake server' and merge them in at the destination):");
            for (String s : didNotHost)
                feedback.logNote(s);
        } else {
            feedback.logNote("Maintenance successful.");
        }
    }

    // Note: the old index is used to indicate time/date of what's been uploaded already.
    private void negotiateFile(Index newIndex, Index oldIndex, HashSet<String> existingHosts, String path) throws IOException {
        // Note that only files in newIndex are negotiated.
        HashMap<String, IndexEntry> hosts = newIndex.entries.get(path);
        // First, do we need to update? If so, we do NOT want to upload, no matter what.
        // Also note that baseGet is set to the winner if one exists,
        // so that deletion metadata sticks.
        IndexEntry baseGet = hosts.values().iterator().next();
        if (baseGet == null)
            throw new RuntimeException("bad index");
        long bestDate = baseGet.time;
        long ourDate = -1;
        for (Map.Entry<String, IndexEntry> people : hosts.entrySet()) {
            if (people.getKey().equals(layout.hostname))
                ourDate = people.getValue().time;
            if (people.getValue().time > bestDate) {
                bestDate = people.getValue().time;
                baseGet = people.getValue();
            }
        }
        // Find people hosting the latest version.
        // (so we don't end up with >1 person hosting a version, and we know where to look)
        // Note that bestHost cannot be us. This is used in an attempt to detect bad uploads.
        // (Unless there's a deletion record involved.)
        HashSet<File> validHosts = new HashSet<>();
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
                                    feedback.logNote("WARN: incomplete file hosted by " + people.getKey() + ".");
                                } else {
                                    if (!people.getKey().equals(layout.hostname))
                                        bestHost = people.getKey();
                                    validHosts.add(shouldBe);
                                }
                            } else {
                                // Delete out of date file so that when we update,
                                // we won't be advertising the out-of-date file
                                feedback.logNote("Deleted out of date file hosted by " + people.getKey() + ".");
                                shouldBe.delete();
                            }
                        } else {
                            // BAD!!!
                            shouldBe.delete();
                        }
                    }
                }
            }
        }
        if (ourDate < bestDate) {
            if (bestHost != null) {
                File res = layout.getLocalFile(baseGet);
                if (baseGet.size == -1) {
                    feedback.logNote("A valid deletion record cropped up, kill it.");
                    // It's dead.
                    res.delete();
                    // Make sure we have no record (deletion or otherwise) to fix Issue #1.
                    // Propagation of deletion records is nice but it's also based on a flawed assumption which leads to them getting repropagated forever.
                    hosts.remove(layout.hostname);
                } else  {
                    File hostedFile = layout.getFile(bestHost, baseGet);
                    // Keep in mind that bestDate and bestGet are linked, but NOT bestHost.
                    // Technically this should all be correct anyway,
                    //  but let's try and keep consistency with the entry we say we're downloading,
                    //  even if it's wrong.
                    long correctSize = hosts.get(bestHost).size;
                    if (hostedFile.length() != correctSize) {
                        feedback.logNote("We need it, but we can only get part of it. Not even bothering.");
                    } else {
                        feedback.doingTask("Downloading");
                        // We need to update to bestHost's version, if possible.
                        layout.ensureLocalFileParent(baseGet);
                        Files.copy(hostedFile.toPath(), res.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        res.setLastModified(newIndex.convertStH(bestDate));
                        if (!hosts.containsKey(layout.hostname)) {
                            hosts.put(layout.hostname, new IndexEntry(baseGet.base, baseGet.name, bestDate, res.length()));
                        } else {
                            hosts.get(layout.hostname).time = bestDate;
                        }
                    }
                }
            } else {
                feedback.logNote("We need it, but we can't get it. Sync on a computer which can.");
            }
        } else if (baseGet.size >= 0) {
            boolean hostUpdate = getHostUpdate(existingHosts, hosts, bestDate, true);
            // We need to see if other people need to update
            if (validHosts.size() == 0) {
                if (hostUpdate) {
                    if (!doNotHost) {
                        feedback.doingTask("Uploading");
                        try {
                            layout.ensureFileParent(layout.hostname, baseGet);
                            Files.copy(layout.getLocalFile(baseGet).toPath(), layout.getFile(layout.hostname, baseGet).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception ioe) {
                            ioe.printStackTrace();
                            feedback.logNote("Something went wrong. Not hosting more files.");
                            doNotHost = true;
                            try {
                                layout.getFile(layout.hostname, baseGet).delete();
                            } catch (Exception e) {
                                feedback.logNote("Too much damage done, bailing.");
                                System.exit(1);
                            }
                        }
                    } else {
                        feedback.logNote("SKIPPED DUE TO DISK SPACE MEASURE!");
                    }
                    // if no valid hosts and no hostupdate, this file is not in flux
                }
            } else if (!hostUpdate) {
                feedback.logNote("Everybody has it, but someone is hosting for some reason. Cleaning up.");
                for (File host : validHosts)
                    host.delete();
            } else {
                feedback.logNote("File still needs to propagate.");
            }
        } else {
            // So, the file's been deleted, and we know it.
            // Does anyone else need to know?
            // (note - if they don't have it in their index, it doesn't matter.
            //  this also prevents circles of update-deletion-records, followed by purges.)
            boolean hostUpdate = getHostUpdate(existingHosts, hosts, bestDate, false);
            if (!hostUpdate) {
                feedback.logNote("Everybody knows the file has been deleted, purging.");
                hosts.remove(layout.hostname);
            } else {
                feedback.logNote("File still needs to be deleted.");
            }
        }
    }

    // existingHosts is the global list of hosts in the world, hosts is the usual host->index for this file, bestDate == lastest version time.
    public static boolean getHostUpdate(HashSet<String> existingHosts, HashMap<String, IndexEntry> hosts, long bestDate, boolean mustExist) {
        // If everybody is supposed to have this record, does somebody not have it?
        if (mustExist)
            for (String s : existingHosts)
                if (!hosts.containsKey(s))
                    return true;
        // Does anyone have an outdated record?
        for (Map.Entry<String, IndexEntry> people : hosts.entrySet())
            if (people.getValue().time < bestDate)
                    return true;
        return false;
    }
}
