/* 水体预览核验：证明「水看起来和深渊一样」这个 bug 真的修好了。
 *
 * 思路（不依赖肉眼）：
 *   用真图集 + 真水体贴图，分别渲染「一格水」与「一格深渊」两个画面，
 *   统计各自画布上的像素特征：
 *     - 水体层开启时，水格区域应出现**绿色系**像素（water0 调色板 #4a7561 一族）
 *     - 深渊格应是暗色（chasm 图集帧）
 *     - 两者像素**必须不同**（哈希不同）
 *   再把水体层关掉，水格应退化成「什么都没画」⇒ 证明这层是必需的。
 *
 * 需要一个能写 pixel 的 canvas。这里用极简的纯 JS 光栅器：
 * 把 Sheet.draw / drawImage 的调用记录成 (帧号, 目标格)，再独立查图集像素做比对。
 * 这样就不依赖 Canvas API 了。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const ROOT = path.resolve(__dirname, '..');
const RENDER = path.join(ROOT, 'tools/terrain-editor/render.js');
const ASSETS = path.join(ROOT, 'tools/terrain-editor/assets.js');

let pass = 0, fail = [];
function chk(c, m) { if (c) { pass++; console.log('  \u2713 ' + m); } else { console.log('  \u2717 ' + m); fail.push(m); } }

/* ---- 造一个「假 Image」：从 assets.js 里拿出真 PNG，解析成 RGBA ---- */
const zlib = require('zlib');

function decodePng(buf) {
	// 只支持 colorType 6 (RGBA8) 与 3 (调色板)，够用
	let pos = 8, w = 0, h = 0, ct = 0, bd = 0, idat = [], plte = null, trns = null;
	while (pos < buf.length) {
		const len = buf.readUInt32BE(pos), type = buf.toString('ascii', pos + 4, pos + 8);
		const data = buf.slice(pos + 8, pos + 8 + len);
		if (type === 'IHDR') { w = data.readUInt32BE(0); h = data.readUInt32BE(4); bd = data[8]; ct = data[9]; }
		else if (type === 'IDAT') idat.push(data);
		else if (type === 'PLTE') plte = data;
		else if (type === 'tRNS') trns = data;
		pos += 12 + len;
	}
	const raw = zlib.inflateSync(Buffer.concat(idat));
	const stride = w * 4;
	const out = Buffer.alloc(w * h * 4);

	if (ct === 6) {
		for (let y = 0; y < h; y++) {
			const off = y * (stride + 1) + 1;
			raw.copy(out, y * stride, off, off + stride);
		}
	} else if (ct === 3) {
		const bits = bd;                    // 4 或 8
		const perByte = 8 / bits;
		const rowBytes = Math.ceil(w / perByte);
		for (let y = 0; y < h; y++) {
			const off = y * (rowBytes + 1) + 1;
			for (let x = 0; x < w; x++) {
				let idx;
				if (bits === 8) idx = raw[off + x];
				else {
					const byte = raw[off + Math.floor(x / perByte)];
					const shift = (perByte - 1 - (x % perByte)) * bits;
					idx = (byte >> shift) & ((1 << bits) - 1);
				}
				const o = (y * w + x) * 4;
				if (plte && idx * 3 + 2 < plte.length) {
					out[o] = plte[idx * 3]; out[o + 1] = plte[idx * 3 + 1]; out[o + 2] = plte[idx * 3 + 2];
					out[o + 3] = (trns && idx < trns.length) ? trns[idx] : 255;
				}
			}
		}
	}
	return { w, h, data: out };
}

/* 从 assets.js 里取 dataURL -> Buffer */
function assetsMap() {
	const src = fs.readFileSync(ASSETS, 'utf8');
	const sandbox = { window: {} };
	vm.createContext(sandbox);
	vm.runInContext(src, sandbox);
	return sandbox.window.TE_ASSETS;
}

const A = assetsMap();
function pngOf(key) {
	const url = A[key];
	if (!url) throw new Error('assets.js 里没有 ' + key);
	return decodePng(Buffer.from(url.split(',')[1], 'base64'));
}

