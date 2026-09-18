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
    nextAction: "Read the unit Today recommends, and see why it chose it",
    waitingOn:
      "The recommendation is rule-based and it says which rule. Overdue recall comes first, then a held false belief, then an unread prerequisite, then the waypoint you are on.",
  },
  {
    id: "learn",
    label: "Learn",
    href: "/app/learn",
    group: "Study",
    icon: <IconBook />,
    purpose: "Authored units: why it matters, explanations at the levels that exist, examples, and what mastery would take.",
    nextAction: "Open your path and read the first unit that is not read yet",
    waitingOn:
      "Each authored unit carries its prerequisites, your reading, and the graded practice and quiz that can move the Practice and Assess chips. Apply fills when you finish a linked project in Studio.",
  },
  {
    id: "map",
    label: "Map",
    href: "/app/map",
    group: "Study",
    icon: <IconMap />,
    purpose: "The knowledge graph: what a unit needs, what it leads to, and what it connects with.",
    nextAction: "Open the map and follow the prerequisite chain into neural networks",
    waitingOn:
      "Typed edges are authored across the units that exist, and nodes now carry your mastery state. Most will read UNKNOWN until there is evidence, which is the honest answer rather than a blank.",
  },
  {
    id: "assess",
    label: "Assess",
    href: "/app/assess",
    group: "Study",
    icon: <IconAssess />,
    purpose: "Quizzes, spaced review, and reports that produce real evidence of mastery.",
    nextAction: "Take the diagnostic, or open a unit and sit its practice set",
    waitingOn:
      "Items are marked on the server and every attempt produces a written report, not a percentage. Exams across a whole path, and code-graded items the Lab will run, come later.",
  },
  {
    id: "lab",
    label: "Lab",
    href: "/app/lab",
    group: "Practice",
    icon: <IconLab />,
    purpose: "Write and run Python in an isolated sandbox. The API never executes your source.",
    nextAction: "Open the demo snippet and press Run",
  },
  {
    id: "studio",
    label: "Studio",
    href: "/app/studio",
    group: "Practice",
    icon: <IconStudio />,
    purpose: "Projects, milestones, and the portfolio you can show someone.",
    nextAction: "Open Build a linear regression model and pass the first milestone",
  },
  {
    id: "notes",
    label: "Notes",
    href: "/app/notes",
    group: "Practice",
    icon: <IconNotes />,
    purpose: "Your own notes, tied back to the units that prompted them.",
    nextAction: "Write a note while reading, from the Notes tab in the companion pane",
    waitingOn:
      "Notes attach to a unit and are yours alone. Linking them to each other and to individual blocks, and searching them beside the curriculum, comes with the search engine in Phase 8.",
  },
  {
    id: "research",
    label: "Research",
    href: "/app/research",
    group: "Horizon",
    icon: <IconResearch />,
    purpose: "Papers with explainers that must cite their sources, plus a literature map.",
    nextAction: "Open a seeded paper and ask the explainer a question it can cite",
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
    nextAction: "Open Career and read which path units are still below PROFICIENT",
  },
  {
    id: "search",
    label: "Search",
    href: "/app/search",
    group: "Workspace",
    icon: <IconSearch size={16} />,
    purpose: "Find published units and blocks across the curriculum — with snippets and links back to the source.",
    nextAction: "Search by title or concept.",
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
      "Access is already enforced by the API: /v1/admin requires the admin role. List drafts, run the critic, publish with audit, and roll back to a snapshot. Drafts never surface in Learn until published.",
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
