/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.core;

import java.io.File;

/**
 * Created on July 23, 2018.
 */
public interface Operation {
    // Should show at least some feedback immediately on start.
    void execute(OperationFeedback feedback);

    interface OperationFeedback {
        void showFeedback(String text, double operationProgress);
    }
    final class GroupOperation implements Operation {
        public final Operation[] group;
        public GroupOperation(Operation[] ops) {
            group = ops;
        }

        @Override
        public void execute(final OperationFeedback feedback) {
            int index = 0;
            for (final Operation o : group) {
                final double base = (index / (double) group.length);
                index++;
                o.execute(new OperationFeedback() {
                    @Override
                    public void showFeedback(String text, double operationProgress) {
                        feedback.showFeedback(text, base + (operationProgress / group.length));
                    }
                });
            }
        }
    }
    final class WasteTimeOperation implements Operation {
        @Override
        public String toString() {
            return "Play W.A. Music";
        }

        @Override
        public void execute(OperationFeedback feedback) {
            for (int i = 0; i < 10; i++) {
                feedback.showFeedback("Wasting time...", i / 10d);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    final class DeleteFileOperation implements Operation {
        final String string;
        final File target;
        public DeleteFileOperation(String why, File shouldBe) {
            string = "Delete " + why + " " + shouldBe;
            target = shouldBe;
        }

        @Override
        public String toString() {
            return string;
        }

        @Override
        public void execute(OperationFeedback feedback) {
            feedback.showFeedback("Deleting " + target, 0);
            target.delete();
        }
    }
}
