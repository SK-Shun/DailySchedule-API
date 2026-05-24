package com.example.demo.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateScheduleSheetRequest(

        @NotBlank(message = "タイトルは必須です")
        @Size(max = 255, message = "タイトルは255文字以内で入力してください")
        String title

) {
}