package com.owner.net;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.owner.constant.Constants;

public class VisitTuLing {

	private ResultCallBack resultCallBack;
	private String resultString="";
	private HttpClient client;
	HttpPost httpPost;
	
	public ResultCallBack getResultCallBack() {
		return resultCallBack;
	}

	public void setResultCallBack(ResultCallBack resultCallBack) {
		this.resultCallBack = resultCallBack;
	}

	private VisitTuLing(){}
	
	public VisitTuLing(ResultCallBack resultCallBack)
	{
		this.resultCallBack=resultCallBack;
		client = Client.getInstance();
		httpPost = new HttpPost(Constants.SERVER_URL);
	}
	
	private Handler h=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 0x11:
				resultCallBack.callback(resultString);
				break;

			default:
				break;
			}
		}};

	public void sendToTarget(final String sendtextString) {
		new Thread(){

			@Override
			public void run() {
				try {           
		            List<NameValuePair> param = new ArrayList<NameValuePair>();
		            param.add(new BasicNameValuePair("key", Constants.API_KEY));
		            param.add(new BasicNameValuePair("info", sendtextString));
		            httpPost.setEntity(new UrlEncodedFormEntity(param, "utf-8"));
		            HttpResponse response = client.execute(httpPost);
		            int code = response.getStatusLine().getStatusCode();
		            if (code == 200) {
		                InputStream is = response.getEntity().getContent();
		                ByteArrayOutputStream baos = new ByteArrayOutputStream();
		                int len = 0;
		                byte[] buffer = new byte[1024];
		                while ((len = is.read(buffer)) != -1) {
		                    baos.write(buffer, 0, len);
		                }
		                is.close();
		                baos.close();
		                byte[] result = baos.toByteArray();
		                resultString = new String(result, "utf-8");
		                h.sendEmptyMessage(0x11);
		            } else {
		                resultString="wrong "+code;
		                h.sendEmptyMessage(0x11);
		            }
		        } catch (Exception e) {		        	
		        	resultString="Exception";
		        	h.sendEmptyMessage(0x11);
		        }finally
		        {
		            
		        }
				super.run();
			}}.start();	
	}

	/*public void send(String sendtextString) {
		sendToTarget(sendtextString);
	}*/

}
