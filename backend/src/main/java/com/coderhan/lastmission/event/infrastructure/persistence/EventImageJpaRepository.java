package com.coderhan.lastmission.event.infrastructure.persistence;

import com.coderhan.lastmission.event.application.EventImageRepository;
import com.coderhan.lastmission.event.domain.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventImageJpaRepository extends JpaRepository<EventImage, Long>, EventImageRepository {
}