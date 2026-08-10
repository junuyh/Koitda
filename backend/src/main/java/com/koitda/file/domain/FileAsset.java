package com.koitda.file.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 파일 메타데이터(읽기 위주). 실제 업로드/생성은 파일 업로드 슬라이스에서 다룬다.
 * 지금은 이미지의 저장 키를 읽어 URL 을 구성하는 용도로만 매핑한다.
 */
@Entity
@Table(name = "file_asset")
public class FileAsset {

	@Id
	private Long id;

	@Column(name = "storage_key")
	private String storageKey;

	@Column(name = "thumbnail_key")
	private String thumbnailKey;

	@Column(name = "content_type")
	private String contentType;

	protected FileAsset() {
	}

	public Long getId() {
		return id;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public String getThumbnailKey() {
		return thumbnailKey;
	}

	public String getContentType() {
		return contentType;
	}
}
