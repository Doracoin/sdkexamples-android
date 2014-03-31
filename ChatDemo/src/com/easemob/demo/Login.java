package com.easemob.demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.easemob.exceptions.EMAuthenticationException;
import com.easemob.exceptions.EMNetworkUnconnectedException;
import com.easemob.exceptions.EMResourceNotExistException;
import com.easemob.exceptions.EaseMobException;
import com.easemob.ui.activity.AlertDialog;
import com.easemob.user.EMUser;
import com.easemob.user.EMUserManager;
import com.easemob.user.callbacks.LoginCallBack;

public class Login extends Activity {
	private static final String TAG = Login.class.getSimpleName();

	private EditText usernameEditText;
	private EditText passwordEditText;
	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		if (ChatDemoApp.getInstance().getUserName() != null && ChatDemoApp.getInstance().getPassword() != null) {
			startActivity(new Intent(this, MainActivity.class));
			finish();
		} else {
			usernameEditText = (EditText) findViewById(R.id.username);
			passwordEditText = (EditText) findViewById(R.id.password);
			String userName = ChatDemoApp.getInstance().getUserName();
			if (userName != null) {
				usernameEditText.setText(userName);
			}
		}
	}

	/**
	 * 登陆
	 * 
	 * @param view
	 */
	public void login(View view) {
		final String userName = usernameEditText.getText().toString();
		final String password = passwordEditText.getText().toString();
		if (userName.isEmpty()) {
			startActivity(new Intent(this, AlertDialog.class).putExtra("msg", getString(R.string.login_input_username)));
		} else if (password.isEmpty()) {
			startActivity(new Intent(this, AlertDialog.class).putExtra("msg", getString(R.string.login_input_pwd)));
		} else {
			showLoginDialog();
			ChatDemoApp.getInstance().setUserName(userName);

			// 登陆到easemob 用户服务器，同时会登陆聊天服务器
			EMUserManager.getInstance().login(userName, password, new LoginCallBack() {
				@Override
				public void onSuccess(Object user) {
					// 登陆成功
					ChatDemoApp.getInstance().setPassword(password);

					closeLogingDialog();
					startActivity(new Intent(Login.this, MainActivity.class).putExtra("loggedin", true));
					//show how to update attributes
					/*
					try {
					    EMUser currentUser = EMUserManager.getInstance().getUser(userName);
					    currentUser.setProperty("ext_attr_string", "this is string value");
					    currentUser.setProperty("ext_attr_int", 108);
					} catch (Exception eee) {
					    eee.printStackTrace();
					}
					*/
					
					finish();
				}

				@Override
				public void onFailure(final EaseMobException cause) {
					// 登陆失败
					Log.e(TAG, "login: " + cause.getMessage());
					closeLogingDialog();

					if (cause instanceof EMAuthenticationException) {
						startActivity(new Intent(Login.this, AlertDialog.class).putExtra("msg", getString(R.string.login_failure) + ": "
								+ getString(R.string.login_failuer_pswerror)));
					} else if (cause instanceof EMNetworkUnconnectedException) {
						startActivity(new Intent(Login.this, AlertDialog.class).putExtra("msg", getString(R.string.login_failure) + ": "
								+ getString(R.string.login_failuer_network_unconnected)));
					} else if (cause instanceof EMResourceNotExistException) {
						startActivity(new Intent(Login.this, AlertDialog.class).putExtra("msg", getString(R.string.login_failure) + ": "
								+ getString(R.string.login_failuer_toast)));
					} else {
						startActivity(new Intent(Login.this, AlertDialog.class).putExtra("msg", getString(R.string.login_failure) + ": "
								+ getString(R.string.login_failuer_toast)));
					}
				}

				@Override
				public void onProgress(final String progress) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.setMessage(progress);
						}
					});
				}
			});

		}
	}

	public void register(View view) {
		startActivityForResult(new Intent(this, Register.class), 0);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (ChatDemoApp.getInstance().getUserName() != null) {
			usernameEditText.setText(ChatDemoApp.getInstance().getUserName());
		}
	}

	private void showLoginDialog() {
		if ((!isFinishing()) && (this.progressDialog == null)) {
			this.progressDialog = new ProgressDialog(this);
		}
		this.progressDialog.setMessage(getString(R.string.logining));
		this.progressDialog.setCanceledOnTouchOutside(false);
		this.progressDialog.show();
	}

	private void closeLogingDialog() {
		if (this.progressDialog != null) {
			this.progressDialog.dismiss();
		}
	}

}