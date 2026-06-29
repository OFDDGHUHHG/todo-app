package com.todoapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 待办事项主界面
 * 功能：显示待办列表、添加、编辑、删除、设置提醒
 */
public class MainActivity extends AppCompatActivity {

    private TodoDatabaseHelper dbHelper;
    private ListView listView;
    private TodoAdapter adapter;
    private List<TodoItem> todoList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new TodoDatabaseHelper(this);
        listView = findViewById(R.id.listView);
        todoList = new ArrayList<>();

        // 加载待办事项
        loadTodos();

        // 添加按钮
        Button btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditTodoActivity.class);
            startActivity(intent);
        });

        // 点击编辑
        listView.setOnItemClickListener((parent, view, position, id) -> {
            TodoItem item = todoList.get(position);
            Intent intent = new Intent(MainActivity.this, AddEditTodoActivity.class);
            intent.putExtra("id", item.getId());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("content", item.getContent());
            intent.putExtra("reminder_time", item.getReminderTime());
            intent.putExtra("priority", item.getPriority());
            startActivity(intent);
        });

        // 长按删除
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            TodoItem item = todoList.get(position);
            showDeleteDialog(item);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodos();
    }

    /**
     * 从数据库加载待办事项
     */
    private void loadTodos() {
        todoList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TodoDatabaseHelper.TABLE_NAME,
                null, null, null, null, null,
                "priority DESC, reminder_time ASC");

        while (cursor.moveToNext()) {
            TodoItem item = new TodoItem();
            item.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            item.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
            item.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("completed")) == 1);
            item.setReminderTime(cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time")));
            item.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow("priority")));
            todoList.add(item);
        }
        cursor.close();
        db.close();

        if (adapter == null) {
            adapter = new TodoAdapter(this, todoList);
            listView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteDialog(TodoItem item) {
        new AlertDialog.Builder(this)
                .setTitle("删除待办")
                .setMessage("确定删除 \"" + item.getTitle() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteTodo(item);
                    cancelAlarm(this, item.getId());
                    loadTodos();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除待办事项
     */
    private void deleteTodo(TodoItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TodoDatabaseHelper.TABLE_NAME, "id = ?", new String[]{String.valueOf(item.getId())});
        db.close();
    }

    /**
     * 更新完成状态
     */
    private void updateCompleted(int id, boolean completed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completed", completed ? 1 : 0);
        db.update(TodoDatabaseHelper.TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    /**
     * 设置闹钟提醒
     */
    public static void setAlarm(Context context, int id, long reminderTime, String title) {
        if (reminderTime <= 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("id", id);
        intent.putExtra("title", title);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 使用精确闹钟
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
    }

    /**
     * 取消闹钟
     */
    public static void cancelAlarm(Context context, int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    /**
     * 自定义适配器
     */
    class TodoAdapter extends ArrayAdapter<TodoItem> {
        public TodoAdapter(Context context, List<TodoItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_todo, parent, false);
            }

            TodoItem item = getItem(position);

            TextView tvTitle = convertView.findViewById(R.id.tvTitle);
            TextView tvTime = convertView.findViewById(R.id.tvTime);
            CheckBox cbCompleted = convertView.findViewById(R.id.cbCompleted);
            View priorityBar = convertView.findViewById(R.id.priorityBar);

            tvTitle.setText(item.getTitle());
            tvTitle.setTextColor(item.isCompleted() ? 0xFF888888 : 0xFF333333);

            if (item.getReminderTime() > 0) {
                tvTime.setText(dateFormat.format(new Date(item.getReminderTime())));
                tvTime.setVisibility(View.VISIBLE);
            } else {
                tvTime.setVisibility(View.GONE);
            }

            // 优先级颜色
            int priorityColor;
            switch (item.getPriority()) {
                case 3: priorityColor = 0xFFFF4444; break; // 高 - 红色
                case 2: priorityColor = 0xFFffaa00; break; // 中 - 橙色
                default: priorityColor = 0xFF44aa44; break; // 低 - 绿色
            }
            priorityBar.setBackgroundColor(priorityColor);

            cbCompleted.setChecked(item.isCompleted());
            cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateCompleted(item.getId(), isChecked);
                if (!isChecked) {
                    // 重新设置闹钟
                    setAlarm(getContext(), item.getId(), item.getReminderTime(), item.getTitle());
                } else {
                    // 取消闹钟
                    cancelAlarm(getContext(), item.getId());
                }
                loadTodos();
            });

            return convertView;
        }
    }
}