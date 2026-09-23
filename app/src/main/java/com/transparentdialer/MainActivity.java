package com.transparentdialer;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
 final int WHITE=Color.WHITE, MUTED=0xff9ba9b8, PANEL=0xff151c26, GREEN=0xff7de1ad;
 LinearLayout root, matches, favorites; TextView number; String digits=""; boolean privacy=false;
 ArrayList<Person> people=new ArrayList<>(); final int REQUEST_CONTACTS=23;
 static class Person {String name,phone,photo;boolean starred; Person(String n,String p,String ph,boolean s){name=n;phone=p;photo=ph;starred=s;}}
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setFontFeatureSettings("kern");return t;}
 void buzz(){try{Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(12,45));}catch(Exception ignored){}}
 @Override public void onCreate(Bundle b){super.onCreate(b);if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CALL_PHONE},24);getWindow().setStatusBarColor(0xff090e15);getWindow().setNavigationBarColor(0xff090e15);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR==0?0:0);loadContacts();draw();}
 void loadContacts(){
  people.clear();if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return;
  try(Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
   new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.PHOTO_URI,ContactsContract.CommonDataKinds.Phone.STARRED},
   null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE ASC")){
   if(c!=null)while(c.moveToNext()){String n=c.getString(0),p=c.getString(1),photo=c.getString(2);boolean star=c.getInt(3)!=0;
    if(!TextUtils.isEmpty(p))people.add(new Person(n==null?"Unknown":n,p,photo,star));}
  }catch(Exception ignored){}
 }
 void refreshWidgets(){AppWidgetManager m=AppWidgetManager.getInstance(this);int[] ids=m.getAppWidgetIds(new ComponentName(this,DialerWidget.class));for(int id:ids)DialerWidget.render(this,m,id);}
 void draw(){
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(0xff090e15);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(20),dp(20),dp(24));sc.addView(root);setContentView(sc);
  TextView brand=label("T R A N S P A R E N T   /   S I G N A T U R E",10,MUTED);root.addView(brand);
  TextView subtitle=label("YOUR PERSONAL DIALER",10,0xff697989);LinearLayout.LayoutParams sub=new LinearLayout.LayoutParams(-1,dp(30));root.addView(subtitle,sub);
  number=label("",29,WHITE);number.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));root.addView(number,new LinearLayout.LayoutParams(-1,dp(65)));updateNumber();
  LinearLayout quick=new LinearLayout(this);quick.setGravity(Gravity.CENTER);root.addView(quick);
  addTextButton(quick,"COPY",()->{if(!digits.isEmpty()){android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(android.content.ClipData.newPlainText("Phone number",digits));Toast.makeText(this,"Number copied",Toast.LENGTH_SHORT).show();}});
  addTextButton(quick,"SAVE",()->{Intent i=new Intent(Intent.ACTION_INSERT,ContactsContract.Contacts.CONTENT_URI);i.putExtra(ContactsContract.Intents.Insert.PHONE,digits);startActivity(i);});
  addTextButton(quick,"PRIVACY",()->{privacy=!privacy;updateNumber();});
  String[][] keys={{"1",""},{"2","ABC"},{"3","DEF"},{"4","GHI"},{"5","JKL"},{"6","MNO"},{"7","PQRS"},{"8","TUV"},{"9","WXYZ"},{"*",""},{"0","+"},{"#",""}};
  for(int r=0;r<4;r++){LinearLayout line=new LinearLayout(this);root.addView(line,new LinearLayout.LayoutParams(-1,dp(72)));
   for(int col=0;col<3;col++){String[] key=keys[r*3+col];LinearLayout cell=new LinearLayout(this);cell.setOrientation(1);cell.setGravity(Gravity.CENTER);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);cp.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(cell,cp);cell.setBackground(bg(PANEL,22));
    TextView big=label(key[0],27,WHITE);big.setTypeface(Typeface.create("sans-serif-light",0));cell.addView(big);
    TextView small=label(key[1].isEmpty()?" ":key[1],9,MUTED);small.setLetterSpacing(.18f);cell.addView(small);
    cell.setOnClickListener(v->{buzz();digits+=key[0];updateNumber();});if(key[0].equals("0"))cell.setOnLongClickListener(v->{buzz();digits+="+";updateNumber();return true;});
   }
  }
  LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);root.addView(actions,new LinearLayout.LayoutParams(-1,dp(78)));
  addAction(actions,"⌫",MUTED,()->{buzz();if(!digits.isEmpty())digits=digits.substring(0,digits.length()-1);updateNumber();});
  addAction(actions,"☎",GREEN,()->{buzz();Intent i=digits.isEmpty()?new Intent(Intent.ACTION_DIAL):new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(digits,"+#*")));startActivity(i);});
  addAction(actions,"CLEAR",MUTED,()->{buzz();digits="";updateNumber();});
  TextView favTitle=label("F A V O U R I T E S",11,MUTED);favTitle.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);root.addView(favTitle,new LinearLayout.LayoutParams(-1,dp(40)));
  favorites=new LinearLayout(this);favorites.setOrientation(1);root.addView(favorites);renderFavorites();
  TextView searchTitle=label("C O N T A C T S   /   T 9   S E A R C H",11,MUTED);searchTitle.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);root.addView(searchTitle,new LinearLayout.LayoutParams(-1,dp(44)));
  matches=new LinearLayout(this);matches.setOrientation(1);root.addView(matches);renderMatches();
  if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){
   TextView grant=label("ALLOW CONTACTS TO ENABLE FAVOURITES + T9",12,GREEN);grant.setPadding(0,dp(20),0,dp(20));root.addView(grant);grant.setOnClickListener(v->requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},REQUEST_CONTACTS));
  }
 }
 void addTextButton(LinearLayout row,String s,Runnable run){TextView t=label(s,10,MUTED);row.addView(t,new LinearLayout.LayoutParams(0,dp(40),1));t.setOnClickListener(v->run.run());}
 void addAction(LinearLayout row,String s,int color,Runnable run){TextView t=label(s,s.equals("CLEAR")?12:29,color);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(60),1);p.setMargins(dp(4),0,dp(4),0);row.addView(t,p);t.setBackground(bg(PANEL,30));t.setOnClickListener(v->run.run());}
 void updateNumber(){if(number!=null)number.setText(digits.isEmpty()?"Enter a number":privacy?digits.replaceAll(".","•"):digits);if(matches!=null)renderMatches();}
 String t9(String s){String abc="ABCDEFGHIJKLMNOPQRSTUVWXYZ";String map="22233344455566677778889999";StringBuilder b=new StringBuilder();for(char ch:s.toUpperCase(Locale.ROOT).toCharArray()){int i=abc.indexOf(ch);if(i>=0)b.append(map.charAt(i));}return b.toString();}
 void renderFavorites(){favorites.removeAllViews();int count=0;HashSet<String> seen=new HashSet<>();for(Person p:people){if(!p.starred||!seen.add(p.phone))continue;addPerson(favorites,p);if(++count==4)break;}if(count==0)favorites.addView(label("Star contacts in Samsung Contacts to show them here",12,MUTED));}
 void renderMatches(){matches.removeAllViews();if(digits.isEmpty()){matches.addView(label("Type digits to find a contact by name or number",12,MUTED));return;}String q=digits.replaceAll("[^0-9]","");if(q.isEmpty())return;int count=0;HashSet<String> seen=new HashSet<>();for(Person p:people){String phone=p.phone.replaceAll("[^0-9]","");if((phone.contains(q)||t9(p.name).contains(q))&&seen.add(p.phone)){addPerson(matches,p);if(++count==6)break;}}if(count==0)matches.addView(label("No matching contacts",12,MUTED));}
 void addPerson(LinearLayout parent,Person p){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(6),dp(8),dp(6));parent.addView(row,new LinearLayout.LayoutParams(-1,dp(64)));
  ImageView avatar=new ImageView(this);avatar.setImageResource(android.R.drawable.ic_menu_myplaces);avatar.setColorFilter(MUTED);avatar.setBackground(bg(PANEL,30));avatar.setPadding(dp(12),dp(12),dp(12),dp(12));row.addView(avatar,new LinearLayout.LayoutParams(dp(46),dp(46)));
  if(p.photo!=null)try{avatar.setImageURI(Uri.parse(p.photo));avatar.clearColorFilter();avatar.setPadding(0,0,0,0);avatar.setClipToOutline(true);}catch(Exception ignored){}
  LinearLayout info=new LinearLayout(this);info.setOrientation(1);info.setPadding(dp(12),0,0,0);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
  TextView name=label(p.name,15,WHITE);name.setGravity(Gravity.START);info.addView(name);
  TextView phone=label(p.phone,12,MUTED);phone.setGravity(Gravity.START);info.addView(phone);
  TextView call=label("☎",23,GREEN);row.addView(call,new LinearLayout.LayoutParams(dp(48),dp(48)));
  call.setOnClickListener(v->{buzz();startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(p.phone,"+#*"))));});
  row.setOnClickListener(v->{digits=p.phone;updateNumber();number.requestFocus();});
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==REQUEST_CONTACTS){loadContacts();refreshWidgets();draw();}}
 @Override protected void onResume(){super.onResume();if(root!=null&&checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED){loadContacts();renderFavorites();renderMatches();refreshWidgets();}}
}