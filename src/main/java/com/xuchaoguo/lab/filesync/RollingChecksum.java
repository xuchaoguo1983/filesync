package com.xuchaoguo.lab.filesync;

/**
 * A general interface for 32-bit checksums that have the "rolling" property.
 */
public interface RollingChecksum extends Cloneable, java.io.Serializable {

	// Methods.
	// -----------------------------------------------------------------------

	/**
	 * Returns the currently-computed 32-bit checksum.
	 * 
	 * @return The checksum.
	 */
	int getValue();

	/**
	 * Resets the internal state of the checksum, so it may be re-used later.
	 */
	void reset();

	/**
	 * Update the checksum with a single byte. This is where the "rolling"
	 * method is used.
	 * 
	 * @param bt
	 *            The next byte.
	 */
	void roll(byte bt);

	/**
	 * Update the checksum by simply "trimming" the least-recently-updated byte
	 * from the internal state. Most, but not all, checksums can support this.
	 */
	void trim();

	/**
	 * Replaces the current internal state with entirely new data.
	 * 
	 * @param buf
	 *            The bytes to checksum.
	 * @param offset
	 *            The offset into <code>buf</code> to start reading.
	 * @param length
	 *            The number of bytes to update.
	 */
	void check(byte[] buf, int offset, int length);

	/**
	 * Copies this checksum instance into a new instance. This method should be
	 * optional, and only implemented if the class implements the
	 * {@link java.lang.Cloneable} interface.
	 * 
	 * @return A clone of this instance.
	 */
	Object clone();

	/**
	 * Tests if a particular checksum is equal to this checksum. This means that
	 * the other object is an instance of this class, and its internal state
	 * equals this checksum's internal state.
	 * 
	 * @param o
	 *            The object to test.
	 * @return <code>true</code> if this checksum equals the other checksum.
	 */
	boolean equals(Object o);
}
