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
import chibipaint.util.*;

public class CPLayersPalette extends CPPalette implements CPArtwork.ICPArtworkListener, ActionListener, ItemListener {

	int layerH = 32, eyeW = 24;

	CPLayerWidget lw;
	JScrollPane sp;
	CPAlphaSlider alphaSlider;
	JComboBox blendCombo;

	CPRenameField renameField;

	JCheckBox cbSampleAllLayers;
	JCheckBox cbLockAlpha;

	String modeNames[] = { "Normal", "Multiply", "Add", "Screen", "Lighten", "Darken", "Subtract", "Dodge", "Burn",
			"Overlay", "Hard Light", "Soft Light", "Vivid Light", "Linear Light", "Pin Light" };

	public CPLayersPalette(CPController controller) {
		super(controller);

		title = "Layers";

		// Widgets creation

		Image icons = controller.loadImage("smallicons.gif");

		CPIconButton addButton = new CPIconButton(icons, 16, 16, 0, 1);
		addButton.addCPActionListener(this);
		addButton.setCPActionCommand("CPAddLayer");

		CPIconButton removeButton = new CPIconButton(icons, 16, 16, 1, 1);
		removeButton.addCPActionListener(this);
		removeButton.setCPActionCommand("CPRemoveLayer");

		alphaSlider = new CPAlphaSlider();

		blendCombo = new JComboBox(modeNames);
		blendCombo.addActionListener(this);

		lw = new CPLayerWidget();
		renameField = new CPRenameField();
		lw.add(renameField);
		sp = new JScrollPane(lw);

		cbSampleAllLayers = new JCheckBox("Sample All Layers");
		cbSampleAllLayers.addItemListener(this);

		cbLockAlpha = new JCheckBox("Lock Alpha");
		cbLockAlpha.addItemListener(this);

		// Layout

		// Add/Remove Layer
		Box hb = Box.createHorizontalBox();
		hb.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		hb.add(addButton);
		hb.add(Box.createRigidArea(new Dimension(5, 0)));
		hb.add(removeButton);
		hb.add(Box.createHorizontalGlue());

		// blend mode
		blendCombo.setPreferredSize(new Dimension(100, 16));

		Box hb2 = Box.createHorizontalBox();
		hb2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		hb2.add(blendCombo);
		hb2.add(Box.createHorizontalGlue());

		// layer opacity
		alphaSlider.setPreferredSize(new Dimension(100, 16));
		alphaSlider.setMaximumSize(new Dimension(100, 16));

		Box hb3 = Box.createHorizontalBox();
		hb3.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		hb3.add(alphaSlider);
		hb3.add(Box.createRigidArea(new Dimension(0, 16)));
		hb3.add(Box.createHorizontalGlue());

		Box hb4 = Box.createHorizontalBox();
		hb4.add(cbSampleAllLayers);
		hb4.add(Box.createHorizontalGlue());

		Box hb5 = Box.createHorizontalBox();
		hb5.add(cbLockAlpha);
		hb5.add(Box.createHorizontalGlue());

		Box vb = Box.createVerticalBox();
		vb.add(hb2);
		vb.add(hb3);
		vb.add(hb4);
		vb.add(hb5);

		setLayout(new BorderLayout());
		add(sp, BorderLayout.CENTER);
		add(vb, BorderLayout.PAGE_START);
		add(hb, BorderLayout.PAGE_END);

		// Set initial values
		CPArtwork artwork = controller.getArtwork();
		alphaSlider.setValue(artwork.getActiveLayer().getAlpha());

		// add listeners
		controller.getArtwork().addListener(this);

		// validate();
		// pack();
	}

	public void actionPerformed(ActionEvent e) {
		CPArtwork artwork = controller.getArtwork();
		if (e.getActionCommand().equals("CPAddLayer")) {
			artwork.addLayer();
		}
		if (e.getActionCommand().equals("CPRemoveLayer")) {
			artwork.removeLayer();
		}
		if (e.getSource() == blendCombo) {
			artwork.setBlendMode(artwork.getActiveLayerNb(), blendCombo.getSelectedIndex());
		}
	}

