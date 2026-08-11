"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { ApiError } from "@/lib/api";
import { gaugeApi, type CalculateBody, type CalculationResult, type SizeInfo } from "@/features/gauge/api";

const DIRECTION_LABEL: Record<string, string> = { LARGER: "더 굵은 바늘", SMALLER: "더 얇은 바늘", SAME: "동일" };
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
    calc.mutate({
      projectId,
      patternGauge: defaults?.patternGauge ?? undefined,
      myGauge: { stitches: s, rows: r, needleSizeMm: null },
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
            <div className="grid grid-cols-2 gap-2">
              <label className="block">
                <span className="mb-1 block text-xs text-neutral-500">내 게이지 코수</span>
                <input inputMode="decimal" value={myStitches} onChange={(e) => setMyStitches(e.target.value)} className={inputClass} />
              </label>
              <label className="block">
                <span className="mb-1 block text-xs text-neutral-500">내 게이지 단수</span>
                <input inputMode="decimal" value={myRows} onChange={(e) => setMyRows(e.target.value)} className={inputClass} />
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
  const g = result.gaugeAdjustment;
  return (
    <section className="mt-8 border-t border-neutral-200 pt-6 dark:border-neutral-800">
      <h2 className="text-sm font-semibold">계산 결과</h2>

      <div className="mt-3 rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
        <p className="text-xs text-neutral-500">조정 시작 콧수</p>
        <p className="mt-0.5 text-2xl font-semibold">{g.adjustedCastOnStitches}<span className="ml-1 text-base text-neutral-400">코</span></p>
        <p className="mt-1 font-mono text-xs text-neutral-400">{g.formula}</p>
      </div>

      <div className="mt-4 overflow-x-auto">
        <table className="w-full min-w-[520px] text-sm">
          <thead>
            <tr className="border-b border-neutral-200 text-left text-xs text-neutral-500 dark:border-neutral-800">
              <th className="py-2 pr-3">부위</th>
              <th className="py-2 pr-3">도안</th>
              <th className="py-2 pr-3">목표</th>
              <th className="py-2 pr-3">차이</th>
              <th className="py-2 pr-3">필요 콧수/단수</th>
              <th className="py-2 pr-3">계산식</th>
            </tr>
          </thead>
          <tbody>
            {result.sizeAdjustments.map((a) => (
              <tr key={a.key} className={`border-b border-neutral-100 dark:border-neutral-900 ${a.differenceCm !== 0 ? "bg-amber-50/50 dark:bg-amber-950/20" : ""}`}>
                <td className="py-2 pr-3 font-medium">{labels[a.key] ?? a.label}</td>
                <td className="py-2 pr-3">{a.patternValue}cm</td>
                <td className="py-2 pr-3">{a.targetValue}cm</td>
                <td className="py-2 pr-3">{a.differenceCm > 0 ? "+" : ""}{a.differenceCm}cm</td>
                <td className="py-2 pr-3 font-medium">{a.requiredStitches}</td>
                <td className="py-2 pr-3 font-mono text-xs text-neutral-400">{a.formula}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 flex flex-wrap gap-3 text-sm">
        <span className="rounded-md bg-neutral-100 px-3 py-1.5 dark:bg-neutral-800">
          바늘 추천: {DIRECTION_LABEL[result.needleRecommendation.direction] ?? result.needleRecommendation.direction}
          {result.needleRecommendation.suggestedMm != null && ` · ${result.needleRecommendation.suggestedMm}mm`}
        </span>
        {result.adjustmentSummary && (
          <span className="rounded-md bg-amber-100 px-3 py-1.5 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300">
            조정 요약: {result.adjustmentSummary}
          </span>
        )}
      </div>
      <p className="mt-1 text-xs text-neutral-400">{result.needleRecommendation.note}</p>

      {result.warnings.length > 0 && (
        <ul className="mt-3 space-y-1">
          {result.warnings.map((w, i) => <li key={i} className="text-xs text-neutral-500">⚠ {w}</li>)}
        </ul>
      )}

      <button type="button" onClick={onApply} disabled={applying}
        className="mt-6 w-full rounded-md border border-neutral-900 px-3 py-2.5 text-sm font-medium disabled:opacity-50 dark:border-neutral-100">
        {applying ? "적용 중…" : "이 계산을 니팅로그에 적용"}
      </button>
    </section>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}
