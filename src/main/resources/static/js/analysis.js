import {getPossibleMoves} from "./variables.js";

const boardEl = document.getElementById('chess-board');
let possibleMoves = getPossibleMoves();

// Draw the 8x8 Grid
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

// --- 2. Parse and Render PGN Move List ---
const pgnString = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Bxc6 dxc6 5. O-O Qf6 6. d4 exd4 7. Bg5 Qd6 8. Nxd4 Be7 9. Be3 Nf6 10. f3 c5 11. Ne2 Be6 12. Nf4 O-O-O 13. Qxd6 cxd6 14. Nc3 d5 15. exd5 Nxd5 16. Nfxd5 Bxd5 17. Na4 Bf6 18. Bxc5 b5 19. Nc3 Bxc3 20. bxc3 Bc4 21. Rf2 Rhe8 22. Bd4 f6 23. Rd2 Re6 24. a3 Rde8 25. h4 Re2 26. Rad1 Rxd2 27. Rxd2 Re2 28. Rxe2 Bxe2 29. h5 Kd7 30. Kf2 Bd1 31. h6 gxh6 32. Ke3 Bxc2 33. Bxf6 Ke6 34. Bg7 h5 35. Kd4 a5 36. Be5 a4 37. Bc7 Kf5 38. c4 bxc4 39. Kxc4 Bb3+ 40. Kd4 Bc2 41. Kd5 Bb3+";

const moveHistoryContainer = document.getElementById('move-history');

// 작성해주신 정규식 적용
const regex = /\d+\.\s+.*?(?=\s+\d+\.|$)/g;
const moves = pgnString.match(regex).map(move => move.trim());

moves.forEach(moveStr => {
  // 공백을 기준으로 순번, 백 무브, 흑 무브 분리
  const parts = moveStr.split(/\s+/);
  const moveNum = parts[0];
  const whiteMove = parts[1] || '';
  const blackMove = parts[2] || '';

  const row = document.createElement('div');
  row.className = 'move-entry';

  // 턴 번호 (예: "1.")
  const numDiv = document.createElement('div');
  numDiv.className = 'move-number';
  numDiv.innerText = moveNum;
  row.appendChild(numDiv);

  // 백 무브 (White)
  const wDiv = document.createElement('div');
  wDiv.className = 'move-notation';
  if (whiteMove) {
    const wSpan = document.createElement('span');
    wSpan.className = 'clickable-move';
    wSpan.innerText = whiteMove;
    wSpan.onclick = () => {
      console.log('White move clicked:', whiteMove);
      // TODO: 클릭 시 체스보드 상태를 이 턴으로 돌리는 로직 추가
    };
    wDiv.appendChild(wSpan);
  }
  row.appendChild(wDiv);

  // 흑 무브 (Black)
  const bDiv = document.createElement('div');
  bDiv.className = 'move-notation';
  if (blackMove) {
    const bSpan = document.createElement('span');
    bSpan.className = 'clickable-move';
    bSpan.innerText = blackMove;
    bSpan.onclick = () => {
      console.log('Black move clicked:', blackMove);
      // TODO: 클릭 시 체스보드 상태를 이 턴으로 돌리는 로직 추가
    };
    bDiv.appendChild(bSpan);
  }
  row.appendChild(bDiv);

  moveHistoryContainer.appendChild(row);
});

// 스크롤을 항상 가장 아래로
moveHistoryContainer.scrollTop = moveHistoryContainer.scrollHeight;

let selectedMoveElement = null;
// 1. 우클릭 메뉴 제어
document.addEventListener('contextmenu', function (e) {
  const moveSpan = e.target.closest('.clickable-move');
  if (moveSpan) {
    e.preventDefault();
    selectedMoveElement = moveSpan;

    const menu = document.getElementById('move-context-menu');
    menu.style.display = 'block';
    menu.style.left = e.pageX + 'px';
    menu.style.top = e.pageY + 'px';
  } else {
    hideMenu();
  }
});

// 2. 메뉴 외 클릭 시 숨김
document.addEventListener('click', hideMenu);

function hideMenu() {
  document.getElementById('move-context-menu').style.display = 'none';
}

