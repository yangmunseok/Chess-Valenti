// 기보(수 목록) 렌더링과 기보 조작: 수 클릭과 이동 버튼, 기호(!, ?) 붙이기, 주석 저장
import {
  currentNode,
  findNodeById,
  gameResultState,
  gameTree,
  setCurrentNode,
  setGameResultState
} from "./state.js";
import {loadBoardFromNode} from "./game.js";
import {drawSymbol} from "./board.js";
import {generatePgn} from "./pgn.js";
import {renderEvaluation} from "./evaluation.js";
import {updateOpeningExplorer} from "./explorer.js";

const SYMBOL_CLASSES = {
  '!!': 'sig-brilliant', '!': 'sig-excellent',
  '?': 'sig-mistake', '??': 'sig-blunder', '?!': 'sig-inaccuracy'
};

let selectedMoveElement = null;

// UI 갱신 (전체 트리 재렌더링)
export function refreshMoveHistory(gameResult = gameResultState) {
  const pgnInput = document.getElementById(
      'pgnInput');
  if (pgnInput) {
    pgnInput.value = generatePgn(gameTree);
  }
  const moveHistory = document.getElementById('move-history');
  moveHistory.innerHTML = `<div class="move-row starting">Starting position</div>`;

  if (gameTree.children.length > 0) {
    renderNodeRecursive(gameTree.children[0], moveHistory, true);
  }

  // 게임 결과가 있다면 마지막에 추가
  if (gameResult && gameResult !== "") {
    const resultDiv = document.createElement('div');
    resultDiv.className = 'game-result-display';
    resultDiv.innerText = gameResult;
    setGameResultState(gameResult);
    moveHistory.appendChild(resultDiv);
  }

  moveHistory.scrollTop = moveHistory.scrollHeight;
}

function renderNodeRecursive(node, container, isMainLine) {
  if (isMainLine) {
    // --- [메인 라인 렌더링] ---
    const isWhite = node.ply % 2 !== 0;
    const moveNum = Math.ceil(node.ply / 2);
    let targetRow;

    const lastElement = container.lastElementChild;
    const prevWasComment = lastElement && lastElement.classList.contains(
        'annotation-block');

    if (isWhite || prevWasComment || !lastElement.classList.contains(
        'move-entry')) {
      targetRow = document.createElement('div');
      targetRow.className = 'move-entry';
      targetRow.innerHTML = `
        <div class="move-number">${moveNum}.</div>
        <div class="move-notation white-part"></div>
        <div class="move-notation black-part">${isWhite ? '...' : ''}</div>`;
      container.appendChild(targetRow);
    } else {
      targetRow = lastElement;
    }

    // 수 삽입
    const moveSpan = document.createElement('span');
    moveSpan.className = `clickable-move ${SYMBOL_CLASSES[node.symbol]}`;
    if (node === currentNode) {
      moveSpan.classList.add('selected-node');
      selectedMoveElement = moveSpan;
    }
    moveSpan.dataset.nodeId = node.id;
    moveSpan.innerText = node.move + node.symbol;
    targetRow.querySelector(
        isWhite ? '.white-part' : '.black-part').innerHTML = '';
    targetRow.querySelector(
        isWhite ? '.white-part' : '.black-part').appendChild(moveSpan);

    // 1. [핵심] 해당 수의 서브 바리에이션을 '수' 바로 다음에 배치 (다음 수보다 먼저!)
    if (node.parent && node.parent.children.length > 1) {
      const brothers = node.parent.children;
      // 현재 노드가 메인(0번)일 때, 1번부터의 형제들이 이 수의 바리에이션임
      for (let i = 1; i < brothers.length; i++) {
        // 형제 노드가 현재 노드와 같은 순서라면 (즉, 현재 노드의 대체수라면)
        if (node === brothers[0]) {
          renderVariationBlock(brothers[i], container, true);
        }
      }
    }

    // 2. 주석 처리 (중괄호 없이 블록으로)
    if (node.comment) {
      const commentDiv = document.createElement('div');
      commentDiv.className = 'annotation-block';
      commentDiv.innerText = node.comment; // 중괄호 제거
      container.appendChild(commentDiv);
      if (isWhite) {
        targetRow.querySelector('.black-part').innerText = '...';
      }
    }

    // 3. 다음 메인 수 렌더링
    if (node.children.length > 0) {
      renderNodeRecursive(node.children[0], container, true);
    }

  }
}

/**
 * @param {ChessNode} startNode - 바리에이션의 시작 노드
 * @param {HTMLElement} container - 부모 컨테이너
 * @param {boolean} isTopLevelVariation - 메인 라인에서 직접 갈라진 것인지 여부
 */
