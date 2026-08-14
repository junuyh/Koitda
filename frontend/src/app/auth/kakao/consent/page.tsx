"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { authApi, type TermsAgreementPayload } from "@/features/auth/api";
import { ConsentAgreements, type ConsentState } from "@/features/auth/ConsentAgreements";
import { inputClass, buttonClass } from "@/components/form";

/**
 * 카카오 신규 회원 개인정보 동의 화면. 티켓·추천 닉네임은 콜백 화면이 sessionStorage 로 넘긴다.
 * 필수 약관 동의 + 닉네임 확정 후 계정을 생성하고 로그인한다.
 */
export default function KakaoConsentPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [ticket, setTicket] = useState<string | null>(null);
  const [nickname, setNickname] = useState("");
  const [consent, setConsent] = useState<ConsentState>({ service: false, privacy: false, marketing: false });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const t = sessionStorage.getItem("kakao_ticket");
    if (!t) {
      router.replace("/login");
      return;
    }
    setTicket(t);
    setNickname(sessionStorage.getItem("kakao_nickname") ?? "");
  }, [router]);

  const complete = useMutation({
    mutationFn: () => {
      const agreements: TermsAgreementPayload[] = [
        { termsType: "SERVICE", termsVersion: "1.0", agreed: consent.service },
        { termsType: "PRIVACY", termsVersion: "1.0", agreed: consent.privacy },
        { termsType: "MARKETING", termsVersion: "1.0", agreed: consent.marketing },
      ];
      return authApi.kakaoComplete(ticket ?? "", nickname.trim(), agreements);
    },
    onSuccess: async () => {
      sessionStorage.removeItem("kakao_ticket");
      sessionStorage.removeItem("kakao_nickname");
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      router.replace("/");
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "가입 처리 중 오류가 발생했습니다."),
  });

  const canSubmit = consent.service && consent.privacy && nickname.trim().length >= 2 && !complete.isPending;

  return (
    <main className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <h1 className="text-2xl font-semibold tracking-tight">카카오로 시작하기</h1>
        <p className="mt-1 text-sm text-neutral-500">코잇다 이용을 위해 아래 항목에 동의해주세요.</p>

        <form className="mt-8 space-y-5"
          onSubmit={(e) => { e.preventDefault(); if (canSubmit) complete.mutate(); }}>
          <div>
            <label htmlFor="nickname" className="mb-1 block text-sm font-medium">닉네임</label>
            <input id="nickname" value={nickname} onChange={(e) => setNickname(e.target.value)}
              className={inputClass} placeholder="2자 이상" />
          </div>

          <ConsentAgreements onChange={setConsent} />

          {error && <p role="alert" className="text-sm text-red-600">{error}</p>}

          <button type="submit" disabled={!canSubmit} className={buttonClass}>
            {complete.isPending ? "가입 중…" : "동의하고 시작하기"}
          </button>
        </form>
      </div>
    </main>
  );
}
