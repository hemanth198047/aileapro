import { ReactNode } from "react";

/** Two-up work surface (explanation + practice, editor + output). Stacks under 1024px. */
export function SplitPane({
  orientation = "horizontal",
  start,
  end,
}: {
  orientation?: "horizontal" | "vertical";
  start: ReactNode;
  end: ReactNode;
}) {
  return (
    <div className="split" data-orientation={orientation}>
      <div>{start}</div>
      <div>{end}</div>
    </div>
  );
}
