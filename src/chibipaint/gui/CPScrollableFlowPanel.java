/*
	ChibiPaint
    Copyright (c) 2006-2008 Marc Schefer

    This file is part of ChibiPaint.

    ChibiPaint is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ChibiPaint is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ChibiPaint. If not, see <http://www.gnu.org/licenses/>.

 */

package chibipaint.gui;

import java.awt.*;

import javax.swing.*;

/*
 * This allows the creation of a FlowLayout panel that wraps around based on the current width but can be scrolled
 * vertically when needed.
 * 
 * It's based on a forum post by JayDS (http://forum.java.sun.com/thread.jspa?forumID=57&threadID=701797&start=2)
 * Thanks Jay!
 * 
 */

public class CPScrollableFlowPanel extends JPanel implements Scrollable {

	public CPScrollableFlowPanel() {
		setLayout(new FlowLayout(FlowLayout.LEFT));
	}
	
	public JScrollPane wrapInScrollPane() {
		return new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}
	
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, getParent().getWidth(), height);
	}

	public Dimension getPreferredSize() {
		return new Dimension(getWidth(), getPreferredHeight());
	}

	public Dimension getPreferredScrollableViewportSize() {
		return super.getPreferredSize();
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		int hundredth = (orientation == SwingConstants.VERTICAL ?
				getParent().getHeight() : getParent().getWidth()) / 100;
		return (hundredth == 0 ? 1 : hundredth);
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return orientation == SwingConstants.VERTICAL ? getParent().getHeight() : getParent().getWidth();
	}

	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	private int getPreferredHeight() {
		int maxHeight = 0;
		for (int i=0, count=getComponentCount(); i<count; i++) {
			Component component = getComponent(i);
			Rectangle r = component.getBounds();
			int height = r.y + r.height;
			if (height > maxHeight) {
				maxHeight = height;
			}
		}
		maxHeight += ((FlowLayout) getLayout()).getVgap();
		return maxHeight;
	}
}
