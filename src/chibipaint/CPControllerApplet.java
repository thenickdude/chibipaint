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

package chibipaint;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import chibipaint.engine.*;
import chibipaint.gui.CPSendDialog;

public class CPControllerApplet extends CPController {

	private ChibiPaint applet;
	private JFrame floatingFrame;
	private String postUrl, exitUrl, exitUrlTarget;

	private boolean hasUnsavedChanges = true;

	public CPControllerApplet(ChibiPaint applet) {
		this.applet = applet;
		getAppletParams();
	}

	public Applet getApplet() {
		return applet;
	}

	public Component getDialogParent() {
		if (floatingFrame != null) {
			return floatingFrame;
		} else {
			return applet;
		}
	}

	/*
	 * public Frame getFloatingFrame() { return frame; }
	 */

	public void setFloatingFrame(JFrame floatingFrame) {
		this.floatingFrame = floatingFrame;
	}

	public void getAppletParams() {
		postUrl = applet.getParameter("postUrl");
		exitUrl = applet.getParameter("exitUrl");
		exitUrlTarget = applet.getParameter("exitUrlTarget");
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("CPFloat")) {
			applet.floatingMode();
		}

		if (e.getActionCommand().equals("CPSend")) {
			sendImage();
			/*
			 * if (sendPng()) { goToExitUrl(); }
			 */
		}

		super.actionPerformed(e);
	}

	public void sendImage() {
		// First creates the PNG data
		byte[] pngData = getPngData(canvas.img); // FIXME: verify canvas.img is
													// always updated

		// The ChibiPaint file data
		ByteArrayOutputStream chibiFileStream = new ByteArrayOutputStream(1024);
		CPChibiFile.write(chibiFileStream, artwork);
		byte[] chibiData = chibiFileStream.toByteArray();

		try {
			CPSendDialog sendDialog = new CPSendDialog(new URL(applet
					.getDocumentBase(), postUrl), pngData, chibiData);

			sendDialog.sendImage();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(getDialogParent(),
					"Error while sending the oekaki..." + e.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	public void goToExitUrl() {
		hasUnsavedChanges = false;
		if (exitUrl != null && !exitUrl.equals("")) {
			try {
				applet.getAppletContext().showDocument(
						new URL(applet.getDocumentBase(), exitUrl),
						exitUrlTarget);
			} catch (Exception e) {
				// FIXME: do something
			}
		} else {
			JOptionPane.showMessageDialog(getDialogParent(),
					"The oekaki was successfully sent", "Send Oekaki",
					JOptionPane.INFORMATION_MESSAGE);
			// new CPMessageBox(this, CPMessageBox.CP_OK_MSGBOX,
			// "The oekaki was successfully sent", "Send Oekaki");
		}
	}

	public boolean hasUnsavedChanges() {
		return hasUnsavedChanges;
	}
}
