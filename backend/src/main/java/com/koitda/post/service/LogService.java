package com.koitda.post.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.post.domain.ContentPost;
import com.koitda.post.domain.PostType;
import com.koitda.post.dto.CreateLogRequest;
import com.koitda.post.dto.LogCreatedResponse;
import com.koitda.post.dto.LogResponse;
import com.koitda.post.repository.ContentPostRepository;
import com.koitda.project.domain.KnittingProject;
import com.koitda.project.domain.ProjectVisibility;
import com.koitda.project.repository.KnittingProjectRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {

	private static final DateTimeFormatter LOG_TITLE = DateTimeFormatter.ofPattern("yyyy.MM.dd'의 로그'");

	private final ContentPostRepository postRepository;
	private final KnittingProjectRepository projectRepository;

	public LogService(ContentPostRepository postRepository, KnittingProjectRepository projectRepository) {
		this.postRepository = postRepository;
		this.projectRepository = projectRepository;
	}

	@Transactional
	public LogCreatedResponse createLog(Long userId, Long projectId, CreateLogRequest req) {
		KnittingProject project = ownedProject(projectId, userId);

		LocalDate logDate = req.logDate() != null ? req.logDate() : LocalDate.now();
		String displayTitle = resolveDisplayTitle(projectId, req.title(), logDate);

		// 상속(POST-010): 미지정 시 니팅로그 공개값을 따른다.
		ProjectVisibility requested = req.visibility() != null ? req.visibility() : project.getVisibility();
		ProjectVisibility logVisibility;
		boolean projectPublished = false;
		if (requested == ProjectVisibility.PUBLIC && project.getVisibility() == ProjectVisibility.PRIVATE) {
			// 상향 전파(POST-011): 확인하면 니팅로그도 공개, 아니면 로그를 비공개로 저장.
			if (req.publishProjectConfirmed()) {
				project.publish();
				logVisibility = ProjectVisibility.PUBLIC;
				projectPublished = true;
			}
			else {
				logVisibility = ProjectVisibility.PRIVATE;
			}
		}
		else {
			logVisibility = requested;
		}

		ContentPost post = ContentPost.forProjectLog(userId, projectId, req.knittingStatus(),
				req.title(), displayTitle, null, logDate, req.comment(), logVisibility);
		postRepository.save(post);

		// 상태 파생: 기록일 기준 최신 로그의 상태로 니팅로그 상태를 갱신(POST-007)
		postRepository.findFirstByProjectIdAndPostTypeAndDeletedAtIsNullOrderByLogDateDescCreatedAtDesc(
						projectId, PostType.PROJECT_LOG)
				.ifPresent(latest -> project.changeStatus(latest.getKnittingStatus()));
		if (logVisibility == ProjectVisibility.PUBLIC) {
			project.increasePublicLogCount();
		}
		projectRepository.save(project);

		return new LogCreatedResponse(post.getId(), displayTitle, post.getKnittingStatus().name(),
				logVisibility.name(), project.getStatus().name(), projectPublished);
	}

	@Transactional(readOnly = true)
	public List<LogResponse> listLogs(Long projectId, Long userId) {
		KnittingProject project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다."));
		boolean visible = project.getUserId().equals(userId)
				|| project.getVisibility() == ProjectVisibility.PUBLIC;
		if (!visible) {
			throw new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다.");
		}
		return postRepository.findByProjectIdAndPostTypeAndDeletedAtIsNullOrderByLogDateDescCreatedAtDesc(
						projectId, PostType.PROJECT_LOG)
				.stream().map(LogResponse::from).toList();
	}

	private KnittingProject ownedProject(Long projectId, Long userId) {
		return projectRepository.findByIdAndDeletedAtIsNull(projectId)
				.filter(p -> p.getUserId().equals(userId))
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다."));
	}

	private String resolveDisplayTitle(Long projectId, String title, LocalDate logDate) {
		if (title != null && !title.isBlank()) {
			return title.trim();
		}
		String base = logDate.format(LOG_TITLE);
		long n = postRepository.countByProjectIdAndPostTypeAndDisplayTitleStartingWith(
				projectId, PostType.PROJECT_LOG, base);
		return n == 0 ? base : base + " (" + (n + 1) + ")";
	}
}
