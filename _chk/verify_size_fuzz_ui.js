/* EGOPD 地形编辑器 —— 尺寸模糊化（C.6）「端到端 UI」核验（无浏览器）
 *
 * 为什么需要它：verify_size_fuzz.js 验的是内核（safeRect / clamp / 类别选择），
 * 但「勾上面板开关 → 生成的 Java 真的变了 → 提示行真的更新了」这条链要经过
 * app.js 的 applyFuzzFromUI() + syncFuzzFromModel()。只验内核等于没验 UI。
 *
 * 本脚本造一个最小 DOM 桩（与 verify_editor_layers_ui.js 同一套结构），
 * 顺序载入 assets → render → editor → codegen → app，然后：
 *   ① 断言 boot() 后面板初始值已回灌内核（syncFuzzFromModel 生效）
 *   ② 勾选开关 / 改数字框（模拟 change 事件）⇒ 内核 + 生成代码 + 提示行三者同步
 *   ③ 三档快捷按钮（#btnFuzzSm/Md/Lg）真的写入四格
 *   ④ 越界输入被 clamp 并**回写输入框**（面板显示 = 实际生效，不许静默不一致）
 *   ⑤ 安全区参考线开关走渲染内核（R.showSafe）
 *   ⑥ 存/读 JSON 往返后面板与内核都恢复（这是最容易漏的一条）
 *   ⑦ 载入一个模糊化档后再碰任意控件，配置不会被默认值覆盖（反向同步验证）
 *
 * 反例自测（--selftest）：把 app.js 的 `refreshFuzzHint()` 从 applyFuzzFromUI 里
 * 去掉，本脚本必须报失败（证明它真的在验 UI 同步，而不是只验内核）。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const zlib = require('zlib');

const DIR = path.join(__dirname, '..', 'tools', 'terrain-editor');
let failures = 0, passes = 0;
function ok(cond, msg, extra) {
	if (cond) { passes++; console.log('  \u2713 ' + msg); }
	else { failures++; console.log('  \u2717 ' + msg + (extra !== undefined ? '  \u2192 ' + JSON.stringify(extra) : '')); }
}
function eq(a, b, msg) { ok(a === b, msg, { got: a, want: b }); }

/* ------------------------------------------------------ 真 PNG 解码 */
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
function pngOfDataURL(url) {
	if (!url || !/^data:image\/png;base64,/.test(url)) return null;
	return decodePng(Buffer.from(url.split(',')[1], 'base64'));
}

