package com.todoapp;

import android.app.KeyguardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 闹钟提醒弹窗界面
 * 到点显示此界面，有铃声和振动
 */
public class AlertActivity extends AppCompatActivity {

    private TextView tvTitle;
    private Button btnDismiss, btnComplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 解锁屏幕并显示在锁屏之上
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // 设置全屏
        getWindow().setBackgroundDrawableResource(android.R.color.white);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        setContentView(R.layout.activity_alert);

        int id = getIntent().getIntExtra("id", 0);
        String title = getIntent().getStringExtra("title");

        tvTitle = findViewById(R.id.tvAlertTitle);
        btnDismiss = findViewById(R.id.btnDismiss);
        btnComplete = findViewById(R.id.btnComplete);

        tvTitle.setText(title);

        btnDismiss.setOnClickListener(v -> {
            AlarmReceiver.stopAlarmSound();
            finish();
        });

        btnComplete.setOnClickListener(v -> {
            // 标记为已完成
            markCompleted(id);
            AlarmReceiver.stopAlarmSound();
            MainActivity.cancelAlarm(this, id);
            finish();
        });
    }

    private void markCompleted(int id) {
        TodoDatabaseHelper dbHelper = new TodoDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completed", 1);
        db.update(TodoDatabaseHelper.TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    @Override
    protected void onDestroy() {
        AlarmReceiver.stopAlarmSound();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        AlarmReceiver.stopAlarmSound();
        super.onBackPressed();
    }
}