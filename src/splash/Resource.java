package splash;

import java.net.URL;

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