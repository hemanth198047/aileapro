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

/** Assessment reads. Mirrors AssessmentViews on the API. */
export type AssessmentSummary = {
  id: string;
  slug: string;
  title: string;
  kind: "practice" | "quiz" | "exam" | "diagnostic";
  description: string | null;
  timeLimitSeconds: number | null;
  passMark: number;
  itemCount: number;
  unit: UnitRef | null;
  /** An attempt already in progress, which starting again resumes rather than replaces. */
  openAttemptId: string | null;
};

export type UnitAssessments = {
  unit: UnitRef;
  practice: AssessmentSummary | null;
  quiz: AssessmentSummary | null;
  /** The highest state this unit's authored items can support, stated rather than implied. */
  ceiling: MasteryState;
  ceilingReason: string;
  /** The one thing standing between the learner and their next rung on this unit. */
  stateNote: string;
  mastery: Mastery;
};

export type ItemType = "mcq" | "multi_select" | "true_false" | "short_text" | "coding_stub";

/** A choice as served. There is no `correct` field: the key does not travel with the item. */
export type Choice = { id: string; text: string };

export type ServedItem = {
  id: string;
  type: ItemType;
  stem: string;
  choices: Choice[];
  difficulty: number;
  bloom: string;
  estimateSeconds: number | null;
  sort: number;
  answered: boolean;
  unit: UnitRef | null;
};

/** A named false belief, with the page that argues against it. */
export type Misconception = {
  code: string;
  claim: string;
  unit: UnitRef | null;
  href: string | null;
};

/** The mark for one item, returned the instant it is answered. */
export type Marked = {
  itemId: string;
  /** Null when the system cannot mark it, which is not the same as wrong. */
  correct: boolean | null;
  unverified: boolean;
  fraction: number;
  correctIds: string[];
  explanation: string;
  raised: Misconception[];
  answeredCount: number;
  servedCount: number;
  complete: boolean;
};

export type AttemptState = {
  id: string;
  kind: AssessmentSummary["kind"];
  status: "in_progress" | "submitted" | "abandoned" | "expired";
  assessmentSlug: string;
  assessmentTitle: string;
  unit: UnitRef | null;
  startedAt: string;
  timeLimitSeconds: number | null;
  secondsRemaining: number | null;
  answeredCount: number;
  servedCount: number;
  /** Null while adaptive: the diagnostic does not know how many it will ask. */
  remaining: number | null;
  nextItem: ServedItem | null;
};

export type ReportLine = { label: string; detail: string; href: string | null };

export type Report = {
  attemptId: string;
  kind: AssessmentSummary["kind"];
  assessmentTitle: string;
  unit: UnitRef | null;
  submittedAt: string;
  fraction: number;
  passMark: number;
  passed: boolean | null;
  answered: number;
  correct: number;
  unmarked: number;
  summary: string;
  readiness: string;
  readinessReason: string;
  strengths: ReportLine[];
  weaknesses: ReportLine[];
  misconceptions: Misconception[];
  missingPrereqs: ReportLine[];
  revision: ReportLine[];
  nextSteps: ReportLine[];
  masteryState: MasteryState;
  masteryNote: string;
};

/** A card waiting to be recalled. The back is withheld until a grade is committed. */
export type DueCard = {
  scheduleId: string;
  cardId: string;
  front: string;
  unit: UnitRef | null;
  due: string;
  daysOverdue: number;
  intervalDays: number;
  reps: number;
  lapses: number;
};

export type ReviewQueue = {
  dueCount: number;
  overdueCount: number;
  leechCount: number;
  cards: DueCard[];
  note: string;
};

export type ReviewResult = {
  scheduleId: string;
  cardId: string;
  back: string;
  grade: number;
  passed: boolean;
  wasDue: boolean;
  intervalBefore: number;
  intervalAfter: number;
  nextDue: string;
  ease: number;
  leech: boolean;
  note: string;
  remaining: number;
};

/** The diagnostic's placement guess for one domain, with how thin the evidence was. */
export type Prior = {
  domain: UnitRef;
  itemsServed: number;
  itemsCorrect: number;
  estimate: number;
  confidence: "low" | "medium" | "high";
  reason: string;
};

export type PlanLine = {
  kind: string;
  unit: UnitRef | null;
  reason: string;
  due: string | null;
  href: string | null;
};

export type StudyPlan = {
  id: string;
  version: number;
  horizon: string;
  reason: string;
  source: string;
  generatedAt: string;
  priors: Prior[];
  items: PlanLine[];
};

export type Roadmap = {
  pathSlug: string;
  pathTitle: string;
  pathVersion: string;
  estimateMinutes: number;
  stops: RoadmapStop[];
};

/** Tutor reads. Mirrors TutorViews on the API. */
export type TutorMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
  action: string | null;
  /** True when a fixture wrote it. The UI is required to say so, never to hide it. */
  development: boolean;
  refusalCode: string | null;
  sources: string[];
  model: string | null;
  promptVersion: number | null;
  createdAt: string;
};

export type TutorConversation = {
  id: string;
  unitSlug: string | null;
  unitTitle: string | null;
  persona: string;
  title: string | null;
  messages: TutorMessage[];
};

