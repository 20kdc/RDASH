/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2;

/**
 * Sync is divided into files, then tasks.
 * This sends back feedback on that.
 * (However, it should look like it's divided into tasks, then files.)
 */
public interface SyncFeedback {
    void handlingFile(String file, double percent);
    void doingTask(String task);
    void logNote(String note);
}
