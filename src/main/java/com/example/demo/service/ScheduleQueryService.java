package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.ScheduleDetailDto;
import com.example.demo.dto.ScheduleSheetDto;

public interface ScheduleQueryService {

    List<ScheduleSheetDto> findAllSheets();

    ScheduleDetailDto getDetailBySlug(String slug);
}