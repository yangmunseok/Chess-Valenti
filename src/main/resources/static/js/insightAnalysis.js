// 사용자 게임에서 폰 구조별 승패 통계를 만든다. 브라우저(Web Worker)와 Node 테스트에서 같이 쓴다.
//
// 흐름: addGame() 으로 게임을 한 판씩 넣는다 → finish() 가 비슷한 구조를 합치고 화면에 보여 줄 구조만 고른다.
//
// 폰 위치는 비트보드로 다룬다. 64칸을 a1=0, b1=1, ..., h8=63 비트로 두는데, JS 비트 연산은 32비트라
// 아래쪽 32칸(1~4랭크)은 lo, 위쪽 32칸(5~8랭크)은 hi 에 나눠 담는다. 한 랭크(8칸)는 8비트이므로
// 파일 마스크는 lo, hi 모두 같은 값을 쓴다.

import {Chess} from './vendor/chess.js';

const FILE_A = 0x01010101;
const WING_EXCLUDED = 0x7e7e7e7e; // b~g 파일
const CENTER_FILES = 0x3c3c3c3c; // c~f 파일

const PIECE_VALUES = {p: 1, n: 3, b: 3, r: 5, q: 9, k: 0};

// 이 판 수보다 많이 둔 구조만 승률·패배율 목록에 넣는다 (판 수가 적으면 승률이 쉽게 0%, 100%가 됨)
const MIN_GAMES_FOR_RATE = 10;
const TOP_FREQUENT = 5;
const TOP_WIN_RATE = 5;
const TOP_LOSS_RATE = 6;

// chess.js 내부 표현(0x88 보드)과 이동 플래그. vendor/chess.js 버전을 올리면 다시 확인해야 한다.
const KSIDE_CASTLE = 32;
const QSIDE_CASTLE = 64;
const SAN_PATTERN = /^([NBRQK])?([a-h])?([1-8])?x?([a-h])([1-8])=?([NBRQ])?[+#]?[?!]*$/;

const BRACE_COMMENT_PATTERN = /\{[^}]*}/g;
const SEMICOLON_COMMENT_PATTERN = /;.*$/gm;
const VARIATION_PATTERN = /\([^()]*\)/g;
const NAG_PATTERN = /\$\d+/g;
const RESULT_PATTERN = /\b(1-0|0-1|1\/2-1\/2|\*)\b/g;
const MOVE_NUMBER_PATTERN = /\b\d+\.(\.\.)?/g;

export function createInsightAnalysis(username) {
  const user = (username ?? '').toLowerCase();
  // 구조 키 → { pawns, results }. Map 은 처음 넣은 순서를 지킨다.
  const structures = new Map();
  let gameCount = 0;

  return {
    /** 게임 한 판을 반영한다. 분석할 수 없는 게임(변형 체스, 잘못된 수순)이면 false. */
    addGame(game) {
      const found = analyzeGame(game);
      if (found === null) {
        return false;
      }
      const playerColor = game.whiteUsername != null && game.whiteUsername.toLowerCase() === user
          ? 'white' : 'black';
      for (const pawns of found) {
        const key = pawnKey(pawns);
        let entry = structures.get(key);
        if (!entry) {
          entry = {pawns, results: newGameResults()};
          structures.set(key, entry);
        }
        addResult(entry.results, playerColor, game.winner);
      }
      gameCount++;
      return true;
    },

    get gameCount() {
      return gameCount;
    },

    get structureCount() {
      return structures.size;
    },

    /** 비슷한 구조를 합치고, 화면에 보여 줄 구조만 FEN 키로 돌려준다. */
    finish() {
      const structureCount = structures.size;
      const filtered = filterSimilar([...structures.values()]);
      const top = {};
      for (const entry of selectTop(filtered)) {
        top[toFen(entry.pawns)] = entry.results;
      }
      return {gameCount, structureCount, filteredCount: filtered.length, top};
    },

    /** 테스트용: 합치기 전의 구조별 결과 (FEN, 결과) 를 넣은 순서대로 */
    aggregated() {
      return [...structures.values()].map(entry => [toFen(entry.pawns), entry.results]);
    },
  };
}

