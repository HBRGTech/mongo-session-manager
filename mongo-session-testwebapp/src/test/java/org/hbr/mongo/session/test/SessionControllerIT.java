/**
 * 
 */
package org.hbr.mongo.session.test;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;

/**
 * Simple Test that requests the output of the Session Controller.
 * 
 * @author Kevin Davis
 *
 */
public class SessionControllerIT {

	/** Multithreaded Http Client */
	protected static final CloseableHttpClient httpClient;
	
	static {
		/* default connection config */
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setCharset(Consts.UTF_8)
				.build();
		
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(1);
		connectionManager.setDefaultConnectionConfig(connectionConfig);
	
		/* create the client */
		httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.153 Safari/537.36")
				.build();		
	}
	
	/**
	 * Make a test call to the servlet
	 * @throws Exception
	 */
	@Test
	public void testSessionController() throws Exception {
		HttpGet method = new HttpGet("http://localhost:9090/mongo-session-testwebapp/get-session");
		CloseableHttpResponse response = httpClient.execute(method);
		InputStream content = null;
		if (response != null) {
			content = response.getEntity().getContent();
			byte[] data = IOUtils.toByteArray(content);
			String output = new String(data);
			System.out.println(output);
		}
		response.close();
	}
	
}
