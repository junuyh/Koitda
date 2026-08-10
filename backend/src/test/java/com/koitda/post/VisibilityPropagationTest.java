package com.koitda.post;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** ⑦ 공개 설정 상속·상향/하향 전파와 공개 불변식 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class VisibilityPropagationTest {

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

	private long createPrivateProject(MockHttpSession session) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/projects").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"EXTERNAL\",\"externalPattern\":{\"title\":\"공개테스트 도안\"}}"))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private void addLog(MockHttpSession session, long projectId, String body) throws Exception {
		mockMvc.perform(post("/api/v1/projects/" + projectId + "/posts").with(csrf()).session(session)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	@Test
	void 공개_미확인이면_로그를_비공개로_저장한다() throws Exception {
		MockHttpSession s = loginSession("vis-a@koitda.dev");
		long id = createPrivateProject(s);
		// 비공개 니팅로그에 공개 로그, 확인 안 함 → 로그 비공개, 니팅로그 그대로 비공개
		mockMvc.perform(post("/api/v1/projects/" + id + "/posts").with(csrf()).session(s)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"knittingStatus\":\"CO\",\"visibility\":\"PUBLIC\",\"publishProjectConfirmed\":false}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.logVisibility").value("PRIVATE"))
				.andExpect(jsonPath("$.projectPublished").value(false));
	}

	@Test
	void 공개_확인하면_니팅로그도_함께_공개된다() throws Exception {
		MockHttpSession s = loginSession("vis-b@koitda.dev");
		long id = createPrivateProject(s);
		mockMvc.perform(post("/api/v1/projects/" + id + "/posts").with(csrf()).session(s)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"knittingStatus\":\"CO\",\"visibility\":\"PUBLIC\",\"publishProjectConfirmed\":true}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.logVisibility").value("PUBLIC"))
				.andExpect(jsonPath("$.projectPublished").value(true));

		mockMvc.perform(get("/api/v1/projects/" + id).session(s))
				.andExpect(jsonPath("$.visibility").value("PUBLIC"))
				.andExpect(jsonPath("$.publicLogCount").value(1));
	}

	@Test
	void 비공개_전환은_미확인이면_428이고_확인하면_로그까지_비공개된다() throws Exception {
		MockHttpSession s = loginSession("vis-c@koitda.dev");
		long id = createPrivateProject(s);
		// 공개 상태로 만들고 공개 로그 1개 보유
		addLog(s, id, "{\"knittingStatus\":\"CO\",\"visibility\":\"PUBLIC\",\"publishProjectConfirmed\":true}");

		// 영향 조회 → 공개 로그 1개
		mockMvc.perform(post("/api/v1/projects/" + id + "/visibility-impact").with(csrf()).session(s)
						.contentType(MediaType.APPLICATION_JSON).content("{\"visibility\":\"PRIVATE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.affectedPublicLogCount").value(1));

		// 확인 없이 비공개 전환 → 428
		mockMvc.perform(patch("/api/v1/projects/" + id + "/visibility").with(csrf()).session(s)
						.contentType(MediaType.APPLICATION_JSON).content("{\"visibility\":\"PRIVATE\"}"))
				.andExpect(status().isPreconditionRequired())
				.andExpect(jsonPath("$.code").value("CONFIRMATION_REQUIRED"));

		// 확인 후 비공개 전환 → 성공, 하위 로그도 비공개
		mockMvc.perform(patch("/api/v1/projects/" + id + "/visibility").with(csrf()).session(s)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"visibility\":\"PRIVATE\",\"confirmed\":true}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/projects/" + id).session(s))
				.andExpect(jsonPath("$.visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.publicLogCount").value(0));
	}
}
