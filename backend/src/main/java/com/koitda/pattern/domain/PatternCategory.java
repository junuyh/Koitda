package com.koitda.pattern.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 카테고리(자기참조 계층). 권장 실측 키(JSONB)는 등록 화면 슬라이스에서 매핑한다. */
@Entity
@Table(name = "pattern_category")
public class PatternCategory {

	@Id
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "parent_id")
	private Long parentId;

	@Column(name = "sort_order")
	private int sortOrder;

	@Column(name = "is_active")
	private boolean active;

	protected PatternCategory() {
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Long getParentId() {
		return parentId;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}
}
