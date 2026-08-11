package com.koitda.project;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.koitda.TestcontainersConfiguration;
import com.koitda.project.service.ProjectService;
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

/** ⑨ 휴지통·복구·완전삭제·90일 배치 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TrashApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ProjectService projectService;

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

	private long createProjectWithLog(MockHttpSession s) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/projects").with(csrf()).session(s)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"EXTERNAL\",\"externalPattern\":{\"title\":\"삭제 대상 도안\"}}"))
				.andExpect(status().isCreated()).andReturn();
		long id = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
		mockMvc.perform(post("/api/v1/projects/" + id + "/posts").with(csrf()).session(s)
				.contentType(MediaType.APPLICATION_JSON).content("{\"knittingStatus\":\"CO\",\"comment\":\"기록\"}"))
				.andExpect(status().isCreated());
		return id;
	}

	@Test
	void 휴지통_이동_복구_완전삭제_흐름() throws Exception {
		MockHttpSession s = loginSession("trash-a@koitda.dev");
		long id = createProjectWithLog(s);

		// 휴지통 이동 → 연결 로그 1개 함께 삭제, 자동 삭제 예정일 반환
		mockMvc.perform(delete("/api/v1/projects/" + id).with(csrf()).session(s))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.connectedLogCount").value(1))
				.andExpect(jsonPath("$.purgeAt").isNotEmpty());

		// 상세·목록에서 사라지고, 휴지통에는 남는다
		mockMvc.perform(get("/api/v1/projects/" + id).session(s)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/projects/mine").session(s)).andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/v1/projects/trash").session(s))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].remainingDays").value(org.hamcrest.Matchers.greaterThanOrEqualTo(88)));

		// 복구 → 상세·로그 되살아남
		mockMvc.perform(post("/api/v1/projects/" + id + "/restore").with(csrf()).session(s)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/projects/" + id).session(s)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/projects/" + id + "/posts").session(s))
				.andExpect(jsonPath("$.length()").value(1));

		// 다시 휴지통 → 완전 삭제 → 휴지통 비었음
		mockMvc.perform(delete("/api/v1/projects/" + id).with(csrf()).session(s)).andExpect(status().isOk());
		mockMvc.perform(delete("/api/v1/projects/" + id + "/permanent").with(csrf()).session(s)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/projects/trash").session(s)).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void 만료된_항목은_배치로_완전삭제된다() throws Exception {
		MockHttpSession s = loginSession("trash-b@koitda.dev");
		long id = createProjectWithLog(s);
		mockMvc.perform(delete("/api/v1/projects/" + id).with(csrf()).session(s)).andExpect(status().isOk());

		// 자동 삭제 예정일을 과거로 당김 → 배치 대상
		jdbc.update("UPDATE knitting_project SET purge_at = now() - interval '1 day' WHERE id = ?", id);

		int purged = projectService.purgeExpired();
		assertTrue(purged >= 1, "완전 삭제 건수: " + purged);

		Integer remain = jdbc.queryForObject("SELECT count(*) FROM knitting_project WHERE id = ?", Integer.class, id);
		assertTrue(remain != null && remain == 0, "완전 삭제 후 행이 남아있음");
	}
}
