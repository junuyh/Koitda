import { apiFetch } from "@/lib/api";

export type TargetType = "POST" | "REVIEW" | "COMMENT" | "USER";

export type LikeResponse = { liked: boolean; likeCount: number };

export type CommentItem = {
  id: number;
  authorNickname: string;
  content: string;
  mine: boolean;
  createdAt: string;
};

export type CommentList = { count: number; items: CommentItem[] };

export type ReportResponse = { validReportCount: number; flagged: boolean; autoHidden: boolean };

export type FollowUser = { userId: number; nickname: string; since: string };

export const socialApi = {
  toggleLike: (targetType: TargetType, targetId: number) =>
    apiFetch<LikeResponse>("/likes", { method: "POST", body: { targetType, targetId } }),
  comments: (targetType: TargetType, targetId: number) =>
    apiFetch<CommentList>(`/comments?targetType=${targetType}&targetId=${targetId}`),
  addComment: (targetType: TargetType, targetId: number, content: string) =>
    apiFetch<CommentItem>("/comments", { method: "POST", body: { targetType, targetId, content } }),
  deleteComment: (commentId: number) =>
    apiFetch<void>(`/comments/${commentId}`, { method: "DELETE" }),
  report: (targetType: TargetType, targetId: number, reasonCode: string, detail?: string) =>
    apiFetch<ReportResponse>("/reports", { method: "POST", body: { targetType, targetId, reasonCode, detail } }),
  follow: (userId: number) => apiFetch<void>(`/follows/${userId}`, { method: "POST" }),
  unfollow: (userId: number) => apiFetch<void>(`/follows/${userId}`, { method: "DELETE" }),
  following: () => apiFetch<FollowUser[]>("/users/me/following"),
  followers: () => apiFetch<FollowUser[]>("/users/me/followers"),
};
