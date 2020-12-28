/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.mdk;

import kdc.sync2.Operation;
import kdc.sync2.hmr.*;

import javax.swing.*;
import java.awt.*;

/**
 * Second MDK state, covers synchronization planning
 * Created on July 23, 2018.
 */
public class ExecutingOperationState implements HMRState.ResizableHMRState {
    public final HMRFrame frame;
    public final Operation op;
    public HMRState after;
    public ExecutingOperationState(HMRFrame f, Operation o, HMRState aft) {
        frame = f;
        op = o;
        after = aft;
    }

    @Override
    public Container createUI() {
        final HMRLabel status = new HMRLabel("Starting the operations immediately.");
        final JProgressBar progress = new JProgressBar();
        progress.setMinimum(0);
        progress.setMaximum(1000);
        // UI active - Start operation!
        new Thread() {
            @Override
            public void run() {
                op.execute(new Operation.OperationFeedback() {
                    @Override
                    public void showFeedback(String text, double operationProgress) {
                        status.setText(text);
                        System.out.println(text + " " + ((int) (operationProgress * 100)) + "%");
                        progress.setValue((int) (operationProgress * 1000));
                    }
                });
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        frame.reset(after);
                    }
                });
            }
        }.start();
        return new HMRSplitterLayout(status, progress, true, 1);
    }

    @Override
    public String toString() {
        return "Executing...";
    }

    @Override
    public void onClose() {
        // Nope :(
    }
}
