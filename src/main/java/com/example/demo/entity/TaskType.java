package com.example.demo.entity;

public enum TaskType {

    WORK("仕事"),
    HOBBY("趣味"),
    BREAK("休憩"),
    HOUSEWORK("家事"),
    LIFE("生活行動"),
    FREE("自由欄");

    private final String label;

    TaskType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}