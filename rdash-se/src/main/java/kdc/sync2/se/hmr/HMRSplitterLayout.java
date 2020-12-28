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
        add(a);
        add(b);
        layout();
        setSize(getPreferredSize());
    }

    @Override
    public void layout() {
        super.layout();

        int room, allSpace;
        Dimension r = getSize();
        Dimension aWanted = cA.getPreferredSize(), bWanted = cB.getPreferredSize();
        int aInitial;
        int bInitial;
        if (vertical) {
            allSpace = room = r.height;
            aInitial = aWanted.height;
            bInitial = bWanted.height;
        } else {
            allSpace = room = r.width;
            aInitial = aWanted.width;
            bInitial = bWanted.width;
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
            // since 1.0d/0.0d are used on elements that should use exactly what they want and no more/less
            if (splitPoint >= 0.5d) {
                exactPos = allSpace - bInitial;
            } else {
                exactPos = aInitial;
            }
            // That's not working? go to minimum usability mode
            if ((exactPos < 0) || (exactPos > allSpace))
                exactPos = allSpace / 2;
        }

        Dimension newWanted;
        if (vertical) {
            cA.setBounds(0, 0, r.width, exactPos);
            cB.setBounds(0, exactPos, r.width, allSpace - exactPos);
            newWanted = new Dimension(Math.max(aWanted.width, bWanted.width), aInitial + bInitial);
        } else {
            cA.setBounds(0, 0, exactPos, r.height);
            cB.setBounds(exactPos, 0, allSpace - exactPos, r.height);
            newWanted = new Dimension(aInitial + bInitial, Math.max(aWanted.height, bWanted.height));
        }
        setPreferredSize(newWanted);
    }
}
