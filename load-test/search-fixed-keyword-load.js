/**
 * 고정 키워드 검색 API 부하 테스트 스크립트입니다.
 *
 * 목적:
 * - 동일한 검색어를 반복 호출하여 캐시 Hit 상황에서 v1, v2, v3의 성능을 비교합니다.
 * - Ramp Up을 통해 가상 사용자 수 증가에 따른 응답 시간, 처리량, 실패율 변화를 확인합니다.
 *
 * 실행 예시:
 * k6 run -e VERSION=v1 load-test/search-fixed-keyword-load.js
 * k6 run -e VERSION=v2 load-test/search-fixed-keyword-load.js
 * k6 run -e VERSION=v3 load-test/search-fixed-keyword-load.js
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

export default function () {
    const keyword = '아이폰';

    const url = `${BASE_URL}/api/${VERSION}/products/search?keyword=${encodeURIComponent(keyword)}&page=0&size=20`;

    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1);
}