package com.koitda.gauge.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.gauge.dto.GaugeDtos.AppliedCalculationSummary;
import com.koitda.gauge.dto.GaugeDtos.CalculateRequest;
import com.koitda.gauge.dto.GaugeDtos.CalculationResponse;
import com.koitda.gauge.dto.GaugeDtos.GaugeDefaultsResponse;
import com.koitda.gauge.service.GaugeService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게이지 계산(GAUGE-001~017). 적용 결과 조회만 공개(공개 니팅로그), 나머지는 로그인 필요. */
@RestController
@RequestMapping("/api/v1")
public class GaugeController {

	private final GaugeService gaugeService;

	public GaugeController(GaugeService gaugeService) {
		this.gaugeService = gaugeService;
	}

	@GetMapping("/projects/{projectId}/gauge-defaults")
	public GaugeDefaultsResponse defaults(@PathVariable Long projectId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return gaugeService.defaults(principal.getUserId(), projectId);
	}

	@PostMapping("/gauge/calculations")
	public CalculationResponse calculate(@RequestBody CalculateRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return gaugeService.calculate(principal.getUserId(), request);
	}

	@GetMapping("/gauge/calculations/{calculationId}")
	public CalculationResponse getCalculation(@PathVariable Long calculationId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return gaugeService.getCalculation(principal.getUserId(), calculationId);
	}

	@PostMapping("/gauge/calculations/{calculationId}/apply")
	public void apply(@PathVariable Long calculationId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		gaugeService.apply(principal.getUserId(), calculationId);
	}

	/** 적용 중인 계산(GAUGE-014) — 공개 니팅로그는 비로그인도 조회 가능. */
	@GetMapping("/projects/{projectId}/gauge-calculation")
	public AppliedCalculationSummary applied(@PathVariable Long projectId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return gaugeService.appliedSummary(principal != null ? principal.getUserId() : null, projectId);
	}

	@GetMapping("/projects/{projectId}/gauge-calculations")
	public List<CalculationResponse> history(@PathVariable Long projectId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return gaugeService.history(principal.getUserId(), projectId);
	}
}
