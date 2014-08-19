package com.owner.tulingrobot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import libcore.io.DiskLruCache;
import libcore.io.DiskLruCache.Snapshot;
import com.owner.constant.Constants;
import com.owner.entity.App;
import com.owner.entity.Data;
import com.owner.entity.Flight;
import com.owner.entity.Groupon;
import com.owner.entity.Hotel;
import com.owner.entity.Lottery;
import com.owner.entity.News;
import com.owner.entity.Novel;
import com.owner.entity.Price;
import com.owner.entity.Privilege;
import com.owner.entity.Restaurant;
import com.owner.entity.Train;
import com.owner.net.JsonConvert;
import com.owner.net.ResultCallBack;
import com.owner.net.VisitTuLing;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements ResultCallBack{

	private LinearLayout content;
	private EditText sendText;
	private Button sendButton;
	private Context context;
	private VisitTuLing visitor;
	private final int SEND_TEXT_NOT_ALLOW_NULL=0x001;
	
	/**
	 * ��¼�����������ػ�ȴ����ص�����
	 */
	private  Set<BitmapWorkerTask> taskCollection;

	/**
	 * ͼƬ���漼���ĺ����࣬���ڻ����������غõ�ͼƬ���ڳ����ڴ�ﵽ�趨ֵʱ�Ὣ�������ʹ�õ�ͼƬ�Ƴ�����
	 */
	public  LruCache<String, Bitmap> mMemoryCache;

	/**
	 * ͼƬӲ�̻�������ࡣ
	 */
	public  DiskLruCache mDiskLruCache;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		context=MainActivity.this;
		content=(LinearLayout) findViewById(R.id.content);
		sendText=(EditText) findViewById(R.id.sendText);
		sendButton=(Button) findViewById(R.id.sendButton);
		
		sendButton.setOnClickListener(l);
		
		visitor=new VisitTuLing(this);
		
		taskCollection = new HashSet<BitmapWorkerTask>();
		// ��ȡӦ�ó����������ڴ�
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemory / 8;
		// ����ͼƬ�����СΪ�����������ڴ��1/8
		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount();
			}
		};
		try {
			// ��ȡͼƬ����·��
			File cacheDir = getDiskCacheDir(context, "thumb2");
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}
			// ����DiskLruCacheʵ������ʼ����������
			mDiskLruCache = DiskLruCache
					.open(cacheDir, getAppVersion(context), 1, 10 * 1024 * 1024);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private OnClickListener l=new OnClickListener() {
				
		@Override
		public void onClick(View v) {
			String sendtextString=sendText.getText().toString();
			sendText.setText("");
			if(sendtextString!=null&&sendtextString.trim().length()>0)
			{
				showTextOnContent(sendtextString);
				sendQuestionForResult(sendtextString);
			}else
			{
				handler.sendEmptyMessage(SEND_TEXT_NOT_ALLOW_NULL);
			}
		}				
	};
	
	private Handler handler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case SEND_TEXT_NOT_ALLOW_NULL:
				Toast.makeText(context, "���͵���Ϣ����Ϊ�գ�", Toast.LENGTH_SHORT).show();
				break;

			default:
				break;
			}
		}};
		
		//��ʾ�Լ����͵���Ϣ
	private void showTextOnContent(String sendtextString) {
		TextView textView=new TextView(context);
		textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		textView.setGravity(Gravity.LEFT);
		textView.setPadding(5, 5, 5, 5);
		//textView.setBackgroundColor(R.color.own_message);
		textView.setText(sendtextString);
		content.addView(textView);
	}

	//���Լ�����Ϣ���ͳ�ȥ
	protected void sendQuestionForResult(String sendtextString) {
		visitor.sendToTarget(sendtextString);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return false;
	}

	//�������������ݺ���
	@Override
	public void callback(String object) {
		final Data data=JsonConvert.convertStringToData(object);
		if(data!=null)
		{
			long code=data.getCode();
			String listString=Constants.gson.toJson(data.getList());
			listString=listString==null?"":listString;
			
			if(code==100000)//�ı�������
			{
				singleTextView(data);
			}else if(code==200000)//��ַ������
			{
				TextView textView=new TextView(context);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				textView.setLayoutParams(lp);
				//textView.setGravity(Gravity.RIGHT);
				//textView.setBackgroundColor(Color.BLUE);
				textView.setPadding(5, 5, 5, 5);
				textView.setText(data.getText());
				content.addView(textView);
				textView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View arg0) {
						Intent i=new Intent();
						i.setClass(context, WebViewActivity.class);
						i.putExtra("url", data.getUrl());
						startActivity(i);
					}
				});
			}else if(code==301000)//С˵
			{		
				String text=data.getText();
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Novel> novels=Constants.gson.fromJson(listString,Constants.novel_list_type);
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(novels!=null&&novels.size()>0)
				{
					for(final Novel novel:novels)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(novel.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(novel.getName());
						textView.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View arg0) {
								Intent i=new Intent();
								i.setClass(context, WebViewActivity.class);
								i.putExtra("url", novel.getDetailurl());
								startActivity(i);
							}
						});
						TextView textView1=new TextView(context);
						textView1.setText(novel.getAuthor());
						linear.addView(imageView);
						linear.addView(textView);
						linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
				
			}else if(code==302000)//����
			{	
				String text=data.getText();				
				
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<News> newss=Constants.gson.fromJson(listString,Constants.news_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(newss!=null&&newss.size()>0)
				{
					for(final News news:newss)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(news.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(news.getArticle());
						
                        textView.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View arg0) {
								Intent i=new Intent();
								i.setClass(context, WebViewActivity.class);
								i.putExtra("url", news.getDetailurl());
								startActivity(i);
							}
						});
						
						/*TextView textView1=new TextView(context);
						textView1.setText(news.getDetailurl());*/
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
				
			}else if(code==304000)//Ӧ�á����������
			{	
				String text=data.getText();
														
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<App> apps=Constants.gson.fromJson(listString,Constants.app_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(apps!=null&&apps.size()>0)
				{
					for(final App app:apps)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(app.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(app.getName());
						
                        textView.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View arg0) {
								Intent i=new Intent();
								i.setClass(context, WebViewActivity.class);
								i.putExtra("url", app.getDetailurl());
								startActivity(i);
							}
						});
						
						/*TextView textView1=new TextView(context);
						textView1.setText(app.getDetailurl());*/
						
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}else if(code==305000)//�г�
			{	
				String text=data.getText();
			    			    						
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Train> trains=Constants.gson.fromJson(listString,Constants.train_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(trains!=null&&trains.size()>0)
				{
					for(final Train train:trains)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(train.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(train.getStart());
						
						 textView.setOnClickListener(new OnClickListener() {
								
								@Override
								public void onClick(View arg0) {
									Intent i=new Intent();
									i.setClass(context, WebViewActivity.class);
									i.putExtra("url", train.getDetailurl());
									startActivity(i);
								}
							});
						
						/*TextView textView1=new TextView(context);
						textView1.setText(train.getDetailurl());*/
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}else if(code==306000)//����
			{	
				String text=data.getText();
											
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Flight> flights=Constants.gson.fromJson(listString,Constants.flight_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(flights!=null&&flights.size()>0)
				{
					for(final Flight flignt:flights)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(flignt.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(flignt.getRoute());
						
						textView.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View arg0) {
								Intent i=new Intent();
								i.setClass(context, WebViewActivity.class);
								i.putExtra("url", flignt.getDetailurl());
								startActivity(i);
							}
						});
						
						/*TextView textView1=new TextView(context);
						textView1.setText(flignt.getDetailurl());*/
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}else if(code==307000)//�Ź�
			{	
				String text=data.getText();
				
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Groupon> groupons=Constants.gson.fromJson(listString,Constants.groupon_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(groupons!=null&&groupons.size()>0)
				{
					for(final Groupon groupon:groupons)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(groupon.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(groupon.getName());
                        textView.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View arg0) {
								Intent i=new Intent();
								i.setClass(context, WebViewActivity.class);
								i.putExtra("url", groupon.getDetailurl());
								startActivity(i);
							}
						});
						/*TextView textView1=new TextView(context);
						textView1.setText(groupon.getDetailurl());*/
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}else if(code==308000)//�Ż�
			{	
				String text=data.getText();
				
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Privilege> privileges=Constants.gson.fromJson(listString,Constants.privilege_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(privileges!=null&&privileges.size()>0)
				{
					for(final Privilege privilege:privileges)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(privilege.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(privilege.getName());
						 textView.setOnClickListener(new OnClickListener() {
								
								@Override
								public void onClick(View arg0) {
									Intent i=new Intent();
									i.setClass(context, WebViewActivity.class);
									i.putExtra("url", privilege.getDetailurl());
									startActivity(i);
								}
							});
						/*TextView textView1=new TextView(context);
						textView1.setText(privilege.getDetailurl());*/
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}else if(code==310000)//�Ƶ�
			{	
				String text=data.getText();
				
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Hotel> hotels=Constants.gson.fromJson(listString,Constants.hotel_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(hotels!=null&&hotels.size()>0)
				{
					for(final Hotel hotel:hotels)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(hotel.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(hotel.getName());
						textView.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View arg0) {
								Intent i=new Intent();
								i.setClass(context, WebViewActivity.class);
								i.putExtra("url", hotel.getDetailurl());
								startActivity(i);
							}
						});
						/*TextView textView1=new TextView(context);
						textView1.setText(hotel.getDetailurl());*/
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}else if(code==311000)//��Ʊ
			{		
				String text=data.getText();
				
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Lottery> lotterys=Constants.gson.fromJson(listString,Constants.lottery_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(lotterys!=null&&lotterys.size()>0)
				{
					for(final Lottery lottery:lotterys)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(lottery.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(lottery.getInfo());
                        
						TextView textView1=new TextView(context);
						textView1.setText(lottery.getDetailurl());
						
                        textView1.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View arg0) {
								Intent i=new Intent();
								i.setClass(context, WebViewActivity.class);
								i.putExtra("url", lottery.getDetailurl());
								startActivity(i);
							}
						});
						
						linear.addView(imageView);
						linear.addView(textView);
						linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}else if(code==312000)//�۸�
			{		
				String text=data.getText();
				
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Price> prices=Constants.gson.fromJson(listString,Constants.price_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(prices!=null&&prices.size()>0)
				{
					for(final Price price:prices)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(price.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(price.getName());
						 textView.setOnClickListener(new OnClickListener() {
								
								@Override
								public void onClick(View arg0) {
									Intent i=new Intent();
									i.setClass(context, WebViewActivity.class);
									i.putExtra("url", price.getDetailurl());
									startActivity(i);
								}
							});
						/*TextView textView1=new TextView(context);
						textView1.setText(price.getDetailurl());*/
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}else if(code==301000)//����
			{		
				String text=data.getText();
				
				TextView tishi=new TextView(context);
				tishi.setText(text);
				
				List<Restaurant> restaurants=Constants.gson.fromJson(listString,Constants.restaurant_list_type);
				
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lp);
				
				linearLayout.addView(tishi);
				
				if(restaurants!=null&&restaurants.size()>0)
				{
					for(final Restaurant restaurant:restaurants)
					{
						LinearLayout linear=new LinearLayout(context);
						linear.setOrientation(LinearLayout.HORIZONTAL);
						
						ImageView imageView=new ImageView(context);
						imageView.setImageURI(Uri.parse(restaurant.getIcon()));
						TextView textView=new TextView(context);
						textView.setText(restaurant.getName());
						textView.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View arg0) {
								Intent i=new Intent();
								i.setClass(context, WebViewActivity.class);
								i.putExtra("url", restaurant.getDetailurl());
								startActivity(i);
							}
						});
						/*TextView textView1=new TextView(context);
						textView1.setText(restaurant.getDetailurl());*/
						linear.addView(imageView);
						linear.addView(textView);
						//linear.addView(textView1);
						
						linearLayout.addView(linear);
					}
					content.addView(linearLayout);
				}
			}
		}						
	}

	//���ɵ�һTextView
	private void singleTextView(final Data object) {
		TextView textView=new TextView(context);
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity=Gravity.RIGHT;
		textView.setLayoutParams(lp);
		//textView.setGravity(Gravity.RIGHT);
		//textView.setBackgroundColor(Color.BLUE);
		textView.setPadding(5, 5, 5, 5);
		final String[] str=object.getText().split("--");
		if(str.length>1)
		{		
			if(str[1]!=null)
			{
				textView.setText(str[0]+"-->����鿴");
				LinearLayout linearLayout=new LinearLayout(context);
				linearLayout.setOrientation(LinearLayout.VERTICAL);
				linearLayout.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams lpp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lpp.gravity=Gravity.RIGHT;
				linearLayout.setLayoutParams(lpp);
				linearLayout.addView(textView);
				ImageView iv=new ImageView(context);
				loadBitmaps(iv, str[1]);			
				//iv.setImageURI(Uri.parse(str[1]));
				linearLayout.addView(iv);
				content.addView(linearLayout);
				
				
				
				textView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View arg0) {
						Intent i=new Intent();
						i.setClass(context, WebViewActivity.class);
						i.putExtra("url", str[1]);
						startActivity(i);
					}
				});
			}
		}else
		{
			textView.setText(object.getText());
			content.addView(textView);
		}		
	}
	
	/**
	 * ʹ��MD5�㷨�Դ����key���м��ܲ����ء�
	 */
	public String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}
	
	/**
	 * �������¼ͬ����journal�ļ��С�
	 */
	public void fluchCache() {
		if (mDiskLruCache != null) {
			try {
				mDiskLruCache.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
	
	/**
	 * ��һ��ͼƬ�洢��LruCache�С�
	 * 
	 * @param key
	 *            LruCache�ļ������ﴫ��ͼƬ��URL��ַ��
	 * @param bitmap
	 *            LruCache�ļ������ﴫ������������ص�Bitmap����
	 */
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemoryCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	/**
	 * ��LruCache�л�ȡһ��ͼƬ����������ھͷ���null��
	 * 
	 * @param key
	 *            LruCache�ļ������ﴫ��ͼƬ��URL��ַ��
	 * @return ��Ӧ�������Bitmap���󣬻���null��
	 */
	public  Bitmap getBitmapFromMemoryCache(String key) {
		return mMemoryCache.get(key);
	}

	/**
	 * ����Bitmap���󡣴˷�������LruCache�м��������Ļ�пɼ���ImageView��Bitmap����
	 * ��������κ�һ��ImageView��Bitmap�����ڻ����У��ͻῪ���첽�߳�ȥ����ͼƬ��
	 */
	public  void loadBitmaps(ImageView imageView, String imageUrl) {
		try {
			Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
			if (bitmap == null) {
				BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				taskCollection.add(task);
				task.execute(imageUrl);
			} else {
				if (imageView != null && bitmap != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ȡ�������������ػ�ȴ����ص�����
	 */
	public void cancelAllTasks() {
		if (taskCollection != null) {
			for (BitmapWorkerTask task : taskCollection) {
				task.cancel(false);
			}
		}
	}

	/**
	 * ���ݴ����uniqueName��ȡӲ�̻����·����ַ��
	 */
	public File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + uniqueName);
	}

	/**
	 * ��ȡ��ǰӦ�ó���İ汾�š�
	 */
	public int getAppVersion(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
					0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}

	
	/**
	 * �첽����ͼƬ������
	 * 
	 * @author guolin
	 */
	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

		/**
		 * ͼƬ��URL��ַ
		 */
		private String imageUrl;
		private ImageView imageView;

		public BitmapWorkerTask(ImageView imageView) {
			this.imageView=imageView;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			imageUrl = params[0];
			FileDescriptor fileDescriptor = null;
			FileInputStream fileInputStream = null;
			Snapshot snapShot = null;
			try {
				// ����ͼƬURL��Ӧ��key
				final String key = hashKeyForDisk(imageUrl);
				// ����key��Ӧ�Ļ���
				snapShot = mDiskLruCache.get(key);
				if (snapShot == null) {
					// ���û���ҵ���Ӧ�Ļ��棬��׼�����������������ݣ���д�뻺��
					DiskLruCache.Editor editor = mDiskLruCache.edit(key);
					if (editor != null) {
						OutputStream outputStream = editor.newOutputStream(0);
						if (downloadUrlToStream(imageUrl, outputStream)) {
							editor.commit();
						} else {
							editor.abort();
						}
					}
					// ���汻д����ٴβ���key��Ӧ�Ļ���
					snapShot = mDiskLruCache.get(key);
				}
				if (snapShot != null) {
					fileInputStream = (FileInputStream) snapShot.getInputStream(0);
					fileDescriptor = fileInputStream.getFD();
				}
				// ���������ݽ�����Bitmap����
				Bitmap bitmap = null;
				if (fileDescriptor != null) {
					bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
				}
				if (bitmap != null) {
					// ��Bitmap������ӵ��ڴ滺�浱��
					addBitmapToMemoryCache(params[0], bitmap);
				}
				return bitmap;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (fileDescriptor == null && fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (IOException e) {
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			// ����Tag�ҵ���Ӧ��ImageView�ؼ��������غõ�ͼƬ��ʾ������
			//ImageView imageView = (ImageView) mPhotoWall.findViewWithTag(imageUrl);
			if (imageView != null && bitmap != null) {
				imageView.setImageBitmap(bitmap);
			}
			taskCollection.remove(this);
		}

		/**
		 * ����HTTP���󣬲���ȡBitmap����
		 * 
		 * @param imageUrl
		 *            ͼƬ��URL��ַ
		 * @return �������Bitmap����
		 */
		private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
			HttpURLConnection urlConnection = null;
			BufferedOutputStream out = null;
			BufferedInputStream in = null;
			try {
				final URL url = new URL(urlString);
				urlConnection = (HttpURLConnection) url.openConnection();
				in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
				out = new BufferedOutputStream(outputStream, 8 * 1024);
				int b;
				while ((b = in.read()) != -1) {
					out.write(b);
				}
				return true;
			} catch (final IOException e) {
				e.printStackTrace();
			} finally {
				if (urlConnection != null) {
					urlConnection.disconnect();
				}
				try {
					if (out != null) {
						out.close();
					}
					if (in != null) {
						in.close();
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			return false;
		}

	}

}
