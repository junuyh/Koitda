"use client";

import { useState } from "react";

// 약관 전문은 순수 모듈(legalText)에서 가져와 재노출 — 서버 컴포넌트 약관 페이지와 공용.
export { SERVICE_TERMS, PRIVACY_POLICY, MARKETING_INFO } from "./legalText";

/** 접었다 펼치는 약관 전문 상자. */
export function TermsDocument({ title, body }: { title: string; body: string }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="ml-6">
      <button type="button" onClick={() => setOpen((v) => !v)}
        className="text-xs text-neutral-500 underline hover:text-neutral-800 dark:hover:text-neutral-200">
        {open ? "전문 접기" : "전문 보기"}
      </button>
      {open && (
        <pre className="mt-1 max-h-40 overflow-y-auto whitespace-pre-wrap rounded-md border border-neutral-200 bg-neutral-50 p-3 text-xs leading-relaxed text-neutral-600 dark:border-neutral-800 dark:bg-neutral-900 dark:text-neutral-400">
          {body}
        </pre>
      )}
    </div>
  );
}
