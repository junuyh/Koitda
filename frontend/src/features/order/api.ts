import { apiFetch } from "@/lib/api";

export type Purchasability = { canPurchase: boolean; reason: string | null; usablePoint: number };
export type Order = { id: number; orderNo: string; totalAmount: number; paymentAmount: number; status: string };
export type LibraryItem = {
  patternId: number;
  patternTitle: string;
  categoryName: string | null;
  purchasedAt: string;
  revoked: boolean;
};

export const orderApi = {
  purchasability: (patternId: number) =>
    apiFetch<Purchasability>(`/patterns/${patternId}/purchasability`),
  createOrder: (patternId: number) =>
    apiFetch<Order>("/orders", { method: "POST", body: { patternId, agreed: true } }),
  completePayment: (orderId: number) =>
    apiFetch<{ orderStatus: string; patternId: number }>(`/orders/${orderId}/payments/complete`, {
      method: "POST",
    }),
  myLibrary: () => apiFetch<LibraryItem[]>("/users/me/pattern-library"),
};