	public void itemStateChanged(ItemEvent e) {
		CPArtwork artwork = controller.getArtwork();
		Object source = e.getItemSelectable();
		if (source == cbSampleAllLayers) {
			artwork.setSampleAllLayers(e.getStateChange() == ItemEvent.SELECTED);
		} else if (source == cbLockAlpha) {
			artwork.setLockAlpha(e.getStateChange() == ItemEvent.SELECTED);
		}
	}

	public void showRenameControl(int layerNb) {
		Dimension d = lw.getSize();
		CPArtwork artwork = controller.getArtwork();
		Object[] layers = artwork.getLayers();

		renameField.setEnabled(true);
		renameField.setVisible(true);
		renameField.requestFocus();

		renameField.setLocation(eyeW, d.height - (layerNb + 1) * layerH);

		renameField.setText(((CPLayer) layers[layerNb]).name);
		renameField.selectAll();

		renameField.layerNb = layerNb;
	}

	public void updateRegion(CPArtwork artwork, CPRect region) {
	}

	public void layerChange(CPArtwork artwork) {
		if (artwork.getActiveLayer().getAlpha() != alphaSlider.value) {
			alphaSlider.setValue(artwork.getActiveLayer().getAlpha());
		}

		if (artwork.getActiveLayer().getBlendMode() != blendCombo.getSelectedIndex()) {
			blendCombo.setSelectedIndex(artwork.getActiveLayer().getBlendMode());
		}

		lw.repaint();
		lw.revalidate();
	}

	class CPLayerWidget extends JComponent implements MouseListener, MouseMotionListener {

		boolean layerDrag, layerDragReally;
		int layerDragNb, layerDragY;

