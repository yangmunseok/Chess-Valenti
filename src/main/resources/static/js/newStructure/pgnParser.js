import {ChessNode, gameTree, nodeMap} from "./state.js";
import {refreshMoveHistory} from "./history.js";

/**
 * PGN 문자열을 입력받아 전체 게임 트리를 다시 구축합니다.
 * @param {string} pgnString
 */
export function importPGN(pgnString) {
  let stack = [];
  let current = gameTree;
  let gameResult = ""; // 게임 결과 저장용 변수

  // 결과 패턴 (1-0, 0-1, 1/2-1/2, *)
  const resultRegex = /^(1-0|0-1|1\/2-1\/2|\*)$/;

  const cleanedPgn = pgnString
  .replace(/\[.*?\]/g, (match) => {
    // [Result "1-0"] 같은 태그에서 결과 미리 추출 시도 (옵션)
    if (match.includes("Result")) {
      const resMatch = match.match(/"(.*?)"/);
      if (resMatch) {
        gameResult = resMatch[1];
      }
    }
    return '';
  })
  .replace(/\{([\s\S]*?)\}/g, ' { $1 } ')
  .replace(/\(/g, ' ( ')
  .replace(/\)/g, ' ) ');

  const tokens = cleanedPgn.split(/\s+/).filter(t => t.trim() !== "");

  for (let i = 0; i < tokens.length; i++) {
    const token = tokens[i];

    if (token === "(") {
      stack.push(current);
      current = current.parent;
    } else if (token === ")") {
      current = stack.pop();
    } else if (token === "{") {
      let comment = "";
      i++;
      while (i < tokens.length && tokens[i] !== "}") {
        comment += tokens[i] + " ";
        i++;
      }
      current.comment = comment.trim();
    } else if (resultRegex.test(token)) {
      // 토큰 흐름 중에 결과를 발견하면 저장
      gameResult = token;
    } else if (/^\d+\./.test(token)) {
      continue;
    } else {
      const moveMatch = token.match(/^([a-zA-Z0-9+#=/-]+)([\!\?]{1,2})?$/);
      if (moveMatch) {
        const newNode = new ChessNode(moveMatch[1], current.ply + 1, current);
        nodeMap.set(newNode.id, newNode);
        newNode.symbol = moveMatch[2] || "";
        current.children.push(newNode);
        current = newNode;
      }
    }
  }

  // 전역 상태나 UI에 결과값 전달
  refreshMoveHistory(gameResult);
}