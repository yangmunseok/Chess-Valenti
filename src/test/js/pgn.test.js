import {test} from 'node:test';
import assert from 'node:assert/strict';
import {gameTree} from '../../main/resources/static/js/state.js';
import {importPGN} from '../../main/resources/static/js/pgnParser.js';
import {generatePgn, getPathToNodeSAN} from '../../main/resources/static/js/pgn.js';

const PGN = '[Event "?"]\n[Result "1-0"]\n\n'
    + '1. e4 e5 2. Nf3 { main } (2. Bc4 Nf6 (2... Bc5)) 2... Nc6 3. Bb5!? 1-0';

test('importPGN builds the main line and variations and returns the result', () => {
  const result = importPGN(PGN, ['e2e4', 'e7e5', 'g1f3']);

  assert.equal(result, '1-0');
  const [e4] = gameTree.children;
  const [e5] = e4.children;
  assert.deepEqual(e5.children.map(n => n.move), ['Nf3', 'Bc4']);

  const [nf3, bc4] = e5.children;
  assert.equal(nf3.comment, 'main');
  assert.equal(nf3.playPath, 'e2e4,e7e5,g1f3');
  assert.deepEqual(bc4.children.map(n => n.move), ['Nf6', 'Bc5']);

  const bb5 = nf3.children[0].children[0];
  assert.equal(bb5.move, 'Bb5');
  assert.equal(bb5.symbol, '!?');
});

test('generatePgn writes the tree back with comments, variations and symbols', () => {
  importPGN(PGN);

  assert.equal(generatePgn(gameTree),
      '1. e4 e5 2. Nf3 { main } (2. Bc4 Nf6 (2... Bc5)) 2... Nc6 3. Bb5!?');
});

test('generated PGN reads back into the same tree', () => {
  importPGN(PGN);
  const pgn = generatePgn(gameTree);

  importPGN(pgn);

  assert.equal(generatePgn(gameTree), pgn);
  const [nf3, bc4] = gameTree.children[0].children[0].children;
  assert.equal(nf3.children[0].children[0].move, 'Bb5');
  assert.deepEqual(bc4.children.map(n => n.move), ['Nf6', 'Bc5']);
});

test('generatePgn of an empty tree is empty', () => {
  importPGN('');

  assert.equal(generatePgn(gameTree), '');
});

test('getPathToNodeSAN numbers the moves from the root to the node', () => {
  importPGN(PGN);
  const bb5 = gameTree.children[0].children[0].children[0].children[0].children[0];

  assert.equal(getPathToNodeSAN(bb5), '1. e4 e5 2. Nf3 Nc6 3. Bb5');
});
