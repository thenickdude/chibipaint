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

import chibipaint.*;
import chibipaint.engine.*;

public class CPStrokePalette extends CPPalette implements ActionListener, CPController.ICPToolListener {

	CPIconButton freeHandButton, lineButton, bezierButton;

	public CPStrokePalette(CPController controller) {
		super(controller);

		title = "Stroke";
		setLayout(new FlowLayout());

		Image icons = controller.loadImage("icons.png");

		freeHandButton = new CPIconButton(icons, 32, 32, 19, 1);
		add(freeHandButton);

		freeHandButton.addCPActionListener(controller);
		freeHandButton.addCPActionListener(this);
		freeHandButton.setCPActionCommand("CPFreeHand");
		freeHandButton.setSelected(true);

		lineButton = new CPIconButton(icons, 32, 32, 20, 1);
		add(lineButton);

		lineButton.addCPActionListener(controller);
		lineButton.addCPActionListener(this);
		lineButton.setCPActionCommand("CPLine");

		bezierButton = new CPIconButton(icons, 32, 32, 22, 1);
		add(bezierButton);

		bezierButton.addCPActionListener(controller);
		bezierButton.addCPActionListener(this);
		bezierButton.setCPActionCommand("CPBezier");

		controller.addToolListener(this);
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

	public void newTool(int tool, CPBrushInfo toolInfo) {
		freeHandButton.setSelected(toolInfo.strokeMode == CPBrushInfo.SM_FREEHAND);
		lineButton.setSelected(toolInfo.strokeMode == CPBrushInfo.SM_LINE);
		bezierButton.setSelected(toolInfo.strokeMode == CPBrushInfo.SM_BEZIER);
	}
}
