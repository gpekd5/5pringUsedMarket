/**
 * 랜덤 키워드 검색 API 부하 테스트 스크립트입니다.
 *
 * 목적:
 * - 실제 사용자 검색 패턴처럼 여러 키워드를 랜덤하게 요청합니다.
 * - 캐시 Hit와 Miss가 섞이는 상황에서 v1, v2, v3의 성능을 비교합니다.
 * - 동일 키워드 반복 테스트보다 실제 서비스 상황에 가까운 부하를 확인합니다.
 *
 * 실행 예시:
 * k6 run -e VERSION=v1 load-test/search-random-keyword-load.js
 * k6 run -e VERSION=v2 load-test/search-random-keyword-load.js
 * k6 run -e VERSION=v3 load-test/search-random-keyword-load.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 10 },
        { duration: '20s', target: 10 },

        { duration: '10s', target: 30 },
        { duration: '20s', target: 30 },

        { duration: '10s', target: 50 },
        { duration: '20s', target: 50 },

        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
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
        'response time < 700ms': (r) => r.timings.duration < 700,
    });

    sleep(1);
}