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

// FEN 기반으로 기물 렌더링
export function renderBoardFromFen(fen, id = 'chess-board') {
  const piecePlacement = fen.split(' ')[0];
  const rows = piecePlacement.split('/');

  document.querySelectorAll('#' + id + ' .square').forEach(sq => {
    const piece = sq.querySelector('.piece');
    if (piece) {
      piece.remove();
    }
  });

  rows.forEach((row, rowIndex) => {
    let colIndex = 0;
    const rank = 8 - rowIndex;
    for (const char of row) {
      if (isNaN(char)) {
        const file = String.fromCharCode(97 + colIndex);
        const coord = file + rank;
        const targetSquare = document.querySelector(
            '#' + id + ` .square[data-coord="${coord}"]`);
        if (targetSquare) {

          const pieceSpan = document.createElement('span');
          const color = (char === char.toUpperCase()) ? 'white' : 'black';
          const pieceType = char.toLowerCase();
          pieceSpan.classList.add('piece', color, pieceType);
          targetSquare.appendChild(pieceSpan);
        }
        colIndex++;
      } else {
        colIndex += parseInt(char);
      }
    }
  });
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
}