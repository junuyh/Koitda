import Link from "next/link";

/** 전역 푸터 — 회사 정보 + 법적 링크(이용약관·개인정보 처리방침). '개인정보 처리방침'은 굵게 강조(관례). */
export default function Footer() {
  return (
    <footer className="mt-auto border-t-2 border-neutral-900 bg-white px-4 py-6 text-sm dark:border-neutral-100 dark:bg-neutral-950">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-3">
        <nav className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <Link href="/terms" className="text-neutral-600 hover:underline dark:text-neutral-300">이용약관</Link>
          <span className="text-neutral-300 dark:text-neutral-700">·</span>
          <Link href="/privacy" className="font-bold text-neutral-900 hover:underline dark:text-neutral-100">개인정보 처리방침</Link>
        </nav>
        <div className="text-xs leading-relaxed text-neutral-400">
          <p className="font-bold text-neutral-500 dark:text-neutral-400">코잇다 (Koitda)</p>
          <p>도안 탐색부터 제작 기록·공유까지, 뜨개 통합 플랫폼</p>
          <p className="mt-1">© 2026 Koitda. 포트폴리오 데모 서비스입니다.</p>
        </div>
      </div>
    </footer>
  );
}
