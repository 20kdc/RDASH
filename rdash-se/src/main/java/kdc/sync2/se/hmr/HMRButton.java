/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.se.hmr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A button that's easier to write.
 * Created on July 23, 2018.
 */
public class HMRButton extends JButton {
    public Runnable callback;
    public HMRButton(final String ok, final Runnable runnable) {
        super(ok);
        callback = runnable;
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
        	if (callback != null)
        	    callback.run();
            }
        });
    }
}
