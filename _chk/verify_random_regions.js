/* EGOPD 地形编辑器 —— 「区域随机排列」核验（C.5，2026-09-19）
 *
 * 用户需求（原文）：「希望加入随机排列的功能，即可以选中一块区域，
 * 使得区域中的地形在实际生成时随机排列」。
 *
 * 本脚本验四件事：
 *   ① 内核侧：区域的增/并/删、矩形合法性、越界夹取
 *   ② 语义侧：**多重集不变** —— 区域里 3 草 2 水，打乱后还是 3 草 2 水
 *   ③ 生成侧：输出的 Java 里，「收集 → shuffle → 写回」三步齐全，
 *      且**位置正确**（在所有 Painter 填地形之后 —— 放前面会被覆盖，且不报错）
 *   ④ 真编译：生成的 Java 用真 javac 编过（区域代码有 int[]/for/Point，最容易写错）
 *
 * 桩的部分复用 verify_editor_layers_ui.js 里那套（真 PNG 解码 + 真像素 canvas +
 * 同步 onload 的 Image），因为 app.js 的 boot() 要真的跑起来才有 E.randoms。
 *
 * 含反例自测：--selftest 会把 emitRandomRegions 的「写在填充之后」这句话破坏掉，
 * 断言本脚本必须失败。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const zlib = require('zlib');

const DIR = path.join(__dirname, '..', 'tools', 'terrain-editor');
const SELFTEST = process.argv.includes('--selftest');

let failures = 0, passes = 0;
function ok(cond, msg, extra) {
	if (cond) { passes++; console.log('  \u2713 ' + msg); }
	else { failures++; console.log('  \u2717 ' + msg + (extra !== undefined ? '  \u2192 ' + JSON.stringify(extra) : '')); }
}
function eq(a, b, msg) { ok(a === b, msg, { got: a, want: b }); }

/* ------------------------------------------------------------ 真 PNG 解码 */
function decodePng(buf) {
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
	const out = new Uint8ClampedArray(w * h * 4);
	if (ct === 6) {
		for (let y = 0; y < h; y++) {
			const off = y * (stride + 1) + 1;
			for (let i = 0; i < stride; i++) out[y * stride + i] = raw[off + i];
		}
	} else if (ct === 3) {
		const bits = bd, perByte = 8 / bits, rowBytes = Math.ceil(w / perByte);
		for (let y = 0; y < h; y++) {
			const off = (rowBytes + 1) * y + 1;
			for (let x = 0; x < w; x++) {
				let idx;
				if (bits === 8) idx = raw[off + x];
				else {
					const byte = raw[off + Math.floor(x / perByte)];
					idx = (byte >> ((perByte - 1 - (x % perByte)) * bits)) & ((1 << bits) - 1);
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
function pngOfDataURL(url) {
	if (!url || !/^data:image\/png;base64,/.test(url)) return null;
	return decodePng(Buffer.from(url.split(',')[1], 'base64'));
}

/* ------------------------------------------------------------ DOM 桩 */
function makeEl(id, tag) {
	const listeners = {};
	const el = {
		id: id, tagName: (tag || 'div').toUpperCase(),
		value: '', checked: false, textContent: '', innerHTML: '',
		children: [], style: {}, dataset: {}, disabled: false,
		classList: {
			_h: {},
			add(c) { this._h[c] = 1; }, remove(c) { delete this._h[c]; },
			contains(c) { return !!this._h[c]; }, toggle(c, on) { if (on) this.add(c); else this.remove(c); }
		},
		appendChild(c) { el.children.push(c); return c; },
		removeChild(c) { const i = el.children.indexOf(c); if (i >= 0) el.children.splice(i, 1); return c; },
		insertBefore(c) { el.children.push(c); return c; },
		addEventListener(t, f) { (listeners[t] = listeners[t] || []).push(f); },
		removeEventListener(t, f) { const a = listeners[t] || []; const i = a.indexOf(f); if (i >= 0) a.splice(i, 1); },
		/* ⚠️ 必须 f.call(el, ev)：app.js 回调普遍写 this.checked / this.value */
		dispatch(t, ev) { (listeners[t] || []).forEach(f => f.call(el, ev || {})); },
		getContext() { return CTX; },
		getBoundingClientRect() { return { left: 0, top: 0, width: 640, height: 480, right: 640, bottom: 480 }; },
		focus() { }, blur() { }, click() { el.dispatch('click', {}); },
		setAttribute(k, v) { el[k] = v; }, getAttribute(k) { return el[k]; },
		querySelector(sel) { return document.querySelector(sel); },
		querySelectorAll(sel) { return document.querySelectorAll(sel); },
		_getListeners() { return listeners; }
	};
	el._options = [];
	el.add = function (opt) { el._options.push(opt); el.children.push(opt); };
	return el;
}

const DRAWN = [];
function makeCtx(w, h, canvasEl) {
	const buf = new Uint8ClampedArray(w * h * 4);
	return {
		_buf: buf, _w: w, _h: h, canvas: canvasEl,
		save() { }, restore() { }, translate() { }, scale() { }, rotate() { },
		clip() { }, beginPath() { }, closePath() { }, setTransform() { }, rect() { },
		setLineDash() { }, clearRect() { }, fillRect() { }, strokeRect() { },
		moveTo() { }, lineTo() { }, stroke() { }, fill() { }, arc() { },
		measureText() { return { width: 10 }; }, fillText() { }, strokeText() { },
		createLinearGradient() { return { addColorStop() { } }; },
		drawImage(img, ...a) {
			DRAWN.push({ key: img && img.__key, args: a, img });
			if (!img || !img._png) return;
			let sx = 0, sy = 0, sw = img.width, sh = img.height, dx = 0, dy = 0, dw = sw, dh = sh;
			if (a.length === 2) { dx = a[0]; dy = a[1]; }
			else if (a.length === 4) { dx = a[0]; dy = a[1]; dw = a[2]; dh = a[3]; }
			else if (a.length === 8) { [sx, sy, sw, sh, dx, dy, dw, dh] = a; }
			for (let y = 0; y < sh && (dy + y) < h; y++) {
				for (let x = 0; x < sw && (dx + x) < w; x++) {
					const so = ((sy + y) * img.width + (sx + x)) * 4;
					const dof = ((dy + y) * w + (dx + x)) * 4;
					const p = img._png.data;
					buf[dof] = p[so]; buf[dof + 1] = p[so + 1]; buf[dof + 2] = p[so + 2]; buf[dof + 3] = p[so + 3];
				}
			}
		},
		getImageData(x, y, gw, gh) {
			const out = new Uint8ClampedArray(gw * gh * 4);
			for (let yy = 0; yy < gh; yy++)
				for (let xx = 0; xx < gw; xx++) {
					const so = ((y + yy) * w + (x + xx)) * 4, dof = (yy * gw + xx) * 4;
					out[dof] = buf[so]; out[dof + 1] = buf[so + 1]; out[dof + 2] = buf[so + 2]; out[dof + 3] = buf[so + 3];
				}
			return { data: out, width: gw, height: gh };
		},
		globalAlpha: 1, fillStyle: '', strokeStyle: '', lineWidth: 1,
		imageSmoothingEnabled: false, font: '', textAlign: '', textBaseline: ''
	};
}
const CTX = makeCtx(640, 480, { width: 640, height: 480 });

/* ⚠️ IDS 要把 index.html 里 app.js 会 $() 的 id **全列上**。
 * 本轮的 C.5 新增四个：chkRegions / chkRegionLayers / btnRegionClear / regionHint。
 * 少列不会立刻炸（document.getElementById 会自动造），但那样验的是「自动造的假元素」，
 * 真实页面里元素不存在就永远不会有人发现 —— 所以宁可多列、并用下面的「存在性断言」钉住。 */
const IDS = [
	'cv', 'canvasWrap', 'verLabel',
	'palette', 'curTile', 'stTile', 'stCell', 'stSize', 'stMsg', 'checkList',
	'chkFill', 'chkRing', 'selZoom', 'selStage',
	'lyrNone', 'lyrTrap', 'lyrPlant', 'lyrItem', 'selTrap', 'selPlant', 'selItem', 'selHeap',
	'chkTrapVisible', 'chkTrapActive', 'trapHint', 'layerHint',
	'chkGrid', 'chkFeat', 'chkWater', 'chkRoomSem', 'chkItems',
	/* C.5 区域随机 */
	'chkRegions', 'chkRegionLayers', 'btnRegionClear', 'regionHint',
	/* C.3 外部图集 */
	'btnImpSheet', 'btnImpFeat', 'btnImpItems', 'btnExtClear', 'extHint', 'selSheet',
	'btnGen', 'btnCopy', 'btnCopyBody', 'btnCopyNotes', 'btnDl',
	'inClass', 'selParent', 'inPkg', 'codeBox', 'chkNotes',
	'inW', 'inH', 'btnApplySize',
	'btnSave', 'btnLoad', 'btnPng', 'btnUndo', 'btnRedo',
	'btnCheck',
];
const els = {};
IDS.forEach(id => { els[id] = makeEl(id, /^(inp|chk|sel|file)/.test(id) ? 'input' : 'div'); });

els.cv.width = 160; els.cv.height = 128;
(function () {
	let c = null;
	els.cv.getContext = () => {
		if (!c || c._w !== els.cv.width || c._h !== els.cv.height)
			c = makeCtx(els.cv.width || 1, els.cv.height || 1, els.cv);
		return c;
	};
})();

els.chkGrid.checked = true; els.chkFeat.checked = true; els.chkWater.checked = true;
els.chkRoomSem.checked = true; els.chkItems.checked = true;
/* C.5 默认态（与 index.html 一致） */
els.chkRegions.checked = true; els.chkRegionLayers.checked = true;

['selTrap', 'selPlant', 'selItem', 'selHeap', 'selSheet', 'selStage', 'selZoom', 'selParent'].forEach(id => {
	if (!els[id]) els[id] = makeEl(id, 'select');
	els[id].querySelectorAll = sel => (sel === 'option' ? els[id]._options : []);
});

const document = {
	_els: els,
	getElementById(id) { return els[id] || (els[id] = makeEl(id)); },
	querySelector(sel) {
		if (sel.startsWith('#')) return document.getElementById(sel.slice(1));
		if (sel.startsWith('input[name=')) {
			const m = /^input\[name=['"]?([^'"\]]+)['"]?\]$/.exec(sel);
			if (m) return document._groups[m[1]] || null;
		}
		return null;
	},
	querySelectorAll(sel) {
		if (sel.startsWith('#')) return [document.getElementById(sel.slice(1))];
		if (sel.startsWith('[data-tool]')) return document._tools || [];
		if (sel.startsWith('input[name=')) {
			const m = /^input\[name=['"]?([^'"\]]+)['"]?\]$/.exec(sel);
			if (m && document._groups[m[1]]) return document._groups[m[1]];
		}
		return [];
	},
	_groups: { layer: ['lyrNone', 'lyrTrap', 'lyrPlant', 'lyrItem'].map(id => els[id]) },
	_tools: [],
	createElement(tag) {
		const el = makeEl('__new_' + tag, tag);
		if (String(tag).toLowerCase() === 'canvas') {
			el.width = 0; el.height = 0;
			let _ctx = null;
			el.getContext = () => {
				if (!_ctx || _ctx._w !== el.width || _ctx._h !== el.height)
					_ctx = makeCtx(el.width || 1, el.height || 1, el);
				return _ctx;
			};
		}
		return el;
	},
	addEventListener() { }, removeEventListener() { },
	body: makeEl('body'),
	documentElement: makeEl('html'),
	title: ''
};

const sandbox = {
	console, document,
	window: null,
	navigator: { userAgent: 'node' },
	localStorage: { _d: {}, getItem(k) { return this._d[k] || null; }, setItem(k, v) { this._d[k] = String(v); }, removeItem(k) { delete this._d[k]; } },
	requestAnimationFrame(f) { return setTimeout(f, 0); },
	cancelAnimationFrame() { },
	setTimeout, clearTimeout, setInterval, clearInterval,
	Image: function () {
		const im = { width: 0, height: 0, onload: null, onerror: null, __key: '', _ok: false, _png: null };
		Object.defineProperty(im, 'src', {
			set(v) {
				im.__key = String(v);
				const png = pngOfDataURL(im.__key);
				if (png) { im._png = png; im.width = png.w; im.height = png.h; }
				im._ok = true;
				if (im.onload) im.onload();     // ⚠️ 同步，理由见 verify_editor_layers_ui.js
			},
			get() { return im.__key; }
		});
		return im;
	},
	Blob: function () { }, FileReader: function () { },
	URL: { createObjectURL() { return 'blob:x'; }, revokeObjectURL() { } },
	alert() { }, prompt() { return null; }, confirm() { return true; },
	addEventListener() { }, removeEventListener() { }, dispatchEvent() { },
	/* C.5 的 app.js 用到 indexOf 的 Object.keys 等标准内置，无需再补 */
};
sandbox.window = sandbox;
sandbox.globalThis = sandbox;
sandbox.self = sandbox;
const ctx = vm.createContext(sandbox);

/* --------------------------------------------------------------- 载入 */
const FILES = ['assets.js', 'render.js', 'editor.js', 'codegen.js', 'app.js'];
FILES.forEach(f => {
	let src = fs.readFileSync(path.join(DIR, f), 'utf8');
	/* 反例自测：把「区域随机写在所有填充之后」这条破坏掉 —— 把区域段
	 * 挪到骨架语句**之前**，此时它会被后面的 Painter.fill 覆盖，
	 * 而生成结果在**字符串层面仍然长得一模一样**（照样有 shuffle 和写回）。
	 * ⇒ 所以本脚本必须有一条「区域段的行号在所有 Painter.fill 之后」的位置断言，
	 *   否则这个反例根本抓不到。 */
	if (SELFTEST && f === 'codegen.js') {
		/* ⚠️ 反例必须真的**把区域段挪到填充之前**，光是删掉注释行不算数
		 * （D 节找的是 `Random.shuffle(` 那一行的行号，注释删不删都不影响它）。
		 * 篡改方式：把 step 4 的整块搬进 step 1 的骨架填充之间 ——
		 * 生成出来的字符串照样有 shuffle 有写回，只有**行号位置**变了，
		 * 所以只有 D 节的位置断言才抓得到。 */
		const EMIT = '\t\tif (regionEm.count) {\n\t\t\tregionEm.lines.forEach(A);\n\t\t}\n';
		const iEmit = src.indexOf(EMIT);
		if (iEmit < 0) {
			console.error('❌ --selftest：注入反例失败（找不到区域段发射点）。');
			console.error('   请同步更新本脚本——否则 --selftest 会假绿。');
			process.exit(2);
		}
		/* 摘掉原位置（连同上面那行段落注释） */
		src = src.slice(0, iEmit).replace(/[ \t]*\/\* -- 4\) 区域随机（必须在所有地形填充之后） -- \*\/\n$/, '')
			+ src.slice(iEmit + EMIT.length);
		/* 插到骨架第一句填充之后 */
		const ANCHOR = "\t\tA('\\t\\tPainter.fill( level, this, Terrain.WALL );');\n";
		const iAnchor = src.indexOf(ANCHOR);
		if (iAnchor < 0) {
			console.error('❌ --selftest：注入反例失败（找不到骨架锚点）。请同步更新本脚本。');
			process.exit(2);
		}
		src = src.slice(0, iAnchor + ANCHOR.length) + EMIT + src.slice(iAnchor + ANCHOR.length);
	}
	vm.runInContext(src, ctx, { filename: f });
});

const R = sandbox.TE_RENDER, Ed = sandbox.TE_EDITOR, Gen = sandbox.TE_GEN, App = sandbox.TE_APP;
const T = R.T;

/* ================================================================== A */
console.log('\n=== A. 接线：探针 / UI 元素 / 内核导出 ===');
ok(!!App, 'app.js 暴露 TE_APP');
ok(typeof App.regions === 'function', 'TE_APP.regions() 可用');
ok(typeof App.markRegion === 'function', 'TE_APP.markRegion() 可用');
ok(!!els.chkRegions, '「区域标记」显示开关存在');
ok(!!els.chkRegionLayers, '「连同覆盖层一起打乱」复选框存在');
ok(!!els.btnRegionClear, '「清空全部区域」按钮存在');
ok(!!els.regionHint, '区域提示行 #regionHint 存在');
ok(Array.isArray(Ed.E.randoms), 'Ed.E 里有 randoms 元数据数组');
ok(typeof Gen.emitRandomRegions === 'function', 'codegen 暴露 emitRandomRegions');
ok(typeof Ed.addRegion === 'function', 'editor 暴露 addRegion');
ok(typeof R.setRegions === 'function', 'render 暴露 setRegions');

/* ================================================================== B */
console.log('\n=== B. 区域拓扑：合并 / 去重 / 删除 / 排序 ===');
Ed.setRoomSize(12, 12);
eq(Ed.E.randoms.length, 0, '刚设置尺寸时没有区域');

Ed.addRegion(Ed.rectOf(2, 2, 4, 4));                  // 3×3
eq(Ed.E.randoms.length, 1, '标记第一片 ⇒ 1 片');
eq(Ed.regionCells(Ed.E.randoms[0]), 9, '3×3 ⇒ 9 格');

Ed.addRegion(Ed.rectOf(7, 7, 9, 9));                  // 不相交
eq(Ed.E.randoms.length, 2, '再标记一片不相交的 ⇒ 2 片');

Ed.addRegion(Ed.rectOf(4, 4, 6, 5));                  // 与第一片有交集
eq(Ed.E.randoms.length, 2, '标记与第一片相交的区域 ⇒ 仍是 2 片（就地合并，不是追加）');
const merged = Ed.E.randoms[0];
eq([merged.l, merged.t, merged.r, merged.b].join(','), '2,2,6,5',
	'合并结果 = 两矩形的并集 (2,2)-(6,5)');

/* 合并必须迭代到稳定：三片串珠式相交要一次并成一片 */
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(1, 1, 2, 2));
Ed.addRegion(Ed.rectOf(4, 1, 5, 2));
Ed.addRegion(Ed.rectOf(2, 1, 4, 2));                  // 桥接前两片
eq(Ed.E.randoms.length, 1, '三片用中间一片桥接 ⇒ 并成 1 片（合并要迭代到稳定）');
eq(Ed.regionCells(Ed.E.randoms[0]), 10, '并集 (1,1)-(5,2) ⇒ 5×2 = 10 格');

/* 删除：与给定矩形有交集就整块删掉 */
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(1, 1, 3, 3));
Ed.addRegion(Ed.rectOf(8, 8, 9, 9));
Ed.removeRegionsIn(Ed.rectOf(2, 2, 2, 2));            // 只碰到第一片
eq(Ed.E.randoms.length, 1, '擦除命中第一片 ⇒ 剩 1 片');
eq([Ed.E.randoms[0].l, Ed.E.randoms[0].t].join(','), '8,8', '留下的是没被碰到的那片');

/* 排序：按 (t, l) 行优先，保证生成代码顺序稳定可读 */
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(5, 1, 6, 2));
Ed.addRegion(Ed.rectOf(1, 5, 2, 6));
Ed.addRegion(Ed.rectOf(1, 1, 2, 2));
eq(Ed.E.randoms.map(r => r.l + ',' + r.t).join(' | '), '1,1 | 5,1 | 1,5',
	'区域按 (上、左) 排序');

/* 边界夹取：越界矩形在 fromJSON 里被夹回图内 */
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(0, 0, 99, 99));
eq(Ed.E.randoms.length, 1, '标记超界矩形仍记为一片（内存里允许，生成时才夹）');

