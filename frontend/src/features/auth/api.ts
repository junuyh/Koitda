import { apiFetch } from "@/lib/api";
import type { LoginInput, SignupInput } from "./schemas";

export type AuthUser = { id: number; nickname: string; roles: string[] };

export type Me = {
  id: number;
  email: string | null; // 카카오(이메일 미동의) 회원은 이메일이 없다
  nickname: string;
  intro: string | null;
  pointBalance: number;
  roles: string[];
  hasPassword: boolean; // 소셜 전용 회원은 false → 비밀번호 변경 UI 숨김
};

// 화면 입력을 서버 계약(agreements 배열)으로 변환한다.
function toSignupPayload(input: SignupInput) {
  return {
    email: input.email,
    password: input.password,
    nickname: input.nickname,
    agreements: [
      { termsType: "SERVICE", termsVersion: "1.0", agreed: input.agreeService },
      { termsType: "PRIVACY", termsVersion: "1.0", agreed: input.agreePrivacy },
      { termsType: "MARKETING", termsVersion: "1.0", agreed: input.agreeMarketing },
    ],
  };
}

export type TermsAgreementPayload = { termsType: string; termsVersion: string; agreed: boolean };

// 카카오 콜백 결과: 기존 회원이면 로그인 완료, 신규면 개인정보 동의 필요.
export type KakaoCallbackResult = {
  status: "LOGGED_IN" | "CONSENT_REQUIRED";
  user: AuthUser | null;
  ticket: string | null;
  suggestedNickname: string | null;
  email: string | null;
};

export const authApi = {
  signup: (input: SignupInput) =>
    apiFetch<AuthUser>("/auth/signup", { method: "POST", body: toSignupPayload(input) }),
  login: (input: LoginInput) =>
    apiFetch<AuthUser>("/auth/login", { method: "POST", body: input }),
  logout: () => apiFetch<null>("/auth/logout", { method: "POST" }),
  me: () => apiFetch<Me>("/users/me"),

  // --- 카카오 소셜 로그인 ---
  kakaoAvailable: () => apiFetch<{ available: boolean }>("/auth/kakao/available"),
  kakaoAuthorizeUrl: () => apiFetch<{ authorizeUrl: string }>("/auth/kakao/authorize-url"),
  kakaoCallback: (code: string, state: string) =>
    apiFetch<KakaoCallbackResult>("/auth/kakao/callback", { method: "POST", body: { code, state } }),
  kakaoComplete: (ticket: string, nickname: string, agreements: TermsAgreementPayload[]) =>
    apiFetch<KakaoCallbackResult>("/auth/kakao/complete", {
      method: "POST",
      body: { ticket, nickname, agreements },
    }),
};
