package org.openml.rapidminer.utils;

import org.openml.apiconnector.io.HttpConnector;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/*
 * Extension of the Openml HttpConnector for JSON requests
 */
public class HttpConnectorJson extends HttpConnector {
	private static final long serialVersionUID = 4883170451697695160L;

	public static Object doApiRequest(String url, HttpEntity entity, String ash, int apiVerboseLevel) throws Exception {
		if (ash == null) {
			throw new Exception("Api key not set. ");
		}
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(entity);
		CloseableHttpResponse response = httpclient.execute(httppost);
		String result = readHttpResponse(response, new URL(url), "POST", apiVerboseLevel);
		return result;
	}

	public static Object doApiRequest(String url, String ash, int apiVerboseLevel) throws Exception {
		if (ash == null) {
			throw new Exception("Api key not set. ");
		}
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(url + "?api_key=" + ash);
		CloseableHttpResponse response = httpclient.execute(httpget);
		String result = readHttpResponse(response, new URL(url), "GET", apiVerboseLevel);
		return result;
	}
}
