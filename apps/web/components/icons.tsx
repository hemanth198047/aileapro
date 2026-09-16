type IconProps = { size?: number; className?: string };

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

export function IconPath({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <circle cx="6" cy="6" r="2.5" />
      <circle cx="18" cy="18" r="2.5" />
      <path d="M8.5 6H14a4 4 0 0 1 0 8H10a4 4 0 0 0 0 8h-.5" />
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

export function IconSpark({ size = 16 }: IconProps) {
  return (
    <svg {...base(size)}>
      <path d="M12 3.5 13.9 9l5.6 1.9-5.6 1.9L12 18.4l-1.9-5.6L4.5 11l5.6-2z" />
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
