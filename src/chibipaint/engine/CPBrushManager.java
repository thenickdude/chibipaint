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

public class CPBrushManager {

	byte[] brush, brushAA;

	byte[] cacheBrush;
	float cacheSize, cacheSqueeze, cacheAngle;
	int cacheType;

	CPGreyBmp texture;

	private static final float MAX_SQUEEZE = 10;

	public static class CPBrushDab {

		// the brush
		public byte[] brush;
		public int width, height;

		// and where and how to apply it
		public int x, y, alpha;
	}

	public CPBrushManager() {
		brush = new byte[201 * 201];
		brushAA = new byte[202 * 202];

		// test texture
		/*
		 * texture = new byte[9]; texture[0] = 0; texture[1] = (byte) 255; texture[2] = (byte) 255; texture[3] = 0;
		 * textureWidth = 2; textureHeight = 2;
		 */
	}

	public CPBrushDab getDab(float x, float y, CPBrushInfo brushInfo) {
		CPBrushDab dab = new CPBrushDab();
		dab.alpha = brushInfo.curAlpha;

		// FIXME: I don't like this special case for ROUND_PIXEL
		// it would be better to have brush presets for working with pixels
		boolean useAA = brushInfo.isAA && brushInfo.type != CPBrushInfo.B_ROUND_PIXEL;

		dab.width = (int) (brushInfo.curSize + .99f);
		dab.height = (int) (brushInfo.curSize + .99f);

		if (useAA) {
			dab.width++;
			dab.height++;
		}

		float nx = x - dab.width / 2.f + .5f;
		float ny = y - dab.height / 2.f + .5f;

		// this is necessary as Java uses convert towards zero float to int conversion
		if (nx < 0) {
			nx -= 1;
		}
		if (ny < 0) {
			ny -= 1;
		}

		if (useAA) {
			float dx = Math.abs(nx - ((int) nx));
			float dy = Math.abs(ny - ((int) ny));
			dab.brush = getBrushWithAA(brushInfo, dx, dy);
		} else {
			dab.brush = getBrush(brushInfo);
		}

		dab.x = (int) nx;
		dab.y = (int) ny;

		if (brushInfo.texture > 0.f && texture != null) {
			// we need a brush bitmap that can be modified everytime
			// the one in "brush" can be kept in cache so if we are using it, make a copy
			if (dab.brush == brush) {
				System.arraycopy(brush, 0, brushAA, 0, dab.width * dab.height);
				dab.brush = brushAA;
			}
			applyTexture(dab, brushInfo.texture);
		}
		return dab;
	}

	byte[] getBrush(CPBrushInfo brushInfo) {
		if (cacheBrush != null && brushInfo.curSize == cacheSize && brushInfo.curSqueeze == cacheSqueeze
				&& brushInfo.curAngle == cacheAngle && brushInfo.type == cacheType) {
			return cacheBrush;
		}

		if (brushInfo.type == CPBrushInfo.B_ROUND_AIRBRUSH) {
			brush = buildBrushSoft(brush, brushInfo);
		} else if (brushInfo.type == CPBrushInfo.B_ROUND_AA) {
			brush = buildBrushAA(brush, brushInfo);
		} else if (brushInfo.type == CPBrushInfo.B_ROUND_PIXEL) {
			brush = buildBrush(brush, brushInfo);
		} else if (brushInfo.type == CPBrushInfo.B_SQUARE_AA) {
			brush = buildBrushSquareAA(brush, brushInfo);
		} else if (brushInfo.type == CPBrushInfo.B_SQUARE_PIXEL) {
			brush = buildBrushSquare(brush, brushInfo);
		}

		cacheBrush = brush;
		cacheSize = brushInfo.curSize;
		cacheType = brushInfo.type;
		cacheSqueeze = brushInfo.curSqueeze;
		cacheAngle = brushInfo.curAngle;

		return brush;
	}