/* ------------------------------------------------------------ DOM 桩 */
function makeEl(id, tag) {
	const listeners = {};
	const el = {
		id, tagName: (tag || 'div').toUpperCase(),
		value: '', checked: false,
		children: [], style: {}, dataset: {}, disabled: false,
		classList: { _h: {}, add(c) { this._h[c] = 1; }, remove(c) { delete this._h[c]; },
			contains(c) { return !!this._h[c]; }, toggle(c, on) { if (on) this.add(c); else this.remove(c); } },
		appendChild(c) { el.children.push(c); return c; },
		removeChild(c) { const i = el.children.indexOf(c); if (i >= 0) el.children.splice(i, 1); return c; },
		insertBefore(c) { el.children.push(c); return c; },
		addEventListener(t, f) { (listeners[t] = listeners[t] || []).push(f); },
		removeEventListener(t, f) { const a = listeners[t] || []; const i = a.indexOf(f); if (i >= 0) a.splice(i, 1); },
		/* ⚠️ 必须 f.call(el, ev) —— app.js 回调里普遍写 this.checked / this.value */
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
	/* 真实 DOM 里 textContent 是 innerHTML 剥掉标签后的文本。
	 * 桩里必须复刻这一条 —— 否则 app.js 用 innerHTML 写、探针用 textContent 读时，
	 * 探针永远拿到空串，断言会集体假红（这里已经踩过一次）。
	 * 用 defineProperty 做成派生值，读的时候才计算。 */
	Object.defineProperty(el, 'textContent', {
		get() {
			return String(el._html || '')
				.replace(/<br\s*\/?>/gi, ' ')
				.replace(/<[^>]*>/g, '');
		},
		set(v) { el._html = String(v); },
		configurable: true
	});
	Object.defineProperty(el, 'innerHTML', {
		get() { return String(el._html || ''); },
		set(v) { el._html = String(v); },
		configurable: true
	});
	el._html = '';
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
			for (let y = 0; y < sh && (dy + y) < h; y++)
				for (let x = 0; x < sw && (dx + x) < w; x++) {
					const so = ((sy + y) * img.width + (sx + x)) * 4;
					const dof = ((dy + y) * w + (dx + x)) * 4;
					const p = img._png.data;
					buf[dof] = p[so]; buf[dof + 1] = p[so + 1]; buf[dof + 2] = p[so + 2]; buf[dof + 3] = p[so + 3];
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

/* 元素表：index.html 里 app.js 会 $() 的 id + C.6 新增的那批 */
const IDS = [
	'cv', 'canvasWrap', 'verLabel',
	'palette', 'curTile', 'stTile', 'stCell', 'stSize', 'stMsg', 'checkList',
	'chkFill', 'chkRing', 'selZoom', 'selStage',
	'lyrNone', 'lyrTrap', 'lyrPlant', 'lyrItem', 'selTrap', 'selPlant', 'selItem', 'selHeap',
	'chkTrapVisible', 'chkTrapActive', 'trapHint', 'layerHint',
	'chkGrid', 'chkFeat', 'chkWater', 'chkRoomSem',
	'btnGen', 'btnCopy', 'btnCopyBody', 'btnCopyNotes', 'btnDl',
	'inClass', 'selParent', 'inPkg', 'codeBox', 'chkNotes',
	'inW', 'inH', 'btnApplySize',
	'btnSave', 'btnLoad', 'btnPng', 'btnUndo', 'btnRedo',
	'btnCheck',
	/* C.5 区域随机 */
	'chkRegionLayers', 'btnRegionClear', 'regionHint', 'chkRegions',
	/* C.6 尺寸模糊化 */
	'chkFuzz', 'fzMinW', 'fzMaxW', 'fzMinH', 'fzMaxH', 'fzGuard',
	'btnFuzzSm', 'btnFuzzMd', 'btnFuzzLg', 'chkShowSafe', 'fuzzHint',
	/* C.3 外部图集 */
	'extHint', 'btnImportAtlas', 'chkRegions',
];
const els = {};
IDS.forEach(id => { els[id] = makeEl(id, 'div'); });
/* 输入型元素给上与 index.html 一致的初值 */
els.inW.value = '10'; els.inH.value = '10';
els.inClass.value = 'MyTerrainRoom';
els.inPkg.value = '';
els.chkFuzz.checked = false;
els.fzMinW.value = '8'; els.fzMaxW.value = '10'; els.fzMinH.value = '8'; els.fzMaxH.value = '10';
els.fzGuard.value = '1';
els.chkShowSafe.checked = true;
els.chkRegions.checked = true; els.chkRegionLayers.checked = true;
els.chkNotes.checked = false;
els.chkGrid.checked = true; els.chkFeat.checked = true; els.chkWater.checked = true;
els.chkRoomSem.checked = true;
els.chkTrapVisible.checked = true; els.chkTrapActive.checked = true;

els.cv.width = 160; els.cv.height = 128;
(function () {
	let c = null;
	els.cv.getContext = () => {
		if (!c || c._w !== els.cv.width || c._h !== els.cv.height)
			c = makeCtx(els.cv.width || 1, els.cv.height || 1, els.cv);
		return c;
	};
})();

['selTrap', 'selPlant', 'selItem', 'selHeap', 'selSheet', 'selStage', 'selZoom', 'selParent'].forEach(id => {
	if (!els[id]) els[id] = makeEl(id, 'select');
	els[id].querySelectorAll = sel => (sel === 'option' ? els[id]._options : []);
});

const document = {
	_els: els,
	getElementById(id) { return els[id] || (els[id] = makeEl(id, 'div')); },
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
		if (sel.startsWith('input[name=')) {
			const m = /^input\[name=['"]?([^'"\]]+)['"]?\]$/.exec(sel);
			if (m && document._groups[m[1]]) return document._groups[m[1]];
		}
		return [];
	},
	_groups: { layer: ['lyrNone', 'lyrTrap', 'lyrPlant', 'lyrItem'].map(id => els[id]) },
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
	body: makeEl('body'), documentElement: makeEl('html'), title: ''
};

const sandbox = {
	console, document, window: null,
	navigator: { userAgent: 'node' },
	localStorage: { _d: {}, getItem(k) { return this._d[k] || null; },
		setItem(k, v) { this._d[k] = String(v); }, removeItem(k) { delete this._d[k]; } },
	requestAnimationFrame(f) { return setTimeout(f, 0); }, cancelAnimationFrame() { },
	setTimeout, clearTimeout, setInterval, clearInterval,
	Image: function () {
		const im = { width: 0, height: 0, onload: null, onerror: null, __key: '', _ok: false, _png: null };
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
	Blob: function () { }, FileReader: function () { },
	URL: { createObjectURL() { return 'blob:x'; }, revokeObjectURL() { } },
	alert() { }, prompt() { return null; }, confirm() { return true; },
	addEventListener() { }, removeEventListener() { }, dispatchEvent() { },
	XMLHttpRequest: function () { },
	indexedDB: undefined,
	RTCPeerConnection: undefined
};
sandbox.window = sandbox;
sandbox.globalThis = sandbox;
sandbox.self = sandbox;
const ctx = vm.createContext(sandbox);

/* --------------------------------------------------------------- 载入 */
const SELFTEST = process.argv.includes('--selftest');
const sources = {};
['assets.js', 'render.js', 'editor.js', 'codegen.js', 'app.js'].forEach(f => {
	sources[f] = fs.readFileSync(path.join(DIR, f), 'utf8');
});
if (SELFTEST) {
	/* 反例：把 app.js 里 applyFuzzFromUI() 的 refreshFuzzHint() 调用摘掉。
	 * 这样内核与生成代码仍然正确，**只有提示行不更新** —— 正是我们要抓的那类
	 * 「半生效」缺陷。若本脚本没能报失败，说明它没在验 UI 同步。 */
	const before = sources['app.js'];
	sources['app.js'] = before.replace(
		'\t\tdraw();                      // 安全区参考线要跟着重画\n\t\trefreshFuzzHint();\n\t\trefreshChecks();             // 检查器 6g 段依赖 fuzz',
		'\t\tdraw();                      // 安全区参考线要跟着重画\n\t\trefreshChecks();             // 检查器 6g 段依赖 fuzz');
	if (sources['app.js'] === before) {
		console.log('!! 反例自测：未能改动 app.js（找不到 refreshFuzzHint 调用点）');
		process.exit(2);
	}
	console.log('\n[反例自测] 已从 applyFuzzFromUI 里摘掉 refreshFuzzHint()，期望提示行断言失败\n');
}
['assets.js', 'render.js', 'editor.js', 'codegen.js', 'app.js'].forEach(f => {
	vm.runInContext(sources[f], ctx, { filename: f });
});

const R = sandbox.TE_RENDER, Ed = sandbox.TE_EDITOR, Gen = sandbox.TE_GEN, App = sandbox.TE_APP;
const T = R.T;

/* ============================================================ A. 桩完整性 */
console.log('\n=== A. 桩完整性 ===');
ok(!!App, 'app.js 暴露 TE_APP');
ok(typeof App.fuzz === 'function', 'TE_APP.fuzz() 可用');
ok(typeof App.applyFuzz === 'function', 'TE_APP.applyFuzz() 可用');
ok(typeof App.fuzzHint === 'function', 'TE_APP.fuzzHint() 可用');
ok(typeof R.showSafe === 'function', 'render.js 暴露 showSafe()（读回开关）');

/* ================================================ B. boot 后的初始同步 */
console.log('\n=== B. boot() 后面板与内核一致（syncFuzzFromModel 生效）===');
const f0 = App.fuzz();
eq(f0.on, false, '初始未启用模糊化');
eq(els.chkFuzz.checked, false, '面板开关也显示未启用');
/* 关键：把面板改成「异于内核默认」的值再重启一次，才能证明是**回灌**而非巧合 */
ok(typeof App.applyFuzz === 'function', '（下面用 applyFuzz 走真实路径改值）');

/* ============================================ C. 面板 → 内核 → 代码 三者同步 */
console.log('\n=== C. 勾选开关 / 改数字框 ⇒ 内核 + 代码 + 提示行同步 ===');

/* 先造一个 12×12 内部房间（画布 14×14），保证夹取一定会发生 */
els.inW.value = '12'; els.inH.value = '12';
els.btnApplySize.dispatch('click', {});
eq(Ed.E.w, 14, '房间画布变成 14×14（内部 12）');

/* 模拟勾选启用 + 填 8~10 */
els.chkFuzz.checked = true;
els.fzMinW.value = '8'; els.fzMaxW.value = '10';
els.fzMinH.value = '8'; els.fzMaxH.value = '10';
els.fzGuard.value = '1';
els.chkFuzz.dispatch('change', {});
eq(App.fuzz().on, true, '勾选后内核 fuzz.on === true');
eq(App.fuzz().minW, 8, '内核 minW === 8');

const codeOn = els.codeBox.value;
ok(/public float\[\] sizeCatProbs\(\)\{/.test(codeOn), '开启后代码含 sizeCatProbs() 覆写');
ok(/setSize\( 8, 10, 8, 10 \);/.test(codeOn), '开启后代码含 setSize( 8, 10, 8, 10 )');
ok(/尺寸模糊化：开启/.test(codeOn), '文件头注释写明「尺寸模糊化：开启」');

const hintOn = App.fuzzHint();
ok(hintOn.indexOf('8~10') >= 0, '提示行写出了随机范围 8~10', hintOn.slice(0, 120));
ok(/安全区/.test(hintOn), '提示行提到了安全区', hintOn.slice(0, 160));
ok(/SizeCategory/.test(hintOn), '提示行写出了选中的 SizeCategory', hintOn.slice(0, 160));

/* 关掉 ⇒ 代码里不该再出现那一对 */
els.chkFuzz.checked = false;
els.chkFuzz.dispatch('change', {});
const codeOff = els.codeBox.value;
ok(!/sizeCatProbs/.test(codeOff), '关闭后代码不含 sizeCatProbs');
ok(!/setSize\(/.test(codeOff), '关闭后代码不含 setSize');
ok(/未启用/.test(App.fuzzHint()), '关闭后提示行说明「未启用」', App.fuzzHint().slice(0, 80));

/* 再打开，供后续段落使用 */
els.chkFuzz.checked = true;
els.chkFuzz.dispatch('change', {});

/* ==================================================== D. 三档快捷按钮 */
console.log('\n=== D. 三档快捷按钮（#btnFuzzSm / Md / Lg）===');
[[ 'btnFuzzSm', 7, 9], ['btnFuzzMd', 8, 10], ['btnFuzzLg', 10, 14]].forEach(function (p) {
	els[p[0]].dispatch('click', {});
	const f = App.fuzz();
	eq(f.minW, p[1], p[0] + ' ⇒ minW = ' + p[1]);
	eq(f.maxW, p[2], p[0] + ' ⇒ maxW = ' + p[2]);
	eq(f.minH, p[1], p[0] + ' ⇒ minH 与宽同步');
	eq(f.maxH, p[2], p[0] + ' ⇒ maxH 与宽同步');
	eq(f.on, true, p[0] + ' ⇒ 顺带把开关打开');
});
/* 大档 10~14 完全落在 LARGE 里 ⇒ 提示行应报 LARGE */
ok(/LARGE/.test(App.fuzzHint()), '点「大 10~14」后提示行报 LARGE 档', App.fuzzHint().slice(0, 160));

/* ======================================== E. 越界输入必须被夹并回写输入框 */
console.log('\n=== E. 越界 / 颠倒的输入：夹取后要回写输入框（面板 = 实际）===');
els.fzMinW.value = '1'; els.fzMaxW.value = '99';
els.fzMinH.value = '99'; els.fzMaxH.value = '1';       // 故意颠倒
els.fzGuard.value = '50';
els.fzMinW.dispatch('change', {});
const fClamp = App.fuzz();
ok(fClamp.minW >= 4 && fClamp.maxW <= 18, '尺寸被夹进 4~18（' + fClamp.minW + '~' + fClamp.maxW + '）');
ok(fClamp.minH <= fClamp.maxH, '颠倒的 min/max 被交换回来（' + fClamp.minH + '~' + fClamp.maxH + '）');
ok(fClamp.guarded <= 8, 'guarded 被夹到 ≤ 8（' + fClamp.guarded + '）');
/* 关键：输入框必须与内核一致 —— 否则用户看到 99，实际却是 18 */
eq(parseInt(els.fzMinW.value, 10), fClamp.minW, 'minW 输入框被回写成夹取后的值');
eq(parseInt(els.fzMaxW.value, 10), fClamp.maxW, 'maxW 输入框被回写成夹取后的值');
eq(parseInt(els.fzMinH.value, 10), fClamp.minH, 'minH 输入框被回写（颠倒已被修正）');
eq(parseInt(els.fzMaxH.value, 10), fClamp.maxH, 'maxH 输入框被回写');
eq(parseInt(els.fzGuard.value, 10), fClamp.guarded, 'guarded 输入框被回写');

/* 复位成干净的 8~10 */
els.fzMinW.value = '8'; els.fzMaxW.value = '10';
els.fzMinH.value = '8'; els.fzMaxH.value = '10'; els.fzGuard.value = '1';
els.fzMinW.dispatch('change', {});

/* ================================================ F. 安全区参考线开关 */
console.log('\n=== F. 安全区参考线开关走渲染内核 ===');
els.chkShowSafe.checked = false;
els.chkShowSafe.dispatch('change', {});
eq(R.showSafe(), false, '取消勾选 ⇒ R.showSafe() === false');
els.chkShowSafe.checked = true;
els.chkShowSafe.dispatch('change', {});
eq(R.showSafe(), true, '重新勾选 ⇒ R.showSafe() === true');
/* 渲染侧镜像的 fuzz 与编辑器侧一致 */
const rf = R.fuzz(), ef = App.fuzz();
eq(rf.on, ef.on, '渲染侧 fuzz.on 与编辑器一致');
eq(rf.minW, ef.minW, '渲染侧 fuzz.minW 与编辑器一致');
eq(rf.guarded, ef.guarded, '渲染侧 fuzz.guarded 与编辑器一致');

/* ================================================= G. 存 / 读 往返 */
console.log('\n=== G. 存 / 读 JSON 往返：内核与面板都要恢复 ===');
App.applyFuzz({ on: true, minW: 9, maxW: 13, minH: 9, maxH: 13, guarded: 2 });
const jSaved = JSON.parse(JSON.stringify(Ed.toJSON()));
eq(jSaved.version, 6, '存档 version === 6（v6 起带 mode）');
eq(jSaved.fuzz.minW, 9, '存档里 minW === 9');
eq(jSaved.fuzz.guarded, 2, '存档里 guarded === 2');

/* 先改成别的值，再读回来 —— 不先改就分不清「恢复了」还是「本来就没动」 */
App.applyFuzz({ on: false, minW: 4, maxW: 4, minH: 4, maxH: 4, guarded: 0 });
ok(App.fuzz().minW === 4, '改成了 4~4（作为对照）');

Ed.fromJSON(jSaved);
/* fromJSON 只改内核，不碰 DOM —— 这里手动走 app.js 的反向同步（模拟载入档） */
els.chkFuzz.checked = Ed.E.fuzz.on;
els.fzMinW.value = Ed.E.fuzz.minW; els.fzMaxW.value = Ed.E.fuzz.maxW;
els.fzMinH.value = Ed.E.fuzz.minH; els.fzMaxH.value = Ed.E.fuzz.maxH;
els.fzGuard.value = Ed.E.fuzz.guarded;
eq(App.fuzz().minW, 9, '读回后内核 minW === 9（不是对照值 4）');
eq(App.fuzz().guarded, 2, '读回后内核 guarded === 2');

/* ================ H. 载入档后碰任意控件不被默认值覆盖（反向同步验证） */
console.log('\n=== H. 载入档后再改别的控件，模糊化配置不被覆盖 ===');
/* 这一步模拟「打开面板就显示默认值、用户随手改一个数字 ⇒ 配置被静默冲掉」那个坑 */
els.fzMinW.value = String(Ed.E.fuzz.minW);
els.fzGuard.value = String(Ed.E.fuzz.guarded);
els.fzGuard.value = '3';                     // 只动 guarded
els.fzGuard.dispatch('change', {});
eq(App.fuzz().minW, 9, '只改 guarded ⇒ minW 仍是 9（没被默认值冲掉）');
eq(App.fuzz().guarded, 3, 'guarded 已更新为 3');
eq(App.fuzz().maxW, 13, 'maxW 仍是 13');

/* ================================================= I. 生成代码可编译性 */
console.log('\n=== I. UI 路径产出的代码仍具备成对结构 ===');
const codeI = els.codeBox.value;
/* 9~13 无解（NORMAL 装不下 13、LARGE 装不下 9）⇒ 必须两边都在，且提示要报无解 */
ok(/sizeCatProbs/.test(codeI), '仍有 sizeCatProbs');
ok(/setSize\(/.test(codeI), '仍有 setSize');
ok(/没有任何尺寸档能装下|无解/.test(App.fuzzHint()),
	'9~13 无解时提示行如实报「无解」', App.fuzzHint().slice(0, 200));

/* 换回有解的范围，确认提示恢复正常 */
App.applyFuzz({ on: true, minW: 10, maxW: 14, minH: 10, maxH: 14, guarded: 1 });
ok(!/没有任何尺寸档能装下|无解/.test(App.fuzzHint()),
	'10~14 有解时提示行不再报无解', App.fuzzHint().slice(0, 200));
ok(/LARGE/.test(App.fuzzHint()), '10~14 选中 LARGE 档');

/* ============================================================ H. 收尾 */
console.log('');
if (SELFTEST) {
	/* 反例自测：期望「提示行同步」那条断言失败 */
	if (failures > 0) {
		console.log('\u2705 反例自测通过：摘掉 refreshFuzzHint() 后本脚本确实报失败（' +
			failures + ' 项）');
		process.exit(0);
	}
	console.log('\u274c 反例自测失败：摘掉 refreshFuzzHint() 竟然还是全绿 —— 本脚本没在验 UI 同步');
	process.exit(1);
}
if (failures) { console.log('\u274c 尺寸模糊化 UI 核验：失败 ' + failures + ' 项（通过 ' + passes + '）'); process.exit(1); }
console.log('\u2705 尺寸模糊化 UI 核验全部通过（' + passes + ' 项）');
