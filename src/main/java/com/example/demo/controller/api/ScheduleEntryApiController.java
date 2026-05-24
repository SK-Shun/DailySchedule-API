package com.example.demo.controller.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.api.request.CreateScheduleEntryRequest;
import com.example.demo.api.response.ApiErrorResponse;
import com.example.demo.api.response.ValidationErrorResponse;
import com.example.demo.dto.ScheduleEntryDto;
import com.example.demo.service.ScheduleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/schedule-sheets/{slug}/entries")
@Tag(name = "Schedule Entry API", description = "スケジュール予定を操作するAPI")
public class ScheduleEntryApiController {

    private final ScheduleService scheduleService;

    public ScheduleEntryApiController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "予定作成",
            description = "指定したスケジュールシートに新しい予定を作成します。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "作成成功"),
            @ApiResponse(
                    responseCode = "400",
                    description = "バリデーションエラー",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "スケジュールシートが見つからない",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "同じ種類の予定が重複している",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ScheduleEntryDto create(
            @Parameter(description = "スケジュールシートのslug", example = "my-schedule")
            @PathVariable String slug,

            @Valid @RequestBody CreateScheduleEntryRequest request
    ) {
        return scheduleService.create(
                slug,
                request.type(),
                request.startMin(),
                request.endMin(),
                request.memo()
        );
    }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "予定削除",
            description = "指定したスケジュールシート内の予定を削除します。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "削除成功"),
            @ApiResponse(
                    responseCode = "404",
                    description = "予定が見つからない",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public void delete(
            @Parameter(description = "スケジュールシートのslug", example = "my-schedule")
            @PathVariable String slug,

            @Parameter(description = "予定ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID entryId
    ) {
        scheduleService.delete(slug, entryId);
    }
}