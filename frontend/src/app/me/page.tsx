"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { authApi } from "@/features/auth/api";
import { pointApi, reviewApi } from "@/features/review/api";
import { socialApi } from "@/features/social/api";
import { meApi } from "@/features/me/api";

const STATUS_LABEL: Record<string, string> = {
  PLANNED: "준비 중", CO: "코잡기", WIP: "뜨는 중", UFO: "잠시 멈춤", FO: "완성",
};

const PURCHASE = [
  { href: "/library", emoji: "📥", label: "구매 내역", desc: "주문·다운로드", accent: "bg-emerald-100 dark:bg-emerald-950/40" },
  { href: "/wishlist", emoji: "🤍", label: "위시리스트", desc: "찜한 도안 관리", accent: "bg-rose-100 dark:bg-rose-950/40" },
];

const TABS = [
  { key: "follows", label: "팔로우" },
  { key: "reviews", label: "내 리뷰" },
  { key: "posts", label: "내 게시글" },
  { key: "comments", label: "내 댓글" },
  { key: "likes", label: "좋아요" },
] as const;
type TabKey = (typeof TABS)[number]["key"];

export default function MyPage() {
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: authApi.me });
  const { data: history } = useQuery({ queryKey: ["point-history"], queryFn: pointApi.history, retry: false });
  const [tab, setTab] = useState<TabKey>("follows");
  const point = history?.balance ?? me?.pointBalance ?? 0;

  return (
    <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8">
      {/* 프로필 헤더 */}
      <section className="flex flex-wrap items-center justify-between gap-4 rounded-3xl border-2 border-neutral-900 bg-white p-6 dark:border-neutral-100 dark:bg-neutral-950">
        <div className="flex items-center gap-4">
          <span className="flex h-14 w-14 items-center justify-center rounded-full border-2 border-neutral-900 bg-amber-300 text-2xl font-black text-neutral-900 dark:border-neutral-100">
            {me?.nickname?.slice(0, 1) ?? "?"}
          </span>
          <div className="min-w-0">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-neutral-400">My Page</p>
            <h1 className="truncate text-2xl font-black tracking-tight">{me?.nickname ?? "나"}님의 뜨개방</h1>
            {me?.email && <p className="truncate text-xs text-neutral-400">{me.email}</p>}
            <Link href="/me/settings" className="mt-1 inline-block text-xs font-bold text-neutral-500 underline hover:text-neutral-900 dark:hover:text-neutral-100">정보 수정</Link>
          </div>
        </div>
        <Link href="/me/points" className="rounded-2xl border-2 border-neutral-900 bg-gradient-to-br from-amber-100 to-amber-200 px-5 py-3 text-right dark:border-neutral-100 dark:from-amber-950/40 dark:to-amber-900/30">
          <span className="block text-[11px] font-bold uppercase tracking-wider text-amber-800/70 dark:text-amber-300/70">포인트</span>
          <span className="text-2xl font-black text-amber-950 dark:text-amber-200">{point.toLocaleString()}<span className="ml-0.5 text-sm">P</span></span>
        </Link>
      </section>

      {/* 구매 */}
      <section className="mt-8">
        <h2 className="mb-3 text-xl font-black tracking-tight">구매</h2>
        <ul className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          {PURCHASE.map((it) => (
            <li key={it.label}><Link href={it.href} className="block h-full"><RecordCard item={it} /></Link></li>
          ))}
        </ul>
      </section>

      {/* 내 활동 — 인페이지 탭 */}
      <section className="mt-8">
        <h2 className="mb-3 text-xl font-black tracking-tight">내 활동</h2>
        <div className="flex flex-wrap gap-2 border-b-2 border-neutral-900 pb-3 dark:border-neutral-100">
          {TABS.map((t) => (
            <button key={t.key} type="button" onClick={() => setTab(t.key)}
              className={`rounded-full border-2 px-4 py-1.5 text-sm font-bold transition ${
                tab === t.key
                  ? "border-neutral-900 bg-neutral-900 text-white dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900"
                  : "border-transparent text-neutral-500 hover:border-neutral-900 dark:hover:border-neutral-100"
              }`}>
              {t.label}
            </button>
          ))}
        </div>
        <div className="mt-4">
          {tab === "follows" && <FollowsTab />}
          {tab === "reviews" && <ReviewsTab />}
          {tab === "posts" && <PostsTab />}
          {tab === "comments" && <CommentsTab />}
          {tab === "likes" && <LikesTab />}
        </div>
      </section>
    </main>
  );
}

function RecordCard({ item }: { item: { emoji: string; label: string; desc: string; accent: string; soon?: boolean; href?: string } }) {
  return (
    <div className={`flex h-full flex-col gap-1 rounded-2xl border-2 border-neutral-900 p-4 transition dark:border-neutral-100 ${item.accent} ${item.soon ? "opacity-60" : "hover:-translate-y-1 hover:shadow-[4px_4px_0_0_rgba(0,0,0,0.9)] dark:hover:shadow-[4px_4px_0_0_rgba(255,255,255,0.9)]"}`}>
      <span className="text-2xl">{item.emoji}</span>
      <span className="mt-1 text-sm font-black">{item.label}</span>
      <span className="text-xs text-neutral-600 dark:text-neutral-300">{item.desc}</span>
    </div>
  );
}

function Empty({ children }: { children: React.ReactNode }) {
  return <p className="py-12 text-center text-sm text-neutral-400">{children}</p>;
}

