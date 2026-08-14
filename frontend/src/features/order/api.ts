import { apiFetch, ApiError } from "@/lib/api";

export type Purchasability = { canPurchase: boolean; reason: string | null; usablePoint: number };
export type Order = { id: number; orderNo: string; totalAmount: number; paymentAmount: number; status: string };
export type LibraryItem = {
  patternId: number;
  patternTitle: string;
  categoryName: string | null;
  purchasedAt: string;
  revoked: boolean;
};

export type OrderItemLine = { patternId: number; patternTitle: string; amount: number };
export type OrderListItem = {
  id: number; orderNo: string; totalAmount: number; paymentAmount: number;
  status: string; orderedAt: string; items: OrderItemLine[];
};
export type OrderDetail = {
  id: number; orderNo: string; status: string; orderedAt: string; items: OrderItemLine[];
  buyerName: string; buyerEmail: string; productAmount: number; discountAmount: number; paymentAmount: number;
};

export const orderApi = {
  myOrders: () => apiFetch<OrderListItem[]>("/users/me/orders"),
  orderDetail: (orderId: number) => apiFetch<OrderDetail>(`/orders/${orderId}`),
  purchasability: (patternId: number) =>
    apiFetch<Purchasability>(`/patterns/${patternId}/purchasability`),
  createOrder: (patternId: number) =>
    apiFetch<Order>("/orders", { method: "POST", body: { patternId, agreed: true } }),
  completePayment: (orderId: number) =>
    apiFetch<{ orderStatus: string; patternId: number }>(`/orders/${orderId}/payments/complete`, {
      method: "POST",
    }),
  myLibrary: () => apiFetch<LibraryItem[]>("/users/me/pattern-library"),
  libraryDetail: (patternId: number) =>
    apiFetch<LibraryDetail>(`/users/me/pattern-library/${patternId}`),
  // PDF 다운로드: POST 로 바이트를 받아 브라우저 다운로드를 트리거한다. fileId 지정 시 해당 PDF, 미지정 시 대표.
  downloadPdf: async (patternId: number, fallbackName: string, fileId?: number) => {
    const csrf = getCookie("XSRF-TOKEN");
    const qs = fileId != null ? `?fileId=${fileId}` : "";
    const res = await fetch(`/api/v1/pattern-library/${patternId}/download${qs}`, {
      method: "POST",
      credentials: "include",
      headers: csrf ? { "X-XSRF-TOKEN": decodeURIComponent(csrf) } : {},
    });
    if (!res.ok) {
      let msg = "다운로드에 실패했습니다.";
      try { const j = await res.json(); msg = j?.message ?? msg; } catch {}
      throw new ApiError(res.status, "DOWNLOAD_FAILED", msg);
    }
    const blob = await res.blob();
    const name = filenameFromDisposition(res.headers.get("Content-Disposition")) ?? fallbackName;
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = name; document.body.appendChild(a); a.click();
    a.remove(); URL.revokeObjectURL(url);
    return { remaining: Number(res.headers.get("X-Download-Remaining") ?? "0") };
  },
};

export type LibraryDetail = {
  patternId: number;
  patternTitle: string;
  sellerBrand: string | null;
  purchasedAt: string;
  revoked: boolean;
  hasPdf: boolean;
  downloadCount: number;
  downloadLimit: number;
  pdfFileIds: number[] | null;
};

function getCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const m = document.cookie.match(new RegExp("(?:^|; )" + name + "=([^;]*)"));
  return m ? m[1] : null;
}

function filenameFromDisposition(v: string | null): string | null {
  if (!v) return null;
  const star = v.match(/filename\*=UTF-8''([^;]+)/i);
  if (star) return decodeURIComponent(star[1]);
  const plain = v.match(/filename="?([^";]+)"?/i);
  return plain ? plain[1] : null;
}
