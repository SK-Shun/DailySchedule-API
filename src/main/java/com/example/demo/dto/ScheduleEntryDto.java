package com.example.demo.dto;

import java.util.UUID;

import com.example.demo.entity.ScheduleEntry;

public record ScheduleEntryDto(
        UUID id,
        String typeName,
        String typeLabel,
        int startMin,
        int endMin,
        String memo
) {

    public static ScheduleEntryDto from(ScheduleEntry entry) {
        return new ScheduleEntryDto(
                entry.getId(),
                entry.getType().name(),
                entry.getType().getLabel(),
                entry.getStartMin(),
                entry.getEndMin(),
                entry.getMemo()
        );
    }
}