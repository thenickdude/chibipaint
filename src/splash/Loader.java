package splash;

import java.io.ByteArrayInputStream;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jnlp.*;

import java.awt.*;
import javax.swing.*;

import splash.ResourceLoader.Resource;

public class Loader extends JApplet implements LoadingListener {

	private static final long serialVersionUID = 1L;

	private LoadingGUI loadingGUI;

	private URL layersUrl;

	enum LoadingStage {
		JARS, LAYERS_FILE, FLAT_FILE
	}

	private LoadingStage stage = LoadingStage.JARS;

	ResourceLoader loader = new ResourceLoader(Loader.this);

	private URL flatUrl;

	public void loadingProgress(final String message, final Double loaded) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
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
					stage = LoadingStage.LAYERS_FILE;

					layersUrl = new URL(getCodeBase(), chibiParam);

					loader.queueResource(layersUrl, "Drawing layers");

					loader.start();
				} else {
					// Fall back to flat image
					if (flatParam != null && flatParam.length() > 0) {
						stage = LoadingStage.FLAT_FILE;

						flatUrl = new URL(getCodeBase(), flatParam);

						loader.queueResource(flatUrl, "Drawing");

						loader.start();
					} else {
						// Nothing to load
						stage = LoadingStage.FLAT_FILE;
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

					getContentPane().remove(loadingGUI);
					loadingGUI = null;

					Constructor<?> c;
					try {
						c = Class.forName("chibipaint.ChibiApplet")
								.getConstructors()[0];
					} catch (ClassNotFoundException ex1) {
						System.err.println("Class not found, ChibiApplet");
						return;
					} catch (SecurityException ex1) {
						System.err
								.println("Security exception loading ChibiApplet");
						return;
					}

					try {
						BasicService basic = null;

						try {
							basic = (BasicService) ServiceManager
									.lookup("javax.jnlp.BasicService");
						} catch (UnavailableServiceException ex) {
						}

						Resource layersFile = loader.resources.get(layersUrl);

						c.newInstance(
								appletThis,
								layersFile.contents != null ? new ByteArrayInputStream(
										layersFile.contents) : null);
					} catch (InvocationTargetException ex) {
						ex.printStackTrace();
					} catch (IllegalArgumentException ex) {
						ex.printStackTrace();
					} catch (IllegalAccessException ex) {
						ex.printStackTrace();
					} catch (InstantiationException ex) {
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
					loadingGUI.setMessageProgress(
							"Couldn't load your drawing's layers.", 0.1);
				}
			});
			JOptionPane
					.showMessageDialog(getContentPane(),
							"Your drawing's layers could not be loaded, please try again later.");
			break;
		case FLAT_FILE:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI
							.setMessageProgress("Couldn't load your drawing.", 0.1);
				}
			});
			JOptionPane
					.showMessageDialog(
							getContentPane(),
							"Your drawing could not be loaded, please try again later.\nThe error returned was:\n"
									+ message);
			break;
		default:
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					loadingGUI.setMessageProgress(message, 1);
					loadingDone();
				}
			});
		}
	}

	// Called from the Swing event dispatcher thread
	public Loader() {
		super();
	}

	public void init() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					loadingGUI = new LoadingGUI();
					getContentPane().add(loadingGUI,
							java.awt.BorderLayout.CENTER);

					validate();

					loader.queuePart("chibi.jar", "chibi", "Chibi Paint");

					loader.start();
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Loader();
	}

}
