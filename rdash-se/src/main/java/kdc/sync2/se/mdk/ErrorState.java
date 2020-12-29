package kdc.sync2.se.mdk;

import java.awt.Container;

import kdc.sync2.se.hmr.HMRLabel;
import kdc.sync2.se.hmr.HMRState;

public class ErrorState implements HMRState.ResizableHMRState {
    private String txt;

    public ErrorState(String text) {
	txt = text;
    }

    @Override
    public Container createUI() {
    	return new HMRLabel("An error has occurred. The synchronization state may be corrupt.\nPlease pay close attention to the synchronized state.\nException details:\n" + txt);
    }

    @Override
    public void onClose() {
	System.exit(1);
    }
}