/**
 * 게임에서 조건을 만족한 폰 구조 목록을 돌려준다 (한 게임 안에서 중복 없이).
 * 분석 대상이 아니거나 수순을 읽을 수 없으면 null.
 */
function analyzeGame(game) {
  if (game.variant !== 'standard' && game.variant !== 'chess') {
    return null;
  }
  const moveText = extractMoveText(game.pgn);
  if (!moveText) {
    return null;
  }

  const chess = new Chess();
  const found = [];
  const visited = new Set();
  let pawnMoveCount = 0;
  try {
    for (const san of moveText.split(' ')) {
      const move = playSan(chess, san);
      if (move.piece === 'p') {
        pawnMoveCount++;
      }
      if (pawnMoveCount < 6) {
        continue;
      }
      const position = readPosition(chess);
      if (!position.materialEven || countCenterPawns(position) < 4) {
        continue;
      }
      const key = pawnKey(position);
      if (visited.has(key)) {
        continue;
      }
      visited.add(key);
      found.push({wLo: position.wLo, wHi: position.wHi, bLo: position.bLo, bHi: position.bHi});
    }
  } catch (e) {
    // 수순 하나라도 읽지 못하면 게임 전체를 건너뛴다 (서버 구현과 같음)
    return null;
  }
  return found;
}

function extractMoveText(pgn) {
  if (pgn == null || pgn.trim() === '') {
    return null;
  }
  // 태그 다음 빈 줄부터 수순이 시작된다
  let moveStart = pgn.indexOf('\n\n');
  if (moveStart === -1) {
    moveStart = pgn.indexOf('\r\n\r\n');
  }
  if (moveStart === -1) {
    moveStart = pgn.indexOf('1. ');
  } else {
    moveStart += 2;
  }
  if (moveStart === -1) {
    return null;
  }
  return sanitizeMoveText(pgn.substring(moveStart));
}

function sanitizeMoveText(moveText) {
  let sanitized = moveText
      .replace(BRACE_COMMENT_PATTERN, ' ')
      .replace(SEMICOLON_COMMENT_PATTERN, ' ');
  let previous;
  do {
    previous = sanitized;
    sanitized = sanitized.replace(VARIATION_PATTERN, ' ');
  } while (previous !== sanitized);
  return sanitized
      .replace(NAG_PATTERN, ' ')
      .replace(RESULT_PATTERN, ' ')
      .replace(MOVE_NUMBER_PATTERN, ' ')
      .replace(/\s+/g, ' ')
      .trim();
}

/**
 * SAN 한 수를 둔다. 둘 수 없는 수면 예외를 던진다.
 *
 * chess.move(san) 은 후보 수마다 SAN(체크 표시 포함)을 다시 만들어 비교하고 앞뒤 FEN 까지 만들어서
 * 초당 2천 수 정도밖에 못 둔다. 여기서는 목적지·기물·구분 표기·승격만 비교해 후보를 고른다.
 * 하나로 좁혀지지 않으면 chess.move(san) 으로 처리한다.
 */
function playSan(chess, san) {
  const move = findMove(chess, san);
  if (move === null) {
    return chess.move(san);
  }
  chess._makeMove(move);
  return move;
}

