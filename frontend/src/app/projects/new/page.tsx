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
  const sellingPatternId = searchParams.get("sellingPatternId");
  const isCatalog = !!sellingPatternId;

  // 판매 도안 연결이면 도안명을 보여주기 위해 상세를 가져온다.
  const { data: pattern } = useQuery({
    queryKey: ["pattern", Number(sellingPatternId)],
    queryFn: () => patternApi.get(Number(sellingPatternId)),
    enabled: isCatalog,
  });

  const [externalTitle, setExternalTitle] = useState("");
  const [externalCreator, setExternalCreator] = useState("");
  const [title, setTitle] = useState("");
  const [note, setNote] = useState("");
  const [visibility, setVisibility] = useState<"PRIVATE" | "PUBLIC">("PRIVATE");
  const [yarns, setYarns] = useState<YarnRow[]>([{ brand: "", yarnName: "", color: "", amount: "" }]);
  const [gauges, setGauges] = useState<GaugeRow[]>([{ stitches: "", rows: "", needleSizeMm: "" }]);
  const [error, setError] = useState<string | null>(null);

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
      connectionType: isCatalog ? "CATALOG" : "EXTERNAL",
      title: title.trim() || undefined,
      note: note.trim() || undefined,
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

    if (isCatalog) {
      body.sellingPatternId = Number(sellingPatternId);
    } else {
      if (!externalTitle.trim()) {
        setError("외부 도안의 도안명을 입력하세요.");
        return;
      }
      body.externalPattern = { title: externalTitle.trim(), creatorName: externalCreator.trim() || undefined };
    }
    create.mutate(body);
  }

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <h1 className="text-2xl font-semibold tracking-tight">니팅로그 만들기</h1>

      <form className="mt-6 space-y-6" onSubmit={submit} noValidate>
        {/* 1. 도안 연결 */}
        <Section title="도안 연결">
          {isCatalog ? (
            <p className="text-sm">
              코잇다 도안 연결: <span className="font-medium">{pattern?.title ?? `#${sellingPatternId}`}</span>
              <span className="block text-xs text-neutral-500">연결 시 원작 게이지·사이즈가 스냅샷으로 복사됩니다.</span>
            </p>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2">
              <Labeled label="도안명 (필수)">
                <input value={externalTitle} onChange={(e) => setExternalTitle(e.target.value)} className={inputClass} />
              </Labeled>
              <Labeled label="원작자 (선택)">
                <input value={externalCreator} onChange={(e) => setExternalCreator(e.target.value)} className={inputClass} />
              </Labeled>
            </div>
          )}
        </Section>

        {/* 2. 제목·비고 */}
        <Section title="니팅로그 정보">
          <Labeled label="제목 (선택, 미입력 시 날짜 기반)">
            <input value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} />
          </Labeled>
          <Labeled label="비고 (선택)">
            <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={2} className={inputClass} />
          </Labeled>
        </Section>

        {/* 3. 실 (다중) */}
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

        {/* 4. 게이지 (다중) */}
        <Section title="게이지" onAdd={() => setGauges((v) => [...v, { stitches: "", rows: "", needleSizeMm: "" }])}>
          {gauges.map((g, i) => (
            <div key={i} className="grid grid-cols-3 gap-2">
              <input placeholder="코수" inputMode="decimal" value={g.stitches} onChange={(e) => setGauges(upd(gauges, i, "stitches", e.target.value))} className={inputClass} />
              <input placeholder="단수" inputMode="decimal" value={g.rows} onChange={(e) => setGauges(upd(gauges, i, "rows", e.target.value))} className={inputClass} />
              <input placeholder="바늘(mm)" inputMode="decimal" value={g.needleSizeMm} onChange={(e) => setGauges(upd(gauges, i, "needleSizeMm", e.target.value))} className={inputClass} />
            </div>
          ))}
        </Section>

        {/* 5. 공개 */}
        <Section title="공개 설정">
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={visibility === "PUBLIC"} onChange={(e) => setVisibility(e.target.checked ? "PUBLIC" : "PRIVATE")} className="h-4 w-4" />
            공개 (기본은 비공개)
          </label>
        </Section>

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
  "w-full rounded-md border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100";

function upd<T extends Record<string, string>>(rows: T[], i: number, key: keyof T, value: string): T[] {
  return rows.map((r, idx) => (idx === i ? { ...r, [key]: value } : r));
}

function Section({ title, onAdd, children }: { title: string; onAdd?: () => void; children: React.ReactNode }) {
  return (
    <section>
      <div className="mb-2 flex items-center justify-between">
        <h2 className="text-sm font-semibold">{title}</h2>
        {onAdd && (
          <button type="button" onClick={onAdd} className="text-xs text-neutral-500 hover:underline">+ 행 추가</button>
        )}
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
