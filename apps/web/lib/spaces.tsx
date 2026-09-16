import { ReactNode } from "react";
import {
  IconAssess,
  IconBook,
  IconCareer,
  IconCompass,
  IconEvolve,
  IconLab,
  IconMap,
  IconNotes,
  IconResearch,
  IconSearch,
  IconSettings,
  IconShield,
  IconStudio,
} from "@aileapro/ui";

/**
 * The spaces from 04-INFORMATION_ARCHITECTURE. This registry is the only place
 * that knows a space exists: navigation, the command palette, breadcrumbs, and
 * the empty states all read from it.
 *
 * `arrivesIn` is deliberately explicit. Until a space has real data behind it,
 * it must say which phase delivers it rather than showing invented numbers.
 */

export type SpaceId =
  | "today"
  | "learn"
  | "map"
  | "lab"
  | "studio"
  | "assess"
  | "research"
  | "evolve"
  | "career"
  | "notes"
  | "search"
  | "settings"
  | "admin";

export type SpaceGroup = "Study" | "Practice" | "Horizon" | "Workspace";

export type Space = {
  id: SpaceId;
  label: string;
  href: string;
  group: SpaceGroup;
  icon: ReactNode;
  /** One sentence, shown under the title and in the orientation column. */
  purpose: string;
  /** The phase that gives this space real content. Absent means it works now. */
  arrivesIn?: string;
  /** What the learner can actually do next. Verb + object, never "Continue". */
  nextAction: string;
  /** Detail for the empty state: what is missing and why. */
  waitingOn?: string;
  adminOnly?: boolean;
};

