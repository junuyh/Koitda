"use client";

import { useEffect, useState } from "react";
import {
  MARKETING_INFO,
  PRIVACY_POLICY,
  SERVICE_TERMS,
  TermsDocument,
} from "./consentDocuments";

export type ConsentState = { service: boolean; privacy: boolean; marketing: boolean };

/**
 * 약관 3종 동의 UI(서비스·개인정보 필수 / 마케팅 선택) + 전문 보기.
 * 자체 상태를 관리하고 변경 시 onChange 로 알린다 — 이메일 가입·카카오 동의 양쪽에서 재사용.
 */
export function ConsentAgreements({ onChange }: { onChange: (s: ConsentState) => void }) {
  const [state, setState] = useState<ConsentState>({ service: false, privacy: false, marketing: false });

  useEffect(() => { onChange(state); }, [state, onChange]);

  const allRequired = state.service && state.privacy;
  const setAll = (v: boolean) => setState({ service: v, privacy: v, marketing: v });

  return (
    <div className="space-y-3">
      <label className="flex items-center gap-2 rounded-lg border-2 border-neutral-900 px-3 py-2 text-sm font-medium dark:border-neutral-100">
        <input type="checkbox" checked={state.service && state.privacy && state.marketing}
          onChange={(e) => setAll(e.target.checked)}
          className="h-4 w-4 accent-neutral-900 dark:accent-neutral-100" />
        약관 전체에 동의합니다.
      </label>

      <div className="space-y-2">
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={state.service}
            onChange={(e) => setState((s) => ({ ...s, service: e.target.checked }))}
            className="h-4 w-4 accent-neutral-900 dark:accent-neutral-100" />
          <span>(필수) 서비스 이용약관에 동의합니다.</span>
        </label>
        <TermsDocument title="서비스 이용약관" body={SERVICE_TERMS} />

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={state.privacy}
            onChange={(e) => setState((s) => ({ ...s, privacy: e.target.checked }))}
            className="h-4 w-4 accent-neutral-900 dark:accent-neutral-100" />
          <span>(필수) 개인정보 수집·이용에 동의합니다.</span>
        </label>
        <TermsDocument title="개인정보 처리방침" body={PRIVACY_POLICY} />

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={state.marketing}
            onChange={(e) => setState((s) => ({ ...s, marketing: e.target.checked }))}
            className="h-4 w-4 accent-neutral-900 dark:accent-neutral-100" />
          <span>(선택) 마케팅 정보 수신에 동의합니다.</span>
        </label>
        <TermsDocument title="마케팅 정보 수신" body={MARKETING_INFO} />
      </div>

      {!allRequired && (
        <p className="text-xs text-neutral-400">서비스 이용약관과 개인정보 수집·이용 동의는 필수입니다.</p>
      )}
    </div>
  );
}