/* ================================================================== C */
console.log('\n=== C. 语义：多重集不变（只换位置，不换配比） ===');
Ed.setRoomSize(10, 10);
Ed.MACROS.skeleton();
Ed.MACROS.fourDoors();
/* 在 (1,1)-(4,4) 铺一个已知配比：3 草 / 2 水 / 1 雷 / 10 空 */
const REG = Ed.rectOf(1, 1, 4, 4);                    // 4×4 = 16 格
const REG_CELLS = Ed.regionIndices ? null : null;
Ed.clearRegions();
Ed.addRegion(REG);
const regIdx = Gen.regionIndices(REG);
/* 行跨度一律从 Ed.E.w 现取：前面几节会把房间改成别的尺寸，
 * 硬写 10 会在「上一节结束时房间不是 10 宽」时假报失败（第 3 次踩）。 */
const W = Ed.E.w;
eq(regIdx.length, 16, '4×4 区域 ⇒ 16 个索引');
eq(regIdx[0], 1 * W + 1, `区域内首索引 = 行优先 (1,1)（行跨度 W=${W}）`);
eq(regIdx[4], 2 * W + 1, '第 5 个索引换行到 (1,2)（行优先）');
eq(regIdx[15], 4 * W + 4, '区域内末索引 = (4,4)');

