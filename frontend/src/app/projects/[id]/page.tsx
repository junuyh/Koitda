"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { projectApi, STATUS_LABEL, type LogItem } from "@/features/project/api";
import { gaugeApi } from "@/features/gauge/api";
import { GaugeResultView } from "@/features/gauge/GaugeResultView";
import { RichEditor } from "@/features/editor/RichEditor";
import { RichContent } from "@/features/editor/RichContent";
import { accentOf } from "@/features/ui/accent";
import type { JSONContent } from "@tiptap/react";

const STATUS_ORDER = ["PLANNED", "CO", "WIP", "UFO", "FO"];

// 레트로(2000년대) 박스 — 두꺼운 라운드 보더
const box = "rounded-[22px] border-2 border-neutral-900 bg-white dark:border-neutral-100 dark:bg-neutral-950";

// 상태별 pill 색 — 진행 단계를 색으로 읽히게(로그 리스트와 동일 규칙)
const STATUS_TONE: Record<string, string> = {
  PLANNED: "bg-neutral-200 text-neutral-700 dark:bg-neutral-700 dark:text-neutral-200",
  CO: "bg-sky-400 text-sky-950",
  WIP: "bg-amber-400 text-amber-950",
  UFO: "bg-neutral-300 text-neutral-700 dark:bg-neutral-600 dark:text-neutral-100",
  FO: "bg-emerald-400 text-emerald-950",
};

function statusPill(status: string | null): string {
  return `rounded-full border border-neutral-900 px-2 py-0.5 text-[11px] font-bold dark:border-neutral-100 ${
    status ? STATUS_TONE[status] ?? STATUS_TONE.PLANNED : STATUS_TONE.PLANNED
  }`;
}

const LOGS_PER_PAGE = 15;

const CRAFT_LABEL: Record<string, string> = { KNIT: "대바늘", CROCHET: "코바늘" };
const MEASURE_LABEL: Record<string, string> = {
  chestCm: "가슴둘레", lengthCm: "총장", sleeveLengthCm: "소매길이", shoulderCm: "어깨너비",
  widthCm: "가로", heightCm: "세로",
};

// 오늘의 로그 본문(TipTap JSON)에서 이미지 src 를 재귀로 모은다. 사진 유무 배지·썸네일용.
function extractImages(doc: unknown): string[] {
  const out: string[] = [];
  const walk = (node: unknown) => {
    if (!node || typeof node !== "object") return;
    const n = node as { type?: string; attrs?: { src?: string }; content?: unknown[] };
    if (n.type === "image" && n.attrs?.src) out.push(n.attrs.src);
    if (Array.isArray(n.content)) n.content.forEach(walk);
  };
  walk(doc);
  return out;
}

// 블록 하나의 텍스트만 이어붙인다(이미지 등 비텍스트 무시).
function blockText(block: unknown): string {
  let t = "";
  const walk = (n: unknown) => {
    if (!n || typeof n !== "object") return;
    const node = n as { type?: string; text?: string; content?: unknown[] };
    if (node.type === "text" && node.text) t += node.text;
    if (Array.isArray(node.content)) node.content.forEach(walk);
  };
  walk(block);
  return t.trim();
}

