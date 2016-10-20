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

package chibipaint.engine;

import java.util.*;

import chibipaint.*;
import chibipaint.engine.CPBrushManager.*;
import chibipaint.util.*;

//FIXME: BROKEN: use setForegroundColor and setBrush, controller's layerChanged replaced by the ICPArtworkListener mechanism

public class CPArtwork {

	public int width, height;

	Vector<CPLayer> layers;
	CPLayer curLayer;
	int activeLayer;

	CPRect curSelection = new CPRect();

	CPLayer fusion, undoBuffer, opacityBuffer;
	CPRect fusionArea, undoArea, opacityArea;

	Random rnd = new Random();

	public interface ICPArtworkListener {

		void updateRegion(CPArtwork artwork, CPRect region);

		void layerChange(CPArtwork artwork);
	}

	private LinkedList<ICPArtworkListener> artworkListeners = new LinkedList();

	// Clipboard

	static private class CPClip {

		CPColorBmp bmp;
		int x, y;

		CPClip(CPColorBmp bmp, int x, int y) {
			this.bmp = bmp;
			this.x = x;
			this.y = y;
		}
	};

	CPClip clipboard = null;

	LinkedList<CPUndo> undoList, redoList;

	private CPBrushInfo curBrush;

	// FIXME: shouldn't be public
	public CPBrushManager brushManager = new CPBrushManager();

	float lastX, lastY, lastPressure;
	int[] brushBuffer = null;

	private int maxUndo = 30;

	//
	// Current Engine Parameters
	//

	boolean sampleAllLayers = false;
	boolean lockAlpha = false;

	int curColor;

	CPBrushTool paintingModes[] = { new CPBrushToolSimpleBrush(), new CPBrushToolEraser(), new CPBrushToolDodge(),
			new CPBrushToolBurn(), new CPBrushToolWatercolor(), new CPBrushToolBlur(), new CPBrushToolSmudge(),
			new CPBrushToolOil(), };

	static final int BURN_CONSTANT = 260;
	static final int BLUR_MIN = 64;
	static final int BLUR_MAX = 1;

	public CPArtwork(int width, int height) {
		this.width = width;
		this.height = height;

		layers = new Vector();

		CPLayer defaultLayer = new CPLayer(width, height);
		defaultLayer.name = getDefaultLayerName();
		defaultLayer.clear(0xffffffff);
		layers.add(defaultLayer);

		curLayer = layers.get(0);
		fusionArea = new CPRect(0, 0, width, height);
		undoArea = new CPRect();
		opacityArea = new CPRect();
		activeLayer = 0;
		curSelection.makeEmpty();

		undoBuffer = new CPLayer(width, height);
		// we reserve a double sized buffer to be used as a 16bits per channel buffer
		opacityBuffer = new CPLayer(width, height);

		fusion = new CPLayer(width, height);

		undoList = new LinkedList();
		redoList = new LinkedList();
	}

	public long getDocMemoryUsed() {
		return (long) width * height * 4 * (3 + layers.size())
				+ (clipboard != null ? clipboard.bmp.getWidth() * clipboard.bmp.getHeight() * 4 : 0);
	}

	public long getUndoMemoryUsed() {
		long total = 0;

		CPColorBmp lastBitmap = clipboard != null ? clipboard.bmp : null;

		for (int i = redoList.size() - 1; i >= 0; i--) {
			CPUndo undo = redoList.get(i);

			total += undo.getMemoryUsed(true, lastBitmap);
		}

		for (CPUndo undo : undoList) {
			total += undo.getMemoryUsed(false, lastBitmap);
		}

		return total;
	}

	public CPLayer getDisplayBM() {
		fusionLayers();
		return fusion;

		// for(int i=0; i<opacityBuffer.data.length; i++)
		// opacityBuffer.data[i] |= 0xff000000;
		// return opacityBuffer;
	}

