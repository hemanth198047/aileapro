"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { api, csrfHeaders, type Me } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    try {
      await api<Me>("/v1/auth/login", {
        method: "POST",
        headers: await csrfHeaders(),
        body: JSON.stringify({ email, password }),
      });
      router.push("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={onSubmit}>
        <p className="kicker">AI Mastery</p>
        <h1>Sign in</h1>
        <p>Continue your path. This is not a video LMS.</p>
        <label htmlFor="email">Email</label>
        <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label htmlFor="password">Password</label>
        <input id="password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <button className="primary" type="submit">Enter</button>
        <p className="error">{error}</p>
        <p>
          New here? <Link href="/register">Create an account</Link>
        </p>
      </form>
    </div>
  );
}
