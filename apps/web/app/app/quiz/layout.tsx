import type { Metadata } from "next";

// The runner is a client component, so the title lives here.
export const metadata: Metadata = { title: "Quiz" };

export default function QuizLayout({ children }: { children: React.ReactNode }) {
  return children;
}
