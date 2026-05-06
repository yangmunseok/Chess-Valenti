import {getPossibleMoves, nodeMap, setPossibleMoves} from "./variables.js";
import {renderBoardFromFen} from "./renderBoardFromFen.js";
import {clearHighlights} from "./utils.js";

let possibleMoves = getPossibleMoves();
let nodeIdCounter = 0; // 고유 ID를 생성하기 위한 카운터

class ChessNode {
  constructor(move, ply, parent = null) {
    this.id = `node-${nodeIdCounter++}`; // 'node-0', 'node-1' 식으로 고유함 보장
    this.move = move;      // 예: "e4", "Nf3"
    this.ply = ply;        // 수 번호 (1.e4 = 1, 1...e5 = 2)
    this.parent = parent;
    this.children = [];    // [0]은 메인 라인, 나머지는 서브 바리에이션
    this.comment = "";     // 어노테이션 저장
    this.fen = ""; // 해당 수 이후의 보드 상태 저장
  }
}

// 트리 관리 객체
let gameTree = new ChessNode("Root", 0);
let currentNode = gameTree; // 현재 보드 상태가 가리키는 노드

function getMoveNotation(from, to, pieceText) {
  // 1. 현재 선택한 이동(from-to)을 제외하고, 동일한 목적지(to)를 가진 수들을 필터링
  const ambiguousMoves = possibleMoves.filter(m => {
    const mFrom = m.substring(0, 2);
    const mTo = m.substring(2, 4);

    // 목적지는 같지만 출발지는 다른 수들 중에서
    if (mTo === to && mFrom !== from) {
      const otherPiece = document.querySelector(
          `.square[data-coord="${mFrom}"]`).textContent;
      // 출발지에 있는 기물 종류(textContent)까지 같은 경우
      return otherPiece === pieceText;
    }
    return false;
  });

  const toPiece = document.querySelector(
      `.square[data-coord="${to}"]`).textContent;

  let capture = "";

  if (toPiece) {
    capture = "x";
  }

  console.log("toPiece:", toPiece)

  const pieceTextToSanText = {
    "b": "B",
    "n": "B",
    "h": "N",
    "j": "N",
    "r": "R",
    "t": "R",
    "w": "Q",
    "q": "Q",
    "k": "K",
    "l": "K",
    "o": (toPiece) ? from.substring(0, 1) : "",
    "p": (toPiece) ? from.substring(0, 1) : ""
  }

  // 2. 분기 처리
  if (ambiguousMoves.length > 0) {
    // [Case 1] 목적지가 같고 기물 종류도 같은 다른 출발지가 존재함 (중복 발생)
    // 예: 두 개의 나이트가 같은 칸으로 갈 수 있는 경우 (Nbd7 vs Nfd7 구분이 필요함)
    console.log("Case 1: Ambiguity detected. Need to specify file or rank.");
    let isRankDuplicated = false;
    let isFileDuplicated = false;
    for (let i = 0; i < ambiguousMoves.length; i++) {
      const mRank = ambiguousMoves[i].substring(1, 2);
      const fromRank = from.substring(1, 2);
      const mFile = ambiguousMoves[i].substring(0, 1);
      const fromFile = from.substring(0, 1);
      if (mRank === fromRank) {
        isRankDuplicated = true;
      }
      if (mFile === fromFile) {
        isFileDuplicated = true;
      }
    }

    let dup = "";
    if (isRankDuplicated) {
      dup += from.substring(0, 1);
    }
    if (isFileDuplicated) {
      dup += from.substring(1, 2);
    }

    return pieceTextToSanText[pieceText] + dup + capture + to;
  } else {
    // [Case 2] 그렇지 않음 (해당 기물 종류로는 이 칸에 갈 수 있는 유일한 기물임)
    // 예: e4, Nf3 등 일반적인 표기
    console.log("Case 2: No ambiguity.");
    return pieceTextToSanText[pieceText] + capture + to;
  }
}

let selectedSquare = null; // 'from'을 기억하는 변수

export function executeMove(from, to) {

  const fromSquare = document.querySelector(`.square[data-coord="${from}"]`);
  const toSquare = document.querySelector(`.square[data-coord="${to}"]`);
  const piece = fromSquare.querySelector('.piece');
  const pieceText = piece.textContent;

  const moveNotation = getMoveNotation(from, to, pieceText);
  console.log(moveNotation)
  // 1. 트리 노드 생성 또는 이동
  let existingChild = currentNode.children.find(
      child => child.move === moveNotation);

  if (existingChild) {
    // 이미 존재하는 수라면 해당 노드로 이동
    currentNode = existingChild;
  } else {
    // 새로운 수라면 노드 생성 (Subvariation 처리)
    const newNode = new ChessNode(moveNotation, currentNode.ply + 1,
        currentNode);
    nodeMap.set(newNode.id, newNode);
    currentNode.children.push(newNode);

    if (currentNode.children.length > 1) {
      // 이미 다음 수가 있는데 새로운 수가 들어온 경우 -> 서브 바리에이션으로 렌더링
      renderVariation(newNode);
    } else {
      // 메인 라인 추가
      renderMainLine(newNode);
    }
    currentNode = newNode;
  }

  // 2. 실제 DOM 기물 이동
  fetch("board",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({san: getPathToNodeSAN(currentNode)})
      }).then(
      async (body) => {
        const json = await body.json();
        renderBoardFromFen(json["fen"]);
        setPossibleMoves(json["legalMove"]);
        updateSquareEvent();
      })
  // 3. PGN 업데이트
  updatePGN();
  clearHighlights();
}

