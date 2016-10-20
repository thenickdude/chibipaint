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

import chibipaint.*;

public class CPMainGUI {

	CPController controller;
	CPPaletteManager paletteManager;

	JMenuBar menuBar;
	JPanel mainPanel;
	JDesktopPane jdp;
	JPanel bg;

	// FIXME: replace this hack by something better
	Map<String, JCheckBoxMenuItem> paletteItems = new HashMap();

	public CPMainGUI(CPController controller) {
		this.controller = controller;
		controller.setMainGUI(this);

		// try {
		// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		// } catch (Exception ex2) {} */

		menuBar = createMainMenu(controller);
		createGUI();
	}

	public JComponent getGUI() {
		return mainPanel;
	}

	public JMenuBar getMenuBar() {
		return menuBar;
	}

	private void createGUI() {
		mainPanel = new JPanel(new BorderLayout());

		jdp = new CPDesktop();
		paletteManager = new CPPaletteManager(controller, jdp);

		createCanvasGUI(jdp);
		mainPanel.add(jdp, BorderLayout.CENTER);

		JPanel statusBar = new CPStatusBar(controller);
		mainPanel.add(statusBar, BorderLayout.PAGE_END);

		// jdp.addContainerListener(this);
	}

	void createCanvasGUI(JComponent c) {
		CPCanvas canvas = new CPCanvas(controller);
		bg = canvas.getContainer();

		c.add(bg);
		canvas.grabFocus();
	}

	private JMenuBar createMainMenu(ActionListener listener) {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu, submenu;
		JMenuItem menuItem;

		//
		// File Menu
		//

		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menu);

		if (controller.isRunningAsApplet()) {
			menuItem = new JMenuItem("Send Oekaki", KeyEvent.VK_S);
			menuItem.getAccessibleContext().setAccessibleDescription(
					"Sends the oekaki to the server and exits ChibiPaint");
			menuItem.setActionCommand("CPSend");
			menuItem.addActionListener(listener);
			menu.add(menuItem);
		}

		//
		// Edit Menu
		//

		menu = new JMenu("Edit");
		menu.setMnemonic(KeyEvent.VK_E);
		menuBar.add(menu);

		menuItem = new JMenuItem("Undo", KeyEvent.VK_U);
		menuItem.getAccessibleContext().setAccessibleDescription("Undoes the most recent action");
		menuItem.setActionCommand("CPUndo");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
		menu.add(menuItem);

		menuItem = new JMenuItem("Redo", KeyEvent.VK_R);
		menuItem.getAccessibleContext().setAccessibleDescription("Redoes a previously undone action");
		menuItem.setActionCommand("CPRedo");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		menu.add(menuItem);

		menuItem = new JMenuItem("Clear History", KeyEvent.VK_H);
		menuItem.getAccessibleContext().setAccessibleDescription("Removes all undo/redo information to regain memory");
		menuItem.setActionCommand("CPClearHistory");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menu.add(new JSeparator());

		menuItem = new JMenuItem("Cut", KeyEvent.VK_T);
		menuItem.getAccessibleContext().setAccessibleDescription("");
		menuItem.setActionCommand("CPCut");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		menu.add(menuItem);

		menuItem = new JMenuItem("Copy", KeyEvent.VK_C);
		menuItem.getAccessibleContext().setAccessibleDescription("");
		menuItem.setActionCommand("CPCopy");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		menu.add(menuItem);

		menuItem = new JMenuItem("Copy Merged", KeyEvent.VK_Y);
		menuItem.getAccessibleContext().setAccessibleDescription("");
		menuItem.setActionCommand("CPCopyMerged");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		menu.add(menuItem);

		menuItem = new JMenuItem("Paste", KeyEvent.VK_P);
		menuItem.getAccessibleContext().setAccessibleDescription("");
		menuItem.setActionCommand("CPPaste");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		menu.add(menuItem);

		menu.add(new JSeparator());

		menuItem = new JMenuItem("Select All", KeyEvent.VK_A);
		menuItem.getAccessibleContext().setAccessibleDescription("Selects the whole canvas");
		menuItem.setActionCommand("CPSelectAll");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
		menu.add(menuItem);

		menuItem = new JMenuItem("Deselect", KeyEvent.VK_D);
		menuItem.getAccessibleContext().setAccessibleDescription("Deselects the whole canvas");
		menuItem.setActionCommand("CPDeselectAll");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
		menu.add(menuItem);

		menu = new JMenu("Layers");
		menu.setMnemonic(KeyEvent.VK_L);
		menuBar.add(menu);

