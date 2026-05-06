/**
 * FEN 문자열을 받아 보드의 기물 배치를 업데이트합니다.
 * @param {string} fen - 예: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
 */
export function renderBoardFromFen(fen) {
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