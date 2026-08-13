"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { orderApi, type OrderListItem } from "@/features/order/api";

const STATUS_LABEL: Record<string, string> = {
  CREATED: "입금 대기", PAID: "주문 완료", COMPLETED: "주문 완료", CANCELED: "취소", REFUNDED: "환불 완료",
};

export default function LibraryPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["my-orders"], queryFn: orderApi.myOrders, retry: false });
  const orders = data ?? [];

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">
      <div className="mb-6 flex items-end justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-neutral-400">Purchases</p>
          <h1 className="mt-1 text-4xl font-black tracking-tight">구매 내역</h1>
          <p className="mt-1 text-sm text-neutral-500">전체 {orders.length}</p>
        </div>
        <Link href="/wishlist" className="text-sm font-bold text-neutral-500 hover:underline">위시리스트</Link>
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-neutral-400">불러오는 중…</p>
      ) : isError ? (
        <p className="py-16 text-center text-sm text-neutral-500">로그인이 필요합니다. <Link href="/login" className="underline">로그인</Link></p>
      ) : orders.length === 0 ? (
        <p className="py-16 text-center text-sm text-neutral-400">구매한 도안이 없습니다. <Link href="/patterns" className="underline">도안 둘러보기</Link></p>
      ) : (
        <ul className="space-y-4">
          {orders.map((o) => <li key={o.id}><OrderCard o={o} /></li>)}
        </ul>
      )}
    </main>
  );
}

function OrderCard({ o }: { o: OrderListItem }) {
  const date = o.orderedAt.slice(0, 10).replace(/-/g, ".");
  const time = new Date(o.orderedAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
  const first = o.items[0];
  return (
    <div className="overflow-hidden rounded-2xl border-2 border-neutral-900 bg-white dark:border-neutral-100 dark:bg-neutral-950">
      <div className="flex items-center justify-between border-b-2 border-dashed border-neutral-200 px-5 py-3 dark:border-neutral-800">
        <p className="text-sm font-black">{date} 주문</p>
        <Link href={`/orders/${o.id}`} className="text-sm font-bold text-orange-500 hover:underline">주문 상세보기 ›</Link>
      </div>
      <div className="p-5">
        <p className="text-sm font-bold">{STATUS_LABEL[o.status] ?? o.status} · {date} {time}</p>
        {o.items.map((it) => (
          <Link key={it.patternId} href={`/patterns/${it.patternId}`} className="mt-3 block">
            <p className="text-lg font-black">{it.patternTitle}</p>
            <p className="mt-0.5 text-sm text-neutral-500">수량 1개</p>
            <p className="mt-2 text-sm font-black">{it.amount.toLocaleString()}원</p>
          </Link>
        ))}
        {first && (
          <div className="mt-4 grid grid-cols-3 gap-2">
            <button type="button" disabled title="곧 제공"
              className="rounded-full border-2 border-neutral-300 py-2 text-sm font-bold text-neutral-400 dark:border-neutral-700">문의하기</button>
            <Link href={`/patterns/${first.patternId}/reviews/new`}
              className="rounded-full border-2 border-neutral-900 py-2 text-center text-sm font-bold dark:border-neutral-100">후기 작성</Link>
            <Link href={`/library/${first.patternId}`}
              className="rounded-full border-2 border-orange-500 py-2 text-center text-sm font-bold text-orange-500 transition hover:bg-orange-50 dark:hover:bg-orange-950/30">다운로드</Link>
          </div>
        )}
      </div>
    </div>
  );
}
