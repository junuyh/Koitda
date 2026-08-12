"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ApiError } from "@/lib/api";
import { orderApi } from "@/features/order/api";

export default function LibraryDetailPage() {
  const params = useParams<{ patternId: string }>();
  const patternId = Number(params.patternId);
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["library-detail", patternId],
    queryFn: () => orderApi.libraryDetail(patternId),
    retry: false,
  });

  const download = useMutation({
    mutationFn: () => orderApi.downloadPdf(patternId, `${data?.patternTitle ?? "pattern"}.pdf`),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ["library-detail", patternId] });
      window.alert(`다운로드를 시작합니다. 남은 횟수 ${r.remaining}회`);
    },
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "다운로드 중 오류가 발생했습니다."),
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !data) return <Centered>구매하지 않은 도안이거나 접근 권한이 없습니다.</Centered>;

  return (
    <main className="mx-auto w-full max-w-xl flex-1 px-4 py-8">
      <Link href="/library" className="text-sm text-neutral-500 hover:underline">← 구매 도안</Link>

      <div className="mt-4 rounded-2xl border border-neutral-200 bg-white p-6 dark:border-neutral-800 dark:bg-neutral-950">
        <p className="text-xs font-semibold uppercase tracking-wider text-neutral-400">구매한 도안</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight">{data.patternTitle}</h1>
        {data.sellerBrand && <p className="mt-1 text-sm text-neutral-500">{data.sellerBrand}</p>}

        <dl className="mt-4 space-y-1.5 text-sm">
          <Row label="구매일">{new Date(data.purchasedAt).toLocaleDateString("ko-KR")}</Row>
          <Row label="다운로드">{data.downloadCount} / {data.downloadLimit}회</Row>
          {data.revoked && <Row label="상태"><span className="text-red-500">환불로 회수됨</span></Row>}
        </dl>

        <div className="mt-6 border-t border-neutral-200 pt-4 dark:border-neutral-800">
          {data.revoked ? (
            <p className="text-sm text-neutral-500">환불된 도안은 다운로드할 수 없습니다.</p>
          ) : !data.hasPdf ? (
            <p className="text-sm text-neutral-500">아직 판매자가 PDF를 등록하지 않았습니다.</p>
          ) : data.downloadCount >= data.downloadLimit ? (
            <p className="text-sm text-neutral-500">다운로드 한도({data.downloadLimit}회)를 모두 사용했습니다.</p>
          ) : (
            <button type="button" onClick={() => download.mutate()} disabled={download.isPending}
              className="w-full rounded-lg bg-neutral-900 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
              {download.isPending ? "준비 중…" : "PDF 다운로드"}
            </button>
          )}
          <p className="mt-2 text-xs text-neutral-400">개인 사용 목적의 저작물입니다. 재배포·공유는 금지됩니다.</p>
        </div>
      </div>
    </main>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-3">
      <dt className="w-16 shrink-0 text-neutral-400">{label}</dt>
      <dd className="text-neutral-800 dark:text-neutral-200">{children}</dd>
    </div>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}
