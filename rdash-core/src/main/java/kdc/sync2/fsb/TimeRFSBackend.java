package kdc.sync2.fsb;

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