function findMove(chess, san) {
  const plain = san.replace(/[+#]?[?!]*$/, '');
  let candidates;
  if (plain === 'O-O' || plain === '0-0' || plain === 'O-O-O' || plain === '0-0-0') {
    const flag = plain.length > 3 ? QSIDE_CASTLE : KSIDE_CASTLE;
    candidates = chess._moves({legal: true, piece: 'k'}).filter(move => move.flags & flag);
  } else {
    const match = SAN_PATTERN.exec(san);
    if (!match) {
      return null;
    }
    const [, piece = 'P', fromFile, fromRank, toFile, toRank, promotion] = match;
    // 0x88 보드: a8 = 0, h8 = 7, a1 = 112. 칸 번호 = (8 - 랭크) * 16 + 파일
    const to = (8 - Number(toRank)) * 16 + (toFile.charCodeAt(0) - 97);
    const fromFileIndex = fromFile ? fromFile.charCodeAt(0) - 97 : -1;
    const fromRankNumber = fromRank ? Number(fromRank) : -1;
    const promotionPiece = promotion?.toLowerCase();
    candidates = chess._moves({legal: true, piece: piece.toLowerCase()}).filter(move =>
        move.to === to
        && (fromFileIndex < 0 || (move.from & 0xf) === fromFileIndex)
        && (fromRankNumber < 0 || 8 - (move.from >> 4) === fromRankNumber)
        && move.promotion === promotionPiece);
  }
  return candidates.length === 1 ? candidates[0] : null;
}

// 현재 판의 폰 비트보드와 기물 점수가 같은지 읽는다
function readPosition(chess) {
  let wLo = 0, wHi = 0, bLo = 0, bHi = 0;
  let whiteMaterial = 0, blackMaterial = 0;
  const board = chess._board; // 0x88 보드: 칸 번호 & 0x88 이 0 이 아니면 판 밖
  for (let square = 0; square < 120; square++) {
    if (square & 0x88) {
      square += 7;
      continue;
    }
    const piece = board[square];
    if (piece) {
      const rank = 7 - (square >> 4);
      const file = square & 0xf;
      if (piece.color === 'w') {
        whiteMaterial += PIECE_VALUES[piece.type];
      } else {
        blackMaterial += PIECE_VALUES[piece.type];
      }
      if (piece.type === 'p') {
        const bit = 1 << ((rank % 4) * 8 + file);
        if (piece.color === 'w') {
          if (rank < 4) wLo |= bit; else wHi |= bit;
        } else {
          if (rank < 4) bLo |= bit; else bHi |= bit;
        }
      }
    }
  }
  return {wLo: wLo >>> 0, wHi: wHi >>> 0, bLo: bLo >>> 0, bHi: bHi >>> 0,
    materialEven: whiteMaterial === blackMaterial};
}

function pawnKey(p) {
  return `${p.wLo}.${p.wHi}.${p.bLo}.${p.bHi}`;
}

function popcount(x) {
  x = x - ((x >>> 1) & 0x55555555);
  x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
  x = (x + (x >>> 4)) & 0x0f0f0f0f;
  return Math.imul(x, 0x01010101) >>> 24;
}

function countCenterPawns(p) {
  return popcount((p.wLo | p.bLo) & CENTER_FILES) + popcount((p.wHi | p.bHi) & CENTER_FILES);
}

function countDoubled(lo, hi) {
  let count = 0;
  for (let file = 0; file < 8; file++) {
    const mask = FILE_A << file;
    if (popcount(lo & mask) + popcount(hi & mask) >= 2) {
      count++;
    }
  }
  return count;
}

function countIsolated(lo, hi) {
  let count = 0;
  for (let file = 0; file < 8; file++) {
    const mask = FILE_A << file;
    const onFile = popcount(lo & mask) + popcount(hi & mask);
    if (onFile === 0) {
      continue;
    }
    const neighbors = (file > 0 ? FILE_A << (file - 1) : 0) | (file < 7 ? FILE_A << (file + 1) : 0);
    if (((lo | hi) & neighbors) === 0) {
      count += onFile;
    }
  }
  return count;
}

// 비슷한 구조인지 비교하기 위한 요약. 양쪽 끝(a, h) 파일의 폰은 빼고 본다.
function summarize(p) {
  const wLo = p.wLo & WING_EXCLUDED, wHi = p.wHi & WING_EXCLUDED;
  const bLo = p.bLo & WING_EXCLUDED, bHi = p.bHi & WING_EXCLUDED;
  return {
    wLo, wHi, bLo, bHi,
    whiteDoubled: countDoubled(wLo, wHi),
    blackDoubled: countDoubled(bLo, bHi),
    whiteIsolated: countIsolated(wLo, wHi),
    blackIsolated: countIsolated(bLo, bHi),
    totalPawns: popcount(p.wLo) + popcount(p.wHi) + popcount(p.bLo) + popcount(p.bHi),
  };
}

function isSimilar(s1, s2) {
  if (s1.totalPawns !== s2.totalPawns) {
    return false;
  }
  const pawns1 = popcount(s1.wLo) + popcount(s1.wHi) + popcount(s1.bLo) + popcount(s1.bHi);
  const pawns2 = popcount(s2.wLo) + popcount(s2.wHi) + popcount(s2.bLo) + popcount(s2.bHi);
  if (pawns1 + pawns2 === 0) {
    return false;
  }
  const equalPawns = popcount((s1.wLo & s2.wLo) | (s1.bLo & s2.bLo))
      + popcount((s1.wHi & s2.wHi) | (s1.bHi & s2.bHi));
  const similarity = Math.trunc((200 * equalPawns) / (pawns1 + pawns2));

  if (s1.whiteDoubled !== s2.whiteDoubled) return false;
  if (s1.blackDoubled !== s2.blackDoubled) return false;
  if (s1.whiteIsolated !== s2.whiteIsolated) return false;
  if (s1.blackIsolated !== s2.blackIsolated) return false;
  return similarity > 80;
}

/**
 * 비슷한 구조끼리 묶고, 묶음마다 가장 많이 둔 구조 하나만 남긴다.
 * 구조는 처음 나온 순서대로 보며, 비슷한 구조가 있는 첫 번째 묶음에 들어간다.
 */
function filterSimilar(entries) {
  const summaries = entries.map(entry => summarize(entry.pawns));
  const groups = [];
  for (let i = 0; i < entries.length; i++) {
    const group = groups.find(members => members.some(idx => isSimilar(summaries[i], summaries[idx])));
    if (group) {
      group.push(i);
    } else {
      groups.push([i]);
    }
  }

  const keep = new Set();
  for (const group of groups) {
    let best = group[0];
    for (const idx of group) {
      if (total(entries[idx].results) > total(entries[best].results)) {
        best = idx;
      }
    }
    keep.add(best);
  }
  return entries.filter((entry, idx) => keep.has(idx));
}

// 화면이 보여 주는 세 목록(많이 둔 구조, 승률, 패배율)에 들어가는 구조만 고른다
function selectTop(entries) {
  const enoughGames = entry => total(entry.results) > MIN_GAMES_FOR_RATE;
  const picked = new Set();
  const pick = (candidates, score, limit) => {
    // Array.prototype.sort 는 안정 정렬이라 점수가 같으면 원래 순서를 지킨다
    [...candidates].sort((a, b) => score(b) - score(a))
        .slice(0, limit)
        .forEach(entry => picked.add(entry));
  };
  pick(entries, entry => total(entry.results), TOP_FREQUENT);
  pick(entries.filter(enoughGames), entry => winRate(entry.results), TOP_WIN_RATE);
  pick(entries.filter(enoughGames), entry => lossRate(entry.results), TOP_LOSS_RATE);
  return [...picked];
}

function newGameResults() {
  return {
    white: {whiteWon: 0, blackWon: 0, drawn: 0},
    black: {whiteWon: 0, blackWon: 0, drawn: 0},
  };
}

// results.white 는 사용자가 백일 때, results.black 은 흑일 때의 결과
function addResult(results, playerColor, winner) {
  const side = results[playerColor];
  if (winner === 'white') {
    side.whiteWon++;
  } else if (winner === 'black') {
    side.blackWon++;
  } else {
    side.drawn++;
  }
}

function total(r) {
  return r.white.whiteWon + r.white.blackWon + r.white.drawn
      + r.black.whiteWon + r.black.blackWon + r.black.drawn;
}

function winRate(r) {
  return (r.white.whiteWon + r.black.blackWon) / total(r);
}

function lossRate(r) {
  return (r.black.whiteWon + r.white.blackWon) / total(r);
}

/** 폰만 남긴 FEN. /pawn-games?fen= 링크와 저장 키로 쓴다. */
export function toFen(p) {
  let fen = '';
  for (let rank = 7; rank >= 0; rank--) {
    const white = rank < 4 ? p.wLo : p.wHi;
    const black = rank < 4 ? p.bLo : p.bHi;
    let empty = 0;
    for (let file = 0; file < 8; file++) {
      const bit = 1 << ((rank % 4) * 8 + file);
      const piece = (white & bit) !== 0 ? 'P' : (black & bit) !== 0 ? 'p' : null;
      if (piece === null) {
        empty++;
        continue;
      }
      if (empty > 0) {
        fen += empty;
        empty = 0;
      }
      fen += piece;
    }
    if (empty > 0) {
      fen += empty;
    }
    if (rank > 0) {
      fen += '/';
    }
  }
  return fen + ' w';
}
