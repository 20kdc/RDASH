package kdc.sync2.fsb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;

import kdc.sync2.core.OperationFeedback;

/**
 * This represents a raw filesystem backend with few capabilities.
 * Paths: "" is the root directory.
 *        "a" is a file or directory called "a" in the root directory.
 *        "a/b" is a file called "b" in the directory called "a" in the root directory.
 */
public abstract class FSBackend {
    /**
     * Maps the filesystem.
     * Note that this is a costly operation.
     */
    public HashMap<String, XState> mapFilesystem(OperationFeedback feedback) {
        HashMap<String, XState> map = new HashMap<String, XState>();
        mapFilesystemRecursiveSearch(map, "", feedback, 0, 1);
        return map;
    }

    private void mapFilesystemRecursiveSearch(HashMap<String, XState> map, String target, OperationFeedback feedback, double start, double end) {
        XState state = getState(target);
        map.put(target, state);
        if (state instanceof DirectoryState) {
            String[] ents = ((DirectoryState) state).entries;
            if (ents.length != 0) {
                double piece = (end - start) / ents.length;
                for (String s : ents) {
                    if (feedback != null)
                        feedback.showFeedback(s, start);
                    double nxtPoint = start + piece;
                    String targetExt = target;
                    if (target != "")
                        targetExt += "/";
                    mapFilesystemRecursiveSearch(map, targetExt + s, feedback, start, nxtPoint);
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
    public abstract XState getState(String fileName);

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
     * If it doesn't exist, this does not throw.
     * If any exception is generated during deletion, this will throw a runtime exception.
     */
    public abstract void delete(String fileName);

    /**
     * Creates an empty directory.
     */
    public abstract void mkdir(String fileName);

    /**
     * Copies a file from one filesystem to another.
     * Note that this has no protection against interruption.
     */
    public void copy(String fileNameSource, FSBackend target, String fileNameTarget) throws IOException {
        InputStream inp = openRead(fileNameSource);
        OutputStream outp = target.openWrite(fileNameTarget);
        inp.transferTo(outp);
        outp.close();
        inp.close();
    }

    /**
     * Creates directories to contain the given file.
     */
    public void ensureDirsToContain(String fileName) {
        int idx = fileName.lastIndexOf('/');
        if (idx != -1) {
            String dirName = fileName.substring(0, idx);
            ensureDirsToContain(dirName);
            XState state = getState(dirName);
            if (state == null) {
                mkdir(dirName);
            } else if (!(state instanceof DirectoryState)) {
                throw new RuntimeException("Conflict between directory and file at " + this + ":" + fileName);
            }
        }
    }

    /**
     * Splits off the last part of a path.
     * Returns null if there is no directory separator.
     */
    public static String dirname(String fileName) {
        int idx = fileName.lastIndexOf('/');
        if (idx != -1)
            return fileName.substring(0, idx);
        return null;
    }

    /**
     * Splits off the last part of a path.
     */
    public static String basename(String fileName) {
        int idx = fileName.lastIndexOf('/');
        if (idx != -1)
            return fileName.substring(idx + 1);
        return fileName;
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
