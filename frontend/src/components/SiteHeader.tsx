"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { authApi } from "@/features/auth/api";

type NavItem = { href: string; label: string; role?: "SELLER" | "ADMIN" };

const NAV: NavItem[] = [
  { href: "/patterns", label: "도안" },
  { href: "/projects", label: "니팅로그" },
  { href: "/library", label: "구매 도안" },
  { href: "/seller/patterns", label: "판매", role: "SELLER" },
  { href: "/admin", label: "심사", role: "ADMIN" },
];

export default function SiteHeader() {
  const pathname = usePathname();
  const queryClient = useQueryClient();

  // me 는 401 이면 null(비로그인). Providers 에서 retry:false 전역 설정.
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: authApi.me });

  const logout = useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => queryClient.setQueryData(["me"], null),
  });

  // 인증 화면에서는 로고만 노출해 흐름을 방해하지 않는다.
  const minimal = pathname === "/login" || pathname === "/signup";

  const roles: string[] = me?.roles ?? [];
  const items = NAV.filter((n) => !n.role || roles.includes(n.role));

  function isActive(href: string) {
    return pathname === href || pathname.startsWith(href + "/");
  }

  return (
    <header className="sticky top-0 z-40 border-b border-neutral-200 bg-white/80 backdrop-blur dark:border-neutral-800 dark:bg-neutral-950/80">
      <div className="mx-auto flex h-14 w-full max-w-5xl items-center gap-1 px-4">
        <Link href="/" className="mr-2 shrink-0 text-lg font-semibold tracking-tight">
          코잇다
        </Link>

        {!minimal && (
          <nav className="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto">
            {items.map((n) => (
              <Link
                key={n.href}
                href={n.href}
                className={`shrink-0 rounded-md px-2.5 py-1.5 text-sm transition ${
                  isActive(n.href)
                    ? "bg-neutral-100 font-medium text-neutral-900 dark:bg-neutral-800 dark:text-neutral-100"
                    : "text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-100"
                }`}
              >
                {n.label}
              </Link>
            ))}
          </nav>
        )}

        <div className="ml-auto flex shrink-0 items-center gap-2">
          {me ? (
            <>
              <Link href="/me" className="hidden text-sm text-neutral-600 hover:text-neutral-900 sm:inline dark:text-neutral-300 dark:hover:text-neutral-100">
                {me.nickname}
              </Link>
              <button
                type="button"
                onClick={() => logout.mutate()}
                disabled={logout.isPending}
                className="rounded-md border border-neutral-300 px-3 py-1.5 text-sm hover:bg-neutral-100 disabled:opacity-50 dark:border-neutral-700 dark:hover:bg-neutral-900"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className="rounded-md px-3 py-1.5 text-sm text-neutral-600 hover:text-neutral-900 dark:text-neutral-300 dark:hover:text-neutral-100">
                로그인
              </Link>
              <Link href="/signup" className="rounded-md bg-neutral-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-neutral-700 dark:bg-neutral-100 dark:text-neutral-900 dark:hover:bg-neutral-300">
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
