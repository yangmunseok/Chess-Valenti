import {initBoard, renderBoardFromFen} from "./board.js";

let totalLoaded = 0;
let totalFiltered = 0;
let selectedPlatform = 'lichess';
let jobId = 0;
let systemUsername = '';

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

// 서버에서 전달받은 데이터를 UI에 반영하는 함수
function onServerUpdate(message) {
  const type = message.goal;
  const count = message.loadedGame;
  const status = message.status;
  jobId = message.jobId;

  if (status === 'error') {
    const errorModal = document.getElementById('error-modal');
    if (errorModal) {
      document.getElementById('error-message').innerText = message.goal;
      errorModal.style.display = 'flex';
    } else {
      alert(message.goal);
    }
    elements.progressSection.style.display = 'none';
    elements.insightFilters.style.display = 'block';
    return;
  }

  if (type === 'load') {
    totalLoaded = count;
    elements.loadCount.innerText = `${totalLoaded}`;
    elements.stepLoad.classList.add('active');
    elements.stepFilter.classList.remove('active');
    elements.stepDone.classList.remove('active');

    if (status === 'done') {
      elements.stepLoad.classList.remove('active');
      elements.stepFilter.classList.add('active');
      if (message.data) {
        totalFiltered = Object.keys(message.data).length;
        elements.filterCount.innerText = `${totalFiltered}`;
      }
    }
  } else if (type === 'filter') {
    elements.stepLoad.classList.remove('active');
    elements.stepFilter.classList.add('active');
    elements.stepDone.classList.remove('active');

    if (status === 'done') {
      if (message.data) {
        totalFiltered = Object.keys(message.data).length;
        elements.filterCount.innerText = `${totalFiltered}`;
      }
      elements.stepFilter.classList.remove('active');
      elements.stepDone.classList.add('active');
      renderResultPage(message.data);
    }
  }
}

const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
const stompClient = new StompJs.Client({
  brokerURL: `${protocol}${window.location.host}/valenti-socket`
});

stompClient.onConnect = (frame) => {
  stompClient.subscribe('/topic/insight', (res) => {
    const message = JSON.parse(res.body);
    if (message.systemUsername === systemUsername) {
      onServerUpdate(message);
    }
  });
};

stompClient.onWebSocketError = (error) => {
  // console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
  // console.error('Broker reported error: ' + frame.headers['message']);
  // console.error('Additional details: ' + frame.body);
};

function connect() {
  stompClient.activate();
}

function disconnect() {
  if (stompClient.connected || stompClient.active) {
    stompClient.deactivate();
  }
}

function sendRequest() {
  const timeControls = document.getElementsByName("timeControl");
  let perfType = "";
  Array.from(timeControls).forEach((el) => {
    if (el.checked) {
      perfType += "," + el.value;
    }
  });

  if (!perfType) {
    alert("최소 하나 이상의 게임 유형을 선택해주세요.");
    return;
  }

  perfType = perfType.substring(1);
  const sinceDate = elements.startDateInput.value;
  if (!sinceDate) {
    alert("분석 시작일을 선택해주세요.");
    return;
  }
  const since = new Date(sinceDate).getTime().toString();

  if (!stompClient.connected) {
    console.warn("STOMP not connected, attempting to reconnect...");
    stompClient.activate();
    setTimeout(() => {
      if (stompClient.connected) {
        stompClient.publish({
          destination: "/app/insight",
          body: JSON.stringify({
            username: elements.usernameInput.value,
            perfType,
            since,
            platform: selectedPlatform
          })
        });
      } else {
        alert("서버와 연결이 원활하지 않습니다. 잠시 후 다시 시도해주세요.");
      }
    }, 500);
    return;
  }

  stompClient.publish({
    destination: "/app/insight",
    body: JSON.stringify({
      username: elements.usernameInput.value,
      perfType,
      since,
      platform: selectedPlatform
    })
  });
}

function sendCancel() {
  if (!stompClient.connected) {
    return;
  }
  stompClient.publish({
    destination: "/app/insight",
    body: JSON.stringify({
      username: elements.usernameInput.value,
      cancel: true,
      id: jobId,
      platform: selectedPlatform
    })
  });
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
  systemUsername = config.username || '';

  connect();

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

  window.addEventListener('beforeunload', disconnect);

  elements.analyzeBtn.onclick = () => {
    const user = elements.usernameInput.value;
    if (!user) {
      return alert("아이디를 입력해주세요.");
    }

    elements.insightFilters.style.display = 'none';
    elements.resultArea.style.display = 'none';
    elements.progressSection.style.display = 'block';

    elements.loadCount.innerText = '0';
    elements.filterCount.innerText = '0';
    elements.stepLoad.classList.add('active');
    elements.stepFilter.classList.remove('active');
    elements.stepDone.classList.remove('active');

    sendRequest();
  };

  elements.partialAnalyzeBtn.onclick = () => {
    sendCancel();
  };
}
