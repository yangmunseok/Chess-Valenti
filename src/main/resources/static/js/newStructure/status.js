// 가상의 데이터 샘플 및 페이지 상태
import {currentNode} from "./state.js";

console.log("imported.")
let currentPage = 1;
const itemsPerPage = 10;
let filteredGames = []; // 검색된 게임 데이터 저장소

/*
document.querySelector('.menu-item-btn:not(.disabled)').addEventListener(
    'click', function () {
      console.log("clciked.")
      const footer = document.getElementById('pawn-structure-analysis');
      // 토글 방식: 열려있으면 닫고, 닫혀있으면 데이터 로드 후 열기
      if (footer.style.display === 'none') {
        footer.style.display = 'block';
        loadPawnStructureData();
      } else {
        footer.style.display = 'none';
      }
    });
*/

async function loadGameStream(url) {
  const res = await fetch(url);
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let batchCount = 0;
  while (true) {
    const {value, done} = await reader.read();
    if (done) {
      break;
    }
    console.log("hmm...");
    buffer += decoder.decode(value, {stream: true});
    const lines = buffer.split('\n');
    buffer = lines.pop();

    for (const line of lines) {
      if (line.trim()) {
        const game = JSON.parse(line);
        filteredGames.push(game);
        batchCount++;
        // 데이터가 들어올 때마다 통계 계산 및 화면 갱신
        // 게임이 1개라도 들어오면 바로 첫 페이지 렌더링

        if (batchCount % 100 === 0) {
          document.getElementById(
              'total-games').innerText = `${filteredGames.length}`;
          filteredGames = filteredGames.sort((game1, game2) => {
            if (Math.max([game1.whitePlayer.elo, game1.blackPlayer.elo])
                < Math.max([game2.whitePlayer.elo, game2.blackPlayer.elo])) {
              return 1;
            }
            if (game1.whitePlayer.elo, game1.blackPlayer.elo
            - (game2.whitePlayer.elo, game2.blackPlayer.elo) < 0) {
              return 1;
            }
            return -1;
          })
          renderGamePage(1);
          await new Promise(requestAnimationFrame);
        }

      }
    }
  }
  // 마지막 남은 버퍼 처리 (끝에 줄바꿈이 없는 경우)
  if (buffer.trim()) {
    filteredGames.push(JSON.parse(buffer));
    document.getElementById(
        'total-games').innerText = `${filteredGames.length}`;
    renderGamePage(1);
  }
  console.log("Streaming finished. Total games:", filteredGames.length);
  updateStats(filteredGames);
  renderGamePage(1);
}

async function loadPawnStructureData(params) {
  // 1. 실제 구현 시 서버 API 호출이나 DB 검색이 일어나는 곳입니다.
  // 여기서는 예시 데이터를 생성합니다.
  if (params) {
    console.log(params)
    await loadGameStream(`/api/games?fen=${currentNode.fen}&usePieceFilter=true
        &whiteQueen=${params.wq}&whiteRook=${params.wr}&whiteBishop=${params.wb}&whiteKnight=${params.wn}
        &blackQueen=${params.bq}&blackRook=${params.br}&blackBishop=${params.bb}&blackKnight=${params.bn}`)
    return
  }
  await loadGameStream(`/api/games?fen=${currentNode.fen}`)
}

function updateStats(games) {
  const total = games.length;
  const wins = games.filter(g => g.result === 'WHITE_WON').length;
  const draws = games.filter(g => g.result === 'DRAW').length;
  const losses = total - wins - draws;

  document.getElementById('total-games').innerText = total;

  // 승률 바 비율 업데이트
  const wPct = ((wins / total) * 100).toFixed(1);
  const dPct = ((draws / total) * 100).toFixed(1);
  const lPct = (100 - wPct - dPct).toFixed(1);

  const wBar = document.getElementById('win-bar');
  wBar.style.width = wPct + '%';
  wBar.innerText = wPct + '%';

  const dBar = document.getElementById('draw-bar');
  dBar.style.width = dPct + '%';
  dBar.innerText = dPct + '%';

  const lBar = document.getElementById('loss-bar');
  lBar.style.width = lPct + '%';
  lBar.innerText = lPct + '%';
}

