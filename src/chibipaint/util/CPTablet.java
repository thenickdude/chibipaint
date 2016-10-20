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

import java.lang.reflect.*;

public class CPTablet {

	private static CPTablet ref;

	public static CPTablet getRef() {
		if (ref == null) {
			ref = new CPTablet();
		}
		return ref;
	}

	boolean tabletOK = false;
	Object tablet;
	Method mPoll, mGetPressure, mGetPressureExtent;

	int pressure = 0, pressureExtent = 1;

	public CPTablet() {
		try {
			Class tabletClass = Class.forName("cello.tablet.JTablet");
			tablet = tabletClass.newInstance();

			mPoll = tabletClass.getMethod("poll", (Class[]) null);
			mGetPressure = tabletClass.getMethod("getPressure", (Class[]) null);
			mGetPressureExtent = tabletClass.getMethod("getPressureExtent", (Class[]) null);
			// tablet_getAngle = jtablet.getMethod("getAngle",null);
			//
			// tablet_getOrientation = jtablet.getMethod("getOrientation",null);
			// tablet_getButtons = jtablet.getMethod("getButtons",null);

			tabletOK = true;
		} catch (Exception e) {
			System.out.print(e.toString());
		}
	}

	private void getTabletInfo() {
		if (tabletOK) {
			try {
				if (((Boolean) mPoll.invoke(tablet, (Object[]) null)).booleanValue()) {
					pressure = ((Integer) mGetPressure.invoke(tablet, (Object[]) null)).intValue();
					pressureExtent = ((Integer) mGetPressureExtent.invoke(tablet, (Object[]) null)).intValue();
				}
			} catch (Exception e) {
				System.out.print(e.toString());
			}
		}
	}

	public float getPressure() {
		getTabletInfo();
		if (!tabletOK) {
			return 1.f;
		} else {
			return (float) pressure / pressureExtent;
		}
	}

	public void mouseDetect() {
		pressure = pressureExtent = 1;
		getTabletInfo();
	}
}
