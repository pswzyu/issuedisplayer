package me.cnzy.railissues.Utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by zhang on 7/29/2015.
 */
public class HTTPUtility {

	public static final String TAG = "CM.HTTPUtil";
	
	public class HTTPResult {
		public int response_code = -1;
		public boolean succeeded = false;
		public HashMap<String, String> error = new HashMap<String, String>();
		public boolean was_download = false;
		public String response_text = null;
		public File response_file = null;
		public HashMap<String, HttpCookie> cookies = 
				new HashMap<String, HttpCookie>();
		public Map<String, List<String>> head_fields = null;

		/**
		 * function used for debugging, print all the info to the Log.d
		 */
		public void dump() {
			String err_str = "";
			for(Entry<String, String> entry : error.entrySet()) {
				if (err_str.length() != 0) err_str += ",";
				err_str += entry.getKey()+":"+entry.getValue();
			}

			String ck_str = "";
			for(Entry<String, HttpCookie> entry : cookies.entrySet()) {
				if (ck_str.length() != 0) ck_str += ",";
				ck_str += entry.getKey()+":"+entry.getValue().toString();
			}

			Log.d(TAG, "Http Response, RespCode:"+response_code+",Succ:"+
				succeeded+",Err:{"+err_str+"},Down:"+was_download+",RespText:"+
				response_text+",RespFile:"+
				(response_file==null?"null":response_file.getAbsolutePath()));
		}
	}

	private static final String LINE_FEED = "\r\n";
	private static final int BUFFER_SIZE = 1024;
	private HttpURLConnection httpConn = null;

	private OutputStream httpOutputStream = null;
	private PrintWriter httpWriter = null;
	private InputStream httpInputStream = null;
	private BufferedReader httpReader = null;

	private FileInputStream fileInputStream = null;
	private FileOutputStream fileOutputStream = null;

	private String boundary = null;
	private String charset = "UTF-8";

	private String cookies = "";
	private HashMap<String, String> string_fields = new HashMap<String, String>();
	private HashMap<String, File> file_fields = new HashMap<String, File>();

	

	public HTTPUtility() {

	}

	public void cleanUp() throws IOException {
		
		if (httpConn != null) {
			httpConn.disconnect();
		}
		
		if (httpWriter != null) {
			httpWriter.close();
		}
		if (httpOutputStream != null) {
			httpOutputStream.close();
		}

		if (httpReader != null) {
			httpReader.close();
		}

		if (httpInputStream != null) {
			httpInputStream.close();
		}

		if (httpConn != null) {
			httpConn.disconnect();
		}

		if (fileInputStream != null) {
			fileInputStream.close();
		}
		if (fileOutputStream != null) {
			fileOutputStream.close();
		}
	}
	

	/**
	 * add a string value to the request
	 * 
	 * @param key
	 * @param value
	 */
	public void addStringField(String key, String value) {
		string_fields.put(key, value);
	}

	/**
	 * add a file field to the request
	 * 
	 * @param key
	 * @param file
	 */
	public void addFileField(String key, File file) {
		file_fields.put(key, file);
	}

	public void addCookie(String key, String value) {
		if (cookies.length() > 0) {
			cookies += "; ";
		}
		// TODO: what if cookie contains special chars??
		cookies += key + "=" + value;
	}

