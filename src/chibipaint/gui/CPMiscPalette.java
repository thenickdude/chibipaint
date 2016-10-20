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

package chibipaint.gui;

import java.awt.*;

import javax.swing.*;

import chibipaint.*;

public class CPMiscPalette extends CPPalette {

	public CPMiscPalette(CPController controller) {
		super(controller);

		title = "Misc";
		setLayout(new FlowLayout());

		Image icons = controller.loadImage("icons.png");

		Component spacer;

		CPIconButton button = new CPIconButton(icons, 32, 32, 13, 1);
		add(button);
		button.addCPActionListener(controller);
		button.setCPActionCommand("CPZoomIn");

		button = new CPIconButton(icons, 32, 32, 14, 1);
		add(button);
		button.addCPActionListener(controller);
		button.setCPActionCommand("CPZoomOut");

		button = new CPIconButton(icons, 32, 32, 15, 1);
		add(button);
		button.addCPActionListener(controller);
		button.setCPActionCommand("CPZoom100");

		spacer = new JPanel();
		spacer.setSize(16, 32);
		add(spacer);

		button = new CPIconButton(icons, 32, 32, 10, 1);
		add(button);
		button.addCPActionListener(controller);
		button.setCPActionCommand("CPUndo");

		button = new CPIconButton(icons, 32, 32, 11, 1);
		add(button);
		button.addCPActionListener(controller);
		button.setCPActionCommand("CPRedo");

		if (controller.isRunningAsApplet()) {
			spacer = new JPanel();
			spacer.setSize(16, 32);
			add(spacer);

			button = new CPIconButton(icons, 32, 32, 12, 1);
			add(button);
			button.addCPActionListener(controller);
			button.setCPActionCommand("CPSend");
		}
	}

}
