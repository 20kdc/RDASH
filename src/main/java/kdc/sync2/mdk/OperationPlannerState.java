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
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created on July 23, 2018.
 */
public class OperationPlannerState implements HMRState.ResizableHMRState {
    public LinkedList<Operation> operations;
    public final HMRFrame frame;
    public HashSet<Operation> disable = new HashSet<Operation>();
    public OperationPlannerState(HMRFrame f, LinkedList<Operation> ops) {
        frame = f;
        operations = ops;
    }

    @Override
    public Container createUI() {
        HMRScrollLayout svl = new HMRScrollLayout(true);
        for (final Operation o : operations) {
            final boolean alreadyDisabled = disable.contains(o);
            svl.addPanel(new HMRSplitterLayout(new JLabel(o.toString()), new HMRButton(alreadyDisabled ? "OFF" : "ON", new Runnable() {
                @Override
                public void run() {
                    if (alreadyDisabled) {
                        disable.remove(o);
                    } else {
                        disable.add(o);
                    }
                    frame.reset(OperationPlannerState.this, false);
                }
            }), false, 1));
        }
        return new HMRSplitterLayout(svl, new HMRButton("Execute", new Runnable() {
            @Override
            public void run() {
                LinkedList<Operation> finOps = new LinkedList<Operation>();
                for (Operation o : operations)
                    if (!disable.contains(o))
                        finOps.add(o);
                frame.reset(new ExecutingOperationState(frame, new Operation.GroupOperation(finOps.toArray(new Operation[0])), new RequestHostnameState(frame)));
            }
        }), true, 1d);
    }

    @Override
    public void onClose() {
        System.exit(0);
    }
}
