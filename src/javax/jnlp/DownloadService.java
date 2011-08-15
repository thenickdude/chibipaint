package javax.jnlp;

public interface DownloadService {

	public abstract void loadPart(String[] array, DownloadServiceListener listener);

}
