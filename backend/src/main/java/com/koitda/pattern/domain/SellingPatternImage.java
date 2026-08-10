package com.koitda.pattern.domain;

import com.koitda.file.domain.FileAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "selling_pattern_image")
public class SellingPatternImage {

	@Id
	private Long id;

	@Column(name = "pattern_id")
	private Long patternId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "file_id")
	private FileAsset file;

	@Column(name = "sort_order")
	private int sortOrder;

	@Column(name = "is_thumbnail")
	private boolean thumbnail;

	protected SellingPatternImage() {
	}

	public Long getPatternId() {
		return patternId;
	}

	public FileAsset getFile() {
		return file;
	}

	public boolean isThumbnail() {
		return thumbnail;
	}
}
