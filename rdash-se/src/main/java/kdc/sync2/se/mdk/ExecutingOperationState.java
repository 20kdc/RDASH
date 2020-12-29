/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.se.mdk;

import kdc.sync2.core.DangerousFailureRuntimeException;
import kdc.sync2.core.Operation;
import kdc.sync2.core.OperationFeedback;
import kdc.sync2.se.hmr.*;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

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
        	try {
        	    op.execute(new OperationFeedback() {
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
        	} catch (final Exception ex) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            ex.printStackTrace(pw);
                            pw.flush();
                            frame.reset(new ErrorState(sw.toString()));
                        }
                    });
        	}
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
