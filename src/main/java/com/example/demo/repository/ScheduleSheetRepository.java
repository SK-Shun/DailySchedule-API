package com.example.demo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.ScheduleSheet;

public interface ScheduleSheetRepository 
        extends JpaRepository<ScheduleSheet, UUID> {

    Optional<ScheduleSheet> findBySlug(String slug);

    boolean existsBySlug(String slug);
    
    void deleteBySlug(String slug);
}