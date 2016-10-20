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
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import chibipaint.*;
import chibipaint.engine.*;
import chibipaint.util.*;

public class CPCanvas extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener,
		ComponentListener, KeyListener, CPController.ICPToolListener, CPController.ICPModeListener,
		CPArtwork.ICPArtworkListener {

	CPController controller;

	// FIXME: this should not be public
	public Image img;

	BufferedImage checkerboardPattern;
	MemoryImageSource imgSource;
	CPRect updateRegion = new CPRect();

	int[] buffer;
	CPArtwork artwork;

	boolean spacePressed = false;

	// canvas transformations
	float zoom = 1, minZoom = .05f, maxZoom = 16.f;
	int offsetX, offsetY;
	float canvasRotation = 0.f;
	AffineTransform transform = new AffineTransform();
	boolean interpolation = false;

	int mouseX, mouseY;

	boolean brushPreview = false;
	Rectangle oldPreviewRect;

	Cursor defaultCursor, hideCursor, moveCursor, crossCursor;
	boolean mouseIn;

	boolean dontStealFocus = false;

	// Grid options

	// FIXME: these shouldn't be public
	public int gridSize = 32;
	public boolean showGrid = false;

	//
	// Modes system: modes control the way the GUI is reacting to the user input
	// All the tools are implemented through modes
	//

	CPMode defaultMode = new CPDefaultMode();
	CPMode colorPickerMode = new CPColorPickerMode();
	CPMode moveCanvasMode = new CPMoveCanvasMode();
	CPMode rotateCanvasMode = new CPRotateCanvasMode();
	CPMode floodFillMode = new CPFloodFillMode();
	CPMode rectSelectionMode = new CPRectSelectionMode();
	CPMode moveToolMode = new CPMoveToolMode();

	// this must correspond to the stroke modes defined in CPToolInfo
	CPMode drawingModes[] = { new CPFreehandMode(), new CPLineMode(), new CPBezierMode(), };

	CPMode curDrawMode = drawingModes[CPBrushInfo.SM_FREEHAND];
	CPMode curSelectedMode = curDrawMode;
	CPMode activeMode = defaultMode;
	
	// Container with scrollbars
	JPanel container;
	JScrollBar horizScroll, vertScroll;
	
	public CPCanvas(CPController ctrl) {
		this.controller = ctrl;
		artwork = ctrl.getArtwork();

		buffer = artwork.getDisplayBM().getData();

		int w = artwork.width;
		int h = artwork.height;

		imgSource = new MemoryImageSource(w, h, buffer, 0, w);
		imgSource.setAnimated(true);
		// imgSource.setFullBufferUpdates(false);
		img = createImage(imgSource);

		centerCanvas();

		ctrl.setCanvas(this);

		int[] pixels = new int[16 * 16];
		Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
		hideCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisiblecursor");
		defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
		moveCursor = new Cursor(Cursor.MOVE_CURSOR);
		crossCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);

		// Creates the checkboard pattern seen in transparent areas
		checkerboardPattern = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		pixels = new int[64 * 64];
		for (int j = 0; j < 64; j++) {
			for (int i = 0; i < 64; i++) {
				if ((i & 0x8) != 0 ^ (j & 0x8) != 0) {
					pixels[i + j * 64] = 0xffffffff;
				} else {
					pixels[i + j * 64] = 0xffcccccc;
				}
			}
		}
		checkerboardPattern.setRGB(0, 0, 64, 64, pixels, 0, 64);

		/*
		 * KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false); Action escapeAction = new
		 * AbstractAction() { public void actionPerformed(ActionEvent e) { controller.setAlpha((int)(.5f*255)); } };
		 * 
		 * getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW). put(escapeKeyStroke, "SPACE"); getActionMap().put("SPACE",
		 * escapeAction);
		 */

		// register as a listener for Mouse & MouseMotion events
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addComponentListener(this);
		addKeyListener(this);

		controller.addToolListener(this);
		controller.addModeListener(this);

		artwork.addListener(this);

		// We'll need to refresh the whole thing at least once
		updateRegion = new CPRect(w, h);

		// So that the tab key will work
		setFocusTraversalKeysEnabled(false);
	}

	public boolean isOpaque() {
		return true;
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// Container and ScrollBars
	// //////////////////////////////////////////////////////////////////////////////////////
	
	public JPanel getContainer() {
		if (container != null) {
			return container;
		}
		
		container = new JPanel();
		container.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.;
		gbc.weighty = 1.;
		container.add(this, gbc);
		
		vertScroll = new JScrollBar(JScrollBar.VERTICAL);
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.VERTICAL;
		container.add(vertScroll, gbc);

		horizScroll = new JScrollBar(JScrollBar.HORIZONTAL);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		container.add(horizScroll, gbc);
		
		updateScrollBars();
		
		horizScroll.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				Point p = getOffset();
				p.x = - e.getValue();
				setOffset(p);
			}
		});
		
		vertScroll.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				Point p = getOffset();
				p.y = - e.getValue();
				setOffset(p);
			}
		});
		
		return container;
	}
	
	void updateScrollBars() {
		if (horizScroll == null || vertScroll == null
		 || horizScroll.getValueIsAdjusting() || vertScroll.getValueIsAdjusting() ) {
			return;
		}
		
		if (img == null) {
			horizScroll.setEnabled(false);
			vertScroll.setEnabled(false);
		}
		
		Rectangle visibleRect = getRefreshArea(new CPRect(img.getWidth(null), img.getHeight(null)));
		updateScrollBar(horizScroll, visibleRect.x, visibleRect.width, getWidth(), -getOffset().x);
		updateScrollBar(vertScroll, visibleRect.y, visibleRect.height, getHeight(), -getOffset().y);
	}
		
	
	void updateScrollBar(JScrollBar scroll, int visMin, int visWidth, int viewSize, int offset) {
		if (visMin >= 0 && visMin + visWidth < viewSize) {
			scroll.setEnabled(false);
		} else {
			scroll.setEnabled(true);
			
			int xMin = Math.min(0, (visMin - viewSize / 4));
			int xMax = Math.max(viewSize, visMin + visWidth + viewSize / 4);
			
			scroll.setValues(offset, viewSize, xMin + offset, xMax + offset);
			scroll.setBlockIncrement(Math.max(1, (int) (viewSize * .66)));
			scroll.setUnitIncrement(Math.max(1, (int) (viewSize * .05)));
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// painting methods
	// //////////////////////////////////////////////////////////////////////////////////////

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		g2d.setColor(new Color(0x606060));
		g2d.fillRect(0, 0, getWidth(), getHeight());

		if (!updateRegion.isEmpty()) {
			artwork.fusionLayers();
			imgSource.newPixels(updateRegion.left, updateRegion.top, updateRegion.getWidth(), updateRegion.getHeight());
			updateRegion.makeEmpty();
		}

		int w = img.getWidth(null);
		int h = img.getHeight(null);

		Graphics2D g2doc = (Graphics2D) g2d.create();
		g2doc.transform(transform);

		// Draw the checkerboard pattern
		// we'll draw the pattern over an area larger than the image
		// and then remove the extra to avoid display problems
		// when the displayed bitmap doesn't match exactly with the checkerboard area

		GeneralPath path = getCheckerboardBackgroundPath(new Rectangle2D.Float(0, 0, w, h));

		// get the bounding rect and make it a bit larger to be sure to include everything
		Rectangle pathRect = path.getBounds();
		pathRect.x -= 2;
		pathRect.y -= 2;
		pathRect.width += 4;
		pathRect.height += 4;

		g2d.setPaint(new TexturePaint(checkerboardPattern, new Rectangle(0, 0, 64, 64)));
		g2d.fill(pathRect);

		// Draw the image on the canvas

		if (interpolation) {
			RenderingHints hints = g2doc.getRenderingHints();
			hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2doc.addRenderingHints(hints);
		}

		g2doc.drawImage(img, 0, 0, w, h, 0, 0, img.getWidth(null), img.getHeight(null), null);

		// Redraw over the checkerboard border, removing a just a little bit of the image to avoid display problems
		path.append(pathRect, false);
		path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
		g2d.setColor(new Color(0x606060));
		g2d.fill(path);

		// This XOR mode guaranties contrast over all colors
		g2d.setColor(Color.black);
		g2d.setXORMode(new Color(0x808080));

		// Draw selection
		if (!artwork.getSelection().isEmpty()) {
			Stroke stroke = g2d.getStroke();
			g2d
					.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
							new float[] { 2f }, 0f));
			g2d.draw(coordToDisplay(artwork.getSelection()));
			g2d.setStroke(stroke);
		}

		// Draw grid
		if (showGrid) {
			Stroke stroke = g2d.getStroke();
			g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, null, 0f));

			CPRect size = artwork.getSize();
			for (int i = gridSize - 1; i < size.getWidth(); i += gridSize) {
				Point2D.Float p1 = coordToDisplay(new Point2D.Float(i, 0));
				Point2D.Float p2 = coordToDisplay(new Point2D.Float(i, size.getHeight() - 1));
				g2d.draw(new Line2D.Float(p1, p2));
			}

			for (int i = gridSize - 1; i < size.getHeight(); i += gridSize) {
				Point2D.Float p1 = coordToDisplay(new Point2D.Float(0, i));
				Point2D.Float p2 = coordToDisplay(new Point2D.Float(size.getWidth() - 1, i));
				g2d.draw(new Line2D.Float(p1, p2));
			}

			g2d.setStroke(stroke);
		}

		// Additional drawing by the current mode
		activeMode.paint(g2d);

		// This bit of code is used to test repaint areas
		/*
		 * if((test++ & 16) == 0) { g.setColor(Color.magenta); Dimension dd = getSize();
		 * g.fillRect(0,0,dd.width,dd.height); g.setColor(Color.black); }
		 */
	}

	private GeneralPath getCheckerboardBackgroundPath(Rectangle2D r) {
		GeneralPath path = new GeneralPath();
		float delta = (canvasRotation == 0f ? 0f : 1f / zoom);

		Point2D.Float p = coordToDisplay(new Point2D.Float((float) r.getX() + delta, (float) r.getY() + delta));
		path.moveTo(p.x, p.y);

		p = coordToDisplay(new Point2D.Float((float) r.getMaxX() - delta, (float) r.getY() + delta));
		path.lineTo(p.x, p.y);

		p = coordToDisplay(new Point2D.Float((float) r.getMaxX() - delta, (float) r.getMaxY() - delta));
		path.lineTo(p.x, p.y);

		p = coordToDisplay(new Point2D.Float((float) r.getX() + delta, (float) r.getMaxY() - delta));
		path.lineTo(p.x, p.y);
		path.closePath();

		return path;
	}

	//
	// Mouse input methods
	//

	public void mouseEntered(MouseEvent e) {
		mouseIn = true;
	}

	public void mouseExited(MouseEvent e) {
		mouseIn = false;
		repaint();
	}

	public void mousePressed(MouseEvent e) {
		requestFocusInWindow();
		activeMode.mousePressed(e);
	}

	public void mouseDragged(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();

		activeMode.mouseDragged(e);
	}

	public void mouseReleased(MouseEvent e) {
		activeMode.mouseReleased(e);
	}

	public void mouseMoved(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();

		if (!dontStealFocus) {
			requestFocusInWindow();
		}
		activeMode.mouseMoved(e);
		CPTablet.getRef().mouseDetect();
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		float factor = 1;
		if (e.getWheelRotation() > 0) {
			factor = 1 / 1.15f;
		}
		if (e.getWheelRotation() < 0) {
			factor = 1.15f;
		}

		Point2D.Float pf = coordToDocument(new Point2D.Float(mouseX, mouseY));
		if (artwork.isPointWithin(pf.x, pf.y)) {
			zoomOnPoint(getZoom() * factor, mouseX, mouseY);
		} else {
			zoomOnPoint(getZoom() * factor, offsetX + (int) (artwork.width * zoom / 2), offsetY
					+ (int) (artwork.height * zoom / 2));
		}
		// FIXME: clean the above code, some coordinates get transformed multiple times for nothing
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// Transformation methods
	// //////////////////////////////////////////////////////////////////////////////////////

	// Zoom

	public void setZoom(float zoom) {
		this.zoom = zoom;
		updateTransform();
	}

	public float getZoom() {
		return zoom;
	}

	// Offset

	public void setOffset(Point p) {
		setOffset(p.x, p.y);
	}

	public void setOffset(int x, int y) {
		offsetX = x;
		offsetY = y;
		updateTransform();
	}

	public Point getOffset() {
		return new Point(offsetX, offsetY);
	}

	// Rotation

	public void setRotation(float angle) {
		canvasRotation = (float) (angle % (2 * Math.PI));
		updateTransform();
	}

	public float getRotation() {
		return canvasRotation;
	}

	public void setInterpolation(boolean enabled) {
		interpolation = enabled;
		repaint();
	}

	public boolean getInterpolation() {
		return interpolation;
	}

	// Update the affine transformation

	void updateTransform() {
		transform.setToIdentity();
		transform.translate(offsetX, offsetY);
		transform.scale(zoom, zoom);
		transform.rotate(canvasRotation);

		updateScrollBars();
		repaint();
	}

	// More advanced zoom methods

	public void zoomOnCenter(float zoom) {
		Dimension d = getSize();
		zoomOnPoint(zoom, d.width / 2, d.height / 2);
	}

	public void zoomOnPoint(float zoom, int centerX, int centerY) {
		zoom = Math.max(minZoom, Math.min(maxZoom, zoom));
		if (getZoom() != zoom) {
			Point offset = getOffset();
			setOffset(offset.x + (int) ((centerX - offset.x) * (1 - zoom / getZoom())), offset.y
					+ (int) ((centerY - offset.y) * (1 - zoom / getZoom())));
			setZoom(zoom);

			CPController.CPViewInfo viewInfo = new CPController.CPViewInfo();
			viewInfo.zoom = zoom;
			viewInfo.offsetX = offsetX;
			viewInfo.offsetY = offsetY;
			controller.callViewListeners(viewInfo);

			repaint();
		}
	}

	public void zoomIn() {
		zoomOnCenter(getZoom() * 2);
	}

	public void zoomOut() {
		zoomOnCenter(getZoom() * .5f);
	}

	public void zoom100() {
		resetRotation();
		zoomOnCenter(1);
		centerCanvas();
	}

	public void centerCanvas() {
		Dimension d = getSize();
		setOffset((d.width - (int) (artwork.width * getZoom())) / 2,
				(d.height - (int) (artwork.height * getZoom())) / 2);
		repaint();
	}

	public void resetRotation() {
		Dimension d = getSize();
		Point2D.Float center = new Point2D.Float(d.width / 2.f, d.height / 2.f);

		AffineTransform rotTrans = new AffineTransform();
		rotTrans.rotate(-getRotation(), center.x, center.y);
		rotTrans.concatenate(transform);

		setOffset((int) rotTrans.getTranslateX(), (int) rotTrans.getTranslateY());
		setRotation(0);
	}

	//
	// Coordinates and refresh areas methods
	//

	public Point2D.Float coordToDocument(Point2D p) {
		Point2D.Float result = new Point2D.Float();

		try {
			transform.inverseTransform(p, result);
		} catch (NoninvertibleTransformException ex) {
		}

		return result;
	}

	public Point coordToDocumentInt(Point2D p) {
		Point2D.Float result = new Point2D.Float();

		try {
			transform.inverseTransform(p, result);
		} catch (NoninvertibleTransformException ex) {
		}

		return new Point((int) result.x, (int) result.y);
	}

	public Point2D.Float coordToDisplay(Point2D p) {
		Point2D.Float result = new Point2D.Float();

		transform.transform(p, result);

		return result;
	}

	public Point coordToDisplayInt(Point2D p) {
		Point2D.Float result = new Point2D.Float();

		transform.transform(p, result);

		return new Point((int) result.x, (int) result.y);
	}

	public Polygon coordToDisplay(CPRect r) {
		Polygon poly = new Polygon();

		Point p = coordToDisplayInt(new Point(r.left, r.top));
		poly.addPoint(p.x, p.y);

		p = coordToDisplayInt(new Point(r.right - 1, r.top));
		poly.addPoint(p.x, p.y);

		p = coordToDisplayInt(new Point(r.right - 1, r.bottom - 1));
		poly.addPoint(p.x, p.y);

		p = coordToDisplayInt(new Point(r.left, r.bottom - 1));
		poly.addPoint(p.x, p.y);

		return poly;
	}

	public Rectangle getRefreshArea(CPRect r) {
		Point p1 = coordToDisplayInt(new Point(r.left - 1, r.top - 1));
		Point p2 = coordToDisplayInt(new Point(r.left - 1, r.bottom));
		Point p3 = coordToDisplayInt(new Point(r.right, r.top - 1));
		Point p4 = coordToDisplayInt(new Point(r.right, r.bottom));

		Rectangle r2 = new Rectangle();

		r2.x = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
		r2.y = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
		r2.width = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x)) - r2.x + 1;
		r2.height = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y)) - r2.y + 1;

		r2.grow(2, 2); // to be sure to include everything

		return r2;
	}

	private void repaintBrushPreview() {
		if (oldPreviewRect != null) {
			Rectangle r = oldPreviewRect;
			oldPreviewRect = null;
			repaint(r.x, r.y, r.width, r.height);
		}
	}

	private Rectangle getBrushPreviewOval() {
		int bSize = (int) (controller.getBrushSize() * zoom);
		return new Rectangle(mouseX - bSize / 2, mouseY - bSize / 2, bSize, bSize);
	}

	//
	// ChibiPaint interfaces methods
	//

	public void newTool(int tool, CPBrushInfo toolInfo) {
		if (curSelectedMode == curDrawMode) {
			curSelectedMode = drawingModes[toolInfo.strokeMode];
		}
		curDrawMode = drawingModes[toolInfo.strokeMode];

		if (!spacePressed && mouseIn) {
			brushPreview = true;

			Rectangle r = getBrushPreviewOval();
			r.grow(2, 2);
			if (oldPreviewRect != null) {
				r = r.union(oldPreviewRect);
				oldPreviewRect = null;
			}

			repaint(r.x, r.y, r.width, r.height);
		}
	}

	public void modeChange(int mode) {
		switch (mode) {
		case CPController.M_DRAW:
			curSelectedMode = curDrawMode;
			break;

		case CPController.M_FLOODFILL:
			curSelectedMode = floodFillMode;
			break;

		case CPController.M_RECT_SELECTION:
			curSelectedMode = rectSelectionMode;
			break;

		case CPController.M_MOVE_TOOL:
			curSelectedMode = moveToolMode;
			break;

		case CPController.M_ROTATE_CANVAS:
			curSelectedMode = rotateCanvasMode;
			break;
		}
	}

	//
	// misc overloaded and interface standard methods
	//

	public Dimension getPreferredSize() {
		return new Dimension((int) (artwork.width * zoom), (int) (artwork.height * zoom));
	}

	public void componentResized(ComponentEvent e) {
		centerCanvas();
		repaint();
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			spacePressed = true;
			repaintBrushPreview();
			if (activeMode == defaultMode) {
				setCursor(moveCursor);
			}
		}

		// FIXME: these should probably go through the controller to be dispatched
		if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
			switch (e.getKeyCode()) {
			// case KeyEvent.VK_ADD:
			case KeyEvent.VK_EQUALS:
				zoomIn();
				break;
			// case KeyEvent.VK_SUBTRACT:
			case KeyEvent.VK_MINUS:
				zoomOut();
				break;
			case KeyEvent.VK_0:
				// case KeyEvent.VK_NUMPAD0:
				zoom100();
				break;
			}
		} else {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ADD:
			case KeyEvent.VK_EQUALS:
				zoomIn();
				break;
			case KeyEvent.VK_SUBTRACT:
			case KeyEvent.VK_MINUS:
				zoomOut();
				break;
			case KeyEvent.VK_1:
				controller.setAlpha((int) (.1f * 255));
				break;
			case KeyEvent.VK_2:
				controller.setAlpha((int) (.2f * 255));
				break;
			case KeyEvent.VK_3:
				controller.setAlpha((int) (.3f * 255));
				break;
			case KeyEvent.VK_4:
				controller.setAlpha((int) (.4f * 255));
				break;
			case KeyEvent.VK_5:
				controller.setAlpha((int) (.5f * 255));
				break;
			case KeyEvent.VK_6:
				controller.setAlpha((int) (.6f * 255));
				break;
			case KeyEvent.VK_7:
				controller.setAlpha((int) (.7f * 255));
				break;
			case KeyEvent.VK_8:
				controller.setAlpha((int) (.8f * 255));
				break;
			case KeyEvent.VK_9:
				controller.setAlpha((int) (.9f * 255));
				break;
			case KeyEvent.VK_0:
				controller.setAlpha(255);
				break;
			}
		}
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			spacePressed = false;
			if (activeMode == defaultMode) {
				setCursor(defaultCursor);
			}
		}
	}

	public void keyTyped(KeyEvent e) {
		switch (e.getKeyChar()) {
		case '[':
			controller.setBrushSize(controller.getBrushSize() - 1);
			break;
		case ']':
			controller.setBrushSize(controller.getBrushSize() + 1);
			break;
		}
	}

	public boolean isFocusable() {
		return true;
	}

	// Unused interface methods
	public void mouseClicked(MouseEvent e) {
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
	}

	public void updateRegion(CPArtwork artwork, CPRect region) {
		updateRegion.union(region);

		Rectangle r = getRefreshArea(region);
		repaint(r.x, r.y, r.width, r.height);
	}

	// unused method
	public void layerChange(CPArtwork artwork) {
	}

	public void showGrid(boolean show) {
		showGrid = show;
		repaint();
	}

	//
	// base class for the different modes
	//

	abstract class CPMode implements MouseListener, MouseMotionListener {

		// Mouse Input

		public void mousePressed(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		// GUI drawing
		public void paint(Graphics2D g2d) {
		}

	}

	//
	// Default UI Mode when not doing anything: used to start the other modes
	//

	//
	// /!\ WARNING: always use getModifiersEx() to test for modifier
	// keys as methods like isAltDown() have unexpected results
	//

	class CPDefaultMode extends CPMode {

		public void mousePressed(MouseEvent e) {
			// FIXME: replace the moveToolMode hack by a new and improved system
			if (!spacePressed && e.getButton() == MouseEvent.BUTTON1
					&& (((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == 0) || curSelectedMode == moveToolMode)) {

				if (!artwork.getActiveLayer().visible && curSelectedMode != rotateCanvasMode
						&& curSelectedMode != rectSelectionMode) {
					return; // don't draw on a hidden layer
				}
				repaintBrushPreview();

				activeMode = curSelectedMode;
				activeMode.mousePressed(e);
			} else if (!spacePressed
					&& (e.getButton() == MouseEvent.BUTTON3 || (e.getButton() == MouseEvent.BUTTON1 && ((e
							.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK)))) {
				repaintBrushPreview();

				activeMode = colorPickerMode;
				activeMode.mousePressed(e);
			} else if ((e.getButton() == MouseEvent.BUTTON2 || spacePressed)
					&& ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == 0)) {
				repaintBrushPreview();

				activeMode = moveCanvasMode;
				activeMode.mousePressed(e);
			} else if ((e.getButton() == MouseEvent.BUTTON2 || spacePressed)
					&& ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK)) {
				repaintBrushPreview();

				activeMode = rotateCanvasMode;
				activeMode.mousePressed(e);
			}

		}

		public void paint(Graphics2D g2d) {
			if (brushPreview && curSelectedMode == curDrawMode) {
				brushPreview = false;

				Rectangle r = getBrushPreviewOval();
				g2d.drawOval(r.x, r.y, r.width, r.height);

				r.grow(2, 2);
				oldPreviewRect = oldPreviewRect != null ? r.union(oldPreviewRect) : r;
			}
		}

		public void mouseMoved(MouseEvent e) {
			if (!spacePressed) {
				brushPreview = true;

				Rectangle r = getBrushPreviewOval();
				r.grow(2, 2);
				if (oldPreviewRect != null) {
					r = r.union(oldPreviewRect);
					oldPreviewRect = null;
				}

				Point p = e.getPoint();
				Point2D.Float pf = coordToDocument(p);
				if (artwork.isPointWithin(pf.x, pf.y)) {
					setCursor(defaultCursor); // FIXME find a cursor that everyone likes
				} else {
					setCursor(defaultCursor);
				}

				repaint(r.x, r.y, r.width, r.height);
			}
		}
	}

	//
	// Freehand mode
	//

	// FIXME: dragLeft no longer necessary, should not specify the drag button

	class CPFreehandMode extends CPMode {

		boolean dragLeft = false;
		Point2D.Float smoothMouse = new Point2D.Float(0, 0);

		public void mousePressed(MouseEvent e) {
			if (!dragLeft && e.getButton() == MouseEvent.BUTTON1) {
				Point p = e.getPoint();
				Point2D.Float pf = coordToDocument(p);

				dragLeft = true;
				artwork.beginStroke(pf.x, pf.y, CPTablet.getRef().getPressure());

				smoothMouse = (Point2D.Float) pf.clone();
			}
		}

		public void mouseDragged(MouseEvent e) {
			Point p = e.getPoint();
			Point2D.Float pf = coordToDocument(p);

			float smoothing = Math.min(.999f, (float) Math.pow(controller.getBrushInfo().smoothing, .3));

			smoothMouse.x = (1f - smoothing) * pf.x + smoothing * smoothMouse.x;
			smoothMouse.y = (1f - smoothing) * pf.y + smoothing * smoothMouse.y;

			if (dragLeft) {
				artwork.continueStroke(smoothMouse.x, smoothMouse.y, CPTablet.getRef().getPressure());
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (dragLeft && e.getButton() == MouseEvent.BUTTON1) {
				dragLeft = false;
				artwork.endStroke();

				activeMode = defaultMode; // yield control to the default mode
			}
		}
	}

	//
	// Line drawing mode
	//

	class CPLineMode extends CPMode {

		boolean dragLine = false;
		Point dragLineFrom, dragLineTo;

		public void mousePressed(MouseEvent e) {
			if (!dragLine && e.getButton() == MouseEvent.BUTTON1) {
				Point p = e.getPoint();

				dragLine = true;
				dragLineFrom = dragLineTo = (Point) p.clone();
			}
		}

		public void mouseDragged(MouseEvent e) {
			Point p = e.getPoint();

			Rectangle r = new Rectangle(Math.min(dragLineFrom.x, dragLineTo.x), Math.min(dragLineFrom.y, dragLineTo.y),
					Math.abs(dragLineFrom.x - dragLineTo.x) + 1, Math.abs(dragLineFrom.y - dragLineTo.y) + 1);
			r = r.union(new Rectangle(Math.min(dragLineFrom.x, p.x), Math.min(dragLineFrom.y, p.y), Math
					.abs(dragLineFrom.x - p.x) + 1, Math.abs(dragLineFrom.y - p.y) + 1));
			dragLineTo = (Point) p.clone();
			repaint(r.x, r.y, r.width, r.height);
		}

		public void mouseReleased(MouseEvent e) {
			if (dragLine && e.getButton() == MouseEvent.BUTTON1) {
				Point p = e.getPoint();
				Point2D.Float pf = coordToDocument(p);

				dragLine = false;

				Point2D.Float from = coordToDocument(dragLineFrom);
				artwork.beginStroke(from.x, from.y, 1);
				artwork.continueStroke(pf.x, pf.y, 1);
				artwork.endStroke();

				Rectangle r = new Rectangle(Math.min(dragLineFrom.x, dragLineTo.x), Math.min(dragLineFrom.y,
						dragLineTo.y), Math.abs(dragLineFrom.x - dragLineTo.x) + 1, Math.abs(dragLineFrom.y
						- dragLineTo.y) + 1);
				repaint(r.x, r.y, r.width, r.height);

				activeMode = defaultMode; // yield control to the default mode
			}
		}

		public void paint(Graphics2D g2d) {
			if (dragLine) {
				g2d.drawLine(dragLineFrom.x, dragLineFrom.y, dragLineTo.x, dragLineTo.y);
			}
		}
	}

	//
	// Bezier drawing mode
	//

	class CPBezierMode extends CPMode {

		// bezier drawing
		static final int BEZIER_POINTS = 500;
		static final int BEZIER_POINTS_PREVIEW = 100;

		boolean dragBezier = false;
		int dragBezierMode; // 0 Initial drag, 1 first control point, 2 second point
		Point2D.Float dragBezierP0, dragBezierP1, dragBezierP2, dragBezierP3;

		public void mousePressed(MouseEvent e) {
			Point2D.Float p = coordToDocument(e.getPoint());

			if (!dragBezier && !spacePressed && e.getButton() == MouseEvent.BUTTON1) {
				dragBezier = true;
				dragBezierMode = 0;
				dragBezierP0 = dragBezierP1 = dragBezierP2 = dragBezierP3 = (Point2D.Float) p.clone();
			}
		}

		public void mouseDragged(MouseEvent e) {
			Point2D.Float p = coordToDocument(e.getPoint());

			if (dragBezier && dragBezierMode == 0) {
				dragBezierP2 = dragBezierP3 = (Point2D.Float) p.clone();
				repaint();
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (dragBezier && e.getButton() == MouseEvent.BUTTON1) {
				if (dragBezierMode == 0) {
					dragBezierMode = 1;
				} else if (dragBezierMode == 1) {
					dragBezierMode = 2;
				} else if (dragBezierMode == 2) {
					dragBezier = false;

					Point2D.Float p0 = dragBezierP0;
					Point2D.Float p1 = dragBezierP1;
					Point2D.Float p2 = dragBezierP2;
					Point2D.Float p3 = dragBezierP3;

					CPBezier bezier = new CPBezier();
					bezier.x0 = p0.x;
					bezier.y0 = p0.y;
					bezier.x1 = p1.x;
					bezier.y1 = p1.y;
					bezier.x2 = p2.x;
					bezier.y2 = p2.y;
					bezier.x3 = p3.x;
					bezier.y3 = p3.y;

					float x[] = new float[BEZIER_POINTS];
					float y[] = new float[BEZIER_POINTS];

					bezier.compute(x, y, BEZIER_POINTS);

					artwork.beginStroke(x[0], y[0], 1);
					for (int i = 1; i < BEZIER_POINTS; i++) {
						artwork.continueStroke(x[i], y[i], 1);
					}
					artwork.endStroke();
					repaint();

					activeMode = defaultMode; // yield control to the default mode
				}
			}
		}

		public void mouseMoved(MouseEvent e) {
			Point2D.Float p = coordToDocument(e.getPoint());

			if (dragBezier && dragBezierMode == 1) {
				dragBezierP1 = (Point2D.Float) p.clone();
				repaint(); // FIXME: repaint only the bezier region
			}

			if (dragBezier && dragBezierMode == 2) {
				dragBezierP2 = (Point2D.Float) p.clone();
				repaint(); // FIXME: repaint only the bezier region
			}
		}

		public void paint(Graphics2D g2d) {
			if (dragBezier) {
				CPBezier bezier = new CPBezier();

				Point2D.Float p0 = coordToDisplay(dragBezierP0);
				Point2D.Float p1 = coordToDisplay(dragBezierP1);
				Point2D.Float p2 = coordToDisplay(dragBezierP2);
				Point2D.Float p3 = coordToDisplay(dragBezierP3);

				bezier.x0 = p0.x;
				bezier.y0 = p0.y;
				bezier.x1 = p1.x;
				bezier.y1 = p1.y;
				bezier.x2 = p2.x;
				bezier.y2 = p2.y;
				bezier.x3 = p3.x;
				bezier.y3 = p3.y;

				int x[] = new int[BEZIER_POINTS_PREVIEW];
				int y[] = new int[BEZIER_POINTS_PREVIEW];
				bezier.compute(x, y, BEZIER_POINTS_PREVIEW);

				g2d.drawPolyline(x, y, BEZIER_POINTS_PREVIEW);
				g2d.drawLine((int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y);
				g2d.drawLine((int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y);
			}
		}
	}

	//
	// Color picker mode
	//

	class CPColorPickerMode extends CPMode {

		int mouseButton;

		public void mousePressed(MouseEvent e) {
			Point p = e.getPoint();
			Point2D.Float pf = coordToDocument(p);

			mouseButton = e.getButton();

			if (artwork.isPointWithin(pf.x, pf.y)) {
				controller.setCurColorRgb(artwork.colorPicker(pf.x, pf.y));
			}

			setCursor(crossCursor);
		}

		public void mouseDragged(MouseEvent e) {
			Point p = e.getPoint();
			Point2D.Float pf = coordToDocument(p);

			if (artwork.isPointWithin(pf.x, pf.y)) {
				controller.setCurColorRgb(artwork.colorPicker(pf.x, pf.y));
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (e.getButton() == mouseButton) {
				setCursor(defaultCursor);
				activeMode = defaultMode; // yield control to the default mode
			}
		}
	}

	//
	// Canvas move mode
	//

	class CPMoveCanvasMode extends CPMode {

		boolean dragMiddle = false;
		int dragMoveX, dragMoveY;
		Point dragMoveOffset;
		int dragMoveButton;

		public void mousePressed(MouseEvent e) {
			Point p = e.getPoint();

			if (!dragMiddle && (e.getButton() == MouseEvent.BUTTON2 || spacePressed)) {
				repaintBrushPreview();

				dragMiddle = true;
				dragMoveButton = e.getButton();
				dragMoveX = p.x;
				dragMoveY = p.y;
				dragMoveOffset = getOffset();
				setCursor(moveCursor);
			}
		}

		public void mouseDragged(MouseEvent e) {
			if (dragMiddle) {
				Point p = e.getPoint();

				setOffset(dragMoveOffset.x + p.x - dragMoveX, offsetY = dragMoveOffset.y + p.y - dragMoveY);
				repaint();
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (dragMiddle && e.getButton() == dragMoveButton) {
				dragMiddle = false;
				setCursor(defaultCursor);

				activeMode = defaultMode; // yield control to the default mode
			}
		}
	}

	//
	// Flood fill mode
	//

	class CPFloodFillMode extends CPMode {

		public void mousePressed(MouseEvent e) {
			Point p = e.getPoint();
			Point2D.Float pf = coordToDocument(p);

			if (artwork.isPointWithin(pf.x, pf.y)) {
				artwork.floodFill(pf.x, pf.y);
				repaint();
			}

			activeMode = defaultMode; // yield control to the default mode
		}

		public void mouseDragged(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}
	}

	//
	// CPRectSelection mode
	//

	class CPRectSelectionMode extends CPMode {

		Point firstClick;
		CPRect curRect = new CPRect();

		public void mousePressed(MouseEvent e) {
			Point p = coordToDocumentInt(e.getPoint());

			curRect.makeEmpty();
			firstClick = p;

			repaint();
		}

		public void mouseDragged(MouseEvent e) {
			Point p = coordToDocumentInt(e.getPoint());
			boolean square = e.isShiftDown();
			int squareDist = Math.max(Math.abs(p.x - firstClick.x), Math.abs(p.y - firstClick.y));

			if (p.x >= firstClick.x) {
				curRect.left = firstClick.x;
				curRect.right = square ? firstClick.x + squareDist : p.x;
			} else {
				curRect.left = square ? firstClick.x - squareDist : p.x;
				curRect.right = firstClick.x;
			}

			if (p.y >= firstClick.y) {
				curRect.top = firstClick.y;
				curRect.bottom = square ? firstClick.y + squareDist : p.y;
			} else {
				curRect.top = square ? firstClick.y - squareDist : p.y;
				curRect.bottom = firstClick.y;
			}

			repaint();
		}

		public void mouseReleased(MouseEvent e) {
			artwork.rectangleSelection(curRect);

			activeMode = defaultMode; // yield control to the default mode
			repaint();
		}

		public void paint(Graphics2D g2d) {
			if (!curRect.isEmpty()) {
				g2d.draw(coordToDisplay(curRect));
			}
		}
	}

	//
	// CPMoveTool mode
	//

	class CPMoveToolMode extends CPMode {

		Point firstClick;

		public void mousePressed(MouseEvent e) {
			Point p = coordToDocumentInt(e.getPoint());
			firstClick = p;

			artwork.beginPreviewMode(e.isAltDown());

			// FIXME: The following hack avoids a slight display glitch
			// if the whole move tool mess is fixed it probably won't be necessary anymore
			artwork.move(0, 0);
		}

		public void mouseDragged(MouseEvent e) {
			Point p = coordToDocumentInt(e.getPoint());
			artwork.move(p.x - firstClick.x, p.y - firstClick.y);
			repaint();
		}

		public void mouseReleased(MouseEvent e) {
			artwork.endPreviewMode();
			activeMode = defaultMode; // yield control to the default mode
			repaint();
		}
	}

	//
	// Canvas rotate mode
	//

	class CPRotateCanvasMode extends CPMode {

		Point firstClick;
		float initAngle;
		AffineTransform initTransform;
		boolean dragged;

		public void mousePressed(MouseEvent e) {
			Point p = e.getPoint();
			firstClick = (Point) p.clone();

			initAngle = getRotation();
			initTransform = new AffineTransform(transform);

			dragged = false;

			repaintBrushPreview();
		}

		public void mouseDragged(MouseEvent e) {
			dragged = true;

			Point p = e.getPoint();
			Dimension d = getSize();
			Point2D.Float center = new Point2D.Float(d.width / 2.f, d.height / 2.f);

			float deltaAngle = (float) Math.atan2(p.y - center.y, p.x - center.x)
					- (float) Math.atan2(firstClick.y - center.y, firstClick.x - center.x);

			AffineTransform rotTrans = new AffineTransform();
			rotTrans.rotate(deltaAngle, center.x, center.y);

			rotTrans.concatenate(initTransform);

			setRotation(initAngle + deltaAngle);
			setOffset((int) rotTrans.getTranslateX(), (int) rotTrans.getTranslateY());
			repaint();
		}

		public void mouseReleased(MouseEvent e) {
			if (!dragged) {
				resetRotation();
			}

			activeMode = defaultMode; // yield control to the default mode
		}
	}

	/*
	 * // // mode //
	 * 
	 * class CPMode extends CPMode { public void mousePressed(MouseEvent e) {} public void mouseDragged(MouseEvent e) {}
	 * public void mouseReleased(MouseEvent e) {} }
	 */
}
