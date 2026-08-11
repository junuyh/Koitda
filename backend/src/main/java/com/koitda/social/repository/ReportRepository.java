package com.koitda.social.repository;

import com.koitda.social.domain.Report;
import com.koitda.social.domain.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

	boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, TargetType targetType, Long targetId);
}
