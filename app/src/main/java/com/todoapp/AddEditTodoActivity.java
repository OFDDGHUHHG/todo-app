package com.todoapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

/**
 * 添加/编辑待办事项界面
 */
public class AddEditTodoActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private TextView tvReminder;
    private RadioGroup rgPriority;
    private Button btnSave, btnCancel;

    private int todoId = -1;
    private long reminderTime = 0;
    private Calendar selectedCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        tvReminder = findViewById(R.id.tvReminder);
        rgPriority = findViewById(R.id.rgPriority);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        selectedCalendar = Calendar.getInstance();

        // 检查是否是编辑模式
        if (getIntent().hasExtra("id")) {
            todoId = getIntent().getIntExtra("id", -1);
            etTitle.setText(getIntent().getStringExtra("title"));
            etContent.setText(getIntent().getStringExtra("content"));
            reminderTime = getIntent().getLongExtra("reminder_time", 0);
            int priority = getIntent().getIntExtra("priority", 1);

            if (reminderTime > 0) {
                selectedCalendar.setTimeInMillis(reminderTime);
                updateReminderText();
            }

            // 设置优先级
            switch (priority) {
                case 3: rgPriority.check(R.id.rbHigh); break;
                case 2: rgPriority.check(R.id.rbMedium); break;
                default: rgPriority.check(R.id.rbLow); break;
            }
        }

        // 设置提醒时间
        tvReminder.setOnClickListener(v -> showDateTimePicker());

        // 保存按钮
        btnSave.setOnClickListener(v -> saveTodo());

        // 取消按钮
        btnCancel.setOnClickListener(v -> finish());
    }

    /**
     * 显示日期时间选择器
     */
    private void showDateTimePicker() {
        // 先选日期
        DatePickerDialog dateDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // 再选时间
                    TimePickerDialog timeDialog = new TimePickerDialog(this,
                            (view1, hourOfDay, minute) -> {
                                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedCalendar.set(Calendar.MINUTE, minute);
                                selectedCalendar.set(Calendar.SECOND, 0);
                                reminderTime = selectedCalendar.getTimeInMillis();
                                updateReminderText();
                            },
                            selectedCalendar.get(Calendar.HOUR_OF_DAY),
                            selectedCalendar.get(Calendar.MINUTE),
                            true);
                    timeDialog.show();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    /**
     * 更新提醒时间显示
     */
    private void updateReminderText() {
        if (reminderTime > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            tvReminder.setText("提醒: " + sdf.format(selectedCalendar.getTime()));
        } else {
            tvReminder.setText("点击设置提醒时间");
        }
    }

    /**
     * 保存待办事项
     */
    private void saveTodo() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        int priority = 1;
        int checkedId = rgPriority.getCheckedRadioButtonId();
        if (checkedId == R.id.rbHigh) priority = 3;
        else if (checkedId == R.id.rbMedium) priority = 2;

        TodoDatabaseHelper dbHelper = new TodoDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("reminder_time", reminderTime);
        values.put("priority", priority);
        values.put("completed", 0);

        if (todoId > 0) {
            // 更新
            db.update(TodoDatabaseHelper.TABLE_NAME, values, "id = ?", new String[]{String.valueOf(todoId)});
            MainActivity.cancelAlarm(this, todoId);
            MainActivity.setAlarm(this, todoId, reminderTime, title);
            Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
        } else {
            // 新增
            long newId = db.insert(TodoDatabaseHelper.TABLE_NAME, null, values);
            MainActivity.setAlarm(this, (int) newId, reminderTime, title);
            Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show();
        }
        db.close();
        finish();
    }
}