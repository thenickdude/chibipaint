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
import java.util.*;

import javax.swing.*;

import chibipaint.*;
import chibipaint.engine.*;

public class CPTexturePalette extends CPPalette {

	Vector<CPGreyBmp> textures = new Vector<CPGreyBmp>();
	CPGreyBmp selectedTexture, processedTexture;

	CPOptionsPanel optionsPanel;
	boolean mirror = false, inverse = false;
	float brightness = 0f, contrast = 0f;

	public CPTexturePalette(CPController controller) {
		super(controller);
		title = "Textures";

		makeProceduralTextures();
		loadTextures("textures32.png", 32, 32, 2);

		setLayout(new BorderLayout());

		// options panel

		optionsPanel = new CPOptionsPanel();

		add(optionsPanel, BorderLayout.WEST);

		// texture panel

		CPScrollableFlowPanel texturesPanel = new CPScrollableFlowPanel();

		JButton button = new CPTextureButton();
		button.setAction(new CPTextureButtonAction(null));
		texturesPanel.add(button);

		for (CPGreyBmp texture : textures) {
			button = new CPTextureButton();
			button.setAction(new CPTextureButtonAction(texture));
			texturesPanel.add(button);
		}

		JScrollPane scrollPane = texturesPanel.wrapInScrollPane();

		scrollPane.setBackground(texturesPanel.getBackground());
		
		add(scrollPane, BorderLayout.CENTER);
	}

	void makeProceduralTextures() {
		CPGreyBmp texture = new CPGreyBmp(2, 2);
		texture.data[0] = (byte) 255;
		texture.data[3] = (byte) 255;
		textures.add(texture);

		textures.add(makeDotTexture(2));
		textures.add(makeDotTexture(3));
		textures.add(makeDotTexture(4));
		textures.add(makeDotTexture(6));
		textures.add(makeDotTexture(8));

		textures.add(makeVertLinesTexture(1, 2));
		textures.add(makeVertLinesTexture(2, 4));

		textures.add(makeHorizLinesTexture(1, 2));
		textures.add(makeHorizLinesTexture(2, 4));

		textures.add(makeCheckerBoardTexture(2));
		textures.add(makeCheckerBoardTexture(4));
		textures.add(makeCheckerBoardTexture(8));
		textures.add(makeCheckerBoardTexture(16));
	}

	void loadTextures(String textureFilename, int width, int height, int nb) {
		Image img = controller.loadImage(textureFilename);

		// block until image is loaded
		MediaTracker tracker = new MediaTracker(this);
		tracker.addImage(img, 0);
		try {
			tracker.waitForAll();
		} catch (Exception ignored) {
		}

		int[] data = new int[width * height];

		for (int i = 0; i < nb; i++) {
			PixelGrabber grabber = new PixelGrabber(img, 0, i * height, width, height, data, 0, width);
			try {
				grabber.grabPixels();
			} catch (InterruptedException e) {
			}

			CPGreyBmp texture = new CPGreyBmp(width, height);
			for (int j = 0; j < width * height; j++) {
				texture.data[j] = (byte) (data[j] & 0xff);
			}

			textures.add(texture);
		}
	}

	CPGreyBmp makeDotTexture(int size) {
		CPGreyBmp texture = new CPGreyBmp(size, size);
		for (int i = 1; i < size * size; i++) {
			texture.data[i] = (byte) 255;
		}
		return texture;
	}

	CPGreyBmp makeCheckerBoardTexture(int size) {
		int textureSize = 2 * size;
		CPGreyBmp texture = new CPGreyBmp(textureSize, textureSize);
		for (int i = 0; i < textureSize; i++) {
			for (int j = 0; j < textureSize; j++) {
				texture.data[i + j * textureSize] = (((i / size) + (j / size)) % 2 == 0) ? (byte) 0 : (byte) 255;
			}
		}
		return texture;
	}

	CPGreyBmp makeVertLinesTexture(int lineSize, int size) {
		CPGreyBmp texture = new CPGreyBmp(size, size);
		for (int i = 0; i < size * size; i++) {
			if (i % size >= lineSize) {
				texture.data[i] = (byte) 255;
			}
		}
		return texture;
	}

	CPGreyBmp makeHorizLinesTexture(int lineSize, int size) {
		CPGreyBmp texture = new CPGreyBmp(size, size);
		for (int i = 0; i < size * size; i++) {
			if (i / size >= lineSize) {
				texture.data[i] = (byte) 255;
			}
		}
		return texture;
	}

	void selectTexture(CPGreyBmp texture) {
		selectedTexture = texture;
		processTexture();
	}

	void processTexture() {
		if (selectedTexture != null) {
			processedTexture = new CPGreyBmp(selectedTexture);

			if (mirror) {
				processedTexture.mirrorHorizontally();
			}

			CPLookUpTable lut = new CPLookUpTable(brightness, contrast);

			if (inverse) {
				lut.inverse();
			}

			processedTexture.applyLUT(lut);
		} else {
			processedTexture = null;
		}

		controller.getArtwork().brushManager.setTexture(processedTexture);
		if (optionsPanel != null) {
			optionsPanel.updateTexture();
		}
	}

