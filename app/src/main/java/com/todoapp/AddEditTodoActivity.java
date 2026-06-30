package com.todoapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;

/**
 * 添加/编辑待办事项页面
 */
public class AddEditTodoActivity extends Activity {

    private EditText etTitle;
    private EditText etContent;
    private CheckBox cbReminder;
    private TextView tvReminderTime;
    private Button btnSave;
    private Button btnDelete;
    
    private TodoDatabaseHelper dbHelper;
    private int todoId = -1; // -1表示新建，其他表示编辑
    private long reminderTime = 0;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        dbHelper = new TodoDatabaseHelper(this);

        // 初始化视图
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        cbReminder = findViewById(R.id.cb_reminder);
        tvReminderTime = findViewById(R.id.tv_reminder_time);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);

        // 检查是否是编辑模式
        Intent intent = getIntent();
        todoId = intent.getIntExtra("todo_id", -1);
        isEditMode = todoId != -1;

        // 设置页面标题
        TextView tvTitle = findViewById(R.id.tv_page_title);
        if (isEditMode) {
            tvTitle.setText("编辑待办事项");
            btnDelete.setVisibility(Button.VISIBLE);
            // 加载待办事项数据
            loadTodoData();
        } else {
            tvTitle.setText("新建待办事项");
            btnDelete.setVisibility(Button.GONE);
        }

        // 设置提醒时间选择
        cbReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showDateTimePicker();
            } else {
                reminderTime = 0;
                tvReminderTime.setText("未设置");
            }
        });

        // 保存按钮
        btnSave.setOnClickListener(v -> saveTodo());

        // 删除按钮
        btnDelete.setOnClickListener(v -> deleteTodo());

        // 取消按钮
        Button btnCancel = findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadTodoData() {
        TodoItem item = dbHelper.getTodoById(todoId);
        if (item != null) {
            etTitle.setText(item.getTitle());
            etContent.setText(item.getContent() != null ? item.getContent() : "");
            reminderTime = item.getReminderTime();
            if (reminderTime > 0) {
                cbReminder.setChecked(true);
                tvReminderTime.setText(formatTime(reminderTime));
            }
        }
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        
        // 先选择日期
        DatePickerDialog dateDialog = new DatePickerDialog(this,
            (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                
                // 然后选择时间
                TimePickerDialog timeDialog = new TimePickerDialog(this,
                    (view1, hourOfDay, minute) -> {
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDate.set(Calendar.MINUTE, minute);
                        selectedDate.set(Calendar.SECOND, 0);
                        
                        reminderTime = selectedDate.getTimeInMillis();
                        tvReminderTime.setText(formatTime(reminderTime));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
                timeDialog.show();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private String formatTime(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return String.format("%d-%02d-%02d %02d:%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE));
    }

    private void saveTodo() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoItem item = new TodoItem();
        item.setTitle(title);
        item.setContent(content);
        item.setReminderTime(cbReminder.isChecked() ? reminderTime : 0);
        item.setPriority(1);

        if (isEditMode) {
            // 更新
            item.setId(todoId);
            item.setCompleted(dbHelper.getTodoById(todoId).isCompleted());
            dbHelper.updateTodo(item);
            
            // 更新闹钟
            if (cbReminder.isChecked() && reminderTime > 0) {
                setAlarm(item);
            } else {
                cancelAlarm(todoId);
            }
            
            Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
        } else {
            // 新建
            long newId = dbHelper.addTodo(item);
            item.setId((int) newId);
            
            // 设置闹钟
            if (cbReminder.isChecked() && reminderTime > 0) {
                setAlarm(item);
            }
            
            Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void deleteTodo() {
        if (todoId != -1) {
            // 取消闹钟
            cancelAlarm(todoId);
            
            // 删除数据库记录
            dbHelper.deleteTodo(todoId);
            
            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setAlarm(TodoItem item) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("todo_id", item.getId());
        intent.putExtra("title", item.getTitle());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 
            item.getId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 使用setAlarmClock确保闹钟在精确时间触发
        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
            reminderTime,
            pendingIntent
        );
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
    }

    private void cancelAlarm(int todoId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }
}