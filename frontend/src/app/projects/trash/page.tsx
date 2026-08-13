"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { projectApi } from "@/features/project/api";

export default function TrashPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useQuery({ queryKey: ["projects", "trash"], queryFn: projectApi.trash });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["projects", "trash"] });
    queryClient.invalidateQueries({ queryKey: ["projects", "mine"] });
  };
  const restore = useMutation({ mutationFn: (id: number) => projectApi.restore(id), onSuccess: invalidate });
  const purge = useMutation({ mutationFn: (id: number) => projectApi.permanentDelete(id), onSuccess: invalidate });

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <Link href="/projects" className="text-sm font-bold text-neutral-500 hover:underline">← 니팅로그</Link>
      <h1 className="mt-1 text-3xl font-black tracking-tight">휴지통</h1>
      <p className="mb-4 mt-1 text-xs text-neutral-500">삭제한 니팅로그는 90일 뒤 완전 삭제됩니다.</p>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-500">로그인이 필요합니다.</p>
      ) : !data || data.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-400">휴지통이 비어 있습니다.</p>
      ) : (
        <ul className="divide-y divide-neutral-200 dark:divide-neutral-800">
          {data.map((t) => (
            <li key={t.id} className="flex items-center justify-between gap-3 py-4">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{t.displayTitle}</p>
                <p className="text-xs text-neutral-500">완전 삭제까지 {t.remainingDays}일</p>
              </div>
              <div className="flex shrink-0 gap-2">
                <button
                  type="button"
                  onClick={() => restore.mutate(t.id)}
                  disabled={restore.isPending}
                  className="rounded-md border border-neutral-300 px-3 py-1.5 text-xs font-medium disabled:opacity-50 dark:border-neutral-700"
                >
                  복구
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (window.confirm("완전히 삭제하면 되돌릴 수 없습니다. 계속할까요?")) purge.mutate(t.id);
                  }}
                  disabled={purge.isPending}
                  className="rounded-md border border-red-300 px-3 py-1.5 text-xs font-medium text-red-600 disabled:opacity-50 dark:border-red-800"
                >
                  완전 삭제
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
