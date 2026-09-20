import { expect, test, type APIRequestContext, type Page } from "@playwright/test";

const API = process.env.AILEAPRO_API_ORIGIN || "http://localhost:8090";

async function apiHealthy(request: APIRequestContext): Promise<boolean> {
  try {
    const res = await request.get(`${API}/actuator/health`, { timeout: 3_000 });
    return res.ok();
  } catch {
    return false;
  }
}

async function registerViaApi(request: APIRequestContext, email: string, password: string) {
  await request.get(`${API}/v1/auth/csrf`);
  const csrf = (await request.storageState()).cookies.find((c) => c.name === "XSRF-TOKEN")?.value;
  const res = await request.post(`${API}/v1/auth/register`, {
    headers: {
      "Content-Type": "application/json",
      ...(csrf ? { "X-XSRF-TOKEN": decodeURIComponent(csrf) } : {}),
    },
    data: { email, password },
  });
  expect(res.ok(), `register failed: ${res.status()} ${await res.text()}`).toBeTruthy();
}

async function loginUi(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: /sign in/i }).click();
  await page.waitForURL(/\/app/);
}

test.describe("P0 public surfaces", () => {
  test("privacy policy is live and substantive", async ({ page }) => {
    await page.goto("/privacy");
    await expect(page.getByRole("heading", { name: "Privacy Policy" })).toBeVisible();
    await expect(page.getByText(/export/i).first()).toBeVisible();
    await expect(page.getByText(/Last updated/i)).toBeVisible();
  });

  test("terms of service are live and substantive", async ({ page }) => {
    await page.goto("/terms");
    await expect(page.getByRole("heading", { name: "Terms of Service" })).toBeVisible();
    await expect(page.getByText(/Acceptable use/i)).toBeVisible();
  });

  test("register and login pages render", async ({ page }) => {
    await page.goto("/register");
    await expect(page.getByRole("heading", { name: /create|register|sign up/i })).toBeVisible();
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: /sign in/i })).toBeVisible();
  });
});

test.describe("P0 authenticated learner loop", () => {
  test.beforeEach(async ({ request }, testInfo) => {
    if (!(await apiHealthy(request))) {
      testInfo.skip(true, "API not reachable — start stack with start.bat / start.sh");
    }
  });

  test("1–2 register/login and reach the app shell", async ({ page, request }) => {
    const email = `e2e-${Date.now()}@aileapro.dev`;
    const password = "Correct-Horse-9!";
    await registerViaApi(request, email, password);
    await loginUi(page, email, password);
    await expect(page).toHaveURL(/\/app/);
  });

  test("3–6 onboarding, learn, map routes exist", async ({ page, request }) => {
    const email = `e2e-nav-${Date.now()}@aileapro.dev`;
    await registerViaApi(request, email, "Correct-Horse-9!");
    await loginUi(page, email, "Correct-Horse-9!");

    for (const path of [
      "/app/onboarding",
      "/app/diagnostic",
      "/app/learn",
      "/app/map",
      "/app/today",
    ]) {
      await page.goto(path);
      await expect(page.locator("body")).not.toContainText("Internal Server Error");
      await expect(page).toHaveURL(new RegExp(path.replace("/", "\\/")));
    }
  });

  test("7–12 learn unit, assess, today, review surfaces", async ({ page, request }) => {
    const email = `e2e-learn-${Date.now()}@aileapro.dev`;
    await registerViaApi(request, email, "Correct-Horse-9!");
    await loginUi(page, email, "Correct-Horse-9!");

    await page.goto("/app/learn/matrix-multiplication");
    await expect(page.getByText(/Matrix Multiplication/i).first()).toBeVisible({ timeout: 15_000 });

    await page.goto("/app/assess");
    await expect(page.locator("body")).not.toContainText("Internal Server Error");

    await page.goto("/app/today");
    await expect(page.locator("body")).not.toContainText("Internal Server Error");

    await page.goto("/app/review");
    await expect(page.locator("body")).not.toContainText("Internal Server Error");
  });

  test("8,13–16 tutor, lab, career, search", async ({ page, request }) => {
    const email = `e2e-tools-${Date.now()}@aileapro.dev`;
    await registerViaApi(request, email, "Correct-Horse-9!");
    await loginUi(page, email, "Correct-Horse-9!");

    for (const path of ["/app/lab", "/app/career", "/app/search", "/app/research", "/app/notes"]) {
      await page.goto(path);
      await expect(page.locator("body")).not.toContainText("Internal Server Error");
    }
  });

  test("17–20 map, studio, settings, evolve, admin-safe settings export UI", async ({
    page,
    request,
  }) => {
    const email = `e2e-close-${Date.now()}@aileapro.dev`;
    await registerViaApi(request, email, "Correct-Horse-9!");
    await loginUi(page, email, "Correct-Horse-9!");

    await page.goto("/app/map");
    await expect(page.locator("body")).not.toContainText("Internal Server Error");

    await page.goto("/app/settings");
    await expect(page.getByText(/export|delete|privacy/i).first()).toBeVisible({ timeout: 10_000 });

    await page.goto("/app/evolve");
    await expect(page.locator("body")).not.toContainText("Internal Server Error");

    await page.goto("/app/studio");
    await expect(page.locator("body")).not.toContainText("Internal Server Error");
  });
});
