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

package chibipaint;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.PixelGrabber;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bootstrap.IChibiApplet;
import chibipaint.engine.AdobeColorTable;
import chibipaint.engine.CPArtwork;
import chibipaint.engine.CPChibiFile;
import chibipaint.gui.CPMainGUI;

public class ChibiApplet implements IChibiApplet {

	CPControllerApplet controller;
	CPMainGUI mainGUI;

	boolean floatingMode = false;
	JPanel floatingPlaceholder;
	JFrame floatingFrame;

	private JApplet applet;

	public ChibiApplet(JApplet applet, InputStream layers, InputStream flat, InputStream swatchStream) {
		this.applet = applet;

		controller = new CPControllerApplet(this, applet);
		controller.setArtwork(createArtwork(layers, flat));

		// FIXME: set a default tool so that we can start drawing
		controller.setTool(CPController.T_PEN);

		createFloatingPlaceholder();

		mainGUI = new CPMainGUI(controller);

		int[] swatches = AdobeColorTable.read(swatchStream);
		if (swatches != null) {
			mainGUI.getSwatchesPalette().setSwatches(swatches);
		}

		applet.setContentPane(mainGUI.getGUI());
		applet.setJMenuBar(mainGUI.getMenuBar());

		applet.validate(); // calling validate is recommended to ensure compatibility

		mainGUI.arrangePalettes();
	}

	public void destroy() {

		// The following bit of voodoo prevents the Java plugin
		// from leaking too much. In many cases it will keep
		// a reference to this JApplet object alive forever.
		// So have to make sure that we remove references
		// to the rest of ChibiPaint so that they can be
		// garbage collected normally.

		applet.setContentPane(new JPanel());
		applet.setJMenuBar(null);

		floatingPlaceholder = null;
		floatingFrame = null;
		controller = null;
		mainGUI = null;
	}

	/**
	 * Create an image from one of the provided sources (any of which may be
	 * null)
	 * 
	 * @param layers
	 * @param flat
	 * @return
	 */
	private CPArtwork createArtwork(InputStream layers, InputStream flat) {
		CPArtwork artwork = null;
		int w = -1, h = -1;
		Image loadImage = null;

		if (layers != null) {
			try {
				artwork = CPChibiFile.read(layers);
				w = artwork.width;
				h = artwork.height;
			} catch (Exception ignored) {
			}
		}

		if ((w < 1 || h < 1) && flat != null) {
			try {
				loadImage = ImageIO.read(flat);
				w = loadImage.getWidth(null);
				h = loadImage.getHeight(null);
			} catch (Exception ignored) {
			}
		}

		if (w < 1 || h < 1) {
			loadImage = null;
			if (applet.getParameter("canvasWidth") != null && applet.getParameter("canvasHeight") != null) {
				w = Integer.parseInt(applet.getParameter("canvasWidth"));
				h = Integer.parseInt(applet.getParameter("canvasHeight"));
			} else {
				w = 320;
				h = 240;
			}
		}
		w = Math.max(1, Math.min(1024, w));
		h = Math.max(1, Math.min(1024, h));

		if (artwork == null) {
			artwork = new CPArtwork(w, h);
		}

		if (loadImage != null) {
			PixelGrabber grabber = new PixelGrabber(loadImage, 0, 0, w, h, artwork.getActiveLayer().data, 0, w);
			try {
				grabber.grabPixels();
			} catch (InterruptedException e) {
			}
		}

		return artwork;
	}

	void createFloatingPlaceholder() {
		/*
		 * Build the panel that will be displayed in the applet when user
		 * switches to floating mode
		 */
		floatingPlaceholder = new JPanel(new BorderLayout());
		JLabel label = new JLabel("ChibiPaint is running in floating mode.\n\nDO NOT CLOSE THIS WINDOW!", JLabel.CENTER);
		label.setFont(new Font("Serif", Font.PLAIN, 16));
		floatingPlaceholder.add(label);
	}

	void floatingMode() {
		if (!floatingMode) {
			// Going to floating mode

			JFrame.setDefaultLookAndFeelDecorated(false);
			floatingFrame = new CPFloatingFrame();
			floatingFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			mainGUI.getGUI().setPreferredSize(new Dimension(Math.max(mainGUI.getGUI().getWidth(), 800), Math.max(mainGUI.getGUI().getHeight(), 600)));

			applet.setContentPane(floatingPlaceholder);
			applet.setJMenuBar(null);
			floatingPlaceholder.revalidate();
			floatingMode = true;

			floatingFrame.setContentPane(mainGUI.getGUI());
			floatingFrame.setJMenuBar(mainGUI.getMenuBar());
			floatingFrame.setVisible(true);
			floatingFrame.pack();

			controller.setFloatingFrame(floatingFrame);
		} else {
			// Going back to normal mode

			// close the frame
			floatingFrame.setVisible(false);
			floatingFrame = null;
			controller.setFloatingFrame(null);
			floatingMode = false;

			// restore the applet
			applet.setContentPane(mainGUI.getGUI());
			applet.setJMenuBar(mainGUI.getMenuBar());
			applet.validate();
		}
	}

	public class CPFloatingFrame extends JFrame {

		private static final long serialVersionUID = 1L;

		public CPFloatingFrame() {
			super("ChibiPaint");
			addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent e) {
					floatingMode();
				}
			});
		}
	}

	public boolean hasUnsavedChanges() {
		return controller.hasUnsavedChanges();
	}
}
