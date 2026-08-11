"use client";

import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { sellerApi, type SellerApplicationBody } from "@/features/seller/api";

const inputClass =
  "w-full rounded-md border border-neutral-300 bg-transparent px-3 py-2 text-sm outline-none focus:border-neutral-900 dark:border-neutral-700 dark:focus:border-neutral-100";

export default function SellerApplyPage() {
  const [form, setForm] = useState({
    brandName: "",
    businessType: "INDIVIDUAL" as "INDIVIDUAL" | "BUSINESS",
    businessNo: "",
    representativeName: "",
    settlementBank: "",
    settlementAccount: "",
    agreed: false,
  });
  const [error, setError] = useState<string | null>(null);

  const apply = useMutation({
    mutationFn: (body: SellerApplicationBody) => sellerApi.apply(body),
    onError: (e) => setError(e instanceof ApiError ? e.message : "신청 중 오류가 발생했습니다."),
  });

  function set<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  if (apply.isSuccess) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 px-4 py-16 text-center">
        <p className="text-lg font-medium">판매자 신청이 접수되었습니다.</p>
        <p className="text-sm text-neutral-500">관리자 승인 후 도안을 등록할 수 있습니다.</p>
        <Link href="/" className="text-sm underline">홈으로</Link>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-lg flex-1 px-4 py-8">
      <h1 className="text-2xl font-semibold tracking-tight">판매자 신청</h1>
      <p className="mt-1 text-sm text-neutral-500">사업자 정보와 정산 정보를 입력해 주세요.</p>

      <form
        className="mt-6 space-y-4"
        onSubmit={(e) => {
          e.preventDefault();
          setError(null);
          if (!form.brandName.trim()) return setError("브랜드명을 입력하세요.");
          if (!form.agreed) return setError("판매 약관에 동의해야 합니다.");
          apply.mutate({
            brandName: form.brandName.trim(),
            businessType: form.businessType,
            businessNo: form.businessNo.trim() || undefined,
            representativeName: form.representativeName.trim() || undefined,
            settlementBank: form.settlementBank.trim() || undefined,
            settlementAccount: form.settlementAccount.trim() || undefined,
            termsVersion: "1.0",
          });
        }}
        noValidate
      >
        <Field label="브랜드명 (필수)">
          <input value={form.brandName} onChange={(e) => set("brandName", e.target.value)} className={inputClass} />
        </Field>
        <Field label="사업자 유형">
          <select value={form.businessType} onChange={(e) => set("businessType", e.target.value as "INDIVIDUAL" | "BUSINESS")} className={inputClass}>
            <option value="INDIVIDUAL">개인</option>
            <option value="BUSINESS">사업자</option>
          </select>
        </Field>
        <Field label="사업자등록번호">
          <input value={form.businessNo} onChange={(e) => set("businessNo", e.target.value)} className={inputClass} />
        </Field>
        <Field label="대표자명">
          <input value={form.representativeName} onChange={(e) => set("representativeName", e.target.value)} className={inputClass} />
        </Field>
        <Field label="정산 은행">
          <input value={form.settlementBank} onChange={(e) => set("settlementBank", e.target.value)} className={inputClass} />
        </Field>
        <Field label="정산 계좌 (암호화 저장)">
          <input value={form.settlementAccount} onChange={(e) => set("settlementAccount", e.target.value)} className={inputClass} />
        </Field>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.agreed} onChange={(e) => set("agreed", e.target.checked)} className="h-4 w-4" />
          (필수) 판매 약관에 동의합니다.
        </label>

        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}

        <button type="submit" disabled={apply.isPending}
          className="w-full rounded-md bg-neutral-900 px-3 py-2.5 text-sm font-medium text-white disabled:opacity-50 dark:bg-neutral-100 dark:text-neutral-900">
          {apply.isPending ? "신청 중…" : "판매자 신청"}
        </button>
      </form>
    </main>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs text-neutral-500">{label}</span>
      {children}
    </label>
  );
}
