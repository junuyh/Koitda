package com.koitda.auth.repository;

import com.koitda.auth.domain.UserSocialLogin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialLoginRepository extends JpaRepository<UserSocialLogin, Long> {

	/** 로그인 시 소셜 연결로 회원을 찾는다. */
	Optional<UserSocialLogin> findByProviderAndProviderUid(String provider, String providerUid);
}
