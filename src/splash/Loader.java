package splash;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import bootstrap.IChibiApplet;

public class Loader extends JApplet implements LoadingListener, IChibiApplet {

	private static final long serialVersionUID = 1L;

	private LoadingGUI loadingGUI = new LoadingGUI();

	private URL layersUrl, flatUrl, swatchesUrl;

	private static final String TEST_LAYERS = "Test layers";
	private static final String TEST_FLAT_IMAGE = "Test flat image";
	
	private static final int INIT = 0;
	private static final int JARS = 1;
	private static final int LAYERS_FILE = 2;
	private static final int FLAT_FILE = 3;
	private static final int SWATCHES = 4;
	private static final int UPLOAD_TEST = 5;

	private int loadStage;

	private IResourceLoader loader;

	private IChibiApplet chibipaint;

	Resource layers = null, flat = null;

	public void loadingProgress(final String message, final Double loaded) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//System.out.println(System.currentTimeMillis() + " - " + message);
				if (loadingGUI != null) {
					loadingGUI.setMessage(message);
					if (loaded != null)
						loadingGUI.setProgress(loaded);
				}
			}
		});
	}
	
	private static String renderThrowable(Throwable aThrowable) {
		StringWriter result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}
	
	private void doUploadTest(URL testURL) {
		try {
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

			HttpURLConnection connection = (HttpURLConnection) testURL.openConnection();
			try {
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Connection", "close");
				connection.setRequestProperty("Content-Type", "multipart/form-data, boundary=" + boundary);
				connection.setRequestProperty("Content-Length", Integer.toString(data.length));
				connection.setRequestProperty("User-Agent", "ChibiPaint Oekaki (" + System.getProperty("os.name") + "; "
						+ System.getProperty("os.version") + ")");
	
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
						throw new RuntimeException("Server replied: " + response);
					}
				} finally {
					rd.close();
				}
			} finally {
				connection.disconnect();
			}
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingDone();
				}
			});
		} catch (Throwable e) {
			loadingFail("Failed to connect to the Chicken Smooothie server!\nThe message was:\n" + renderThrowable(e));
		}
	}

	public void loadingDone() {
		switch (loadStage) {
		case JARS:
			try {
				String chibiParam = getParameter("loadChibiFile");
				String flatParam = getParameter("loadImage");

				if (chibiParam != null && chibiParam.length() > 0) {
					loadStage = LAYERS_FILE;

					layersUrl = new URL(getCodeBase(), chibiParam);

					loader.queueResource(layersUrl, "drawing layers");

					loader.start();
				} else {
					// Fall back to flat image
					if (flatParam != null && flatParam.length() > 0) {
						loadStage = FLAT_FILE;

						flatUrl = new URL(getCodeBase(), flatParam);

						loader.queueResource(flatUrl, "drawing");

						loader.start();
					} else {
						// Nothing to load
						loadStage = FLAT_FILE;
						loadingDone();
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			break;

		case LAYERS_FILE:
		case FLAT_FILE:
			loadStage = SWATCHES;

			layers = loader.removeResource(layersUrl);
			flat = loader.removeResource(flatUrl);

			String swatchesParam = getParameter("loadSwatches");

			if (swatchesParam != null && swatchesParam.length() > 0) {
				try {
					swatchesUrl = new URL(getCodeBase(), swatchesParam);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					return;
				}
				loader.queueResource(swatchesUrl, "swatches");
				loader.start();
			} else {
				loadingDone();
			}

			break;
		case SWATCHES:
			loadStage = UPLOAD_TEST;

			loadingProgress("Checking your connection to Chicken Smoothie...", new Double(100.0));
			
			final String testParam = getParameter("testUrl");
				new Thread(new Runnable() {
					public void run() {
						try {
							doUploadTest(new URL(getCodeBase(), testParam));
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
				}).start();
			break;
		case UPLOAD_TEST:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					startPainter();
				}
			});
			break;
		}
	}

	private void startPainter() {	
		loadingGUI.setMessage("Starting...");
		loadingGUI.setProgress(1);
		System.err.println("Starting...");

		loadingGUI.finished();
		
		Constructor<?> c;
		try {
			c = Class.forName("chibipaint.ChibiApplet").getConstructors()[0];
		} catch (ClassNotFoundException ex1) {
			loadingGUI.setMessage("Class not found, ChibiApplet");
			System.err.println("Class not found, ChibiApplet");
			return;
		} catch (SecurityException ex1) {
			loadingGUI.setMessage("Security exception loading ChibiApplet");
			System.err.println("Security exception loading ChibiApplet");
			return;
		}

		Resource swatches = loader.removeResource(swatchesUrl);

		try {
			chibipaint = (IChibiApplet) c.newInstance(Loader.this,
					layers != null && layers.contents != null ? new ByteArrayInputStream(layers.contents) : null,
					flat != null && flat.contents != null ? new ByteArrayInputStream(flat.contents) : null,
					swatches != null && swatches.contents != null ? new ByteArrayInputStream(swatches.contents) : null);
			
			//We won't be using this again so free up memory:
			loadingGUI = null;
		} catch (InvocationTargetException ex) {
			if (ex.getCause() != null && ex.getCause() instanceof OutOfMemoryError)
				loadingGUI.setMessage("Couldn't start because Java ran out of memory!");
			ex.printStackTrace();
		} catch (Exception ex) {
			loadingGUI.setMessage(ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void loadingFail(final String message) {
		switch (loadStage) {
		case LAYERS_FILE:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessageProgress("Couldn't load your drawing's layers.", 0.1);
				}
			});
			JOptionPane.showMessageDialog(getContentPane(),
					"Your drawing's layers could not be loaded, please try again later.\nThe error returned was:\n"
							+ message);
			break;
		case FLAT_FILE:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessageProgress("Couldn't load your drawing.", 0.1);
				}
			});
			JOptionPane.showMessageDialog(getContentPane(),
					"Your drawing could not be loaded, please try again later.\nThe error returned was:\n" + message);
			break;
		case INIT:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessage(message);
					loadingGUI.setShowImages(false);
				}
			});
			break;
		case JARS:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessage("Error loading the drawing tools, please refresh to try again.\n(" + message + ")");
				}
			});
			break;
		case SWATCHES:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessage("Your color swatches could not be loaded, please try again later.\nThe error returned was:\n" + message);
				}
			});
			break;			
		default:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessage("Error loading " + loadStage + ": " + message);
				}
			});
		}
	}

	public void init() {
		chibipaint = null;
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				loadStage = INIT;

				setContentPane(loadingGUI);

				String javaVersion = System.getProperty("java.version");
				
				if (javaVersion.startsWith("1.4.")) {
					loadingFail("Your version of Java is too old (you need at least 1.5)\nPlease visit www.java.com to upgrade!");
				} else {
					loadStage = JARS;
					if (loader.queuePart("chibi.jar", "chibi", "drawing tools")) {
						loader.start();
					} else {
						//Skip straight to the "done downloading JARs" step
						loadingDone();
					}
				}
			}
		});
	}

	public static void main(String[] args) {
		new Loader();
	}

	public Loader() {
		try {
			ServiceManager.lookup("javax.jnlp.DownloadService");

			System.out.println("Loading by JNLP");
			loader = new ResourceLoaderJNLP(Loader.this);
		} catch (UnavailableServiceException e) {
			System.out.println("Loading by old loader");
			loader = new ResourceLoaderOld(Loader.this);
		}

	}

	/**
	 * Pass through to ChibiApplet.hasUnsavedChanges()
	 * 
	 * @return
	 */
	public boolean hasUnsavedChanges() {
		return chibipaint != null ? chibipaint.hasUnsavedChanges() : false;
	}
}
