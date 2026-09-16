"use client";

export type TabItem = {
  id: string;
  label: string;
  /** Disabled tabs still show, so the learner can see the mode exists. */
  disabled?: boolean;
  title?: string;
};

export function Tabs({
  items,
  value,
  onChange,
  label,
}: {
  items: TabItem[];
  value: string;
  onChange: (id: string) => void;
  label: string;
}) {
  return (
    <div className="tabs" role="tablist" aria-label={label}>
      {items.map((item) => (
        <button
          key={item.id}
          type="button"
          role="tab"
          className="tab"
          aria-selected={item.id === value}
          disabled={item.disabled}
          title={item.title}
          onClick={() => onChange(item.id)}
        >
          {item.label}
        </button>
      ))}
    </div>
  );
}
