package com.koitda.pattern.service;

import com.koitda.pattern.dto.CategoryResponse;
import com.koitda.pattern.repository.PatternCategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

	private final PatternCategoryRepository categoryRepository;

	public CategoryService(PatternCategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> list() {
		return categoryRepository.findByActiveTrueOrderBySortOrderAscIdAsc()
				.stream().map(CategoryResponse::from).toList();
	}
}
