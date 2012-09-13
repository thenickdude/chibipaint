package splash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class ResourceLoaderOld implements IResourceLoader {

	private HashMap<URL, Resource> resources = new HashMap<URL, Resource>();

	private LoadingListener listener;

	private static final boolean debug = false;

	public void queueResource(URL url, String friendlyName) {
		resources.put(url, new Resource(url, friendlyName));
	}

	public ResourceLoaderOld(LoadingListener listener) {
		this.listener = listener;
	}

	/**
	 * Start downloading the queued resources and return immediately (notifying
	 * listener of progress events).
	 */
	public void start() {
		new Thread(new Runnable() {
			public void run() {
				try {
					for (Resource resource : resources.values()) {
						HttpURLConnection connection = (HttpURLConnection) resource.url.openConnection();
						try {
							connection.setUseCaches(false); // Bypassing the
															// cache
															// is important

							ByteArrayOutputStream outBuf = new ByteArrayOutputStream();

							int length = connection.getContentLength();

							listener.loadingProgress("Loading " + resource.friendlyName + (length != -1 ? " (0%)" : "")
									+ "...", new Double(0));

							InputStream in = connection.getInputStream();
							byte[] chunk = new byte[16 * 1024];
							int len = 0;

							debugSleep(1000);

							while ((len = in.read(chunk)) >= 0) {
								outBuf.write(chunk, 0, len);

								debugSleep(100);

								if (length != -1) {
									double progress = (double) outBuf.size() / length;

									listener.loadingProgress("Loading " + resource.friendlyName + " ("
											+ (int) (progress * 100) + "%)...", new Double(progress));
								}
							}

							resource.contents = outBuf.toByteArray();
						} catch (IOException ex) {
							listener.loadingFail(connection.getResponseCode() + " " + connection.getResponseMessage());
							return;
						}

					}

					listener.loadingDone();
				} catch (IOException ex) {
					listener.loadingFail("Couldn't open connection " + ex.getMessage());
				}
			}
		}).start();
	}

	private static void debugSleep(int millis) {
		if (debug)
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public boolean queuePart(String fileName, String partName, String friendlyName) {
		return false;
	}
	
	public Resource removeResource(URL url) {
		return resources.remove(url);
	}
}
