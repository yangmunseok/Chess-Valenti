import {
  ChessNode,
  currentNode,
  getPossibleMoves,
  nodeMap,
  setCurrentNode,
  setPossibleMoves
} from "./state.js";
import {bindMoveInput, clearHighlights, drawSymbol, renderBoardFromFen} from "./board.js";
import {analyzePosition, ensurePosition, isPromotion, playMove, withoutCheckMark} from "./position.js";

// 수를 둔 뒤 부를 함수. 기보와 평가 화면을 갱신하는 일은 analysis-init.js 가 정해서 넘긴다.
// game.js 가 history.js 등을 직접 import 하면 서로를 import 하는 순환이 생긴다.
let afterMove = () => {};

export function onMoveExecuted(listener) {
  afterMove = listener;
}

let pendingMove = null;

function showPromotionChoice(from, to) {
  const color = currentNode.fen.split(" ")[1] === "w" ? "white" : "black";
  document.querySelectorAll(".promotion-options div").forEach(el => {
    el.classList.remove("white", "black");
    el.classList.add(color);
  });
  document.getElementById("promotion-overlay").style.display = "block";
  pendingMove = {from, to};
}

export async function executeMove(from, to, promotionPiece = null) {
  if (isPromotion(getPossibleMoves(), from, to) && promotionPiece === null) {
    showPromotionChoice(from, to);
    return;
  }
  pendingMove = null;

  const move = playMove(ensurePosition(currentNode).fen, from, to, promotionPiece);

  // 이미 있는 수면 그 노드로 간다. PGN 에서 읽은 수는 체크 표시(+)가 없을 수 있어서 빼고 비교한다.
  const existingChild = currentNode.children.find(
      child => withoutCheckMark(child.move) === withoutCheckMark(move.san));

  if (existingChild) {
    setCurrentNode(existingChild);
  } else {
    const newNode = new ChessNode(move.san, currentNode.ply + 1, currentNode);
    newNode.uci = move.uci;
    newNode.fen = move.fen;
    newNode.playPath = currentNode.playPath
        ? (currentNode.playPath + "," + move.uci)
        : move.uci;

    nodeMap.set(newNode.id, newNode);
    currentNode.children.push(newNode);
    setCurrentNode(newNode);
  }

  await loadBoardFromNode(currentNode);
  afterMove();
}

// node 국면을 보드에 그리고 그 국면의 합법 수로 칸 클릭을 연결한다
export async function loadBoardFromNode(node) {
  if (!node) {
    return;
  }

  clearHighlights();

  try {
    ensurePosition(node);
  } catch (err) {
    console.error(`Cannot play ${node.move} at ply ${node.ply}:`, err);
    return;
  }

  if (!node.legalMove || node.legalMove.length === 0) {
    const position = analyzePosition(node.fen);
    node.legalMove = position.legalMoves;
    if (node.ply > 0) {
      if (position.checkmate && !node.move.endsWith('#')) {
        node.move += '#';
      } else if (position.inCheck && !position.checkmate && !node.move.endsWith('+')) {
        node.move += '+';
      }
    }
  }

  renderBoardFromFen(node.fen);

  // Visual symbol feedback
  if (node.symbol && node.uci && node.uci.length >= 4) {
    drawSymbol(node.uci.substring(2, 4), node.symbol);
  }

  setPossibleMoves(node.legalMove);
  bindMoveInput(getPossibleMoves(), executeMove);
}

export function selectPromotion(pieceType) {
  document.getElementById("promotion-overlay").style.display = "none";
  executeMove(pendingMove.from, pendingMove.to, pieceType);
}