		public CPLayerWidget() {
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		public int getLayerNb(Point p) {
			Dimension d = getSize();
			return (d.height - p.y) / layerH;
		}

		public void update(Graphics g) {
			paint(g);
		}

		public void paintComponent(Graphics g) {
			Graphics g2 = g.create();

			Dimension d = getSize();
			CPArtwork artwork = controller.getArtwork();
			Object[] layers = artwork.getLayers();

			g2.setColor(new Color(0x606060));
			g2.fillRect(0, 0, d.width, d.height - layers.length * layerH);

			g2.setColor(Color.black);
			g2.translate(0, d.height - layerH);
			for (int i = 0; i < layers.length; i++) {
				drawLayer(g2, (CPLayer) layers[i], i == artwork.getActiveLayerNb());
				g2.translate(0, -layerH);
			}

			if (layerDragReally) {
				g2.translate(0, layers.length * layerH - (d.height - layerH));
				g2.drawRect(0, layerDragY - layerH / 2, d.width, layerH);

				int layerOver = (d.height - layerDragY) / layerH;
				if (layerOver <= layers.length && layerOver != layerDragNb && layerOver != layerDragNb + 1) {
					g2.fillRect(0, d.height - layerOver * layerH - 2, d.width, 4);
				}
			}
		}

		private void drawLayer(Graphics g, CPLayer layer, boolean selected) {
			Dimension d = getSize();

			if (selected) {
				g.setColor(new Color(0xB0B0C0));
			} else {
				g.setColor(Color.white);
			}
			g.fillRect(0, 0, d.width, layerH);
			g.setColor(Color.black);
			g.drawLine(0, 0, d.width, 0);
			g.drawLine(eyeW, 0, eyeW, layerH);

			g.drawString(layer.name, eyeW + 6, 12);
			g.drawLine(eyeW + 6, layerH / 2, d.width - 6, layerH / 2);
			g.drawString(modeNames[layer.blendMode] + ": " + layer.alpha + "%", eyeW + 6, 27);

			if (layer.visible) {
				g.fillOval(eyeW / 2 - 5, layerH / 2 - 5, 10, 10);
			} else {
				g.drawOval(eyeW / 2 - 5, layerH / 2 - 5, 10, 10);
			}
		}

		public Dimension getPreferredSize() {
			CPArtwork artwork = controller.getArtwork();
			return new Dimension(60, layerH * artwork.getLayersNb());
		}

		public void mouseClicked(MouseEvent e) {
			Point p = e.getPoint();
			CPArtwork artwork = controller.getArtwork();
			int layerIndex = getLayerNb(p);

			if (e.getClickCount() == 2 && layerIndex >= 0 && layerIndex < artwork.getLayersNb() && p.x > eyeW) {
				showRenameControl(layerIndex);
			} else if (renameField.isVisible()) {
				renameField.renameAndHide();
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			// click, moved from mouseClicked due
			// to problems with focus and stuff
			if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
				Point p = e.getPoint();
				CPArtwork artwork = controller.getArtwork();

				int layerIndex = getLayerNb(p);
				if (layerIndex >= 0 && layerIndex < artwork.getLayersNb()) {
					CPLayer layer = artwork.getLayer(layerIndex);
					if (p.x < eyeW) {
						artwork.setLayerVisibility(layerIndex, !layer.visible);
					} else {
						artwork.setActiveLayer(layerIndex);
					}

				}
			}

			if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
				Dimension d = getSize();
				CPArtwork artwork = controller.getArtwork();
				Object[] layers = artwork.getLayers();

				int layerOver = (d.height - e.getPoint().y) / layerH;
				if (layerOver < layers.length) {
					layerDrag = true;
					layerDragY = e.getPoint().y;
					layerDragNb = layerOver;
					repaint();
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (layerDrag && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
				Dimension d = getSize();
				CPArtwork artwork = controller.getArtwork();
				Object[] layers = artwork.getLayers();

				layerDrag = true;
				layerDragY = e.getPoint().y;
				int layerOver = (d.height - layerDragY) / layerH;

				if (layerOver >= 0 && layerOver <= layers.length && layerOver != layerDragNb
						&& layerOver != layerDragNb + 1) {
					artwork.moveLayer(layerDragNb, layerOver);
				}

				layerDrag = false;
				layerDragReally = false;
				repaint();
			}
		}

		public void mouseDragged(MouseEvent e) {
			if (layerDrag) {
				layerDragReally = true;
				layerDragY = e.getPoint().y;
				repaint();
			}
		}

		public void mouseMoved(MouseEvent e) {
		}
	}

	class CPAlphaSlider extends CPSlider {

		public CPAlphaSlider() {
			super(100);
			minValue = 0;
		}

		public void onValueChange() {
			title = "Opacity: " + value + "%";
			CPArtwork artwork = controller.getArtwork();
			artwork.setLayerAlpha(artwork.getActiveLayerNb(), value);
		}
	}

	class CPRenameField extends JTextField implements FocusListener, ActionListener {

		int layerNb;

		public CPRenameField() {
			setSize(new Dimension(100, 20));

			layerNb = -1;
			setVisible(false);
			setEnabled(false);

			addActionListener(this);
			addFocusListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			renameAndHide();
		}

		public void focusGained(FocusEvent e) {
			// FIXME: hack to avoid losing the focus to the main canvas
			controller.canvas.dontStealFocus = true;
		}

		public void focusLost(FocusEvent e) {
			controller.canvas.dontStealFocus = false;
			renameAndHide();
		}

		public void renameAndHide() {
			CPArtwork artwork = controller.getArtwork();

			if (layerNb >= 0 && layerNb < artwork.getLayersNb()) {
				artwork.setLayerName(layerNb, getText());
			}

			layerNb = -1;
			setVisible(false);
			setEnabled(false);
		}
	}
}
