import {getPossibleMoves} from "./state.js";
import {executeMove} from "./game.js";

let selectedSquare = null;

// 체스판 초기 격자 그리기
export function initBoard(id = 'chess-board') {
  const boardEl = document.getElementById(id);
  if (!boardEl) {
    return;
  }
  boardEl.innerHTML = ''; // 초기화

  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const square = document.createElement('div');
      const isLight = (r + c) % 2 === 0;
      square.className = `square ${isLight ? 'light' : 'dark'}`;

      const numToFile = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
      square.dataset.coord = numToFile[c] + (8 - r);
      boardEl.appendChild(square);
    }
  }
}

// FEN 기반으로 기물 렌더링 (애니메이션 지원)
export function renderBoardFromFen(fen, id = 'chess-board') {
  const boardEl = document.getElementById(id);
  if (!boardEl) return;

  // Clear existing symbols
  boardEl.querySelectorAll('.square-symbol').forEach(s => s.remove());

  const piecePlacement = fen.split(' ')[0];
  const rows = piecePlacement.split('/');

  // 1. 기존 기물들의 위치 정보 저장 (FLIP: First)
  const oldPieces = {}; // { typeKey: [{ el, rect, coord }] }
  boardEl.querySelectorAll('.piece').forEach(piece => {
    const coord = piece.parentElement.dataset.coord;
    const type = [...piece.classList].find(c => !['piece', 'white', 'black'].includes(c));
    const color = piece.classList.contains('white') ? 'white' : 'black';
    const key = `${color}-${type}`;
    
    if (!oldPieces[key]) oldPieces[key] = [];
    oldPieces[key].push({
      el: piece,
      rect: piece.getBoundingClientRect(),
      coord: coord
    });
  });

  // 2. 새 상태를 위한 데이터 준비
  const newPositions = [];
  rows.forEach((row, rowIndex) => {
    let colIndex = 0;
    const rank = 8 - rowIndex;
    for (const char of row) {
      if (isNaN(char)) {
        const file = String.fromCharCode(97 + colIndex);
        const coord = file + rank;
        const color = (char === char.toUpperCase()) ? 'white' : 'black';
        const type = char.toLowerCase();
        newPositions.push({ coord, color, type, key: `${color}-${type}`, matched: false });
        colIndex++;
      } else {
        colIndex += parseInt(char);
      }
    }
  });

  const usedOldPieces = new Set();
  const pieceMatches = []; // [{ el, oldRect }]

  // 3. [Pass 1] 제자리에 있는 기물들 먼저 선점 (Same square match)
  // 이 단계에서 매칭된 기물은 애니메이션이 발생하지 않음
  newPositions.forEach(pos => {
    const matchIndex = oldPieces[pos.key]?.findIndex(p => p.coord === pos.coord && !usedOldPieces.has(p.el));
    if (matchIndex !== undefined && matchIndex !== -1) {
      const match = oldPieces[pos.key][matchIndex];
      const targetSquare = boardEl.querySelector(`.square[data-coord="${pos.coord}"]`);
      
      const pieceEl = match.el;
      usedOldPieces.add(pieceEl);
      pieceEl.style.transition = 'none';
      pieceEl.style.transform = 'none';
      targetSquare.appendChild(pieceEl);
      
      pos.matched = true;
      // 위치가 같으므로 pieceMatches에 넣지 않음 (또는 넣어도 delta가 0)
    }
  });

  // 4. [Pass 2] 이동한 기물들 매칭 (Remaining match)
  newPositions.forEach(pos => {
    if (pos.matched) return;

    const matchIndex = oldPieces[pos.key]?.findIndex(p => !usedOldPieces.has(p.el));
    const targetSquare = boardEl.querySelector(`.square[data-coord="${pos.coord}"]`);

    if (matchIndex !== undefined && matchIndex !== -1) {
      const match = oldPieces[pos.key][matchIndex];
      const pieceEl = match.el;
      usedOldPieces.add(pieceEl);
      
      pieceEl.style.transition = 'none';
      pieceEl.style.transform = 'none';
      targetSquare.appendChild(pieceEl);
      
      pieceMatches.push({ el: pieceEl, oldRect: match.rect });
      pos.matched = true;
    } else {
      // 신규 기물 (프로모션 등)
      const pieceEl = document.createElement('span');
      pieceEl.classList.add('piece', pos.color, pos.type);
      targetSquare.appendChild(pieceEl);
    }
  });

  // 5. 사용되지 않은 기물 제거 (캡처됨)
  Object.values(oldPieces).flat().forEach(p => {
    if (!usedOldPieces.has(p.el)) {
      p.el.remove();
    }
  });

  // 6. 애니메이션 실행 (FLIP: Invert & Play)
  requestAnimationFrame(() => {
    pieceMatches.forEach(match => {
      const newRect = match.el.getBoundingClientRect();
      const deltaX = match.oldRect.left - newRect.left;
      const deltaY = match.oldRect.top - newRect.top;

      if (deltaX !== 0 || deltaY !== 0) {
        match.el.style.transition = 'none';
        match.el.style.transform = `translate(${deltaX}px, ${deltaY}px)`;

        requestAnimationFrame(() => {
          match.el.style.transition = 'transform 0.25s ease-out';
          match.el.style.transform = 'none';
        });
      }
    });
  });
}

