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
		Long su = jdbc.queryForObject("INSERT INTO users(nickname) VALUES ('PDF공방') RETURNING id", Long.class);
		Long sid = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, 'PDF공방') RETURNING id", Long.class, su);
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
