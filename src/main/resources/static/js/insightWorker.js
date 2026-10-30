// 분석을 화면 스레드와 분리해 돌리는 Web Worker. 게임 수천 판을 처리하는 동안 화면이 멈추지 않는다.
//
// 사용: new Worker('/js/insightWorker.js', {type: 'module'})
//   보내는 메시지
//     {type: 'start', username}        새 분석 시작
//     {type: 'games', games: [...]}    게임 묶음 (game = {winner, pgn, whiteUsername, blackUsername, variant})
//     {type: 'finish'}                 다 보냈음
//   받는 메시지
//     {type: 'progress', loadedGame, structureCount}
//     {type: 'result', gameCount, structureCount, filteredCount, top}
//     {type: 'error', message}

import {createInsightAnalysis} from './insightAnalysis.js';

let analysis = null;

self.onmessage = ({data}) => {
  try {
    switch (data.type) {
      case 'start':
        analysis = createInsightAnalysis(data.username);
        break;
      case 'games':
        data.games.forEach(game => analysis.addGame(game));
        self.postMessage({
          type: 'progress',
          loadedGame: analysis.gameCount,
          structureCount: analysis.structureCount,
        });
        break;
      case 'finish':
        self.postMessage({type: 'result', ...analysis.finish()});
        analysis = null;
        break;
      default:
        throw new Error(`Unknown message type: ${data.type}`);
    }
  } catch (e) {
    self.postMessage({type: 'error', message: e.message});
  }
};
