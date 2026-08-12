import { apiUpload } from "@/lib/api";

export type UploadUsage = "PATTERN_IMAGE" | "PROJECT_IMAGE" | "REVIEW_IMAGE" | "PROFILE";

/** 업로드한 파일을 브라우저에서 바로 표시할 수 있는 서빙 경로(동일 출처 프록시). */
export const fileUrl = (id: number) => `/api/v1/files/${id}`;

export const fileApi = {
  upload: (file: File, usageType: UploadUsage = "PATTERN_IMAGE") => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("usageType", usageType);
    return apiUpload<{ id: number }>("/files", fd);
  },
};
