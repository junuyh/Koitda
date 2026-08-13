"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { adminApi } from "@/features/admin/api";
import { PatternStatusBadge } from "@/features/admin/StatusBadge";

const CRAFT_LABEL: Record<string, string> = { KNIT: "대바늘", CROCHET: "코바늘" , MIXED: "혼합" };
const MEASURE_LABEL: Record<string, string> = {
  chestCm: "가슴둘레", lengthCm: "총장", sleeveLengthCm: "소매길이", shoulderCm: "어깨너비",
};

export default function AdminPatternReviewPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const router = useRouter();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");

  const { data: p, isLoading, isError } = useQuery({
    queryKey: ["admin-pattern", id],
    queryFn: () => adminApi.patternDetail(id),
    retry: false,
  });

  async function invalidate() {
    await queryClient.invalidateQueries({ queryKey: ["admin-pattern", id] });
    await queryClient.invalidateQueries({ queryKey: ["admin-patterns"] });
  }

  const approve = useMutation({
    mutationFn: () => adminApi.approvePattern(id),
    onSuccess: async () => { await invalidate(); router.push("/admin/patterns"); },
    onError: (e) => setError(e instanceof ApiError ? e.message : "승인 중 오류가 발생했습니다."),
  });
  const reject = useMutation({
    mutationFn: () => adminApi.rejectPattern(id, reason.trim()),
    onSuccess: async () => { await invalidate(); router.push("/admin/patterns"); },
    onError: (e) => setError(e instanceof ApiError ? e.message : "반려 중 오류가 발생했습니다."),
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !p) return <Centered>도안을 찾을 수 없거나 접근 권한이 없습니다.</Centered>;

  const pending = p.productStatus === "PENDING";
  const price = p.salePrice ?? p.regularPrice;
  const measureKeys = keysOf(p.sizeInfo?.sizes ?? []);

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <Link href="/admin/patterns" className="text-xs text-neutral-500 hover:underline">← 도안 심사</Link>
      <div className="mt-2 flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">{p.title ?? "(제목 없음)"}</h1>
        <PatternStatusBadge status={p.productStatus} />
      </div>
      <p className="mt-1 text-sm text-neutral-500">
        {[p.sellerBrand, p.designerName].filter(Boolean).join(" · ") || "판매자"}
        {p.craftType ? ` · ${CRAFT_LABEL[p.craftType] ?? p.craftType}` : ""}
        {p.difficulty ? ` · ${p.difficulty}` : ""}
      </p>
      <p className="mt-2 text-lg font-semibold">{price != null ? `${price.toLocaleString()}원` : "가격 미정"}</p>

      {p.rejectionReason && (
        <p className="mt-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">
          이전 반려 사유: {p.rejectionReason}
        </p>
      )}

      {p.description && <p className="mt-4 whitespace-pre-wrap text-sm text-neutral-700 dark:text-neutral-300">{p.description}</p>}

      {p.gaugeInfo && (
        <Block title="게이지">
          <p className="text-sm">{p.gaugeInfo.stitches}코 × {p.gaugeInfo.rows}단 / {p.gaugeInfo.swatchWidthCm}×{p.gaugeInfo.swatchHeightCm}cm · 바늘 {p.gaugeInfo.needleSizeMm}mm</p>
        </Block>
      )}

      {p.sizeInfo?.sizes?.length ? (
        <Block title="사이즈 · 완성 실측">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-left text-xs text-neutral-500 dark:border-neutral-800">
                  <th className="py-2 pr-3">사이즈</th>
                  <th className="py-2 pr-3">시작 콧수</th>
                  {measureKeys.map((k) => <th key={k} className="py-2 pr-3">{MEASURE_LABEL[k] ?? k}</th>)}
                </tr>
              </thead>
              <tbody>
                {p.sizeInfo.sizes.map((s, i) => (
                  <tr key={i} className="border-b border-neutral-100 dark:border-neutral-900">
                    <td className="py-2 pr-3 font-medium">{s.label}</td>
                    <td className="py-2 pr-3">{s.castOnStitches}코</td>
                    {measureKeys.map((k) => <td key={k} className="py-2 pr-3">{s.measurements?.[k] != null ? `${s.measurements[k]}cm` : "–"}</td>)}
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
        {!pending ? (
          <p className="text-center text-sm text-neutral-500">현재 상태: {p.productStatus} — 심사 대기(PENDING) 도안만 처리할 수 있습니다.</p>
        ) : rejecting ? (
          <div className="space-y-3">
            <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={3}
              placeholder="반려 사유를 입력하세요 (판매자에게 전달됩니다)"
              className="w-full rounded-md border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100" />
            <div className="flex gap-2">
              <button type="button" onClick={() => { setError(null); if (!reason.trim()) { setError("반려 사유를 입력하세요."); return; } reject.mutate(); }}
                disabled={reject.isPending}
                className="flex-1 rounded-md bg-red-600 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50">
                {reject.isPending ? "반려 중…" : "반려 확정"}
              </button>
              <button type="button" onClick={() => { setRejecting(false); setReason(""); setError(null); }}
                className="rounded-md border border-neutral-300 px-3 py-2.5 text-sm dark:border-neutral-700">취소</button>
            </div>
          </div>
        ) : (
          <div className="flex gap-2">
            <button type="button" onClick={() => { setError(null); approve.mutate(); }} disabled={approve.isPending}
              className="flex-1 rounded-md bg-neutral-900 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
              {approve.isPending ? "승인 중…" : "승인"}
            </button>
            <button type="button" onClick={() => setRejecting(true)}
              className="rounded-md border border-red-300 px-4 py-2.5 text-sm font-medium text-red-600 dark:border-red-900">반려</button>
          </div>
        )}
      </div>
    </main>
  );
}

function keysOf(sizes: Array<{ measurements?: Record<string, number> }>): string[] {
  const keys: string[] = [];
  for (const s of sizes) for (const k of Object.keys(s.measurements ?? {})) if (!keys.includes(k)) keys.push(k);
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
