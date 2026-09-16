"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { api, csrfHeaders, type Me } from "@/lib/api";

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    try {
      await api<Me>("/v1/auth/register", {
        method: "POST",
        headers: await csrfHeaders(),
        body: JSON.stringify({ email, password }),
      });
      router.push("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not register");
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={onSubmit}>
        <p className="kicker">AI Mastery</p>
        <h1>Create account</h1>
        <p>Start with foundations. Mastery is earned, not clicked.</p>
        <label htmlFor="email">Email</label>
        <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label htmlFor="password">Password (min 8)</label>
        <input id="password" type="password" autoComplete="new-password" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} required />
        <button className="primary" type="submit">Register</button>
        <p className="error">{error}</p>
        <p>
          Already enrolled? <Link href="/login">Sign in</Link>
        </p>
      </form>
    </div>
  );
}
