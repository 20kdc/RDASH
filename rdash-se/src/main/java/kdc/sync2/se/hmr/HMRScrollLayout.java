/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.se.hmr;

import javax.swing.*;
import java.awt.*;

/**
 * Created on July 23, 2018.
 */
public class HMRScrollLayout extends JScrollPane {
    private final JPanel corePanel;

    public HMRScrollLayout(int direction) {
        corePanel = new JPanel();
        setViewportView(corePanel);
        if (direction == 0) {
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            corePanel.setLayout(new GridLayout(1, 0));
        } else if (direction == 1) {
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            corePanel.setLayout(new GridLayout(0, 1));
        } else if (direction == 2) {
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            corePanel.setLayout(new GridLayout(1, 0));
        } else if (direction == 3) {
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            corePanel.setLayout(new GridLayout(0, 1));
        }
    }

    public void addPanel(Component c) {
        corePanel.add(c);
    }
}
