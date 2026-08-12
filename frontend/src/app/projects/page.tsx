"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { projectApi, STATUS_LABEL, type ProjectListItem } from "@/features/project/api";
import { accentOf } from "@/features/ui/accent";

// 상태별 pill 색 — 진행 단계를 색으로 읽히게(완성=초록, 뜨는 중=노랑…)
const STATUS_TONE: Record<string, string> = {
  PLANNED: "bg-neutral-200 text-neutral-700 dark:bg-neutral-700 dark:text-neutral-200",
  CO: "bg-sky-400 text-sky-950",
  WIP: "bg-amber-400 text-amber-950",
  UFO: "bg-neutral-300 text-neutral-700 dark:bg-neutral-600 dark:text-neutral-100",
  FO: "bg-emerald-400 text-emerald-950",
};

export default function MyProjectsPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["projects", "mine"], queryFn: projectApi.mine });

  return (
    <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8">
      <div className="mb-6 flex items-end justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-neutral-400">My Studio</p>
          <h1 className="mt-1 text-4xl font-black tracking-tight">내 니팅로그</h1>
        </div>
        <Link href="/projects/trash" className="text-sm text-neutral-500 hover:underline">휴지통</Link>
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-500">
          로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link>
        </p>
      ) : (
        <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {(data ?? []).map((p) => (
            <ProjectCard key={p.id} p={p} />
          ))}
          {/* 새로 만들기 카드 */}
          <li>
            <Link
              href="/projects/new"
              className="flex h-full min-h-[13rem] flex-col items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-neutral-400 text-neutral-400 transition hover:border-neutral-900 hover:text-neutral-900 dark:hover:border-neutral-100 dark:hover:text-neutral-100"
            >
              <span className="text-3xl font-black">＋</span>
              <span className="text-sm font-bold">새 니팅로그</span>
            </Link>
          </li>
        </ul>
      )}

      {!isLoading && !isError && (data ?? []).length === 0 && (
        <p className="mt-6 text-center text-sm text-neutral-400">아직 니팅로그가 없습니다. 첫 니팅로그를 만들어 보세요.</p>
      )}
    </main>
  );
}

function ProjectCard({ p }: { p: ProjectListItem }) {
  const accent = accentOf(p.id);
  return (
    <li className="group flex flex-col overflow-hidden rounded-2xl border-2 border-neutral-900 bg-white transition hover:-translate-y-1 hover:shadow-[4px_4px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:bg-neutral-950 dark:hover:shadow-[4px_4px_0_0_rgba(255,255,255,0.9)]">
      <Link href={`/projects/${p.id}`} className="flex flex-1 flex-col">
        {/* 커버 — 이미지가 없으므로 accent 그라데이션 + 이니셜 */}
        <div className={`relative flex aspect-[4/3] items-center justify-center overflow-hidden border-b-2 border-neutral-900 bg-gradient-to-br text-4xl font-black text-neutral-900/20 dark:border-neutral-100 dark:text-neutral-100/20 ${accent.wash}`}>
          <span>{p.displayTitle.slice(0, 1)}</span>
          <span className={`absolute left-2 top-2 rounded-full border border-neutral-900 px-2 py-0.5 text-[11px] font-bold dark:border-neutral-100 ${STATUS_TONE[p.status] ?? STATUS_TONE.PLANNED}`}>
            {STATUS_LABEL[p.status] ?? p.status}
          </span>
          {p.visibility !== "PUBLIC" && (
            <span className="absolute right-2 top-2 rounded-full border border-neutral-900 bg-white px-2 py-0.5 text-[11px] font-semibold dark:border-neutral-100 dark:bg-neutral-950">
              🔒
            </span>
          )}
        </div>

        <div className="flex flex-1 flex-col gap-1 p-3">
          <p className="line-clamp-1 text-sm font-bold">✓ {p.displayTitle}</p>
          <p className="line-clamp-1 text-xs text-neutral-500">
            {p.patternTitle ?? "도안"} · {p.patternType === "EXTERNAL" ? "외부" : "코잇다"}
          </p>
          <p className="mt-auto pt-1 text-xs text-neutral-400">{new Date(p.createdAt).toLocaleDateString("ko-KR")}</p>
        </div>
      </Link>
    </li>
  );
}
