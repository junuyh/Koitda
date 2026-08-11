package com.koitda.point.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.point.dto.PointDtos.PointHistoryResponse;
import com.koitda.point.dto.PointDtos.PointTxItem;
import com.koitda.point.repository.PointTransactionRepository;
import com.koitda.user.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지 포인트 조회(POINT-006). 로그인 필요. */
@RestController
@RequestMapping("/api/v1/users/me/point-transactions")
public class PointController {

	private final PointTransactionRepository txRepository;
	private final UserRepository userRepository;

	public PointController(PointTransactionRepository txRepository, UserRepository userRepository) {
		this.txRepository = txRepository;
		this.userRepository = userRepository;
	}

	@GetMapping
	public PointHistoryResponse history(@AuthenticationPrincipal CustomUserDetails principal) {
		Long userId = principal.getUserId();
		int balance = userRepository.findById(userId).map(u -> u.getPointBalance()).orElse(0);
		var items = txRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
				.map(PointTxItem::from).toList();
		return new PointHistoryResponse(balance, items);
	}
}
