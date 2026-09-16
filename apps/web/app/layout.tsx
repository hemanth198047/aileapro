import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "AILEPRO",
    template: "%s · AILEPRO",
  },
  description: "AILEPRO — the AI university, lab, and personal tutor for practitioners.",
  applicationName: "AILEPRO",
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: dark)", color: "#0a0c11" },
    { media: "(prefers-color-scheme: light)", color: "#f6f7fb" },
  ],
};

// Applies the stored theme before first paint so there is no flash of the wrong surface.
const themeScript = `(function(){try{var t=localStorage.getItem('ailepro-theme');if(t!=='light'&&t!=='dark'){t=window.matchMedia('(prefers-color-scheme: light)').matches?'light':'dark';}document.documentElement.dataset.theme=t;}catch(e){document.documentElement.dataset.theme='dark';}})();`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" data-theme="dark" suppressHydrationWarning>
      <body>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
        {children}
      </body>
    </html>
  );
}
