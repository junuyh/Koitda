package com.koitda.gauge.repository;

import com.koitda.gauge.domain.GaugeCalculation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GaugeCalculationRepository extends JpaRepository<GaugeCalculation, Long> {

	Optional<GaugeCalculation> findByProjectIdAndAppliedTrue(Long projectId);

	List<GaugeCalculation> findByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);

	/**
	 * 새 적용 전에 기존 적용을 해제한다(GAUGE-013: 적용 1건). 대상은 '기존 applied 행'이라 방금 조회한
	 * 새 계산 엔티티와 겹치지 않으므로 clearAutomatically 는 쓰지 않는다(쓰면 새 엔티티가 detach 되어 apply 유실).
	 */
	@Modifying(flushAutomatically = true)
	@Query("update GaugeCalculation g set g.applied = false where g.projectId = :projectId and g.applied = true")
	int unapplyAll(@Param("projectId") Long projectId);
}
