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

	@Test
	void 로그_공개비공개를_수정에서_되돌릴수있다() throws Exception {
		MockHttpSession owner = loginSession("logvis-owner@koitda.dev");
		MvcResult r = mockMvc.perform(post("/api/v1/projects").with(csrf()).session(owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"EXTERNAL\",\"externalPattern\":{\"title\":\"공개도안\"},\"visibility\":\"PUBLIC\"}"))
				.andExpect(status().isCreated()).andReturn();
		long projectId = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();

		// 공개 로그 작성
		MvcResult log = mockMvc.perform(post("/api/v1/projects/" + projectId + "/posts").with(csrf()).session(owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"WIP\",\"comment\":\"공개로그\",\"visibility\":\"PUBLIC\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.logVisibility").value("PUBLIC")).andReturn();
		long postId = ((Number) JsonPath.read(log.getResponse().getContentAsString(), "$.id")).longValue();

		// 수정에서 비공개로 되돌리기(사용자 신고 케이스)
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.patch("/api/v1/projects/" + projectId + "/posts/" + postId).with(csrf()).session(owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"WIP\",\"visibility\":\"PRIVATE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility").value("PRIVATE"));

		// 목록에도 비공개 반영
		mockMvc.perform(get("/api/v1/projects/" + projectId + "/posts").session(owner))
				.andExpect(jsonPath("$[0].visibility").value("PRIVATE"));

		// 다시 공개로(프로젝트가 공개라 확인 불필요)
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.patch("/api/v1/projects/" + projectId + "/posts/" + postId).with(csrf()).session(owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"WIP\",\"visibility\":\"PUBLIC\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility").value("PUBLIC"));
	}

	@Test
	void 비공개_니팅로그의_로그를_공개로_바꾸려면_확인이_필요하다_428() throws Exception {
		MockHttpSession owner = loginSession("logvis-private@koitda.dev");
		long projectId = createExternalProject(owner); // 기본 비공개
		MvcResult log = mockMvc.perform(post("/api/v1/projects/" + projectId + "/posts").with(csrf()).session(owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"WIP\",\"comment\":\"비공개로그\"}"))
				.andExpect(status().isCreated()).andReturn();
		long postId = ((Number) JsonPath.read(log.getResponse().getContentAsString(), "$.id")).longValue();

		// 확인 없이 공개 시도 → 428
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.patch("/api/v1/projects/" + projectId + "/posts/" + postId).with(csrf()).session(owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"WIP\",\"visibility\":\"PUBLIC\"}"))
				.andExpect(status().is(428));

		// 확인하면 로그·니팅로그 함께 공개
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.patch("/api/v1/projects/" + projectId + "/posts/" + postId).with(csrf()).session(owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"WIP\",\"visibility\":\"PUBLIC\",\"publishProjectConfirmed\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility").value("PUBLIC"));
		mockMvc.perform(get("/api/v1/projects/" + projectId).session(owner))
				.andExpect(jsonPath("$.visibility").value("PUBLIC"));
	}

	@Test
	void 공개_니팅로그는_작성자만_편집할수있다() throws Exception {
		// 소유자: 공개 외부 니팅로그 생성
		MockHttpSession owner = loginSession("owner-log@koitda.dev");
		MvcResult r = mockMvc.perform(post("/api/v1/projects").with(csrf()).session(owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"EXTERNAL\",\"externalPattern\":{\"title\":\"공개도안\"},\"visibility\":\"PUBLIC\"}"))
				.andExpect(status().isCreated()).andReturn();
		long projectId = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();

		// 소유자 상세: mine=true
		mockMvc.perform(get("/api/v1/projects/" + projectId).session(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mine").value(true));

		// 타인: 공개라 열람은 되지만 mine=false
		MockHttpSession other = loginSession("intruder-log@koitda.dev");
		mockMvc.perform(get("/api/v1/projects/" + projectId).session(other))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mine").value(false));

		// 타인은 오늘의 로그 작성 불가(소유자 검증 → 404)
		mockMvc.perform(post("/api/v1/projects/" + projectId + "/posts").with(csrf()).session(other)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"WIP\",\"comment\":\"남의 로그에 작성 시도\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

		// 타인은 재료(실·바늘·게이지) 편집 불가(404)
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.patch("/api/v1/projects/" + projectId + "/materials").with(csrf()).session(other)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"yarns\":[{\"brand\":\"침입\",\"yarnName\":\"실\"}]}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

		// 비로그인도 작성 불가(401)
		mockMvc.perform(post("/api/v1/projects/" + projectId + "/posts").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"knittingStatus\":\"WIP\",\"comment\":\"익명\"}"))
				.andExpect(status().isUnauthorized());
	}
}
