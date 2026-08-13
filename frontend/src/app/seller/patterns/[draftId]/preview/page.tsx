"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { sellerPatternApi, type SellerPatternPreview } from "@/features/seller/api";
import { accentOf } from "@/features/ui/accent";
import { RichContent } from "@/features/editor/RichContent";
import type { JSONContent } from "@tiptap/react";

const CRAFT_LABEL: Record<string, string> = { KNIT: "대바늘", CROCHET: "코바늘", MIXED: "혼합" };
const MEASURE_LABEL: Record<string, string> = {
  chestCm: "가슴둘레", lengthCm: "총장", sleeveLengthCm: "소매길이", shoulderCm: "어깨너비", armholeCm: "암홀", widthCm: "가로", heightCm: "세로",
};

function youtubeEmbed(url: string): string | null {
  const m = url.match(/(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|shorts\/))([\w-]{11})/);
  return m ? `https://www.youtube.com/embed/${m[1]}` : null;
}

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
  const accent = accentOf(p.id);
  const cover = p.images?.find((im) => im.thumbnail)?.url ?? p.images?.[0]?.url ?? null;
  const measureKeys = p.sizeInfo?.sizes ? measurementKeys(p.sizeInfo.sizes) : [];
  const g = p.gaugeInfo as (SellerPatternPreview["gaugeInfo"] & { text?: string }) | null;

  return (
    <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8">
      <div className="flex items-center justify-between">
        <Link href={`/seller/patterns/${draftId}/edit`} className="text-sm font-bold text-neutral-500 hover:underline">← 수정으로</Link>
        <span className="rounded-full border-2 border-neutral-900 px-3 py-1 text-xs font-bold dark:border-neutral-100">미리보기 · 구매자에게 보이는 화면</span>
      </div>

      {/* 상단 카드 — 실제 도안 상세와 동일 레이아웃 */}
      <div className="mt-4 grid gap-6 overflow-hidden rounded-3xl border-2 border-neutral-900 bg-white p-5 md:grid-cols-2 dark:border-neutral-100 dark:bg-neutral-950">
        <div className={`relative flex aspect-square items-center justify-center overflow-hidden rounded-2xl border-2 border-neutral-900 bg-gradient-to-br text-6xl font-black text-neutral-900/20 dark:border-neutral-100 dark:text-neutral-100/20 ${accent.wash}`}>
          <span>{(p.title ?? "도").slice(0, 1)}</span>
          {cover && (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={cover} alt="" onError={(e) => { e.currentTarget.style.display = "none"; }} className="absolute inset-0 h-full w-full object-cover" />
          )}
          {p.craftType && (
            <span className={`absolute left-3 top-3 rounded-full border-2 border-neutral-900 px-2.5 py-0.5 text-xs font-bold dark:border-neutral-100 ${accent.solid}`}>
              {CRAFT_LABEL[p.craftType] ?? p.craftType}
            </span>
          )}
        </div>

        <div className="flex flex-col">
          <h1 className="text-3xl font-black tracking-tight">{p.title ?? "(제목 없음)"}</h1>
          <p className="mt-1 text-sm text-neutral-500">{[p.sellerBrand, p.designerName].filter(Boolean).join(" · ") || "판매자"}</p>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-black">
              {p.salePrice === 0 ? "무료" : p.salePrice != null ? `${p.salePrice.toLocaleString()}원` : "가격 미정"}
            </span>
            {p.regularPrice != null && p.salePrice != null && p.regularPrice > p.salePrice && (
              <span className="text-sm text-neutral-400 line-through">{p.regularPrice.toLocaleString()}원</span>
            )}
          </div>
          <dl className="mt-4 grid grid-cols-2 gap-y-1.5 text-sm">
            <Meta label="뜨개 방식" value={p.craftType ? CRAFT_LABEL[p.craftType] : null} />
            <Meta label="난이도" value={p.difficulty} />
            <Meta label="언어" value={p.language} />
            <Meta label="페이지" value={p.pageCount != null ? `${p.pageCount}p` : null} />
          </dl>
        </div>
      </div>

      <div className="mt-6 space-y-4">
        {(g?.stitches != null || g?.text) && (
          <Block title="게이지">
            {g?.stitches != null && (
              <p className="text-sm">{g.stitches}코 × {g.rows}단{g.swatchWidthCm ? ` (${g.swatchWidthCm}×${g.swatchHeightCm}cm)` : ""}{g.needleSizeMm ? ` · 바늘 ${g.needleSizeMm}mm` : ""}</p>
            )}
            {g?.text && <p className="mt-1 whitespace-pre-line text-sm text-neutral-600 dark:text-neutral-300">{g.text}</p>}
          </Block>
        )}

        {p.sizeInfo?.sizes?.length ? (
          <Block title="사이즈 · 완성 실측">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[360px] text-sm">
                <thead>
                  <tr className="border-b-2 border-neutral-900 text-left dark:border-neutral-100">
                    <th className="py-1.5 pr-4 font-bold">사이즈</th>
                    {measureKeys.map((k) => <th key={k} className="py-1.5 pr-4 font-bold">{MEASURE_LABEL[k] ?? k}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {p.sizeInfo.sizes.map((s, i) => (
                    <tr key={i} className="border-b border-neutral-200 dark:border-neutral-800">
                      <td className="py-1.5 pr-4">{s.label}</td>
                      {measureKeys.map((k) => <td key={k} className="py-1.5 pr-4">{s.measurements?.[k] != null ? `${s.measurements[k]}cm` : "–"}</td>)}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Block>
        ) : null}

        {p.yarnRequirement && <Block title="실 소요량"><p className="whitespace-pre-line text-sm">{p.yarnRequirement}</p></Block>}

        {p.referenceVideoUrl && youtubeEmbed(p.referenceVideoUrl) && (
          <Block title="참고 영상">
            <div className="relative w-full overflow-hidden rounded-xl border-2 border-neutral-900 pb-[56.25%] dark:border-neutral-100">
              <iframe src={youtubeEmbed(p.referenceVideoUrl) as string} title="참고 영상" allowFullScreen className="absolute inset-0 h-full w-full" />
            </div>
          </Block>
        )}

        {(p.descriptionDocument || p.description) && (
          <Block title="상세 설명">
            {p.descriptionDocument ? <RichContent doc={p.descriptionDocument as JSONContent} />
              : <p className="whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{p.description}</p>}
          </Block>
        )}
      </div>

      {error && <p role="alert" className="mt-4 text-sm text-red-600">{error}</p>}

      <div className="mt-8">
        {canSubmit ? (
          <button type="button" disabled={submit.isPending} onClick={() => { setError(null); submit.mutate(); }}
            className="w-full rounded-full border-2 border-neutral-900 bg-neutral-900 px-4 py-3.5 text-sm font-black text-white transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] disabled:opacity-50 dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900">
            {submit.isPending ? "제출 중…" : "이 도안 심사 제출하기"}
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
  for (const s of sizes) for (const k of Object.keys(s.measurements ?? {})) if (!keys.includes(k)) keys.push(k);
  return keys;
}

function Meta({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return <><dt className="text-neutral-400">{label}</dt><dd className="font-semibold">{value}</dd></>;
}

function Block({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border-2 border-neutral-900 bg-white p-5 dark:border-neutral-100 dark:bg-neutral-950">
      <h2 className="mb-3 text-xs font-bold uppercase tracking-[0.15em] text-neutral-400">{title}</h2>
      {children}
    </section>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}
