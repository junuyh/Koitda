import { apiFetch } from "@/lib/api";

export type PatternListItem = {
  id: number;
  title: string;
  designerName: string | null;
  sellerBrand: string | null;
  salePrice: number | null;
  regularPrice: number | null;
  difficulty: string | null;
  craftType: string | null;
  thumbnailKey: string | null;
  wishCount: number;
  publicProjectCount: number;
  wished: boolean;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Category = {
  id: number;
  name: string;
  parentId: number | null;
  sortOrder: number;
};

export type WishResult = { wished: boolean; wishCount: number };

export type GaugeInfo = {
  stitches?: number;
  rows?: number;
  swatchWidthCm?: number;
  swatchHeightCm?: number;
  needleSizeMm?: number;
} | null;

export type SizeInfo = {
  sizes: Array<{
    label: string;
    castOnStitches: number;
    measurements: Record<string, number>;
  }>;
} | null;

export type PatternDetail = {
  id: number;
  title: string;
  designerName: string | null;
  sellerBrand: string | null;
  categoryId: number | null;
  categoryName: string | null;
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
  publishedAt: string | null;
  viewCount: number;
  reviewCount: number;
  wishCount: number;
  publicProjectCount: number;
  wished: boolean;
  images: Array<{ key: string | null; thumbnail: boolean }>;
  gaugeInfo: GaugeInfo;
  sizeInfo: SizeInfo;
};

export type PatternListParams = {
  q?: string;
  categoryId?: number;
  craftType?: "KNIT" | "CROCHET";
  page?: number;
  size?: number;
};

function toQuery(params: PatternListParams): string {
  const sp = new URLSearchParams();
  if (params.q) sp.set("q", params.q);
  if (params.categoryId != null) sp.set("categoryId", String(params.categoryId));
  if (params.craftType) sp.set("craftType", params.craftType);
  if (params.page != null) sp.set("page", String(params.page));
  if (params.size != null) sp.set("size", String(params.size));
  const s = sp.toString();
  return s ? `?${s}` : "";
}

export const patternApi = {
  list: (params: PatternListParams) =>
    apiFetch<PageResponse<PatternListItem>>(`/patterns${toQuery(params)}`),
  get: (id: number) => apiFetch<PatternDetail>(`/patterns/${id}`),
  categories: () => apiFetch<Category[]>("/pattern-categories"),
  addWish: (id: number) => apiFetch<WishResult>(`/patterns/${id}/wish`, { method: "POST" }),
  removeWish: (id: number) => apiFetch<WishResult>(`/patterns/${id}/wish`, { method: "DELETE" }),
};
