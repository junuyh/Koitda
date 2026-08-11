"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { projectApi, STATUS_LABEL } from "@/features/project/api";
import { gaugeApi } from "@/features/gauge/api";

const STATUS_ORDER = ["PLANNED", "CO", "WIP", "UFO", "FO"];

// 레트로(2000년대) 박스 — 두꺼운 라운드 보더
const box = "rounded-[22px] border-2 border-neutral-900 bg-white dark:border-neutral-100 dark:bg-neutral-950";

export default function ProjectDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: p, isLoading, isError } = useQuery({ queryKey: ["project", id], queryFn: () => projectApi.get(id) });
  const { data: logs } = useQuery({ queryKey: ["project", id, "logs"], queryFn: () => projectApi.logs(id) });
  const { data: appliedGauge } = useQuery({
    queryKey: ["gauge-applied", id],
    queryFn: () => gaugeApi.applied(id),
    retry: false,
  });

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

  const trash = useMutation({
    mutationFn: () => projectApi.moveToTrash(id),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: ["projects", "mine"] });
      window.alert(`휴지통으로 옮겼습니다. 연결 로그 ${res.connectedLogCount}개도 함께 이동했으며, 90일 뒤 완전 삭제됩니다.`);
      router.push("/projects");
    },
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !p) return <Centered>니팅로그를 찾을 수 없습니다.</Centered>;

  const snap = p.patternSnapshot;
  const measureKeys = snap?.sizes?.[0] ? Object.keys(snap.sizes[0].measurements) : [];

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <Link href="/projects" className="text-sm text-neutral-500 hover:underline">← 내 니팅로그</Link>

      <div className="mt-4 space-y-4">
        {/* 헤더 박스 */}
        <div className={`${box} p-5`}>
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="text-xs font-bold uppercase tracking-wider text-neutral-400">Knitting Log</p>
              <h1 className="mt-1 text-2xl font-bold tracking-tight">{p.displayTitle}</h1>
            </div>
            <span className="shrink-0 rounded-full border-2 border-neutral-900 px-3 py-1 text-xs font-bold dark:border-neutral-100">
              {STATUS_LABEL[p.status] ?? p.status}
            </span>
          </div>

          <dl className="mt-4 space-y-1.5 text-sm">
            <Row label="도안">
              {p.patternType === "EXTERNAL" ? p.external?.title ?? "외부 도안" : snap?.title ?? "코잇다 도안"}
              {p.patternType === "EXTERNAL" && p.external?.creatorName ? ` · ${p.external.creatorName}` : ""}
            </Row>
            <Row label="공개">{p.visibility === "PUBLIC" ? "공개" : "비공개"}</Row>
            {p.note && <Row label="코멘트">{p.note}</Row>}
          </dl>

          <div className="mt-4 flex flex-wrap gap-2 border-t-2 border-dashed border-neutral-200 pt-3 dark:border-neutral-800">
            <button type="button" onClick={() => changeVisibility.mutate(p.visibility === "PUBLIC" ? "PRIVATE" : "PUBLIC")}
              disabled={changeVisibility.isPending}
              className="rounded-full border-2 border-neutral-900 px-4 py-1.5 text-xs font-medium disabled:opacity-50 dark:border-neutral-100">
              {p.visibility === "PUBLIC" ? "비공개로 전환" : "공개로 전환"}
            </button>
            <button type="button"
              onClick={() => { if (window.confirm("이 니팅로그를 휴지통으로 옮길까요? 연결된 오늘의 로그도 함께 이동합니다.")) trash.mutate(); }}
              disabled={trash.isPending}
              className="rounded-full border-2 border-red-500 px-4 py-1.5 text-xs font-medium text-red-500 disabled:opacity-50">
              삭제
            </button>
          </div>
        </div>

        {/* 원작 스냅샷 */}
        {(snap?.gauge || (snap?.sizes && snap.sizes.length > 0)) && (
          <Section title="원작 정보 (연결 시점 복사)">
            {snap?.gauge && (
              <Row label="게이지">{snap.gauge.stitches}코 × {snap.gauge.rows}단{snap.gauge.needleSizeMm ? ` · 바늘 ${snap.gauge.needleSizeMm}mm` : ""}</Row>
            )}
            {snap?.sizes && snap.sizes.length > 0 && (
              <div className="mt-3 overflow-x-auto">
                <table className="w-full min-w-[360px] text-sm">
                  <thead>
                    <tr className="border-b-2 border-neutral-900 text-left dark:border-neutral-100">
                      <th className="py-1.5 pr-4 font-bold">사이즈</th>
                      <th className="py-1.5 pr-4 font-bold">시작 콧수</th>
                      {measureKeys.map((k) => <th key={k} className="py-1.5 pr-4 font-bold">{k}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {snap.sizes.map((s) => (
                      <tr key={s.label} className="border-b border-neutral-200 dark:border-neutral-800">
                        <td className="py-1.5 pr-4">{s.label}</td>
                        <td className="py-1.5 pr-4">{s.castOnStitches}코</td>
                        {measureKeys.map((k) => <td key={k} className="py-1.5 pr-4">{s.measurements[k]}cm</td>)}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Section>
        )}

        {/* 재료 */}
        {p.yarns.length > 0 && (
          <Section title="실">
            <ul className="space-y-1 text-sm">
              {p.yarns.map((y, i) => <li key={i}>{[y.brand, y.yarnName, y.color, y.amount].filter(Boolean).join(" · ") || "-"}</li>)}
            </ul>
          </Section>
        )}
        {p.gauges.length > 0 && (
          <Section title="내 게이지">
            <ul className="space-y-1 text-sm">
              {p.gauges.map((g, i) => <li key={i}>{g.stitches}코 × {g.rows}단{g.needleSizeMm ? ` · ${g.needleSizeMm}mm` : ""}</li>)}
            </ul>
          </Section>
        )}

        {/* 게이지 계산 */}
        {p.patternType !== "EXTERNAL" && (
          <Section title="게이지 계산">
            {appliedGauge ? (
              <div className="flex items-center justify-between gap-3">
                <div className="text-sm">
                  <p>조정 시작 콧수 <span className="font-bold">{appliedGauge.adjustedCastOnStitches}코</span></p>
                  {appliedGauge.adjustmentSummary && <p className="mt-0.5 text-xs text-amber-600 dark:text-amber-400">조정: {appliedGauge.adjustmentSummary}</p>}
                </div>
                <Link href={`/projects/${id}/gauge`} className="shrink-0 text-xs font-medium underline">다시 계산</Link>
              </div>
            ) : (
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm text-neutral-500">내 게이지 기준으로 조정 콧수·부위별 필요 콧수를 계산해 보세요.</p>
                <Link href={`/projects/${id}/gauge`} className="shrink-0 rounded-full border-2 border-neutral-900 px-4 py-1.5 text-sm font-medium dark:border-neutral-100">게이지 계산</Link>
              </div>
            )}
          </Section>
        )}

        {/* 오늘의 로그 */}
        <Section title="오늘의 로그">
          <QuickLogForm projectId={id} currentStatus={p.status} projectVisibility={p.visibility} onDone={invalidate} />
          <ul className="mt-4 divide-y-2 divide-dashed divide-neutral-200 dark:divide-neutral-800">
            {(logs ?? []).map((l) => (
              <li key={l.id} className="py-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-bold">{l.displayTitle}</span>
                  <span className="text-xs text-neutral-500">{l.logDate} · {l.knittingStatus ? STATUS_LABEL[l.knittingStatus] : ""}</span>
                </div>
                {l.comment && <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">{l.comment}</p>}
              </li>
            ))}
            {(logs ?? []).length === 0 && <li className="py-3 text-sm text-neutral-400">아직 로그가 없습니다.</li>}
          </ul>
        </Section>
      </div>
    </main>
  );
}

function QuickLogForm({
  projectId, currentStatus, projectVisibility, onDone,
}: { projectId: number; currentStatus: string; projectVisibility: string; onDone: () => void }) {
  const [status, setStatus] = useState("CO");
  const [comment, setComment] = useState("");
  const [makePublic, setMakePublic] = useState(false);

  useEffect(() => { setStatus(currentStatus === "PLANNED" ? "CO" : currentStatus); }, [currentStatus]);

  const submit = useMutation({
    mutationFn: () => {
      const body: Parameters<typeof projectApi.createLog>[1] = { knittingStatus: status, comment: comment.trim() || undefined };
      if (makePublic) {
        body.visibility = "PUBLIC";
        if (projectVisibility === "PRIVATE") {
          body.publishProjectConfirmed = window.confirm("이 니팅로그는 비공개입니다. 로그를 공개하면 니팅로그도 함께 공개됩니다. 함께 공개할까요?");
        }
      }
      return projectApi.createLog(projectId, body);
    },
    onSuccess: () => { setComment(""); setMakePublic(false); onDone(); },
  });

  return (
    <form className="rounded-2xl border-2 border-dashed border-neutral-400 p-3 dark:border-neutral-600"
      onSubmit={(e) => { e.preventDefault(); submit.mutate(); }}>
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="상태"
          className="rounded-full border-2 border-neutral-900 bg-transparent px-3 py-2 text-sm dark:border-neutral-100">
          {STATUS_ORDER.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
        </select>
        <input value={comment} onChange={(e) => setComment(e.target.value)} placeholder="오늘의 기록 한 줄"
          className="flex-1 rounded-full border-2 border-neutral-900 bg-transparent px-4 py-2 text-sm dark:border-neutral-100" />
        <button type="submit" disabled={submit.isPending}
          className="rounded-full bg-neutral-900 px-5 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">기록</button>
      </div>
      <label className="mt-2 flex items-center gap-2 text-xs text-neutral-500">
        <input type="checkbox" checked={makePublic} onChange={(e) => setMakePublic(e.target.checked)} className="h-3.5 w-3.5" />
        이 로그 공개
      </label>
    </form>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-3">
      <dt className="w-16 shrink-0 text-neutral-400">{label}</dt>
      <dd className="min-w-0 text-neutral-800 dark:text-neutral-200">{children}</dd>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className={`${box} p-5`}>
      <h2 className="mb-3 text-xs font-bold uppercase tracking-wider text-neutral-400">{title}</h2>
      {children}
    </section>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}
