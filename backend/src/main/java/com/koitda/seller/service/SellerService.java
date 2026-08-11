package com.koitda.seller.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.seller.domain.ApplicationStatus;
import com.koitda.seller.domain.SellerApplication;
import com.koitda.seller.domain.SellerProfile;
import com.koitda.seller.dto.SellerDtos.SellerApplicationRequest;
import com.koitda.seller.dto.SellerDtos.SellerApplicationResponse;
import com.koitda.seller.repository.SellerApplicationRepository;
import com.koitda.seller.repository.SellerProfileRepository;
import com.koitda.user.domain.RoleType;
import com.koitda.user.domain.User;
import com.koitda.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerService {

	private final SellerApplicationRepository applicationRepository;
	private final SellerProfileRepository profileRepository;
	private final UserRepository userRepository;

	public SellerService(SellerApplicationRepository applicationRepository,
			SellerProfileRepository profileRepository, UserRepository userRepository) {
		this.applicationRepository = applicationRepository;
		this.profileRepository = profileRepository;
		this.userRepository = userRepository;
	}

	/** 판매자 신청(SELLER-001). */
	@Transactional
	public SellerApplicationResponse apply(Long userId, SellerApplicationRequest req) {
		if (profileRepository.existsByUserId(userId)) {
			throw new ApiException(ErrorCode.APPLICATION_ALREADY_PENDING, "이미 판매자입니다.");
		}
		if (applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING)) {
			throw new ApiException(ErrorCode.APPLICATION_ALREADY_PENDING, "이미 심사 중인 신청이 있습니다.");
		}
		SellerApplication app = applicationRepository.save(SellerApplication.create(
				userId, req.brandName(), req.businessType(), req.businessNo(), req.representativeName(),
				req.settlementBank(), req.settlementAccount(), req.termsVersion()));
		return new SellerApplicationResponse(app.getId(), app.getStatus().name());
	}

	/** 관리자 승인(ADMIN-001) — 판매자 프로필 생성 + SELLER 역할 부여(겸직). */
	@Transactional
	public void approve(Long reviewerId, Long applicationId) {
		SellerApplication app = pendingApplication(applicationId);
		if (!profileRepository.existsByUserId(app.getUserId())) {
			profileRepository.save(SellerProfile.create(app.getUserId(), app.getBrandName()));
		}
		User applicant = userRepository.findById(app.getUserId())
				.orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND, "신청자를 찾을 수 없습니다."));
		applicant.addRole(RoleType.SELLER);
		userRepository.save(applicant);
		app.approve(reviewerId);
	}

	/** 관리자 반려. */
	@Transactional
	public void reject(Long reviewerId, Long applicationId, String reason) {
		SellerApplication app = pendingApplication(applicationId);
		app.reject(reviewerId, reason);
	}

	private SellerApplication pendingApplication(Long applicationId) {
		SellerApplication app = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND, "신청을 찾을 수 없습니다."));
		if (app.getStatus() != ApplicationStatus.PENDING) {
			throw new ApiException(ErrorCode.INVALID_APPLICATION_STATE, "이미 처리된 신청입니다.");
		}
		return app;
	}
}
