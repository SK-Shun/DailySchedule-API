package com.example.demo.service;

import com.example.demo.dto.ScheduleSheetDto;
import com.example.demo.entity.ScheduleSheet;

public interface ScheduleSheetService {

    ScheduleSheetDto createSheet(String title);

    void deleteBySlug(String slug);

    ScheduleSheet findBySlug(String slug);
}