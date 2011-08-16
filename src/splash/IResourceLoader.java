package splash;

import java.net.URL;

public interface IResourceLoader {

	public abstract boolean queuePart(String fileName, String partName, String friendlyName);

	public abstract void queueResource(URL url, String friendlyName);

	/**
	 * Start downloading the queued resources and return immediately (notifying
	 * listener of progress events).
	 */
	public abstract void start();

	public abstract Resource removeResource(URL url);
}