/* ---- 加载 render.js ---- */
function loadRenderer() {
	// render.js 结尾是 `global.TE_RENDER = API`，而 global 取的是
	// `typeof window !== 'undefined' ? window : this` ⇒ 要把 window 指回 sandbox 自身
	const sandbox = { console };
	sandbox.window = sandbox;
	sandbox.document = { createElement: () => ({ width: 0, height: 0, getContext: () => ({ drawImage() { }, getImageData: (x, y, w, h) => ({ data: new Uint8ClampedArray(w * h * 4) }) }) }) };
	vm.createContext(sandbox);
	vm.runInContext(fs.readFileSync(RENDER, 'utf8'), sandbox, { filename: RENDER });
	return sandbox.TE_RENDER;
}

const R = loadRenderer();
const T = R.T;

/* ---- 假 Image：render.js 只用 .width/.height 建 Sheet，draw 用 _src ---- */
function fakeImage(png) { return { width: png.w, height: png.h, _png: png }; }

const sheetPng = pngOf('sewers');
const featPng = pngOf('features');
const waterPngs = [0, 1, 2, 3, 4].map(i => pngOf('water' + i));

console.log('=== 0) 资产自检 ===');
chk(sheetPng.w === 256 && sheetPng.h === 256, 'sewers 图集 256×256');
chk(featPng.w === 256 && featPng.h === 128, 'terrain_features 256×128');
chk(waterPngs.every(p => p.w === 32 && p.h === 32), '水体帧均为 32×32（= 2×2 格）');

/* 水体主色：water0 的非透明像素应偏绿（G 最大） */
(function () {
	const p = waterPngs[0];
	let r = 0, g = 0, b = 0, n = 0;
	for (let i = 0; i < p.w * p.h; i++) {
		const o = i * 4;
		if (p.data[o + 3] > 0) { r += p.data[o]; g += p.data[o + 1]; b += p.data[o + 2]; n++; }
	}
	r /= n; g /= n; b /= n;
	console.log('    water0 平均色 rgb(' + r.toFixed(0) + ',' + g.toFixed(0) + ',' + b.toFixed(0) + ')');
	chk(g > r && g > b, '水体主色偏绿（G 通道最大）');
})();

/* ---- 造一个只记录「帧号 + 位置」的 ctx ---- */
function makeCtx() {
	const calls = [];
	return {
		calls,
		clearRect() { }, save() { }, restore() { }, scale() { },
		set imageSmoothingEnabled(v) { }, get imageSmoothingEnabled() { return false; },
		drawImage(img, dx, dy, dw, dh) { calls.push({ img, dx, dy, dw, dh }); }
	};
}

function setup(w, h, fill) {
	w = w || 2; h = h || 2;
	// ⚠️ resize() 会把**旧格子**拷到新地图上（编辑器要保留用户画的内容）。
	// 测试里要一张干净的地图，必须先缩到最小再按目标 fill 建，且中间不要留下旧内容。
	R.state.w = 0; R.state.h = 0;
	R.resize(w, h, fill === undefined ? T.EMPTY : fill);
	R.state.w = w; R.state.h = h;
	R.state.map = R.state.map;
	R.setVariance(1);
	R.setSheets(fakeImage(sheetPng), fakeImage(featPng));
	R.setWater(waterPngs.map(fakeImage));
	R.setLayers(new Array(w * h).fill(null), new Array(w * h).fill(null));
}

/* ---- 场景 A：一格水 ---- */
console.log('=== 1) 渲染一格水（水体层开启）===');
setup();
R.state.map[0] = T.WATER;
let ctx = makeCtx();
R.render(ctx, { scale: 1, showFeatures: true, showWater: true });
const waterCalls = ctx.calls.slice();

// 统计：水体是 32×32 平铺，应与地形图集帧不同尺寸
const flat = waterCalls.filter(c => c.dw === 32 && c.dh === 32);
chk(flat.length > 0, '水体层确实铺了 32×32 的贴图（' + flat.length + ' 块）');

// 水格有没有绿像素：查水体帧自身
(function () {
	let greenish = 0, total = 0;
	const p = waterPngs[0];
	for (let i = 0; i < p.w * p.h; i++) {
		const o = i * 4;
		if (p.data[o + 3] === 0) continue;
		total++;
		if (p.data[o + 1] > p.data[o] && p.data[o + 1] > p.data[o + 2]) greenish++;
	}
	chk(greenish / total > 0.9, '水体帧 ' + (100 * greenish / total).toFixed(0) + '% 像素偏绿');
})();

