"use client";

import { useEffect, useRef, useState } from "react";
import { Button, Callout, IconAlert, IconSpark, Tabs } from "@aileapro/ui";
import { Markdown, MarkdownInline } from "@aileapro/ui/markdown";
import type { UnitBlock } from "@/lib/api";
import { useUnitProgress } from "@/components/unit-progress";

/**
 * Renders a unit's authored blocks in read mode.
 *
 * Block order comes from the author, not from this component. The only rearrangement is
 * that explanation levels are collected into one switcher, because they are alternative
 * readings of the same idea rather than successive sections. The shallowest authored level
 * opens first; choosing one from what the learner already understands needs graded evidence
 * to choose from, so it waits for the assessment engine.
 */

const LEVELS = ["eli5", "beginner", "intermediate", "advanced", "expert", "research"] as const;

const LEVEL_LABELS: Record<string, string> = {
  eli5: "Simplest",
  beginner: "Beginner",
  intermediate: "Intermediate",
  advanced: "Advanced",
  expert: "Expert",
  research: "Research",
};

const SECTION_LABELS: Record<string, string> = {
  why: "Why this matters",
  objectives: "Objectives",
  prereq_check: "Before you start",
  formal_definition: "Definition",
  math_intuition: "Intuition",
  math_formal: "Formally",
  visual: "Diagram",
  code: "In code",
  example: "Worked example",
  counterexample: "Counterexample",
  application: "Where this shows up",
  common_mistake: "Common mistakes",
  misconception: "Misconception",
  exercise_set: "Exercises",
  quiz_set: "Self-check",
  challenge: "Challenge",
  interview: "Interview angle",
  assessment_criteria: "How this is assessed",
  mastery_criteria: "What mastery looks like",
  related: "Related",
  references: "References",
};

function isLevel(type: string) {
  return (LEVELS as readonly string[]).includes(type);
}

export function UnitBody({ blocks }: { blocks: UnitBlock[] }) {
  const levels = blocks
    .filter((block) => isLevel(block.type))
    .sort((a, b) => LEVELS.indexOf(a.type as never) - LEVELS.indexOf(b.type as never));
  const firstLevelId = levels[0]?.id;

  return (
    <>
      {blocks.map((block) => {
        if (isLevel(block.type)) {
          // The whole switcher is rendered once, where the first level block sits.
          return block.id === firstLevelId ? (
            <ExplanationLevels key={block.id} levels={levels} />
          ) : null;
        }
        // Exercises belong to the practice dock below the body, so they are skipped here
        // rather than printed twice in one page.
        if (block.type === "exercise_set") {
          return null;
        }
        return <BlockSection key={block.id} block={block} />;
      })}
    </>
  );
}

function ExplanationLevels({ levels }: { levels: UnitBlock[] }) {
  const [active, setActive] = useState(levels[0].id);
  const current = levels.find((level) => level.id === active) ?? levels[0];
  const { mastery, report } = useUnitProgress();

  // The level on screen is reported, including the one that opens by default, because reading
  // an explanation is the part of INTRODUCED that a page load alone does not prove.
  useEffect(() => {
    report("explanation_level", { level: current.type });
  }, [current.type, report]);

  const shallower = levels.findIndex((level) => level.id === active) - 1;
  const deeper = levels.findIndex((level) => level.id === active) + 1;

  return (
    <section className="card unit__block" aria-label="Explanation">
      <div className="card__head">
        <span className="card__title">Explanation</span>
        <span className="muted mono">{levels.length} of 6 levels authored</span>
      </div>

      {levels.length > 1 ? (
        <Tabs
          label="Explanation level"
          value={active}
          onChange={setActive}
          items={levels.map((level) => ({
            id: level.id,
            label: LEVEL_LABELS[level.type] ?? level.type,
          }))}
        />
      ) : null}

      <Markdown>{current.payload.markdown ?? ""}</Markdown>

      {/* Simpler and Deeper are always offered, and disabled when the author stopped writing,
          so the control tells you where the authoring ends instead of hiding it. */}
      <div className="levelctl">
        <Button
          variant="outline"
          disabled={shallower < 0}
          onClick={() => setActive(levels[shallower].id)}
        >
          Simpler
        </Button>
        <Button
          variant="outline"
          disabled={deeper >= levels.length}
          onClick={() => setActive(levels[deeper].id)}
        >
          Deeper
        </Button>
        <span className="hint">
          {mastery && mastery.levelsSeen.length > 0
            ? `You have opened ${mastery.levelsSeen.length} of these ${levels.length}.`
            : "Reading one of these is what INTRODUCED means."}
        </span>
      </div>
    </section>
  );
}

