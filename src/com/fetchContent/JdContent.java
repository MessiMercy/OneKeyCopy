package com.fetchContent;

import java.io.StringReader;
import java.util.ArrayList;
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

public class JdContent extends GetContent {
	String skUid = null;

	public JdContent(String url) {
		super(url);
	}

	@Override
	protected String findScript(String html) {
		int a = html.indexOf("var pageConfig =") + 16;
		int b = html.indexOf("common_config2") + 15;
		String script = html.substring(a, b) + "}}";
		return script;
	}

	@Override
	protected String handleScript(String script) {
		return script;
	}

	@Override
	protected void setExtraValues(String html) {
		Document doc = Jsoup.parse(html);
		findPropertyType(doc);
		Elements pics = doc.select("ul.lh").select("img[data-img=1]");
		if (pics.size() != 0) {
			setMainPic(changeToBigPic(pics.get(0).attr("src")));
			for (Element element : pics) {
				auctionPics.add(element.attr("src"));
			}
		}
		Elements attributes = doc.select("li[title]");
		for (Element element : attributes) {
			String value = element.text();
			if (value.contains("：") || value.contains(":")) {
				value = value.replaceAll("：", ":");
				String[] properties = value.split(":");
				attributeList.put(properties[0], properties[1]);
			}
		}
		// findImgInDesc(html);
	}

	@Override
	protected void setValues(String json) {
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(true);
		JsonElement element = new JsonParser().parse(reader);
		JsonObject product = element.getAsJsonObject().get("product").getAsJsonObject();
		skUid = product.get("skuid").getAsString();
		setPrice(skUid);
		setTitle(product.get("name").getAsString());
		String descUrl = product.get("desc").getAsString();
		String descHtml = getHtml(descUrl);
		setItemDesc(descHtml);
		findImgInDesc(descHtml);
		itemJson = product.toString();
	}

	private String changeToBigPic(String picUrl) {
		String pic = picUrl.replaceAll("n\\w", "n1");
		return pic;

	}

	private void setPrice(String skUid) {
		String url = "http://p.3.cn/prices/get?type=1&area=1_72_2799&pdtk=&pduid=410744404&pdpin=&pdbp=0&skuid=J_"
				+ skUid + "&callback=cnp";
		String priceHtml = getHtml(url);
		String regex = "\"p\":.*?\"(.*?)\"";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(priceHtml);
		if (matcher.find()) {
			String findResult = matcher.group();
			if (findResult != null) {
				price = findResult.substring(5, findResult.length() - 1);
			}
		}
	}

	@Override
	protected void findImgInDesc(String html) {
		if (!html.equals("")) {
			String regex = "img\\w\\w\\.360buyimg\\.com/.*?\\.(png|jpg|jpeg)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			while (matcher.find()) {
				descPics.add(matcher.group());
			}
		}
	}

	protected void findPropertyType(Document doc) {
		Elements elements = doc.select("div.item>a[title]").select("a[clstag]");
		if (elements.size() == 0) {
			return;
		}
		for (Element element : elements) {
			String title = element.attr("title");
			String clstag = handleClstag(element.attr("clstag"));
			// System.out.println(element.toString());
			if (element.children().size() != 0 && element.child(0).hasAttr("data-img")) {
				title += changeToBigPic("#http:" + element.child(0).attr("src"));
			}
			if (propertyType.containsKey(clstag)) {
				ArrayList<String> list = propertyType.get(clstag);
				list.add(title);
				propertyType.put(clstag, list);
			} else {
				ArrayList<String> list = new ArrayList<>();
				list.add(title);
				propertyType.put(clstag, list);
			}
		}

	}

	private String handleClstag(String temp) {
		int left = temp.lastIndexOf("|") + 1;
		int right = temp.indexOf("-");
		String str = temp.substring(left, right);
		if (str.equals("yanse")) {
			str = "颜色";
		}
		if (str.equals("banben")) {
			str = "版本";
		}
		return str;
	}

}
