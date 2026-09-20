type IconProps = { size?: number };

function base(size: number) {
  return {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.75,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": true,
  };
}

export function IconSearch({ size = 15 }: IconProps) {
  return (
    <svg {...base(size)}>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.2-3.2" />
    </svg>
  );
}

export function IconCompass({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <circle cx="12" cy="12" r="9" />
      <path d="m15.5 8.5-2 5-5 2 2-5z" />
    </svg>
  );
}

export function IconBook({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M5 4.5A1.5 1.5 0 0 1 6.5 3H19v15H6.5A1.5 1.5 0 0 0 5 19.5z" />
      <path d="M5 19.5A1.5 1.5 0 0 1 6.5 18H19v3H6.5A1.5 1.5 0 0 1 5 19.5z" />
    </svg>
  );
}

export function IconMap({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <circle cx="6" cy="7" r="2.2" />
      <circle cx="18" cy="9" r="2.2" />
      <circle cx="11" cy="18" r="2.2" />
      <path d="m8 7.6 7.8 1M16.9 11l-4.4 5M9.4 16.4 7 9.1" />
    </svg>
  );
}

export function IconLab({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M10 3v6.2L4.8 18A2 2 0 0 0 6.5 21h11a2 2 0 0 0 1.7-3L14 9.2V3" />
      <path d="M9 3h6" />
      <path d="M7.5 15h9" />
    </svg>
  );
}

export function IconStudio({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="m12 3 8 4.5-8 4.5-8-4.5z" />
      <path d="m4 12.5 8 4.5 8-4.5" />
      <path d="m4 17 8 4.5 8-4.5" />
    </svg>
  );
}

export function IconAssess({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <rect x="4" y="4" width="16" height="16" rx="2.5" />
      <path d="m8.5 12.2 2.3 2.3 4.7-4.8" />
    </svg>
  );
}

export function IconResearch({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M6 3h8l4 4v14H6z" />
      <path d="M14 3v4h4" />
      <path d="M9 12h6M9 16h4" />
    </svg>
  );
}

export function IconEvolve({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M3 13h4l2.5-6 4 12L16 13h5" />
    </svg>
  );
}

export function IconCareer({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <rect x="3" y="7" width="18" height="13" rx="2" />
      <path d="M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7" />
      <path d="M3 12h18" />
    </svg>
  );
}

export function IconNotes({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M5 19.5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v14.5" />
      <path d="M5 19.5A1.5 1.5 0 0 0 6.5 21H19" />
      <path d="M9 8h6M9 12h6" />
    </svg>
  );
}

export function IconSettings({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.2 14.2a1.6 1.6 0 0 0 .32 1.76l.06.06a1.9 1.9 0 1 1-2.7 2.7l-.05-.06a1.6 1.6 0 0 0-2.72 1.14v.16a1.9 1.9 0 1 1-3.8 0v-.09a1.6 1.6 0 0 0-2.78-1.06l-.06.06a1.9 1.9 0 1 1-2.7-2.7l.06-.06A1.6 1.6 0 0 0 3.9 13.4h-.16a1.9 1.9 0 0 1 0-3.8h.09a1.6 1.6 0 0 0 1.06-2.78l-.06-.06a1.9 1.9 0 1 1 2.7-2.7l.06.06a1.6 1.6 0 0 0 2.72-1.06V3a1.9 1.9 0 1 1 3.8 0v.09a1.6 1.6 0 0 0 2.78 1.06l.06-.06a1.9 1.9 0 1 1 2.7 2.7l-.06.06a1.6 1.6 0 0 0 1.06 2.72h.16a1.9 1.9 0 0 1 0 3.8h-.09a1.6 1.6 0 0 0-1.48 1z" />
    </svg>
  );
}

export function IconShield({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M12 3 5 6v6c0 4.2 2.9 7.6 7 9 4.1-1.4 7-4.8 7-9V6z" />
      <path d="m9.3 12 1.9 1.9 3.6-3.7" />
    </svg>
  );
}

export function IconSpark({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M12 3.5 13.9 9l5.6 1.9-5.6 1.9L12 18.4l-1.9-5.6L4.5 11l5.6-2z" />
    </svg>
  );
}

export function IconChart({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M4 20h16" />
      <path d="M7 20v-6M12 20V7M17 20v-9" />
    </svg>
  );
}

export function IconSun({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6 7 7M17 17l1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4" />
    </svg>
  );
}

export function IconMoon({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M20 14.2A8.2 8.2 0 0 1 9.8 4a8.4 8.4 0 1 0 10.2 10.2" />
    </svg>
  );
}

export function IconLogout({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M14 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h8" />
      <path d="M17 15.5 20.5 12 17 8.5" />
      <path d="M20 12H10" />
    </svg>
  );
}

export function IconAlert({ size = 15 }: IconProps) {
  return (
    <svg {...base(size)}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7.5v5M12 16h.01" />
    </svg>
  );
}

export function IconArrowRight({ size = 15 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M5 12h13" />
      <path d="m13 6.5 5.5 5.5L13 17.5" />
    </svg>
  );
}

export function IconPanel({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <path d="M15 4v16" />
    </svg>
  );
}

export function IconClose({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="m6 6 12 12M18 6 6 18" />
    </svg>
  );
}

export function IconCheck({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="m5 13 4.5 4.5L19 7" />
    </svg>
  );
}

export function IconRepeat({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M4 9a5 5 0 0 1 5-5h11m0 0-3-3m3 3-3 3" />
      <path d="M20 15a5 5 0 0 1-5 5H4m0 0 3 3m-3-3 3-3" />
    </svg>
  );
}
