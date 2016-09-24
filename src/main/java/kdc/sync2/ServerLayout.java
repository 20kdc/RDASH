/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2;

import java.io.File;

/**
 * Abstracts away the various file paths involved.
 */
public class ServerLayout {
    public final String hostname;

    public ServerLayout(String host) {
        hostname = host;
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
            // assume that the DB is broken
            getIndex(hostname).delete();
            throw new RuntimeException("SYSTEM FAILURE (couldn't create dir.)");
        }
    }

    public File getLocalDir() {
        File f = new File(hostname);
        ensureDir(f);
        return f;
    }

    public File getLocalFile(IndexEntry baseGet) {
        return new File(hostname + baseGet.base + baseGet.name);
    }
}
