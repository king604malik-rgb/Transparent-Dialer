package com.transparentdialer;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
public class DialerWidget extends AppWidgetProvider {
 static final String ACTION="com.transparentdialer.KEY", EXTRA_KEY="key", EXTRA_ID="widget";
 static final int[] KEYS={R.id.k1,R.id.k2,R.id.k3,R.id.k4,R.id.k5,R.id.k6,R.id.k7,R.id.k8,R.id.k9,R.id.star,R.id.k0,R.id.hash,R.id.clear,R.id.call,R.id.delete,R.id.contacts};
 static final String[] VALUES={"1","2","3","4","5","6","7","8","9","*","0","#","CLEAR","CALL","DEL","CONTACTS"};
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("dialer",Context.MODE_PRIVATE);}
 static void render(Context c,AppWidgetManager m,int id){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
  String number=prefs(c).getString("n"+id,"");
  v.setTextViewText(R.id.display,number.isEmpty()?" ":number);
  for(int i=0;i<KEYS.length;i++){
   Intent intent=new Intent(c,DialerWidget.class).setAction(ACTION).putExtra(EXTRA_ID,id).putExtra(EXTRA_KEY,VALUES[i]);
   PendingIntent pi=PendingIntent.getBroadcast(c,id*100+i,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   v.setOnClickPendingIntent(KEYS[i],pi);
  }
  m.updateAppWidget(id,v);
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)render(c,m,id);}
 @Override public void onDeleted(Context c,int[] ids){SharedPreferences.Editor e=prefs(c).edit();for(int id:ids)e.remove("n"+id);e.apply();}
 @Override public void onReceive(Context c,Intent intent){
  super.onReceive(c,intent);if(!ACTION.equals(intent.getAction()))return;
  int id=intent.getIntExtra(EXTRA_ID,-1);if(id<0)return;
  String key=intent.getStringExtra(EXTRA_KEY);if(key==null)return;
  String n=prefs(c).getString("n"+id,"");
  if("CALL".equals(key)||"CONTACTS".equals(key)){
   Intent open;
   if("CONTACTS".equals(key))open=new Intent(Intent.ACTION_VIEW,android.provider.ContactsContract.Contacts.CONTENT_URI);
   else open=n.isEmpty()?new Intent(Intent.ACTION_DIAL):new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(n,"+#*")));
   open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   try{c.startActivity(open);}catch(Exception ignored){}
   return;
  }
  if("CLEAR".equals(key))n="";
  else if("DEL".equals(key)){if(!n.isEmpty())n=n.substring(0,n.length()-1);}
  else if(n.length()<32)n+=key;
  prefs(c).edit().putString("n"+id,n).apply();render(c,AppWidgetManager.getInstance(c),id);
 }
}