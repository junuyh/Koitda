import { apiFetch } from "@/lib/api";

export type SellerApplicationBody = {
  brandName: string;
  businessType?: "INDIVIDUAL" | "BUSINESS";
  businessNo?: string;
  representativeName?: string;
  settlementBank?: string;
  settlementAccount?: string;
  termsVersion: string;
};

export const sellerApi = {
  apply: (body: SellerApplicationBody) =>
    apiFetch<{ id: number; status: string }>("/seller-applications", { method: "POST", body }),
};

// ---------------------------------------------------------------- 도안 등록(SELLER-002·003)

export type GaugeBody = {
  stitches?: number;
  rows?: number;
  swatchWidthCm?: number;
  swatchHeightCm?: number;
  needleSizeMm?: number;
  text?: string;
};

export type SizeRow = {
  label: string;
  castOnStitches?: number;
  measurements?: Record<string, number>;
};

export type SavePatternDraftBody = {
  title?: string;
  designerName?: string;
  categoryId?: number;
  craftType?: "KNIT" | "CROCHET";
  difficulty?: string;
  language?: string;
  regularPrice?: number;
  salePrice?: number;
  productForm?: string;
  deliveryMethod?: string;
  availabilityDays?: number;
  referenceVideoUrl?: string;
  pageCount?: number;
  yarnRequirement?: string;
  description?: string;
  descriptionDocument?: unknown;
  gauge?: GaugeBody;
  sizes?: SizeRow[];
  imageFileIds?: number[];
  thumbnailFileId?: number;
  pdfFileId?: number;
};

export type DraftSaved = { draftId: number; productStatus: string; missingFields: string[] };

export type SellerPatternListItem = {
  id: number;
  title: string | null;
  productStatus: string;
  regularPrice: number | null;
  salePrice: number | null;
  thumbnailUrl: string | null;
  updatedAt: string | null;
  publishedAt: string | null;
  rejectionReason: string | null;
};

export type SellerPatternPreview = {
  id: number;
  title: string | null;
  designerName: string | null;
  sellerBrand: string | null;
  categoryId: number | null;
  craftType: string | null;
  difficulty: string | null;
  language: string | null;
  regularPrice: number | null;
  salePrice: number | null;
  productForm: string | null;
  deliveryMethod: string | null;
  availabilityDays: number | null;
  referenceVideoUrl: string | null;
  pageCount: number | null;
  yarnRequirement: string | null;
  description: string | null;
  descriptionDocument: unknown;
  productStatus: string;
  rejectionReason: string | null;
  publishedAt: string | null;
  pdfFileId: number | null;
  images: Array<{ url: string | null; thumbnail: boolean }>;
  gaugeInfo: { stitches?: number; rows?: number; swatchWidthCm?: number; swatchHeightCm?: number; needleSizeMm?: number } | null;
  sizeInfo: { sizes: Array<{ label: string; castOnStitches: number; measurements?: Record<string, number> }> } | null;
  needleInfo: unknown;
  techniqueInfo: unknown;
};

export const sellerPatternApi = {
  list: (status?: string) =>
    apiFetch<SellerPatternListItem[]>(`/seller/patterns${status ? `?status=${status}` : ""}`),
  createDraft: (body: SavePatternDraftBody) =>
    apiFetch<DraftSaved>("/seller/pattern-drafts", { method: "POST", body }),
  updateDraft: (id: number, body: SavePatternDraftBody) =>
    apiFetch<DraftSaved>(`/seller/pattern-drafts/${id}`, { method: "PATCH", body }),
  preview: (id: number) =>
    apiFetch<SellerPatternPreview>(`/seller/pattern-drafts/${id}/preview`),
  submit: (id: number) =>
    apiFetch<{ patternId: number; productStatus: string }>(`/seller/pattern-drafts/${id}/submit`, { method: "POST" }),
};
