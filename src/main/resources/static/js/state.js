export let possibleMoves = [];
export let gameResultState = "";
export const nodeMap = new Map();

export function setGameResultState(result) {
  gameResultState = result;
}

export function setPossibleMoves(moves) {
  possibleMoves = moves;
}

export function getPossibleMoves() {
  return possibleMoves;
}

export let nodeIdCounter = 0;

export class ChessNode {
  constructor(move, ply, parent = null) {
    this.id = `node-${nodeIdCounter++}`;
    this.move = move; // SAN format
    this.uci = ""; // UCI format (e.g., e2e4)
    this.playPath = ""; // Cumulative UCI path (e.g., d2d4,d7d5)
    this.ply = ply;
    this.parent = parent;
    this.children = [];
    this.comment = "";
    this.fen = "";
    this.legalMove = [];
    this.symbol = "";
    this.eval = "";
  }
}

export let gameTree = new ChessNode("Root", 0);
gameTree.fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
export let currentNode = gameTree;

export function setCurrentNode(node) {
  currentNode = node;
}

export function findNodeById(id) {
  return nodeMap.get(id);
}

export function initGameTree(root) {
  gameTree = root;
}