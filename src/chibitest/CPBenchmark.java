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

package chibitest;

import java.util.*;

import chibipaint.engine.*;
import chibipaint.engine.CPBrushManager.*;
import chibipaint.util.*;

public class CPBenchmark {

	public CPBenchmark() {
	}

	public static void main(String[] args) {
		CPBenchmark bench = new CPBenchmark();

		if (args.length > 0 && args[0].equals("blending")) {
			bench.blendingBenchmark(args);
		} else if (args.length > 0 && args[0].equals("dabs")) {
			bench.brushDabsBenchmark(args);
		} else {
			System.out.println("no valid benchmark selected");
		}
	}

	void blendingBenchmark(String[] args) {
		int iterations = args.length > 1 ? Integer.valueOf(args[1]) : 100;
		int testW = args.length > 2 ? Integer.valueOf(args[2]) : 512;
		int testH = testW;
		CPLayer l1 = new CPLayer(testW, testH), l2 = new CPLayer(testW, testH);

		System.out
				.println("Benchmarking layer blending " + testW + "x" + testH + ", iterations=" + iterations + "\n\n");

		boolean fa = false;

		System.out.println("Testing if layer is full-alpha mode");
		System.out.println("===================================");

		prepareLayers(l1, l2, 0);
		double lastTime = System.currentTimeMillis(), newTime;
		for (int i = 0; i < iterations; i++) {
			l2.hasAlpha();
		}
		newTime = System.currentTimeMillis();
		System.out.println("Result time: " + String.valueOf((newTime - lastTime) / 1000.) + "s "
				+ String.valueOf((newTime - lastTime) * 1000. / (iterations)) + "us per iteration "
				+ String.valueOf((newTime - lastTime) * 1000000. / ((double) iterations * testW * testH))
				+ "ns per pixel\n");

		for (int i = 0; i < 2; i++, fa = !fa) {
			System.out.println("Full Alpha: " + (fa ? "on" : "off"));

			System.out.println("Normal blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_NORMAL);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Multiply blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_MULTIPLY);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Add blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_ADD);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Screen blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_SCREEN);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Lighten blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_LIGHTEN);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Darken blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_DARKEN);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Subtract blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_SUBTRACT);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Dodge blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_DODGE);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Burn blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_BURN);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Overlay blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_OVERLAY);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Hard light blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_HARDLIGHT);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Soft light blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_SOFTLIGHT);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Vivid light blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_VIVIDLIGHT);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);

			System.out.println("Pin light blending mode");
			System.out.println("====================");

			l1.setBlendMode(CPLayer.LM_PINLIGHT);
			prepareLayers(l1, l2, 0);
			blendingBench(l1, l2, iterations, fa);
		}
	}

	static void prepareLayers(CPLayer l1, CPLayer l2, int mode) {
		random(l1, l1.getSize());
		random(l2, l2.getSize());
		// l1.clear(0xffffffff);
		// l2.clear(0xffffffff);
	}

	static void blendingBench(CPLayer l1, CPLayer l2, int iterations, boolean useFullAlpha) {
		double lastTime = System.currentTimeMillis(), newTime;
		CPRect size = l1.getSize();

		if (useFullAlpha) {
			for (int i = 0; i < iterations; i++) {
				l1.fusionWithFullAlpha(l2, size);
			}
		} else {
			for (int i = 0; i < iterations; i++) {
				l1.fusionWith(l2, size);
			}
		}
		newTime = System.currentTimeMillis();
		System.out.println("Result time: "
				+ String.valueOf((newTime - lastTime) / 1000.)
				+ "s "
				+ String.valueOf((newTime - lastTime) * 1000. / (iterations))
				+ "us per iteration "
				+ String.valueOf((newTime - lastTime) * 1000000.
						/ ((double) iterations * size.getWidth() * size.getHeight())) + "ns per pixel\n");
	}

	static public void random(CPLayer l, CPRect r) {
		int[] data = l.getData();

		CPRect rect = l.getSize();
		rect.clip(r);

		Random rnd = new Random();

		for (int j = rect.top; j < rect.bottom; j++) {
			for (int i = rect.left; i < rect.right; i++) {
				data[i + j * l.getWidth()] = rnd.nextInt();
			}
		}
	}

	void brushDabsBenchmark(String[] args) {
		int iterations = args.length > 1 ? Integer.valueOf(args[1]) : 10000;
		int sizeMin = args.length > 2 ? Integer.valueOf(args[2]) : 1;
		int sizeMax = args.length > 3 ? Integer.valueOf(args[3]) : 200;

		System.out.println("Benchmarking brush dabs size " + sizeMin + "-" + sizeMax + ", iterations=" + iterations
				+ "\n\n");

		System.out.println("Pixel Brush");
		System.out.println("===========");

		dabBench(CPBrushInfo.B_ROUND_PIXEL, false, iterations, sizeMin, sizeMax - sizeMin, false);

		System.out.println("Round AA Brush");
		System.out.println("==============");

		dabBench(CPBrushInfo.B_ROUND_AA, false, iterations, sizeMin, sizeMax - sizeMin, false);

		System.out.println("Round Soft Brush");
		System.out.println("================");

		dabBench(CPBrushInfo.B_ROUND_AIRBRUSH, false, iterations, sizeMin, sizeMax - sizeMin, false);

		System.out.println("\n== with a texture ==\n");

		System.out.println("Pixel Brush");
		System.out.println("===========");

		dabBench(CPBrushInfo.B_ROUND_PIXEL, false, iterations, sizeMin, sizeMax - sizeMin, true);

		System.out.println("Round AA Brush");
		System.out.println("==============");

		dabBench(CPBrushInfo.B_ROUND_AA, false, iterations, sizeMin, sizeMax - sizeMin, true);

		System.out.println("Round Soft Brush");
		System.out.println("================");

		dabBench(CPBrushInfo.B_ROUND_AIRBRUSH, false, iterations, sizeMin, sizeMax - sizeMin, true);

		System.out.println("\n== with AA ==\n");

		System.out.println("Pixel Brush");
		System.out.println("===========");

		dabBench(CPBrushInfo.B_ROUND_PIXEL, true, iterations, sizeMin, sizeMax - sizeMin, false);

		System.out.println("Round AA Brush");
		System.out.println("==============");

		dabBench(CPBrushInfo.B_ROUND_AA, true, iterations, sizeMin, sizeMax - sizeMin, false);

		System.out.println("Round Soft Brush");
		System.out.println("================");

		dabBench(CPBrushInfo.B_ROUND_AIRBRUSH, true, iterations, sizeMin, sizeMax - sizeMin, false);

		System.out.println("\n== with AA and a texture ==\n");

		System.out.println("Pixel Brush");
		System.out.println("===========");

		dabBench(CPBrushInfo.B_ROUND_PIXEL, true, iterations, sizeMin, sizeMax - sizeMin, true);

		System.out.println("Round AA Brush");
		System.out.println("==============");

		dabBench(CPBrushInfo.B_ROUND_AA, true, iterations, sizeMin, sizeMax - sizeMin, true);

		System.out.println("Round Soft Brush");
		System.out.println("================");

		dabBench(CPBrushInfo.B_ROUND_AIRBRUSH, true, iterations, sizeMin, sizeMax - sizeMin, true);
	}

	static void dabBench(int type, boolean useAA, int iterations, int sMin, int sDiff, boolean useTexture) {
		CPBrushManager manager = new CPBrushManager();
		CPBrushInfo brush = new CPBrushInfo();

		brush.type = type;
		brush.isAA = useAA;

		if (useTexture) {
			brush.texture = 1;

			int textureSize = 8;
			CPGreyBmp texture = new CPGreyBmp(textureSize, textureSize);
			for (int i = 0; i < textureSize * textureSize; i++) {
				texture.data[i] = (byte) (((i & 1) == 1) ? 255 : 0);
			}
			manager.setTexture(texture);
		}

		double lastTime = System.currentTimeMillis(), newTime;

		@SuppressWarnings("unused")
		CPBrushManager.CPBrushDab dab;

		for (int i = 0; i < iterations; i++) {
			brush.curSize = i % (sDiff + 1) + sMin;
			dab = manager.getDab(100.5f, 100.5f, brush);
		}

		newTime = System.currentTimeMillis();
		System.out.println("Result time: " + String.valueOf((newTime - lastTime) / 1000.) + "s "
				+ String.valueOf((newTime - lastTime) * 1000. / (iterations)) + "us per iteration\n");
	}

}
