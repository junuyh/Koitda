package com.koitda;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		// 운영·로컬과 동일한 메이저 버전으로 고정해 테스트 환경 편차를 없앤다.
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
	}

}
