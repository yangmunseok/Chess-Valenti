import {afterEach, test} from 'node:test';
import assert from 'node:assert/strict';
import {
  InsightLoadError,
  loadChessComGames,
  loadLichessGames,
} from '../../main/resources/static/js/insightLoader.js';

const realFetch = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = realFetch;
});

// url 에 맞는 응답을 돌려주는 가짜 fetch. 요청한 url 은 calls 에 남는다.
function stubFetch(routes) {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push(url);
    if (options.signal?.aborted) {
      throw new DOMException('aborted', 'AbortError');
    }
    const route = Object.entries(routes).find(([prefix]) => url.startsWith(prefix));
    if (!route) {
      return new Response('', {status: 404});
    }
    return typeof route[1] === 'function' ? route[1](options) : route[1].clone();
  };
  return calls;
}

// 문자열 조각들을 차례로 흘려보내는 응답 본문
function streamOf(chunks) {
  const encoder = new TextEncoder();
  return new ReadableStream({
    start(controller) {
      chunks.forEach(chunk => controller.enqueue(encoder.encode(chunk)));
      controller.close();
    },
  });
}

function lichessLine(winner, white, black) {
  return JSON.stringify({
    winner, pgn: '1. e4 e5', variant: 'standard',
    players: {white: {user: {name: white}}, black: {user: {name: black}}},
  }) + '\n';
}

async function collect(load, options) {
  const games = [];
  await load({perfType: 'blitz,rapid', since: 0, onGames: batch => games.push(...batch), ...options});
  return games;
}

test('lichess: reads ndjson games even when lines are split across chunks', async () => {
  const lines = lichessLine('white', 'me', 'you') + lichessLine(undefined, 'you', 'me');
  const calls = stubFetch({
    'https://lichess.org/api/games/user/': new Response(
        streamOf([lines.slice(0, 30), lines.slice(30, 100), lines.slice(100)])),
  });

  const games = await collect(loadLichessGames, {username: 'me', since: 1700000000000});

  assert.deepEqual(games, [
    {winner: 'white', pgn: '1. e4 e5', whiteUsername: 'me', blackUsername: 'you', variant: 'standard'},
    {winner: undefined, pgn: '1. e4 e5', whiteUsername: 'you', blackUsername: 'me', variant: 'standard'},
  ]);
  assert.equal(calls[0], 'https://lichess.org/api/games/user/me'
      + '?pgnInJson=true&perfType=blitz%2Crapid&since=1700000000000');
});

test('lichess: unknown user and rate limit become readable errors', async () => {
  stubFetch({'https://lichess.org/api/games/user/nobody': new Response('', {status: 404})});
  await assert.rejects(collect(loadLichessGames, {username: 'nobody'}),
      e => e instanceof InsightLoadError && e.code === 'not_found');

  stubFetch({'https://lichess.org/api/games/user/busy': new Response('', {status: 429})});
  await assert.rejects(collect(loadLichessGames, {username: 'busy'}),
      e => e instanceof InsightLoadError && e.code === 'rate_limited' && e.message.includes('1분'));
});

test('lichess: stopping keeps the games received so far without an error', async () => {
  const controller = new AbortController();
  stubFetch({
    'https://lichess.org/api/games/user/': () => new Response(new ReadableStream({
      start(stream) {
        stream.enqueue(new TextEncoder().encode(lichessLine('white', 'me', 'you')));
        // 두 번째 게임은 오지 않고, 사용자가 중단한다
        controller.signal.addEventListener('abort', () => stream.error(
            new DOMException('aborted', 'AbortError')));
        setTimeout(() => controller.abort(), 50);
      },
    })),
  });

  const games = await collect(loadLichessGames, {username: 'me', signal: controller.signal});

  assert.equal(games.length, 1);
});

test('chess.com: loads months from the since month and keeps requested games only', async () => {
  const since = Date.UTC(2024, 1, 15); // 2024-02-15
  const game = (timeClass, endTime, rules = 'chess') => ({
    pgn: `[Event "${timeClass}"]`, time_class: timeClass, rules, end_time: endTime / 1000,
    white: {username: 'Me', result: 'win'}, black: {username: 'you', result: 'resigned'},
  });
  const calls = stubFetch({
    'https://api.chess.com/pub/player/me/games/archives': Response.json({
      archives: [
        'https://api.chess.com/pub/player/me/games/2024/01',
        'https://api.chess.com/pub/player/me/games/2024/02',
        'https://api.chess.com/pub/player/me/games/2024/03',
      ],
    }),
    'https://api.chess.com/pub/player/me/games/2024/02': Response.json({
      games: [
        game('blitz', Date.UTC(2024, 1, 10)), // since 이전
        game('blitz', Date.UTC(2024, 1, 20)),
        game('bullet', Date.UTC(2024, 1, 21)), // 요청하지 않은 유형
      ],
    }),
    'https://api.chess.com/pub/player/me/games/2024/03': Response.json({
      games: [game('rapid', Date.UTC(2024, 2, 1)), game('blitz', Date.UTC(2024, 2, 2), 'chess960')],
    }),
  });

  const games = await collect(loadChessComGames, {username: 'Me', since});

  assert.deepEqual(games.map(g => g.pgn), ['[Event "blitz"]', '[Event "rapid"]']);
  assert.deepEqual(games[0], {
    winner: 'white', pgn: '[Event "blitz"]', whiteUsername: 'Me', blackUsername: 'you',
    variant: 'chess',
  });
  // 2024년 1월 아카이브는 since 이전이라 받지 않는다
  assert.ok(!calls.some(url => url.endsWith('/2024/01')));
});

test('chess.com: unknown user becomes a readable error', async () => {
  stubFetch({});

  await assert.rejects(collect(loadChessComGames, {username: 'nobody'}),
      e => e instanceof InsightLoadError && e.code === 'not_found');
});

test('network failure becomes a readable error', async () => {
  globalThis.fetch = async () => {
    throw new TypeError('Failed to fetch');
  };

  await assert.rejects(collect(loadLichessGames, {username: 'me'}),
      e => e instanceof InsightLoadError && e.code === 'failed');
});
