"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { authApi } from "@/features/auth/api";
import { meApi } from "@/features/me/api";

export default function SettingsPage() {
  const queryClient = useQueryClient();
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: authApi.me });
  const [nickname, setNickname] = useState("");
  const [msg, setMsg] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => { if (me?.nickname) setNickname(me.nickname); }, [me?.nickname]);

  const save = useMutation({
    mutationFn: () => meApi.updateProfile(nickname.trim()),
    onSuccess: async () => {
      setMsg("닉네임을 변경했습니다.");
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ["me"] });
    },
    onError: (e) => { setError(e instanceof ApiError ? e.message : "변경 중 오류가 발생했습니다."); setMsg(null); },
  });

  return (
    <main className="mx-auto w-full max-w-lg flex-1 px-4 py-8">
      <Link href="/me" className="text-sm font-bold text-neutral-500 hover:underline">← 마이페이지</Link>
      <h1 className="mt-2 text-3xl font-black tracking-tight">정보 수정</h1>

      <section className="mt-5 rounded-3xl border-2 border-neutral-900 bg-white p-5 dark:border-neutral-100 dark:bg-neutral-950">
        <form onSubmit={(e) => { e.preventDefault(); if (nickname.trim()) save.mutate(); }} className="space-y-4">
          <label className="block">
            <span className="mb-1 block text-xs font-bold text-neutral-500">이메일</span>
            <input value={me?.email ?? ""} disabled
              className="w-full rounded-xl border-2 border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-400 dark:border-neutral-800 dark:bg-neutral-900" />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-bold text-neutral-500">닉네임</span>
            <input value={nickname} onChange={(e) => setNickname(e.target.value)} maxLength={30}
              className="w-full rounded-xl border-2 border-neutral-900 bg-transparent px-3 py-2 text-sm font-bold outline-none dark:border-neutral-100" />
          </label>

          {msg && <p className="text-sm text-green-600 dark:text-green-400">{msg}</p>}
          {error && <p role="alert" className="text-sm text-red-600">{error}</p>}

          <button type="submit" disabled={save.isPending || !nickname.trim() || nickname.trim() === me?.nickname}
            className="w-full rounded-full border-2 border-neutral-900 bg-neutral-900 px-4 py-2.5 text-sm font-bold text-white transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] disabled:opacity-50 disabled:shadow-none dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900">
            {save.isPending ? "저장 중…" : "저장"}
          </button>
        </form>
      </section>

      <p className="mt-4 text-xs text-neutral-400">비밀번호 변경은 곧 제공됩니다.</p>
    </main>
  );
}
