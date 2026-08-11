package com.koitda.pattern.domain;

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

@Entity
@Table(name = "selling_pattern_image")
public class SellingPatternImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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

	/** 도안 등록 시 이미지 행 생성. file 은 서비스에서 참조 프록시로 넘긴다. */
	public static SellingPatternImage create(Long patternId, FileAsset file, int sortOrder, boolean thumbnail) {
		SellingPatternImage img = new SellingPatternImage();
		img.patternId = patternId;
		img.file = file;
		img.sortOrder = sortOrder;
		img.thumbnail = thumbnail;
		return img;
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
