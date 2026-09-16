"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { api, csrfHeaders, type Me } from "@/lib/api";
import { AuthLayout } from "@/components/auth-layout";
import { Brand } from "@/components/brand";
import { Button, ErrorAlert, Field, PasswordField } from "@/components/ui";

export default function LoginPage() {
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
      await api<Me>("/v1/auth/login", {
        method: "POST",
        headers: await csrfHeaders(),
        body: JSON.stringify({ email, password }),
      });
      router.push("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
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
          <h2>Sign in</h2>
          <p className="muted">Pick up your path where you left it.</p>
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
          autoComplete="current-password"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <ErrorAlert message={error} />
        <Button type="submit" variant="primary" block loading={busy}>
          {busy ? "Signing in…" : "Sign in"}
        </Button>
        <p className="auth__foot">
          New to AILEPRO? <Link href="/register">Create an account</Link>
        </p>
      </form>
    </AuthLayout>
  );
}
