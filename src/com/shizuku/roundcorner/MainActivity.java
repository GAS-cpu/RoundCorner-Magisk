package com.shizuku.roundcorner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** 启动入口：点一次图标即可启动圆角服务，并激活应用（使开机自启生效） */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, CornerService.class));
        Toast.makeText(this, "屏幕圆角已启用", Toast.LENGTH_SHORT).show();
        finish();
    }
}
