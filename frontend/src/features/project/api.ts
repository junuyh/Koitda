import { apiFetch } from "@/lib/api";

export type ProjectListItem = {
  id: number;
  displayTitle: string;
  status: string;
  visibility: string;
  patternTitle: string | null;
  patternType: string;
  sellingPatternId: number | null;
  externalPatternId: number | null;
  thumbnailUrl: string | null;
  createdAt: string;
};

export type MaterialGauge = { stitches: number; rows: number; needleSizeMm: number | null; measuredStage: string | null };

export type ProjectDetail = {
  id: number;
  title: string | null;
  displayTitle: string;
  status: string;
  visibility: string;
  publicLogCount: number;
  note: string | null;
  createdAt: string;
  patternType: string;
  sellingPatternId: number | null;
  externalPatternId: number | null;
  patternSnapshot: {
    title?: string;
    designerName?: string;
    categoryName?: string;
    craftType?: string;
    difficulty?: string;
    language?: string;
    referenceVideoUrl?: string;
    yarnRequirement?: string;
    pageCount?: number;
    gauge?: { stitches?: number; rows?: number; needleSizeMm?: number; swatchWidthCm?: number; swatchHeightCm?: number };
    needles?: unknown;
    sizes?: Array<{ label: string; castOnStitches: number; measurements: Record<string, number> }>;
  } | null;
  external: { title: string; creatorName: string | null } | null;
  yarns: Array<{ brand: string | null; yarnName: string | null; color: string | null; amount: string | null; unit: string | null; note: string | null }>;
  needles: Array<{ needleType: string | null; sizeMm: number | null; lengthCm: number | null; note: string | null }>;
  gauges: MaterialGauge[];
  images: Array<{ url: string | null }>;
};

export type TrashItem = {
  id: number;
  displayTitle: string;
  deletedAt: string;
  purgeAt: string;
  remainingDays: number;
};

export type LogItem = {
  id: number;
  displayTitle: string;
  knittingStatus: string | null;
  logDate: string;
  visibility: string;
  comment: string | null;
  contentDocument: unknown;
};

export type CreateProjectBody = {
  connectionType: "CATALOG" | "EXTERNAL";
  sellingPatternId?: number;
  externalPattern?: { title: string; creatorName?: string };
  title?: string;
  note?: string;
  visibility?: "PRIVATE" | "PUBLIC";
  yarns?: Array<{ brand?: string; yarnName?: string; color?: string; amount?: string; unit?: string }>;
  needles?: Array<{ needleType?: string; sizeMm?: number; lengthCm?: number }>;
  gauges?: Array<{ stitches?: number; rows?: number; needleSizeMm?: number; measuredStage?: string }>;
  imageFileIds?: number[];
};

export const STATUS_LABEL: Record<string, string> = {
  PLANNED: "준비 중",
  CO: "코잡기",
  WIP: "뜨는 중",
  UFO: "잠시 멈춤",
  FO: "완성",
};

export type FeedItem = {
  id: number;
  displayTitle: string;
  status: string;
  authorNickname: string;
  thumbnailUrl: string | null;
  likeCount: number;
  createdAt: string;
};

export const projectApi = {
  mine: () => apiFetch<ProjectListItem[]>("/projects/mine"),
  feed: (sort: "recent" | "likes" = "recent", page = 0, size = 12) =>
    apiFetch<FeedItem[]>(`/projects/feed?sort=${sort}&page=${page}&size=${size}`),
  get: (id: number) => apiFetch<ProjectDetail>(`/projects/${id}`),
  create: (body: CreateProjectBody) =>
    apiFetch<{ id: number; displayTitle: string }>("/projects", { method: "POST", body }),
  logs: (projectId: number) => apiFetch<LogItem[]>(`/projects/${projectId}/posts`),
  createLog: (
    projectId: number,
    body: {
      knittingStatus: string;
      comment?: string;
      contentDocument?: unknown;
      logDate?: string;
      title?: string;
      visibility?: "PRIVATE" | "PUBLIC";
      publishProjectConfirmed?: boolean;
    },
  ) => apiFetch<{ projectStatus: string; logVisibility: string; projectPublished: boolean }>(
    `/projects/${projectId}/posts`, { method: "POST", body }),
  visibilityImpact: (projectId: number, visibility: "PRIVATE" | "PUBLIC") =>
    apiFetch<{ affectedPublicLogCount: number }>(`/projects/${projectId}/visibility-impact`, {
      method: "POST",
      body: { visibility },
    }),
  changeVisibility: (projectId: number, visibility: "PRIVATE" | "PUBLIC", confirmed: boolean) =>
    apiFetch<null>(`/projects/${projectId}/visibility`, { method: "PATCH", body: { visibility, confirmed } }),
  moveToTrash: (projectId: number) =>
    apiFetch<{ connectedLogCount: number; purgeAt: string }>(`/projects/${projectId}`, { method: "DELETE" }),
  trash: () => apiFetch<TrashItem[]>("/projects/trash"),
  restore: (projectId: number) => apiFetch<null>(`/projects/${projectId}/restore`, { method: "POST" }),
  permanentDelete: (projectId: number) =>
    apiFetch<null>(`/projects/${projectId}/permanent`, { method: "DELETE" }),
};
