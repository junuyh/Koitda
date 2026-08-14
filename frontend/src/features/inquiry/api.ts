import { apiFetch } from "@/lib/api";

export type InquiryItem = {
  id: number;
  askerNickname: string;
  isPrivate: boolean;
  locked: boolean;
  content: string | null;
  answer: string | null;
  answered: boolean;
  mine: boolean;
  canAnswer: boolean;
  createdAt: string;
  answeredAt: string | null;
};

export type InquiryList = {
  items: InquiryItem[];
  canAsk: boolean;
  isSeller: boolean;
};

export type SellerInquiryItem = {
  id: number;
  patternId: number;
  patternTitle: string | null;
  askerNickname: string;
  isPrivate: boolean;
  content: string;
  answer: string | null;
  answered: boolean;
  createdAt: string;
  answeredAt: string | null;
};

export type SellerInbox = { items: SellerInquiryItem[]; unansweredCount: number };

export const inquiryApi = {
  list: (patternId: number) =>
    apiFetch<InquiryList>(`/patterns/${patternId}/inquiries`),
  create: (patternId: number, content: string, isPrivate: boolean) =>
    apiFetch<{ id: number }>(`/patterns/${patternId}/inquiries`, {
      method: "POST",
      body: { content, isPrivate },
    }),
  answer: (inquiryId: number, answer: string) =>
    apiFetch<void>(`/inquiries/${inquiryId}/answer`, { method: "POST", body: { answer } }),
  update: (inquiryId: number, content: string, isPrivate: boolean) =>
    apiFetch<void>(`/inquiries/${inquiryId}`, { method: "PATCH", body: { content, isPrivate } }),
  remove: (inquiryId: number) =>
    apiFetch<void>(`/inquiries/${inquiryId}`, { method: "DELETE" }),
  sellerInbox: (onlyUnanswered = false) =>
    apiFetch<SellerInbox>(`/seller/inquiries${onlyUnanswered ? "?unanswered=true" : ""}`),
};