		menuItem = new JMenuItem("Duplicate", KeyEvent.VK_D);
		menuItem.getAccessibleContext().setAccessibleDescription("Creates a copy of the currently selected layer");
		menuItem.setActionCommand("CPLayerDuplicate");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		menu.add(menuItem);

		menu.add(new JSeparator());

		menuItem = new JMenuItem("Merge Down", KeyEvent.VK_E);
		menuItem.getAccessibleContext().setAccessibleDescription(
				"Merges the currently selected layer with the one directly below it");
		menuItem.setActionCommand("CPLayerMergeDown");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		menu.add(menuItem);

		/*
		 * menuItem = new JMenuItem("Merge Visible", KeyEvent.VK_V);
		 * menuItem.getAccessibleContext().setAccessibleDescription("Merges all the visible layers");
		 * menuItem.setActionCommand("CPLayerMergeVisible"); menuItem.addActionListener(listener);
		 * menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK |
		 * ActionEvent.SHIFT_MASK)); menu.add(menuItem);
		 */

		menuItem = new JMenuItem("Merge All Layers", KeyEvent.VK_A);
		menuItem.getAccessibleContext().setAccessibleDescription("Merges all the layers");
		menuItem.setActionCommand("CPLayerMergeAll");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		//
		// Effects Menu
		//
		menu = new JMenu("Effects");
		menu.setMnemonic(KeyEvent.VK_E);
		menuBar.add(menu);

		menuItem = new JMenuItem("Clear", KeyEvent.VK_C);
		menuItem.getAccessibleContext().setAccessibleDescription("Clears the selected area");
		menuItem.setActionCommand("CPClear");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		menu.add(menuItem);

		menuItem = new JMenuItem("Fill", KeyEvent.VK_F);
		menuItem.getAccessibleContext().setAccessibleDescription("Fills the selected area with the current color");
		menuItem.setActionCommand("CPFill");
		menuItem.addActionListener(listener);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		menu.add(menuItem);

		menuItem = new JMenuItem("Flip Horizontal", KeyEvent.VK_H);
		menuItem.getAccessibleContext().setAccessibleDescription("Flips the current selected area horizontally");
		menuItem.setActionCommand("CPHFlip");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menuItem = new JMenuItem("Flip Vertical", KeyEvent.VK_V);
		menuItem.getAccessibleContext().setAccessibleDescription("Flips the current selected area vertically");
		menuItem.setActionCommand("CPVFlip");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menuItem = new JMenuItem("Invert", KeyEvent.VK_I);
		menuItem.getAccessibleContext().setAccessibleDescription("Invert the image colors");
		menuItem.setActionCommand("CPFXInvert");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		submenu = new JMenu("Blur");
		submenu.setMnemonic(KeyEvent.VK_B);