export function drawSymbol(coord, symbol, id = 'chess-board') {
  if (!symbol) return;
  const boardEl = document.getElementById(id);
  if (!boardEl) return;
  
  // Clear any existing symbol first
  boardEl.querySelectorAll('.square-symbol').forEach(s => s.remove());

  const targetSquare = boardEl.querySelector(`.square[data-coord="${coord}"]`);
  if (!targetSquare) return;

  const symbolMap = {
    '!!': { class: 'symbol-brilliant', text: '!!' },
    '!': { class: 'symbol-excellent', text: '!' },
    '?': { class: 'symbol-mistake', text: '?' },
    '??': { class: 'symbol-blunder', text: '??' },
    '?!': { class: 'symbol-inaccuracy', text: '?!' }
  };

  const config = symbolMap[symbol];
  if (!config) return;

  const symbolEl = document.createElement('div');
  symbolEl.className = `square-symbol ${config.class}`;
  symbolEl.innerText = config.text;
  targetSquare.appendChild(symbolEl);
}

function getPieceSymbol(char) {
  const symbols = {
    'r': 't', 'n': 'j', 'b': 'n', 'q': 'w', 'k': 'l', 'p': 'o',
    'R': 'r', 'N': 'h', 'B': 'b', 'Q': 'q', 'K': 'k', 'P': 'p'
  };
  return symbols[char] || '';
}

// 클릭 및 이동 하이라이트 이벤트 바인딩
export function updateSquareEvent() {
  const possibleMoves = getPossibleMoves();

  document.querySelectorAll('#chess-board .square').forEach(square => {
    square.onclick = function () {
      const clickedCoord = this.dataset.coord;

      if (this.classList.contains('possible-move')) {
        const from = selectedSquare;
        const to = clickedCoord;
        executeMove(from, to);
        clearHighlights();
        selectedSquare = null;
        return;
      }

      clearHighlights();
      const targets = possibleMoves
      .filter(move => move.substring(0, 2) === clickedCoord)
      .map(move => move.substring(2, 4));

      if (targets.length > 0) {
        selectedSquare = clickedCoord;
        this.classList.add('selected-sq');
        targets.forEach(targetCoord => {
          const targetSquare = document.querySelector(
              `.square[data-coord="${targetCoord}"]`);
          if (targetSquare) {
            targetSquare.classList.add('possible-move');
          }
        });
      } else {
        selectedSquare = null;
      }
    };
  });
}

export function clearHighlights() {
  document.querySelectorAll('.square').forEach(sq => {
    sq.classList.remove('selected-sq', 'possible-move');
  });
  selectedSquare = null;
}