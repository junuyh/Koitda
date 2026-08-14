"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { reviewApi, type ReviewItem } from "@/features/review/api";
import { socialApi } from "@/features/social/api";

const STATUS_LABEL: Record<string, string> = {
  PLANNED: "준비 중", CO: "코잡기", WIP: "뜨는 중", UFO: "잠시 멈춤", FO: "완성",
};

export function ReviewSection({ patternId, loggedIn }: { patternId: number; loggedIn: boolean }) {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["reviews", patternId],
    queryFn: () => reviewApi.list(patternId),
  });

  const remove = useMutation({
    mutationFn: (reviewId: number) => reviewApi.remove(reviewId),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: ["reviews", patternId] });
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      await queryClient.invalidateQueries({ queryKey: ["point-history"] });
      if (res.revokeFailReason) window.alert(`리뷰를 삭제했지만 포인트는 회수하지 못했습니다.\n${res.revokeFailReason}`);
      else if (res.revokedPoint > 0) window.alert(`리뷰를 삭제하고 ${res.revokedPoint.toLocaleString()}P를 회수했습니다.`);
    },
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "삭제 중 오류가 발생했습니다."),
  });

  const canWrite = loggedIn && data?.purchased && !data?.myReviewId;

  return (
    <section className="mt-4 rounded-2xl border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-950">
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-baseline gap-2">
          <h2 className="text-xs font-semibold uppercase tracking-wider text-neutral-400">리뷰 {data ? `(${data.items.length})` : ""}</h2>
          {(() => {
            const rated = data?.items.filter((r) => r.rating != null) ?? [];
            if (rated.length === 0) return null;
            const avg = rated.reduce((s, r) => s + (r.rating ?? 0), 0) / rated.length;
            return <span className="text-sm font-bold text-amber-500">★ {avg.toFixed(1)} <span className="text-xs font-normal text-neutral-400">({rated.length})</span></span>;
          })()}
        </div>
        {canWrite && (
          <Link href={`/patterns/${patternId}/reviews/new`}
            className="rounded-md bg-neutral-900 px-3 py-1.5 text-sm font-medium text-white dark:bg-neutral-100 dark:text-neutral-900">
            리뷰 쓰기
          </Link>
        )}
        {loggedIn && data && !data.purchased && (
          <span className="text-xs text-neutral-400">구매자만 리뷰를 쓸 수 있어요</span>
        )}
      </div>

      {isLoading ? (
        <p className="py-6 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : !data || data.items.length === 0 ? (
        <p className="py-6 text-center text-sm text-neutral-500">아직 리뷰가 없어요. 첫 리뷰를 남겨보세요.</p>
      ) : (
        <ul className="space-y-3">
          {data.items.map((r) => (
            <Row key={r.id} r={r} patternId={patternId} loggedIn={loggedIn}
              onDelete={() => { if (window.confirm("리뷰를 삭제하면 지급된 포인트가 회수됩니다. 삭제할까요?")) remove.mutate(r.id); }}
              deleting={remove.isPending} />
          ))}
        </ul>
      )}
    </section>
  );
}

