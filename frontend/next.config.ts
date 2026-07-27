import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker 이미지용 — .next/standalone 산출물 생성 (Dockerfile이 이걸 복사)
  output: "standalone",
};

export default nextConfig;
