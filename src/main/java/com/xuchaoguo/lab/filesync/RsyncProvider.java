package com.xuchaoguo.lab.filesync;

/**
 * This provider implements the MD4 message digest, and is provided to ensure
 * that MD4 is available.
 */
public final class RsyncProvider extends java.security.Provider {
	private static final long serialVersionUID = 1L;

	public RsyncProvider() {
		super("filesync", 1.0, "rsync provider; implementing MD4");

		put("MessageDigest.MD4", "com.xuchaoguo.lab.filesync.MD4");
	}
}
