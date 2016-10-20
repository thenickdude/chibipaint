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

public class CPBezier {

	// How to use this class:
	//
	// 1 - set the 4 points coordinates (x0-3, y0-3)
	// two options:
	// 2a - call init() with desired dt then read the current coordinate (Bx, By) and use nextPoint() to compute the
	// next point
	// 2b - use one of the "compute" methods to compute the values for the whole curve in one step

	// The 4 points coordinates
	public float x0, y0;
	public float x1, y1;
	public float x2, y2;
	public float x3, y3;

	// used to compute the Bezier curve with the forward differences method
	private double Bx, dBx, ddBx, dddBx;
	private double By, dBy, ddBy, dddBy;

	public void init(double dt) {
		// Implements a fast degree-3 Bezier curve using the forward differences method
		//
		// Reference for this algorithm:
		// "Curves and Surfaces for Computer Graphics" by David Salomon, page 189

		double q1 = 3. * dt;
		double q2 = q1 * dt;
		double q3 = dt * dt * dt;
		double q4 = 2. * q2;
		double q5 = 6. * q3;
		double q6x = x0 - 2. * x1 + x2;
		double q6y = y0 - 2. * y1 + y2;
		double q7x = 3. * (x1 - x2) - x0 + x3;
		double q7y = 3. * (y1 - y2) - y0 + y3;

		Bx = x0;
		By = y0;

		dBx = (x1 - x0) * q1 + q6x * q2 + q7x * q3;
		dBy = (y1 - y0) * q1 + q6y * q2 + q7y * q3;

		ddBx = q6x * q4 + q7x * q5;
		ddBy = q6y * q4 + q7y * q5;

		dddBx = q7x * q5;
		dddBy = q7y * q5;
	}

	public void nextPoint() {
		Bx += dBx;
		By += dBy;
		dBx += ddBx;
		dBy += ddBy;
		ddBx += dddBx;
		ddBy += dddBy;
	}

	public void compute(int x[], int y[], int elements) {
		init(1. / elements);

		x[0] = (int) Bx;
		y[0] = (int) By;
		for (int i = 1; i < elements; i++) {
			Bx += dBx;
			By += dBy;
			dBx += ddBx;
			dBy += ddBy;
			ddBx += dddBx;
			ddBy += dddBy;

			x[i] = (int) Bx;
			y[i] = (int) By;
		}
	}

	public void compute(float x[], float y[], int elements) {
		init(1. / elements);

		x[0] = (float) Bx;
		y[0] = (float) By;
		for (int i = 1; i < elements; i++) {
			Bx += dBx;
			By += dBy;
			dBx += ddBx;
			dBy += ddBy;
			ddBx += dddBx;
			ddBy += dddBy;

			x[i] = (float) Bx;
			y[i] = (float) By;
		}
	}

}
