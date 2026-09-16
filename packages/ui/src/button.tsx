"use client";

import { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "primary" | "outline" | "ghost";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  block?: boolean;
  loading?: boolean;
};

export function Button({
  variant = "outline",
  block = false,
  loading = false,
  children,
  className = "",
  disabled,
  ...rest
}: ButtonProps) {
  const classes = ["btn", `btn--${variant}`, block ? "btn--block" : "", className]
    .filter(Boolean)
    .join(" ");
  return (
    <button className={classes} disabled={disabled || loading} {...rest}>
      {loading ? <span className="btn__spinner" aria-hidden /> : null}
      {children}
    </button>
  );
}

type IconButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  /** Required: icon-only controls must still have an accessible name. */
  label: string;
  icon: ReactNode;
};

export function IconButton({
  variant = "ghost",
  label,
  icon,
  className = "",
  ...rest
}: IconButtonProps) {
  return (
    <button
      className={["btn", `btn--${variant}`, "btn--icon", className].filter(Boolean).join(" ")}
      aria-label={label}
      title={label}
      {...rest}
    >
      {icon}
    </button>
  );
}
