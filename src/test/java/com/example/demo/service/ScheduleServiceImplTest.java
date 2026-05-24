package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dto.ScheduleEntryDto;
import com.example.demo.entity.ScheduleEntry;
import com.example.demo.entity.ScheduleSheet;
import com.example.demo.entity.TaskType;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.service.exception.ScheduleConflictException;
import com.example.demo.service.exception.ScheduleEntryNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private ScheduleRepository repo;

    @Mock
    private ScheduleSheetService sheetService;

    @InjectMocks
    private ScheduleServiceImpl service;

    @Test
    @DisplayName("正常系：予定を作成できる")
    void create_shouldCreateSchedule_whenValidInput() {
        String slug = "my-schedule";
        ScheduleSheet sheet = new ScheduleSheet("My Schedule", slug);

        when(sheetService.findBySlug(slug)).thenReturn(sheet);
        when(repo.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                slug, TaskType.WORK, 600, 540
        )).thenReturn(false);
        when(repo.save(any(ScheduleEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleEntryDto result = service.create(
                slug, TaskType.WORK, 540, 600, "  勉強する  "
        );

        assertThat(result.typeName()).isEqualTo("WORK");
        assertThat(result.typeLabel()).isEqualTo("仕事");
        assertThat(result.startMin()).isEqualTo(540);
        assertThat(result.endMin()).isEqualTo(600);
        assertThat(result.memo()).isEqualTo("勉強する");

        verify(repo).save(any());
    }

    @Test
    @DisplayName("正常系：memoがトリムされる")
    void create_shouldTrimMemo_whenMemoHasWhitespace() {
        String slug = "test";
        ScheduleSheet sheet = new ScheduleSheet("Test", slug);

        when(sheetService.findBySlug(slug)).thenReturn(sheet);
        when(repo.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                slug, TaskType.HOBBY, 700, 600
        )).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ScheduleEntryDto result = service.create(
                slug, TaskType.HOBBY, 600, 700, "  遊ぶ  "
        );

        assertThat(result.memo()).isEqualTo("遊ぶ");
    }

    @Test
    @DisplayName("正常系：memoがnullの場合は空文字になる")
    void create_shouldSetEmptyMemo_whenMemoIsNull() {
        String slug = "test";
        ScheduleSheet sheet = new ScheduleSheet("Test", slug);

        when(sheetService.findBySlug(slug)).thenReturn(sheet);
        when(repo.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                slug, TaskType.LIFE, 800, 700
        )).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ScheduleEntryDto result = service.create(
                slug, TaskType.LIFE, 700, 800, null
        );

        assertThat(result.memo()).isEqualTo("");
    }

    @Test
    @DisplayName("異常系：typeがnullなら例外")
    void create_shouldThrowException_whenTypeIsNull() {
        assertThatThrownBy(() ->
                service.create("test", null, 100, 200, "memo")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("予定種別は必須です");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("異常系：startMinが範囲外なら例外")
    void create_shouldThrowException_whenStartMinIsOutOfRange() {
        assertThatThrownBy(() ->
                service.create("test", TaskType.WORK, -1, 200, "memo")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("時間は 0〜1440 分の範囲で指定してください");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("異常系：endMinが範囲外なら例外")
    void create_shouldThrowException_whenEndMinIsOutOfRange() {
        assertThatThrownBy(() ->
                service.create("test", TaskType.WORK, 100, 1500, "memo")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("時間は 0〜1440 分の範囲で指定してください");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("異常系：startMin >= endMinなら例外")
    void create_shouldThrowException_whenStartMinIsAfterOrEqualToEndMin() {
        assertThatThrownBy(() ->
                service.create("test", TaskType.WORK, 300, 300, "memo")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("開始は終了より前にしてください");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("異常系：同じ種類の予定が重複していたら例外")
    void create_shouldThrowException_whenOverlapExistsForSameType() {
        String slug = "test";
        ScheduleSheet sheet = new ScheduleSheet("Test", slug);

        when(sheetService.findBySlug(slug)).thenReturn(sheet);
        when(repo.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                slug, TaskType.WORK, 600, 500
        )).thenReturn(true);

        assertThatThrownBy(() ->
                service.create(slug, TaskType.WORK, 500, 600, "memo")
        )
                .isInstanceOf(ScheduleConflictException.class)
                .hasMessage("同じ種類の予定が重複しています");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("内部検証：saveに渡されるエンティティが正しい")
    void create_shouldPassCorrectEntityToRepository_whenSaving() {
        String slug = "test";
        ScheduleSheet sheet = new ScheduleSheet("Test", slug);

        when(sheetService.findBySlug(slug)).thenReturn(sheet);
        when(repo.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                slug, TaskType.BREAK, 800, 700
        )).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(slug, TaskType.BREAK, 700, 800, "  休憩  ");

        ArgumentCaptor<ScheduleEntry> captor =
                ArgumentCaptor.forClass(ScheduleEntry.class);
        verify(repo).save(captor.capture());

        ScheduleEntry saved = captor.getValue();

        assertThat(saved.getSheet()).isSameAs(sheet);
        assertThat(saved.getType()).isEqualTo(TaskType.BREAK);
        assertThat(saved.getStartMin()).isEqualTo(700);
        assertThat(saved.getEndMin()).isEqualTo(800);
        assertThat(saved.getMemo()).isEqualTo("休憩");
    }

    @Test
    @DisplayName("正常系：予定を削除できる")
    void delete_shouldDeleteEntry_whenEntryExists() {
        String slug = "test";
        UUID id = UUID.randomUUID();

        when(repo.existsByIdAndSheet_Slug(id, slug)).thenReturn(true);

        service.delete(slug, id);

        verify(repo).deleteByIdAndSheet_Slug(id, slug);
    }

    @Test
    @DisplayName("異常系：存在しない予定なら例外")
    void delete_shouldThrowException_whenEntryDoesNotExist() {
        String slug = "test";
        UUID id = UUID.randomUUID();

        when(repo.existsByIdAndSheet_Slug(id, slug)).thenReturn(false);

        assertThatThrownBy(() ->
                service.delete(slug, id)
        )
                .isInstanceOf(ScheduleEntryNotFoundException.class)
                .hasMessage("対象の予定が存在しません");

        verify(repo, never()).deleteByIdAndSheet_Slug(any(), any());
    }
}