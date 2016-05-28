package com.xuchaoguo.lab.filesync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A number of useful, static methods.
 * 
 * @version $Revision: 1.7 $
 */
public final class Util {

	// Constants and variables.
	// -----------------------------------------------------------------------

	/** The characters for Base64 encoding. */
	public static final String BASE_64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	// Constructors.
	// -----------------------------------------------------------------------

	/** This class cannot be instantiated. */
	private Util() {
	}

	// Class methods.
	// -----------------------------------------------------------------------

	/**
	 * Base-64 encode a byte array, returning the returning string.
	 * 
	 * <p>
	 * Note that this method exists merely to be compatible with the
	 * challenge-response authentication method of rsyncd. It is <em>not</em>
	 * technincally a Base-64 encoder.
	 * 
	 * @param buf
	 *            The byte array to encode.
	 * @return <tt>buf</tt> encoded in Base64.
	 */
	public static final String base64(byte[] buf) {
		int bitOffset, byteOffset, index = 0;
		int bytes = (buf.length * 8 + 5) / 6;
		StringBuffer out = new StringBuffer(bytes);

		for (int i = 0; i < bytes; i++) {
			byteOffset = (i * 6) / 8;
			bitOffset = (i * 6) % 8;
			if (bitOffset < 3) {
				index = (buf[byteOffset] >>> (2 - bitOffset)) & 0x3f;
			} else {
				index = (buf[byteOffset] << (bitOffset - 2)) & 0x3f;
				if (byteOffset + 1 < buf.length) {
					index |= (buf[byteOffset + 1] & 0xff) >>> (8 - (bitOffset - 2));
				}
			}
			out.append(BASE_64.charAt(index));
		}

		return out.toString();
	}

	/**
	 * Write a String as a sequece of ASCII bytes.
	 * 
	 * @param out
	 *            The {@link java.io.OutputStream} to write to.
	 * @param ascii
	 *            The ASCII string to write.
	 * @throws java.io.IOException
	 *             If writing fails.
	 */
	public static void writeASCII(OutputStream out, String ascii)
			throws IOException {
		try {
			out.write(ascii.getBytes("US-ASCII"));
		} catch (java.io.UnsupportedEncodingException shouldNotHappen) {
		}
	}

	/**
	 * Read up to a '\n' or '\r', and return the resulting string. The input is
	 * assumed to be ISO-8859-1.
	 * 
	 * @param in
	 *            The {@link java.io.InputStream} to read from.
	 * @return The line read, without the line terminator.
	 */
	public static String readLine(InputStream in) throws IOException {
		StringBuffer s = new StringBuffer();
		int c = in.read();
		while (c != -1 && c != '\n') {
			if (c != '\r')
				s.append((char) (c & 0xff));
			c = in.read();
		}
		if (s.length() == 0 && c == -1) {
			return null;
		}
		return s.toString();
	}

	// From gnu.crypto.util.Util

	/** Hexadecimal digits. */
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Convert a byte array to a big-endian ordered hexadecimal string.
	 * 
	 * @param b
	 *            The bytes to convert.
	 * @return A hexadecimal representation to <tt>b</tt>.
	 */
	public static String toHexString(byte[] b) {
		return toHexString(b, 0, b.length);
	}

	/**
	 * Convert a byte array to a big-endian ordered hexadecimal string.
	 * 
	 * @param b
	 *            The bytes to convert.
	 * @return A hexadecimal representation to <tt>b</tt>.
	 */
	public static String toHexString(byte[] b, int off, int len) {
		char[] buf = new char[len * 2];
		for (int i = 0, j = 0, k; i < len;) {
			k = b[off + i++];
			buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
			buf[j++] = HEX_DIGITS[k & 0x0F];
		}
		return new String(buf);
	}
}