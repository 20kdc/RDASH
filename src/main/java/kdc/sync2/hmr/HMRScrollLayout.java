/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.hmr;

import javax.swing.*;
import java.awt.*;

/**
 * Created on July 23, 2018.
 */
public class HMRScrollLayout extends JScrollPane {
    private final JPanel corePanel;

    public HMRScrollLayout(boolean direction) {
        corePanel = new JPanel();
        setViewportView(corePanel);
        if (direction) {
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            corePanel.setLayout(new GridLayout(0, 1));
        } else {
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            corePanel.setLayout(new GridLayout(1, 0));
        }
    }

    public void addPanel(Component c) {
        corePanel.add(c);
    }
}
