/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.core;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;

import kdc.sync2.fsb.FSBackend;
import kdc.sync2.fsb.FSBackend.XState;
import kdc.sync2.fsb.FSHandle;
import kdc.sync2.fsb.TimeRWFSBackend;
import kdc.sync2.fsb.impl.RealFSBackend;

/**
 * Abstracts away the various file paths involved.
 */
public class ServerLayout {
    public final String hostname;
    public final TimeRWFSBackend local;
    public final TimeRWFSBackend scratch;
    public final FSBackend server;
    private final FSHandle criticalFlag;
    private final HashMap<String, XState> serverObjects2 = new HashMap<>();

    public ServerLayout(String host) {
        hostname = host;
        local = new RealFSBackend(host + "/");
        local.mkdir("");
        scratch = new RealFSBackend("");
        server = new RealFSBackend("server/");
        server.mkdir("");
        criticalFlag = new FSHandle(server, "sync2.crit.txt");
    }

    public void updateServerMirror() {
        serverObjects2.clear();
        serverObjects2.putAll(server.mapFilesystem(null));
    }

    // it is assumed that the hostfiles are under this.
    // they do not have to be named.
    public FSHandle getIndexDirectory() {
        return new FSHandle(server, "index");
    }
    public FSHandle getIndex(String host) {
        return new FSHandle(server, "index/" + host);
    }

    public FSHandle getFile(String host, IndexEntry value) {
        return new FSHandle(server, "host." + host + value.base + value.name);
    }
    public XState getFileState(String host, IndexEntry value) {
        String place = "server/host." + host + value.base + value.name;
        return serverObjects2.get(place);
    }

    public void ensureFileParent(String host, IndexEntry value) {
        server.mkdir("host." + host + value.base);
    }
    public void ensureLocalFileParent(IndexEntry value) {
        local.mkdir(value.base.substring(1));
    }

    public String getCriticalFlag() {
        if (criticalFlag.exists()) {
            try {
                InputStream fis = criticalFlag.openRead();
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

    public void setCriticalFlag(String flag) {
        if (flag == null) {
            criticalFlag.delete();
        } else {
            try {
                OutputStream fos = server.openWrite("sync2.crit.txt");
                PrintStream ps = new PrintStream(fos, false, "UTF-8");
                ps.println(flag);
                ps.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public FSHandle getLocalTemp() {
        return new FSHandle(scratch, "sync2.temp.bin");
    }

    public FSHandle getLocalDir() {
        return new FSHandle(local, "");
    }

    public FSHandle getLocalFile(IndexEntry baseGet) {
        return new FSHandle(local, baseGet.base.substring(1) + baseGet.name);
    }

}