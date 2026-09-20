"use client";

import { useState } from "react";
import { Button, Callout, ErrorAlert, Field } from "@aileapro/ui";
import { api, csrfHeaders } from "@/lib/api";
import { useSession } from "@/components/session";

/**
 * Email delivery is not wired yet, so the verification token is printed in the
 * API log. The form itself is real: it posts to /v1/auth/verify-email.
 */
export function VerifyEmailForm() {
  const { refresh } = useSession();
  const [token, setToken] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api("/v1/auth/verify-email", {
        method: "POST",
        headers: await csrfHeaders(),
        body: JSON.stringify({ token: token.trim() }),
      });
      setDone(true);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Verification failed");
    } finally {
      setBusy(false);
    }
  }

  if (done) {
    return <Callout tone="success">Email verified. Thank you.</Callout>;
  }

  return (
    <form className="stack" onSubmit={onSubmit} noValidate>
      <Field
        label="Verification token"
        value={token}
        onChange={(e) => setToken(e.target.value)}
        placeholder="Paste the token from the API log"
        hint="Email sending is not configured yet, so the API prints your token to its log on registration."
        required
      />
      <ErrorAlert message={error} />
      <div>
        <Button type="submit" variant="primary" loading={busy} disabled={!token.trim()}>
          {busy ? "Verifying…" : "Verify email"}
        </Button>
      </div>
    </form>
  );
}
