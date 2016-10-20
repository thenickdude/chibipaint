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

import chibipaint.*;
import chibipaint.engine.*;

public class CPBrushPalette extends CPPalette implements CPController.ICPToolListener, ActionListener {

	CPAlphaSlider alphaSlider;
	CPSizeSlider sizeSlider;

	CPCheckBox alphaCB, sizeCB, scatteringCB;
	CPSlider resatSlider, bleedSlider, spacingSlider, scatteringSlider, smoothingSlider;

	JComboBox tipCombo;
	String tipNames[] = { "Round Pixelated", "Round Hard Edge", "Round Soft", "Square Pixelated", "Square Hard Edge" };

	@SuppressWarnings("serial")
	public CPBrushPalette(CPController ctrlr) {
		super(ctrlr);

		setSize(160, 270);

		title = "Brush";
		// setBounds(getInnerDimensions());

		setLayout(null);

		alphaSlider = new CPAlphaSlider();
		alphaSlider.setLocation(20, 120);
		alphaSlider.setSize(130, 16);
		add(alphaSlider);

		CPBrushPreview brushPreview = new CPBrushPreview();
		brushPreview.setLocation(5, 25);
		add(brushPreview);

		// Label l = new Label("Opacity: ");
		// c.add(l);
		// l.setLocation(5, 70);

		alphaCB = new CPAlphaCB();
		alphaCB.setLocation(2, 120);
		alphaCB.setSize(16, 16);
		add(alphaCB);

		sizeSlider = new CPSizeSlider();
		sizeSlider.setLocation(20, 95);
		sizeSlider.setSize(130, 16);
		add(sizeSlider);

		sizeCB = new CPSizeCB();
		sizeCB.setLocation(2, 95);
		sizeCB.setSize(16, 16);
		add(sizeCB);

		tipCombo = new JComboBox(tipNames);
		tipCombo.addActionListener(this);
		tipCombo.setLocation(5, 5);
		tipCombo.setSize(120, 16);
		add(tipCombo);

		resatSlider = new CPSlider(100) {

			public void onValueChange() {
				controller.getBrushInfo().resat = value / 100f;
				controller.callToolListeners();
				title = "Color: " + value + "%";
			}
		};
		resatSlider.setLocation(20, 145);
		resatSlider.setSize(130, 16);
		add(resatSlider);

		bleedSlider = new CPSlider(100) {

			public void onValueChange() {
				controller.getBrushInfo().bleed = value / 100f;
				controller.callToolListeners();
				title = "Blend: " + value + "%";
			}
		};
		bleedSlider.setLocation(20, 170);
		bleedSlider.setSize(130, 16);
		add(bleedSlider);

		spacingSlider = new CPSlider(100) {

			public void onValueChange() {
				controller.getBrushInfo().spacing = value / 100f;
				controller.callToolListeners();
				title = "Spacing: " + value + "%";
			}
		};
		spacingSlider.setLocation(20, 195);
		spacingSlider.setSize(130, 16);
		add(spacingSlider);

		scatteringCB = new CPCheckBox() {

			public void onValueChange() {
				controller.getBrushInfo().pressureScattering = state;
				controller.callToolListeners();
			}
		};
		scatteringCB.setLocation(2, 220);
		scatteringCB.setSize(16, 16);
		add(scatteringCB);

		scatteringSlider = new CPSlider(1000) {

			public void onValueChange() {
				controller.getBrushInfo().scattering = value / 100f;
				controller.callToolListeners();
				title = "Scattering: " + value + "%";
			}
		};
		scatteringSlider.setLocation(20, 220);
		scatteringSlider.setSize(130, 16);
		add(scatteringSlider);

		smoothingSlider = new CPSlider(100) {

			public void onValueChange() {
				controller.getBrushInfo().smoothing = value / 100f;
				controller.callToolListeners();
				title = "Smoothing: " + value + "%";
			}
		};
		smoothingSlider.setLocation(20, 245);
		smoothingSlider.setSize(130, 16);
		add(smoothingSlider);

		alphaSlider.setValue(ctrlr.getAlpha());
		sizeSlider.setValue(ctrlr.getBrushSize());
		sizeCB.setValue(ctrlr.getBrushInfo().pressureSize);
		alphaCB.setValue(ctrlr.getBrushInfo().pressureAlpha);
		tipCombo.setSelectedIndex(ctrlr.getBrushInfo().type);

		resatSlider.setValue((int) (ctrlr.getBrushInfo().resat * 100));
		bleedSlider.setValue((int) (ctrlr.getBrushInfo().bleed * 100));
		spacingSlider.setValue((int) (ctrlr.getBrushInfo().spacing * 100));
		scatteringCB.setValue(ctrlr.getBrushInfo().pressureScattering);
		scatteringSlider.setValue((int) (ctrlr.getBrushInfo().scattering * 100));
		smoothingSlider.setValue((int) (ctrlr.getBrushInfo().smoothing * 100));

		ctrlr.addToolListener(this);
	}

