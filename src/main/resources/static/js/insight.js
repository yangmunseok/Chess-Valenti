import {initBoard, renderBoardFromFen} from "./board.js";
import {InsightLoadError, loadChessComGames, loadLichessGames} from "./insightLoader.js";

let selectedPlatform = 'lichess';
// 진행 중인 분석. 중단 버튼과 결과 처리에서 쓴다.
let currentRun = null;

const elements = {};

function initElements() {
  elements.progressSection = document.getElementById("progress-section");
  elements.insightFilters = document.querySelector(".insight-filters");
  elements.analyzeBtn = document.getElementById('analyze-btn');
  elements.partialAnalyzeBtn = document.getElementById('partial-analyze-btn');
  elements.stepLoad = document.getElementById('step-load');
  elements.stepFilter = document.getElementById('step-filter');
  elements.stepDone = document.getElementById('step-done');
  elements.usernameInput = document.getElementById('username-input');
  elements.startDateInput = document.getElementById('start-date');
  elements.resultArea = document.getElementById("result-area");
  elements.resultTitle = document.getElementById('result-title');
  elements.frequentStructures = document.getElementById('frequent-structures');
  elements.topWinStructures = document.getElementById('top-win-structures');
  elements.worstWinStructure = document.getElementById('worst-win-structures');
  elements.loadCount = document.getElementById('load-count');
  elements.filterCount = document.getElementById('filter-count');
}

function readRequest() {
  const perfType = Array.from(document.getElementsByName("timeControl"))
      .filter(el => el.checked)
      .map(el => el.value)
      .join(',');
  if (!perfType) {
    alert("최소 하나 이상의 게임 유형을 선택해주세요.");
    return null;
  }
  const sinceDate = elements.startDateInput.value;
  if (!sinceDate) {
    alert("분석 시작일을 선택해주세요.");
    return null;
  }
  return {
    username: elements.usernameInput.value.trim(),
    perfType,
    since: new Date(sinceDate).getTime(),
  };
}

function setStep(step) {
  elements.stepLoad.classList.toggle('active', step === 'load');
  elements.stepFilter.classList.toggle('active', step === 'filter');
  elements.stepDone.classList.toggle('active', step === 'done');
}

function showError(message) {
  const errorModal = document.getElementById('error-modal');
  if (errorModal) {
    document.getElementById('error-message').innerText = message;
    errorModal.style.display = 'flex';
  } else {
    alert(message);
  }
  elements.progressSection.style.display = 'none';
  elements.insightFilters.style.display = 'block';
}

/**
 * 게임을 받아 Worker 에 넘기고, 다 받으면(또는 중단하면) 결과를 계산해 보여 준다.
 * 받는 일은 이 화면 스레드에서, 분석은 Worker 에서 동시에 진행된다.
 */
async function runAnalysis(request) {
  const controller = new AbortController();
  const worker = new Worker('/js/insightWorker.js', {type: 'module'});
  const run = {controller, worker};
  currentRun = run;
  let loadedGame = 0;

  const stop = () => {
    worker.terminate();
    if (currentRun === run) {
      currentRun = null;
    }
  };

  worker.onmessage = ({data}) => {
    if (data.type === 'progress') {
      elements.filterCount.innerText = `${data.structureCount}`;
    } else if (data.type === 'result') {
      stop();
      elements.filterCount.innerText = `${data.filteredCount}`;
      setStep('done');
      renderResultPage(data.top);
    } else if (data.type === 'error') {
      stop();
      showError("분석 중 오류가 발생했습니다.");
    }
  };
  worker.onerror = () => {
    stop();
    showError("분석 중 오류가 발생했습니다.");
  };

  worker.postMessage({type: 'start', username: request.username});
  const load = selectedPlatform === 'chesscom' ? loadChessComGames : loadLichessGames;
  try {
    await load({
      ...request,
      signal: controller.signal,
      onGames: games => {
        loadedGame += games.length;
        elements.loadCount.innerText = `${loadedGame}`;
        worker.postMessage({type: 'games', games});
      },
    });
  } catch (e) {
    stop();
    showError(e instanceof InsightLoadError ? e.message : "게임을 불러오지 못했습니다.");
    return;
  }

  // 다 받았거나 '여기까지 데이터로 분석' 으로 멈췄다. 비슷한 구조를 합치고 상위 구조를 고른다.
  setStep('filter');
  worker.postMessage({type: 'finish'});
}

