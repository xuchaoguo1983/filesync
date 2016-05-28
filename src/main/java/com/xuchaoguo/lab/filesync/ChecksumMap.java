package com.xuchaoguo.lab.filesync;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.xuchaoguo.lab.filesync.ChecksumPair.StrongKey;

/**
 * fast map for checksum two key search
 * 
 * @author xuchaoguo
 * 
 */
public class ChecksumMap {
	private Map<Integer, List<ChecksumPair>> dataMap = new HashMap<>();

	public ChecksumMap() {

	}

	public void reset(List<ChecksumPair> pairs) {
		dataMap.clear();

		if (pairs != null && pairs.size() > 0) {
			for (ChecksumPair pair : pairs)
				this.add(pair);
		}
	}

	public void add(ChecksumPair pair) {
		List<ChecksumPair> list = dataMap.get(pair.getWeak());
		if (list == null) {
			list = new LinkedList<>();
			dataMap.put(pair.getWeak(), list);
		}

		list.add(pair);
	}

	public boolean isExist(Integer week) {
		return dataMap.containsKey(week);
	}

	public ChecksumPair getByStrong(Integer week, StrongKey strongKey) {
		List<ChecksumPair> pairs = dataMap.get(week);
		if (pairs == null || pairs.size() == 0)
			return null;

		for (ChecksumPair pair : pairs) {
			if (pair.getStrong().equals(strongKey))
				return pair;
		}

		return null;
	}
}
