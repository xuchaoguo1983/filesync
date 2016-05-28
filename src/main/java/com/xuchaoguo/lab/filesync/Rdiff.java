package com.xuchaoguo.lab.filesync;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Rdiff {
	/** Rdiff/rproxy signature magic. */
	public static final int SIG_MAGIC = 0x72730136;

	/** Rdiff/rproxy delta magic. */
	public static final int DELTA_MAGIC = 0x72730236;

	public static final short CHAR_OFFSET = 31;

	public static final byte OP_END = 0x00;

	public static final byte OP_LITERAL_N1 = 0x41;
	public static final byte OP_LITERAL_N2 = 0x42;
	public static final byte OP_LITERAL_N4 = 0x43;
	public static final byte OP_LITERAL_N8 = 0x44;

	public static final byte OP_COPY_N4_N4 = 0x4f;

	/**
	 * The checksum configuration
	 */
	private Configuration config;

	// Constructors.
	// -----------------------------------------------------------------
	public Rdiff(Configuration c) {
		config = c;
	}

	/**
	 * Make the signatures from data coming in through the input stream.
	 * 
	 * @param in
	 *            The input stream to generate signatures for.
	 * @return A List of signatures.
	 * @throws java.io.IOException
	 *             If reading fails.
	 */
	public List<ChecksumPair> makeSignatures(InputStream in)
			throws IOException, NoSuchAlgorithmException {
		return new Generator(config).generateSums(in);
	}

	/**
	 * Write the signatures to the specified output stream.
	 * 
	 * @param sigs
	 *            The signatures to write.
	 * @param out
	 *            The OutputStream to write to.
	 * @throws java.io.IOException
	 *             If writing fails.
	 */
	public void writeSignatures(List<ChecksumPair> sigs, OutputStream out)
			throws IOException {
		writeInt(SIG_MAGIC, out);
		writeInt(config.blockLength, out);
		writeInt(config.strongSumLength, out);

		for (Iterator<ChecksumPair> i = sigs.iterator(); i.hasNext();) {
			ChecksumPair pair = i.next();
			writeInt(pair.getWeak(), out);
			out.write(pair.getStrong().getBytes(), 0, config.strongSumLength);
		}
	}

	/**
	 * Read the signatures from the input stream.
	 * 
	 * @param in
	 *            The InputStream to read the signatures from.
	 * @return A collection of {@link ChecksumPair}s read.
	 * @throws java.io.IOException
	 *             If the input stream is malformed.
	 */
	public List<ChecksumPair> readSignatures(InputStream in) throws IOException {
		List<ChecksumPair> sigs = new LinkedList<>();
		int header = readInt(in);
		if (header != SIG_MAGIC) {
			throw new IOException("Bad signature header: 0x"
					+ Integer.toHexString(header));
		}
		long off = 0;
		config.blockLength = readInt(in);
		config.strongSumLength = readInt(in);

		int weak;
		byte[] strong = new byte[config.strongSumLength];
		do {
			try {
				weak = readInt(in);
				int len = in.read(strong);
				if (len < config.strongSumLength)
					break;
				sigs.add(new ChecksumPair(weak, strong, off));
				off += config.blockLength;
			} catch (EOFException eof) {
				break;
			}
		} while (true);
		return sigs;
	}

	/**
	 * Make a collection of {@link Delta}s from the given sums and InputStream.
	 * 
	 * @param sums
	 *            A collection of {@link ChecksumPair}s generated from the "old"
	 *            file.
	 * @param in
	 *            The InputStream for the "new" file.
	 * @return A collection of {@link Delta}s that will patch the old file to
	 *         the new.
	 * @throws java.io.IOException
	 *             If reading fails.
	 */
	public List<Delta> makeDeltas(List<ChecksumPair> sums, InputStream in)
			throws IOException, NoSuchAlgorithmException {
		return new Matcher(config).hashSearch(sums, in);
	}

	/**
	 * Write deltas to an output stream.
	 * 
	 * @param deltas
	 *            A collection of {@link Delta}s to write.
	 * @param out
	 *            The OutputStream to write to.
	 * @throws java.io.IOException
	 *             If writing fails.
	 */
	public void writeDeltas(List<Delta> deltas, OutputStream out)
			throws IOException {
		writeInt(DELTA_MAGIC, out);
		for (Iterator<Delta> i = deltas.iterator(); i.hasNext();) {
			Delta o = i.next();
			if (o instanceof Offsets) {
				writeCopy((Offsets) o, out);
			} else if (o instanceof DataBlock) {
				writeLiteral((DataBlock) o, out);
			}
		}
		out.write(OP_END);
	}

	/**
	 * Read a collection of {@link Delta}s from the InputStream.
	 * 
	 * @param in
	 *            The InputStream to read from.
	 * @return A collection of {@link Delta}s read.
	 * @throws java.io.IOException
	 *             If the input stream is malformed.
	 */
	public List<Delta> readDeltas(InputStream in) throws IOException {
		List<Delta> deltas = new LinkedList<>();
		int header = readInt(in);
		if (header != DELTA_MAGIC) {
			throw new IOException("Bad delta header: 0x"
					+ Integer.toHexString(header));
		}
		int command;
		long offset = 0;
		byte[] buf;
		while ((command = in.read()) != -1) {
			switch (command) {
			case OP_END:
				return deltas;
			case OP_LITERAL_N1:
				buf = new byte[(int) readInt(1, in)];
				in.read(buf);
				deltas.add(new DataBlock(offset, buf));
				offset += buf.length;
				break;
			case OP_LITERAL_N2:
				buf = new byte[(int) readInt(2, in)];
				in.read(buf);
				deltas.add(new DataBlock(offset, buf));
				offset += buf.length;
				break;
			case OP_LITERAL_N4:
				buf = new byte[(int) readInt(4, in)];
				in.read(buf);
				deltas.add(new DataBlock(offset, buf));
				offset += buf.length;
				break;
			case OP_COPY_N4_N4:
				int oldOff = (int) readInt(4, in);
				int bs = (int) readInt(4, in);
				deltas.add(new Offsets(oldOff, offset, bs));
				offset += bs;
				break;
			default:
				throw new IOException("Bad delta command: 0x"
						+ Integer.toHexString(command));
			}
		}
		throw new IOException("Didn't recieve RS_OP_END.");
	}

	/**
	 * Patch the file <code>basis</code> using <code>deltas</code>, writing the
	 * patched file to <code>out</code>.
	 * 
	 * @param basis
	 *            The basis file.
	 * @param deltas
	 *            The collection of {@link Delta}s to apply.
	 * @param out
	 *            The OutputStream to write the patched file to.
	 * @throws java.io.IOException
	 *             If reading/writing fails.
	 */
	public void rebuildFile(File basis, List<Delta> deltas, OutputStream out)
			throws IOException {
		Collections.sort(deltas, new Comparator<Delta>() {
			@Override
			public int compare(Delta o1, Delta o2) {
				return o1.getWriteOffset() > o2.getWriteOffset() ? 1 : -1;
			}

		});

		RandomAccessFile f = new RandomAccessFile(basis, "r");

		byte[] buf = new byte[config.blockLength];
		for (Delta delta : deltas) {
			if (delta instanceof DataBlock) {
				out.write(((DataBlock) delta).getData());
			} else {
				f.seek(((Offsets) delta).getOldOffset());
				int len = 0, total = 0;
				do {
					len = f.read(buf);
					total += len;
					out.write(buf, 0, len);
				} while (total < delta.getBlockLength());
			}
		}

		f.close();
	}

	/**
	 * Read a four-byte big-endian integer from the InputStream.
	 * 
	 * @param in
	 *            The InputStream to read from.
	 * @return The integer read.
	 * @throws java.io.IOException
	 *             if reading fails.
	 */
	private static int readInt(InputStream in) throws IOException {
		int i = 0;
		for (int j = 3; j >= 0; j--) {
			int k = in.read();
			if (k == -1)
				throw new EOFException();
			i |= (k & 0xff) << 8 * j;
		}
		return i;
	}

	/**
	 * Write the lowest <code>len</code> bytes of <code>l</code> to
	 * <code>out</code> in big-endian byte order.
	 * 
	 * @param l
	 *            The integer to write.
	 * @param len
	 *            The number of bytes to write.
	 * @param out
	 *            The OutputStream to write to.
	 * @throws java.io.IOException
	 *             If writing fails.
	 */
	private static void writeInt(long l, int len, OutputStream out)
			throws IOException {
		for (int i = len - 1; i >= 0; i--) {
			out.write((int) (l >>> i * 8) & 0xff);
		}
	}

	/**
	 * Write a four-byte integer in big-endian byte order to <code>out</code>.
	 * 
	 * @param i
	 *            The integer to write.
	 * @param out
	 *            The OutputStream to write to.
	 * @throws java.io.IOException
	 *             If writing fails.
	 */
	private static void writeInt(int i, OutputStream out) throws IOException {
		out.write((byte) ((i >>> 24) & 0xff));
		out.write((byte) ((i >>> 16) & 0xff));
		out.write((byte) ((i >>> 8) & 0xff));
		out.write((byte) (i & 0xff));
	}

	/**
	 * Write a "COPY" command to <code>out</code>.
	 * 
	 * @param off
	 *            The {@link Offsets} object to write as a COPY command.
	 * @param out
	 *            The OutputStream to write to.
	 * @throws java.io.IOException
	 *             if writing fails.
	 */
	private static void writeCopy(Offsets off, OutputStream out)
			throws IOException {
		out.write(OP_COPY_N4_N4);
		writeInt(off.getOldOffset(), 4, out);
		writeInt(off.getBlockLength(), out);
	}

	/**
	 * Write a "LITERAL" command to <code>out</code>.
	 * 
	 * @param d
	 *            The {@link DataBlock} to write as a LITERAL command.
	 * @param out
	 *            The OutputStream to write to.
	 * @throws java.io.IOException
	 *             if writing fails.
	 */
	private static void writeLiteral(DataBlock d, OutputStream out)
			throws IOException {
		byte cmd = 0;
		int param_len;

		switch (param_len = integerLength(d.getBlockLength())) {
		case 1:
			cmd = OP_LITERAL_N1;
			break;
		case 2:
			cmd = OP_LITERAL_N2;
			break;
		case 4:
			cmd = OP_LITERAL_N4;
			break;
		}

		out.write(cmd);
		writeInt(d.getBlockLength(), param_len, out);
		out.write(d.getData());
	}

	/**
	 * Check if a long integer needs to be represented by 1, 2, 4 or 8 bytes.
	 * 
	 * @param l
	 *            The long to test.
	 * @return The effective length, in bytes, of the argument.
	 */
	private static int integerLength(long l) {
		if ((l & ~0xffL) == 0) {
			return 1;
		} else if ((l & ~0xffffL) == 0) {
			return 2;
		} else if ((l & ~0xffffffffL) == 0) {
			return 4;
		}
		return 8;
	}

	/**
	 * Read a variable-length integer from the input stream. This method reads
	 * <code>len</code> bytes from <code>in</code>, interpolating them as
	 * composing a big-endian integer.
	 * 
	 * @param len
	 *            The number of bytes to read.
	 * @param in
	 *            The InputStream to read from.
	 * @return The integer.
	 * @throws java.io.IOException
	 *             if reading fails.
	 */
	private static long readInt(int len, InputStream in) throws IOException {
		long i = 0;
		for (int j = len - 1; j >= 0; j--) {
			int k = in.read();
			if (k == -1)
				throw new EOFException();
			i |= (k & 0xff) << 8 * j;
		}
		return i;
	}
}
