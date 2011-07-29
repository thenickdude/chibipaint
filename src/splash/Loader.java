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

	private LoadingScreen loadingScreen;

	private URL layersUrl;
	
	ResourceLoader loader = new ResourceLoader(Loader.this);

	public void loadingProgress(final String message, final Double loaded) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				loadingScreen.setMessage(message);
				if (loaded != null)
					loadingScreen.setProgress(loaded);
			}
		});
	}

	public void loadingDone() {

		final JApplet appletThis = this;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				loadingScreen.setMessage("Starting...");
				loadingScreen.setProgress(1);
				System.err.println("Starting...");

				getContentPane().remove(loadingScreen);
				loadingScreen = null;

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
					
					c.newInstance(appletThis, layersFile.contents != null ? new ByteArrayInputStream(layersFile.contents) : null);
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

	public void loadingFail(final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				loadingScreen.setMessage(message);
				loadingDone();
			}
		});
	}

	// Called from the Swing event dispatcher thread
	public Loader() {
		super();
	}

	public void init() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					setLayout(new BorderLayout());
					setPreferredSize(new Dimension(600, 400));

					loadingScreen = new LoadingScreen();
					getContentPane().add(loadingScreen,
							java.awt.BorderLayout.CENTER);

					validate();

					loader.queuePart("chibi.jar", "chibi", "Chibi Paint");

					try {
						layersUrl = new URL(getCodeBase(),
								getParameter("loadChibiFile"));

						loader.queueResource(layersUrl, "Drawing layers");
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}

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
