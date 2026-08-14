package com.koitda.gauge;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.koitda.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** ⑬ 게이지 계산(GAUGE-006·007·011·013·014). 서버 수식 정확도와 적용/공개를 검증한다. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GaugeCalcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private void signup(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"password123","nickname":"%s",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":true}]}
						""".formatted(email, email.split("@")[0]))).andExpect(status().isCreated());
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) r.getRequest().getSession(false);
	}

	private long seedPattern() {
		Long su = jdbc.queryForObject("INSERT INTO users(nickname) VALUES ('게이지공방') RETURNING id", Long.class);
		Long sid = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, '게이지공방') RETURNING id", Long.class, su);
		return jdbc.queryForObject("""
				INSERT INTO selling_pattern (seller_id, title, craft_type, regular_price, sale_price, product_status, published_at, gauge_info, size_info)
				VALUES (?, '게이지 스웨터', 'KNIT', 10000, 10000, 'APPROVED', now(),
				  '{"stitches":22,"rows":30,"swatchWidthCm":10,"swatchHeightCm":10,"needleSizeMm":4.5}'::jsonb,
				  '{"sizes":[{"label":"2 (M)","castOnStitches":148,"measurements":{"chestCm":106,"lengthCm":60,"sleeveLengthCm":47}}]}'::jsonb)
				RETURNING id
				""", Long.class, sid);
	}

	/** 시작 콧수 없이 완성 치수만 있는 도안(시작 콧수는 선택 항목). */
	private long seedPatternNoCastOn() {
		String brand = "노시작콧수공방_" + java.util.UUID.randomUUID().toString().substring(0, 8);
		Long su = jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, brand);
		Long sid = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id", Long.class, su, brand);
		return jdbc.queryForObject("""
				INSERT INTO selling_pattern (seller_id, title, craft_type, regular_price, sale_price, product_status, published_at, gauge_info, size_info)
				VALUES (?, '치수만 스웨터', 'KNIT', 10000, 10000, 'APPROVED', now(),
				  '{"stitches":22,"rows":30,"swatchWidthCm":10,"swatchHeightCm":10,"needleSizeMm":4.5}'::jsonb,
				  '{"sizes":[{"label":"2 (M)","measurements":{"chestCm":106,"lengthCm":60,"sleeveLengthCm":47}}]}'::jsonb)
				RETURNING id
				""", Long.class, sid);
	}

	@Test
	void 시작콧수가_없어도_치수기준으로_게이지_계산이_된다() throws Exception {
		signup("gauge-nocaston@koitda.dev");
		MockHttpSession user = login("gauge-nocaston@koitda.dev");
		long patternId = seedPatternNoCastOn();

		MvcResult proj = mockMvc.perform(post("/api/v1/projects").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"CATALOG\",\"sellingPatternId\":%d,\"visibility\":\"PRIVATE\"}".formatted(patternId)))
				.andExpect(status().isCreated()).andReturn();
		long projectId = ((Number) JsonPath.read(proj.getResponse().getContentAsString(), "$.id")).longValue();

		// 시작 콧수가 없어도 계산 성공(422 아님). 조정 콧수는 null, 필요 콧수(치수 기준)는 계산됨.
		mockMvc.perform(post("/api/v1/gauge/calculations").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"projectId":%d,"patternGauge":{"stitches":22,"rows":30,"needleSizeMm":4.5},
						 "myGauge":{"stitches":24,"rows":32},"selectedSizeLabel":"2 (M)",
						 "targetMeasurements":{"chestCm":111}}
						""".formatted(projectId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gaugeAdjustment.adjustedCastOnStitches").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.sizeAdjustments[0].key").value("chestCm"))
				.andExpect(jsonPath("$.sizeAdjustments[0].requiredStitches").value(266));
	}

	@Test
	void 도안_연결_니팅로그에서_게이지_계산을_실행하고_적용한다() throws Exception {
		signup("gauge-user@koitda.dev");
		MockHttpSession user = login("gauge-user@koitda.dev");
		long patternId = seedPattern();

		// 도안 연결 니팅로그 생성(스냅샷 복사) — 공개로 만들어 GAUGE-014 도 검증
		MvcResult proj = mockMvc.perform(post("/api/v1/projects").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"CATALOG\",\"sellingPatternId\":%d,\"visibility\":\"PUBLIC\"}".formatted(patternId)))
				.andExpect(status().isCreated()).andReturn();
		long projectId = ((Number) JsonPath.read(proj.getResponse().getContentAsString(), "$.id")).longValue();

		// 기본값: 도안 게이지·사이즈가 스냅샷에서 채워진다(GAUGE-002)
		mockMvc.perform(get("/api/v1/projects/" + projectId + "/gauge-defaults").session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requiresManualInput").value(false))
				.andExpect(jsonPath("$.patternGauge.stitches").value(22))
				.andExpect(jsonPath("$.sizes[0].castOnStitches").value(148));

		// 계산: 조정 콧수 148×(24÷22)=161.45→161(GAUGE-006), 품 111×24÷10=266.4→266(GAUGE-011)
		MvcResult calc = mockMvc.perform(post("/api/v1/gauge/calculations").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"projectId":%d,"patternGauge":{"stitches":22,"rows":30,"needleSizeMm":4.5},
						 "myGauge":{"stitches":24,"rows":32},"selectedSizeLabel":"2 (M)",
						 "targetMeasurements":{"chestCm":111}}
						""".formatted(projectId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gaugeAdjustment.adjustedCastOnStitches").value(161))
				.andExpect(jsonPath("$.needleRecommendation.direction").value("LARGER"))
				.andExpect(jsonPath("$.adjustmentSummary").value("품 +5cm"))
				.andReturn();
		long calcId = ((Number) JsonPath.read(calc.getResponse().getContentAsString(), "$.calculationId")).longValue();

		// 품(치수 기준) 필요 콧수 266 이 부위별 결과에 있다(측정 순서상 chestCm 이 첫 행)
		mockMvc.perform(get("/api/v1/gauge/calculations/" + calcId).session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sizeAdjustments[0].key").value("chestCm"))
				.andExpect(jsonPath("$.sizeAdjustments[0].requiredStitches").value(266));

		// 적용 → 니팅로그 요약에 반영(GAUGE-013)
		mockMvc.perform(post("/api/v1/gauge/calculations/" + calcId + "/apply").with(csrf()).session(user))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/projects/" + projectId + "/gauge-calculation").session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.adjustedCastOnStitches").value(161))
				.andExpect(jsonPath("$.hasAdjustment").value(true));

		// 공개 니팅로그라 비로그인도 적용 계산을 본다(GAUGE-014)
		mockMvc.perform(get("/api/v1/projects/" + projectId + "/gauge-calculation"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.adjustmentSummary").value("품 +5cm"));
	}
}
