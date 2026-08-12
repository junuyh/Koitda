"use client";

import { useRef, useState } from "react";
import { ApiError } from "@/lib/api";
import { fileApi, fileUrl, type UploadUsage } from "@/features/file/api";

/**
 * 이미지 다중 업로드 위젯. 선택 즉시 업로드하고 file_asset id 목록을 부모에 올린다.
 * 첫 번째 이미지가 대표(썸네일). value 는 업로드된 fileId 배열.
 */
export function ImageUploader({
  value, onChange, usageType = "PATTERN_IMAGE", max = 7,
}: {
  value: number[];
  onChange: (ids: number[]) => void;
  usageType?: UploadUsage;
  max?: number;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSelect(files: FileList | null) {
    if (!files || files.length === 0) return;
    setError(null);
    setUploading(true);
    try {
      const room = max - value.length;
      const picked = Array.from(files).slice(0, Math.max(0, room));
      const ids: number[] = [];
      for (const f of picked) {
        const res = await fileApi.upload(f, usageType);
        ids.push(res.id);
      }
      onChange([...value, ...ids]);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "업로드 중 오류가 발생했습니다.");
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  }

  return (
    <div>
      <div className="flex flex-wrap gap-2">
        {value.map((id, i) => (
          <div key={id} className="relative h-20 w-20 overflow-hidden rounded-lg border border-neutral-300 dark:border-neutral-700">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={fileUrl(id)} alt="" className="h-full w-full object-cover" />
            {i === 0 && (
              <span className="absolute left-0 top-0 bg-neutral-900/80 px-1 text-[10px] text-white">대표</span>
            )}
            <button type="button" onClick={() => onChange(value.filter((x) => x !== id))}
              className="absolute right-0.5 top-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-neutral-900/70 text-xs text-white">×</button>
          </div>
        ))}

        {value.length < max && (
          <button type="button" onClick={() => inputRef.current?.click()} disabled={uploading}
            className="flex h-20 w-20 flex-col items-center justify-center rounded-lg border-2 border-dashed border-neutral-300 text-xs text-neutral-500 disabled:opacity-50 dark:border-neutral-700">
            {uploading ? "올리는 중…" : "+ 이미지"}
          </button>
        )}
      </div>
      <input ref={inputRef} type="file" accept="image/*" multiple hidden onChange={(e) => onSelect(e.target.files)} />
      {error && <p role="alert" className="mt-1 text-xs text-red-600">{error}</p>}
      <p className="mt-1 text-xs text-neutral-400">최대 {max}장 · 첫 이미지가 대표로 표시됩니다.</p>
    </div>
  );
}