function Row({ r, patternId, loggedIn, onDelete, deleting }: {
  r: ReviewItem; patternId: number; loggedIn: boolean; onDelete: () => void; deleting: boolean;
}) {
  const queryClient = useQueryClient();
  const [showComments, setShowComments] = useState(false);
  const [liked, setLiked] = useState(r.liked);
  const [likeCount, setLikeCount] = useState(r.likeCount);

  const like = useMutation({
    mutationFn: () => socialApi.toggleLike("REVIEW", r.id),
    onSuccess: (res) => { setLiked(res.liked); setLikeCount(res.likeCount); },
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "잠시 후 다시 시도해 주세요."),
  });
  const report = useMutation({
    mutationFn: () => socialApi.report("REVIEW", r.id, "INAPPROPRIATE"),
    onSuccess: (res) => window.alert(res.autoHidden ? "신고가 접수되어 자동 숨김되었습니다." : "신고가 접수되었습니다."),
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "신고 중 오류가 발생했습니다."),
  });

  return (
    <li className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium">{r.authorNickname}</span>
          {r.knittingStatus && (
            <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300">
              {STATUS_LABEL[r.knittingStatus] ?? r.knittingStatus}
            </span>
          )}
          {r.mine && <span className="text-xs text-neutral-400">내 리뷰</span>}
        </div>
        {r.mine && (
          <button type="button" onClick={onDelete} disabled={deleting}
            className="text-xs text-red-500 hover:underline disabled:opacity-50">삭제</button>
        )}
      </div>
      {r.rating != null && (
        <p className="mt-1.5 text-sm" aria-label={`별점 ${r.rating}점`}>
          <span className="text-amber-400">{"★".repeat(r.rating)}</span>
          <span className="text-neutral-300 dark:text-neutral-600">{"★".repeat(5 - r.rating)}</span>
          <span className="ml-1 align-middle text-xs font-bold text-neutral-500">{r.rating}.0</span>
        </p>
      )}
      {r.title && <p className="mt-1.5 text-sm font-medium">{r.title}</p>}
      {r.contentText && <p className="mt-1 whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{r.contentText}</p>}
      {r.images.length > 0 && (
        <div className="mt-2 flex gap-2 overflow-x-auto">
          {r.images.map((src, i) => (
            // eslint-disable-next-line @next/next/no-img-element
            <img key={i} src={src} alt="" onError={(e) => { e.currentTarget.style.display = "none"; }}
              className="h-24 w-24 shrink-0 rounded-lg border border-neutral-200 object-cover dark:border-neutral-800" />
          ))}
        </div>
      )}
      {r.gaugeAdjustmentSummary && (
        <p className="mt-2 inline-block rounded bg-amber-50 px-2 py-1 text-xs text-amber-700 dark:bg-amber-950/40 dark:text-amber-300">
          게이지 조정: {r.gaugeAdjustmentSummary}
        </p>
      )}

      {/* 좋아요·댓글·신고 (SOCIAL-001·002·009) */}
      <div className="mt-3 flex items-center gap-4 text-sm">
        <button type="button" onClick={() => loggedIn ? like.mutate() : window.alert("로그인이 필요합니다.")}
          disabled={like.isPending} aria-pressed={liked}
          className={`flex items-center gap-1 ${liked ? "text-red-500" : "text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-100"}`}>
          {liked ? "♥" : "♡"} {likeCount}
        </button>
        <button type="button" onClick={() => setShowComments((v) => !v)}
          className="flex items-center gap-1 text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-100">
          💬 {r.commentCount}
        </button>
        {loggedIn && !r.mine && (
          <button type="button" onClick={() => { if (window.confirm("이 리뷰를 신고할까요?")) report.mutate(); }}
            className="ml-auto text-xs text-neutral-400 hover:text-neutral-600">신고</button>
        )}
      </div>

      {showComments && (
        <CommentThread reviewId={r.id} loggedIn={loggedIn}
          onChanged={() => queryClient.invalidateQueries({ queryKey: ["reviews", patternId] })} />
      )}
    </li>
  );
}

function CommentThread({ reviewId, loggedIn, onChanged }: {
  reviewId: number; loggedIn: boolean; onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const key = ["comments", reviewId];
  const [text, setText] = useState("");

  const { data, isLoading } = useQuery({ queryKey: key, queryFn: () => socialApi.comments("REVIEW", reviewId) });

  const add = useMutation({
    mutationFn: () => socialApi.addComment("REVIEW", reviewId, text.trim()),
    onSuccess: async () => { setText(""); await queryClient.invalidateQueries({ queryKey: key }); onChanged(); },
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "댓글 등록 중 오류가 발생했습니다."),
  });
  const del = useMutation({
    mutationFn: (id: number) => socialApi.deleteComment(id),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: key }); onChanged(); },
  });

  return (
    <div className="mt-3 border-t border-neutral-100 pt-3 dark:border-neutral-900">
      {isLoading ? (
        <p className="text-xs text-neutral-400">댓글 불러오는 중…</p>
      ) : (
        <ul className="space-y-2">
          {data?.items.map((c) => (
            <li key={c.id} className="flex items-start justify-between gap-2 text-sm">
              <span><span className="font-medium">{c.authorNickname}</span> <span className="text-neutral-700 dark:text-neutral-300">{c.content}</span></span>
              {c.mine && <button type="button" onClick={() => del.mutate(c.id)} className="shrink-0 text-xs text-red-400 hover:underline">삭제</button>}
            </li>
          ))}
          {data && data.items.length === 0 && <li className="text-xs text-neutral-400">첫 댓글을 남겨보세요.</li>}
        </ul>
      )}

      {loggedIn ? (
        <form className="mt-3 flex gap-2" onSubmit={(e) => { e.preventDefault(); if (text.trim()) add.mutate(); }}>
          <input value={text} onChange={(e) => setText(e.target.value)} placeholder="댓글 달기…"
            className="flex-1 rounded-md border border-neutral-300 bg-transparent px-3 py-1.5 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100" />
          <button type="submit" disabled={add.isPending || !text.trim()}
            className="rounded-md bg-neutral-900 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">등록</button>
        </form>
      ) : (
        <p className="mt-2 text-xs text-neutral-400">댓글은 로그인 후 작성할 수 있어요.</p>
      )}
    </div>
  );
}
