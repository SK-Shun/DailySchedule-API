package com.example.demo.controller.api;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.advice.ApiExceptionHandler;
import com.example.demo.api.request.CreateScheduleSheetRequest;
import com.example.demo.dto.ScheduleDetailDto;
import com.example.demo.dto.ScheduleSheetDto;
import com.example.demo.service.ScheduleQueryService;
import com.example.demo.service.ScheduleSheetService;
import com.example.demo.service.exception.ScheduleSheetNotFoundException;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ScheduleSheetApiController.class)
@Import({
        ApiExceptionHandler.class,
        JacksonAutoConfiguration.class
})
class ScheduleSheetApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ScheduleSheetService sheetService;

    @MockitoBean
    ScheduleQueryService queryService;

    @Test
    void findAll_shouldReturnScheduleSheets() throws Exception {
        when(queryService.findAllSheets()).thenReturn(List.of(
                new ScheduleSheetDto("Study Plan", "study-plan"),
                new ScheduleSheetDto("Work Plan", "work-plan")
        ));

        mockMvc.perform(get("/api/schedule-sheets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Study Plan"))
                .andExpect(jsonPath("$[0].slug").value("study-plan"))
                .andExpect(jsonPath("$[1].title").value("Work Plan"))
                .andExpect(jsonPath("$[1].slug").value("work-plan"));
    }

    @Test
    void findBySlug_shouldReturnScheduleDetail() throws Exception {
        ScheduleDetailDto detail = new ScheduleDetailDto(
                new ScheduleSheetDto("Study Plan", "study-plan"),
                List.of()
        );

        when(queryService.getDetailBySlug("study-plan")).thenReturn(detail);

        mockMvc.perform(get("/api/schedule-sheets/{slug}", "study-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sheet.title").value("Study Plan"))
                .andExpect(jsonPath("$.sheet.slug").value("study-plan"))
                .andExpect(jsonPath("$.entries").isArray());
    }

    @Test
    void findBySlug_whenSheetNotFound_shouldReturn404() throws Exception {
        when(queryService.getDetailBySlug("not-found"))
                .thenThrow(new ScheduleSheetNotFoundException(
                        "スケジュールシートが見つかりません: not-found"
                ));

        mockMvc.perform(get("/api/schedule-sheets/{slug}", "not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Schedule Sheet Not Found"))
                .andExpect(jsonPath("$.message").value("スケジュールシートが見つかりません: not-found"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/not-found"));
    }

    @Test
    void create_shouldReturnCreatedScheduleSheet() throws Exception {
        CreateScheduleSheetRequest request =
                new CreateScheduleSheetRequest("Study Plan");

        when(sheetService.createSheet("Study Plan"))
                .thenReturn(new ScheduleSheetDto("Study Plan", "study-plan"));

        mockMvc.perform(post("/api/schedule-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Study Plan"))
                .andExpect(jsonPath("$.slug").value("study-plan"));

        verify(sheetService).createSheet("Study Plan");
    }

    @Test
    void create_whenTitleIsBlank_shouldReturn400() throws Exception {
        CreateScheduleSheetRequest request =
                new CreateScheduleSheetRequest("");

        mockMvc.perform(post("/api/schedule-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("入力値が不正です"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("タイトルは必須です")));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/schedule-sheets/{slug}", "study-plan"))
                .andExpect(status().isNoContent());

        verify(sheetService).deleteBySlug("study-plan");
    }

    @Test
    void delete_whenSheetNotFound_shouldReturn404() throws Exception {
        doThrow(new ScheduleSheetNotFoundException(
                "スケジュールシートが見つかりません: not-found"
        ))
                .when(sheetService)
                .deleteBySlug(anyString());

        mockMvc.perform(delete("/api/schedule-sheets/{slug}", "not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Schedule Sheet Not Found"))
                .andExpect(jsonPath("$.message").value("スケジュールシートが見つかりません: not-found"))
                .andExpect(jsonPath("$.path").value("/api/schedule-sheets/not-found"));
    }
}