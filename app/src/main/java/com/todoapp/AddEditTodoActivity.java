package com.todoapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 添加/编辑待办事项页面
 */
public class AddEditTodoActivity extends Activity {
    
    private EditText etTitle, etNote;
    private Button btnSave, btnPickTime, btnDelete;
    private ImageButton btnBack;
    private TextView tvTitleBar, tvSelectedTime;
    
    private TodoDatabaseHelper dbHelper;
    private Calendar selectedCalendar;
    private long reminderTime = 0;
    
    // 编辑模式
    private int todoId = -1;
    private boolean isEditMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);
        
        dbHelper = new TodoDatabaseHelper(this);
        
        // 初始化控件
        initViews();
        
        // 检查是否是编辑模式
        Intent intent = getIntent();
        todoId = intent.getIntExtra("todo_id", -1);
        isEditMode = todoId != -1;
        
        if (isEditMode) {
            loadTodoData();
            tvTitleBar.setText("编辑待办事项");
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvTitleBar.setText("添加待办事项");
            btnDelete.setVisibility(View.GONE);
        }
        
        // 设置监听
        setupListeners();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitleBar = findViewById(R.id.tvTitle);
        btnSave = findViewById(R.id.btnSave);
        etTitle = findViewById(R.id.etTodoTitle);
        etNote = findViewById(R.id.etTodoNote);
        btnPickTime = findViewById(R.id.btnPickTime);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        btnDelete = findViewById(R.id.btnDelete);
    }
    
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> finish());
        
        // 选择时间
        btnPickTime.setOnClickListener(v -> showDateTimePicker());
        
        // 保存按钮
        btnSave.setOnClickListener(v -> saveTodo());
        
        // 删除按钮
        btnDelete.setOnClickListener(v -> deleteTodo());
    }
    
    private void loadTodoData() {
        TodoItem item = dbHelper.getTodoById(todoId);
        if (item != null) {
            etTitle.setText(item.getTitle());
            etNote.setText(item.getContent());
            reminderTime = item.getReminderTime();
            if (reminderTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA);
                tvSelectedTime.setText("提醒时间: " + sdf.format(new Date(reminderTime)));
                tvSelectedTime.setVisibility(View.VISIBLE);
                selectedCalendar = Calendar.getInstance();
                selectedCalendar.setTimeInMillis(reminderTime);
            }
        }
    }
    
    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        
        // 先选择日期
        DatePickerDialog dateDialog = new DatePickerDialog(this,
            (view, year, month, dayOfMonth) -> {
                if (selectedCalendar == null) {
                    selectedCalendar = Calendar.getInstance();
                }
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                
                // 再选择时间
                showTimePicker();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));
        
        dateDialog.show();
    }
    
    private void showTimePicker() {
        Calendar calendar = selectedCalendar != null ? selectedCalendar : Calendar.getInstance();
        
        TimePickerDialog timeDialog = new TimePickerDialog(this,
            (view, hourOfDay, minute) -> {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedCalendar.set(Calendar.MINUTE, minute);
                selectedCalendar.set(Calendar.SECOND, 0);
                
                reminderTime = selectedCalendar.getTimeInMillis();
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA);
                tvSelectedTime.setText("提醒时间: " + sdf.format(new Date(reminderTime)));
                tvSelectedTime.setVisibility(View.VISIBLE);
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true);
        
        timeDialog.show();
    }
    
    private void saveTodo() {
        String title = etTitle.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入待办内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isEditMode) {
            // 更新
            dbHelper.updateTodo(todoId, title, note, reminderTime);
            
            // 更新闹钟
            if (reminderTime > 0) {
                setAlarm(todoId, title, reminderTime);
            } else {
                cancelAlarm(todoId);
            }
            
            Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
        } else {
            // 新增
            long newId = dbHelper.addTodo(title, note, reminderTime);
            
            // 设置闹钟
            if (reminderTime > 0) {
                setAlarm(newId, title, reminderTime);
            }
            
            Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show();
        }
        
        finish();
    }
    
    private void deleteTodo() {
        dbHelper.deleteTodo(todoId);
        cancelAlarm(todoId);
        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    /**
     * 设置闹钟提醒
     */
    private void setAlarm(long todoId, String title, long time) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("todo_id", todoId);
        intent.putExtra("todo_title", title);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, (int)todoId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // 使用精确闹钟
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }
    
    /**
     * 取消闹钟
     */
    private void cancelAlarm(int todoId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, todoId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }
}