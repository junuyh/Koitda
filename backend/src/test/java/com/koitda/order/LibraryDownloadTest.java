package com.koitda.order;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** ⑫ 구매 도안 상세·PDF 다운로드(LIBRARY): 소유자만 다운로드, 횟수 증가, 미구매 403. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LibraryDownloadTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private MockHttpSession login(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"password123","nickname":"%s",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":true}]}
						""".formatted(email, email.split("@")[0]))).andExpect(status().isCreated());
		MvcResult r = mockMvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) r.getRequest().getSession(false);
	}

	private long seedPatternWithPdf(MockHttpSession seller, byte[] pdf) throws Exception {
		// 닉네임은 유니크 제약이 있어 테스트마다 다른 값을 써야 한다.
		String brand = "PDF공방_" + java.util.UUID.randomUUID().toString().substring(0, 8);
		Long su = jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, brand);
		Long sid = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id", Long.class, su, brand);
		long patternId = jdbc.queryForObject("""
				INSERT INTO selling_pattern (seller_id, title, craft_type, regular_price, sale_price, product_status, published_at)
				VALUES (?, 'PDF 스웨터', 'KNIT', 8000, 8000, 'APPROVED', now()) RETURNING id
				""", Long.class, sid);
		// PDF 업로드 후 도안 current_file_id 에 연결
		MockMultipartFile file = new MockMultipartFile("file", "p.pdf", "application/pdf", pdf);
		MvcResult up = mockMvc.perform(multipart("/api/v1/files").file(file).param("usageType", "PATTERN_PDF")
				.with(csrf()).session(seller)).andExpect(status().isOk()).andReturn();
		long fileId = ((Number) JsonPath.read(up.getResponse().getContentAsString(), "$.id")).longValue();
		jdbc.update("UPDATE selling_pattern SET current_file_id = ? WHERE id = ?", fileId, patternId);
		return patternId;
	}

	private void purchase(MockHttpSession s, long patternId) throws Exception {
		MvcResult o = mockMvc.perform(post("/api/v1/orders").with(csrf()).session(s)
				.contentType(MediaType.APPLICATION_JSON).content("{\"patternId\":%d,\"agreed\":true}".formatted(patternId)))
				.andExpect(status().isCreated()).andReturn();
		long orderId = ((Number) JsonPath.read(o.getResponse().getContentAsString(), "$.id")).longValue();
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments/complete").with(csrf()).session(s))
				.andExpect(status().isOk());
	}

	/** 이미 등록된 도안에 PDF 하나를 더 업로드해 pdf_file_ids 에 연결한다. */
	private long uploadPdfInto(MockHttpSession seller, long patternId, byte[] pdf, long primaryFileId) throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "extra.pdf", "application/pdf", pdf);
		MvcResult up = mockMvc.perform(multipart("/api/v1/files").file(file).param("usageType", "PATTERN_PDF")
				.with(csrf()).session(seller)).andExpect(status().isOk()).andReturn();
		long fileId = ((Number) JsonPath.read(up.getResponse().getContentAsString(), "$.id")).longValue();
		jdbc.update("UPDATE selling_pattern SET pdf_file_ids = ?::jsonb WHERE id = ?",
				"[" + primaryFileId + "," + fileId + "]", patternId);
		return fileId;
	}

	@Test
	void 여러_PDF_도안은_파일별로_다운로드되고_남의_파일은_거부된다() throws Exception {
		MockHttpSession seller = login("multi-seller@koitda.dev");
		byte[] pdf1 = "%PDF-1.4 first".getBytes();
		long patternId = seedPatternWithPdf(seller, pdf1);
		long primary = jdbc.queryForObject("SELECT current_file_id FROM selling_pattern WHERE id = ?", Long.class, patternId);
		byte[] pdf2 = "%PDF-1.4 second".getBytes();
		long secondFileId = uploadPdfInto(seller, patternId, pdf2, primary);

		MockHttpSession buyer = login("multi-buyer@koitda.dev");
		purchase(buyer, patternId);

		// 상세: pdf_file_ids 두 개 노출(대표가 첫 번째)
		mockMvc.perform(get("/api/v1/users/me/pattern-library/" + patternId).session(buyer))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pdfFileIds.length()").value(2))
				.andExpect(jsonPath("$.pdfFileIds[0]").value((int) primary))
				.andExpect(jsonPath("$.pdfFileIds[1]").value((int) secondFileId));

		// 대표(fileId 미지정) 다운로드 → 첫 번째 PDF
		MvcResult dl0 = mockMvc.perform(post("/api/v1/pattern-library/" + patternId + "/download").with(csrf()).session(buyer))
				.andExpect(status().isOk()).andReturn();
		org.junit.jupiter.api.Assertions.assertArrayEquals(pdf1, dl0.getResponse().getContentAsByteArray());

		// 두 번째 PDF 를 fileId 로 지정해 다운로드
		MvcResult dl1 = mockMvc.perform(post("/api/v1/pattern-library/" + patternId + "/download")
						.param("fileId", String.valueOf(secondFileId)).with(csrf()).session(buyer))
				.andExpect(status().isOk()).andReturn();
		org.junit.jupiter.api.Assertions.assertArrayEquals(pdf2, dl1.getResponse().getContentAsByteArray());

		// 이 도안에 속하지 않는 fileId 는 거부(FILE_NOT_FOUND → 404)
		mockMvc.perform(post("/api/v1/pattern-library/" + patternId + "/download")
						.param("fileId", "999999").with(csrf()).session(buyer))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
	}

	@Test
	void 구매자는_PDF를_다운로드하고_횟수가_증가한다() throws Exception {
		MockHttpSession seller = login("pdf-seller@koitda.dev");
		byte[] pdf = "%PDF-1.4 test".getBytes();
		long patternId = seedPatternWithPdf(seller, pdf);

		MockHttpSession buyer = login("pdf-buyer@koitda.dev");
		purchase(buyer, patternId);

		// 상세: PDF 있음, 다운로드 0회
		mockMvc.perform(get("/api/v1/users/me/pattern-library/" + patternId).session(buyer))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasPdf").value(true))
				.andExpect(jsonPath("$.downloadCount").value(0));

		// 다운로드: PDF 바이트 + 남은 횟수 헤더
		MvcResult dl = mockMvc.perform(post("/api/v1/pattern-library/" + patternId + "/download").with(csrf()).session(buyer))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"))
				.andExpect(header().string("X-Download-Remaining", "9"))
				.andReturn();
		org.junit.jupiter.api.Assertions.assertArrayEquals(pdf, dl.getResponse().getContentAsByteArray());

		// 다운로드 후 횟수 1 증가
		mockMvc.perform(get("/api/v1/users/me/pattern-library/" + patternId).session(buyer))
				.andExpect(jsonPath("$.downloadCount").value(1));

		// 미구매자는 다운로드 불가(403)
		MockHttpSession other = login("pdf-nonbuyer@koitda.dev");
		mockMvc.perform(post("/api/v1/pattern-library/" + patternId + "/download").with(csrf()).session(other))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_PURCHASED"));
	}
}
