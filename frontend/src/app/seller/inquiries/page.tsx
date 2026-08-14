"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { inquiryApi, type SellerInquiryItem } from "@/features/inquiry/api";

/** 판매자 인박스(알림) — 내 도안에 달린 문의. 미답변 우선, 여기서 바로 답변한다. */
export default function SellerInquiriesPage() {
  const [onlyUnanswered, setOnlyUnanswered] = useState(true);
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["seller-inbox", onlyUnanswered],
    queryFn: () => inquiryApi.sellerInbox(onlyUnanswered),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["seller-inbox"] });
    queryClient.invalidateQueries({ queryKey: ["seller-inbox-count"] });
  };

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <div className="flex items-center justify-between">
        <div>
          <Link href="/seller/patterns" className="text-sm text-neutral-500 hover:underline">← 내 도안</Link>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">
            문의 {data ? `· 미답변 ${data.unansweredCount}` : ""}
          </h1>
        </div>
        <label className="flex items-center gap-2 text-sm text-neutral-600 dark:text-neutral-300">
          <input type="checkbox" checked={onlyUnanswered} onChange={(e) => setOnlyUnanswered(e.target.checked)}
            className="h-4 w-4 accent-neutral-900 dark:accent-neutral-100" />
          미답변만 보기
        </label>
      </div>

      <div className="mt-6">
        {isLoading ? (
          <p className="py-10 text-center text-sm text-neutral-400">불러오는 중…</p>
        ) : isError ? (
          <p className="py-10 text-center text-sm text-neutral-400">불러오지 못했습니다. 판매자만 접근할 수 있습니다.</p>
        ) : !data || data.items.length === 0 ? (
          <p className="py-12 text-center text-sm text-neutral-500">
            {onlyUnanswered ? "미답변 문의가 없습니다. 👍" : "아직 문의가 없습니다."}
          </p>
        ) : (
          <ul className="space-y-3">
            {data.items.map((q) => <InboxRow key={q.id} q={q} onChanged={invalidate} />)}
          </ul>
        )}
      </div>
    </main>
  );
}

function InboxRow({ q, onChanged }: { q: SellerInquiryItem; onChanged: () => void }) {
  const [answer, setAnswer] = useState("");
  const [open, setOpen] = useState(!q.answered);

  const submit = useMutation({
    mutationFn: () => inquiryApi.answer(q.id, answer.trim()),
    onSuccess: async () => { setAnswer(""); onChanged(); },
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "답변 등록 중 오류가 발생했습니다."),
  });

  return (
    <li className="rounded-lg border-2 border-neutral-900 p-4 dark:border-neutral-100">
      <div className="flex items-center justify-between gap-2">
        <Link href={`/patterns/${q.patternId}`} className="truncate text-sm font-semibold hover:underline">
          {q.patternTitle || "(제목 없음)"}
        </Link>
        <div className="flex shrink-0 items-center gap-2">
          {q.isPrivate && <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-500 dark:bg-neutral-800 dark:text-neutral-400">🔒 비공개</span>}
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${q.answered ? "bg-green-100 text-green-700 dark:bg-green-950/40 dark:text-green-300" : "bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-300"}`}>
            {q.answered ? "답변완료" : "미답변"}
          </span>
        </div>
      </div>
      <p className="mt-1 text-xs text-neutral-400">{q.askerNickname} · {new Date(q.createdAt).toLocaleDateString("ko-KR")}</p>
      <p className="mt-2 whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{q.content}</p>

      {q.answered && q.answer ? (
        <div className="mt-3 rounded-lg bg-neutral-50 p-3 dark:bg-neutral-900">
          <p className="text-xs font-semibold text-amber-700 dark:text-amber-400">내 답변</p>
          <p className="mt-1 whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{q.answer}</p>
        </div>
      ) : open ? (
        <form className="mt-3 space-y-2" onSubmit={(e) => { e.preventDefault(); if (answer.trim()) submit.mutate(); }}>
          <textarea value={answer} onChange={(e) => setAnswer(e.target.value)} rows={2} autoFocus
            placeholder="답변을 입력하세요."
            className="w-full resize-none rounded-lg border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100" />
          <button type="submit" disabled={submit.isPending || !answer.trim()}
            className="rounded-md bg-neutral-900 px-4 py-1.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
            {submit.isPending ? "등록 중…" : "답변 등록"}
          </button>
        </form>
      ) : (
        <button type="button" onClick={() => setOpen(true)}
          className="mt-3 text-sm text-neutral-500 hover:underline">답변하기</button>
      )}
    </li>
  );
}
