package com.fetchContent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

public class CrawlerLib {
	static CloseableHttpClient client = getInstanceClient(false);
	// static MongoClient mongoClient = new MongoClient("localhost", 27017);
	// static MongoDatabase database = mongoClient.getDatabase("mydb");
	// static MongoCollection<org.bson.Document> collection =
	// database.getCollection("taobao");

	public static void main(String[] args) {

	}

	public static HttpUriRequest getResponse(String url, List<NameValuePair> list, Header[] headers, HttpHost proxy) {
		RequestConfig.Builder configBuilder = RequestConfig.custom().setConnectTimeout(6000).setSocketTimeout(6000)
				.setCookieSpec(CookieSpecs.STANDARD_STRICT).setConnectionRequestTimeout(60000);
		RequestConfig config = null;
		if (proxy == null) {
			config = configBuilder.build();
		} else {
			config = configBuilder.setProxy(proxy).build();
		}
		// use the method setCookieSpec to make the header which named
		// set-cookie effect
		HttpUriRequest request = null;
		if (list == null) {
			HttpGet get = new HttpGet(url);
			get.setConfig(config);
			request = get;
		} else {
			HttpPost post = new HttpPost(url);
			post.setConfig(config);
			HttpEntity entity = null;
			try {
				entity = new UrlEncodedFormEntity(list, "utf-8");
				post.setEntity(entity);// 此处有个低级错误，谨记！
				request = post;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		request.setHeader("Connection", "keep-alive");
		request.setHeaders(headers);
		System.out.println("ready to link " + url);
		return request;
	}

	private static String getAgent() {
		String opera = "Opera/9.80  (Windows NT 5.2; U; zh-cn) Presto/2.9.168 Version/11.51";
		String other = "(Windows NT 5.2; U; zh-cn) Presto/2.9.168 Version/11.51";
		// String ie9 = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1;
		// WOW64; Trident/5.0)";
		String aoyou = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322; Maxthon 2.0)";
		String qq = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322) QQBrowser/6.8.10793.201";
		String green = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322; GreenBrowser)";
		String se360 = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322; 360SE)";
		String ie9 = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0";
		String safari = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50";
		String fireFox = "Mozilla/5.0 (Windows NT 6.1; rv:2.0.1) Gecko/20100101 Firefox/4.0.1";
		String chrome = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.94 Safari/537.36";
		String[] agent = { ie9, safari, fireFox, chrome, opera, other, aoyou, qq, green, se360 };
		Random random = new Random();
		int i = random.nextInt(10);
		return agent[i];

	}

	/**
	 * 根据需要初始化httpclient
	 */
	public static CloseableHttpClient getInstanceClient(boolean isNeedProxy) {
		CloseableHttpClient httpClient;
		// SSLContext sslContext = SSLContexts.createSystemDefault();
		// SSLConnectionSocketFactory sslsf = new
		// SSLConnectionSocketFactory(sslContext,
		// NoopHostnameVerifier.INSTANCE);
		SSLContextBuilder sslBuilder = new SSLContextBuilder();
		try {
			sslBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		SSLConnectionSocketFactory sslsf = null;
		try {
			sslsf = new SSLConnectionSocketFactory(sslBuilder.build());
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
		manager.setMaxTotal(20);// 设置最大连接数
		manager.setDefaultMaxPerRoute(100); // 单路由最大连接数
		HttpRequestRetryHandler handler = new DefaultHttpRequestRetryHandler(2, true);
		HttpClientBuilder builder = HttpClients.custom().setRetryHandler(handler).setConnectionManager(manager)
				.setConnectionTimeToLive(6000, TimeUnit.MILLISECONDS).setSSLSocketFactory(sslsf);
		if (isNeedProxy) {
			HttpHost proxy = new HttpHost("127.0.0.1", 8087);// 设置代理ip
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			builder = builder.setRoutePlanner(routePlanner);
		}
		builder.setUserAgent(getAgent());
		httpClient = builder.build();
		return httpClient;
	}

	public static void printResult(String resource, boolean append) {
		File resultFile = new File("test.txt");
		if (!resultFile.exists()) {
			try {
				resultFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(resultFile, append);
			writer.write(resource + "\r\n");
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	String ensureNotEmpty(String str) {
		return str.equals("") ? "no value" : str;
	}
}
