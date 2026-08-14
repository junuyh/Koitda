package com.koitda.inquiry.repository;

import com.koitda.inquiry.domain.PatternInquiry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatternInquiryRepository extends JpaRepository<PatternInquiry, Long> {

	/** 도안별 문의 목록(최신순, 미삭제). 공개/비공개 마스킹은 서비스에서 처리한다. */
	List<PatternInquiry> findByPatternIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long patternId);

	/**
	 * 판매자 인박스: 내 도안들에 달린 문의(최신순). 도안→판매자 조인(selling_pattern.seller.userId).
	 * onlyUnanswered=true 면 미답변만.
	 */
	@Query("""
			SELECT new com.koitda.inquiry.repository.SellerInquiryRow(
			    q.id, q.patternId, p.title, q.content, q.answer, q.isPrivate,
			    q.answeredAt, q.createdAt, u.nickname)
			FROM PatternInquiry q
			  JOIN SellingPattern p ON p.id = q.patternId
			  JOIN User u ON u.id = q.userId
			WHERE p.seller.userId = :sellerUserId
			  AND q.deletedAt IS NULL
			  AND (:onlyUnanswered = false OR q.answeredAt IS NULL)
			ORDER BY q.createdAt DESC
			""")
	List<SellerInquiryRow> findSellerInbox(@Param("sellerUserId") Long sellerUserId,
			@Param("onlyUnanswered") boolean onlyUnanswered);

	/** 판매자 미답변 문의 개수(배지). */
	@Query("""
			SELECT COUNT(q)
			FROM PatternInquiry q JOIN SellingPattern p ON p.id = q.patternId
			WHERE p.seller.userId = :sellerUserId AND q.deletedAt IS NULL AND q.answeredAt IS NULL
			""")
	long countUnansweredForSeller(@Param("sellerUserId") Long sellerUserId);
}
