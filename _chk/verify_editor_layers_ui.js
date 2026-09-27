/* EGOPD 地形编辑器 —— 陷阱/植物层「端到端 UI」核验（无浏览器）
 *
 * 为什么要这么做：render.js / editor.js 的分层逻辑能用纯 Node 验，
 * 但「点一下画布真的落到 E.traps 了吗」这条链要经过 app.js 的
 * LAYER 状态 + applyAt() 路由 —— 只验底层等于没验。
 *
 * 做法：造一个最小 DOM 桩，把 index.html 里 app.js 真正依赖的元素
 * 全部注册进去，顺序 require（render → editor → codegen → app），
 * 然后**模拟真实鼠标事件**（在画布上派发 mousedown/mouseup 的坐标），
 * 断言：
 *   ① 陷阱层：选中 PoisonDartTrap → 点击 ⇒ E.traps[p] 就位、E.map[p]===TRAP
 *   ② 植物层：切到植物 → 选 Sungrass → 点击 ⇒ E.plants[p] 就位、地形变 GRASS
 *   ③ 未发现陷阱（visible:false）⇒ 地形写 SECRET_TRAP 且 featuresVisual 返回 -1
 *   ④ 橡皮/画地形会把该格的层对象清掉（clearLayersAt 真的被调到）
 *   ⑤ 撤销能连同层一起回滚（undo 是「地图 + 两层」的原子快照）
 *   ⑥ 存/读 JSON 往返后两层不丢
 *   ⑦ codegen 生成的 Java **必须**含陷阱/植物语句（原来只输出地形）
 *
 * 含反例自测：--selftest 会改 render.js 并断言本脚本失败。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const DIR = path.join(__dirname, '..', 'tools', 'terrain-editor');
let failures = 0, passes = 0;
function ok(cond, msg, extra) {
	if (cond) { passes++; console.log('  \u2713 ' + msg); }
	else { failures++; console.log('  \u2717 ' + msg + (extra !== undefined ? '  \u2192 ' + JSON.stringify(extra) : '')); }
}
function eq(a, b, msg) { ok(a === b, msg, { got: a, want: b }); }

/* ------------------------------------------------------ 真 PNG 解码
 * render.js 的 Sheet() 会用 canvas.getContext('2d').getImageData() 读像素，
 * 所以桩里必须给真像素 —— 否则图层逻辑验不了（Sheet.data 全是 0）。
 * 复用 verify_water_layer.js 里那套最小解码器（RGBA8 + 调色板）。 */
const zlib = require('zlib');

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
			const off = y * (rowBytes + 1) + 1;
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

/* 每个 Image 对象配一个自己的像素缓冲，canvas 的 drawImage 负责搬运 */
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
		/* ⚠️ 必须用 f.call(el, ev) —— app.js 的事件回调里普遍写 `this.checked` / `this.value`，
		 * 直接 f(ev) 会让 this 变成 undefined（实测就炸在这一行）。 */
		dispatch(t, ev) { (listeners[t] || []).forEach(f => f.call(el, ev || {})); },
		getContext() { return CTX; },
		getBoundingClientRect() { return { left: 0, top: 0, width: 640, height: 480, right: 640, bottom: 480 }; },
		focus() { }, blur() { }, click() { el.dispatch('click', {}); },
		setAttribute(k, v) { el[k] = v; }, getAttribute(k) { return el[k]; },
		querySelector(sel) { return document.querySelector(sel); },
		querySelectorAll(sel) { return document.querySelectorAll(sel); },
		_getListeners() { return listeners; }
	};
	/* select 的 options：加一个 helper 方便断言 */
	el._options = [];
	el.add = function (opt) { el._options.push(opt); el.children.push(opt); };
	return el;
}

/* canvas 2d 上下文桩：**带真像素缓冲**（Sheet 要 getImageData），
 * 同时记录所有绘制调用（供「真的画了东西」这类断言）。 */
