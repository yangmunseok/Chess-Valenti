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

// node 아래 트리 전체를 PGN 으로 만든다. 첫 번째 자식이 메인 라인이고, 나머지 자식은 변화수로
// 메인 수 바로 뒤에 괄호로 넣는다. 변화수나 주석 뒤에 이어지는 흑의 수에는 "2..." 처럼 번호를 다시 붙인다.
export function generatePgn(node) {
  return writeLine(node, false);
}

function writeLine(node, numberFirstMove) {
  const parts = [];
  let current = node;
  let numberNext = numberFirstMove;
  while (current.children.length > 0) {
    const [main, ...variations] = current.children;
    parts.push(moveText(main, numberNext));
    variations.forEach(variation => {
      const rest = writeLine(variation, Boolean(variation.comment));
      parts.push(`(${moveText(variation, true)}${rest ? ' ' + rest : ''})`);
    });
    numberNext = variations.length > 0 || Boolean(main.comment);
    current = main;
  }
  return parts.join(' ');
}

function moveText(node, withBlackNumber) {
  const moveNumber = Math.ceil(node.ply / 2);
  const isWhite = node.ply % 2 !== 0;
  const prefix = isWhite ? `${moveNumber}. ` : (withBlackNumber ? `${moveNumber}... ` : '');
  const comment = node.comment ? ` { ${node.comment} }` : '';
  return `${prefix}${node.move}${node.symbol}${comment}`;
}