export type TutorConversationSummary = Omit<TutorConversation, "messages"> & {
  messageCount: number;
  createdAt: string;
  lastMessageAt: string | null;
};

export type TutorStatus = {
  available: boolean;
  development: boolean;
  retrieval: boolean;
  providerSlug: string | null;
  modelSlug: string | null;
  usdSpent: number;
  usdCap: number;
  turnsUsed: number;
  turnsCap: number;
  actions: { slug: string; label: string; hint: string }[];
  personas: { slug: string; title: string; hint: string }[];
};

export type TutorContext = {
  unitSlug: string | null;
  unitTitle: string | null;
  masteryState: string;
  readSolid: boolean;
  openMisconceptions: string[];
  recentMistakes: string[];
};

export type TutorSourceBlock = {
  blockId: string;
  unitSlug: string;
  unitTitle: string;
  blockType: string;
  knowledgeClass: string;
  updatedAt: string | null;
  href: string;
};

export type TutorSources = {
  retrieval: boolean;
  unitInContext: string;
  blocks: TutorSourceBlock[];
  note: string;
};

export type SearchHit = {
  type: "unit" | "block" | "path";
  id: string;
  slug: string;
  title: string;
  snippet: string;
  href: string;
  knowledgeClass: string;
  updatedAt: string | null;
  score: number;
  unitId: string | null;
  unitSlug: string | null;
  blockType: string | null;
};

export type SearchPage = {
  query: string;
  results: SearchHit[];
  types: string[];
  total: number;
  note: string;
};

/** Lab runs. Mirrors LabViews on the API. Source never executes in the browser or the API JVM. */
export type LabRun = {
  id: string;
  language: string;
  status: "queued" | "running" | "succeeded" | "failed" | "timed_out" | "cancelled";
  source: string;
  sourceHash: string;
  stdin: string | null;
  stdout: string | null;
  stderr: string | null;
  exitCode: number | null;
  durationMs: number | null;
  timedOut: boolean;
  cancelled: boolean;
  development: boolean;
  executorKind: string;
  errorCode: string | null;
  errorMessage: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
};

export type LabHistory = {
  runs: LabRun[];
  note: string;
  remainingToday: number;
  dailyCap: number;
};

export type LabStatus = {
  available: boolean;
  development: boolean;
  executorKind: string;
  remainingToday: number;
  dailyCap: number;
  demoSource: string;
};

/** Events the turn endpoint sends, in the order they arrive. */
export type TutorStreamEvent =
  | { type: "token"; text: string }
  | { type: "done"; messageId: string; development: boolean; refusalCode: string }
  | { type: "error"; code: string; message: string };

/**
 * Sends one turn and yields events as they arrive.
 *
 * <p>Server-sent events read off a POST response rather than through `EventSource`, which
 * cannot send a body or a CSRF header and would put the learner's question in a URL. The
 * parsing is hand-rolled for the same reason: the format is four lines, and a dependency to
 * read four lines is a dependency to keep updated forever.
 */
export async function tutorTurn(
  conversationId: string,
  body: { text?: string; action?: string },
  onEvent: (event: TutorStreamEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const res = await fetch(`/v1/tutor/conversations/${conversationId}/messages`, {
    method: "POST",
    credentials: "include",
    headers: await csrfHeaders(),
    body: JSON.stringify(body),
    signal,
  });

  if (!res.ok || !res.body) {
    const failed = await res.json().catch(() => ({}));
    throw new ApiError(
      res.status,
      failed?.error?.message ?? `The tutor could not be reached (${res.status})`,
      failed?.error?.code,
    );
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    // Events are separated by a blank line. Anything after the last one is a partial event
    // and stays in the buffer until the rest of it arrives.
    const parts = buffer.split("\n\n");
    buffer = parts.pop() ?? "";
    for (const part of parts) {
      const event = parseEvent(part);
      if (event) onEvent(event);
    }
  }
}

function parseEvent(raw: string): TutorStreamEvent | null {
  let name = "";
  const data: string[] = [];
  for (const line of raw.split("\n")) {
    if (line.startsWith("event:")) name = line.slice(6).trim();
    if (line.startsWith("data:")) data.push(line.slice(5).trim());
  }
  if (!name || data.length === 0) return null;

  try {
    const payload = JSON.parse(data.join("\n"));
    if (name === "token") return { type: "token", text: payload.text ?? "" };
    if (name === "done") {
      return {
        type: "done",
        messageId: payload.messageId,
        development: Boolean(payload.development),
        refusalCode: payload.refusalCode ?? "",
      };
    }
    if (name === "error") {
      return { type: "error", code: payload.code ?? "internal_error", message: payload.message ?? "" };
    }
  } catch {
    // A half-written event is not worth surfacing: the next read completes it, and a parse
    // error here would look to the learner like the tutor failed when it did not.
    return null;
  }
  return null;
}

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

export function searchCurriculum(q: string, types?: string[]): Promise<SearchPage> {
  const params = new URLSearchParams();
  params.set("q", q);
  if (types?.length) params.set("types", types.join(","));
  return api<SearchPage>(`/v1/search?${params}`);
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
