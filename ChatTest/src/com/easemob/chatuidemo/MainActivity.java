package com.easemob.chatuidemo;

import java.io.File;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.EMError;
import com.easemob.EMEventListener;
import com.easemob.EMNotifierEvent;
import com.easemob.chat.EMChat;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.EMMessage.ChatType;
import com.easemob.chat.VoiceMessageBody;
import com.easemob.chatuidemo.helper.HXSDKHelper;
import com.easemob.chatuidemo.utils.CommonUtils;
import com.easemob.util.EMLog;
import com.easemob.util.VoiceRecorder;

public class MainActivity extends Activity {
	private MainActivity context;
	private Button holdToRec;
	private TextView recordingHint;
	/**录音类*/
	private VoiceRecorder voiceRecorder;
	/**要发送给的用户名*/
	private String toChatUsername = "916692273";
	/***/
	private EMConversation conversation;
	
	/**登录名*/
	private String userName="776146966";
	/**登录密码*/
	private String password="776146966";
	
	/**用于播放语音消息*/
	private MediaPlayer mediaPlayer = null;
	
	/**语音是否播放中*/
	private boolean isPlaying=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context=this;
		setContentView(R.layout.activity_main);
		voiceRecorder=new VoiceRecorder(handler);
		init();
		login();
//		registMessageReceiver();
		registMessageListener();
		EMChat.getInstance().setAppInited();//设置SDK以广播形式接收新消息
//		EMChatManager.getInstance().getChatOptions().setShowNotificationInBackgroud(false);//
		
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setMode(AudioManager.MODE_NORMAL);
		audioManager.setSpeakerphoneOn(true);
	}
	
	/**此Handler用于更新界面，接受0-13十四个等级的音量大小*/
	private Handler handler=new Handler(){
		public void handleMessage(android.os.Message msg) {
			System.out.println("The voice volum: "+msg.what);
		};
	};
	
	private void init(){
		holdToRec=(Button) findViewById(R.id.hold_to_record);
		holdToRec.setOnTouchListener(new RecTouch());
		recordingHint=(TextView) findViewById(R.id.recordingHint);
	}
	
	/**登录*/
	private void login(){
		EMChatManager.getInstance().login(userName,password,new EMCallBack() {//回调
			@Override
			public void onSuccess() {
				runOnUiThread(new Runnable() {
					public void run() {
						EMGroupManager.getInstance().loadAllGroups();
						EMChatManager.getInstance().loadAllConversations();
						Log.d("main", "登陆聊天服务器成功！");		
					}
				});
			}

			@Override
			public void onProgress(int progress, String status) {

			}

			@Override
			public void onError(int code, String message) {
				Log.d("main", "登陆聊天服务器失败！");
			}
		});
	}
	
	/**手势监听*/
	private class RecTouch implements OnTouchListener{

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!CommonUtils.isExitsSdcard()) {
					String st4 = getResources().getString(R.string.Send_voice_need_sdcard_support);
					Toast.makeText(context, st4, Toast.LENGTH_SHORT).show();
					return false;
				}
				try {
					v.setPressed(true);
//					wakeLock.acquire();
//					if (VoicePlayClickListener.isPlaying)
//						VoicePlayClickListener.currentPlayListener.stopPlayVoice();
//					recordingContainer.setVisibility(View.VISIBLE);
					recordingHint.setText(getString(R.string.move_up_to_cancel));
					recordingHint.setBackgroundColor(Color.TRANSPARENT);
					voiceRecorder.startRecording(null, toChatUsername, getApplicationContext());
				} catch (Exception e) {
					e.printStackTrace();
					v.setPressed(false);
//					if (wakeLock.isHeld())
//						wakeLock.release();
					if (voiceRecorder != null)
						voiceRecorder.discardRecording();
//					recordingContainer.setVisibility(View.INVISIBLE);
					Toast.makeText(context, R.string.recoding_fail, Toast.LENGTH_SHORT).show();
					return false;
				}

				return true;
			case MotionEvent.ACTION_MOVE: {
				if (event.getY() < 0) {
					recordingHint.setText(getString(R.string.release_to_cancel));
					recordingHint.setBackgroundResource(R.drawable.recording_hint_bg);
				} else {
					recordingHint.setText(getString(R.string.move_up_to_cancel));
					recordingHint.setBackgroundColor(Color.TRANSPARENT);
				}
				return true;
			}
			case MotionEvent.ACTION_UP:
				v.setPressed(false);
