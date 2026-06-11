import {setPossibleMoves, gameTree, setCurrentNode} from "./state.js";
import {renderBoardFromFen, updateSquareEvent, initBoard} from "./board.js";
import {initHistoryEvents, refreshMoveHistory} from "./history.js";
import {importPGN} from "./pgnParser.js";
import {selectPromotion} from "./game.js";

/**
 * Analysis 페이지 초기화 함수
 * @param {Object} config 서버에서 전달받은 초기 설정 데이터
 */
export function initAnalysisPage(config) {
  const {fen, legalMove, pgn, uciMoves, idx} = config;

  // 1. 체스판 및 UI 이벤트 초기화
  initBoard();
  initHistoryEvents();

  // 2. 초기 데이터 렌더링
  renderBoardFromFen(fen);
  setPossibleMoves(legalMove);
  importPGN(pgn, uciMoves);

  // 3. 게임 트리 노드 동기화 (기존 로직 유지)
  syncGameTree(idx, fen, legalMove);

  // 4. 전역 UI 이벤트 리스너 등록
  bindUIEvents();
}

/**
 * 서버에서 전달받은 idx에 맞춰 게임 트리 노드를 동기화합니다.
 */
function syncGameTree(idx, fen, legalMove) {
  let node = gameTree;
  for (let i = 0; i < idx; i++) {
    if (node.children.length > 0) {
      node = node.children[0];
    } else {
      break;
    }
  }

  // 현재 노드의 데이터를 컨트롤러에서 전달받은 값으로 초기화
  if (node) {
    node.fen = fen;
    node.legalMove = legalMove;
    setCurrentNode(node);
    refreshMoveHistory();
  }

  // 칸 이벤트 업데이트
  updateSquareEvent();
}

/**
 * 페이지 내 공통 UI 이벤트를 바인딩합니다.
 */
function bindUIEvents() {
  // 프로모션 버튼 이벤트
  const promoButtons = document.querySelectorAll('.promo-btn');
  promoButtons.forEach(button => {
    button.addEventListener('click', function () {
      const selectedPiece = this.getAttribute('data-piece');
      selectPromotion(selectedPiece);
    });
  });

  // PGN 복사 버튼 이벤트
  const copyBtn = document.getElementById('copyPgnBtn');
  if (copyBtn) {
    copyBtn.addEventListener('click', handlePgnCopy);
  }
}

/**
 * PGN 복사 로직 및 피드백 처리
 */
function handlePgnCopy() {
  const pgnInput = document.getElementById('pgnInput');
  const btnText = this.querySelector('.btn-text');
  if (!pgnInput || !btnText) return;

  const originalBtn = this;
  const originalText = btnText.textContent;

  // 클립보드 복사 실행
  pgnInput.select();
  navigator.clipboard.writeText(pgnInput.value).then(() => {
    // 복사 성공 시 시각적 피드백
    btnText.textContent = 'Copied!';
    originalBtn.style.borderColor = '#666';

    setTimeout(() => {
      btnText.textContent = originalText;
      originalBtn.style.borderColor = '';
    }, 1500);
  });
}
