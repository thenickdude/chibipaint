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
import chibipaint.gui.CPSwatchesPalette;

public class CPControllerApplet extends CPController {

	private ChibiApplet chibipaint;
	private JFrame floatingFrame;

	/** URL to send our image data to */
	private String postUrl;
	/** Where to go if we decide to "post" our oekaki */
	private String postedUrl, postedUrlTarget;
	/** Where to go if we decide to stop drawing */
	private String exitUrl, exitUrlTarget;

	private Applet applet;

	public CPControllerApplet(ChibiApplet chibipaint, Applet applet) {
		this.chibipaint = chibipaint;
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
		postedUrl = applet.getParameter("postedUrl");
		postedUrlTarget = applet.getParameter("postedUrlTarget");
		exitUrl = applet.getParameter("exitUrl");
		exitUrlTarget = applet.getParameter("exitUrlTarget");
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("CPFloat")) {
			chibipaint.floatingMode();
		} else if (e.getActionCommand().equals("CPSend")) {
			sendOekaki();
		} else if (e.getActionCommand().equals("CPPost")) {
			setHasUnsavedChanges(false);
			goToPostedUrl();
		} else if (e.getActionCommand().equals("CPExit")) {
			setHasUnsavedChanges(false);
			goToExitUrl();
		} else if (e.getActionCommand().equals("CPSaved")) {
			setHasUnsavedChanges(false);
		}

		super.actionPerformed(e);
	}

	private static String renderThrowable(Throwable aThrowable) {
		StringWriter result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}
	
	/**
	 * Send the oekaki to the server
	 */
	public void sendOekaki() {

		try {
			// FIXME: verify canvas.img is
			// always updated

			ByteArrayOutputStream bufferStream;

			int rotation = (int) Math.round(canvas.getRotation() / Math.PI * 2);

			//Just in case:
			rotation %= 4;
			
			//We want [0..3] as output
			if (rotation < 0)
				rotation += 4;
			
			MultipartRequest request = new MultipartRequest();
			
			request.addPart("picture", "chibipaint.png", "image/png", getImageAsPNG(canvas.img, rotation));

			if (!isSimpleDrawing()) {
				// We must send layers
				bufferStream = new ByteArrayOutputStream(1024);

				CPChibiFile.write(bufferStream, artwork);
				
				request.addPart("chibifile", "chibipaint.chi", "application/octet-stream", bufferStream.toByteArray());

				bufferStream = null;
				
				if (rotation != 0) {
					//Oekaki will have to rotate the layers on load when we edit this drawing
					request.addPart("rotation", null, "text/plain", Integer.toString(rotation).getBytes());
				}
			}

			CPSwatchesPalette swatchesPal = chibipaint.mainGUI.getSwatchesPalette();

			if (swatchesPal.isModified()) {
				bufferStream = new ByteArrayOutputStream(1024);
				int[] swatches = swatchesPal.getSwatches();
				AdobeColorTable.write(bufferStream, swatches);
				
				request.addPart("swatches", "chibipaint.aco", "application/octet-stream", bufferStream.toByteArray());
				
				bufferStream = null;
			}

			CPSendDialog sendDialog = new CPSendDialog(chibipaint.mainGUI.getGUI(), this, new URL(applet.getCodeBase(),
					postUrl), request, exitUrl == null || exitUrl.length() == 0);

			sendDialog.sendImage();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(getDialogParent(), "Error while sending the oekaki, your drawing has not been saved!\n" + renderThrowable(e),
					"Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	public void goToExitUrl() {
		if (exitUrl != null && exitUrl.length() > 0) {
			try {
				if (exitUrlTarget != null && exitUrlTarget.length() > 0)
					applet.getAppletContext().showDocument(new URL(applet.getDocumentBase(), exitUrl), exitUrlTarget);
				else
					applet.getAppletContext().showDocument(new URL(applet.getDocumentBase(), exitUrl));
			} catch (Exception e) {
				// FIXME: do something
			}
		}
	}

	public void goToPostedUrl() {
		if (postedUrl != null && postedUrl.length() > 0) {
			try {
				if (postedUrlTarget != null && postedUrlTarget.length() > 0)
					applet.getAppletContext().showDocument(new URL(applet.getCodeBase(), postedUrl), postedUrlTarget);
				else
					applet.getAppletContext().showDocument(new URL(applet.getCodeBase(), postedUrl));
			} catch (Exception e) {
				// FIXME: do something
			}
		}
	}
}
