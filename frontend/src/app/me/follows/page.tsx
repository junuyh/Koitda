"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { socialApi, type FollowUser } from "@/features/social/api";

export default function FollowsPage() {
  const [tab, setTab] = useState<"following" | "followers">("following");
  const { data: following, isError: e1 } = useQuery({ queryKey: ["following"], queryFn: socialApi.following, retry: false });
  const { data: followers, isError: e2 } = useQuery({ queryKey: ["followers"], queryFn: socialApi.followers, retry: false });

  const list = tab === "following" ? following : followers;
  const isError = tab === "following" ? e1 : e2;

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <Link href="/me" className="text-sm font-bold text-neutral-500 hover:underline">← 마이페이지</Link>
      <h1 className="mt-2 text-3xl font-black tracking-tight">팔로우</h1>

      <div className="mt-4 flex rounded-full border-2 border-neutral-900 p-0.5 text-sm font-bold dark:border-neutral-100">
        {(["following", "followers"] as const).map((t) => (
          <button key={t} type="button" onClick={() => setTab(t)}
            className={`flex-1 rounded-full px-4 py-1.5 ${tab === t ? "bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-900" : "text-neutral-500"}`}>
            {t === "following" ? `팔로잉 ${following?.length ?? 0}` : `팔로워 ${followers?.length ?? 0}`}
          </button>
        ))}
      </div>

      {isError ? (
        <p className="py-16 text-center text-sm text-neutral-400">로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link></p>
      ) : !list || list.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-500">
          {tab === "following" ? "아직 팔로우한 사람이 없어요." : "아직 나를 팔로우한 사람이 없어요."}
        </p>
      ) : (
        <ul className="mt-5 space-y-2">
          {list.map((u) => <li key={u.userId}><FollowRow u={u} showUnfollow={tab === "following"} /></li>)}
        </ul>
      )}
    </main>
  );
}

function FollowRow({ u, showUnfollow }: { u: FollowUser; showUnfollow: boolean }) {
  const queryClient = useQueryClient();
  const [unfollowed, setUnfollowed] = useState(false);
  const unfollow = useMutation({
    mutationFn: () => socialApi.unfollow(u.userId),
    onSuccess: () => { setUnfollowed(true); queryClient.invalidateQueries({ queryKey: ["following"] }); },
  });
  const follow = useMutation({
    mutationFn: () => socialApi.follow(u.userId),
    onSuccess: () => { setUnfollowed(false); queryClient.invalidateQueries({ queryKey: ["following"] }); },
  });

  return (
    <div className="flex items-center justify-between gap-3 rounded-2xl border-2 border-neutral-900 p-3 dark:border-neutral-100">
      <div className="flex min-w-0 items-center gap-3">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 border-neutral-900 bg-violet-200 text-sm font-black text-neutral-900 dark:border-neutral-100">
          {u.nickname.slice(0, 1)}
        </span>
        <span className="line-clamp-1 text-sm font-bold">{u.nickname}</span>
      </div>
      {showUnfollow && (
        unfollowed ? (
          <button type="button" onClick={() => follow.mutate()} disabled={follow.isPending}
            className="shrink-0 rounded-full border-2 border-neutral-900 bg-neutral-900 px-3 py-1 text-xs font-bold text-white disabled:opacity-50 dark:border-neutral-100 dark:bg-neutral-100 dark:text-neutral-900">
            팔로우
          </button>
        ) : (
          <button type="button" onClick={() => unfollow.mutate()} disabled={unfollow.isPending}
            className="shrink-0 rounded-full border-2 border-neutral-900 px-3 py-1 text-xs font-bold disabled:opacity-50 dark:border-neutral-100">
            언팔로우
          </button>
        )
      )}
    </div>
  );
}
