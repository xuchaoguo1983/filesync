package com.xuchaoguo.lab.rync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Security;
import java.util.List;

import com.xuchaoguo.lab.filesync.ChecksumPair;
import com.xuchaoguo.lab.filesync.Configuration;
import com.xuchaoguo.lab.filesync.Delta;
import com.xuchaoguo.lab.filesync.Rdiff;
import com.xuchaoguo.lab.filesync.RsyncProvider;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
		Security.addProvider(new RsyncProvider());

		try {
			Configuration c = new Configuration();
			Rdiff rdf = new Rdiff(c);
			// 1. 计算获得客户端（待同步文件）的文件签名
			List<ChecksumPair> pairs = rdf.makeSignatures(new FileInputStream(
					"/Users/xuchaoguo/temp/rsync/sig/client.doc"));
			// 2. 将文件签名写入输出流
			rdf.writeSignatures(pairs, new FileOutputStream(
					"/Users/xuchaoguo/temp/rsync/sig/sig.txt"));
			// 3. 从输入流中读取解析文件签名
			List<ChecksumPair> pairs2 = rdf.readSignatures(new FileInputStream(
					"/Users/xuchaoguo/temp/rsync/sig/sig.txt"));

			// 4. 根据签名文件，服务器与最新文件比较得出差异（增量文件）
			List<Delta> deltas = rdf.makeDeltas(pairs2, new FileInputStream(
					"/Users/xuchaoguo/temp/rsync/sig/server.doc"));
			// 5. 服务器保存差异文件到输出流
			rdf.writeDeltas(deltas, new FileOutputStream(
					"/Users/xuchaoguo/temp/rsync/sig/delta.txt"));
			// 6. 客户端读取差异流
			List<Delta> deltas2 = rdf.readDeltas(new FileInputStream(
					"/Users/xuchaoguo/temp/rsync/sig/delta.txt"));
			// 7. 客户端根据差异流，生成最新的文件
			rdf.rebuildFile(new File(
					"/Users/xuchaoguo/temp/rsync/sig/client.doc"), deltas2,
					new FileOutputStream(
							"/Users/xuchaoguo/temp/rsync/sig/latest.doc"));

			// 8. 比较文件是否同步完成
			// TODO:

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
