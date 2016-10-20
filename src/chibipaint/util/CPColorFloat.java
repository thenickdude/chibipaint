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

package chibipaint.util;

public class CPColorFloat implements Cloneable {

	public float r, g, b;

	public CPColorFloat() {
		r = g = b = 0f;
	}

	public CPColorFloat(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public CPColorFloat(int color) {
		r = (color >>> 16 & 0xff) / 255f;
		g = (color >>> 8 & 0xff) / 255f;
		b = (color & 0xff) / 255f;
	}

	public int toInt() {
		return Math.max(0, Math.min(255, (int) (r * 255f))) << 16 | Math.max(0, Math.min(255, (int) (g * 255f))) << 8
				| Math.max(0, Math.min(255, (int) (b * 255f)));
	}

	public void mixWith(CPColorFloat color, float alpha) {
		r = r * (1f - alpha) + color.r * alpha;
		g = g * (1f - alpha) + color.g * alpha;
		b = b * (1f - alpha) + color.b * alpha;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (Exception ignored) {
			throw new Error("Uh oh");
		}
	}
}
