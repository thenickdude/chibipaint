package test;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import javax.jnlp.ClipboardService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

public class Test extends JApplet {

	private static final String TEST_LAYERS = "Test layers";
	private static final String TEST_FLAT_IMAGE = "Test flat image";
	private URL testUrl;
	private JTextArea log;

	private boolean errors = false;
	private JButton btnCopy;

	public void logAppend(final boolean error, final String message) {
		if (error)
			errors = true;

		for (String line : message.split("\\n", 100)) {
			// Append is thread-safe
			log.append((error ? "[error] - " : "") + line + "\n");
		}
	}

	public Test() {
		super();

		JPanel panel = new JPanel();

		panel.setLayout(new BorderLayout());

		log = new JTextArea();

		panel.add(new JScrollPane(log), BorderLayout.CENTER);

		ClipboardService cs;

		try {
			cs = (ClipboardService) ServiceManager.lookup("javax.jnlp.ClipboardService");
		} catch (UnavailableServiceException e) {
			cs = null;
			System.err.println("No clipboard available: " + renderThrowable(e));
		}

		if (cs != null) {
			final ClipboardService finalcs = cs;

			JPanel bottom = new JPanel();

			bottom.setLayout(new FlowLayout(FlowLayout.RIGHT));

			btnCopy = new JButton("Copy to clipboard");
			btnCopy.setEnabled(false);
			btnCopy.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					StringSelection ss = new StringSelection(log.getText());
					finalcs.setContents(ss);	
				}
			});
			bottom.add(btnCopy);

			panel.add(bottom, BorderLayout.SOUTH);
		}

		getContentPane().setLayout(new BorderLayout());

		getContentPane().add(panel, BorderLayout.CENTER);

		validate();
	}

	public static String renderThrowable(Throwable aThrowable) {
		StringWriter result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

	public void start() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					logAppend(false, "System info:\n");

					Runtime runtime = Runtime.getRuntime();
					logAppend(false, "Memory: " + runtime.freeMemory() / (1024 * 1024) + "MB / " + runtime.maxMemory()
							/ (1024 * 1024) + "MB free");

					try {
						logAppend(
								false,
								"Java: " + System.getProperty("java.version") + " / "
										+ System.getProperty("java.vendor"));
						logAppend(
								false,
								"Operating system: " + System.getProperty("os.name") + " / "
										+ System.getProperty("os.arch") + " / " + System.getProperty("os.version"));
					} catch (Throwable e) {
						logAppend(true, "Got error while listing system properties:");
						logAppend(true, renderThrowable(e));
					}

					logAppend(false, "");

					logAppend(false, "Testing out Oekaki features...\n");

					String testString = getParameter("testUrl");

					if (testString == null) {
						logAppend(true, "Test URL was null!");
					} else {
						if (!(testString.startsWith("http://test.chickensmoothie.com/oekaki/test.php") || testString
								.startsWith("http://www.chickensmoothie.com/oekaki/test.php"))) {
							logAppend(true, "Test URL was: '" + testString + "'");
						}
					}

					try {
						testUrl = new URL(testString);
						logAppend(false, "Test URL read correctly.\n");
					} catch (MalformedURLException e1) {
						logAppend(true, "Malformed URL, so test stopped.");
						btnCopy.setEnabled(true);
						return;
					}

					new Thread(new Runnable() {
						public void run() {
							try {
								logAppend(false,
										"Connecting to Chicken Smoothie using the old method, please wait...\n");

								try {
									testSendImageOld();
									logAppend(false, "Upload success!");
								} catch (Throwable e) {
									logAppend(true, "Error during sending image:");
									logAppend(true, renderThrowable(e));
									logAppend(true, "Upload failed.");
								}

								logAppend(false, "");
								logAppend(false,
										"Connecting to Chicken Smoothie using the new method, please wait...\n");

								try {
									testSendImageNew();
									logAppend(false, "Upload success!");
								} catch (Throwable e) {
									logAppend(true, "Error during sending image:");
									logAppend(true, renderThrowable(e));
									logAppend(true, "Upload failed.");
								}
								logAppend(false, "");

								if (!errors) {
									logAppend(false, "All tests completed successfully!");
								} else {
									logAppend(false, "Tests are now done, but there were errors!");
								}
							} finally {
								btnCopy.setEnabled(true);
							}
						}
					}).start();
				}
			});
		} catch (Throwable e) {
			logAppend(true, renderThrowable(e));
		}
	}

	public void testSendImageNew() throws IOException {
		/* Render our request into a buffer */
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream bos = new DataOutputStream(buffer);

		final String boundary = "---------------------------309542943615284";

		bos.writeBytes("--" + boundary + "\r\n");
		bos.writeBytes("Content-Disposition: form-data; name=\"picture\"; filename=\"chibipaint.png\"\r\n");
		bos.writeBytes("Content-Type: image/png\r\n\r\n");
		bos.writeBytes(TEST_FLAT_IMAGE);
		bos.writeBytes("\r\n");

		bos.writeBytes("--" + boundary + "\r\n");
		bos.writeBytes("Content-Disposition: form-data; name=\"chibifile\"; filename=\"chibipaint.chi\"\r\n");
		bos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
		bos.writeBytes(TEST_LAYERS);
		bos.writeBytes("\r\n");

		bos.writeBytes("--" + boundary + "--\r\n");

		bos.flush();

		final byte[] data = buffer.toByteArray();
		buffer = null;

		HttpURLConnection connection = (HttpURLConnection) testUrl.openConnection();

		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "multipart/form-data, boundary=" + boundary);
		connection.setRequestProperty("Content-Length", Integer.toString(data.length));
		connection.setRequestProperty("User-Agent", "ChibiPaint Oekaki (" + System.getProperty("os.name") + "; "
				+ System.getProperty("os.version") + ")");

		logAppend(false, "Uploading test...");

		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
		try {
			/*
			 * Now the upload of the request body begins.
			 * 
			 * Write the data in chunks so we can give a progress bar
			 */
			final int CHUNK_SIZE = 8 * 1024;

			for (int chunk_pos = 0; chunk_pos < data.length; chunk_pos += CHUNK_SIZE) {
				/* Last chunk can be smaller than the others */
				int this_chunk = Math.min(data.length - chunk_pos, CHUNK_SIZE);

				dos.write(data, chunk_pos, this_chunk);
				dos.flush();
			}
		} finally {
			dos.close();
		}

		logAppend(false, "Finished sending request.");

		logAppend(false, "Reading response...");

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
				throw new RuntimeException("Server replied: " + response);
			}
		} finally {
			rd.close();
		}
	}

	public void testSendImageOld() throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream bos = new DataOutputStream(buffer);

		final String boundary = "---------------------------309542943615284";

		bos.writeBytes("--" + boundary + "\r\n");
		bos.writeBytes("Content-Disposition: form-data; name=\"picture\"; filename=\"chibipaint.png\"\r\n");
		bos.writeBytes("Content-Type: image/png\r\n\r\n");
		bos.writeBytes(TEST_FLAT_IMAGE);
		bos.writeBytes("\r\n");

		bos.writeBytes("--" + boundary + "\r\n");
		bos.writeBytes("Content-Disposition: form-data; name=\"chibifile\"; filename=\"chibipaint.chi\"\r\n");
		bos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
		bos.writeBytes(TEST_LAYERS);
		bos.writeBytes("\r\n");

		bos.writeBytes("--" + boundary + "--\r\n");

		bos.flush();

		final byte[] data = buffer.toByteArray();

		int port = testUrl.getPort();
		if (port < 0) {
			port = 80;
		}

		SocketAddress sockaddr = new InetSocketAddress(testUrl.getHost(), port);
		Socket s = new Socket();
		s.connect(sockaddr, 10000);

		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
		try {
			logAppend(false, "Uploading test...");

			dos.writeBytes("POST " + testUrl.getFile() + " HTTP/1.0\r\n");
			dos.writeBytes("Host: " + testUrl.getHost() + "\r\n");
			dos.writeBytes("User-Agent: ChibiPaint Oekaki (" + System.getProperty("os.name") + "; "
					+ System.getProperty("os.version") + ")\r\n");
			dos.writeBytes("Cache-Control: nocache\r\n");
			dos.writeBytes("Content-Type: multipart/form-data; boundary=" + boundary + "\r\n");
			dos.writeBytes("Content-Length: " + data.length + "\r\n");
			dos.writeBytes("\r\n");

			/*
			 * Write the data in chunks so we can give a progress bar
			 */

			final int CHUNK_SIZE = 8 * 1024;

			for (int chunk_pos = 0; chunk_pos < data.length; chunk_pos += CHUNK_SIZE) {
				/* Last chunk can be smaller than the others */
				int this_chunk = Math.min(data.length - chunk_pos, CHUNK_SIZE);

				dos.write(data, chunk_pos, this_chunk);
				dos.flush();
			}

			logAppend(false, "Finished sending request.");

			logAppend(false, "Reading response...");

			// Read the answer from the server and verifies it's
			// OK

			BufferedReader rd = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
			try {
				String line;
				String response = "";
				while ((line = rd.readLine()) != null && line.length() > 0) {
					response += line + "\n";
				}

				line = rd.readLine(); // should be our answer

				response += line + "\n";

				if (!line.startsWith("CHIBIOK")) {
					throw new RuntimeException("Server replied: " + response);
				}
			} finally {
				rd.close();
			}
		} finally {
			dos.close();
		}
	}
}