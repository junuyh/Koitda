package com.koitda.pattern;

import static org.hamcrest.Matchers.hasItem;
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

/**
 * ⑩ 판매자 도안 등록·심사(SELLER-002·003, PATTERN-011, ADMIN-001).
 * 임시저장 → 제출 → 관리자 승인 → 카탈로그 노출의 세로 흐름과 권한·구조 검증을 확인한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SellerPatternRegistrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private long signup(String email) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"password123","nickname":"%s",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":true}]}
						""".formatted(email, email.split("@")[0])))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) r.getRequest().getSession(false);
	}

	/** 회원가입 후 seller_profile 을 직접 넣어 SELLER 역할을 부여한다(승인 흐름은 SellerApplicationTest 가 검증). */
	private MockHttpSession loginAsSeller(String email, String brand) throws Exception {
		long userId = signup(email);
		jdbc.update("INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?)", userId, brand);
		jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, 'SELLER')", userId);
		return login(email);
	}

	private MockHttpSession loginAsAdmin(String email) throws Exception {
		long userId = signup(email);
		jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, 'ADMIN')", userId);
		return login(email);
	}

	private static final String VALID_DRAFT = """
			{
			  "title":"바람 스웨터","designerName":"라라","craftType":"KNIT","difficulty":"보통",
			  "language":"ko","regularPrice":15000,"salePrice":12000,"productForm":"PDF",
			  "deliveryMethod":"DOWNLOAD","pageCount":12,"yarnRequirement":"메리노 400g",
			  "description":"톱다운 요크 스웨터",
			  "gauge":{"stitches":22,"rows":30,"swatchWidthCm":10,"swatchHeightCm":10,"needleSizeMm":4.5},
			  "sizes":[
			    {"label":"1 (S)","castOnStitches":132,"measurements":{"chestCm":96,"lengthCm":58}},
			    {"label":"2 (M)","castOnStitches":148,"measurements":{"chestCm":106,"lengthCm":60}}
			  ]
			}
			""";

	@Test
	void 임시저장_제출_관리자승인을_거치면_카탈로그에_노출된다() throws Exception {
		MockHttpSession seller = loginAsSeller("seller1@koitda.dev", "라라니트");

		// 1) 임시저장 → DRAFT, 이미지·PDF 누락 안내
		MvcResult saved = mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content(VALID_DRAFT))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.productStatus").value("DRAFT"))
				.andExpect(jsonPath("$.missingFields").isArray())
				.andReturn();
		long draftId = ((Number) JsonPath.read(saved.getResponse().getContentAsString(), "$.draftId")).longValue();

		// 아직 승인 전이므로 공개 카탈로그 상세엔 없음(404)
		mockMvc.perform(get("/api/v1/patterns/" + draftId)).andExpect(status().isNotFound());

		// 2) 제출 → PENDING
		mockMvc.perform(post("/api/v1/seller/pattern-drafts/" + draftId + "/submit").with(csrf()).session(seller))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productStatus").value("PENDING"));

		// 3) 관리자 승인 → APPROVED, published_at 기록
		MockHttpSession admin = loginAsAdmin("admin-pat@koitda.dev");
		mockMvc.perform(post("/api/v1/admin/patterns/" + draftId + "/approve").with(csrf()).session(admin))
				.andExpect(status().isOk());

		// 4) 공개 카탈로그 상세에 노출되고 사이즈별 시작 콧수가 보인다(PATTERN-004)
		mockMvc.perform(get("/api/v1/patterns/" + draftId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("바람 스웨터"))
				.andExpect(jsonPath("$.sizeInfo.sizes[0].castOnStitches").value(132))
				.andExpect(jsonPath("$.gaugeInfo.stitches").value(22));

		String status = jdbc.queryForObject(
				"SELECT product_status FROM selling_pattern WHERE id = ?", String.class, draftId);
		assert "APPROVED".equals(status);
	}

	@Test
	void 관리자_심사_큐에_제출된_도안이_보이고_상세를_열_수_있다() throws Exception {
		MockHttpSession seller = loginAsSeller("queue-seller@koitda.dev", "큐브랜드");
		MvcResult saved = mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content(VALID_DRAFT))
				.andExpect(status().isCreated()).andReturn();
		long draftId = ((Number) JsonPath.read(saved.getResponse().getContentAsString(), "$.draftId")).longValue();
		mockMvc.perform(post("/api/v1/seller/pattern-drafts/" + draftId + "/submit").with(csrf()).session(seller))
				.andExpect(status().isOk());

		MockHttpSession admin = loginAsAdmin("queue-admin@koitda.dev");

		// 심사 대기 큐에 방금 제출한 도안이 판매자명과 함께 나타난다
		mockMvc.perform(get("/api/v1/admin/patterns?status=PENDING").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + draftId + ")].sellerBrand").value(hasItem("큐브랜드")));

		// 심사 상세 — 상태 무관, 사이즈·게이지 노출
		mockMvc.perform(get("/api/v1/admin/patterns/" + draftId).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productStatus").value("PENDING"))
				.andExpect(jsonPath("$.sizeInfo.sizes[0].castOnStitches").value(132));

		// 일반 사용자는 심사 큐 접근 불가(403)
		mockMvc.perform(get("/api/v1/admin/patterns?status=PENDING").session(seller))
				.andExpect(status().isForbidden());
	}

	@Test
	void 사이즈명이_없으면_422_스키마오류() throws Exception {
		// 시작 콧수(castOnStitches)는 선택 항목이 되었으므로, 사이즈명(label) 누락만 스키마 오류다.
		MockHttpSession seller = loginAsSeller("seller2@koitda.dev", "브랜드2");
		String bad = """
				{"title":"x","craftType":"KNIT",
				 "gauge":{"stitches":22,"rows":30,"swatchWidthCm":10,"swatchHeightCm":10,"needleSizeMm":4.5},
				 "sizes":[{"castOnStitches":120}]}
				""";
		mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content(bad))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("PATTERN_SIZE_SCHEMA_INVALID"));
	}

	@Test
	void 시작콧수가_없어도_사이즈명만_있으면_임시저장된다() throws Exception {
		// 코바늘 등 시작 콧수 개념이 없는 도안을 위해 castOnStitches 는 선택 항목이다.
		MockHttpSession seller = loginAsSeller("nocaston-seller@koitda.dev", "노캐스트브랜드");
		String body = """
				{"title":"코바늘 도안","craftType":"CROCHET","regularPrice":5000,
				 "sizes":[{"label":"one size","measurements":{"chestCm":90}}]}
				""";
		mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.productStatus").value("DRAFT"));
	}

	@Test
	void 게이지_사이즈_없이_제출하면_422() throws Exception {
		MockHttpSession seller = loginAsSeller("seller3@koitda.dev", "브랜드3");
		// 게이지·사이즈 없이 임시저장(구조 검증은 통과 — 필드 자체가 없음)
		String minimal = "{\"title\":\"제목만\",\"craftType\":\"KNIT\",\"regularPrice\":1000}";
		MvcResult saved = mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content(minimal))
				.andExpect(status().isCreated()).andReturn();
		long draftId = ((Number) JsonPath.read(saved.getResponse().getContentAsString(), "$.draftId")).longValue();

		mockMvc.perform(post("/api/v1/seller/pattern-drafts/" + draftId + "/submit").with(csrf()).session(seller))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("PATTERN_SIZE_SCHEMA_INVALID"));
	}

	@Test
	void 여러_PDF를_등록하면_대표는_첫번째이고_목록이_저장된다() throws Exception {
		MockHttpSession seller = loginAsSeller("multipdf-seller@koitda.dev", "멀티PDF브랜드");

		// PDF 두 개 업로드
		long f1 = uploadPdf(seller, "a.pdf");
		long f2 = uploadPdf(seller, "b.pdf");

		String body = """
				{
				  "title":"멀티PDF 스웨터","craftType":"KNIT","regularPrice":10000,
				  "gauge":{"stitches":22,"rows":30,"swatchWidthCm":10,"swatchHeightCm":10,"needleSizeMm":4.5},
				  "sizes":[{"label":"S","castOnStitches":120,"measurements":{"chestCm":90}}],
				  "pdfFileIds":[%d,%d]
				}
				""".formatted(f1, f2);
		MvcResult saved = mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn();
		long draftId = ((Number) JsonPath.read(saved.getResponse().getContentAsString(), "$.draftId")).longValue();

		// 미리보기: 대표(pdfFileId)=첫 번째, 목록(pdfFileIds)=[f1,f2]
		mockMvc.perform(get("/api/v1/seller/pattern-drafts/" + draftId + "/preview").session(seller))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pdfFileId").value((int) f1))
				.andExpect(jsonPath("$.pdfFileIds.length()").value(2))
				.andExpect(jsonPath("$.pdfFileIds[0]").value((int) f1))
				.andExpect(jsonPath("$.pdfFileIds[1]").value((int) f2));

		// DB: current_file_id=대표, pdf_file_ids JSONB 저장
		Long current = jdbc.queryForObject(
				"SELECT current_file_id FROM selling_pattern WHERE id = ?", Long.class, draftId);
		assert current != null && current == f1;
		String ids = jdbc.queryForObject(
				"SELECT pdf_file_ids::text FROM selling_pattern WHERE id = ?", String.class, draftId);
		assert ids != null && ids.contains(String.valueOf(f1)) && ids.contains(String.valueOf(f2));
	}

	private long uploadPdf(MockHttpSession session, String name) throws Exception {
		org.springframework.mock.web.MockMultipartFile file =
				new org.springframework.mock.web.MockMultipartFile("file", name, "application/pdf", "%PDF-1.4 x".getBytes());
		MvcResult up = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.multipart("/api/v1/files").file(file).param("usageType", "PATTERN_PDF").with(csrf()).session(session))
				.andExpect(status().isOk()).andReturn();
		return ((Number) JsonPath.read(up.getResponse().getContentAsString(), "$.id")).longValue();
	}

	@Test
	void 판매자가_아니면_등록_접근은_403() throws Exception {
		signup("plainuser@koitda.dev");
		MockHttpSession user = login("plainuser@koitda.dev");
		mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON).content(VALID_DRAFT))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void 남의_초안은_보이지_않는다_404() throws Exception {
		MockHttpSession sellerA = loginAsSeller("owner@koitda.dev", "오너브랜드");
		MvcResult saved = mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(sellerA)
				.contentType(MediaType.APPLICATION_JSON).content(VALID_DRAFT))
				.andExpect(status().isCreated()).andReturn();
		long draftId = ((Number) JsonPath.read(saved.getResponse().getContentAsString(), "$.draftId")).longValue();

		MockHttpSession sellerB = loginAsSeller("intruder@koitda.dev", "침입브랜드");
		mockMvc.perform(get("/api/v1/seller/pattern-drafts/" + draftId + "/preview").session(sellerB))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PATTERN_NOT_FOUND"));
	}

	@Test
	void 제출_전_도안은_관리자가_승인할_수_없다_409() throws Exception {
		MockHttpSession seller = loginAsSeller("seller4@koitda.dev", "브랜드4");
		MvcResult saved = mockMvc.perform(post("/api/v1/seller/pattern-drafts").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content(VALID_DRAFT))
				.andExpect(status().isCreated()).andReturn();
		long draftId = ((Number) JsonPath.read(saved.getResponse().getContentAsString(), "$.draftId")).longValue();

		MockHttpSession admin = loginAsAdmin("admin2@koitda.dev");
		mockMvc.perform(post("/api/v1/admin/patterns/" + draftId + "/approve").with(csrf()).session(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_PATTERN_STATE"));
	}
}
