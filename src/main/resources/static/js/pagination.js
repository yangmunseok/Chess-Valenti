// 관리자 목록의 페이지 번호 줄. 번호 링크는 10개씩 보여 주고, 양쪽 화살표 버튼은 이전/다음 10개로 넘긴다.
// 마크업: <div class="pagination" data-page="현재 페이지(0부터)">
//           <button data-page-step="-10">  <a>1</a> <a>2</a> ...  <button data-page-step="10">
// 이 파일을 불러오면 페이지의 모든 .pagination 을 초기화한다.

export const PAGES_PER_WINDOW = 10;

// currentPage(0부터)가 들어 있는 10개 묶음의 첫 번호(0부터)
export function windowStart(currentPage) {
  return Math.floor(Math.max(0, currentPage) / PAGES_PER_WINDOW) * PAGES_PER_WINDOW;
}

// start 에서 step(±10)만큼 옮긴 묶음의 첫 번호. 첫 묶음과 마지막 묶음을 넘어가지 않는다.
export function shiftWindow(start, step, totalPages) {
  const last = windowStart(Math.max(0, totalPages - 1));
  return Math.min(last, Math.max(0, start + step));
}

export function initPagination(pagination) {
  const links = [...pagination.querySelectorAll('a')];
  const prev = pagination.querySelector('[data-page-step^="-"]');
  const next = pagination.querySelector('[data-page-step]:not([data-page-step^="-"])');
  let start = windowStart(Number(pagination.dataset.page) || 0);

  const render = () => {
    links.forEach((link, i) => {
      link.style.display = i >= start && i < start + PAGES_PER_WINDOW ? '' : 'none';
    });
    prev.disabled = start === 0;
    next.disabled = start + PAGES_PER_WINDOW >= links.length;
  };

  pagination.querySelectorAll('[data-page-step]').forEach(button =>
      button.addEventListener('click', () => {
        start = shiftWindow(start, Number(button.dataset.pageStep), links.length);
        render();
      }));
  render();
}

if (typeof document !== 'undefined') {
  document.querySelectorAll('.pagination').forEach(initPagination);
}
