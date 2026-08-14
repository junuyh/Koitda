package com.koitda.order.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.file.service.FileService;
import com.koitda.file.service.FileService.FileContent;
import com.koitda.order.domain.PatternLibrary;
import com.koitda.order.dto.OrderDtos.LibraryDetailResponse;
import com.koitda.order.repository.PatternLibraryRepository;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.repository.SellingPatternRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** 구매 도안(라이브러리) 상세·다운로드. 다운로드는 소유자만, 한도 내에서(LIBRARY). */
@Service
public class LibraryService {

	private final PatternLibraryRepository libraryRepository;
	private final SellingPatternRepository patternRepository;
	private final FileService fileService;
	private final ObjectMapper objectMapper;

	public LibraryService(PatternLibraryRepository libraryRepository,
			SellingPatternRepository patternRepository, FileService fileService,
			ObjectMapper objectMapper) {
		this.libraryRepository = libraryRepository;
		this.patternRepository = patternRepository;
		this.fileService = fileService;
		this.objectMapper = objectMapper;
	}

	public record DownloadResult(String fileName, FileContent content, int remaining) {
	}

	@Transactional(readOnly = true)
	public LibraryDetailResponse detail(Long userId, Long patternId) {
		PatternLibrary lib = libraryRepository
				.findFirstByUserIdAndPatternIdOrderByPurchasedAtAsc(userId, patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_PURCHASED, "구매하지 않은 도안입니다."));
		SellingPattern p = patternRepository.findById(patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
		return new LibraryDetailResponse(patternId, p.getTitle(),
				p.getSeller() != null ? p.getSeller().getBrandName() : null,
				lib.getPurchasedAt(), lib.isRevoked(), p.getCurrentFileId() != null,
				lib.getDownloadCount(), lib.getDownloadLimit(), p.getPdfFileIds());
	}

	/**
	 * PDF 다운로드. 회수되지 않은 사용권 소유자만, 한도 내에서. 다운로드 횟수를 1 증가시킨다.
	 * (워터마크 삽입은 후속 — 지금은 원본 PDF 를 소유자에게만 제공하고 다운로드를 기록한다.)
	 */
	@Transactional
	public DownloadResult download(Long userId, Long patternId, Long fileId) {
		PatternLibrary lib = libraryRepository
				.findFirstByUserIdAndPatternIdAndRevokedAtIsNull(userId, patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_PURCHASED, "다운로드 권한이 없습니다."));
		SellingPattern p = patternRepository.findById(patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
		// 특정 PDF(fileId) 지정 시 이 도안의 PDF 목록 안에 있어야 한다. 미지정이면 대표(current).
		Long targetId = p.getCurrentFileId();
		if (fileId != null) {
			if (!pdfIdsOf(p).contains(fileId)) {
				throw new ApiException(ErrorCode.FILE_NOT_FOUND, "이 도안의 PDF가 아닙니다.");
			}
			targetId = fileId;
		}
		if (targetId == null) {
			throw new ApiException(ErrorCode.FILE_NOT_FOUND, "등록된 PDF 파일이 없습니다.");
		}
		try {
			lib.recordDownload();
		} catch (IllegalStateException e) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "다운로드 한도를 초과했습니다.");
		}
		FileContent content = fileService.load(targetId);
		String safeTitle = p.getTitle() == null ? "pattern" : p.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
		return new DownloadResult(safeTitle + ".pdf", content, lib.getDownloadLimit() - lib.getDownloadCount());
	}

	private java.util.List<Long> pdfIdsOf(SellingPattern p) {
		var ids = new java.util.ArrayList<Long>();
		if (p.getCurrentFileId() != null) {
			ids.add(p.getCurrentFileId());
		}
		if (p.getPdfFileIds() != null && !p.getPdfFileIds().isBlank()) {
			try {
				for (var n : objectMapper.readTree(p.getPdfFileIds())) {
					ids.add(n.asLong());
				}
			} catch (Exception ignored) {
				// 파싱 실패 시 대표만 허용
			}
		}
		return ids;
	}
}
