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

public class CPControllerApplet extends CPController {

	private ChibiPaint applet;
	private JFrame floatingFrame;
	private String postUrl, exitUrl, exitUrlTarget;

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
			if (sendPng()) {
				goToExitUrl();
			}
		}

		super.actionPerformed(e);
	}

	public boolean sendPng() {
		String response = "";

		// First creates the PNG data
		byte[] pngData = getPngData(canvas.img); // FIXME: verify canvas.img is always updated

		// The ChibiPaint file data
		ByteArrayOutputStream chibiFileStream = new ByteArrayOutputStream(1024);
		CPChibiFile.write(chibiFileStream, artwork);
		byte[] chibiData = chibiFileStream.toByteArray();

		boolean sendLayers;
		int choice = JOptionPane
				.showConfirmDialog(
						getDialogParent(),
						"You're about to send your oekaki to the server and end your ChibiPaint session.\n\nWould you like to send the layers file as well?\nAdditional upload size: "
								+ chibiData.length
								/ 1024
								+ " KB \nTotal upload size:"
								+ (chibiData.length + pngData.length)
								/ 1024
								+ " KB\n\nThe layers file allows you to edit your oekaki later with all its layers intact\n\n"
								+ "Choose 'Yes' to send both files. (recommended)\n"
								+ "Choose 'No' to send the finished picture only.\n"
								+ "Choose 'Cancel' if you wish to continue editing your picture without sending it.\n\n",
						"Send Oekaki", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

		if (choice == JOptionPane.YES_OPTION) {
			sendLayers = true;
		} else if (choice == JOptionPane.NO_OPTION) {
			sendLayers = false;
		} else {
			return false;
		}

		try {
			URL url = new URL(applet.getDocumentBase(), postUrl);
			// new CPMessageBox(this, CPMessageBox.CP_OK_MSGBOX, url.toString()+" / "+url.getHost()+" / "+url.getFile(),
			// "debug");

			String boundary = "---------------------------309542943615284";
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			DataOutputStream bos = new DataOutputStream(buffer);

			bos.writeBytes("--" + boundary + "\r\n");
			bos.writeBytes("Content-Disposition: form-data; name=\"picture\"; filename=\"chibipaint.png\"\r\n");
			bos.writeBytes("Content-Type: image/png\r\n\r\n");
			bos.write(pngData, 0, pngData.length);
			bos.writeBytes("\r\n");

			if (sendLayers) {
				bos.writeBytes("--" + boundary + "\r\n");
				bos.writeBytes("Content-Disposition: form-data; name=\"chibifile\"; filename=\"chibipaint.chi\"\r\n");
				bos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
				bos.write(chibiData, 0, chibiData.length);
				bos.writeBytes("\r\n");
			}
			bos.writeBytes("--" + boundary + "--\r\n");

			bos.flush();

			byte[] data = buffer.toByteArray();

			int port = url.getPort();
			if (port < 0) {
				port = 80;
			}
			Socket s = new Socket(url.getHost(), port);
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

			dos.writeBytes("POST " + url.getFile() + " HTTP/1.0\r\n");
			dos.writeBytes("Host: " + url.getHost() + "\r\n");
			dos.writeBytes("User-Agent: ChibiPaint Oekaki (" + System.getProperty("os.name") + "; "
					+ System.getProperty("os.version") + ")\r\n");
			dos.writeBytes("Cache-Control: nocache\r\n");
			dos.writeBytes("Content-Type: multipart/form-data; boundary=" + boundary + "\r\n");
			dos.writeBytes("Content-Length: " + data.length + "\r\n");
			dos.writeBytes("\r\n");

			dos.write(data, 0, data.length);
			dos.flush();

			//
			// Read the answer from the server and verifies it's OK

			BufferedReader rd = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
			String line;
			while ((line = rd.readLine()) != null && line.length() > 0) {
				response += line + "\n";
			}

			line = rd.readLine(); // should be our answer
			if (!line.startsWith("CHIBIOK")) {
				throw new Exception("Error: Unexpected answer from the server");
			} else {
				response += line + "\n";
			}

			dos.close();
			rd.close();

			return true;
		} catch (Exception e) {
			System.out.print("Error while sending the oekaki..." + e.getMessage() + "\n");
			JOptionPane.showMessageDialog(getDialogParent(), "Error while sending the oekaki..." + e.getMessage()
					+ response, "Error", JOptionPane.ERROR_MESSAGE);
			// new CPMessageBox(this, CPMessageBox.CP_OK_MSGBOX, "Error while sending the
			// oekaki..."+e.getMessage()+response, "Error");
			return false;
		}
	}

	public void goToExitUrl() {
		if (exitUrl != null && !exitUrl.equals("")) {
			try {
				applet.getAppletContext().showDocument(new URL(applet.getDocumentBase(), exitUrl), exitUrlTarget);
			} catch (Exception e) {
				// FIXME: do something
			}
		} else {
			JOptionPane.showMessageDialog(getDialogParent(), "The oekaki was successfully sent", "Send Oekaki",
					JOptionPane.INFORMATION_MESSAGE);
			// new CPMessageBox(this, CPMessageBox.CP_OK_MSGBOX, "The oekaki was successfully sent", "Send Oekaki");
		}
	}
}
