package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dto.ScheduleSheetDto;
import com.example.demo.entity.ScheduleSheet;
import com.example.demo.repository.ScheduleSheetRepository;
import com.example.demo.service.exception.ScheduleSheetNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScheduleSheetServiceImplTest {

    @Mock
    private ScheduleSheetRepository repo;

    @InjectMocks
    private ScheduleSheetServiceImpl service;

    @Test
    void createSheet_shouldTrimTitleAndGenerateSlug() {
        when(repo.existsBySlug("my-schedule")).thenReturn(false);
        when(repo.save(any(ScheduleSheet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleSheetDto result = service.createSheet("  My Schedule  ");

        assertThat(result.title()).isEqualTo("My Schedule");
        assertThat(result.slug()).isEqualTo("my-schedule");

        ArgumentCaptor<ScheduleSheet> captor =
                ArgumentCaptor.forClass(ScheduleSheet.class);
        verify(repo).save(captor.capture());

        ScheduleSheet saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("My Schedule");
        assertThat(saved.getSlug()).isEqualTo("my-schedule");
    }

    @Test
    void createSheet_shouldAppendNumberWhenSlugAlreadyExists() {
        when(repo.existsBySlug("my-schedule")).thenReturn(true);
        when(repo.existsBySlug("my-schedule-1")).thenReturn(true);
        when(repo.existsBySlug("my-schedule-2")).thenReturn(false);
        when(repo.save(any(ScheduleSheet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleSheetDto result = service.createSheet("My Schedule");

        assertThat(result.slug()).isEqualTo("my-schedule-2");
    }

    @Test
    void createSheet_shouldFallbackToDefaultSlugWhenNormalizedIsBlank() {
        when(repo.existsBySlug("sheet")).thenReturn(false);
        when(repo.save(any(ScheduleSheet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleSheetDto result = service.createSheet("あいうえお");

        assertThat(result.title()).isEqualTo("あいうえお");
        assertThat(result.slug()).isEqualTo("sheet");
    }

    @Test
    void createSheet_shouldThrowExceptionWhenTitleIsNull() {
        assertThatThrownBy(() -> service.createSheet(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("タイトルは必須です");

        verify(repo, never()).save(any());
    }

    @Test
    void createSheet_shouldThrowExceptionWhenTitleIsBlank() {
        assertThatThrownBy(() -> service.createSheet("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("タイトルは必須です");

        verify(repo, never()).save(any());
    }

    @Test
    void findBySlug_shouldReturnSheetWhenExists() {
        ScheduleSheet sheet = new ScheduleSheet("My Schedule", "my-schedule");

        when(repo.findBySlug("my-schedule")).thenReturn(Optional.of(sheet));

        ScheduleSheet result = service.findBySlug("my-schedule");

        assertThat(result).isSameAs(sheet);
    }

    @Test
    void findBySlug_shouldThrowExceptionWhenNotFound() {
        when(repo.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findBySlug("missing"))
                .isInstanceOf(ScheduleSheetNotFoundException.class)
                .hasMessage("スケジュールシートが見つかりません: missing");
    }

    @Test
    void deleteBySlug_shouldDeleteWhenExists() {
        when(repo.existsBySlug("my-schedule")).thenReturn(true);

        service.deleteBySlug("my-schedule");

        verify(repo).deleteBySlug("my-schedule");
    }

    @Test
    void deleteBySlug_shouldThrowExceptionWhenNotFound() {
        when(repo.existsBySlug("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteBySlug("missing"))
                .isInstanceOf(ScheduleSheetNotFoundException.class)
                .hasMessage("スケジュールシートが見つかりません: missing");

        verify(repo, never()).deleteBySlug(any());
    }
}