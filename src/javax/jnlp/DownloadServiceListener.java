package javax.jnlp;

public interface DownloadServiceListener {
	  public abstract void progress(java.net.URL arg0, java.lang.String arg1, long arg2, long arg3, int arg4);
	  
	  public abstract void validating(java.net.URL arg0, java.lang.String arg1, long arg2, long arg3, int arg4);
	  
	  public abstract void upgradingArchive(java.net.URL arg0, java.lang.String arg1, int arg2, int arg3);
	  
	  public abstract void downloadFailed(java.net.URL arg0, java.lang.String arg1);
}
