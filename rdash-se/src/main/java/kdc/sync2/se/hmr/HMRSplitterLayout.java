/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.se.hmr;

import javax.swing.*;
import java.awt.*;

/**
 * A badly made but working implementation of the SplitterLayout design.
 * Created on July 22, 2018.
 */
public class HMRSplitterLayout extends JPanel {
    private final Component cA, cB;
    private final boolean vertical;
    private final double splitPoint;

    public HMRSplitterLayout(Component a, Component b, boolean v, double w) {
	cA = a;
	cB = b;
	vertical = v;
	splitPoint = w;
	// Use BoxLayout to calculate preferred sizes, but then modify
	setLayout(new BoxLayout(this, vertical ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS));
	setLayout(new InternalLayoutManager());
	add(a);
	add(b);
	doLayout();
	setSize(getPreferredSize());
    }

    private class InternalLayoutManager implements LayoutManager {
	private Rectangle aBounds = new Rectangle(0, 0, 1, 1);
	private Rectangle bBounds = new Rectangle(0, 0, 1, 1);
	private Dimension minimumSize = new Dimension(1, 1);
	private Dimension preferredSize = new Dimension(1, 1);
	private void performRecalculation() {
	    int room, allSpace;
	    Dimension r = getSize();
	    Dimension aWanted = cA.getPreferredSize(), bWanted = cB.getPreferredSize();
	    Dimension aMinimum = cA.getMinimumSize(), bMinimum = cB.getMinimumSize();
	    int aInitial;
	    int bInitial;
	    int aInitialMinimum;
	    int bInitialMinimum;
	    if (vertical) {
		allSpace = room = r.height;
		aInitial = aWanted.height;
		bInitial = bWanted.height;
		aInitialMinimum = aMinimum.height;
		bInitialMinimum = bMinimum.height;
	    } else {
		allSpace = room = r.width;
		aInitial = aWanted.width;
		bInitial = bWanted.width;
		aInitialMinimum = aMinimum.width;
		bInitialMinimum = bMinimum.width;
	    }
	    room -= aInitial + bInitial;
	    // Room is now the amount of spare space available.
	    int exactPos = (int) (splitPoint * allSpace);
	    if (room >= 0) {
		// If we *can* table-align, do so, but give that up if need be
		boolean newAlg = ((exactPos >= aInitial) && (exactPos <= (allSpace - bInitial)));
		int oldAlg = ((int) (splitPoint * room)) + aInitial;
		if (!newAlg)
		    exactPos = oldAlg;
	    } else {
		// Prioritize the element that's given the least room,
		// since 1.0d/0.0d are used on elements that should use exactly what they want
		// and no more/less
		if (splitPoint >= 0.5d) {
		    exactPos = allSpace - bInitial;
		} else {
		    exactPos = aInitial;
		}
		// That's not working? go to minimum usability mode
		if ((exactPos < 0) || (exactPos > allSpace))
		    exactPos = allSpace / 2;
	    }

	    if (vertical) {
		aBounds = new Rectangle(0, 0, r.width, exactPos);
		bBounds = new Rectangle(0, exactPos, r.width, allSpace - exactPos);
		preferredSize = new Dimension(Math.max(aWanted.width, bWanted.width), aInitial + bInitial);
		minimumSize = new Dimension(Math.max(aMinimum.width, bMinimum.width), aInitialMinimum + bInitialMinimum);
	    } else {
		aBounds = new Rectangle(0, 0, exactPos, r.height);
		bBounds = new Rectangle(exactPos, 0, allSpace - exactPos, r.height);
		preferredSize = new Dimension(aInitial + bInitial, Math.max(aWanted.height, bWanted.height));
		minimumSize = new Dimension(aInitialMinimum + bInitialMinimum, Math.max(aMinimum.height, bMinimum.height));
	    }
	}

	@Override
	public void addLayoutComponent(String arg0, Component arg1) {
	}

	@Override
	public void layoutContainer(Container arg0) {
	    performRecalculation();
	    cA.setBounds(aBounds);
	    cB.setBounds(bBounds);
	}

	@Override
	public Dimension minimumLayoutSize(Container arg0) {
	    performRecalculation();
	    return minimumSize;
	}

	@Override
	public Dimension preferredLayoutSize(Container arg0) {
	    performRecalculation();
	    return preferredSize;
	}

	@Override
	public void removeLayoutComponent(Component arg0) {
	}
    }
}
