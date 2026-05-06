import {nodeMap} from "./variables.js";

let selectedMoveElement = null;

// 하이라이트 초기화 함수
export function clearHighlights() {
  document.querySelectorAll('.square').forEach(sq => {
    sq.classList.remove('selected-sq', 'possible-move');
  });
}

// 3. Move 클릭 시 '선택됨' 표시 (어느 수에 저장할지 명시)
document.addEventListener('click', function (e) {
  const moveSpan = e.target.closest('.clickable-move');
  if (moveSpan) {
    // 이전 선택 해제
    document.querySelectorAll('.clickable-move').forEach(
        el => el.style.backgroundColor = '');

    selectedMoveElement = moveSpan;
    // 선택된 수 하이라이트 (Linear 스타일)
    selectedMoveElement.style.backgroundColor = 'rgba(59, 130, 246, 0.2)';
  }
});

document.querySelector('.editor-footer .btn').addEventListener('click',
    function () {
      const textarea = document.querySelector('.editor-textarea');
      const content = textarea.value.trim();

      if (!content || !selectedMoveElement) {
        return;
      }

      // 1. 트리 데이터 업데이트
      const id = selectedMoveElement.dataset.nodeId;
      const targetNode = findNodeById(parseInt(id)); // ply로 노드를 찾는 유틸 함수 필요

      if (targetNode) {
        targetNode.comment = content;

        // 2. PGN 재생성 (저장용)
        updatePGN();

        // 3. UI 업데이트 (기존 주석 블록이 있으면 수정, 없으면 새로고침/생성)
        // 여기서는 간단하게 전체 UI를 다시 그리거나, 현재 노드 아래에 블록을 삽입합니다.
        refreshMoveHistory();
      }
    });

function generatePGN(node) {
  if (node.ply === 0 && node.children.length === 0) {
    return "";
  }

  let pgn = "";
  node.children.forEach((child, index) => {
    const moveNum = child.ply % 2 !== 0 ? Math.ceil(child.ply / 2) + ". " : "";

    // 주석(Comment) 처리
    const commentPart = child.comment ? `{ ${child.comment} } ` : "";

    if (index === 0) { // 메인 라인
      pgn += `${moveNum}${child.move} ${commentPart}`;
      pgn += generatePGN(child);
    } else { // 서브 바리에이션 (괄호 처리)
      const varMoveNum = Math.ceil(child.ply / 2) + (child.ply % 2 === 0
          ? "... " : ". ");
      pgn += `(${varMoveNum}${child.move} ${commentPart}${generatePGN(
          child)}) `;
    }
  });
  return pgn.trim();
}

function updatePGN(gameTree) {
  const fullPGN = generatePGN(gameTree);
  console.log("Current PGN:", fullPGN);
  // 이 데이터를 스토리지에 저장하거나 서버로 전송
}

function findNodeById(id) {
  return nodeMap.get(id);
}