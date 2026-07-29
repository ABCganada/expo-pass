package com.coderhan.lastmission.event.infrastructure.persistence;

import com.coderhan.lastmission.event.application.EventCategoryRepository;
import com.coderhan.lastmission.event.domain.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventCategoryJpaRepository extends JpaRepository<EventCategory, Long>, EventCategoryRepository {
}