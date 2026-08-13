package com.koitda.user.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * 회원(ERD USER). 물리 테이블명은 예약어 회피로 users.
 * 역할은 별도 컬럼이 아니라 user_role 다중 행으로 보유한다.
 * point_balance 는 POINT_TRANSACTION 합계의 캐시이며 진실의 출처가 아니다.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 탈퇴 시 NULL 로 파기(DATA-001). */
	@Column(name = "email")
	private String email;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(name = "nickname", nullable = false)
	private String nickname;

	/** FILE_ASSET.id 참조. 파일 도메인 도입 전까지는 식별자만 보관한다. */
	@Column(name = "profile_image_id")
	private Long profileImageId;

	@Column(name = "intro")
	private String intro;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "pattern_lock_hash")
	private String patternLockHash;

	@Column(name = "pattern_lock_updated_at")
	private OffsetDateTime patternLockUpdatedAt;

	@Column(name = "point_balance", nullable = false)
	private int pointBalance = 0;

	@Column(name = "withdrawn_at")
	private OffsetDateTime withdrawnAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	/**
	 * 겸직을 표현하는 역할 집합. granted_at 은 DB 기본값(now())이 채우므로 매핑하지 않는다.
	 * 로그인 시 권한 계산에 필요하므로 EAGER.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "role")
	@Enumerated(EnumType.STRING)
	private Set<RoleType> roles = EnumSet.noneOf(RoleType.class);

	protected User() {
	}

	private User(String email, String passwordHash, String nickname) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.nickname = nickname;
	}

	/** 신규 가입 회원 생성. 기본 역할 USER 를 부여한다. */
	public static User createMember(String email, String passwordHash, String nickname) {
		User user = new User(email, passwordHash, nickname);
		user.roles.add(RoleType.USER);
		return user;
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public void addRole(RoleType role) {
		this.roles.add(role);
	}

	/** 닉네임 변경(마이페이지 정보 수정). */
	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	/** 비밀번호 변경 — 인자는 이미 해시된 값(BCrypt). */
	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	/**
	 * 포인트 잔액 캐시를 delta 만큼 조정한다(진실의 출처는 POINT_TRANSACTION).
	 * 음수 잔액은 불변식 위반이므로 막는다. 호출자는 거래 원장도 함께 기록해야 한다.
	 */
	public void adjustPoint(int delta) {
		int next = this.pointBalance + delta;
		if (next < 0) {
			throw new IllegalStateException("포인트 잔액은 음수가 될 수 없습니다.");
		}
		this.pointBalance = next;
		this.updatedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getNickname() {
		return nickname;
	}

	public Long getProfileImageId() {
		return profileImageId;
	}

	public String getIntro() {
		return intro;
	}

	public UserStatus getStatus() {
		return status;
	}

	public int getPointBalance() {
		return pointBalance;
	}

	public Set<RoleType> getRoles() {
		return roles;
	}
}
