package splash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jnlp.DownloadService;
import javax.jnlp.DownloadServiceListener;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

public class ResourceLoader implements DownloadServiceListener {

	public class Resource {
		URL url;
		String friendlyName;

		byte[] contents;

		public Resource(URL url, String friendlyName) {
			super();
			this.url = url;
			this.friendlyName = friendlyName;
		}
	}

	private HashMap<String, String> friendlyNames = new HashMap<String, String>();

	private ArrayList<String> parts = new ArrayList<String>();
	HashMap<URL, Resource> resources = new HashMap<URL, Resource>();

	private DownloadService downloadService;
	private LoadingListener listener;
	
	private static final boolean debug = true; 

	public void queuePart(String fileName, String partName, String friendlyName) {
		parts.add(partName);
		friendlyNames.put(fileName, friendlyName);
	}

	public void queueResource(URL url, String friendlyName) {
		resources.put(url, new Resource(url, friendlyName));
	}

	public ResourceLoader(LoadingListener listener) {
		this.listener = listener;

		try {
			downloadService = (DownloadService) ServiceManager
					.lookup("javax.jnlp.DownloadService");
		} catch (UnavailableServiceException e) {
			listener.loadingFail("Loading failed, couldn't find download service!");
			System.err
					.println("Loading failed, couldn't find download service!");
			return;
		}
	}

	/**
	 * Start downloading the queued resources and return immediately (notifying
	 * listener of progress events).
	 */
	public void start() {
		new Thread(new Runnable() {
			public void run() {
				try {
					downloadService.loadPart(parts.toArray(new String[0]),
							ResourceLoader.this);

					for (Resource resource : resources.values()) {
						HttpURLConnection connection = (HttpURLConnection) resource.url
								.openConnection();
						try {
							connection.setUseCaches(false); // Bypassing the
															// cache
															// is important

							ByteArrayOutputStream outBuf = new ByteArrayOutputStream();

							int length = connection.getContentLength();

							listener.loadingProgress("Loading "
									+ resource.friendlyName
									+ (length != -1 ? " (0%)" : "") + "...",
									new Double(0));

							InputStream in = connection.getInputStream();
							byte[] chunk = new byte[16 * 1024];
							int len = 0;

							debugSleep(1500);
							
							while ((len = in.read(chunk)) >= 0) {
								outBuf.write(chunk, 0, len);

								debugSleep(100);

								if (length != -1) {
									double progress = (double) outBuf.size()
											/ length;

									listener.loadingProgress("Loading "
											+ resource.friendlyName + " ("
											+ (int) (progress * 100) + "%)...",
											progress);
								}
							}

							resource.contents = outBuf.toByteArray();
						} catch (IOException ex) {
							listener.loadingFail(connection.getResponseCode()
									+ " " + connection.getResponseMessage());
							return;
						}

					}

					listener.loadingDone();
					parts.clear();
				} catch (IOException ex) {
					listener.loadingFail("Couldn't open connection "
							+ ex.getMessage());
				}
			}
		}).start();
	}

	public void downloadFailed(java.net.URL url, java.lang.String version) {
		listener.loadingFail("Loading failed, couldn't download '"
				+ url.toString() + "'.");
	}

	public String getURLFilename(java.net.URL url) {
		String fn = url.getFile();
		fn = fn.substring(fn.lastIndexOf('/') + 1);
		
		if (fn.indexOf('?') != -1) {
			fn = fn.substring(0, fn.indexOf('?'));
		}
		
		return fn;
	}

	public void progress(java.net.URL url, java.lang.String version,
			long readSoFar, long total, int overallPercent) {
		debugSleep(150);
		
		if (url == null) {
			return;
		}
		String fn = getURLFilename(url);

		if (overallPercent == -1)
			listener.loadingProgress(
					"Loading " + friendlyNames.get(fn) + "...", null);
		else
			listener.loadingProgress("Loading " + friendlyNames.get(fn) + " ("
					+ overallPercent + "%)...", (double) overallPercent / 100);
	}

	private static void debugSleep(int millis) {
		if (debug)
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public void upgradingArchive(java.net.URL url, java.lang.String version,
			int patchPercent, int overallPercent) {

	}

	public void validating(java.net.URL url, java.lang.String version,
			long entry, long total, int overallPercent) {

		if (url == null) {
			listener.loadingProgress("Verifying...", null);
			return;
		}

		String fn = getURLFilename(url);

		if (overallPercent == -1)
			listener.loadingProgress("Verifying " + friendlyNames.get(fn)
					+ "...", null);
		else
			listener.loadingProgress("Verifying " + friendlyNames.get(fn)
					+ " (" + overallPercent + "%)...",
					(double) overallPercent / 100);
	}
}
