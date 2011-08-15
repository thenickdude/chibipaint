package javax.jnlp;

import java.io.IOException;

public interface FileOpenService {

	public FileContents openFileDialog(String s, String[] strings) throws IOException;

}
