import type { Metadata } from "next";

// Assess reads live queues, so the page is a client component and the title lives here.
export const metadata: Metadata = { title: "Assess" };

export default function AssessLayout({ children }: { children: React.ReactNode }) {
  return children;
}
