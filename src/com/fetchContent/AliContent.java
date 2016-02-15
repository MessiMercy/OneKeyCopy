package com.fetchContent;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class AliContent extends GetContent {
	private String itemConfig = null;

	public AliContent(String url) {
		super(url);
	}

	@Override
	protected String findScript(String html) {
		Document doc = Jsoup.parse(html);
		Elements elements = doc.select("script[type=text/javascript]");
		System.out.println("elements size = " + elements.size());
		String target = null;
		for (Element element : elements) {
			if (element.toString().contains("iDetailData")) {
				target = element.toString();
				break;
			}
		}
		System.out.println("----------\r\n");
		int left = target.indexOf("iDetailData") + 13;
		int right = target.lastIndexOf(";");
		System.out.println(left + "-------------" + right);
		try {
			int itemConfigLeft = target.indexOf("iDetailConfig") + 15;
			String configTarget = target.substring(itemConfigLeft, left);
			int itemConfigRight = configTarget.lastIndexOf(";");
			itemConfig = configTarget.substring(0, itemConfigRight);
		} catch (Exception e) {
			System.out.println("itemConfig find error");
		}
		target = target.substring(left, right);
		return target;
	}

	@Override
	protected String handleScript(String script) {
		return script;
	}

	@Override
	protected void setExtraValues(String html) {
		Document doc = Jsoup.parse(html);
		findCount(doc);
		setMainPic(findMainPic(doc));
		auctionPics = findAuctionPics(doc);
		setTitle(findTitle(doc));
		String descUrl = findDescUrl(doc);
		// assert (descUrl == null);
		System.out.println(descUrl);
		String descHtml = getHtml(descUrl);
		findImgInDesc(descHtml);
		price = findPrice(doc);
		setAttributeList(findProperties(doc));
	}

	@Override
	protected void setValues(String json) {
		findPropertyTypeAndSmallStock(json);
		setItemJson(itemConfig);
		return;
	}

	@Override
	protected void findImgInDesc(String html) {
		if (!html.equals("")) {
			String regex = "http.*?\\.alicdn\\.com/.*?\\.(png|jpg|jpeg)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			while (matcher.find()) {
				descPics.add(matcher.group());
			}
		}
	}

	private String findMainPic(Document doc) {
		Element element = doc.select("meta[property=og:image]").first();
		String mainPicUrl = null;
		if (element != null) {
			mainPicUrl = element.attr("content");
		}
		return mainPicUrl;

	}

	private ArrayList<String> findAuctionPics(Document doc) {
		ArrayList<String> list = new ArrayList<>();
		Elements elements = doc.select("a.box-img[hidefocus=true]").select("img[alt]");
		if (elements != null) {
			for (Element element : elements) {
				list.add(handleAuctionPic(element.attr("src")));
			}
		}
		return list;
	}

	private String handleAuctionPic(String string) {
		return string.replaceAll("60x60", "400x400");
	}

	private String findTitle(Document doc) {
		String title = null;
		Element element = doc.select("meta[property=og:title]").first();
		if (element != null) {
			title = element.attr("content");
		}
		return title;
	}

	private String findDescUrl(Document doc) {
		String desc = null;
		Element element = doc.select("div[id=desc-lazyload-container]").first();
		if (element != null) {
			desc = element.attr("data-tfs-url");
		}
		return desc;
	}

	private String findPrice(Document doc) {
		String price = null;
		Element priceElement = doc.select("meta[property=og:product:orgprice]").first();
		if (priceElement != null) {
			price = priceElement.attr("content");
		}
		return price;
	}

	private LinkedHashMap<String, String> findProperties(Document doc) {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		Elements keys = doc.select("td.de-feature");
		Elements values = doc.select("td.de-value");
		if (keys.size() == values.size()) {
			for (int i = 0; i < keys.size(); i++) {
				String key = keys.get(i).text();
				String value = values.get(i).text();
				map.put(key, value);
			}
		}
		return map;
	}

	private void findPropertyTypeAndSmallStock(String json) {
		// String left = "";
		// JsonElement element =
		String target = json;
		JsonReader reader = new JsonReader(new StringReader(target));
		reader.setLenient(true);
		JsonElement element = new JsonParser().parse(reader);
		JsonArray array = null;
		try {
			array = element.getAsJsonObject().get("sku").getAsJsonObject().get("skuProps").getAsJsonArray();
		} catch (NullPointerException e) {
			System.out.println("没有具体小项");
			return;
		}
		for (JsonElement jsonElement : array) {
			ArrayList<String> pro = new ArrayList<>();
			JsonObject object = jsonElement.getAsJsonObject();
			String key = object.get("prop").getAsString();
			// System.out.println(key);
			JsonArray value = object.get("value").getAsJsonArray();
			for (JsonElement jsonElement2 : value) {
				String value2 = "";
				value2 = jsonElement2.getAsJsonObject().get("name").getAsString();
				if (jsonElement2.getAsJsonObject().has("imageUrl")) {
					value2 += ("#" + jsonElement2.getAsJsonObject().get("imageUrl").getAsString());
				}
				// System.out.println(" " + value2);
				pro.add(value2);
			}
			propertyType.put(key, pro);
		}
		JsonObject skuMap = element.getAsJsonObject().get("sku").getAsJsonObject().get("skuMap").getAsJsonObject();
		Iterator<Entry<String, JsonElement>> it = skuMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, JsonElement> entry = it.next();
			// System.out.println(entry.getKey());
			String name = entry.getKey();
			String bookCount = entry.getValue().getAsJsonObject().get("canBookCount").getAsString();
			if (bookCount != null) {
				smallStock.put(name, bookCount);
			} else {
				smallStock.put(name, new Random().nextInt(10) + "");
			}
			// System.out.println(" " + bookCount);

		}

	}

	private void findCount(Document doc) {
		String str = null;
		try {
			str = doc.select("span.total").first().text();
		} catch (NullPointerException e) {
			return;
		}
		String regex = "\\w+";
		Matcher matcher = Pattern.compile(regex).matcher(str);
		if (matcher.find()) {
			count = matcher.group();
		}
		return;

	}

}
