import type { Metadata } from "next";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Notes" };

export default function NotesPage() {
  return <SpacePlaceholder id="notes" />;
}
