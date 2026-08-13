"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { authApi } from "@/features/auth/api";
import { patternApi, type KnittingStats } from "@/features/pattern/api";
import { orderApi } from "@/features/order/api";
import { ReviewSection } from "@/features/review/ReviewSection";
import { RichContent } from "@/features/editor/RichContent";
import { accentOf } from "@/features/ui/accent";
import type { JSONContent } from "@tiptap/react";
import { ApiError } from "@/lib/api";

const CRAFT_LABEL: Record<string, string> = { KNIT: "대바늘", CROCHET: "코바늘" };

/** YouTube URL → 임베드 URL. 유튜브가 아니면 null(링크로 폴백). */
function youtubeEmbed(url: string): string | null {
  const m = url.match(/(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|shorts\/))([\w-]{11})/);
  return m ? `https://www.youtube.com/embed/${m[1]}` : null;
}
const MEASURE_LABEL: Record<string, string> = {
  chestCm: "가슴둘레",
  lengthCm: "총장",
  sleeveLengthCm: "소매길이",
  shoulderCm: "어깨너비",
};

export default function PatternDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: me } = useQuery({ queryKey: ["me"], queryFn: authApi.me });
  const { data: p, isLoading, isError } = useQuery({
    queryKey: ["pattern", id],
    queryFn: () => patternApi.get(id),
  });
  const { data: stats } = useQuery({
    queryKey: ["knitting-stats", id],
    queryFn: () => patternApi.knittingStats(id),
    retry: false,
  });

  const wish = useMutation({
    mutationFn: () => (p?.wished ? patternApi.removeWish(id) : patternApi.addWish(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pattern", id] }),
  });

  const { data: purchasability } = useQuery({
    queryKey: ["purchasability", id],
    queryFn: () => orderApi.purchasability(id),
    enabled: !!me,
  });

  // 데모: 주문 생성 후 곧바로 결제 완료 처리한다.
  const purchase = useMutation({
    mutationFn: async () => {
      const order = await orderApi.createOrder(id);
      await orderApi.completePayment(order.id);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["purchasability", id] });
      router.push("/library");
    },
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "구매 처리 중 오류가 발생했습니다."),
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !p) return <Centered>도안을 찾을 수 없습니다.</Centered>;

  const measurementKeys = p.sizeInfo?.sizes?.[0]
    ? Object.keys(p.sizeInfo.sizes[0].measurements)
    : [];
  const accent = accentOf(p.id);
  const cover = p.images?.find((im) => im.thumbnail)?.url ?? p.images?.[0]?.url;

  return (
    <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8">
      <Link href="/patterns" className="text-sm font-bold text-neutral-500 hover:underline">← 목록</Link>

      <div className="mt-4 grid gap-6 overflow-hidden rounded-3xl border-2 border-neutral-900 bg-white p-5 md:grid-cols-2 dark:border-neutral-100 dark:bg-neutral-950">
        {/* 대표 이미지 — 깨지면 글자 placeholder 로 폴백 */}
        <div className={`relative flex aspect-square items-center justify-center overflow-hidden rounded-2xl border-2 border-neutral-900 bg-gradient-to-br text-6xl font-black text-neutral-900/20 dark:border-neutral-100 dark:text-neutral-100/20 ${accent.wash}`}>
          <span>{p.title.slice(0, 1)}</span>
          {cover && (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={cover as string} alt={p.title}
              onError={(e) => { e.currentTarget.style.display = "none"; }}
              className="absolute inset-0 h-full w-full object-cover" />
          )}
          {p.craftType && (
            <span className={`absolute left-3 top-3 rounded-full border-2 border-neutral-900 px-2.5 py-0.5 text-xs font-bold dark:border-neutral-100 ${accent.solid}`}>
              {CRAFT_LABEL[p.craftType] ?? p.craftType}
            </span>
          )}
        </div>

        <div className="flex flex-col">
          {p.categoryName && (
            <span className={`inline-block w-fit rounded-full px-3 py-0.5 text-xs font-semibold ${accent.soft}`}>{p.categoryName}</span>
          )}
          <h1 className="mt-2 text-3xl font-black tracking-tight">{p.title}</h1>
          <p className="mt-1 text-sm text-neutral-500">
            {p.designerName ?? "원작자 미상"}
            {p.sellerBrand && ` · ${p.sellerBrand}`}
          </p>

          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-3xl font-black">
              {p.salePrice != null ? `${p.salePrice.toLocaleString()}원` : "-"}
            </span>
            {p.regularPrice != null && p.regularPrice !== p.salePrice && (
              <span className="text-sm text-neutral-400 line-through">
                {p.regularPrice.toLocaleString()}원
              </span>
            )}
          </div>

          <dl className="mt-4 grid grid-cols-2 gap-y-1.5 text-sm">
            <Meta label="뜨개 방식" value={p.craftType ? CRAFT_LABEL[p.craftType] : null} />
            <Meta label="난이도" value={p.difficulty} />
            <Meta label="언어" value={p.language} />
            <Meta label="페이지" value={p.pageCount != null ? `${p.pageCount}p` : null} />
            <Meta label="위시" value={`${p.wishCount}`} />
            <Meta label="공개 니팅로그" value={`${p.publicProjectCount}`} />
          </dl>

          <div className="mt-5 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                if (!me) return router.push("/login");
                wish.mutate();
              }}
              disabled={wish.isPending}
              aria-pressed={p.wished}
              aria-label={p.wished ? "위시 해제" : "위시 등록"}
              className="flex items-center gap-1.5 rounded-full border-2 border-neutral-900 px-4 py-2 text-sm font-bold dark:border-neutral-100"
            >
              <span className={p.wished ? "text-red-500" : "text-neutral-400"}>{p.wished ? "♥" : "♡"}</span>
              위시
            </button>
            <Link
              href={me ? `/projects/new?sellingPatternId=${p.id}` : "/login"}
              className="rounded-full border-2 border-neutral-900 px-4 py-2 text-sm font-bold dark:border-neutral-100"
            >
              니팅로그 만들기
            </Link>

            {/* 구매 상태별 버튼(ORDER-005) */}
            {!me ? (
              <Link href="/login" className="rounded-full border-2 border-neutral-900 bg-neutral-900 px-4 py-2 text-sm font-bold text-white transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900">
                로그인 후 구매
              </Link>
            ) : purchasability && !purchasability.canPurchase && purchasability.reason?.includes("보유") ? (
              <Link href={`/library/${p.id}`} className={`rounded-full border-2 border-neutral-900 px-4 py-2 text-sm font-bold dark:border-neutral-100 ${accent.solid}`}>
                구매 도안 보기
              </Link>
            ) : (
              <button
                type="button"
                onClick={() => purchase.mutate()}
                disabled={purchase.isPending || (purchasability?.canPurchase === false && p.salePrice !== 0)}
                className="rounded-full border-2 border-neutral-900 bg-neutral-900 px-4 py-2 text-sm font-bold text-white transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] disabled:opacity-50 disabled:shadow-none dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900"
              >
                {purchase.isPending ? "구매 중…"
                  : `구매하기${p.salePrice != null ? ` · ${p.salePrice === 0 ? "무료" : `${p.salePrice.toLocaleString()}원`}` : ""}`}
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="mt-6 space-y-4">
      {/* 니팅로그 집계 — 코잇다의 핵심: 다른 사람들이 실제로 어떻게 떴는지 */}
      {stats && stats.projectCount > 0 && <KnittingStatsSection stats={stats} />}

      {/* 게이지 — 모든 도안 통일 표시(미등록도 항목 노출) */}
      <Section title="게이지">
        {p.gaugeInfo ? (
          <p className="text-sm text-neutral-700 dark:text-neutral-300">
            {p.gaugeInfo.stitches}코 × {p.gaugeInfo.rows}단
            {p.gaugeInfo.swatchWidthCm && ` (${p.gaugeInfo.swatchWidthCm}×${p.gaugeInfo.swatchHeightCm}cm)`}
            {p.gaugeInfo.needleSizeMm && ` · 바늘 ${p.gaugeInfo.needleSizeMm}mm`}
          </p>
        ) : <Empty />}
      </Section>

      {/* 사이즈별 시작 콧수·완성 실측 */}
      <Section title="사이즈">
        {p.sizeInfo && p.sizeInfo.sizes.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[420px] border-collapse text-sm">
              <thead>
                <tr className="border-b-2 border-neutral-900 text-left dark:border-neutral-100">
                  <th className="py-2 pr-4 font-bold">사이즈</th>
                  <th className="py-2 pr-4 font-bold">시작 콧수</th>
                  {measurementKeys.map((k) => (
                    <th key={k} className="py-2 pr-4 font-bold">{MEASURE_LABEL[k] ?? k}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {p.sizeInfo.sizes.map((s) => (
                  <tr key={s.label} className="border-b border-neutral-100 dark:border-neutral-900">
                    <td className="py-2 pr-4">{s.label}</td>
                    <td className="py-2 pr-4">{s.castOnStitches}코</td>
                    {measurementKeys.map((k) => (
                      <td key={k} className="py-2 pr-4">{s.measurements[k] ?? "-"}cm</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <Empty />}
      </Section>

      <Section title="실 소요량">
        {p.yarnRequirement ? <p className="text-sm">{p.yarnRequirement}</p> : <Empty />}
      </Section>

      <Section title="상세 설명">
        {p.descriptionDocument ? (
          <RichContent doc={p.descriptionDocument as JSONContent} />
        ) : p.description ? (
          <p className="whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{p.description}</p>
        ) : <Empty />}
      </Section>

      {p.referenceVideoUrl && (
        <Section title="참고 영상">
          {youtubeEmbed(p.referenceVideoUrl) ? (
            <div className="relative w-full overflow-hidden rounded-xl border-2 border-neutral-900 pb-[56.25%] dark:border-neutral-100">
              <iframe src={youtubeEmbed(p.referenceVideoUrl) as string} title="참고 영상" allowFullScreen
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                className="absolute inset-0 h-full w-full" />
            </div>
          ) : (
            <a href={p.referenceVideoUrl} target="_blank" rel="noreferrer" className="text-sm text-blue-600 underline">영상 보기</a>
          )}
        </Section>
      )}
      </div>

      <ReviewSection patternId={id} loggedIn={!!me} />
    </main>
  );
}

/** 니팅로그 집계 — 이 도안을 다른 사람들이 어떤 실·바늘·게이지로 떴는지(코잇다의 차별점). */
function KnittingStatsSection({ stats }: { stats: KnittingStats }) {
  const max = (arr: Array<{ count: number }>) => Math.max(1, ...arr.map((x) => x.count));
  return (
    <section className="rounded-2xl border-2 border-neutral-900 bg-amber-50 p-5 dark:border-neutral-100 dark:bg-amber-950/20">
      <div className="flex items-baseline justify-between">
        <h2 className="text-sm font-black uppercase tracking-[0.15em] text-amber-700 dark:text-amber-300">니팅로그로 보는 실제 제작</h2>
        <p className="text-sm font-bold text-neutral-600 dark:text-neutral-300">
          {stats.projectCount}명이 떴어요{stats.finishedCount > 0 ? ` · 완성 ${stats.finishedCount}` : ""}
        </p>
      </div>

      <div className="mt-4 grid gap-5 sm:grid-cols-3">
        <StatBlock title="🧶 자주 쓴 실" empty="아직 실 기록이 없어요"
          rows={stats.yarns.map((y) => ({ label: y.label, count: y.count }))} max={max(stats.yarns)} />
        <StatBlock title="🪡 자주 쓴 바늘" empty="아직 바늘 기록이 없어요"
          rows={stats.needles.map((n) => ({ label: `${n.sizeMm}mm`, count: n.count }))} max={max(stats.needles)} />
        <StatBlock title="📏 게이지 분포" empty="아직 게이지 기록이 없어요"
          rows={stats.gauges.map((g) => ({ label: `${g.stitches}코 × ${g.rows}단`, count: g.count }))} max={max(stats.gauges)} />
      </div>
      <p className="mt-3 text-xs text-neutral-400">구매자·제작자들의 니팅로그 기록을 익명 집계한 값입니다.</p>
    </section>
  );
}

function StatBlock({ title, rows, max, empty }: {
  title: string; rows: Array<{ label: string; count: number }>; max: number; empty: string;
}) {
  return (
    <div>
      <p className="mb-2 text-xs font-bold text-neutral-500">{title}</p>
      {rows.length === 0 ? (
        <p className="text-xs text-neutral-400">{empty}</p>
      ) : (
        <ul className="space-y-1.5">
          {rows.map((r, i) => (
            <li key={i}>
              <div className="flex items-center justify-between gap-2 text-xs">
                <span className="line-clamp-1 font-medium text-neutral-800 dark:text-neutral-200">{r.label}</span>
                <span className="shrink-0 font-bold text-neutral-500">{r.count}</span>
              </div>
              <div className="mt-0.5 h-1.5 overflow-hidden rounded-full bg-neutral-200 dark:bg-neutral-800">
                <div className="h-full rounded-full bg-amber-400" style={{ width: `${Math.round((r.count / max) * 100)}%` }} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}

function Empty() {
  return <p className="text-sm text-neutral-400">미등록</p>;
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border-2 border-neutral-900 bg-white p-5 dark:border-neutral-100 dark:bg-neutral-950">
      <h2 className="mb-3 text-xs font-bold uppercase tracking-[0.15em] text-neutral-400">{title}</h2>
      {children}
    </section>
  );
}

function Meta({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return (
    <>
      <dt className="text-neutral-400">{label}</dt>
      <dd className="font-semibold">{value}</dd>
    </>
  );
}
