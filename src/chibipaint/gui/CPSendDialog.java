package chibipaint.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
import javax.swing.WindowConstants;

public class CPSendDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JProgressBar progress;
	private JButton btnCancel;
	private JLabel lblStatus;

	/** URL to post the images to */
	private URL postUrl;

	/** Image data to send to server */
	private byte[] flatData, layersData, swatchData;

	private Component parent;

	private volatile boolean cancel;

	private ActionListener notifyCompleted;

	private boolean alreadyPosted;

	/**
	 * 
	 * @param parent
	 *            Parent for dialog in windowing hierachy
	 * @param notifyCompleted
	 *            Listener to notify if URLs need to be jumped to afterwards
	 * @param postUrl
	 *            URL to post image data to
	 * @param flatData
	 *            PNG image to post
	 * @param layersData
	 *            CHI image to post (or null if you don't want layers)
	 * @param alreadyPosted
	 *            True if the image has already been posted to the forum and we
	 *            shouldn't offer to "leave without posting"
	 */
	public CPSendDialog(Component parent, ActionListener notifyCompleted, URL postUrl, byte[] flatData,
			byte[] layersData, byte[] swatchData, boolean alreadyPosted) {
		this.postUrl = postUrl;
		this.flatData = flatData;
		this.layersData = layersData;
		this.swatchData = swatchData;
		this.parent = parent;
		this.notifyCompleted = notifyCompleted;
		this.alreadyPosted = alreadyPosted;

		setTitle("Saving oekaki...");

		Container contentPane = getContentPane();

		contentPane.setLayout(new BorderLayout());

		JPanel mainPane = new JPanel();
		{
			mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
			mainPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

			lblStatus = new JLabel();
			{
				lblStatus.setText("Contacting server...");
				lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
			}
			mainPane.add(lblStatus);
			mainPane.add(Box.createVerticalGlue());

			progress = new JProgressBar();
			{
				progress.setIndeterminate(true);
			}
			mainPane.add(progress);
			mainPane.add(Box.createVerticalGlue());
			mainPane.setPreferredSize(new Dimension(400, 80));
		}
		contentPane.add(mainPane, BorderLayout.CENTER);

		JPanel buttonPane = new JPanel();
		{
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

			btnCancel = new JButton("Cancel");
			{
				btnCancel.addActionListener(this);
			}
			buttonPane.add(btnCancel);
		}
		contentPane.add(buttonPane, BorderLayout.SOUTH);

		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				btnCancel.doClick();
			}
		});
	}

	/*
	 * Start a (non-blocking) send of the image to the server. Call from the
	 * Swing event thread.
	 */
	public void sendImage() throws IOException {
		/*
		 * Do the quick parts in this thread (preparing the message to be sent)
		 */
		pack();
		setLocationRelativeTo(parent);

		setVisible(true);

		/* Render our request into a buffer */
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream bos = new DataOutputStream(buffer);

		final String boundary = "---------------------------309542943615284";

		bos.writeBytes("--" + boundary + "\r\n");
		bos.writeBytes("Content-Disposition: form-data; name=\"picture\"; filename=\"chibipaint.png\"\r\n");
		bos.writeBytes("Content-Type: image/png\r\n\r\n");
		bos.write(flatData, 0, flatData.length);
		bos.writeBytes("\r\n");

		if (layersData != null) {
			bos.writeBytes("--" + boundary + "\r\n");
			bos.writeBytes("Content-Disposition: form-data; name=\"chibifile\"; filename=\"chibipaint.chi\"\r\n");
			bos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
			bos.write(layersData, 0, layersData.length);
			bos.writeBytes("\r\n");
		}

		//Swatches
		{
			bos.writeBytes("--" + boundary + "\r\n");
			bos.writeBytes("Content-Disposition: form-data; name=\"swatches\"; filename=\"chibipaint.aco\"\r\n");
			bos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
			bos.write(swatchData, 0, swatchData.length);
			bos.writeBytes("\r\n");
		}
		
		bos.writeBytes("--" + boundary + "--\r\n");

		bos.flush();

		final byte[] data = buffer.toByteArray();
		buffer = null;

		/*
		 * Now do the parts that might block (server communication) in a child
		 * thread so that we don't block the GUI.
		 */
		new Thread(new Runnable() {
			public void run() {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							progress.setMinimum(0);
							progress.setMaximum(data.length);
							progress.setValue(0);
							progress.setIndeterminate(false);
							lblStatus.setText("Saving drawing...");
						}
					});

					HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();

					connection.setDoOutput(true);
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Content-Type", "multipart/form-data, boundary=" + boundary);
					connection.setRequestProperty("Content-Length", Integer.toString(data.length));
					connection.setRequestProperty("User-Agent", "ChibiPaint Oekaki (" + System.getProperty("os.name")
							+ "; " + System.getProperty("os.version") + ")");

					DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
					try {
						/*
						 * Now the upload of the request body begins.
						 * 
						 * Write the data in chunks so we can give a progress
						 * bar
						 */
						final int CHUNK_SIZE = 8 * 1024;

						for (int chunk_pos = 0; chunk_pos < data.length; chunk_pos += CHUNK_SIZE) {
							if (cancel)
								break;

							/* Last chunk can be smaller than the others */
							int this_chunk = Math.min(data.length - chunk_pos, CHUNK_SIZE);

							dos.write(data, chunk_pos, this_chunk);
							dos.flush();

							final int progress_pos = chunk_pos + this_chunk;

							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									lblStatus.setText("Saving drawing... (" + (progress_pos / 1024) + "kB/"
											+ (data.length / 1024) + "kB done)");
									progress.setValue(progress_pos);
								}
							});

						}
					} finally {
						dos.close();
					}

					if (cancel) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								progress.setVisible(false);
								lblStatus.setText("Save cancelled");
								btnCancel.setText("Close");
								btnCancel.setActionCommand("close");
							}
						});
						return;
					}

					/*
					 * Read the answer from the server and verifies it's OK
					 */

					BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
					try {
						String line;
						String response = "";
						while ((line = rd.readLine()) != null && line.length() > 0) {
							response += line + "\n";
						}

						if (!response.startsWith("CHIBIOK")) {
							throw new RuntimeException("Unexpected answer from the server: " + response);
						}

						// Sweet, it saved!
						if (alreadyPosted) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									setVisible(false);
									Object[] options = { "Yes, view the post", "No, keep drawing" };
									int chosen = JOptionPane.showOptionDialog(parent,
											"Your oekaki has been saved, would you like to view it on the forum now?",
											"Oekaki saved", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
											null, options, options[0]);
									switch (chosen) {
									case JOptionPane.YES_OPTION:
										notifyCompleted.actionPerformed(new ActionEvent(this, 0, "CPPost"));
										break;
									case JOptionPane.NO_OPTION:
									case JOptionPane.CLOSED_OPTION:
										notifyCompleted.actionPerformed(new ActionEvent(this, 0, "CPSaved"));
									}
								}
							});
						} else {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									setVisible(false);
									Object[] options = { "Yes, post it now", "No, keep drawing",
											"No, I'll finish it later" };
									int chosen = JOptionPane.showOptionDialog(parent,
											"Your oekaki has been saved, would you like to post it to the forum now?",
											"Oekaki saved", JOptionPane.YES_NO_CANCEL_OPTION,
											JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
									switch (chosen) {
									case JOptionPane.YES_OPTION:
										notifyCompleted.actionPerformed(new ActionEvent(this, 0, "CPPost"));
										break;
									case JOptionPane.CANCEL_OPTION:
										notifyCompleted.actionPerformed(new ActionEvent(this, 0, "CPExit"));
										break;
									case JOptionPane.NO_OPTION:
									case JOptionPane.CLOSED_OPTION:
										notifyCompleted.actionPerformed(new ActionEvent(this, 0, "CPSaved"));
									}
								}
							});
						}
					} finally {
						rd.close();
					}
				} catch (final Exception e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							progress.setIndeterminate(false);
							progress.setValue(0);
							lblStatus.setText("<html>Error, your drawing has not been saved!</html>");

							JOptionPane.showMessageDialog(CPSendDialog.this,
									"Error, your drawing has not been saved.\n\n" + e.getMessage(), "Error",
									JOptionPane.ERROR_MESSAGE);

							btnCancel.setActionCommand("close");
							btnCancel.setText("Close");
						}
					});
				}

			}
		}).start();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnCancel) {
			if (btnCancel.getActionCommand().equals("close")) {
				setVisible(false);
			} else {
				cancel = true;
			}
		}
	}
}
