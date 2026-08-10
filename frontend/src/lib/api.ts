// 백엔드 API 호출 공통 래퍼.
// - 동일 출처(/api/*)로 호출하므로 세션 쿠키가 자동 전송된다.
// - 상태 변경 요청에는 CSRF 토큰(XSRF-TOKEN 쿠키 → X-XSRF-TOKEN 헤더)을 실어 보낸다.

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp("(?:^|; )" + name + "=([^;]*)"));
  return match ? decodeURIComponent(match[1]) : null;
}

// CSRF 쿠키가 없으면 /csrf 를 먼저 호출해 쿠키를 심고 토큰을 읽는다.
async function ensureCsrfToken(): Promise<string> {
  let token = readCookie("XSRF-TOKEN");
  if (!token) {
    await fetch("/api/v1/csrf", { credentials: "include" });
    token = readCookie("XSRF-TOKEN");
  }
  return token ?? "";
}

type RequestOptions = {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  body?: unknown;
};

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = { "Content-Type": "application/json" };

  if (method !== "GET") {
    headers["X-XSRF-TOKEN"] = await ensureCsrfToken();
  }

  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    credentials: "include",
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (response.status === 204) {
    return null as T;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new ApiError(
      response.status,
      data?.code ?? "UNKNOWN",
      data?.message ?? "요청을 처리하지 못했습니다.",
      data?.fieldErrors ?? {},
    );
  }

  return data as T;
}
