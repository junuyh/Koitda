package com.koitda.post.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.post.dto.CreateLogRequest;
import com.koitda.post.dto.LogCreatedResponse;
import com.koitda.post.dto.LogResponse;
import com.koitda.post.service.LogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/posts")
public class LogController {

	private final LogService logService;

	public LogController(LogService logService) {
		this.logService = logService;
	}

	/** 오늘의 로그 작성(POST-001·003·007). 로그인·소유자 필요. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LogCreatedResponse create(@PathVariable Long projectId,
			@Valid @RequestBody CreateLogRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return logService.createLog(principal.getUserId(), projectId, request);
	}

	/** 로그 목록(POST-012). 기록일 내림차순. */
	@GetMapping
	public List<LogResponse> list(@PathVariable Long projectId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return logService.listLogs(projectId, principal.getUserId());
	}
}
