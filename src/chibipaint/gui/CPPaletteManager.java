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

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import chibipaint.CPController;

public class CPPaletteManager implements ContainerListener {

	CPController controller;
	JDesktopPane jdp;

	Map<String, CPPalette> palettes = new HashMap<String, CPPalette>();
	List<CPPaletteFrame> paletteFrames = new Vector<CPPaletteFrame>();
	List<CPPaletteFrame> hiddenFrames = new Vector<CPPaletteFrame>();

	private CPPalette palTextures, palMisc, palTool, palStroke, palLayers, palBrush, palColor;

	public CPSwatchesPalette palSwatches;
	
	interface ICPPaletteContainer {
		public void setLocation(int x, int y);

		public void setSize(int w, int h);

		public int getX();

		public int getY();

		public void addPalette(CPPalette palette);

		public void removePalette(CPPalette palette);

		public void showPalette(CPPalette palette);

		public List<CPPalette> getPalettesList();

		public int getHeight();

		public int getWidth();
	}

	class CPPaletteFrame extends JInternalFrame implements ICPPaletteContainer {

		private static final long serialVersionUID = 1L;

		private List<CPPalette> list = new Vector<CPPalette>();
		
		public CPPaletteFrame(CPPalette palette) {
			super("", palette.getResizable(), true, false, false); // resizable/closable frame
			putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
			addPalette(palette);
			setVisible(true);
			setMinimumSize(null);
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
			return new Vector<CPPalette>(list);
		}
	}

