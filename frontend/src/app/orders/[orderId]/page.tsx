"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useParams } from "next/navigation";
import { orderApi } from "@/features/order/api";

const STATUS_LABEL: Record<string, string> = {
  CREATED: "입금 대기", PAID: "주문 완료", COMPLETED: "주문 완료", CANCELED: "취소", REFUNDED: "환불 완료",
};

export default function OrderDetailPage() {
  const params = useParams<{ orderId: string }>();
  const orderId = Number(params.orderId);
  const { data: o, isLoading, isError } = useQuery({
    queryKey: ["order", orderId], queryFn: () => orderApi.orderDetail(orderId), retry: false,
  });

  if (isLoading) return <Centered>불러오는 중…</Centered>;
  if (isError || !o) return <Centered>주문을 찾을 수 없습니다.</Centered>;

  const date = o.orderedAt.slice(0, 10).replace(/-/g, ".");
  const time = new Date(o.orderedAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
  const first = o.items[0];

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8">
      <Link href="/library" className="text-sm font-bold text-neutral-500 hover:underline">← 구매 내역</Link>
      <h1 className="mt-2 text-3xl font-black tracking-tight">주문 상세</h1>
      <p className="mt-1 text-sm text-neutral-500">상품 {o.items.length}개 · 주문 {date} · 주문 번호 <span className="font-bold text-neutral-700 dark:text-neutral-200">{o.orderNo}</span></p>

      {/* 주문 상품 */}
      <section className="mt-5 rounded-2xl border-2 border-neutral-900 bg-white p-5 dark:border-neutral-100 dark:bg-neutral-950">
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
              className="rounded-full border-2 border-neutral-900 py-2 text-center text-sm font-bold dark:border-neutral-100">리뷰 작성</Link>
            <Link href={`/library/${first.patternId}`}
              className="rounded-full border-2 border-neutral-900 bg-emerald-300 py-2 text-center text-sm font-bold text-emerald-950 transition hover:shadow-[3px_3px_0_0_rgba(0,0,0,0.9)] dark:border-neutral-100">다운로드</Link>
          </div>
        )}
      </section>

      {/* 주문자 정보 (조회 전용 — 수정은 마이페이지) */}
      <section className="mt-4">
        <h2 className="mb-2 text-lg font-black tracking-tight">주문자 정보</h2>
        <dl className="rounded-2xl border-2 border-neutral-900 p-5 text-sm dark:border-neutral-100">
          <Row label="이름">{o.buyerName}</Row>
          <Row label="이메일">{o.buyerEmail}</Row>
        </dl>
      </section>

      {/* 결제 정보 */}
      <section className="mt-4">
        <h2 className="mb-2 text-lg font-black tracking-tight">결제 정보</h2>
        <dl className="rounded-2xl border-2 border-neutral-900 p-5 text-sm dark:border-neutral-100">
          <PayRow label="상품 금액">{o.productAmount.toLocaleString()}원</PayRow>
          <PayRow label="할인 금액">{o.discountAmount > 0 ? "-" : ""}{o.discountAmount.toLocaleString()}원</PayRow>
          <div className="mt-2 border-t-2 border-dashed border-neutral-200 pt-2 dark:border-neutral-800">
            <PayRow label="총 결제 금액" bold>{o.paymentAmount.toLocaleString()}원</PayRow>
          </div>
        </dl>
      </section>
    </main>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-4 py-1">
      <dt className="w-24 shrink-0 text-neutral-400">{label}</dt>
      <dd className="min-w-0 font-medium text-neutral-800 dark:text-neutral-200">{children}</dd>
    </div>
  );
}

function PayRow({ label, children, bold }: { label: string; children: React.ReactNode; bold?: boolean }) {
  return (
    <div className="flex items-center justify-between py-1">
      <span className={bold ? "font-black" : "text-neutral-500"}>{label}</span>
      <span className={bold ? "text-lg font-black" : "font-medium"}>{children}</span>
    </div>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex flex-1 items-center justify-center py-16 text-sm text-neutral-400">{children}</main>;
}
