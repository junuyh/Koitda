"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { reviewApi, type MyReview } from "@/features/review/api";

const STATUS_LABEL: Record<string, string> = {
  PLANNED: "준비 중", CO: "코잡기", WIP: "뜨는 중", UFO: "잠시 멈춤", FO: "완성",
};

export default function MyReviewsPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["my-reviews"], queryFn: reviewApi.mine, retry: false });

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <Link href="/me" className="text-sm font-bold text-neutral-500 hover:underline">← 마이페이지</Link>
      <h1 className="mt-2 text-3xl font-black tracking-tight">내 리뷰</h1>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-400">로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link></p>
      ) : !data || data.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-500">아직 작성한 리뷰가 없어요. 구매한 도안에 리뷰를 남겨보세요.</p>
      ) : (
        <ul className="mt-5 space-y-3">
          {data.map((r) => <li key={r.id}><ReviewCard r={r} /></li>)}
        </ul>
      )}
    </main>
  );
}

function ReviewCard({ r }: { r: MyReview }) {
  return (
    <Link href={`/patterns/${r.patternId}`}
      className="block rounded-2xl border-2 border-neutral-900 p-4 transition hover:-translate-y-0.5 hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:hover:shadow-[3px_3px_0_0_rgba(255,255,255,0.9)]">
      <div className="flex items-center justify-between gap-2">
        <div className="flex min-w-0 items-center gap-2">
          {r.knittingStatus && (
            <span className="shrink-0 rounded-full border border-neutral-900 bg-emerald-300 px-2 py-0.5 text-[11px] font-bold text-emerald-950 dark:border-neutral-100">
              {STATUS_LABEL[r.knittingStatus] ?? r.knittingStatus}
            </span>
          )}
          <span className="line-clamp-1 text-sm font-bold">{r.patternTitle}</span>
        </div>
        <div className="flex shrink-0 items-center gap-2 text-xs text-neutral-400">
          {r.visibility !== "PUBLIC" && <span>비공개</span>}
          <span>♥ {r.likeCount}</span>
        </div>
      </div>
      {(r.title || r.contentText) && (
        <p className="mt-1.5 line-clamp-2 text-sm text-neutral-600 dark:text-neutral-300">{r.title || r.contentText}</p>
      )}
      <p className="mt-1 text-xs text-neutral-400">{new Date(r.createdAt).toLocaleDateString("ko-KR")}</p>
    </Link>
  );
}
