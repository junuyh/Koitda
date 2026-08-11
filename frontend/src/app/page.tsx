"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { authApi } from "@/features/auth/api";

type Shortcut = { href: string; title: string; desc: string; role?: "SELLER" | "ADMIN" };

const SHORTCUTS: Shortcut[] = [
  { href: "/patterns", title: "도안 둘러보기", desc: "원하는 도안을 검색하고 위시에 담아요." },
  { href: "/projects", title: "내 니팅로그", desc: "제작 과정을 기록하고 상태를 관리해요." },
  { href: "/library", title: "구매 도안", desc: "구매한 도안을 확인하고 다운로드해요." },
  { href: "/seller/patterns", title: "판매 도안 관리", desc: "도안을 등록·제출하고 판매 상태를 봐요.", role: "SELLER" },
  { href: "/admin", title: "심사 콘솔", desc: "판매자·도안 등록을 승인·반려해요.", role: "ADMIN" },
];

export default function HomePage() {
  const { data: me, isLoading } = useQuery({ queryKey: ["me"], queryFn: authApi.me });
  const roles: string[] = me?.roles ?? [];
  const shortcuts = SHORTCUTS.filter((s) => !s.role || roles.includes(s.role));

  return (
    <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-12">
      <section className="text-center">
        <h1 className="text-4xl font-semibold tracking-tight">코잇다</h1>
        <p className="mt-3 text-sm text-neutral-500">
          도안 탐색부터 제작 기록·공유까지, 뜨개 통합 플랫폼
        </p>

        {isLoading ? (
          <p className="mt-6 text-sm text-neutral-400">불러오는 중…</p>
        ) : me ? (
          <p className="mt-5 text-sm text-neutral-600 dark:text-neutral-300">
            <span className="font-medium">{me.nickname}</span>님, 반갑습니다 · 포인트 {me.pointBalance.toLocaleString()}P
          </p>
        ) : (
          <div className="mt-6 flex justify-center gap-3">
            <Link href="/patterns" className="rounded-md bg-neutral-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-neutral-700 dark:bg-neutral-100 dark:text-neutral-900">
              도안 둘러보기
            </Link>
            <Link href="/login" className="rounded-md border border-neutral-300 px-5 py-2.5 text-sm font-medium hover:bg-neutral-100 dark:border-neutral-700 dark:hover:bg-neutral-900">
              로그인
            </Link>
          </div>
        )}
      </section>

      {me && (
        <section className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {shortcuts.map((s) => (
            <Link key={s.href} href={s.href}
              className="rounded-lg border border-neutral-200 p-5 transition hover:border-neutral-400 dark:border-neutral-800 dark:hover:border-neutral-600">
              <h2 className="text-base font-semibold">{s.title}</h2>
              <p className="mt-1.5 text-sm text-neutral-500">{s.desc}</p>
            </Link>
          ))}
          {!roles.includes("SELLER") && (
            <Link href="/seller/apply"
              className="rounded-lg border border-dashed border-neutral-300 p-5 transition hover:border-neutral-500 dark:border-neutral-700">
              <h2 className="text-base font-semibold">판매자 신청</h2>
              <p className="mt-1.5 text-sm text-neutral-500">내 도안을 코잇다에서 판매해 보세요.</p>
            </Link>
          )}
        </section>
      )}
    </main>
  );
}