	public CPPaletteManager(CPController controller, JDesktopPane desktop) {
		this.controller = controller;
		this.jdp = desktop;

		desktop.addContainerListener(this);

		// Color Palette

		palColor = new CPColorPalette(controller);
		{
			palettes.put("color", palColor);

			CPPaletteFrame frame = new CPPaletteFrame(palColor);
			paletteFrames.add(frame);
			
			frame.pack();

			desktop.add(frame);
		}

		// Brush Palette

		palBrush = new CPBrushPalette(controller);
		{
			palettes.put("brush", palBrush);

			CPPaletteFrame frame = new CPPaletteFrame(palBrush);
			paletteFrames.add(frame);

			frame.pack();
			desktop.add(frame);
		}

		// Layers Palette

		palLayers = new CPLayersPalette(controller);
		{
			palettes.put("layers", palLayers);

			CPPaletteFrame frame = new CPPaletteFrame(palLayers);
			paletteFrames.add(frame);

			frame.pack();
			frame.setSize(170, 300);
			desktop.add(frame);
		}

		// Stroke Palette

		palStroke = new CPStrokePalette(controller);
		{
			palettes.put("stroke", palStroke);

			CPPaletteFrame frame = new CPPaletteFrame(palStroke);
			paletteFrames.add(frame);

			frame.pack();
			desktop.add(frame);
		}

		// Tool Palette
		palTool = new CPToolPalette(controller);
		{
			palettes.put("tool", palTool);

			CPPaletteFrame frame = new CPPaletteFrame(palTool);
			
			frame.pack();
			
			paletteFrames.add(frame);
			desktop.add(frame);
		}

		// Swatches Palette

		palSwatches = new CPSwatchesPalette(controller);
		{
			palettes.put("swatches", palSwatches);

			CPPaletteFrame frame = new CPPaletteFrame(palSwatches);
			paletteFrames.add(frame);

			frame.pack();
			desktop.add(frame);
		}

		// Misc Palette

		palMisc = new CPMiscPalette(controller);
		{
			palettes.put("misc", palMisc);

			CPPaletteFrame frame = new CPPaletteFrame(palMisc);
			paletteFrames.add(frame);

			frame.pack();
			// frame.setSize(111, 125);
			desktop.add(frame);
		}

		// Textures Palette

		palTextures = new CPTexturePalette(controller);
		{
			palettes.put("textures", palTextures);

			CPPaletteFrame frame = new CPPaletteFrame(palTextures);
			paletteFrames.add(frame);
			
			frame.pack();

			desktop.add(frame);
		}
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

	/**
	 * Pop palettes that are currently outside the visible area back into view.
	 */
	public void constrainPalettes() {
		int windowWidth = jdp.getWidth();
		int windowHeight = jdp.getHeight();

		for (CPPalette palette : palettes.values()) {
			ICPPaletteContainer container = palette.getContainer();
			
			/* Move palettes that are more than half out of the frame back into it */
			if (container.getX() + container.getWidth() / 2 > windowWidth) {
				container.setLocation(windowWidth - container.getWidth(), container.getY());
			}

			if (container.getY() + container.getHeight() / 2 > windowHeight) {
				container.setLocation(container.getX(), windowHeight - container.getHeight());
			}
		}
		
		//Move small palettes to the front so that they aren't completely hidden
		((JInternalFrame) palSwatches.container).moveToFront();
		
		//Special handling for the swatches palette being under the brush palette:
		boolean widthToSpare = windowWidth - palTool.getSize().width - palMisc.getWidth() - palStroke.getWidth() - palColor.getWidth() - palBrush.getWidth() - 15 > 0;

		if (palSwatches.getContainer().getX() + palSwatches.getContainer().getWidth() == 
				palBrush.getContainer().getX() + palBrush.getContainer().getWidth() &&
				Math.abs(palSwatches.getContainer().getY() - palBrush.getContainer().getY()) < 20) {
			palSwatches.getContainer().setLocation(palBrush.getContainer().getX() - palSwatches.getContainer().getWidth() - (widthToSpare ? 5 : 1), 0);
		}
		
		//Special handling for layers palette being too damn tall:
		if (palLayers.getContainer().getY() + palLayers.getContainer().getHeight() > windowHeight)
			palLayers.getContainer().setSize(palLayers.getContainer().getWidth(), Math.max(windowHeight - palLayers.getContainer().getY(), 200));
	}
	
	/**
	 * Rearrange the palettes from scratch into a useful arrangement.
	 */
	public void arrangePalettes() {
		int windowWidth = jdp.getWidth();
		int windowHeight = jdp.getHeight();
		
		boolean widthToSpare = windowWidth - palTool.getSize().width - palMisc.getWidth() - palStroke.getWidth() - palColor.getWidth() - palBrush.getWidth() - 15 > 0;

		palBrush.getContainer().setLocation(windowWidth - palBrush.getContainer().getWidth() - 15, 0);

		int bottomOfBrush = palBrush.getY() + palBrush.getContainer().getHeight();

		palLayers.getContainer().setLocation(palBrush.getContainer().getX(),
				windowHeight - bottomOfBrush > 300 ? bottomOfBrush + 5 : bottomOfBrush);

		palLayers.getContainer().setSize(palBrush.getContainer().getWidth(), windowHeight - palLayers.getContainer().getY());

		palTool.getContainer().setLocation(0, 0);
		
		palMisc.getContainer().setLocation(palTool.getX() + palTool.getContainer().getWidth() + (widthToSpare ? 5 : 1), 0);
		if (widthToSpare) {
			palStroke.getContainer().setLocation(palMisc.getContainer().getX() + palMisc.getContainer().getWidth() + (widthToSpare ? 5 : 1), 0);
		} else {
			palStroke.getContainer().setLocation(palMisc.getContainer().getX(), palMisc.getContainer().getY() + palMisc.getContainer().getHeight() + 1);
		}
		palSwatches.getContainer().setLocation(palBrush.getContainer().getX() - palSwatches.getContainer().getWidth() - (widthToSpare ? 5 : 1), 0);

		palTextures.getContainer().setSize(Math.min(palLayers.getContainer().getX() - palTextures.getContainer().getX(), 480), palTextures.getContainer().getHeight());
		palTextures.getContainer().setLocation(palColor.getX() + palColor.getContainer().getWidth() + 4,
				windowHeight - palTextures.getContainer().getHeight());

		palColor.getContainer()
				.setLocation(
						0,
						Math.max(palTool.getContainer().getY() + palTool.getContainer().getHeight(),
								windowHeight - palColor.getContainer().getHeight()));
	}

}
