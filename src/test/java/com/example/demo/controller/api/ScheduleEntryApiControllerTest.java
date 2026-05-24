package com.example.demo.controller.api;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.advice.ApiExceptionHandler;
import com.example.demo.api.request.CreateScheduleEntryRequest;
import com.example.demo.dto.ScheduleEntryDto;
import com.example.demo.entity.TaskType;
import com.example.demo.service.ScheduleService;
import com.example.demo.service.exception.ScheduleConflictException;
import com.example.demo.service.exception.ScheduleEntryNotFoundException;
import com.example.demo.service.exception.ScheduleSheetNotFoundException;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ScheduleEntryApiController.class)
@Import({
        ApiExceptionHandler.class,
        JacksonAutoConfiguration.class
})
class ScheduleEntryApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ScheduleService scheduleService;

    @Test
    void create_shouldReturnCreatedEntry() throws Exception {
        UUID entryId = UUID.randomUUID();

        CreateScheduleEntryRequest request =
                new CreateScheduleEntryRequest(
                        TaskType.WORK,
                        540,
                        600,
                        "数学の勉強"
                );

        when(scheduleService.create(
                "study-plan",
                TaskType.WORK,
                540,
                600,
                "数学の勉強"
        )).thenReturn(
                new ScheduleEntryDto(
                        entryId,
                        "WORK",
                        "仕事",
                        540,
                        600,
                        "数学の勉強"
                )
        );

        mockMvc.perform(post("/api/schedule-sheets/{slug}/entries", "study-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(entryId.toString()))
                .andExpect(jsonPath("$.typeName").value("WORK"))
                .andExpect(jsonPath("$.typeLabel").value("仕事"))
                .andExpect(jsonPath("$.startMin").value(540))
                .andExpect(jsonPath("$.endMin").value(600))
                .andExpect(jsonPath("$.memo").value("数学の勉強"));

        verify(scheduleService).create(
                "study-plan",
                TaskType.WORK,
                540,
                600,
                "数学の勉強"
        );
    }

    @Test
    void create_whenRequestIsInvalid_shouldReturn400() throws Exception {
        CreateScheduleEntryRequest request =
                new CreateScheduleEntryRequest(
                        null,
                        -1,
                        1441,
                        "a".repeat(501)
                );

        mockMvc.perform(post("/api/schedule-sheets/{slug}/entries", "study-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("入力値が不正です"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/study-plan/entries"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("type")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("startMin")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("endMin")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("memo")));
    }

    @Test
    void create_whenSheetNotFound_shouldReturn404() throws Exception {
        CreateScheduleEntryRequest request =
                new CreateScheduleEntryRequest(
                        TaskType.WORK,
                        540,
                        600,
                        "数学の勉強"
                );

        when(scheduleService.create(
                "not-found",
                TaskType.WORK,
                540,
                600,
                "数学の勉強"
        )).thenThrow(new ScheduleSheetNotFoundException(
                "スケジュールシートが見つかりません: not-found"
        ));

        mockMvc.perform(post("/api/schedule-sheets/{slug}/entries", "not-found")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Schedule Sheet Not Found"))
                .andExpect(jsonPath("$.message").value("スケジュールシートが見つかりません: not-found"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/not-found/entries"));
    }

    @Test
    void create_whenScheduleConflict_shouldReturn409() throws Exception {
        CreateScheduleEntryRequest request =
                new CreateScheduleEntryRequest(
                        TaskType.WORK,
                        540,
                        600,
                        "数学の勉強"
                );

        when(scheduleService.create(
                "study-plan",
                TaskType.WORK,
                540,
                600,
                "数学の勉強"
        )).thenThrow(new ScheduleConflictException(
                "同じ種類の予定が重複しています"
        ));

        mockMvc.perform(post("/api/schedule-sheets/{slug}/entries", "study-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Schedule Conflict"))
                .andExpect(jsonPath("$.message").value("同じ種類の予定が重複しています"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/study-plan/entries"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        UUID entryId = UUID.randomUUID();

        mockMvc.perform(delete(
                        "/api/schedule-sheets/{slug}/entries/{entryId}",
                        "study-plan",
                        entryId
                ))
                .andExpect(status().isNoContent());

        verify(scheduleService).delete("study-plan", entryId);
    }

    @Test
    void delete_whenEntryNotFound_shouldReturn404() throws Exception {
        UUID entryId = UUID.randomUUID();

        doThrow(new ScheduleEntryNotFoundException(
                "予定が見つかりません: " + entryId
        ))
                .when(scheduleService)
                .delete("study-plan", entryId);

        mockMvc.perform(delete(
                        "/api/schedule-sheets/{slug}/entries/{entryId}",
                        "study-plan",
                        entryId
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Schedule Entry Not Found"))
                .andExpect(jsonPath("$.message").value("予定が見つかりません: " + entryId))
                .andExpect(jsonPath("$.path").value(
                        "/api/schedule-sheets/study-plan/entries/" + entryId
                ));
    }
}