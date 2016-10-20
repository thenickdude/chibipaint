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

import javax.swing.*;

class CPSlider extends JComponent implements MouseListener, MouseMotionListener {

	int value, valueRange;
	int minValue = 0, maxValue;
	String title;

	boolean dragNormal = false, dragPrecise = false;
	int dragPreciseX;
	
	boolean centerMode = false;

	public CPSlider(int valueRange) {
		setBackground(Color.white);

		this.valueRange = valueRange;
		maxValue = valueRange;
		value = valueRange;

		title = "";

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void paint(Graphics g) {
		Dimension d = getSize();

		g.setColor(Color.white);
		g.fillRect(0, 0, d.width, d.height);

		g.setColor(Color.black);
		if (centerMode) {
			if (value >= valueRange /2) {
				g.fillRect(d.width / 2, 0, (value - valueRange/2) * d.width / valueRange, d.height);
			} else {
				g.fillRect(value * d.width / valueRange, 0, (valueRange/2 - value) * d.width / valueRange, d.height);
			}
		} else {
			g.fillRect(0, 0, value * d.width / valueRange, d.height);
		}

		g.setColor(Color.white);
		g.setXORMode(Color.black);
		g.drawString(title, 2, 14);
	}

	public void onValueChange() {
	}

	public void onFinalValueChange() {
	}

	public void setValue(int value) {
		int newValue = Math.max(minValue, Math.min(maxValue, value));
		this.value = newValue;
		onValueChange();
		repaint();
	}

	public void mouseSelect(MouseEvent e) {
		Dimension d = getSize();

		int x = e.getX();
		setValue(x * valueRange / d.width);
	}

	public void mousePressed(MouseEvent e) {
		boolean drag = dragNormal || dragPrecise;
		if (!drag && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			dragNormal = true;
			mouseSelect(e);
		}
		if (!drag && (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
			dragPrecise = true;
			dragPreciseX = e.getPoint().x;
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (dragNormal) {
			mouseSelect(e);
		} else if (dragPrecise) {
			int diff = (e.getPoint().x - dragPreciseX) / 4;
			if (diff != 0) {
				setValue(value + diff);
				dragPreciseX = e.getPoint().x;
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (dragNormal && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			dragNormal = false;
			onFinalValueChange();
		}
		if (dragPrecise && (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
			dragPrecise = false;
			onFinalValueChange();
		}
	}

	// Unused interface methods
	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}
}
