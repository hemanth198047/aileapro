# k6 smoke — light concurrent load against health + public auth CSRF.
# Requires API up:  k6 run infra/scripts/k6-smoke.js
# Optional:         k6 run -e API=http://localhost:8090 infra/scripts/k6-smoke.js
import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 5,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<2000"],
  },
};

const API = __ENV.API || "http://localhost:8090";

export default function () {
  const health = http.get(`${API}/actuator/health`);
  check(health, {
    "health 200": (r) => r.status === 200,
    "health up": (r) => String(r.body).includes("UP") || r.status === 200,
  });

  const csrf = http.get(`${API}/v1/auth/csrf`);
  check(csrf, {
    "csrf 200": (r) => r.status === 200,
  });

  sleep(0.5);
}
