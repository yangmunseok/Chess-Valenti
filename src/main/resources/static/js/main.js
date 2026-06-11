import {initBoard} from "./board.js";
import {initHistoryEvents} from "./history.js";
import {selectPromotion} from "./game.js";
// HTML 파싱이 완료되면 실행
document.addEventListener("DOMContentLoaded", () => {
  // 1. 체스판 HTML 격자 세팅
  initBoard();

  // 2. 우측 패널(Annotation, Click 등) UI 이벤트 초기화
  initHistoryEvents();

  // (선택 사항) 시작 시 FEN을 받아오는 서버 통신이나 기본 배치 로직 호출
  // renderBoardFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");

  const promoButtons = document.querySelectorAll('.promo-btn');

  promoButtons.forEach(button => {
    button.addEventListener('click', function () {
      // 클릭된 버튼의 data-piece 값을 가져옵니다 ('q', 'r' 등)
      console.log("haaaang")
      const selectedPiece = this.getAttribute('data-piece');
      // 이전에 정의한 프로모션 실행 함수를 호출합니다.
      selectPromotion(selectedPiece);
    });
  });
});