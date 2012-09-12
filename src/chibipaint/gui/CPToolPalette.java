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
import java.awt.event.*;

import javax.swing.ToolTipManager;

import chibipaint.*;

public class CPToolPalette extends CPPalette implements ActionListener {

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(85, 325);
	}
	
	public CPToolPalette(CPController controller) {
		super(controller);

		setPreferredSize(new Dimension(85, 325));
		ToolTipManager.sharedInstance().setInitialDelay(600);

		title = "Tools";
		setLayout(new WrapLayout());

		Image icons = controller.loadImage("icons.png");
		CPIconButton button;

		button = new CPIconButton(icons, 32, 32, 0, 1, "Marquee");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPRectSelection");

		button = new CPIconButton(icons, 32, 32, 1, 1, "Move tool");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPMoveTool");

		button = new CPIconButton(icons, 32, 32, 2, 1, "Flood fill");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPFloodFill");

		button = new CPIconButton(icons, 32, 32, 29, 1, "Rotate canvas");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPRotateCanvas");
		button.setCPActionCommandDouble("CPResetCanvasRotation");

		button = new CPIconButton(icons, 32, 32, 5, 1, "Pencil");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPPencil");

		button = new CPIconButton(icons, 32, 32, 6, 1, "");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPPen");
		button.setSelected(true);

		button = new CPIconButton(icons, 32, 32, 7, 1, "Airbrush");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPAirbrush");

		button = new CPIconButton(icons, 32, 32, 18, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPWater");

		button = new CPIconButton(icons, 32, 32, 8, 1, "Eraser");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPEraser");

		button = new CPIconButton(icons, 32, 32, 9, 1, "Soft eraser");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPSoftEraser");

		button = new CPIconButton(icons, 32, 32, 24, 1, "Smudge");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPSmudge");

		button = new CPIconButton(icons, 32, 32, 28, 1, "");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPBlender");

		button = new CPIconButton(icons, 32, 32, 16, 1, "");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPDodge");

		button = new CPIconButton(icons, 32, 32, 17, 1, "");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPBurn");

		button = new CPIconButton(icons, 32, 32, 23, 1, "Blur");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPBlur");

		button = new CPIconButton(icons, 32, 32, 30, 1, "Color picker");
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPColorPicker");
	}

	public void actionPerformed(ActionEvent e) {
		Component[] components = getComponents();
		for (Component c : components) {
			if (c != e.getSource()) {
				((CPIconButton) c).setSelected(false);
			}
		}

		((CPIconButton) e.getSource()).setSelected(true);
	}
}
