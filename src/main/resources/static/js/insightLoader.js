// 브라우저에서 Lichess, Chess.com 공개 API 로 사용자 게임을 받아 온다.
// 요청이 사용자 IP 에서 나가므로 API 한도도 사용자마다 따로 적용된다.
//
// 두 함수 모두 받은 게임을 묶음으로 onGames(games) 에 넘긴다.
//   game = {winner: 'white' | 'black' | undefined, pgn, whiteUsername, blackUsername, variant}
// signal 로 중단하면 예외 없이 그때까지 받은 데서 끝난다.

const LICHESS_API = 'https://lichess.org/api';
const CHESS_COM_API = 'https://api.chess.com/pub';

// 너무 잦은 메시지를 피하려고 이만큼 모이거나 이 시간이 지나면 한 번에 넘긴다
const BATCH_SIZE = 50;
const BATCH_INTERVAL_MILLIS = 300;

export class InsightLoadError extends Error {
  /** @param code 'not_found' | 'rate_limited' | 'failed' */
  constructor(code, message) {
    super(message);
    this.code = code;
  }
}

/**
 * @param perfType 'blitz,rapid' 처럼 쉼표로 구분한 게임 유형
 * @param since    이 시각(epoch 밀리초) 이후의 게임만
 */
export async function loadLichessGames({username, perfType, since, signal, onGames}) {
  const params = new URLSearchParams({pgnInJson: 'true', perfType, since: String(since)});
  const url = `${LICHESS_API}/games/user/${encodeURIComponent(username)}?${params}`;
  const batch = createBatch(onGames);

  await stopOnAbort(signal, async () => {
    const response = await fetchOrFail(url, {headers: {Accept: 'application/x-ndjson'}, signal});
    checkStatus(response, 'Lichess', username);
    // ndjson: 한 줄이 게임 한 판. 한 판씩 읽어 넘기므로 전체를 메모리에 들고 있지 않는다.
    await readLines(response.body, line => batch.add(toLichessGame(JSON.parse(line))));
  });
  batch.flush();
}

/**
 * Chess.com 은 월별 아카이브라서 since 가 속한 달부터 한 달씩 차례로 받는다.
 */
export async function loadChessComGames({username, perfType, since, signal, onGames}) {
  const user = username.toLowerCase();
  const timeClasses = new Set(perfType.split(',').map(value => value.trim().toLowerCase()));
  const sinceDate = new Date(since);
  const sinceMonth = sinceDate.getUTCFullYear() * 12 + sinceDate.getUTCMonth();
  const batch = createBatch(onGames);

  await stopOnAbort(signal, async () => {
    const archivesResponse = await fetchOrFail(
        `${CHESS_COM_API}/player/${encodeURIComponent(user)}/games/archives`, {signal});
    checkStatus(archivesResponse, 'Chess.com', username);
    const {archives = []} = await archivesResponse.json();

    for (const archiveUrl of archives) {
      const match = /\/games\/(\d{4})\/(\d{2})$/.exec(archiveUrl);
      if (!match || Number(match[1]) * 12 + Number(match[2]) - 1 < sinceMonth) {
        continue;
      }
      const response = await fetchOrFail(archiveUrl, {signal});
      checkStatus(response, 'Chess.com', username);
      const {games = []} = await response.json();
      games
          .filter(game => game.rules === 'chess'
              && timeClasses.has(game.time_class)
              && game.end_time * 1000 >= since)
          .forEach(game => batch.add(toChessComGame(game)));
    }
  });
  batch.flush();
}

function toLichessGame(response) {
  return {
    winner: response.winner,
    pgn: response.pgn,
    whiteUsername: response.players?.white?.user?.name,
    blackUsername: response.players?.black?.user?.name,
    variant: response.variant,
  };
}

function toChessComGame(game) {
  const winner = game.white?.result === 'win' ? 'white'
      : game.black?.result === 'win' ? 'black' : undefined;
  return {
    winner,
    pgn: game.pgn,
    whiteUsername: game.white?.username,
    blackUsername: game.black?.username,
    variant: 'chess',
  };
}

function createBatch(onGames) {
  let games = [];
  let lastFlushedAt = Date.now();
  const flush = () => {
    if (games.length > 0) {
      onGames(games);
      games = [];
    }
    lastFlushedAt = Date.now();
  };
  return {
    add(game) {
      games.push(game);
      if (games.length >= BATCH_SIZE || Date.now() - lastFlushedAt >= BATCH_INTERVAL_MILLIS) {
        flush();
      }
    },
    flush,
  };
}

// 사용자가 중단하면(AbortError) 실패가 아니라 그때까지 받은 게임으로 끝낸다
async function stopOnAbort(signal, action) {
  try {
    await action();
  } catch (e) {
    if (signal?.aborted) {
      return;
    }
    throw e;
  }
}

async function fetchOrFail(url, options) {
  try {
    return await fetch(url, options);
  } catch (e) {
    if (options.signal?.aborted) {
      throw e;
    }
    throw new InsightLoadError('failed', '게임을 불러오지 못했습니다. 네트워크 상태를 확인해주세요.');
  }
}

function checkStatus(response, platform, username) {
  if (response.ok) {
    return;
  }
  if (response.status === 404) {
    throw new InsightLoadError('not_found', `존재하지 않는 사용자 아이디입니다: ${username}`);
  }
  if (response.status === 429) {
    // Lichess 규칙: 429 를 받으면 1분 동안 요청하지 말 것
    throw new InsightLoadError('rate_limited',
        `${platform} 요청 한도를 초과했습니다. 1분 뒤에 다시 시도해주세요.`);
  }
  throw new InsightLoadError('failed', `${platform}에서 게임을 불러오지 못했습니다. (${response.status})`);
}

// 스트림을 줄 단위로 읽는다. 조각(chunk) 경계에서 잘린 줄은 다음 조각과 이어 붙인다.
async function readLines(body, onLine) {
  const reader = body.pipeThrough(new TextDecoderStream()).getReader();
  let buffer = '';
  for (;;) {
    const {value, done} = await reader.read();
    if (done) {
      break;
    }
    buffer += value;
    let newline;
    while ((newline = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, newline);
      buffer = buffer.slice(newline + 1);
      if (line.trim() !== '') {
        onLine(line);
      }
    }
  }
  if (buffer.trim() !== '') {
    onLine(buffer);
  }
}
