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
            <input value={me?.email ?? "카카오 로그인 (이메일 없음)"} disabled
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

      {/* 소셜 전용 회원(카카오)은 비밀번호가 없어 변경 UI 를 감춘다. */}
      {me?.hasPassword && <PasswordSection email={me?.email ?? ""} />}
    </main>
  );
}

/** 비밀번호 변경 — 이메일 인증번호 확인 후 적용(데모: 코드가 화면에 표시됨). */
function PasswordSection({ email }: { email: string }) {
  const [code, setCode] = useState("");
  const [demoCode, setDemoCode] = useState<string | null>(null);
  const [newPw, setNewPw] = useState("");
  const [msg, setMsg] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const issue = useMutation({
    mutationFn: () => meApi.issuePasswordCode(),
    onSuccess: (r) => { setDemoCode(r.demoCode); setMsg(null); setError(null); },
    onError: (e) => setError(e instanceof ApiError ? e.message : "인증번호 발급에 실패했습니다."),
  });
  const change = useMutation({
    mutationFn: () => meApi.changePassword(code.trim(), newPw),
    onSuccess: () => { setMsg("비밀번호를 변경했습니다."); setError(null); setCode(""); setNewPw(""); setDemoCode(null); },
    onError: (e) => { setError(e instanceof ApiError ? e.message : "변경 중 오류가 발생했습니다."); setMsg(null); },
  });

  return (
    <section className="mt-6 rounded-3xl border-2 border-neutral-900 bg-white p-5 dark:border-neutral-100 dark:bg-neutral-950">
      <h2 className="text-sm font-black">비밀번호 변경</h2>
      <p className="mt-1 text-xs text-neutral-400">{email}로 인증번호를 보내 확인 후 변경합니다.</p>

      <div className="mt-4 space-y-3">
        <div className="flex gap-2">
          <input value={code} onChange={(e) => setCode(e.target.value)} inputMode="numeric" maxLength={6}
            placeholder="인증번호 6자리" aria-label="인증번호"
            className="w-full rounded-xl border-2 border-neutral-900 bg-transparent px-3 py-2 text-sm outline-none dark:border-neutral-100" />
          <button type="button" onClick={() => issue.mutate()} disabled={issue.isPending}
            className="shrink-0 rounded-full border-2 border-neutral-900 px-4 py-2 text-sm font-bold disabled:opacity-50 dark:border-neutral-100">
            {issue.isPending ? "발급 중…" : "인증번호 받기"}
          </button>
        </div>
        {demoCode && (
          <p className="rounded-xl bg-amber-100 px-3 py-2 text-xs font-bold text-amber-800 dark:bg-amber-950/40 dark:text-amber-300">
            데모 인증번호: <span className="font-mono">{demoCode}</span> (운영에서는 이메일로 전송됩니다)
          </p>
        )}
        <input type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)} minLength={8} maxLength={72}
          placeholder="새 비밀번호 (8자 이상)" aria-label="새 비밀번호"
          className="w-full rounded-xl border-2 border-neutral-900 bg-transparent px-3 py-2 text-sm outline-none dark:border-neutral-100" />

        {msg && <p className="text-sm text-green-600 dark:text-green-400">{msg}</p>}
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}

        <button type="button" onClick={() => change.mutate()}
          disabled={change.isPending || !code.trim() || newPw.length < 8}
          className="w-full rounded-full border-2 border-neutral-900 bg-neutral-900 px-4 py-2.5 text-sm font-bold text-white transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] disabled:opacity-50 disabled:shadow-none dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900">
          {change.isPending ? "변경 중…" : "비밀번호 변경"}
        </button>
      </div>
    </section>
  );
}