export const SPACES: Space[] = [
  {
    id: "today",
    label: "Today",
    href: "/app/today",
    group: "Study",
    icon: <IconCompass />,
    purpose: "Your one recommended action, reviews that are due, and anything that needs attention.",
    nextAction: "Start the first unit of the AI Engineer path",
    waitingOn:
      "Ranking a recommendation needs mastery evidence, which starts being recorded in Phase 5. Until then Today points at the start of the authored path rather than pretending to know where you are.",
  },
  {
    id: "learn",
    label: "Learn",
    href: "/app/learn",
    group: "Study",
    icon: <IconBook />,
    purpose: "Authored units: why it matters, explanations at the levels that exist, examples, and what mastery would take.",
    nextAction: "Open the AI Engineer path and read a unit",
    waitingOn:
      "Ten units are authored and published. Level selection, evidence chips, and the practice dock arrive in Phase 5.",
  },
  {
    id: "map",
    label: "Map",
    href: "/app/map",
    group: "Study",
    icon: <IconMap />,
    purpose: "The knowledge graph: what you know, what is next, and what is blocking it.",
    arrivesIn: "Phase 4",
    nextAction: "Nothing yet — the graph arrives in Phase 4",
    waitingOn:
      "The nodes exist now: 57 domains and the authored units under them. Phase 4 adds the typed prerequisite edges and the neighbourhood view, with a list fallback alongside the canvas.",
  },
  {
    id: "assess",
    label: "Assess",
    href: "/app/assess",
    group: "Study",
    icon: <IconAssess />,
    purpose: "Quizzes, exams, and spaced review that produce real evidence of mastery.",
    arrivesIn: "Phase 6",
    nextAction: "Nothing yet — assessment arrives in Phase 6",
    waitingOn:
      "Phase 6 brings the question bank, attempt reports that are more than a percentage, the placement diagnostic, and due reviews.",
  },
  {
    id: "lab",
    label: "Lab",
    href: "/app/lab",
    group: "Practice",
    icon: <IconLab />,
    purpose: "Write and run code against real datasets in an isolated container.",
    arrivesIn: "Phase 9",
    nextAction: "Nothing yet — the lab arrives in Phase 9",
    waitingOn:
      "The executor service exists but runs nothing yet. Phase 9 wires Python run/stop/output and history. The API process will never execute learner code.",
  },
  {
    id: "studio",
    label: "Studio",
    href: "/app/studio",
    group: "Practice",
    icon: <IconStudio />,
    purpose: "Projects, milestones, and the portfolio you can show someone.",
    arrivesIn: "Phase 10",
    nextAction: "Nothing yet — projects arrive in Phase 10",
    waitingOn:
      "Phase 10 ships the first two project briefs with milestones and submissions. Completing one is what fills the Apply evidence chip.",
  },
  {
    id: "notes",
    label: "Notes",
    href: "/app/notes",
    group: "Practice",
    icon: <IconNotes />,
    purpose: "Your own linked notes, tied back to the units that prompted them.",
    arrivesIn: "Phase 5",
    nextAction: "Nothing yet — notes arrive in Phase 5",
    waitingOn:
      "Notes attach to a unit and a block. The blocks exist now; capture, linking, and the notes pane are Phase 5.",
  },
  {
    id: "research",
    label: "Research",
    href: "/app/research",
    group: "Horizon",
    icon: <IconResearch />,
    purpose: "Papers with explainers that must cite their sources, plus a literature map.",
    arrivesIn: "Phase 11",
    nextAction: "Nothing yet — research arrives in Phase 11",
    waitingOn:
      "Explainers are held to forced citations and tested against fabricated-reference fixtures before this space opens.",
  },
  {
    id: "evolve",
    label: "Evolve",
    href: "/app/evolve",
    group: "Horizon",
    icon: <IconEvolve />,
    purpose: "What changed in AI since you learned it, and whether it matters to you.",
    arrivesIn: "Phase 15",
    nextAction: "Nothing yet — the update feed arrives in Phase 15",
    waitingOn:
      "The feed depends on watchers, proposals, and an admin review queue. Timestamped items only — no unreviewed claims.",
  },
  {
    id: "career",
    label: "Career",
    href: "/app/career",
    group: "Horizon",
    icon: <IconCareer />,
    purpose: "Role readiness, skill gaps, and interview practice drawn from your evidence.",
    arrivesIn: "Phase 12",
    nextAction: "Nothing yet — career tools arrive in Phase 12",
    waitingOn:
      "Gaps are computed from assessed mastery against a target role. With no assessments recorded, any readiness score would be fiction.",
  },
  {
    id: "search",
    label: "Search",
    href: "/app/search",
    group: "Workspace",
    icon: <IconSearch size={16} />,
    purpose: "One query across units, notes, papers, and code — with sources attached.",
    arrivesIn: "Phase 8",
    nextAction: "Nothing yet — search arrives in Phase 8",
    waitingOn:
      "There are now published blocks to index. Phase 8 embeds them into pgvector and adds the query API, so search stays off until it can return real results with sources.",
  },
  {
    id: "settings",
    label: "Settings",
    href: "/app/settings",
    group: "Workspace",
    icon: <IconSettings />,
    purpose: "Your account, appearance, and the controls that already apply.",
    nextAction: "Review your account details",
  },
  {
    id: "admin",
    label: "Admin",
    href: "/app/admin",
    group: "Workspace",
    icon: <IconShield />,
    purpose: "Content authoring, versioning, and the update review queue.",
    arrivesIn: "Phase 13",
    nextAction: "Nothing yet — the content studio arrives in Phase 13",
    waitingOn:
      "Access is already enforced by the API: /v1/admin requires the admin role. Drafts will never surface in Learn until published.",
    adminOnly: true,
  },
];

export const SPACE_GROUPS: SpaceGroup[] = ["Study", "Practice", "Horizon", "Workspace"];

export function getSpace(id: SpaceId): Space {
  const space = SPACES.find((s) => s.id === id);
  if (!space) throw new Error(`Unknown space: ${id}`);
  return space;
}

export function spaceForPath(pathname: string): Space | undefined {
  return SPACES.find((s) => pathname === s.href || pathname.startsWith(`${s.href}/`));
}
