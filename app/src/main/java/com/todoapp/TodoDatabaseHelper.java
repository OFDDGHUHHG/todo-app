package com.todoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库帮助类
 * 使用SQLite存储待办事项，支持备份和导入
 */
public class TodoDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "todo.db";
    public static final int DATABASE_VERSION = 2;
    public static final String TABLE_NAME = "todos";

    public TodoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "content TEXT, " +
                "completed INTEGER DEFAULT 0, " +
                "reminder_time INTEGER DEFAULT 0, " +
                "priority INTEGER DEFAULT 1, " +
                "created_time INTEGER DEFAULT (strftime('%s', 'now') * 1000)" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 添加待办事项
    public long addTodo(TodoItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", item.getTitle());
        values.put("content", item.getContent() != null ? item.getContent() : "");
        values.put("completed", item.isCompleted() ? 1 : 0);
        values.put("reminder_time", item.getReminderTime());
        values.put("priority", item.getPriority());
        return db.insert(TABLE_NAME, null, values);
    }
    
    // 添加待办事项（便捷方法）
    public long addTodo(String title, String content, long reminderTime) {
        TodoItem item = new TodoItem(title, content, reminderTime, 0);
        return addTodo(item);
    }

    // 更新待办事项
    public int updateTodo(TodoItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", item.getTitle());
        values.put("content", item.getContent() != null ? item.getContent() : "");
        values.put("completed", item.isCompleted() ? 1 : 0);
        values.put("reminder_time", item.getReminderTime());
        values.put("priority", item.getPriority());
        return db.update(TABLE_NAME, values, "id = ?", 
                new String[]{String.valueOf(item.getId())});
    }
    
    // 更新待办事项（便捷方法）
    public int updateTodo(int id, String title, String content, long reminderTime) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content != null ? content : "");
        values.put("reminder_time", reminderTime);
        return db.update(TABLE_NAME, values, "id = ?", 
                new String[]{String.valueOf(id)});
    }

    // 删除待办事项
    public int deleteTodo(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME, "id = ?", 
                new String[]{String.valueOf(id)});
    }

    // 获取所有待办事项
    public List<TodoItem> getAllTodos() {
        List<TodoItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null,
                "completed ASC, created_time DESC");
        while (cursor.moveToNext()) {
            TodoItem item = new TodoItem();
            item.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            item.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
            item.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("completed")) == 1);
            item.setReminderTime(cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time")));
            item.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow("priority")));
            item.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow("created_time")));
            list.add(item);
        }
        cursor.close();
        return list;
    }

    // 获取未完成的待办事项
    public List<TodoItem> getUncompletedTodos() {
        List<TodoItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "completed = 0", null, null, null,
                "created_time DESC");
        while (cursor.moveToNext()) {
            TodoItem item = new TodoItem();
            item.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            item.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
            item.setCompleted(false);
            item.setReminderTime(cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time")));
            item.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow("priority")));
            item.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow("created_time")));
            list.add(item);
        }
        cursor.close();
        return list;
    }

    // 获取已完成的待办事项
    public List<TodoItem> getCompletedTodos() {
        List<TodoItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "completed = 1", null, null, null,
                "created_time DESC");
        while (cursor.moveToNext()) {
            TodoItem item = new TodoItem();
            item.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            item.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
            item.setCompleted(true);
            item.setReminderTime(cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time")));
            item.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow("priority")));
            item.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow("created_time")));
            list.add(item);
        }
        cursor.close();
        return list;
    }

    // 获取待办事项数量统计
    public int getTotalCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getUncompletedCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE completed = 0", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getCompletedCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE completed = 1", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // 标记完成/取消完成
    public void toggleCompleted(int id, boolean completed) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completed", completed ? 1 : 0);
        db.update(TABLE_NAME, values, "id = ?", 
                new String[]{String.valueOf(id)});
    }

    // 获取单个待办事项
    public TodoItem getTodoById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "id = ?", 
                new String[]{String.valueOf(id)}, null, null, null);
        TodoItem item = null;
        if (cursor.moveToFirst()) {
            item = new TodoItem();
            item.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            item.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
            item.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("completed")) == 1);
            item.setReminderTime(cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time")));
            item.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow("priority")));
            item.setCreatedTime(cursor.getLong(cursor.getColumnIndexOrThrow("created_time")));
        }
        cursor.close();
        return item;
    }

    // 导出所有数据为JSON字符串
    public String exportToJson() {
        JSONArray jsonArray = new JSONArray();
        List<TodoItem> todos = getAllTodos();
        for (TodoItem item : todos) {
            try {
                JSONObject json = new JSONObject();
                json.put("id", item.getId());
                json.put("title", item.getTitle());
                json.put("content", item.getContent());
                json.put("completed", item.isCompleted());
                json.put("reminder_time", item.getReminderTime());
                json.put("priority", item.getPriority());
                json.put("created_time", item.getCreatedTime());
                jsonArray.put(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jsonArray.toString();
    }

    // 从JSON导入数据
    public int importFromJson(String jsonString) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            SQLiteDatabase db = getWritableDatabase();
            // 清空现有数据
            db.delete(TABLE_NAME, null, null);
            
            int count = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put("title", json.getString("title"));
                values.put("content", json.optString("content", ""));
                values.put("completed", json.optBoolean("completed", false) ? 1 : 0);
                values.put("reminder_time", json.optLong("reminder_time", 0));
                values.put("priority", json.optInt("priority", 1));
                values.put("created_time", json.optLong("created_time", System.currentTimeMillis()));
                db.insert(TABLE_NAME, null, values);
                count++;
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}