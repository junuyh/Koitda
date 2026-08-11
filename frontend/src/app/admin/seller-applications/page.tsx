"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { adminApi, type AdminApplicationItem } from "@/features/admin/api";
import { ApplicationStatusBadge } from "@/features/admin/StatusBadge";

const FILTERS: Array<{ value: string; label: string }> = [
  { value: "PENDING", label: "심사 대기" },
  { value: "APPROVED", label: "승인" },
  { value: "REJECTED", label: "반려" },
  { value: "", label: "전체" },
];

const BIZ_LABEL: Record<string, string> = { INDIVIDUAL: "개인", BUSINESS: "사업자" };

export default function AdminApplicationsPage() {
  const [status, setStatus] = useState("PENDING");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-applications", status],
    queryFn: () => adminApi.listApplications(status || undefined),
    retry: false,
  });

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <Link href="/admin" className="text-xs text-neutral-500 hover:underline">← 심사 콘솔</Link>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">판매자 심사</h1>

      <div className="mt-4 flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button key={f.value} type="button" onClick={() => setStatus(f.value)}
            className={`rounded-full px-3 py-1 text-xs ${status === f.value ? "bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-900" : "border border-neutral-300 text-neutral-600 dark:border-neutral-700 dark:text-neutral-300"}`}>
            {f.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {isLoading ? (
          <p className="py-10 text-center text-sm text-neutral-400">불러오는 중…</p>
        ) : isError ? (
          <p className="py-10 text-center text-sm text-neutral-400">관리자만 접근할 수 있습니다.</p>
        ) : !data || data.length === 0 ? (
          <p className="py-12 text-center text-sm text-neutral-500">해당 상태의 신청이 없습니다.</p>
        ) : (
          <ul className="space-y-3">{data.map((a) => <Card key={a.id} a={a} />)}</ul>
        )}
      </div>
    </main>
  );
}

function Card({ a }: { a: AdminApplicationItem }) {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");

  async function invalidate() {
    await queryClient.invalidateQueries({ queryKey: ["admin-applications"] });
  }
  const approve = useMutation({
    mutationFn: () => adminApi.approveApplication(a.id),
    onSuccess: invalidate,
    onError: (e) => setError(e instanceof ApiError ? e.message : "승인 중 오류가 발생했습니다."),
  });
  const reject = useMutation({
    mutationFn: () => adminApi.rejectApplication(a.id, reason.trim()),
    onSuccess: async () => { setRejecting(false); await invalidate(); },
    onError: (e) => setError(e instanceof ApiError ? e.message : "반려 중 오류가 발생했습니다."),
  });

  const pending = a.status === "PENDING";

  return (
    <li className="rounded-lg border border-neutral-200 p-4 dark:border-neutral-800">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold">{a.brandName}</p>
          <p className="mt-1 text-xs text-neutral-500">
            {a.businessType ? BIZ_LABEL[a.businessType] ?? a.businessType : "유형 미상"}
            {a.representativeName ? ` · 대표 ${a.representativeName}` : ""}
            {a.businessNo ? ` · 사업자 ${a.businessNo}` : ""}
          </p>
          <p className="mt-0.5 text-xs text-neutral-500">
            정산 {a.settlementBank ?? "-"} {a.settlementAccountMasked ?? ""}
          </p>
          {a.rejectionReason && <p className="mt-1 text-xs text-red-600">반려 사유: {a.rejectionReason}</p>}
        </div>
        <ApplicationStatusBadge status={a.status} />
      </div>

      {error && <p role="alert" className="mt-2 text-sm text-red-600">{error}</p>}

      {pending && (
        <div className="mt-3">
          {rejecting ? (
            <div className="space-y-2">
              <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={2}
                placeholder="반려 사유"
                className="w-full rounded-md border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100" />
              <div className="flex gap-2">
                <button type="button" disabled={reject.isPending}
                  onClick={() => { setError(null); if (!reason.trim()) { setError("반려 사유를 입력하세요."); return; } reject.mutate(); }}
                  className="rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white disabled:opacity-50">
                  {reject.isPending ? "반려 중…" : "반려 확정"}
                </button>
                <button type="button" onClick={() => { setRejecting(false); setReason(""); setError(null); }}
                  className="rounded-md border border-neutral-300 px-3 py-2 text-sm dark:border-neutral-700">취소</button>
              </div>
            </div>
          ) : (
            <div className="flex gap-2">
              <button type="button" disabled={approve.isPending} onClick={() => { setError(null); approve.mutate(); }}
                className="rounded-md bg-neutral-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
                {approve.isPending ? "승인 중…" : "승인"}
              </button>
              <button type="button" onClick={() => setRejecting(true)}
                className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-600 dark:border-red-900">반려</button>
            </div>
          )}
        </div>
      )}
    </li>
  );
}
