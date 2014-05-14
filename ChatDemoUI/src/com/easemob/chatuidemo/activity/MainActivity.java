package com.easemob.chatuidemo.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContactListener;
import com.easemob.chat.EMContactManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMMessage;
import com.easemob.chat.EMNotifier;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoApplication;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.db.InviteMessgeDao;
import com.easemob.chatuidemo.db.UserDao;
import com.easemob.chatuidemo.domain.InviteMessage;
import com.easemob.chatuidemo.domain.InviteMessage.InviteMesageStatus;
import com.easemob.chatuidemo.domain.User;

public class MainActivity extends FragmentActivity {

	// 未读消息textview
	private TextView unreadLabel;
	private Button[] mTabs;
	private ContactlistFragment contactListFragment;
	private ChatHistoryFragment chatHistoryFragment;
	private SettingsFragment settingFragment;
	private Fragment[] fragments;
	private int index;
	private RelativeLayout[] tab_containers;
	//当前fragment的index
	private int currentTabIndex;
	private NewMessageBroadcastReceiver msgReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		inviteMessgeDao = new InviteMessgeDao(this);
		userDao = new UserDao(this);
		chatHistoryFragment = new ChatHistoryFragment();
		contactListFragment = new ContactlistFragment();
		settingFragment = new SettingsFragment();
		fragments = new Fragment[] {chatHistoryFragment, contactListFragment, settingFragment };
		// 添加显示第一个fragment
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, chatHistoryFragment).
			add(R.id.fragment_container, contactListFragment).hide(contactListFragment).show(chatHistoryFragment).commit();
		
		// 注册一个接收消息的BroadcastReceiver
		msgReceiver = new NewMessageBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter(EMChatManager.getInstance().getNewMessageBroadcastAction());
		intentFilter.setPriority(3);
		registerReceiver(msgReceiver, intentFilter);

		// 注册一个ack回执消息的BroadcastReceiver
		IntentFilter ackMessageIntentFilter = new IntentFilter(EMChatManager.getInstance().getAckMessageBroadcastAction());
		intentFilter.setPriority(3);
		registerReceiver(ackMessageReceiver, ackMessageIntentFilter);
		
		//注册一个好友请求拒绝等的BroadcastReceiver
		IntentFilter inviteIntentFilter = new IntentFilter(EMChatManager.getInstance().getContactInviteEventBroadcastAction());
		registerReceiver(contactInviteReceiver, inviteIntentFilter);
		
		//setContactListener监听联系人的变化等
		EMContactManager.getInstance().setContactListener(new RemoteContactListener());
	}

	/**
	 * 初始化组件
	 */
	private void initView() {
		unreadLabel = (TextView) findViewById(R.id.unread_msg_number);

		mTabs = new Button[3];
		mTabs[0] = (Button) findViewById(R.id.btn_conversation);
		mTabs[1] = (Button) findViewById(R.id.btn_address_list);
		mTabs[2] = (Button) findViewById(R.id.btn_setting);
		//把第一个tab设为选中状态
		mTabs[0].setSelected(true);
		
	}
	
	/**
	 * button点击事件
	 * @param view
	 */
	public void onTabClicked(View view) {
		switch (view.getId()) {
		case R.id.btn_conversation:
			index = 0;
			break;
		case R.id.btn_address_list:
			index = 1;
			break;
		case R.id.btn_setting:
			index = 2;
			break;
		}
		if (currentTabIndex != index) {
			FragmentTransaction trx = getSupportFragmentManager().beginTransaction();
			trx.hide(fragments[currentTabIndex]);
			if (!fragments[index].isAdded()) {
				trx.add(R.id.fragment_container, fragments[index]);
			}
			trx.show(fragments[index]).commit();
		}
		mTabs[currentTabIndex].setSelected(false);
		//把当前tab设为选中状态
		mTabs[index].setSelected(true);
		currentTabIndex = index;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//注销广播接收者
		try {
			unregisterReceiver(msgReceiver);
		} catch (Exception e) {}
		try {
			unregisterReceiver(ackMessageReceiver);
		} catch (Exception e) {}
		try {
			unregisterReceiver(contactInviteReceiver);
		} catch (Exception e) {}
		
	}


	/**
	 * 刷新未读消息数
	 */
	public void updateUnreadLabel() {
		int count = getUnreadMsgCountTotal();
		if (count > 0) {
			unreadLabel.setText(String.valueOf(count));
			unreadLabel.setVisibility(View.VISIBLE);
		} else {
			unreadLabel.setVisibility(View.INVISIBLE);
		}
	}
	
	/**
	 * 获取未读消息数
	 * 
	 * @return
	 */
	public int getUnreadMsgCountTotal() {
		int unreadMsgCountTotal = 0;
		unreadMsgCountTotal = EMChatManager.getInstance().getUnreadMsgsCount();
		return unreadMsgCountTotal;
	}
	
	/**
	 * 新消息广播接收者
	 * 
	 * 
	 */
	private class NewMessageBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//消息id
			String msgId = intent.getStringExtra("msgid");
			//获取mesage对象
			EMMessage message = EMChatManager.getInstance().getMessage(msgId);
			
			//刷新bottom bar消息未读数
			updateUnreadLabel();
			if(currentTabIndex == 0){
				//当前页面如果为聊天历史页面，刷新此页面
				if(chatHistoryFragment != null){
					chatHistoryFragment.refresh();
				}
			}
			//注销广播，否则在ChatActivity中会收到这个广播
			abortBroadcast();
		}
	}
	
	/**
	 * 消息回执BroadcastReceiver
	 */
	private BroadcastReceiver ackMessageReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String msgid = intent.getStringExtra("msgid");
			String from = intent.getStringExtra("from");
			EMConversation conversation = EMChatManager.getInstance().getConversation(from);
			if(conversation != null){
				//把message设为已读
				EMMessage msg = conversation.getMessage(msgid);
				if(msg != null){
					msg.isAcked = true;
				}
			}
			abortBroadcast();
		}
	};
	
	private BroadcastReceiver contactInviteReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			final String reason = intent.getStringExtra("reason");
			final boolean isResponse = intent.getBooleanExtra("isResponse", false);
			final String from = intent.getStringExtra("username");
			
			InviteMessage msg = new InviteMessage();
			msg.setFrom(from);
			msg.setTime(System.currentTimeMillis());
			msg.setReason(reason);		
			
			if(!isResponse){
				//设成未验证
				msg.setStatus(InviteMesageStatus.NO_VALIDATION);
				msg.setInviteFromMe(false);
			}else{
				//已同意
				msg.setStatus(InviteMesageStatus.AGREED);
				msg.setInviteFromMe(true);
			}
			//保存msg
			inviteMessgeDao.saveMessage(msg);
			//未读数加1
			User user = DemoApplication.getInstance().getContactList().get(Constant.NEW_FRIENDS_USERNAME);
			user.setUnreadMsgCount(user.getUnreadMsgCount()+1);
			EMNotifier notifier = new EMNotifier(MainActivity.this);
			//提示有新消息
			notifier.notifyOnNewMsg();
			//刷新好友页面ui
			if(currentTabIndex == 1)
				contactListFragment.refresh();
		}
		
	};
	private InviteMessgeDao inviteMessgeDao;
	private UserDao userDao;
	
	class RemoteContactListener implements EMContactListener{

		@Override
		public void onContactAdded(List<String> usernameList) {
			//保存增加的联系人
			Map<String,User> localUsers = DemoApplication.getInstance().getContactList();
			Map<String, User> toAddUsers = new HashMap<String, User>();
			for(String username : usernameList){
				User user = new User();
				user.setUsername(username);
				//暂时有个bug，添加好友时可能会回调added方法两次
				if(!localUsers.containsKey(username)){
					userDao.saveContact(user);
				}
				toAddUsers.put(username, user);
			}
			localUsers.putAll(toAddUsers);
			//刷新ui
			if(currentTabIndex == 1)
				contactListFragment.refresh();
			
		}

		@Override
		public void onContactDeleted(List<String> usernameList) {
			//删除联系人
			Map<String,User> localUsers = DemoApplication.getInstance().getContactList();
			for(String username : usernameList){
				localUsers.remove(username);
				userDao.deleteContact(username);
			}
			//刷新ui
			if(currentTabIndex == 1)
				contactListFragment.refresh();
			updateUnreadLabel();
		}

		
	}
	
	
	
}