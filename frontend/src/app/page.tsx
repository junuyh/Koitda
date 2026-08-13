"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useEffect, useState } from "react";
import { authApi } from "@/features/auth/api";
import { patternApi, type PatternListItem } from "@/features/pattern/api";
import { projectApi, type FeedItem, STATUS_LABEL } from "@/features/project/api";
import { accentOf } from "@/features/ui/accent";

const CRAFT_LABEL: Record<string, string> = { KNIT: "대바늘", CROCHET: "코바늘" , MIXED: "혼합" };

const STATUS_TONE: Record<string, string> = {
  PLANNED: "bg-neutral-200 text-neutral-700",
  CO: "bg-sky-400 text-sky-950",
  WIP: "bg-amber-400 text-amber-950",
  UFO: "bg-neutral-300 text-neutral-700",
  FO: "bg-emerald-400 text-emerald-950",
};

// 상단 배너 슬라이드 — CMS 없이 프론트 정적 구성(포트폴리오 데모).
const BANNERS = [
  { title: "나만의 뜨개 기록,\n코잇다에서 시작", sub: "도안부터 완성까지 한 흐름으로", cta: "도안 둘러보기", href: "/patterns", accent: "bg-amber-300 text-amber-950" },
  { title: "다른 사람의 제작 정보가\n내 시작을 돕는다", sub: "공개 니팅로그 둘러보기", cta: "둘러보기", href: "/explore", accent: "bg-sky-300 text-sky-950" },
  { title: "내 도안을\n코잇다에서 판매하기", sub: "판매자 신청하고 도안을 올려보세요", cta: "판매자 신청", href: "/seller/apply", accent: "bg-rose-300 text-rose-950" },
  { title: "게이지 계산으로\n사이즈 실패 없이", sub: "내 게이지에 맞춰 콧수 자동 조정", cta: "도안 보기", href: "/patterns", accent: "bg-violet-300 text-violet-950" },
];

export default function HomePage() {
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: authApi.me });
  const { data: best } = useQuery({ queryKey: ["patterns", "best"], queryFn: () => patternApi.bestSellers(10) });
  const { data: latest } = useQuery({ queryKey: ["patterns", "latest"], queryFn: () => patternApi.latest(10) });

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
      <BannerCarousel />

      {me && (
        <p className="mt-6 text-sm text-neutral-500">
          <span className="font-bold text-neutral-800 dark:text-neutral-200">{me.nickname}</span>님, 반갑습니다 · 포인트 {me.pointBalance.toLocaleString()}P
        </p>
      )}

      <Rail title="Best Sellers" moreHref="/patterns" items={best ?? []} ranked />
      <Rail title="최신 등록" moreHref="/patterns" items={latest?.items ?? []} />

      <PublicFeed />
    </main>
  );
}

/* ── 배너 캐러셀 ─────────────────────────────────────────── */
function BannerCarousel() {
  const [i, setI] = useState(0);
  const n = BANNERS.length;
  const go = (d: number) => setI((p) => (p + d + n) % n);

  useEffect(() => {
    const t = setInterval(() => setI((p) => (p + 1) % n), 5000);
    return () => clearInterval(t);
  }, [n]);

  const b = BANNERS[i];
  return (
    <section className="relative overflow-hidden rounded-3xl border-2 border-neutral-900 dark:border-neutral-100">
      <div className={`flex min-h-[220px] flex-col justify-between gap-4 p-7 transition-colors sm:min-h-[240px] ${b.accent}`}>
        <div>
          <h2 className="whitespace-pre-line text-2xl font-black leading-tight tracking-tight sm:text-3xl">{b.title}</h2>
          <p className="mt-2 text-sm font-medium opacity-80">{b.sub}</p>
        </div>
        <Link href={b.href} className="w-fit rounded-full border-2 border-neutral-900 bg-white px-5 py-2 text-sm font-bold text-neutral-900 transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)]">
          {b.cta} →
        </Link>
      </div>

      {/* 좌우 화살표 + 카운터 */}
      <div className="absolute bottom-4 right-4 flex items-center gap-1 rounded-full border-2 border-neutral-900 bg-white/90 px-2 py-1 text-xs font-bold text-neutral-900">
        <button type="button" aria-label="이전 배너" onClick={() => go(-1)} className="px-1">‹</button>
        <span>{i + 1} / {n}</span>
        <button type="button" aria-label="다음 배너" onClick={() => go(1)} className="px-1">›</button>
      </div>
      {/* 점 인디케이터 */}
      <div className="absolute bottom-5 left-7 flex gap-1.5">
        {BANNERS.map((_, idx) => (
          <button key={idx} type="button" aria-label={`${idx + 1}번 배너`} onClick={() => setI(idx)}
            className={`h-2 rounded-full border border-neutral-900 transition-all ${idx === i ? "w-5 bg-neutral-900" : "w-2 bg-white/70"}`} />
        ))}
      </div>
    </section>
  );
}

