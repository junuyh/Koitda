"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { authApi } from "@/features/auth/api";
import { patternApi, type PatternListItem } from "@/features/pattern/api";
import { accentOf } from "@/features/ui/accent";

const PAGE_SIZE = 12;

const CRAFT_LABEL: Record<string, string> = { KNIT: "대바늘", CROCHET: "코바늘" };

export default function PatternListPage() {
  const [keyword, setKeyword] = useState(""); // 입력 중 값
  const [q, setQ] = useState(""); // 적용된 검색어
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [craftType, setCraftType] = useState<"KNIT" | "CROCHET" | undefined>();
  const [page, setPage] = useState(0);

  const { data: me } = useQuery({ queryKey: ["me"], queryFn: authApi.me });
  const { data: categories } = useQuery({ queryKey: ["categories"], queryFn: patternApi.categories });
  const { data, isLoading, isError } = useQuery({
    queryKey: ["patterns", { q, categoryId, craftType, page }],
    queryFn: () => patternApi.list({ q, categoryId, craftType, page, size: PAGE_SIZE }),
  });

  function resetTo(fn: () => void) {
    fn();
    setPage(0);
  }

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
      <div className="mb-6 flex items-end justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-neutral-400">Explore</p>
          <h1 className="mt-1 text-4xl font-black tracking-tight">도안 둘러보기</h1>
        </div>
        <Link href="/" className="text-sm text-neutral-500 hover:underline">홈</Link>
      </div>

      {/* 검색·필터 */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <form
          className="flex flex-1 gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            resetTo(() => setQ(keyword.trim()));
          }}
        >
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="도안명·원작자 검색"
            aria-label="도안 검색"
            className="w-full rounded-full border-2 border-neutral-900 bg-transparent px-4 py-2 text-sm outline-none dark:border-neutral-100"
          />
          <button type="submit" className="shrink-0 rounded-full bg-neutral-900 px-5 py-2 text-sm font-bold text-white dark:bg-neutral-100 dark:text-neutral-900">
            검색
          </button>
        </form>

        <select
          aria-label="카테고리"
          className={selectClass}
          value={categoryId ?? ""}
          onChange={(e) => resetTo(() => setCategoryId(e.target.value ? Number(e.target.value) : undefined))}
        >
          <option value="">전체 카테고리</option>
          {categories?.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>

        <select
          aria-label="뜨개 방식"
          className={selectClass}
          value={craftType ?? ""}
          onChange={(e) =>
            resetTo(() => setCraftType(e.target.value ? (e.target.value as "KNIT" | "CROCHET") : undefined))
          }
        >
          <option value="">전체 방식</option>
          <option value="KNIT">대바늘</option>
          <option value="CROCHET">코바늘</option>
        </select>
      </div>

      {/* 결과 */}
      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-red-600">목록을 불러오지 못했습니다.</p>
      ) : !data || data.items.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-400">조건에 맞는 도안이 없습니다.</p>
      ) : (
        <>
          <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {data.items.map((p) => (
              <PatternCard key={p.id} pattern={p} loggedIn={!!me} />
            ))}
          </ul>

          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </main>
  );
}

const selectClass =
  "rounded-full border-2 border-neutral-900 bg-transparent px-4 py-2 text-sm outline-none dark:border-neutral-100";

function PatternCard({ pattern, loggedIn }: { pattern: PatternListItem; loggedIn: boolean }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const accent = accentOf(pattern.id);

  const wish = useMutation({
    mutationFn: () => (pattern.wished ? patternApi.removeWish(pattern.id) : patternApi.addWish(pattern.id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patterns"] }),
  });

  function onWishClick() {
    if (!loggedIn) {
      router.push("/login");
      return;
    }
    wish.mutate();
  }

  return (
    <li className="group relative flex flex-col overflow-hidden rounded-2xl border-2 border-neutral-900 bg-white transition hover:-translate-y-1 hover:shadow-[4px_4px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:bg-neutral-950 dark:hover:shadow-[4px_4px_0_0_rgba(255,255,255,0.9)]">
      <Link href={`/patterns/${pattern.id}`} className="flex flex-col">
        {/* 대표 이미지 — 깨지면 글자 placeholder 로 폴백 */}
        <div className={`relative flex aspect-square items-center justify-center overflow-hidden border-b-2 border-neutral-900 bg-gradient-to-br text-4xl font-black text-neutral-900/20 dark:border-neutral-100 dark:text-neutral-100/20 ${accent.wash}`}>
          <span>{pattern.title.slice(0, 1)}</span>
          {pattern.thumbnailUrl && (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={pattern.thumbnailUrl} alt={pattern.title}
              onError={(e) => { e.currentTarget.style.display = "none"; }}
              className="absolute inset-0 h-full w-full object-cover" />
          )}
          {pattern.craftType && (
            <span className={`absolute left-2 top-2 rounded-full border border-neutral-900 px-2 py-0.5 text-[11px] font-bold dark:border-neutral-100 ${accent.solid}`}>
              {CRAFT_LABEL[pattern.craftType] ?? pattern.craftType}
            </span>
          )}
        </div>

        <div className="flex flex-1 flex-col gap-1 p-3">
          <p className="line-clamp-1 text-sm font-bold">{pattern.title}</p>
          <p className="line-clamp-1 text-xs text-neutral-500">
            {pattern.designerName ?? pattern.sellerBrand ?? "원작자 미상"}
          </p>
          <div className="mt-1.5 flex items-center justify-between">
            <span className="text-base font-black">
              {pattern.salePrice != null ? `${pattern.salePrice.toLocaleString()}원` : "-"}
            </span>
            {pattern.difficulty && (
              <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${accent.soft}`}>{pattern.difficulty}</span>
            )}
          </div>
        </div>
      </Link>

      {/* 위시 버튼은 Link 밖에 두어 카드 이동과 분리 */}
      <button
        type="button"
        onClick={onWishClick}
        disabled={wish.isPending}
        aria-label={pattern.wished ? "위시 해제" : "위시 등록"}
        aria-pressed={pattern.wished}
        className="absolute right-2 top-2 rounded-full border border-neutral-900 bg-white px-2 py-0.5 text-sm font-semibold dark:border-neutral-100 dark:bg-neutral-950"
      >
        {pattern.wished ? "♥" : "♡"} {pattern.wishCount}
      </button>
    </li>
  );
}

function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (p: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="mt-8 flex items-center justify-center gap-4 text-sm">
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page <= 0}
        className="rounded-md border border-neutral-300 px-3 py-1.5 disabled:opacity-40 dark:border-neutral-700"
      >
        이전
      </button>
      <span className="text-neutral-500">{page + 1} / {totalPages}</span>
      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="rounded-md border border-neutral-300 px-3 py-1.5 disabled:opacity-40 dark:border-neutral-700"
      >
        다음
      </button>
    </div>
  );
}
