package com.easemob.chatuidemo.adapter;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.ImageView;

import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.VoiceMessageBody;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.activity.ChatActivity;

class VoicePlayClickListener implements View.OnClickListener {

	EMMessage message;
	VoiceMessageBody voiceBody;
	ImageView voiceIconView;

	private AnimationDrawable voiceAnimation = null;
	MediaPlayer mediaPlayer = null;
	ImageView iv_read_status;
	private Context context;
	Activity activity;
	private String username;
	private int chatType;

	static boolean isPlaying = false;
	static VoicePlayClickListener currentPlayListener = null;
	static EMMessage currentMessage = null;


	/**
	 * 
	 * @param message
	 * @param v
	 * @param iv_read_status
	 * @param context
	 * @param activity
	 * @param user
	 * @param chatType
	 */
	public VoicePlayClickListener(EMMessage message, ImageView v, ImageView iv_read_status, Context context, Activity activity,
			String username, int chatType) {
		this.message = message;
		voiceBody = (VoiceMessageBody) message.getBody();
		this.iv_read_status = iv_read_status;
		this.context = context;
		voiceIconView = v;
		this.activity = activity;
		this.username = username;
		this.chatType = chatType;
	}

	private void stopPlayVoice() {
		voiceAnimation.stop();
		if (message.direct == EMMessage.Direct.RECEIVE) {
			voiceIconView.setImageResource(R.drawable.chatfrom_voice_playing);
		} else {
			voiceIconView.setImageResource(R.drawable.chatto_voice_playing);
		}
		// stop play voice
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
		}
		isPlaying = false;
	}

	private void playVoice(String filePath) {
		if (!(new File(filePath).exists())) {
			return;
		}

		mediaPlayer = new MediaPlayer();
		if (EMChatManager.getInstance().getChatOptions().getUseSpeaker())
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
		else
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
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
			currentPlayListener = this;
			currentMessage = message;
			mediaPlayer.start();
			showAnimation();
			try {
				if (!message.isAcked) {
					message.isAcked = true;
					EMChatManager.getInstance().ackMessageRead(message.getFrom(), message.getMsgId());
				}
			} catch (Exception e) {
				message.isAcked = false;
			}
		} catch (Exception e) {
		}
	}

	// show the voice playing animation
	private void showAnimation() {
		// play voice, and start animation
		if (message.direct == EMMessage.Direct.RECEIVE) {
				voiceIconView.setImageResource(R.anim.voice_from_icon);
		} else {
				voiceIconView.setImageResource(R.anim.voice_to_icon);
		}
		voiceAnimation = (AnimationDrawable) voiceIconView.getDrawable();
		voiceAnimation.start();
	}

	@Override
	public void onClick(View v) {
		if (iv_read_status != null && iv_read_status.getVisibility() == View.VISIBLE) {
			iv_read_status.setVisibility(View.INVISIBLE);
			// ContentValues values = new ContentValues();
			// values.put(MessageTable.COLUMN_NAME_IS_READED,1);
			// EaseMobMsgDB.updateMessage(context, user instanceof Group?
			// ((Group) user).getGroupId() : user.getUsername(),
			// message.getRowId(), values);
			message.setAttribute("played", true);
		}

		if (isPlaying) {
			currentPlayListener.stopPlayVoice();
			if (currentMessage != null && currentMessage.hashCode() == message.hashCode()) {
				currentMessage = null;
				return;
			}
		}

		if (message.direct == EMMessage.Direct.SEND) {
			// for sent msg, we will try to play the voice file directly
			playVoice(voiceBody.getLocalUrl());
		} else {
			// for received msg. if already download the voice file,
			// play direclty
			File file = new File(voiceBody.getLocalUrl());
			if (file.exists() && file.isFile())
				playVoice(voiceBody.getLocalUrl());
			else {
//				System.err.println("!!! need to check!!!!! download voice changed in sdk2.0");
//				Intent intent = new Intent(activity, AlertDialog.class).putExtra("msg", "语音文件接收失败，是否下载此文件").putExtra("isCanceShow", true)
//						.putExtra("voicePath", voiceBody.localUrl);
//				activity.startActivityForResult(intent, ChatActivity.REQUEST_CODE_DOWNLOAD_VOICE);

			}
		}
	}

	interface OnVoiceStopListener {
		void onStop();

		void onStart();
	}

}