function updateCardUI(cardElement, stats, side) {
  const barContainer = cardElement.querySelector('.win-rate-bar');

  if (side !== "total" && stats.whiteWon + stats.blackWon + stats.drawn === 0) {
    barContainer.classList.add('no-data');
    barContainer.innerHTML = `<span class="no-data-text">No games played as ${side}</span>`;
    cardElement.querySelector('.count-summary').innerText = `0W 0D 0L`;
    return;
  }

  barContainer.classList.remove('no-data');

  if (side === "total") {
    barContainer.innerHTML = `
      <div class="bar win"></div>
      <div class="bar draw"></div>
      <div class="bar loss"></div>
    `;
    const total = getTotal(stats);
    const winP = Math.round(getWinRate(stats) * 100);
    const lossP = Math.round(getLossRate(stats) * 100);
    const drawP = 100 - winP - lossP;
    const winBar = barContainer.querySelector('.win');
    const drawBar = barContainer.querySelector('.draw');
    const lossBar = barContainer.querySelector('.loss');

    renderBar(winBar, winP);
    renderBar(drawBar, drawP);
    renderBar(lossBar, lossP);

    cardElement.querySelector('.count-summary').innerText = `${stats.white.whiteWon + stats.black.blackWon}W ${stats.white.drawn + stats.black.drawn}D ${stats.white.blackWon + stats.black.whiteWon}L`;
    return;
  }

  barContainer.innerHTML = `
    <div class="bar white"></div>
    <div class="bar draw"></div>
    <div class="bar black"></div>
  `;

  const drawn = Math.round(stats.drawn / 2);
  const total = stats.whiteWon + drawn + stats.blackWon;
  const whiteWonP = Math.round((stats.whiteWon / total) * 100);
  const drawP = Math.round((drawn / total) * 100);
  const blackWonP = 100 - whiteWonP - drawP;

  const newWhiteWonBar = barContainer.querySelector('.white');
  const newDrawBar = barContainer.querySelector('.draw');
  const newBlackWonBar = barContainer.querySelector('.black');

  newWhiteWonBar.style.width = whiteWonP + '%';
  newDrawBar.style.width = drawP + '%';
  newBlackWonBar.style.width = blackWonP + '%';

  newWhiteWonBar.innerText = whiteWonP > 10 ? whiteWonP + '%' : '';
  newDrawBar.innerText = drawP > 10 ? drawP + '%' : '';
  newBlackWonBar.innerText = blackWonP > 10 ? blackWonP + '%' : '';

  if (side === "white") {
    cardElement.querySelector('.count-summary').innerText = `${stats.whiteWon}W ${drawn}D ${stats.blackWon}L`;
    return;
  }
  cardElement.querySelector('.count-summary').innerText = `${stats.blackWon}W ${drawn}D ${stats.whiteWon}L`;
}

function renderBar(barElement, percentage) {
  barElement.style.width = percentage + '%';
  barElement.innerHTML = percentage > 10 ? percentage + '%' : '';
}

function getWinRate(stats) {
  const total = getTotal(stats);
  const won = stats.white.whiteWon + stats.black.blackWon;
  return won / total;
}

function getLossRate(stats) {
  const total = getTotal(stats);
  const won = stats.black.whiteWon + stats.white.blackWon;
  return won / total;
}

function getTotal(stats) {
  return stats.white.whiteWon + stats.white.drawn + stats.white.blackWon
      + stats.black.whiteWon + stats.black.drawn + stats.black.blackWon;
}

function renderPawnStructure(games, canvas, canvasName) {
  let i = 1;
  for (const key in games) {
    const stats = games[key];
    const id = canvasName + '-' + i;
    const card = document.createElement("div");
    card.className = "structure-card";
    card.innerHTML = `
      <div class="mini-board-wrapper">
        <div id="${id}" class="board"></div>
      </div>
      <div class="card-stats-area">
        <div class="side-selector">
          <button class="side-btn active" data-side="total">Total</button>
          <button class="side-btn" data-side="white">White</button>
          <button class="side-btn" data-side="black">Black</button>
        </div>
        <div class="rate-display">
          <div class="win-rate-bar"></div>
          <div class="count-summary">0W 0D 0L</div>
        </div>
      </div>
    `;
    canvas.appendChild(card);
    initBoard(id);
    renderBoardFromFen(key, id);

    const boardWrapper = card.querySelector('.mini-board-wrapper');
    boardWrapper.onclick = () => {
      location.href = `/pawn-games?fen=${encodeURIComponent(key)}`;
    };

    const buttons = card.querySelectorAll('.side-btn');
    buttons.forEach(btn => {
      btn.onclick = () => {
        buttons.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        if (btn.dataset.side === "total") {
          updateCardUI(card, stats, "total");
        } else {
          updateCardUI(card, stats[btn.dataset.side], btn.dataset.side);
        }
      };
    });
    updateCardUI(card, stats, "total");
    i++;
  }
}

