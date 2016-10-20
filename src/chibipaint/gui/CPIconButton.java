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
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class CPIconButton extends JComponent implements MouseListener {

	Image icons;
	int iconW, iconH, iconIndex, border;
	String actionCommand, actionCommandDoubleClick = null;
	LinkedList actionListeners = new LinkedList();

	boolean mouseOver = false, mousePressed = false, selected = false;
	boolean onClickDown = false;

	public CPIconButton(Image icons, int iconW, int iconH, int iconIndex, int border) {
		this.icons = icons;
		this.iconW = iconW;
		this.iconH = iconH;
		this.iconIndex = iconIndex;
		this.border = border;

		MediaTracker tracker = new MediaTracker(this);
		tracker.addImage(icons, 0);
		try {
			tracker.waitForAll();
		} catch (Exception ignored) {
		}

		addMouseListener(this);
	}

	public void setSelected(boolean s) {
		if (selected != s) {
			selected = s;
			repaint();
		}
	}

	public void paint(Graphics g) {
		Dimension d = getSize();
		g.drawImage(icons, border, border, iconW + border, iconH + border, 0, iconIndex * iconH, iconW, (iconIndex + 1)
				* iconH, null);

		if (mouseOver && !mousePressed) {
			g.setColor(Color.orange);
		} else if (selected || mousePressed) {
			g.setColor(Color.red);
		} else {
			g.setColor(Color.black);
		}

		g.drawRect(0, 0, d.width - 1, d.height - 1);
	}

	public void mouseClicked(MouseEvent e) {
		if (!onClickDown && e.getClickCount() == 2 && actionCommandDoubleClick != null) {
			callActionListenersDouble();
		}
	}

	public void mouseEntered(MouseEvent e) {
		mouseOver = true;
		repaint();
	}

	public void mouseExited(MouseEvent e) {
		mouseOver = false;
		repaint();
	}

	public void mousePressed(MouseEvent e) {
		requestFocusInWindow();

		mousePressed = true;
		repaint();
		if (onClickDown) {
			callActionListeners();
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (!onClickDown && mouseOver) {
			callActionListeners();
		}
		mousePressed = false;
		repaint();
	}

	void addCPActionListener(ActionListener l) {
		actionListeners.addLast(l);
	}

	void setCPActionCommand(String command) {
		actionCommand = command;
	}

	public void setCPActionCommandDouble(String action) {
		actionCommandDoubleClick = action;
	}

	public void callActionListeners() {
		for (Object l : actionListeners) {
			ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand);
			((ActionListener) l).actionPerformed(e);
		}
	}

	public void callActionListenersDouble() {
		for (Object l : actionListeners) {
			ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommandDoubleClick);
			((ActionListener) l).actionPerformed(e);
		}
	}

	public Dimension getPreferredSize() {
		return new Dimension(iconW + 2 * border, iconH + 2 * border);
	}

	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

}
