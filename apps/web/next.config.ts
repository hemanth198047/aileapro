import type { NextConfig } from "next";

const apiOrigin = process.env.AILEPRO_API_ORIGIN ?? "http://localhost:8090";

const nextConfig: NextConfig = {
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
