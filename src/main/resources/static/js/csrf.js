// Spring Security CSRF 토큰을 fetch 헤더에 넣는다. 페이지 <head> 의 _csrf, _csrf_header 메타 태그를 읽는다.
// 메타 태그가 없는 페이지(로그인하지 않아도 되는 페이지 등)에서는 headers 를 그대로 돌려준다.
export function withCsrf(headers = {}) {
  const headerName = document.querySelector('meta[name="_csrf_header"]')?.content;
  const token = document.querySelector('meta[name="_csrf"]')?.content;
  return headerName && token ? {...headers, [headerName]: token} : {...headers};
}
