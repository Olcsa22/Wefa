package hu.lanoga.toolbox.util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;

@SuppressWarnings("deprecation")
public class HttpFileTransferUtil {
	
	// TODO: unit test hozzá, valamilyen publikus teszt oldalról szedjen le, simpleDownload()-hoz legalább... lásd https://httpbin.org/#/Images például
	// TODO: eredeti fájlnevet (reponse header nem dolgozza fel)

	private HttpFileTransferUtil() {
		//
	}

	private static FileDescriptor downloadCommon(final String fileUrl, final DefaultHttpClient client) throws Exception {

		final HttpGet httpget = new HttpGet(fileUrl);

		CloseableHttpResponse getResponse = null;

		try {

			final HttpContext context = new BasicHttpContext();
			getResponse = client.execute(httpget, context);

			if (getResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new IOException(getResponse.getStatusLine().toString());
			}

			final FileDescriptor fileDescriptor;

			{
				
				// TODO: itt a response header-ből kellene kitúrni

				// final HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
				// final String currentUrl = URLDecoder.decode(currentReq.getURI().toString(), "UTF-8");
				//
				// final int i = currentUrl.lastIndexOf('/');
				// String fileName = null;
				// if (i < 0) {
				// fileName = currentUrl;
				// } else {
				// fileName = currentUrl.substring(i + 1);
				// }

				String fileName = UUID.randomUUID().toString() + ".dat";

				fileDescriptor = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2(fileName);

			}

			try (InputStream is = getResponse.getEntity().getContent(); OutputStream os = new BufferedOutputStream(new FileOutputStream(fileDescriptor.getFile()), 128 * 1024)) {
				IOUtils.copyLarge(is, os);
			}

			return fileDescriptor;

		} finally {
			if (getResponse != null) {
				getResponse.close();
			}
		}
	}

	/**
	 * mindig friss {@link DefaultHttpClient} példánnyal dolgozik 
	 * (ez tiszta megoldás, de sok fájl esetén azért nagy lehet az overhead) 
	 * 
	 * @param fileUrl
	 * 
	 * @return
	 * 		temp status
	 */
	public static FileDescriptor simpleDownload(final String fileUrl) {
		return simpleDownload(fileUrl, new DefaultHttpClient(), true);
	}

	/**
	 * @param fileUrl
	 * @param client
	 * @param closeClientAfter
	 * 
	 * @return
	 * 		temp status
	 */
	public static FileDescriptor simpleDownload(final String fileUrl, final DefaultHttpClient client, final boolean closeClientAfter) {

		ToolboxAssert.isTrue(StringUtils.isNoneBlank(fileUrl));
		ToolboxAssert.notNull(client);

		try {

			// client.setCookieStore(new BasicCookieStore()); // itt nem kellenek cookie-k

			return downloadCommon(fileUrl, client);

		} catch (final Exception e) {
			throw new ToolboxGeneralException("file download failed", e);
		} finally {
			if (closeClientAfter) {
				client.close();
			}
		}

	}

	/**
	 * mindig friss {@link DefaultHttpClient} példánnyal dolgozik 
	 * (ez tiszta megoldás, de sok fájl esetén azért nagy lehet az overhead) 
	 * 
	 * @param loginUrl
	 * @param username
	 * @param password
	 * @param fileUrl
	 * 
	 * @return
	 * 		temp status
	 */
	public static FileDescriptor downloadAfterSessionLogin(final String loginUrl, final String username, final String password, final String fileUrl) {

		try (DefaultHttpClient client = new DefaultHttpClient()) {
			client.setCookieStore(new BasicCookieStore());
			return downloadAfterSessionLogin(loginUrl, username, password, fileUrl, client, false);
		}

	}

	/**
	 * @param loginUrl
	 * @param username
	 * @param password
	 * @param fileUrl
	 * @param client
	 * @param closeClientAfter
	 * 
	 * @return
	 * 		temp status
	 */
	public static FileDescriptor downloadAfterSessionLogin(final String loginUrl, final String username, final String password, final String fileUrl, final DefaultHttpClient client, final boolean closeClientAfter) {

		ToolboxAssert.isTrue(StringUtils.isNoneBlank(loginUrl, username, password, fileUrl));
		ToolboxAssert.notNull(client);

		try {

			final HttpPost post = new HttpPost(loginUrl);

			final List<NameValuePair> parameters = new ArrayList<>();
			parameters.add(new BasicNameValuePair("username", username));
			parameters.add(new BasicNameValuePair("password", password));
			final UrlEncodedFormEntity sendentity = new UrlEncodedFormEntity(parameters, HTTP.UTF_8);
			post.setEntity(sendentity);

			CloseableHttpResponse postResponse = null;
			try {
				postResponse = client.execute(post);
				EntityUtils.consume(postResponse.getEntity()); // TODO: ennek nem tudom mi a szerepe, kell?
			} finally {
				if (postResponse != null) {
					postResponse.close();
				}
			}

			return downloadCommon(fileUrl, client);

		} catch (final Exception e) {
			throw new ToolboxGeneralException("file download failed", e);
		} finally {
			if (closeClientAfter) {
				client.close();
			}
		}

	}

}
