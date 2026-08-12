package com.koitda.file.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.file.domain.FileAsset;
import com.koitda.file.repository.FileAssetRepository;
import com.koitda.file.storage.StorageService;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 파일 업로드·조회. 이미지 위주(cut1). PDF·워터마크는 다운로드 슬라이스에서 확장한다. */
@Service
public class FileService {

	private static final long MAX_IMAGE_BYTES = 15L * 1024 * 1024;
	private static final long MAX_PDF_BYTES = 30L * 1024 * 1024;
	private static final Map<String, String> IMAGE_EXT = Map.of(
			"image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp", "image/gif", ".gif");
	private static final Set<String> IMAGE_USAGE = Set.of("PATTERN_IMAGE", "PROJECT_IMAGE", "REVIEW_IMAGE", "PROFILE");
	private static final Set<String> PDF_USAGE = Set.of("PATTERN_PDF");

	private final StorageService storage;
	private final FileAssetRepository fileRepository;

	public FileService(StorageService storage, FileAssetRepository fileRepository) {
		this.storage = storage;
		this.fileRepository = fileRepository;
	}

	public record FileContent(String contentType, byte[] bytes) {
	}

	/** 파일 업로드(이미지·PDF). 용도로 허용 형식·크기를 나눈다. 반환값은 file_asset id. */
	@Transactional
	public Long upload(Long uploaderId, String usageTypeRaw, MultipartFile file) {
		String usageType = (usageTypeRaw == null || usageTypeRaw.isBlank()) ? "PATTERN_IMAGE" : usageTypeRaw;
		if (file == null || file.isEmpty()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "빈 파일입니다.");
		}
		String contentType = file.getContentType();
		String ext;
		if (PDF_USAGE.contains(usageType)) {
			if (file.getSize() > MAX_PDF_BYTES) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "PDF는 30MB 이하만 업로드할 수 있습니다.");
			}
			if (!"application/pdf".equals(contentType)) {
				throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE, "PDF 파일만 업로드할 수 있습니다.");
			}
			ext = ".pdf";
		} else if (IMAGE_USAGE.contains(usageType)) {
			if (file.getSize() > MAX_IMAGE_BYTES) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "이미지는 15MB 이하만 업로드할 수 있습니다.");
			}
			ext = contentType == null ? null : IMAGE_EXT.get(contentType);
			if (ext == null) {
				throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE, "JPG·PNG·WEBP·GIF 이미지만 업로드할 수 있습니다.");
			}
		} else {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "허용되지 않는 용도입니다.");
		}

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (java.io.IOException e) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "파일을 읽을 수 없습니다.");
		}
		String key = usageType.toLowerCase() + "/" + UUID.randomUUID() + ext;
		storage.put(key, bytes, contentType);

		FileAsset saved = fileRepository.save(FileAsset.completed(
				uploaderId, usageType, file.getOriginalFilename(), contentType, bytes.length, key));
		return saved.getId();
	}

	/** 파일 실체 조회(서빙용). 삭제되지 않은 파일만. */
	@Transactional(readOnly = true)
	public FileContent load(Long fileId) {
		FileAsset f = fileRepository.findById(fileId)
				.filter(x -> !x.isDeleted())
				.orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND, "파일을 찾을 수 없습니다."));
		byte[] bytes = storage.read(f.getStorageKey());
		return new FileContent(f.getContentType() != null ? f.getContentType() : "application/octet-stream", bytes);
	}
}
