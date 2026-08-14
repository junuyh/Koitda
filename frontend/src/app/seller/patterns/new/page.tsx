"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api";
import { sellerPatternApi, type SavePatternDraftBody } from "@/features/seller/api";
import { PatternForm } from "@/features/seller/PatternForm";

export default function NewPatternPage() {
  const router = useRouter();
  const queryClient = useQueryClient();

  const save = useMutation({
    // intent 를 함께 실어 성공 후 이동 여부를 결정한다.
    mutationFn: (v: { body: SavePatternDraftBody; intent: "save" | "next" }) =>
      sellerPatternApi.createDraft(v.body).then((res) => ({ res, intent: v.intent })),
    onSuccess: async ({ res, intent }) => {
      await queryClient.invalidateQueries({ queryKey: ["seller-patterns"] });
      // "다음"이면 미리보기로, 그냥 저장이면 수정 화면(이어서 보완)으로.
      router.push(intent === "next"
        ? `/seller/patterns/${res.draftId}/preview`
        : `/seller/patterns/${res.draftId}/edit`);
    },
    onError: (e) => window.alert(e instanceof ApiError ? `저장 실패\n\n${e.message}` : "서버 오류로 저장하지 못했습니다.\n잠시 후 다시 시도해 주세요."),
  });

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <Link href="/seller/patterns" className="text-xs text-neutral-500 hover:underline">← 내 도안</Link>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">새 도안 등록</h1>
      <p className="mt-1 text-sm text-neutral-500">임시저장 후 미리보기하고 제출하면 관리자 심사를 거칩니다.</p>

      <PatternForm submitting={save.isPending} submitLabel="임시저장"
        onSubmit={(body, intent) => save.mutate({ body, intent })} />
    </main>
  );
}
