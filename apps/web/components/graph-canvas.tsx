"use client";

import Link from "next/link";
import type { GraphEdge, GraphNode, Neighborhood, UnitRef } from "@/lib/api";

/**
 * The neighbourhood drawn as rings around the focus unit: distance from the centre is hops,
 * nothing more. Node colour deliberately carries no meaning while mastery is unmeasured —
 * a green node would be a lie until Phase 5 records evidence.
 *
 * The canvas is decoration over the adjacency list, not a replacement for it. Every node here
 * is also a row in {@link GraphList}, which is what keyboard and screen reader users get, and
 * which stays accurate no matter how the layout behaves.
 */

const VIEW = { width: 900, height: 460 };
const CENTRE = { x: VIEW.width / 2, y: VIEW.height / 2 };
const NODE = { width: 152, height: 46 };
const RINGS = [
  { rx: 0, ry: 0 },
  { rx: 205, ry: 105 },
  { rx: 315, ry: 155 },
  { rx: 372, ry: 188 },
];

type Placed = GraphNode & { x: number; y: number };

function place(nodes: GraphNode[]): Placed[] {
  const byHop = new Map<number, GraphNode[]>();
  for (const node of nodes) {
    const ring = byHop.get(node.hops) ?? [];
    ring.push(node);
    byHop.set(node.hops, ring);
  }

  const placed: Placed[] = [];
  for (const [hops, ring] of [...byHop.entries()].sort((a, b) => a[0] - b[0])) {
    const geometry = RINGS[Math.min(hops, RINGS.length - 1)];
    if (hops === 0) {
      placed.push({ ...ring[0], x: CENTRE.x, y: CENTRE.y });
      continue;
    }
    const step = (Math.PI * 2) / ring.length;
    // Odd rings start at the top, even rings half a step over, so nodes on different rings
    // do not line up radially and hide each other's labels.
    const offset = -Math.PI / 2 + (hops % 2 === 0 ? step / 2 : 0);
    ring.forEach((node, index) => {
      const angle = offset + step * index;
      placed.push({
        ...node,
        x: CENTRE.x + geometry.rx * Math.cos(angle),
        y: CENTRE.y + geometry.ry * Math.sin(angle),
      });
    });
  }
  return placed;
}

function edgeClass(edge: GraphEdge) {
  return `gnode-edge gnode-edge--${edge.type.toLowerCase().replace(/_/g, "-")}`;
}

/**
 * A node with neither blocks nor published children: it is mapped, but following it leads to
 * nothing to read. A domain that merely holds its units is not a dead end.
 */
export function isDeadEnd(unit: UnitRef) {
  return !unit.hasContent && unit.childCount === 0;
}

/**
 * Stops the line at the edge of each node box instead of its centre, so the arrowhead is
 * visible and direction is readable rather than hidden under a label.
 */
function trim(from: Placed, to: Placed) {
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const length = Math.hypot(dx, dy) || 1;
  const unit = { x: dx / length, y: dy / length };
  const gap = 6;
  const halfWidth = NODE.width / 2;
  const halfHeight = NODE.height / 2;
  const toBorder = Math.min(
    Math.abs(unit.x) < 1e-6 ? Infinity : halfWidth / Math.abs(unit.x),
    Math.abs(unit.y) < 1e-6 ? Infinity : halfHeight / Math.abs(unit.y),
  );
  const inset = Math.min(toBorder + gap, length / 2 - 1);
  return {
    x1: from.x + unit.x * inset,
    y1: from.y + unit.y * inset,
    x2: to.x - unit.x * inset,
    y2: to.y - unit.y * inset,
  };
}

