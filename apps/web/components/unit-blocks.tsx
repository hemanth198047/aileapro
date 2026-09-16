"use client";

import { useState } from "react";
import { Callout, IconAlert, IconSpark, Tabs } from "@aileapro/ui";
import { Markdown, MarkdownInline } from "@aileapro/ui/markdown";
import type { UnitBlock } from "@/lib/api";

/**
 * Renders a unit's authored blocks in read mode.
 *
 * Block order comes from the author, not from this component. The only rearrangement is
 * that explanation levels are collected into one switcher, because they are alternative
 * readings of the same idea rather than successive sections. Which level you are shown by
 * default is a Phase 5 decision; for now the shallowest authored level opens first.
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
        return <BlockSection key={block.id} block={block} />;
      })}
    </>
  );
}

function ExplanationLevels({ levels }: { levels: UnitBlock[] }) {
  const [active, setActive] = useState(levels[0].id);
  const current = levels.find((level) => level.id === active) ?? levels[0];

  return (
    <section className="card unit__block" aria-label="Explanation">
      <div className="card__head">
        <span className="card__title">Explanation</span>
        <span className="muted mono">
          {levels.length} of 6 levels authored
        </span>
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

      <p className="hint" style={{ marginTop: 16 }}>
        Levels are the author&apos;s, and only the ones written are offered. Choosing a level for
        you from what you have understood so far is Phase 5.
      </p>
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
    return (
      <section className="card unit__block" aria-label={label}>
        <div className="card__head">
          <span className="card__title">{label}</span>
          <span className="muted mono">Not recorded as evidence</span>
        </div>
        <div className="selfcheck">
          {block.payload.items.map((item, index) => (
            <details className="selfcheck__item" key={index}>
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
          Self-check only. Graded items, misconception tagging, and spaced review arrive with the
          assessment engine in Phase 6, and nothing here touches your mastery record.
        </p>
      </section>
    );
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
            <IconSpark size={12} /> Phase 5 records this
          </span>
        ) : null}
      </div>
      {markdown ? <Markdown>{markdown}</Markdown> : null}
    </section>
  );
}