function BlockSection({ block }: { block: UnitBlock }) {
  const label = SECTION_LABELS[block.type] ?? block.type.replace(/_/g, " ");
  const markdown = block.payload.markdown;

  if (block.type === "why") {
    return (
      <section className="card unit__block unit__why" aria-label={label}>
        <div className="card__head">
          <span className="kicker">{label}</span>
        </div>
        {markdown ? <Markdown>{markdown}</Markdown> : null}
      </section>
    );
  }

  if (block.type === "misconception") {
    return (
      <section className="card unit__block" aria-label={label}>
        <div className="card__head">
          <span className="card__title">{label}</span>
          {block.payload.code ? <span className="badge badge--warn">{block.payload.code}</span> : null}
        </div>
        {block.payload.claim ? (
          <Callout tone="warn" icon={<IconAlert />}>
            {block.payload.claim}
          </Callout>
        ) : null}
        {markdown ? <Markdown>{markdown}</Markdown> : null}
      </section>
    );
  }

  if (block.type === "quiz_set" && block.payload.items?.length) {
    return <SelfCheck block={block} label={label} />;
  }

  return (
    <section className="card unit__block" aria-label={label}>
      <div className="card__head">
        <span className="card__title">{label}</span>
        {block.type === "exercise_set" && block.payload.checkedByPlatform === false ? (
          <span className="badge">Checked by you, not the platform</span>
        ) : null}
        {block.type === "mastery_criteria" ? (
          <span className="badge badge--brand">
            <IconSpark size={12} /> Reading alone will not meet this
          </span>
        ) : null}
      </div>
      {markdown ? <Markdown>{markdown}</Markdown> : null}
    </section>
  );
}

/**
 * Self-check questions with the answers hidden. Revealing one is reported and deliberately
 * worth nothing: the platform did not see you answer, so it has no business claiming you did.
 */
function SelfCheck({ block, label }: { block: UnitBlock; label: string }) {
  const { report } = useUnitProgress();
  const revealed = useRef(new Set<number>());

  return (
    <section className="card unit__block" aria-label={label}>
      <div className="card__head">
        <span className="card__title">{label}</span>
        <span className="muted mono">Not recorded as evidence</span>
      </div>
      <div className="selfcheck">
        {block.payload.items?.map((item, index) => (
          <details
            className="selfcheck__item"
            key={index}
            onToggle={(event) => {
              if (event.currentTarget.open && !revealed.current.has(index)) {
                revealed.current.add(index);
                report("self_check_revealed");
              }
            }}
          >
            <summary>
              <span className="selfcheck__n">{index + 1}</span>
              <MarkdownInline>{item.question}</MarkdownInline>
            </summary>
            <div className="selfcheck__answer">
              <Markdown>{item.answer}</Markdown>
            </div>
          </details>
        ))}
      </div>
      <p className="hint" style={{ marginTop: 12 }}>
        Self-check only. Revealing an answer is stored as history and earns no evidence, because
        seeing the answer is not recalling it. Graded items and misconception tagging arrive with
        the assessment engine in Phase 6.
      </p>
    </section>
  );
}
