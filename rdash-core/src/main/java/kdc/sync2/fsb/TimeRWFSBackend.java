package kdc.sync2.fsb;

import java.io.IOException;

/**
 * This guarantees FileTimeState rather than FileState, but also allows setting the time.
 */
public abstract class TimeRWFSBackend extends TimeRFSBackend {
    /**
     * Updates the time of a file.
     * 
     * @param fileName Filename
     * @param time     The new time
     */
    public abstract void changeTime(String fileName, long time);
}
