"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { authApi } from "@/features/auth/api";

export default function HomePage() {
  const queryClient = useQueryClient();

  // 401 이면 로그아웃 상태로 간주한다(retry:false 는 Providers 에서 전역 설정).
  const { data: me, isLoading } = useQuery({ queryKey: ["me"], queryFn: authApi.me });

  const logout = useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => queryClient.setQueryData(["me"], null),
  });

  return (
    <main className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-md text-center">
        <h1 className="text-3xl font-semibold tracking-tight">코잇다</h1>
        <p className="mt-2 text-sm text-neutral-500">
          도안 탐색부터 제작 기록·공유까지, 뜨개 통합 플랫폼
        </p>

        <div className="mt-8">
          {isLoading ? (
            <p className="text-sm text-neutral-400">불러오는 중…</p>
          ) : me ? (
            <div className="space-y-4">
              <p className="text-sm">
                <span className="font-medium">{me.nickname}</span>님, 반갑습니다.
              </p>
              <p className="text-xs text-neutral-500">
                역할: {me.roles.join(", ")} · 포인트: {me.pointBalance.toLocaleString()}P
              </p>
              <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
                <Link
                  href="/patterns"
                  className="rounded-md bg-neutral-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-neutral-700 dark:bg-neutral-100 dark:text-neutral-900 dark:hover:bg-neutral-300"
                >
                  도안 둘러보기
                </Link>
                <Link
                  href="/projects"
                  className="rounded-md border border-neutral-300 px-5 py-2.5 text-sm font-medium hover:bg-neutral-100 dark:border-neutral-700 dark:hover:bg-neutral-900"
                >
                  내 니팅로그
                </Link>
                <button
                  type="button"
                  onClick={() => logout.mutate()}
                  disabled={logout.isPending}
                  className="rounded-md border border-neutral-300 px-5 py-2.5 text-sm font-medium hover:bg-neutral-100 dark:border-neutral-700 dark:hover:bg-neutral-900"
                >
                  로그아웃
                </button>
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
              <Link
                href="/patterns"
                className="rounded-md bg-neutral-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-neutral-700 dark:bg-neutral-100 dark:text-neutral-900 dark:hover:bg-neutral-300"
              >
                도안 둘러보기
              </Link>
              <Link
                href="/login"
                className="rounded-md border border-neutral-300 px-5 py-2.5 text-sm font-medium hover:bg-neutral-100 dark:border-neutral-700 dark:hover:bg-neutral-900"
              >
                로그인
              </Link>
              <Link
                href="/signup"
                className="rounded-md border border-neutral-300 px-5 py-2.5 text-sm font-medium hover:bg-neutral-100 dark:border-neutral-700 dark:hover:bg-neutral-900"
              >
                회원가입
              </Link>
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
