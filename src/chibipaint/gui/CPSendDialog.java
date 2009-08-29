package chibipaint.gui;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import chibipaint.ChibiPaint;

public class CPSendDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JProgressBar progress = new JProgressBar();
	private JButton btnCancel = new JButton("Cancel");
	private JLabel lblStatus = new JLabel();
	private JPanel pnlBottom = new JPanel();
	
	/** URL to post the images to */
	private URL postUrl;

	/** Send the .chi file along with the png? */
	private boolean sendLayers;

	/** Image data to send to server */
	private byte[] pngData, chibiData;

	public CPSendDialog(URL postUrl, byte[] pngData, byte[] chibiData) {
		this.postUrl = postUrl;
		this.pngData = pngData;
		this.chibiData = chibiData;

		progress.setIndeterminate(true);
		lblStatus.setText("Contacting server...");
		
		pnlBottom.add(progress);	
	}

	/*
	 * Start a (non-blocking) send of the image to the server. Call from the
	 * Swing event thread.
	 */
	public void sendImage() throws IOException {

		/*
		 * Do the quick parts in this thread (preparing the message to be sent)
		 */
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		getContentPane().add(lblStatus);
		getContentPane().add(progress);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(btnCancel);

		getContentPane().add(buttonPane);
		
		setMinimumSize(new Dimension(600,400));
		pack();
		setVisible(true);
		
		/* Render our request into a buffer */
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream bos = new DataOutputStream(buffer);

		final String boundary = "---------------------------309542943615284";

		bos.writeBytes("--" + boundary + "\r\n");
		bos
				.writeBytes("Content-Disposition: form-data; name=\"picture\"; filename=\"chibipaint.png\"\r\n");
		bos.writeBytes("Content-Type: image/png\r\n\r\n");
		bos.write(pngData, 0, pngData.length);
		bos.writeBytes("\r\n");

		if (sendLayers) {
			bos.writeBytes("--" + boundary + "\r\n");
			bos
					.writeBytes("Content-Disposition: form-data; name=\"chibifile\"; filename=\"chibipaint.chi\"\r\n");
			bos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
			bos.write(chibiData, 0, chibiData.length);
			bos.writeBytes("\r\n");
		}
		bos.writeBytes("--" + boundary + "--\r\n");

		bos.flush();

		final byte[] data = buffer.toByteArray();

		/*
		 * Now do the parts that might block (server communication) in a child
		 * thread so that we don't block the GUI.
		 */

		new Thread(new Runnable() {

			public void run() {
				try {
					int port = postUrl.getPort();
					if (port < 0) {
						port = 80;
					}
					Socket s = new Socket(postUrl.getHost(), port);

					DataOutputStream dos = new DataOutputStream(
							new BufferedOutputStream(s.getOutputStream()));

					dos.writeBytes("POST " + postUrl.getFile()
							+ " HTTP/1.0\r\n");
					dos.writeBytes("Host: " + postUrl.getHost() + "\r\n");
					dos.writeBytes("User-Agent: ChibiPaint Oekaki ("
							+ System.getProperty("os.name") + "; "
							+ System.getProperty("os.version") + ")\r\n");
					dos.writeBytes("Cache-Control: nocache\r\n");
					dos
							.writeBytes("Content-Type: multipart/form-data; boundary="
									+ boundary + "\r\n");
					dos.writeBytes("Content-Length: " + data.length + "\r\n");
					dos.writeBytes("\r\n");

					/* Now the upload of the request body begins */
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							progress.setMinimum(0);
							progress.setMaximum(data.length);
							progress.setValue(0);
							progress.setIndeterminate(false);
							lblStatus.setText("Sending drawing...");
						}
					});

					/* Write the data in chunks so we can give a progress bar */

					final int CHUNK_SIZE = 8 * 1024;

					for (int chunk_pos = 0; chunk_pos < data.length; chunk_pos += CHUNK_SIZE) {
						/* Last chunk can be smaller than the others */
						int this_chunk = Math.min(data.length - chunk_pos,
								CHUNK_SIZE);

						dos.write(data, chunk_pos, this_chunk);
						dos.flush();

						final int progress_pos = chunk_pos + this_chunk;

						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								progress.setValue(progress_pos);
							}
						});

					}

					//
					// Read the answer from the server and verifies it's OK

					BufferedReader rd = new BufferedReader(
							new InputStreamReader(s.getInputStream(), "UTF-8"));
					String line;
					String response = "";
					while ((line = rd.readLine()) != null && line.length() > 0) {
						response += line + "\n";
					}

					line = rd.readLine(); // should be our answer
					if (!line.startsWith("CHIBIOK")) {
						throw new Exception(
								"Error: Unexpected answer from the server");
					} else {
						response += line + "\n";
					}

					dos.close();
					rd.close();
				} catch (Exception e) {
					System.out.print("Error while sending the oekaki..."
							+ e.getMessage() + "\n");
					/*
					 * JOptionPane.showMessageDialog(getDialogParent(),
					 * "Error while sending the oekaki..." + e.getMessage() +
					 * response, "Error", JOptionPane.ERROR_MESSAGE);
					 */
				}
			}
		}).start();
	}

}
