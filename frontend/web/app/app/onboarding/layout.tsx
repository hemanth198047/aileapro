import type { Metadata } from "next";

/** The page itself is a client component, so its title lives here. */
export const metadata: Metadata = { title: "Setup" };

export default function OnboardingLayout({ children }: { children: React.ReactNode }) {
  return children;
}
