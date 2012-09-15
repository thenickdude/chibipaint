package javax.jnlp;

import java.io.IOException;

public interface FileSaveService {

	public FileContents saveFileDialog(java.lang.String arg0, java.lang.String[] arg1, java.io.InputStream arg2, java.lang.String arg3) throws IOException;

	FileContents saveAsFileDialog(java.lang.String pathHint, java.lang.String[] extensions, FileContents contents);
}
