"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { adminApi, type AdminPatternListItem } from "@/features/admin/api";
import { PatternStatusBadge } from "@/features/admin/StatusBadge";

const FILTERS: Array<{ value: string; label: string }> = [
  { value: "PENDING", label: "심사 대기" },
  { value: "APPROVED", label: "판매 중" },
  { value: "REJECTED", label: "반려" },
  { value: "", label: "전체" },
];

export default function AdminPatternsPage() {
  const [status, setStatus] = useState("PENDING");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-patterns", status],
    queryFn: () => adminApi.listPatterns(status || undefined),
    retry: false,
  });

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <Link href="/admin" className="text-xs text-neutral-500 hover:underline">← 심사 콘솔</Link>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">도안 심사</h1>

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
          <p className="py-12 text-center text-sm text-neutral-500">해당 상태의 도안이 없습니다.</p>
        ) : (
          <ul className="space-y-2">{data.map((p) => <Row key={p.id} p={p} />)}</ul>
        )}
      </div>
    </main>
  );
}

function Row({ p }: { p: AdminPatternListItem }) {
  const price = p.salePrice ?? p.regularPrice;
  return (
    <li className="rounded-md border border-neutral-200 dark:border-neutral-800">
      <Link href={`/admin/patterns/${p.id}`} className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-neutral-50 dark:hover:bg-neutral-900">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{p.title || "(제목 없음)"}</p>
          <p className="mt-0.5 text-xs text-neutral-500">
            {p.sellerBrand ?? "판매자"}{price != null ? ` · ${price.toLocaleString()}원` : ""}
          </p>
        </div>
        <PatternStatusBadge status={p.productStatus} />
      </Link>
    </li>
  );
}
