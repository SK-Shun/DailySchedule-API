package com.example.demo.dto;

import java.util.List;

public record ScheduleDetailDto(
        ScheduleSheetDto sheet,
        List<ScheduleEntryDto> entries
) {
}