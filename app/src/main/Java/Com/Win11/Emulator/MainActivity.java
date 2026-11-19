package com.win11.emulator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تمام صفحه کردن محیط ویندوز ۱۱
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        requestStoragePermission();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // اتصال پل جاوا اسکریپت با نام Win11Native
        webView.addJavascriptInterface(new Win11Bridge(), "Win11Native");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    public class Win11Bridge {

        // ۱. خواندن فایل‌ها و پوشه‌های واقعی گوشی برای نمایش در File Explorer ویندوز ۱۱
        @JavascriptInterface
        public String listFiles(String path) {
            JSONArray result = new JSONArray();
            try {
                File dir = (path == null || path.isEmpty()) 
                        ? Environment.getExternalStorageDirectory() 
                        : new File(path);

                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        JSONObject item = new JSONObject();
                        item.put("name", file.getName());
                        item.put("path", file.getAbsolutePath());
                        item.put("isDir", file.isDirectory());
                        item.put("size", file.length());
                        result.put(item);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result.toString();
        }

        // ۲. اجرای فایل واقعی .exe با Box64 + Wine و باز کردن صفحه نمایش X11
        @JavascriptInterface
        public void runExe(String exePath) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                "در حال اجرای: " + new File(exePath).getName(), Toast.LENGTH_SHORT).show());

            new Thread(() -> {
                try {
                    String filesDir = getFilesDir().getAbsolutePath();
                    String prefixDir = filesDir + "/prefix";
                    String wineBin = filesDir + "/usr/bin/wine";
                    String box64Bin = filesDir + "/usr/bin/box64";

                    // باز کردن اپلیکیشن Termux-X11 برای نمایش پنجره گرافیکی ویندوز
                    Intent x11Intent = getPackageManager().getLaunchIntentForPackage("com.termux.x11");
                    if (x11Intent != null) {
                        x11Intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(x11Intent);
                    }

                    // تنظیم متغیرهای محیطی برای شتاب‌دهنده گرافیکی Vulkan/DXVK و Wine
                    ProcessBuilder pb = new ProcessBuilder(box64Bin, wineBin, exePath);
                    Map<String, String> env = pb.environment();
                    env.put("DISPLAY", ":0");
                    env.put("WINEPREFIX", prefixDir);
                    env.put("BOX64_DYNAREC", "1");
                    env.put("WINEDEBUG", "-all");
                    env.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");
                    env.put("TU_DEBUG", "noconform"); // فعال‌سازی درایور Turnip برای Adreno

                    pb.directory(new File(exePath).getParentFile());
                    pb.start();

                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                        "خطا در اجرای موتور Box64/Wine: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }

        // ۳. باز کردن مستقیم صفحه نمایش گرافیکی X11
        @JavascriptInterface
        public void openX11Display() {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.termux.x11");
            if (intent != null) {
                startActivity(intent);
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                    "ابتدا برنامه Termux:X11 را نصب کنید", Toast.LENGTH_LONG).show());
            }
        }
    }
}
