package com.xuchaoguo.lab.filesync;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A Configuration is a mere collection of objects and values that compose a
 * particular configuration for the algorithm, for example the message digest
 * that computes the strong checksum.
 * 
 * <p>
 * Usage of a Configuration involves setting the member fields of this object to
 * thier appropriate values; thus, it is up to the programmer to specify the
 * {@link #strongSum}, {@link #weakSum}, {@link #blockLength} and
 * {@link #strongSumLength} to be used. The other fields are optional.
 * </p>
 * 
 */
public class Configuration implements Cloneable, java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// Constants and variables.
	// ------------------------------------------------------------------------

	/**
	 * The default block size.
	 */
	public static final int BLOCK_LENGTH = 1024;

	/**
	 * The default strong sum length
	 */
	public static final int STRONG_LENGTH = 8;

	/**
	 * The default chunk size.
	 */
	public static final int CHUNK_SIZE = 32768;

	public static final short CHAR_OFFSET = 31;

	/**
	 * The message digest that computes the stronger checksum.
	 */
	public transient MessageDigest strongSum;

	/**
	 * The rolling checksum.
	 */
	public transient RollingChecksum weakSum;

	/**
	 * The length of blocks to checksum.
	 */
	public int blockLength;

	/**
	 * The effective length of the strong sum.
	 */
	public int strongSumLength;

	/**
	 * Whether or not to do run-length encoding when making Deltas.
	 */
	public boolean doRunLength;

	/**
	 * The seed for the checksum, to perturb the strong checksum and help avoid
	 * collisions in plain rsync (or in similar applicaitons).
	 */
	public byte[] checksumSeed;

	/**
	 * The maximum size of byte arrays to create, when they are needed. This
	 * vale defaults to 32 kilobytes.
	 */
	public int chunkSize;

	// Constructors.
	// ------------------------------------------------------------------------

	public Configuration() throws NoSuchAlgorithmException {
		blockLength = BLOCK_LENGTH;
		strongSumLength = STRONG_LENGTH;
		chunkSize = CHUNK_SIZE;
		strongSum = MessageDigest.getInstance("MD4");
		weakSum = new Checksum32(CHAR_OFFSET);
	}

	/**
	 * Private copying constructor.
	 */
	private Configuration(Configuration that) {
		try {
			this.strongSum = (MessageDigest) (that.strongSum != null ? that.strongSum
					.clone() : null);
		} catch (CloneNotSupportedException cnse) {
			try {
				this.strongSum = MessageDigest.getInstance(that.strongSum
						.getAlgorithm());
			} catch (NoSuchAlgorithmException nsae) {
				// Fucked up situation. We die now.
				throw new Error(nsae);
			}
		}
		this.weakSum = (RollingChecksum) (that.weakSum != null ? that.weakSum
				.clone() : null);
		this.blockLength = that.blockLength;
		this.doRunLength = that.doRunLength;
		this.strongSumLength = that.strongSumLength;
		this.checksumSeed = (byte[]) (that.checksumSeed != null ? that.checksumSeed
				.clone() : null);
		this.chunkSize = that.chunkSize;
	}

	// Instance methods.
	// -----------------------------------------------------------------------

	public Object clone() {
		return new Configuration(this);
	}

	// Serialization methods.
	// -----------------------------------------------------------------------

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeUTF(strongSum != null ? strongSum.getAlgorithm() : "NONE");
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		String s = in.readUTF();
		if (!s.equals("NONE")) {
			try {
				strongSum = MessageDigest.getInstance(s);
			} catch (NoSuchAlgorithmException nsae) {
				throw new java.io.InvalidObjectException(nsae.getMessage());
			}
		}
	}

}
