/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.hmr;

import javax.swing.*;

public class HMRLabel extends JTextArea {
    public HMRLabel(String s) {
        super(s);
        setWrapStyleWord(true);
        setLineWrap(true);
        setEditable(false);
    }
}
