package chibipaint.engine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AdobeColorTable {

	public static final int ACO_COLORSPACE_RGB = 0;
	public static final int ACO_COLORSPACE_HSB = 1;
	public static final int ACO_COLORSPACE_CMYK = 2;
	public static final int ACO_COLORSPACE_LAB = 7;
	public static final int ACO_COLORSPACE_GRAYSCALE = 8;

	/**
	 * Read an .aco (Adobe COlor) swatches file and return an array of RGB
	 * colors.
	 * 
	 * Supports version 1 palettes, only RGB format.
	 * 
	 * @param input
	 * @return An array of colours, or null if the file was not supported.
	 */
	public static int[] read(InputStream input) {
		if (input == null)
			return null;
		
		DataInputStream in = new DataInputStream(input);
		int version, count;
		int[] result;

		try {
			version = in.readShort();
			if (version != 1)
				return null;
			count = in.readShort();

			result = new int[count];
			for (int i = 0; i < count; i++) {
				int colourspace = in.readShort();

				if (colourspace != ACO_COLORSPACE_RGB)
					return null;

				// Scale back down from 16-bit to 8-bit
				int r = (in.readUnsignedShort() * 255) / 65535;
				int g = (in.readUnsignedShort() * 255) / 65535;
				int b = (in.readUnsignedShort() * 255) / 65535;
				in.readUnsignedShort(); // third value unused

				result[i] = r << 16 | g << 8 | b;
			}

			return result;
		} catch (IOException e) {
		}

		return null;
	}

	/**
	 * Write an .aco (Adobe COlor) swatches file of the given RGB colours.
	 * 
	 * @param palette
	 * @param colours
	 * @throws IOException
	 */
	public static void write(OutputStream output, int[] colours) throws IOException {
		DataOutputStream out = new DataOutputStream(output);
		try {
			out.writeShort(1); // Version 1
			out.writeShort(colours.length); // Number of colours
			for (int colour : colours) {
				out.writeShort(ACO_COLORSPACE_RGB);

				// Scale up colours to 16-bits
				out.writeShort(((colour >> 16) & 0xFF) * 257);
				out.writeShort(((colour >> 8) & 0xFF) * 257);
				out.writeShort((colour & 0xFF) * 257);
				out.writeShort(0);
			}
		} finally {
			out.close();
		}
	}

}
