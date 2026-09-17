/**
 * Evidence chips per 05-UX_SPEC: Read, Practice, Assess, Apply.
 *
 * Empty is the honest default, each chip carries the date it last moved, and no chip is ever
 * derived from another — completing Read never fills Assess. The tooltip says what the chip
 * rests on, because a coloured dot with nothing behind it is the thing this product is
 * trying not to be.
 */

export type EvidenceState = "empty" | "partial" | "solid";

/** @param at ISO timestamp of the evidence behind this chip, null while empty. */
export type EvidenceChip = { state: EvidenceState; at?: string | null };

export type Evidence = {
  read?: EvidenceChip;
  practice?: EvidenceChip;
  assess?: EvidenceChip;
  apply?: EvidenceChip;
};

const ORDER: { key: keyof Evidence; label: string; earnedBy: string }[] = [
  { key: "read", label: "Read", earnedBy: "reading an explanation for long enough to be plausible" },
  { key: "practice", label: "Practice", earnedBy: "exercises you complete" },
  { key: "assess", label: "Assess", earnedBy: "graded items" },
  { key: "apply", label: "Apply", earnedBy: "a project, lab, or teach-back" },
];

export function MasteryChips({ evidence = {} }: { evidence?: Evidence }) {
  return (
    <div className="chips">
      {ORDER.map(({ key, label, earnedBy }) => {
        const chip = evidence[key];
        const state = chip?.state ?? "empty";
        const title =
          state === "empty"
            ? `${label}: no evidence yet — earned by ${earnedBy}`
            : `${label}: ${state}${chip?.at ? ` — last evidence ${formatDay(chip.at)}` : ""}`;
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

function formatDay(iso: string) {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toISOString().slice(0, 10);
}
