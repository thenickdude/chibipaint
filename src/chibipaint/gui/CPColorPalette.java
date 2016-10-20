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
import java.awt.image.*;

import javax.swing.*;

import chibipaint.*;
import chibipaint.util.*;

public class CPColorPalette extends CPPalette implements chibipaint.CPController.ICPColorListener {

	CPColor curColor = new CPColor();
	CPColorSelect colorSelect;
	CPColorSlider colorSlider;
	CPColorShow colorShow;

	public CPColorPalette(CPController controller) {
		super(controller);

		// setSize(175, 185);

		title = "Color";
		// setBounds(getInnerDimensions());

		setLayout(new FlowLayout());
		colorSelect = new CPColorSelect();
		add(colorSelect);
		colorSlider = new CPColorSlider(colorSelect);
		add(colorSlider);

		colorShow = new CPColorShow();
		colorShow.setPreferredSize(new Dimension(160, 20));
		colorShow.color = controller.getCurColorRgb();
		add(colorShow);

		controller.addColorListener(this);
	}

	public void newColor(CPColor color) {
		if (!curColor.isEqual(color)) {
			curColor.copyFrom(color);
			colorSelect.setColor(color);
			colorSlider.setHue(curColor.getHue());
		}
		colorShow.color = color.getRgb();
		colorShow.repaint();
	}

	public class CPColorSelect extends JComponent implements MouseListener, MouseMotionListener {

		int[] data;
		int w, h;
		Image img;
		CPColor color;
		boolean needRefresh;

		public CPColorSelect() {
			w = h = 128;
			setBackground(Color.black); // tmp to help see refresh problems
			setSize(new Dimension(w, h));

			data = new int[w * h];
			img = createImage(new MemoryImageSource(w, h, data, 0, w));
			color = new CPColor();

			makeBitmap();
			needRefresh = false;

			addMouseListener(this);
			addMouseMotionListener(this);
		}

		public void setHue(int hue) {
			if (color.getHue() != hue) {
				color.setHue(hue);
				needRefresh = true;
				repaint();
				controller.setCurColor(color);
			}
		}

		public void setColor(CPColor newColor) {
			if (!color.isEqual(newColor)) {
				color.copyFrom(newColor);
				needRefresh = true;
				repaint();
				controller.setCurColor(color);
			}
		}

		void makeBitmap() {
			CPColor col = (CPColor) color.clone();
			for (int j = 0; j < h; j++) {
				col.setValue(255 - (j * 255) / h);
				for (int i = 0; i < w; i++) {
					col.setSaturation((i * 255) / w);
					data[i + j * w] = 0xff000000 | col.rgb;
				}
			}
		}

		public void update(Graphics g) {
			paint(g);
		}

		public void paint(Graphics g) {
			if (needRefresh) {
				makeBitmap();
				needRefresh = false;
			}
			img.flush();
			g.drawImage(img, 0, 0, Color.red, null);

			int x = color.getSaturation() * w / 255;
			int y = (255 - color.getValue()) * h / 255;
			g.setColor(Color.white);
			g.setXORMode(Color.black);
			g.drawOval(x - 5, y - 5, 10, 10);
		}

		public void mouseSelect(MouseEvent e) {
			int sat = e.getX() * 255 / w;
			int value = 255 - e.getY() * 255 / h;

			color.setSaturation(Math.max(0, Math.min(255, sat)));
			color.setValue(Math.max(0, Math.min(255, value)));

			repaint();
			controller.setCurColor(color);
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			mouseSelect(e);
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
			mouseSelect(e);
		}

		public Dimension getPreferredSize() {
			return new Dimension(w, h);
		}
	}

	public class CPColorSlider extends JComponent implements MouseListener, MouseMotionListener {

		int[] data;
		int w, h;
		Image img;
		int hue;

		CPColorSelect selecter;

		public CPColorSlider(CPColorSelect selecter) {
			w = 24;
			h = 128;

			this.selecter = selecter;

			setBackground(Color.black); // tmp to help see refresh problems
			setSize(new Dimension(w, h));

			data = new int[w * h];
			img = createImage(new MemoryImageSource(w, h, data, 0, w));
			hue = 0;

			makeBitmap();

			addMouseListener(this);
			addMouseMotionListener(this);
		}

		void makeBitmap() {
			CPColor color = new CPColor(0, 255, 255);
			for (int j = 0; j < h; j++) {
				color.setHue((j * 359) / h);
				for (int i = 0; i < w; i++) {
					data[i + j * w] = 0xff000000 | color.rgb;
				}
			}
		}

		public void update(Graphics g) {
			paint(g);
		}

		public void paint(Graphics g) {
			img.flush();
			g.drawImage(img, 0, 0, Color.red, null);

			int y = (hue * h) / 360;
			g.setColor(Color.white);
			g.setXORMode(Color.black);
			g.drawLine(0, y, w, y);
		}

		public void mouseSelect(MouseEvent e) {
			int _hue = e.getY() * 360 / h;
			hue = Math.max(0, Math.min(359, _hue));
			repaint();

			if (selecter != null) {
				selecter.setHue(hue);
				// controller.setCurColor(color.GetRgb());
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			mouseSelect(e);
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
			mouseSelect(e);
		}

		private void setHue(int h) {
			hue = h;
			repaint();
		}

		public Dimension getPreferredSize() {
			return new Dimension(w, h);
		}
	}

	public class CPColorShow extends JComponent {

		int color;

		public void update(Graphics g) {
			paint(g);
		}

		public void paint(Graphics g) {
			Dimension d = getSize();
			g.setColor(new Color(color));
			g.fillRect(0, 0, d.width, d.height);
		}
	}
}
