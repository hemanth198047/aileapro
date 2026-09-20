import type { Metadata } from "next";

// Learn and the unit reader are client components, so the title lives here.
export const metadata: Metadata = { title: "Learn" };

export default function LearnLayout({ children }: { children: React.ReactNode }) {
  return children;
}
