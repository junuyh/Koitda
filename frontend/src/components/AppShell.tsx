"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { authApi } from "@/features/auth/api";

type NavItem = { href: string; label: string; icon: React.ReactNode; role?: "SELLER" | "ADMIN" };

function Icon({ d }: { d: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className="h-[18px] w-[18px] shrink-0">
      <path d={d} />
    </svg>
  );
}

const NAV: NavItem[] = [
  { href: "/patterns", label: "도안", icon: <Icon d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5z" /> },
  { href: "/explore", label: "둘러보기", icon: <Icon d="M21 21l-4.35-4.35M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z" /> },
  { href: "/projects", label: "내 니팅로그", icon: <Icon d="M12 20h9M3 20h3M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z" /> },
  { href: "/coitgi", label: "코잇기", icon: <Icon d="M4 7h16M4 12h16M4 17h10M2 7v10M22 7v10" /> },
  { href: "/library", label: "구매 도안", icon: <Icon d="M21 8v13H3V8M1 3h22v5H1zM10 12h4" /> },
  { href: "/seller/patterns", label: "판매", role: "SELLER", icon: <Icon d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.3 2.3a1 1 0 0 0 .7 1.7H17M9 20a1 1 0 1 0 0 2 1 1 0 0 0 0-2zm7 0a1 1 0 1 0 0 2 1 1 0 0 0 0-2z" /> },
  { href: "/admin", label: "심사", role: "ADMIN", icon: <Icon d="M9 12l2 2 4-4M12 3l7 4v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V7z" /> },
];

export default function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const queryClient = useQueryClient();
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: authApi.me });
  const logout = useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => queryClient.setQueryData(["me"], null),
  });

  const roles: string[] = me?.roles ?? [];
  const items = NAV.filter((n) => !n.role || roles.includes(n.role));
  const isActive = (href: string) => pathname === href || pathname.startsWith(href + "/");

  const authPage = pathname === "/login" || pathname === "/signup";

  const navLinks = (
    <>
      {items.map((n) => (
        <Link key={n.href} href={n.href}
          className={`flex items-center gap-3 rounded-full border-2 px-3 py-2 text-sm font-bold transition ${
            isActive(n.href)
              ? "border-neutral-900 bg-neutral-900 text-white dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900"
              : "border-transparent text-neutral-600 hover:border-neutral-900 dark:text-neutral-300 dark:hover:border-neutral-100"
          }`}>
          {n.icon}
          <span>{n.label}</span>
        </Link>
      ))}
    </>
  );

  return (
    <div className="flex min-h-full w-full">
      {/* 좌측 사이드바 (데스크톱) */}
      {!authPage && (
        <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r-2 border-neutral-900 bg-white px-3 py-5 md:flex dark:border-neutral-100 dark:bg-neutral-950">
          <Link href="/" className="mb-6 flex items-center gap-2 px-2 text-2xl font-black tracking-tight">
            <span className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-neutral-900 bg-amber-300 text-base text-neutral-900 dark:border-neutral-100">코</span>
            코잇다
          </Link>
          <nav className="flex flex-1 flex-col gap-1.5">{navLinks}</nav>
          {!me && (
            <Link href="/login" className="mt-2 rounded-full border-2 border-neutral-900 bg-neutral-900 px-3 py-2 text-center text-sm font-bold text-white dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900">
              로그인
            </Link>
          )}
        </aside>
      )}

      {/* 메인 영역 */}
      <div className="flex min-w-0 flex-1 flex-col bg-neutral-50 dark:bg-neutral-900/40">
        {/* 상단바 */}
        <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b-2 border-neutral-900 bg-white/85 px-4 backdrop-blur dark:border-neutral-100 dark:bg-neutral-950/85">
          {/* 모바일 로고 */}
          <Link href="/" className="text-lg font-black tracking-tight md:hidden">코잇다</Link>

          <div className="ml-auto flex items-center gap-2">
            {me ? (
              <>
                <button type="button" aria-label="알림"
                  className="hidden rounded-full p-2 text-neutral-500 hover:bg-neutral-100 sm:block dark:hover:bg-neutral-800">
                  <Icon d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0" />
                </button>
                <Link href="/me" className="flex items-center gap-2 rounded-full py-1 pl-1 pr-3 hover:bg-neutral-100 dark:hover:bg-neutral-800">
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-neutral-900 text-xs font-semibold text-white dark:bg-neutral-100 dark:text-neutral-900">
                    {me.nickname.slice(0, 1)}
                  </span>
                  <span className="hidden text-sm text-neutral-700 sm:inline dark:text-neutral-200">{me.nickname}</span>
                </Link>
                <button type="button" onClick={() => logout.mutate()} disabled={logout.isPending}
                  className="rounded-full border-2 border-neutral-900 px-3 py-1.5 text-sm font-bold hover:bg-neutral-100 disabled:opacity-50 dark:border-neutral-100 dark:hover:bg-neutral-800">
                  로그아웃
                </button>
              </>
            ) : !authPage ? (
              <>
                <Link href="/login" className="rounded-full px-3 py-1.5 text-sm font-bold text-neutral-600 hover:text-neutral-900 dark:text-neutral-300">로그인</Link>
                <Link href="/signup" className="rounded-full border-2 border-neutral-900 bg-neutral-900 px-3 py-1.5 text-sm font-bold text-white dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900">회원가입</Link>
              </>
            ) : null}
          </div>
        </header>

        {/* 모바일 가로 네비 */}
        {!authPage && (
          <nav className="flex gap-1.5 overflow-x-auto border-b-2 border-neutral-900 bg-white px-3 py-2 md:hidden dark:border-neutral-100 dark:bg-neutral-950">
            {items.map((n) => (
              <Link key={n.href} href={n.href}
                className={`shrink-0 rounded-full border-2 px-3 py-1.5 text-sm font-bold ${
                  isActive(n.href)
                    ? "border-neutral-900 bg-neutral-900 text-white dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900"
                    : "border-transparent text-neutral-500 hover:border-neutral-900 dark:hover:border-neutral-100"
                }`}>
                {n.label}
              </Link>
            ))}
          </nav>
        )}

        {children}
      </div>
    </div>
  );
}
