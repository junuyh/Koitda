"use client";

import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { patternApi } from "@/features/pattern/api";
import type { SavePatternDraftBody, SellerPatternPreview } from "@/features/seller/api";
import { ImageUploader } from "@/features/file/ImageUploader";
import { fileApi } from "@/features/file/api";
import { RichEditor } from "@/features/editor/RichEditor";
import type { JSONContent } from "@tiptap/react";

/** preview 이미지 URL(/api/v1/files/123)에서 fileId 를 뽑는다(수정 진입 프리필용). */
function fileIdsFromPreview(p?: SellerPatternPreview): number[] {
  if (!p?.images) return [];
  return p.images
    .map((im) => Number((im.url ?? "").split("/").pop()))
    .filter((n) => Number.isFinite(n) && n > 0);
}

/** PATTERN-011 권장 실측 항목(의류). 사이즈 행마다 선택 입력. */
const MEASURE_COLS: Array<{ key: string; label: string }> = [
  { key: "chestCm", label: "가슴둘레" },
  { key: "lengthCm", label: "총장" },
  { key: "sleeveLengthCm", label: "소매길이" },
  { key: "shoulderCm", label: "어깨너비" },
];

type SizeRowState = { label: string; castOnStitches: string } & Record<string, string>;
type GaugeState = { stitches: string; rows: string; swatchWidthCm: string; swatchHeightCm: string; needleSizeMm: string; text: string };
type NeedleRow = { type: "KNIT" | "CROCHET"; sizeMm: string };

const emptyGauge: GaugeState = { stitches: "", rows: "", swatchWidthCm: "10", swatchHeightCm: "10", needleSizeMm: "", text: "" };
const emptySize = (): SizeRowState => ({ label: "", castOnStitches: "" });
const emptyNeedle = (): NeedleRow => ({ type: "KNIT", sizeMm: "" });

export const inputClass =
  "w-full rounded-md border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100";

/** preview(수정 진입) → 폼 초기 상태 매핑. */
function fromPreview(p: SellerPatternPreview): FormState {
  return {
    title: p.title ?? "",
    designerName: p.designerName ?? "",
    categoryId: p.categoryId != null ? String(p.categoryId) : "",
    craftType: (p.craftType as FormState["craftType"]) ?? "KNIT",
    difficulty: p.difficulty ?? "",
    language: p.language ?? "ko",
    regularPrice: p.regularPrice != null ? String(p.regularPrice) : "",
    salePrice: p.salePrice != null ? String(p.salePrice) : "",
    productForm: p.productForm ?? "",
    deliveryMethod: p.deliveryMethod ?? "",
    availabilityDays: p.availabilityDays != null ? String(p.availabilityDays) : "",
    referenceVideoUrl: p.referenceVideoUrl ?? "",
    pageCount: p.pageCount != null ? String(p.pageCount) : "",
    yarnRequirement: p.yarnRequirement ?? "",
    description: p.description ?? "",
    gauge: p.gaugeInfo
      ? {
          stitches: numStr(p.gaugeInfo.stitches),
          rows: numStr(p.gaugeInfo.rows),
          swatchWidthCm: numStr(p.gaugeInfo.swatchWidthCm),
          swatchHeightCm: numStr(p.gaugeInfo.swatchHeightCm),
          needleSizeMm: numStr(p.gaugeInfo.needleSizeMm),
          text: (p.gaugeInfo as { text?: string }).text ?? "",
        }
      : { ...emptyGauge },
    needles: (() => {
      const raw = (p as { needleInfo?: unknown }).needleInfo;
      if (Array.isArray(raw) && raw.length > 0) {
        return raw.map((n) => {
          const nn = n as { type?: string; sizeMm?: number };
          return { type: (nn.type === "CROCHET" ? "CROCHET" : "KNIT") as "KNIT" | "CROCHET", sizeMm: numStr(nn.sizeMm) };
        });
      }
      return [emptyNeedle()];
    })(),
    sizes:
      p.sizeInfo?.sizes?.map((s) => {
        const row: SizeRowState = { label: s.label ?? "", castOnStitches: numStr(s.castOnStitches) };
        for (const c of MEASURE_COLS) row[c.key] = numStr(s.measurements?.[c.key]);
        return row;
      }) ?? [emptySize()],
  };
}

function numStr(n: number | undefined | null): string {
  return n == null ? "" : String(n);
}

