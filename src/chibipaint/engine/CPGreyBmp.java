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

public class CPGreyBmp extends CPBitmap {

	public byte[] data;

	// Allocates a new bitmap
	public CPGreyBmp(int width, int height) {
		super(width, height);
		this.data = new byte[width * height];
	}
	
	public CPGreyBmp(CPGreyBmp original) {
		super(original.width, original.height);
		this.data = new byte[width * height];
		
		System.arraycopy(original.data, 0, data, 0, width*height);
	}
	
	public void inverse() {
		for (int i=0, size=width*height; i<size; i++) {
			data[i] = (byte) (255 - data[i]);
		}
	}

	public void mirrorHorizontally() {
		byte[] newData = new byte[width*height];
		
		for (int j=0; j<height; j++) {
			for (int i=0; i<width; i++) {
				newData[j*width + i] = data[j*width + width-i-1];
			}
		}
		
		data = newData;
	}
	
	public void applyLUT(CPLookUpTable lut) {
		for (int i=0, size=width*height; i<size; i++) {
			data[i] = (byte) lut.table[data[i] & 0xff];
		}
	}

}