	public void newTool(int tool, CPBrushInfo toolInfo) {
		if (toolInfo.alpha != alphaSlider.value) {
			alphaSlider.setValue(toolInfo.alpha);
		}

		if (toolInfo.size != sizeSlider.value) {
			sizeSlider.setValue(toolInfo.size);
		}

		if (toolInfo.pressureSize != sizeCB.state) {
			sizeCB.setValue(toolInfo.pressureSize);
		}

		if (toolInfo.pressureAlpha != alphaCB.state) {
			alphaCB.setValue(toolInfo.pressureAlpha);
		}

		if (toolInfo.type != tipCombo.getSelectedIndex()) {
			tipCombo.setSelectedIndex(toolInfo.type);
		}

		if ((int) (toolInfo.resat * 100.f) != resatSlider.value) {
			resatSlider.setValue((int) (toolInfo.resat * 100.f));
		}

		if ((int) (toolInfo.bleed * 100.f) != bleedSlider.value) {
			bleedSlider.setValue((int) (toolInfo.bleed * 100.f));
		}

		if ((int) (toolInfo.spacing * 100.f) != spacingSlider.value) {
			spacingSlider.setValue((int) (toolInfo.spacing * 100.f));
		}

		if (toolInfo.pressureScattering != scatteringCB.state) {
			scatteringCB.setValue(toolInfo.pressureScattering);
		}

		if ((int) (toolInfo.scattering * 100.f) != scatteringSlider.value) {
			scatteringSlider.setValue((int) (toolInfo.scattering * 100.f));
		}

		if ((int) (toolInfo.smoothing * 100.f) != smoothingSlider.value) {
			smoothingSlider.setValue((int) (toolInfo.smoothing * 100.f));
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == tipCombo) {
			controller.getBrushInfo().type = tipCombo.getSelectedIndex();
		}
	}

	class CPBrushPreview extends JComponent implements MouseListener, MouseMotionListener, CPController.ICPToolListener {

		int w, h;
		int size;

		public CPBrushPreview() {
			w = h = 64;
			setBackground(Color.white);
			setSize(new Dimension(w, h));

			addMouseListener(this);
			addMouseMotionListener(this);
			controller.addToolListener(this);

			size = 16;
		}

		public void paint(Graphics g) {
			g.drawOval(w / 2 - size / 2, h / 2 - size / 2, size, size);
		}

		public void mouseSelect(MouseEvent e) {
			int x = e.getX() - w / 2;
			int y = e.getY() - h / 2;

			int newSize = (int) Math.sqrt((x * x + y * y)) * 2;
			size = Math.max(1, Math.min(200, newSize));

			repaint();
			controller.setBrushSize(size);
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

		public void newTool(int tool, CPBrushInfo toolInfo) {
			if (toolInfo.size != size) {
				size = toolInfo.size;
				repaint();
			}
		}
	}

	class CPAlphaSlider extends CPSlider {

		public CPAlphaSlider() {
			super(255);
			minValue = 1;
		}

		public void onValueChange() {
			controller.setAlpha(value);
			title = "Opacity: " + value;
		}
	}

	class CPSizeSlider extends CPSlider {

		public CPSizeSlider() {
			super(200);
			minValue = 1;
		}

		public void onValueChange() {
			controller.setBrushSize(value);
			title = "Brush Size: " + value;
		}
	}

	class CPCheckBox extends JComponent implements MouseListener {

		boolean state = false;

		public CPCheckBox() {
			addMouseListener(this);
		}

		public void paint(Graphics g) {
			Dimension d = getSize();

			if (state) {
				g.fillOval(3, 3, d.width - 5, d.height - 5);
			} else {
				g.drawOval(3, 3, d.width - 6, d.height - 6);
			}
		}

		public void setValue(boolean b) {
			state = b;
			onValueChange();
			repaint();
		}

		public void onValueChange() {
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			setValue(!state);
		}

		public void mouseReleased(MouseEvent e) {
		}
	}

	class CPAlphaCB extends CPCheckBox {

		public void onValueChange() {
			controller.getBrushInfo().pressureAlpha = state;
			controller.callToolListeners();
		}
	}

	class CPSizeCB extends CPCheckBox {

		public void onValueChange() {
			controller.getBrushInfo().pressureSize = state;
			controller.callToolListeners();
		}
	}

}

// setLocation((int) (e.getX() + getLocation().getX()), (int) (e.getY() + getLocation().getY()));
