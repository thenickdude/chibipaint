package splash;

/**
 * An interface you should implement to receive information about the progress
 * of loading resources
 */
public interface LoadingListener {

	/**
	 * Called to give a progress update on the loading of resources.
	 * 
	 * @param message
	 *            Loading message
	 * @param loaded
	 *            A double between 0 and 1 inclusive that gives the total
	 *            progress of the loading, or null if the progress isn't known.
	 */
	public void loadingProgress(String message, Double loaded);

	public void loadingDone();

	public void loadingFail(String message);
}