	byte[] getBrushWithAA(CPBrushInfo brushInfo, float dx, float dy) {
		byte[] nonAABrush = getBrush(brushInfo);

		int intSize = (int) (brushInfo.curSize + .99f);
		int intSizeAA = (int) (brushInfo.curSize + .99f) + 1;

		for (int y = 0; y < intSizeAA; y++) {
			for (int x = 0; x < intSizeAA; x++) {
				brushAA[y * intSizeAA + x] = 0;
			}
		}

		for (int y = 0; y < intSize; y++) {
			for (int x = 0; x < intSize; x++) {
				int brushAlpha = nonAABrush[y * intSize + x] & 0xff;

				brushAA[y * intSizeAA + x] += (int) (brushAlpha * (1 - dx) * (1 - dy));
				brushAA[y * intSizeAA + (x + 1)] += (int) (brushAlpha * dx * (1 - dy));
				brushAA[(y + 1) * intSizeAA + x + 1] += (int) (brushAlpha * dx * dy);
				brushAA[(y + 1) * intSizeAA + x] += (int) (brushAlpha * (1 - dx) * dy);
			}
		}

		return brushAA;
	}

	byte[] buildBrush(byte[] brush, CPBrushInfo brushInfo) {
		int intSize = (int) (brushInfo.curSize + .99f);
		float center = intSize / 2.f;
		float sqrRadius = (brushInfo.curSize / 2) * (brushInfo.curSize / 2);

		float xFactor = 1f + brushInfo.curSqueeze * MAX_SQUEEZE;

		float cosA = (float) Math.cos(brushInfo.curAngle);
		float sinA = (float) Math.sin(brushInfo.curAngle);

		int offset = 0;
		for (int j = 0; j < intSize; j++) {
			for (int i = 0; i < intSize; i++) {
				float x = (i + .5f - center);
				float y = (j + .5f - center);
				float dx = (x * cosA - y * sinA) * xFactor;
				float dy = (y * cosA + x * sinA);

				float sqrDist = dx * dx + dy * dy;

				if (sqrDist <= sqrRadius) {
					brush[offset++] = (byte) 0xff;
				} else {
					brush[offset++] = 0;
				}
			}
		}

		return brush;
	}

	byte[] buildBrushAA(byte[] brush, CPBrushInfo brushInfo) {
		int intSize = (int) (brushInfo.curSize + .99f);
		float center = intSize / 2.f;
		float sqrRadius = (brushInfo.curSize / 2) * (brushInfo.curSize / 2);
		float sqrRadiusInner = ((brushInfo.curSize - 2) / 2) * ((brushInfo.curSize - 2) / 2);
		float sqrRadiusOuter = ((brushInfo.curSize + 2) / 2) * ((brushInfo.curSize + 2) / 2);

		float xFactor = 1f + brushInfo.curSqueeze * MAX_SQUEEZE;
		float cosA = (float) Math.cos(brushInfo.curAngle);
		float sinA = (float) Math.sin(brushInfo.curAngle);

		int offset = 0;
		for (int j = 0; j < intSize; j++) {
			for (int i = 0; i < intSize; i++) {
				float x = (i + .5f - center);
				float y = (j + .5f - center);
				float dx = (x * cosA - y * sinA) * xFactor;
				float dy = (y * cosA + x * sinA);

				float sqrDist = dx * dx + dy * dy;

				if (sqrDist <= sqrRadiusInner) {
					brush[offset++] = (byte) 0xff;
				} else if (sqrDist > sqrRadiusOuter) {
					brush[offset++] = 0;
				} else {
					int count = 0;
					for (int oj = 0; oj < 4; oj++) {
						for (int oi = 0; oi < 4; oi++) {
							x = i + oi * (1.f / 4.f) - center;
							y = j + oj * (1.f / 4.f) - center;
							dx = (x * cosA - y * sinA) * xFactor;
							dy = (y * cosA + x * sinA);

							sqrDist = dx * dx + dy * dy;
							if (sqrDist <= sqrRadius) {
								count += 1;
							}
						}
					}
					brush[offset++] = (byte) Math.min(count * 16, 255);
				}
			}
		}

		return brush;
	}

	byte[] buildBrushSquare(byte[] brush, CPBrushInfo brushInfo) {
		int intSize = (int) (brushInfo.curSize + .99f);
		float center = intSize / 2.f;

		float size = brushInfo.curSize * (float) Math.sin(Math.PI / 4);
		float sizeX = (size / 2) / (1f + brushInfo.curSqueeze * MAX_SQUEEZE);
		float sizeY = (size / 2);

		float cosA = (float) Math.cos(brushInfo.curAngle);
		float sinA = (float) Math.sin(brushInfo.curAngle);

		int offset = 0;
		for (int j = 0; j < intSize; j++) {
			for (int i = 0; i < intSize; i++) {
				float x = (i + .5f - center);
				float y = (j + .5f - center);
				float dx = Math.abs(x * cosA - y * sinA);
				float dy = Math.abs(y * cosA + x * sinA);

				if (dx <= sizeX && dy <= sizeY) {
					brush[offset++] = (byte) 0xff;
				} else {
					brush[offset++] = 0;
				}
			}
		}

		return brush;
	}

