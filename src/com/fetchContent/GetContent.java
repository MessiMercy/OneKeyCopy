package com.fetchContent;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class GetContent {
	public CloseableHttpClient client = CrawlerLib.getInstanceClient(false);
	protected String itemJson = null;
	protected String title = null;
	protected String price = null;
	protected String mainPic = null;
	protected String itemDesc = null;
	protected String html = null;
	protected ArrayList<String> auctionPics = new ArrayList<>();
	protected ArrayList<String> descPics = new ArrayList<>();
	protected LinkedHashMap<String, String> attributeList = new LinkedHashMap<>();
	protected LinkedHashMap<String, ArrayList<String>> propertyType = new LinkedHashMap<>();// 尺码颜色等小分类key和value
	protected HashMap<String, String> smallStock = new HashMap<>();// 小库存
	protected String count = null;// 总库存

	public String getItemJson() {
		return itemJson;
	}

	protected void setItemJson(String itemJson) {
		this.itemJson = itemJson;
	}

	public GetContent(String url) {
		html = getHtml(url);
		String script = findScript(html);
		String json = handleScript(script);
		setValues(json);
		setExtraValues(html);
	}

	public HashMap<String, String> getAttributeList() {
		return attributeList;
	}

	public void setAttributeList(LinkedHashMap<String, String> attributeList) {
		this.attributeList = attributeList;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPrice() {
		return price;
	}

	public String getMainPic() {
		return mainPic;
	}

	public void setMainPic(String mainPic) {
		this.mainPic = mainPic;
	}

	public String getItemDesc() {
		return itemDesc;
	}

	public void setItemDesc(String itemDesc) {
		this.itemDesc = itemDesc;
	}

	public ArrayList<String> getAuctionPics() {
		return auctionPics;
	}

	public ArrayList<String> getDescPics() {
		return descPics;
	}

	public static void main(String[] args) {
		// String html =
		// getHtml("https://item.taobao.com/item.htm?id=521160410707");
		// String script = findScript(html);
		// String jsonStr = handleScript(script);
		// JsonElement element = new JsonParser().parse(new JsonReader(new
		// StringReader(jsonStr)));
		// System.out.println();
		// CrawlerLib.printResult(script, false);
	}

	/**
	 * 默认为gbk编码,得到网页的html代码
	 * 
	 */
	protected String getHtml(String url) {
		if (!url.startsWith("http")) {
			url = "http:" + url;
		}
		if (url.contains("\'")) {
			url = url.replaceAll("\'", "");
		}
		HttpUriRequest request = CrawlerLib.getResponse(url, null, null, null);
		HttpResponse response = null;
		String html = null;
		try {
			response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode > 199 && statusCode < 300) {
				html = EntityUtils.toString(response.getEntity(), "gbk");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return html;

	}

	protected String getHtml(String url, String refer) {
		if (!url.startsWith("http")) {
			url = "http:" + url;
		}
		if (url.contains("\'")) {
			url = url.replaceAll("\'", "");
		}
		HttpUriRequest request = CrawlerLib.getResponse(url, null, null, null);
		request.addHeader("Referer", refer);
		HttpResponse response = null;
		String html = null;
		try {
			response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode > 199 && statusCode < 300) {
				html = EntityUtils.toString(response.getEntity(), "gbk");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return html;

	}

	/**
	 * 在返回的html中找到包含商品信息的json串
	 */
	protected String findScript(String html) {
		String startStr = "var g_config";
		String endStr = "g_config.tadInfo =";
		int leftIndex = html.indexOf(startStr) + 14;
		int rightIndex = html.indexOf(endStr) - 1;
		if (rightIndex > leftIndex) {
			return html.substring(leftIndex, rightIndex);
		}
		return html;

	}

	protected String findDesc(String html) {
		String desc = "";
		String regex = "descUrl\\s+:.*?\\n";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);
		if (matcher.find()) {
			desc = matcher.group();
		}
		return desc;

	}

	protected String findDescUrl(String desc) {
		String regex = "'.*?'";
		String descUrl = "";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(desc);
		while (matcher.find()) {
			String temp = matcher.group();
			if (temp.contains("com")) {
				descUrl = temp;
				return descUrl;
			}
		}
		return descUrl;

	}

	protected String handleScript(String script) {
		String html = script;
		String desc = findDesc(script);
		html = html.replace(desc, "");
		html = html.replace("+new Date", "'a'");
		// JsonReader reader2 = new JsonReader(new StringReader(html));
		// reader2.setLenient(true);
		// JsonElement element = new JsonParser().parse(reader2);
		// JsonElement element = new JsonParser().parse(html);
		// String str = element.getAsJsonObject().get("shopName").getAsString();
		// System.out.println(str);
		return html;

	}

	/** 从网页的html中抓取相应参数 */
	protected void setExtraValues(String html) {
		Document doc = Jsoup.parse(html);
		String price = doc.select("input[name=current_price]").first().attr("value");
		Elements elements = doc.select("li[title]");
		System.out.println(elements.size());
		for (Element element : elements) {
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
		this.price = price;
		findPropertyType(doc);
	}

	/** 从提取的json中设置相应参数 */
	protected void setValues(String json) {
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(true);
		JsonElement element = new JsonParser().parse(reader);
		JsonObject item = element.getAsJsonObject().get("idata").getAsJsonObject().get("item").getAsJsonObject();
		String title = item.get("title").getAsString();
		String mainPic = item.get("pic").getAsString();
		JsonArray array = item.get("auctionImages").getAsJsonArray();
		for (JsonElement jsonElement : array) {
			auctionPics.add(jsonElement.getAsString());
		}
		String desc = findDesc(html);
		String descUrl = findDescUrl(desc);
		String descHtml = getHtml(descUrl);
		setTitle(title);
		setMainPic(mainPic);
		setItemDesc(descHtml);
		findImgInDesc(descHtml);
		findCount(element);
		setItemJson(item.toString());
	}

	protected void findCount(JsonElement element) {
		String sibUrl = null;
		String itemId = null;
		String count = null;
		String refer = null;
		String regex = "virtQuantity\":\"\\w+?\"";
		JsonObject object = element.getAsJsonObject();
		itemId = object.get("itemId").getAsString();
		sibUrl = object.get("sibUrl").getAsString();
		refer = "https://item.taobao.com/item.htm?id=" + itemId;
		String html = getHtml(sibUrl, refer);
		// CrawlerLib.getResponse(sibUrl, null, null, null);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);
		if (matcher.find()) {
			String find = matcher.group();
			count = find.substring(15, find.length() - 1);
		}
		this.count = count;
		findSmallStock(html);
	}

	protected void findPropertyType(Document doc) {
		Elements elements = doc.select("ul[data-property]");
		if (elements.size() == 0) {
			System.out.println("-----------------no property type");
			return;
		}
		for (Element element : elements) {
			ArrayList<String> list = new ArrayList<>();
			Elements elements2 = element.getElementsByTag("span");
			// System.out.println(element.text());
			String key = element.attr("data-property");// 属性名
			System.out.println("key = " + key);
			for (Element element2 : elements2) {
				// System.out.println(element2.text());// 属性值
				String styleUrl = null;
				String dataValue = null;
				if (element2.parent().hasAttr("style")) {
					dataValue = element2.parent().parent().attr("data-value");
					String temp = element2.parent().attr("style");
					int left = temp.indexOf("(") + 1;
					int right = temp.indexOf(")");
					styleUrl = "http:" + temp.substring(left, right);
				}
				if (styleUrl != null) {
					list.add(dataValue + "#" + element2.text() + "#" + styleUrl);
				} else {
					list.add(dataValue + "#" + element2.text());
				}
			}
			System.out.println("listsize = " + list.size());
			propertyType.put(key, list);
		}

	}

	protected void findSmallStock(String sibHtml) {
		if (sibHtml == null || !sibHtml.contains("sku")) {
			return;
		}
		String left = "\"sku\"";
		String right = "g_config.PointData=";
		int leftIndex = sibHtml.indexOf(left) + 6;
		int rightIndex = sibHtml.indexOf(right) - 2;
		if (rightIndex <= leftIndex) {
			return;
		}
		// System.out.println(rightIndex - leftIndex + " " + (rightIndex >
		// leftIndex ? true : false));
		String json = sibHtml.substring(leftIndex, rightIndex);
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(true);
		JsonElement parser = new JsonParser().parse(reader);
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

	protected void findImgInDesc(String html) {
		if (!html.equals("")) {
			String regex = "img\\.alicdn\\.com/.*?\\.(png|jpg|jpeg)";
			// http.*?\.alicdn\.com/.*?\.(png|jpg|jpeg)
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			while (matcher.find()) {
				descPics.add(matcher.group());
			}
		}
	}

}
