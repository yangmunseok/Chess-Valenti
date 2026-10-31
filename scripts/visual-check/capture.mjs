// 주요 페이지를 헤드리스 Chrome 으로 찍는다. CSS 를 고치기 전후에 찍어 compare.mjs 로 비교한다.
//
// 1) 앱을 H2(test 프로필)로 띄운다. 프로젝트 루트에서:
//      SERVER_PORT=8090 SPRING_CONFIG_ADDITIONALLOCATION=optional:file:./src/test/resources/ \
//        ./mvnw spring-boot:run -DskipJsTests=true -Dspring-boot.run.useTestClasspath=true -Dspring-boot.run.profiles=test
// 2) 처음 한 번 테스트 데이터를 넣으며 기준을 찍는다:
//      node scripts/visual-check/capture.mjs --seed target/visual/baseline
// 3) CSS 를 고친 뒤 다시 찍고 비교한다 (같은 앱을 계속 띄워 두면 데이터도 같다):
//      node scripts/visual-check/capture.mjs target/visual/current
//      node scripts/visual-check/compare.mjs target/visual/baseline target/visual/current
//
// 환경 변수: BASE_URL (기본 http://127.0.0.1:8090), CHROME_PATH, ADMIN_USER / ADMIN_PASSWORD
// (기본값은 application-test.yml 의 관리자 계정)
import {spawn} from 'node:child_process';
import {existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';

const args = process.argv.slice(2);
const seed = args.includes('--seed');
const outDir = args.find(arg => !arg.startsWith('--'));
if (!outDir) {
  console.error('usage: node capture.mjs [--seed] <output dir>');
  process.exit(2);
}
const BASE_URL = process.env.BASE_URL ?? 'http://127.0.0.1:8090';
const ADMIN_USER = process.env.ADMIN_USER ?? 'admin';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD ?? 'test-admin-password';
const PAWN_FEN = 'rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2';

// [파일 이름, 경로]. 로그인하지 않은 상태와 관리자로 로그인한 상태로 나눈다.
const PUBLIC_PAGES = [
  ['landing', '/landing'],
  ['login', '/login'],
  ['signup', '/signup'],
  ['forgot-password', '/forgot-password'],
  ['login-required', '/login-required'],
  ['access-denied', '/access-denied'],
];
const ADMIN_PAGES = [
  ['home', '/'],
  ['insight', '/insight'],
  ['study', '/study'],
  ['inquiry', '/inquiry'],
  ['analysis', '/analysis'],
  ['pawn-games', `/pawn-games?fen=${encodeURIComponent(PAWN_FEN)}`],
  ['post-notice', '/posts/1'],
  ['post-study', '/posts/3'],
  ['inquiry-detail', '/inquiries/1'],
  ['admin-users', '/admin/users'],
  ['admin-user', '/admin/users/1'],
  ['admin-support', '/admin/support'],
  ['admin-contents-notice', '/admin/contents?mode=notice'],
  ['admin-contents-faq', '/admin/contents?mode=faq'],
  ['admin-contents-study', '/admin/contents?mode=study'],
  ['admin-insight', '/admin/insight'],
  ['admin-finance', '/admin/finance'],
  ['admin-editor', '/admin/editor?type=NOTICE'],
];
const VIEWPORTS = [
  ['desktop', {width: 1400, height: 900, deviceScaleFactor: 1, mobile: false}],
  ['mobile', {width: 390, height: 844, deviceScaleFactor: 1, mobile: true}],
];
const MAX_HEIGHT = 6000;

// 찍을 때마다 값이 바뀌는 요소. 내용을 고정 값으로 바꾼다 (예: 로그인할 때마다 늘어나는 접속자 수)
const VOLATILE = ['.value > span:first-child'];

// 찍기 전에 화면을 고정한다: 애니메이션과 깜박이는 커서를 끄고, 날짜·시각과 "N분 전"을 같은 값으로 바꾼다
const FREEZE = `(() => {
  const style = document.createElement('style');
  style.textContent = '*, *::before, *::after { animation: none !important; transition: none !important; caret-color: transparent !important; }';
  document.head.appendChild(style);
  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  const date = /\\d{4}[-.\\/]\\d{1,2}[-.\\/]\\d{1,2}(\\.?\\s*\\d{1,2}:\\d{2}(:\\d{2})?)?/g;
  const ago = /\\d+\\s*(초|분|시간|일)\\s*전/g;
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    node.nodeValue = node.nodeValue.replace(date, '2026-01-01 00:00').replace(ago, '1분 전');
  }
  document.querySelectorAll(${JSON.stringify(VOLATILE.join(','))}).forEach(el => {
    if (/^\\d+$/.test(el.textContent.trim())) {
      el.textContent = '1';
    }
  });
  return true;
})()`;

const chromePath = process.env.CHROME_PATH ?? [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/usr/bin/google-chrome',
].find(path => existsSync(path));
if (!chromePath) {
  console.error('Chrome not found. Set CHROME_PATH.');
  process.exit(2);
}

const profile = mkdtempSync(join(tmpdir(), 'visual-check-'));
const port = 9400 + Math.floor(Math.random() * 400);
const chrome = spawn(chromePath, ['--headless=new', `--remote-debugging-port=${port}`,
  `--user-data-dir=${profile}`, '--hide-scrollbars', '--force-color-profile=srgb',
  '--font-render-hinting=none', 'about:blank'], {stdio: 'ignore'});

try {
  const cdp = await connect(port);
  await cdp.send('Page.enable');
  await cdp.send('Runtime.enable');
  await cdp.send('Network.enable');
  // 분석 스크립트는 막는다 (화면과 무관하고 요청마다 결과가 다를 수 있다)
  await cdp.send('Network.setBlockedURLs', {urls: ['*googletagmanager.com*', '*google-analytics.com*']});

  mkdirSync(outDir, {recursive: true});
  await capturePages(cdp, PUBLIC_PAGES);
  await login(cdp);
  if (seed) {
    await seedData(cdp);
  }
  await capturePages(cdp, ADMIN_PAGES);
  console.log(`\nscreenshots: ${outDir}`);
} finally {
  chrome.kill();
  await sleep(500);
  rmSync(profile, {recursive: true, force: true, maxRetries: 5});
}

async function capturePages(cdp, pages) {
  for (const [viewportName, metrics] of VIEWPORTS) {
    await cdp.send('Emulation.setDeviceMetricsOverride', metrics);
    for (const [name, path] of pages) {
      await navigate(cdp, path);
      await cdp.evaluate(FREEZE);
      await sleep(300);
      const {cssContentSize} = await cdp.send('Page.getLayoutMetrics');
      const height = Math.min(MAX_HEIGHT, Math.max(metrics.height, Math.ceil(cssContentSize.height)));
      const {data} = await cdp.send('Page.captureScreenshot', {
        format: 'png',
        captureBeyondViewport: true,
        clip: {x: 0, y: 0, width: metrics.width, height, scale: 1},
      });
      const file = `${name}.${viewportName}.png`;
      writeFileSync(join(outDir, file), Buffer.from(data, 'base64'));
      const at = await cdp.evaluate('location.pathname + location.search');
      console.log(`${file.padEnd(40)} ${at}`);
    }
  }
}

async function navigate(cdp, path) {
  const loaded = cdp.waitFor('Page.loadEventFired', 15000);
  await cdp.send('Page.navigate', {url: BASE_URL + path});
  await loaded;
  // 웹 글꼴과 이미지, fetch 로 채우는 목록을 기다린다
  await cdp.evaluate('document.fonts.ready.then(() => true)');
  await sleep(1500);
}

async function login(cdp) {
  await navigate(cdp, '/login');
  await cdp.evaluate(`(() => {
    document.getElementById('username').value = ${JSON.stringify(ADMIN_USER)};
    document.getElementById('password').value = ${JSON.stringify(ADMIN_PASSWORD)};
    document.querySelector('form.login-form').submit();
    return true;
  })()`);
  await cdp.waitFor('Page.loadEventFired', 15000);
  const at = await cdp.evaluate('location.pathname');
  if (at === '/login') {
    throw new Error('admin login failed');
  }
}

// 게시글 1~3(공지, FAQ, 스터디), 문의 1, 댓글, 관리자의 인사이트 결과를 만든다. 앱을 새로 띄운 직후에 한 번만 실행한다.
async function seedData(cdp) {
  await navigate(cdp, '/admin/editor?type=NOTICE');
  const result = await cdp.evaluate(`(async () => {
    const header = document.querySelector('meta[name="_csrf_header"]').content;
    const token = document.querySelector('meta[name="_csrf"]').content;
    const post = async fields => {
      const form = new FormData();
      Object.entries(fields).forEach(([key, value]) => form.append(key, value));
      return (await fetch('/api/admin/posts', {method: 'POST', headers: {[header]: token}, body: form})).status;
    };
    const json = async (url, body) => (await fetch(url, {method: 'POST',
      headers: {[header]: token, 'Content-Type': 'application/json'}, body: JSON.stringify(body)})).status;
    return [
      await post({title: '서버 점검 안내', content: '<p>1월 1일 새벽 2시부터 4시까지 서버를 점검합니다.</p><p>점검 중에는 분석 기능을 쓸 수 없습니다.</p>', postType: 'NOTICE'}),
      await post({title: '분석은 무료인가요?', content: '<p>기본 분석은 무료입니다.</p>', postType: 'FAQ'}),
      await post({title: '카로칸 디펜스 기초', content: '<p>1. e4 c6 으로 시작하는 단단한 오프닝입니다.</p>', postType: 'STUDY', difficulty: 'BEGINNER', introduction: '카로칸의 기본 구조를 배웁니다.'}),
      await json('/api/inquiries', {title: '결제 후 등급이 바뀌지 않았어요', content: '결제는 완료되었는데 등급이 그대로입니다.', category: 'INQUIRY_ACCOUNT'}),
      await json('/api/posts/1/comments', {content: '확인했습니다. 감사합니다.'}),
      // 인사이트 결과 화면(미니 보드, 승률 막대)이 보이도록 저장된 결과를 하나 만든다
      await json('/api/insight', {username: 'hikaru', perfType: 'blitz', since: 1735689600000, data: {
        '8/pp3ppp/8/2ppp3/2PPP3/8/PP3PPP/8 w': {white: {whiteWon: 12, blackWon: 5, drawn: 3}, black: {whiteWon: 4, blackWon: 9, drawn: 2}},
        '8/pp2pppp/3p4/8/3PP3/8/PPP2PPP/8 w': {white: {whiteWon: 8, blackWon: 7, drawn: 4}, black: {whiteWon: 6, blackWon: 6, drawn: 1}},
        '8/ppp2ppp/3p4/4p3/4P3/3P4/PPP2PPP/8 w': {white: {whiteWon: 3, blackWon: 9, drawn: 1}, black: {whiteWon: 10, blackWon: 2, drawn: 0}},
      }}),
    ].join(',');
  })()`);
  console.log(`seed: ${result}`);
  if (!/^(20\d,?)+$/.test(result)) {
    throw new Error(`seeding failed: ${result}`);
  }
}

async function connect(debugPort) {
  let targets;
  for (let i = 0; i < 100 && !targets; i++) {
    try {
      targets = await (await fetch(`http://127.0.0.1:${debugPort}/json`)).json();
    } catch {
      await sleep(200);
    }
  }
  const page = targets?.find(target => target.type === 'page');
  if (!page) {
    throw new Error('could not connect to Chrome');
  }
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    ws.onopen = resolve;
    ws.onerror = reject;
  });
  let nextId = 0;
  const pending = new Map();
  const waiters = [];
  ws.onmessage = event => {
    const message = JSON.parse(event.data);
    if (message.id && pending.has(message.id)) {
      const {resolve, reject} = pending.get(message.id);
      pending.delete(message.id);
      message.error ? reject(new Error(message.error.message)) : resolve(message.result);
    } else if (message.method) {
      for (const waiter of waiters.filter(w => w.method === message.method)) {
        waiters.splice(waiters.indexOf(waiter), 1);
        waiter.resolve(message.params);
      }
    }
  };
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++nextId;
    pending.set(id, {resolve, reject});
    ws.send(JSON.stringify({id, method, params}));
  });
  return {
    send,
    waitFor: (method, timeout) => new Promise((resolve, reject) => {
      const waiter = {method, resolve};
      waiters.push(waiter);
      setTimeout(() => {
        if (waiters.includes(waiter)) {
          waiters.splice(waiters.indexOf(waiter), 1);
          reject(new Error(`timeout waiting for ${method}`));
        }
      }, timeout);
    }),
    async evaluate(expression) {
      const result = await send('Runtime.evaluate', {expression, awaitPromise: true, returnByValue: true});
      if (result.exceptionDetails) {
        throw new Error(result.exceptionDetails.exception?.description ?? result.exceptionDetails.text);
      }
      return result.result.value;
    },
  };
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
