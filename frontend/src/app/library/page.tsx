"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { orderApi, type LibraryItem } from "@/features/order/api";

export default function LibraryPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["library"], queryFn: orderApi.myLibrary });
  const items = data ?? [];

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <div className="mb-6 flex items-end justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-neutral-400">Purchases</p>
          <h1 className="mt-1 text-4xl font-black tracking-tight">구매 내역</h1>
        </div>
        <Link href="/wishlist" className="text-sm font-bold text-neutral-500 hover:underline">위시리스트</Link>
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-500">로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link></p>
      ) : items.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-400">구매한 도안이 없습니다. <Link href="/patterns" className="underline">도안 둘러보기</Link></p>
      ) : (
        <ul className="space-y-4">
          {items.map((it) => <li key={it.patternId}><OrderCard it={it} /></li>)}
        </ul>
      )}
    </main>
  );
}

function OrderCard({ it }: { it: LibraryItem }) {
  const date = it.purchasedAt.slice(0, 10).replace(/-/g, ".");
  return (
    <div className="overflow-hidden rounded-2xl border-2 border-neutral-900 bg-white dark:border-neutral-100 dark:bg-neutral-950">
      <div className="flex items-center justify-between border-b-2 border-dashed border-neutral-200 px-5 py-3 dark:border-neutral-800">
        <p className="text-sm font-black">{date} 구매</p>
        <Link href={`/library/${it.patternId}`} className="text-sm font-bold text-orange-500 hover:underline">주문 상세보기 ›</Link>
      </div>
      <div className="p-5">
        <p className="text-sm font-bold text-neutral-500">
          {it.revoked ? "환불 완료" : "구매 완료"} · {date}
        </p>
        <Link href={`/patterns/${it.patternId}`} className="mt-3 block">
          <p className="text-lg font-black">{it.patternTitle}</p>
          <p className="mt-0.5 text-sm text-neutral-500">{it.categoryName ?? "도안"} · 수량 1개</p>
        </Link>

        <div className="mt-4 grid grid-cols-3 gap-2">
          <button type="button" disabled title="곧 제공"
            className="rounded-full border-2 border-neutral-300 py-2 text-sm font-bold text-neutral-400 dark:border-neutral-700">문의하기</button>
          <Link href={`/patterns/${it.patternId}/reviews/new`}
            className="rounded-full border-2 border-neutral-900 py-2 text-center text-sm font-bold dark:border-neutral-100">후기 작성</Link>
          {it.revoked ? (
            <span className="rounded-full border-2 border-neutral-300 py-2 text-center text-sm font-bold text-neutral-400 dark:border-neutral-700">다운로드</span>
          ) : (
            <Link href={`/library/${it.patternId}`}
              className="rounded-full border-2 border-orange-500 py-2 text-center text-sm font-bold text-orange-500 transition hover:bg-orange-50 dark:hover:bg-orange-950/30">다운로드</Link>
          )}
        </div>
      </div>
    </div>
  );
}
