package com.stribogkonsult.bluetooth.DUN.treminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.stribogkonsult.bluetooth.DUN.terminal.R;

public class MyblueActivity extends Activity {

	private static EditText mEditText;
	private Button mSendButton;
	private EditText mEditTextToSend;

	private static final String TAG = "BTTERMINAL";
	private static final boolean D = false;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothSocket btSocket = null;
	public static final int BYTES_READ = 1;
	private ConnectedThread mConnectedThread;
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_ENABLE_BT = 3;

	private static final UUID MY_UUID = UUID
			.fromString("00001103-0000-1000-8000-00805F9B34FB");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mEditTextToSend = (EditText) findViewById(R.id.edit_text_out);
		mEditText = (EditText) findViewById(R.id.editText1);
		mEditTextToSend.setText("at");
		mEditTextToSend.requestFocus();
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String message = mEditTextToSend.getText().toString() + "\r";
				byte[] out = message.getBytes();
				synchronized (this) {
					mConnectedThread.write(out);
				}
			}
		});
		
		Button eqButton = (Button) findViewById(R.id.button_eq);
		eqButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int start = mEditTextToSend.getSelectionStart();
				int end = mEditTextToSend.getSelectionEnd();
				mEditTextToSend.getText().replace(
						Math.min(start, end), Math.max(start, end),"=", 0, 1);
				
			}
		});
		
		
		
		mSendButton.setEnabled(false);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			errorExit("Bluetooth is not available.", true);
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.menu_about:
			serverIntent = new Intent(this, AboutDUNTerminal.class);
			startActivity(serverIntent);
			return true;
		}
		return false;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (btSocket == null)
				startDeviceList();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		// finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
		}
		finish();
	}

	private void errorExit(String text, Boolean fMakeToast) {
		if (fMakeToast)
			Toast.makeText(this, text, Toast.LENGTH_LONG).show();

		if(D) 
			Log.e(TAG, "errorExit: " + text + "\n");

		if (fMakeToast)
			finish();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				try {
					btSocket = device
							.createRfcommSocketToServiceRecord(MY_UUID);
				} catch (IOException e) {
					errorExit("Can't create socket", true);
				}
				try {
					btSocket.connect();
				} catch (IOException e) {
					errorExit("Can't connect to device", true);
				}
				mSendButton.setEnabled(true);
				mConnectedThread = new ConnectedThread(btSocket);
				mConnectedThread.start();
			} else {
				// finish();
			}
			break;
		case REQUEST_ENABLE_BT:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				waitBT();
				startDeviceList();
			} else {
				errorExit("Please enable your BT and re-run this program.",
						true);
			}
		}
	}

	private void waitBT() {
		for (int i = 0; i < 5; i++) {
			try {
				Thread.sleep(3000);
				if (mBluetoothAdapter.isEnabled()) {
					break;
				}
			} catch (InterruptedException e) {
				errorExit("Unknown error.", true);
			}
		}
		if (!mBluetoothAdapter.isEnabled()) {
			errorExit("Please enable your BT and re-run this program.", true);
		}
	}

	public void startDeviceList() {
		if(btSocket != null){
			return;
		}
		Intent serverIntent = null;
		serverIntent = new Intent(getApplicationContext(),
				DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
	}

	private class ConnectedThread extends Thread {
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		private final BluetoothSocket mmSocket;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				errorExit("Can't connect to device", true);
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer;
			int bytes;
			while (true) {
				try {
					buffer = new byte[1024];
					bytes = mmInStream.read(buffer);
					mHandler.obtainMessage(BYTES_READ, bytes, -1, buffer)
							.sendToTarget();
					buffer = null;
				} catch (IOException e) {
					errorExit("Lost connection", false);
					return;
				}
			}
		}

		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
			} catch (IOException e) {
				if(D) 
					Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				if(D) 
					Log.e(TAG, "close() of connect socket failed", e);
			}
		}

	}
	static class MyHandler extends Handler{}
	private final static Handler mHandler = new MyHandler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BYTES_READ:
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				mEditText.append(readMessage);
				break;
			}
		}
	};
}
