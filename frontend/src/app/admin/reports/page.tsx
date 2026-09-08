"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { adminApi, type ReportedItem } from "@/features/admin/api";

const REASON_LABEL: Record<string, string> = {
  INAPPROPRIATE: "부적절한 내용",
  SPAM: "스팸/광고",
  COPYRIGHT: "저작권 침해",
  ETC: "기타",
};

const FILTERS = [
  { value: "PENDING", label: "미처리" },
  { value: "HIDDEN", label: "숨김" },
  { value: "DISMISSED", label: "무시" },
  { value: "RESTORED", label: "복원" },
];

export default function AdminReportsPage() {
  const [resolution, setResolution] = useState("PENDING");
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-reports", resolution],
    queryFn: () => adminApi.listReports(resolution),
  });
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin-reports"] });

  const act = useMutation({
    mutationFn: ({ id, action }: { id: number; action: "hide" | "dismiss" | "restore" }) =>
      action === "hide" ? adminApi.hideReport(id)
        : action === "dismiss" ? adminApi.dismissReport(id)
        : adminApi.restoreReport(id),
    onSuccess: invalidate,
    onError: (e) => window.alert(e instanceof ApiError ? e.message : "처리 중 오류가 발생했습니다."),
  });

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <Link href="/admin" className="text-xs text-neutral-500 hover:underline">← 심사 콘솔</Link>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">신고 관리</h1>
      <p className="mt-1 text-sm text-neutral-500">신고된 콘텐츠를 검토해 숨김·무시·복원합니다. (현재 리뷰 대상)</p>

      <div className="mt-4 flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button key={f.value} type="button" onClick={() => setResolution(f.value)}
            className={`rounded-full px-3 py-1 text-xs font-bold ${resolution === f.value ? "bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-900" : "border border-neutral-300 text-neutral-600 dark:border-neutral-700 dark:text-neutral-300"}`}>
            {f.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {isLoading ? (
          <p className="py-10 text-center text-sm text-neutral-400">불러오는 중…</p>
        ) : isError ? (
          <p className="py-10 text-center text-sm text-neutral-400">불러오지 못했습니다. 관리자만 접근할 수 있습니다.</p>
        ) : !data || data.length === 0 ? (
          <p className="py-12 text-center text-sm text-neutral-500">
            {resolution === "PENDING" ? "미처리 신고가 없습니다. 👍" : "해당 상태의 신고가 없습니다."}
          </p>
        ) : (
          <ul className="space-y-3">
            {data.map((r) => (
              <ReportRow key={r.moderationId} r={r} busy={act.isPending}
                onAction={(action) => act.mutate({ id: r.moderationId, action })} />
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}

function ReportRow({ r, onAction, busy }: {
  r: ReportedItem; onAction: (a: "hide" | "dismiss" | "restore") => void; busy: boolean;
}) {
  return (
    <li className="rounded-lg border-2 border-neutral-900 p-4 dark:border-neutral-100">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs font-bold text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300">
            {r.targetType === "REVIEW" ? "리뷰" : r.targetType}
          </span>
          <span className="text-xs font-bold text-red-500">신고 {r.reportCount}건</span>
          {r.flagged && <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-bold text-red-700 dark:bg-red-950/40 dark:text-red-300">주의</span>}
          {r.hidden && <span className="rounded-full bg-neutral-200 px-2 py-0.5 text-xs font-bold text-neutral-600 dark:bg-neutral-700 dark:text-neutral-300">숨김됨</span>}
        </div>
        <span className="text-xs text-neutral-400">{r.lastReportedAt ? new Date(r.lastReportedAt).toLocaleDateString("ko-KR") : ""}</span>
      </div>

      {r.title && <p className="mt-2 text-sm font-bold">{r.title}</p>}
      {r.preview && <p className="mt-1 whitespace-pre-line text-sm text-neutral-600 dark:text-neutral-300">{r.preview}</p>}
      <p className="mt-1 text-xs text-neutral-400">작성자 {r.authorNickname ?? "-"}</p>

      {r.reasons.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {r.reasons.map((code) => (
            <span key={code} className="rounded-full border border-neutral-300 px-2 py-0.5 text-xs text-neutral-500 dark:border-neutral-700">
              {REASON_LABEL[code] ?? code}
            </span>
          ))}
        </div>
      )}

      <div className="mt-3 flex gap-2">
        {r.resolution === "HIDDEN" ? (
          <button type="button" disabled={busy} onClick={() => onAction("restore")}
            className="rounded-full border-2 border-neutral-900 px-3 py-1.5 text-xs font-bold disabled:opacity-50 dark:border-neutral-100">숨김 해제</button>
        ) : (
          <button type="button" disabled={busy} onClick={() => { if (window.confirm("이 콘텐츠를 숨길까요? 공개 목록에서 즉시 제외됩니다.")) onAction("hide"); }}
            className="rounded-full border-2 border-red-500 bg-red-500 px-3 py-1.5 text-xs font-bold text-white disabled:opacity-50">숨김</button>
        )}
        {r.resolution === "PENDING" && (
          <button type="button" disabled={busy} onClick={() => onAction("dismiss")}
            className="rounded-full border-2 border-neutral-900 px-3 py-1.5 text-xs font-bold disabled:opacity-50 dark:border-neutral-100">무시(유지)</button>
        )}
      </div>
    </li>
  );
}
