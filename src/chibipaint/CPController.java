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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import chibipaint.engine.*;
import chibipaint.gui.*;
import chibipaint.util.*;

public abstract class CPController implements ActionListener {

	final static String VERSION_STRING = "0.7.11.2";

	private CPColor curColor = new CPColor();
	// int curAlpha = 255;
	// int brushSize = 16;

	// some important object references
	public CPArtwork artwork;
	public CPCanvas canvas;

	CPBrushInfo[] tools;
	int curBrush = T_PENCIL;
	int curMode = M_DRAW;

	private LinkedList<ICPColorListener> colorListeners = new LinkedList();
	private LinkedList<ICPToolListener> toolListeners = new LinkedList();
	private LinkedList<ICPModeListener> modeListeners = new LinkedList();
	private LinkedList<ICPViewListener> viewListeners = new LinkedList();
	private LinkedList<ICPEventListener> cpEventListeners = new LinkedList();

	//
	// Definition of all the standard tools available
	//

	public static final int T_PENCIL = 0;
	public static final int T_ERASER = 1;
	public static final int T_PEN = 2;
	public static final int T_SOFTERASER = 3;
	public static final int T_AIRBRUSH = 4;
	public static final int T_DODGE = 5;
	public static final int T_BURN = 6;
	public static final int T_WATER = 7;
	public static final int T_BLUR = 8;
	public static final int T_SMUDGE = 9;
	public static final int T_BLENDER = 10;
	public static final int T_MAX = 11;

	//
	// Definition of all the modes available
	//

	public static final int M_DRAW = 0;
	public static final int M_FLOODFILL = 1;
	public static final int M_RECT_SELECTION = 2;
	public static final int M_MOVE_TOOL = 3;
	public static final int M_ROTATE_CANVAS = 4;
	public static final int M_MAX = 5;

	// Image loader cache
	private Map<String, Image> imageCache = new HashMap<String, Image>();

	private CPMainGUI mainGUI;

	public interface ICPColorListener {

		public void newColor(CPColor color);
	}

	public interface ICPToolListener {

		public void newTool(int tool, CPBrushInfo toolInfo);
	}

	public interface ICPModeListener {

		public void modeChange(int mode);
	}

	public interface ICPViewListener {

		public void viewChange(CPViewInfo viewInfo);
	}

	public interface ICPEventListener {

		public void cpEvent();
	}

	public static class CPViewInfo {

		public float zoom;
		public int offsetX, offsetY;
	}

	public CPController() {
		tools = new CPBrushInfo[T_MAX];
		tools[T_PENCIL] = new CPBrushInfo(T_PENCIL, 16, 255, true, false, .5f, .05f, false, true,
				CPBrushInfo.B_ROUND_AA, CPBrushInfo.M_PAINT, 1f, 0f);
		tools[T_ERASER] = new CPBrushInfo(T_ERASER, 16, 255, true, false, .5f, .05f, false, false,
				CPBrushInfo.B_ROUND_AA, CPBrushInfo.M_ERASE, 1f, 0f);
		tools[T_PEN] = new CPBrushInfo(T_PEN, 2, 128, true, false, .5f, .05f, true, false, CPBrushInfo.B_ROUND_AA,
				CPBrushInfo.M_PAINT, 1f, 0f);
		tools[T_SOFTERASER] = new CPBrushInfo(T_SOFTERASER, 16, 64, false, true, .5f, .05f, false, true,
				CPBrushInfo.B_ROUND_AIRBRUSH, CPBrushInfo.M_ERASE, 1f, 0f);
		tools[T_AIRBRUSH] = new CPBrushInfo(T_AIRBRUSH, 50, 32, false, true, .5f, .05f, false, true,
				CPBrushInfo.B_ROUND_AIRBRUSH, CPBrushInfo.M_PAINT, 1f, 0f);
		tools[T_DODGE] = new CPBrushInfo(T_DODGE, 30, 32, false, true, .5f, .05f, false, true,
				CPBrushInfo.B_ROUND_AIRBRUSH, CPBrushInfo.M_DODGE, 1f, 0f);
		tools[T_BURN] = new CPBrushInfo(T_BURN, 30, 32, false, true, .5f, .05f, false, true,
				CPBrushInfo.B_ROUND_AIRBRUSH, CPBrushInfo.M_BURN, 1f, 0f);
		tools[T_WATER] = new CPBrushInfo(T_WATER, 30, 70, false, true, .5f, .02f, false, true, CPBrushInfo.B_ROUND_AA,
				CPBrushInfo.M_WATER, .3f, .6f);
		tools[T_BLUR] = new CPBrushInfo(T_BLUR, 20, 255, false, true, .5f, .05f, false, true,
				CPBrushInfo.B_ROUND_PIXEL, CPBrushInfo.M_BLUR, 1f, 0f);
		tools[T_SMUDGE] = new CPBrushInfo(T_SMUDGE, 20, 128, false, true, .5f, .01f, false, true,
				CPBrushInfo.B_ROUND_AIRBRUSH, CPBrushInfo.M_SMUDGE, 0f, 1f);
		tools[T_BLENDER] = new CPBrushInfo(T_SMUDGE, 20, 60, false, true, .5f, .1f, false, true,
				CPBrushInfo.B_ROUND_AIRBRUSH, CPBrushInfo.M_OIL, 0f, .07f);
	}

