import type { Metadata } from "next";

export const metadata: Metadata = { title: "Diagnostic" };

export default function DiagnosticLayout({ children }: { children: React.ReactNode }) {
  return children;
}