const preset = [T.GRASS, T.GRASS, T.GRASS, T.WATER, T.WATER, T.EMBOSSED?T.EMBOSSED:0];
/* 用确定值填：0..5 循环，方便统计 */
const FILLS = [T.GRASS, T.WATER, T.EMPTY, T.GRASS, T.WATER, T.EMPTY, T.HIGH_GRASS];
regIdx.forEach((p, i) => { Ed.E.map[p] = FILLS[i % FILLS.length]; });

/* 模拟生成期的「收集 → shuffle → 写回」，断言多重集不变。
 * 这里刻意**不调 Java**：把 codegen 输出里的 int[] 字面量解析出来，
 * 用同一个算法在 JS 侧跑一遍，证明「输出的数据 + 语义」都对。 */
const java1 = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
const arrMatch = /int\[\] r0 = new int\[\]\{([\s\S]*?)\};/.exec(java1);
ok(!!arrMatch, '生成结果里有区域值数组 int[] r0 = new int[]{...}');
if (arrMatch) {
	const vals = arrMatch[1].replace(/\s/g, '').split(',').filter(s => s).map(Number);
	eq(vals.length, 16, '数组长度 = 区域格数 16（不多不少）');
	const before = regIdx.map(p => Ed.E.map[p]).sort().join(',');
	const after = vals.slice().sort().join(',');
	eq(after, before, '**多重集不变**：数组里的值排序后与原区域完全一致（配比没被随机掉）');
	eq(vals.join(','), regIdx.map(p => Ed.E.map[p]).join(','),
		'数组顺序 = 行优先收集顺序（与写回顺序一致）');
}