function renderResultPage(map) {
  if (!map || Object.keys(map).length === 0) {
    elements.frequentStructures.innerHTML = '<p class="no-data-msg">조건에 맞는 게임 데이터가 없습니다.</p>';
    elements.topWinStructures.innerHTML = '<p class="no-data-msg">-</p>';
    elements.worstWinStructure.innerHTML = '<p class="no-data-msg">-</p>';
    elements.resultTitle.innerText = "No results for " + elements.usernameInput.value;
    elements.progressSection.style.display = 'none';
    elements.resultArea.style.display = 'block';
    return;
  }

  elements.frequentStructures.innerHTML = '';
  elements.topWinStructures.innerHTML = '';
  elements.worstWinStructure.innerHTML = '';

  elements.resultTitle.innerText = "Analysis for " + elements.usernameInput.value;
  const jsonSortedByTotalGames = Object.entries(map).sort(([, v1], [, v2]) => {
    return getTotal(v2) - getTotal(v1);
  }).slice(0, 5).reduce((acc, [k, v]) => {
    acc[k] = v;
    return acc;
  }, {});
  const jsonSortedByWinRate = Object.entries(map).sort(([, v1], [, v2]) => {
    return getWinRate(v2) - getWinRate(v1);
  }).filter(([, v]) => {
    return getTotal(v) > 10;
  }).slice(0, 5).reduce((acc, [k, v]) => {
    acc[k] = v;
    return acc;
  }, {});
  const jsonSortedByLossRate = Object.entries(map).sort(([, v1], [, v2]) => {
    return getLossRate(v2) - getLossRate(v1);
  }).filter(([, v]) => {
    return getTotal(v) > 10;
  }).slice(0, 6).reduce((acc, [k, v]) => {
    acc[k] = v;
    return acc;
  }, {});
  renderPawnStructure(jsonSortedByTotalGames, elements.frequentStructures, 'frequentStructures');
  renderPawnStructure(jsonSortedByWinRate, elements.topWinStructures, 'topWinStructures');
  renderPawnStructure(jsonSortedByLossRate, elements.worstWinStructure, 'worstWinStructure');

  elements.progressSection.style.display = 'none';
  elements.resultArea.style.display = 'block';
}

export function initInsight(config) {
  initElements();

  const defaultStartDate = new Date();
  defaultStartDate.setFullYear(defaultStartDate.getFullYear() - 1);
  elements.startDateInput.value = defaultStartDate.toISOString().split('T')[0];

  if (config.savedInsightData) {
    elements.usernameInput.value = config.savedLichessUsername || "";
    if (config.savedSince) {
      elements.startDateInput.value = new Date(config.savedSince).toISOString().split('T')[0];
    }
    if (config.savedPerfType) {
      const perfs = config.savedPerfType.split(',');
      const checkboxes = document.getElementsByName("timeControl");
      checkboxes.forEach(cb => {
        cb.checked = perfs.includes(cb.value);
      });
    }
    renderResultPage(config.savedInsightData);
  }

  document.querySelectorAll('.platform-toggle .tgl-btn').forEach(button => {
    button.onclick = () => {
      document.querySelectorAll('.platform-toggle .tgl-btn').forEach(
          el => el.classList.remove('active'));
      button.classList.add('active');
      selectedPlatform = button.dataset.platform;
      elements.usernameInput.placeholder = selectedPlatform === 'chesscom'
          ? 'Chess.com 아이디 입력...'
          : 'Lichess 아이디 입력...';
    };
  });

  elements.analyzeBtn.onclick = () => {
    if (!elements.usernameInput.value.trim()) {
      return alert("아이디를 입력해주세요.");
    }
    const request = readRequest();
    if (!request || currentRun) {
      return;
    }

    elements.insightFilters.style.display = 'none';
    elements.resultArea.style.display = 'none';
    elements.progressSection.style.display = 'block';

    elements.loadCount.innerText = '0';
    elements.filterCount.innerText = '0';
    setStep('load');

    runAnalysis(request);
  };

  // 받기를 멈추고 지금까지 받은 게임으로 결과를 낸다
  elements.partialAnalyzeBtn.onclick = () => {
    currentRun?.controller.abort();
  };
}
