// 在 Node 里跑：把 render.js 的纯计算部分接上假 canvas，比对帧号
// 用法：node _te_harness.js <render.js绝对路径> <cases.json绝对路径>
const fs = require('fs');
const vm = require('vm');

// 假 DOM：Sheet 需要 createElement('canvas') / getContext('2d') / getImageData
function fakeCtx() {
  return { drawImage(){}, getImageData(w,h){ return {data:new Uint8ClampedArray(4)}; },
           clearRect(){}, save(){}, restore(){}, scale(){} };
}
const sandbox = {
  window: {},
  document: { createElement(){ return { width:0, height:0, getContext(){ return fakeCtx(); } }; } },
  console
};
sandbox.window = sandbox;
vm.createContext(sandbox);
// 显式吃绝对路径，别用 __dirname（harness 与 render.js 不在同一目录）
const RENDER_JS = process.argv[2];
vm.runInContext(fs.readFileSync(RENDER_JS, 'utf8'), sandbox);
const R = sandbox.TE_RENDER;

const cases = JSON.parse(fs.readFileSync(process.argv[3], 'utf8'));
let bad = 0, checked = 0;
const failDetail = [];

for (const c of cases) {
  const S = R.state;
  S.w = c.w; S.h = c.h; S.depth = c.depth; S.featuresStage = c.stage;
  S.map = Int32Array.from(c.map);
  S.variance = new Uint8Array(S.w * S.h);
  // 用与 Python 相同的 LCG 重算 variance
  let s = c.seed >>> 0;
  for (let i = 0; i < S.variance.length; i++) {
    s = (Math.imul(s, 1664525) + 1013904223) >>> 0;
    S.variance[i] = (s >>> 16) % 100;
  }

  for (let p = 0; p < c.map.length; p++) {
    const got = {
      terrain: R.terrainVisual(p, S.map[p]),
      raised:  R.raisedVisual(p, S.map[p]),
      walls:   R.wallsVisual(p, S.map[p], null),
      features:R.featuresVisual(p, S.map[p]),
    };
    for (const k of ['terrain','raised','walls','features']) {
      checked++;
      const exp = c[k][p];
      const g = (got[k] === undefined) ? -1 : got[k];
      if (g !== exp) {
        bad++;
        if (failDetail.length < 25) {
          failDetail.push(`  [${c.name}] pos=${p}(${p%c.w},${Math.floor(p/c.w)}) ` +
            `tile=${S.map[p]} ${k}: JS=${g} PY=${exp}`);
        }
      }
    }
  }
}
console.log(JSON.stringify({checked, bad, failDetail}));
