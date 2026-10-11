package com.timetable.backend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableRepository
        extends JpaRepository<Timetable, Long> {
}