package com.koitda.gauge.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.gauge.domain.GaugeCalculation;
import com.koitda.gauge.dto.GaugeDtos.AppliedCalculationSummary;
import com.koitda.gauge.dto.GaugeDtos.CalculateRequest;
import com.koitda.gauge.dto.GaugeDtos.CalculationResponse;
import com.koitda.gauge.dto.GaugeDtos.GaugeDefaultsResponse;
import com.koitda.gauge.dto.GaugeDtos.GaugeInput;
import com.koitda.gauge.dto.GaugeDtos.MyGaugeSuggestion;
import com.koitda.gauge.dto.GaugeDtos.SizeInfo;
import com.koitda.gauge.repository.GaugeCalculationRepository;
import com.koitda.project.domain.KnittingProject;
import com.koitda.project.domain.ProjectGauge;
import com.koitda.project.domain.ProjectVisibility;
import com.koitda.project.repository.KnittingProjectRepository;
import com.koitda.project.repository.ProjectGaugeRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 게이지 계산 흐름(GAUGE-001~017). 계산은 GaugeCalculator(서버 수식)로만 처리한다. */
@Service
public class GaugeService {

	private static final Map<String, String> MEASUREMENT_LABELS = Map.of(
			"chestCm", "가슴둘레", "lengthCm", "총장", "sleeveLengthCm", "소매길이", "shoulderCm", "어깨너비");

	private final KnittingProjectRepository projectRepository;
	private final ProjectGaugeRepository projectGaugeRepository;
	private final GaugeCalculationRepository calculationRepository;
	private final ObjectMapper objectMapper;

	public GaugeService(KnittingProjectRepository projectRepository, ProjectGaugeRepository projectGaugeRepository,
			GaugeCalculationRepository calculationRepository, ObjectMapper objectMapper) {
		this.projectRepository = projectRepository;
		this.projectGaugeRepository = projectGaugeRepository;
		this.calculationRepository = calculationRepository;
		this.objectMapper = objectMapper;
	}

	/** 계산 기본값(GAUGE-002·003·004). 도안 게이지·사이즈는 니팅로그 스냅샷에서, 내 게이지는 등록값에서. */
	@Transactional(readOnly = true)
	public GaugeDefaultsResponse defaults(Long userId, Long projectId) {
		KnittingProject project = ownProject(userId, projectId);
		List<MyGaugeSuggestion> suggestions = projectGaugeRepository
				.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
				.map(this::toSuggestion).toList();

		boolean external = project.getSellingPatternId() == null || project.getPatternSnapshot() == null;
		if (external) {
			return new GaugeDefaultsResponse("EXTERNAL", true, null, List.of(), Map.of(), suggestions);
		}
		JsonNode snap = readTree(project.getPatternSnapshot());
		GaugeInput patternGauge = parseGauge(snap.get("gauge"));
		List<SizeInfo> sizes = parseSizes(snap.get("sizes"));
		boolean requiresManual = patternGauge == null || sizes.isEmpty();
		return new GaugeDefaultsResponse("CATALOG", requiresManual, patternGauge, sizes,
				measurementLabels(sizes), suggestions);
	}

	/** 계산 실행(GAUGE-006·007·011). 결과를 저장하되 적용하지는 않는다. */
	@Transactional
	public CalculationResponse calculate(Long userId, CalculateRequest req) {
		KnittingProject project = ownProject(userId, req.projectId());

		SizeInfo size = resolveSize(project, req.selectedSizeLabel());
		GaugeInput patternGauge = req.patternGauge() != null ? req.patternGauge() : patternGaugeFromSnapshot(project);
		GaugeInput myGauge = req.myGauge();

		requirePositive(patternGauge, "도안 게이지");
		requirePositive(myGauge, "내 게이지");
		// 계산은 '치수 × 내 게이지' 기준(GAUGE-011)이라 시작 콧수는 필요 없다. 완성 치수가 있는 사이즈만 있으면 된다.
		if (size == null || size.measurements() == null || size.measurements().isEmpty()) {
			throw new ApiException(ErrorCode.GAUGE_INPUT_REQUIRED, "완성 치수가 있는 도안 사이즈를 선택하세요.");
		}

		Map<String, String> labels = measurementLabels(List.of(size));
		CalculationResponse result = GaugeCalculator.calculate(patternGauge, myGauge, size,
				req.targetMeasurements(), labels);

		GaugeCalculation saved = calculationRepository.save(GaugeCalculation.create(
				req.projectId(), toJson(patternGauge), toJson(myGauge), req.selectedSizeLabel(),
				toJson(req.targetMeasurements()), toJson(result),
				result.gaugeAdjustment().adjustedCastOnStitches(), result.adjustmentSummary(),
				result.adjustmentSummary() != null));

		return withId(result, saved.getId());
	}

	@Transactional(readOnly = true)
	public CalculationResponse getCalculation(Long userId, Long calculationId) {
		GaugeCalculation calc = calculationRepository.findById(calculationId)
				.orElseThrow(() -> new ApiException(ErrorCode.GAUGE_CALCULATION_NOT_FOUND, "계산을 찾을 수 없습니다."));
		ownProject(userId, calc.getProjectId());
		return readResult(calc);
	}

