package com.koitda.point.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.point.domain.PointTransaction;
import com.koitda.point.repository.PointTransactionRepository;
import com.koitda.user.domain.User;
import com.koitda.user.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 적립·회수(POINT-001·003). 잔액 캐시(users.point_balance)와 원장(point_transaction)을
 * 한 트랜잭션에서 함께 갱신해 '잔액 = 거래합계' 불변식을 유지한다.
 * 지급액은 서버에서만 결정한다(클라이언트가 보낸 금액을 신뢰하지 않는다).
 */
@Service
public class PointService {

	/** 리뷰 최초 등록 보상. 정책 미확정 항목 — 기본값 500P 로 고정하고 문서에 반영 대상으로 남긴다. */
	public static final int REVIEW_REWARD = 500;

	private final PointTransactionRepository txRepository;
	private final UserRepository userRepository;

	public PointService(PointTransactionRepository txRepository, UserRepository userRepository) {
		this.txRepository = txRepository;
		this.userRepository = userRepository;
	}

	public record EarnResult(int earned, int balance) {
	}

	public record RevokeResult(int revoked, int balance, String failReason) {
	}

	/** 리뷰 적립(POINT-001). 리뷰당 1회 — 이미 적립됐으면 멱등하게 현재 잔액만 돌려준다. */
	@Transactional
	public EarnResult awardReviewPoint(Long userId, Long reviewId) {
		User user = user(userId);
		if (txRepository.existsByReviewIdAndTxType(reviewId, "EARN")) {
			return new EarnResult(0, user.getPointBalance());
		}
		user.adjustPoint(REVIEW_REWARD);
		txRepository.save(PointTransaction.earn(userId, REVIEW_REWARD, user.getPointBalance(),
				"REVIEW_CREATED", reviewId, "REVIEW_EARN:" + reviewId));
		return new EarnResult(REVIEW_REWARD, user.getPointBalance());
	}

	/**
	 * 리뷰 회수(POINT-003). 지급했던 만큼 차감하되, 잔액이 부족하면 회수하지 않고 사유만 기록한다.
	 * 이미 회수됐으면 아무 것도 하지 않는다.
	 */
	@Transactional
	public RevokeResult revokeReviewPoint(Long userId, Long reviewId) {
		User user = user(userId);
		Optional<PointTransaction> earn = txRepository.findByReviewIdAndTxType(reviewId, "EARN");
		if (earn.isEmpty() || txRepository.existsByReviewIdAndTxType(reviewId, "REVOKE")) {
			return new RevokeResult(0, user.getPointBalance(), null);
		}
		int amount = earn.get().getAmount();
		if (user.getPointBalance() < amount) {
			String reason = "잔액 부족으로 회수하지 못함(보유 " + user.getPointBalance() + "P, 회수 대상 " + amount + "P)";
			txRepository.save(PointTransaction.revokeFailed(userId, user.getPointBalance(),
					"REVIEW_DELETED", reviewId, "REVIEW_REVOKE:" + reviewId, reason));
			return new RevokeResult(0, user.getPointBalance(), reason);
		}
		user.adjustPoint(-amount);
		txRepository.save(PointTransaction.revoke(userId, -amount, user.getPointBalance(),
				"REVIEW_DELETED", reviewId, "REVIEW_REVOKE:" + reviewId));
		return new RevokeResult(amount, user.getPointBalance(), null);
	}

	private User user(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "사용자를 찾을 수 없습니다."));
	}
}
