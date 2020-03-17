/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2;

import kdc.sync2.IndexEntry;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Abstracts away the various file paths involved.
 */
public class ServerLayout {
    public final String hostname;
    private final HashMap<String, XState> serverObjects = new HashMap<>();

    public ServerLayout(String host) {
        hostname = host;
    }

    public void updateServerMirror() {
        serverObjects.clear();
        recursiveSearch("server", false);
    }

    private void recursiveSearch(String target, boolean canDeleteEmpty) {
        File f = new File(target);
        if (f.isDirectory()) {
            boolean found = false;
            for (String s : f.list()) {
                recursiveSearch(target + "/" + s, true);
                found = true;
            }
            if ((!found) && canDeleteEmpty) {
                f.delete();
            } else {
                serverObjects.put(target, new DirectoryState());
            }
        } else if (f.isFile()) {
            serverObjects.put(target, new FileState(f.length()));
        } else {
            if (f.exists())
                serverObjects.put(target, new XState());
        }
    }

    // it is assumed that the hostfiles are under this.
    // they do not have to be named.
    public File getIndexDirectory() {
        return new File("server/index");
    }
    public File getIndex(String host) {
        return new File("server/index/" + host);
    }

    public File getFile(String host, IndexEntry value) {
        return new File("server/host." + host + value.base + value.name);
    }
    public XState getFileState(String host, IndexEntry value) {
        String place = "server/host." + host + value.base + value.name;
        return serverObjects.get(place);
    }

    public void ensureFileParent(String host, IndexEntry value) {
        ensureDir(new File("server/host." + host + value.base));
    }
    public void ensureLocalFileParent(IndexEntry value) {
        ensureDir(new File(hostname + value.base));
    }

    private void ensureDir(File dir) {
        if (dir.exists())
            if (!dir.isDirectory())
                dir.delete();
        if (!dir.mkdirs()) {
            if (dir.exists())
                if (dir.isDirectory())
                    return;
            throw new RuntimeException("SYSTEM FAILURE (couldn't create dir.)");
        }
    }

    public String getCriticalFlag() {
        File f = new File("sync2.crit.txt");
        if (f.exists()) {
            try {
                FileInputStream fis = new FileInputStream(f);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                String r = br.readLine();
                fis.close();
                return r;
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        return null;
    }

    public void setCriticalFlag(IndexEntry flag) {
        File f = new File("sync2.crit.txt");
        if (flag == null) {
            f.delete();
        } else {
            try {
                FileOutputStream fos = new FileOutputStream(f);
                PrintStream ps = new PrintStream(fos, false, "UTF-8");
                ps.println(flag.base + flag.name);
                ps.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public File getLocalTemp() {
        return new File("sync2.temp.bin");
    }

    public File getLocalDir() {
        File f = new File(hostname);
        ensureDir(f);
        return f;
    }

    public File getLocalFile(IndexEntry baseGet) {
        return new File(hostname + baseGet.base + baseGet.name);
    }

    public static class XState {
    	
    }
    public static class FileState extends XState {
    	public final long size;
    	public FileState(long s) {
    		size = s;
    	}
    }
    public static class DirectoryState extends XState {
    	
    }
}
