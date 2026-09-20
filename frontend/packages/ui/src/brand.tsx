export function BrandMark({ size = 30 }: { size?: number }) {
  return (
    <span className="brand__mark" style={{ width: size, height: size }} aria-hidden>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinejoin="round">
        <path d="M12 3 4 20h4l4-9 4 9h4z" strokeLinecap="round" />
        <path d="M9.4 15h5.2" strokeLinecap="round" />
      </svg>
    </span>
  );
}

export function Brand({ large = false }: { large?: boolean }) {
  return (
    <span className={large ? "brand brand--lg" : "brand"}>
      <BrandMark size={large ? 40 : 30} />
      <span className="brand__word">AILEAPRO</span>
    </span>
  );
}
