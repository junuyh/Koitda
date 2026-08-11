"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { sellerPatternApi } from "@/features/seller/api";

const CRAFT_LABEL: Record<string, string> = { KNIT: "대바늘", CROCHET: "코바늘" };
const MEASURE_LABEL: Record<string, string> = {
  chestCm: "가슴둘레",
  lengthCm: "총장",
  sleeveLengthCm: "소매길이",
  shoulderCm: "어깨너비",
};

export default function PatternPreviewPage() {
  const params = useParams<{ draftId: string }>();
  const draftId = Number(params.draftId);
  const router = useRouter();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const { data: p, isLoading, isError } = useQuery({
    queryKey: ["seller-pattern-preview", draftId],
    queryFn: () => sellerPatternApi.preview(draftId),
  });

  const submit = useMutation({
    mutationFn: () => sellerPatternApi.submit(draftId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["seller-pattern-preview", draftId] });
      await queryClient.invalidateQueries({ queryKey: ["seller-patterns"] });
      router.push("/seller/patterns");
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "제출 중 오류가 발생했습니다."),
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !p) return <Centered>도안을 찾을 수 없습니다.</Centered>;

  const canSubmit = p.productStatus === "DRAFT" || p.productStatus === "REJECTED";
  const priceText = p.salePrice != null ? `${p.salePrice.toLocaleString()}원` : p.regularPrice != null ? `${p.regularPrice.toLocaleString()}원` : "가격 미정";

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <div className="flex items-center justify-between">
        <Link href={`/seller/patterns/${draftId}/edit`} className="text-xs text-neutral-500 hover:underline">← 수정으로</Link>
        <span className="text-xs text-neutral-400">미리보기 (구매자에게 보이는 화면)</span>
      </div>

      <h1 className="mt-3 text-2xl font-semibold tracking-tight">{p.title ?? "(제목 없음)"}</h1>
      <p className="mt-1 text-sm text-neutral-500">
        {[p.sellerBrand, p.designerName].filter(Boolean).join(" · ") || "판매자"}
        {p.craftType ? ` · ${CRAFT_LABEL[p.craftType] ?? p.craftType}` : ""}
        {p.difficulty ? ` · ${p.difficulty}` : ""}
      </p>
      <p className="mt-2 text-lg font-semibold">{priceText}</p>

      {p.description && <p className="mt-4 whitespace-pre-wrap text-sm text-neutral-700 dark:text-neutral-300">{p.description}</p>}

      {/* 게이지 */}
      {p.gaugeInfo && (
        <Block title="게이지">
          <p className="text-sm">
            {p.gaugeInfo.stitches}코 × {p.gaugeInfo.rows}단 / {p.gaugeInfo.swatchWidthCm}×{p.gaugeInfo.swatchHeightCm}cm · 바늘 {p.gaugeInfo.needleSizeMm}mm
          </p>
        </Block>
      )}

      {/* 사이즈별 시작 콧수·실측 (PATTERN-004) */}
      {p.sizeInfo?.sizes?.length ? (
        <Block title="사이즈 · 완성 실측">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-left text-xs text-neutral-500 dark:border-neutral-800">
                  <th className="py-2 pr-3">사이즈</th>
                  <th className="py-2 pr-3">시작 콧수</th>
                  {measurementKeys(p.sizeInfo.sizes).map((k) => (
                    <th key={k} className="py-2 pr-3">{MEASURE_LABEL[k] ?? k}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {p.sizeInfo.sizes.map((s, i) => (
                  <tr key={i} className="border-b border-neutral-100 dark:border-neutral-900">
                    <td className="py-2 pr-3 font-medium">{s.label}</td>
                    <td className="py-2 pr-3">{s.castOnStitches}코</td>
                    {measurementKeys(p.sizeInfo!.sizes).map((k) => (
                      <td key={k} className="py-2 pr-3">{s.measurements?.[k] != null ? `${s.measurements[k]}cm` : "–"}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Block>
      ) : null}

      {p.yarnRequirement && <Block title="실 소요량"><p className="text-sm">{p.yarnRequirement}</p></Block>}
      {p.pageCount != null && <Block title="페이지 수"><p className="text-sm">{p.pageCount}쪽</p></Block>}

      {error && <p role="alert" className="mt-4 text-sm text-red-600">{error}</p>}

      <div className="mt-8 border-t border-neutral-200 pt-4 dark:border-neutral-800">
        {canSubmit ? (
          <button type="button" disabled={submit.isPending} onClick={() => { setError(null); submit.mutate(); }}
            className="w-full rounded-md bg-neutral-900 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
            {submit.isPending ? "제출 중…" : "심사 제출"}
          </button>
        ) : (
          <p className="text-center text-sm text-neutral-500">현재 상태: {p.productStatus} — 제출할 수 없습니다.</p>
        )}
      </div>
    </main>
  );
}

function measurementKeys(sizes: Array<{ measurements?: Record<string, number> }>): string[] {
  const keys: string[] = [];
  for (const s of sizes) {
    for (const k of Object.keys(s.measurements ?? {})) {
      if (!keys.includes(k)) keys.push(k);
    }
  }
  return keys;
}

function Block({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-5">
      <h2 className="mb-1 text-sm font-semibold">{title}</h2>
      {children}
    </section>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}
