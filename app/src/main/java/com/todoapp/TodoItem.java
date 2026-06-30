package com.todoapp;

/**
 * 待办事项数据模型
 */
public class TodoItem {
    private int id;
    private String title;
    private String content;
    private boolean completed;
    private long reminderTime;
    private int priority;
    private long createdTime;

    public TodoItem() {}

    public TodoItem(String title, String content, long reminderTime, int priority) {
        this.title = title;
        this.content = content;
        this.reminderTime = reminderTime;
        this.priority = priority;
        this.completed = false;
        this.createdTime = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public long getReminderTime() { return reminderTime; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }

    public boolean hasReminder() {
        return reminderTime > 0;
    }
}