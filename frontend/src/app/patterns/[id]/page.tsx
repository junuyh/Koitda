"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { authApi } from "@/features/auth/api";
import { patternApi } from "@/features/pattern/api";

const CRAFT_LABEL: Record<string, string> = { KNIT: "대바늘", CROCHET: "코바늘" };
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

  const wish = useMutation({
    mutationFn: () => (p?.wished ? patternApi.removeWish(id) : patternApi.addWish(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pattern", id] }),
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !p) return <Centered>도안을 찾을 수 없습니다.</Centered>;

  const measurementKeys = p.sizeInfo?.sizes?.[0]
    ? Object.keys(p.sizeInfo.sizes[0].measurements)
    : [];

  return (
    <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8">
      <Link href="/patterns" className="text-sm text-neutral-500 hover:underline">← 목록</Link>

      <div className="mt-4 grid gap-8 md:grid-cols-2">
        {/* 대표 이미지 자리 */}
        <div className="flex aspect-square items-center justify-center rounded-lg bg-neutral-100 text-5xl font-semibold text-neutral-300 dark:bg-neutral-900">
          {p.title.slice(0, 1)}
        </div>

        <div>
          {p.categoryName && <p className="text-xs text-neutral-500">{p.categoryName}</p>}
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">{p.title}</h1>
          <p className="mt-1 text-sm text-neutral-500">
            {p.designerName ?? "원작자 미상"}
            {p.sellerBrand && ` · ${p.sellerBrand}`}
          </p>

          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-2xl font-bold">
              {p.salePrice != null ? `${p.salePrice.toLocaleString()}원` : "-"}
            </span>
            {p.regularPrice != null && p.regularPrice !== p.salePrice && (
              <span className="text-sm text-neutral-400 line-through">
                {p.regularPrice.toLocaleString()}원
              </span>
            )}
          </div>

          <dl className="mt-4 grid grid-cols-2 gap-y-1 text-sm">
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
              className="rounded-md border border-neutral-300 px-4 py-2 text-sm font-medium dark:border-neutral-700"
            >
              {p.wished ? "♥ 위시 담김" : "♡ 위시 담기"}
            </button>
            <Link
              href={me ? `/projects/new?sellingPatternId=${p.id}` : "/login"}
              className="rounded-md bg-neutral-900 px-4 py-2 text-sm font-medium text-white dark:bg-neutral-100 dark:text-neutral-900"
            >
              니팅로그 만들기
            </Link>
          </div>
        </div>
      </div>

      {/* 게이지 */}
      {p.gaugeInfo && (
        <Section title="게이지">
          <p className="text-sm text-neutral-700 dark:text-neutral-300">
            {p.gaugeInfo.stitches}코 × {p.gaugeInfo.rows}단
            {p.gaugeInfo.swatchWidthCm && ` (${p.gaugeInfo.swatchWidthCm}×${p.gaugeInfo.swatchHeightCm}cm)`}
            {p.gaugeInfo.needleSizeMm && ` · 바늘 ${p.gaugeInfo.needleSizeMm}mm`}
          </p>
        </Section>
      )}

      {/* 사이즈별 시작 콧수·완성 실측 */}
      {p.sizeInfo && p.sizeInfo.sizes.length > 0 && (
        <Section title="사이즈">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[420px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-left dark:border-neutral-800">
                  <th className="py-2 pr-4 font-medium">사이즈</th>
                  <th className="py-2 pr-4 font-medium">시작 콧수</th>
                  {measurementKeys.map((k) => (
                    <th key={k} className="py-2 pr-4 font-medium">{MEASURE_LABEL[k] ?? k}</th>
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
        </Section>
      )}

      {p.yarnRequirement && (
        <Section title="실 소요량"><p className="text-sm">{p.yarnRequirement}</p></Section>
      )}
      {p.description && (
        <Section title="상세 설명">
          <p className="whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{p.description}</p>
        </Section>
      )}
      {p.referenceVideoUrl && (
        <Section title="참고 영상">
          <a href={p.referenceVideoUrl} target="_blank" rel="noreferrer" className="text-sm text-blue-600 underline">
            영상 보기
          </a>
        </Section>
      )}
    </main>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-8 border-t border-neutral-200 pt-6 dark:border-neutral-800">
      <h2 className="mb-2 text-sm font-semibold">{title}</h2>
      {children}
    </section>
  );
}

function Meta({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return (
    <>
      <dt className="text-neutral-500">{label}</dt>
      <dd>{value}</dd>
    </>
  );
}
