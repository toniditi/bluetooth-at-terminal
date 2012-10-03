package com.stribogkonsult.bluetooth.DUN.treminal;


import com.stribogkonsult.bluetooth.DUN.terminal.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.EditText;

public class AboutDUNTerminal  extends Activity{
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		String version = getString(R.string.versionName);
		String buildAt = getString(R.string.build_at);
//		String app_name = getString(R.string.app_name);
//		TextView aboutText = (TextView) findViewById(R.id.textView_aboutTitle);
		EditText aboutText2 = (EditText) findViewById(R.id.editText_about);
		WebView webView = (WebView) findViewById(R.id.webView1);

		
		
		String CopyRight = "Copyright (C) 2012 Stribog Konsult";
		
		webView.loadUrl("file:///android_asset/about.html");
		aboutText2.setText( CopyRight+"\nVersion: "+version+"\nBuild at: "+ buildAt);
	}
}
