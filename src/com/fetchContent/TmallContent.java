package com.fetchContent;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class TmallContent extends GetContent {

	public TmallContent(String url) {
		super(url);
	}

	protected String handleScript(String script) {
		return script;
	}

	protected String findScript(String html) {
		String startStr = "\"api\"";
		String endStr = "\"valTimeLeft\"";
		int leftIndex = html.indexOf(startStr) - 1;
		int rightIndex = html.indexOf(endStr) - 1;
		if (rightIndex > leftIndex) {
			return html.substring(leftIndex, rightIndex) + "}";
		}
		return html;

	}

	// protected String getHtml(String url) {
	// System.out.println("\\\\\\\\\\\\\\\\\\");
	// return url;
	//
	// }

	protected void setValues(String json) {
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(true);
		JsonElement element = new JsonParser().parse(reader);
		JsonObject itemDo = element.getAsJsonObject().get("itemDO").getAsJsonObject();
		setTitle(itemDo.get("title").getAsString());
		this.price = itemDo.get("reservePrice").getAsString();
		String descUrl = element.getAsJsonObject().get("api").getAsJsonObject().get("descUrl").getAsString();
		String descHtml = getHtml(descUrl);
		setItemDesc(descHtml);
		findImgInDesc(descHtml);
		findCount(element);
		setItemJson(itemDo.toString());
	}

	protected void findCount(JsonElement element) {
		String sibUrl = null;
		String itemId = null;
		String count = null;
		String refer = null;
		String regex = "icTotalQuantity\":(\\w+)";
		JsonObject object = element.getAsJsonObject();
		// itemId = object.get("itemId").getAsString();
		sibUrl = object.get("initApi").getAsString();
		itemId = object.get("rateConfig").getAsJsonObject().get("itemId").getAsString();
		refer = "https://detail.tmall.com/item.htm?id=" + itemId;
		String html = getHtml(sibUrl, refer);
		// CrawlerLib.getResponse(sibUrl, null, null, null);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);
		if (matcher.find()) {
			String find = matcher.group();
			count = find.substring(17, find.length());
		}
		this.count = count;

	}

	protected void findSmallStock(String html) {
		if (html == null || !html.contains("skuMap")) {
			return;
		}
		String left = "\"skuMap\"";
		String right = "\"valLoginIndicator\"";
		int leftIndex = html.indexOf(left) + 9;
		int rightIndex = html.indexOf(right) - 2;
		if (rightIndex <= leftIndex) {
			return;
		}
		// System.out.println(rightIndex - leftIndex + " " + (rightIndex >
		// leftIndex ? true : false));
		String json = html.substring(leftIndex, rightIndex);
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(true);
		JsonElement parser = null;
		try {
			parser = new JsonParser().parse(reader);
		} catch (Exception e) {
			System.out.println("没有小样");
			return;
		}
		Iterator<Entry<String, JsonElement>> it = parser.getAsJsonObject().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, JsonElement> entry = it.next();
			String key = entry.getKey();
			JsonElement element = entry.getValue();
			if (element.isJsonObject()) {
				smallStock.put(key, entry.getValue().getAsJsonObject().get("stock").getAsString());
			}
		}
	}

	protected void setExtraValues(String html) {
		Document doc = Jsoup.parse(html);
		// String mainPic = doc.select("img[src^=//img");
		Elements auctionImage = doc.select("img[src^=//img");
		for (int i = 1; i < auctionImage.size(); i++) {
			Element element = auctionImage.get(i);
			String smallImage = element.attr("src");
			String bigImage = "http:" + smallImage.replaceAll("60x60", "430x430");
			if (i == 1) {
				setMainPic(bigImage);
			}
			this.auctionPics.add(bigImage);
		}
		findSmallStock(html);
		setItemProperties(html);
		findPropertyType(doc);
	}

	protected void setItemProperties(String html) {
		Document doc = Jsoup.parse(html);
		Elements ItemPro = doc.select("li[title]");
		for (Element element : ItemPro) {
			String text = element.text();
			// System.out.println(text);
			if (text.contains(":") || text.contains("：")) {
				text = text.replaceAll("：", ":");
				String[] splits = text.split(":");
				if (splits.length == 2) {
					attributeList.put(splits[0], splits[1]);
				}
			}
		}

	}

	public static void main(String[] args) {
	}

}