export function updateSquareEvent() {
  console.log("updateSquareEvent Invoked");
  possibleMoves = getPossibleMoves();
  console.log(possibleMoves);
  document.querySelectorAll('.square').forEach(square => {
    square.onclick = function () {
      const clickedCoord = this.dataset.coord; // 현재 클릭한 칸 (a2, e4 등)

      // --- 상황 1: 이동 실행 (두 번째 클릭) ---
      // 클릭한 칸이 '이동 가능한 칸' 하이라이트가 되어 있다면?
      if (this.classList.contains('possible-move')) {
        const from = selectedSquare;
        const to = clickedCoord;

        // 1. 실제 기물 이동 함수 호출
        executeMove(from, to);

        // 2. 이동 후 상태 초기화
        clearHighlights();
        selectedSquare = null;
        return;
      }

      // --- 상황 2: 기물 선택 (첫 번째 클릭) ---
      clearHighlights(); // 기존 하이라이트 제거

      // 클릭한 칸에서 출발하는 수들이 있는지 확인
      const targets = possibleMoves
      .filter(move => move.substring(0, 2) === clickedCoord)
      .map(move => move.substring(2, 4));

      if (targets.length > 0) {
        // 'from' 좌표 저장
        selectedSquare = clickedCoord;
        this.classList.add('selected-sq');

        // 갈 수 있는 'to' 칸들에 하이라이트 뿌리기
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

function renderMainLine(node) {
  const moveHistory = document.getElementById('move-history');
  // 홀수 ply는 새로운 행(row) 생성, 짝수 ply는 기존 행에 추가
  if (node.ply % 2 !== 0) {
    const row = document.createElement('div');
    row.className = 'move-entry';
    row.innerHTML = `<div class="move-number">${Math.ceil(node.ply / 2)}.</div>
                         <div class="move-notation"><span class="clickable-move" data-node-id="${node.ply}">${node.move}</span></div>
                         <div class="move-notation"></div>`;
    moveHistory.appendChild(row);
  } else {
    const lastRow = moveHistory.lastElementChild;
    const notations = lastRow.querySelectorAll('.move-notation');
    notations[1].innerHTML = `<span class="clickable-move" data-node-id="${node.ply}">${node.move}</span>`;
  }
}

function renderVariation(node) {
  // 이미지에서 본 것처럼 별도의 annotation-block 안에 서브 라인 생성
  const parentRow = document.querySelector(
      `[data-node-id="${node.parent.ply}"]`).closest('.move-entry');
  let variationBlock = parentRow.nextElementSibling;

  if (!variationBlock || !variationBlock.classList.contains(
      'annotation-block')) {
    variationBlock = document.createElement('div');
    variationBlock.className = 'annotation-block';
    parentRow.after(variationBlock);
  }

  const varSpan = document.createElement('span');
  varSpan.className = 'variation-line';
  varSpan.innerHTML = `<span class="highlight-move">(${Math.ceil(
      node.ply / 2)}${node.ply % 2 == 0 ? '...' : '.'} ${node.move})</span> `;
  variationBlock.appendChild(varSpan);
}

function renderNodeUI(node) {
  const moveHistory = document.getElementById('move-history');

  // Case A: 서브 바리에이션 (부모의 첫 번째 자식이 아닌 경우)
  if (node.parent && node.parent.children[0] !== node) {
    // ply가 아닌 고유한 node.id로 부모 요소를 찾음
    const parentSpan = document.querySelector(
        `[data-node-id="${node.parent.id}"]`);
    if (!parentSpan) {
      return;
    } // 부모가 렌더링 안 되어 있으면 중단

    const parentRow = parentSpan.closest('.move-entry') || parentSpan.closest(
        '.annotation-block');

    let varBlock = parentRow.nextElementSibling;
    // 바로 뒤에 블록이 없거나 다른 용도의 블록이면 새로 생성
    if (!varBlock || !varBlock.classList.contains('annotation-block')) {
      varBlock = document.createElement('div');
      varBlock.className = 'annotation-block';
      parentRow.after(varBlock);
    }

    // 수 표시 (괄호 및 반수 번호 계산)
    const moveNum = Math.ceil(node.ply / 2) + (node.ply % 2 === 0 ? "... "
        : ". ");
    const varSpan = document.createElement('span');
    varSpan.className = 'highlight-move';
    // data-node-id에 고유한 node.id 저장
    varSpan.innerHTML = `<span class="clickable-move" data-node-id="${node.id}">${moveNum}${node.move}</span> `;
    varBlock.appendChild(varSpan);

    // 주석이 있다면 같은 블록 내에 텍스트 추가
    if (node.comment) {
      const commentSpan = document.createElement('span');
      commentSpan.className = 'comment-text';
      commentSpan.innerText = ` { ${node.comment} } `;
      varBlock.appendChild(commentSpan);
    }
  }

  // Case B: 메인 라인 (부모의 첫 번째 자식인 경우)
  else {
    let targetRow;

    if (node.ply % 2 !== 0) {
      // 백의 수: 새로운 행 생성
      targetRow = document.createElement('div');
      targetRow.className = 'move-entry';
      targetRow.innerHTML = `
        <div class="move-number">${Math.ceil(node.ply / 2)}.</div>
        <div class="move-notation"><span class="clickable-move" data-node-id="${node.id}">${node.move}</span></div>
        <div class="move-notation"></div>`;
      moveHistory.appendChild(targetRow);
    } else {
      // 흑의 수: 마지막 행의 두 번째 칸에 추가
      targetRow = moveHistory.lastElementChild;
      const notations = targetRow.querySelectorAll('.move-notation');
      notations[1].innerHTML = `<span class="clickable-move" data-node-id="${node.id}">${node.move}</span>`;
    }

    // 메인 라인에서도 주석이 있다면 아래에 별도 블록 생성
    if (node.comment) {
      const commentDiv = document.createElement('div');
      commentDiv.className = 'annotation-block new-save';
      commentDiv.innerText = node.comment;
      targetRow.after(commentDiv);
    }
  }
}

function refreshMoveHistory() {
  const moveHistory = document.getElementById('move-history');

  // 1. 기존 UI 초기화 (시작 문구 제외하고 비우기)
  moveHistory.innerHTML = `
        <div class="move-row starting">
            <div class="dot-indicator"></div>
            Starting position
        </div>
    `;

  // 2. 재귀 함수를 호출하여 트리 순회 시작
  // gameTree.children을 순회하며 메인라인과 바리에이션을 그림
  renderTree(gameTree);

  // 3. 스크롤을 마지막으로 이동
  moveHistory.scrollTop = moveHistory.scrollHeight;
}

/**
 * 트리를 순회하며 노드를 UI에 배치하는 핵심 재귀 함수
 */
function renderTree(parentNode) {
  if (!parentNode.children || parentNode.children.length === 0) {
    return;
  }

  parentNode.children.forEach((node, index) => {
    // 첫 번째 자식은 메인 라인(or 현재 바리에이션의 주 경로)
    // 그 외 자식들은 새로운 서브 바리에이션 블록으로 처리
    if (index === 0) {
      renderNodeUI(node); // 이전에 만든 UI 렌더링 함수 호출
      renderTree(node);   // 다음 수로 계속 진행
    } else {
      // 서브 바리에이션은 별도의 로직으로 렌더링 후 해당 가지(branch) 추적
      renderNodeUI(node);
      renderTree(node);
    }
  });
}

/**
 * 현재 노드까지의 메인 경로를 SAN(Standard Algebraic Notation) 형식으로 반환
 * @param {ChessNode} targetNode - 현재 보드 상태를 나타내는 노드
 * @returns {string} - "1. e4 e5 2. c4 f6" 형식의 문자열
 */
function getPathToNodeSAN(targetNode) {
  let path = [];
  let curr = targetNode;

  // 1. 루트(ply 0)에 도달할 때까지 부모를 타고 올라가며 배열에 저장
  while (curr && curr.ply > 0) {
    path.unshift(curr); // 배열 맨 앞에 추가하여 순서 유지
    curr = curr.parent;
  }

  // 2. 경로 배열을 순회하며 번호와 함께 문자열 생성
  let sanParts = [];
  path.forEach((node) => {
    // 백의 수일 때만 번호를 붙임 (ply 1, 3, 5...)
    if (node.ply % 2 !== 0) {
      const moveNum = Math.ceil(node.ply / 2);
      sanParts.push(`${moveNum}. ${node.move}`);
    } else {
      // 흑의 수는 번호 없이 move만 추가
      sanParts.push(node.move);
    }
  });

  return sanParts.join(' ');
}

// 1. 기보 클릭 이벤트 리스너 업데이트
document.addEventListener('click', function (e) {
  const moveSpan = e.target.closest('.clickable-move');
  if (moveSpan) {
    const nodeId = moveSpan.dataset.nodeId;
    const targetNode = findNodeById(nodeId);

    if (targetNode) {
      // 보드 업데이트
      renderBoardFromFen(targetNode.fen);
      // 현재 노드 위치 업데이트
      currentNode = targetNode;
      // 선택 하이라이트 UI 처리
      //updateSelectionUI(moveSpan);
    }
  }
});

// 2. 새로운 수 추가 시 FEN 저장 로직
function onMoveExecuted(moveSan, newFen) {
  let newNode = new ChessNode(moveSan, currentNode.ply + 1, currentNode);
  newNode.fen = newFen; // 이동 후의 FEN을 노드에 저장

  nodeMap.set(newNode.id, newNode);
  currentNode.children.push(newNode);
  currentNode = newNode;

  refreshMoveHistory();
}