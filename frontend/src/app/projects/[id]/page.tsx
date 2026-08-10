"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { projectApi, STATUS_LABEL } from "@/features/project/api";

const STATUS_ORDER = ["PLANNED", "CO", "WIP", "UFO", "FO"];

export default function ProjectDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const queryClient = useQueryClient();

  const { data: p, isLoading, isError } = useQuery({ queryKey: ["project", id], queryFn: () => projectApi.get(id) });
  const { data: logs } = useQuery({ queryKey: ["project", id, "logs"], queryFn: () => projectApi.logs(id) });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["project", id] });
    queryClient.invalidateQueries({ queryKey: ["project", id, "logs"] });
  };

  const changeVisibility = useMutation({
    mutationFn: async (target: "PRIVATE" | "PUBLIC") => {
      if (target === "PRIVATE") {
        const impact = await projectApi.visibilityImpact(id, "PRIVATE");
        if (
          impact.affectedPublicLogCount > 0 &&
          !window.confirm(`공개 중인 로그 ${impact.affectedPublicLogCount}개가 함께 비공개로 전환됩니다. 계속할까요?`)
        ) {
          return;
        }
        await projectApi.changeVisibility(id, "PRIVATE", true);
      } else {
        await projectApi.changeVisibility(id, "PUBLIC", false);
      }
    },
    onSuccess: invalidate,
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !p) return <Centered>니팅로그를 찾을 수 없습니다.</Centered>;

  const snap = p.patternSnapshot;
  const measureKeys = snap?.sizes?.[0] ? Object.keys(snap.sizes[0].measurements) : [];

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <Link href="/projects" className="text-sm text-neutral-500 hover:underline">← 내 니팅로그</Link>

      <div className="mt-3 flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{p.displayTitle}</h1>
          <p className="mt-1 text-sm text-neutral-500">
            {p.patternType === "EXTERNAL" ? p.external?.title ?? "외부 도안" : snap?.title ?? "코잇다 도안"}
            {p.patternType === "EXTERNAL" && p.external?.creatorName ? ` · ${p.external.creatorName}` : ""}
            {" · "}{p.visibility === "PUBLIC" ? "공개" : "비공개"}
          </p>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-1">
          <span className="rounded-full bg-neutral-900 px-3 py-1 text-xs font-medium text-white dark:bg-neutral-100 dark:text-neutral-900">
            {STATUS_LABEL[p.status] ?? p.status}
          </span>
          <button
            type="button"
            onClick={() => changeVisibility.mutate(p.visibility === "PUBLIC" ? "PRIVATE" : "PUBLIC")}
            disabled={changeVisibility.isPending}
            className="text-xs text-neutral-500 underline disabled:opacity-50"
          >
            {p.visibility === "PUBLIC" ? "비공개로 전환" : "공개로 전환"}
          </button>
        </div>
      </div>

      {p.note && <p className="mt-4 whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{p.note}</p>}

      {/* 원작 스냅샷 (판매 도안 연결) */}
      {snap?.gauge && (
        <Section title="원작 게이지 (연결 시점 복사)">
          <p className="text-sm">{snap.gauge.stitches}코 × {snap.gauge.rows}단{snap.gauge.needleSizeMm ? ` · 바늘 ${snap.gauge.needleSizeMm}mm` : ""}</p>
        </Section>
      )}
      {snap?.sizes && snap.sizes.length > 0 && (
        <Section title="원작 사이즈">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[360px] text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-left dark:border-neutral-800">
                  <th className="py-1.5 pr-4 font-medium">사이즈</th>
                  <th className="py-1.5 pr-4 font-medium">시작 콧수</th>
                  {measureKeys.map((k) => <th key={k} className="py-1.5 pr-4 font-medium">{k}</th>)}
                </tr>
              </thead>
              <tbody>
                {snap.sizes.map((s) => (
                  <tr key={s.label} className="border-b border-neutral-100 dark:border-neutral-900">
                    <td className="py-1.5 pr-4">{s.label}</td>
                    <td className="py-1.5 pr-4">{s.castOnStitches}코</td>
                    {measureKeys.map((k) => <td key={k} className="py-1.5 pr-4">{s.measurements[k]}cm</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Section>
      )}

      {/* 재료 */}
      {p.yarns.length > 0 && (
        <Section title="실">
          <ul className="space-y-1 text-sm">
            {p.yarns.map((y, i) => (
              <li key={i}>{[y.brand, y.yarnName, y.color, y.amount].filter(Boolean).join(" · ") || "-"}</li>
            ))}
          </ul>
        </Section>
      )}
      {p.gauges.length > 0 && (
        <Section title="내 게이지">
          <ul className="space-y-1 text-sm">
            {p.gauges.map((g, i) => (
              <li key={i}>{g.stitches}코 × {g.rows}단{g.needleSizeMm ? ` · ${g.needleSizeMm}mm` : ""}</li>
            ))}
          </ul>
        </Section>
      )}

      {/* 오늘의 로그 */}
      <Section title="오늘의 로그">
        <QuickLogForm projectId={id} currentStatus={p.status} projectVisibility={p.visibility} onDone={invalidate} />
        <ul className="mt-4 divide-y divide-neutral-100 dark:divide-neutral-900">
          {(logs ?? []).map((l) => (
            <li key={l.id} className="py-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">{l.displayTitle}</span>
                <span className="text-xs text-neutral-500">
                  {l.logDate} · {l.knittingStatus ? STATUS_LABEL[l.knittingStatus] : ""}
                </span>
              </div>
              {l.comment && <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">{l.comment}</p>}
            </li>
          ))}
          {(logs ?? []).length === 0 && <li className="py-3 text-sm text-neutral-400">아직 로그가 없습니다.</li>}
        </ul>
      </Section>
    </main>
  );
}

function QuickLogForm({
  projectId,
  currentStatus,
  projectVisibility,
  onDone,
}: {
  projectId: number;
  currentStatus: string;
  projectVisibility: string;
  onDone: () => void;
}) {
  const [status, setStatus] = useState("CO");
  const [comment, setComment] = useState("");
  const [makePublic, setMakePublic] = useState(false);

  // 첫 로그 제안 CO, 이후는 현재 상태(POST-003)
  useEffect(() => {
    setStatus(currentStatus === "PLANNED" ? "CO" : currentStatus);
  }, [currentStatus]);

  const submit = useMutation({
    mutationFn: () => {
      const body: Parameters<typeof projectApi.createLog>[1] = {
        knittingStatus: status,
        comment: comment.trim() || undefined,
      };
      if (makePublic) {
        body.visibility = "PUBLIC";
        // 비공개 니팅로그를 공개 로그로 올리면 니팅로그도 함께 공개된다 → 확인(POST-011)
        if (projectVisibility === "PRIVATE") {
          body.publishProjectConfirmed = window.confirm(
            "이 니팅로그는 비공개입니다. 로그를 공개하면 니팅로그도 함께 공개됩니다. 함께 공개할까요?",
          );
        }
      }
      return projectApi.createLog(projectId, body);
    },
    onSuccess: () => {
      setComment("");
      setMakePublic(false);
      onDone();
    },
  });

  return (
    <form
      className="rounded-md border border-neutral-200 p-3 dark:border-neutral-800"
      onSubmit={(e) => { e.preventDefault(); submit.mutate(); }}
    >
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="상태"
          className="rounded-md border border-neutral-300 bg-transparent px-2 py-2 text-sm dark:border-neutral-700">
          {STATUS_ORDER.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
        </select>
        <input value={comment} onChange={(e) => setComment(e.target.value)} placeholder="오늘의 기록 한 줄"
          className="flex-1 rounded-md border border-neutral-300 bg-transparent px-3 py-2 text-sm dark:border-neutral-700" />
        <button type="submit" disabled={submit.isPending}
          className="rounded-md bg-neutral-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
          기록
        </button>
      </div>
      <label className="mt-2 flex items-center gap-2 text-xs text-neutral-500">
        <input type="checkbox" checked={makePublic} onChange={(e) => setMakePublic(e.target.checked)} className="h-3.5 w-3.5" />
        이 로그 공개
      </label>
    </form>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-8 border-t border-neutral-200 pt-6 dark:border-neutral-800">
      <h2 className="mb-2 text-sm font-semibold">{title}</h2>
      {children}
    </section>
  );
}
