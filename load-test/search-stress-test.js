/**
 * 검색 API Stress Test 스크립트입니다.
 *
 * 목적:
 * - VU를 50명 이상으로 증가시키며 검색 API의 성능 저하 지점을 확인합니다.
 * - p95 응답 시간 증가, 실패율 발생, RPS 정체 여부를 기준으로 포화 가능성을 판단합니다.
 *
 * 실행 예시:
 * k6 run -e VERSION=v1 load-test/search-stress-test.js
 * k6 run -e VERSION=v2 load-test/search-stress-test.js
 * k6 run -e VERSION=v3 load-test/search-stress-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '20s', target: 50 },
        { duration: '30s', target: 50 },

        { duration: '20s', target: 100 },
        { duration: '30s', target: 100 },

        { duration: '20s', target: 200 },
        { duration: '30s', target: 200 },

        { duration: '20s', target: 300 },
        { duration: '30s', target: 300 },

        { duration: '20s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1000'],
    },
};

const BASE_URL = 'http://localhost:8080';
const VERSION = __ENV.VERSION || 'v1';

const keywords = [
    '아이폰',
    '맥북',
    '갤럭시',
    '에어팟',
    '키보드',
    '마우스',
    '모니터',
    '가방',
    '나이키',
    '패딩',
];

export default function () {
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];

    const url = `${BASE_URL}/api/${VERSION}/products/search?keyword=${encodeURIComponent(keyword)}&page=0&size=20`;

    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}