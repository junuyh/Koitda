package com.koitda.project.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.pattern.domain.ProductStatus;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.repository.SellingPatternRepository;
import com.koitda.project.domain.ExternalPattern;
import com.koitda.project.domain.KnittingProject;
import com.koitda.project.domain.ProjectGauge;
import com.koitda.project.domain.ProjectNeedle;
import com.koitda.project.domain.ProjectVisibility;
import com.koitda.project.domain.ProjectYarn;
import com.koitda.project.dto.CreateProjectRequest;
import com.koitda.project.dto.ProjectCreatedResponse;
import com.koitda.project.dto.ProjectDetailResponse;
import com.koitda.post.domain.PostType;
import com.koitda.post.repository.ContentPostRepository;
import com.koitda.project.dto.ProjectGroupResponse;
import com.koitda.project.dto.ProjectListItemResponse;
import com.koitda.project.dto.ProjectTrashDtos.TrashItemResponse;
import com.koitda.project.dto.ProjectTrashDtos.TrashResult;
import com.koitda.project.dto.VisibilityImpactResponse;
import java.time.OffsetDateTime;
import com.koitda.project.repository.ExternalPatternRepository;
import com.koitda.project.repository.KnittingProjectRepository;
import com.koitda.project.domain.ProjectImage;
import com.koitda.project.repository.ProjectGaugeRepository;
import com.koitda.project.repository.ProjectImageRepository;
import com.koitda.project.repository.ProjectNeedleRepository;
import com.koitda.project.repository.ProjectYarnRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class ProjectService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	private final KnittingProjectRepository projectRepository;
	private final ExternalPatternRepository externalRepository;
	private final SellingPatternRepository sellingPatternRepository;
	private final ProjectYarnRepository yarnRepository;
	private final ProjectNeedleRepository needleRepository;
	private final ProjectGaugeRepository gaugeRepository;
	private final ProjectImageRepository imageRepository;
	private final ContentPostRepository contentPostRepository;
	private final com.koitda.file.repository.FileAssetRepository fileAssetRepository;
	private final ObjectMapper objectMapper;

	public ProjectService(KnittingProjectRepository projectRepository,
			ExternalPatternRepository externalRepository,
			SellingPatternRepository sellingPatternRepository,
			ProjectYarnRepository yarnRepository, ProjectNeedleRepository needleRepository,
			ProjectGaugeRepository gaugeRepository, ProjectImageRepository imageRepository,
			ContentPostRepository contentPostRepository,
			com.koitda.file.repository.FileAssetRepository fileAssetRepository,
			ObjectMapper objectMapper) {
		this.projectRepository = projectRepository;
		this.externalRepository = externalRepository;
		this.sellingPatternRepository = sellingPatternRepository;
		this.yarnRepository = yarnRepository;
		this.needleRepository = needleRepository;
		this.gaugeRepository = gaugeRepository;
		this.imageRepository = imageRepository;
		this.contentPostRepository = contentPostRepository;
		this.fileAssetRepository = fileAssetRepository;
		this.objectMapper = objectMapper;
	}

	/** 공개 변경 영향 조회(PROJECT-019) — 비공개 전환 시 함께 비공개될 공개 로그 수. */
	@Transactional(readOnly = true)
	public VisibilityImpactResponse visibilityImpact(Long userId, Long projectId, ProjectVisibility target) {
		ownedProject(projectId, userId);
		long affected = (target == ProjectVisibility.PRIVATE)
				? contentPostRepository.countByProjectIdAndPostTypeAndVisibilityAndDeletedAtIsNull(
						projectId, PostType.PROJECT_LOG, ProjectVisibility.PUBLIC)
				: 0;
		return new VisibilityImpactResponse(affected);
	}

	/** 니팅로그 공개 설정 변경. 비공개 전환은 하위 로그를 함께 비공개로 내린다(하향 전파, PROJECT-019). */
	@Transactional
	public void changeVisibility(Long userId, Long projectId, ProjectVisibility target, boolean confirmed) {
		KnittingProject project = ownedProject(projectId, userId);
		if (project.getVisibility() == target) {
			return;
		}
		if (target == ProjectVisibility.PRIVATE) {
			long affected = contentPostRepository.countByProjectIdAndPostTypeAndVisibilityAndDeletedAtIsNull(
					projectId, PostType.PROJECT_LOG, ProjectVisibility.PUBLIC);
			if (affected > 0 && !confirmed) {
				throw new ApiException(ErrorCode.CONFIRMATION_REQUIRED,
						"공개 중인 로그 " + affected + "개가 함께 비공개로 전환됩니다.");
			}
			contentPostRepository.makeProjectLogsPrivate(projectId);
			project.makePrivate();
		}
		else {
			project.publish();
		}
		projectRepository.save(project);
	}

	private KnittingProject ownedProject(Long projectId, Long userId) {
		return projectRepository.findByIdAndDeletedAtIsNull(projectId)
				.filter(p -> p.getUserId().equals(userId))
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다."));
	}

	private KnittingProject ownedTrashedProject(Long projectId, Long userId) {
		return projectRepository.findByIdAndUserId(projectId, userId)
				.filter(p -> p.getDeletedAt() != null)
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "휴지통에서 찾을 수 없습니다."));
	}

	// ---- 휴지통(⑨) ----

	/** 휴지통 이동 — 연결 로그도 함께 논리 삭제(PROJECT-016, DATA-003). */
	@Transactional
	public TrashResult moveToTrash(Long userId, Long projectId) {
		KnittingProject project = ownedProject(projectId, userId);
		project.moveToTrash();
		int logCount = contentPostRepository.softDeleteByProject(projectId, project.getDeletedAt(), project.getPurgeAt());
		projectRepository.save(project);
		return new TrashResult(logCount, project.getPurgeAt());
	}

	/** 복구 — 니팅로그와 연결 로그를 되살린다. */
	@Transactional
	public void restore(Long userId, Long projectId) {
		KnittingProject project = ownedTrashedProject(projectId, userId);
		project.restore();
		contentPostRepository.restoreByProject(projectId);
		projectRepository.save(project);
	}

	/** 완전 삭제 — FK ON DELETE CASCADE 로 연결 로그·재료도 함께 제거된다. */
	@Transactional
	public void permanentDelete(Long userId, Long projectId) {
		KnittingProject project = ownedTrashedProject(projectId, userId);
		projectRepository.delete(project);
	}

	@Transactional(readOnly = true)
	public List<TrashItemResponse> trashList(Long userId) {
		return projectRepository.findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId).stream()
				.map(TrashItemResponse::from).toList();
	}

	/** 90일 경과 항목 완전 삭제(배치). 삭제 건수 반환. */
	@Transactional
	public int purgeExpired() {
		List<KnittingProject> expired = projectRepository.findByPurgeAtBefore(OffsetDateTime.now());
		projectRepository.deleteAll(expired);
		return expired.size();
	}

	@Transactional
	public ProjectCreatedResponse create(Long userId, CreateProjectRequest req) {
		ProjectVisibility visibility = req.visibility() != null ? req.visibility() : ProjectVisibility.PRIVATE;
		String displayTitle = resolveDisplayTitle(userId, req.title());
		Integer sequence = (req.title() != null && !req.title().isBlank()) ? null : null;

		KnittingProject project;
		if (req.connectionType() == CreateProjectRequest.ConnectionType.CATALOG) {
			if (req.sellingPatternId() == null) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "판매 도안 연결에는 도안 ID가 필요합니다.");
			}
			SellingPattern sp = sellingPatternRepository
					.findByIdAndProductStatus(req.sellingPatternId(), ProductStatus.APPROVED)
					.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
			project = KnittingProject.forCatalog(userId, sp.getId(), req.title(), displayTitle, sequence,
					visibility, req.note(), buildSnapshot(sp), null);
		}
		else {
			Long externalId = resolveExternalPattern(userId, req);
			project = KnittingProject.forExternal(userId, externalId, req.title(), displayTitle, sequence,
					visibility, req.note());
		}

		projectRepository.save(project);
		saveMaterials(project.getId(), req);
		return ProjectCreatedResponse.from(project);
	}

	/** 내 니팅로그 플랫 목록. */
	@Transactional(readOnly = true)
	public List<ProjectListItemResponse> myProjects(Long userId) {
		return projectRepository.findMyProjects(userId).stream()
				.map(ProjectListItemResponse::from).toList();
	}

	/** 공개 니팅로그 피드(둘러보기). sort=likes|recent, 오프셋 페이지네이션. 비로그인도 조회. */
	@Transactional(readOnly = true)
	public List<com.koitda.project.dto.ProjectFeedItemResponse> publicFeed(String sort, int page, int size) {
		int pageSize = Math.min(Math.max(size, 1), 100);
		int offset = Math.max(page, 0) * pageSize;
		var views = "likes".equalsIgnoreCase(sort)
				? projectRepository.findPublicFeedByLikes(pageSize, offset)
				: projectRepository.findPublicFeedRecent(pageSize, offset);
		return views.stream().map(com.koitda.project.dto.ProjectFeedItemResponse::from).toList();
	}

	/** 코잇기 — 연결 도안 기준 그룹 목록. */
	@Transactional(readOnly = true)
	public List<ProjectGroupResponse> groupedByPattern(Long userId) {
		return projectRepository.groupedByPattern(userId).stream()
				.map(ProjectGroupResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public ProjectDetailResponse detail(Long projectId, Long userId) {
		KnittingProject p = projectRepository.findByIdAndDeletedAtIsNull(projectId)
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다."));

		boolean visible = p.getUserId().equals(userId) || p.getVisibility() == ProjectVisibility.PUBLIC;
		if (!visible) {
			throw new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다.");
		}

		ProjectDetailResponse.ExternalInfo external = null;
		if (p.getExternalPatternId() != null) {
			external = externalRepository.findById(p.getExternalPatternId())
					.map(e -> new ProjectDetailResponse.ExternalInfo(e.getTitle(), e.getCreatorName()))
					.orElse(null);
		}

		List<ProjectDetailResponse.Yarn> yarns = yarnRepository
				.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
				.map(y -> new ProjectDetailResponse.Yarn(y.getBrand(), y.getYarnName(), y.getColor(),
						y.getAmount(), y.getUnit(), y.getNote()))
				.toList();
		List<ProjectDetailResponse.Needle> needles = needleRepository
				.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
				.map(n -> new ProjectDetailResponse.Needle(n.getNeedleType(), n.getSizeMm(),
						n.getLengthCm(), n.getNote()))
				.toList();
		List<ProjectDetailResponse.Gauge> gauges = gaugeRepository
				.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
				.map(g -> new ProjectDetailResponse.Gauge(g.getStitches(), g.getRows(),
						g.getNeedleSizeMm(), g.getMeasuredStage()))
				.toList();

		String patternType = (p.getExternalPatternId() != null) ? "EXTERNAL" : "CATALOG";

		List<ProjectDetailResponse.Image> images = imageRepository.findByProject(projectId).stream()
				.map(pi -> new ProjectDetailResponse.Image(
						pi.getFile() != null ? "/api/v1/files/" + pi.getFile().getId() : null))
				.toList();

		return new ProjectDetailResponse(p.getId(), p.getTitle(), p.getDisplayTitle(),
				p.getStatus().name(), p.getVisibility().name(), p.getPublicLogCount(), p.getNote(),
				p.getCreatedAt(), patternType, p.getSellingPatternId(), p.getExternalPatternId(),
				p.getPatternSnapshot(), external, yarns, needles, gauges, images);
	}

	// ---- helpers ----

	private String resolveDisplayTitle(Long userId, String title) {
		if (title != null && !title.isBlank()) {
			return title.trim();
		}
		String base = LocalDate.now().format(DATE);
		long sameDate = projectRepository.countByUserIdAndDisplayTitleStartingWith(userId, base);
		return sameDate == 0 ? base : base + " (" + (sameDate + 1) + ")";
	}

	private Long resolveExternalPattern(Long userId, CreateProjectRequest req) {
		if (req.externalPatternId() != null) {
			ExternalPattern ext = externalRepository.findById(req.externalPatternId())
					.filter(e -> e.getUserId().equals(userId))
					.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "외부 도안을 찾을 수 없습니다."));
			return ext.getId();
		}
		CreateProjectRequest.ExternalPatternInput in = req.externalPattern();
		if (in == null || in.title() == null || in.title().isBlank()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "외부 도안은 도안명이 필요합니다.");
		}
		ExternalPattern saved = externalRepository.save(new ExternalPattern(userId, in.title().trim(),
				in.creatorName(), in.purchasePlace(), in.purchaseUrl(), in.purchasePrice(),
				in.purchaseDate(), in.memo(), in.craftType(), in.categoryId()));
		return saved.getId();
	}

	/** 원작 정보 스냅샷(PROJECT-017) — 연결 시점의 도안 게이지·바늘·사이즈를 복사한다. */
	private String buildSnapshot(SellingPattern p) {
		ObjectNode snap = objectMapper.createObjectNode();
		snap.put("title", p.getTitle());
		snap.put("designerName", p.getDesignerName());
		if (p.getGaugeInfo() != null) {
			snap.set("gauge", objectMapper.readTree(p.getGaugeInfo()));
		}
		if (p.getNeedleInfo() != null) {
			snap.set("needles", objectMapper.readTree(p.getNeedleInfo()));
		}
		if (p.getSizeInfo() != null) {
			JsonNode sizes = objectMapper.readTree(p.getSizeInfo()).get("sizes");
			if (sizes != null) {
				snap.set("sizes", sizes);
			}
		}
		return objectMapper.writeValueAsString(snap);
	}

	private void saveMaterials(Long projectId, CreateProjectRequest req) {
		int i = 0;
		for (CreateProjectRequest.YarnInput y : req.yarnsOrEmpty()) {
			yarnRepository.save(new ProjectYarn(projectId, y.brand(), y.yarnName(), y.color(),
					y.amount(), y.unit(), y.note(), i++));
		}
		i = 0;
		for (CreateProjectRequest.NeedleInput n : req.needlesOrEmpty()) {
			needleRepository.save(new ProjectNeedle(projectId, n.needleType(), n.sizeMm(),
					n.lengthCm(), n.note(), i++));
		}
		i = 0;
		for (CreateProjectRequest.GaugeInput g : req.gaugesOrEmpty()) {
			gaugeRepository.save(new ProjectGauge(projectId, g.stitches(), g.rows(), g.swatchWidthCm(),
					g.swatchHeightCm(), g.needleSizeMm(), g.measuredStage(), i++));
		}
		// 대표 이미지(PROJECT-010) — 최대 7개. file_id 참조만 저장한다.
		i = 0;
		for (Long fileId : req.imageFileIdsOrEmpty()) {
			if (i >= 7) {
				break;
			}
			if (fileId == null || !fileAssetRepository.existsById(fileId)) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 이미지 파일입니다.");
			}
			imageRepository.save(new ProjectImage(projectId, fileAssetRepository.getReferenceById(fileId), i++));
		}
	}
}
