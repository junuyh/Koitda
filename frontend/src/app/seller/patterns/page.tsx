"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { sellerPatternApi, type SellerPatternListItem } from "@/features/seller/api";

const STATUS_FILTERS: Array<{ value: string; label: string }> = [
  { value: "", label: "전체" },
  { value: "DRAFT", label: "임시저장" },
  { value: "PENDING", label: "심사 중" },
  { value: "APPROVED", label: "판매 중" },
  { value: "REJECTED", label: "반려" },
];

export default function SellerPatternsPage() {
  const [status, setStatus] = useState("");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["seller-patterns", status],
    queryFn: () => sellerPatternApi.list(status || undefined),
  });

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">내 도안</h1>
        <Link href="/seller/patterns/new"
          className="rounded-md bg-neutral-900 px-3 py-2 text-sm font-medium text-white dark:bg-neutral-100 dark:text-neutral-900">
          + 새 도안 등록
        </Link>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {STATUS_FILTERS.map((f) => (
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
          <p className="py-10 text-center text-sm text-neutral-400">목록을 불러오지 못했습니다. 판매자만 접근할 수 있습니다.</p>
        ) : !data || data.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-sm text-neutral-500">아직 등록한 도안이 없습니다.</p>
            <Link href="/seller/patterns/new" className="mt-2 inline-block text-sm underline">첫 도안 등록하기</Link>
          </div>
        ) : (
          <ul className="space-y-2">
            {data.map((p) => <Row key={p.id} p={p} />)}
          </ul>
        )}
      </div>
    </main>
  );
}

function Row({ p }: { p: SellerPatternListItem }) {
  const editable = p.productStatus === "DRAFT" || p.productStatus === "REJECTED";
  const href = editable ? `/seller/patterns/${p.id}/edit` : `/seller/patterns/${p.id}/preview`;
  const price = p.salePrice ?? p.regularPrice;
  return (
    <li className="rounded-md border border-neutral-200 dark:border-neutral-800">
      <Link href={href} className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-neutral-50 dark:hover:bg-neutral-900">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{p.title || "(제목 없음)"}</p>
          <p className="mt-0.5 text-xs text-neutral-500">
            {price != null ? `${price.toLocaleString()}원` : "가격 미정"}
            {p.productStatus === "REJECTED" && p.rejectionReason ? ` · 반려: ${p.rejectionReason}` : ""}
          </p>
        </div>
        <StatusBadge status={p.productStatus} />
      </Link>
    </li>
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
  return <span className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-medium ${color[status] ?? ""}`}>{label[status] ?? status}</span>;
}
