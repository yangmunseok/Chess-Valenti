import {
  currentNode,
  findNodeById,
  gameResultState,
  gameTree,
  setCurrentNode,
  setGameResultState
} from "./state.js";
import {generateFullPgn, loadBoardFromNode, updatePGN} from "./game.js";
import {updateOpeningExplorer} from "./explorer.js";

let selectedMoveElement = null;

// UI 갱신 (전체 트리 재렌더링)
export function refreshMoveHistory(gameResult = gameResultState) {
  const pgnInput = document.getElementById(
      'pgnInput').innerText = generateFullPgn();
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
    const symbolMap = {
      '!!': 'sig-brilliant', '!': 'sig-excellent',
      '?': 'sig-mistake', '??': 'sig-blunder', '?!': 'sig-inaccuracy'
    }
    moveSpan.className = `clickable-move ${symbolMap[node.symbol]}`;
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
    const symbolMap = {
      '!!': 'sig-brilliant', '!': 'sig-excellent',
      '?': 'sig-mistake', '??': 'sig-blunder', '?!': 'sig-inaccuracy'
    }
    moveSpan.className = `clickable-move highlight-move ${symbolMap[curr.symbol]}`;
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

// 각종 UI 이벤트 바인딩
export function initHistoryEvents() {
  // 1. 기보 클릭 시 턴 되돌리기
  document.addEventListener('click', function (e) {
    const moveSpan = e.target.closest('.clickable-move');
    const textarea = document.querySelector('.editor-textarea'); // 주석 입력창

    if (moveSpan) {
      // 선택 하이라이트
      document.querySelectorAll('.clickable-move').forEach(
          el => el.classList.remove("selected-node"));
      selectedMoveElement = moveSpan;
      selectedMoveElement.classList.add("selected-node");

      const nodeId = moveSpan.dataset.nodeId;
      const targetNode = findNodeById(nodeId);
      console.log(targetNode)
      if (targetNode) {
        if (textarea) {
          textarea.value = targetNode.comment || "";
        }

        loadBoardFromNode(targetNode).then(() => {
          setCurrentNode(targetNode);
          renderEvaluataion();
          updateOpeningExplorer();
          document.querySelectorAll('.clickable-move').forEach(
              el => {
                if (el.dataset.nodeId === targetNode.id) {
                  console.log("hi")
                  el.classList.add("selected-node")
                  selectedMoveElement = el
                } else {
                  el.classList.remove("selected-node")
                }
              });
        });
      }
    }
  })
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

  function hideMenu() {
    const menu = document.getElementById('move-context-menu');
    if (menu) {
      menu.style.display = 'none';
    }
  }

  // HTML에 있는 기호 버튼 onclick과 연동 (전역 노출 필요 시 window 객체 활용)
  window.addSymbol = function (symbol) {
    if (!selectedMoveElement) {
      return;
    }
    const selectedNode = findNodeById(selectedMoveElement.dataset.nodeId)
    selectedNode.symbol = symbol;
    let baseText = selectedMoveElement.innerText.replace(/[!?]+$/, '');
    const hasSpace = baseText.endsWith(' ');

    baseText = baseText.trimEnd();
    selectedMoveElement.innerText = baseText + symbol + (hasSpace ? ' ' : '');

    selectedMoveElement.classList.remove('sig-brilliant', 'sig-excellent',
        'sig-mistake', 'sig-blunder', 'sig-inaccuracy');

    if (symbol === '!!') {
      selectedMoveElement.classList.add('sig-brilliant');
    } else if (symbol === '!') {
      selectedMoveElement.classList.add('sig-excellent');
    } else if (symbol === '??') {
      selectedMoveElement.classList.add('sig-blunder');
    } else if (symbol === '?') {
      selectedMoveElement.classList.add('sig-mistake');
    } else if (symbol === '?!') {
      selectedMoveElement.classList.add(
          'sig-inaccuracy');
    }
    hideMenu();
  };

  // 3. Annotation 저장
  document.querySelector('.editor-footer .btn').addEventListener('click',
      function () {
        const textarea = document.querySelector('.editor-textarea');
        const content = textarea.value.trim();

        if (!content || !selectedMoveElement) {
          return;
        }

        const id = selectedMoveElement.dataset.nodeId;
        const targetNode = findNodeById(id);

        if (targetNode) {
          targetNode.comment = content;
          updatePGN();
          refreshMoveHistory();
          textarea.value = '';
        }
      });

  document.querySelector('#btn-first').addEventListener('click', function () {
    const targetNode = gameTree
    loadBoardFromNode(targetNode).then(() => {
      setCurrentNode(targetNode);
      renderEvaluataion();
      document.querySelectorAll('.clickable-move').forEach(
          el => {
            if (el.dataset.nodeId === targetNode.id) {
              el.classList.add("selected-node")
              selectedMoveElement = el
            } else {
              el.classList.remove("selected-node")
            }
          });
    })
  })
  document.querySelector('#btn-last').addEventListener('click', function () {
    let targetNode = gameTree
    while (targetNode.children.length > 0) {
      targetNode = targetNode.children[0];
    }
    loadBoardFromNode(targetNode).then(() => {
      setCurrentNode(targetNode);
      renderEvaluataion();
      document.querySelectorAll('.clickable-move').forEach(
          el => {
            if (el.dataset.nodeId === targetNode.id) {
              el.classList.add("selected-node")
              selectedMoveElement = el
            } else {
              el.classList.remove("selected-node")
            }
          });
    })
  })
  // 요기까지 일단.
  document.querySelector('#go-back-btn').addEventListener(
      'click', function () {
        const targetNode = currentNode.parent;
        if (targetNode.ply === 0) {
          return;
        }

        loadBoardFromNode(targetNode).then(() => {
          //refreshMoveHistory();
          setCurrentNode(targetNode);
          renderEvaluataion();
          document.querySelectorAll('.clickable-move').forEach(
              el => {
                if (el.dataset.nodeId === targetNode.id) {
                  el.classList.add("selected-node")
                  selectedMoveElement = el
                } else {
                  el.classList.remove("selected-node")
                }
              });
        });
      })

  // 요기까지 일단.
  document.querySelector('#go-front-btn').addEventListener(
      'click', function () {

        const targetNode = currentNode.children[0];
        if (!targetNode) {
          return;
        }

        loadBoardFromNode(targetNode).then(() => {
          //refreshMoveHistory();
          setCurrentNode(targetNode);
          renderEvaluataion();
          updateOpeningExplorer();
          document.querySelectorAll('.clickable-move').forEach(
              el => {
                if (el.dataset.nodeId === targetNode.id) {
                  el.classList.add("selected-node")
                  selectedMoveElement = el;
                } else {
                  el.classList.remove("selected-node")
                }
              });
        });
      })
  document.getElementById('menu-toggle-btn')?.addEventListener('click',
      function () {
        const editor = document.getElementById('annotation-editor');
        const menu = document.getElementById('extra-menu');
        const explorer = document.getElementById('opening-explorer');

        // 모든 패널 비활성화 후 에디터/메뉴 토글
        const isMenuVisible = menu.classList.contains('active');

        document.querySelectorAll('.side-pannel .panel').forEach(
            p => p.classList.remove('active'));

        if (isMenuVisible) {
          editor.classList.add('active');
          this.style.color = '';
        } else {
          menu.classList.add('active');
          this.style.color = '#3b82f6';
        }
      });

  document.getElementById('opening-explorer-btn')?.addEventListener('click',
      () => {
        document.querySelectorAll('.side-pannel .panel').forEach(
            p => p.classList.remove('active'));
        document.getElementById('opening-explorer').classList.add('active');
        updateOpeningExplorer();
      });

  document.getElementById('explorer-back-btn')?.addEventListener('click',
      () => {
        document.querySelectorAll('.side-pannel .panel').forEach(
            p => p.classList.remove('active'));
        document.getElementById('extra-menu').classList.add('active');
      });
}

const toggle = document.querySelector('.toggle');
const evaluation = document.querySelector('.engine-evaluation');
const evalFill = document.getElementById('eval-fill');
let evaluationStream = null;

function updateEvalBar(score, isMate = false) {
  if (!evalFill) {
    return;
  }

  let percent;
  if (isMate) {
    percent = score > 0 ? 100 : 0;
  } else {
    // Clamped linear scale: +/- 10.0 -> 100%/0%
    percent = Math.max(0, Math.min(100, 50 + (score / 10) * 50));
  }

  evalFill.style.height = percent + '%';
}

function closeEvaluationStream() {
  if (evaluationStream) {
    evaluationStream.close();
    evaluationStream = null;
  }
}

function renderEvaluationData(data) {
  evaluation.style.display = 'inline-block';

  if (data.pvs && data.pvs.length > 0) {
    if (data.pvs[0].hasOwnProperty("cp") && data.pvs[0].cp !== null) {
      const score = data.pvs[0].cp / 100;
      const displayScore = (score > 0 ? "+" : "") + score.toFixed(2);
      evaluation.textContent = displayScore;
      updateEvalBar(score);
    } else if (data.pvs[0].hasOwnProperty("mate") && data.pvs[0].mate !== null) {
      const mate = data.pvs[0].mate;
      evaluation.textContent = '#' + mate;
      updateEvalBar(mate, true);
    }
  }
}

export const renderEvaluataion = async () => {
  closeEvaluationStream();

  if (currentNode.eval !== "") {
    evaluation.textContent = currentNode.eval;
    // Parse stored eval if available to update bar
    const storedEval = currentNode.eval;
    if (storedEval.startsWith('#')) {
      updateEvalBar(parseInt(storedEval.substring(1)), true);
    } else if (storedEval !== "Mate!") {
      updateEvalBar(parseFloat(storedEval));
    }
    return;
  }

  if (currentNode.move.endsWith('#')) {
    evaluation.textContent = 'Mate!';
    const winnerIsWhite = (currentNode.ply % 2 !== 0);
    updateEvalBar(winnerIsWhite ? 1 : -1, true);
    return;
  }

  if (toggle.checked) {
    const streamFen = currentNode.fen;
    let receivedEvaluation = false;
    evaluationStream = new EventSource(
        `/api/evaluation/stream?fen=${encodeURIComponent(streamFen)}`
    );

    evaluationStream.onmessage = (event) => {
      if (streamFen !== currentNode.fen) {
        closeEvaluationStream();
        return;
      }
      receivedEvaluation = true;
      renderEvaluationData(JSON.parse(event.data));
    };

    evaluationStream.addEventListener('done', () => {
      closeEvaluationStream();
    });

    evaluationStream.onerror = (e) => {
      console.warn("Stockfish eval failed:", e);
      closeEvaluationStream();
      if (!receivedEvaluation) {
        evaluation.style.display = 'none';
      }
    };
  } else {
    evaluation.style.display = 'none';
  }
}

if (toggle) {
  toggle.addEventListener('change', renderEvaluataion);
}

