package com.koitda.social.repository;

import com.koitda.social.domain.Report;
import com.koitda.social.domain.TargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

	boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, TargetType targetType, Long targetId);

	/** 관리자 신고 상세: 한 대상에 접수된 신고들(최신순). */
	List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, Long targetId);
}
