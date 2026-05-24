package com.example.demo.advice;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.controller.api.ScheduleEntryApiController;
import com.example.demo.controller.api.ScheduleSheetApiController;
import com.example.demo.entity.TaskType;
import com.example.demo.service.ScheduleQueryService;
import com.example.demo.service.ScheduleService;
import com.example.demo.service.ScheduleSheetService;
import com.example.demo.service.exception.ScheduleConflictException;
import com.example.demo.service.exception.ScheduleEntryNotFoundException;
import com.example.demo.service.exception.ScheduleSheetNotFoundException;

@WebMvcTest(controllers = {
        ScheduleSheetApiController.class,
        ScheduleEntryApiController.class
})
@Import(ApiExceptionHandler.class)
@DisplayName("ApiExceptionHandler")
class ApiExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ScheduleSheetService sheetService;

    @MockitoBean
    ScheduleQueryService queryService;

    @MockitoBean
    ScheduleService scheduleService;

    @Nested
    @DisplayName("404 Not Found")
    class NotFound {

        @Test
        @DisplayName("ScheduleSheetNotFoundException を 404 JSON に変換する")
        void handleScheduleSheetNotFound() throws Exception {
            String slug = "not-found";
            String message = "スケジュールシートが見つかりません: " + slug;

            when(queryService.getDetailBySlug(slug))
                    .thenThrow(new ScheduleSheetNotFoundException(message));

            mockMvc.perform(get("/api/schedule-sheets/{slug}", slug))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Schedule Sheet Not Found"))
                    .andExpect(jsonPath("$.message").value(message))
                    .andExpect(jsonPath("$.path").value("/api/schedule-sheets/" + slug));
        }

        @Test
        @DisplayName("ScheduleEntryNotFoundException を 404 JSON に変換する")
        void handleScheduleEntryNotFound() throws Exception {
            String slug = "study-plan";
            UUID entryId = UUID.randomUUID();
            String message = "予定が見つかりません: " + entryId;

            doThrow(new ScheduleEntryNotFoundException(message))
                    .when(scheduleService)
                    .delete(slug, entryId);

            mockMvc.perform(delete(
                            "/api/schedule-sheets/{slug}/entries/{entryId}",
                            slug,
                            entryId
                    ))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Schedule Entry Not Found"))
                    .andExpect(jsonPath("$.message").value(message))
                    .andExpect(jsonPath("$.path").value(
                            "/api/schedule-sheets/" + slug + "/entries/" + entryId
                    ));
        }
    }

    @Nested
    @DisplayName("409 Conflict")
    class Conflict {

        @Test
        @DisplayName("ScheduleConflictException を 409 JSON に変換する")
        void handleScheduleConflict() throws Exception {
            String slug = "study-plan";
            String message = "同じ種類の予定が重複しています";

            when(scheduleService.create(
                    eq(slug),
                    eq(TaskType.WORK),
                    eq(540),
                    eq(600),
                    eq("Java学習")
            )).thenThrow(new ScheduleConflictException(message));

            String requestBody = """
                    {
                      "type": "WORK",
                      "startMin": 540,
                      "endMin": 600,
                      "memo": "Java学習"
                    }
                    """;

            mockMvc.perform(post("/api/schedule-sheets/{slug}/entries", slug)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Schedule Conflict"))
                    .andExpect(jsonPath("$.message").value(message))
                    .andExpect(jsonPath("$.path").value(
                            "/api/schedule-sheets/" + slug + "/entries"
                    ));
        }
    }

    @Nested
    @DisplayName("400 Bad Request")
    class BadRequest {

        @Test
        @DisplayName("CreateScheduleSheetRequest のバリデーションエラーを 400 JSON に変換する")
        void handleScheduleSheetValidationError() throws Exception {
            String requestBody = """
                    {
                      "title": ""
                    }
                    """;

            mockMvc.perform(post("/api/schedule-sheets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.message").value("入力値が不正です"))
                    .andExpect(jsonPath("$.path").value("/api/schedule-sheets"))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[*].field")
                            .value(containsInAnyOrder("title")))
                    .andExpect(jsonPath("$.errors[*].message")
                            .value(containsInAnyOrder("タイトルは必須です")));
        }

        @Test
        @DisplayName("CreateScheduleEntryRequest の複数バリデーションエラーを 400 JSON に変換する")
        void handleScheduleEntryValidationError() throws Exception {
            String slug = "study-plan";

            String requestBody = """
                    {
                      "type": null,
                      "startMin": -1,
                      "endMin": 1441,
                      "memo": "%s"
                    }
                    """.formatted("a".repeat(501));

            mockMvc.perform(post("/api/schedule-sheets/{slug}/entries", slug)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.message").value("入力値が不正です"))
                    .andExpect(jsonPath("$.path").value(
                            "/api/schedule-sheets/" + slug + "/entries"
                    ))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                            "type",
                            "startMin",
                            "endMin",
                            "memo"
                    )))
                    .andExpect(jsonPath("$.errors[*].message").value(containsInAnyOrder(
                            "予定種別を選択してください",
                            "開始時刻は0以上で入力してください",
                            "終了時刻は1440以下で入力してください",
                            "メモは500文字以内で入力してください"
                    )));
        }

        @Test
        @DisplayName("IllegalArgumentException を 400 JSON に変換する")
        void handleIllegalArgumentException() throws Exception {
            String slug = "study-plan";
            String message = "開始時刻は終了時刻より前にしてください";

            when(scheduleService.create(
                    eq(slug),
                    eq(TaskType.WORK),
                    eq(600),
                    eq(540),
                    any()
            )).thenThrow(new IllegalArgumentException(message));

            String requestBody = """
                    {
                      "type": "WORK",
                      "startMin": 600,
                      "endMin": 540,
                      "memo": "不正な時間"
                    }
                    """;

            mockMvc.perform(post("/api/schedule-sheets/{slug}/entries", slug)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(message))
                    .andExpect(jsonPath("$.path").value(
                            "/api/schedule-sheets/" + slug + "/entries"
                    ));
        }
    }

    @Nested
    @DisplayName("500 Internal Server Error")
    class InternalServerError {

        @Test
        @DisplayName("想定外の例外を 500 JSON に変換する")
        void handleUnexpectedException() throws Exception {
            when(queryService.findAllSheets())
                    .thenThrow(new RuntimeException("DB接続失敗"));

            mockMvc.perform(get("/api/schedule-sheets"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("予期しないエラーが発生しました"))
                    .andExpect(jsonPath("$.path").value("/api/schedule-sheets"));
        }
    }
}