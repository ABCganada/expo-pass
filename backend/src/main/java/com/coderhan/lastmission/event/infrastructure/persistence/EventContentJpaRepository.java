package com.coderhan.lastmission.event.infrastructure.persistence;

import com.coderhan.lastmission.event.application.EventContentRepository;
import com.coderhan.lastmission.event.domain.EventContent;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventContentJpaRepository extends JpaRepository<EventContent, Long>, EventContentRepository {
}