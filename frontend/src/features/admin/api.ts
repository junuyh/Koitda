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

  // 신고 관리(ADMIN-002)
  listReports: (resolution?: string) =>
    apiFetch<ReportedItem[]>(`/admin/reports${resolution ? `?resolution=${resolution}` : ""}`),
  hideReport: (moderationId: number) =>
    apiFetch<void>(`/admin/reports/${moderationId}/hide`, { method: "POST" }),
  dismissReport: (moderationId: number) =>
    apiFetch<void>(`/admin/reports/${moderationId}/dismiss`, { method: "POST" }),
  restoreReport: (moderationId: number) =>
    apiFetch<void>(`/admin/reports/${moderationId}/restore`, { method: "POST" }),
};

export type ReportedItem = {
  moderationId: number;
  targetType: string;
  targetId: number;
  reportCount: number;
  flagged: boolean;
  hidden: boolean;
  resolution: string;
  title: string | null;
  preview: string | null;
  authorNickname: string | null;
  reasons: string[];
  lastReportedAt: string | null;
};
