package com.koitda.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.koitda.TestcontainersConfiguration;
import com.koitda.user.domain.RoleType;
import com.koitda.user.domain.User;
import com.koitda.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** 한 계정이 여러 역할을 동시에 보유(AUTH-005)하는 것이 엔티티 저장·재조회에서 유지되는지 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserRolePersistenceTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void 판매자_승인은_USER_역할을_유지한_채_SELLER_를_추가한다() {
		User user = User.createMember("seller@koitda.dev", "hash", "겸직러");
		user.addRole(RoleType.SELLER);
		Long id = userRepository.save(user).getId();

		User reloaded = userRepository.findById(id).orElseThrow();
		assertThat(reloaded.getRoles()).containsExactlyInAnyOrder(RoleType.USER, RoleType.SELLER);
	}
}
