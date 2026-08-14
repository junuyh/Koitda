"use client";

import { useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api";
import { authApi } from "@/features/auth/api";

/**
 * 카카오 인가 후 리다이렉트되는 콜백 화면. code+state 를 백엔드에 넘겨
 * 기존 회원이면 바로 로그인, 신규면 개인정보 동의 화면으로 보낸다.
 */
export default function KakaoCallbackPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return; // 개발 모드 이중 실행 방지(code 는 1회용)
    ran.current = true;

    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    const state = params.get("state");
    const kakaoError = params.get("error");

    if (kakaoError || !code || !state) {
      setError("카카오 로그인이 취소되었거나 잘못된 요청입니다.");
      return;
    }

    (async () => {
      try {
        const result = await authApi.kakaoCallback(code, state);
        if (result.status === "LOGGED_IN") {
          await queryClient.invalidateQueries({ queryKey: ["me"] });
          router.replace("/");
          return;
        }
        // 신규 회원 → 동의 화면으로. 티켓·추천 닉네임은 sessionStorage 로 전달(URL 노출 회피).
        sessionStorage.setItem("kakao_ticket", result.ticket ?? "");
        sessionStorage.setItem("kakao_nickname", result.suggestedNickname ?? "");
        router.replace("/auth/kakao/consent");
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "카카오 로그인 처리 중 오류가 발생했습니다.");
      }
    })();
  }, [router, queryClient]);

  return (
    <main className="flex flex-1 items-center justify-center px-4 py-16 text-center">
      {error ? (
        <div>
          <p className="text-sm text-red-600">{error}</p>
          <Link href="/login" className="mt-3 inline-block text-sm underline">로그인 화면으로</Link>
        </div>
      ) : (
        <p className="text-sm text-neutral-500">카카오 로그인 처리 중…</p>
      )}
    </main>
  );
}
