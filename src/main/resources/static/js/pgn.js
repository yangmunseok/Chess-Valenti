// 게임 트리(ChessNode)를 PGN 문자열로 바꾼다. DOM 을 쓰지 않으므로 Node 에서 테스트할 수 있다.

// 루트부터 targetNode 까지의 수를 "1. e4 e5 2. Nf3" 형식으로 만든다
export function getPathToNodeSAN(targetNode) {
  let path = [];
  let curr = targetNode;
  while (curr && curr.ply > 0) {
    path.unshift(curr);
    curr = curr.parent;
  }
  let sanParts = [];
  path.forEach((node) => {
    if (node.ply % 2 !== 0) {
      sanParts.push(
          `${Math.ceil(node.ply / 2)}. ${node.move}`);
    } else {
      sanParts.push(node.move);
    }
  });
  return sanParts.join(' ');
}

// node 아래 트리 전체를 PGN 으로 만든다. 첫 번째 자식이 메인 라인이고 나머지는 괄호 안 변화수다.
export function generatePgn(node) {
  if (node.ply === 0 && node.children.length === 0) {
    return "";
  }
  let pgn = "";
  node.children.forEach((child, index) => {
    const moveNum = child.ply % 2 !== 0 ? Math.ceil(child.ply / 2) + ". " : "";
    const commentPart = child.comment ? `{ ${child.comment} } ` : "";

    if (index === 0) {
      pgn += `${moveNum}${child.move}${child.symbol} ${commentPart}`;
      pgn += generatePgn(child);
    } else {
      const varMoveNum = Math.ceil(child.ply / 2) + (child.ply % 2 === 0
          ? "... " : ". ");
      pgn += `(${varMoveNum}${child.move}${child.symbol} ${commentPart}${generatePgn(
          child)}) `;
    }
  });
  return pgn.trim();
}
