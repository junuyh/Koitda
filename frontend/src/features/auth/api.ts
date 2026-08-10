import { apiFetch } from "@/lib/api";
import type { LoginInput, SignupInput } from "./schemas";

export type AuthUser = { id: number; nickname: string; roles: string[] };

export type Me = {
  id: number;
  email: string;
  nickname: string;
  intro: string | null;
  pointBalance: number;
  roles: string[];
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

export const authApi = {
  signup: (input: SignupInput) =>
    apiFetch<AuthUser>("/auth/signup", { method: "POST", body: toSignupPayload(input) }),
  login: (input: LoginInput) =>
    apiFetch<AuthUser>("/auth/login", { method: "POST", body: input }),
  logout: () => apiFetch<null>("/auth/logout", { method: "POST" }),
  me: () => apiFetch<Me>("/users/me"),
};
