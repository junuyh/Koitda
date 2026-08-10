package com.koitda.post;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.koitda.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LogApiTest {

	@Autowired
	private MockMvc mockMvc;

	private MockHttpSession loginSession(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"password123","nickname":"%s",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":true}]}
						""".formatted(email, email.split("@")[0])))
				.andExpect(status().isCreated());
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) login.getRequest().getSession(false);
	}

	private long createExternalProject(MockHttpSession session) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/projects").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"EXTERNAL\",\"externalPattern\":{\"title\":\"로그용 도안\"}}"))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private void addLog(MockHttpSession session, long projectId, String statusCode, String logDate,
			String expectedProjectStatus) throws Exception {
		String date = logDate == null ? "" : ",\"logDate\":\"" + logDate + "\"";
		mockMvc.perform(post("/api/v1/projects/" + projectId + "/posts").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"" + statusCode + "\"" + date + ",\"comment\":\"진행\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.projectStatus").value(expectedProjectStatus));
	}

	@Test
	void 로그를_쓰면_니팅로그_상태가_최신_로그를_따른다() throws Exception {
		MockHttpSession session = loginSession("log-user@koitda.dev");
		long projectId = createExternalProject(session);

		// 첫 로그 CO → 니팅로그 상태 CO
		addLog(session, projectId, "CO", null, "CO");
		// 오늘 WIP 로그 → WIP
		addLog(session, projectId, "WIP", null, "WIP");
		// 어제 날짜로 FO 로그 추가해도, 기록일 최신(오늘 WIP)이 우선 → 여전히 WIP
		addLog(session, projectId, "FO", "2020-01-01", "WIP");

		// 상세에도 반영
		mockMvc.perform(get("/api/v1/projects/" + projectId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WIP"));

		// 로그 목록 3건
		mockMvc.perform(get("/api/v1/projects/" + projectId + "/posts").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
	}
}
