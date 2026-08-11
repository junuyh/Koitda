"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { orderApi } from "@/features/order/api";

export default function LibraryPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["library"], queryFn: orderApi.myLibrary });

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">구매 도안</h1>
        <Link href="/patterns" className="text-sm text-neutral-500 hover:underline">도안 둘러보기</Link>
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-500">
          로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link>
        </p>
      ) : !data || data.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-400">구매한 도안이 없습니다.</p>
      ) : (
        <ul className="divide-y divide-neutral-200 dark:divide-neutral-800">
          {data.map((it) => (
            <li key={it.patternId} className="flex items-center justify-between gap-3 py-4">
              <Link href={`/patterns/${it.patternId}`} className="min-w-0 hover:opacity-80">
                <p className="truncate text-sm font-medium">{it.patternTitle}</p>
                <p className="truncate text-xs text-neutral-500">
                  {it.categoryName ?? "도안"} · 구매일 {it.purchasedAt.slice(0, 10)}
                </p>
              </Link>
              {it.revoked && (
                <span className="shrink-0 rounded-full bg-neutral-100 px-2.5 py-1 text-xs text-neutral-500 dark:bg-neutral-800">
                  환불됨
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
