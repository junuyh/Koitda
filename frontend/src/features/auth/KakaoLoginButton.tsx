"use client";

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { authApi } from "@/features/auth/api";

/** 카카오로 계속하기 버튼. 서버에 카카오 설정이 없으면(available=false) 렌더하지 않는다. */
export function KakaoLoginButton({ label = "카카오로 계속하기" }: { label?: string }) {
  const [loading, setLoading] = useState(false);
  const { data } = useQuery({
    queryKey: ["kakao-available"],
    queryFn: () => authApi.kakaoAvailable(),
    staleTime: 5 * 60 * 1000,
  });

  if (!data?.available) return null;

  const start = async () => {
    setLoading(true);
    try {
      const { authorizeUrl } = await authApi.kakaoAuthorizeUrl();
      window.location.href = authorizeUrl; // 카카오 인가 화면으로 이동
    } catch (e) {
      window.alert(e instanceof ApiError ? e.message : "카카오 로그인을 시작하지 못했습니다.");
      setLoading(false);
    }
  };

  return (
    <button type="button" onClick={start} disabled={loading}
      className="flex w-full items-center justify-center gap-2 rounded-lg bg-[#FEE500] px-3 py-2.5 text-sm font-medium text-[#191600] disabled:opacity-60">
      <span aria-hidden className="text-base leading-none">💬</span>
      {loading ? "이동 중…" : label}
    </button>
  );
}
