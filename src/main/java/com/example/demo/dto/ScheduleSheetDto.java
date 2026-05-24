package com.example.demo.dto;

import com.example.demo.entity.ScheduleSheet;

public record ScheduleSheetDto(
        String title,
        String slug
) {

    public static ScheduleSheetDto from(ScheduleSheet sheet) {
        return new ScheduleSheetDto(
                sheet.getTitle(),
                sheet.getSlug()
        );
    }
}