function renderVariationBlock(startNode, container,
    isTopLevelVariation = false) {
  const varBlock = document.createElement('div');

  // 1. 클래스 할당: 메인에서 직접 빠지면 박스(annotation-block), 중첩이면 라인(variation-line)
  if (isTopLevelVariation) {
    varBlock.className = 'annotation-block variation-container';
  } else {
    varBlock.className = 'variation-line';
  }

  container.appendChild(varBlock);

  const content = document.createElement('div');
  content.className = 'variation-content';
  // 괄호 스팬 생성을 제거했습니다.
  varBlock.appendChild(content);

  let curr = startNode;
  while (curr) {
    const isWhite = curr.ply % 2 !== 0;
    const moveNum = Math.ceil(curr.ply / 2);

    // 첫 수이거나 백의 수일 때 번호 표시
    const prefix = (isWhite || curr === startNode) ? `${moveNum}${isWhite ? '.'
        : '...'} ` : '';

    const moveSpan = document.createElement('span');
    moveSpan.className = `clickable-move highlight-move ${SYMBOL_CLASSES[curr.symbol]}`;
    moveSpan.dataset.nodeId = curr.id;
    moveSpan.innerText = `${prefix}${curr.move}${curr.symbol} `;
    content.appendChild(moveSpan);

    if (curr === currentNode) {
      moveSpan.classList.add('selected-node');
      selectedMoveElement = moveSpan;
    }

    // 주석 처리
    if (curr.comment) {
      const commentSpan = document.createElement('span');
      commentSpan.className = 'var-comment';
      commentSpan.innerText = ` ${curr.comment} `;
      content.appendChild(commentSpan);
    }

    // [중요] 중첩 바리에이션 처리 (가지 안의 가지)
    if (curr.children.length > 1) {
      for (let j = 1; j < curr.children.length; j++) {
        // 중첩 호출 시 isTopLevelVariation을 false로 전달
        renderVariationBlock(curr.children[j], content, false);
      }
    }

    curr = curr.children[0];
  }
}

// 기보에서 nodeId 수를 선택 표시한다
function markSelectedMove(nodeId) {
  document.querySelectorAll('.clickable-move').forEach(el => {
    if (el.dataset.nodeId === nodeId) {
      el.classList.add("selected-node");
      selectedMoveElement = el;
    } else {
      el.classList.remove("selected-node");
    }
  });
}

// targetNode 국면으로 이동한다. 기보 클릭과 이동 버튼이 함께 쓴다.
function goToNode(targetNode) {
  if (!targetNode) {
    return;
  }
  loadBoardFromNode(targetNode).then(() => {
    setCurrentNode(targetNode);
    renderEvaluation();
    updateOpeningExplorer();
    markSelectedMove(targetNode.id);
  });
}

function hideMenu() {
  const menu = document.getElementById('move-context-menu');
  if (menu) {
    menu.style.display = 'none';
  }
}

// 우클릭 메뉴에서 고른 기호를 선택한 수에 붙인다
function addSymbol(symbol) {
  if (!selectedMoveElement) {
    return;
  }
  const selectedNode = findNodeById(selectedMoveElement.dataset.nodeId);
  selectedNode.symbol = symbol;
  refreshMoveHistory();

  // Draw symbol on board if it's the current node
  if (selectedNode === currentNode && selectedNode.uci) {
    const to = selectedNode.uci.substring(2, 4);
    drawSymbol(to, symbol);
  }

  hideMenu();
}

function lastMainLineNode() {
  let node = gameTree;
  while (node.children.length > 0) {
    node = node.children[0];
  }
  return node;
}

function showPanel(panelId) {
  document.querySelectorAll('.side-pannel .panel').forEach(
      p => p.classList.remove('active'));
  document.getElementById(panelId).classList.add('active');
}

// 각종 UI 이벤트 바인딩
export function initHistoryEvents() {
  // 1. 기보 클릭 시 그 국면으로 이동
  document.addEventListener('click', function (e) {
    const moveSpan = e.target.closest('.clickable-move');
    if (!moveSpan) {
      return;
    }
    const targetNode = findNodeById(moveSpan.dataset.nodeId);
    if (!targetNode) {
      return;
    }
    markSelectedMove(targetNode.id);
    const textarea = document.querySelector('.editor-textarea'); // 주석 입력창
    if (textarea) {
      textarea.value = targetNode.comment || "";
    }
    goToNode(targetNode);
  });

  // 2. 우클릭 메뉴 및 기호
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

  document.addEventListener('click', hideMenu);

  document.querySelectorAll('#move-context-menu [data-symbol]').forEach(item =>
      item.addEventListener('click', () => addSymbol(item.dataset.symbol)));

  // 3. Annotation 저장
  document.querySelector('.editor-footer .btn').addEventListener('click',
      function () {
        const textarea = document.querySelector('.editor-textarea');
        const content = textarea.value.trim();

        if (!content || !selectedMoveElement) {
          return;
        }

        const targetNode = findNodeById(selectedMoveElement.dataset.nodeId);
        if (targetNode) {
          targetNode.comment = content;
          refreshMoveHistory();
          textarea.value = '';
        }
      });

  // 4. 이동 버튼
  document.querySelector('#btn-first').addEventListener('click', () => goToNode(gameTree));
  document.querySelector('#btn-last').addEventListener('click',
      () => goToNode(lastMainLineNode()));
  document.querySelector('#go-back-btn').addEventListener('click',
      () => goToNode(currentNode.parent));
  document.querySelector('#go-front-btn').addEventListener('click',
      () => goToNode(currentNode.children[0]));

  // 5. 옆 패널 전환
  document.getElementById('menu-toggle-btn')?.addEventListener('click',
      function () {
        // 모든 패널 비활성화 후 에디터/메뉴 토글
        const isMenuVisible = document.getElementById('extra-menu').classList.contains('active');
        if (isMenuVisible) {
          showPanel('annotation-editor');
          this.style.color = '';
        } else {
          showPanel('extra-menu');
          this.style.color = '#3b82f6';
        }
      });

  document.getElementById('opening-explorer-btn')?.addEventListener('click',
      () => {
        showPanel('opening-explorer');
        updateOpeningExplorer();
      });

  document.getElementById('explorer-back-btn')?.addEventListener('click',
      () => showPanel('extra-menu'));
}