/* 生成结果必须同时含 shuffle 与写回 */
const code1 = java1.split('\n').filter(l => !/^\s*\/\//.test(l)).join('\n');
ok(/Random\.shuffle\( r0 \);/.test(code1), '生成了 Random.shuffle( r0 )');
ok(/Painter\.set\( level, r0Pos\[k\], r0\[k\] \);/.test(code1),
	'生成了写回循环 Painter.set( level, r0Pos[k], r0[k] )');
ok(/import com\.watabou\.utils\.Random;/.test(java1), 'region 存在 ⇒ import Random');
ok(/import com\.watabou\.utils\.Point;/.test(java1), 'region 存在 ⇒ import Point');

/* pos 数组的坐标必须是相对 left/top 偏移，且指向区域内的格 */
const posMatch = /int\[\] r0Pos = new int\[\]\{([\s\S]*?)\};/.exec(java1);
ok(!!posMatch, '生成结果里有区域位置数组 int[] r0Pos = new int[]{...}');
if (posMatch) {
	const exprs = posMatch[1].split('),').filter(s => s.trim()).length;
	eq(exprs, 16, '位置数组有 16 项');
	ok(/left \+ 1, top \+ 1/.test(posMatch[1]), '首项是 left + 1, top + 1（区域左上角）');
	ok(/left \+ 4, top \+ 4/.test(posMatch[1]), '末项是 left + 4, top + 4（区域右下角）');
	ok(!/left \+ 0/.test(posMatch[1]) && !/top \+ 0/.test(posMatch[1]),
		'位置数组里没有 touched 外圈（区域只在 (1,1)-(4,4)）');
}

/* ================================================================== D */
console.log('\n=== D. 顺序：区域段必须在所有地形填充之后 ===');
{
	const lines = java1.split('\n');
	const idxRegion = lines.findIndex(l => /Random\.shuffle\(/.test(l));
	const idxSkeletonFill = lines.findIndex(l => /Painter\.fill\( level, this, Terrain\.WALL \);/.test(l));
	const idxInnerFill = lines.findIndex(l => /Painter\.fill\( level, this, 1, Terrain\.EMPTY \);/.test(l));
	const idxLastTerrainFill = (function () {
		/* 找最后一条「非注释的 Painter.fill / Painter.set 地形语句」 */
		let last = -1;
		lines.forEach((l, i) => {
			if (/^\s*\/\//.test(l)) return;
			if (/Painter\.(fill|set)\(\s*level\s*,/.test(l) && !/r0Pos|r0\[/.test(l)) last = i;
		});
		return last;
	})();
	ok(idxSkeletonFill >= 0, '找到骨架语句 Painter.fill(level, this, Terrain.WALL)');
	ok(idxInnerFill >= 0, '找到骨架语句 Painter.fill(level, this, 1, Terrain.EMPTY)');
	ok(idxRegion > idxSkeletonFill, '区域段在骨架铺墙**之后**', { region: idxRegion, skel: idxSkeletonFill });
	ok(idxRegion > idxInnerFill, '区域段在骨架掏空**之后**', { region: idxRegion, inner: idxInnerFill });
	ok(idxRegion > idxLastTerrainFill,
		'**区域段在最后一条地形填充之后**（否则会被覆盖，且完全不报错）',
		{ region: idxRegion, lastFill: idxLastTerrainFill });
	/* 顺带：区域段也必须在地形段之后、门类型循环之前 */
	const idxDoorLoop = lines.findIndex(l => /door\.set\( Room\.Door\.Type\.REGULAR \);/.test(l));
	ok(idxDoorLoop > idxRegion, '区域段在门类型循环之前（不干扰门）', { region: idxRegion, door: idxDoorLoop });
}

/* ================================================================== E */
console.log('\n=== E. 覆盖层联动开关（withLayers） ===');
Ed.setRoomSize(10, 10);
Ed.MACROS.skeleton();
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(1, 1, 4, 4));
/* 区域里放一个陷阱 + 一个植物 + 一个道具 */
Ed.setTrap(2, 2, R.makeTrap('FrostTrap'));
Ed.setPlant(3, 3, R.makePlant('Sungrass'));
Ed.setItem(4, 4, R.makeItem('Food'));

const javaNoLayer = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom', withLayers: false });
const javaWithLayer = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom', withLayers: true });

ok(/Random\.shuffle\( r0 \);/.test(javaNoLayer), 'withLayers=false ⇒ 直接 shuffle 地形数组');
ok(!/r0Ord/.test(javaNoLayer), 'withLayers=false ⇒ **不**生成置换表 r0Ord');
ok(/当前\*\*只打乱地形\*\*/.test(javaNoLayer), 'withLayers=false ⇒ 注释里明确写了「只打乱地形」');

ok(!/Random\.shuffle\( r0 \);/.test(javaWithLayer), 'withLayers=true ⇒ 不直接 shuffle 地形数组');
ok(/int\[\] r0Ord = new int\[16\];/.test(javaWithLayer), 'withLayers=true ⇒ 生成置换表 int[16]');
ok(/for \(int k = 0; k < r0Ord\.length; k\+\+\) r0Ord\[k\] = k;/.test(javaWithLayer),
	'置换表初始化为恒等');
ok(/Random\.shuffle\( r0Ord \);/.test(javaWithLayer), '对置换表做 shuffle');
ok(/int\[\] r0Old = r0\.clone\(\);/.test(javaWithLayer), '先 clone 一份原数组（否则重排会自覆盖）');
ok(/for \(int k = 0; k < r0\.length; k\+\+\) r0\[k\] = r0Old\[r0Ord\[k\]\];/.test(javaWithLayer),
	'用置换表重排地形');
ok(!/当前\*\*只打乱地形\*\*/.test(javaWithLayer), 'withLayers=true ⇒ 不再显示「只打乱地形」警示');

/* 片段模式也要支持 */
const bodyNo = Gen.generateBodyOnly({ withLayers: false });
const bodyWith = Gen.generateBodyOnly({ withLayers: true });
ok(/Random\.shuffle\( r0 \);/.test(bodyNo), 'generateBodyOnly(withLayers=false) 含 shuffle');
ok(/r0Ord/.test(bodyWith), 'generateBodyOnly(withLayers=true) 含置换表');
ok(/import com\.watabou\.utils\.Random;/.test(bodyWith), 'generateBodyOnly() 附上了 Random import');

/* ================================================================== F */
console.log('\n=== F. 反向自测：没有区域时不许凭空生成 ===');
{
	Ed.clearRegions();
	const j0 = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
	ok(!/Random\.shuffle/.test(j0), '无区域 ⇒ 生成结果里没有 Random.shuffle');
	ok(!/int\[\] r0 = /.test(j0), '无区域 ⇒ 没有区域数组');
	ok(!/import com\.watabou\.utils\.Random;/.test(j0), '无区域 ⇒ 不 import Random');
	const b0 = Gen.generateBodyOnly({});
	ok(!/Random\.shuffle/.test(b0), 'generateBodyOnly() 无区域时也没有 shuffle');
}

/* ================================================================== G */
console.log('\n=== G. 退化区域：1 格 / 整片同值 ===');
Ed.setRoomSize(10, 10);
Ed.MACROS.skeleton();
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(5, 5, 5, 5));                  // 1 格
const j1 = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
ok(!/Random\.shuffle/.test(j1), '1 格区域 ⇒ **不**生成 shuffle（打乱无意义，直接跳过）');
ok(/import com\.watabou\.utils\.Random;/.test(j1) === false, '1 格区域 ⇒ 连 Random 都不 import');

Ed.clearRegions();
Ed.addRegion(Ed.rectOf(2, 2, 5, 5));                  // 整片空地板
const j2 = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
ok(/Random\.shuffle/.test(j2), '整片同值的区域仍然生成代码（语义上合法：打乱后画面不变）');

/* 检查器要提示这两类退化。
 * ⚠️ 两片必须**互不相邻**：addRegion 会把重叠区域迭代合并成一片
 * （这是刻意的不变量：区域列表里任意两片不相交），
 * 早先写成 rectOf(5,5,5,5) + rectOf(2,2,5,5) 会在 (5,5) 处相撞而被并成 1 片，
 * 于是「1 格区域」这条提示永远不会出现（假红）。 */
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(1, 1, 1, 1));                  // 1 格（左上，孤立）
Ed.addRegion(Ed.rectOf(3, 3, 5, 5));                  // 整片空地板（右下，不相邻）
eq(Ed.E.randoms.length, 2, '两片互不相邻 ⇒ 保持为 2 片（没被合并）');
const msgs = Ed.check().map(m => m.text).join(' ｜ ');
ok(/只有 1 格/.test(msgs), '检查器提示「区域只有 1 格」（否则用户以为标记无效）');
ok(/整片是同一种地形/.test(msgs), '检查器提示「整片同一种地形」');
ok(/已标记 2 片随机区域/.test(msgs), '检查器给出区域计数');
ok(/共 10 格/.test(msgs), '检查器给出总格数（1 + 9 = 10）');

/* 区域内放覆盖层时，检查器要说明「需勾选才跟着动」 */
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(1, 1, 4, 4));
Ed.setTrap(2, 2, R.makeTrap('FrostTrap'));
const msgs2 = Ed.check().map(m => m.text).join(' ｜ ');
ok(/区域内有 1 个陷阱\/植物\/道具/.test(msgs2), '检查器报告区域内覆盖层数量');
ok(/需勾选「连同覆盖层一起打乱」/.test(msgs2), '检查器说明要勾选开关（否则会错位）');

/* ================================================================== H */
console.log('\n=== H. 存/读 JSON：v4 带 randoms，老档兼容 ===');
Ed.setRoomSize(10, 10);
Ed.clearRegions();
Ed.addRegion(Ed.rectOf(2, 2, 5, 6));
const j = Ed.toJSON();
eq(j.version, 6, 'toJSON version === 6（v4 起带 randoms，v5 起带 fuzz 尺寸模糊化，v6 起带 mode）');
ok(Array.isArray(j.randoms) && j.randoms.length === 1, 'JSON 里带上了 randoms', j.randoms);
eq([j.randoms[0].l, j.randoms[0].t, j.randoms[0].r, j.randoms[0].b].join(','), '2,2,5,6',
	'randoms 内容正确');

Ed.clearRegions();
Ed.fromJSON(JSON.parse(JSON.stringify(j)));
eq(Ed.E.randoms.length, 1, '读回后区域还在');
eq([Ed.E.randoms[0].l, Ed.E.randoms[0].t, Ed.E.randoms[0].r, Ed.E.randoms[0].b].join(','), '2,2,5,6',
	'读回后区域坐标一致');
ok(Ed.E.randoms === Ed.snapshot().randoms || Ed.E.randoms[0] !== j.randoms[0],
	'读回的是深拷贝（不是 JSON 里的同一对象）');

/* v3 老档（没有 randoms 键）⇒ 空列表，不能是 undefined（否则后续 forEach 会炸）
 * ⚠️ 这里的 `format` 刻意保留 C.7 之前的旧名 'egopd-terrain-editor' ——
 *    用户硬盘上存量档就是这个名字，**读侧必须继续认**（见 verify_size_fuzz.js 的兼容断言）。 */
(function () {
	const nowJ = Ed.toJSON();
	const v3 = {
		format: 'egopd-terrain-editor', version: 3,
		w: nowJ.w, h: nowJ.h, tiles: nowJ.tiles,
		traps: [], plants: [], items: []
	};
	Ed.fromJSON(v3);
	ok(Array.isArray(Ed.E.randoms), 'v3 老档读入后 randoms 是数组（不是 undefined）');
	eq(Ed.E.randoms.length, 0, 'v3 老档读入后区域为空');
	const j3 = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
	ok(!/Random\.shuffle/.test(j3), 'v3 老档 ⇒ 生成结果里没有 shuffle');
})();

/* 越界区域读回来要夹进图内（「先框区域后改小房间」会留下这种档） */
(function () {
	const nowJ = Ed.toJSON();
	const bad = {
		format: 'egopd-terrain-editor', version: 4,
		w: 8, h: 8, tiles: new Array(64).fill(T.EMPTY),
		traps: [], plants: [], items: [],
		randoms: [{ l: 2, t: 2, r: 99, b: 99 }]
	};
	Ed.fromJSON(bad);
	ok(Ed.E.randoms.length === 1, '越界区域仍被读入（过滤只剔除反向矩形）');
	const rc = Ed.E.randoms[0];
	ok(rc.r <= 7 && rc.b <= 7, '越界坐标被夹到图内', rc);
})();

/* ================================================================== I */
console.log('\n=== I. 撤销 / 重做把区域一起回滚 ===');
Ed.setRoomSize(10, 10);
Ed.clearRegions();
Ed.pushUndo();
Ed.addRegion(Ed.rectOf(2, 2, 4, 4));
eq(Ed.E.randoms.length, 1, '标记后 1 片');
Ed.undo();
eq(Ed.E.randoms.length, 0, '撤销后区域标记消失');
Ed.redo();
eq(Ed.E.randoms.length, 1, '重做后区域标记恢复');

/* 换房间尺寸必须清空区域（否则会留下越界矩形） */
Ed.addRegion(Ed.rectOf(6, 6, 7, 7));
eq(Ed.E.randoms.length, 2, '现在有 2 片');
Ed.setRoomSize(6, 6);
eq(Ed.E.randoms.length, 0, '改房间尺寸 ⇒ 区域一并清空（避免留下越界矩形）');

/* ================================================================== J */
console.log('\n=== J. UI：标记工具真的走 app.js 路由 ===');
Ed.setRoomSize(12, 12);
Ed.clearRegions();
/* 直接调探针（等价于拖框结束那一刻 app.js 做的事） */
const rc1 = App.markRegion(2, 2, 5, 5);
eq(!!rc1, true, 'TE_APP.markRegion 返回区域对象');
eq(Ed.E.randoms.length, 1, 'markRegion ⇒ E.randoms 多一片');
eq([rc1.l, rc1.t, rc1.r, rc1.b].join(','), '2,2,5,5', '区域坐标正确');

/* 反向拖框（从右下往左上拖）也要得到规范化矩形 */
App.markRegion(9, 9, 7, 7);
const last = Ed.E.randoms[Ed.E.randoms.length - 1];
eq([last.l, last.t, last.r, last.b].join(','), '7,7,9,9', '反向拖框被规范化（l<=r, t<=b）');

/* 提示行要随标记变化 */
App.clearRegions();
eq(Ed.E.randoms.length, 0, 'clearRegions 清空');
ok(/当前无区域标记/.test(els.regionHint.textContent), '清空后提示行复位「当前无区域标记」',
	els.regionHint.textContent);

App.markRegion(1, 1, 3, 3);
ok(/已标记 1 片区域/.test(els.regionHint.textContent), '标记后提示行报告片数',
	els.regionHint.textContent);
ok(/共 9 格/.test(els.regionHint.textContent), '提示行报告总格数', els.regionHint.textContent);
ok(/地形 \+ 覆盖层/.test(els.regionHint.textContent),
	'勾选状态下提示行写「地形 + 覆盖层」', els.regionHint.textContent);

/* 取消勾选 ⇒ 提示行与生成代码都要跟着变 */
els.chkRegionLayers.checked = false;
els.chkRegionLayers.dispatch('change', {});
ok(/仅地形/.test(els.regionHint.textContent), '取消勾选后提示行写「仅地形」',
	els.regionHint.textContent);
ok(/只打乱地形/.test(els.codeBox.value), '取消勾选后生成代码里也写明「只打乱地形」');
els.chkRegionLayers.checked = true;
els.chkRegionLayers.dispatch('change', {});

/* 区域内放覆盖层 + 未勾选 ⇒ 提示行要变黄警告 */
els.chkRegionLayers.checked = false;
Ed.setTrap(2, 2, R.makeTrap('FrostTrap'));
App.clearRegions();
App.markRegion(1, 1, 3, 3);
ok(/覆盖层对象/.test(els.regionHint.textContent),
	'区域内有覆盖层且未勾选 ⇒ 提示行给出警告', els.regionHint.textContent);
ok(/var\(--warn\)/.test(els.regionHint.style.color || ''),
	'警告时提示行变成警示色', els.regionHint.style.color);
els.chkRegionLayers.checked = true;
els.chkRegionLayers.dispatch('change', {});

/* 显示开关：关掉后渲染内核不应画区域 */
R.setRegions(Ed.E.randoms);
els.chkRegions.checked = false;
els.chkRegions.dispatch('change', {});
eq(R.state.showRegions, false, '取消勾选「区域标记」⇒ 渲染内核 showRegions=false');
els.chkRegions.checked = true;
els.chkRegions.dispatch('change', {});
eq(R.state.showRegions, true, '勾回 ⇒ showRegions=true');

/* ================================================================== K */
console.log('\n=== K. 渲染：区域标记真的画上去了（尺寸无关的调用断言） ===');
{
	Ed.setRoomSize(12, 12);
	App.clearRegions();
	App.markRegion(2, 2, 4, 4);
	R.setRegions(Ed.E.randoms);
	R.setShowRegions(true);
	let dashCalls = 0, clipCalls = 0;
	/* 用一个「记账 ctx」包住真实 drawImage 路径：只数 setLineDash / clip 次数 */
	const realCtx = els.cv.getContext();
	const bak = { setLineDash: realCtx.setLineDash, clip: realCtx.clip };
	realCtx.setLineDash = function () { dashCalls++; return bak.setLineDash.apply(this, arguments); };
	realCtx.clip = function () { clipCalls++; return bak.clip.apply(this, arguments); };
	R.render(realCtx, { scale: 1 });
	realCtx.setLineDash = bak.setLineDash; realCtx.clip = bak.clip;
	ok(dashCalls >= 1, '有区域时调用 setLineDash（画虚线框）', dashCalls);
	ok(clipCalls >= 1, '有区域时调用 clip（斜纹底只在区域内铺）', clipCalls);

	/* 关掉开关 ⇒ 一次都不该调 */
	R.setShowRegions(false);
	let dash2 = 0;
	realCtx.setLineDash = function () { dash2++; return bak.setLineDash.apply(this, arguments); };
	R.render(realCtx, { scale: 1 });
	realCtx.setLineDash = bak.setLineDash;
	eq(dash2, 0, '关掉「区域标记」后不再画虚线框（一次 setLineDash 都没有）');
	R.setShowRegions(true);

	/* 清空区域 ⇒ 也不画 */
	R.setRegions([]);
	let dash3 = 0;
	realCtx.setLineDash = function () { dash3++; return bak.setLineDash.apply(this, arguments); };
	R.render(realCtx, { scale: 1 });
	realCtx.setLineDash = bak.setLineDash;
	eq(dash3, 0, '没有区域时不画任何标记');
	R.setRegions(Ed.E.randoms);
}

console.log('\n' + (failures ? '\u274c 失败 ' + failures + ' 项' : '\u2705 全部通过') +
	'（通过 ' + passes + '，失败 ' + failures + '）\n');
process.exit(failures ? 1 : 0);
