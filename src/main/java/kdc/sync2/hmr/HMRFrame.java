/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.hmr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created on July 22, 2018.
 */
public class HMRFrame extends JFrame {
    private HMRState closeHandler;
    public HMRFrame() {
        super("HMRFrame");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if (closeHandler != null)
                    closeHandler.onClose();
            }
        });
        setSize(400, 300);
        setResizable(false);
    }

    public void startDialog(String s) {
        JOptionPane.showMessageDialog(this, s);
    }

    public void reset(HMRState core) {
        reset(core, true);
    }

    public void reset(HMRState core, boolean resize) {
        closeHandler = core;
        Container jp = core.createUI();
        setContentPane(jp);
        setTitle(core.toString());
        setResizable(core instanceof HMRState.ResizableHMRState);
        // AWT is broken enough to imitate Windows, so this MUST be done this way.
        if (resize)
            pack();
        setVisible(true);
    }
}
