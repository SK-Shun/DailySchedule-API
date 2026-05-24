package com.example.demo.service.exception;

public class ScheduleEntryNotFoundException extends RuntimeException {

    public ScheduleEntryNotFoundException(String message) {
        super(message);
    }
}