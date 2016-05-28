package com.xuchaoguo.lab.filesync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import com.xuchaoguo.lab.filesync.ChecksumPair.StrongKey;

/**
 * <p>
 * Methods for performing the checksum search. The result of a search is a
 * {@link java.util.List} of {@link Delta} objects that, when applied to a
 * method in {@link Rebuilder}, will reconstruct the new version of the data.
 * </p>
 * 
 * @author xuchaoguo
 */
public final class Matcher {
	// Constants and variables.
	// -----------------------------------------------------------------
	/**
	 * Our configuration. Contains such things as our rolling checksum and
	 * message digest.
	 */
	private final Configuration config;

	// Constructors.
	// -----------------------------------------------------------------

	/**
	 * Create a matcher with the specified configuration.
	 * 
	 * @param config
	 *            The {@link Configuration} for this Matcher.
	 */
	public Matcher(Configuration config) {
		this.config = config;
	}

	// Instance methods.
	// -----------------------------------------------------------------

	/**
	 * Search a portion of a byte buffer.
	 * 
	 * @param sums
	 *            The checksums to search for.
	 * @param buf
	 *            The data buffer to search.
	 * @param offset
	 *            The offset in <code>buf</code> to begin.
	 * @param len
	 *            The number of bytes to search from <code>buf</code>.
	 * @param baseOffset
	 *            The offset from whence <code>buf</code> came.
	 * @return A collection of {@link Delta}s derived from this search.
	 */
	private List<Delta> hashSearch(ChecksumMap map, byte[] buf, int offset,
			int len) {
		List<Delta> deltas = new LinkedList<>();

		int blockSize = Math.min(config.blockLength, len);
		byte[] block = new byte[blockSize];
		byte[] bytes = new byte[config.strongSumLength];

		int i = offset;
		int rest = len;
		int idx = 0;
		int j = 0;
		do {
			blockSize = Math.min(config.blockLength, rest);
			System.arraycopy(buf, i, block, 0, blockSize);
			rest -= blockSize;

			config.weakSum.check(block, 0, blockSize);

			for (j = 0; j <= rest; j++) {
				if (j > 0) {
					config.weakSum.roll(buf[i + blockSize + j - 1]);
				}

				int weak = config.weakSum.getValue();

				if (map.isExist(weak)) {
					config.strongSum.reset();
					config.strongSum.update(buf, i + j, blockSize);
					if (config.checksumSeed != null) {
						config.strongSum.update(config.checksumSeed);
					}

					System.arraycopy(config.strongSum.digest(), 0, bytes, 0,
							bytes.length);

					ChecksumPair pair = map.getByStrong(weak, new StrongKey(
							bytes));
					if (pair != null) {
						// matched
						if (j > 0) {
							DataBlock d = new DataBlock(idx, buf, i, j);
							deltas.add(d);
							idx += j;
						}

						Offsets o = new Offsets(pair.getOffset(), idx,
								blockSize);
						deltas.add(o);

						idx += blockSize;
						i += (blockSize + j);
						break;
					}
				}
			}

			if (j > rest) {
				// no match
				DataBlock d = new DataBlock(idx, buf, i, rest + blockSize);
				deltas.add(d);
				break;
			} else {
				rest -= j;
			}
		} while (rest > 0);

		return deltas;
	}

	/**
	 * Search a file.
	 * 
	 * @param sums
	 *            The checksums to search for.
	 * @param f
	 *            The file to search.
	 * @return A list of {@link Delta}s derived from this search.
	 * @throws IOException
	 *             If <i>f</i> cannot be read.
	 */
	public List<Delta> hashSearch(List<ChecksumPair> sums, File f)
			throws IOException {
		return hashSearch(sums, new FileInputStream(f));
	}

	/**
	 * Search an input stream.
	 * 
	 * @param sums
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public List<Delta> hashSearch(List<ChecksumPair> sums, InputStream in)
			throws IOException {
		List<Delta> deltas = new LinkedList<>();

		byte[] buffer = new byte[config.chunkSize];
		int len = 0;

		ChecksumMap map = new ChecksumMap();
		map.reset(sums);

		while ((len = in.read(buffer)) != -1) {
			List<Delta> list = this.hashSearch(map, buffer, 0, len);

			deltas.addAll(list);
		}

		return deltas;
	}
}
