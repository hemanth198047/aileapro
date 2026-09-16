export async function csrfHeaders(): Promise<HeadersInit> {
  await fetch("/v1/auth/csrf", { credentials: "include" });
  const token = readCookie("XSRF-TOKEN");
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) headers["X-XSRF-TOKEN"] = decodeURIComponent(token);
  return headers;
}

export function readCookie(name: string): string | undefined {
  const parts = document.cookie.split(";").map((p) => p.trim());
  const hit = parts.find((p) => p.startsWith(`${name}=`));
  return hit?.slice(name.length + 1);
}

export type Me = {
  id: string;
  email: string;
  emailVerified: boolean;
  roles: string[];
};

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(path, { credentials: "include", ...init });
  if (res.status === 204) return undefined as T;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    const message = body?.error?.message ?? `Request failed (${res.status})`;
    throw new Error(message);
  }
  return body as T;
}
