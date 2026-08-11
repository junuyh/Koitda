import { apiFetch } from "@/lib/api";
import type { SellerPatternPreview } from "@/features/seller/api";

export type AdminPatternListItem = {
  id: number;
  title: string | null;
  sellerBrand: string | null;
  productStatus: string;
  regularPrice: number | null;
  salePrice: number | null;
  updatedAt: string | null;
};

export type AdminApplicationItem = {
  id: number;
  brandName: string;
  businessType: "INDIVIDUAL" | "BUSINESS" | null;
  businessNo: string | null;
  representativeName: string | null;
  settlementBank: string | null;
  settlementAccountMasked: string | null;
  status: string;
  rejectionReason: string | null;
  createdAt: string | null;
  reviewedAt: string | null;
};

export const adminApi = {
  // 도안 심사
  listPatterns: (status?: string) =>
    apiFetch<AdminPatternListItem[]>(`/admin/patterns${status ? `?status=${status}` : ""}`),
  patternDetail: (id: number) => apiFetch<SellerPatternPreview>(`/admin/patterns/${id}`),
  approvePattern: (id: number) =>
    apiFetch<void>(`/admin/patterns/${id}/approve`, { method: "POST" }),
  rejectPattern: (id: number, reason: string) =>
    apiFetch<void>(`/admin/patterns/${id}/reject`, { method: "POST", body: { reason } }),

  // 판매자 신청 심사
  listApplications: (status?: string) =>
    apiFetch<AdminApplicationItem[]>(`/admin/seller-applications${status ? `?status=${status}` : ""}`),
  approveApplication: (id: number) =>
    apiFetch<void>(`/admin/seller-applications/${id}/approve`, { method: "POST" }),
  rejectApplication: (id: number, reason: string) =>
    apiFetch<void>(`/admin/seller-applications/${id}/reject`, { method: "POST", body: { reason } }),
};
