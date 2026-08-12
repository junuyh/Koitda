"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { projectApi, type FeedItem, STATUS_LABEL } from "@/features/project/api";
import { accentOf } from "@/features/ui/accent";

const PAGE_SIZE = 16;

const STATUS_TONE: Record<string, string> = {
  PLANNED: "bg-neutral-200 text-neutral-700",
  CO: "bg-sky-400 text-sky-950",
  WIP: "bg-amber-400 text-amber-950",
  UFO: "bg-neutral-300 text-neutral-700",
  FO: "bg-emerald-400 text-emerald-950",
};

export default function ExplorePage() {
  const [sort, setSort] = useState<"recent" | "likes">("recent");
  const [pages, setPages] = useState(1); // 누적 로드 페이지 수

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
      <div className="mb-6 flex items-end justify-between gap-3">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-neutral-400">Explore</p>
          <h1 className="mt-1 text-4xl font-black tracking-tight">공개 니팅로그</h1>
        </div>
        <div className="flex rounded-full border-2 border-neutral-900 p-0.5 text-sm font-bold dark:border-neutral-100">
          {(["recent", "likes"] as const).map((s) => (
            <button key={s} type="button" onClick={() => { setSort(s); setPages(1); }}
              className={`rounded-full px-4 py-1.5 ${sort === s ? "bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-900" : "text-neutral-500"}`}>
              {s === "recent" ? "최신순" : "좋아요순"}
            </button>
          ))}
        </div>
      </div>

      <FeedList sort={sort} pages={pages} onMore={() => setPages((p) => p + 1)} />
    </main>
  );
}

function FeedList({ sort, pages, onMore }: { sort: "recent" | "likes"; pages: number; onMore: () => void }) {
  // 누적 개수만큼 한 번에 조회(page=0, size=누적). 훅 개수를 고정해 rules-of-hooks 를 지킨다.
  const size = PAGE_SIZE * pages;
  const { data, isLoading: loading } = useQuery({
    queryKey: ["feed", sort, "explore", size],
    queryFn: () => projectApi.feed(sort, 0, size),
  });
  const items: FeedItem[] = data ?? [];
  const canMore = items.length === size; // 요청 수만큼 다 찼으면 더 있을 수 있음

  if (loading && items.length === 0) {
    return <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>;
  }
  if (items.length === 0) {
    return <p className="py-16 text-center text-sm text-neutral-400">아직 공개된 니팅로그가 없습니다.</p>;
  }
  return (
    <>
      <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {items.map((f) => <li key={f.id}><Card f={f} /></li>)}
      </ul>
      {canMore && (
        <div className="mt-8 text-center">
          <button type="button" onClick={onMore}
            className="rounded-full border-2 border-neutral-900 px-6 py-2 text-sm font-bold transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100">
            더보기
          </button>
        </div>
      )}
    </>
  );
}

function Card({ f }: { f: FeedItem }) {
  const accent = accentOf(f.id);
  return (
    <Link href={`/projects/${f.id}`}
      className="group flex flex-col overflow-hidden rounded-2xl border-2 border-neutral-900 bg-white transition hover:-translate-y-1 hover:shadow-[4px_4px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:bg-neutral-950 dark:hover:shadow-[4px_4px_0_0_rgba(255,255,255,0.9)]">
      <div className={`relative flex aspect-[4/3] items-center justify-center overflow-hidden border-b-2 border-neutral-900 bg-gradient-to-br text-3xl font-black text-neutral-900/20 dark:border-neutral-100 dark:text-neutral-100/20 ${accent.wash}`}>
        <span>{f.displayTitle.slice(0, 1)}</span>
        {f.thumbnailUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={f.thumbnailUrl} alt="" onError={(e) => { e.currentTarget.style.display = "none"; }}
            className="absolute inset-0 h-full w-full object-cover" />
        )}
        <span className={`absolute left-2 top-2 rounded-full border border-neutral-900 px-2 py-0.5 text-[10px] font-bold dark:border-neutral-100 ${STATUS_TONE[f.status] ?? STATUS_TONE.PLANNED}`}>
          {STATUS_LABEL[f.status] ?? f.status}
        </span>
      </div>
      <div className="flex flex-1 flex-col gap-0.5 p-2.5">
        <p className="line-clamp-1 text-sm font-bold">{f.displayTitle}</p>
        <div className="mt-0.5 flex items-center justify-between text-xs text-neutral-500">
          <span className="line-clamp-1">{f.authorNickname}</span>
          <span className="shrink-0 font-bold">♥ {f.likeCount}</span>
        </div>
      </div>
    </Link>
  );
}
