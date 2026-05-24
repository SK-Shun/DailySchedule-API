package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.dto.ScheduleDetailDto;
import com.example.demo.dto.ScheduleEntryDto;
import com.example.demo.dto.ScheduleSheetDto;
import com.example.demo.entity.ScheduleEntry;
import com.example.demo.entity.ScheduleSheet;
import com.example.demo.entity.TaskType;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.ScheduleSheetRepository;
import com.example.demo.service.exception.ScheduleSheetNotFoundException;

class ScheduleQueryServiceImplTest {

    private ScheduleSheetRepository sheetRepository;
    private ScheduleRepository scheduleRepository;
    private ScheduleQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        sheetRepository = mock(ScheduleSheetRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        service = new ScheduleQueryServiceImpl(
                sheetRepository,
                scheduleRepository
        );
    }

    @Test
    void findAllSheets_returnsDtoList() {
        ScheduleSheet sheet = new ScheduleSheet("タイトル", "slug");

        when(sheetRepository.findAll())
                .thenReturn(List.of(sheet));

        List<ScheduleSheetDto> result = service.findAllSheets();

        assertThat(result).hasSize(1);

        assertThat(result.get(0).title())
                .isEqualTo("タイトル");

        assertThat(result.get(0).slug())
                .isEqualTo("slug");
    }

    @Test
    void getDetailBySlug_returnsDetailDto() {
        String slug = "test-slug";

        ScheduleSheet sheet =
                new ScheduleSheet("タイトル", slug);

        ScheduleEntry entry = new ScheduleEntry();
        entry.setSheet(sheet);
        entry.setType(TaskType.WORK);
        entry.setStartMin(60);
        entry.setEndMin(120);
        entry.setMemo("メモ");

        when(sheetRepository.findBySlug(slug))
                .thenReturn(Optional.of(sheet));

        when(scheduleRepository
                .findBySheet_SlugOrderByStartMinAsc(slug))
                .thenReturn(List.of(entry));

        ScheduleDetailDto result =
                service.getDetailBySlug(slug);

        assertThat(result.sheet().title())
                .isEqualTo("タイトル");

        assertThat(result.sheet().slug())
                .isEqualTo(slug);

        assertThat(result.entries())
                .hasSize(1);

        ScheduleEntryDto dto =
                result.entries().get(0);

        assertThat(dto.typeName())
                .isEqualTo("WORK");

        assertThat(dto.typeLabel())
                .isEqualTo("仕事");

        assertThat(dto.startMin())
                .isEqualTo(60);

        assertThat(dto.endMin())
                .isEqualTo(120);

        assertThat(dto.memo())
                .isEqualTo("メモ");
    }

    @Test
    void getDetailBySlug_whenNotFound_throwsException() {
        String slug = "not-found";

        when(sheetRepository.findBySlug(slug))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getDetailBySlug(slug)
        )
                .isInstanceOf(ScheduleSheetNotFoundException.class)
                .hasMessage(
                        "スケジュールシートが見つかりません: not-found"
                );

        verify(scheduleRepository, never())
                .findBySheet_SlugOrderByStartMinAsc(any());
    }
}