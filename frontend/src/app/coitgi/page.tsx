"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useMemo, useState } from "react";
import { projectApi, STATUS_LABEL, type ProjectListItem } from "@/features/project/api";
import { accentOf } from "@/features/ui/accent";

const STATUS_TONE: Record<string, string> = {
  PLANNED: "bg-neutral-200 text-neutral-700 dark:bg-neutral-700 dark:text-neutral-200",
  CO: "bg-sky-400 text-sky-950",
  WIP: "bg-amber-400 text-amber-950",
  UFO: "bg-neutral-300 text-neutral-700 dark:bg-neutral-600 dark:text-neutral-100",
  FO: "bg-emerald-400 text-emerald-950",
};

function statusPill(status: string | null): string {
  return `shrink-0 rounded-full border border-neutral-900 px-2 py-0.5 text-[11px] font-bold dark:border-neutral-100 ${
    status ? STATUS_TONE[status] ?? STATUS_TONE.PLANNED : STATUS_TONE.PLANNED
  }`;
}

type PatternGroupData = {
  key: string;
  title: string;
  type: string;
  seed: number;
  projects: ProjectListItem[];
};

// 니팅로그를 연결 도안 기준으로 묶는다(도안 타래 = 별도 테이블 아님, 그룹 결과).
function groupByPattern(items: ProjectListItem[]): PatternGroupData[] {
  const map = new Map<string, PatternGroupData>();
  for (const p of items) {
    const key =
      p.sellingPatternId != null ? `c:${p.sellingPatternId}`
      : p.externalPatternId != null ? `e:${p.externalPatternId}`
      : `t:${p.patternTitle ?? "?"}`;
    if (!map.has(key)) {
      map.set(key, {
        key,
        title: p.patternTitle ?? "도안 미연결",
        type: p.patternType,
        seed: p.sellingPatternId ?? p.externalPatternId ?? 0,
        projects: [],
      });
    }
    map.get(key)!.projects.push(p);
  }
  return [...map.values()];
}

export default function CoitgiPage() {
  const { data: mine, isLoading, isError } = useQuery({ queryKey: ["projects", "mine"], queryFn: projectApi.mine });
  const groups = useMemo(() => groupByPattern(mine ?? []), [mine]);

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <div className="mb-6">
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-neutral-400">Coitgi</p>
        <h1 className="mt-1 text-4xl font-black tracking-tight">코잇기</h1>
        <p className="mt-2 text-sm text-neutral-500">내 니팅로그를 연결한 도안(도안 타래) 기준으로 묶어 봅니다.</p>
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-500">로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link></p>
      ) : groups.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-400">아직 니팅로그가 없습니다. <Link href="/patterns" className="underline">도안을 골라</Link> 첫 기록을 시작해 보세요.</p>
      ) : (
        <div className="space-y-4">
          {groups.map((g) => <PatternGroup key={g.key} group={g} />)}
        </div>
      )}
    </main>
  );
}

function PatternGroup({ group }: { group: PatternGroupData }) {
  const accent = accentOf(group.seed);
  return (
    <section className="overflow-hidden rounded-3xl border-2 border-neutral-900 dark:border-neutral-100">
      {/* 도안 타래 헤더 */}
      <div className={`flex items-center justify-between gap-3 border-b-2 border-neutral-900 px-5 py-3 dark:border-neutral-100 ${accent.wash}`}>
        <div className="flex min-w-0 items-center gap-2">
          <span aria-hidden>🧶</span>
          <h2 className="line-clamp-1 text-lg font-black tracking-tight">{group.title}</h2>
          <span className="shrink-0 rounded-full border border-neutral-900 px-2 py-0.5 text-[11px] font-bold dark:border-neutral-100">
            {group.type === "EXTERNAL" ? "외부" : "코잇다"}
          </span>
        </div>
        <span className="shrink-0 text-sm font-bold text-neutral-700 dark:text-neutral-200">니팅로그 {group.projects.length}</span>
      </div>

      {/* 니팅로그 목록 */}
      <ul className="divide-y divide-neutral-200 bg-white dark:divide-neutral-800 dark:bg-neutral-950">
        {group.projects.map((p) => <ProjectRow key={p.id} project={p} />)}
      </ul>
    </section>
  );
}

function ProjectRow({ project }: { project: ProjectListItem }) {
  const [open, setOpen] = useState(false);
  return (
    <li className="px-5 py-3">
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <button type="button" onClick={() => setOpen((v) => !v)} aria-expanded={open}
            aria-label={open ? "오늘의 로그 접기" : "오늘의 로그 펼치기"}
            className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md border border-neutral-900 text-xs font-bold dark:border-neutral-100">
            {open ? "▾" : "▸"}
          </button>
          <span className={statusPill(project.status)}>{STATUS_LABEL[project.status] ?? project.status}</span>
          <Link href={`/projects/${project.id}`} className="line-clamp-1 text-sm font-bold hover:underline">
            {project.displayTitle}
          </Link>
        </div>
        {project.visibility !== "PUBLIC" && <span className="shrink-0 text-xs text-neutral-400">비공개</span>}
      </div>

      {open && <LogSublist projectId={project.id} />}
    </li>
  );
}

function LogSublist({ projectId }: { projectId: number }) {
  const { data: logs, isLoading } = useQuery({
    queryKey: ["project", projectId, "logs"],
    queryFn: () => projectApi.logs(projectId),
  });

  if (isLoading) return <p className="mt-2 pl-8 text-xs text-neutral-400">불러오는 중…</p>;
  if (!logs || logs.length === 0) return <p className="mt-2 pl-8 text-xs text-neutral-400">오늘의 로그가 없습니다.</p>;

  return (
    <ul className="mt-2 space-y-1.5 border-l-2 border-dashed border-neutral-300 pl-4 dark:border-neutral-700">
      {logs.map((l) => (
        <li key={l.id}>
          <Link href={`/projects/${projectId}`} className="flex items-center gap-2 text-sm hover:underline">
            <span className={statusPill(l.knittingStatus)}>
              {l.knittingStatus ? STATUS_LABEL[l.knittingStatus] ?? l.knittingStatus : "로그"}
            </span>
            <span className="line-clamp-1 text-neutral-700 dark:text-neutral-300">{l.comment || l.displayTitle}</span>
            <span className="ml-auto shrink-0 text-xs text-neutral-400">{l.logDate}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
