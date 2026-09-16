import { ReactNode } from "react";
import { IconAlert } from "./icons";

type Tone = "neutral" | "danger" | "success" | "warn";

export function Callout({
  tone = "neutral",
  icon,
  children,
  role,
}: {
  tone?: Tone;
  icon?: ReactNode;
  children: ReactNode;
  role?: "alert" | "status";
}) {
  return (
    <p className={tone === "neutral" ? "callout" : `callout callout--${tone}`} role={role}>
      {icon ?? null}
      <span>{children}</span>
    </p>
  );
}

export function ErrorAlert({ message }: { message: string }) {
  if (!message) return null;
  return (
    <Callout tone="danger" role="alert" icon={<IconAlert />}>
      {message}
    </Callout>
  );
}
