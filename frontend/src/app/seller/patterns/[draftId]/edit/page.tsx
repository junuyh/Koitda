"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { sellerPatternApi, type DraftSaved, type SavePatternDraftBody } from "@/features/seller/api";
import { PatternForm } from "@/features/seller/PatternForm";

const MISSING_LABEL: Record<string, string> = {
  title: "도안명",
  craftType: "뜨개 방식",
  regularPrice: "정상가",
  gauge: "게이지",
  sizes: "사이즈",
  images: "이미지",
  pdf: "PDF 파일",
};

export default function EditPatternPage() {
  const params = useParams<{ draftId: string }>();
  const draftId = Number(params.draftId);
  const router = useRouter();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<DraftSaved | null>(null);

  const { data: preview, isLoading, isError } = useQuery({
    queryKey: ["seller-pattern-preview", draftId],
    queryFn: () => sellerPatternApi.preview(draftId),
  });

  const save = useMutation({
    mutationFn: (body: SavePatternDraftBody) => sellerPatternApi.updateDraft(draftId, body),
    onSuccess: async (res) => {
      setSaved(res);
      await queryClient.invalidateQueries({ queryKey: ["seller-pattern-preview", draftId] });
      await queryClient.invalidateQueries({ queryKey: ["seller-patterns"] });
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "저장 중 오류가 발생했습니다."),
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !preview) return <Centered>도안을 찾을 수 없습니다.</Centered>;

  const editable = preview.productStatus === "DRAFT" || preview.productStatus === "REJECTED";

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <Link href="/seller/patterns" className="text-xs text-neutral-500 hover:underline">← 내 도안</Link>
      <div className="mt-2 flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">도안 수정</h1>
        <StatusBadge status={preview.productStatus} />
      </div>

      {preview.productStatus === "REJECTED" && preview.rejectionReason && (
        <p className="mt-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">
          반려 사유: {preview.rejectionReason}
        </p>
      )}

      {saved && (
        <div className="mt-4 rounded-md border border-neutral-200 p-3 text-sm dark:border-neutral-800">
          <p className="font-medium">임시저장되었습니다.</p>
          {saved.missingFields.length > 0 ? (
            <p className="mt-1 text-neutral-500">
              제출 전 보완: {saved.missingFields.map((f) => MISSING_LABEL[f] ?? f).join(", ")}
            </p>
          ) : (
            <p className="mt-1 text-neutral-500">필수 항목이 모두 채워졌습니다. 미리보기 후 제출하세요.</p>
          )}
          <div className="mt-2 flex gap-3">
            <Link href={`/seller/patterns/${draftId}/preview`} className="text-xs underline">미리보기 · 제출 →</Link>
          </div>
        </div>
      )}

      {error && <p role="alert" className="mt-4 text-sm text-red-600">{error}</p>}

      {editable ? (
        <PatternForm
          initial={preview}
          submitting={save.isPending}
          submitLabel="임시저장"
          onSubmit={(b) => { setError(null); setSaved(null); save.mutate(b); }}
        />
      ) : (
        <p className="mt-6 text-sm text-neutral-500">
          심사 중이거나 승인된 도안은 이 화면에서 수정할 수 없습니다.{" "}
          <Link href={`/seller/patterns/${draftId}/preview`} className="underline">미리보기</Link>
        </p>
      )}

      <div className="mt-6">
        <button type="button" onClick={() => router.push(`/seller/patterns/${draftId}/preview`)}
          className="text-sm text-neutral-500 hover:underline">미리보기로 이동 →</button>
      </div>
    </main>
  );
}

function StatusBadge({ status }: { status: string }) {
  const label: Record<string, string> = { DRAFT: "임시저장", PENDING: "심사 중", APPROVED: "판매 중", REJECTED: "반려", SUSPENDED: "정지" };
  const color: Record<string, string> = {
    DRAFT: "bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300",
    PENDING: "bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300",
    APPROVED: "bg-green-100 text-green-700 dark:bg-green-950/40 dark:text-green-300",
    REJECTED: "bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-300",
    SUSPENDED: "bg-neutral-200 text-neutral-600",
  };
  return <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${color[status] ?? ""}`}>{label[status] ?? status}</span>;
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}
