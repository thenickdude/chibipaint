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

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import chibipaint.*;

public class CPPaletteManager implements ContainerListener {

	CPController controller;
	JDesktopPane jdp;

	Map<String, CPPalette> palettes = new HashMap();
	List<CPPaletteFrame> paletteFrames = new Vector();
	List<CPPaletteFrame> hiddenFrames = new Vector();

	interface ICPPaletteContainer {

		public void addPalette(CPPalette palette);

		public void removePalette(CPPalette palette);

		public void showPalette(CPPalette palette);

		public List<CPPalette> getPalettesList();
	}

	class CPPaletteFrame extends JInternalFrame implements ICPPaletteContainer {

		private List<CPPalette> list = new Vector();

		public CPPaletteFrame(CPPalette palette) {
			super("", true, true, false, false); // resizable/closable frame
			putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
			addPalette(palette);
			setVisible(true);
		}

		public void addPalette(CPPalette palette) {
			getContentPane().add(palette);
			setTitle(palette.title);
			palette.setContainer(this);

			list.add(palette);
		}

		public void removePalette(CPPalette palette) {
		}

		public void showPalette(CPPalette palette) {
		}

		public List<CPPalette> getPalettesList() {
			return new Vector(list);
		}
	}

	public CPPaletteManager(CPController controller, JDesktopPane desktop) {
		this.controller = controller;
		this.jdp = desktop;

		desktop.addContainerListener(this);

		// Color Palette

		CPPalette palette = new CPColorPalette(controller);
		palettes.put("color", palette);

		CPPaletteFrame frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(175, 175);
		frame.setLocation(0, 385);
		desktop.add(frame);

		// Brush Palette

		palette = new CPBrushPalette(controller);
		palettes.put("brush", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setLocation(638, 3);
		desktop.add(frame);

		// Layers Palette

		palette = new CPLayersPalette(controller);
		palettes.put("layers", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(170, 300);
		frame.setLocation(629, 253);
		desktop.add(frame);

		// Stroke Palette

		palette = new CPStrokePalette(controller);
		palettes.put("stroke", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setLocation(110, 64);
		desktop.add(frame);

		// Tool Palette

		palette = new CPToolPalette(controller);
		palettes.put("tool", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(90, 390);
		frame.setLocation(0, 0);
		desktop.add(frame);

		// Swatches Palette

		palette = new CPSwatchesPalette(controller);
		palettes.put("swatches", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(111, 125);
		frame.setLocation(512, 5);
		desktop.add(frame);

		// Misc Palette

		palette = new CPMiscPalette(controller);
		palettes.put("misc", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		// frame.setSize(111, 125);
		frame.setLocation(110, 0);
		desktop.add(frame);

		// Misc Palette

		palette = new CPTexturePalette(controller);
		palettes.put("textures", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(400, 220);
		frame.setLocation(190, 340);
		desktop.add(frame);
	}

	public void showPalette(String paletteName, boolean show) {
		CPPalette palette = palettes.get(paletteName);
		if (palette == null) {
			return;
		}

		showPalette(palette, show);
	}

	public void showPalette(CPPalette palette, boolean show) {
		// FIXME: this will need to be replaced by something more generic
		CPPaletteFrame frame = (CPPaletteFrame) palette.getContainer();
		if (frame == null) {
			return;
		}

		if (show) {
			jdp.add(frame, 0);
			frame.setVisible(true);
		} else {
			frame.setVisible(false);
			jdp.remove(frame);
		}
		controller.getMainGUI().setPaletteMenuItem(palette.title, show);

		// FIXME: focus hack
		controller.canvas.grabFocus();
	}

	public void componentAdded(ContainerEvent e) {
	}

	public void componentRemoved(ContainerEvent e) {
		if (e.getChild() instanceof CPPaletteFrame) {
			CPPaletteFrame frame = (CPPaletteFrame) e.getChild();
			for (CPPalette palette : frame.getPalettesList()) {
				controller.getMainGUI().setPaletteMenuItem(palette.title, false);
			}
		}
	}

	public void togglePalettes() {
		if (hiddenFrames.isEmpty()) {
			for (CPPaletteFrame frame : paletteFrames) {
				if (frame.isVisible()) {
					for (CPPalette pal : frame.getPalettesList()) {
						showPalette(pal, false);
					}
					hiddenFrames.add(frame);
				}
			}
		} else {
			for (CPPaletteFrame frame : hiddenFrames) {
				if (!frame.isVisible()) {
					for (CPPalette pal : frame.getPalettesList()) {
						showPalette(pal, true);
					}
				}
			}
			hiddenFrames.clear();
		}
	}

}
