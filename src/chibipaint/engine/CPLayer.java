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

import chibipaint.util.*;

public class CPLayer extends CPColorBmp {

	public int blendMode = LM_NORMAL, alpha = 100;
	public String name;
	public boolean visible = true;

	public final static int LM_NORMAL = 0;
	public final static int LM_MULTIPLY = 1;
	public final static int LM_ADD = 2;
	public final static int LM_SCREEN = 3;
	public final static int LM_LIGHTEN = 4;
	public final static int LM_DARKEN = 5;
	public final static int LM_SUBTRACT = 6;
	public final static int LM_DODGE = 7;
	public final static int LM_BURN = 8;
	public final static int LM_OVERLAY = 9;
	public final static int LM_HARDLIGHT = 10;
	public final static int LM_SOFTLIGHT = 11;
	public final static int LM_VIVIDLIGHT = 12;
	public final static int LM_LINEARLIGHT = 13;
	public final static int LM_PINLIGHT = 14;

	//
	// Look-Up tables for some of the blend modes
	//

	private static int softLightLUTSquare[];
	private static int softLightLUTSquareRoot[];

	public CPLayer(int width, int height) {
		super(width, height);

		name = new String("");
		clear(0xffffff);

		if (softLightLUTSquare == null) {
			makeLookUpTables();
		}
	}

	public void makeLookUpTables() {

		// V - V^2 table
		softLightLUTSquare = new int[256];
		for (int i = 0; i < 256; i++) {
			double v = i / 255.;
			softLightLUTSquare[i] = (int) ((v - v * v) * 255.);
		}

		// sqrt(V) - V table
		softLightLUTSquareRoot = new int[256];
		for (int i = 0; i < 256; i++) {
			double v = i / 255.;
			softLightLUTSquareRoot[i] = (int) ((Math.sqrt(v) - v) * 255.);
		}
	}

	public void clear() {
		clear(0);
	}

	public void clear(int color) {
		for (int i = 0; i < width * height; i++) {
			data[i] = color;
		}
	}

