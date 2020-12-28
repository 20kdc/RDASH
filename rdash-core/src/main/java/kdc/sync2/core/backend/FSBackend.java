package kdc.sync2.core.backend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import kdc.sync2.core.OperationFeedback;
import kdc.sync2.core.ServerLayout.DirectoryState;
import kdc.sync2.core.ServerLayout.FileState;
import kdc.sync2.core.ServerLayout.XState;

/**
 * This represents a raw filesystem backend with few capabilities.
 */
public abstract class FSBackend {
    /**
     * Maps the filesystem.
     * Note that this is a costly operation.
     */
    public HashMap<String, XState> mapFilesystem(OperationFeedback feedback) throws IOException {
        HashMap<String, XState> map = new HashMap<String, XState>();
        mapFilesystemRecursiveSearch(map, "", feedback, 0, 1);
        return map;
    }

    private void mapFilesystemRecursiveSearch(HashMap<String, XState> map, String target, OperationFeedback feedback, double start, double end) throws IOException {
        XState state = getState(target);
        map.put(target, state);
        if (state instanceof DirectoryState) {
            String[] ents = ((DirectoryState) state).entries;
            if (ents.length != 0) {
                double piece = (end - start) / ents.length;
                for (String s : ents) {
                    double nxtPoint = start + piece;
                    mapFilesystemRecursiveSearch(map, target + "/" + s, feedback, start, nxtPoint);
                    start = nxtPoint;
                }
            }
        }
    }

    /**
     * Gets the state of a specific file or directory.
     * Returns null if the file was not found.
     * @param fileName
     * @return nope
     * @throws IOException
     */
    public abstract XState getState(String fileName) throws IOException;

    /**
     * Opens a stream to read the file.
     * @return file stream
     */
    public abstract InputStream openRead(String fileName) throws IOException;

    /**
     * Opens a stream to write the file.
     * @return file stream
     */
    public abstract OutputStream openWrite(String fileName) throws IOException;

    /**
     * Deletes a file or an empty directory.
     */
    public abstract void delete(String fileName) throws IOException;

    /**
     * Copies a file from one filesystem to another.
     * Note that this has no protection against interruption.
     * @throws IOException
     */
    public void copy(String fileNameSource, FSBackend target, String fileNameTarget) throws IOException {
        InputStream inp = openRead(fileNameSource);
        OutputStream outp = target.openWrite(fileNameTarget);
        inp.transferTo(outp);
        outp.close();
        inp.close();
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
        public final String[] entries;

        public DirectoryState(String[] ents) {
            entries = ents;
        }
    }
}
