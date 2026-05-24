package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "schedule_entry")
public class ScheduleEntry {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sheet_id", nullable = false)
    private ScheduleSheet sheet;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TaskType type;

    @Column(name = "start_min", nullable = false)
    private int startMin;

    @Column(name = "end_min", nullable = false)
    private int endMin;

    @Column(name = "memo", nullable = false)
    private String memo = "";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (memo == null) {
            memo = "";
        }
    }

    public UUID getId() { return id; }

    public ScheduleSheet getSheet() { return sheet; }
    public void setSheet(ScheduleSheet sheet) { this.sheet = sheet; }

    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }

    public int getStartMin() { return startMin; }
    public void setStartMin(int startMin) { this.startMin = startMin; }

    public int getEndMin() { return endMin; }
    public void setEndMin(int endMin) { this.endMin = endMin; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}