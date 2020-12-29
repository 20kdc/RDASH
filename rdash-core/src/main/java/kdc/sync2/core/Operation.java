/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.core;

import kdc.sync2.fsb.FSHandle;

/**
 * Created on July 23, 2018.
 */
public abstract class Operation {
    public boolean isEssential() {
        return false;
    }

    public abstract String explain();

    // Should show at least some feedback immediately on start.
    public abstract void execute(OperationFeedback feedback);

    public static abstract class GroupOperation extends Operation {
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
    public static abstract class WasteTimeOperation extends Operation {
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

    public static abstract class DeleteFileOperation extends Operation {
        final String string;
        final FSHandle target;
        public DeleteFileOperation(String why, FSHandle shouldBe) {
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
