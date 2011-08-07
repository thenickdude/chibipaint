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

	/**
	 * Send the oekaki to the server
	 */
	public void sendOekaki() {

		try {
			byte[] pngData = getPngData(canvas.img);
			// FIXME: verify canvas.img is
			// always updated

			byte[] chibiData, swatchData;

			ByteArrayOutputStream bufferStream;

			if (artwork.getLayers().length > 1) {
				bufferStream = new ByteArrayOutputStream(1024);

				CPChibiFile.write(bufferStream, artwork);
				chibiData = bufferStream.toByteArray();
			} else {
				// Don't need to send multiple layers if we only have one
				chibiData = null;
			}

			CPSwatchesPalette swatchesPal = chibipaint.mainGUI.getSwatchesPalette();

			bufferStream = new ByteArrayOutputStream(1024);

			int[] swatches = swatchesPal.getSwatches();
			AdobeColorTable.write(bufferStream, swatches);

			swatchData = bufferStream.toByteArray();

			CPSendDialog sendDialog = new CPSendDialog(chibipaint.mainGUI.getGUI(), this, new URL(applet.getCodeBase(),
					postUrl), pngData, chibiData, swatchData, exitUrl == null || exitUrl.length() == 0);

			sendDialog.sendImage();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(getDialogParent(), "Error while sending the oekaki..." + e.getMessage(),
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
