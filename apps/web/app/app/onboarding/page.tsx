"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Button, Callout, ErrorAlert, IconArrowRight, IconSpark } from "@aileapro/ui";
import {
  api,
  csrfHeaders,
  type PathSummary,
  type Profile,
} from "@/lib/api";

/**
 * Onboarding: a goal, an amount of time, and how you like things explained.
 *
 * <p>Three questions, on one page, because a five-step wizard for three fields is theatre.
 * Nothing asked here is treated as knowledge — the experience question aims the reading
 * order and nothing else, and the page says so rather than letting a learner think they have
 * claimed credit for linear algebra.
 */

const MODALITIES = [
  { id: "visual", label: "Diagrams", note: "Pictures before symbols" },
  { id: "code", label: "Code", note: "Show me the implementation" },
  { id: "math", label: "Maths", note: "Give me the derivation" },
  { id: "analogy", label: "Analogies", note: "Relate it to something I know" },
  { id: "text", label: "Prose", note: "Just explain it carefully" },
];

const EXPERIENCE = [
  { id: "none", label: "New to programming" },
  { id: "some_programming", label: "I can write some code" },
  { id: "professional_developer", label: "I develop software professionally" },
  { id: "some_ml", label: "I have touched machine learning" },
  { id: "working_in_ml", label: "I work in ML already" },
];

const HOURS = [2, 4, 6, 10, 15, 20];

export default function OnboardingPage() {
  const router = useRouter();
  const [paths, setPaths] = useState<PathSummary[] | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [targetPath, setTargetPath] = useState("");
  const [hours, setHours] = useState<number | null>(null);
  const [modalities, setModalities] = useState<string[]>([]);
  const [experience, setExperience] = useState("");
  const [goals, setGoals] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [published, existing] = await Promise.all([
          api<PathSummary[]>("/v1/paths"),
          api<Profile | null>("/v1/me/profile"),
        ]);
        if (cancelled) return;
        setPaths(published);
        setProfile(existing);
        // Returning learners edit what they already chose rather than starting from blank.
        setTargetPath(existing?.targetPathSlug ?? published[0]?.slug ?? "");
        setHours(existing?.hoursPerWeek ?? null);
        setModalities(existing?.modalities ?? []);
        setExperience(existing?.experience ?? "");
        setGoals(existing?.goals ?? "");
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Could not load the paths");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const ready = targetPath !== "" && hours !== null;

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      await api<Profile>("/v1/me/profile", {
        method: "PUT",
        headers: await csrfHeaders(),
        body: JSON.stringify({
          targetPath,
          hoursPerWeek: hours,
          modalities,
          experience: experience || null,
          goals: goals.trim() || null,
        }),
      });
      router.push("/app/today");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save your profile");
      setSaving(false);
    }
  };

  return (
    <>
      <header className="space__head">
        <div className="space__titles">
          <p className="kicker">Setup</p>
          <h1>{profile?.onboardedAt ? "Your goal" : "What are you aiming at?"}</h1>
          <p className="lede">
            Three answers, and they only aim the recommendation. None of this counts as evidence
            of what you know — that has to be earned unit by unit.
          </p>
        </div>
        <div className="space__meta">
          <span className="badge">Takes a minute</span>
        </div>
      </header>

      {error ? <ErrorAlert message={error} /> : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">1 · The path</span>
          {paths ? <span className="muted mono">{paths.length} published</span> : null}
        </div>
        {paths === null ? (
          <p className="muted">Loading the published paths…</p>
        ) : paths.length === 0 ? (
          <div className="empty">
            <strong>No paths are published yet.</strong>
            <span>Without one there is nothing to aim at, so recommendations stay off.</span>
          </div>
        ) : (
          <div className="pickers">
            {paths.map((path) => (
              <label className="picker" key={path.id} data-checked={targetPath === path.slug}>
                <input
                  type="radio"
                  name="path"
                  value={path.slug}
                  checked={targetPath === path.slug}
                  onChange={() => setTargetPath(path.slug)}
                />
                <span className="picker__body">
                  <strong>{path.title}</strong>
                  <span className="muted">
                    {path.waypointCount} units · about {Math.round(path.estimateMinutes / 60)} h of
                    reading
                  </span>
                  {path.description ? <span className="picker__note">{path.description}</span> : null}
                </span>
              </label>
            ))}
          </div>
        )}
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">2 · Hours a week</span>
        </div>
        <p className="hint">
          Used to size the estimate on your next action, not to set a deadline you will be
          nagged about.
        </p>
        <div className="chipset">
          {HOURS.map((option) => (
            <button
              type="button"
              className="chipbtn"
              key={option}
              data-checked={hours === option}
              aria-pressed={hours === option}
              onClick={() => setHours(option)}
            >
              {option} h
            </button>
          ))}
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">3 · How you like things explained</span>
          <span className="muted mono">Optional</span>
        </div>
        <div className="chipset">
          {MODALITIES.map((modality) => {
            const checked = modalities.includes(modality.id);
            return (
              <button
                type="button"
                className="chipbtn"
                key={modality.id}
                data-checked={checked}
                aria-pressed={checked}
                title={modality.note}
                onClick={() =>
                  setModalities((current) =>
                    checked
                      ? current.filter((id) => id !== modality.id)
                      : [...current, modality.id],
                  )
                }
              >
                {modality.label}
              </button>
            );
          })}
        </div>

        <div className="field" style={{ marginTop: 20 }}>
          <label className="field__label" htmlFor="experience">
            Where you are starting from
          </label>
          <select
            id="experience"
            className="input"
            value={experience}
            onChange={(event) => setExperience(event.target.value)}
          >
            <option value="">Prefer not to say</option>
            {EXPERIENCE.map((option) => (
              <option key={option.id} value={option.id}>
                {option.label}
              </option>
            ))}
          </select>
          <p className="hint">
            This orders what you are offered first. It does not fill a single evidence chip, and
            it cannot: you have not shown anything yet.
          </p>
        </div>

        <div className="field">
          <label className="field__label" htmlFor="goals">
            What you want out of this
          </label>
          <textarea
            id="goals"
            className="input"
            rows={3}
            value={goals}
            placeholder="For example: understand transformers well enough to read the papers."
            onChange={(event) => setGoals(event.target.value)}
          />
        </div>
      </section>

      <section className="card">
        <Callout icon={<IconSpark />}>
          Your answers are stored against your account and only used to rank what to do next.
          You can change them here at any time.
        </Callout>
        <div className="space__actions" style={{ marginTop: 16 }}>
          <Button variant="primary" onClick={save} disabled={!ready || saving}>
            {saving ? "Saving…" : "Save and see what is next"}
            <IconArrowRight />
          </Button>
          <Link className="btn btn--outline" href="/app/today">
            Back to Today
          </Link>
        </div>
        {!ready ? (
          <p className="hint" style={{ marginTop: 12 }}>
            A path and an hours figure are needed before a recommendation can mean anything.
          </p>
        ) : null}
      </section>
    </>
  );
}
