"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { api, csrfHeaders, type Me } from "@/lib/api";
import { AuthLayout } from "@/components/auth-layout";
import { Brand } from "@/components/brand";
import { Button, ErrorAlert, Field, PasswordField } from "@/components/ui";

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await api<Me>("/v1/auth/register", {
        method: "POST",
        headers: await csrfHeaders(),
        body: JSON.stringify({ email, password }),
      });
      router.push("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not register");
      setBusy(false);
    }
  }

  return (
    <AuthLayout>
      <form className="auth__form" onSubmit={onSubmit} noValidate>
        <div className="auth__mobile-brand">
          <Brand />
        </div>
        <div className="auth__head">
          <h2>Create your account</h2>
          <p className="muted">Start at foundations. Mastery is earned, not clicked.</p>
        </div>
        <Field
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoFocus
        />
        <PasswordField
          label="Password"
          autoComplete="new-password"
          placeholder="At least 8 characters"
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          strength
        />
        <ErrorAlert message={error} />
        <Button type="submit" variant="primary" block loading={busy}>
          {busy ? "Creating account…" : "Create account"}
        </Button>
        <p className="auth__legal">
          Your first path is generated after a short placement. You can change pace or depth at
          any time.
        </p>
        <p className="auth__foot">
          Already enrolled? <Link href="/login">Sign in</Link>
        </p>
      </form>
    </AuthLayout>
  );
}
