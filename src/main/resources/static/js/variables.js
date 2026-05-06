let possibleMoves = [];
export const nodeMap = new Map();

export function setPossibleMoves(moves) {
  possibleMoves = moves;

}

export function getPossibleMoves() {
  return possibleMoves;
}