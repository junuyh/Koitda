import Link from "next/link";
import { PRIVACY_POLICY } from "@/features/auth/legalText";

export const metadata = { title: "개인정보 처리방침 · 코잇다" };

export default function PrivacyPage() {
  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-10">
      <Link href="/" className="text-sm text-neutral-500 hover:underline">← 홈</Link>
      <h1 className="mt-2 text-2xl font-black tracking-tight">개인정보 처리방침</h1>
      <p className="mt-1 text-xs text-neutral-400">시행일 2026-08-14 · 데모용 표준 문구</p>
      <pre className="mt-6 whitespace-pre-wrap rounded-2xl border-2 border-neutral-900 p-5 text-sm leading-relaxed text-neutral-700 dark:border-neutral-100 dark:text-neutral-300">
{PRIVACY_POLICY}
      </pre>
    </main>
  );
}
