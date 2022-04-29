package com.example.imomoe;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    // view部分
    @SuppressLint("StaticFieldLeak")
    private static WebView webView;
    private FrameLayout mLayout;


    // 广告的
    private String[] adUrls = {"app"};
    final String file_for_ad="adurls.txt";

    // 设置的
    private final String[] properties = {"notes_for_bangumi","notes_for_test","timeSet_skipOp","is_log_play_history","during_history"};
    private final JSONObject settings_json = new JSONObject();
    final String file_for_setting="settings_contents.txt";
    float timeSet_skipOp = 86; // 跳op的时间

    // 播放记录
    final String file_for_playLog = "play_time_logs.txt"; // 播放记录
    final long days_today = LocalDate.now().toEpochDay();         // 天数
    private String is_log_play_history = "true";                 // 是否自动记录播放历史
    float during_history = 10;                                    // 记录播放历史的时间（此前的都清空）
    private String can_skip = "false";
    private String skip_time = "1";
    String bangumi_url;
    JSONObject playLog_json = new JSONObject();                   // 存放所有播放记录

    // 通信的
    private final receiver_main receiverMain = new receiver_main();
    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.mLayout);
        webView = findViewById(R.id.webView);


        // 更新广告字符
        save_file(file_for_ad,"","APPEND"); // 表示先创建adurls.txt
        String newAD= null; // 每次启动都要刷新广告
        try {
            newAD = read_file(file_for_ad);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (newAD!=null & !newAD.equals("")){
            String[] newADs=newAD.split(",");
            adUrls=new_strs(adUrls,newADs);
        }
        Log.d("zhiwei","original adurls:"+list_join(",",adUrls));

        // 更新设置信息
        save_file(file_for_setting,"","APPEND"); // 表示先创建settings_contents.txt
        String info_setting = null; // 每次启动都要更新设置
        try {
            info_setting = read_file(file_for_setting);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (info_setting!=null & info_setting.length()>10){
            try {
                JSONObject new_setting = new JSONObject(info_setting);
                for (String property : properties) { // 我靠，我爱了呀，这个语法推荐转换
                    settings_json.put(property, new_setting.get(property));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 检查遗漏
        try {
            for (String property : properties) {
                if (!settings_json.has(property)){
                    settings_json.put(property, "");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            is_log_play_history = settings_json.getString("is_log_play_history");
            if (is_log_play_history.length()<=0){
                is_log_play_history = "true";
            }
            String temp = settings_json.getString("during_history");
            if (temp.length()>0){
                during_history = Float.parseFloat(temp);
            } else {
                during_history = 10;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 读取播放记录
        save_file(file_for_playLog,"","APPEND");    // 同样是先保存（创建）
        String newLogs = "";
        String[] newLogs_list = null;
        try {
            newLogs = read_file(file_for_playLog);
            save_file(file_for_playLog,"","PRIVATE"); // 清空
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 将存储的记录读取到 playLog_json 中
        if (newLogs.length()>10){
            newLogs_list = newLogs.split("playLog");
            for (String newlog : newLogs_list) {
                if (newlog.length()>1){
                    JSONObject new_log;
                    try {
                        new_log = new JSONObject(newlog);
                        String temp_log = Objects.requireNonNull(new_log.names()).toString();
                        String name = temp_log.substring(2,temp_log.length()-2);
                        String value = new_log.getString(name);
                        long long_date=Long.parseLong(value.substring(0,value.indexOf(",")));
                        if (days_today-long_date<=during_history){
                            playLog_json.put(name,value);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // 刷新playLog，并验证
            String[] names = playLog_json.names().toString().replace("[\"","").replace("\"]","").split("\",\"");
            for (String name : names){
                try {
                    save_file(file_for_playLog,"{\""+name+"\":"+"\""+playLog_json.getString(name)+"\"}playLog","APPEND");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            try {
                Log.d("zhiwei","rewrite playLog: "+read_file(file_for_playLog));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // 载入网页
        try {
            webView.loadUrl("https://m.yhdmp.live/"); //樱花动漫
        }
        catch(Exception e){
            webView.loadUrl("https://m.yhdmp.net/"); //樱花动漫
        }


        // webView设置
        webView.requestFocusFromTouch();
        webView.requestFocus();
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);// 启用JS脚本
        webSettings.setSupportZoom(true); // 支持缩放
        webSettings.setBuiltInZoomControls(true); // 启用内置缩放装置
        webSettings.setDomStorageEnabled(true); //我靠这一行打开就放开了广告，但是关掉有些不能看了


        // JS与Java的交互
        webView.addJavascriptInterface(new js_interface(this), "Settings");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("broadcast of yhdm");
        registerReceiver(receiverMain,intentFilter);

        // 下载功能
        webView.setDownloadListener(new MyWebViewDownLoadListener());

        webView.setWebViewClient(new WebViewClient() {
            // 当初为了屏蔽广告而设立的
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 判断url链接中是否含有某个字段，如果有就执行指定的跳转（不执行跳转url链接），如果没有就加载url链接
                return !url.contains("yhdmp");
            }

            // 隐藏广告，后面通过js来删除广告
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (!block_ad(getBaseContext(), url, adUrls)) {
                    return super.shouldInterceptRequest(view, url);//正常加载
                } else {
                    return new WebResourceResponse(null, null, null);//含有广告资源屏蔽请求
                }
            }

            // js，操作一些东西
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                bangumi_url = null;
                can_skip = "false";
                if (url.contains("vp/") & url.contains(".html")){
                    bangumi_url=url.substring(url.indexOf("vp/")+3,url.indexOf(".html"));
                }

                if (playLog_json.has(bangumi_url)){
                    can_skip = "true";
                    try {
                        String temp_time = playLog_json.getString(bangumi_url);
                        skip_time = temp_time.substring(temp_time.indexOf(",")+1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

//                 删除广告的js，顺便储存广告。第一优先级  div1
                webView.evaluateJavascript("javascript:"
                        + "var div1=document.getElementsByTagName('style');"
                        +"div1[div1.length-1].nextElementSibling.click();" // 先删
                        + "var returnValue=div1[div1.length-1].innerHTML;" // 后存
                        + "(function myFunction(value) {return value;})(returnValue);", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (value.length()>10) {
                            String res = value.split("\\.")[1];

                            if (!Arrays.asList(adUrls).contains(res)){
                                // 此处修改，目标为res

                                adUrls=new_strs(adUrls,new String[] {res});
                                Toast.makeText(getApplicationContext(),"记录广告："+res,Toast.LENGTH_SHORT).show();

                                save_file(file_for_ad,res+",","APPEND");
                                webView.reload();
                            }
                        }
                    }
                });

                // 设置图标的js，放后面      div_head, btn_setting
                webView.loadUrl("javascript:"
                                +"var div_head=document.getElementsByClassName('nav fr')[0];"
                                +"div_head.removeChild(document.getElementById('nav_fr_to_pc'));"
                                +"let btn_setting = document.createElement('button');"
                                +"btn_setting.style=\"position:absolute;right:88px;background-color:transparent;color:#FFFFFF;border:2.4px solid white;border-radius:50%;display:right;height:23px;width:23px;margin-top:10px;text-align:center;font-family:Consolas;font-weight:bold;\";"
                                +"btn_setting.textContent='S';"
                                +"btn_setting.title='设置';"
                                +"div_head.insertBefore(btn_setting,div_head.childNodes[0]);"

                                +"btn_setting.onclick=function(){"
                                +"btn_setting.style.border='2.4px solid #66ccff';"
                                +"Settings.load_settings();};"
                );


                // 显示广告，和播放记录数量：
                webView.loadUrl("javascript:"
                        +"var div_ad=document.getElementById('ad_setting');"
                        +"if (div_ad) {"
                        +"var div_ad_p2=document.getElementById('ad_p2');"
                        +"var div_ad_p2_summary=document.getElementById('ad_p2_summary');"
                        +"let div_ad_number=document.createElement('div');"
                        +"div_ad_number.innerHTML='<p>："+adUrls.length+"</p>';"
                        +"div_ad.insertBefore(div_ad_number,div_ad_p2);"

                        +"let div_ad_details=document.createElement('div');"
                        +"div_ad_details.innerHTML='<p>"+list_join(", ",adUrls)+"</p>';"
                        +"div_ad_details.style=\"font-size:12px\";"
                        +"div_ad_p2.insertBefore(div_ad_details,div_ad_p2_summary.nextElementSibling);"

                        +"var playLog_number=document.createElement('div');"
                        +"playLog_number.innerHTML='<p>："+playLog_json.length()+"</p>';"
                        +"playLog_number.style='float:left';"
                        +"var playLog_div=document.getElementById('playLog');"
                        +"var playLog_number_div=document.getElementById('playLog_number_next');"
                        +"playLog_div.insertBefore(playLog_number,playLog_number_div);"

                        +"var clear_logs=document.getElementById('clear_logs');"
                        +"clear_logs.onclick=function(){"
                        +"if (confirm('确认清空记录？')){Settings.clear_logs();playLog_number.innerHTML='<p>：0</p>';};};"
                        +"};"
                );

                // 显示或者保存设置
                String p1="";
                String p3="";
                String p4="";
                String p5="";
                try {
                    p1=settings_json.getString(properties[0]);
                    p3=settings_json.getString(properties[2]);
                    p4=settings_json.getString(properties[3]);
                    p5=settings_json.getString(properties[4]);
                    if (p3.length()>0) {
                        timeSet_skipOp=Float.parseFloat(p3);
                    }else {
                        timeSet_skipOp=1;
                    }
                    if (p4.length()>0) {
                        is_log_play_history = p4;
                    }else {
                        is_log_play_history = "true";
                    }
                    if (p5.length()>0) {
                        during_history=Float.parseFloat(p5);
                    }else {
                        during_history=10;
                    }
                } catch (JSONException ignored) {
                }
                p1=p1.replace("Sign_N_ZhiWei","\\n").replace("Sign_R_ZhiWei","\\r");

                // 显示设置         div1s, div2s, div3s, div_saveSet, p1,p2,p3,strings
                webView.loadUrl("javascript:"
                    +"var div1s=document.getElementById('notes_for_bangumi');if (div1s){"
                        +"div1s.value='"+p1+"';"
                        +"var div2s=document.getElementById('notes_for_test');"
                        +"var div3s=document.getElementById('timeSet_skipOp');"
                        +"div3s.value="+timeSet_skipOp+";"
                        +"var div4y=document.getElementById('history_check_yes');"
                        +"var div4n=document.getElementById('history_check_no');"
                        +"if ("+is_log_play_history+"){div4y.checked=true;}"
                        +"else {div4n.checked=true;};"
                        +"var div5s=document.getElementById('play_history_during');"
                        +"div5s.value="+during_history+";"

                        +"var settings_temp='nth';"
                        +"function save_settings(){"
                            +"var p1=div1s.value.split('\\n').join('Sign_N_ZhiWei').split('\\r').join('Sign_R_ZhiWei');"
                            +"var p2=div2s.value.split('\\n').join('Sign_N_ZhiWei').split('\\r').join('Sign_R_ZhiWei');"
                            +"var p3=div3s.value;"
                            +"var p4=div4y.checked;"
                            +"var p5=div5s.value;"
                            +"var settings_new= '{' + 'p1:' +'\"'+p1+'\"'  +',p2:'+'\"'+p2+'\"'  +',p3:'+'\"'+p3+'\"' +',p4:'+'\"'+p4+'\"' +',p5:'+'\"'+p5+'\"' +'}';"
                            +"if (settings_temp != settings_new){Settings.save_settings(settings_new);};"
                            +"settings_temp = settings_new;"
                        +"}"
                    +"setInterval(save_settings, 200);}"
                );

                // 私货js，防止骚扰加个小概率，放在最后      new1, new2
                webView.loadUrl("javascript:if (Math.random()<0.1){"
                        +"if (document.getElementById('author_setting')===null){"
                        +"var new1=document.getElementsByTagName('a');"
                        +"let new2=document.createElement('div');"
                        +"new2.innerHTML=\"<p style='color:gray;font-size:15px;font-family:\'Times New Roman\',\'楷体\';'>如有问题请及时反馈给：写这个app的伟人：QQ: 2634205895</p>\";"
                        +"document.getElementsByTagName('body')[0].insertBefore(new2,new1[new1.length-1]);}}"
                );

                // 跳op的js，放在最后      btn_op, div1o, div3o, div4o, y, time_current, time_log, msg, v1, v2, v3, viDeo
                webView.loadUrl("javascript:"
                        +"var div4o=document.getElementsByClassName('fav fr');"
                        +"let btn_op = document.createElement('button');"
                        +"btn_op.innerHTML='跳op';btn_op.id='skip_op';"
                        +"var div6o=document.getElementsByClassName('playbg');"
                        +"div6o[0].insertBefore(btn_op,div4o[0]);"

                        +"$(function(){"
                            +"var div1o=document.getElementsByTagName('iframe');"
                            +"var y=(div1o[0].contentWindow || div1o[0].contentDocument);if (y.document)y=y.document;"
                            +"var div3o=y.getElementsByTagName('video');"

                            // 获取视频
                            +"function getVideo(){"
                                +"var v1=document.getElementsByTagName('iframe');"
                                +"var v2=(v1[0].contentWindow || v1[0].contentDocument);if (v2.document)v2=v2.document;"
                                +"var v3=v2.getElementsByTagName('video');"
                                +"return v3[0];"
                            +"};"

                            // 跳op
                            +"btn_op.onclick=function(){"
                            +"Settings.print('skip op');"
                            +"getVideo().currentTime+="+timeSet_skipOp+";};"

//                          // 启动监听的函数
                                +"function listen(){"
                                +"Settings.print('allow to log');"
                                +"var time_log=0;"
                                +"let time_show_div = document.createElement('input');"
                                +"time_show_div.type='text';time_show_div.style='width:40px; margin-left:15px; margin-right:5px; color:gray; border:none;';"
                                +"time_show_div.value='0:00';"
                                +"div6o[0].insertBefore(time_show_div,div4o[0]);"
                                +"var viDeo=getVideo();"

                                +"if ("+can_skip+"){"       // 因为优化了，所以不需要按钮，劳资nb
                                +"Settings.print('can skip');"
                                +"viDeo.currentTime="+skip_time+";"
                                +"};"

                                +"viDeo.addEventListener('timeupdate',function(){"
                                    +"var time_current=Math.ceil(this.currentTime)-1;"  // -1修正
                                    +"if (time_current-time_log>=1 || time_current-time_log<-1){"
                                    +"time_log=parseInt(time_current);"

                                    +"var tail=time_log % 60;if (tail<10){tail='0'+tail;};"
                                    +"if (time_log>=3600){var time_show=parseInt(time_log/3600)+':'+parseInt((time_log % 3600)/60)+':'+tail;}"
                                    +"else{var time_show=parseInt(time_log/60)+':'+tail;};"
                                    +"time_show_div.value=time_show;"

                                    +"var msg='{'+"+ "'\""+bangumi_url+"\"'" +"+':\"'+"+ "'"+days_today+"'" +"+','+time_log" +"+'\"}';"
                                    +"Settings.show_time_play(msg);};"
                                +"});"

                                +"Settings.print('load successfully');"
                            +"};"

                            // 新的延迟sleep方式
                            +"if ("+is_log_play_history+"){"
                            +"var times_for_load = 1000;"
                            +"var interval = setInterval(function(){"
                            +"if (getVideo() || times_for_load <0){"
                            +"clearInterval(interval);"
                            +"listen();} else{times_for_load = times_for_load -1;};"
                            +"}, 100);};"

                        +"});"
                );

                Log.d("zhiwei","page finish");

            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            final ViewGroup parent = (ViewGroup) webView.getParent();
            // 全屏
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                mLayout.addView(view);
                mLayout.setVisibility(View.VISIBLE);
                mLayout.bringToFront();
                parent.removeView(webView);
//                setFullScreen();
            }

            // 退出全屏
            @Override
            public void onHideCustomView() {
//                webView.bringToFront();
                parent.addView(webView);
                super.onHideCustomView();
                mLayout.removeAllViews();
                mLayout.setVisibility(View.GONE);
//                quitFullScreen();
            }

        });

    }


    // 下载的类
    private class MyWebViewDownLoadListener implements DownloadListener {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }


    // 接收广播的类
    private class receiver_main extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent){
            if (intent==null) return;
            String type = intent.getStringExtra("type");
            if (type==null) return;

            // 根据不同类型消息执行动作

            // 加载设置界面
            if (type.equals("reload_settings")){
                webView.loadUrl("file:///android_asset/Settings.html");
                Log.d("zhiwei","reload settings");}

            // 更改设置
            else if (type.equals("save_settings")){
                String data = intent.getStringExtra("data");
                Log.d("zhiwei",data);

                try {
                    JSONObject newsettings = new JSONObject(data);
                    for (int i=0;i<properties.length;i++){
                        settings_json.put(properties[i],newsettings.getString("p"+(i+1)));
                    }
                    save_file(file_for_setting,settings_json.toString(),"PRIVATE");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // 播放记录
            else if (type.equals("update_play_logs")){
                String data = intent.getStringExtra("data");
                String name = data.substring(2,data.indexOf(":")-1);
                String value= data.substring(data.indexOf(":")+2,data.indexOf("}")-1);
                save_file(file_for_playLog,data+"playLog","APPEND");
                try {
                    playLog_json.put(name,value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // 清空播放记录
            else if (type.equals("clear_logs")){
                playLog_json = new JSONObject();
                save_file(file_for_playLog,"","PRIVATE");
                Toast.makeText(getApplicationContext(),"清空了播放记录",Toast.LENGTH_SHORT).show();
            }

        }
    }


    //横竖屏切换监听
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        switch (config.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                break;
//            case Configuration.ORIENTATION_SQUARE:
//                break;
//            case Configuration.ORIENTATION_UNDEFINED:
//                break;
        }
    }

    @Override
    protected void onDestroy() {
//        save_file(file_for_setting,"","APPEND");
        super.onDestroy();
        webView.destroy();
        webView = null;
        unregisterReceiver(receiverMain);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()){
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    // 后面是一些自定义的函数，上面用到的

    // 屏蔽广告
    private boolean block_ad(Context context, String url, String[] adUrls) {
        for (String adUrl : adUrls) {
            if (url.contains(adUrl)) {
                return true;
            }
        }
        return false;
    }

    // 保存文件
    private void save_file(String filename, String content, String mode){
        FileOutputStream outstream=null;
        try {
            if (mode.equals("APPEND")){
                outstream = openFileOutput(filename,MODE_APPEND);}
            else {outstream = openFileOutput(filename,MODE_PRIVATE);} // 若不是追加则全部为重写
            outstream.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert outstream != null;
                outstream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 新的读取文件方式
    private String read_file(String filename) throws IOException {
        FileInputStream in = null;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            in = openFileInput(filename);
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content.toString();

    }

    // 字符串数组添加
    private String[] new_strs(String[] str1,String[] aim){
        String[] result=new String[str1.length+aim.length];
        System.arraycopy(str1, 0, result, 0, str1.length);
        System.arraycopy(aim, 0, result, str1.length, aim.length);
        return result;
    }

    // 拼接字符串数组，类似python的.join方法
    private String list_join(String a,String[] A) {
        String result="";
        if (A.length>1){
            for (int i=0;i<A.length-1;i++){
                result+=A[i]+a;
            }
        }
        result+=A[A.length-1];
        return result;
    }

}


// js交互的类
class js_interface {
    private final Context context;
    public js_interface(Context context){
        this.context=context;
    }
    private float time_log;

    // 设置界面
    @JavascriptInterface
    public void load_settings() {
        Intent intent = new Intent("broadcast of yhdm");
        intent.putExtra("type","reload_settings");
        context.sendBroadcast(intent);
    }

    // 保存设置
    @JavascriptInterface
    public void save_settings(String strings) {
        Intent intent = new Intent("broadcast of yhdm");
        intent.putExtra("type","save_settings");
        intent.putExtra("data",strings);
        context.sendBroadcast(intent);
    }

    // 记录视频播放时间
    @JavascriptInterface
    public void show_time_play(String msg) throws InterruptedException {
        Log.d("zhiwei","current time: "+msg);
        Intent intent = new Intent("broadcast of yhdm");
        intent.putExtra("type","update_play_logs");
        intent.putExtra("data",msg);
        context.sendBroadcast(intent);
    }

    // 记录视频播放时间
    @JavascriptInterface
    public void print(String msg) throws InterruptedException {
        Log.d("zhiwei","print: "+msg);
    }

    // 清空播放记录
    @JavascriptInterface
    public void clear_logs() {
        Log.d("zhiwei","clear logs");
        Intent intent = new Intent("broadcast of yhdm");
        intent.putExtra("type","clear_logs");
        context.sendBroadcast(intent);
    }

}