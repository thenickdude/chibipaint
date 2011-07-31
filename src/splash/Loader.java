package splash;

import java.io.ByteArrayInputStream;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.*;

import splash.ResourceLoader.Resource;

public class Loader extends JApplet implements LoadingListener {

	private static final long serialVersionUID = 1L;

	private LoadingGUI loadingGUI = new LoadingGUI();

	private URL layersUrl, flatUrl;

	static final int JARS=0;
	static final int LAYERS_FILE=1;
	static final int FLAT_FILE=2;

	//Loading stage
	private int stage;

	private ResourceLoader loader = new ResourceLoader(Loader.this);

	public void loadingProgress(final String message, final Double loaded) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				System.out.println(System.currentTimeMillis() + " - " + message);

				loadingGUI.setMessage(message);
				if (loaded != null)
					loadingGUI.setProgress(loaded);
			}
		});
	}

	public void loadingDone() {

		final JApplet appletThis = this;

		switch (stage) {
		case JARS:
			try {
				String chibiParam = getParameter("loadChibiFile");
				String flatParam = getParameter("loadImage");

				if (chibiParam != null && chibiParam.length() > 0) {
					stage = LAYERS_FILE;

					layersUrl = new URL(getCodeBase(), chibiParam);

					loader.queueResource(layersUrl, "Drawing layers");

					loader.start();
				} else {
					// Fall back to flat image
					if (flatParam != null && flatParam.length() > 0) {
						stage = FLAT_FILE;

						flatUrl = new URL(getCodeBase(), flatParam);

						loader.queueResource(flatUrl, "Drawing");

						loader.start();
					} else {
						// Nothing to load
						stage = FLAT_FILE;
						loadingDone();
						return;
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			break;

		case LAYERS_FILE:
		case FLAT_FILE:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessage("Starting...");
					loadingGUI.setProgress(1);
					System.err.println("Starting...");

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

					Resource layers = loader.resources.remove(layersUrl);
					Resource flat = loader.resources.remove(flatUrl);

					try {
						c.newInstance(appletThis, layers != null && layers.contents != null ? new ByteArrayInputStream(
								layers.contents) : null,
								flat != null && flat.contents != null ? new ByteArrayInputStream(flat.contents) : null);
					} catch (Exception ex) {
						loadingGUI.setMessage(ex.getMessage());
						ex.printStackTrace();
					}
				}
			});
		}
	}

	public void loadingFail(final String message) {
		switch (stage) {
		case LAYERS_FILE:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessageProgress("Couldn't load your drawing's layers.", 0.1);
				}
			});
			JOptionPane.showMessageDialog(getContentPane(),
					"Your drawing's layers could not be loaded, please try again later.");
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
		case JARS:
		default:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessage(message);
				}
			});
		}
	}

	public void init() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				stage = JARS;

				setContentPane(loadingGUI);

				loader.queuePart("chibi.jar", "chibi", "Chibi Paint");
				loader.start();
			}
		});
	}

	public static void main(String[] args) {
		new Loader();
	}

}
