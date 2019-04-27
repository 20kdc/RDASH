/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.mdk;

import kdc.sync2.Operation;
import kdc.sync2.OperationLists;
import kdc.sync2.ServerLayout;
import kdc.sync2.Synchronizer;
import kdc.sync2.hmr.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;

/**
 * First MDK state, asks for hostname
 * Created on July 22, 2018.
 */
public class RequestHostnameState implements HMRState {
    public HMRFrame frame;
    public boolean noHost;
    public RequestHostnameState(HMRFrame f, boolean nh) {
        frame = f; noHost = nh;
    }

    @Override
    public String toString() {
        return "Enter Hostname...";
    }

    @Override
    public Container createUI() {
        final HMRTextField tf;
        return new HMRSplitterLayout(tf = new HMRTextField(""), new HMRButton("OK", new Runnable() {
            @Override
            public void run() {
                if (tf.getText().length() == 0) {
                    frame.startDialog("There is no hostname.");
                } else if (!new File(tf.getText()).isDirectory()) {
                    // This check relies on a detail... :(
                    frame.startDialog("The directory does not exist. Please create it.");
                } else {
                    final ServerLayout theServ = new ServerLayout(tf.getText());
                    final Synchronizer theSync = new Synchronizer(theServ);
                    final OperationLists ol = new OperationLists();
                    frame.reset(new ExecutingOperationState(frame, theSync.prepareSync(noHost, ol), new OperationPlannerState(frame, ol, RequestHostnameState.this)));
                }
            }
        }), false, 1d);
    }

    @Override
    public void onClose() {
        System.exit(0);
    }
}
