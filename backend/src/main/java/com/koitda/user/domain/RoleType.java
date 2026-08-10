package com.koitda.user.domain;

/** 역할. 한 계정이 여러 개를 동시에 가질 수 있다(AUTH-005). 배타적 단일 값이 아니다. */
public enum RoleType {
	USER,
	SELLER,
	ADMIN
}
