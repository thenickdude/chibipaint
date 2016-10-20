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


public class CPLookUpTable {
	
	int[] table = new int[256];
	
	public CPLookUpTable() {
		loadIdentity();
	}
	
	public CPLookUpTable(float brightness, float contrast) {
		loadBrightnessContrast(brightness, contrast);
	}
	
	public void loadIdentity() {
		for (int i=0; i<256; i++) {
			table[i] = i;
		}
	}
	
	public void loadBrightnessContrast(float brightness, float contrast) {
		float slope = contrast > 0.f ? (1f / (1.0001f - contrast)): 1f + contrast;
		float offset = .5f - slope * .5f + brightness;
		for (int i=0; i<256; i++) {
			float x = i / 255.f;
			float y = x * slope + offset;
			
			table[i] = Math.min(255, Math.max((int) (y * 255.f), 0));
		}		
	}
	
	public void inverse() {
		for (int i=0; i<256; i++) {
			table[i] = 255 - table[i];
		}
	}

}
