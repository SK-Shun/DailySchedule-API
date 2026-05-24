package com.example.demo.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.demo.entity.TaskType;

public record CreateScheduleEntryRequest(

        @NotNull(message = "予定種別を選択してください")
        TaskType type,

        @NotNull(message = "開始時刻は必須です")
        @Min(value = 0, message = "開始時刻は0以上で入力してください")
        @Max(value = 1439, message = "開始時刻は1439以下で入力してください")
        Integer startMin,

        @NotNull(message = "終了時刻は必須です")
        @Min(value = 1, message = "終了時刻は1以上で入力してください")
        @Max(value = 1440, message = "終了時刻は1440以下で入力してください")
        Integer endMin,

        @Size(max = 500, message = "メモは500文字以内で入力してください")
        String memo

) {
}