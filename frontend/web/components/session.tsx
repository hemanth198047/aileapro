"use client";

import { createContext, useContext } from "react";
import type { Me } from "@/lib/api";

export type Session = {
  me: Me;
  isAdmin: boolean;
  signOut: () => void;
  /** Refetches /v1/me, e.g. after email verification changes the account state. */
  refresh: () => Promise<void>;
};

const SessionContext = createContext<Session | null>(null);

export function SessionProvider({
  value,
  children,
}: {
  value: Session;
  children: React.ReactNode;
}) {
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

/** Only valid inside /app, where the shell layout has already loaded the session. */
export function useSession(): Session {
  const session = useContext(SessionContext);
  if (!session) throw new Error("useSession must be used inside the /app shell");
  return session;
}