//				recordingContainer.setVisibility(View.INVISIBLE);
//				if (wakeLock.isHeld())
//					wakeLock.release();
				if (event.getY() < 0) {
					// discard the recorded audio.
					voiceRecorder.discardRecording();

				} else {
					// stop recording and send voice file
					String st1 = getResources().getString(R.string.Recording_without_permission);
					String st2 = getResources().getString(R.string.The_recording_time_is_too_short);
					String st3 = getResources().getString(R.string.send_failure_please);
					try {
						int length = voiceRecorder.stopRecoding();
						if (length > 0) {
							sendVoice(voiceRecorder.getVoiceFilePath(), length, false);
						} else if (length == EMError.INVALID_FILE) {
							Toast.makeText(getApplicationContext(), st1, Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(getApplicationContext(), st2, Toast.LENGTH_SHORT).show();
						}
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(context, st3, Toast.LENGTH_SHORT).show();
					}

				}
				return true;
			default:
//				recordingContainer.setVisibility(View.INVISIBLE);
				if (voiceRecorder != null)
					voiceRecorder.discardRecording();
				return false;
			}
		}
		
	}
	
	/**
	 * 发送语音
	 * 
	 * @param filePath
	 * @param fileName
	 * @param length
	 * @param isResend
	 */
	private void sendVoice(String filePath, int length, boolean isResend) {
		if (!(new File(filePath).exists())) {
			return;
		}
		try {
			conversation = EMChatManager.getInstance().getConversation(toChatUsername);
			EMMessage message = EMMessage.createSendMessage(EMMessage.Type.VOICE);
			//如果是群聊，设置chattype,默认是单聊
//			message.setChatType(ChatType.GroupChat);
			VoiceMessageBody body = new VoiceMessageBody(new File(filePath), length);
			message.addBody(body);
			message.setReceipt(toChatUsername);
			conversation.addMessage(message);
			EMChatManager.getInstance().sendMessage(message, new EMCallBack(){

				@Override
				public void onError(int arg0, String arg1) {
					
				}

				@Override
				public void onProgress(int arg0, String arg1) {
					
				}

				@Override
				public void onSuccess() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(context, "发送成功！", Toast.LENGTH_SHORT).show();
						}
					});
				}});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**注册接收消息*/
	private void registMessageReceiver(){
		NewMessageBroadcastReceiver msgReceiver =new NewMessageBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter(EMChatManager.getInstance().getNewMessageBroadcastAction());
		intentFilter.setPriority(3);
		registerReceiver(msgReceiver, intentFilter);
	}
	
	private class NewMessageBroadcastReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			//消息id
	        String msgId = intent.getStringExtra("msgid");
	        //发消息的人的username(userid)
	        String msgFrom = intent.getStringExtra("from");
	        //消息类型，文本，图片，语音消息等,这里返回的值为msg.type.ordinal()。
	        //所以消息type实际为是enum类型
	        int msgType = intent.getIntExtra("type", 0);
	        Log.d("main", "new message id:" + msgId + " from:" + msgFrom + " type:" + msgType);
	        //更方便的方法是通过msgId直接获取整个message
	        EMMessage message = EMChatManager.getInstance().getMessage(msgId);
	        switch(message.getType()){
	        case VOICE://语音
	        	VoiceMessageBody voiceBody=(VoiceMessageBody) message.getBody();
	        	File file = new File(voiceBody.getLocalUrl());
				if (file.exists() && file.isFile()){
					playVoice(voiceBody.getLocalUrl());
				}else{
					EMLog.e("Play VoiceMessage", "file not exist");
				}
	        	break;
			case CMD:
				break;
			case FILE://文件
				break;
			case IMAGE://图片
				break;
			case LOCATION://位置
				break;
			case TXT://文本
				break;
			case VIDEO://视频
				break;
			default:
				break;
	        }
		}
		
	}
	
	/**接收消息事件*/
	private void registMessageListener(){
		//有选择性的接收某些类型event事件
		EMChatManager.getInstance().registerEventListener(new EMEventListener() {
					
			@Override
			public void onEvent(EMNotifierEvent event) {
				// TODO Auto-generated method stub
				EMMessage message = (EMMessage) event.getData();
				switch(message.getType()){
		        case VOICE://语音
//		        	Toast.makeText(context, "收到语音消息。", Toast.LENGTH_SHORT).show();
		        	Log.e("新消息接收----->", "收到语音消息.");
		        	VoiceMessageBody voiceBody=(VoiceMessageBody) message.getBody();
//		        	File file = new File(voiceBody.getLocalUrl());
//					if (file.exists() && file.isFile()){
		        	while(true){
		        		if(message.status == EMMessage.Status.SUCCESS){
		        			playVoice(voiceBody.getLocalUrl());
		        			break;
		        		}
		        	}
//					}else{
//						EMLog.e("Play VoiceMessage", "file not exist");
//					}
		        	break;
				case CMD:
					break;
				case FILE://文件
					break;
				case IMAGE://图片
					break;
				case LOCATION://位置
					break;
				case TXT://文本
					break;
				case VIDEO://视频
					break;
				default:
					break;
		        }
			}
			}, new EMNotifierEvent.Event[]{EMNotifierEvent.Event.EventNewMessage,}
		);
	}
	
	/**播放语音消息*/
	public void playVoice(String filePath) {
		if (!(new File(filePath).exists())) {
			Log.e("PlayVoice----->", "file not exists\nfilepath: "+filePath);
			return;
		}
//		((ChatActivity) activity).playMsgId = message.getMsgId();
		
		mediaPlayer = new MediaPlayer();
//		if (HXSDKHelper.getInstance().getModel().getSettingMsgSpeaker()) {
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
//		} else {
//			audioManager.setSpeakerphoneOn(false);// 关闭扬声器
//			// 把声音设定成Earpiece（听筒）出来，设定为正在通话中
//			audioManager.setMode(AudioManager.MODE_IN_CALL);
//			mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
//		}
		try {
			mediaPlayer.setDataSource(filePath);
			mediaPlayer.prepare();
			mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {
					// TODO Auto-generated method stub
					mediaPlayer.release();
					mediaPlayer = null;
					stopPlayVoice(); // stop animation
				}

			});
			isPlaying = true;