type FormState = {
  title: string;
  designerName: string;
  categoryId: string;
  craftType: "KNIT" | "CROCHET" | "MIXED";
  difficulty: string;
  language: string;
  regularPrice: string;
  salePrice: string;
  productForm: string;
  deliveryMethod: string;
  availabilityDays: string;
  referenceVideoUrl: string;
  pageCount: string;
  yarnRequirement: string;
  description: string;
  gauge: GaugeState;
  needles: NeedleRow[];
  sizes: SizeRowState[];
};

const blankForm: FormState = {
  title: "",
  designerName: "",
  categoryId: "",
  craftType: "KNIT",
  difficulty: "",
  language: "ko",
  regularPrice: "",
  salePrice: "",
  productForm: "PDF",
  deliveryMethod: "DOWNLOAD",
  availabilityDays: "365", // 제공 기간 자동 1년(사용자가 바꾸면 그 값)
  referenceVideoUrl: "",
  pageCount: "",
  yarnRequirement: "",
  description: "",
  gauge: { ...emptyGauge },
  needles: [emptyNeedle()],
  sizes: [emptySize()],
};

export function PatternForm({
  initial,
  submitting,
  submitLabel,
  onSubmit,
}: {
  initial?: SellerPatternPreview;
  submitting: boolean;
  submitLabel: string;
  onSubmit: (body: SavePatternDraftBody) => void;
}) {
  const [form, setForm] = useState<FormState>(() => (initial ? fromPreview(initial) : blankForm));
  const [imageIds, setImageIds] = useState<number[]>(() => fileIdsFromPreview(initial));
  const [pdfId, setPdfId] = useState<number | null>(() => initial?.pdfFileId ?? null);
  const [pdfUploading, setPdfUploading] = useState(false);
  const [descDoc, setDescDoc] = useState<JSONContent | null>(() => (initial?.descriptionDocument as JSONContent) ?? null);
  const [error, setError] = useState<string | null>(null);

  const { data: categories } = useQuery({ queryKey: ["pattern-categories"], queryFn: patternApi.categories });
  // 카테고리 트리를 들여쓰기된 평면 옵션으로 펼친다(대분류 → 소분류).
  const categoryOptions = useMemo(() => {
    const all = categories ?? [];
    const byParent = new Map<number | null, typeof all>();
    for (const c of all) {
      const k = c.parentId ?? null;
      if (!byParent.has(k)) byParent.set(k, []);
      byParent.get(k)!.push(c);
    }
    for (const arr of byParent.values()) arr.sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id);
    const out: Array<{ id: number; label: string }> = [];
    const walk = (parent: number | null, depth: number) => {
      for (const c of byParent.get(parent) ?? []) {
        out.push({ id: c.id, label: `${"　".repeat(depth)}${depth > 0 ? "└ " : ""}${c.name}` });
        walk(c.id, depth + 1);
      }
    };
    walk(null, 0);
    return out;
  }, [categories]);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }
  function setGauge<K extends keyof GaugeState>(key: K, value: string) {
    setForm((f) => ({ ...f, gauge: { ...f.gauge, [key]: value } }));
  }
  function setSize(i: number, key: string, value: string) {
    setForm((f) => ({ ...f, sizes: f.sizes.map((r, idx) => (idx === i ? { ...r, [key]: value } : r)) }));
  }
  function setNeedle(i: number, patch: Partial<NeedleRow>) {
    setForm((f) => ({ ...f, needles: f.needles.map((r, idx) => (idx === i ? { ...r, ...patch } : r)) }));
  }

  function build(): SavePatternDraftBody | null {
    setError(null);

    // 사용 바늘(여러 개). mm 가 있는 행만 채택.
    const needles = form.needles
      .filter((n) => n.sizeMm.trim() !== "")
      .map((n) => ({ type: n.type, sizeMm: Number(n.sizeMm) }));
    if (needles.some((n) => !Number.isFinite(n.sizeMm) || n.sizeMm <= 0)) {
      setError("바늘 mm 는 0보다 큰 숫자여야 합니다.");
      return null;
    }

    // 게이지: 코·단·기준크기(4개)는 모두 채우거나 모두 비우거나. 코바늘 등은 비우고 자유 서술만.
    const g = form.gauge;
    const numVals = [g.stitches, g.rows, g.swatchWidthCm, g.swatchHeightCm];
    const gaugeFilled = numVals.filter((v) => v.trim() !== "").length;
    let gauge: SavePatternDraftBody["gauge"];
    if (gaugeFilled > 0 && gaugeFilled < 4) {
      setError("게이지 숫자는 코수·단수·기준 너비·기준 높이를 모두 입력하거나 모두 비워 주세요. (코바늘은 비우고 자유 서술)");
      return null;
    }
    const gaugeText = g.text.trim();
    if (gaugeFilled === 4) {
      if (needles.length === 0) {
        setError("게이지 계산을 위해 사용 바늘을 1개 이상 입력하세요.");
        return null;
      }
      const nums = {
        stitches: Number(g.stitches),
        rows: Number(g.rows),
        swatchWidthCm: Number(g.swatchWidthCm),
        swatchHeightCm: Number(g.swatchHeightCm),
      };
      if (Object.values(nums).some((n) => !Number.isFinite(n) || n <= 0)) {
        setError("게이지 값은 모두 0보다 큰 숫자여야 합니다.");
        return null;
      }
      // 게이지 계산 기준 바늘 = 첫 번째 바늘 mm
      gauge = { ...nums, needleSizeMm: needles[0].sizeMm, text: gaugeText || undefined };
    } else if (gaugeText) {
      gauge = { text: gaugeText }; // 자유 텍스트만
    }

    // 사이즈: 사이즈명이 있는 행만 '의도된' 행으로 본다. 시작 콧수는 선택(입력 시 양의 정수).
    const sizes: SavePatternDraftBody["sizes"] = [];
    for (const row of form.sizes) {
      if (!row.label.trim()) continue;
      let castOn: number | undefined;
      if (row.castOnStitches.trim() !== "") {
        const cast = Number(row.castOnStitches);
        if (!Number.isInteger(cast) || cast <= 0) {
          setError(`'${row.label}' 사이즈의 시작 콧수는 양의 정수로 입력하세요(선택 항목).`);
          return null;
        }
        castOn = cast;
      }
      const measurements: Record<string, number> = {};
      for (const c of MEASURE_COLS) {
        const v = row[c.key];
        if (v && v.trim() !== "") {
          const n = Number(v);
          if (!Number.isFinite(n) || n < 0) {
            setError(`'${row.label}' 사이즈의 ${c.label} 값이 올바르지 않습니다.`);
            return null;
          }
          measurements[c.key] = n;
        }
      }
      sizes.push({ label: row.label.trim(), castOnStitches: castOn, measurements });
    }

    return {
      title: form.title.trim() || undefined,
      designerName: form.designerName.trim() || undefined,
      categoryId: form.categoryId ? Number(form.categoryId) : undefined,
      craftType: form.craftType,
      difficulty: form.difficulty.trim() || undefined,
      language: form.language.trim() || undefined,
      regularPrice: form.regularPrice ? Number(form.regularPrice) : undefined,
      salePrice: form.salePrice ? Number(form.salePrice) : undefined,
      productForm: "PDF", // 상품 형식·다운로드 방식은 현재 PDF 다운로드 고정(폼에서 제거)
      deliveryMethod: "DOWNLOAD",
      availabilityDays: form.availabilityDays ? Number(form.availabilityDays) : undefined,
      referenceVideoUrl: form.referenceVideoUrl.trim() || undefined,
      pageCount: form.pageCount ? Number(form.pageCount) : undefined,
      yarnRequirement: form.yarnRequirement.trim() || undefined,
      description: form.description.trim() || undefined,
      descriptionDocument: descDoc ?? undefined,
      gauge,
      needle: needles.length ? needles : undefined,
      sizes: sizes.length ? sizes : undefined,
      imageFileIds: imageIds.length ? imageIds : undefined,
      thumbnailFileId: imageIds[0],
      pdfFileId: pdfId ?? undefined,
    };
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const body = build();
    if (body) onSubmit(body);
  }

  return (
    <form className="mt-6 space-y-6" onSubmit={handleSubmit} noValidate>
      <Section title="기본 정보">
        <div className="grid gap-3 sm:grid-cols-2">
          <Labeled label="도안명">
            <input value={form.title} onChange={(e) => set("title", e.target.value)} className={inputClass} />
          </Labeled>
          <Labeled label="원작자">
            <input value={form.designerName} onChange={(e) => set("designerName", e.target.value)} className={inputClass} />
          </Labeled>
          <Labeled label="카테고리">
            <select value={form.categoryId} onChange={(e) => set("categoryId", e.target.value)} className={inputClass}>
              <option value="">선택 안 함</option>
              {categoryOptions.map((c) => (
                <option key={c.id} value={c.id}>{c.label}</option>
              ))}
            </select>
          </Labeled>
          <Labeled label="뜨개 방식">
            <select value={form.craftType} onChange={(e) => set("craftType", e.target.value as FormState["craftType"])} className={inputClass}>
              <option value="KNIT">대바늘</option>
              <option value="CROCHET">코바늘</option>
              <option value="MIXED">혼합</option>
            </select>
          </Labeled>
          <Labeled label="난이도">
            <input value={form.difficulty} onChange={(e) => set("difficulty", e.target.value)} placeholder="초급·중급·고급" className={inputClass} />
          </Labeled>
          <Labeled label="언어">
            <input value={form.language} onChange={(e) => set("language", e.target.value)} className={inputClass} />
          </Labeled>
        </div>
      </Section>

      <Section title="판매 정보">
        <div className="grid gap-3 sm:grid-cols-2">
          <Labeled label="정상가 (원)">
            <input inputMode="numeric" value={form.regularPrice} onChange={(e) => set("regularPrice", e.target.value)} className={inputClass} />
          </Labeled>
          <Labeled label="판매가 (원)">
            <input inputMode="numeric" value={form.salePrice} onChange={(e) => set("salePrice", e.target.value)} className={inputClass} />
          </Labeled>
          <Labeled label="제공 기간 (일, 기본 1년)">
            <input inputMode="numeric" value={form.availabilityDays} onChange={(e) => set("availabilityDays", e.target.value)} placeholder="365" className={inputClass} />
          </Labeled>
          <Labeled label="페이지 수">
            <input inputMode="numeric" value={form.pageCount} onChange={(e) => set("pageCount", e.target.value)} className={inputClass} />
          </Labeled>
        </div>
      </Section>

      <Section title="게이지">
        <p className="mb-2 text-xs text-neutral-500">
          대바늘은 코수·단수·기준 크기를 모두 채우거나 모두 비워 주세요(게이지 계산 기준값).
          코바늘은 10×10 게이지가 없을 수 있어요 — 그때는 비우고 실 소요량·상세 설명에 서술하세요.
        </p>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          <GaugeInput label="코수" value={form.gauge.stitches} onChange={(v) => setGauge("stitches", v)} />
          <GaugeInput label="단수" value={form.gauge.rows} onChange={(v) => setGauge("rows", v)} />
          <GaugeInput label="기준 너비(cm)" value={form.gauge.swatchWidthCm} onChange={(v) => setGauge("swatchWidthCm", v)} />
          <GaugeInput label="기준 높이(cm)" value={form.gauge.swatchHeightCm} onChange={(v) => setGauge("swatchHeightCm", v)} />
        </div>
        <label className="mt-3 block">
          <span className="mb-1 block text-[11px] text-neutral-500">게이지 자유 서술 (코바늘 등 — 예: 1인치(2.5cm)=한길긴뜨기 10코)</span>
          <input value={form.gauge.text} onChange={(e) => setGauge("text", e.target.value)} className={inputClass} />
        </label>
      </Section>

      <Section title="사용 바늘" onAdd={() => set("needles", [...form.needles, emptyNeedle()])}>
        <p className="mb-2 text-xs text-neutral-500">게이지와 별도로 입력합니다. 여러 개 등록 가능하며, 첫 번째 바늘이 게이지 계산 기준이 됩니다.</p>
        <div className="space-y-2">
          {form.needles.map((n, i) => (
            <div key={i} className="flex items-center gap-2">
              <select value={n.type} onChange={(e) => setNeedle(i, { type: e.target.value as NeedleRow["type"] })} className={`${inputClass} max-w-32`}>
                <option value="KNIT">대바늘</option>
                <option value="CROCHET">코바늘</option>
              </select>
              <input placeholder="mm (예: 4.5)" inputMode="decimal" value={n.sizeMm} onChange={(e) => setNeedle(i, { sizeMm: e.target.value })} className={inputClass} />
              {form.needles.length > 1 && (
                <button type="button" onClick={() => set("needles", form.needles.filter((_, idx) => idx !== i))} className="shrink-0 text-xs text-red-500 hover:underline">삭제</button>
              )}
            </div>
          ))}
        </div>
      </Section>

      <Section title="사이즈 · 완성 실측" onAdd={() => set("sizes", [...form.sizes, emptySize()])}>
        <p className="mb-2 text-xs text-neutral-500">사이즈명만 필수입니다. 시작 콧수·완성 실측은 선택이며, 실측은 게이지 계산의 기준이 됩니다. (의류 상의·드레스는 가슴둘레 권장)</p>
        <div className="space-y-3">
          {form.sizes.map((row, i) => (
            <div key={i} className="rounded-md border border-neutral-200 p-3 dark:border-neutral-800">
              {/* 앞: 필수/주요 데이터 — 사이즈명 · 가슴둘레 · 총장 · 소매길이 */}
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-6">
                <div className="sm:col-span-3">
                  <input placeholder="사이즈명 (예: M)" value={row.label} onChange={(e) => setSize(i, "label", e.target.value)} className={inputClass} />
                </div>
                {MEASURE_COLS.slice(0, 3).map((c) => (
                  <input key={c.key} placeholder={c.label} inputMode="decimal" value={row[c.key] ?? ""} onChange={(e) => setSize(i, c.key, e.target.value)} className={inputClass} />
                ))}
              </div>
              {/* 뒤: 부가 데이터 — 어깨너비 · 시작 콧수(게이지 계산용, 구매 전 미노출) */}
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <input placeholder={`${MEASURE_COLS[3].label} (선택)`} inputMode="decimal" value={row[MEASURE_COLS[3].key] ?? ""} onChange={(e) => setSize(i, MEASURE_COLS[3].key, e.target.value)} className={`${inputClass} max-w-40`} />
                <input placeholder="시작 콧수 (선택·게이지 계산용)" inputMode="numeric" value={row.castOnStitches} onChange={(e) => setSize(i, "castOnStitches", e.target.value)} className={`${inputClass} max-w-56`} />
                {form.sizes.length > 1 && (
                  <button type="button" onClick={() => set("sizes", form.sizes.filter((_, idx) => idx !== i))} className="ml-auto text-xs text-red-500 hover:underline">행 삭제</button>
                )}
              </div>
            </div>
          ))}
        </div>
      </Section>

      <Section title="상세">
        <Labeled label="실 소요량">
          <input value={form.yarnRequirement} onChange={(e) => set("yarnRequirement", e.target.value)} placeholder="메리노 400g 등" className={inputClass} />
        </Labeled>
        <Labeled label="참고 동영상 URL">
          <input value={form.referenceVideoUrl} onChange={(e) => set("referenceVideoUrl", e.target.value)} className={inputClass} />
        </Labeled>
        <Labeled label="상세 설명 (블로그 형식 — 줄글·사진·표)">
          <RichEditor
            initial={(initial?.descriptionDocument as JSONContent) ?? null}
            usageType="PATTERN_IMAGE"
            onChange={(v) => { setDescDoc(v.json); set("description", v.text); }}
          />
        </Labeled>
      </Section>

      <Section title="이미지">
        <ImageUploader value={imageIds} onChange={setImageIds} usageType="PATTERN_IMAGE" max={7} />
      </Section>

      <Section title="도안 PDF">
        {pdfId ? (
          <div className="flex items-center gap-3 text-sm">
            <span className="rounded-md bg-neutral-100 px-2 py-1 dark:bg-neutral-800">PDF 첨부됨 (#{pdfId})</span>
            <button type="button" onClick={() => setPdfId(null)} className="text-xs text-red-500 hover:underline">제거</button>
          </div>
        ) : (
          <label className="inline-flex cursor-pointer items-center gap-2 rounded-md border-2 border-dashed border-neutral-300 px-3 py-2 text-sm text-neutral-500 dark:border-neutral-700">
            {pdfUploading ? "올리는 중…" : "+ PDF 선택"}
            <input type="file" accept="application/pdf" hidden disabled={pdfUploading}
              onChange={async (e) => {
                const f = e.target.files?.[0];
                if (!f) return;
                setPdfUploading(true);
                try { const r = await fileApi.upload(f, "PATTERN_PDF"); setPdfId(r.id); }
                catch { window.alert("PDF 업로드에 실패했습니다."); }
                finally { setPdfUploading(false); e.target.value = ""; }
              }} />
          </label>
        )}
        <p className="mt-1 text-xs text-neutral-400">구매자가 이 PDF를 다운로드합니다. (PDF만, 30MB 이하)</p>
      </Section>

      {error && <p role="alert" className="text-sm text-red-600">{error}</p>}

      <button type="submit" disabled={submitting}
        className="w-full rounded-md bg-neutral-900 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
        {submitting ? "저장 중…" : submitLabel}
      </button>
    </form>
  );
}

function GaugeInput({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <label className="block">
      <span className="mb-1 block text-[11px] text-neutral-500">{label}</span>
      <input inputMode="decimal" value={value} onChange={(e) => onChange(e.target.value)} className={inputClass} />
    </label>
  );
}

function Section({ title, onAdd, children }: { title: string; onAdd?: () => void; children: React.ReactNode }) {
  return (
    <section>
      <div className="mb-2 flex items-center justify-between">
        <h2 className="text-sm font-semibold">{title}</h2>
        {onAdd && (
          <button type="button" onClick={onAdd} className="text-xs text-neutral-500 hover:underline">+ 행 추가</button>
        )}
      </div>
      <div>{children}</div>
    </section>
  );
}

function Labeled({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="mb-3 block">
      <span className="mb-1 block text-xs text-neutral-500">{label}</span>
      {children}
    </label>
  );
}
