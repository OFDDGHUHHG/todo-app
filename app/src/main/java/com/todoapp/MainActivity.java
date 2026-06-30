package com.todoapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 待办事项主界面
 * 功能：显示列表、编辑删除、分类统计、备份导入、闹钟提醒
 * 作者：富二代好牛逼
 * 微信：L597551791
 * 日期：2025年1月
 */
public class MainActivity extends AppCompatActivity {

    private ListView lvTodos;
    private LinearLayout layoutTabs;
    private TextView tvTabAll, tvTabPending, tvTabCompleted;
    private TextView tvCountAll, tvCountPending, tvCountCompleted;
    private TextView tvCurrentFilter;
    private Button btnAdd;

    private TodoDatabaseHelper dbHelper;
    private TodoAdapter adapter;
    private List<TodoItem> allTodos = new ArrayList<>();
    private List<TodoItem> filteredTodos = new ArrayList<>();

    private int currentFilter = 0; // 0=全部, 1=未完成, 2=已完成
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private SimpleDateFormat backupDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    // 统计数据
    private int countAll = 0;
    private int countPending = 0;
    private int countCompleted = 0;

    private static final String CHANNEL_ID = "todo_reminder";
    private static final int REQUEST_ALARM_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        checkAlarmPermission();

        dbHelper = new TodoDatabaseHelper(this);

