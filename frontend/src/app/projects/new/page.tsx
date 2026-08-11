"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { ApiError } from "@/lib/api";
import { patternApi } from "@/features/pattern/api";
import { projectApi, type CreateProjectBody } from "@/features/project/api";

type YarnRow = { brand: string; yarnName: string; color: string; amount: string };
type GaugeRow = { stitches: string; rows: string; needleSizeMm: string };

export default function NewProjectPage() {
  return (
    <Suspense fallback={<main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">불러오는 중…</main>}>
      <NewProjectForm />
    </Suspense>
  );
}

function NewProjectForm() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const initialPatternId = searchParams.get("sellingPatternId");

  // 도안 연결: 코잇다 도안(CATALOG) vs 외부 도안(EXTERNAL)
  const [connection, setConnection] = useState<"CATALOG" | "EXTERNAL">(initialPatternId ? "CATALOG" : "CATALOG");
  const [selectedPatternId, setSelectedPatternId] = useState<number | null>(
    initialPatternId ? Number(initialPatternId) : null,
  );
  const [search, setSearch] = useState("");
  const [externalTitle, setExternalTitle] = useState("");
  const [externalCreator, setExternalCreator] = useState("");

  const [title, setTitle] = useState("");
  const [comment, setComment] = useState("");
  const [visibility, setVisibility] = useState<"PRIVATE" | "PUBLIC">("PRIVATE");
  const [yarns, setYarns] = useState<YarnRow[]>([{ brand: "", yarnName: "", color: "", amount: "" }]);
  const [gauges, setGauges] = useState<GaugeRow[]>([{ stitches: "", rows: "", needleSizeMm: "" }]);
  const [error, setError] = useState<string | null>(null);

  // 선택된 코잇다 도안의 이름 표시
  const { data: selectedPattern } = useQuery({
    queryKey: ["pattern", selectedPatternId],
    queryFn: () => patternApi.get(selectedPatternId as number),
    enabled: connection === "CATALOG" && selectedPatternId != null,
  });

  // 도안 검색(연결용)
  const { data: results } = useQuery({
    queryKey: ["pattern-search", search],
    queryFn: () => patternApi.list({ q: search, size: 6 }),
    enabled: connection === "CATALOG" && selectedPatternId == null && search.trim().length > 0,
  });

  const create = useMutation({
    mutationFn: (body: CreateProjectBody) => projectApi.create(body),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: ["projects", "mine"] });
      router.push(`/projects/${res.id}`);
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "생성 중 오류가 발생했습니다."),
  });

  function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const body: CreateProjectBody = {
      connectionType: connection,
      title: title.trim() || undefined,
      note: comment.trim() || undefined,
      visibility,
      yarns: yarns
        .filter((y) => y.brand || y.yarnName || y.color || y.amount)
        .map((y) => ({ brand: y.brand || undefined, yarnName: y.yarnName || undefined, color: y.color || undefined, amount: y.amount || undefined })),
      gauges: gauges
        .filter((g) => g.stitches || g.rows || g.needleSizeMm)
        .map((g) => ({
          stitches: g.stitches ? Number(g.stitches) : undefined,
          rows: g.rows ? Number(g.rows) : undefined,
          needleSizeMm: g.needleSizeMm ? Number(g.needleSizeMm) : undefined,
          measuredStage: "SWATCH",
        })),
    };

    if (connection === "CATALOG") {
      if (!selectedPatternId) { setError("연결할 코잇다 도안을 선택하세요."); return; }
      body.sellingPatternId = selectedPatternId;
    } else {
      if (!externalTitle.trim()) { setError("외부 도안의 도안명을 입력하세요."); return; }
      body.externalPattern = { title: externalTitle.trim(), creatorName: externalCreator.trim() || undefined };
    }
    create.mutate(body);
  }

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <h1 className="text-2xl font-semibold tracking-tight">니팅로그 만들기</h1>

      <form className="mt-6 space-y-6" onSubmit={submit} noValidate>
        {/* 1. 제목 */}
        <Section title="제목">
          <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="선택, 미입력 시 날짜 기반" className={inputClass} />
        </Section>

        {/* 2. 도안 연결 */}
        <Section title="도안 연결">
          <div className="mb-3 flex gap-2">
            <TypeButton active={connection === "CATALOG"} onClick={() => setConnection("CATALOG")}>코잇다 도안</TypeButton>
            <TypeButton active={connection === "EXTERNAL"} onClick={() => setConnection("EXTERNAL")}>외부 도안</TypeButton>
          </div>

          {connection === "CATALOG" ? (
            selectedPatternId ? (
              <div className="flex items-center justify-between rounded-md border border-neutral-300 px-3 py-2 dark:border-neutral-700">
                <span className="text-sm">
                  연결됨: <span className="font-medium">{selectedPattern?.title ?? `#${selectedPatternId}`}</span>
                  <span className="ml-2 text-xs text-neutral-500">연결 시 원작 게이지·사이즈가 스냅샷으로 복사됩니다.</span>
                </span>
                <button type="button" onClick={() => { setSelectedPatternId(null); setSearch(""); }} className="text-xs text-neutral-500 hover:underline">변경</button>
              </div>
            ) : (
              <div>
                <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="도안명·원작자로 검색" className={inputClass} />
                {results && results.items.length > 0 && (
                  <ul className="mt-2 divide-y divide-neutral-100 rounded-md border border-neutral-200 dark:divide-neutral-800 dark:border-neutral-800">
                    {results.items.map((p) => (
                      <li key={p.id}>
                        <button type="button" onClick={() => setSelectedPatternId(p.id)}
                          className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-neutral-50 dark:hover:bg-neutral-900">
                          <span>{p.title} <span className="text-xs text-neutral-500">{p.sellerBrand ?? p.designerName ?? ""}</span></span>
                          <span className="text-xs text-neutral-400">연결</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                {search.trim() && results && results.items.length === 0 && (
                  <p className="mt-2 text-xs text-neutral-400">검색 결과가 없어요.</p>
                )}
              </div>
            )
          ) : (
            <div className="grid gap-3 sm:grid-cols-2">
              <Labeled label="도안명 (필수)"><input value={externalTitle} onChange={(e) => setExternalTitle(e.target.value)} className={inputClass} /></Labeled>
              <Labeled label="원작자 (선택)"><input value={externalCreator} onChange={(e) => setExternalCreator(e.target.value)} className={inputClass} /></Labeled>
            </div>
          )}
        </Section>

        {/* 3. 실 */}
        <Section title="실" onAdd={() => setYarns((v) => [...v, { brand: "", yarnName: "", color: "", amount: "" }])}>
          {yarns.map((y, i) => (
            <div key={i} className="grid grid-cols-2 gap-2 sm:grid-cols-4">
              <input placeholder="브랜드" value={y.brand} onChange={(e) => setYarns(upd(yarns, i, "brand", e.target.value))} className={inputClass} />
              <input placeholder="실 이름" value={y.yarnName} onChange={(e) => setYarns(upd(yarns, i, "yarnName", e.target.value))} className={inputClass} />
              <input placeholder="색상" value={y.color} onChange={(e) => setYarns(upd(yarns, i, "color", e.target.value))} className={inputClass} />
              <input placeholder="양" value={y.amount} onChange={(e) => setYarns(upd(yarns, i, "amount", e.target.value))} className={inputClass} />
            </div>
          ))}
        </Section>

        {/* 4. 게이지 (바늘 정보 포함) */}
        <Section title="게이지 · 바늘" onAdd={() => setGauges((v) => [...v, { stitches: "", rows: "", needleSizeMm: "" }])}>
          {gauges.map((g, i) => (
            <div key={i} className="grid grid-cols-3 gap-2">
              <input placeholder="코수" inputMode="decimal" value={g.stitches} onChange={(e) => setGauges(upd(gauges, i, "stitches", e.target.value))} className={inputClass} />
              <input placeholder="단수" inputMode="decimal" value={g.rows} onChange={(e) => setGauges(upd(gauges, i, "rows", e.target.value))} className={inputClass} />
              <input placeholder="바늘(mm)" inputMode="decimal" value={g.needleSizeMm} onChange={(e) => setGauges(upd(gauges, i, "needleSizeMm", e.target.value))} className={inputClass} />
            </div>
          ))}
        </Section>

        {/* 5. 코멘트 + 공개 */}
        <Section title="코멘트">
          <input value={comment} onChange={(e) => setComment(e.target.value)} placeholder="한 줄 코멘트 (선택)" className={inputClass} />
          <label className="mt-3 flex items-center gap-2 text-sm">
            <input type="checkbox" checked={visibility === "PUBLIC"} onChange={(e) => setVisibility(e.target.checked ? "PUBLIC" : "PRIVATE")} className="h-4 w-4" />
            공개 (기본은 비공개)
          </label>
        </Section>

        <p className="text-xs text-neutral-400">대표 이미지 등록은 파일 업로드 도입 후 추가됩니다.</p>

        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}

        <button type="submit" disabled={create.isPending}
          className="w-full rounded-md bg-neutral-900 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
          {create.isPending ? "만드는 중…" : "니팅로그 만들기"}
        </button>
      </form>
    </main>
  );
}

const inputClass =
  "w-full rounded-lg border-2 border-neutral-900 bg-transparent px-3 py-2 text-sm outline-none dark:border-neutral-100";

function upd<T extends Record<string, string>>(rows: T[], i: number, key: keyof T, value: string): T[] {
  return rows.map((r, idx) => (idx === i ? { ...r, [key]: value } : r));
}

function TypeButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button type="button" onClick={onClick}
      className={`rounded-full border-2 border-neutral-900 px-4 py-1.5 text-sm dark:border-neutral-100 ${active ? "bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-900" : "text-neutral-700 dark:text-neutral-200"}`}>
      {children}
    </button>
  );
}

function Section({ title, onAdd, children }: { title: string; onAdd?: () => void; children: React.ReactNode }) {
  return (
    <section className="rounded-[22px] border-2 border-neutral-900 bg-white p-5 dark:border-neutral-100 dark:bg-neutral-950">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-xs font-bold uppercase tracking-wider text-neutral-400">{title}</h2>
        {onAdd && <button type="button" onClick={onAdd} className="rounded-full border border-neutral-400 px-2.5 py-0.5 text-xs text-neutral-500 hover:border-neutral-900 hover:text-neutral-900 dark:hover:border-neutral-100 dark:hover:text-neutral-100">+ 행 추가</button>}
      </div>
      <div className="space-y-2">{children}</div>
    </section>
  );
}

function Labeled({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs text-neutral-500">{label}</span>
      {children}
    </label>
  );
}
