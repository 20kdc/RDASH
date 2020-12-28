/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.se.hmr;

import javax.swing.*;
import java.awt.*;

/**
 * Created on July 22, 2018.
 */
public interface HMRState {
    Container createUI();
    void onClose();
    interface ResizableHMRState extends HMRState {
    }
}
