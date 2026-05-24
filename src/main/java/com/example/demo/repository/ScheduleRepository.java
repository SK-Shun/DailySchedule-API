package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.ScheduleEntry;
import com.example.demo.entity.TaskType;

public interface ScheduleRepository extends JpaRepository<ScheduleEntry, UUID> {

    boolean existsBySheet_SlugAndTypeAndStartMinLessThanAndEndMinGreaterThan(
            String slug,
            TaskType type,
            int endMin,
            int startMin
    );
    
    boolean existsByIdAndSheet_Slug(UUID id, String sheetSlug);

    void deleteByIdAndSheet_Slug(UUID id, String sheetSlug);

    List<ScheduleEntry> findBySheet_SlugOrderByStartMinAsc(String slug);
}