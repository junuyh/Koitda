package com.koitda.pattern.repository;

import com.koitda.pattern.domain.PatternCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternCategoryRepository extends JpaRepository<PatternCategory, Long> {

	List<PatternCategory> findByActiveTrueOrderBySortOrderAscIdAsc();
}
