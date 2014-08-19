package com.owner.tulingrobot;

import java.lang.reflect.Type;
import java.util.List;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
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
				Toast.makeText(context, "发送的信息不能为空！", Toast.LENGTH_SHORT).show();
				break;

			default:
				break;
			}
		}};
		
		//显示自己发送的信息
	private void showTextOnContent(String sendtextString) {
		TextView textView=new TextView(context);
		textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		textView.setGravity(Gravity.LEFT);
		textView.setPadding(5, 5, 5, 5);
		//textView.setBackgroundColor(R.color.own_message);
		textView.setText(sendtextString);
		content.addView(textView);
	}

	//把自己的信息发送出去
	protected void sendQuestionForResult(String sendtextString) {
		visitor.sendToTarget(sendtextString);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return false;
	}

	//服务器返回数据函数
	@Override
	public void callback(String object) {
		final Data data=JsonConvert.convertStringToData(object);
		if(data!=null)
		{
			long code=data.getCode();
			String listString=Constants.gson.toJson(data.getList());
			listString=listString==null?"":listString;
			
			if(code==100000)//文本类数据
			{
				singleTextView(data.getText());
			}else if(code==200000)//网址类数据
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
			}else if(code==301000)//小说
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
				
			}else if(code==302000)//新闻
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
				
			}else if(code==304000)//应用、软件、下载
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
			}else if(code==305000)//列车
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
			}else if(code==306000)//航班
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
			}else if(code==307000)//团购
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
			}else if(code==308000)//优惠
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
			}else if(code==310000)//酒店
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
			}else if(code==311000)//彩票
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
			}else if(code==312000)//价格
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
			}else if(code==301000)//餐厅
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

	//生成单一TextView
	private void singleTextView(String object) {
		TextView textView=new TextView(context);
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity=Gravity.RIGHT;
		textView.setLayoutParams(lp);
		//textView.setGravity(Gravity.RIGHT);
		//textView.setBackgroundColor(Color.BLUE);
		textView.setPadding(5, 5, 5, 5);
		textView.setText(object);
		content.addView(textView);
	}

}
