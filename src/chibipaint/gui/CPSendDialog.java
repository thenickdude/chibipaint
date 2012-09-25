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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
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

import chibipaint.engine.MultipartRequest;

public class CPSendDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JProgressBar barProgress;
	private JButton btnCancel;
	private JLabel lblStatus;

	/** URL to post the images to */
	private URL postUrl;

	/** Request to send to server */
	private MultipartRequest postData;

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
	public CPSendDialog(Component parent, ActionListener notifyCompleted, URL postUrl, MultipartRequest postData,
			boolean alreadyPosted) {
		this.postUrl = postUrl;
		this.postData = postData;
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

			barProgress = new JProgressBar();
			{
				barProgress.setIndeterminate(true);
			}
			mainPane.add(barProgress);
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
		pack();
		setLocationRelativeTo(parent);

		setVisible(true);
		
		/*
		 * Do the parts that might block (server communication) in a child
		 * thread so that we don't block the GUI.
		 */
		new Thread(new Runnable() {
			public void run() {
				try {
					HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
					connection.setDoInput(true);
					connection.setDoOutput(true);
					connection.setUseCaches(false);
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Connection", "close");
					connection.setRequestProperty("Content-Type", "multipart/form-data, boundary=" + postData.getBoundary());
					final int requestLength = postData.getRequestLength();
					connection.setRequestProperty("Content-Length", Integer.toString(requestLength));
					connection.setRequestProperty("User-Agent", "ChibiPaint Oekaki (" + System.getProperty("os.name")
							+ "; " + System.getProperty("os.version") + ")");
					
					boolean haveProgress = false;
					
					try {
						//This method only available from JVM 1.5, so check if it is available so we can skip it on 1.4:
						Method setFixedLengthStreamingMode = HttpURLConnection.class.getMethod("setFixedLengthStreamingMode", int.class);
						setFixedLengthStreamingMode.invoke(connection, requestLength);
						haveProgress = true;
					} catch (Throwable e) {
						e.printStackTrace();
					}
					
					final boolean finalHaveProgress = haveProgress;
					
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							barProgress.setMinimum(0);
							barProgress.setMaximum(requestLength);
							barProgress.setValue(0);
							barProgress.setIndeterminate(!finalHaveProgress);
							lblStatus.setText("Saving drawing, please wait...");
						}
					});

					OutputStream outputStream = connection.getOutputStream();
					try {
						/*
						 * Now the upload of the request body begins.
						 * 
						 * Write the data in chunks so we can give a progress
						 * bar
						 */
						final int CHUNK_SIZE = haveProgress ? 16 * 1024 : 64 * 1024;
						byte[] chunk = new byte[CHUNK_SIZE];
						int bytesRead, progress = 0;
						
						InputStream postDataStream = postData.getInputStream();
						
						while ((bytesRead = postDataStream.read(chunk)) != -1) {
							if (cancel)
								break;

							outputStream.write(chunk, 0, bytesRead);
							
							progress += bytesRead;

							if (haveProgress) {
								final int final_progress = progress;
								outputStream.flush();

								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										lblStatus.setText("Saving drawing... (" + (final_progress / 1024) + "kB/"
												+ (requestLength / 1024) + "kB done)");
										barProgress.setValue(final_progress);
									}
								});
							}
						}
					} finally {
						outputStream.flush();
						outputStream.close();
					}

					if (cancel) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								barProgress.setVisible(false);
								lblStatus.setText("Save cancelled");
								btnCancel.setText("Close");
								btnCancel.setActionCommand("close");
							}
						});
						return;
					}

					
					System.out.println("Reading response from server...");
					
					/*
					 * Read the answer from the server and verifies it's OK
					 */

					BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
					try {
						String line;
						String response = "";
						while ((line = rd.readLine()) != null) {
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
							barProgress.setIndeterminate(false);
							barProgress.setValue(0);
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
