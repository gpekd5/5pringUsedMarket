/**
 * 검색 API Breaking Point Test 스크립트입니다.
 *
 * 목적:
 * - 검색 API가 어느 부하 지점부터 응답 시간이 급증하거나 실패하는지 확인합니다.
 * - sleep 없이 요청을 반복하여 서버의 한계 지점을 찾습니다.
 *
 * 실행 예시:
 * k6 run -e VERSION=v1 load-test/search-breaking-point-test.js
 * k6 run -e VERSION=v2 load-test/search-breaking-point-test.js
 * k6 run -e VERSION=v3 load-test/search-breaking-point-test.js
 */

import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '20s', target: 50 },
        { duration: '20s', target: 100 },
        { duration: '20s', target: 200 },
        { duration: '20s', target: 400 },
        { duration: '20s', target: 600 },
        { duration: '20s', target: 800 },
        { duration: '20s', target: 1000 },
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
}