// 1. 기호 추가 및 색상 클래스 적용
function addSymbol(symbol) {
  if (!selectedMoveElement) {
    return;
  }

  // 기존 기호 및 클래스 초기화
  let baseText = selectedMoveElement.innerText.replace(/[!?]+$/, '');
  selectedMoveElement.innerText = baseText + symbol;
  selectedMoveElement.classList.remove('sig-brilliant', 'sig-excellent',
      'sig-mistake',
      'sig-blunder', 'sig-inaccuracy');

  // 기호에 따른 클래스 부여
  if (symbol === '!!') {
    selectedMoveElement.classList.add('sig-brilliant');
  } else if (symbol === '!') {
    selectedMoveElement.classList.add('sig-excellent');
  } else if (symbol === '??') {
    selectedMoveElement.classList.add('sig-blunder');
  } else if (symbol === '?') {
    selectedMoveElement.classList.add('sig-mistake');
  } else if (symbol === '?!') {
    selectedMoveElement.classList.add('sig-inaccuracy');
  }

  hideMenu();
}

// 2. Annotation 저장 로직
document.querySelector('.editor-footer .btn').addEventListener('click',
    function () {
      const textarea = document.querySelector('.editor-textarea');
      const content = textarea.value.trim();

      if (!content) {
        alert("내용을 입력해주세요.");
        return;
      }

      if (!selectedMoveElement) {
        alert("설명을 추가할 수를 먼저 선택(클릭)해주세요.");
        return;
      }

      // 해당 Move가 속한 행(row) 찾기
      const parentRow = selectedMoveElement.closest('.move-entry');

      // 이미 해당 수 바로 아래 annotation-block이 있는지 확인 (있으면 내용 업데이트, 없으면 신규 생성)
      let nextElement = parentRow.nextElementSibling;
      if (nextElement && nextElement.classList.contains('annotation-block')) {
        nextElement.innerHTML = content;
      } else {
        // 새로운 Annotation Block 생성
        const newBlock = document.createElement('div');
        newBlock.className = 'annotation-block new-save';
        newBlock.innerHTML = content;

        // 클릭한 수의 행 바로 뒤에 삽입
        parentRow.after(newBlock);
      }

      // 저장 후 피드백 및 초기화
      textarea.value = '';
      console.log(`Saved for move: ${selectedMoveElement.innerText}`);
    });

// 하이라이트 초기화 함수
function clearHighlights() {
  document.querySelectorAll('.square').forEach(sq => {
    sq.classList.remove('selected-sq', 'possible-move');
  });
}

/**
 * FEN 문자열을 받아 보드의 기물 배치를 업데이트합니다.
 * @param {string} fen - 예: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
 */
function renderBoardFromFen(fen) {
  // 1. FEN에서 배치 정보(첫 번째 섹션)만 추출
  const piecePlacement = fen.split(' ')[0];
  const rows = piecePlacement.split('/');

  // 2. 모든 칸의 기존 기물 제거
  document.querySelectorAll('.square').forEach(sq => {
    const piece = sq.querySelector('.piece');
    if (piece) {
      piece.remove();
    }
  });

  // 3. FEN 데이터 순회하며 기물 배치
  rows.forEach((row, rowIndex) => {
    let colIndex = 0;
    const rank = 8 - rowIndex; // 8, 7, ..., 1
    for (const char of row) {
      if (isNaN(char)) {
        // 기물인 경우 (p, R, n 등)
        const file = String.fromCharCode(97 + colIndex); // 'a', 'b', ...
        const coord = file + rank;
        const targetSquare = document.querySelector(
            `.square[data-coord="${coord}"]`);

        if (targetSquare) {
          const pieceSpan = document.createElement('span');
          pieceSpan.className = `piece ${char === char.toUpperCase() ? ''
              : 'b'}`;
          pieceSpan.innerText = getPieceSymbol(char); // 기물 유니코드 심볼 함수 호출
          targetSquare.appendChild(pieceSpan);
        }
        colIndex++;
      } else {
        // 숫자(빈 칸)인 경우 숫자만큼 옆으로 이동
        colIndex += parseInt(char);
      }
    }
  });
}

/**
 * 기물 문자에 따른 유니코드 심볼 반환 (옵션)
 */
function getPieceSymbol(char) {
  const symbols = {
    'r': 't', 'n': 'j', 'b': 'n', 'q': 'w', 'k': 'l', 'p': 'o',
    'R': 'r', 'N': 'h', 'B': 'b', 'Q': 'q', 'K': 'k', 'P': 'p'
  };
  return symbols[char] || '';
}