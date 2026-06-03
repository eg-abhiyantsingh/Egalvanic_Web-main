// ═══════════════════════════════════════════════════════════════════════
// k6 load / stress / spike / soak test — API tier (Type I, non-functional)
// ═══════════════════════════════════════════════════════════════════════
// Boundary (honest): Selenium/PerfVerifier measures CLIENT-side render timing
// for one user. THIS measures SERVER throughput + latency under concurrency.
// They are complementary, not interchangeable.
//
// Profiles (set via PROFILE env): load | stress | spike | soak
//   load   - steady expected traffic
//   stress - ramp past capacity to find the breaking point
//   spike  - sudden surge then drop (autoscaling / queue behavior)
//   soak   - sustained load over time (memory leaks / resource exhaustion)
//
// Run (k6 installed):
//   BASE_URL=https://acme.qa.egalvanic.ai \
//   USER_EMAIL=... USER_PASSWORD=... PROFILE=load \
//   k6 run perf/k6-load-test.js
// ═══════════════════════════════════════════════════════════════════════
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'https://acme.qa.egalvanic.ai';
const API = `${BASE}/api`;
// Credentials are REQUIRED via env — never hardcode secrets in a committed script.
const EMAIL = __ENV.USER_EMAIL;
const PASSWORD = __ENV.USER_PASSWORD;
if (!EMAIL || !PASSWORD) {
  throw new Error('USER_EMAIL and USER_PASSWORD env vars are required (no hardcoded defaults).');
}
// Defence-in-depth: only ever send credentials to trusted eGalvanic hosts, so a
// mis-set BASE_URL can't exfiltrate the login POST to an attacker-controlled origin.
if (!/^https:\/\/[a-z0-9.-]+\.egalvanic\.ai(\/|$)/.test(BASE)) {
  throw new Error('BASE_URL must be an https *.egalvanic.ai host. Refusing to send credentials to: ' + BASE);
}
const PROFILE = __ENV.PROFILE || 'load';

const loginTime = new Trend('login_duration_ms');
const listTime = new Trend('list_duration_ms');
const errorRate = new Rate('errors');

const PROFILES = {
  // ramping-vus stages per profile
  load:   [ { duration: '1m', target: 20 }, { duration: '3m', target: 20 }, { duration: '1m', target: 0 } ],
  stress: [ { duration: '2m', target: 50 }, { duration: '3m', target: 200 }, { duration: '2m', target: 0 } ],
  spike:  [ { duration: '10s', target: 5 }, { duration: '20s', target: 300 }, { duration: '1m', target: 5 }, { duration: '10s', target: 0 } ],
  soak:   [ { duration: '2m', target: 30 }, { duration: '56m', target: 30 }, { duration: '2m', target: 0 } ],
};

export const options = {
  stages: PROFILES[PROFILE] || PROFILES.load,
  thresholds: {
    http_req_duration: ['p(95)<2000'],   // 95% of requests under 2s
    errors: ['rate<0.01'],               // <1% errors
  },
};

// One login per VU iteration start (token cached on the VU).
function login() {
  const res = http.post(`${API}/auth/v2/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } });
  loginTime.add(res.timings.duration);
  const ok = check(res, { 'login 2xx': (r) => r.status >= 200 && r.status < 300 });
  errorRate.add(!ok);
  try { return JSON.parse(res.body).access_token; } catch (e) { return null; }
}

export default function () {
  const token = login();
  const authHeaders = { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };

  // Hit the read-heavy list endpoints real users load.
  const endpoints = [
    `${API}/planned_workorder/`,
    `${API}/sld/sessions`,
  ];
  endpoints.forEach((url) => {
    const res = http.get(url, { ...authHeaders, tags: { name: url } });
    listTime.add(res.timings.duration);
    const ok = check(res, { [`GET ${url} 2xx`]: (r) => r.status >= 200 && r.status < 300 });
    errorRate.add(!ok);
  });

  sleep(1); // pacing: ~1 req/s per VU
}
