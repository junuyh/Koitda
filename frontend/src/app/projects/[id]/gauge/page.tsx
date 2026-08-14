"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { ApiError } from "@/lib/api";
import { gaugeApi, type CalculateBody, type CalculationResult, type SizeInfo } from "@/features/gauge/api";
import { GaugeResultView } from "@/features/gauge/GaugeResultView";

const inputClass =
  "w-full rounded-md border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100";

export default function GaugeCalcPage() {
  const params = useParams<{ id: string }>();
  const projectId = Number(params.id);
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: defaults, isLoading, isError } = useQuery({
    queryKey: ["gauge-defaults", projectId],
    queryFn: () => gaugeApi.defaults(projectId),
    retry: false,
  });

  const [sizeLabel, setSizeLabel] = useState<string>("");
  const [myStitches, setMyStitches] = useState("");
  const [myRows, setMyRows] = useState("");
  const [myNeedle, setMyNeedle] = useState(""); // 비우면 도안 바늘 mm 로 자동
  const [targets, setTargets] = useState<Record<string, string>>({});
  const [result, setResult] = useState<CalculationResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 기본값 로드 시 첫 사이즈·추천 게이지·목표치수 프리필
  useEffect(() => {
    if (!defaults) return;
    const first = defaults.sizes[0];
    if (first) {
      setSizeLabel(first.label);
      setTargets(Object.fromEntries(Object.entries(first.measurements).map(([k, v]) => [k, String(v)])));
    }
    const sug = defaults.myGaugeSuggestions[0];
    if (sug) {
      if (sug.stitches != null) setMyStitches(String(sug.stitches));
      if (sug.rows != null) setMyRows(String(sug.rows));
    }
  }, [defaults]);

  const selectedSize: SizeInfo | undefined = useMemo(
    () => defaults?.sizes.find((s) => s.label === sizeLabel),
    [defaults, sizeLabel],
  );

  function onSizeChange(label: string) {
    setSizeLabel(label);
    const s = defaults?.sizes.find((x) => x.label === label);
    if (s) setTargets(Object.fromEntries(Object.entries(s.measurements).map(([k, v]) => [k, String(v)])));
    setResult(null);
  }

  const calc = useMutation({
    mutationFn: (body: CalculateBody) => gaugeApi.calculate(body),
    onSuccess: (r) => setResult(r),
    onError: (e) => setError(e instanceof ApiError ? e.message : "계산 중 오류가 발생했습니다."),
  });

  const apply = useMutation({
    mutationFn: () => gaugeApi.apply(result!.calculationId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["project", projectId] });
      await queryClient.invalidateQueries({ queryKey: ["gauge-applied", projectId] });
      router.push(`/projects/${projectId}`);
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "적용 중 오류가 발생했습니다."),
  });

  function runCalc() {
    setError(null);
    if (!selectedSize) { setError("사이즈를 선택하세요."); return; }
    const s = Number(myStitches), r = Number(myRows);
    if (!s || !r || s <= 0 || r <= 0) { setError("내 게이지(코수·단수)를 입력하세요."); return; }
    // 도안 값과 다른 부위만 targetMeasurements 로 보낸다(GAUGE-010)
    const changed: Record<string, number> = {};
    for (const [k, v] of Object.entries(targets)) {
      const num = Number(v);
      if (Number.isFinite(num) && num !== selectedSize.measurements[k]) changed[k] = num;
    }
    // 바늘 mm 미입력 시 도안 바늘과 동일하다고 본다(GAUGE-004).
    const needle = myNeedle.trim() ? Number(myNeedle) : (defaults?.patternGauge?.needleSizeMm ?? null);
    calc.mutate({
      projectId,
      patternGauge: defaults?.patternGauge ?? undefined,
      myGauge: { stitches: s, rows: r, needleSizeMm: needle },
      selectedSizeLabel: sizeLabel,
      targetMeasurements: changed,
    });
  }

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !defaults) return <Centered>니팅로그를 찾을 수 없습니다.</Centered>;

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <Link href={`/projects/${projectId}`} className="text-xs text-neutral-500 hover:underline">← 니팅로그</Link>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">게이지 계산</h1>
      <p className="mt-1 text-sm text-neutral-500">내 게이지 기준으로 조정 콧수와 부위별 필요 콧수를 계산합니다. (참고값 · 스와치 확인 권장)</p>

      {defaults.requiresManualInput ? (
        <p className="mt-6 rounded-md border border-neutral-200 px-4 py-6 text-center text-sm text-neutral-500 dark:border-neutral-800">
          외부 도안은 도안 게이지·사이즈 직접 입력이 필요합니다(준비 중). 코잇다 도안 연결 니팅로그에서 계산할 수 있어요.
        </p>
      ) : (
        <>
          <section className="mt-6 grid gap-4 sm:grid-cols-2">
            <label className="block">
              <span className="mb-1 block text-xs text-neutral-500">사이즈</span>
              <select value={sizeLabel} onChange={(e) => onSizeChange(e.target.value)} className={inputClass}>
                {defaults.sizes.map((s) => <option key={s.label} value={s.label}>{s.label} (시작 {s.castOnStitches}코)</option>)}
              </select>
            </label>
            <div className="grid grid-cols-3 gap-2">
              <label className="block">
                <span className="mb-1 block text-xs text-neutral-500">내 게이지 코수</span>
                <input inputMode="decimal" value={myStitches} onChange={(e) => setMyStitches(e.target.value)} className={inputClass} />
              </label>
              <label className="block">
                <span className="mb-1 block text-xs text-neutral-500">내 게이지 단수</span>
                <input inputMode="decimal" value={myRows} onChange={(e) => setMyRows(e.target.value)} className={inputClass} />
              </label>
              <label className="block">
                <span className="mb-1 block text-xs text-neutral-500">내 바늘 mm</span>
                <input inputMode="decimal" value={myNeedle} onChange={(e) => setMyNeedle(e.target.value)}
                  placeholder={defaults.patternGauge?.needleSizeMm != null ? `도안 ${defaults.patternGauge.needleSizeMm}` : "선택"}
                  className={inputClass} />
              </label>
            </div>
          </section>

          {defaults.patternGauge && (
            <p className="mt-2 text-xs text-neutral-400">
              도안 게이지: {defaults.patternGauge.stitches}코 × {defaults.patternGauge.rows}단
              {defaults.patternGauge.needleSizeMm ? ` · 바늘 ${defaults.patternGauge.needleSizeMm}mm` : ""}
            </p>
          )}

          <section className="mt-5">
            <h2 className="mb-2 text-sm font-semibold">부위별 목표 치수 <span className="font-normal text-neutral-400">(바꾼 부위만 반영, 미변경은 도안 값)</span></h2>
            <div className="grid gap-2 sm:grid-cols-2">
              {selectedSize && Object.keys(selectedSize.measurements).map((k) => (
                <label key={k} className="flex items-center gap-2 text-sm">
                  <span className="w-20 shrink-0 text-neutral-500">{defaults.measurementLabels[k] ?? k}</span>
                  <input inputMode="decimal" value={targets[k] ?? ""} onChange={(e) => setTargets((t) => ({ ...t, [k]: e.target.value }))} className={inputClass} />
                  <span className="text-xs text-neutral-400">cm</span>
                </label>
              ))}
            </div>
          </section>

          {error && <p role="alert" className="mt-4 text-sm text-red-600">{error}</p>}

          <button type="button" onClick={runCalc} disabled={calc.isPending}
            className="mt-5 w-full rounded-md bg-neutral-900 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
            {calc.isPending ? "계산 중…" : "계산하기"}
          </button>

          {result && <Result result={result} labels={defaults.measurementLabels} onApply={() => apply.mutate()} applying={apply.isPending} />}
        </>
      )}
    </main>
  );
}

