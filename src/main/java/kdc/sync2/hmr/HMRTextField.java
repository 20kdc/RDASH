/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.hmr;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Because the ordinary text field doesn't properly update size as you type in it.
 * Created on July 23, 2018.
 */
public class HMRTextField extends JTextField {
    public HMRTextField(String text) {
        super(text);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                parentDoLayout();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                parentDoLayout();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                parentDoLayout();
            }

            private void parentDoLayout() {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getParent().doLayout();
                    }
                });
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // Ugly, but it works for now and is necessary to avoid backstabs
        if (d.width < 256)
            d.width = 256;
        return d;
    }
}
