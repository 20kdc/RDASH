/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.core;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import kdc.sync2.fsb.FSHandle;
import kdc.sync2.fsb.TimeRFSBackend.FileTimeState;

/**
 * Not actually just one index, but a database of indexes.
 */
public class Index {
    public HashMap<String, HashMap<String, IndexEntry>> entries = new HashMap<>();

    // NTFS Bad Stuff Prevention System. If the system's timer is accurate to about 15 seconds, all should be well.
    // Note that changing this will mess with index file compatibility.
    public long neededResolution = 15000;
    // Convert server time to host time.
    public long convertStH(long bestDate) {
        return bestDate * neededResolution;
    }
    public long convertHtS(long bestDate) {
        long r = bestDate / (neededResolution / 2); // with the default of 15000, this should be 7500.
        long rd = r / 2;
        if (r % 2 == 1)
            rd++;
        return rd;
    }

    public HashMap<String, IndexEntry> ensureEntry(String key) {
        if (entries.containsKey(key))
            return entries.get(key);
        HashMap<String, IndexEntry> es = new HashMap<>();
        entries.put(key, es);
        return es;
    }

    public String fillIndexFromFile(FSHandle f) {
        // Certainly not a magical index.
        try {
            DataInputStream dis = new DataInputStream(f.openRead());
            String hname = dis.readUTF();
            if (!hname.equals(f.getName())) {
                System.out.println("Index contamination detected, " + f.getName());
                return null;
            }
            // Sanity check.
            while (dis.readInt() == 4957) {
                IndexEntry res = new IndexEntry(dis);
                ensureEntry(res.filename).put(hname, res);
            }
            dis.close();
            return hname;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void pourSubIndexToFile(String hname, FSHandle f) throws IOException {
        DataOutputStream dos = new DataOutputStream(f.openWrite());
        dos.writeUTF(hname);
        for (String s : entries.keySet()) {
            HashMap<String, IndexEntry> hs = entries.get(s);
            if (hs.containsKey(hname)) {
                dos.writeInt(4957); // versioning/sanity check
                hs.get(hname).write(dos);
            }
        }
        dos.writeInt(0);
        dos.close();
    }

    public void fillIndexFromDir(String hname, FSHandle f) {
        fillIndexFromDirInt(hname, "", f);
    }
    // base always ends in "/"
    private void fillIndexFromDirInt(String hname, String base, FSHandle baseF) {
        for (FSHandle f : baseF.listFiles()) {
            if (f.isDirectory()) {
                fillIndexFromDirInt(hname, base + f.getName() + "/", f);
            } else {
                HashMap<String, IndexEntry> hm = ensureEntry(base + f.getName());
                FileTimeState fts = (FileTimeState) f.getState();
                hm.put(hname, new IndexEntry(base + f.getName(), convertHtS(fts.time), fts.size));
            }
        }
    }

    public void createDeletions(Index theOldDatabase, String theOldHost, String theNewHost) {
        // Firstly, old deletion records are forwarded, UNLESS some record exists on the new host.
        for (Map.Entry<String, HashMap<String, IndexEntry>> e : theOldDatabase.entries.entrySet()) {
            if (e.getValue().containsKey(theOldHost)) {
                IndexEntry old = e.getValue().get(theOldHost);
                if (entries.containsKey(e.getKey()))
                    if (entries.get(e.getKey()).containsKey(theNewHost))
                        continue;
                if (old.size == -1)
                    ensureEntry(e.getKey()).put(theNewHost, new IndexEntry(old.filename, old.time, -1));
            }
        }
        // Secondly, files which have "disappeared" have deletion records (dated to just after the last known update) created.
        HashSet<IndexEntry> oldLiving = theOldDatabase.getLivingFiles(theOldHost);
        HashSet<String> newLivingPaths = pathizeSet(getLivingFiles(theNewHost));
        for (IndexEntry s : oldLiving) {
            if (!newLivingPaths.contains(s.filename)) {
                // Create deletion record, and if an update happens within 5 seconds, it's rejected,
                //  but otherwise updates from outside will simply replace the file.
                ensureEntry(s.filename).put(theNewHost, new IndexEntry(s.filename, s.time + 5000, -1));
            }
        }
    }

    private HashSet<String> pathizeSet(HashSet<IndexEntry> livingFiles) {
        HashSet<String> n = new HashSet<>();
        for (IndexEntry ie : livingFiles)
            n.add(ie.filename);
        return n;
    }

    // Retrieve all index entries, apart from deletion records.
    public HashSet<IndexEntry> getLivingFiles(String theNewHost) {
        HashSet<IndexEntry> n = new HashSet<>();
        for (Map.Entry<String, HashMap<String, IndexEntry>> e : entries.entrySet()) {
            if (e.getValue().containsKey(theNewHost)) {
                // All paths on this host go here.
                IndexEntry res = e.getValue().get(theNewHost);
                if (res.size != -1)
                    n.add(res);
            }
        }
        return n;
    }
}
