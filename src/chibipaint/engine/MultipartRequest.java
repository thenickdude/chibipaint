package chibipaint.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/* 
 * Copyright Nicholas Sherlock 2012.
 */
class MultipartItem {
	private String boundary, name, filename, contentType;
	private byte[] data;

	MultipartItem(String boundary, String name, String filename, String contentType, byte[] data) {
		this.boundary = boundary;
		this.name = name;
		this.filename = filename;
		this.contentType = contentType;
		this.data = data;
	}

	public byte[] renderHeader() {
		return 
			("\r\n--" + boundary + "\r\n" +
			"Content-Disposition: form-data; name=\"" + name + "\"" + (filename != null ? "; filename=\"" + filename + "\"" : "") + "\r\n" +
			"Content-Type: " + contentType + "\r\n\r\n").getBytes();
	}

	public byte[] getData() {
		return data;
	}
	
	/**
	 * Get the size of this part + its headers and footer in bytes.
	 * 
	 * @return the size of this part + its headers and footer in bytes.
	 */
	public int size() {
		return renderHeader().length + data.length;
	}
}

/**
 * Input stream over several other input streams.
 * 
 * @author Nicholas Sherlock
 * 
 */
class InputStreamInputStream extends InputStream {

	private InputStream[] streams;
	private int streamIndex;

	public InputStreamInputStream(InputStream[] streams) {
		this.streams = streams;
		this.streamIndex = 0;
	}

	@Override
	public int read() throws IOException {
		if (streamIndex >= streams.length)
			return -1; // Finished reading all data!

		int result = streams[streamIndex].read();

		if (result == -1) {
			// This stream is exhausted so advance to the next stream.
			streamIndex++;
			return read();
		}

		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		if (streamIndex >= streams.length)
			return -1; // Finished reading all data!

		int result = streams[streamIndex].read(b);

		if (result == -1) {
			// This stream is exhausted so advance to the next stream.
			streamIndex++;
			return read(b);
		}

		return result;
	}
}

public class MultipartRequest {

	private ArrayList<MultipartItem> parts = new ArrayList<MultipartItem>();

	private final String boundary = "-----------------------ilmfewq3012rh42943615284";
	private final String footer = "\r\n--" + boundary + "--\r\n";

	public MultipartRequest() {
	}

	public void addPart(String name, String filename, String contentType, byte[] data) {
		parts.add(new MultipartItem(boundary, name, filename, contentType, data));
	}

	/**
	 * Get the total length of this multipart request in bytes.
	 * 
	 * @return Length in bytes
	 */
	public int getRequestLength() {
		int result = 0;

		for (int i = 0; i < parts.size(); i++) {
			result += parts.get(i).size();
		}

		result += footer.length();

		return result;
	}

	/**
	 * Get an input stream which reads this whole request.
	 * 
	 * @return
	 */
	public InputStream getInputStream() {
		InputStream[] streams = new InputStream[parts.size() * 2 + 1];

		for (int i = 0; i < parts.size(); i++) {
			streams[i * 2] = new ByteArrayInputStream(parts.get(i).renderHeader());
			streams[i * 2 + 1] = new ByteArrayInputStream(parts.get(i).getData());
		}
		
		streams[streams.length - 1] = new ByteArrayInputStream(footer.getBytes());
		
		return new InputStreamInputStream(streams);
	}

	public String getBoundary() {
		return boundary;
	}

}
