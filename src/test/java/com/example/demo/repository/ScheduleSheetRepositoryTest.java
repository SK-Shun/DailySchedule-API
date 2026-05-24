package com.example.demo.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.demo.entity.ScheduleSheet;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScheduleSheetRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private ScheduleSheetRepository scheduleSheetRepository;
    
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("save: ScheduleSheetを保存できる")
    void save() {
        ScheduleSheet sheet = new ScheduleSheet("My Schedule", "my-schedule");

        ScheduleSheet saved = scheduleSheetRepository.saveAndFlush(sheet);

        entityManager.refresh(saved);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("My Schedule");
        assertThat(saved.getSlug()).isEqualTo("my-schedule");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findBySlug: slugで取得できる")
    void findBySlug() {
        scheduleSheetRepository.saveAndFlush(new ScheduleSheet("Study Plan", "study-plan"));

        Optional<ScheduleSheet> result = scheduleSheetRepository.findBySlug("study-plan");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Study Plan");
    }

    @Test
    @DisplayName("existsBySlug: 存在するslugならtrue")
    void existsBySlug_true() {
        scheduleSheetRepository.saveAndFlush(new ScheduleSheet("Work Plan", "work-plan"));

        assertThat(scheduleSheetRepository.existsBySlug("work-plan")).isTrue();
    }

    @Test
    @DisplayName("deleteBySlug: slug指定で削除できる")
    void deleteBySlug() {
        scheduleSheetRepository.saveAndFlush(new ScheduleSheet("Delete Target", "delete-target"));

        scheduleSheetRepository.deleteBySlug("delete-target");
        scheduleSheetRepository.flush();

        assertThat(scheduleSheetRepository.findBySlug("delete-target")).isEmpty();
    }

    @Test
    @DisplayName("slug重複は一意制約違反になる")
    void save_duplicateSlug_throwsException() {
        scheduleSheetRepository.saveAndFlush(new ScheduleSheet("Plan A", "duplicate-slug"));

        assertThatThrownBy(() ->
                scheduleSheetRepository.saveAndFlush(new ScheduleSheet("Plan B", "duplicate-slug")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}