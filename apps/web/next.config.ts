import type { NextConfig } from "next";

const apiOrigin = process.env.AILEAPRO_API_ORIGIN ?? "http://localhost:8090";

const nextConfig: NextConfig = {
  // @aileapro/ui ships TypeScript source, so Next must compile it.
  transpilePackages: ["@aileapro/ui"],
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
