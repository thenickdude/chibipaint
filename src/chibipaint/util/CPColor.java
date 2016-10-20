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

public class CPColor implements Cloneable {

	public int rgb;
	public int hue, saturation, value;

	public CPColor() {
		setRgb(0);
	}

	public CPColor(int rgb) {
		setRgb(rgb);
	}

	public CPColor(int hue, int saturation, int value) {
		setHsv(hue, saturation, value);
	}

	public void setRgb(int rgb) {
		this.rgb = rgb;
		rgbToHsv();
	}

	public void setHsv(int hue, int value, int saturation) {
		this.hue = hue;
		this.saturation = saturation;
		this.value = value;

		hsvToRgb();
	}

	void rgbToHsv() {
		int r = rgb >> 16;
		int g = (rgb >> 8) & 0xff;
		int b = rgb & 0xff;

		// Value
		value = Math.max(r, Math.max(g, b));

		// Saturation
		int mini = Math.min(r, Math.min(g, b));
		if (value == 0) {
			saturation = 0;
		} else {
			saturation = (int) ((float) (value - mini) / (float) value * 255.f);
		}

		// Hue
		if (saturation == 0) {
			hue = 0;
		} else {
			float cr = (float) (value - r) / (float) (value - mini);
			float cg = (float) (value - g) / (float) (value - mini);
			float cb = (float) (value - b) / (float) (value - mini);

			float _hue = 0;
			if (value == r) {
				_hue = cb - cg;
			}
			if (value == g) {
				_hue = 2 + cr - cb;
			}
			if (value == b) {
				_hue = 4 + cg - cr;
			}

			_hue *= 60;
			if (_hue < 0) {
				_hue += 360;
			}

			hue = (int) (_hue);
		}
	}

	void hsvToRgb() {
		// no saturation means it's just a shade of grey
		if (saturation == 0) {
			rgb = (value << 16) | (value << 8) | value;
		}

		float f = (hue) / 60.f;
		f = f - (float) Math.floor(f);

		float s = saturation / 255.f;
		int m = (int) (value * (1 - s));
		int n = (int) (value * (1 - s * f));
		int k = (int) (value * (1 - s * (1 - f)));

		switch (hue / 60) {
		case 0:
			rgb = (value << 16) | (k << 8) | m;
			break;
		case 1:
			rgb = (n << 16) | (value << 8) | m;
			break;
		case 2:
			rgb = (m << 16) | (value << 8) | k;
			break;
		case 3:
			rgb = (m << 16) | (n << 8) | value;
			break;
		case 4:
			rgb = (k << 16) | (m << 8) | value;
			break;
		case 5:
			rgb = (value << 16) | (m << 8) | n;
			break;
		default:
			rgb = 0; // invalid hue
			break;
		}
	}

	public void setHue(int hue) {
		this.hue = hue;
		hsvToRgb();
	}

	public void setSaturation(int saturation) {
		this.saturation = saturation;
		hsvToRgb();
	}

	public void setValue(int value) {
		this.value = value;
		hsvToRgb();
	}

	public int getRgb() {
		return rgb;
	}

	public int getHue() {
		return hue;
	}

	public int getSaturation() {
		return saturation;
	}

	public int getValue() {
		return value;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (Exception ignored) {
			throw new Error("Uh oh");
		}
	}

	public void copyFrom(CPColor c) {
		rgb = c.rgb;
		hue = c.hue;
		saturation = c.saturation;
		value = c.value;
	}

	public boolean isEqual(CPColor color) {
		return rgb == color.rgb && hue == color.hue && saturation == color.saturation && value == color.value;
	}

}
