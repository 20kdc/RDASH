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
            LinkedList<Runnable> refreshers = new LinkedList<>();
            final HMRButton btnAllOff = new HMRButton("ALL OFF", () -> {
                for (final Operation o : operations.getStage(s)) {
                    if (o.isEssential())
                        continue;
                    disable.add(o);
                }
                for (final Runnable r : refreshers)
                    r.run();
            });
            final HMRButton btnAllOn = new HMRButton("ALL ON", () -> {
                for (final Operation o : operations.getStage(s))
                    disable.remove(o);
                for (final Runnable r : refreshers)
                    r.run();
            });
            svl.addPanel(new HMRSplitterLayout(btnAllOff, btnAllOn, false, 0.5f));
            for (final Operation o : operations.getStage(s)) {
                final boolean alreadyDisabled = disable.contains(o);
                final HMRButton btnX = new HMRButton("?", () -> {
                    JOptionPane.showMessageDialog(frame, o.explain(), o.toString(), JOptionPane.OK_OPTION);
                });
                final HMRButton btn = new HMRButton(alreadyDisabled ? "OFF" : "ON", null);
                refreshers.add(() -> {
                    if (disable.contains(o)) {
                        btn.setText("OFF");
                    } else {
                        btn.setText("ON");
                    }
                });
                btn.callback = () -> {
                    if (o.isEssential()) {
                        if (JOptionPane.showConfirmDialog(frame, "This operation is considered essential. Disabling it may cause corruption. Are you sure?", o.toString(), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }
                    if (disable.contains(o)) {
                        disable.remove(o);
                        btn.setText("ON");
                    } else {
                        disable.add(o);
                        btn.setText("OFF");
                    }
                };
                HMRSplitterLayout actions = new HMRSplitterLayout(btnX, btn, false, 1);
                svl.addPanel(new HMRSplitterLayout(new HMRLabel(o.toString()), actions, false, 1));
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
                frame.reset(new ExecutingOperationState(frame, new Operation.GroupOperation(finOps.toArray(new Operation[0])) {
                    @Override
                    public String explain() {
                        return "Running the planned operations.";
                    }
                }, onEndState));
            }
        }), true, 1d);
    }

    @Override
    public void onClose() {
        System.exit(0);
    }
}
