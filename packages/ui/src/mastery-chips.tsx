/**
 * Evidence chips per 05-UX_SPEC: Read, Practice, Assess, Apply.
 * Empty is the honest default — completing Read never fills Assess.
 */

export type EvidenceState = "empty" | "partial" | "solid";

export type Evidence = {
  read?: EvidenceState;
  practice?: EvidenceState;
  assess?: EvidenceState;
  apply?: EvidenceState;
};

const ORDER: { key: keyof Evidence; label: string }[] = [
  { key: "read", label: "Read" },
  { key: "practice", label: "Practice" },
  { key: "assess", label: "Assess" },
  { key: "apply", label: "Apply" },
];

export function MasteryChips({
  evidence = {},
  lastEvidenceAt,
}: {
  evidence?: Evidence;
  /** ISO date of the most recent evidence, shown as the chip tooltip. */
  lastEvidenceAt?: string;
}) {
  return (
    <div className="chips">
      {ORDER.map(({ key, label }) => {
        const state = evidence[key] ?? "empty";
        const title =
          state === "empty"
            ? `${label}: no evidence yet`
            : `${label}: ${state}${lastEvidenceAt ? ` — last evidence ${lastEvidenceAt}` : ""}`;
        return (
          <span className="chip" data-state={state} key={key} title={title}>
            <span className="chip__dot" aria-hidden />
            {label}
          </span>
        );
      })}
    </div>
  );
}
