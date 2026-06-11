import {
  ChessNode,
  currentNode,
  gameTree,
  getPossibleMoves,
  nodeMap,
  setCurrentNode,
  setPossibleMoves
} from "./state.js";
import {drawSymbol, renderBoardFromFen, updateSquareEvent} from "./board.js";
import {refreshMoveHistory, renderEvaluataion} from "./history.js";

function getMoveNotation(from, to, pieceText) {
  //check if castle or not
  if (pieceText === "k") {
    if (from === "e1" && to === "g1") {
      return "O-O";
    }
    if (from === "e1" && to === "c1") {
      return "O-O-O";
    }
    if (from === "e8" && to === "g8") {
      return "O-O";
    }
    if (from === "e8" && to === "c8") {
      return "O-O-O";
    }
  }

  const possibleMoves = getPossibleMoves();
  const ambiguousMoves = possibleMoves.filter(m => {
    const mFrom = m.substring(0, 2);
    const mTo = m.substring(2, 4);
    if (mTo === to && mFrom !== from) {
      return document.querySelector(
          `.square[data-coord="${mFrom}"] span`).classList.contains(pieceText);
    }
    return false;
  });

  const toSquareClass = document.querySelector(
      `.square[data-coord="${to}"]`);
  const toPiece = toSquareClass.querySelector('.piece');

  let capture = toPiece ? "x" : "";

  const pieceTextToSanText = {
    "b": "B",
    "n": "N",
    "r": "R",
    "q": "Q",
    "k": "K",
    "p": toPiece ? from.substring(0, 1) : ""
  };

  if (pieceText === "p") {
    if (to.endsWith("8") || to.endsWith("1")) {
      return pieceTextToSanText[pieceText] + capture + to + '=';
    }
    return pieceTextToSanText[pieceText] + capture + to;
  }

  if (ambiguousMoves.length > 0) {
    let isRankDuplicated = false;
    let isFileDuplicated = false;
    for (let i = 0; i < ambiguousMoves.length; i++) {
      if (ambiguousMoves[i].substring(1, 2) === from.substring(1,
          2)) {
        isRankDuplicated = true;
      }
      if (ambiguousMoves[i].substring(0, 1) === from.substring(0,
          1)) {
        isFileDuplicated = true;
      }
    }

    let dup = "";
    if (isFileDuplicated) {
      dup += from.substring(1, 2);
    } else if (isRankDuplicated) {
      dup += from;
    } else {
      dup += from.substring(0, 1);
    }

    return pieceTextToSanText[pieceText] + dup + capture + to;
  } else {
    return pieceTextToSanText[pieceText] + capture + to;
  }
}

export async function executeMove(from, to, promotionPiece = null) {
  const fromSquare = document.querySelector(`.square[data-coord="${from}"]`);
  const piece = fromSquare.querySelector('.piece')
  const classes = [...piece.classList];
  const pieceText = classes.find(c => !["piece", "white", "black"].includes(c));
  let moveNotation = getMoveNotation(from, to, pieceText);

  if (moveNotation.endsWith('=')) {
    if (promotionPiece === null) {
      document.querySelectorAll(".promotion-options div").forEach(el => {
        if (piece.classList.contains("white")) {
          el.classList.remove("black");
          el.classList.add("white");
          return;
        }
        el.classList.remove("white");
        el.classList.add("black");
      });
      document.getElementById("promotion-overlay").style.display = "block"
      pendingMove = {from, to, moveNotation};
      return;
    }
    pendingMove = null;
    moveNotation += promotionPiece.toUpperCase();
  }

  const uciMove = from + to + (promotionPiece ? promotionPiece.toLowerCase()
      : "");

  let existingChild = currentNode.children.find(
      child => child.move === moveNotation);

  if (existingChild) {
    setCurrentNode(existingChild);
  } else {
    const newNode = new ChessNode(moveNotation, currentNode.ply + 1,
        currentNode);
    newNode.uci = uciMove;
    newNode.playPath = currentNode.playPath
        ? (currentNode.playPath + "," + uciMove)
        : uciMove;

    nodeMap.set(newNode.id, newNode);
    currentNode.children.push(newNode);
    setCurrentNode(newNode);
  }

  await loadBoardFromNode(currentNode);
  refreshMoveHistory();
  renderEvaluataion();
  updatePGN();
}

