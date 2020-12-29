/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.se.mdk;

import kdc.sync2.core.Operation;
import kdc.sync2.core.OperationLists;
import kdc.sync2.se.hmr.*;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created on July 23, 2018.
 */
public class OperationPlannerState implements HMRState.ResizableHMRState {
    public final OperationLists operations;
    public final HMRFrame frame;
    public final HashSet<Operation> disable = new HashSet<Operation>();
    public final HMRState onEndState;
    public OperationPlannerState(HMRFrame f, OperationLists ops, HMRState oes) {
        frame = f;
        operations = ops;
        onEndState = oes;
    }

    @Override
    public Container createUI() {
        JTabbedPane jtp = new JTabbedPane();
        for (String s : operations.stages) {
            HMRScrollLayout svl = new HMRScrollLayout(1);
            for (final Operation o : operations.getStage(s)) {
                final boolean alreadyDisabled = disable.contains(o);
                final HMRButton btn = new HMRButton(alreadyDisabled ? "OFF" : "ON", null);
                btn.callback = new Runnable() {
                    @Override
                    public void run() {
                        if (disable.contains(o)) {
                            disable.remove(o);
                            btn.setText("ON");
                        } else {
                            disable.add(o);
                            btn.setText("OFF");
                        }
                    }
                };
                svl.addPanel(new HMRSplitterLayout(new HMRLabel(o.toString()), btn, false, 1));
            }
            jtp.addTab(s, svl);
        }
        return new HMRSplitterLayout(jtp, new HMRButton("Execute", new Runnable() {
            @Override
            public void run() {
                LinkedList<Operation> finOps = new LinkedList<Operation>();
                for (String s : operations.stages)
                    for (Operation o : operations.getStage(s))
                        if (!disable.contains(o))
                            finOps.add(o);
                frame.reset(new ExecutingOperationState(frame, new Operation.GroupOperation(finOps.toArray(new Operation[0])), onEndState));
            }
        }), true, 1d);
    }

    @Override
    public void onClose() {
        System.exit(0);
    }
}