function Result({ result, labels, onApply, applying }: {
  result: CalculationResult; labels: Record<string, string>; onApply: () => void; applying: boolean;
}) {
  const { data: ai } = useQuery({ queryKey: ["gauge-ai-available"], queryFn: () => gaugeApi.aiAvailable(), staleTime: 5 * 60 * 1000 });
  const [advice, setAdvice] = useState<string | null>(null);
  const getAdvice = useMutation({
    mutationFn: () => gaugeApi.aiAdvice(result.calculationId),
    onSuccess: (r) => setAdvice(r.advice),
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "AI 조언 생성에 실패했습니다."),
  });

  return (
    <section className="mt-8 border-t border-neutral-200 pt-6 dark:border-neutral-800">
      <h2 className="mb-3 text-sm font-semibold">계산 결과</h2>
      <GaugeResultView result={result} labels={labels} />

      {/* AI 조언 — 키 설정 시에만. 수치는 코드 계산 결과, AI 는 조언 문구만 생성. */}
      {ai?.available && (
        <div className="mt-5 rounded-2xl border-2 border-amber-300 bg-amber-50 p-4 dark:border-amber-800 dark:bg-amber-950/20">
          <div className="flex items-center justify-between gap-2">
            <span className="text-sm font-bold text-amber-800 dark:text-amber-300">✨ AI 게이지 조언</span>
            <button type="button" onClick={() => getAdvice.mutate()} disabled={getAdvice.isPending}
              className="rounded-full border-2 border-neutral-900 px-3 py-1 text-xs font-bold disabled:opacity-50 dark:border-neutral-100">
              {getAdvice.isPending ? "생성 중…" : advice ? "다시 받기" : "조언 받기"}
            </button>
          </div>
          {advice
            ? <p className="mt-2 whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-200">{advice}</p>
            : <p className="mt-2 text-xs text-neutral-500">계산 결과를 바탕으로 바늘·스와치 조정 팁을 받아보세요. (참고용 · 저장되지 않음)</p>}
        </div>
      )}

      <button type="button" onClick={onApply} disabled={applying}
        className="mt-6 w-full rounded-full border-2 border-neutral-900 px-3 py-2.5 text-sm font-bold disabled:opacity-50 dark:border-neutral-100">
        {applying ? "적용 중…" : "이 계산을 니팅로그에 적용"}
      </button>
    </section>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}
