import {test} from 'node:test';
import assert from 'node:assert/strict';
import {ChessNode, gameTree} from '../../main/resources/static/js/state.js';
import {importPGN} from '../../main/resources/static/js/pgnParser.js';
import {
  analyzePosition,
  ensurePosition,
  isPromotion,
  playMove,
  withoutCheckMark
} from '../../main/resources/static/js/position.js';

const START = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

test('analyzePosition lists legal moves in UCI', () => {
  const position = analyzePosition(START);

  assert.equal(position.legalMoves.length, 20);
  assert.ok(position.legalMoves.includes('e2e4'));
  assert.ok(position.legalMoves.includes('g1f3'));
  assert.equal(position.inCheck, false);
  assert.equal(position.checkmate, false);
});

test('analyzePosition detects checkmate', () => {
  // 1. f3 e5 2. g4 Qh4#
  const position = analyzePosition('rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3');

  assert.deepEqual(position.legalMoves, []);
  assert.equal(position.inCheck, true);
  assert.equal(position.checkmate, true);
});

test('playMove writes SAN with disambiguation, castling, check and promotion', () => {
  // 두 나이트가 d2 로 갈 수 있으면 출발 칸 파일을 붙인다
  assert.equal(playMove('4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1', 'b1', 'd2').san, 'Nbd2');
  assert.equal(playMove('r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1', 'e1', 'g1').san, 'O-O');
  assert.equal(playMove('4k3/8/8/8/8/8/8/R3K3 w - - 0 1', 'a1', 'a8').san, 'Ra8+');

  const promotion = playMove('8/4P1k1/8/8/8/8/8/4K3 w - - 0 1', 'e7', 'e8', 'Q');
  assert.equal(promotion.san, 'e8=Q');
  assert.equal(promotion.uci, 'e7e8q');
});

test('playMove rejects an illegal move', () => {
  assert.throws(() => playMove(START, 'e2', 'e5'));
});

test('isPromotion is true only for pawn moves to the last rank', () => {
  const legalMoves = analyzePosition('8/4P1k1/8/8/8/8/8/4K3 w - - 0 1').legalMoves;

  assert.equal(isPromotion(legalMoves, 'e7', 'e8'), true);
  assert.equal(isPromotion(legalMoves, 'e1', 'd1'), false);
});

test('withoutCheckMark compares moves read from PGN and played on the board', () => {
  assert.equal(withoutCheckMark('Qh4#'), 'Qh4');
  assert.equal(withoutCheckMark('Bb5+'), 'Bb5');
  assert.equal(withoutCheckMark('O-O'), 'O-O');
});

test('ensurePosition fills fen, uci and playPath of PGN nodes from the root', () => {
  importPGN('1. e4 e5 2. Nf3 (2. Bc4 Nf6) 2... Nc6');
  const [nf3, bc4] = gameTree.children[0].children[0].children;
  const nf6 = bc4.children[0];

  ensurePosition(nf6);

  assert.equal(nf6.fen, 'rnbqkb1r/pppp1ppp/5n2/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR w KQkq - 2 3');
  assert.equal(nf6.uci, 'g8f6');
  assert.equal(nf6.playPath, 'e2e4,e7e5,f1c4,g8f6');
  // 거쳐 간 조상도 채워진다
  assert.equal(bc4.uci, 'f1c4');
  assert.equal(nf3.fen, '');
});

test('ensurePosition throws when a PGN move is illegal', () => {
  const root = new ChessNode('Root', 0);
  root.fen = START;
  const illegal = new ChessNode('Ke5', 1, root);

  assert.throws(() => ensurePosition(illegal));
});
