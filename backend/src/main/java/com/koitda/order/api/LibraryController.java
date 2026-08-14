package com.koitda.order.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.order.dto.OrderDtos.LibraryDetailResponse;
import com.koitda.order.service.LibraryService;
import com.koitda.order.service.LibraryService.DownloadResult;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 구매 도안 상세·PDF 다운로드. 로그인·소유자 확인. */
@RestController
@RequestMapping("/api/v1")
public class LibraryController {

	private final LibraryService libraryService;

	public LibraryController(LibraryService libraryService) {
		this.libraryService = libraryService;
	}

	@GetMapping("/users/me/pattern-library/{patternId}")
	public LibraryDetailResponse detail(@PathVariable Long patternId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return libraryService.detail(principal.getUserId(), patternId);
	}

	@PostMapping("/pattern-library/{patternId}/download")
	public ResponseEntity<byte[]> download(@PathVariable Long patternId,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Long fileId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		DownloadResult r = libraryService.download(principal.getUserId(), patternId, fileId);
		String encoded = URLEncoder.encode(r.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header("Content-Disposition", ContentDisposition.attachment().filename(encoded).build().toString())
				.header("X-Download-Remaining", String.valueOf(r.remaining()))
				.body(r.content().bytes());
	}
}