	Image createTextureImage(CPGreyBmp texture, int width, int height) {
		int[] buffer = new int[width * height];
		for (int i = 0; i < width * height; i++) {
			buffer[i] = texture.data[texture.getWidth() * (i / width % texture.getHeight())
					+ (i % width % texture.getWidth())];
			buffer[i] = 0xff << 24 | (buffer[i] & 0xff) << 16 | (buffer[i] & 0xff) << 8 | (buffer[i] & 0xff);
		}
		return createImage(new MemoryImageSource(width, height, buffer, 0, width));
	}

	public boolean getResizable() {
		return true;
	}
	
	class CPTextureButton extends JButton {

		private static final int width = 32;
		private static final int height = 32;

		public CPTextureButton() {
			super("Test");
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();

			if (getAction() instanceof CPTextureButtonAction) {
				CPGreyBmp texture = ((CPTextureButtonAction) getAction()).texture;

				if (texture != null) {
					g2d.drawImage(createTextureImage(texture, width, height), 0, 0, null);
				} else {
					g2d.setColor(Color.white);
					g2d.fill(new Rectangle(width, height));
				}
			}
		}

		public Dimension getPreferredSize() {
			return new Dimension(width, height);
		}

		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		public Dimension getMinimumSize() {
			return getPreferredSize();
		}
	}

	class CPTextureButtonAction extends AbstractAction {
		CPGreyBmp texture;

		public CPTextureButtonAction(CPGreyBmp texture) {
			this.texture = texture;
		}

		public void actionPerformed(ActionEvent e) {
			selectTexture(texture);
		}
	}

	class CPOptionsPanel extends JPanel {

		private JCheckBox cbInverse;
		private JCheckBox cbMirror;
		private CPSlider slBrightness;
		private CPSlider slContrast;

		private CPTextureSample sample;
		private JButton btnCustomize;

		public CPOptionsPanel() {

			setLayout(new WrapLayout());

			sample = new CPTextureSample();
			add(sample);

			final Box pnlTextureControls = Box.createVerticalBox();

			btnCustomize = new JButton("Customize");

			btnCustomize.setMargin(new Insets(0,2,0,2));
			btnCustomize.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JPopupMenu pop = new JPopupMenu();
					pop.setBorder(BorderFactory.createCompoundBorder(pop.getBorder(),BorderFactory.createEmptyBorder(4, 4, 4, 4)));
					pop.setLayout(new BorderLayout());
					pop.add(pnlTextureControls, BorderLayout.CENTER);
					pop.show(btnCustomize, btnCustomize.getWidth(), 0);
				}
			});
			btnCustomize.setEnabled(false);
			add(btnCustomize);

			cbInverse = new JCheckBox("Inverse");
			cbInverse.setAlignmentX(Component.LEFT_ALIGNMENT);
			cbInverse.addActionListener(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					inverse = ((JCheckBox) e.getSource()).isSelected();
					processTexture();
				}
			});
			pnlTextureControls.add(cbInverse);

			cbMirror = new JCheckBox("Mirror");
			cbMirror.setAlignmentX(Component.LEFT_ALIGNMENT);
			cbMirror.addActionListener(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					mirror = ((JCheckBox) e.getSource()).isSelected();
					processTexture();
				}
			});
			pnlTextureControls.add(cbMirror);

			slBrightness = new CPSlider(200) {
				public void onValueChange() {
					brightness = (value - 100) / 100f;
					title = "Brightness: " + (value - 100) + "%";
					processTexture();
				}
			};
			slBrightness.centerMode = true;
			slBrightness.setPreferredSize(new Dimension(100, 16));
			slBrightness.setValue(100);

			Box b = Box.createHorizontalBox();
			b.add(slBrightness);
			b.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			pnlTextureControls.add(b);

			slContrast = new CPSlider(200) {
				public void onValueChange() {
					contrast = (value - 100) / 100f;
					title = "Contrast: " + (value - 100) + "%";
					processTexture();
				}
			};
			slContrast.centerMode = true;
			slContrast.setPreferredSize(new Dimension(20, 16));
			slContrast.setValue(100);

			b = Box.createHorizontalBox();
			b.add(slContrast);
			b.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			pnlTextureControls.add(b);

			JButton resetButton = new JButton("Reset");
			resetButton.setPreferredSize(new Dimension(40, 16));
			resetButton.addActionListener(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					if (cbInverse.isSelected()) {
						cbInverse.doClick();
					}
					if (cbMirror.isSelected()) {
						cbMirror.doClick();
					}
					slBrightness.setValue(100);
					slContrast.setValue(100);
				}
			});
			pnlTextureControls.add(resetButton);

			// add(vb);
		}

		public void updateTexture() {
			btnCustomize.setEnabled(processedTexture!=null);
			repaint();
		}

		public Dimension getPreferredSize() {
			return new Dimension(120, 1);
		}
	}

	class CPTextureSample extends JComponent {
		private static final long serialVersionUID = 1L;

		public Dimension getPreferredSize() {
			return new Dimension(64, 64);
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D) g.create();

			g2d.draw(new Rectangle(0, 0, getWidth() - 1, getHeight() - 1));
			if (processedTexture != null) {
				g2d.drawImage(createTextureImage(processedTexture, getWidth() - 2, getHeight() - 2), 1, 1, null);
			} else {
				g2d.setColor(Color.white);
				g2d.fill(new Rectangle(1, 1, getWidth() - 2, getHeight() - 2));
			}

		}
	}
}
