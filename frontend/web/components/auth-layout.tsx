import { ReactNode } from "react";
import { Brand, IconChart, IconLab, IconMap, IconShield } from "@aileapro/ui";

const points = [
  {
    icon: <IconMap size={14} />,
    title: "A path, not a playlist",
    body: "The curriculum adapts to what you have actually proven you can do.",
  },
  {
    icon: <IconLab size={14} />,
    title: "Build in a real lab",
    body: "Every concept lands in a sandboxed environment where your code runs.",
  },
  {
    icon: <IconChart size={14} />,
    title: "Mastery you can evidence",
    body: "Skills are scored from work you ship, then reviewed on a spaced schedule.",
  },
  {
    icon: <IconShield size={14} />,
    title: "Current by design",
    body: "The intelligence engine tracks the field and refreshes material as it moves.",
  },
];

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth">
      <aside className="auth__aside">
        <Brand large />
        <div className="auth__body">
          <div className="auth__pitch">
            <p className="kicker">AI university · lab · tutor</p>
            <h1>Go from curious to world-class.</h1>
            <p className="lede">
              AILEAPRO teaches the way a great mentor would: diagnose, explain, make you build it,
              then come back when you are about to forget.
            </p>
          </div>
          <ul className="auth__points">
            {points.map((point) => (
              <li className="auth__point" key={point.title}>
                <span className="auth__point-icon">{point.icon}</span>
                <span>
                  <span className="auth__point-title">{point.title}</span>
                  <br />
                  <span className="auth__point-body">{point.body}</span>
                </span>
              </li>
            ))}
          </ul>
        </div>
      </aside>
      <main className="auth__panel">{children}</main>
    </div>
  );
}
