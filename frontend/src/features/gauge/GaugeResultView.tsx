"use client";

import type { CalculationResult } from "@/features/gauge/api";

const DIRECTION_LABEL: Record<string, string> = { LARGER: "더 굵은 바늘", SMALLER: "더 얇은 바늘", SAME: "동일" };

/**
 * 게이지 계산 결과 전체 뷰 — 조정 콧수·부위별 필요 콧수/단수·바늘 추천·경고.
 * 게이지 계산 페이지와 니팅로그 상세(적용 결과)에서 공유한다.
 */
export function GaugeResultView({
  result,
  labels = {},
}: {
  result: CalculationResult;
  labels?: Record<string, string>;
}) {
  const g = result.gaugeAdjustment;
  return (
    <div>
      {/* 조정 시작 콧수는 도안에 시작 콧수가 있을 때만. 없으면 아래 '부위별 필요 콧수'(치수 기준)가 실계산이다. */}
      {g.adjustedCastOnStitches != null && (
        <div className="rounded-2xl border-2 border-neutral-900 p-4 dark:border-neutral-100">
          <p className="text-xs text-neutral-500">조정 시작 콧수</p>
          <p className="mt-0.5 text-2xl font-black">{g.adjustedCastOnStitches}<span className="ml-1 text-base font-normal text-neutral-400">코</span></p>
          <p className="mt-1 font-mono text-xs text-neutral-400">{g.formula}</p>
        </div>
      )}

      <div className="mt-4 overflow-x-auto">
        <table className="w-full min-w-[520px] text-sm">
          <thead>
            <tr className="border-b-2 border-neutral-900 text-left text-xs text-neutral-500 dark:border-neutral-100">
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
                <td className="py-2 pr-3 font-bold">{labels[a.key] ?? a.label}</td>
                <td className="py-2 pr-3">{a.patternValue}cm</td>
                <td className="py-2 pr-3">{a.targetValue}cm</td>
                <td className="py-2 pr-3">{a.differenceCm > 0 ? "+" : ""}{a.differenceCm}cm</td>
                <td className="py-2 pr-3 font-bold">{a.requiredStitches}</td>
                <td className="py-2 pr-3 font-mono text-xs text-neutral-400">{a.formula}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 flex flex-wrap gap-2 text-sm">
        <span className="rounded-full border-2 border-neutral-900 px-3 py-1 font-bold dark:border-neutral-100">
          바늘 추천: {DIRECTION_LABEL[result.needleRecommendation.direction] ?? result.needleRecommendation.direction}
          {result.needleRecommendation.suggestedMm != null && ` · ${result.needleRecommendation.suggestedMm}mm`}
        </span>
        {result.adjustmentSummary && (
          <span className="rounded-full bg-amber-100 px-3 py-1 font-semibold text-amber-800 dark:bg-amber-950/40 dark:text-amber-300">
            조정 요약: {result.adjustmentSummary}
          </span>
        )}
      </div>
      {result.needleRecommendation.note && <p className="mt-1 text-xs text-neutral-400">{result.needleRecommendation.note}</p>}

      {result.warnings.length > 0 && (
        <ul className="mt-3 space-y-1">
          {result.warnings.map((w, i) => <li key={i} className="text-xs text-neutral-500">⚠ {w}</li>)}
        </ul>
      )}
    </div>
  );
}
