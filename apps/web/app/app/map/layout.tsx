import type { Metadata } from "next";

// The map reads its focus from the URL, so the page is a client component and the title lives here.
export const metadata: Metadata = { title: "Map" };

export default function MapLayout({ children }: { children: React.ReactNode }) {
  return children;
}
