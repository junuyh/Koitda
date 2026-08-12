package com.koitda.file.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.file.service.FileService;
import com.koitda.file.service.FileService.FileContent;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 파일 업로드·서빙. 업로드는 로그인 필요, 서빙(GET)은 공개(도안 이미지 등). */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

	private final FileService fileService;

	public FileController(FileService fileService) {
		this.fileService = fileService;
	}

	public record UploadResponse(Long id) {
	}

	@PostMapping
	public UploadResponse upload(@RequestParam("file") MultipartFile file,
			@RequestParam(value = "usageType", required = false) String usageType,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return new UploadResponse(fileService.uploadImage(principal.getUserId(), usageType, file));
	}

	@GetMapping("/{fileId}")
	public ResponseEntity<byte[]> serve(@PathVariable Long fileId) {
		FileContent c = fileService.load(fileId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(c.contentType()))
				.cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic())
				.body(c.bytes());
	}
}
