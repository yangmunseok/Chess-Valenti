// pawn-games.html: 폰 구조가 같은 게임 목록. 서버가 넘긴 FEN 으로 미리보기 보드를 그리고 게임을 페이지 단위로 불러온다.
import {initBoard, renderBoardFromFen} from "../board.js";

export function initPawnGames(targetFen) {
  let currentPage = 0; // 0-indexed for backend
  const itemsPerPage = 15;
  let totalPages = 0;

  // Initialize
  initBoard('preview-board');
  renderBoardFromFen(targetFen, 'preview-board');
  fetchGames(0);

  async function fetchGames(page) {
    currentPage = page;
    const url = `/api/games/paginated?fen=${encodeURIComponent(
        targetFen)}&page=${page}&size=${itemsPerPage}`;

    document.getElementById(
        'game-list-body').innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 40px; color: var(--text-secondary);">게임 데이터를 불러오고 있습니다...</td></tr>';

    try {
      const res = await fetch(url);
      if (!res.ok) {
        throw new Error('Fetch failed');
      }

      const data = await res.json();
      totalPages = data.page.totalPages;

      document.getElementById('total-games').innerText = data.page.totalElements;
      renderGameTable(data.content);
      updatePaginationControls();

      if (data.content.length === 0) {
        document.getElementById(
            'game-list-body').innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 40px; color: var(--text-secondary);">해당 구조의 게임이 없습니다.</td></tr>';
      }
    } catch (err) {
      console.error(err);
      document.getElementById(
          'game-list-body').innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 40px; color: var(--danger);">데이터를 불러오지 못했습니다.</td></tr>';
    }
  }

  function renderGameTable(games) {
    const tbody = document.getElementById('game-list-body');
    const isMobile = window.innerWidth <= 768;

    tbody.innerHTML = games.map(game => `
      <tr>
          <td class="hide-mobile">${game.event}</td>
          <td>${game.whitePlayer.name}</td>
          <td>${game.blackPlayer.name}</td>
          <td>${game.whitePlayer.elo}</td>
          <td>${game.blackPlayer.elo}</td>
          <td class="result-cell">${game.result}</td>
          <td class="hide-mobile">${game.date}</td>
          <td><a class="view-btn" href="/games/${game.gameOffset}?idx=${game.moveIdx}" target="_blank">👁️</a></td>
      </tr>
    `).join('');
  }

  function updatePaginationControls() {
    document.getElementById('page-info').innerText = `${currentPage + 1} / ${Math.max(1,
        totalPages)}`;
    document.getElementById('prev-page').disabled = (currentPage === 0);
    document.getElementById('next-page').disabled = (currentPage >= totalPages - 1 || totalPages
        === 0);
  }

  document.getElementById('prev-page').onclick = () => {
    if (currentPage > 0) {
      fetchGames(currentPage - 1);
    }
  };
  document.getElementById('next-page').onclick = () => {
    if (currentPage < totalPages - 1) {
      fetchGames(currentPage + 1);
    }
  };
}
