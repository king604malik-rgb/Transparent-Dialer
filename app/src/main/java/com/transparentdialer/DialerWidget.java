package com.transparentdialer;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.widget.RemoteViews;
public class DialerWidget extends AppWidgetProvider {
 static final String ACTION="com.transparentdialer.KEY", EXTRA_KEY="key", EXTRA_ID="widget";
 static final int[] KEYS={R.id.k1,R.id.k2,R.id.k3,R.id.k4,R.id.k5,R.id.k6,R.id.k7,R.id.k8,R.id.k9,R.id.star,R.id.k0,R.id.hash,R.id.clear,R.id.call,R.id.delete,R.id.contacts};
 static final String[] VALUES={"1","2","3","4","5","6","7","8","9","*","0","#","CLEAR","CALL","DEL","CONTACTS"};
 static void placeCall(Context c,String phone){
  if(phone==null||phone.trim().isEmpty())return;
  String uri="tel:"+Uri.encode(phone,"+#*");
  Intent call=new Intent(c.checkSelfPermission(Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED?Intent.ACTION_CALL:Intent.ACTION_DIAL,Uri.parse(uri));
  call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try{c.startActivity(call);}catch(SecurityException denied){try{Intent fallback=new Intent(Intent.ACTION_DIAL,Uri.parse(uri));fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(fallback);}catch(Exception ignored){}}catch(Exception ignored){}
 }
 static String formatNumber(String raw){
  if(raw==null||raw.isEmpty())return "";
  if(raw.indexOf('*')>=0||raw.indexOf('#')>=0)return raw;
  boolean plus=raw.startsWith("+");
  String d=raw.replaceAll("[^0-9]","");
  // North American numbers: 604 -> 604, 604555 -> 604-555, 6045556565 -> (604) 555-6565.
  if(d.length()<=3)return (plus?"+":"")+d;
  if(d.length()<=6)return (plus?"+":"")+d.substring(0,3)+"-"+d.substring(3);
  if(d.length()<=10)return (plus?"+":"")+"("+d.substring(0,3)+") "+d.substring(3,6)+"-"+d.substring(6);
  // +1 / 1 followed by a 10-digit NANP number.
  if(d.length()==11&&d.charAt(0)=='1')return (plus?"+":"")+"1 ("+d.substring(1,4)+") "+d.substring(4,7)+"-"+d.substring(7);
  // Keep longer/international numbers readable without changing the digits used to call.
  StringBuilder out=new StringBuilder(plus?"+":"");int first=d.length()%3;if(first==0)first=3;
  out.append(d.substring(0,first));for(int i=first;i<d.length();i+=3)out.append(' ').append(d.substring(i,Math.min(i+3,d.length())));
  return out.toString();
 }
 static String t9(String name){String abc="ABCDEFGHIJKLMNOPQRSTUVWXYZ",map="22233344455566677778889999";StringBuilder out=new StringBuilder();for(char ch:name.toUpperCase(java.util.Locale.ROOT).toCharArray()){int i=abc.indexOf(ch);if(i>=0)out.append(map.charAt(i));}return out.toString();}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("dialer",Context.MODE_PRIVATE);}
 static void render(Context c,AppWidgetManager m,int id){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
  String number=prefs(c).getString("n"+id,"");
  v.setTextViewText(R.id.display,number.isEmpty()?" ":formatNumber(number));
  for(int i=0;i<KEYS.length;i++){
   Intent intent=new Intent(c,DialerWidget.class).setAction(ACTION).putExtra(EXTRA_ID,id).putExtra(EXTRA_KEY,VALUES[i]);
   PendingIntent pi=PendingIntent.getBroadcast(c,id*100+i,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   v.setOnClickPendingIntent(KEYS[i],pi);
  }
  v.setTextViewText(R.id.match,"S I G N A T U R E");
  if(!number.isEmpty()&&c.checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED){
   String query=number.replaceAll("[^0-9]","");
   if(!query.isEmpty())try(Cursor found=c.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE ASC")){
    if(found!=null)while(found.moveToNext()){
     String name=found.getString(0),phone=found.getString(1);
     if(name==null||phone==null)continue;
     if(phone.replaceAll("[^0-9]","").contains(query)||t9(name).contains(query)){
      v.setTextViewText(R.id.match,name+"  ·  TAP TO CALL");
      Intent matched=new Intent(c,DialerWidget.class).setAction(ACTION).putExtra(EXTRA_ID,id).putExtra(EXTRA_KEY,"FAV:"+phone);
      v.setOnClickPendingIntent(R.id.match,PendingIntent.getBroadcast(c,id*100+60,matched,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
      break;
     }
    }
   }catch(Exception ignored){}
  }
  int[] favIds={R.id.fav1,R.id.fav2,R.id.fav3};
  if(c.checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED){
   try(Cursor cur=c.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},
    ContactsContract.CommonDataKinds.Phone.STARRED+"=1",null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE ASC")){
    int pos=0;java.util.HashSet<String> seen=new java.util.HashSet<>();
    if(cur!=null)while(cur.moveToNext()&&pos<3){
     String name=cur.getString(0),phone=cur.getString(1);if(phone==null||!seen.add(phone))continue;
     v.setTextViewText(favIds[pos],name==null?"FAV":name.split(" ")[0]);
     Intent dial=new Intent(c,DialerWidget.class).setAction(ACTION).putExtra(EXTRA_ID,id).putExtra(EXTRA_KEY,"FAV:"+phone);
     v.setOnClickPendingIntent(favIds[pos],PendingIntent.getBroadcast(c,id*100+30+pos,dial,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));pos++;
    }
    for(;pos<3;pos++){v.setTextViewText(favIds[pos],"·");}
   }catch(Exception ignored){}
  }else{for(int fav:favIds)v.setTextViewText(fav,"·");}
  m.updateAppWidget(id,v);
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)render(c,m,id);}
 @Override public void onDeleted(Context c,int[] ids){SharedPreferences.Editor e=prefs(c).edit();for(int id:ids)e.remove("n"+id);e.apply();}
 @Override public void onReceive(Context c,Intent intent){
  super.onReceive(c,intent);if(!ACTION.equals(intent.getAction()))return;
  int id=intent.getIntExtra(EXTRA_ID,-1);if(id<0)return;
  String key=intent.getStringExtra(EXTRA_KEY);if(key==null)return;
  String n=prefs(c).getString("n"+id,"");
  if(key.startsWith("FAV:")){placeCall(c,key.substring(4));return;}
  if("CALL".equals(key)||"CONTACTS".equals(key)){
   Intent open;
   if("CONTACTS".equals(key))open=new Intent(Intent.ACTION_VIEW,ContactsContract.Contacts.CONTENT_URI);
   else if(!n.isEmpty()){placeCall(c,n);return;}
   else open=new Intent(Intent.ACTION_DIAL);
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