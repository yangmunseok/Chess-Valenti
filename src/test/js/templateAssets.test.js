import {test} from 'node:test';
import assert from 'node:assert/strict';
import {existsSync, readdirSync, readFileSync, statSync} from 'node:fs';
import {join} from 'node:path';
import {fileURLToPath} from 'node:url';

// 템플릿이 불러오는 /css, /js 파일이 실제로 있는지 확인한다. 파일을 옮기거나 지울 때 링크가 깨지지 않게 한다.
const RESOURCES = fileURLToPath(new URL('../../main/resources/', import.meta.url));

function htmlFiles(dir) {
  return readdirSync(dir).flatMap(name => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? htmlFiles(path) : name.endsWith('.html') ? [path] : [];
  });
}

test('every /css and /js file a template links to exists', () => {
  const missing = [];
  for (const file of htmlFiles(join(RESOURCES, 'templates'))) {
    const html = readFileSync(file, 'utf-8');
    for (const [, path] of html.matchAll(/["{(]\/?((?:css|js)\/[\w./-]+\.(?:css|js))/g)) {
      if (!existsSync(join(RESOURCES, 'static', path))) {
        missing.push(`${file.slice(RESOURCES.length)} -> /${path}`);
      }
    }
  }
  assert.deepEqual(missing, []);
});
