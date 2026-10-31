import {test} from 'node:test';
import assert from 'node:assert/strict';
import {shiftWindow, windowStart} from '../../main/resources/static/js/pagination.js';

test('windowStart finds the group of ten that holds the current page', () => {
  assert.equal(windowStart(0), 0);
  assert.equal(windowStart(9), 0);
  assert.equal(windowStart(10), 10);
  assert.equal(windowStart(27), 20);
  assert.equal(windowStart(-1), 0);
});

test('shiftWindow moves by ten pages and stays within the pages', () => {
  // 25쪽: 묶음은 0, 10, 20
  assert.equal(shiftWindow(0, 10, 25), 10);
  assert.equal(shiftWindow(10, 10, 25), 20);
  assert.equal(shiftWindow(20, 10, 25), 20);
  assert.equal(shiftWindow(20, -10, 25), 10);
  assert.equal(shiftWindow(0, -10, 25), 0);
  // 10쪽 이하면 묶음이 하나뿐
  assert.equal(shiftWindow(0, 10, 10), 0);
  assert.equal(shiftWindow(0, 10, 0), 0);
});
