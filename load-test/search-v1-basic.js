/**
 * 검색 API k6 Smoke Test 스크립트입니다.
 *
 * 목적:
 * - k6가 검색 API를 정상적으로 호출할 수 있는지 확인합니다.
 * - 부하 테스트 전 status 200, checks 100%, http_req_failed 0% 여부를 검증합니다.
 *
 * 실행 예시:
 * k6 run load-test/search-v1-basic.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 1,
    iterations: 5,
};

export default function () {
    const url = 'http://localhost:8080/api/v1/products/search?keyword=%EC%95%84%EC%9D%B4%ED%8F%B0&page=0&size=20';

    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(1);
}