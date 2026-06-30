package com.todoapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // 设置当前日期
        TextView tvDate = findViewById(R.id.tvAboutDate);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
        tvDate.setText("发布日期：" + sdf.format(new Date()));

        // 返回按钮
        findViewById(R.id.btnBackAbout).setOnClickListener(v -> finish());
    }
}