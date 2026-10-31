import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readdirSync, readFileSync} from 'node:fs';

// static/js 모듈끼리의 import 관계를 지키는 테스트.
// 서로를 import 하는 순환이 생기면 모듈 하나만 필요한 페이지도 전부를 불러오게 된다.
const JS_DIR = new URL('../../main/resources/static/js/', import.meta.url);

function importGraph() {
  const graph = new Map();
  for (const file of readdirSync(JS_DIR).filter(name => name.endsWith('.js'))) {
    const source = readFileSync(new URL(file, JS_DIR), 'utf-8');
    const imports = [...source.matchAll(/^import\s[^;]*?from\s+["']\.\/([\w.-]+\.js)["']/gm)]
        .map(match => match[1]);
    graph.set(file, imports);
  }
  return graph;
}

function findCycle(graph) {
  const visiting = new Set();
  const done = new Set();
  const visit = (file, path) => {
    if (visiting.has(file)) {
      return [...path.slice(path.indexOf(file)), file];
    }
    if (done.has(file)) {
      return null;
    }
    visiting.add(file);
    for (const next of graph.get(file) ?? []) {
      const cycle = visit(next, [...path, file]);
      if (cycle) {
        return cycle;
      }
    }
    visiting.delete(file);
    done.add(file);
    return null;
  };
  for (const file of graph.keys()) {
    const cycle = visit(file, []);
    if (cycle) {
      return cycle;
    }
  }
  return null;
}

test('static/js modules do not import each other in a cycle', () => {
  const cycle = findCycle(importGraph());

  assert.equal(cycle, null, `import cycle: ${cycle?.join(' -> ')}`);
});

test('board.js imports nothing, so board-only pages load just that file', () => {
  assert.deepEqual(importGraph().get('board.js'), []);
});
