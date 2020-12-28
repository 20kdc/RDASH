/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * An entry in an Index.
 * Note that the Index is not immutable.
 * Furthermore, note that an IndexEntry should never be shared between two Indexes for this reason.
 */
public class IndexEntry {
    // base always ends in "/", while name is the filename.
    public String base, name;
    public long time, size;

    public IndexEntry(String base, String name, long l, long sz) {
        this.base = base;
        this.name = name;
        time = l;
        size = sz;
    }

    public IndexEntry(DataInputStream dis) throws IOException {
        base = dis.readUTF();
        name = dis.readUTF();
        time = dis.readLong();
        size = dis.readLong();
    }

    public void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(base);
        dos.writeUTF(name);
        dos.writeLong(time);
        dos.writeLong(size);
    }
}
