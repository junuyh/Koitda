import { apiFetch } from "@/lib/api";

export type MyPost = {
  postId: number;
  projectId: number;
  displayTitle: string;
  knittingStatus: string | null;
  logDate: string | null;
  createdAt: string;
};

export type MyComment = { id: number; content: string; createdAt: string };

export type MyLikedLog = { postId: number; projectId: number; displayTitle: string; logDate: string | null };

export const meApi = {
  posts: () => apiFetch<MyPost[]>("/users/me/posts"),
  comments: () => apiFetch<MyComment[]>("/users/me/comments"),
  likes: () => apiFetch<MyLikedLog[]>("/users/me/likes"),
  updateProfile: (nickname: string) =>
    apiFetch<{ id: number; nickname: string }>("/users/me", { method: "PATCH", body: { nickname } }),
};