        initViews();
        setupTabs();
        loadTodos();
    }

    private void initViews() {
        lvTodos = findViewById(R.id.lvTodos);
        layoutTabs = findViewById(R.id.layoutTabs);
        tvTabAll = findViewById(R.id.tvTabAll);
        tvTabPending = findViewById(R.id.tvTabPending);
        tvTabCompleted = findViewById(R.id.tvTabCompleted);
        tvCountAll = findViewById(R.id.tvCountAll);
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountCompleted = findViewById(R.id.tvCountCompleted);
        tvCurrentFilter = findViewById(R.id.tvCurrentFilter);
        btnAdd = findViewById(R.id.btnAdd);

        adapter = new TodoAdapter(this, filteredTodos);
        lvTodos.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditTodoActivity.class);
            startActivity(intent);
        });

        // 点击列表项 - 编辑
        lvTodos.setOnItemClickListener((parent, view, position, id) -> {
            TodoItem item = filteredTodos.get(position);
            Intent intent = new Intent(this, AddEditTodoActivity.class);
            intent.putExtra("id", item.getId());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("content", item.getContent());
            intent.putExtra("reminder_time", item.getReminderTime());
            intent.putExtra("priority", item.getPriority());
            startActivity(intent);
        });

        // 长按列表项 - 删除/更多操作
        lvTodos.setOnItemLongClickListener((parent, view, position, id) -> {
            TodoItem item = filteredTodos.get(position);
            showItemMenu(view, item);
            return true;
        });
    }

    private void setupTabs() {
        tvTabAll.setOnClickListener(v -> {
            currentFilter = 0;
            applyFilter();
        });
        tvTabPending.setOnClickListener(v -> {
            currentFilter = 1;
            applyFilter();
        });
        tvTabCompleted.setOnClickListener(v -> {
            currentFilter = 2;
            applyFilter();
        });
    }

    private void applyFilter() {
        filteredTodos.clear();
        switch (currentFilter) {
            case 0:
                filteredTodos.addAll(allTodos);
                tvCurrentFilter.setText("全部待办");
                break;
            case 1:
                for (TodoItem item : allTodos) {
                    if (!item.isCompleted()) filteredTodos.add(item);
                }
                tvCurrentFilter.setText("未完成");
                break;
            case 2:
                for (TodoItem item : allTodos) {
                    if (item.isCompleted()) filteredTodos.add(item);
                }
                tvCurrentFilter.setText("已完成");
                break;
        }
        adapter.notifyDataSetChanged();
        updateTabStyles();
    }

    private void updateTabStyles() {
        // 重置样式
        tvTabAll.setTextColor(0xFF666666);
        tvTabPending.setTextColor(0xFF666666);
        tvTabCompleted.setTextColor(0xFF666666);

        // 高亮当前选中
        switch (currentFilter) {
            case 0: tvTabAll.setTextColor(0xFF2196F3); break;
            case 1: tvTabPending.setTextColor(0xFF2196F3); break;
            case 2: tvTabCompleted.setTextColor(0xFF2196F3); break;
        }
    }

    private void showItemMenu(View anchor, TodoItem item) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(Menu.NONE, 1, 1, "编辑");
        popup.getMenu().add(Menu.NONE, 2, 2, "删除");
        if (!item.isCompleted()) {
            popup.getMenu().add(Menu.NONE, 3, 3, "标记完成");
        } else {
            popup.getMenu().add(Menu.NONE, 3, 3, "取消完成");
        }

        popup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case 1: // 编辑
                    Intent intent = new Intent(this, AddEditTodoActivity.class);
                    intent.putExtra("id", item.getId());
                    intent.putExtra("title", item.getTitle());
                    intent.putExtra("content", item.getContent());
                    intent.putExtra("reminder_time", item.getReminderTime());
                    intent.putExtra("priority", item.getPriority());
                    startActivity(intent);
                    break;
                case 2: // 删除
                    showDeleteDialog(item);
                    break;
                case 3: // 完成/取消完成
                    toggleCompleted(item);
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void showDeleteDialog(TodoItem item) {
        new AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定删除「" + item.getTitle() + "」吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                deleteTodo(item);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void deleteTodo(TodoItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TodoDatabaseHelper.TABLE_NAME, "id = ?", new String[]{String.valueOf(item.getId())});
        db.close();

        // 取消闹钟
        cancelAlarm(this, item.getId());

        loadTodos();
        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
    }

    private void toggleCompleted(TodoItem item) {
        boolean newCompleted = !item.isCompleted();
        updateCompleted(item.getId(), newCompleted);

        if (newCompleted) {
            cancelAlarm(this, item.getId());
        } else {
            if (item.getReminderTime() > 0) {
                setAlarm(this, item.getId(), item.getReminderTime(), item.getTitle());
            }
        }

        loadTodos();
    }

    private void loadTodos() {
        allTodos.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TodoDatabaseHelper.TABLE_NAME,
            null, null, null, null, null, "completed ASC, priority DESC, reminder_time ASC");

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
            long reminderTime = cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time"));
            int priority = cursor.getInt(cursor.getColumnIndexOrThrow("priority"));
            boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed")) == 1;

            allTodos.add(new TodoItem(id, title, content, reminderTime, priority, completed));
        }
        cursor.close();
        db.close();

        // 更新统计
        countAll = allTodos.size();
        countPending = 0;
        countCompleted = 0;
        for (TodoItem item : allTodos) {
            if (item.isCompleted()) countCompleted++;
            else countPending++;
        }

        tvCountAll.setText("(" + countAll + ")");
        tvCountPending.setText("(" + countPending + ")");
        tvCountCompleted.setText("(" + countCompleted + ")");

        applyFilter();

        // 检查并恢复错过提醒的闹钟
        restoreMissedAlarms();
    }

    private void restoreMissedAlarms() {
        for (TodoItem item : allTodos) {
            if (!item.isCompleted() && item.getReminderTime() > 0) {
                if (item.getReminderTime() > System.currentTimeMillis()) {
                    setAlarm(this, item.getId(), item.getReminderTime(), item.getTitle());
                }
            }
        }
    }

    private void updateCompleted(int id, boolean completed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completed", completed ? 1 : 0);
        db.update(TodoDatabaseHelper.TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    /**
     * 设置闹钟提醒
     */
    public static void setAlarm(Context context, int id, long time, String title) {
        if (time <= 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("id", id);
        intent.putExtra("title", title);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "待办提醒", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("待办事项闹钟提醒");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodos();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_backup) {
            backupData();
            return true;
        } else if (id == R.id.action_import) {
            importData();
            return true;
        } else if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 备份数据到文件
     */
    private void backupData() {
        try {
            JSONArray jsonArray = new JSONArray();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(TodoDatabaseHelper.TABLE_NAME, null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                JSONObject obj = new JSONObject();
                obj.put("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                obj.put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")));
                obj.put("content", cursor.getString(cursor.getColumnIndexOrThrow("content")));
                obj.put("reminder_time", cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time")));
                obj.put("priority", cursor.getInt(cursor.getColumnIndexOrThrow("priority")));
                obj.put("completed", cursor.getInt(cursor.getColumnIndexOrThrow("completed")));
                jsonArray.put(obj);
            }
            cursor.close();
            db.close();

            // 生成文件名
            String fileName = "todo_backup_" + backupDateFormat.format(new Date()) + ".json";
            
            // 存储位置
            File backupDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用应用私有目录
                backupDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backup");
            } else {
                backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TodoBackup");
            }
            
            if (!backupDir.exists()) backupDir.mkdirs();
            File backupFile = new File(backupDir, fileName);

            // 写入文件
            FileOutputStream fos = new FileOutputStream(backupFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(jsonArray.toString());
            writer.close();
            fos.close();

            // 显示备份信息
            String message = "备份成功！\n\n文件名：" + fileName + "\n保存位置：" + backupFile.getAbsolutePath();
            new AlertDialog.Builder(this)
                .setTitle("备份完成")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .setNegativeButton("分享文件", (dialog, which) -> shareBackupFile(backupFile))
                .show();

        } catch (Exception e) {
            Toast.makeText(this, "备份失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareBackupFile(File file) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        Uri uri = Uri.fromFile(file);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(shareIntent, "分享备份文件"));
    }

    /**
     * 从文件导入数据
     */
    private void importData() {
        // 提示选择文件位置
        new AlertDialog.Builder(this)
            .setTitle("导入数据")
            .setMessage("请将备份文件放置在以下位置后点击导入：\n\n" +
                "Android 10+: Android/data/com.todoapp/files/Documents/backup/\n\n" +
                "旧版本: Documents/TodoBackup/\n\n" +
                "文件格式: todo_backup_*.json")
            .setPositiveButton("选择文件导入", (dialog, which) -> selectAndImportFile())
            .setNegativeButton("取消", null)
            .show();
    }

    private void selectAndImportFile() {
        // 尝试从默认位置读取最近的备份文件
        File backupDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backupDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backup");
        } else {
            backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TodoBackup");
        }

        if (!backupDir.exists() || backupDir.listFiles() == null || backupDir.listFiles().length == 0) {
            Toast.makeText(this, "未找到备份文件，请先放置备份文件到指定位置", Toast.LENGTH_LONG).show();
            return;
        }

        // 找到最新的备份文件
        File latestFile = null;
        for (File f : backupDir.listFiles()) {
            if (f.getName().startsWith("todo_backup_") && f.getName().endsWith(".json")) {
                if (latestFile == null || f.lastModified() > latestFile.lastModified()) {
                    latestFile = f;
                }
            }
        }

        if (latestFile == null) {
            Toast.makeText(this, "未找到有效的备份文件", Toast.LENGTH_LONG).show();
            return;
        }

        // 确认导入
        new AlertDialog.Builder(this)
            .setTitle("确认导入")
            .setMessage("找到备份文件：" + latestFile.getName() + "\n\n导入将覆盖现有数据，是否继续？")
            .setPositiveButton("导入", (dialog, which) -> doImportFile(latestFile))
            .setNegativeButton("取消", null)
            .show();
    }

    private void doImportFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray jsonArray = new JSONArray(sb.toString());

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // 清空现有数据
            db.delete(TodoDatabaseHelper.TABLE_NAME, null, null);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put("title", obj.getString("title"));
                values.put("content", obj.optString("content", ""));
                values.put("reminder_time", obj.optLong("reminder_time", 0));
                values.put("priority", obj.optInt("priority", 1));
                values.put("completed", obj.optInt("completed", 0));
                db.insert(TodoDatabaseHelper.TABLE_NAME, null, values);
            }
            db.close();

            loadTodos();

            // 重新设置闹钟
            for (TodoItem item : allTodos) {
                if (!item.isCompleted() && item.getReminderTime() > 0) {
                    if (item.getReminderTime() > System.currentTimeMillis()) {
                        setAlarm(this, item.getId(), item.getReminderTime(), item.getTitle());
                    }
                }
            }

            Toast.makeText(this, "导入成功！共导入 " + jsonArray.length() + " 条记录", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 显示关于页面
     */
    private void showAbout() {
        new AlertDialog.Builder(this)
            .setTitle("关于待办事项")
            .setMessage("待办事项APP v1.0\n\n" +
                "功能介绍：\n" +
                "• 添加/编辑/删除待办事项\n" +
                "• 设置提醒时间，闹钟提醒\n" +
                "• 优先级管理（高/中/低）\n" +
                "• 已完成/未完成分类显示\n" +
                "• 条数统计显示\n" +
                "• 数据备份与导入\n" +
                "• 完全本地存储，离线可用\n\n" +
                "作者：富二代好牛逼\n" +
                "微信：L597551791\n" +
                "日期：2025年1月")
            .setPositiveButton("确定", null)
            .show();
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
            TextView tvContent = convertView.findViewById(R.id.tvContent);
            TextView tvTime = convertView.findViewById(R.id.tvTime);
            CheckBox cbCompleted = convertView.findViewById(R.id.cbCompleted);
            View priorityBar = convertView.findViewById(R.id.priorityBar);
            ImageButton btnMore = convertView.findViewById(R.id.btnMore);

            tvTitle.setText(item.getTitle());
            tvTitle.setTextColor(item.isCompleted() ? 0xFF888888 : 0xFF333333);
            if (item.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // 显示内容
            if (item.getContent() != null && !item.getContent().isEmpty()) {
                tvContent.setText(item.getContent());
                tvContent.setVisibility(View.VISIBLE);
            } else {
                tvContent.setVisibility(View.GONE);
            }

            // 显示提醒时间
            if (item.getReminderTime() > 0) {
                String timeStr = dateFormat.format(new Date(item.getReminderTime()));
                if (item.getReminderTime() <= System.currentTimeMillis() && !item.isCompleted()) {
                    timeStr += " (已到期)";
                    tvTime.setTextColor(0xFFFF4444);
                } else {
                    tvTime.setTextColor(0xFF666666);
                }
                tvTime.setText(timeStr);
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

            // 完成状态复选框
            cbCompleted.setChecked(item.isCompleted());
            cbCompleted.setOnCheckedChangeListener(null); // 先清除监听器
            cbCompleted.setChecked(item.isCompleted()); // 重设值
            cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                toggleCompleted(item);
            });

            // 更多按钮
            btnMore.setOnClickListener(v -> showItemMenu(v, item));

            return convertView;
        }
    }
}