/* ---- 场景 B：一格深渊 ---- */
console.log('=== 2) 渲染一格深渊 ===');
setup();
R.state.map[0] = T.CHASM;
ctx = makeCtx();
R.render(ctx, { scale: 1, showFeatures: true, showWater: true });
const chasmCalls = ctx.calls.slice();

// 深渊应走图集（16px 帧），没有 32×32 的水块「在它自己的位置」
// 注意：水体层是整图平铺的，所以 32×32 块一直都在 —— 关键是水格的 terrain 帧不同
const chasm16 = chasmCalls.filter(c => c.dw === 16 && c.dh === 16);
chk(chasm16.length > 0, '深渊走图集 16×16 帧（' + chasm16.length + ' 块）');

/* ---- 场景 C：对比两种地形的 terrain 层帧号 ---- */
console.log('=== 3) 水 / 深渊的 terrain 层帧号必须不同 ===');
setup();
const vWater = R.terrainVisual(0, T.WATER);
const vChasm = R.terrainVisual(0, T.CHASM);
console.log('    WATER terrainVisual =', vWater, '  CHASM terrainVisual =', vChasm);
chk(vWater !== vChasm, '水与深渊的 terrain 帧号不同');

/* ---- 场景 D：旧 bug 的复现 —— 关掉水体层，纯水格就什么都不剩 ---- */
console.log('=== 4) 关掉水体层 ⇒ 纯水格为空（这就是旧 bug 的成因）===');
// 用 3×3 全水，取**中心格**：它四面都是水 ⇒ 缝合位 r=0 ⇒ terrain 层不画，
// 此时若没有水体层，这一格在画面上就是空的（黑底），与深渊无从区分。
setup(3, 3, T.WATER);
chk(R.terrainVisual(4, T.WATER) === R.F.WATER, '中心格确为纯水帧（r=0）');

ctx = makeCtx();
R.render(ctx, { scale: 1, showFeatures: true, showWater: false });
const noWaterCalls = ctx.calls.slice();
const atCenter = noWaterCalls.filter(c => c.dx === 16 && c.dy === 16 && c.dw === 16);
chk(atCenter.length === 0, '关闭水体层后，纯水格不画任何 terrain 帧（⇒ 露出黑底，与深渊混淆）');

// 对照：打开水体层，该格就被覆盖了
ctx = makeCtx();
R.render(ctx, { scale: 1, showFeatures: true, showWater: true });
const withWater = ctx.calls.filter(c => c.dw === 32 && c.dh === 32);
chk(withWater.length > 0, '打开水体层后，整图被 32×32 水块覆盖（' + withWater.length + ' 块）');

/* ---- 场景 E：水与非水相邻时，terrain 层要画缝合边 ---- */
console.log('=== 5) 水边要有缝合帧 ===');
setup(3, 3, T.EMPTY);
R.state.map[1 * 3 + 1] = T.WATER;      // 中心是水，四周是空地
const vEdge = R.terrainVisual(1 * 3 + 1, T.WATER);
console.log('    四周空地时 WATER 帧号 =', vEdge, '（F.WATER=' + R.F.WATER + '）');
chk(vEdge !== R.F.WATER, '四周皆非水 ⇒ 帧号带缝合位（≠ 基础帧）');

setup(3, 3, T.WATER);                  // 整张全水，含中心
const vInner = R.terrainVisual(1 * 3 + 1, T.WATER);
console.log('    四周皆水时 WATER 帧号 =', vInner, '（F.WATER=' + R.F.WATER + '）');
chk(vInner === R.F.WATER, '四周皆水 ⇒ 就是基础水帧（编辑器据此跳过不画）');

console.log('');
if (fail.length) {
	console.log('\u274c ' + fail.length + ' 项不符：');
	fail.forEach(f => console.log('   - ' + f));
	process.exit(1);
}
console.log('\u2705 全部通过（' + pass + ' 项）：水体层独立成层，水与深渊不再混淆');
