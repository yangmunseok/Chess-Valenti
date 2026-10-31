import {test} from 'node:test';
import assert from 'node:assert/strict';
import {withCsrf} from '../../main/resources/static/js/csrf.js';

// <head> 의 메타 태그만 흉내 낸다
function fakeDocument(meta) {
  return {
    querySelector(selector) {
      const name = selector.match(/meta\[name="(.+)"\]/)?.[1];
      return name in meta ? {content: meta[name]} : null;
    },
  };
}

test('withCsrf adds the token under the header name from the meta tags', () => {
  globalThis.document = fakeDocument({_csrf: 'token-1', _csrf_header: 'X-CSRF-TOKEN'});

  assert.deepEqual(withCsrf({'Content-Type': 'application/json'}),
      {'Content-Type': 'application/json', 'X-CSRF-TOKEN': 'token-1'});
});

test('withCsrf leaves headers alone on pages without the meta tags', () => {
  globalThis.document = fakeDocument({});

  const headers = {'Content-Type': 'application/json'};
  assert.deepEqual(withCsrf(headers), headers);
  assert.notEqual(withCsrf(headers), headers);
});
