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

package chibiapp;

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.swing.JFrame;

import chibipaint.CPController;
import chibipaint.engine.CPArtwork;
import chibipaint.engine.CPChibiFile;
import chibipaint.gui.CPMainGUI;

public class ChibiApp extends JFrame {

	CPControllerApplication controller;
	CPMainGUI mainGUI;

	public ChibiApp(File loadImage) {
		super("ChibiPaint");

		controller = new CPControllerApplication(this);

		if (loadImage != null) {
			try {
				controller.setArtwork(CPChibiFile.read(new BufferedInputStream(new FileInputStream(loadImage))));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
		
		if (controller.artwork == null) {
			controller.setArtwork(new CPArtwork(600, 400));
		}

		// FIXME: set a default tool so that we can start drawing
		controller.setTool(CPController.T_PEN);

		mainGUI = new CPMainGUI(controller);

		setContentPane(mainGUI.getGUI());
		setJMenuBar(mainGUI.getMenuBar());

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 600);

		pack();
		setVisible(true);

		mainGUI.arrangePalettes();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(900, 600);
	}

	public static void main(final String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				new ChibiApp(args.length >= 1 ? new File(args[0]) : null);
			}
		});
	}
}
