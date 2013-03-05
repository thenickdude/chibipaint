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

package chibiapp;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import chibipaint.CPController;
import chibipaint.engine.CPChibiFile;

public class CPControllerApplication extends CPController {

	JFrame mainFrame;

	public CPControllerApplication(JFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("CPSave")) {
			saveOekaki();
		}

		super.actionPerformed(e);
	}

	public void saveOekaki() {
		try {
			int rotation = (int) Math.round(canvas.getRotation() / Math.PI * 2);

			// Just in case:
			rotation %= 4;

			// We want [0..3] as output
			if (rotation < 0)
				rotation += 4;

			JFileChooser saveDialog = new JFileChooser();

			saveDialog.setDialogTitle("Save flat drawing (PNG)");
			
			int returnVal = saveDialog.showSaveDialog(mainFrame);

			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}

			byte[] png = getImageAsPNG(canvas.img, rotation);

			OutputStream out = new FileOutputStream(saveDialog.getSelectedFile());
			try {
				out.write(png);
				png = null;
			} finally {
				out.close();
			}

			if (!isSimpleDrawing()) {
				saveDialog.setDialogTitle("Save layers file (CHI)");
				
				returnVal = saveDialog.showSaveDialog(mainFrame);

				if (returnVal != JFileChooser.APPROVE_OPTION) {
					return;
				}

				out = new BufferedOutputStream(new FileOutputStream(saveDialog.getSelectedFile()));
				try {
					CPChibiFile.write(out, artwork);
				} finally {
					out.close();
				}
			}
		} catch (Exception e) {
			ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
			PrintWriter writer = new PrintWriter(errBuf);
			
			e.printStackTrace(writer);
			
			writer.close();
			
			JOptionPane.showMessageDialog(mainFrame, errBuf.toString());
		}
	}

	public Component getDialogParent() {
		return mainFrame;
	}
}
