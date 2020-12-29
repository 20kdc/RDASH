/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.se.hmr;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.*;
import javax.swing.text.View;

/**
 * Ok, so here's what's going on here.
 * 1. JTextArea is just plain better than JLabel.
 * 2. However, JTextArea has stupid layout issues which need to be worked around.
 * While I can't fix all of the layout issues, I can at least try.
 * This may end up being changed to something custom in future if I can fix the layout issues by doing that.
 */
public class HMRLabel extends JTextArea {
    public HMRLabel(String s) {
        super(s);
        setWrapStyleWord(true);
        setLineWrap(true);
        setEditable(false);
        hmrUpdateSizes();
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public void setText(String arg0) {
        super.setText(arg0);
        hmrUpdateSizes();
    }

    public void hmrUpdateSizes() {
	View rView = getUI().getRootView(this);
	rView.setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
	Dimension size = new Dimension((int) rView.getMinimumSpan(View.X_AXIS), (int) rView.getMinimumSpan(View.Y_AXIS));
	setMinimumSize(size);
	setPreferredSize(size);
    }
}