		menuItem = new JMenuItem("Box Blur...", KeyEvent.VK_B);
		menuItem.getAccessibleContext().setAccessibleDescription("Blur effect");
		menuItem.setActionCommand("CPFXBoxBlur");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);

		menu.add(submenu);

		submenu = new JMenu("Noise");
		submenu.setMnemonic(KeyEvent.VK_N);

		menuItem = new JMenuItem("Render Monochromatic", KeyEvent.VK_M);
		menuItem.getAccessibleContext().setAccessibleDescription("Fills the selection with noise");
		menuItem.setActionCommand("CPMNoise");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);

		menuItem = new JMenuItem("Render Color", KeyEvent.VK_C);
		menuItem.getAccessibleContext().setAccessibleDescription("Fills the selection with colored noise");
		menuItem.setActionCommand("CPCNoise");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);

		menu.add(submenu);

		//
		// View Menu
		//

		menu = new JMenu("View");
		menu.setMnemonic(KeyEvent.VK_V);
		menuBar.add(menu);

		if (controller.isRunningAsApplet()) {
			menuItem = new JMenuItem("Floating mode", KeyEvent.VK_F);
			menuItem.getAccessibleContext().setAccessibleDescription("Opens ChibiPaint in an independent window");
			menuItem.setActionCommand("CPFloat");
			menuItem.addActionListener(listener);
			menu.add(menuItem);
			menu.add(new JSeparator());
		}

		menuItem = new JMenuItem("Zoom In", KeyEvent.VK_I);
		menuItem.getAccessibleContext().setAccessibleDescription("Zooms In");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("CPZoomIn");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menuItem = new JMenuItem("Zoom Out", KeyEvent.VK_O);
		menuItem.getAccessibleContext().setAccessibleDescription("Zooms Out");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("CPZoomOut");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menuItem = new JMenuItem("Zoom 100%", KeyEvent.VK_1);
		menuItem.getAccessibleContext().setAccessibleDescription("Resets the zoom factor to 100%");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("CPZoom100");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menu.add(new JSeparator());

		menuItem = new JCheckBoxMenuItem("Use Linear Interpolation", false);
		menuItem.setMnemonic(KeyEvent.VK_L);
		menuItem.getAccessibleContext().setAccessibleDescription(
				"Linear interpolation is used to give a smoothed looked to the picture when zoomed in");
		menuItem.setActionCommand("CPLinearInterpolation");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menu.add(new JSeparator());

		menuItem = new JCheckBoxMenuItem("Show Grid", false);
		menuItem.setMnemonic(KeyEvent.VK_G);
		menuItem.getAccessibleContext().setAccessibleDescription("Displays a grid over the image");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("CPToggleGrid");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menuItem = new JMenuItem("Grid options...", KeyEvent.VK_D);
		menuItem.getAccessibleContext().setAccessibleDescription("Shows the grid options dialog box");
		menuItem.setActionCommand("CPGridOptions");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		menu.add(new JSeparator());

		submenu = new JMenu("Palettes");
		submenu.setMnemonic(KeyEvent.VK_P);

		menuItem = new JMenuItem("Toggle Palettes", KeyEvent.VK_P);
		menuItem.getAccessibleContext().setAccessibleDescription("Hides or shows all palettes");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		menuItem.setActionCommand("CPTogglePalettes");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);

		submenu.add(new JSeparator());

		menuItem = new JCheckBoxMenuItem("Show Brush", true);
		menuItem.setMnemonic(KeyEvent.VK_B);
		menuItem.setActionCommand("CPPalBrush");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);
		paletteItems.put("Brush", (JCheckBoxMenuItem) menuItem);

		menuItem = new JCheckBoxMenuItem("Show Color", true);
		menuItem.setMnemonic(KeyEvent.VK_C);
		menuItem.setActionCommand("CPPalColor");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);
		paletteItems.put("Color", (JCheckBoxMenuItem) menuItem);

		menuItem = new JCheckBoxMenuItem("Show Layers", true);
		menuItem.setMnemonic(KeyEvent.VK_Y);
		menuItem.setActionCommand("CPPalLayers");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);
		paletteItems.put("Layers", (JCheckBoxMenuItem) menuItem);

		menuItem = new JCheckBoxMenuItem("Show Misc", true);
		menuItem.setMnemonic(KeyEvent.VK_M);
		menuItem.setActionCommand("CPPalMisc");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);
		paletteItems.put("Misc", (JCheckBoxMenuItem) menuItem);

		menuItem = new JCheckBoxMenuItem("Show Stroke", true);
		menuItem.setMnemonic(KeyEvent.VK_S);
		menuItem.setActionCommand("CPPalStroke");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);
		paletteItems.put("Stroke", (JCheckBoxMenuItem) menuItem);

		menuItem = new JCheckBoxMenuItem("Show Swatches", true);
		menuItem.setMnemonic(KeyEvent.VK_W);
		menuItem.setActionCommand("CPPalSwatches");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);
		paletteItems.put("Color Swatches", (JCheckBoxMenuItem) menuItem);

		menuItem = new JCheckBoxMenuItem("Show Textures", true);
		menuItem.setMnemonic(KeyEvent.VK_X);
		menuItem.setActionCommand("CPPalTextures");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);
		paletteItems.put("Textures", (JCheckBoxMenuItem) menuItem);

		menuItem = new JCheckBoxMenuItem("Show Tools", true);
		menuItem.setMnemonic(KeyEvent.VK_T);
		menuItem.setActionCommand("CPPalTool");
		menuItem.addActionListener(listener);
		submenu.add(menuItem);
		paletteItems.put("Tools", (JCheckBoxMenuItem) menuItem);

		menu.add(submenu);

		//
		// Help Menu
		//

		menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);
		menuBar.add(menu);

		menuItem = new JMenuItem("About...", KeyEvent.VK_A);
		menuItem.getAccessibleContext().setAccessibleDescription("Displays some information about ChibiPaint");
		menuItem.setActionCommand("CPAbout");
		menuItem.addActionListener(listener);
		menu.add(menuItem);

		return menuBar;
	}

	public void showPalette(String palette, boolean show) {
		paletteManager.showPalette(palette, show);
	}

	public void setPaletteMenuItem(String title, boolean selected) {
		JCheckBoxMenuItem item = paletteItems.get(title);
		if (item != null) {
			item.setSelected(selected);
		}
	}

	public void togglePalettes() {
		paletteManager.togglePalettes();
	}

	class CPDesktop extends JDesktopPane {

		public CPDesktop() {
			addComponentListener(new ComponentAdapter() {

				public void componentResized(ComponentEvent e) {
					bg.setSize(getSize());
					bg.validate();
				}
			});
		}
	}
}
