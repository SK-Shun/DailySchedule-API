package com.example.demo.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.api.request.CreateScheduleEntryRequest;
import com.example.demo.api.request.CreateScheduleSheetRequest;
import com.example.demo.entity.TaskType;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(JacksonAutoConfiguration.class)
@Transactional
class ScheduleApiFlowIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("スケジュールシート作成 → 予定作成 → 詳細取得 → 予定削除 → シート削除まで成功する")
    void scheduleApiFlow_success() throws Exception {
        CreateScheduleSheetRequest sheetRequest =
                new CreateScheduleSheetRequest("Study Plan");

        mockMvc.perform(post("/api/schedule-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sheetRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Study Plan"))
                .andExpect(jsonPath("$.slug").value("study-plan"));

        mockMvc.perform(get("/api/schedule-sheets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Study Plan"))
                .andExpect(jsonPath("$[0].slug").value("study-plan"));

        CreateScheduleEntryRequest entryRequest =
                new CreateScheduleEntryRequest(
                        TaskType.WORK,
                        540,
                        600,
                        "英語の勉強"
                );

        String entryResponseJson = mockMvc.perform(post(
                                "/api/schedule-sheets/{slug}/entries",
                                "study-plan"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.typeName").value("WORK"))
                .andExpect(jsonPath("$.typeLabel").value("仕事"))
                .andExpect(jsonPath("$.startMin").value(540))
                .andExpect(jsonPath("$.endMin").value(600))
                .andExpect(jsonPath("$.memo").value("英語の勉強"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String entryId = objectMapper.readTree(entryResponseJson)
                .get("id")
                .asString();

        mockMvc.perform(get("/api/schedule-sheets/{slug}", "study-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sheet.title").value("Study Plan"))
                .andExpect(jsonPath("$.sheet.slug").value("study-plan"))
                .andExpect(jsonPath("$.entries", hasSize(1)))
                .andExpect(jsonPath("$.entries[0].id").value(entryId))
                .andExpect(jsonPath("$.entries[0].typeName").value("WORK"))
                .andExpect(jsonPath("$.entries[0].typeLabel").value("仕事"))
                .andExpect(jsonPath("$.entries[0].startMin").value(540))
                .andExpect(jsonPath("$.entries[0].endMin").value(600))
                .andExpect(jsonPath("$.entries[0].memo").value("英語の勉強"));

        mockMvc.perform(delete(
                        "/api/schedule-sheets/{slug}/entries/{entryId}",
                        "study-plan",
                        entryId
                ))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedule-sheets/{slug}", "study-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries", hasSize(0)));

        mockMvc.perform(delete("/api/schedule-sheets/{slug}", "study-plan"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedule-sheets/{slug}", "study-plan"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Schedule Sheet Not Found"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/study-plan"));
    }

    @Test
    @DisplayName("存在しないシートに予定を作成すると404になる")
    void createEntry_whenSheetNotFound_shouldReturn404() throws Exception {
        CreateScheduleEntryRequest request =
                new CreateScheduleEntryRequest(
                        TaskType.WORK,
                        540,
                        600,
                        "存在しないシートへの登録"
                );

        mockMvc.perform(post(
                                "/api/schedule-sheets/{slug}/entries",
                                "not-found"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Schedule Sheet Not Found"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/not-found/entries"));
    }

    @Test
    @DisplayName("不正な予定作成リクエストは400になる")
    void createEntry_whenRequestIsInvalid_shouldReturn400() throws Exception {
        CreateScheduleSheetRequest sheetRequest =
                new CreateScheduleSheetRequest("Validation Test");

        mockMvc.perform(post("/api/schedule-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sheetRequest)))
                .andExpect(status().isCreated());

        String invalidJson = """
                {
                  "type": null,
                  "startMin": -1,
                  "endMin": 1441,
                  "memo": "%s"
                }
                """.formatted("a".repeat(501));

        mockMvc.perform(post(
                                "/api/schedule-sheets/{slug}/entries",
                                "validation-test"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("入力値が不正です"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/validation-test/entries"))
                .andExpect(jsonPath("$.errors", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(4)));
    }

    @Test
    @DisplayName("同じ種類の予定で時間が重複すると409になる")
    void createEntry_whenScheduleConflicts_shouldReturn409() throws Exception {
        CreateScheduleSheetRequest sheetRequest =
                new CreateScheduleSheetRequest("Conflict Test");

        mockMvc.perform(post("/api/schedule-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sheetRequest)))
                .andExpect(status().isCreated());

        CreateScheduleEntryRequest first =
                new CreateScheduleEntryRequest(
                        TaskType.WORK,
                        540,
                        600,
                        "1つ目"
                );

        CreateScheduleEntryRequest second =
                new CreateScheduleEntryRequest(
                        TaskType.WORK,
                        570,
                        630,
                        "重複"
                );

        mockMvc.perform(post(
                                "/api/schedule-sheets/{slug}/entries",
                                "conflict-test"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(
                                "/api/schedule-sheets/{slug}/entries",
                                "conflict-test"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Schedule Conflict"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/conflict-test/entries"));
    }
}