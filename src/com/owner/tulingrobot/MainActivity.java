package com.owner.tulingrobot;

import com.owner.net.ResultCallBack;
import com.owner.net.VisitTuLing;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
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
		
	private void showTextOnContent(String sendtextString) {
		TextView textView=new TextView(context);
		textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		textView.setGravity(Gravity.LEFT);
		textView.setPadding(5, 5, 5, 5);
		textView.setBackgroundColor(R.color.own_message);
		textView.setText(sendtextString);
		content.addView(textView);
	}

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
		TextView textView=new TextView(context);
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity=Gravity.RIGHT;
		textView.setLayoutParams(lp);
		//textView.setGravity(Gravity.RIGHT);
		textView.setBackgroundColor(Color.BLUE);
		textView.setPadding(5, 5, 5, 5);
		textView.setText(object);
		content.addView(textView);
	}

}
