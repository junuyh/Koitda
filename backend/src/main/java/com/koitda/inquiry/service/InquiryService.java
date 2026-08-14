package com.koitda.inquiry.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.inquiry.domain.PatternInquiry;
import com.koitda.inquiry.dto.InquiryDtos.AnswerInquiryRequest;
import com.koitda.inquiry.dto.InquiryDtos.CreateInquiryRequest;
import com.koitda.inquiry.dto.InquiryDtos.InquiryCreatedResponse;
import com.koitda.inquiry.dto.InquiryDtos.InquiryItem;
import com.koitda.inquiry.dto.InquiryDtos.InquiryListResponse;
import com.koitda.inquiry.dto.InquiryDtos.SellerInboxResponse;
import com.koitda.inquiry.dto.InquiryDtos.SellerInquiryItem;
import com.koitda.inquiry.dto.InquiryDtos.UpdateInquiryRequest;
import com.koitda.inquiry.repository.PatternInquiryRepository;
import com.koitda.pattern.domain.ProductStatus;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.repository.SellingPatternRepository;
import com.koitda.user.domain.User;
import com.koitda.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도안 문의(문의하기). 도안 상세에서 구매 전 사용자가 판매자에게 질문한다.
 * 공개/비공개 열람 제어와, 판매자 인박스(미답변 목록 = "알림")를 담당한다.
 */
@Service
public class InquiryService {

	private static final int MAX_CONTENT = 2000;

	private final PatternInquiryRepository inquiryRepository;
	private final SellingPatternRepository patternRepository;
	private final UserRepository userRepository;

	public InquiryService(PatternInquiryRepository inquiryRepository,
			SellingPatternRepository patternRepository, UserRepository userRepository) {
		this.inquiryRepository = inquiryRepository;
		this.patternRepository = patternRepository;
		this.userRepository = userRepository;
	}

	/** 문의 작성. 승인된 도안에만, 로그인 사용자. 본문 필수. */
	@Transactional
	public InquiryCreatedResponse create(Long userId, Long patternId, CreateInquiryRequest req) {
		requireApprovedPattern(patternId);
		String content = normalizeContent(req.content());
		PatternInquiry q = PatternInquiry.create(patternId, userId, content, req.isPrivate());
		inquiryRepository.save(q);
		return new InquiryCreatedResponse(q.getId());
	}

	/**
	 * 도안 상세 문의 목록. 비로그인 허용. 비공개 문의는 작성자·판매자·관리자만 본문을 본다.
	 * 권한 없는 사용자에게는 locked 로 존재만 노출한다.
	 */
	@Transactional(readOnly = true)
	public InquiryListResponse list(Long patternId, Long viewerId, boolean isAdmin) {
		SellingPattern pattern = patternRepository.findById(patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
		Long sellerUserId = pattern.getSeller() != null ? pattern.getSeller().getUserId() : null;
		boolean viewerIsSeller = viewerId != null && viewerId.equals(sellerUserId);

		List<PatternInquiry> rows = inquiryRepository
				.findByPatternIdAndDeletedAtIsNullOrderByCreatedAtDesc(patternId);
		Map<Long, String> nicknames = nicknamesOf(rows.stream().map(PatternInquiry::getUserId).toList());

		List<InquiryItem> items = rows.stream().map(q -> {
			boolean mine = viewerId != null && q.isOwnedBy(viewerId);
			boolean canView = !q.isPrivate() || mine || viewerIsSeller || isAdmin;
			boolean locked = q.isPrivate() && !canView;
			String nickname = locked ? "비공개" : nicknames.getOrDefault(q.getUserId(), "탈퇴한 사용자");
			return new InquiryItem(
					q.getId(), nickname, q.isPrivate(), locked,
					locked ? null : q.getContent(),
					locked ? null : q.getAnswer(),
					q.isAnswered(), mine, viewerIsSeller,
					q.getCreatedAt(), q.getAnsweredAt());
		}).toList();

		return new InquiryListResponse(items, viewerId != null, viewerIsSeller);
	}

	/** 판매자 답변. 해당 도안의 판매자만. */
	@Transactional
	public void answer(Long inquiryId, Long sellerUserId, AnswerInquiryRequest req) {
		PatternInquiry q = activeInquiry(inquiryId);
		SellingPattern pattern = patternRepository.findById(q.getPatternId())
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
		Long ownerSeller = pattern.getSeller() != null ? pattern.getSeller().getUserId() : null;
		if (ownerSeller == null || !ownerSeller.equals(sellerUserId)) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, "이 도안의 판매자만 답변할 수 있습니다.");
		}
		q.answer(normalizeContent(req.answer()), sellerUserId);
	}

	/** 작성자 문의 수정 — 미답변일 때만. */
	@Transactional
	public void update(Long inquiryId, Long userId, UpdateInquiryRequest req) {
		PatternInquiry q = activeInquiry(inquiryId);
		if (!q.isOwnedBy(userId)) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, "본인 문의만 수정할 수 있습니다.");
		}
		if (q.isAnswered()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "답변이 달린 문의는 수정할 수 없습니다.");
		}
		q.editContent(normalizeContent(req.content()), req.isPrivate());
	}

	/** 문의 삭제 — 작성자 또는 관리자. 논리 삭제. */
	@Transactional
	public void delete(Long inquiryId, Long userId, boolean isAdmin) {
		PatternInquiry q = activeInquiry(inquiryId);
		if (!q.isOwnedBy(userId) && !isAdmin) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, "본인 문의만 삭제할 수 있습니다.");
		}
		q.softDelete();
	}

	/** 판매자 인박스(미답변 = 알림). onlyUnanswered 로 필터. */
	@Transactional(readOnly = true)
	public SellerInboxResponse sellerInbox(Long sellerUserId, boolean onlyUnanswered) {
		List<SellerInquiryItem> items = inquiryRepository.findSellerInbox(sellerUserId, onlyUnanswered).stream()
				.map(r -> new SellerInquiryItem(r.id(), r.patternId(), r.patternTitle(), r.askerNickname(),
						r.isPrivate(), r.content(), r.answer(), r.answeredAt() != null, r.createdAt(), r.answeredAt()))
				.toList();
		long unanswered = inquiryRepository.countUnansweredForSeller(sellerUserId);
		return new SellerInboxResponse(items, unanswered);
	}

	// ---------------------------------------------------------------- 내부

	private void requireApprovedPattern(Long patternId) {
		patternRepository.findByIdAndProductStatus(patternId, ProductStatus.APPROVED)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
	}

	private PatternInquiry activeInquiry(Long inquiryId) {
		return inquiryRepository.findById(inquiryId)
				.filter(x -> !x.isDeleted())
				.orElseThrow(() -> new ApiException(ErrorCode.INQUIRY_NOT_FOUND, "문의를 찾을 수 없습니다."));
	}

	private String normalizeContent(String raw) {
		String c = raw == null ? "" : raw.trim();
		if (c.isEmpty()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "내용을 입력해주세요.");
		}
		if (c.length() > MAX_CONTENT) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "내용은 " + MAX_CONTENT + "자 이내여야 합니다.");
		}
		return c;
	}

	private Map<Long, String> nicknamesOf(List<Long> userIds) {
		Map<Long, String> map = new HashMap<>();
		if (userIds.isEmpty()) {
			return map;
		}
		for (User u : userRepository.findAllById(userIds)) {
			map.put(u.getId(), u.getNickname());
		}
		return map;
	}
}