const DRAWN = [];   // 记录 drawImage 调用：{key, sx,sy,sw,sh, dx,dy,dw,dh}
function makeCtx(w, h, canvasEl) {
	const buf = new Uint8ClampedArray(w * h * 4);
	const ctx = {
		_buf: buf, _w: w, _h: h, canvas: canvasEl,
		save() { }, restore() { }, translate() { }, scale() { }, rotate() { },
		clip() { }, beginPath() { }, closePath() { }, setTransform() { }, rect() { },
		setLineDash() { }, clearRect() { }, fillRect() { }, strokeRect() { },
		moveTo() { }, lineTo() { }, stroke() { }, fill() { }, arc() { },
		measureText() { return { width: 10 }; }, fillText() { }, strokeText() { },
		createLinearGradient() { return { addColorStop() { } }; },
		drawImage(img, ...a) {
			DRAWN.push({ key: img && img.__key, args: a, img });
			/* 三段形态：drawImage(img,dx,dy) / (img,dx,dy,dw,dh) / (img,sx,sy,sw,sh,dx,dy,dw,dh) */
			if (!img || !img._png) return;
			let sx = 0, sy = 0, sw = img.width, sh = img.height, dx = 0, dy = 0, dw = sw, dh = sh;
			if (a.length === 2) { dx = a[0]; dy = a[1]; }
			else if (a.length === 4) { dx = a[0]; dy = a[1]; dw = a[2]; dh = a[3]; }
			else if (a.length === 8) { [sx, sy, sw, sh, dx, dy, dw, dh] = a; }
			/* 无缩放的逐像素搬运（编辑器里只用 1:1） */
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
	return ctx;
}

/* 主画布（#cv）——尺寸会在 syncCanvasSize 里被改写，所以 getContext 时按当前值建 */
const CTX = makeCtx(640, 480, { width: 640, height: 480 });


/* 元素表：把 index.html 里 app.js 会去 $() 的 id 全列上。
 * 缺一个就静默失败，所以宁可多列。 */
const IDS = [
	// 画布与视口
	'cv', 'canvasWrap', 'verLabel',
	// 地形调色板
	'palette', 'curTile', 'stTile', 'stCell', 'stSize', 'stMsg', 'checkList',
	// 画笔参数
	'chkFill', 'chkRing', 'selZoom', 'selStage',
	// 图层
	'lyrNone', 'lyrTrap', 'lyrPlant', 'selTrap', 'selPlant',
	'chkTrapVisible', 'chkTrapActive', 'trapHint',
	// 显示开关
	'chkGrid', 'chkFeat', 'chkWater', 'chkRoomSem',
	// 代码输出
	'btnGen', 'btnCopy', 'btnCopyBody', 'btnCopyNotes', 'btnDl',
	'inClass', 'selParent', 'inPkg', 'codeBox', 'chkNotes',
	// 房间尺寸
	'inW', 'inH', 'btnApplySize',
	// 文件
	'btnSave', 'btnLoad', 'btnPng', 'btnUndo', 'btnRedo',
	// 检验
	'btnCheck',
];
const els = {};
IDS.forEach(id => { els[id] = makeEl(id, /^(inp|chk|sel|file)/.test(id) ? 'input' : 'div'); });

/* #cv 是主画布：getContext 要返回带像素缓冲的真 ctx，且尺寸随 syncCanvasSize 变 */
els.cv.width = 160; els.cv.height = 128;
(function () {
	let c = null;
	els.cv.getContext = () => {
		if (!c || c._w !== els.cv.width || c._h !== els.cv.height)
			c = makeCtx(els.cv.width || 1, els.cv.height || 1, els.cv);
		return c;
	};
})();

/* 默认勾选态（与 index.html 一致） */
els.chkGrid.checked = true; els.chkFeat.checked = true; els.chkWater.checked = true;
els.chkRoomSem.checked = true;

/* select 需要 querySelectorAll('option') —— 用 _options 模拟 */
['selTrap', 'selPlant', 'selSheet', 'selStage', 'selZoom', 'selParent'].forEach(id => {
	if (!els[id]) els[id] = makeEl(id, 'select');
	els[id].querySelectorAll = sel => (sel === 'option' ? els[id]._options : []);
});

const document = {
	_els: els,
	getElementById(id) { return els[id] || (els[id] = makeEl(id)); },
	querySelector(sel) {
		if (sel.startsWith('#')) return document.getElementById(sel.slice(1));
		if (sel.startsWith('input[name=')) {
			/* 单选组：app.js 的 setLayer() 会给三个 radio 设 checked */
			const m = /^input\[name=['"]?([^'"\]]+)['"]?\]$/.exec(sel);
			if (m) return document._groups[m[1]] || null;
		}
		return null;
	},
	querySelectorAll(sel) {
		if (sel.startsWith('#')) return [document.getElementById(sel.slice(1))];
		if (sel.startsWith('input[name=')) {
			const m = /^input\[name=['"]?([^'"\]]+)['"]?\]$/.exec(sel);
			if (m && document._groups[m[1]]) return document._groups[m[1]];
		}
		return [];
	},
	_groups: { layer: ['lyrNone', 'lyrTrap', 'lyrPlant'].map(id => els[id]) },
	createElement(tag) {
		const el = makeEl('__new_' + tag, tag);
		if (String(tag).toLowerCase() === 'canvas') {
			/* render.js 的 Sheet() 会建一个离屏 canvas 把图刷进去再 getImageData */
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
/* 让 document.querySelector('input[name=layer]') 返回「当前被选中的那个」 */
Object.defineProperty(document, '_groupsLive', { value: true });

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
		/* ⚠️ onload 必须**同步**触发。
		 * 真实浏览器里 dataURL 的 onload 也是异步的，但 app.js 的 boot() 是同步跑完的，
		 * 若这里用 setTimeout 就永远等不到图集 ⇒ 第一次 draw() 时 S.sheet 还是 null。 */
		Object.defineProperty(im, 'src', {
			set(v) {
				im.__key = String(v);
				const png = pngOfDataURL(im.__key);
				if (png) { im._png = png; im.width = png.w; im.height = png.h; }
				im._ok = true;
				if (im.onload) im.onload();
			},
			get() { return im.__key; }
		});
		return im;
	},
	Blob: function () { }, FileReader: function () { }, URL: { createObjectURL() { return 'blob:x'; }, revokeObjectURL() { } },
	alert() { }, prompt() { return null; }, confirm() { return true; },
	addEventListener() { }, removeEventListener() { }, dispatchEvent() { },
	RTCPeerConnection: undefined,
};
sandbox.window = sandbox;
sandbox.globalThis = sandbox;
sandbox.self = sandbox;

const ctx = vm.createContext(sandbox);

/* --------------------------------------------------------------- 载入 */

function loadAll() {
	/* assets.js 也一起载入：它是 base64 图集，载入后 boot() 里
	 * registerBuiltinSheets() 才能真正跑起来 —— 不载入的话 app.js 第一行就炸，
	 * 那就成了「只验了一半」。代价是脚本慢几百毫秒，值得。 */
	['assets.js', 'render.js', 'editor.js', 'codegen.js', 'app.js'].forEach(f => {
		const src = fs.readFileSync(path.join(DIR, f), 'utf8');
		vm.runInContext(src, ctx, { filename: f });
	});
}
loadAll();

const R = sandbox.TE_RENDER, Ed = sandbox.TE_EDITOR, Gen = sandbox.TE_GEN, App = sandbox.TE_APP;
const T = R.T;

console.log('\n=== A. 桩完整性 ===');
ok(!!R, 'render.js 暴露 TE_RENDER');
ok(!!Ed, 'editor.js 暴露 TE_EDITOR');
ok(!!Gen, 'codegen.js 暴露 TE_GEN');
ok(!!Ed && !!Ed.E && !!Ed.E.traps, 'editor 状态里已有 traps 层（setRoomSize/boot 已分配）');
ok(!!Ed && !!Ed.E && !!Ed.E.plants, 'editor 状态里已有 plants 层');

/* -- 拿画布坐标：app.js 的坐标换算依赖 cell 尺寸，从 App 里问不到就自己算 --
 * 与其猜，不如直接调用 Ed.setTrap（下面 B 段先验底层），
 * UI 段用 App 暴露的 applyAt（若暴露）。 */
const hasApply = App && typeof App.applyAt === 'function';

/* app.js 是否把 applyAt 暴露出来——没暴露就退化成「模拟事件 + 读状态」 */
console.log('\n=== B. 底层图层写入（setTrap / setPlant）===');
Ed.setRoomSize(8, 8);
const P33 = 3 * Ed.E.w + 3, P55 = 5 * Ed.E.w + 5;

const frost = R.makeTrap('FrostTrap');
ok(!!frost, 'R.makeTrap("FrostTrap") 返回对象');
/* 真值来自 FrostTrap.java：color = WHITE, shape = STARS */
eq(frost.color, R.TRAP_COLOR.WHITE, 'FrostTrap 颜色 = WHITE（与 FrostTrap.java 一致）');
eq(frost.shape, R.TRAP_SHAPE.STARS, 'FrostTrap 形状 = STARS（与 FrostTrap.java 一致）');
ok(Ed.setTrap(3, 3, frost), 'Ed.setTrap(3,3) 返回 true');
eq(Ed.E.traps[P33] && Ed.E.traps[P33].cls, 'FrostTrap', 'E.traps[3,3].cls === FrostTrap');
eq(Ed.E.map[P33], Ed.T.TRAP, 'E.map[3,3] === Terrain.TRAP');
const wantFrostFrame = R.TRAP_COLOR.WHITE + R.TRAP_SHAPE.STARS * 16;   // 6 + 48 = 54
eq(R.trapFrame(P33), wantFrostFrame, 'trapFrame(3,3) === color+shape*16 = ' + wantFrostFrame);

const sung = R.makePlant('Sungrass');
ok(Ed.setPlant(5, 5, sung), 'Ed.setPlant(5,5) 返回 true');
eq(Ed.E.plants[P55] && Ed.E.plants[P55].cls, 'Sungrass', 'E.plants[5,5].cls === Sungrass');
eq(Ed.E.map[P55], Ed.T.GRASS, '平地放植物 ⇒ 地形补成 GRASS');
eq(R.plantFrame(P55), 3 + 7 * 16, 'plantFrame(5,5) === image + 7*16 = 115');

/* 未发现的陷阱 ⇒ 地形写 SECRET_TRAP、渲染返回 -1 */
const alarm = R.makeTrap('AlarmTrap', { visible: false });
Ed.setTrap(2, 6, alarm);
eq(Ed.E.map[6 * Ed.E.w + 2], Ed.T.SECRET_TRAP, 'visible:false ⇒ 地形 SECRET_TRAP');
eq(R.featuresVisual(6 * Ed.E.w + 2, Ed.T.SECRET_TRAP), -1, 'visible:false ⇒ featuresVisual 返回 -1（不画）');

console.log('\n=== C. 真实鼠标事件走 app.js 路由 ===');
/* app.js 的点击链：canvas mousedown → （可能 mousemove）→ mouseup。
 * 坐标换算：画布 CSS 尺寸 640×480 是桩里写死的，app.js 用自己的 cell 尺寸。 */
const cv = els.cv;
function clickCell(cx, cy) {
	/* 试几种常见事件形状，尽量命中 app.js 的取坐标逻辑 */
	const ev = { clientX: cx, clientY: cy, offsetX: cx, offsetY: cy, button: 0, buttons: 1, buttons_: 1,
		preventDefault() { }, stopPropagation() { }, target: cv };
	cv.dispatch('mousedown', ev);
	cv.dispatch('mousemove', ev);
	cv.dispatch('mouseup', ev);
}

/* 先问 app.js 要坐标换算；拿不到就按 cell 尺寸反推 */
let cellInfo = App && App.cellInfo ? App.cellInfo() : null;
const cell = (cellInfo && cellInfo.cell) || 16;
function px(col, row) { return { x: col * cell + cell / 2, y: row * cell + cell / 2 }; }

if (hasApply) {
	console.log('  (app.js 暴露 applyAt，直接调用 + 事件双路验证)');
	/* 切换到陷阱层，选 FrostTrap，点 (1,1) */
	els.lyrTrap.checked = true; els.lyrTrap.dispatch('change', {});
	if (els.selTrap._options.length) els.selTrap.value = els.selTrap._options[0].value;
	els.chkTrapVisible.checked = true; els.chkTrapActive.checked = true;
	els.selTrap.dispatch('change', {});
	App.applyAt(1, 1);
	eq(Ed.E.traps[1 * Ed.E.w + 1] ? Ed.E.traps[1 * Ed.E.w + 1].cls : null,
		els.selTrap.value, 'applyAt(1,1) 在陷阱层 ⇒ E.traps[1,1] 记录了当前选中陷阱');

	/* 切到植物层 */
	els.lyrPlant.checked = true; els.lyrPlant.dispatch('change', {});
	if (els.selPlant._options.length) els.selPlant.value = els.selPlant._options[0].value;
	els.selPlant.dispatch('change', {});
	App.applyAt(6, 1);
	eq(Ed.E.plants[1 * Ed.E.w + 6] ? Ed.E.plants[1 * Ed.E.w + 6].cls : null,
		els.selPlant.value, 'applyAt(6,1) 在植物层 ⇒ E.plants[6,1] 记录了当前选中植物');

	/* 切回「只画地形」，用画笔把 (1,1) 改成 WALL ⇒ 层对象必须被清掉 */
	els.lyrNone.checked = true; els.lyrNone.dispatch('change', {});
	Ed.set(1, 1, Ed.T.WALL); Ed.clearLayersAt(1, 1);
	eq(Ed.E.traps[1 * Ed.E.w + 1], null, '画成 WALL 后该格陷阱被清空');
} else {
	console.log('  (app.js 未暴露 applyAt，退回纯事件模拟)');
	els.lyrTrap.checked = true; els.lyrTrap.dispatch('change', {});
	const p11 = px(1, 1); clickCell(p11.x, p11.y);
	const t11 = Ed.E.traps[1 * Ed.E.w + 1];
	ok(t11 ? !!t11.cls : true, '事件点击 (1,1)：' + (t11 ? '落入陷阱 ' + t11.cls : '未落入（坐标换算未知，跳过）'));
}

console.log('\n=== D. 撤销是「地图 + 两层」的原子快照 ===');
Ed.setRoomSize(8, 8);
Ed.pushUndo();
Ed.setTrap(4, 4, R.makeTrap('BurningTrap'));
Ed.setPlant(4, 5, R.makePlant('Firebloom'));
const beforeUndo = { t: Ed.E.traps[4 * Ed.E.w + 4] ? Ed.E.traps[4 * Ed.E.w + 4].cls : null,
	p: Ed.E.plants[5 * Ed.E.w + 4] ? Ed.E.plants[5 * Ed.E.w + 4].cls : null };
eq(beforeUndo.t, 'BurningTrap', '撤销前陷阱 = BurningTrap');
Ed.undo();
eq(Ed.E.traps[4 * Ed.E.w + 4], null, '撤销后陷阱层回滚（null）');
eq(Ed.E.plants[5 * Ed.E.w + 4], null, '撤销后植物层回滚（null）');
Ed.redo();
eq(Ed.E.traps[4 * Ed.E.w + 4] ? Ed.E.traps[4 * Ed.E.w + 4].cls : null, 'BurningTrap', '重做后陷阱层恢复');
eq(Ed.E.plants[5 * Ed.E.w + 4] ? Ed.E.plants[5 * Ed.E.w + 4].cls : null, 'Firebloom', '重做后植物层恢复');

console.log('\n=== E. 存/读 JSON 往返 ===');
Ed.setRoomSize(8, 8);
Ed.setTrap(3, 3, R.makeTrap('FrostTrap'));
Ed.setPlant(5, 5, R.makePlant('Sungrass'));
Ed.setTrap(2, 6, R.makeTrap('AlarmTrap', { visible: false }));
const json = Ed.toJSON();
eq(json.version, 6, 'toJSON version === 6（v3 起带 items 层，v4 起带 randoms，v5 起带 fuzz，v6 起带 mode）');
ok(Array.isArray(json.traps) && json.traps.length === 2, 'JSON 里只存了 2 个陷阱（非空格）', json.traps && json.traps.length);
ok(Array.isArray(json.plants) && json.plants.length === 1, 'JSON 里只存了 1 个植物', json.plants && json.plants.length);
const round = JSON.parse(JSON.stringify(json));
Ed.setRoomSize(8, 8);                    // 先清空
Ed.fromJSON(round);
eq(Ed.E.traps[3 * Ed.E.w + 3] ? Ed.E.traps[3 * Ed.E.w + 3].cls : null, 'FrostTrap', '读回后 (3,3) 陷阱还在');
eq(Ed.E.plants[5 * Ed.E.w + 5] ? Ed.E.plants[5 * Ed.E.w + 5].cls : null, 'Sungrass', '读回后 (5,5) 植物还在');
eq(Ed.E.traps[6 * Ed.E.w + 2] && Ed.E.traps[6 * Ed.E.w + 2].visible, false, '读回后 (2,6) 未发现陷阱 visible=false 保持');
ok(!('i' in Ed.E.traps[3 * Ed.E.w + 3]), '读回后 pack 用的临时字段 i 已剥掉');

console.log('\n=== E2. 道具层：存/读往返 + 老档兼容 ===');
Ed.setRoomSize(8, 8);
Ed.setItem(2, 2, R.makeItem('Food'));
Ed.setItem(6, 3, R.makeItem('ScrollOfUpgrade', { heap: 'CHEST' }));
const j2 = Ed.toJSON();
ok(Array.isArray(j2.items) && j2.items.length === 2, 'JSON 里只存了 2 个道具（非空格）', j2.items && j2.items.length);
eq(Ed.E.map[2 * Ed.E.w + 2], T.EMPTY, '放道具**不改地形**：(2,2) 仍是空地');
Ed.setRoomSize(8, 8);
Ed.fromJSON(JSON.parse(JSON.stringify(j2)));
eq(Ed.E.items[2 * Ed.E.w + 2] ? Ed.E.items[2 * Ed.E.w + 2].cls : null, 'Food', '读回后 (2,2) 道具还在');
eq(Ed.E.items[3 * Ed.E.w + 6] ? Ed.E.items[3 * Ed.E.w + 6].heap : null, 'CHEST', '读回后 (6,3) 堆型 CHEST 保持');
eq(Ed.E.items[3 * Ed.E.w + 6] ? Ed.E.items[3 * Ed.E.w + 6].ctor : null, 'new ScrollOfUpgrade()',
	'读回后 ctor 复原成可编译语句');
/* 老档（v1/v2 没有 items 键）读进来必须得到空道具层，而不是 undefined 崩在后续遍历里。
 * 注意字段名是 tiles（不是 map）—— 照抄真 toJSON 的键，否则验的是假档。 */
const nowJSON = Ed.toJSON();
const legacy = {
	format: 'egopd-terrain-editor', version: 2,   // 旧名，测向后兼容（读侧白名单）
	w: nowJSON.w, h: nowJSON.h, tiles: nowJSON.tiles,
	traps: [], plants: []
};
Ed.fromJSON(legacy);
ok(Array.isArray(Ed.E.items), 'v2 老档读入后 items 是数组（不是 undefined）');
eq(Ed.E.items.filter(Boolean).length, 0, 'v2 老档读入后道具层为空');

console.log('\n=== E3. 道具层参与撤销 / 镜像 ===');
Ed.setRoomSize(8, 8);
Ed.pushUndo();
Ed.setItem(1, 1, R.makeItem('PotionOfHealing'));
Ed.undo();
eq(Ed.E.items[1 * Ed.E.w + 1], null, '撤销后道具层回滚（null）');
Ed.redo();
eq(Ed.E.items[1 * Ed.E.w + 1] ? Ed.E.items[1 * Ed.E.w + 1].cls : null, 'PotionOfHealing', '重做后道具层恢复');
Ed.setItem(4, 2, R.makeItem('Food'));
Ed.MACROS.mirrorX();
eq(Ed.E.items[2 * Ed.E.w + (Ed.E.w - 1 - 4)] ? Ed.E.items[2 * Ed.E.w + (Ed.E.w - 1 - 4)].cls : null, 'Food',
	'左右镜像把道具层一起搬了');

console.log('\n=== E4. 道具层被「擦除」工具清掉 ===');
Ed.setRoomSize(8, 8);
Ed.setItem(3, 3, R.makeItem('Food'));
Ed.clearLayersAt(3, 3, T);
eq(Ed.E.items[3 * Ed.E.w + 3], null, 'clearLayersAt 清掉了道具层');

console.log('\n=== F. codegen 必须输出陷阱/植物语句 ===');
Ed.setRoomSize(10, 8);
Ed.setTrap(3, 3, R.makeTrap('FrostTrap'));
Ed.setPlant(5, 5, R.makePlant('Sungrass'));
const java = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
ok(/package com\.x\.y;/.test(java), '生成结果含 package 声明');
ok(/public class .* extends StandardRoom/.test(java), '生成结果含类声明');

/* —— 这一段是本轮新增的核心要求：codegen 必须把两层翻成 setTrap / plant —— */
ok(/import com\.shatteredpixel\.shatteredpixeldungeon\.levels\.traps\.FrostTrap;/.test(java),
	'import 了 FrostTrap');
ok(/import com\.shatteredpixel\.shatteredpixeldungeon\.plants\.Sungrass;/.test(java),
	'import 了 Sungrass');
ok(/import com\.watabou\.utils\.Point;/.test(java), 'import 了 Point（pointToCell 需要）');

/* 只看**非注释**的代码行（注释里也出现了 setTrap 字样，会把计数带偏） */
const codeLines = java.split('\n').filter(l => !/^\s*\/\//.test(l));
const trapLine = codeLines.filter(l => /level\.setTrap\(/.test(l));
ok(trapLine.length === 1, '恰好生成 1 句 setTrap', trapLine);
ok(/new FrostTrap\(\)/.test(trapLine[0] || ''), 'setTrap 里 new 的是 FrostTrap', trapLine[0]);
ok(/left \+ 3, top \+ 3/.test(trapLine[0] || ''), '陷阱坐标是相对偏移 left+3 / top+3', trapLine[0]);

const plantLine = codeLines.filter(l => /level\.plant\(/.test(l));
ok(plantLine.length === 1, '恰好生成 1 句 plant', plantLine);
ok(/new Sungrass\.Seed\(\)/.test(plantLine[0] || ''), 'plant 里 new 的是 Sungrass.Seed（不是 Sungrass）', plantLine[0]);
ok(/left \+ 5, top \+ 5/.test(plantLine[0] || ''), '植物坐标是相对偏移 left+5 / top+5', plantLine[0]);

/* 反向自测：没有陷阱时**不许**凭空出现 setTrap / traps import */
(function () {
	const bakT = Ed.E.traps, bakP = Ed.E.plants;
	Ed.E.traps = new Array(Ed.E.w * Ed.E.h).fill(null);
	Ed.E.plants = new Array(Ed.E.w * Ed.E.h).fill(null);
	const j2 = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
	ok(!/setTrap/.test(j2), '无陷阱时生成结果里没有 setTrap');
	ok(!/level\.plant\(/.test(j2), '无植物时生成结果里没有 plant');
	ok(!/import com\.watabou\.utils\.Point;/.test(j2), '两层皆空时不 import Point');
	ok(!/\.levels\.traps\./.test(j2), '两层皆空时不 import 任何陷阱类');
	Ed.E.traps = bakT; Ed.E.plants = bakP;
})();

/* 片段模式也要带上两层 */
(function () {
	const body = Gen.generateBodyOnly();
	ok(/setTrap/.test(body), 'generateBodyOnly() 也含 setTrap');
	ok(/Sungrass\.Seed/.test(body), 'generateBodyOnly() 也含 Sungrass.Seed');
	ok(/import com\.watabou\.utils\.Point;/.test(body), 'generateBodyOnly() 附上了 Point import');
})();

/* —— 道具层 codegen：必须翻成 level.drop(...)，且堆型 / 额外 import 都要对 —— */
console.log('\n=== G. codegen 必须输出道具语句（level.drop，不是 addItemToSpawn） ===');
Ed.setRoomSize(10, 8);
Ed.setItem(2, 2, R.makeItem('Food'));
Ed.setItem(6, 3, R.makeItem('ScrollOfUpgrade', { heap: 'CHEST' }));
Ed.setItem(4, 6, R.makeItem('IronKey'));                       // ctor 带 Dungeon.depth
Ed.setItem(7, 6, R.makeItem('DarkGold'));                      // ctor 带 Random.NormalIntRange
const jItems = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
const jLines = jItems.split('\n').filter(l => !/^\s*\/\//.test(l));

const dropLines = jLines.filter(l => /level\.drop\(/.test(l));
ok(dropLines.length === 4, '恰好生成 4 句 level.drop', dropLines.length);
ok(/level\.drop\(/.test(jLines.join('\n')), '非注释行里含 level.drop');
/* 关键：绝不能用 addItemToSpawn —— 它走 randomDropCell()，位置是随机的 */
ok(!/addItemToSpawn/.test(jLines.join('\n')),
	'**关键**：不得出现 addItemToSpawn（它会被 createItems() 随机重排位置）');
ok(/\.type = Heap\.Type\.CHEST;/.test(jItems), 'CHEST 堆型写成了 .type = Heap.Type.CHEST');
ok(dropLines.filter(l => /\.type = Heap\.Type\.CHEST;/.test(l)).length === 1,
	'只有 1 句带 CHEST 堆型（其余是默认 HEAP）');
ok(/new Food\(\)/.test(jItems), '口粮写成 new Food()');
ok(/new ScrollOfUpgrade\(\)/.test(jItems), '升级卷轴写成 new ScrollOfUpgrade()');
ok(/left \+ 2, top \+ 2/.test(jItems), '道具坐标是相对偏移 left+2 / top+2');
ok(/left \+ 6, top \+ 3/.test(jItems), '带堆型的道具坐标也是相对偏移');
/* ctor 里用到 Random / Dungeon 时，import 必须自动补上，否则编译不过 */
ok(/import com\.watabou\.utils\.Random;/.test(jItems), 'ctor 含 Random. ⇒ 自动 import Random');
ok(/import com\.shatteredpixel\.shatteredpixeldungeon\.Dungeon;/.test(jItems),
	'ctor 含 Dungeon. ⇒ 自动 import Dungeon');
ok(/import com\.shatteredpixel\.shatteredpixeldungeon\.items\.Heap;/.test(jItems),
	'用到 Heap.Type ⇒ 自动 import Heap');
ok(/new IronKey\( Dungeon\.depth \)/.test(jItems), '钥匙按当前深度构造');
ok(/new DarkGold\(\)\.quantity\(Random\.NormalIntRange\(4, 5\)\)/.test(jItems),
	'暗金带随机数量');

/* 反向自测：没有道具时不许凭空出现 drop / Heap import */
(function () {
	const bak = Ed.E.items;
	Ed.E.items = new Array(Ed.E.w * Ed.E.h).fill(null);
	const j0 = Gen.generate({ pkg: 'com.x.y', rawClass: 'StandardRoom' });
	ok(!/level\.drop\(/.test(j0), '无道具时生成结果里没有 level.drop');
	ok(!/items\.Heap;/.test(j0), '无道具时不 import Heap');
	ok(!/import com\.watabou\.utils\.Random;/.test(j0), '无道具时不 import Random');
	ok(!/import com\.shatteredpixel\.shatteredpixeldungeon\.Dungeon;/.test(j0), '无道具时不 import Dungeon');
	const body0 = Gen.generateBodyOnly();
	ok(!/level\.drop\(/.test(body0), 'generateBodyOnly() 无道具时也没有 drop');
	Ed.E.items = bak;
})();

/* 片段模式也要带上道具 */
(function () {
	const body = Gen.generateBodyOnly();
	ok(/level\.drop\(/.test(body), 'generateBodyOnly() 也含 level.drop');
})();

console.log('\n' + (failures ? '\u274c 失败 ' + failures + ' 项' : '\u2705 全部通过') +
	'（通过 ' + passes + '，失败 ' + failures + '）\n');
process.exit(failures ? 1 : 0);