function renderGamePage(page) {
  currentPage = page;
  const start = (page - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  const pagedData = filteredGames.slice(start, end);

  const tbody = document.getElementById('game-list-body');
  tbody.innerHTML = pagedData.map(game => `
        <tr>
            <td>${game.event}</td>
            <td>${game.whitePlayer.name}(${game.whitePlayer.elo})</td>
            <td>${game.blackPlayer.name}(${game.blackPlayer.elo})</td>
            <td class="result-cell">${game.result}</td>
            <td>${game.date}</td>
            <td><a class="view-btn" href="/games/${game.gameOffset}?idx=${game.moveIdx}">👁️</a></td>
        </tr>
    `).join('');

  // 페이지 정보 업데이트
  const totalPages = Math.ceil(filteredGames.length / itemsPerPage);
  document.getElementById(
      'page-info').innerText = `${currentPage} / ${totalPages}`;
  document.getElementById('prev-page').disabled = (currentPage === 1);
  document.getElementById('next-page').disabled = (currentPage === totalPages);
}

// 페이지네이션 이벤트 리스너
document.getElementById('prev-page').onclick = () => renderGamePage(
    currentPage - 1);
document.getElementById('next-page').onclick = () => renderGamePage(
    currentPage + 1)

// 1. 상태 관리
const filterValues = {
  wq: 1, wr: 2, wb: 2, wn: 2, // White
  bq: 1, br: 2, bb: 2, bn: 2  // Black
};

document.querySelectorAll('.stepper button').forEach(btn => {
  btn.addEventListener('click', (e) => {
    const item = e.target.closest('.filter-item');
    const piece = item.dataset.piece;
    const countSpan = item.querySelector('.count');
    const isPlus = e.target.classList.contains('plus');

    // Queen은 1, 나머지는 2가 최대값
    const max = piece.endsWith('q') ? 1 : 2;

    if (isPlus && filterValues[piece] < max) {
      filterValues[piece]++;
    } else if (!isPlus && filterValues[piece] > 0) {
      filterValues[piece]--;
    }

    countSpan.innerText = filterValues[piece];
  });
});

// 2. 패널 전환 함수
function switchPanel(panelId) {
  document.querySelectorAll('.side-pannel .panel').forEach(
      p => p.classList.remove('active'));
  document.getElementById(panelId).classList.add('active');
}

// 3. 이벤트 리스너 등록
document.addEventListener('DOMContentLoaded', () => {
  // 메인 메뉴에서 폰 구조 탐색 버튼 클릭 시
  document.querySelector('.menu-item-btn:not(.disabled)')?.addEventListener(
      'click', () => {
        switchPanel('pawn-filter-section');
      });

  // 필터 뒤로가기
  document.getElementById('filter-back-btn')?.addEventListener('click', () => {
    switchPanel('extra-menu');
  });

  // 검색 실행 버튼
  const runSearch = (useFilter) => {
    const params = useFilter ? filterValues : null;

    // 하단 결과창 표시
    const footer = document.getElementById('pawn-structure-analysis');
    footer.style.display = 'block';

    // 데이터 로드 로직 호출 (기존에 만든 loadPawnStructureData 호출)
    loadPawnStructureData(params);

    // 선택 사항: 검색 후 다시 메인 메뉴나 에디터로 돌아가게 할 수 있음
    switchPanel('extra-menu');
  };

  document.getElementById('search-with-filter')?.addEventListener('click',
      () => runSearch(true));
  document.getElementById('search-no-filter')?.addEventListener('click',
      () => runSearch(false));
});
