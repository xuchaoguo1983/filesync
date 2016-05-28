package com.xuchaoguo.lab.filesync;

import java.util.Arrays;


/**
 * A pair of weak and strong checksums for use with the Rsync algorithm. The
 * weak "rolling" checksum is typically a 32-bit sum derived from the Adler32
 * algorithm; the strong checksum is usually a 128-bit MD4 checksum.
 * 
 */
public class ChecksumPair implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// Constants and variables.
	// -------------------------------------------------------------------------

	/**
	 * The weak, rolling checksum.
	 * 
	 * @since 1.1
	 */
	int weak;

	/**
	 * The strong checksum.
	 * 
	 * @since 1.1
	 */
	StrongKey strong;

	/**
	 * The offset in the original data where this pair was generated.
	 */
	long offset;

	/** The number of bytes these sums are over. */
	int length;

	/** The sequence number of these sums. */
	int seq;

	// Constructors.
	// -------------------------------------------------------------------------

	/**
	 * Create a new checksum pair.
	 * 
	 * @param weak
	 *            The weak, rolling checksum.
	 * @param strong
	 *            The strong checksum.
	 * @param offset
	 *            The offset at which this checksum was computed.
	 * @param length
	 *            The length of the data over which this sum was computed.
	 * @param seq
	 *            The sequence number of this checksum pair.
	 */
	public ChecksumPair(int weak, byte[] strong, long offset, int length,
			int seq) {
		this.weak = weak;
		this.strong = new StrongKey(strong);
		this.offset = offset;
		this.length = length;
		this.seq = seq;
	}

	/**
	 * Create a new checksum pair with no length or sequence fields.
	 * 
	 * @param weak
	 *            The weak checksum.
	 * @param strong
	 *            The strong checksum.
	 * @param offset
	 *            The offset at which this checksum was computed.
	 */
	public ChecksumPair(int weak, byte[] strong, long offset) {
		this(weak, strong, offset, 0, 0);
	}

	/**
	 * Create a new checksum pair with no associated offset.
	 * 
	 * @param weak
	 *            The weak checksum.
	 * @param strong
	 *            The strong checksum.
	 */
	public ChecksumPair(int weak, byte[] strong) {
		this(weak, strong, -1L, 0, 0);
	}

	/**
	 * Default 0-arguments constructor for package access.
	 */
	ChecksumPair() {
	}

	// Instance methods.
	// -------------------------------------------------------------------------

	/**
	 * Get the weak checksum.
	 * 
	 * @return The weak checksum.
	 * @since 1.1
	 */
	public int getWeak() {
		return weak;
	}

	/**
	 * Get the strong checksum.
	 * 
	 * @return The strong checksum.
	 * @since 1.1
	 */
	public StrongKey getStrong() {
		return strong;
	}

	/**
	 * Return the offset from where this checksum pair was generated.
	 * 
	 * @return The offset.
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * Return the length of the data for which this checksum pair was generated.
	 * 
	 * @return The length.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Return the sequence number of this checksum pair, if any.
	 * 
	 * @return The sequence number.
	 */
	public int getSequence() {
		return seq;
	}

	// Public instance methods overriding java.lang.Object.
	// -------------------------------------------------------------------------

	public int hashCode() {
		return weak;
	}

	/**
	 * We define equality for this object as equality between two weak sums and
	 * equality between two strong sums.
	 * 
	 * @param obj
	 *            The Object to test.
	 * @return True if both checksum pairs are equal.
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof StrongKey))
			throw new ClassCastException(obj.getClass().getName());		
		
		ChecksumPair pair = (ChecksumPair)obj;
		
		if (this == pair)
			return true;
		
		return weak == (pair).weak
				&& this.strong.equals(pair.getStrong());
	}

	/**
	 * Returns a String representation of this pair.
	 * 
	 * @return The String representation of this pair.
	 * @since 1.2
	 */
	public String toString() {
		return "len=" + length + " offset=" + offset + " weak=" + weak
				+ " strong=" + Util.toHexString(strong.getBytes());
	}
	
	public static class StrongKey implements java.io.Serializable, Comparable<StrongKey> {
		private static final long serialVersionUID = 1L;
		// Constants and variables.
		// --------------------------------------------------------------

		/**
		 * The key itself. An array of some number of bytes.
		 * 
		 * @since 1.0
		 */
		protected byte[] key;

		// Constructors.
		// --------------------------------------------------------------

		/**
		 * Create a new key with the specified bytes. <code>key</code> can be
		 * <code>null</code>.
		 * 
		 * @since 1.0
		 * @param key
		 *            The bytes that will make up this key.
		 */
		StrongKey(byte[] key) {
			if (key != null)
				this.key = (byte[]) key.clone();
			else
				this.key = key;
		}

		// Instance methods.
		// --------------------------------------------------------------

		/**
		 * Return the bytes that compose this Key.
		 * 
		 * @since 1.0
		 * @return {@link #key}, the bytes that compose this key.
		 */
		public byte[] getBytes() {
			if (key != null)
				return (byte[]) key.clone();
			return null;
		}

		/**
		 * Set the byte array that composes this key.
		 * 
		 * @since 1.0
		 * @param key
		 *            The bytes that will compose this key.
		 */
		public void setBytes(byte[] key) {
			if (key != null)
				this.key = (byte[]) key.clone();
			else
				this.key = key;
		}

		/**
		 * The length, in bytes, of this key.
		 * 
		 * @since 1.0
		 * @return The length of this key in bytes.
		 */
		public int length() {
			if (key != null)
				return key.length;
			return 0;
		}

		// Public instance methods overriding java.lang.Object -----------

		/**
		 * Return a zero-padded hexadecimal string representing this key.
		 * 
		 * @return A hexadecimal string of the bytes in {@link #key}.
		 * @since 1.0
		 */
		public String toString() {
			if (key == null || key.length == 0)
				return "nil";
			return Util.toHexString(key);
		}

		/**
		 * The hash code for this key. This is defined as the XOR of all 32-bit
		 * blocks of the {@link #key} array.
		 * 
		 * @return The hash code for this key.
		 * @since 1.0
		 */
		public int hashCode() {
			if (key == null)
				return 0;
			int code = 0;
			for (int i = key.length - 1; i >= 0; i--)
				code ^= ((int) key[i] & 0xff) << (((key.length - i - 1) * 8) % 32);
			return code;
		}

		/**
		 * Test if this key equals another. Two keys are equal if the method
		 * {@link java.util.Arrays#equals(byte[],byte[])} returns true for thier key
		 * arrays.
		 * 
		 * @since 1.0
		 * @throws java.lang.ClassCastException
		 *             If o is not a StrongKey.
		 * @param o
		 *            The object to compare to.
		 * @return <tt>true</tt> If this key is equivalent to the argument.
		 */
		public boolean equals(Object o) {
			return Arrays.equals(key, ((StrongKey) o).key);
		}

		// java.lang.Comparable interface implementation -----------------

		/**
		 * Compare this object to another. This method returns an integer value less
		 * than, equal to, or greater than zero if this key is less than, equal to,
		 * or greater than the given key. This method will return
		 * <ul>
		 * <li>0 if the {@link #key} fields are references to the same array.
		 * <li>1 if {@link #key} in this class is null.
		 * <li>-1 if {@link #key} in <tt>o</tt> is null (null is always less than
		 * everything).
		 * <li>0 if the lengths of the {@link #key} arrays are the same and their
		 * contents are equivalent.
		 * <li>The difference between the lengths of the keys if different.
		 * <li>The difference between the first two different members of the arrays.
		 * </ul>
		 * 
		 * @since 1.0
		 * @throws java.lang.ClassCastException
		 *             If o is not a StrongKey.
		 * @param o
		 *            The key to compare to.
		 * @return An integer derived from the differences of the two keys.
		 */
		public int compareTo(StrongKey sk) {
			if (key == sk.key)
				return 0;
			if (key == null)
				return 1;
			if (sk.key == null)
				return -1;

			if (key.length != sk.length())
				return key.length - sk.length();

			byte[] arr = sk.getBytes();
			for (int i = 0; i < key.length; i++)
				if (key[i] != arr[i])
					return (key[i] - arr[i]);
			return 0;
		}
	}

}
