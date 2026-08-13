"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { patternApi, type PatternListItem } from "@/features/pattern/api";
import { accentOf } from "@/features/ui/accent";

export default function WishlistPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useQuery({ queryKey: ["wishes"], queryFn: () => patternApi.wishes(), retry: false });
  const [editing, setEditing] = useState(false);
  const [selected, setSelected] = useState<Set<number>>(new Set());

  const items = data?.items ?? [];

  const removeSelected = useMutation({
    mutationFn: async () => {
      // 배치 삭제 엔드포인트가 없어 선택분을 순차 해제한다.
      for (const id of selected) await patternApi.removeWish(id);
    },
    onSuccess: async () => {
      setSelected(new Set());
      setEditing(false);
      await queryClient.invalidateQueries({ queryKey: ["wishes"] });
    },
  });

  function toggle(id: number) {
    setSelected((s) => {
      const n = new Set(s);
      if (n.has(id)) n.delete(id); else n.add(id);
      return n;
    });
  }

  return (
    <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8">
      <div className="mb-6 flex items-end justify-between gap-3">
        <div>
          <Link href="/me" className="text-sm font-bold text-neutral-500 hover:underline">← 마이페이지</Link>
          <h1 className="mt-1 text-4xl font-black tracking-tight">위시리스트</h1>
        </div>
        {items.length > 0 && (
          editing ? (
            <div className="flex gap-2">
              <button type="button" onClick={() => removeSelected.mutate()} disabled={selected.size === 0 || removeSelected.isPending}
                className="rounded-full border-2 border-red-500 px-4 py-1.5 text-sm font-bold text-red-500 disabled:opacity-40">
                선택 삭제 {selected.size > 0 ? `(${selected.size})` : ""}
              </button>
              <button type="button" onClick={() => { setEditing(false); setSelected(new Set()); }}
                className="rounded-full border-2 border-neutral-900 px-4 py-1.5 text-sm font-bold dark:border-neutral-100">완료</button>
            </div>
          ) : (
            <button type="button" onClick={() => setEditing(true)}
              className="rounded-full border-2 border-neutral-900 px-4 py-1.5 text-sm font-bold dark:border-neutral-100">편집</button>
          )
        )}
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-400">로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link></p>
      ) : items.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-400">찜한 도안이 없어요. <Link href="/patterns" className="underline">도안 둘러보기</Link></p>
      ) : (
        <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {items.map((p) => (
            <li key={p.id}><WishCard p={p} editing={editing} checked={selected.has(p.id)} onToggle={() => toggle(p.id)} /></li>
          ))}
        </ul>
      )}
    </main>
  );
}

function WishCard({ p, editing, checked, onToggle }: {
  p: PatternListItem; editing: boolean; checked: boolean; onToggle: () => void;
}) {
  const accent = accentOf(p.id);
  const body = (
    <div className={`group relative flex flex-col overflow-hidden rounded-2xl border-2 bg-white transition dark:bg-neutral-950 ${checked ? "border-red-500" : "border-neutral-900 dark:border-neutral-100"} ${editing ? "" : "hover:-translate-y-1 hover:shadow-[4px_4px_0_0_rgba(0,0,0,0.9)] dark:hover:shadow-[4px_4px_0_0_rgba(255,255,255,0.9)]"}`}>
      <div className={`relative flex aspect-square items-center justify-center overflow-hidden border-b-2 border-neutral-900 bg-gradient-to-br text-4xl font-black text-neutral-900/20 dark:border-neutral-100 dark:text-neutral-100/20 ${accent.wash}`}>
        <span>{p.title.slice(0, 1)}</span>
        {p.thumbnailUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={p.thumbnailUrl} alt="" onError={(e) => { e.currentTarget.style.display = "none"; }} className="absolute inset-0 h-full w-full object-cover" />
        )}
        {editing && (
          <span className={`absolute right-2 top-2 flex h-6 w-6 items-center justify-center rounded-full border-2 border-neutral-900 text-xs font-black ${checked ? "bg-red-500 text-white" : "bg-white text-transparent"}`}>✓</span>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-0.5 p-3">
        <p className="line-clamp-1 text-sm font-bold">{p.title}</p>
        <p className="line-clamp-1 text-xs text-neutral-500">{p.designerName ?? p.sellerBrand ?? "원작자 미상"}</p>
        <p className="mt-0.5 text-sm font-black">{p.salePrice != null ? `${p.salePrice.toLocaleString()}원` : "-"}</p>
      </div>
    </div>
  );
  if (editing) return <button type="button" onClick={onToggle} className="block w-full text-left">{body}</button>;
  return <Link href={`/patterns/${p.id}`} className="block">{body}</Link>;
}
