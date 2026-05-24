package com.example.demo.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.entity.ScheduleEntry;
import com.example.demo.entity.ScheduleSheet;
import com.example.demo.entity.TaskType;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScheduleRepositoryTest {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSheetRepository scheduleSheetRepository;

    @Test
    @DisplayName("同一typeで時間が重複していればtrue")
    void exists_overlap_true() {
        ScheduleSheet sheet = createSheet("My Schedule", "my-schedule");

        createEntry(sheet, TaskType.WORK, 60, 120, "既存");

        boolean result =
                scheduleRepository.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                        sheet.getSlug(),
                        TaskType.WORK,
                        90,
                        30
                );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("同一typeでも重複していなければfalse")
    void exists_overlap_false() {
        ScheduleSheet sheet = createSheet("My Schedule", "my-schedule");

        createEntry(sheet, TaskType.WORK, 60, 120, "既存");

        boolean result =
                scheduleRepository.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                        sheet.getSlug(),
                        TaskType.WORK,
                        180,
                        120
                );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("typeが違えば重複していてもfalse")
    void exists_overlap_different_type() {
        ScheduleSheet sheet = createSheet("My Schedule", "my-schedule");

        createEntry(sheet, TaskType.WORK, 60, 120, "既存");

        boolean result =
                scheduleRepository.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                        sheet.getSlug(),
                        TaskType.HOBBY,
                        100,
                        80
                );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("対象sheetに属していればtrue")
    void existsByIdAndSheetSlug_true() {
        ScheduleSheet sheet = createSheet("Sheet", "sheet");

        ScheduleEntry entry = createEntry(sheet, TaskType.WORK, 60, 120, "test");

        boolean result =
                scheduleRepository.existsByIdAndSheet_Slug(entry.getId(), sheet.getSlug());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("別sheetならfalse")
    void existsByIdAndSheetSlug_false() {
        ScheduleSheet sheet1 = createSheet("Sheet1", "sheet-1");
        ScheduleSheet sheet2 = createSheet("Sheet2", "sheet-2");

        ScheduleEntry entry = createEntry(sheet1, TaskType.WORK, 60, 120, "test");

        boolean result =
                scheduleRepository.existsByIdAndSheet_Slug(entry.getId(), sheet2.getSlug());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("指定ID+slugで削除できる")
    void deleteByIdAndSheetSlug() {
        ScheduleSheet sheet = createSheet("Sheet", "sheet");

        ScheduleEntry entry = createEntry(sheet, TaskType.WORK, 60, 120, "delete");

        scheduleRepository.deleteByIdAndSheet_Slug(entry.getId(), sheet.getSlug());
        scheduleRepository.flush();

        assertThat(scheduleRepository.existsById(entry.getId())).isFalse();
    }

    @Test
    @DisplayName("startMin昇順で取得できる")
    void findBySheetSlugOrderByStartMinAsc() {
        ScheduleSheet sheet = createSheet("Sheet", "sheet");

        ScheduleEntry e1 = createEntry(sheet, TaskType.WORK, 120, 180, "2番目");
        ScheduleEntry e2 = createEntry(sheet, TaskType.HOBBY, 60, 100, "1番目");
        ScheduleEntry e3 = createEntry(sheet, TaskType.BREAK, 200, 240, "3番目");

        List<ScheduleEntry> result =
                scheduleRepository.findBySheet_SlugOrderByStartMinAsc(sheet.getSlug());

        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(ScheduleEntry::getId)
                .containsExactly(e2.getId(), e1.getId(), e3.getId());
    }

    @Test
    @DisplayName("該当データがなければ空リスト")
    void findBySheetSlug_empty() {
        ScheduleSheet sheet = createSheet("Sheet", "sheet");

        List<ScheduleEntry> result =
                scheduleRepository.findBySheet_SlugOrderByStartMinAsc(sheet.getSlug());

        assertThat(result).isEmpty();
    }

    private ScheduleSheet createSheet(String title, String slugBase) {
        String uniqueSlug = slugBase + "-" + UUID.randomUUID();

        ScheduleSheet sheet = new ScheduleSheet(title, uniqueSlug);
        return scheduleSheetRepository.saveAndFlush(sheet);
    }

    private ScheduleEntry createEntry(
            ScheduleSheet sheet,
            TaskType type,
            int startMin,
            int endMin,
            String memo
    ) {
        ScheduleEntry entry = new ScheduleEntry();
        entry.setSheet(sheet);
        entry.setType(type);
        entry.setStartMin(startMin);
        entry.setEndMin(endMin);
        entry.setMemo(memo);

        return scheduleRepository.saveAndFlush(entry);
    }
}