export function GraphCanvas({ graph }: { graph: Neighborhood }) {
  const placed = place(graph.nodes);
  const byId = new Map(placed.map((node) => [node.unit.id, node]));

  return (
    <figure className="graph">
      <svg
        className="graph__svg"
        viewBox={`0 0 ${VIEW.width} ${VIEW.height}`}
        role="img"
        aria-label={`${graph.nodes.length} units within ${graph.hops} hops of ${graph.focus.title}. The same relationships are listed below.`}
      >
        <defs>
          <marker
            id="graph-arrow"
            viewBox="0 0 10 10"
            refX="9"
            refY="5"
            markerWidth="7"
            markerHeight="7"
            orient="auto-start-reverse"
          >
            <path d="M 0 0 L 10 5 L 0 10 z" />
          </marker>
        </defs>

        <g className="graph__edges">
          {graph.edges.map((edge) => {
            const from = byId.get(edge.fromId);
            const to = byId.get(edge.toId);
            if (!from || !to) return null;
            const line = trim(from, to);
            return (
              <line
                key={edge.id}
                className={edgeClass(edge)}
                x1={line.x1}
                y1={line.y1}
                x2={line.x2}
                y2={line.y2}
                markerEnd={edge.symmetric ? undefined : "url(#graph-arrow)"}
              >
                <title>{`${from.unit.title} ${edge.type.replace(/_/g, " ").toLowerCase()} ${to.unit.title}`}</title>
              </line>
            );
          })}
        </g>

        <g className="graph__nodes">
          {placed.map((node) => (
            <a
              key={node.unit.id}
              href={`/app/learn/${node.unit.slug}`}
              className={`gnode${node.hops === 0 ? " gnode--focus" : ""}${
                isDeadEnd(node.unit) ? " gnode--empty" : ""
              }`}
            >
              <title>
                {node.hops === 0
                  ? `${node.unit.title} — the unit this map is centred on`
                  : `${node.unit.title} — ${node.hops} hop${node.hops === 1 ? "" : "s"} away`}
              </title>
              <rect
                x={node.x - NODE.width / 2}
                y={node.y - NODE.height / 2}
                width={NODE.width}
                height={NODE.height}
                rx={10}
              />
              <text x={node.x} y={node.y + 4} textAnchor="middle">
                {truncate(node.unit.title, 22)}
              </text>
            </a>
          ))}
        </g>
      </svg>
      <figcaption className="graph__caption">
        Distance from the centre is hops. Colour means nothing yet: mastery is not recorded until
        Phase 5, so a coloured node would be a claim about you that no one has measured.
      </figcaption>
    </figure>
  );
}

/** The accessible and authoritative view: every node with the relationships that reach it. */
export function GraphList({ graph }: { graph: Neighborhood }) {
  const byId = new Map(graph.nodes.map((node) => [node.unit.id, node]));

  return (
    <ul className="adjacency">
      {graph.nodes.map((node) => {
        const related = graph.edges
          .filter((edge) => edge.fromId === node.unit.id || edge.toId === node.unit.id)
          .map((edge) => {
            const outgoing = edge.fromId === node.unit.id;
            const other = byId.get(outgoing ? edge.toId : edge.fromId);
            return { edge, outgoing, other };
          })
          .filter((entry) => entry.other)
          .sort((a, b) => Number(b.edge.gating) - Number(a.edge.gating));

        return (
          <li className="adjacency__item" key={node.unit.id}>
            <div className="adjacency__head">
              <Link className="adjacency__title" href={`/app/learn/${node.unit.slug}`}>
                {node.unit.title}
              </Link>
              <span className="adjacency__badges">
                <span className="badge">{node.hops === 0 ? "centre" : `${node.hops} hop`}</span>
                <span className="badge">{node.unit.kind.replace(/_/g, " ")}</span>
                {node.unit.hasContent ? null : node.unit.childCount > 0 ? (
                  <span className="badge">{node.unit.childCount} inside</span>
                ) : (
                  <span className="badge badge--warn">nothing to read yet</span>
                )}
                <span className="badge">mastery {node.masteryState}</span>
              </span>
            </div>
            {related.length > 0 ? (
              <ul className="adjacency__edges">
                {related.map(({ edge, outgoing, other }) => (
                  <li key={`${edge.id}-${node.unit.id}`}>
                    <span className={`etag etag--${edge.type.toLowerCase().replace(/_/g, "-")}`}>
                      {edgePhrase(edge.type, outgoing)}
                    </span>
                    <Link href={`/app/learn/${other!.unit.slug}`}>{other!.unit.title}</Link>
                    {edge.note ? <span className="adjacency__note">{edge.note}</span> : null}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="hint">No edges of the selected types reach this unit.</p>
            )}
          </li>
        );
      })}
    </ul>
  );
}

/** Reads the edge from this node's side, so a row is a sentence rather than a type name. */
export function edgePhrase(type: string, outgoing: boolean): string {
  switch (type) {
    case "PREREQUISITE_OF":
      return outgoing ? "must be read before" : "requires";
    case "DEPENDS_ON":
      return outgoing ? "helps with" : "helped by";
    case "RELATED_TO":
      return "reads well with";
    case "APPLIED_IN":
      return outgoing ? "is applied in" : "applies";
    case "SPECIALIZATION_OF":
      return outgoing ? "is a case of" : "generalises";
    case "ALTERNATIVE_TO":
      return "is an alternative to";
    case "EVOLUTION_OF":
      return outgoing ? "came before" : "came after";
    default:
      return type.replace(/_/g, " ").toLowerCase();
  }
}

function truncate(value: string, max: number) {
  return value.length <= max ? value : `${value.slice(0, max - 1)}…`;
}
