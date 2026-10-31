// 두 스크린샷 폴더를 픽셀 단위로 비교한다. 다른 화면은 diff 이미지(달라진 픽셀 = 빨강)를 남긴다.
// 계산된 스타일(*.styles.json)이 있으면 요소별 속성 값도 비교해 달라진 속성을 보여 준다.
//
//   node scripts/visual-check/compare.mjs <기준 폴더> <비교 폴더> [diff 폴더]
//
// 종료 코드: 모두 같으면 0, 하나라도 다르면 1
import {existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync} from 'node:fs';
import {join} from 'node:path';
import {decodePng, encodePng} from './png.mjs';

const [baseDir, nextDir, diffDir = join(nextDir, '..', 'diff')] = process.argv.slice(2);
if (!baseDir || !nextDir) {
  console.error('usage: node compare.mjs <baseline dir> <current dir> [diff dir]');
  process.exit(2);
}

// 안티앨리어싱 정도의 미세한 차이는 무시한다 (채널당 0~255 중 이 값 이하)
const CHANNEL_TOLERANCE = 8;

const names = [...new Set([...readdirSync(baseDir), ...readdirSync(nextDir)])]
    .filter(name => name.endsWith('.png')).sort();
let changed = 0;
for (const name of names) {
  const basePath = join(baseDir, name);
  const nextPath = join(nextDir, name);
  if (!existsSync(basePath) || !existsSync(nextPath)) {
    console.log(`MISSING  ${name} (${existsSync(basePath) ? 'only in baseline' : 'only in current'})`);
    changed++;
    continue;
  }
  const result = compare(decodePng(readFileSync(basePath)), decodePng(readFileSync(nextPath)));
  if (result.differentPixels === 0 && result.sameSize) {
    console.log(`same     ${name}`);
    continue;
  }
  changed++;
  mkdirSync(diffDir, {recursive: true});
  writeFileSync(join(diffDir, name), encodePng(result.diff));
  const box = result.box;
  console.log(`CHANGED  ${name}: ${result.differentPixels} px`
      + (result.sameSize ? '' : `, size ${result.baseSize} -> ${result.nextSize}`)
      + (box ? `, area x ${box.minX}-${box.maxX}, y ${box.minY}-${box.maxY}` : ''));
}
// 계산된 스타일 비교 (*.styles.json). 같은 위치의 요소끼리 속성 값을 비교한다.
let styleChanged = 0;
const styleNames = [...new Set([...readdirSync(baseDir), ...readdirSync(nextDir)])]
    .filter(name => name.endsWith('.styles.json')).sort();
for (const name of styleNames) {
  const basePath = join(baseDir, name);
  const nextPath = join(nextDir, name);
  if (!existsSync(basePath) || !existsSync(nextPath)) {
    continue;
  }
  const base = JSON.parse(readFileSync(basePath, 'utf-8'));
  const next = JSON.parse(readFileSync(nextPath, 'utf-8'));
  const differences = [];
  for (const path of new Set([...Object.keys(base.elements), ...Object.keys(next.elements)])) {
    const a = base.elements[path]?.split('|');
    const b = next.elements[path]?.split('|');
    if (!a || !b) {
      differences.push(`${path}: ${a ? 'removed' : 'added'}`);
      continue;
    }
    base.props.forEach((prop, i) => {
      if (a[i] !== b[i]) {
        differences.push(`${path}: ${prop} ${a[i]} -> ${b[i]}`);
      }
    });
  }
  if (differences.length) {
    styleChanged++;
    console.log(`STYLES   ${name}: ${differences.length} differences`);
    differences.slice(0, 8).forEach(d => console.log(`           ${d}`));
  }
}

console.log(`\n${names.length - changed} same, ${changed} changed`
    + (changed ? ` (diff images: ${diffDir})` : '')
    + (styleNames.length ? `; computed styles: ${styleNames.length - styleChanged} same, ${styleChanged} changed` : ''));
process.exit(changed || styleChanged ? 1 : 0);

function compare(base, next) {
  const width = Math.max(base.width, next.width);
  const height = Math.max(base.height, next.height);
  const pixels = Buffer.alloc(width * height * 4);
  let differentPixels = 0;
  let box = null;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const target = (y * width + x) * 4;
      const a = pixelAt(base, x, y);
      const b = pixelAt(next, x, y);
      const different = !a || !b || a.some((value, i) => Math.abs(value - b[i]) > CHANNEL_TOLERANCE);
      if (different) {
        differentPixels++;
        pixels.set([255, 0, 0, 255], target);
        box = box ?? {minX: x, maxX: x, minY: y, maxY: y};
        box.minX = Math.min(box.minX, x);
        box.maxX = Math.max(box.maxX, x);
        box.minY = Math.min(box.minY, y);
        box.maxY = Math.max(box.maxY, y);
      } else {
        // 같은 픽셀은 흐리게 남겨 어디가 바뀌었는지 위치를 알 수 있게 한다
        const gray = Math.round((a[0] + a[1] + a[2]) / 3);
        const faded = 255 - Math.round((255 - gray) * 0.25);
        pixels.set([faded, faded, faded, 255], target);
      }
    }
  }
  return {
    differentPixels,
    box,
    sameSize: base.width === next.width && base.height === next.height,
    baseSize: `${base.width}x${base.height}`,
    nextSize: `${next.width}x${next.height}`,
    diff: {width, height, pixels},
  };
}

function pixelAt(image, x, y) {
  if (x >= image.width || y >= image.height) {
    return null;
  }
  const offset = (y * image.width + x) * 4;
  return [image.pixels[offset], image.pixels[offset + 1], image.pixels[offset + 2]];
}