	/**
	 * Use this function to initiate a post request it will save the returned
	 * file to the dir supplied to this function
	 *
	 * @param requestURL
	 *            the url where the request is going to be sent to
	 * @param dir
	 *            the dir that you want to save the returned file, can be null
	 *            if you are sure that the response type is not going to be
	 *            downloaded
	 * @return the JSON reply from server, filename of the downloaded file will
	 *         be in the "file_path" index. null/server returned failure message
	 *         will be returned if failed
	 * @throws IOException
	 */
	public HTTPResult doPost(String requestURL, File dir) throws IOException {

		URL url = new URL(requestURL);
		httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setConnectTimeout(20000);
		httpConn.setRequestMethod("POST");

		httpConn.setUseCaches(false);
		httpConn.setDoOutput(true); // indicates POST method
		httpConn.setDoInput(true);

		httpConn.setRequestProperty("User-Agent", "ClothesMatcherAndroid");
		httpConn.setRequestProperty("Cookie", cookies);
		// if there is no file fields, then use urlencode
		if (file_fields.isEmpty()) {
			httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		} else { // otherwise, use multipart encoding
			// creates a unique boundary based on time stamp
			boundary = getBoundary();
			httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		}

		httpOutputStream = httpConn.getOutputStream();
		httpWriter = new PrintWriter(new OutputStreamWriter(httpOutputStream, charset), true);

		// if there is no file fields, then use urlencode
		if (file_fields.isEmpty()) {
			httpWriter.append(getPostDataString(string_fields));
			httpWriter.flush();
		} else { // otherwise, use multipart encoding
			// add the string and files in the hashmap into the request
			for (Entry<String, String> entry : string_fields.entrySet()) {
				addFormField(entry.getKey(), entry.getValue());
			}
			for (Entry<String, File> entry : file_fields.entrySet()) {
				addFilePart(entry.getKey(), entry.getValue());
			}

			httpWriter.append(LINE_FEED).flush();
			httpWriter.append("--" + boundary + "--").append(LINE_FEED);
		}

		httpWriter.close();

		// receive from the server !
		HTTPResult response = new HTTPResult();

		// checks server's status code first
		response.response_code = httpConn.getResponseCode();
		if (response.response_code == HttpURLConnection.HTTP_OK) {

			String response_type = httpConn.getContentType();
			
			Map<String, List<String>> headerFields = httpConn.getHeaderFields();
			response.head_fields = headerFields;
			parseAndAddCookies(headerFields.get("Set-Cookie"), response);
			parseAndAddCookies(headerFields.get("Set-Cookie2"), response);

			httpInputStream = httpConn.getInputStream();
			
			response.was_download = shouldDownload(response_type);

			if (response.was_download) {

				String fileName = null;
				String disposition = httpConn.getHeaderField("Content-Disposition");
				String contentType = httpConn.getContentType();
				int contentLength = httpConn.getContentLength();

				if (disposition != null) {
					// extracts file name from header field
					int index = disposition.indexOf("filename=");
					if (index > 0) {
						fileName = disposition.substring(index + 10, disposition.length() - 1);
					}
				} else {
					// extracts file name from URL
					fileName = requestURL.substring(requestURL.lastIndexOf("/") + 1, requestURL.length());
				}

				if (fileName == null ) {
					response.error.put("filename", "No filename in the HTTP response!");
				} else if (dir == null) {
					response.error.put("filename", "No dir given, but the response is downloadable!");
				} else {
					response.response_file = new File(dir, fileName);
					fileOutputStream = new FileOutputStream(response.response_file);
					int bytesRead = -1;
					byte[] buffer = new byte[BUFFER_SIZE];
					while ((bytesRead = httpInputStream.read(buffer)) != -1) {
						fileOutputStream.write(buffer, 0, bytesRead);
					}
					fileOutputStream.close();
					
					response.succeeded = true;
				}
				httpInputStream.close();
			} else {
				httpReader = new BufferedReader(new InputStreamReader(httpInputStream));
				String line = null;
				String all_lines = "";
				while ((line = httpReader.readLine()) != null) {
					all_lines += line;
				}
				response.response_text = all_lines;
				httpReader.close();
				
				response.succeeded = true;
			}
		} else {
			response.error.put("http", "Server returned non-OK status: " + response.response_code);
		}

		httpConn.disconnect();

		response.dump();

		return response;

	}

	/**
	 * Adds a form field to the request, used only for the multipart encoding
	 * 
	 * @param name
	 *            field name
	 * @param value
	 *            field value
	 */
	private void addFormField(String name, String value) {
		httpWriter.append("--" + boundary).append(LINE_FEED);
		httpWriter.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
		httpWriter.append("Content-Type: text/plain; charset=" + charset).append(LINE_FEED);
		httpWriter.append(LINE_FEED);
		httpWriter.append(value).append(LINE_FEED);
		httpWriter.flush();
	}

	/**
	 * Adds a upload file section to the request, used only for the multipart
	 * encoding
	 * 
	 * @param fieldName
	 *            name attribute in <input type="file" name="..." />
	 * @param uploadFile
	 *            a File to be uploaded
	 * @throws IOException
	 */
	private void addFilePart(String fieldName, File uploadFile) throws IOException {
		String fileName = uploadFile.getName();
		httpWriter.append("--" + boundary).append(LINE_FEED);
		httpWriter.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"")
				.append(LINE_FEED);
		httpWriter.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
		httpWriter.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
		httpWriter.append(LINE_FEED);
		httpWriter.flush();

		fileInputStream = new FileInputStream(uploadFile);
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = fileInputStream.read(buffer)) != -1) {
			httpOutputStream.write(buffer, 0, bytesRead);
		}
		httpOutputStream.flush();
		fileInputStream.close();

		httpWriter.append(LINE_FEED);
		httpWriter.flush();
	}

	/**
	 * Adds a header field to the request.
	 * 
	 * @param name
	 *            - name of the header field
	 * @param value
	 *            - value of the header field
	 */
	private void addHeaderField(String name, String value) {
		httpWriter.append(name + ": " + value).append(LINE_FEED);
		httpWriter.flush();
	}

	/**
	 * get the urlencoded post data string
	 * 
	 * @param params
	 * @return
	 * @throws IOException
	 */
	private String getPostDataString(HashMap<String, String> params) throws IOException {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for (Entry<String, String> entry : params.entrySet()) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		}

		return result.toString();
	}
	private void parseAndAddCookies(List<String> cookiesHeader, HTTPResult response) {
		if (cookiesHeader == null || response == null) {
			return;
		}
		for (String set_line : cookiesHeader) {
			List<HttpCookie> cks = HttpCookie.parse(set_line);
			for (HttpCookie ck : cks) {
				response.cookies.put(ck.getName(), ck);
			}
		}
	}

	private boolean shouldDownload(String ct) {
		if (ct == null || ct.contains("text")) {
			return false;
		}
		return true;
	}
	
	private String getBoundary() {
		return "===" + System.currentTimeMillis() + "===";
	}

}
