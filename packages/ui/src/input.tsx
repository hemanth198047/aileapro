"use client";

import { InputHTMLAttributes, ReactNode, useId, useState } from "react";

type FieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  hint?: string;
  action?: ReactNode;
};

export function Field({ label, hint, action, id, className = "", ...rest }: FieldProps) {
  const generated = useId();
  const inputId = id ?? generated;
  const hintId = hint ? `${inputId}-hint` : undefined;
  return (
    <div className="field">
      <div className="field__top">
        <label className="label" htmlFor={inputId}>
          {label}
        </label>
        {action}
      </div>
      <div className="input-wrap">
        <input id={inputId} className={`input ${className}`} aria-describedby={hintId} {...rest} />
      </div>
      {hint ? (
        <p className="hint" id={hintId}>
          {hint}
        </p>
      ) : null}
    </div>
  );
}

type PasswordFieldProps = Omit<FieldProps, "type"> & { strength?: boolean };

export function PasswordField({
  strength = false,
  label,
  hint,
  action,
  id,
  value,
  ...rest
}: PasswordFieldProps) {
  const generated = useId();
  const inputId = id ?? generated;
  const [visible, setVisible] = useState(false);
  const score = scorePassword(String(value ?? ""));

  return (
    <div className="field">
      <div className="field__top">
        <label className="label" htmlFor={inputId}>
          {label}
        </label>
        {action}
      </div>
      <div className="input-wrap">
        <input
          {...rest}
          id={inputId}
          value={value}
          type={visible ? "text" : "password"}
          className="input input--with-action"
        />
        <button
          type="button"
          className="input-action"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "Hide password" : "Show password"}
        >
          {visible ? "Hide" : "Show"}
        </button>
      </div>
      {strength ? (
        <>
          <div className="meter" aria-hidden>
            {[0, 1, 2, 3].map((i) => (
              <span key={i} className="meter__bar" data-on={i < score.bars} data-level={score.level} />
            ))}
          </div>
          <p className="hint">{score.label}</p>
        </>
      ) : hint ? (
        <p className="hint">{hint}</p>
      ) : null}
    </div>
  );
}

function scorePassword(value: string) {
  if (!value) return { bars: 0, level: "weak" as const, label: "Use at least 8 characters." };
  let points = 0;
  if (value.length >= 8) points += 1;
  if (value.length >= 14) points += 1;
  if (/[^A-Za-z0-9]/.test(value)) points += 1;
  if (/[A-Z]/.test(value) && /[0-9]/.test(value)) points += 1;
  if (points <= 1) return { bars: 1, level: "weak" as const, label: "Weak — add length and variety." };
  if (points === 2) return { bars: 2, level: "fair" as const, label: "Fair — a longer passphrase is safer." };
  if (points === 3) return { bars: 3, level: "fair" as const, label: "Good." };
  return { bars: 4, level: "strong" as const, label: "Strong." };
}