	public void setArtwork(CPArtwork artwork) {
		this.artwork = artwork;
	}

	public void setCanvas(CPCanvas canvas) {
		this.canvas = canvas;
	}

	public void setCurColor(CPColor color) {
		if (!curColor.isEqual(color)) {
			artwork.setForegroundColor(color.getRgb());

			curColor.copyFrom(color);
			for (Object l : colorListeners) {
				((ICPColorListener) l).newColor(color);
			}
		}
	}

	public CPColor getCurColor() {
		return (CPColor) curColor.clone();
	}

	public int getCurColorRgb() {
		return curColor.getRgb();
	}

	public void setCurColorRgb(int color) {
		CPColor col = new CPColor(color);
		setCurColor(col);
	}

	public void setBrushSize(int size) {
		tools[curBrush].size = Math.max(1, Math.min(200, size));
		callToolListeners();
	}

	public int getBrushSize() {
		return tools[curBrush].size;
	}

	public void setAlpha(int alpha) {
		tools[curBrush].alpha = alpha;
		callToolListeners();
	}

	public int getAlpha() {
		return tools[curBrush].alpha;
	}

	public void setTool(int tool) {
		setMode(M_DRAW);
		curBrush = tool;
		artwork.setBrush(tools[tool]);
		callToolListeners();
	}

	public CPBrushInfo getBrushInfo() {
		return tools[curBrush];
	}

	public void setMode(int mode) {
		curMode = mode;
		callModeListeners();
	}

	/*
	 * public CPToolInfo getModeInfo() { return modes[curMode]; }
	 */

