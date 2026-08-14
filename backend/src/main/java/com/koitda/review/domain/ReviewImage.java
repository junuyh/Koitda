package com.koitda.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 리뷰 이미지(REVIEW). 로그에서 리뷰를 만들 때 니팅로그 대표사진을 '복사'해 붙인다 —
 * 파일 실체가 아니라 file_id 만 공유(원본 로그를 지워도 리뷰 사진은 유지).
 */
@Entity
@Table(name = "review_image")
public class ReviewImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "review_id", nullable = false)
	private Long reviewId;

	@Column(name = "file_id", nullable = false)
	private Long fileId;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected ReviewImage() {
	}

	public ReviewImage(Long reviewId, Long fileId, int sortOrder) {
		this.reviewId = reviewId;
		this.fileId = fileId;
		this.sortOrder = sortOrder;
	}

	public Long getReviewId() {
		return reviewId;
	}

	public Long getFileId() {
		return fileId;
	}
}
