"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { authApi } from "@/features/auth/api";
import { pointApi, type PointTx } from "@/features/review/api";

const TX_LABEL: Record<string, string> = {
  EARN: "적립", USE: "사용", REVOKE: "회수", RETURN: "반환", EXPIRE: "소멸",
};
const REASON_LABEL: Record<string, string> = {
  REVIEW_CREATED: "리뷰 작성", REVIEW_DELETED: "리뷰 삭제", PATTERN_PURCHASE: "도안 구매", ORDER_CANCELED: "주문 취소",
};

export default function MyPage() {
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: authApi.me });
  const { data: history, isLoading, isError } = useQuery({
    queryKey: ["point-history"],
    queryFn: pointApi.history,
    retry: false,
  });

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <h1 className="text-2xl font-semibold tracking-tight">마이페이지</h1>
      {me && <p className="mt-1 text-sm text-neutral-500">{me.nickname} · {me.email}</p>}

      <section className="mt-6 rounded-lg border border-neutral-200 p-5 dark:border-neutral-800">
        <p className="text-xs text-neutral-500">보유 포인트</p>
        <p className="mt-1 text-3xl font-semibold">{(history?.balance ?? me?.pointBalance ?? 0).toLocaleString()}<span className="ml-1 text-lg text-neutral-400">P</span></p>
      </section>

      <section className="mt-6">
        <h2 className="mb-2 text-sm font-semibold">포인트 내역</h2>
        {isLoading ? (
          <p className="py-8 text-center text-sm text-neutral-400">불러오는 중…</p>
        ) : isError ? (
          <p className="py-8 text-center text-sm text-neutral-400">로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link></p>
        ) : !history || history.transactions.length === 0 ? (
          <p className="py-8 text-center text-sm text-neutral-500">아직 포인트 내역이 없어요. 구매한 도안에 리뷰를 남기면 적립됩니다.</p>
        ) : (
          <ul className="divide-y divide-neutral-100 dark:divide-neutral-900">
            {history.transactions.map((t) => <TxRow key={t.id} t={t} />)}
          </ul>
        )}
      </section>
    </main>
  );
}

function TxRow({ t }: { t: PointTx }) {
  const positive = t.amount > 0;
  const failed = t.amount === 0 && t.failReason;
  return (
    <li className="flex items-center justify-between gap-3 py-3">
      <div className="min-w-0">
        <p className="text-sm font-medium">
          {REASON_LABEL[t.reasonCode ?? ""] ?? TX_LABEL[t.txType] ?? t.txType}
          {failed && <span className="ml-1 text-xs text-neutral-400">(회수 실패)</span>}
        </p>
        <p className="mt-0.5 text-xs text-neutral-400">
          {new Date(t.createdAt).toLocaleString("ko-KR")}
          {t.failReason && ` · ${t.failReason}`}
        </p>
      </div>
      <div className="shrink-0 text-right">
        <p className={`text-sm font-semibold ${positive ? "text-green-600 dark:text-green-400" : failed ? "text-neutral-400" : "text-red-600 dark:text-red-400"}`}>
          {positive ? "+" : ""}{t.amount.toLocaleString()}P
        </p>
        <p className="text-xs text-neutral-400">잔액 {t.balanceAfter.toLocaleString()}P</p>
      </div>
    </li>
  );
}
