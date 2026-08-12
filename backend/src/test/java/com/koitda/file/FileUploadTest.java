package com.koitda.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 파일 업로드·서빙: 이미지 업로드→서빙, 지원하지 않는 형식 거부, 없는 파일 404. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FileUploadTest {

	@Autowired
	private MockMvc mockMvc;

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

	@Test
	void 이미지를_업로드하고_서빙받는다() throws Exception {
		MockHttpSession session = login("file-upload@koitda.dev");
		byte[] png = { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4 };
		MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", png);

		MvcResult up = mockMvc.perform(multipart("/api/v1/files").file(file).param("usageType", "PATTERN_IMAGE")
				.with(csrf()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andReturn();
		long id = ((Number) JsonPath.read(up.getResponse().getContentAsString(), "$.id")).longValue();

		// 서빙은 공개(비로그인)로 접근 가능하고 원본 바이트를 그대로 준다
		MvcResult served = mockMvc.perform(get("/api/v1/files/" + id))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/png"))
				.andReturn();
		assertArrayEquals(png, served.getResponse().getContentAsByteArray(), "서빙 바이트가 원본과 다름");
	}

	@Test
	void 지원하지_않는_형식은_415이고_없는_파일은_404() throws Exception {
		MockHttpSession session = login("file-reject@koitda.dev");
		MockMultipartFile txt = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
		mockMvc.perform(multipart("/api/v1/files").file(txt).param("usageType", "PATTERN_IMAGE")
				.with(csrf()).session(session))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));

		mockMvc.perform(get("/api/v1/files/99999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
	}
}
