package com.transparentdialer;
import android.app.Activity; import android.os.Bundle; import android.widget.TextView;
public class MainActivity extends Activity { @Override public void onCreate(Bundle b){super.onCreate(b); TextView t=new TextView(this);t.setText("Transparent Dialer\n\nLong-press your home screen → Widgets → Transparent Dialer.");t.setTextSize(22);t.setPadding(32,64,32,32);setContentView(t);} }
