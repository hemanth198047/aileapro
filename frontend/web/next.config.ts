import type { NextConfig } from "next";

const apiOrigin = process.env.AILEAPRO_API_ORIGIN ?? "http://localhost:8090";

// A verification build run while the dev server is up would otherwise overwrite
// the chunks that server is still serving, and every route starts answering 500.
// Production (build / start) writes to .next-prod; `next dev` keeps .next.
const distDir =
  process.env.AILEAPRO_NEXT_DIST ??
  (process.env.NODE_ENV === "production" ? ".next-prod" : ".next");

const nextConfig: NextConfig = {
  // @aileapro/ui ships TypeScript source, so Next must compile it.
  transpilePackages: ["@aileapro/ui"],
  distDir,
  async rewrites() {
    return [
      {
        source: "/v1/:path*",
        destination: `${apiOrigin}/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
