"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { sellerPatternApi, type SavePatternDraftBody } from "@/features/seller/api";
import { PatternForm } from "@/features/seller/PatternForm";

export default function NewPatternPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: (body: SavePatternDraftBody) => sellerPatternApi.createDraft(body),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: ["seller-patterns"] });
      router.push(`/seller/patterns/${res.draftId}/edit`);
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "저장 중 오류가 발생했습니다."),
  });

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <Link href="/seller/patterns" className="text-xs text-neutral-500 hover:underline">← 내 도안</Link>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">새 도안 등록</h1>
      <p className="mt-1 text-sm text-neutral-500">임시저장 후 미리보기하고 제출하면 관리자 심사를 거칩니다.</p>

      {error && <p role="alert" className="mt-4 text-sm text-red-600">{error}</p>}

      <PatternForm submitting={save.isPending} submitLabel="임시저장" onSubmit={(b) => { setError(null); save.mutate(b); }} />
    </main>
  );
}