export function getPathToNodeSAN(targetNode) {
  let path = [];
  let curr = targetNode;
  while (curr && curr.ply > 0) {
    path.unshift(curr);
    curr = curr.parent;
  }
  let sanParts = [];
  path.forEach((node) => {
    if (node.ply % 2 !== 0) {
      sanParts.push(
          `${Math.ceil(node.ply / 2)}. ${node.move}`);
    } else {
      sanParts.push(node.move);
    }
  });
  return sanParts.join(' ');
}

// PGN 생성 및 로깅
export function updatePGN() {
  const fullPGN = generatePGN(gameTree);
  //console.log("Current Full PGN:\n", fullPGN);
}

export function generateFullPgn() {
  return generatePGN(gameTree);
}

function generatePGN(node) {
  if (node.ply === 0 && node.children.length === 0) {
    return "";
  }
  let pgn = "";
  node.children.forEach((child, index) => {
    const moveNum = child.ply % 2 !== 0 ? Math.ceil(child.ply / 2) + ". " : "";
    const commentPart = child.comment ? `{ ${child.comment} } ` : "";

    if (index === 0) {
      pgn += `${moveNum}${child.move}${child.symbol} ${commentPart}`;
      pgn += generatePGN(child);
    } else {
      const varMoveNum = Math.ceil(child.ply / 2) + (child.ply % 2 === 0
          ? "... " : ". ");
      pgn += `(${varMoveNum}${child.move}${child.symbol} ${commentPart}${generatePGN(
          child)}) `;
    }
  });
  return pgn.trim();
}

export async function loadBoardFromNode(node) {
  if (!node) {
    return;
  }

  if (node.fen) {
    if (!node.legalMove || node.legalMove.length === 0) {
      // Only fetch if it's NOT a root node (root should have legal moves initialized or fetched)
      // Actually, let's fetch if we don't have them, but check if we're at a game end
      const res = await fetch(`/board?fen=${node.fen}`);
      const json = await res.json();
      node.legalMove = json["legalMove"];
      if (json["isMated"] && !node.move.endsWith('#')) {
        node.move += '#';
      } else if (json["isKingAttacked"] && !node.move.endsWith('+')) {
        node.move += '+';
      }
    }
  } else {
    const san = getPathToNodeSAN(node);
    const res = await fetch(`/board?san=${san}`)
    const json = await res.json();
    node.fen = json["fen"];
    node.legalMove = json["legalMove"];

    // Populate UCI info for the entire path if available
    if (json["uciMoves"]) {
      let curr = node;
      for (let i = json["uciMoves"].length - 1; i >= 0; i--) {
        if (!curr || curr.ply === 0) {
          break;
        }
        curr.uci = json["uciMoves"][i];
        curr = curr.parent;
      }

      let pathNodes = [];
      let temp = node;
      while (temp && temp.ply > 0) {
        pathNodes.unshift(temp);
        temp = temp.parent;
      }
      pathNodes.forEach(n => {
        const parentPath = n.parent ? n.parent.playPath : "";
        n.playPath = parentPath ? (parentPath + "," + n.uci) : n.uci;
      });
    }

    if (json["isMated"] && !node.move.endsWith('#')) {
      node.move += '#';
    } else if (json["isKingAttacked"] && !node.move.endsWith('+')) {
      node.move += '+';
    }
  }

  renderBoardFromFen(node.fen);

  // Visual symbol feedback
  try {
    if (node.symbol) {
      let targetSquare = null;
      if (node.uci && node.uci.length >= 4) {
        targetSquare = node.uci.substring(2, 4);
      } else if (node.playPath) {
        const moves = node.playPath.split(',');
        const lastMove = moves[moves.length - 1];
        if (lastMove && lastMove.length >= 4) {
          targetSquare = lastMove.substring(2, 4);
        }
      }

      if (targetSquare) {
        drawSymbol(targetSquare, node.symbol);
      }
    }
  } catch (err) {
    console.error("Error drawing symbol:", err);
  }

  setPossibleMoves(node.legalMove || []);
  updateSquareEvent();
}

let pendingMove = null;

export function selectPromotion(pieceType) {
  document.getElementById("promotion-overlay").style.display = "none";
  executeMove(pendingMove.from, pendingMove.to, pieceType);
}