"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { reviewApi, type CreateReviewBody } from "@/features/review/api";

const STATUS_LABEL: Record<string, string> = {
  PLANNED: "준비 중", CO: "코잡기", WIP: "뜨는 중", UFO: "잠시 멈춤", FO: "완성",
};

const inputClass =
  "w-full rounded-md border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100";

export default function NewReviewPage() {
  const params = useParams<{ id: string }>();
  const patternId = Number(params.id);
  const router = useRouter();
  const queryClient = useQueryClient();

  const [sourcePostId, setSourcePostId] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [contentText, setContentText] = useState("");
  const [rating, setRating] = useState<number>(5);
  const [visibility, setVisibility] = useState<"PUBLIC" | "PRIVATE">("PUBLIC");
  const [error, setError] = useState<string | null>(null);

  const { data: logs } = useQuery({
    queryKey: ["loadable-logs", patternId],
    queryFn: () => reviewApi.loadableLogs(patternId),
  });

  const create = useMutation({
    mutationFn: (body: CreateReviewBody) => reviewApi.create(patternId, body),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: ["reviews", patternId] });
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      await queryClient.invalidateQueries({ queryKey: ["point-history"] });
      if (res.earnedPoint > 0) {
        window.alert(`리뷰가 등록되고 ${res.earnedPoint.toLocaleString()}P가 적립되었습니다! (잔액 ${res.pointBalance.toLocaleString()}P)`);
      }
      router.push(`/patterns/${patternId}`);
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "리뷰 등록 중 오류가 발생했습니다."),
  });

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!title.trim() && !contentText.trim() && sourcePostId == null) {
      setError("제목이나 내용을 입력하거나, 불러올 로그를 선택하세요.");
      return;
    }
    create.mutate({
      sourcePostId: sourcePostId ?? undefined,
      title: title.trim() || undefined,
      contentText: contentText.trim() || undefined,
      rating,
      visibility,
    });
  }

  return (
    <main className="mx-auto w-full max-w-xl flex-1 px-4 py-8">
      <Link href={`/patterns/${patternId}`} className="text-xs text-neutral-500 hover:underline">← 도안으로</Link>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">리뷰 쓰기</h1>
      <p className="mt-1 text-sm text-neutral-500">최초 등록 시 포인트가 적립됩니다. 오늘의 로그를 불러오거나 새로 작성하세요.</p>

      <form className="mt-6 space-y-5" onSubmit={submit} noValidate>
        {logs && logs.length > 0 && (
          <section>
            <h2 className="mb-2 text-sm font-semibold">오늘의 로그 불러오기 (선택)</h2>
            <div className="space-y-2">
              <label className="flex items-center gap-2 text-sm">
                <input type="radio" name="src" checked={sourcePostId == null} onChange={() => setSourcePostId(null)} />
                새로 작성
              </label>
              {logs.map((l) => (
                <label key={l.postId} className="flex items-center gap-2 text-sm">
                  <input type="radio" name="src" checked={sourcePostId === l.postId} onChange={() => setSourcePostId(l.postId)} />
                  <span>{l.displayTitle}</span>
                  {l.knittingStatus && (
                    <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-500 dark:bg-neutral-800">
                      {STATUS_LABEL[l.knittingStatus] ?? l.knittingStatus}
                    </span>
                  )}
                  {l.logDate && <span className="text-xs text-neutral-400">{l.logDate}</span>}
                </label>
              ))}
            </div>
            <p className="mt-1 text-xs text-neutral-400">
              불러온 본문은 복사되어 원본 로그와 독립적으로 유지됩니다.
              {sourcePostId != null && " 선택한 로그의 대표 사진도 리뷰에 함께 등록됩니다. 📷"}
            </p>
          </section>
        )}

        {/* 별점 (REVIEW) */}
        <div>
          <span className="mb-1 block text-xs text-neutral-500">별점</span>
          <div className="flex items-center gap-1" role="radiogroup" aria-label="별점">
            {[1, 2, 3, 4, 5].map((n) => (
              <button key={n} type="button" onClick={() => setRating(n)} aria-label={`${n}점`}
                className={`text-3xl leading-none transition ${n <= rating ? "text-amber-400" : "text-neutral-300 dark:text-neutral-600"}`}>
                ★
              </button>
            ))}
            <span className="ml-2 text-sm font-bold text-neutral-500">{rating}.0</span>
          </div>
        </div>

        <label className="block">
          <span className="mb-1 block text-xs text-neutral-500">제목 {sourcePostId != null && "(비우면 로그 제목 사용)"}</span>
          <input value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} />
        </label>

        <label className="block">
          <span className="mb-1 block text-xs text-neutral-500">내용 {sourcePostId != null && "(비우면 로그 내용 복사)"}</span>
          <textarea value={contentText} onChange={(e) => setContentText(e.target.value)} rows={5} className={inputClass} />
        </label>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={visibility === "PUBLIC"} onChange={(e) => setVisibility(e.target.checked ? "PUBLIC" : "PRIVATE")} className="h-4 w-4" />
          공개 (다른 사용자의 도안 선택에 도움이 됩니다)
        </label>

        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}

        <button type="submit" disabled={create.isPending}
          className="w-full rounded-md bg-neutral-900 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
          {create.isPending ? "등록 중…" : "리뷰 등록 · 포인트 받기"}
        </button>
      </form>
    </main>
  );
}
