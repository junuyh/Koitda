import { apiFetch } from "@/lib/api";

export type ReviewItem = {
  id: number;
  authorNickname: string;
  title: string | null;
  contentText: string | null;
  knittingStatus: string | null;
  gaugeAdjustmentSummary: string | null;
  likeCount: number;
  commentCount: number;
  liked: boolean;
  mine: boolean;
  createdAt: string;
};

export type ReviewListResponse = {
  items: ReviewItem[];
  purchased: boolean;
  myReviewId: number | null;
};

export type ReviewCreated = {
  reviewId: number;
  contentText: string | null;
  gaugeAdjustmentSummary: string | null;
  earnedPoint: number;
  pointBalance: number;
};

export type LoadableLog = {
  postId: number;
  displayTitle: string;
  logDate: string | null;
  knittingStatus: string | null;
};

export type CreateReviewBody = {
  sourcePostId?: number;
  title?: string;
  contentText?: string;
  visibility?: "PUBLIC" | "PRIVATE";
};

export type ReviewDeleted = { revokedPoint: number; pointBalance: number; revokeFailReason: string | null };

export const reviewApi = {
  list: (patternId: number) => apiFetch<ReviewListResponse>(`/patterns/${patternId}/reviews`),
  loadableLogs: (patternId: number) => apiFetch<LoadableLog[]>(`/patterns/${patternId}/reviews/loadable-logs`),
  create: (patternId: number, body: CreateReviewBody) =>
    apiFetch<ReviewCreated>(`/patterns/${patternId}/reviews`, { method: "POST", body }),
  remove: (reviewId: number) => apiFetch<ReviewDeleted>(`/reviews/${reviewId}`, { method: "DELETE" }),
};

// ---------------------------------------------------------------- 포인트(POINT-006)

export type PointTx = {
  id: number;
  txType: string;
  amount: number;
  balanceAfter: number;
  reasonCode: string | null;
  reviewId: number | null;
  failReason: string | null;
  createdAt: string;
};

export type PointHistory = { balance: number; transactions: PointTx[] };

export const pointApi = {
  history: () => apiFetch<PointHistory>("/users/me/point-transactions"),
};
