import type { Metadata } from "next";
import { Field } from "@aileapro/ui";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Search" };

export default function SearchPage() {
  return (
    <SpacePlaceholder id="search">
      <section className="card">
        <div className="card__head">
          <span className="card__title">Query</span>
          <span className="muted mono">Disabled</span>
        </div>
        <Field
          label="Search everything"
          placeholder="Nothing is indexed yet"
          disabled
          hint="The field is disabled rather than returning zero results for every query."
        />
      </section>
    </SpacePlaceholder>
  );
}
