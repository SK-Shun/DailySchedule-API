package com.example.demo.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ScheduleEntryDto;
import com.example.demo.entity.ScheduleEntry;
import com.example.demo.entity.ScheduleSheet;
import com.example.demo.entity.TaskType;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.service.exception.ScheduleConflictException;
import com.example.demo.service.exception.ScheduleEntryNotFoundException;

@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository repo;
    private final ScheduleSheetService sheetService;

    public ScheduleServiceImpl(ScheduleRepository repo, ScheduleSheetService sheetService) {
        this.repo = repo;
        this.sheetService = sheetService;
    }

    @Override
    public ScheduleEntryDto create(String sheetSlug, TaskType type, int startMin, int endMin, String memo) {
        validateType(type);
        validateRange(startMin, endMin);

        ScheduleSheet sheet = sheetService.findBySlug(sheetSlug);

        boolean conflicted = repo.existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
                sheetSlug,
                type,
                endMin,
                startMin
        );

        if (conflicted) {
            throw new ScheduleConflictException("同じ種類の予定が重複しています");
        }

        ScheduleEntry entry = new ScheduleEntry();
        entry.setSheet(sheet);
        entry.setType(type);
        entry.setStartMin(startMin);
        entry.setEndMin(endMin);
        entry.setMemo(normalizeMemo(memo));

        ScheduleEntry savedEntry = repo.save(entry);

        return ScheduleEntryDto.from(savedEntry);
    }

    @Override
    public void delete(String sheetSlug, UUID entryId) {
        if (!repo.existsByIdAndSheet_Slug(entryId, sheetSlug)) {
            throw new ScheduleEntryNotFoundException("対象の予定が存在しません");
        }

        repo.deleteByIdAndSheet_Slug(entryId, sheetSlug);
    }

    private void validateType(TaskType type) {
        if (type == null) {
            throw new IllegalArgumentException("予定種別は必須です");
        }
    }

    private void validateRange(int startMin, int endMin) {
        if (startMin < 0 || startMin > 1440 || endMin < 0 || endMin > 1440) {
            throw new IllegalArgumentException("時間は 0〜1440 分の範囲で指定してください");
        }

        if (startMin >= endMin) {
            throw new IllegalArgumentException("開始は終了より前にしてください");
        }
    }

    private String normalizeMemo(String memo) {
        return memo == null ? "" : memo.trim();
    }
}