function statusChip(status: string | null) {
  if (!status) return null;
  return <span className="shrink-0 rounded-full border border-neutral-900 bg-amber-300 px-2 py-0.5 text-[11px] font-bold text-amber-950 dark:border-neutral-100">{STATUS_LABEL[status] ?? status}</span>;
}

/* ── 팔로우 ── */
function FollowsTab() {
  const [sub, setSub] = useState<"following" | "followers">("following");
  const { data: following } = useQuery({ queryKey: ["following"], queryFn: socialApi.following, retry: false });
  const { data: followers } = useQuery({ queryKey: ["followers"], queryFn: socialApi.followers, retry: false });
  const list = sub === "following" ? following : followers;
  return (
    <div>
      <div className="mb-3 flex gap-2 text-xs font-bold">
        {(["following", "followers"] as const).map((s) => (
          <button key={s} type="button" onClick={() => setSub(s)}
            className={`rounded-full px-3 py-1 ${sub === s ? "bg-neutral-200 dark:bg-neutral-800" : "text-neutral-400"}`}>
            {s === "following" ? `팔로잉 ${following?.length ?? 0}` : `팔로워 ${followers?.length ?? 0}`}
          </button>
        ))}
      </div>
      {!list || list.length === 0 ? <Empty>{sub === "following" ? "팔로우한 사람이 없어요." : "나를 팔로우한 사람이 없어요."}</Empty> : (
        <ul className="space-y-2">
          {list.map((u) => (
            <li key={u.userId} className="flex items-center gap-3 rounded-2xl border-2 border-neutral-900 p-3 dark:border-neutral-100">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 border-neutral-900 bg-violet-200 text-sm font-black text-neutral-900 dark:border-neutral-100">{u.nickname.slice(0, 1)}</span>
              <span className="line-clamp-1 text-sm font-bold">{u.nickname}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/* ── 내 리뷰 ── */
function ReviewsTab() {
  const { data } = useQuery({ queryKey: ["my-reviews"], queryFn: reviewApi.mine, retry: false });
  if (!data || data.length === 0) return <Empty>아직 작성한 리뷰가 없어요.</Empty>;
  return (
    <ul className="space-y-2">
      {data.map((r) => (
        <li key={r.id}>
          <Link href={`/patterns/${r.patternId}`} className="block rounded-2xl border-2 border-neutral-900 p-3 transition hover:-translate-y-0.5 hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:hover:shadow-[3px_3px_0_0_rgba(255,255,255,0.9)]">
            <div className="flex items-center justify-between gap-2">
              <div className="flex min-w-0 items-center gap-2">{statusChip(r.knittingStatus)}<span className="line-clamp-1 text-sm font-bold">{r.patternTitle}</span></div>
              <span className="shrink-0 text-xs text-neutral-400">♥ {r.likeCount}</span>
            </div>
            {(r.title || r.contentText) && <p className="mt-1 line-clamp-1 text-sm text-neutral-500">{r.title || r.contentText}</p>}
          </Link>
        </li>
      ))}
    </ul>
  );
}

/* ── 내 게시글(공개 오늘의 로그) ── */
function PostsTab() {
  const { data } = useQuery({ queryKey: ["my-posts"], queryFn: meApi.posts, retry: false });
  if (!data || data.length === 0) return <Empty>공개한 오늘의 로그가 없어요.</Empty>;
  return (
    <ul className="space-y-2">
      {data.map((p) => (
        <li key={p.postId}>
          <Link href={`/projects/${p.projectId}`} className="flex items-center justify-between gap-2 rounded-2xl border-2 border-neutral-900 p-3 transition hover:-translate-y-0.5 hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:hover:shadow-[3px_3px_0_0_rgba(255,255,255,0.9)]">
            <div className="flex min-w-0 items-center gap-2">{statusChip(p.knittingStatus)}<span className="line-clamp-1 text-sm font-bold">{p.displayTitle}</span></div>
            <span className="shrink-0 text-xs text-neutral-400">{p.logDate}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

/* ── 내 댓글 ── */
function CommentsTab() {
  const { data } = useQuery({ queryKey: ["my-comments"], queryFn: meApi.comments, retry: false });
  if (!data || data.length === 0) return <Empty>작성한 댓글이 없어요.</Empty>;
  return (
    <ul className="space-y-2">
      {data.map((c) => (
        <li key={c.id} className="rounded-2xl border-2 border-neutral-900 p-3 dark:border-neutral-100">
          <p className="text-sm text-neutral-700 dark:text-neutral-300">{c.content}</p>
          <p className="mt-1 text-xs text-neutral-400">{new Date(c.createdAt).toLocaleString("ko-KR")}</p>
        </li>
      ))}
    </ul>
  );
}

/* ── 좋아요(오늘의 로그) ── */
function LikesTab() {
  const { data } = useQuery({ queryKey: ["my-likes"], queryFn: meApi.likes, retry: false });
  if (!data || data.length === 0) return <Empty>좋아요한 오늘의 로그가 없어요.</Empty>;
  return (
    <ul className="space-y-2">
      {data.map((l) => (
        <li key={l.postId}>
          <Link href={`/projects/${l.projectId}`} className="flex items-center justify-between gap-2 rounded-2xl border-2 border-neutral-900 p-3 transition hover:-translate-y-0.5 hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:hover:shadow-[3px_3px_0_0_rgba(255,255,255,0.9)]">
            <span className="line-clamp-1 text-sm font-bold">♥ {l.displayTitle}</span>
            <span className="shrink-0 text-xs text-neutral-400">{l.logDate}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