	public void fusionLayers() {
		if (fusionArea.isEmpty()) {
			return;
		}

		mergeOpacityBuffer(curColor, false);

		fusion.clear(fusionArea, 0x00ffffff);
		boolean fullAlpha = true, first = true;
		for (CPLayer l : layers) {
			if (!first) {
				fullAlpha = fullAlpha && fusion.hasAlpha(fusionArea);
			}

			if (l.visible) {
				first = false;
				if (fullAlpha) {
					l.fusionWithFullAlpha(fusion, fusionArea);
				} else {
					l.fusionWith(fusion, fusionArea);
				}
			}
		}

		fusionArea.makeEmpty();
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	// Listeners
	// ///////////////////////////////////////////////////////////////////////////////////

	public void addListener(ICPArtworkListener listener) {
		artworkListeners.addLast(listener);
	}

	public void callListenersUpdateRegion(CPRect region) {
		for (ICPArtworkListener l : artworkListeners) {
			l.updateRegion(this, region);
		}
	}

	public void callListenersLayerChange() {
		for (ICPArtworkListener l : artworkListeners) {
			l.layerChange(this);
		}
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	// Global Parameters
	// ///////////////////////////////////////////////////////////////////////////////////

	public void setSampleAllLayers(boolean b) {
		sampleAllLayers = b;
	}

	public void setLockAlpha(boolean b) {
		lockAlpha = b;
	}

	public void setForegroundColor(int color) {
		curColor = color;
	}

	public void setBrush(CPBrushInfo brush) {
		curBrush = brush;
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	// Paint engine
	// ///////////////////////////////////////////////////////////////////////////////////

	public void beginStroke(float x, float y, float pressure) {
		if (curBrush == null) {
			return;
		}

		paintingModes[curBrush.paintMode].beginStroke(x, y, pressure);
	}

	public void continueStroke(float x, float y, float pressure) {
		if (curBrush == null) {
			return;
		}

		paintingModes[curBrush.paintMode].continueStroke(x, y, pressure);
	}

	public void endStroke() {
		if (curBrush == null) {
			return;
		}

		paintingModes[curBrush.paintMode].endStroke();
	}

	void mergeOpacityBuffer(int color, boolean clear) {
		if (!opacityArea.isEmpty()) {
			if (curBrush.paintMode != CPBrushInfo.M_ERASE || !lockAlpha) {
				paintingModes[curBrush.paintMode].mergeOpacityBuf(opacityArea, color);
			} else {
				// FIXME: it would be nice to be able to set the paper color
				paintingModes[CPBrushInfo.M_PAINT].mergeOpacityBuf(opacityArea, 0xffffff);
			}

			if (lockAlpha) {
				restoreAlpha(opacityArea);
			}

			if (clear) {
				opacityBuffer.clear(opacityArea, 0);
			}

			opacityArea.makeEmpty();
		}
	}

	void restoreAlpha(CPRect r) {
		getActiveLayer().copyAlphaFrom(undoBuffer, r);
	}

	// Extend this class to create new tools and brush types
	abstract class CPBrushTool {

		abstract public void beginStroke(float x, float y, float pressure);

		abstract public void continueStroke(float x, float y, float pressure);

		abstract public void endStroke();

		abstract public void mergeOpacityBuf(CPRect dstRect, int color);
	}

	abstract class CPBrushToolBase extends CPBrushTool {

		public void beginStroke(float x, float y, float pressure) {
			undoBuffer.copyFrom(curLayer);
			undoArea.makeEmpty();

			opacityBuffer.clear();
			opacityArea.makeEmpty();

			lastX = x;
			lastY = y;
			lastPressure = pressure;
			paintDab(x, y, pressure);
		}

		public void continueStroke(float x, float y, float pressure) {
			float dist = (float) Math.sqrt(((lastX - x) * (lastX - x) + (lastY - y) * (lastY - y)));
			float spacing = Math.max(curBrush.minSpacing, curBrush.curSize * curBrush.spacing);

			if (dist > spacing) {
				float nx = lastX, ny = lastY, np = lastPressure;

				float df = (spacing - 0.001f) / dist;
				for (float f = df; f <= 1.f; f += df) {
					nx = f * x + (1.f - f) * lastX;
					ny = f * y + (1.f - f) * lastY;
					np = f * pressure + (1.f - f) * lastPressure;
					paintDab(nx, ny, np);
				}
				lastX = nx;
				lastY = ny;
				lastPressure = np;
			}
		}

		public void endStroke() {
			undoArea.clip(getSize());
			if (!undoArea.isEmpty()) {
				mergeOpacityBuffer(curColor, false);
				addUndo(new CPUndoPaint());
			}
			brushBuffer = null;
		}

		void paintDab(float x, float y, float pressure) {
			curBrush.applyPressure(pressure);
			if (curBrush.scattering > 0f) {
				x += rnd.nextGaussian() * curBrush.curScattering / 4f;
				y += rnd.nextGaussian() * curBrush.curScattering / 4f;
				// x += (rnd.nextFloat() - .5f) * tool.scattering;
				// y += (rnd.nextFloat() - .5f) * tool.scattering;
			}
			CPBrushDab dab = brushManager.getDab(x, y, curBrush);
			paintDab(dab);
		}

		void paintDab(CPBrushDab dab) {
			CPRect srcRect = new CPRect(dab.width, dab.height);
			CPRect dstRect = new CPRect(dab.width, dab.height);
			dstRect.translate(dab.x, dab.y);

			clipSourceDest(srcRect, dstRect);

			// drawing entirely outside the canvas
			if (dstRect.isEmpty()) {
				return;
			}

			undoArea.union(dstRect);
			opacityArea.union(dstRect);
			invalidateFusion(dstRect);

			paintDabImplementation(srcRect, dstRect, dab);
		}

		abstract void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab);
	}

	class CPBrushToolSimpleBrush extends CPBrushToolBase {

		void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab) {
			// FIXME: there should be no reference to a specific tool here
			// create a new brush parameter instead
			if (curBrush.isAirbrush) {
				paintFlow(srcRect, dstRect, dab.brush, dab.width, Math.max(1, dab.alpha / 8));
			} else if (curBrush.toolNb == CPController.T_PEN) {
				paintFlow(srcRect, dstRect, dab.brush, dab.width, Math.max(1, dab.alpha / 2));
			} else {
				// paintOpacityFlow(srcRect, dstRect, brush, dab.stride, alpha, 255);
				// paintOpacityFlow(srcRect, dstRect, brush, dab.stride, 128, alpha);
				paintOpacity(srcRect, dstRect, dab.brush, dab.width, dab.alpha);
			}
		}

		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.data;
			int[] undoData = undoBuffer.data;

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];

						int destAlpha = destColor >>> 24;
						int newLayerAlpha = opacityAlpha + destAlpha * (255 - opacityAlpha) / 255;
						int realAlpha = 255 * opacityAlpha / newLayerAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = (((color >>> 16 & 0xff) * realAlpha + (destColor >>> 16 & 0xff) * invAlpha) / 255) << 16
								& 0xff0000
								| (((color >>> 8 & 0xff) * realAlpha + (destColor >>> 8 & 0xff) * invAlpha) / 255) << 8
								& 0xff00 | (((color & 0xff) * realAlpha + (destColor & 0xff) * invAlpha) / 255) & 0xff;

						newColor |= newLayerAlpha << 24 & 0xff000000;
						curLayer.data[dstOffset] = newColor;
					}
				}
			}
		}

		void paintOpacity(CPRect srcRect, CPRect dstRect, byte[] brush, int w, int alpha) {
			int[] opacityData = opacityBuffer.data;

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int brushAlpha = (brush[srcOffset] & 0xff) * alpha;
					if (brushAlpha != 0) {
						int opacityAlpha = opacityData[dstOffset];
						if (brushAlpha > opacityAlpha) {
							opacityData[dstOffset] = brushAlpha;
						}
					}

				}
			}
		}

		void paintFlow(CPRect srcRect, CPRect dstRect, byte[] brush, int w, int alpha) {
			int[] opacityData = opacityBuffer.data;

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int brushAlpha = (brush[srcOffset] & 0xff) * alpha;
					if (brushAlpha != 0) {
						int opacityAlpha = Math.min(255 * 255, opacityData[dstOffset]
								+ (255 - opacityData[dstOffset] / 255) * brushAlpha / 255);
						opacityData[dstOffset] = opacityAlpha;
					}

				}
			}
		}

		void paintOpacityFlow(CPRect srcRect, CPRect dstRect, byte[] brush, int w, int opacity, int flow) {
			int[] opacityData = opacityBuffer.data;

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int brushAlpha = (brush[srcOffset] & 0xff) * flow;
					if (brushAlpha != 0) {
						int opacityAlpha = opacityData[dstOffset];

						int newAlpha = Math.min(255 * 255, opacityAlpha + (opacity - opacityAlpha / 255) * brushAlpha
								/ 255);
						newAlpha = Math.min(opacity * (brush[srcOffset] & 0xff), newAlpha);
						if (newAlpha > opacityAlpha) {
							opacityData[dstOffset] = newAlpha;
						}
					}

				}
			}
		}
	}

	class CPBrushToolEraser extends CPBrushToolSimpleBrush {

		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.data;
			int[] undoData = undoBuffer.data;

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];
						int destAlpha = destColor >>> 24;

						int realAlpha = destAlpha * (255 - opacityAlpha) / 255;
						curLayer.data[dstOffset] = destColor & 0xffffff | realAlpha << 24;
					}
				}
			}
		}
	}

	class CPBrushToolDodge extends CPBrushToolSimpleBrush {

		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.data;
			int[] undoData = undoBuffer.data;

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];
						if ((destColor & 0xff000000) != 0) {
							opacityAlpha += 255;
							int r = (destColor >>> 16 & 0xff) * opacityAlpha / 255;
							int g = (destColor >>> 8 & 0xff) * opacityAlpha / 255;
							int b = (destColor & 0xff) * opacityAlpha / 255;

							if (r > 255) {
								r = 255;
							}
							if (g > 255) {
								g = 255;
							}
							if (b > 255) {
								b = 255;
							}

							int newColor = destColor & 0xff000000 | r << 16 | g << 8 | b;
							curLayer.data[dstOffset] = newColor;
						}
					}
				}
			}
		}
	}

	class CPBrushToolBurn extends CPBrushToolSimpleBrush {

		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.data;
			int[] undoData = undoBuffer.data;

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];
						if ((destColor & 0xff000000) != 0) {
							// opacityAlpha = 255 - opacityAlpha;

							int r = destColor >>> 16 & 0xff;
							int g = destColor >>> 8 & 0xff;
							int b = destColor & 0xff;

							r = r - (BURN_CONSTANT - r) * opacityAlpha / 255;
							g = g - (BURN_CONSTANT - g) * opacityAlpha / 255;
							b = b - (BURN_CONSTANT - b) * opacityAlpha / 255;

							if (r < 0) {
								r = 0;
							}
							if (g < 0) {
								g = 0;
							}
							if (b < 0) {
								b = 0;
							}

							int newColor = destColor & 0xff000000 | r << 16 | g << 8 | b;
							curLayer.data[dstOffset] = newColor;
						}
					}
				}
			}
		}
	}

	class CPBrushToolBlur extends CPBrushToolSimpleBrush {

		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.data;
			int[] undoData = undoBuffer.data;

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int blur = BLUR_MIN + (BLUR_MAX - BLUR_MIN) * opacityAlpha / 255;

						int destColor = undoData[dstOffset];
						int a = blur * (destColor >>> 24 & 0xff);
						int r = blur * (destColor >>> 16 & 0xff);
						int g = blur * (destColor >>> 8 & 0xff);
						int b = blur * (destColor & 0xff);
						int sum = blur + 4;

						destColor = undoData[j > 0 ? dstOffset - width : dstOffset];
						a += destColor >>> 24 & 0xff;
						r += destColor >>> 16 & 0xff;
						g += destColor >>> 8 & 0xff;
						b += destColor & 0xff;

						destColor = undoData[j < height - 1 ? dstOffset + width : dstOffset];
						a += destColor >>> 24 & 0xff;
						r += destColor >>> 16 & 0xff;
						g += destColor >>> 8 & 0xff;
						b += destColor & 0xff;

						destColor = undoData[i > 0 ? dstOffset - 1 : dstOffset];
						a += destColor >>> 24 & 0xff;
						r += destColor >>> 16 & 0xff;
						g += destColor >>> 8 & 0xff;
						b += destColor & 0xff;

						destColor = undoData[i < width - 1 ? dstOffset + 1 : dstOffset];
						a += destColor >>> 24 & 0xff;
						r += destColor >>> 16 & 0xff;
						g += destColor >>> 8 & 0xff;
						b += destColor & 0xff;

						a /= sum;
						r /= sum;
						g /= sum;
						b /= sum;
						curLayer.data[dstOffset] = a << 24 | r << 16 | g << 8 | b;
					}
				}
			}
		}
	}

	// Brushes derived from this class use the opacity buffer
	// as a simple alpha layer
	class CPBrushToolDirectBrush extends CPBrushToolSimpleBrush {

		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.data;
			int[] undoData = undoBuffer.data;

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int color1 = opacityData[dstOffset];
					int alpha1 = (color1 >>> 24);
					if (alpha1 <= 0) {
						continue;
					}
					int color2 = undoData[dstOffset];
					int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						curLayer.data[dstOffset] = newAlpha << 24
								| (((color1 >>> 16 & 0xff) * realAlpha + (color2 >>> 16 & 0xff) * invAlpha) / 255) << 16
								| (((color1 >>> 8 & 0xff) * realAlpha + (color2 >>> 8 & 0xff) * invAlpha) / 255) << 8
								| (((color1 & 0xff) * realAlpha + (color2 & 0xff) * invAlpha) / 255);
					}
				}
			}
		}
	}

	class CPBrushToolWatercolor extends CPBrushToolDirectBrush {

		static final int wcMemory = 50;
		static final int wxMaxSampleRadius = 64;

		LinkedList<CPColorFloat> previousSamples;

		public void beginStroke(float x, float y, float pressure) {
			previousSamples = null;

			super.beginStroke(x, y, pressure);
		}

		void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab) {
			if (previousSamples == null) {
				CPColorFloat startColor = sampleColor((dstRect.left + dstRect.right) / 2,
						(dstRect.top + dstRect.bottom) / 2, Math.max(1, Math.min(wxMaxSampleRadius,
								dstRect.getWidth() * 2 / 6)), Math.max(1, Math.min(wxMaxSampleRadius, dstRect
								.getHeight() * 2 / 6)));

				previousSamples = new LinkedList();
				for (int i = 0; i < wcMemory; i++) {
					previousSamples.addLast(startColor);
				}
			}
			CPColorFloat wcColor = new CPColorFloat(0, 0, 0);
			for (CPColorFloat sample : previousSamples) {
				wcColor.r += sample.r;
				wcColor.g += sample.g;
				wcColor.b += sample.b;
			}
			wcColor.r /= previousSamples.size();
			wcColor.g /= previousSamples.size();
			wcColor.b /= previousSamples.size();

			// resaturation
			int color = curColor & 0xffffff;
			wcColor.mixWith(new CPColorFloat(color), curBrush.resat * curBrush.resat);

			int newColor = wcColor.toInt();

			// bleed
			wcColor.mixWith(sampleColor((dstRect.left + dstRect.right) / 2, (dstRect.top + dstRect.bottom) / 2, Math
					.max(1, Math.min(wxMaxSampleRadius, dstRect.getWidth() * 2 / 6)), Math.max(1, Math.min(
					wxMaxSampleRadius, dstRect.getHeight() * 2 / 6))), curBrush.bleed);

			previousSamples.addLast(wcColor);
			previousSamples.removeFirst();

			paintDirect(srcRect, dstRect, dab.brush, dab.width, Math.max(1, dab.alpha / 4), newColor);
			mergeOpacityBuffer(0, false);
			if (sampleAllLayers) {
				fusionLayers();
			}
		}

		void paintDirect(CPRect srcRect, CPRect dstRect, byte[] brush, int w, int alpha, int color1) {
			int[] opacityData = opacityBuffer.data;

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int alpha1 = (brush[srcOffset] & 0xff) * alpha / 255;
					if (alpha1 <= 0) {
						continue;
					}

					int color2 = opacityData[dstOffset];
					int alpha2 = (color2 >>> 24);

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						// The usual alpha blending formula C = A * alpha + B * (1 - alpha)
						// has to rewritten in the form C = A + (1 - alpha) * B - (1 - alpha) *A
						// that way the rounding up errors won't cause problems

						int newColor = newAlpha << 24
								| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
										* invAlpha) / 255)) << 16
								| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
										* invAlpha) / 255)) << 8
								| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));

						opacityData[dstOffset] = newColor;
					}
				}
			}
		}

		CPColorFloat sampleColor(int x, int y, int dx, int dy) {
			LinkedList<CPColorFloat> samples = new LinkedList<CPColorFloat>();

			CPLayer layerToSample = sampleAllLayers ? fusion : getActiveLayer();

			samples.addLast(new CPColorFloat(layerToSample.getPixel(x, y) & 0xffffff));

			for (float r = 0.25f; r < 1.001f; r += .25f) {
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x + r * dx), y) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x - r * dx), y) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel(x, (int) (y + r * dy)) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel(x, (int) (y - r * dy)) & 0xffffff));

				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x + r * .7f * dx), (int) (y + r * .7f
						* dy)) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x + r * .7f * dx), (int) (y - r * .7f
						* dy)) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x - r * .7f * dx), (int) (y + r * .7f
						* dy)) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x - r * .7f * dx), (int) (y - r * .7f
						* dy)) & 0xffffff));
			}

			CPColorFloat average = new CPColorFloat(0, 0, 0);
			for (CPColorFloat sample : samples) {
				average.r += sample.r;
				average.g += sample.g;
				average.b += sample.b;
			}
			average.r /= samples.size();
			average.g /= samples.size();
			average.b /= samples.size();

			return average;
		}
	}

	class CPBrushToolOil extends CPBrushToolDirectBrush {

		void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab) {
			if (brushBuffer == null) {
				brushBuffer = new int[dab.width * dab.height];
				for (int i = brushBuffer.length - 1; --i >= 0;) {
					brushBuffer[i] = 0;
				}
				// curLayer.copyRect(dstRect, brushBuffer);
				oilAccumBuffer(srcRect, dstRect, brushBuffer, dab.width, 255);
			} else {
				oilResatBuffer(srcRect, dstRect, brushBuffer, dab.width, (int) ((curBrush.resat <= 0f) ? 0 : Math.max(
						1, (curBrush.resat * curBrush.resat) * 255)), curColor & 0xffffff);
				oilPasteBuffer(srcRect, dstRect, brushBuffer, dab.brush, dab.width, dab.alpha);
				oilAccumBuffer(srcRect, dstRect, brushBuffer, dab.width, (int) ((curBrush.bleed) * 255));
			}
			mergeOpacityBuffer(0, false);
			if (sampleAllLayers) {
				fusionLayers();
			}
		}

		private void oilAccumBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, int w, int alpha) {
			CPLayer layerToSample = sampleAllLayers ? fusion : getActiveLayer();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int color1 = layerToSample.data[dstOffset];
					int alpha1 = (color1 >>> 24) * alpha / 255;
					if (alpha1 <= 0) {
						continue;
					}

					int color2 = buffer[srcOffset];
					int alpha2 = (color2 >>> 24);

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = newAlpha << 24
								| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
										* invAlpha) / 255)) << 16
								| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
										* invAlpha) / 255)) << 8
								| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));

						buffer[srcOffset] = newColor;
					}
				}
			}
		}

		private void oilResatBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, int w, int alpha1, int color1) {
			if (alpha1 <= 0) {
				return;
			}

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int color2 = buffer[srcOffset];
					int alpha2 = (color2 >>> 24);

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = newAlpha << 24
								| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
										* invAlpha) / 255)) << 16
								| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
										* invAlpha) / 255)) << 8
								| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));

						buffer[srcOffset] = newColor;
					}
				}
			}
		}

		private void oilPasteBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, byte[] brush, int w, int alpha) {
			int[] opacityData = opacityBuffer.data;

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int color1 = buffer[srcOffset];
					int alpha1 = (color1 >>> 24) * (brush[srcOffset] & 0xff) * alpha / (255 * 255);
					if (alpha1 <= 0) {
						continue;
					}

					int color2 = curLayer.data[dstOffset];
					int alpha2 = (color2 >>> 24);

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = newAlpha << 24
								| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
										* invAlpha) / 255)) << 16
								| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
										* invAlpha) / 255)) << 8
								| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));

						opacityData[dstOffset] = newColor;

					}
				}
			}
		}
	}

	class CPBrushToolSmudge extends CPBrushToolDirectBrush {

		void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab) {
			if (brushBuffer == null) {
				brushBuffer = new int[dab.width * dab.height];
				smudgeAccumBuffer(srcRect, dstRect, brushBuffer, dab.width, 0);
			} else {
				smudgeAccumBuffer(srcRect, dstRect, brushBuffer, dab.width, dab.alpha);
				smudgePasteBuffer(srcRect, dstRect, brushBuffer, dab.brush, dab.width, dab.alpha);

				if (lockAlpha) {
					restoreAlpha(dstRect);
				}
			}
			opacityArea.makeEmpty();
			if (sampleAllLayers) {
				fusionLayers();
			}
		}

		public void mergeOpacityBuf(CPRect dstRect, int color) {
		}

		private void smudgeAccumBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, int w, int alpha) {

			CPLayer layerToSample = sampleAllLayers ? fusion : getActiveLayer();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int layerColor = layerToSample.data[dstOffset];
					int opacityAlpha = 255 - alpha;
					if (opacityAlpha > 0) {
						int destColor = buffer[srcOffset];

						int destAlpha = 255;
						int newLayerAlpha = opacityAlpha + destAlpha * (255 - opacityAlpha) / 255;
						int realAlpha = 255 * opacityAlpha / newLayerAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = (((layerColor >>> 24 & 0xff) * realAlpha + (destColor >>> 24 & 0xff) * invAlpha) / 255) << 24
								& 0xff000000
								| (((layerColor >>> 16 & 0xff) * realAlpha + (destColor >>> 16 & 0xff) * invAlpha) / 255) << 16
								& 0xff0000
								| (((layerColor >>> 8 & 0xff) * realAlpha + (destColor >>> 8 & 0xff) * invAlpha) / 255) << 8
								& 0xff00
								| (((layerColor & 0xff) * realAlpha + (destColor & 0xff) * invAlpha) / 255)
								& 0xff;

						if (newColor == destColor) {
							if ((layerColor & 0xff0000) > (destColor & 0xff0000)) {
								newColor += 1 << 16;
							} else if ((layerColor & 0xff0000) < (destColor & 0xff0000)) {
								newColor -= 1 << 16;
							}

							if ((layerColor & 0xff00) > (destColor & 0xff00)) {
								newColor += 1 << 8;
							} else if ((layerColor & 0xff00) < (destColor & 0xff00)) {
								newColor -= 1 << 8;
							}

							if ((layerColor & 0xff) > (destColor & 0xff)) {
								newColor += 1;
							} else if ((layerColor & 0xff) < (destColor & 0xff)) {
								newColor -= 1;
							}
						}

						buffer[srcOffset] = newColor;
					}
				}
			}

			if (srcRect.left > 0) {
				int fill = srcRect.left;
				for (int j = srcRect.top; j < srcRect.bottom; j++) {
					int offset = j * w;
					int fillColor = buffer[offset + srcRect.left];
					for (int i = 0; i < fill; i++) {
						buffer[offset++] = fillColor;
					}
				}
			}

			if (srcRect.right < w) {
				int fill = w - srcRect.right;
				for (int j = srcRect.top; j < srcRect.bottom; j++) {
					int offset = j * w + srcRect.right;
					int fillColor = buffer[offset - 1];
					for (int i = 0; i < fill; i++) {
						buffer[offset++] = fillColor;
					}
				}
			}

			for (int j = 0; j < srcRect.top; j++) {
				System.arraycopy(buffer, srcRect.top * w, buffer, j * w, w);
			}

			for (int j = srcRect.bottom; j < w; j++) {
				System.arraycopy(buffer, (srcRect.bottom - 1) * w, buffer, j * w, w);
			}
		}

		private void smudgePasteBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, byte[] brush, int w, int alpha) {
			int[] undoData = undoBuffer.data;

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * width;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int bufferColor = buffer[srcOffset];
					int opacityAlpha = (bufferColor >>> 24) * (brush[srcOffset] & 0xff) / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];

						int realAlpha = 255;
						int invAlpha = 255 - realAlpha;

						int newColor = (((bufferColor >>> 24 & 0xff) * realAlpha + (destColor >>> 24 & 0xff) * invAlpha) / 255) << 24
								& 0xff000000
								| (((bufferColor >>> 16 & 0xff) * realAlpha + (destColor >>> 16 & 0xff) * invAlpha) / 255) << 16
								& 0xff0000
								| (((bufferColor >>> 8 & 0xff) * realAlpha + (destColor >>> 8 & 0xff) * invAlpha) / 255) << 8
								& 0xff00
								| (((bufferColor & 0xff) * realAlpha + (destColor & 0xff) * invAlpha) / 255)
								& 0xff;

						curLayer.data[dstOffset] = newColor;
					}
				}
			}
		}
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	// Layer methods
	// ///////////////////////////////////////////////////////////////////////////////////

	public void setActiveLayer(int i) {
		if (i < 0 || i >= layers.size()) {
			return;
		}

		activeLayer = i;
		curLayer = layers.get(i);
		callListenersLayerChange();
	}

	public int getActiveLayerNb() {
		return activeLayer;
	}

	public CPLayer getActiveLayer() {
		return curLayer;
	}

	public CPLayer getLayer(int i) {
		if (i < 0 || i >= layers.size()) {
			return null;
		}

		return layers.get(i);
	}

	//
	// Undo / Redo
	//

	public void undo() {
		if (!canUndo()) {
			return;
		}

		CPUndo undo = undoList.removeFirst();
		undo.undo();
		redoList.addFirst(undo);
	}

	public void redo() {
		if (!canRedo()) {
			return;
		}

		CPUndo redo = redoList.removeFirst();
		redo.redo();
		undoList.addFirst(redo);
	}

	public boolean canUndo() {
		return !undoList.isEmpty();
	}

	public boolean canRedo() {
		return !redoList.isEmpty();
	}

	private void addUndo(CPUndo undo) {
		if (undoList.isEmpty() || !(undoList.getFirst()).merge(undo)) {
			if (undoList.size() >= maxUndo) {
				undoList.removeLast();
			}
			undoList.addFirst(undo);
		} else {
			// Two merged changes can mean no change at all
			// don't leave a useless undo in the list
			if ((undoList.getFirst()).noChange()) {
				undoList.removeFirst();
			}
		}
		if (!redoList.isEmpty()) {
			redoList = new LinkedList();
		}
	}

	public void clearHistory() {
		undoList = new LinkedList();
		redoList = new LinkedList();

		Runtime r = Runtime.getRuntime();
		r.gc();
	}

	//
	//
	//

	public int colorPicker(float x, float y) {
		// not really necessary and could potentially the repaint
		// of the canvas to miss that area
		// fusionLayers();

		return fusion.getPixel((int) x, (int) y) & 0xffffff;
	}

	public boolean isPointWithin(float x, float y) {
		return x >= 0 && y >= 0 && (int) x < width && (int) y < height;
	}

	// FIXME: 2007-01-13 I'm moving this to the CPRect class
	// find where this version is used and change the
	// code to use the CPRect version

	public void clipSourceDest(CPRect srcRect, CPRect dstRect) {
		// FIXME:
		// /!\ dstRect bottom and right are ignored and instead we clip
		// against the width, height of the layer. :/
		//

		// this version would be enough in most cases (when we don't need
		// srcRect bottom and right to be clipped)
		// it's left here in case it's needed to make a faster version
		// of this function
		// dstRect.right = Math.min(width, dstRect.left + srcRect.getWidth());
		// dstRect.bottom = Math.min(height, dstRect.top + srcRect.getHeight());

		// new dest bottom/right
		dstRect.right = dstRect.left + srcRect.getWidth();
		if (dstRect.right > width) {
			srcRect.right -= dstRect.right - width;
			dstRect.right = width;
		}

		dstRect.bottom = dstRect.top + srcRect.getHeight();
		if (dstRect.bottom > height) {
			srcRect.bottom -= dstRect.bottom - height;
			dstRect.bottom = height;
		}

		// new src top/left
		if (dstRect.left < 0) {
			srcRect.left -= dstRect.left;
			dstRect.left = 0;
		}

		if (dstRect.top < 0) {
			srcRect.top -= dstRect.top;
			dstRect.top = 0;
		}
	}

	public Object[] getLayers() {
		return layers.toArray();
	}

	public int getLayersNb() {
		return layers.size();
	}

	public CPRect getSize() {
		return new CPRect(width, height);
	}

	//
	// Selection methods
	//

	// Gets the current selection rect or a rectangle covering the whole canvas if there are no selections
	public CPRect getSelectionAutoSelect() {
		CPRect r;

		if (!curSelection.isEmpty()) {
			r = (CPRect) curSelection.clone();
		} else {
			r = getSize();
		}

		return r;
	}

	// Gets the current selection rect
	public CPRect getSelection() {
		return (CPRect) curSelection.clone();
	}

	void setSelection(CPRect r) {
		curSelection.set(r);
		curSelection.clip(getSize());
	}

	void emptySelection() {
		curSelection.makeEmpty();
	}

	//
	//
	//

	public void invalidateFusion(CPRect r) {
		fusionArea.union(r);
		callListenersUpdateRegion(r);
	}

	public void invalidateFusion() {
		invalidateFusion(new CPRect(0, 0, width, height));
	}

	public void setLayerVisibility(int layer, boolean visible) {
		addUndo(new CPUndoLayerVisible(layer, getLayer(layer).visible, visible));
		getLayer(layer).visible = visible;
		invalidateFusion();
		callListenersLayerChange();
	}

	public void addLayer() {
		addUndo(new CPUndoAddLayer(activeLayer));

		CPLayer newLayer = new CPLayer(width, height);
		newLayer.name = getDefaultLayerName();
		layers.add(activeLayer + 1, newLayer);
		setActiveLayer(activeLayer + 1);

		invalidateFusion();
		callListenersLayerChange();
	}

	public void removeLayer() {
		if (layers.size() > 1) {
			addUndo(new CPUndoRemoveLayer(activeLayer, curLayer));
			layers.remove(activeLayer);
			setActiveLayer(activeLayer < layers.size() ? activeLayer : activeLayer - 1);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void duplicateLayer() {
		String copySuffix = " Copy";

		addUndo(new CPUndoDuplicateLayer(activeLayer));
		CPLayer newLayer = new CPLayer(width, height);
		newLayer.copyFrom(layers.elementAt(activeLayer));
		if (!newLayer.name.endsWith(copySuffix)) {
			newLayer.name += copySuffix;
		}
		layers.add(activeLayer + 1, newLayer);

		setActiveLayer(activeLayer + 1);
		invalidateFusion();
		callListenersLayerChange();
	}

	public void mergeDown(boolean createUndo) {
		if (layers.size() > 0 && activeLayer > 0) {
			if (createUndo) {
				addUndo(new CPUndoMergeDownLayer(activeLayer));
			}

			layers.elementAt(activeLayer).fusionWithFullAlpha(layers.elementAt(activeLayer - 1),
					new CPRect(width, height));
			layers.remove(activeLayer);
			setActiveLayer(activeLayer - 1);

			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void mergeAllLayers(boolean createUndo) {
		if (layers.size() > 1) {
			if (createUndo) {
				addUndo(new CPUndoMergeAllLayers());
			}

			fusionLayers();
			layers.clear();

			CPLayer layer = new CPLayer(width, height);
			layer.name = getDefaultLayerName();
			layer.copyDataFrom(fusion);
			layers.add(layer);
			setActiveLayer(0);

			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void moveLayer(int from, int to) {
		if (from < 0 || from >= getLayersNb() || to < 0 || to > getLayersNb() || from == to) {
			return;
		}
		addUndo(new CPUndoMoveLayer(from, to));
		moveLayerReal(from, to);
	}

	private void moveLayerReal(int from, int to) {
		CPLayer layer = layers.remove(from);
		if (to <= from) {
			layers.add(to, layer);
			setActiveLayer(to);
		} else {
			layers.add(to - 1, layer);
			setActiveLayer(to - 1);
		}

		invalidateFusion();
		callListenersLayerChange();
	}

	public void setLayerAlpha(int layer, int alpha) {
		if (getLayer(layer).getAlpha() != alpha) {
			addUndo(new CPUndoLayerAlpha(layer, alpha));
			getLayer(layer).setAlpha(alpha);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void setBlendMode(int layer, int blendMode) {
		if (getLayer(layer).getBlendMode() != blendMode) {
			addUndo(new CPUndoLayerMode(layer, blendMode));
			getLayer(layer).setBlendMode(blendMode);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void setLayerName(int layer, String name) {
		if (getLayer(layer).name != name) {
			addUndo(new CPUndoLayerRename(layer, name));
			getLayer(layer).name = name;
			callListenersLayerChange();
		}
	}

	public void floodFill(float x, float y) {
		undoBuffer.copyFrom(curLayer);
		undoArea = new CPRect(width, height);

		curLayer.floodFill((int) x, (int) y, curColor | 0xff000000);

		addUndo(new CPUndoPaint());
		invalidateFusion();
	}

	public void fill(int color) {
		CPRect r = getSelectionAutoSelect();

		undoBuffer.copyFrom(curLayer);
		undoArea = r;

		curLayer.clear(r, color);

		addUndo(new CPUndoPaint());
		invalidateFusion();
	}

	public void clear() {
		fill(0xffffff);
	}

	public void hFlip() {
		CPRect r = getSelectionAutoSelect();

		undoBuffer.copyFrom(curLayer);
		undoArea = r;

		curLayer.copyRegionHFlip(r, undoBuffer);

		addUndo(new CPUndoPaint());
		invalidateFusion();
	}

	public void vFlip() {
		CPRect r = getSelectionAutoSelect();

		undoBuffer.copyFrom(curLayer);
		undoArea = r;

		curLayer.copyRegionVFlip(r, undoBuffer);

		addUndo(new CPUndoPaint());
		invalidateFusion();
	}

	public void monochromaticNoise() {
		CPRect r = getSelectionAutoSelect();

		undoBuffer.copyFrom(curLayer);
		undoArea = r;

		curLayer.fillWithNoise(r);

		addUndo(new CPUndoPaint());
		invalidateFusion();
	}

	public void colorNoise() {
		CPRect r = getSelectionAutoSelect();

		undoBuffer.copyFrom(curLayer);
		undoArea = r;

		curLayer.fillWithColorNoise(r);

		addUndo(new CPUndoPaint());
		invalidateFusion();
	}

	public void boxBlur(int radiusX, int radiusY, int iterations) {
		CPRect r = getSelectionAutoSelect();

		undoBuffer.copyFrom(curLayer);
		undoArea = r;

		for (int c = 0; c < iterations; c++) {
			curLayer.boxBlur(r, radiusX, radiusY);
		}

		addUndo(new CPUndoPaint());
		invalidateFusion();
	}

	public void invert() {
		CPRect r = getSelectionAutoSelect();

		undoBuffer.copyFrom(curLayer);
		undoArea = r;

		curLayer.invert(r);

		addUndo(new CPUndoPaint());
		invalidateFusion();
	}

	public void rectangleSelection(CPRect r) {
		CPRect newSelection = (CPRect) r.clone();
		newSelection.clip(getSize());

		addUndo(new CPUndoRectangleSelection(getSelection(), newSelection));

		setSelection(newSelection);
	}

	public void beginPreviewMode(boolean copy) {
		// !!!! awful awful hack !!! will break as soon as CPMultiUndo is used for other things
		// FIXME: ASAP!
		if (!copy && !undoList.isEmpty() && redoList.isEmpty() && (undoList.getFirst() instanceof CPMultiUndo)
				&& (((CPMultiUndo) undoList.getFirst()).undoes[0] instanceof CPUndoPaint)
				&& ((CPUndoPaint) ((CPMultiUndo) undoList.getFirst()).undoes[0]).layer == getActiveLayerNb()) {
			undo();
			copy = prevModeCopy;
		} else {
			movePrevX = 0;
			movePrevY = 0;

			undoBuffer.copyFrom(curLayer);
			undoArea.makeEmpty();

			opacityBuffer.clear();
			opacityArea.makeEmpty();
		}

		moveInitSelect = null;
		moveModeCopy = copy;
	}

	public void endPreviewMode() {
		CPUndo undo = new CPUndoPaint();
		if (moveInitSelect != null) {
			CPUndo[] undoArray = { undo, new CPUndoRectangleSelection(moveInitSelect, getSelection()) };
			undo = new CPMultiUndo(undoArray);
		} else {
			// !!!!!!
			// FIXME: this is required just to make the awful move hack work
			CPUndo[] undoArray = { undo };
			undo = new CPMultiUndo(undoArray);
		}
		addUndo(undo);

		moveInitSelect = null;
		movePrevX = movePrevX2;
		movePrevY = movePrevY2;
		prevModeCopy = moveModeCopy;
	}

	// temp awful hack
	CPRect moveInitSelect = null;
	int movePrevX, movePrevY, movePrevX2, movePrevY2;
	boolean moveModeCopy, prevModeCopy;

	public void move(int offsetX, int offsetY) {
		CPRect srcRect;

		offsetX += movePrevX;
		offsetY += movePrevY;

		if (moveInitSelect == null) {
			srcRect = getSelectionAutoSelect();
			if (!getSelection().isEmpty()) {
				moveInitSelect = getSelection();
			}
		} else {
			srcRect = (CPRect) moveInitSelect.clone();
		}
		curLayer.copyFrom(undoBuffer);

		if (!moveModeCopy) {
			curLayer.clear(srcRect, 0);
		}

		curLayer.pasteAlphaRect(undoBuffer, srcRect, srcRect.left + offsetX, srcRect.top + offsetY);

		undoArea = new CPRect();
		if (!moveModeCopy) {
			undoArea.union(srcRect);
		}
		srcRect.translate(offsetX, offsetY);
		undoArea.union(srcRect);

		invalidateFusion();

		if (moveInitSelect != null) {
			CPRect sel = (CPRect) moveInitSelect.clone();
			sel.translate(offsetX, offsetY);
			setSelection(sel);
		}

		// this is a really bad idea :D
		movePrevX2 = offsetX;
		movePrevY2 = offsetY;
	}

	// ////
	// Copy/Paste

	public void cutSelection(boolean createUndo) {
		CPRect sel = getSelection();
		if (sel.isEmpty()) {
			return;
		}

		clipboard = new CPClip(new CPColorBmp(curLayer, sel), sel.left, sel.top);

		if (createUndo) {
			addUndo(new CPUndoCut(clipboard.bmp, sel.left, sel.top, getActiveLayerNb(), sel));
		}

		curLayer.clear(sel, 0);
		invalidateFusion();
	}

	public void copySelection() {
		CPRect sel = getSelection();
		if (sel.isEmpty()) {
			return;
		}

		clipboard = new CPClip(new CPColorBmp(curLayer, sel), sel.left, sel.top);
	}

	public void copySelectionMerged() {
		CPRect sel = getSelection();
		if (sel.isEmpty()) {
			return;
		}

		// make sure the fusioned picture is up to date
		fusionLayers();
		clipboard = new CPClip(new CPColorBmp(fusion, sel), sel.left, sel.top);
	}

	public void pasteClipboard(boolean createUndo) {
		if (clipboard != null) {
			pasteClip(createUndo, clipboard);
		}
	}

	public void pasteClip(boolean createUndo, CPClip clip) {
		if (createUndo) {
			addUndo(new CPUndoPaste(clip, getActiveLayerNb(), getSelection()));
		}

		// FIXME: redundant code, should use AddLayer's code??
		CPLayer newLayer = new CPLayer(width, height);
		newLayer.name = getDefaultLayerName();
		layers.add(activeLayer + 1, newLayer);
		setActiveLayer(activeLayer + 1);

		CPRect r = clip.bmp.getSize();
		int x, y;
		if (r.isInside(getSize())) {
			x = clip.x;
			y = clip.y;
		} else {
			x = (width - clip.bmp.width) / 2;
			y = (height - clip.bmp.height) / 2;
		}

		curLayer.pasteBitmap(clip.bmp, x, y);
		emptySelection();

		invalidateFusion();
		callListenersLayerChange();
	}

	// ////////////////////////////////////////////////////
	// Miscellaneous functions

	public String getDefaultLayerName() {
		String prefix = "Layer ";
		int highestLayerNb = 0;
		for (CPLayer l : layers) {
			if (l.name.matches("^" + prefix + "[0-9]+$")) {
				highestLayerNb = Math.max(highestLayerNb, Integer.parseInt(l.name.substring(prefix.length())));
			}
		}
		return prefix + (highestLayerNb + 1);
	}

	public boolean hasAlpha() {
		return fusion.hasAlpha();
	}

	// ////////////////////////////////////////////////////
	// Undo classes

	class CPUndoPaint extends CPUndo {

		int layer;
		CPRect rect;
		int[] data;

		public CPUndoPaint() {
			layer = getActiveLayerNb();
			rect = new CPRect(undoArea);

			data = undoBuffer.copyRectXOR(curLayer, rect);
			undoArea.makeEmpty();
		}

		public void undo() {
			getLayer(layer).setRectXOR(data, rect);
			invalidateFusion(rect);
		}

		public void redo() {
			getLayer(layer).setRectXOR(data, rect);
			invalidateFusion(rect);
		}

		public long getMemoryUsed(boolean undone, Object param) {
			return data.length * 4;
		}
	}

	class CPUndoLayerVisible extends CPUndo {

		int layer;
		boolean oldVis, newVis;

		public CPUndoLayerVisible(int layer, boolean oldVis, boolean newVis) {
			this.layer = layer;
			this.oldVis = oldVis;
			this.newVis = newVis;
		}

		public void redo() {
			getLayer(layer).visible = newVis;
			invalidateFusion();
			callListenersLayerChange();
		}

		public void undo() {
			getLayer(layer).visible = oldVis;
			invalidateFusion();
			callListenersLayerChange();
		}

		public boolean merge(CPUndo u) {
			if (u instanceof CPUndoLayerVisible && layer == ((CPUndoLayerVisible) u).layer) {
				newVis = ((CPUndoLayerVisible) u).newVis;
				return true;
			}
			return false;
		}

		public boolean noChange() {
			return oldVis == newVis;
		}
	}

	class CPUndoAddLayer extends CPUndo {

		int layer;

		public CPUndoAddLayer(int layer) {
			this.layer = layer;
		}

		public void undo() {
			layers.remove(layer + 1);
			setActiveLayer(layer);
			invalidateFusion();
			callListenersLayerChange();
		}

		public void redo() {
			CPLayer newLayer = new CPLayer(width, height);
			newLayer.name = getDefaultLayerName();
			layers.add(layer + 1, newLayer);

			setActiveLayer(layer + 1);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

	class CPUndoDuplicateLayer extends CPUndo {

		int layer;

		public CPUndoDuplicateLayer(int layer) {
			this.layer = layer;
		}

		public void undo() {
			layers.remove(layer + 1);
			setActiveLayer(layer);
			invalidateFusion();
			callListenersLayerChange();
		}

		public void redo() {
			String copySuffix = " Copy";

			CPLayer newLayer = new CPLayer(width, height);
			newLayer.copyFrom(layers.elementAt(layer));
			if (!newLayer.name.endsWith(copySuffix)) {
				newLayer.name += copySuffix;
			}
			layers.add(layer + 1, newLayer);

			setActiveLayer(layer + 1);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

	class CPUndoRemoveLayer extends CPUndo {

		int layer;
		CPLayer layerObj;

		public CPUndoRemoveLayer(int layer, CPLayer layerObj) {
			this.layer = layer;
			this.layerObj = layerObj;
		}

		public void undo() {
			layers.add(layer, layerObj);
			setActiveLayer(layer);
			invalidateFusion();
			callListenersLayerChange();
		}

		public void redo() {
			layers.remove(layer);
			setActiveLayer(layer < layers.size() ? layer : layer - 1);
			invalidateFusion();
			callListenersLayerChange();
		}

		public long getMemoryUsed(boolean undone, Object param) {
			return undone ? 0 : width * height * 4;
		}

	}

	class CPUndoMergeDownLayer extends CPUndo {

		int layer;
		CPLayer layerBottom, layerTop;

		public CPUndoMergeDownLayer(int layer) {
			this.layer = layer;
			layerBottom = new CPLayer(width, height);
			layerBottom.copyFrom(layers.elementAt(layer - 1));
			layerTop = layers.elementAt(layer);
		}

		public void undo() {
			layers.elementAt(layer - 1).copyFrom(layerBottom);
			layers.add(layer, layerTop);
			setActiveLayer(layer);

			layerBottom = layerTop = null;

			invalidateFusion();
			callListenersLayerChange();
		}

		public void redo() {
			layerBottom = new CPLayer(width, height);
			layerBottom.copyFrom(layers.elementAt(layer - 1));
			layerTop = layers.elementAt(layer);

			setActiveLayer(layer);
			mergeDown(false);
		}

		public long getMemoryUsed(boolean undone, Object param) {
			return undone ? 0 : width * height * 4 * 2;
		}
	}

	class CPUndoMergeAllLayers extends CPUndo {

		Vector<CPLayer> oldLayers;
		int oldActiveLayer;

		public CPUndoMergeAllLayers() {
			oldLayers = (Vector<CPLayer>) layers.clone();
			oldActiveLayer = getActiveLayerNb();
		}

		public void undo() {
			layers = (Vector<CPLayer>) oldLayers.clone();
			setActiveLayer(oldActiveLayer);

			invalidateFusion();
			callListenersLayerChange();
		}

		public void redo() {
			mergeAllLayers(false);
		}

		public long getMemoryUsed(boolean undone, Object param) {
			return undone ? 0 : oldLayers.size() * width * height * 4;
		}
	}

	class CPUndoMoveLayer extends CPUndo {

		int from, to;

		public CPUndoMoveLayer(int from, int to) {
			this.from = from;
			this.to = to;
		}

		public void undo() {
			if (to <= from) {
				moveLayerReal(to, from + 1);
			} else {
				moveLayerReal(to - 1, from);
			}
		}

		public void redo() {
			moveLayerReal(from, to);
		}
	}

	class CPUndoLayerAlpha extends CPUndo {

		int layer;
		int from, to;

		public CPUndoLayerAlpha(int layer, int alpha) {
			this.from = getLayer(layer).getAlpha();
			this.to = alpha;
			this.layer = layer;
		}

		public void undo() {
			getLayer(layer).setAlpha(from);
			invalidateFusion();
			callListenersLayerChange();
		}

		public void redo() {
			getLayer(layer).setAlpha(to);
			invalidateFusion();
			callListenersLayerChange();
		}

		public boolean merge(CPUndo u) {
			if (u instanceof CPUndoLayerAlpha && layer == ((CPUndoLayerAlpha) u).layer) {
				to = ((CPUndoLayerAlpha) u).to;
				return true;
			}
			return false;
		}

		public boolean noChange() {
			return from == to;
		}
	}

	class CPUndoLayerMode extends CPUndo {

		int layer;
		int from, to;

		public CPUndoLayerMode(int layer, int mode) {
			this.from = getLayer(layer).getBlendMode();
			this.to = mode;
			this.layer = layer;
		}

		public void undo() {
			getLayer(layer).setBlendMode(from);
			invalidateFusion();
			callListenersLayerChange();
		}

		public void redo() {
			getLayer(layer).setBlendMode(to);
			invalidateFusion();
			callListenersLayerChange();
		}

		public boolean merge(CPUndo u) {
			if (u instanceof CPUndoLayerMode && layer == ((CPUndoLayerMode) u).layer) {
				to = ((CPUndoLayerMode) u).to;
				return true;
			}
			return false;
		}

		public boolean noChange() {
			return from == to;
		}
	}

	class CPUndoLayerRename extends CPUndo {

		int layer;
		String from, to;

		public CPUndoLayerRename(int layer, String name) {
			this.from = getLayer(layer).name;
			this.to = name;
			this.layer = layer;
		}

		public void undo() {
			getLayer(layer).name = from;
			callListenersLayerChange();
		}

		public void redo() {
			getLayer(layer).name = to;
			callListenersLayerChange();
		}

		public boolean merge(CPUndo u) {
			if (u instanceof CPUndoLayerRename && layer == ((CPUndoLayerRename) u).layer) {
				to = ((CPUndoLayerRename) u).to;
				return true;
			}
			return false;
		}

		public boolean noChange() {
			return from.equals(to);
		}
	}

	class CPUndoRectangleSelection extends CPUndo {

		CPRect from, to;

		public CPUndoRectangleSelection(CPRect from, CPRect to) {
			this.from = (CPRect) from.clone();
			this.to = (CPRect) to.clone();
		}

		public void undo() {
			setSelection(from);
		}

		public void redo() {
			setSelection(to);
		}

		public boolean merge(CPUndo u) {
			return false;
		}

		public boolean noChange() {
			return from.equals(to);
		}
	}

	// used to encapsulate multiple undo operation as one
	class CPMultiUndo extends CPUndo {

		CPUndo[] undoes;

		public CPMultiUndo(CPUndo[] undoes) {
			this.undoes = undoes;
		}

		public void undo() {
			for (int i = undoes.length - 1; i >= 0; i--) {
				undoes[i].undo();
			}
		}

		public void redo() {
			for (int i = 0; i < undoes.length; i++) {
				undoes[i].redo();
			}
		}

		public boolean merge(CPUndo u) {
			return false;
		}

		public boolean noChange() {
			boolean noChange = true;
			for (int i = 0; i < undoes.length; i++) {
				noChange = noChange && undoes[i].noChange();
			}
			return noChange;
		}

		public long getMemoryUsed(boolean undone, Object param) {
			long total = 0;
			for (CPUndo undo : undoes) {
				total += undo.getMemoryUsed(undone, param);
			}
			return total;
		}
	}

	class CPUndoCut extends CPUndo {

		CPColorBmp bmp;
		int x, y, layer;
		CPRect selection;

		public CPUndoCut(CPColorBmp bmp, int x, int y, int layerNb, CPRect selection) {
			this.bmp = bmp;
			this.x = x;
			this.y = y;
			this.layer = layerNb;
			this.selection = (CPRect) selection.clone();
		}

		public void undo() {
			setActiveLayer(layer);
			curLayer.pasteBitmap(clipboard.bmp, x, y);
			setSelection(selection);
			invalidateFusion();
		}

		public void redo() {
			setActiveLayer(layer);
			CPRect r = bmp.getSize();
			r.translate(x, y);
			curLayer.clear(r, 0);
			emptySelection();
			invalidateFusion();
		}

		public long getMemoryUsed(boolean undone, Object param) {
			return bmp == param ? 0 : bmp.width * bmp.height * 4;
		}
	}

	class CPUndoPaste extends CPUndo {

		CPClip clip;
		int layer;
		CPRect selection;

		public CPUndoPaste(CPClip clip, int layerNb, CPRect selection) {
			this.clip = clip;
			this.layer = layerNb;
			this.selection = (CPRect) selection.clone();
		}

		public void undo() {
			layers.remove(layer + 1);
			setActiveLayer(layer);
			setSelection(selection);

			invalidateFusion();
			callListenersLayerChange();
		}

		public void redo() {
			setActiveLayer(layer);
			pasteClip(false, clip);
		}

		public long getMemoryUsed(boolean undone, Object param) {
			return clip.bmp == param ? 0 : clip.bmp.width * clip.bmp.height * 4;
		}

	}

}
