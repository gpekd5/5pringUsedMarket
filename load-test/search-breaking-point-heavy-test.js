import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '20s', target: 1000 },
        { duration: '20s', target: 2000 },
        { duration: '20s', target: 3000 },
        { duration: '20s', target: 5000 },
        { duration: '20s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1000'],
    },
};

const BASE_URL = 'http://localhost:8080';
const VERSION = __ENV.VERSION || 'v2';

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