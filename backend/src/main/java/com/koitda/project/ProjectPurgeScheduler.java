package com.koitda.project;

import com.koitda.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 휴지통 90일 경과 항목을 완전 삭제하는 배치(DATA-003). 매일 새벽 4시. */
@Component
public class ProjectPurgeScheduler {

	private static final Logger log = LoggerFactory.getLogger(ProjectPurgeScheduler.class);

	private final ProjectService projectService;

	public ProjectPurgeScheduler(ProjectService projectService) {
		this.projectService = projectService;
	}

	@Scheduled(cron = "0 0 4 * * *")
	public void purgeExpired() {
		int purged = projectService.purgeExpired();
		if (purged > 0) {
			log.info("휴지통 완전 삭제 배치: {}건 제거", purged);
		}
	}
}
