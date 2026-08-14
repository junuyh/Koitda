package com.koitda.gauge.ai;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Google Gemini 텍스트 생성 클라이언트(게이지 조언용).
 * 계산·수치는 코드가 담당하고, 이 클라이언트는 '자연어 조언'만 생성한다(숫자를 지어내지 않도록 프롬프트로 제약).
 * 키는 환경변수(GEMINI_API_KEY)로만 주입 — 저장소·응답에 노출하지 않는다.
 */
@Component
public class GeminiClient {

	private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

	private final String apiKey;
	private final String model;
	private final RestClient restClient = RestClient.create();
	private final ObjectMapper objectMapper;

	public GeminiClient(
			@Value("${gemini.api-key:}") String apiKey,
			@Value("${gemini.model:gemini-flash-latest}") String model,
			ObjectMapper objectMapper) {
		this.apiKey = apiKey;
		this.model = model;
		this.objectMapper = objectMapper;
	}

	private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";
	private volatile String resolvedModel; // 실제로 동작한 모델을 캐시

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}

	/** 프롬프트 → 생성 텍스트. 이미 확정된 모델이 있으면 그걸로, 없으면 후보를 차례로 시도해 되는 모델을 찾는다. */
	public String generate(String prompt) {
		if (!isConfigured()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "AI 조언이 설정되지 않았습니다. 관리자에게 문의하세요.");
		}
		// 이미 동작 확인된 모델이 있으면 그대로.
		if (resolvedModel != null) {
			try {
				return callGenerate(resolvedModel, prompt);
			} catch (Exception e) {
				resolvedModel = null; // 그 모델이 갑자기 막히면 다시 탐색
			}
		}
		// 후보(설정 모델 → 키가 지원하는 텍스트 모델들)를 순서대로 시도, 처음 성공한 모델을 확정.
		RuntimeException last = null;
		for (String candidate : candidateModels()) {
			try {
				String text = callGenerate(candidate, prompt);
				resolvedModel = candidate;
				log.info("Gemini 모델 확정: {}", candidate);
				return text;
			} catch (RestClientResponseException e) {
				log.warn("Gemini 모델 '{}' 실패: status={}", candidate, e.getStatusCode());
				last = e;
			} catch (ApiException e) {
				last = e;
			} catch (Exception e) {
				log.warn("Gemini 모델 '{}' 호출 오류", candidate, e);
				last = new RuntimeException(e);
			}
		}
		log.warn("사용 가능한 Gemini 모델을 찾지 못했습니다.", last);
		throw new ApiException(ErrorCode.INTERNAL_ERROR, "AI 조언 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
	}

	/** 시도할 모델 후보를 우선순위대로. 설정값을 먼저, 이어서 키가 지원하는 텍스트 flash 계열. */
	private java.util.List<String> candidateModels() {
		var out = new java.util.LinkedHashSet<String>();
		if (model != null && !model.isBlank()) {
			out.add(model.trim());
		}
		// 안정적인 별칭 우선.
		out.add("gemini-flash-latest");
		out.add("gemini-flash-lite-latest");
		// 키가 실제 지원하는 텍스트 모델(이미지·TTS·임베딩 등 제외)을 flash 우선으로 추가.
		try {
			String raw = restClient.get().uri(BASE + "/models?pageSize=200")
					.header("x-goog-api-key", apiKey).retrieve().body(String.class);
			JsonNode models = objectMapper.readTree(raw).path("models");
			var flash = new java.util.ArrayList<String>();
			var others = new java.util.ArrayList<String>();
			for (JsonNode m : models) {
				boolean gen = false;
				for (JsonNode meth : m.path("supportedGenerationMethods")) {
					if ("generateContent".equals(meth.asString())) { gen = true; break; }
				}
				if (!gen) continue;
				String name = m.path("name").asString().replaceFirst("^models/", "");
				String l = name.toLowerCase();
				if (l.contains("image") || l.contains("tts") || l.contains("embedding") || l.contains("vision")
						|| l.contains("robotics") || l.contains("lyria") || l.contains("computer")
						|| l.contains("deep-research") || l.contains("antigravity") || l.contains("nano")
						|| l.contains("customtools")) {
					continue; // 텍스트 조언에 부적합
				}
				if (l.contains("flash")) flash.add(name); else others.add(name);
			}
			out.addAll(flash);
			out.addAll(others);
		} catch (Exception e) {
			log.warn("Gemini 모델 목록 조회 실패(후보는 기본값으로 진행)", e);
		}
		return new java.util.ArrayList<>(out);
	}

	/** 실제 generateContent 호출. */
	private String callGenerate(String modelName, String prompt) {
		var payload = objectMapper.createObjectNode();
		var parts = payload.putArray("contents").addObject().putArray("parts");
		parts.addObject().put("text", prompt);
		String raw = restClient.post().uri(BASE + "/models/" + modelName + ":generateContent")
				.header("x-goog-api-key", apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(objectMapper.writeValueAsString(payload))
				.retrieve()
				.body(String.class);
		return extractText(raw);
	}

	/** 응답에서 candidates[0].content.parts[*].text 를 이어붙인다. */
	private String extractText(String raw) {
		try {
			JsonNode root = objectMapper.readTree(raw);
			JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
			StringBuilder sb = new StringBuilder();
			if (parts.isArray()) {
				for (JsonNode p : parts) {
					if (p.hasNonNull("text")) {
						sb.append(p.get("text").asString());
					}
				}
			}
			String text = sb.toString().trim();
			if (text.isEmpty()) {
				log.warn("Gemini 응답에 텍스트 없음: {}", raw);
				throw new ApiException(ErrorCode.INTERNAL_ERROR, "AI 조언을 받지 못했습니다.");
			}
			return text;
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.warn("Gemini 응답 파싱 실패: {}", raw);
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "AI 응답을 해석하지 못했습니다.");
		}
	}
}
