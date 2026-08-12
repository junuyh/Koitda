package com.koitda.file.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 파일 메타데이터. 실체는 StorageService(로컬/S3)가 storage_key 위치에 보관하고,
 * 여기에는 키·소유자·용도·크기 등 메타만 남긴다.
 */
@Entity
@Table(name = "file_asset")
public class FileAsset {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uploader_id", nullable = false)
	private Long uploaderId;

	@Column(name = "usage_type", nullable = false)
	private String usageType;

	@Column(name = "original_name")
	private String originalName;

	@Column(name = "content_type")
	private String contentType;

	@Column(name = "byte_size")
	private Long byteSize;

	@Column(name = "storage_key", nullable = false)
	private String storageKey;

	@Column(name = "thumbnail_key")
	private String thumbnailKey;

	@Column(name = "upload_status", nullable = false)
	private String uploadStatus = "PENDING";

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	protected FileAsset() {
	}

	/** 업로드 완료된 파일 메타 생성. 저장소에 실체를 넣은 뒤 호출한다. */
	public static FileAsset completed(Long uploaderId, String usageType, String originalName, String contentType,
			long byteSize, String storageKey) {
		FileAsset f = new FileAsset();
		f.uploaderId = uploaderId;
		f.usageType = usageType;
		f.originalName = originalName;
		f.contentType = contentType;
		f.byteSize = byteSize;
		f.storageKey = storageKey;
		f.uploadStatus = "COMPLETED";
		return f;
	}

	public boolean isDeleted() {
		return deletedAt != null;
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

	public Long getUploaderId() {
		return uploaderId;
	}
}
