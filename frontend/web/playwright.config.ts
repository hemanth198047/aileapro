/**
 * Playwright P0 smoke — public pages always; authenticated flows when API is up.
 *
 *   cd frontend/web && pnpm exec playwright test
 *
 * Env:
 *   PLAYWRIGHT_BASE_URL  (default http://localhost:3000)
 *   AILEAPRO_API_ORIGIN  (default http://localhost:8090) — enables API-backed cases
 */
import { defineConfig, devices } from "@playwright/test";

const port = 3000;
const baseURL = process.env.PLAYWRIGHT_BASE_URL || `http://127.0.0.1:${port}`;

export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"]],
  use: {
    baseURL,
    trace: "on-first-retry",
  },
  webServer: {
    command: "pnpm start -- --port " + port,
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