	public void clear(CPRect r, int color) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);
		for (int j = rect.top; j < rect.bottom; j++) {
			for (int i = rect.left; i < rect.right; i++) {
				data[i + j * width] = color;
			}
		}
	}

	public int[] getData() {
		return data;
	}

	public int getAlpha() {
		return alpha;
	}

	public int getBlendMode() {
		return blendMode;
	}

	public void copyFrom(CPLayer l) {
		name = l.name;
		blendMode = l.blendMode;
		alpha = l.alpha;
		visible = l.visible;

		copyDataFrom(l);

	}

	// Layer blending functions
	//
	// The FullAlpha versions are the ones that work in all cases
	// others need the bottom layer to be 100% opaque but are faster

	public void fusionWith(CPLayer fusion, CPRect r) {
		if (alpha <= 0) {
			return;
		}

		switch (blendMode) {
		case LM_NORMAL:
			if (alpha >= 100) {
				fusionWithNormalNoAlpha(fusion, r);
			} else {
				fusionWithNormal(fusion, r);
			}
			break;

		case LM_MULTIPLY:
			fusionWithMultiply(fusion, r);
			break;

		case LM_ADD:
			fusionWithAdd(fusion, r);
			break;

		case LM_SCREEN:
			fusionWithScreenFullAlpha(fusion, r);
			break;

		case LM_LIGHTEN:
			fusionWithLightenFullAlpha(fusion, r);
			break;

		case LM_DARKEN:
			fusionWithDarkenFullAlpha(fusion, r);
			break;

		case LM_SUBTRACT:
			fusionWithSubtractFullAlpha(fusion, r);
			break;

		case LM_DODGE:
			fusionWithDodgeFullAlpha(fusion, r);
			break;

		case LM_BURN:
			fusionWithBurnFullAlpha(fusion, r);
			break;

		case LM_OVERLAY:
			fusionWithOverlayFullAlpha(fusion, r);
			break;

		case LM_HARDLIGHT:
			fusionWithHardLightFullAlpha(fusion, r);
			break;

		case LM_SOFTLIGHT:
			fusionWithSoftLightFullAlpha(fusion, r);
			break;

		case LM_VIVIDLIGHT:
			fusionWithVividLightFullAlpha(fusion, r);
			break;

		case LM_LINEARLIGHT:
			fusionWithLinearLightFullAlpha(fusion, r);
			break;

		case LM_PINLIGHT:
			fusionWithPinLightFullAlpha(fusion, r);
			break;
		}
	}

	public void fusionWithFullAlpha(CPLayer fusion, CPRect r) {
		if (alpha <= 0) {
			return;
		}

		switch (blendMode) {
		case LM_NORMAL:
			fusionWithNormalFullAlpha(fusion, r);
			break;

		case LM_MULTIPLY:
			fusionWithMultiplyFullAlpha(fusion, r);
			break;

		case LM_ADD:
			fusionWithAddFullAlpha(fusion, r);
			break;

		case LM_SCREEN:
			fusionWithScreenFullAlpha(fusion, r);
			break;

		case LM_LIGHTEN:
			fusionWithLightenFullAlpha(fusion, r);
			break;

		case LM_DARKEN:
			fusionWithDarkenFullAlpha(fusion, r);
			break;

		case LM_SUBTRACT:
			fusionWithSubtractFullAlpha(fusion, r);
			break;

		case LM_DODGE:
			fusionWithDodgeFullAlpha(fusion, r);
			break;

		case LM_BURN:
			fusionWithBurnFullAlpha(fusion, r);
			break;

		case LM_OVERLAY:
			fusionWithOverlayFullAlpha(fusion, r);
			break;

		case LM_HARDLIGHT:
			fusionWithHardLightFullAlpha(fusion, r);
			break;

		case LM_SOFTLIGHT:
			fusionWithSoftLightFullAlpha(fusion, r);
			break;

		case LM_VIVIDLIGHT:
			fusionWithVividLightFullAlpha(fusion, r);
			break;

		case LM_LINEARLIGHT:
			fusionWithLinearLightFullAlpha(fusion, r);
			break;

		case LM_PINLIGHT:
			fusionWithPinLightFullAlpha(fusion, r);
			break;
		}
	}

	public void fusionWithMultiply(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha = (color1 >>> 24) * this.alpha / 100;
				if (alpha == 0) {
					continue;
				} else {
					int color2 = fusion.data[off];
					color1 = color1 ^ 0xffffffff;
					fusion.data[off] = 0xff000000
							| ((color2 >>> 16 & 0xff) - (color1 >>> 16 & 0xff) * (color2 >>> 16 & 0xff) * alpha
									/ (255 * 255)) << 16
							| ((color2 >>> 8 & 0xff) - (color1 >>> 8 & 0xff) * (color2 >>> 8 & 0xff) * alpha
									/ (255 * 255)) << 8
							| ((color2 & 0xff) - (color1 & 0xff) * (color2 & 0xff) * alpha / (255 * 255));
				}
			}
		}
	}

	public void fusionWithNormal(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha = (color1 >>> 24) * this.alpha / 100;
				if (alpha == 0) {
					continue;
				} else if (alpha == 255) {
					fusion.data[off] = color1;
				} else {
					int color2 = fusion.data[off];

					int invAlpha = 255 - alpha;
					fusion.data[off] = 0xff000000
							| (((color1 >>> 16 & 0xff) * alpha + (color2 >>> 16 & 0xff) * invAlpha) / 255) << 16
							| (((color1 >>> 8 & 0xff) * alpha + (color2 >>> 8 & 0xff) * invAlpha) / 255) << 8
							| (((color1 & 0xff) * alpha + (color2 & 0xff) * invAlpha) / 255);
				}
			}
		}
	}

	public void fusionWithNormalNoAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha = color1 >>> 24;
				if (alpha == 0) {
					continue;
				} else if (alpha == 255) {
					fusion.data[off] = color1;
				} else {
					int color2 = fusion.data[off];

					int invAlpha = 255 - alpha;
					fusion.data[off] = 0xff000000
							| (((color1 >>> 16 & 0xff) * alpha + (color2 >>> 16 & 0xff) * invAlpha) / 255) << 16
							| (((color1 >>> 8 & 0xff) * alpha + (color2 >>> 8 & 0xff) * invAlpha) / 255) << 8
							| (((color1 & 0xff) * alpha + (color2 & 0xff) * invAlpha) / 255);
				}
			}
		}
	}

	public void fusionWithAdd(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha = (color1 >>> 24) * this.alpha / 100;
				if (alpha == 0) {
					continue;
				} else {
					int color2 = fusion.data[off];

					int r = Math.min(255, (color2 >>> 16 & 0xff) + alpha * (color1 >>> 16 & 0xff) / 255);
					int g = Math.min(255, (color2 >>> 8 & 0xff) + alpha * (color1 >>> 8 & 0xff) / 255);
					int b = Math.min(255, (color2 & 0xff) + alpha * (color1 & 0xff) / 255);

					fusion.data[off] = 0xff000000 | r << 16 | g << 8 | b;
				}
			}
		}
	}

	// Normal Alpha Mode
	// C = A*d + B*(1-d) and d = aa / (aa + ab - aa*ab)

	public void fusionWithNormalFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;
				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int realAlpha = alpha1 * 255 / newAlpha;
					int invAlpha = 255 - realAlpha;

					fusion.data[off] = newAlpha << 24
							| (((color1 >>> 16 & 0xff) * realAlpha + (color2 >>> 16 & 0xff) * invAlpha) / 255) << 16
							| (((color1 >>> 8 & 0xff) * realAlpha + (color2 >>> 8 & 0xff) * invAlpha) / 255) << 8
							| (((color1 & 0xff) * realAlpha + (color2 & 0xff) * invAlpha) / 255);
				}
			}
		}
		fusion.alpha = 100;
	}

	// Multiply Mode
	// C = (A*aa*(1-ab) + B*ab*(1-aa) + A*B*aa*ab) / (aa + ab - aa*ab)

	public void fusionWithMultiplyFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;
				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;

					fusion.data[off] = newAlpha << 24
							| (((color1 >>> 16 & 0xff) * alpha1n2) + ((color2 >>> 16 & 0xff) * alphan12) + (color1 >>> 16 & 0xff)
									* (color2 >>> 16 & 0xff) * alpha12 / 255)
									/ newAlpha << 16
							| (((color1 >>> 8 & 0xff) * alpha1n2) + ((color2 >>> 8 & 0xff) * alphan12) + (color1 >>> 8 & 0xff)
									* (color2 >>> 8 & 0xff) * alpha12 / 255)
									/ newAlpha << 8
							| (((color1 & 0xff) * alpha1n2) + ((color2 & 0xff) * alphan12) + (color1 & 0xff)
									* (color2 & 0xff) * alpha12 / 255) / newAlpha;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Linear Dodge (Add) Mode
	// C = (aa * A + ab * B) / (aa + ab - aa*ab)

	public void fusionWithAddFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;
				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {

					/*
					 * // this version seems slower than the Math.min one int r = (alpha2 * (color2 >>> 16 & 0xff) +
					 * alpha1 * (color1 >>> 16 & 0xff)) / newAlpha; r |= ((~((r & 0xffffff00) - 1) >> 16) | r) & 0xff;
					 * int g = (alpha2 * (color2 >>> 8 & 0xff) + alpha1 * (color1 >>> 8 & 0xff)) / newAlpha; g |= ((~((g &
					 * 0xffffff00) - 1) >> 16) | g) & 0xff; int b = (alpha2 * (color2 & 0xff) + alpha1 * (color1 &
					 * 0xff)) / newAlpha; b |= ((~((b & 0xffffff00) - 1) >> 16) | b) & 0xff;
					 */

					int r = Math.min(255, (alpha2 * (color2 >>> 16 & 0xff) + alpha1 * (color1 >>> 16 & 0xff))
							/ newAlpha);
					int g = Math.min(255, (alpha2 * (color2 >>> 8 & 0xff) + alpha1 * (color1 >>> 8 & 0xff)) / newAlpha);
					int b = Math.min(255, (alpha2 * (color2 & 0xff) + alpha1 * (color1 & 0xff)) / newAlpha);

					fusion.data[off] = newAlpha << 24 | r << 16 | g << 8 | b;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Linear Burn (Sub) Mode
	// C = (aa * A + ab * B - aa*ab ) / (aa + ab - aa*ab)

	public void fusionWithSubtractFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;
				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2;

					int r = (alpha2 * (color2 >>> 16 & 0xff) + alpha1 * (color1 >>> 16 & 0xff) - alpha12) / newAlpha;
					r = r & (~r >>> 24); // binary magic to clamp negative values to zero without using a condition

					int g = (alpha2 * (color2 >>> 8 & 0xff) + alpha1 * (color1 >>> 8 & 0xff) - alpha12) / newAlpha;
					g = g & (~g >>> 24);

					int b = (alpha2 * (color2 & 0xff) + alpha1 * (color1 & 0xff) - alpha12) / newAlpha;
					b = b & (~b >>> 24);

					fusion.data[off] = newAlpha << 24 | r << 16 | g << 8 | b;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Screen Mode
	// same as Multiply except all color channels are inverted and the result too
	// C = 1 - (((1-A)*aa*(1-ab) + (1-B)*ab*(1-aa) + (1-A)*(1-B)*aa*ab) / (aa + ab - aa*ab))

	public void fusionWithScreenFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;
				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;
					color1 ^= 0xffffff;
					color2 ^= 0xffffff;

					fusion.data[off] = newAlpha << 24
							| (0xffffff ^ ((((color1 >>> 16 & 0xff) * alpha1n2) + ((color2 >>> 16 & 0xff) * alphan12) + (color1 >>> 16 & 0xff)
									* (color2 >>> 16 & 0xff) * alpha12 / 255)
									/ newAlpha << 16
									| (((color1 >>> 8 & 0xff) * alpha1n2) + ((color2 >>> 8 & 0xff) * alphan12) + (color1 >>> 8 & 0xff)
											* (color2 >>> 8 & 0xff) * alpha12 / 255)
											/ newAlpha << 8 | (((color1 & 0xff) * alpha1n2)
									+ ((color2 & 0xff) * alphan12) + (color1 & 0xff) * (color2 & 0xff) * alpha12 / 255)
									/ newAlpha));
				}
			}
		}
		fusion.alpha = 100;
	}

	// Lighten Mode
	// if B >= A: C = A*d + B*(1-d) and d = aa * (1-ab) / (aa + ab - aa*ab)
	// if A > B: C = B*d + A*(1-d) and d = ab * (1-aa) / (aa + ab - aa*ab)

	public void fusionWithLightenFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;
				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					// This alpha is used when color1 > color2
					int alpha12 = alpha2 * (alpha1 ^ 0xff) / newAlpha;
					int invAlpha12 = alpha12 ^ 0xff;

					// This alpha is used when color2 > color1
					int alpha21 = alpha1 * (alpha2 ^ 0xff) / newAlpha;
					int invAlpha21 = alpha21 ^ 0xff;

					int color = newAlpha << 24;
					int c1, c2;

					c1 = (color1 >>> 16 & 0xff);
					c2 = (color2 >>> 16 & 0xff);
					color |= ((c2 >= c1) ? (c1 * alpha21 + c2 * invAlpha21) : (c2 * alpha12 + c1 * invAlpha12)) / 255 << 16;

					c1 = (color1 >>> 8 & 0xff);
					c2 = (color2 >>> 8 & 0xff);
					color |= ((c2 >= c1) ? (c1 * alpha21 + c2 * invAlpha21) : (c2 * alpha12 + c1 * invAlpha12)) / 255 << 8;

					c1 = color1 & 0xff;
					c2 = color2 & 0xff;
					color |= ((c2 >= c1) ? (c1 * alpha21 + c2 * invAlpha21) : (c2 * alpha12 + c1 * invAlpha12)) / 255;

					fusion.data[off] = color;

				}
			}
		}
		fusion.alpha = 100;
	}

	// Darken Mode
	// if B >= A: C = B*d + A*(1-d) and d = ab * (1-aa) / (aa + ab - aa*ab)
	// if A > B: C = A*d + B*(1-d) and d = aa * (1-ab) / (aa + ab - aa*ab)

	public void fusionWithDarkenFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;
				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					// This alpha is used when color1 > color2
					int alpha12 = alpha1 * (alpha2 ^ 0xff) / newAlpha;
					int invAlpha12 = alpha12 ^ 0xff;

					// This alpha is used when color2 > color1
					int alpha21 = alpha2 * (alpha1 ^ 0xff) / newAlpha;
					int invAlpha21 = alpha21 ^ 0xff;

					int color = newAlpha << 24;
					int c1, c2;

					c1 = (color1 >>> 16 & 0xff);
					c2 = (color2 >>> 16 & 0xff);
					color |= ((c2 >= c1) ? (c2 * alpha21 + c1 * invAlpha21) : (c1 * alpha12 + c2 * invAlpha12)) / 255 << 16;

					c1 = (color1 >>> 8 & 0xff);
					c2 = (color2 >>> 8 & 0xff);
					color |= ((c2 >= c1) ? (c2 * alpha21 + c1 * invAlpha21) : (c1 * alpha12 + c2 * invAlpha12)) / 255 << 8;

					c1 = color1 & 0xff;
					c2 = color2 & 0xff;
					color |= ((c2 >= c1) ? (c2 * alpha21 + c1 * invAlpha21) : (c1 * alpha12 + c2 * invAlpha12)) / 255;

					fusion.data[off] = color;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Dodge Mode
	//
	// C = (aa*(1-ab)*A + (1-aa)*ab*B + aa*ab*B/(1-A)) / (aa + ab - aa*ab)

	public void fusionWithDodgeFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;

				if (alpha1 == 0) {
					continue;
				}

				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;
					int invColor1 = color1 ^ 0xffffffff;

					fusion.data[off] = newAlpha << 24
							| (((color1 >>> 16 & 0xff) * alpha1n2) + ((color2 >>> 16 & 0xff) * alphan12) + alpha12
									* (((invColor1 >>> 16 & 0xff) == 0) ? 255 : Math.min(255, 255
											* (color2 >>> 16 & 0xff) / (invColor1 >>> 16 & 0xff))))
									/ newAlpha << 16
							| (((color1 >>> 8 & 0xff) * alpha1n2) + ((color2 >>> 8 & 0xff) * alphan12) + alpha12
									* (((invColor1 >>> 8 & 0xff) == 0) ? 255 : Math.min(255, 255
											* (color2 >>> 8 & 0xff) / (invColor1 >>> 8 & 0xff))))
									/ newAlpha << 8
							| (((color1 & 0xff) * alpha1n2) + ((color2 & 0xff) * alphan12) + alpha12
									* (((invColor1 & 0xff) == 0) ? 255 : Math.min(255, 255 * (color2 & 0xff)
											/ (invColor1 & 0xff)))) / newAlpha;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Burn Mode
	//
	// C = (aa*(1-ab)*A + (1-aa)*ab*B + aa*ab*(1-(1-B)/A)) / (aa + ab - aa*ab)

	public void fusionWithBurnFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;

				if (alpha1 == 0) {
					continue;
				}

				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;
					int invColor2 = color2 ^ 0xffffffff;

					fusion.data[off] = newAlpha << 24
							| (((color1 >>> 16 & 0xff) * alpha1n2) + ((color2 >>> 16 & 0xff) * alphan12) + alpha12
									* (((color1 >>> 16 & 0xff) == 0) ? 0 : Math.min(255, 255
											* (invColor2 >>> 16 & 0xff) / (color1 >>> 16 & 0xff)) ^ 0xff))
									/ newAlpha << 16
							| (((color1 >>> 8 & 0xff) * alpha1n2) + ((color2 >>> 8 & 0xff) * alphan12) + alpha12
									* (((color1 >>> 8 & 0xff) == 0) ? 0 : Math.min(255, 255 * (invColor2 >>> 8 & 0xff)
											/ (color1 >>> 8 & 0xff)) ^ 0xff))
									/ newAlpha << 8
							| (((color1 & 0xff) * alpha1n2) + ((color2 & 0xff) * alphan12) + alpha12
									* (((color1 & 0xff) == 0) ? 0 : Math.min(255, 255 * (invColor2 & 0xff)
											/ (color1 & 0xff)) ^ 0xff)) / newAlpha;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Overlay Mode
	// If B <= 0.5 C = (A*aa*(1-ab) + B*ab*(1-aa) + aa*ab*(2*A*B) / (aa + ab - aa*ab)
	// If B > 0.5 C = (A*aa*(1-ab) + B*ab*(1-aa) + aa*ab*(1 - 2*(1-A)*(1-B)) / (aa + ab - aa*ab)

	public void fusionWithOverlayFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;

				if (alpha1 == 0) {
					continue;
				}

				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;

					int color = newAlpha << 24;
					int c1, c2;

					c1 = (color1 >>> 16 & 0xff);
					c2 = (color2 >>> 16 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c2 <= 127) ? (alpha12 * 2 * c1 * c2 / 255)
							: (alpha12 * ((2 * (c1 ^ 0xff) * (c2 ^ 0xff) / 255) ^ 0xff))))
							/ newAlpha << 16;

					c1 = (color1 >>> 8 & 0xff);
					c2 = (color2 >>> 8 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c2 <= 127) ? (alpha12 * 2 * c1 * c2 / 255)
							: (alpha12 * ((2 * (c1 ^ 0xff) * (c2 ^ 0xff) / 255) ^ 0xff))))
							/ newAlpha << 8;

					c1 = color1 & 0xff;
					c2 = color2 & 0xff;
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c2 <= 127) ? (alpha12 * 2 * c1 * c2 / 255)
							: (alpha12 * ((2 * (c1 ^ 0xff) * (c2 ^ 0xff) / 255) ^ 0xff))))
							/ newAlpha;

					fusion.data[off] = color;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Hard Light Mode (same as Overlay with A and B swapped)
	// If A <= 0.5 C = (A*aa*(1-ab) + B*ab*(1-aa) + aa*ab*(2*A*B) / (aa + ab - aa*ab)
	// If A > 0.5 C = (A*aa*(1-ab) + B*ab*(1-aa) + aa*ab*(1 - 2*(1-A)*(1-B)) / (aa + ab - aa*ab)

	public void fusionWithHardLightFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;

				if (alpha1 == 0) {
					continue;
				}

				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;

					int color = newAlpha << 24;
					int c1, c2;

					c1 = (color1 >>> 16 & 0xff);
					c2 = (color2 >>> 16 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * 2 * c1 * c2 / 255)
							: (alpha12 * ((2 * (c1 ^ 0xff) * (c2 ^ 0xff) / 255) ^ 0xff))))
							/ newAlpha << 16;

					c1 = (color1 >>> 8 & 0xff);
					c2 = (color2 >>> 8 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * 2 * c1 * c2 / 255)
							: (alpha12 * ((2 * (c1 ^ 0xff) * (c2 ^ 0xff) / 255) ^ 0xff))))
							/ newAlpha << 8;

					c1 = color1 & 0xff;
					c2 = color2 & 0xff;
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * 2 * c1 * c2 / 255)
							: (alpha12 * ((2 * (c1 ^ 0xff) * (c2 ^ 0xff) / 255) ^ 0xff))))
							/ newAlpha;

					fusion.data[off] = color;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Soft Light Mode
	// A < 0.5 => C = (2*A - 1) * (B - B^2) + B
	// A > 0.5 => C = (2*A - 1) * (sqrt(B) - B) + B

	public void fusionWithSoftLightFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;

				if (alpha1 == 0) {
					continue;
				}

				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;

					int color = newAlpha << 24;
					int c1, c2;

					c1 = (color1 >>> 16 & 0xff);
					c2 = (color2 >>> 16 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * ((2 * c1 - 255)
							* softLightLUTSquare[c2] / 255 + c2)) : (alpha12 * ((2 * c1 - 255)
							* softLightLUTSquareRoot[c2] / 255 + c2))))
							/ newAlpha << 16;

					c1 = (color1 >>> 8 & 0xff);
					c2 = (color2 >>> 8 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * ((2 * c1 - 255)
							* softLightLUTSquare[c2] / 255 + c2)) : (alpha12 * ((2 * c1 - 255)
							* softLightLUTSquareRoot[c2] / 255 + c2))))
							/ newAlpha << 8;

					c1 = color1 & 0xff;
					c2 = color2 & 0xff;
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * ((2 * c1 - 255)
							* softLightLUTSquare[c2] / 255 + c2)) : (alpha12 * ((2 * c1 - 255)
							* softLightLUTSquareRoot[c2] / 255 + c2))))
							/ newAlpha;

					fusion.data[off] = color;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Vivid Light Mode
	// A < 0.5 => C = 1 - (1-B) / (2*A)
	// A > 0.5 => C = B / (2*(1-A))

	public void fusionWithVividLightFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;

				if (alpha1 == 0) {
					continue;
				}

				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;

					int color = newAlpha << 24;
					int c1, c2;

					c1 = (color1 >>> 16 & 0xff);
					c2 = (color2 >>> 16 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * ((c1 == 0) ? 0 : 255 - Math
							.min(255, (255 - c2) * 255 / (2 * c1)))) : (alpha12 * (c1 == 255 ? 255 : Math.min(255, c2
							* 255 / (2 * (255 - c1)))))))
							/ newAlpha << 16;

					c1 = (color1 >>> 8 & 0xff);
					c2 = (color2 >>> 8 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * ((c1 == 0) ? 0 : 255 - Math
							.min(255, (255 - c2) * 255 / (2 * c1)))) : (alpha12 * (c1 == 255 ? 255 : Math.min(255, c2
							* 255 / (2 * (255 - c1)))))))
							/ newAlpha << 8;

					c1 = color1 & 0xff;
					c2 = color2 & 0xff;
					color |= (alpha1n2 * c1 + alphan12 * c2 + ((c1 <= 127) ? (alpha12 * ((c1 == 0) ? 0 : 255 - Math
							.min(255, (255 - c2) * 255 / (2 * c1)))) : (alpha12 * (c1 == 255 ? 255 : Math.min(255, c2
							* 255 / (2 * (255 - c1)))))))
							/ newAlpha;

					fusion.data[off] = color;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Linear Light Mode
	// C = B + 2*A -1

	public void fusionWithLinearLightFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;

				if (alpha1 == 0) {
					continue;
				}

				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;

					int color = newAlpha << 24;
					int c1, c2;

					c1 = (color1 >>> 16 & 0xff);
					c2 = (color2 >>> 16 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + (alpha12 * Math.min(255, Math.max(0, c2 + 2 * c1 - 255))))
							/ newAlpha << 16;

					c1 = (color1 >>> 8 & 0xff);
					c2 = (color2 >>> 8 & 0xff);
					color |= (alpha1n2 * c1 + alphan12 * c2 + (alpha12 * Math.min(255, Math.max(0, c2 + 2 * c1 - 255))))
							/ newAlpha << 8;

					c1 = color1 & 0xff;
					c2 = color2 & 0xff;
					color |= (alpha1n2 * c1 + alphan12 * c2 + (alpha12 * Math.min(255, Math.max(0, c2 + 2 * c1 - 255))))
							/ newAlpha;

					fusion.data[off] = color;
				}
			}
		}
		fusion.alpha = 100;
	}

	// Pin Light Mode
	// B > 2*A => C = 2*A
	// B < 2*A-1 => C = 2*A-1
	// else => C = B

	public void fusionWithPinLightFullAlpha(CPLayer fusion, CPRect rc) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(rc);

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			for (int i = rect.left; i < rect.right; i++, off++) {
				int color1 = data[off];
				int alpha1 = (color1 >>> 24) * alpha / 100;

				if (alpha1 == 0) {
					continue;
				}

				int color2 = fusion.data[off];
				int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

				int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
				if (newAlpha > 0) {
					int alpha12 = alpha1 * alpha2 / 255;
					int alpha1n2 = alpha1 * (alpha2 ^ 0xff) / 255;
					int alphan12 = (alpha1 ^ 0xff) * alpha2 / 255;

					int color = newAlpha << 24;
					int c1, c2, c3;

					c1 = (color1 >>> 16 & 0xff);
					c2 = (color2 >>> 16 & 0xff);
					c3 = (c2 >= 2 * c1) ? (2 * c1) : (c2 <= 2 * c1 - 255) ? (2 * c1 - 255) : c2;
					color |= (alpha1n2 * c1 + alphan12 * c2 + alpha12 * c3) / newAlpha << 16;

					c1 = (color1 >>> 8 & 0xff);
					c2 = (color2 >>> 8 & 0xff);
					c3 = (c2 >= 2 * c1) ? (2 * c1) : (c2 <= 2 * c1 - 255) ? (2 * c1 - 255) : c2;
					color |= (alpha1n2 * c1 + alphan12 * c2 + alpha12 * c3) / newAlpha << 8;

					c1 = color1 & 0xff;
					c2 = color2 & 0xff;
					c3 = (c2 >= 2 * c1) ? (2 * c1) : (c2 <= 2 * c1 - 255) ? (2 * c1 - 255) : c2;
					color |= (alpha1n2 * c1 + alphan12 * c2 + alpha12 * c3) / newAlpha;

					fusion.data[off] = color;
				}
			}
		}
		fusion.alpha = 100;
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	public void setBlendMode(int blendMode) {
		this.blendMode = blendMode;
	}

	public void rendererCheckboard(CPRect r) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);
		for (int j = rect.top; j < rect.bottom; j++) {
			for (int i = rect.left; i < rect.right; i++) {
				if ((i & 0x8) != 0 ^ (j & 0x8) != 0) {
					data[i + j * width] = 0xffffffff;
				} else {
					data[i + j * width] = 0xffcccccc;
				}
			}
		}
	}

	public void copyRegionHFlip(CPRect r, CPLayer source) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);

		for (int j = rect.top; j < rect.bottom; j++) {
			for (int i = rect.left, s = rect.right - 1; i < rect.right; i++, s--) {
				data[i + j * width] = source.data[s + j * width];
			}
		}
	}

	public void copyRegionVFlip(CPRect r, CPLayer source) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);

		for (int j = rect.top, s = rect.bottom - 1; j < rect.bottom; j++, s--) {
			for (int i = rect.left; i < rect.right; i++) {
				data[i + j * width] = source.data[i + s * width];
			}
		}
	}

	public void fillWithNoise(CPRect r) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);

		int value;
		Random rnd = new Random();

		for (int j = rect.top; j < rect.bottom; j++) {
			for (int i = rect.left; i < rect.right; i++) {
				value = rnd.nextInt();
				value &= 0xff;
				value |= (value << 8) | (value << 16) | 0xff000000;
				data[i + j * width] = value;
			}
		}
	}

	public void fillWithColorNoise(CPRect r) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);

		Random rnd = new Random();

		for (int j = rect.top; j < rect.bottom; j++) {
			for (int i = rect.left; i < rect.right; i++) {
				data[i + j * width] = rnd.nextInt() | 0xff000000;
			}
		}
	}

	public void invert(CPRect r) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);

		for (int j = rect.top; j < rect.bottom; j++) {
			for (int i = rect.left; i < rect.right; i++) {
				data[i + j * width] ^= 0xffffff;
			}
		}
	}

	public boolean hasAlpha() {
		if (alpha != 100) {
			return true;
		}

		int andPixels = 0xff000000;
		int max = width * height;
		for (int i = 0; i < max; i++) {
			andPixels &= data[i];
		}

		return andPixels != 0xff000000;
	}

	public boolean hasAlpha(CPRect r) {
		if (alpha != 100) {
			return true;
		}

		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);

		int andPixels = 0xff000000;

		for (int j = rect.top; j < rect.bottom; j++) {
			int off = rect.left + j * width;
			int max = off + rect.right - rect.left;
			for (; off < max; off++) {
				andPixels &= data[off];
			}
		}

		return andPixels != 0xff000000;
	}
}
