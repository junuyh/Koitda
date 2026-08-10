package com.koitda.common.security;

import com.koitda.user.domain.RoleType;
import com.koitda.user.domain.User;
import com.koitda.user.domain.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Spring Security 인증 주체. userId 를 실어 컨트롤러가 현재 사용자를 식별한다. */
public class CustomUserDetails implements UserDetails {

	private final Long userId;
	private final String email;
	private final String passwordHash;
	private final boolean active;
	private final List<GrantedAuthority> authorities;

	public CustomUserDetails(User user) {
		this.userId = user.getId();
		this.email = user.getEmail();
		this.passwordHash = user.getPasswordHash();
		this.active = user.getStatus() == UserStatus.ACTIVE;
		this.authorities = user.getRoles().stream()
				.map(RoleType::name)
				.map(name -> "ROLE_" + name)
				.map(SimpleGrantedAuthority::new)
				.map(GrantedAuthority.class::cast)
				.toList();
	}

	public Long getUserId() {
		return userId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return active;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}
}
