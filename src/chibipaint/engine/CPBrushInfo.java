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

public class CPBrushInfo implements Cloneable {

	// Stroke modes
	public static final int SM_FREEHAND = 0;
	public static final int SM_LINE = 1;
	public static final int SM_BEZIER = 2;

	// Brush dab types
	public static final int B_ROUND_PIXEL = 0;
	public static final int B_ROUND_AA = 1;
	public static final int B_ROUND_AIRBRUSH = 2;
	public static final int B_SQUARE_PIXEL = 3;
	public static final int B_SQUARE_AA = 4;

	// painting modes
	public static final int M_PAINT = 0;
	public static final int M_ERASE = 1;
	public static final int M_DODGE = 2;
	public static final int M_BURN = 3;
	public static final int M_WATER = 4;
	public static final int M_BLUR = 5;
	public static final int M_SMUDGE = 6;
	public static final int M_OIL = 7;

	// Brush settings
	public int toolNb;
	public int size, alpha;
	public boolean isAA, isAirbrush;
	public float minSpacing, spacing;
	public boolean pressureSize, pressureAlpha;
	public int type, paintMode;
	public int strokeMode = SM_FREEHAND;
	public float resat = 1f, bleed = 0f;

	public float texture = 1f;

	public boolean pressureScattering = false;
	public float scattering = 0.f, curScattering;

	public float squeeze = 0f, curSqueeze;
	public float angle = (float) Math.PI, curAngle;

	public float smoothing = 0f;

	// Current brush setting (once tablet pressure and stuff is applied)
	public float curSize;
	public int curAlpha;

	public CPBrushInfo() {
	}

	public CPBrushInfo(int toolNb, int size, int alpha, boolean isAA, boolean isAirbrush, float minSpacing,
			float spacing, boolean pressureSize, boolean pressureAlpha, int brushType, int paintMode, float resat,
			float bleed) {
		this.toolNb = toolNb;
		this.size = size;
		this.alpha = alpha;
		this.isAA = isAA;
		this.isAirbrush = isAirbrush;
		this.minSpacing = minSpacing;
		this.spacing = spacing;

		this.pressureSize = pressureSize;
		this.pressureAlpha = pressureAlpha;

		this.type = brushType;
		this.paintMode = paintMode;

		this.resat = resat;
		this.bleed = bleed;
	}

	public void applyPressure(float pressure) {
		// FIXME: no variable size for smudge and oil :(
		if (pressureSize && paintMode != M_SMUDGE && paintMode != M_OIL) {
			curSize = Math.max(.1f, size * pressure);
		} else {
			curSize = Math.max(.1f, size);
		}

		// FIXME: what is the point of doing that?
		if (curSize > 16) {
			curSize = (int) curSize;
		}

		curAlpha = pressureAlpha ? (int) (alpha * pressure) : alpha;
		curSqueeze = squeeze;
		curAngle = angle;
		curScattering = scattering * curSize * (pressureScattering ? pressure : 1.f);

		// tests
		// curScattering = scattering * pressure;
		// curSqueeze = squeeze * pressure;
		// curAngle = angle * pressure;

	}

	public Object clone() {
		try {
			return super.clone();
		} catch (Exception ignored) {
			return null;
		}
	}
}
