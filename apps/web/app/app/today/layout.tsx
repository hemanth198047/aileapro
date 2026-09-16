import type { Metadata } from "next";

// The page itself is a client component, so its title lives here.
export const metadata: Metadata = { title: "Today" };

export default function TodayLayout({ children }: { children: React.ReactNode }) {
  return children;
}
