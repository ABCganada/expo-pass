package com.coderhan.lastmission.event.infrastructure.persistence;

import com.coderhan.lastmission.event.application.EventBookmarkRepository;
import com.coderhan.lastmission.event.domain.EventBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventBookmarkJpaRepository extends JpaRepository<EventBookmark, Long>, EventBookmarkRepository {
}