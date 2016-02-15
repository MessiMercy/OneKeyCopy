package com.fetchContent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class Test {

	public static void main(String[] args) {
		GetContent content = new AliContent(
				"http://detail.1688.com/offer/526083361591.html?spm=a2604.7906952.1998923086.4.FsXhNf");
		System.out.println(content.getTitle());
		System.out.println(content.getPrice());
		// System.out.println(content.getAttributeList());
		HashMap<String, String> map = content.smallStock;
		System.out.println(map.size());
		System.out.println("¿â´æ = " + content.count);
		System.out.println(content.getAuctionPics().size() + "------------" + content.getDescPics().size());
		System.out.println("propertysize = " + content.propertyType.size());
		Iterator<Entry<String, String>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> entry = it.next();
			System.out.println(entry.getKey() + "---" + entry.getValue());
		}
	}

}