/* ── 도안 가로 스크롤 레일 ───────────────────────────────── */
function Rail({ title, moreHref, items, ranked }: { title: string; moreHref: string; items: PatternListItem[]; ranked?: boolean }) {
  if (items.length === 0) return null;
  return (
    <section className="mt-10">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-xl font-black tracking-tight">{title}</h2>
        <Link href={moreHref} className="text-sm font-bold text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-100">더보기 →</Link>
      </div>
      <ul className="flex snap-x gap-4 overflow-x-auto pb-2">
        {items.map((p, idx) => (
          <li key={p.id} className="w-40 shrink-0 snap-start sm:w-44">
            <PatternMiniCard p={p} rank={ranked ? idx + 1 : undefined} />
          </li>
        ))}
      </ul>
    </section>
  );
}

function PatternMiniCard({ p, rank }: { p: PatternListItem; rank?: number }) {
  const accent = accentOf(p.id);
  return (
    <Link href={`/patterns/${p.id}`}
      className="group flex flex-col overflow-hidden rounded-2xl border-2 border-neutral-900 bg-white transition hover:-translate-y-1 hover:shadow-[4px_4px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:bg-neutral-950 dark:hover:shadow-[4px_4px_0_0_rgba(255,255,255,0.9)]">
      <div className={`relative flex aspect-square items-center justify-center overflow-hidden border-b-2 border-neutral-900 bg-gradient-to-br text-3xl font-black text-neutral-900/20 dark:border-neutral-100 dark:text-neutral-100/20 ${accent.wash}`}>
        <span>{p.title.slice(0, 1)}</span>
        {p.thumbnailUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={p.thumbnailUrl} alt="" onError={(e) => { e.currentTarget.style.display = "none"; }}
            className="absolute inset-0 h-full w-full object-cover" />
        )}
        {rank != null && (
          <span className="absolute left-2 top-2 flex h-7 w-7 items-center justify-center rounded-full border-2 border-neutral-900 bg-white text-sm font-black text-neutral-900">{rank}</span>
        )}
        {p.craftType && (
          <span className={`absolute right-2 top-2 rounded-full border border-neutral-900 px-2 py-0.5 text-[10px] font-bold dark:border-neutral-100 ${accent.solid}`}>
            {CRAFT_LABEL[p.craftType] ?? p.craftType}
          </span>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-0.5 p-2.5">
        <p className="line-clamp-1 text-sm font-bold">{p.title}</p>
        <p className="line-clamp-1 text-xs text-neutral-500">{p.designerName ?? p.sellerBrand ?? "원작자 미상"}</p>
        <p className="mt-0.5 text-sm font-black">{p.salePrice != null ? `${p.salePrice.toLocaleString()}원` : "-"}</p>
      </div>
    </Link>
  );
}

/* ── 공개 니팅로그 피드 ──────────────────────────────────── */
function PublicFeed() {
  const [sort, setSort] = useState<"recent" | "likes">("recent");
  const { data: feed } = useQuery({ queryKey: ["feed", sort, "home"], queryFn: () => projectApi.feed(sort, 0, 10) });

  return (
    <section className="mt-10">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="text-xl font-black tracking-tight">공개 니팅로그</h2>
        <div className="flex items-center gap-2">
          <div className="flex rounded-full border-2 border-neutral-900 p-0.5 text-xs font-bold dark:border-neutral-100">
            {(["recent", "likes"] as const).map((s) => (
              <button key={s} type="button" onClick={() => setSort(s)}
                className={`rounded-full px-3 py-1 ${sort === s ? "bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-900" : "text-neutral-500"}`}>
                {s === "recent" ? "최신순" : "좋아요순"}
              </button>
            ))}
          </div>
          <Link href="/explore" className="text-sm font-bold text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-100">더보기 →</Link>
        </div>
      </div>

      {(feed ?? []).length === 0 ? (
        <p className="rounded-2xl border-2 border-dashed border-neutral-300 py-12 text-center text-sm text-neutral-400 dark:border-neutral-700">아직 공개된 니팅로그가 없습니다.</p>
      ) : (
        <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {(feed ?? []).map((f) => <li key={f.id}><FeedCard f={f} /></li>)}
        </ul>
      )}
    </section>
  );
}

function FeedCard({ f }: { f: FeedItem }) {
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
