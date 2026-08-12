package com.koitda.project.domain;

import com.koitda.file.domain.FileAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 니팅로그 대표 이미지(PROJECT-010, 최대 7개). 실체는 file_asset, 여기선 참조만. */
@Entity
@Table(name = "project_image")
public class ProjectImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "file_id")
	private FileAsset file;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected ProjectImage() {
	}

	public ProjectImage(Long projectId, FileAsset file, int sortOrder) {
		this.projectId = projectId;
		this.file = file;
		this.sortOrder = sortOrder;
	}

	public FileAsset getFile() {
		return file;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
