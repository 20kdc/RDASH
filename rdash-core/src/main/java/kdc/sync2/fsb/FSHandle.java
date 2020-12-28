package kdc.sync2.fsb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import kdc.sync2.fsb.FSBackend.*;

public final class FSHandle {
    public final FSBackend host;
    public final String name;

    public FSHandle(FSBackend o, String n) {
        host = o;
        name = n;
    }

    @Override
    public String toString() {
        return host + ":" + name;
    }

    public String getName() {
        return FSBackend.basename(name);
    }

    public void copy(FSHandle other) throws IOException {
        host.copy(name, other.host, other.name);
    }

    public FSHandle[] listFiles() {
        XState state = host.getState(name);
        if (state == null)
            return null;
        if (!(state instanceof DirectoryState))
            return null;
        String[] ip = ((DirectoryState) state).entries;
        FSHandle[] fsh = new FSHandle[ip.length];
        for (int i = 0; i < ip.length; i++)
            fsh[i] = new FSHandle(host, name + "/" + ip[i]);
        return fsh;
    }

    public void setLastModified(long time) {
        ((TimeRWFSBackend) host).changeTime(name, time);
    }

    public XState getState() {
        return host.getState(name);
    }

    public boolean exists() {
        return getState() != null;
    }

    public boolean isFile() {
        return getState() instanceof FileState;
    }

    public long length() {
        return ((FileState) getState()).size;
    }

    public boolean isDirectory() {
        return getState() instanceof DirectoryState;
    }

    public InputStream openRead() throws IOException {
        return host.openRead(name);
    }

    public OutputStream openWrite() throws IOException {
        return host.openWrite(name);
    }

    public void delete() {
        host.delete(name);
    }

}
