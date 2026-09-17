export async function csrfHeaders(): Promise<HeadersInit> {
  await fetch("/v1/auth/csrf", { credentials: "include" });
  const token = readCookie("XSRF-TOKEN");
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) headers["X-XSRF-TOKEN"] = decodeURIComponent(token);
  return headers;
}

export function readCookie(name: string): string | undefined {
  const parts = document.cookie.split(";").map((p) => p.trim());
  const hit = parts.find((p) => p.startsWith(`${name}=`));
  return hit?.slice(name.length + 1);
}

export type Me = {
  id: string;
  email: string;
  emailVerified: boolean;
  roles: string[];
};

/** Catalog reads. Mirrors CurriculumViews on the API; learners only ever get published rows. */
export type UnitRef = {
  id: string;
  slug: string;
  title: string;
  description: string | null;
  kind: string;
  difficulty: number | null;
  estimateMinutes: number | null;
  volatility: string;
  knowledgeClass: string;
  hasContent: boolean;
  childCount: number;
};

export type PathSummary = {
  id: string;
  slug: string;
  title: string;
  description: string | null;
  roleTarget: string | null;
  version: string;
  waypointCount: number;
  estimateMinutes: number;
};

export type Waypoint = {
  sort: number;
  requiredMasteryState: string | null;
  unit: UnitRef;
};

/** The detail response carries the waypoints themselves, so it has no separate count. */
export type PathDetail = Omit<PathSummary, "waypointCount"> & { waypoints: Waypoint[] };

/** Block payloads are typed per block kind; every authored block has `markdown` or `items`. */
export type BlockPayload = {
  markdown?: string;
  language?: string;
  code?: string;
  claim?: string;
  kind?: string;
  selfCheck?: boolean;
  checkedByPlatform?: boolean;
  items?: { question: string; answer: string }[];
};

export type UnitBlock = {
  id: string;
  type: string;
  sort: number;
  payload: BlockPayload;
};

export type UnitResource = {
  title: string;
  url: string;
  publisher: string | null;
  authors: string | null;
  sourceType: string;
  license: string | null;
  publishedOn: string | null;
  lastVerifiedAt: string | null;
  purpose: string | null;
  excerpt: string | null;
};

export type UnitDetail = {
  id: string;
  slug: string;
  title: string;
  description: string | null;
  kind: string;
  difficulty: number | null;
  estimateMinutes: number | null;
  volatility: string;
  knowledgeClass: string;
  version: string;
  lastReviewedAt: string | null;
  lastVerifiedAt: string | null;
  objectives: string[];
  tags: string[];
  breadcrumb: UnitRef[];
  children: UnitRef[];
  blocks: UnitBlock[];
  resources: UnitResource[];
};

/** Graph reads. Mirrors GraphViews on the API. */
export type GraphNode = {
  unit: UnitRef;
  /** Distance from the focus unit, so the map can lay nodes out in rings. */
  hops: number;
  masteryState: MasteryState;
};

export type GraphEdge = {
  id: string;
  fromId: string;
  toId: string;
  type: string;
  weight: number;
  /** Symmetric edges are drawn without an arrowhead. */
  symmetric: boolean;
  /** Gating edges can block progress, so they are drawn and listed first. */
  gating: boolean;
  note: string | null;
};

export type Neighborhood = {
  focus: UnitRef;
  hops: number;
  maxHops: number;
  nodeBudget: number;
  truncated: boolean;
  /** True now that evidence is recorded; node states are the learner's own. */
  masteryTracked: boolean;
  edgeTypes: string[];
  nodes: GraphNode[];
  edges: GraphEdge[];
};

export type PrerequisiteStep = {
  unit: UnitRef;
  depth: number;
  direct: boolean;
  masteryState: MasteryState;
};

export type Prerequisites = {
  unit: UnitRef;
  masteryTracked: boolean;
  required: PrerequisiteStep[];
  advisory: PrerequisiteStep[];
};

/** Learner state. Mirrors LearningViews on the API. */
export type MasteryState =
  | "UNKNOWN"
  | "INTRODUCED"
  | "FAMILIAR"
  | "UNDERSTOOD"
  | "PRACTICED"
  | "PROFICIENT"
  | "MASTERED";

export type EvidenceChipState = "empty" | "partial" | "solid";

/** `at` is when the chip last moved, which is what makes it a claim rather than a colour. */
export type EvidenceChip = { state: EvidenceChipState; at: string | null };

export type Evidence = {
  read: EvidenceChip;
  practice: EvidenceChip;
  assess: EvidenceChip;
  apply: EvidenceChip;
};

export type Mastery = {
  unitId: string;
  unitSlug: string;
  state: MasteryState;
  /** Internal ranking score. Never rendered as a headline percentage. */
  score: number;
  evidence: Evidence;
  dwellSeconds: number;
  /** Visible reading time the Read chip needs before it goes solid. */
  readFloorSeconds: number;
  levelsSeen: string[];
  openedCount: number;
  firstOpenedAt: string | null;
  lastEventAt: string | null;
};

/** The events the client is allowed to report. All of them are reading signals. */
export type LearningEventKind =
  | "unit_opened"
  | "dwell"
  | "explanation_level"
  | "self_check_revealed"
  | "exercise_viewed";

export type NextAction = {
  kind: "read" | "practice" | "assess" | "apply" | "setup" | "blocked";
  verb: string;
  object: string;
  minutes: number | null;
  /** One sentence the learner can argue with. Always rendered. */
  reason: string;
  rule: string;
  href: string;
  unit: UnitRef | null;
  mastery: Mastery | null;
};

export type Today = {
  next: NextAction;
  pathSlug: string | null;
  pathTitle: string | null;
  waypointCount: number;
  waypointsStarted: number;
  waypointsIntroduced: number;
  reviews: NextAction[];
  /** Why the review queue is empty, so an empty list is never left unexplained. */
  reviewsNote: string;
  onboarded: boolean;
};

export type Profile = {
  targetPathId: string | null;
  targetPathSlug: string | null;
  targetPathTitle: string | null;
  hoursPerWeek: number | null;
  modalities: string[];
  experience: string | null;
  goals: string | null;
  onboardedAt: string | null;
};

export type Note = {
  id: string;
  unitId: string | null;
  unitSlug: string | null;
  unitTitle: string | null;
  body: string;
  createdAt: string;
};

export type RoadmapStop = {
  sort: number;
  requiredMasteryState: string | null;
  requirementMet: boolean;
  unit: UnitRef;
  mastery: Mastery;
};

export type Roadmap = {
  pathSlug: string;
  pathTitle: string;
  pathVersion: string;
  estimateMinutes: number;
  stops: RoadmapStop[];
};

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(path, { credentials: "include", ...init });
  if (res.status === 204) return undefined as T;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    const message = body?.error?.message ?? `Request failed (${res.status})`;
    throw new ApiError(res.status, message, body?.error?.code);
  }
  return body as T;
}
