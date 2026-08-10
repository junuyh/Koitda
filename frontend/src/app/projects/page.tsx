"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { projectApi, STATUS_LABEL } from "@/features/project/api";

export default function MyProjectsPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["projects", "mine"], queryFn: projectApi.mine });

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">내 니팅로그</h1>
        <Link
          href="/projects/new"
          className="rounded-md bg-neutral-900 px-4 py-2 text-sm font-medium text-white dark:bg-neutral-100 dark:text-neutral-900"
        >
          니팅로그 만들기
        </Link>
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-500">
          로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link>
        </p>
      ) : !data || data.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-400">아직 니팅로그가 없습니다. 첫 니팅로그를 만들어 보세요.</p>
      ) : (
        <ul className="divide-y divide-neutral-200 dark:divide-neutral-800">
          {data.map((p) => (
            <li key={p.id}>
              <Link href={`/projects/${p.id}`} className="flex items-center justify-between gap-3 py-4 hover:opacity-80">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{p.displayTitle}</p>
                  <p className="truncate text-xs text-neutral-500">
                    {p.patternTitle ?? "도안"} · {p.patternType === "EXTERNAL" ? "외부 도안" : "코잇다 도안"}
                    {p.visibility === "PUBLIC" ? " · 공개" : " · 비공개"}
                  </p>
                </div>
                <span className="shrink-0 rounded-full bg-neutral-100 px-2.5 py-1 text-xs font-medium text-neutral-700 dark:bg-neutral-800 dark:text-neutral-200">
                  {STATUS_LABEL[p.status] ?? p.status}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
