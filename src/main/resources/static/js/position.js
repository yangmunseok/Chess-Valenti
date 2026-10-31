// 체스 규칙 계산: 국면(FEN), 합법 수, 체크/메이트, 수 기록(SAN). 서버(/board)에 묻지 않고 chess.js 로 브라우저에서 계산한다.
// DOM 을 쓰지 않으므로 Node 에서 테스트할 수 있다.
import {Chess} from "./vendor/chess.js";

const toUci = move => move.from + move.to + (move.promotion ?? "");

// fen 국면의 합법 수(UCI 배열)와 체크/메이트 여부
export function analyzePosition(fen) {
  const chess = new Chess(fen);
  return {
    legalMoves: chess.moves({verbose: true}).map(toUci),
    inCheck: chess.inCheck(),
    checkmate: chess.isCheckmate(),
  };
}

// fen 국면에서 from→to 로 둔 결과. 규칙에 어긋나면 예외를 던진다.
export function playMove(fen, from, to, promotion = null) {
  const chess = new Chess(fen);
  const move = chess.move({from, to, ...(promotion ? {promotion: promotion.toLowerCase()} : {})});
  return {san: move.san, uci: toUci(move), fen: chess.fen()};
}

// from→to 가 폰 승진이라 기물을 골라야 하는지
export function isPromotion(legalMoves, from, to) {
  return legalMoves.some(move => move.length === 5 && move.startsWith(from + to));
}

// 체크(+)와 메이트(#) 표시를 뺀 수. PGN 에서 읽은 수와 보드에서 둔 수를 비교할 때 쓴다.
export function withoutCheckMark(san) {
  return san.replace(/[+#]+$/, "");
}

/**
 * node 의 국면(fen)과 수(uci, playPath)를 채운다. 비어 있으면 부모 국면에서 node.move 를 두어 계산하며,
 * 부모도 비어 있으면 루트까지 거슬러 올라간다. PGN 에서 읽은 노드는 수(SAN)만 있으므로 처음 볼 때 계산된다.
 * 수가 규칙에 어긋나면 예외를 던진다.
 */
export function ensurePosition(node) {
  if (node.fen) {
    return node;
  }
  const parent = ensurePosition(node.parent);
  const chess = new Chess(parent.fen);
  const move = chess.move(node.move);
  node.fen = chess.fen();
  node.uci = toUci(move);
  node.playPath = parent.playPath ? `${parent.playPath},${node.uci}` : node.uci;
  return node;
}
