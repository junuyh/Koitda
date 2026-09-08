"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { adminApi } from "@/features/admin/api";

export default function AdminHomePage() {
  const patterns = useQuery({
    queryKey: ["admin-patterns", "PENDING"],
    queryFn: () => adminApi.listPatterns("PENDING"),
    retry: false,
  });
  const applications = useQuery({
    queryKey: ["admin-applications", "PENDING"],
    queryFn: () => adminApi.listApplications("PENDING"),
    retry: false,
  });
  const reports = useQuery({
    queryKey: ["admin-reports", "PENDING"],
    queryFn: () => adminApi.listReports("PENDING"),
    retry: false,
  });

  const forbidden = patterns.isError || applications.isError;

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <h1 className="text-2xl font-semibold tracking-tight">관리자 심사 콘솔</h1>
      <p className="mt-1 text-sm text-neutral-500">판매자 신청과 도안 등록을 승인·반려합니다.</p>

      {forbidden ? (
        <p className="mt-8 rounded-md border border-neutral-200 px-4 py-6 text-center text-sm text-neutral-500 dark:border-neutral-800">
          관리자만 접근할 수 있습니다.
        </p>
      ) : (
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          <ConsoleCard
            href="/admin/patterns"
            title="도안 심사"
            desc="제출된 도안을 검토해 판매를 승인합니다."
            count={patterns.data?.length}
            loading={patterns.isLoading}
          />
          <ConsoleCard
            href="/admin/seller-applications"
            title="판매자 심사"
            desc="사업자·정산 정보를 확인해 판매자를 승인합니다."
            count={applications.data?.length}
            loading={applications.isLoading}
          />
          <ConsoleCard
            href="/admin/reports"
            title="신고 관리"
            desc="신고된 콘텐츠를 검토해 숨김·무시합니다."
            count={reports.data?.length}
            loading={reports.isLoading}
          />
        </div>
      )}
    </main>
  );
}

function ConsoleCard({ href, title, desc, count, loading }: {
  href: string; title: string; desc: string; count?: number; loading: boolean;
}) {
  return (
    <Link href={href}
      className="block rounded-lg border border-neutral-200 p-5 transition hover:border-neutral-400 dark:border-neutral-800 dark:hover:border-neutral-600">
      <div className="flex items-center justify-between">
        <h2 className="text-base font-semibold">{title}</h2>
        {loading ? (
          <span className="text-xs text-neutral-400">…</span>
        ) : count && count > 0 ? (
          <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-700 dark:bg-amber-950/40 dark:text-amber-300">
            대기 {count}
          </span>
        ) : (
          <span className="rounded-full bg-neutral-100 px-2.5 py-1 text-xs text-neutral-500 dark:bg-neutral-800">대기 없음</span>
        )}
      </div>
      <p className="mt-2 text-sm text-neutral-500">{desc}</p>
    </Link>
  );
}
