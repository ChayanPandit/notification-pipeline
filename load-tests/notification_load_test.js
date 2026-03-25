import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ── Custom metrics ────────────────────────────────────────────────────────────
const acceptedRequests   = new Counter('accepted_requests');
const duplicateRequests  = new Counter('duplicate_requests');
const failedRequests     = new Counter('failed_requests');
const successRate        = new Rate('success_rate');
const requestDuration    = new Trend('request_duration', true);

// ── Test configuration ────────────────────────────────────────────────────────
export const options = {
    scenarios: {

        // Scenario 1: warm up — gentle ramp
        warm_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '15s', target: 5  },   // ramp to 5 users
                { duration: '15s', target: 5  },   // hold
            ],
            gracefulRampDown: '5s',
        },

        // Scenario 2: normal load
        normal_load: {
            executor: 'ramping-vus',
            startTime: '30s',
            startVUs: 5,
            stages: [
                { duration: '30s', target: 20  },  // ramp to 20 users
                { duration: '30s', target: 20  },  // hold
                { duration: '15s', target: 0   },  // ramp down
            ],
            gracefulRampDown: '5s',
        },

        // Scenario 3: spike — sudden burst
        spike: {
            executor: 'ramping-vus',
            startTime: '2m',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50  },  // sudden spike to 50 users
                { duration: '20s', target: 50  },  // hold spike
                { duration: '10s', target: 0   },  // drop off
            ],
            gracefulRampDown: '5s',
        },
    },

    thresholds: {
        http_req_duration: ['p(95)<500'],   // 95% of requests under 500ms
        success_rate:      ['rate>0.95'],   // at least 95% succeed
        http_req_failed:   ['rate<0.05'],   // less than 5% HTTP errors
    },
};

// ── Helpers ───────────────────────────────────────────────────────────────────
const EVENT_TYPES = ['ORDER_PLACED', 'ORDER_SHIPPED', 'PASSWORD_RESET', 'PROMO_ALERT'];
const BASE_URL    = 'http://localhost:8080';

function randomEventType() {
    return EVENT_TYPES[Math.floor(Math.random() * EVENT_TYPES.length)];
}

function randomRecipient() {
    return `user-${Math.floor(Math.random() * 1000)}`;
}

// Unique key per VU per iteration — guarantees no duplicates across the run
function idempotencyKey(vuId, iteration) {
    return `load-test-${vuId}-${iteration}-${Date.now()}`;
}

// ── Main test function (runs once per VU per iteration) ───────────────────────
export default function () {
    const payload = JSON.stringify({
        eventType:   randomEventType(),
        recipientId: randomRecipient(),
        payload: {
            orderId: `ord-${Math.floor(Math.random() * 100000)}`,
            source:  'k6-load-test',
        },
    });

    const params = {
        headers: {
            'Content-Type':    'application/json',
            'Idempotency-Key': idempotencyKey(__VU, __ITER),
        },
    };

    const start    = Date.now();
    const response = http.post(`${BASE_URL}/api/v1/events`, payload, params);
    const duration = Date.now() - start;

    requestDuration.add(duration);

    const isAccepted  = response.status === 202;
    const isDuplicate = response.status === 200;
    const isFailed    = response.status >= 400;

    check(response, {
        'status is 202 or 200': (r) => r.status === 202 || r.status === 200,
        'response has notificationId': (r) => {
            try {
                return JSON.parse(r.body).notificationId !== undefined;
            } catch {
                return false;
            }
        },
    });

    if (isAccepted)  acceptedRequests.add(1);
    if (isDuplicate) duplicateRequests.add(1);
    if (isFailed)    failedRequests.add(1);
    successRate.add(!isFailed);

    sleep(0.5); // 500ms between iterations per VU
}

// ── Summary ───────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const summary = {
        totalRequests:    data.metrics.http_reqs.values.count,
        successRate:      (data.metrics.success_rate.values.rate * 100).toFixed(2) + '%',
        p95Latency:       data.metrics.http_req_duration.values['p(95)'].toFixed(2) + 'ms',
        p99Latency:       data.metrics.http_req_duration.values['p(99)'].toFixed(2) + 'ms',
        avgLatency:       data.metrics.http_req_duration.values.avg.toFixed(2) + 'ms',
        accepted:         data.metrics.accepted_requests?.values.count ?? 0,
        duplicates:       data.metrics.duplicate_requests?.values.count ?? 0,
        failed:           data.metrics.failed_requests?.values.count ?? 0,
    };

    console.log('\n════════════════════════════════════════');
    console.log('        LOAD TEST SUMMARY');
    console.log('════════════════════════════════════════');
    console.log(`Total requests  : ${summary.totalRequests}`);
    console.log(`Success rate    : ${summary.successRate}`);
    console.log(`Avg latency     : ${summary.avgLatency}`);
    console.log(`p95 latency     : ${summary.p95Latency}`);
    console.log(`p99 latency     : ${summary.p99Latency}`);
    console.log(`Accepted (202)  : ${summary.accepted}`);
    console.log(`Duplicates (200): ${summary.duplicates}`);
    console.log(`Failed (4xx/5xx): ${summary.failed}`);
    console.log('════════════════════════════════════════\n');

    return {
        'load-tests/results/summary.json': JSON.stringify(data, null, 2),
    };
}