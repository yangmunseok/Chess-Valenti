import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {createInsightAnalysis, toFen} from '../../main/resources/static/js/insightAnalysis.js';

const RESOURCES = new URL('../resources/insight/', import.meta.url);

// games.ndjson: Chess.com 공개 게임 300판 (Hikaru, 2024년 1월)
// expected-*: 같은 게임을 이전 서버(Java) 분석 구현에 넣어 만든 결과. 옮긴 로직이 같은지 확인한다.

// Windows 에서 체크아웃하면 줄 끝이 CRLF 가 될 수 있다
function readLines(name) {
  return readFileSync(new URL(name, RESOURCES), 'utf-8')
      .split(/\r?\n/)
      .filter(line => line.trim() !== '');
}

function loadFixture() {
  return readLines('games.ndjson').map(line => JSON.parse(line));
}

function analyzeFixture() {
  const analysis = createInsightAnalysis('hikaru');
  loadFixture().forEach(game => analysis.addGame(game));
  return analysis;
}

const START_PAWNS = {wLo: 0x0000ff00, wHi: 0, bLo: 0, bHi: 0x00ff0000};

function game(moves, overrides = {}) {
  return {
    winner: 'white',
    pgn: `[Event "?"]\n[White "me"]\n[Black "you"]\n\n${moves}`,
    whiteUsername: 'me',
    blackUsername: 'you',
    variant: 'standard',
    ...overrides,
  };
}

// 6번째 폰 이동(3...c5) 직후부터 조건을 만족하는 수순
const OPEN_CENTER = '1. e4 e5 2. d4 d5 3. c4 c5 4. Nf3 Nc6 1-0';

test('aggregated structures match the server implementation exactly', () => {
  const expected = readLines('expected-aggregated.txt');

  const actual = analyzeFixture().aggregated().map(([fen, r]) =>
      `${fen}|${r.white.whiteWon} ${r.white.blackWon} ${r.white.drawn} `
      + `${r.black.whiteWon} ${r.black.blackWon} ${r.black.drawn}`);

  assert.equal(actual.length, expected.length);
  assert.deepEqual(actual, expected);
});

test('filtered and top structures match the server implementation', () => {
  const expected = JSON.parse(
      readFileSync(new URL('expected-result.json', RESOURCES), 'utf-8'));

  const result = analyzeFixture().finish();

  assert.equal(result.structureCount, expected.structureCount);
  assert.equal(result.filteredCount, expected.filteredCount);
  assert.deepEqual(Object.keys(result.top), Object.keys(expected.top));
  assert.deepEqual(result.top, expected.top);
});

test('toFen writes pawns only, from rank 8 to rank 1', () => {
  assert.equal(toFen(START_PAWNS), '8/pppppppp/8/8/8/8/PPPPPPPP/8 w');
});

test('counts a structure once per game, from the player side', () => {
  const analysis = createInsightAnalysis('ME');

  // 4. Nf3 Nc6 은 폰 구조가 그대로라 한 번만 센다
  assert.equal(analysis.addGame(game(OPEN_CENTER)), true);
  const [[fen, results]] = analysis.aggregated();

  assert.equal(fen, '8/pp3ppp/8/2ppp3/2PPP3/8/PP3PPP/8 w');
  assert.deepEqual(results.white, {whiteWon: 1, blackWon: 0, drawn: 0});
});

test('ignores comments, variations and NAGs in the move text', () => {
  const analysis = createInsightAnalysis('me');
  const annotated = '1. e4 {[%clk 0:03:00]} e5 $1 2. d4 (2. Nf3 Nc6) d5 ; note\n'
      + '3. c4 3... c5 4. Nf3 Nc6 1-0';

  analysis.addGame(game(annotated));

  assert.deepEqual(analysis.aggregated().map(([fen]) => fen),
      ['8/pp3ppp/8/2ppp3/2PPP3/8/PP3PPP/8 w']);
});

test('skips variants and games with unreadable moves', () => {
  const analysis = createInsightAnalysis('me');

  assert.equal(analysis.addGame(game(OPEN_CENTER, {variant: 'chess960'})), false);
  assert.equal(analysis.addGame(game('1. e4 e5 2. Ke5 1-0')), false);
  assert.equal(analysis.addGame(game(OPEN_CENTER, {pgn: ''})), false);
  assert.equal(analysis.structureCount, 0);
  assert.equal(analysis.gameCount, 0);
});

test('draws are counted when there is no winner', () => {
  const analysis = createInsightAnalysis('you');

  analysis.addGame(game(OPEN_CENTER, {winner: undefined}));

  const [[, results]] = analysis.aggregated();
  assert.deepEqual(results.black, {whiteWon: 0, blackWon: 0, drawn: 1});
});
