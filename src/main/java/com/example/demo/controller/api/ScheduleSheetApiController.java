package com.example.demo.controller.api;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.api.request.CreateScheduleSheetRequest;
import com.example.demo.api.response.ApiErrorResponse;
import com.example.demo.api.response.ValidationErrorResponse;
import com.example.demo.dto.ScheduleDetailDto;
import com.example.demo.dto.ScheduleSheetDto;
import com.example.demo.service.ScheduleQueryService;
import com.example.demo.service.ScheduleSheetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/schedule-sheets")
@Tag(name = "Schedule Sheet API", description = "スケジュールシートを操作するAPI")
public class ScheduleSheetApiController {

    private final ScheduleSheetService sheetService;
    private final ScheduleQueryService queryService;

    public ScheduleSheetApiController(
            ScheduleSheetService sheetService,
            ScheduleQueryService queryService
    ) {
        this.sheetService = sheetService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "スケジュールシート一覧取得")
    @ApiResponse(responseCode = "200", description = "取得成功")
    public List<ScheduleSheetDto> findAll() {
        return queryService.findAllSheets();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "スケジュールシート詳細取得")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取得成功"),
            @ApiResponse(
                    responseCode = "404",
                    description = "スケジュールシートが見つからない",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ScheduleDetailDto findBySlug(
            @Parameter(description = "スケジュールシートのslug", example = "my-schedule")
            @PathVariable String slug
    ) {
        return queryService.getDetailBySlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "スケジュールシート作成")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "作成成功"),
            @ApiResponse(
                    responseCode = "400",
                    description = "バリデーションエラー",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            )
    })
    public ScheduleSheetDto create(
            @Valid @RequestBody CreateScheduleSheetRequest request
    ) {
        return sheetService.createSheet(request.title());
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "スケジュールシート削除")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "削除成功"),
            @ApiResponse(
                    responseCode = "404",
                    description = "スケジュールシートが見つからない",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public void delete(
            @Parameter(description = "削除するスケジュールシートのslug", example = "my-schedule")
            @PathVariable String slug
    ) {
        sheetService.deleteBySlug(slug);
    }
}