"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { inquiryApi, type InquiryItem } from "@/features/inquiry/api";

/** 도안 상세 '문의하기' — 리뷰 하단. 공개/비공개 선택, 판매자 답변, 작성자 삭제. */
export function InquirySection({ patternId, loggedIn }: { patternId: number; loggedIn: boolean }) {
  const queryClient = useQueryClient();
  const key = ["inquiries", patternId];
  const { data, isLoading } = useQuery({ queryKey: key, queryFn: () => inquiryApi.list(patternId) });

  const [content, setContent] = useState("");
  const [isPrivate, setIsPrivate] = useState(false);
  const invalidate = () => queryClient.invalidateQueries({ queryKey: key });

  const create = useMutation({
    mutationFn: () => inquiryApi.create(patternId, content.trim(), isPrivate),
    onSuccess: async () => { setContent(""); setIsPrivate(false); await invalidate(); },
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "문의 등록 중 오류가 발생했습니다."),
  });
  const remove = useMutation({
    mutationFn: (id: number) => inquiryApi.remove(id),
    onSuccess: invalidate,
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "삭제 중 오류가 발생했습니다."),
  });

  return (
    <section className="mt-4 rounded-2xl border-2 border-neutral-900 bg-white p-5 dark:border-neutral-100 dark:bg-neutral-950">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-xs font-semibold uppercase tracking-wider text-neutral-400">
          문의하기 {data ? `(${data.items.length})` : ""}
        </h2>
        {data?.isSeller && (
          <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-950/50 dark:text-amber-300">
            내 도안 · 답변 가능
          </span>
        )}
      </div>

      {/* 작성 폼 */}
      {loggedIn ? (
        <form className="mb-4 space-y-2"
          onSubmit={(e) => { e.preventDefault(); if (content.trim()) create.mutate(); }}>
          <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={3}
            placeholder="도안에 대해 판매자에게 궁금한 점을 물어보세요."
            className="w-full resize-none rounded-lg border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100" />
          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2 text-sm text-neutral-600 dark:text-neutral-300">
              <input type="checkbox" checked={isPrivate} onChange={(e) => setIsPrivate(e.target.checked)}
                className="h-4 w-4 accent-neutral-900 dark:accent-neutral-100" />
              🔒 비공개 (판매자와 나만 볼 수 있어요)
            </label>
            <button type="submit" disabled={create.isPending || !content.trim()}
              className="rounded-md bg-neutral-900 px-4 py-1.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
              {create.isPending ? "등록 중…" : "문의 등록"}
            </button>
          </div>
        </form>
      ) : (
        <p className="mb-4 text-xs text-neutral-400">문의는 로그인 후 작성할 수 있어요.</p>
      )}

      {/* 목록 */}
      {isLoading ? (
        <p className="py-6 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : !data || data.items.length === 0 ? (
        <p className="py-6 text-center text-sm text-neutral-500">아직 문의가 없어요.</p>
      ) : (
        <ul className="space-y-3">
          {data.items.map((q) => (
            <InquiryRow key={q.id} q={q} onDelete={() => {
              if (window.confirm("이 문의를 삭제할까요?")) remove.mutate(q.id);
            }} onChanged={invalidate} />
          ))}
        </ul>
      )}
    </section>
  );
}

function InquiryRow({ q, onDelete, onChanged }: {
  q: InquiryItem; onDelete: () => void; onChanged: () => void;
}) {
  const [answering, setAnswering] = useState(false);
  const [answer, setAnswer] = useState("");

  const submitAnswer = useMutation({
    mutationFn: () => inquiryApi.answer(q.id, answer.trim()),
    onSuccess: async () => { setAnswering(false); setAnswer(""); onChanged(); },
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "답변 등록 중 오류가 발생했습니다."),
  });

  const date = new Date(q.createdAt).toLocaleDateString("ko-KR");

  return (
    <li className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium">{q.askerNickname}</span>
          {q.isPrivate && (
            <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-500 dark:bg-neutral-800 dark:text-neutral-400">
              🔒 비공개
            </span>
          )}
          {q.mine && <span className="text-xs text-neutral-400">내 문의</span>}
          <span className="text-xs text-neutral-400">{date}</span>
        </div>
        {q.mine && (
          <button type="button" onClick={onDelete} className="text-xs text-red-500 hover:underline">삭제</button>
        )}
      </div>

      {q.locked ? (
        <p className="mt-2 text-sm italic text-neutral-400">비공개 문의입니다.</p>
      ) : (
        <p className="mt-2 whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{q.content}</p>
      )}

      {/* 답변 */}
      {q.answered && q.answer && (
        <div className="mt-3 rounded-lg bg-neutral-50 p-3 dark:bg-neutral-900">
          <p className="text-xs font-semibold text-amber-700 dark:text-amber-400">판매자 답변</p>
          <p className="mt-1 whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{q.answer}</p>
        </div>
      )}

      {/* 판매자 답변 입력 (미답변 + 답변 권한) */}
      {q.canAnswer && !q.answered && !q.locked && (
        answering ? (
          <form className="mt-3 space-y-2" onSubmit={(e) => { e.preventDefault(); if (answer.trim()) submitAnswer.mutate(); }}>
            <textarea value={answer} onChange={(e) => setAnswer(e.target.value)} rows={2} autoFocus
              placeholder="답변을 입력하세요."
              className="w-full resize-none rounded-lg border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100" />
            <div className="flex gap-2">
              <button type="submit" disabled={submitAnswer.isPending || !answer.trim()}
                className="rounded-md bg-neutral-900 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">등록</button>
              <button type="button" onClick={() => setAnswering(false)}
                className="rounded-md px-3 py-1.5 text-sm text-neutral-500 hover:underline">취소</button>
            </div>
          </form>
        ) : (
          <button type="button" onClick={() => setAnswering(true)}
            className="mt-3 rounded-md border border-neutral-300 px-3 py-1.5 text-sm text-neutral-600 hover:border-neutral-900 dark:border-neutral-700 dark:text-neutral-300">
            답변하기
          </button>
        )
      )}
    </li>
  );
}
