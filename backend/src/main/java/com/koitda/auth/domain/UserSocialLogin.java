package com.koitda.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 소셜 로그인 연결(카카오). users 와 1:N — 한 회원이 여러 프로바이더를 붙일 수 있다.
 * (provider, providerUid) 유니크로 같은 소셜 계정이 두 회원에 붙는 것을 DB가 막는다.
 */
@Entity
@Table(name = "user_social_login")
public class UserSocialLogin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "provider", nullable = false)
	private String provider;

	@Column(name = "provider_uid", nullable = false)
	private String providerUid;

	@Column(name = "created_at", nullable = false, updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	protected UserSocialLogin() {
	}

	public UserSocialLogin(Long userId, String provider, String providerUid) {
		this.userId = userId;
		this.provider = provider;
		this.providerUid = providerUid;
	}

	public Long getUserId() {
		return userId;
	}

	public String getProvider() {
		return provider;
	}

	public String getProviderUid() {
		return providerUid;
	}
}
