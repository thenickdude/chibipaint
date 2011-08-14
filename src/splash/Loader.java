package splash;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import splash.ResourceLoader.Resource;
import bootstrap.IChibiApplet;

public class Loader extends JApplet implements LoadingListener, IChibiApplet {

	private static final long serialVersionUID = 1L;

	private LoadingGUI loadingGUI = new LoadingGUI();

	private URL layersUrl, flatUrl, swatchesUrl;

	private static final int JARS = 0;
	private static final int LAYERS_FILE = 1;
	private static final int FLAT_FILE = 2;
	private static final int SWATCHES = 3;

	// Loading stage
	private int stage;

	private ResourceLoader loader = new ResourceLoader(Loader.this);

	private IChibiApplet chibipaint;

	Resource layers = null, flat = null;

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
			stage = SWATCHES;

			layers = loader.resources.remove(layersUrl);
			flat = loader.resources.remove(flatUrl);

			String swatchesParam = getParameter("loadSwatches");

			if (swatchesParam != null && swatchesParam.length() > 0) {
				try {
					swatchesUrl = new URL(getCodeBase(), swatchesParam);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					return;
				}
				loader.queueResource(swatchesUrl, "Swatches");
				loader.start();
			} else {
				loadingDone();
			}

			break;
		case SWATCHES:
			final Resource layersFinal = layers, flatFinal = flat;
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

					Resource swatches = loader.resources.remove(swatchesUrl);

					try {
						chibipaint = (IChibiApplet) c.newInstance(appletThis,
								layersFinal != null && layersFinal.contents != null ? new ByteArrayInputStream(layersFinal.contents)
										: null, flatFinal != null && flatFinal.contents != null ? new ByteArrayInputStream(
										flatFinal.contents) : null,
								swatches != null && swatches.contents != null ? new ByteArrayInputStream(
										swatches.contents) : null);
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
					"Your drawing's layers could not be loaded, please try again later.\nThe error returned was:\n" + message);
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
					loadingGUI.setMessage("Error loading " + stage + ": " + message);
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

	/**
	 * Pass through to ChibiApplet.hasUnsavedChanges()
	 * 
	 * @return
	 */
	public boolean hasUnsavedChanges() {
		return chibipaint != null ? chibipaint.hasUnsavedChanges() : false;
	}
}
