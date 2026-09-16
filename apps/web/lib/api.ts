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

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(path, { credentials: "include", ...init });
  if (res.status === 204) return undefined as T;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    const message = body?.error?.message ?? `Request failed (${res.status})`;
    throw new ApiError(res.status, message, body?.error?.code);
  }
  return body as T;
}
