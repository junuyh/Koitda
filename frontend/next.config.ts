import type { NextConfig } from "next";

// 브라우저 요청을 동일 출처로 유지하기 위해 /api/* 를 Spring 백엔드로 프록시한다.
// 교차 출처로 직접 호출하면 세션·CSRF 쿠키가 SameSite 정책에 막힌다.
const backendOrigin = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      { source: "/api/:path*", destination: `${backendOrigin}/api/:path*` },
    ];
  },
};

export default nextConfig;