	public void actionPerformed(ActionEvent e) {
		if (artwork == null || canvas == null) {
			return; // this shouldn't happen but just in case
		}

		if (e.getActionCommand().equals("CPZoomIn")) {
			canvas.zoomIn();
		}
		if (e.getActionCommand().equals("CPZoomOut")) {
			canvas.zoomOut();
		}
		if (e.getActionCommand().equals("CPZoom100")) {
			canvas.zoom100();
		}

		if (e.getActionCommand().equals("CPUndo")) {
			artwork.undo();
		}
		if (e.getActionCommand().equals("CPRedo")) {
			artwork.redo();
		}
		if (e.getActionCommand().equals("CPClearHistory")) {
			int choice = JOptionPane
					.showConfirmDialog(
							getDialogParent(),
							"You're about to clear the current Undo/Redo history.\nThis operation cannot be undone, are you sure you want to do that?",
							"Clear History", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (choice == JOptionPane.OK_OPTION) {
				artwork.clearHistory();
			}
		}

		if (e.getActionCommand().equals("CPPencil")) {
			setTool(T_PENCIL);
		}
		if (e.getActionCommand().equals("CPPen")) {
			setTool(T_PEN);
		}
		if (e.getActionCommand().equals("CPEraser")) {
			setTool(T_ERASER);
		}
		if (e.getActionCommand().equals("CPSoftEraser")) {
			setTool(T_SOFTERASER);
		}
		if (e.getActionCommand().equals("CPAirbrush")) {
			setTool(T_AIRBRUSH);
		}
		if (e.getActionCommand().equals("CPDodge")) {
			setTool(T_DODGE);
		}
		if (e.getActionCommand().equals("CPBurn")) {
			setTool(T_BURN);
		}
		if (e.getActionCommand().equals("CPWater")) {
			setTool(T_WATER);
		}
		if (e.getActionCommand().equals("CPBlur")) {
			setTool(T_BLUR);
		}
		if (e.getActionCommand().equals("CPSmudge")) {
			setTool(T_SMUDGE);
		}
		if (e.getActionCommand().equals("CPBlender")) {
			setTool(T_BLENDER);
		}

		// Modes

		if (e.getActionCommand().equals("CPFloodFill")) {
			setMode(M_FLOODFILL);
		}

		if (e.getActionCommand().equals("CPRectSelection")) {
			setMode(M_RECT_SELECTION);
		}

		if (e.getActionCommand().equals("CPMoveTool")) {
			setMode(M_MOVE_TOOL);
		}

		if (e.getActionCommand().equals("CPRotateCanvas")) {
			setMode(M_ROTATE_CANVAS);
		}

		// Stroke modes

		if (e.getActionCommand().equals("CPFreeHand")) {
			tools[curBrush].strokeMode = CPBrushInfo.SM_FREEHAND;
			callToolListeners();
		}
		if (e.getActionCommand().equals("CPLine")) {
			tools[curBrush].strokeMode = CPBrushInfo.SM_LINE;
			callToolListeners();
		}
		if (e.getActionCommand().equals("CPBezier")) {
			tools[curBrush].strokeMode = CPBrushInfo.SM_BEZIER;
			callToolListeners();
		}

		if (e.getActionCommand().equals("CPAbout")) {
			JOptionPane.showMessageDialog(getDialogParent(), "ChibiPaint by Codexus\n" + "Version "
					+ VERSION_STRING + "\n\n" + "Copyright (c) 2006-2008 Marc Schefer. All Rights Reserved.\n\n"
					+ "ChibiPaint is free software: you can redistribute it and/or modify\n"
					+ "it under the terms of the GNU General Public License as published by\n"
					+ "the Free Software Foundation, either version 3 of the License, or\n"
					+ "(at your option) any later version.\n\n"

					+ "ChibiPaint is distributed in the hope that it will be useful,\n"
					+ "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
					+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
					+ "GNU General Public License for more details.\n\n"

					+ "You should have received a copy of the GNU General Public License\n"
					+ "along with ChibiPaint. If not, see <http://www.gnu.org/licenses/>.\n",
					"About ChibiPaint...", JOptionPane.PLAIN_MESSAGE);
		}

		if (e.getActionCommand().equals("CPTest")) {
		}

		// Layers actions

		if (e.getActionCommand().equals("CPLayerDuplicate")) {
			artwork.duplicateLayer();
		}

		if (e.getActionCommand().equals("CPLayerMergeDown")) {
			artwork.mergeDown(true);
		}

		if (e.getActionCommand().equals("CPLayerMergeAll")) {
			artwork.mergeAllLayers(true);
		}

		if (e.getActionCommand().equals("CPFill")) {
			artwork.fill(getCurColorRgb() | 0xff000000);
		}

		if (e.getActionCommand().equals("CPClear")) {
			artwork.clear();
		}

		if (e.getActionCommand().equals("CPSelectAll")) {
			artwork.rectangleSelection(artwork.getSize());
			canvas.repaint();
		}

		if (e.getActionCommand().equals("CPDeselectAll")) {
			artwork.rectangleSelection(new CPRect());
			canvas.repaint();
		}

		if (e.getActionCommand().equals("CPHFlip")) {
			artwork.hFlip();
		}

		if (e.getActionCommand().equals("CPVFlip")) {
			artwork.vFlip();
		}

		if (e.getActionCommand().equals("CPMNoise")) {
			artwork.monochromaticNoise();
		}

		if (e.getActionCommand().equals("CPCNoise")) {
			artwork.colorNoise();
		}

		if (e.getActionCommand().equals("CPFXBoxBlur")) {
			showBoxBlurDialog();
		}

		if (e.getActionCommand().equals("CPFXInvert")) {
			artwork.invert();
		}

		if (e.getActionCommand().equals("CPCut")) {
			artwork.cutSelection(true);
		}

		if (e.getActionCommand().equals("CPCopy")) {
			artwork.copySelection();
		}

		if (e.getActionCommand().equals("CPCopyMerged")) {
			artwork.copySelectionMerged();
		}

		if (e.getActionCommand().equals("CPPaste")) {
			artwork.pasteClipboard(true);
		}

		if (e.getActionCommand().equals("CPLinearInterpolation")) {
			canvas.setInterpolation(((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPToggleGrid")) {
			canvas.showGrid(((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPGridOptions")) {
			showGridOptionsDialog();
		}

		if (e.getActionCommand().equals("CPResetCanvasRotation")) {
			canvas.resetRotation();
		}

		if (e.getActionCommand().equals("CPPalColor")) {
			mainGUI.showPalette("color", ((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPPalBrush")) {
			mainGUI.showPalette("brush", ((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPPalLayers")) {
			mainGUI.showPalette("layers", ((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPPalStroke")) {
			mainGUI.showPalette("stroke", ((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPPalSwatches")) {
			mainGUI.showPalette("swatches", ((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPPalTool")) {
			mainGUI.showPalette("tool", ((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPPalMisc")) {
			mainGUI.showPalette("misc", ((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPPalTextures")) {
			mainGUI.showPalette("textures", ((JCheckBoxMenuItem) e.getSource()).isSelected());
		}

		if (e.getActionCommand().equals("CPTogglePalettes")) {
			mainGUI.togglePalettes();
		}

		callCPEventListeners();
	}

	public void addColorListener(ICPColorListener listener) {
		colorListeners.addLast(listener);
	}

	public void addToolListener(ICPToolListener listener) {
		toolListeners.addLast(listener);
	}

	public void callToolListeners() {
		for (ICPToolListener l : toolListeners) {
			l.newTool(curBrush, tools[curBrush]);
		}
	}

	public void addModeListener(ICPModeListener listener) {
		modeListeners.addLast(listener);
	}

	public void callModeListeners() {
		for (ICPModeListener l : modeListeners) {
			l.modeChange(curMode);
		}
	}

	public void addViewListener(ICPViewListener listener) {
		viewListeners.addLast(listener);
	}

	public void callViewListeners(CPViewInfo info) {
		for (ICPViewListener l : viewListeners) {
			l.viewChange(info);
		}
	}

	public void addCPEventListener(ICPEventListener listener) {
		cpEventListeners.addLast(listener);
	}

	public void callCPEventListeners() {
		for (ICPEventListener l : cpEventListeners) {
			l.cpEvent();
		}
	}

	byte[] getPngData(Image img) {
		int imageType = artwork.hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

		// FIXME: Wouldn't it be better to use a BufferedImage and avoid this anyway?
		BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), imageType);
		Graphics bg = bi.getGraphics();
		bg.drawImage(img, 0, 0, null);
		bg.dispose();

		ByteArrayOutputStream pngFileStream = new ByteArrayOutputStream(1024);
		try {
			ImageIO.write(bi, "png", pngFileStream);
		} catch (IOException e) {
		}
		byte[] pngData = pngFileStream.toByteArray();

		return pngData;
	}

	public Image loadImage(String imageName) {
		Image img = imageCache.get(imageName);
		if (img == null) {
			try {
				ClassLoader loader = getClass().getClassLoader();
				Class[] classes = { Image.class };

				URL url = loader.getResource("images/" + imageName);
				img = (Image) url.openConnection().getContent(classes);
			} catch (Throwable t) {
			}
			imageCache.put(imageName, img);
		}
		return img;
	}

	public CPArtwork getArtwork() {
		return artwork;
	}

	public void setMainGUI(CPMainGUI mainGUI) {
		this.mainGUI = mainGUI;
	}

	public CPMainGUI getMainGUI() {
		return mainGUI;
	}

	// returns the Component to be used as parent to display dialogs
	abstract public Component getDialogParent();

	//
	// misc dialog boxes that shouldn't be here v___v

	public void showBoxBlurDialog() {
		JPanel panel = new JPanel();

		panel.add(new JLabel("Blur amount:"));
		SpinnerModel blurXSM = new SpinnerNumberModel(3, 1, 100, 1);
		JSpinner blurX = new JSpinner(blurXSM);
		panel.add(blurX);

		panel.add(new JLabel("Iterations:"));
		SpinnerModel iterSM = new SpinnerNumberModel(1, 1, 8, 1);
		JSpinner iter = new JSpinner(iterSM);
		panel.add(iter);

		Object[] array = { "Box blur\n\n", panel };
		int choice = JOptionPane.showConfirmDialog(getDialogParent(), array, "Box Blur", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (choice == JOptionPane.OK_OPTION) {
			int blur = ((Integer) blurX.getValue()).intValue();
			int iterations = ((Integer) iter.getValue()).intValue();

			artwork.boxBlur(blur, blur, iterations);
			canvas.repaint();
		}
	}

	public void showGridOptionsDialog() {
		JPanel panel = new JPanel();

		panel.add(new JLabel("Grid Size:"));
		SpinnerModel sizeSM = new SpinnerNumberModel(canvas.gridSize, 1, 1000, 1);
		JSpinner sizeSpinner = new JSpinner(sizeSM);
		panel.add(sizeSpinner);

		Object[] array = { "Grid Options\n\n", panel };
		int choice = JOptionPane.showConfirmDialog(getDialogParent(), array, "Grid Options",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice == JOptionPane.OK_OPTION) {
			int size = ((Integer) sizeSpinner.getValue()).intValue();

			canvas.gridSize = size;
			canvas.repaint();
		}

	}

	public boolean isRunningAsApplet() {
		return this instanceof CPControllerApplet;
	}
}