	/** 계산 적용(GAUGE-013) — 이전 적용을 해제하고 이 계산을 적용한다(니팅로그당 1건). */
	@Transactional
	public void apply(Long userId, Long calculationId) {
		GaugeCalculation calc = calculationRepository.findById(calculationId)
				.orElseThrow(() -> new ApiException(ErrorCode.GAUGE_CALCULATION_NOT_FOUND, "계산을 찾을 수 없습니다."));
		ownProject(userId, calc.getProjectId());
		calculationRepository.unapplyAll(calc.getProjectId());
		calc.apply();
	}

	/** 적용 중인 계산(GAUGE-014). 공개 니팅로그는 비로그인·타인도 조회 가능. */
	@Transactional(readOnly = true)
	public AppliedCalculationSummary appliedSummary(Long viewerUserId, Long projectId) {
		KnittingProject project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다."));
		boolean owner = viewerUserId != null && viewerUserId.equals(project.getUserId());
		if (project.getVisibility() != ProjectVisibility.PUBLIC && !owner) {
			throw new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다.");
		}
		return calculationRepository.findByProjectIdAndAppliedTrue(projectId)
				.map(c -> new AppliedCalculationSummary(c.getId(), c.getAdjustedCastOnStitches(),
						c.getAdjustmentSummary(), c.isHasAdjustment(), c.getSelectedSizeLabel(),
						c.getMyGauge(), c.getPatternGauge(), c.getResult()))
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public List<CalculationResponse> history(Long userId, Long projectId) {
		ownProject(userId, projectId);
		return calculationRepository.findByProjectIdOrderByCreatedAtDescIdDesc(projectId).stream()
				.map(this::readResult).toList();
	}

	// ---------------------------------------------------------------- 내부

	private KnittingProject ownProject(Long userId, Long projectId) {
		return projectRepository.findByIdAndUserId(projectId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "니팅로그를 찾을 수 없습니다."));
	}

	private SizeInfo resolveSize(KnittingProject project, String label) {
		if (project.getPatternSnapshot() == null) {
			return null;
		}
		JsonNode sizes = readTree(project.getPatternSnapshot()).get("sizes");
		if (sizes == null) {
			return null;
		}
		for (SizeInfo s : parseSizes(sizes)) {
			if (s.label() != null && s.label().equals(label)) {
				return s;
			}
		}
		return null;
	}

	private GaugeInput patternGaugeFromSnapshot(KnittingProject project) {
		if (project.getPatternSnapshot() == null) {
			return null;
		}
		return parseGauge(readTree(project.getPatternSnapshot()).get("gauge"));
	}

	private MyGaugeSuggestion toSuggestion(ProjectGauge g) {
		return new MyGaugeSuggestion(g.getId(),
				g.getStitches() != null ? g.getStitches().doubleValue() : null,
				g.getRows() != null ? g.getRows().doubleValue() : null,
				g.getMeasuredStage());
	}

	private GaugeInput parseGauge(JsonNode g) {
		if (g == null || g.isNull()) {
			return null;
		}
		Double stitches = num(g, "stitches");
		Double rows = num(g, "rows");
		if (stitches == null || rows == null) {
			return null;
		}
		return new GaugeInput(stitches, rows, num(g, "needleSizeMm"));
	}

	private List<SizeInfo> parseSizes(JsonNode sizes) {
		List<SizeInfo> out = new ArrayList<>();
		if (sizes == null || !sizes.isArray()) {
			return out;
		}
		for (JsonNode s : sizes) {
			Map<String, Double> meas = new LinkedHashMap<>();
			JsonNode m = s.get("measurements");
			if (m != null && m.isObject()) {
				m.propertyStream().forEach(e -> {
					if (e.getValue().isNumber()) {
						meas.put(e.getKey(), e.getValue().doubleValue());
					}
				});
			}
			Integer castOn = s.hasNonNull("castOnStitches") ? s.get("castOnStitches").intValue() : null;
			String label = s.hasNonNull("label") ? s.get("label").asString() : null;
			out.add(new SizeInfo(label, castOn, meas));
		}
		return out;
	}

	private Map<String, String> measurementLabels(List<SizeInfo> sizes) {
		Map<String, String> labels = new LinkedHashMap<>();
		for (SizeInfo s : sizes) {
			if (s.measurements() != null) {
				for (String k : s.measurements().keySet()) {
					labels.putIfAbsent(k, MEASUREMENT_LABELS.getOrDefault(k, k));
				}
			}
		}
		return labels;
	}

	private void requirePositive(GaugeInput g, String name) {
		if (g == null || g.stitches() == null || g.rows() == null || g.stitches() <= 0 || g.rows() <= 0) {
			throw new ApiException(ErrorCode.GAUGE_INPUT_REQUIRED, name + " 코수·단수가 필요합니다.");
		}
	}

	private CalculationResponse readResult(GaugeCalculation calc) {
		try {
			CalculationResponse r = objectMapper.readValue(calc.getResult(), CalculationResponse.class);
			return withId(r, calc.getId());
		} catch (RuntimeException e) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "계산 결과를 읽을 수 없습니다.");
		}
	}

	private CalculationResponse withId(CalculationResponse r, Long id) {
		return new CalculationResponse(id, r.gaugeAdjustment(), r.sizeAdjustments(), r.needleRecommendation(),
				r.adjustmentSummary(), r.warnings());
	}

	private JsonNode readTree(String json) {
		return objectMapper.readTree(json);
	}

	private String toJson(Object o) {
		return o == null ? null : objectMapper.writeValueAsString(o);
	}

	private Double num(JsonNode node, String field) {
		JsonNode v = node.get(field);
		return (v != null && v.isNumber()) ? v.doubleValue() : null;
	}
}