	byte[] buildBrushSquareAA(byte[] brush, CPBrushInfo brushInfo) {
		int intSize = (int) (brushInfo.curSize + .99f);
		float center = intSize / 2.f;

		float size = brushInfo.curSize * (float) Math.sin(Math.PI / 4);
		float sizeX = (size / 2) / (1f + brushInfo.curSqueeze * MAX_SQUEEZE);
		float sizeY = (size / 2);

		float sizeXInner = sizeX - 1;
		float sizeYInner = sizeY - 1;

		float sizeXOuter = sizeX + 1;
		float sizeYOuter = sizeY + 1;

		float cosA = (float) Math.cos(brushInfo.curAngle);
		float sinA = (float) Math.sin(brushInfo.curAngle);

		int offset = 0;
		for (int j = 0; j < intSize; j++) {
			for (int i = 0; i < intSize; i++) {
				float x = (i + .5f - center);
				float y = (j + .5f - center);
				float dx = Math.abs(x * cosA - y * sinA);
				float dy = Math.abs(y * cosA + x * sinA);

				if (dx <= sizeXInner && dy <= sizeYInner) {
					brush[offset++] = (byte) 0xff;
				} else if (dx > sizeXOuter || dy > sizeYOuter) {
					brush[offset++] = 0;
				} else {
					int count = 0;
					for (int oj = 0; oj < 4; oj++) {
						for (int oi = 0; oi < 4; oi++) {
							x = i + oi * (1.f / 4.f) - center;
							y = j + oj * (1.f / 4.f) - center;
							dx = Math.abs(x * cosA - y * sinA);
							dy = Math.abs(y * cosA + x * sinA);

							if (dx <= sizeX && dy <= sizeY) {
								count += 1;
							}
						}
					}
					brush[offset++] = (byte) Math.min(count * 16, 255);
				}
			}
		}

		return brush;
	}

	byte[] buildBrushSoft(byte[] brush, CPBrushInfo brushInfo) {
		int intSize = (int) (brushInfo.curSize + .99f);
		float center = intSize / 2.f;
		float sqrRadius = (brushInfo.curSize / 2) * (brushInfo.curSize / 2);

		float xFactor = 1f + brushInfo.curSqueeze * MAX_SQUEEZE;
		float cosA = (float) Math.cos(brushInfo.curAngle);
		float sinA = (float) Math.sin(brushInfo.curAngle);

		// byte[] brush = new int[size * size];
		int offset = 0;
		for (int j = 0; j < intSize; j++) {
			for (int i = 0; i < intSize; i++) {
				float x = (i + .5f - center);
				float y = (j + .5f - center);
				float dx = (x * cosA - y * sinA) * xFactor;
				float dy = (y * cosA + x * sinA);

				float sqrDist = dx * dx + dy * dy;

				if (sqrDist <= sqrRadius) {
					brush[offset++] = (byte) (255 * (1 - (sqrDist / sqrRadius)));
				} else {
					brush[offset++] = 0;
				}
			}
		}

		return brush;
	}

	void applyTexture(CPBrushDab dab, float textureAmount) {
		int amount = (int) (textureAmount * 255f);
		int offset = 0;
		for (int j = 0; j < dab.height; j++) {

			for (int i = 0; i < dab.width; i++) {
				int brushValue = (dab.brush[offset]) & 0xff;
				int textureX = (i + dab.x) % texture.width;
				if (textureX < 0) {
					textureX += texture.width;
				}

				int textureY = (j + dab.y) % texture.height;
				if (textureY < 0) {
					textureY += texture.height;
				}

				int textureValue = (texture.data[textureX + textureY * texture.width]) & 0xff;
				dab.brush[offset] = (byte) (brushValue * ((textureValue * amount / 255) ^ 0xff) / 255);
				offset++;
			}
		}
	}

	public void setTexture(CPGreyBmp texture) {
		this.texture = texture;
	}
}
