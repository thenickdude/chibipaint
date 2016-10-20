/*
	ChibiPaint
    Copyright (c) 2006-2008 Marc Schefer

    This file is part of ChibiPaint.

    ChibiPaint is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ChibiPaint is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ChibiPaint. If not, see <http://www.gnu.org/licenses/>.

 */

package chibipaint.engine;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class CPChibiFile {

	protected static final byte CHIB[] = { 67, 72, 73, 66 };
	protected static final byte IOEK[] = { 73, 79, 69, 75 };
	protected static final byte HEAD[] = { 72, 69, 65, 68 };
	protected static final byte LAYR[] = { 76, 65, 89, 82 };
	protected static final byte ZEND[] = { 90, 69, 78, 68 };

	static public boolean write(OutputStream os, CPArtwork a) {
		try {
			writeMagic(os);
			os.flush();

			Deflater def = new Deflater(7);
			DeflaterOutputStream dos = new DeflaterOutputStream(os, def);
			// OutputStream dos = os;

			writeHeader(dos, a);

			for (Object l : a.layers) {
				writeLayer(dos, (CPLayer) l);
			}

			writeEnd(dos);

			dos.flush();
			dos.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	static public void writeInt(OutputStream os, int i) throws IOException {
		byte[] temp = { (byte) (i >>> 24), (byte) ((i >>> 16) & 0xff), (byte) ((i >>> 8) & 0xff), (byte) (i & 0xff) };
		os.write(temp);
	}

	static public void writeIntArray(OutputStream os, int arr[]) throws IOException {
		byte[] temp = new byte[arr.length * 4];
		int idx = 0;
		for (int i : arr) {
			temp[idx++] = (byte) (i >>> 24);
			temp[idx++] = (byte) ((i >>> 16) & 0xff);
			temp[idx++] = (byte) ((i >>> 8) & 0xff);
			temp[idx++] = (byte) (i & 0xff);
		}

		os.write(temp);
	}

	static public void writeMagic(OutputStream os) throws IOException {
		os.write(CHIB);
		os.write(IOEK);
	}

	static public void writeEnd(OutputStream os) throws IOException {
		os.write(ZEND);
		writeInt(os, 0);
	}

	static public void writeHeader(OutputStream os, CPArtwork a) throws IOException {
		os.write(HEAD); // Chunk ID
		writeInt(os, 16); // ChunkSize

		writeInt(os, 0); // Current Version: Major: 0 Minor: 0
		writeInt(os, a.width);
		writeInt(os, a.height);
		writeInt(os, a.getLayersNb());
	}

	static public void writeLayer(OutputStream os, CPLayer l) throws IOException {
		byte[] title = l.name.getBytes("UTF-8");

		os.write(LAYR); // Chunk ID
		writeInt(os, 20 + l.data.length * 4 + title.length); // ChunkSize

		writeInt(os, 20 + title.length); // Data offset from start of header
		writeInt(os, l.blendMode); // layer blend mode
		writeInt(os, l.alpha); // layer opacity
		writeInt(os, l.visible ? 1 : 0); // layer visibility and future flags

		writeInt(os, title.length);
		os.write(title);

		writeIntArray(os, l.data);
	}

	static public CPArtwork read(InputStream is) {
		try {
			if (!readMagic(is)) {
				return null; // not a ChibiPaint file
			}

			InflaterInputStream iis = new InflaterInputStream(is);
			CPChibiChunk chunk = new CPChibiChunk(iis);
			if (!chunk.is(HEAD)) {
				return null; // not a valid file
			}

			CPChibiHeader header = new CPChibiHeader(iis, chunk);
			if ((header.version >>> 16) > 0) {
				return null; // the file version is higher than what we can deal with, bail out
			}

			CPArtwork a = new CPArtwork(header.width, header.height);
			a.layers.remove(0); // FIXME: it would be better not to have created it in the first place

			while (true) {
				chunk = new CPChibiChunk(iis);

				if (chunk.is(ZEND)) {
					break;
				} else if (chunk.is(LAYR)) {
					readLayer(iis, chunk, a);
				} else {
					realSkip(iis, chunk.chunkSize);
				}
			}

			a.setActiveLayer(0);
			return a;

		} catch (IOException e) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	static private void readLayer(InputStream is, CPChibiChunk chunk, CPArtwork a) throws IOException {
		CPLayer l = new CPLayer(a.width, a.height);

		int offset = readInt(is);
		l.blendMode = readInt(is); // layer blend mode
		l.alpha = readInt(is);
		l.visible = (readInt(is) & 1) != 0;

		int titleLength = readInt(is);
		byte[] title = new byte[titleLength];
		realRead(is, title, titleLength);
		l.name = new String(title, "UTF-8");

		realSkip(is, offset - 20 - titleLength);
		readIntArray(is, l.data, l.width * l.height);

		a.layers.add(l);

		realSkip(is, chunk.chunkSize - offset - l.width * l.height * 4);
	}

	static private void readIntArray(InputStream is, int[] intArray, int size) throws IOException {
		byte[] buffer = new byte[size * 4];

		realRead(is, buffer, size * 4);

		int off = 0;
		for (int i = 0; i < size; i++) {
			intArray[i] = ((buffer[off++] & 0xff) << 24) | ((buffer[off++] & 0xff) << 16)
					| ((buffer[off++] & 0xff) << 8) | (buffer[off++] & 0xff);
		}
	}

	static public int readInt(InputStream is) throws IOException {
		return is.read() << 24 | is.read() << 16 | is.read() << 8 | is.read();
	}

	static void realSkip(InputStream is, long bytesToSkip) throws IOException {
		long skipped = 0, value;
		while (skipped < bytesToSkip) {
			value = is.read();
			if (value < 0) {
				throw new RuntimeException("EOF!");
			}

			skipped++;
			skipped += is.skip(bytesToSkip - skipped);
		}
	}

	static void realRead(InputStream is, byte[] buffer, int bytesToRead) throws IOException {
		int read = 0, value;
		while (read < bytesToRead) {
			value = is.read();
			if (value < 0) {
				throw new RuntimeException("EOF!");
			}

			buffer[read++] = (byte) value;
			read += is.read(buffer, read, bytesToRead - read);
		}
	}

	static public boolean readMagic(InputStream is) throws IOException {
		byte[] buffer = new byte[4];

		realRead(is, buffer, 4);
		if (!Arrays.equals(buffer, CHIB)) {
			return false;
		}

		realRead(is, buffer, 4);
		if (!Arrays.equals(buffer, IOEK)) {
			return false;
		}

		return true;
	}

	static class CPChibiChunk {

		byte[] chunkType = new byte[4];
		int chunkSize;

		public CPChibiChunk(InputStream is) throws IOException {
			realRead(is, chunkType, 4);
			chunkSize = readInt(is);
		}

		private boolean is(byte[] chunkType) {
			return Arrays.equals(this.chunkType, chunkType);
		}
	}

	static class CPChibiHeader {

		int version, width, height, layersNb;

		public CPChibiHeader(InputStream is, CPChibiChunk chunk) throws IOException {
			version = readInt(is);
			width = readInt(is);
			height = readInt(is);
			layersNb = readInt(is);

			realSkip(is, chunk.chunkSize - 16);
		}
	}

}
