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
			@Value("${gemini.model:gemini-2.0-flash}") String model,
			ObjectMapper objectMapper) {
		this.apiKey = apiKey;
		this.model = model;
		this.objectMapper = objectMapper;
	}

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}

	/** 프롬프트 → 생성 텍스트. 실패 시 사용자에게는 일반 메시지, 로그에 원인 기록. */
	public String generate(String prompt) {
		if (!isConfigured()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "AI 조언이 설정되지 않았습니다. 관리자에게 문의하세요.");
		}
		String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
		String body;
		try {
			// 요청 스키마: {"contents":[{"parts":[{"text": ...}]}]}
			var payload = objectMapper.createObjectNode();
			var contents = payload.putArray("contents");
			var parts = contents.addObject().putArray("parts");
			parts.addObject().put("text", prompt);

			String raw = restClient.post().uri(url)
					.header("x-goog-api-key", apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(objectMapper.writeValueAsString(payload))
					.retrieve()
					.body(String.class);
			return extractText(raw);
		} catch (RestClientResponseException e) {
			log.warn("Gemini 호출 실패: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "AI 조언 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.warn("Gemini 호출 중 오류", e);
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "AI 조언 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
		}
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
