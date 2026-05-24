package com.example.demo.service.exception;

public class ScheduleSheetNotFoundException extends RuntimeException {

    public ScheduleSheetNotFoundException(String message) {
        super(message);
    }
}