// 리스트에서 보여줄 한 줄 요약: 이미지 제외 첫 텍스트 줄, 표면 "표".
function logSnippet(doc: unknown, fallback: string | null): string {
  const content = (doc as { content?: unknown[] } | null)?.content;
  if (Array.isArray(content)) {
    for (const block of content) {
      const b = block as { type?: string } | null;
      if (!b || typeof b !== "object") continue;
      if (b.type === "image") continue;
      if (b.type === "table") return "표";
      const t = blockText(block);
      if (t) return t;
    }
  }
  return (fallback ?? "").split("\n").map((s) => s.trim()).find(Boolean) ?? "";
}

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

  // 오늘의 로그 팝업(모달)로 열린 로그. 리스트가 이미 본문을 담고 있어 추가 조회 없이 연다.
  const [openLog, setOpenLog] = useState<LogItem | null>(null);
  const [writeOpen, setWriteOpen] = useState(false); // 작성 폼 팝업
  const [logPage, setLogPage] = useState(0); // 로그 리스트 페이지(15개씩)

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

  const accent = accentOf(p.id);
  const cover = p.images?.find((im) => im.url)?.url ?? null;
  const patternLabel =
    p.patternType === "EXTERNAL" ? p.external?.title ?? "외부 도안" : snap?.title ?? "코잇다 도안";
  const designer = p.patternType === "EXTERNAL" ? p.external?.creatorName : snap?.designerName;

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      {/* 상단 행: 뒤로 + (우측 끝) 공개 전환·삭제 */}
      <div className="flex items-center justify-between gap-3">
        <Link href="/projects" className="text-sm font-bold text-neutral-500 hover:underline">← 내 니팅로그</Link>
        <div className="flex gap-2">
          <button type="button" onClick={() => changeVisibility.mutate(p.visibility === "PUBLIC" ? "PRIVATE" : "PUBLIC")}
            disabled={changeVisibility.isPending}
            className="rounded-full border-2 border-neutral-900 px-4 py-1.5 text-xs font-bold disabled:opacity-50 dark:border-neutral-100">
            {p.visibility === "PUBLIC" ? "비공개로 전환" : "공개로 전환"}
          </button>
          <button type="button"
            onClick={() => { if (window.confirm("이 니팅로그를 휴지통으로 옮길까요? 연결된 오늘의 로그도 함께 이동합니다.")) trash.mutate(); }}
            disabled={trash.isPending}
            className="rounded-full border-2 border-red-500 px-4 py-1.5 text-xs font-bold text-red-500 disabled:opacity-50">
            삭제
          </button>
        </div>
      </div>

      <div className="mt-4 space-y-4">
        {/* Notion 페이지형 헤더: 커버(대표 이미지) + 제목 + 속성 행 */}
        <div className={`overflow-hidden ${box}`}>
          {/* 커버 = 대표 이미지. 상태는 좌측, 사진 관리 버튼은 우측. */}
          <div className={`relative h-40 bg-gradient-to-br sm:h-48 ${accent.wash}`}>
            {cover && (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={cover} alt="" onError={(e) => { e.currentTarget.style.display = "none"; }}
                className="h-full w-full object-cover" />
            )}
            <span className={`absolute left-4 top-4 rounded-full border-2 border-neutral-900 px-3 py-1 text-xs font-bold dark:border-neutral-100 ${accent.solid}`}>
              {STATUS_LABEL[p.status] ?? p.status}
            </span>
          </div>

          <div className="p-5">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-neutral-400">Knitting Log</p>
            <h1 className="mt-1 text-3xl font-black tracking-tight">{p.displayTitle}</h1>

            {/* 속성 행 */}
            <dl className="mt-4 divide-y divide-neutral-100 dark:divide-neutral-900">
              <Prop icon="🧶" label="도안">
                {patternLabel}
                {p.patternType === "EXTERNAL" && (
                  <span className={`ml-2 rounded-full px-2 py-0.5 text-[11px] font-semibold ${accent.soft}`}>외부</span>
                )}
              </Prop>
              {designer && <Prop icon="✍️" label="원작자">{designer}</Prop>}
              <Prop icon="📅" label="시작일">{new Date(p.createdAt).toLocaleDateString("ko-KR")}</Prop>
              <Prop icon="🔓" label="공개">
                <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${p.visibility === "PUBLIC" ? accent.soft : "bg-neutral-100 text-neutral-500 dark:bg-neutral-800 dark:text-neutral-400"}`}>
                  {p.visibility === "PUBLIC" ? "공개" : "비공개"}
                </span>
              </Prop>
              {p.note && <Prop icon="💬" label="코멘트">{p.note}</Prop>}
            </dl>
          </div>
        </div>

        {/* 원작 정보 — 연결 시점에 복사한 도안 원작 정보 전체 */}
        {snap && (
          <Section title="원작 정보 (연결 시점 복사)">
            <dl className="space-y-1.5 text-sm">
              {snap.categoryName && <Row label="카테고리">{snap.categoryName}</Row>}
              {snap.craftType && <Row label="구분">{CRAFT_LABEL[snap.craftType] ?? snap.craftType}</Row>}
              {snap.difficulty && <Row label="난이도">{snap.difficulty}</Row>}
              {snap.language && <Row label="언어">{snap.language}</Row>}
              {snap.gauge && (snap.gauge.stitches != null || snap.gauge.rows != null) && (
                <Row label="게이지">
                  {snap.gauge.stitches}코 × {snap.gauge.rows}단
                  {snap.gauge.swatchWidthCm ? ` (${snap.gauge.swatchWidthCm}×${snap.gauge.swatchHeightCm}cm)` : ""}
                  {snap.gauge.needleSizeMm ? ` · 바늘 ${snap.gauge.needleSizeMm}mm` : ""}
                </Row>
              )}
              {snap.yarnRequirement && <Row label="실 소요량">{snap.yarnRequirement}</Row>}
              {snap.pageCount != null && <Row label="페이지 수">{snap.pageCount}p</Row>}
              <Row label="참고 영상">
                {snap.referenceVideoUrl
                  ? <a href={snap.referenceVideoUrl} target="_blank" rel="noreferrer" className="text-blue-600 underline">영상 보기</a>
                  : <span className="text-neutral-400">없음</span>}
              </Row>
            </dl>

            {snap.sizes && snap.sizes.length > 0 && (
              <div className="mt-3 overflow-x-auto">
                <table className="w-full min-w-[360px] text-sm">
                  <thead>
                    <tr className="border-b-2 border-neutral-900 text-left dark:border-neutral-100">
                      <th className="py-1.5 pr-4 font-bold">사이즈</th>
                      <th className="py-1.5 pr-4 font-bold">시작 콧수</th>
                      {measureKeys.map((k) => <th key={k} className="py-1.5 pr-4 font-bold">{MEASURE_LABEL[k] ?? k}</th>)}
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

        {/* 게이지 — 적용한 내 게이지(굵게) + 계산 결과 전체 */}
        {p.patternType !== "EXTERNAL" && (
          <Section title="게이지">
            {appliedGauge ? (
              <>
                <div className="flex items-start justify-between gap-3">
                  <p className="text-sm">
                    적용한 게이지{" "}
                    <span className="text-xl font-black">
                      {appliedGauge.myGauge?.stitches}코 {appliedGauge.myGauge?.rows}단
                    </span>
                    {appliedGauge.myGauge?.needleSizeMm != null && (
                      <span className="text-sm text-neutral-500"> · 바늘 {appliedGauge.myGauge.needleSizeMm}mm</span>
                    )}
                  </p>
                  <Link href={`/projects/${id}/gauge`} className="shrink-0 text-xs font-bold underline">다시 계산</Link>
                </div>
                {appliedGauge.result && (
                  <div className="mt-4">
                    <GaugeResultView result={appliedGauge.result} labels={MEASURE_LABEL} />
                  </div>
                )}
              </>
            ) : (
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm text-neutral-500">내 게이지 기준으로 조정 콧수·부위별 필요 콧수를 계산해 보세요.</p>
                <Link href={`/projects/${id}/gauge`} className="shrink-0 rounded-full border-2 border-neutral-900 px-4 py-1.5 text-sm font-bold dark:border-neutral-100">게이지 계산</Link>
              </div>
            )}
          </Section>
        )}

        {/* 오늘의 로그 — 게시물 리스트(15개씩). 작성은 팝업, 카드 클릭은 본문 팝업. */}
        <section className={`${box} p-5`}>
          <div className="mb-3 flex items-center justify-between gap-3">
            <h2 className="text-xs font-bold uppercase tracking-wider text-neutral-400">오늘의 로그</h2>
            <button type="button" onClick={() => setWriteOpen(true)}
              className="rounded-full border-2 border-neutral-900 bg-lime-300 px-4 py-1.5 text-sm font-bold text-neutral-900 transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100">
              + 오늘의 로그
            </button>
          </div>

          {(() => {
            const all = logs ?? [];
            const totalPages = Math.max(1, Math.ceil(all.length / LOGS_PER_PAGE));
            const page = Math.min(logPage, totalPages - 1);
            const slice = all.slice(page * LOGS_PER_PAGE, page * LOGS_PER_PAGE + LOGS_PER_PAGE);
            if (all.length === 0) {
              return <p className="py-3 text-sm text-neutral-400">아직 로그가 없습니다. 첫 기록을 남겨보세요.</p>;
            }
            return (
              <>
                <ul className="space-y-2">
                  {slice.map((l) => {
                    const images = extractImages(l.contentDocument);
                    return (
                      <li key={l.id}>
                        <button type="button" onClick={() => setOpenLog(l)}
                          className="w-full rounded-2xl border-2 border-neutral-900 p-3 text-left transition hover:-translate-y-0.5 hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100 dark:hover:shadow-[3px_3px_0_0_rgba(255,255,255,0.9)]">
                          <div className="flex justify-between gap-3">
                            {/* 좌: 상태·제목·요약 */}
                            <div className="min-w-0 flex-1">
                              <div className="flex items-center gap-2">
                                <span className={statusPill(l.knittingStatus)}>
                                  {l.knittingStatus ? STATUS_LABEL[l.knittingStatus] ?? l.knittingStatus : "로그"}
                                </span>
                                <span className="line-clamp-1 text-sm font-bold">{l.displayTitle}</span>
                              </div>
                              {(() => { const s = logSnippet(l.contentDocument, l.comment); return s ? <p className="mt-1.5 line-clamp-1 text-sm text-neutral-500">{s}</p> : null; })()}
                            </div>
                            {/* 우: 날짜 + 그 아래 대표 이미지 썸네일 */}
                            <div className="flex shrink-0 flex-col items-end gap-1.5">
                              <div className="flex items-center gap-2 text-xs text-neutral-400">
                                {l.visibility === "PUBLIC" && <span className="font-bold text-neutral-500">공개</span>}
                                <span>{l.logDate}</span>
                              </div>
                              {images.length > 0 && (
                                <div className="relative">
                                  {/* eslint-disable-next-line @next/next/no-img-element */}
                                  <img src={images[0]} alt="" onError={(e) => { e.currentTarget.style.display = "none"; }}
                                    className="h-16 w-16 rounded-lg border border-neutral-300 object-cover dark:border-neutral-700" />
                                  {images.length > 1 && (
                                    <span className="absolute bottom-1 right-1 rounded-md bg-black/70 px-1.5 py-0.5 text-[10px] font-bold text-white">+{images.length - 1}</span>
                                  )}
                                </div>
                              )}
                            </div>
                          </div>
                        </button>
                      </li>
                    );
                  })}
                </ul>

                {totalPages > 1 && (
                  <div className="mt-4 flex items-center justify-center gap-4 text-sm">
                    <button type="button" onClick={() => setLogPage(page - 1)} disabled={page <= 0}
                      className="rounded-full border-2 border-neutral-900 px-3 py-1 font-bold disabled:opacity-40 dark:border-neutral-100">이전</button>
                    <span className="text-neutral-500">{page + 1} / {totalPages}</span>
                    <button type="button" onClick={() => setLogPage(page + 1)} disabled={page >= totalPages - 1}
                      className="rounded-full border-2 border-neutral-900 px-3 py-1 font-bold disabled:opacity-40 dark:border-neutral-100">다음</button>
                  </div>
                )}
              </>
            );
          })()}
        </section>
      </div>

      {openLog && <LogModal log={openLog} onClose={() => setOpenLog(null)} />}

      {writeOpen && (
        <Modal onClose={() => setWriteOpen(false)} maxWidth="max-w-3xl">
          <div className="flex items-center justify-between gap-3">
            <h3 className="text-lg font-black tracking-tight">오늘의 로그 작성</h3>
            <button type="button" onClick={() => setWriteOpen(false)} aria-label="닫기"
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 border-neutral-900 text-sm font-bold hover:bg-neutral-100 dark:border-neutral-100 dark:hover:bg-neutral-800">
              ✕
            </button>
          </div>
          <div className="mt-4">
            <QuickLogForm projectId={id} currentStatus={p.status} projectVisibility={p.visibility}
              onDone={() => { invalidate(); setWriteOpen(false); }} />
          </div>
        </Modal>
      )}
    </main>
  );
}

/** 모달 공통 셸 — 배경 딤·Esc·스크롤 잠금. body 포털로 렌더해 뷰포트 정중앙에 고정. */
function Modal({ onClose, children, maxWidth = "max-w-2xl" }: { onClose: () => void; children: React.ReactNode; maxWidth?: string }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => { window.removeEventListener("keydown", onKey); document.body.style.overflow = ""; };
  }, [onClose]);

  if (typeof document === "undefined") return null; // SSR 가드
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} aria-hidden />
      <div className={`relative z-10 max-h-[85vh] w-full ${maxWidth} overflow-y-auto rounded-3xl border-2 border-neutral-900 bg-white p-6 dark:border-neutral-100 dark:bg-neutral-950`}>
        {children}
      </div>
    </div>,
    document.body,
  );
}

/** 오늘의 로그 본문 팝업. 리스트에서 넘겨받은 데이터로 본문을 렌더한다. */
function LogModal({ log, onClose }: { log: LogItem; onClose: () => void }) {
  return (
    <Modal onClose={onClose}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className={statusPill(log.knittingStatus)}>
              {log.knittingStatus ? STATUS_LABEL[log.knittingStatus] ?? log.knittingStatus : "로그"}
            </span>
            <span className="text-xs text-neutral-400">{log.logDate}</span>
            <span className="text-xs font-bold text-neutral-500">{log.visibility === "PUBLIC" ? "공개" : "비공개"}</span>
          </div>
          <h3 className="mt-2 text-xl font-black tracking-tight">{log.displayTitle}</h3>
        </div>
        <button type="button" onClick={onClose} aria-label="닫기"
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 border-neutral-900 text-sm font-bold hover:bg-neutral-100 dark:border-neutral-100 dark:hover:bg-neutral-800">
          ✕
        </button>
      </div>

      <div className="mt-4">
        {log.contentDocument ? (
          <div className="text-sm text-neutral-800 dark:text-neutral-200">
            <RichContent doc={log.contentDocument as JSONContent} />
          </div>
        ) : log.comment ? (
          <p className="whitespace-pre-line text-sm text-neutral-700 dark:text-neutral-300">{log.comment}</p>
        ) : (
          <p className="text-sm text-neutral-400">내용이 없습니다.</p>
        )}
      </div>
    </Modal>
  );
}

function QuickLogForm({
  projectId, currentStatus, projectVisibility, onDone,
}: { projectId: number; currentStatus: string; projectVisibility: string; onDone: () => void }) {
  const [status, setStatus] = useState("CO");
  const [title, setTitle] = useState("");
  const [docJson, setDocJson] = useState<JSONContent | null>(null);
  const [docText, setDocText] = useState("");
  const [makePublic, setMakePublic] = useState(false);
  const [editorKey, setEditorKey] = useState(0); // 제출 후 에디터 초기화용

  useEffect(() => { setStatus(currentStatus === "PLANNED" ? "CO" : currentStatus); }, [currentStatus]);

  const submit = useMutation({
    mutationFn: () => {
      const hasBody = docText.trim().length > 0;
      const body: Parameters<typeof projectApi.createLog>[1] = {
        knittingStatus: status,
        title: title.trim() || undefined,
        comment: docText.trim() || undefined,
        contentDocument: hasBody ? (docJson ?? undefined) : undefined,
      };
      if (makePublic) {
        body.visibility = "PUBLIC";
        if (projectVisibility === "PRIVATE") {
          body.publishProjectConfirmed = window.confirm("이 니팅로그는 비공개입니다. 로그를 공개하면 니팅로그도 함께 공개됩니다. 함께 공개할까요?");
        }
      }
      return projectApi.createLog(projectId, body);
    },
    onSuccess: () => { setTitle(""); setDocJson(null); setDocText(""); setMakePublic(false); setEditorKey((k) => k + 1); onDone(); },
  });

  return (
    <form onSubmit={(e) => { e.preventDefault(); submit.mutate(); }}>
      <div className="mb-2 flex items-center gap-2">
        <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="상태"
          className="rounded-full border-2 border-neutral-900 bg-transparent px-3 py-2 text-sm dark:border-neutral-100">
          {STATUS_ORDER.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
        </select>
        <label className="flex items-center gap-2 text-xs text-neutral-500">
          <input type="checkbox" checked={makePublic} onChange={(e) => setMakePublic(e.target.checked)} className="h-3.5 w-3.5" />
          이 로그 공개
        </label>
      </div>
      <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={60}
        placeholder="제목 (비우면 날짜로 자동 지정)" aria-label="로그 제목"
        className="mb-2 w-full rounded-xl border-2 border-neutral-900 bg-transparent px-3 py-2 text-sm font-bold outline-none dark:border-neutral-100" />
      <RichEditor key={editorKey} usageType="PROJECT_IMAGE" placeholder="오늘의 기록 — 줄글·사진·표"
        onChange={(v) => { setDocJson(v.json); setDocText(v.text); }} />
      <div className="mt-3 flex justify-center">
        <button type="submit" disabled={submit.isPending || !docText.trim()}
          className="rounded-full border-2 border-neutral-900 bg-lime-300 px-8 py-2 text-sm font-bold text-neutral-900 transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] disabled:opacity-50 disabled:shadow-none dark:border-neutral-100">기록</button>
      </div>
    </form>
  );
}

// Notion 페이지 속성 행 — 아이콘 + 라벨 + 값
function Prop({ icon, label, children }: { icon: string; label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start gap-3 py-2 text-sm">
      <div className="flex w-28 shrink-0 items-center gap-2 text-neutral-400">
        <span aria-hidden>{icon}</span>
        <span>{label}</span>
      </div>
      <div className="min-w-0 flex-1 font-medium text-neutral-800 dark:text-neutral-200">{children}</div>
    </div>
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
