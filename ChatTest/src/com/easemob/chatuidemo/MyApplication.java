package com.easemob.chatuidemo;

import android.app.Application;

import com.easemob.chat.EMChat;

public class MyApplication extends Application {
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		EMChat.getInstance().init(this);
		
	}

}
