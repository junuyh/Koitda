package com.koitda.user;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * 이메일 인증번호(데모). 실제 SMTP 연동 대신 인메모리에 보관하고, dev 편의를 위해 발급 코드를 응답에 담아 반환한다.
 * 운영에서는 메일 발송으로 대체하고 응답에서 코드를 제거해야 한다.
 */
@Service
public class EmailVerificationService {

	private record Entry(String code, Instant expiresAt) {
	}

	private final ConcurrentHashMap<Long, Entry> store = new ConcurrentHashMap<>();

	/** 6자리 코드 발급(5분 유효). 반환값은 데모용 노출 코드. */
	public String issue(Long userId) {
		String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
		store.put(userId, new Entry(code, Instant.now().plus(5, ChronoUnit.MINUTES)));
		return code;
	}

	/** 코드 검증 — 일치·미만료면 소비(1회용), 아니면 예외. */
	public void verify(Long userId, String code) {
		Entry e = store.get(userId);
		if (e == null || e.expiresAt().isBefore(Instant.now())) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "인증번호가 만료되었습니다. 다시 요청하세요.");
		}
		if (!e.code().equals(code)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "인증번호가 올바르지 않습니다.");
		}
		store.remove(userId);
	}
}
