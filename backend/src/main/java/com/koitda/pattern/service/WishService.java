package com.koitda.pattern.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.dto.WishResponse;
import com.koitda.pattern.repository.SellingPatternRepository;
import com.koitda.pattern.repository.WishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishService {

	private final WishRepository wishRepository;
	private final SellingPatternRepository patternRepository;

	public WishService(WishRepository wishRepository, SellingPatternRepository patternRepository) {
		this.wishRepository = wishRepository;
		this.patternRepository = patternRepository;
	}

	@Transactional
	public WishResponse add(Long userId, Long patternId) {
		if (!patternRepository.existsById(patternId)) {
			throw new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다.");
		}
		// 실제로 새로 등록됐을 때만 카운터를 올린다(재요청은 0행 → 멱등).
		if (wishRepository.insertIfAbsent(userId, patternId) == 1) {
			patternRepository.addWishCount(patternId, 1);
		}
		return new WishResponse(true, currentWishCount(patternId));
	}

	@Transactional
	public WishResponse remove(Long userId, Long patternId) {
		if (wishRepository.deleteWish(userId, patternId) == 1) {
			patternRepository.addWishCount(patternId, -1);
		}
		return new WishResponse(false, currentWishCount(patternId));
	}

	private int currentWishCount(Long patternId) {
		return patternRepository.findById(patternId).map(SellingPattern::getWishCount).orElse(0);
	}
}