//			currentPlayListener = this;
			mediaPlayer.start();
//			showAnimation();

			// 如果是接收的消息
//			if (message.direct == EMMessage.Direct.RECEIVE) {
//				try {
//					if (!message.isAcked) {
//						message.isAcked = true;
//						// 告知对方已读这条消息
//						if (chatType != ChatType.GroupChat)
//							EMChatManager.getInstance().ackMessageRead(message.getFrom(), message.getMsgId());
//					}
//				} catch (Exception e) {
//					message.isAcked = false;
//				}
//				if (!message.isListened() && iv_read_status != null && iv_read_status.getVisibility() == View.VISIBLE) {
//					// 隐藏自己未播放这条语音消息的标志
//					iv_read_status.setVisibility(View.INVISIBLE);
//					EMChatManager.getInstance().setMessageListened(message);
//				}
//
//			}

		} catch (Exception e) {
			Log.e("语音播放 ----->", e.getMessage(),e);
		}
	}
	
	/**停止语音消息*/
	public void stopPlayVoice() {
//		voiceAnimation.stop();
//		if (message.direct == EMMessage.Direct.RECEIVE) {
//			voiceIconView.setImageResource(R.drawable.chatfrom_voice_playing);
//		} else {
//			voiceIconView.setImageResource(R.drawable.chatto_voice_playing);
//		}
		// stop play voice
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
		}
		isPlaying = false;
//		((ChatActivity) activity).playMsgId = null;
//		adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
