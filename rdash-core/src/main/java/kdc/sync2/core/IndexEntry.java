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
    // Filename. Example: "moo/sound1.wav"
    public String filename;
    public long time, size;

    public IndexEntry(String filename, long l, long sz) {
        this.filename = filename;
        time = l;
        size = sz;
    }

    public IndexEntry(DataInputStream dis) throws IOException {
        String versionS = dis.readUTF();
        if (!versionS.startsWith("`")) {
            // versionS is of the form "/moo/"
            // name is of the form "sound1.wav"
            String name = dis.readUTF();
            filename = versionS.substring(1) + name;
            time = dis.readLong();
            size = dis.readLong();
        } else {
            int version = Integer.parseInt(versionS.substring(1));
            versionS = "";
            if (version == 0) {
                filename = dis.readUTF();
                time = dis.readLong();
                size = dis.readLong();
            } else {
                throw new IOException("cannot understand version " + version);
            }
        }
    }

    public void write(DataOutputStream dos) throws IOException {
        dos.writeUTF("`0");
        dos.writeUTF(filename);
        dos.writeLong(time);
        dos.writeLong(size);
    }
}
