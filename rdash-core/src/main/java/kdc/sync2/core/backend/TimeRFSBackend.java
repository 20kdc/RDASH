package kdc.sync2.core.backend;

/**
 * This guarantees returning FileTimeState rather than FileState.
 */
public abstract class TimeRFSBackend extends FSBackend {

    public static class FileTimeState extends FileState {
        public final long time;

        public FileTimeState(long size, long t) {
            super(size);
            time = t;
        }
    }

}
