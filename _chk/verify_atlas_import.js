/* EGOPD 地形编辑器 —— 「外部图集导入」核验（C.3）
 *
 * 验的是 app.js 里那条**只有跑起来才能验**的链路：
 *   文件选择器 → 尺寸校验 → 登记进 SHEETS → 成为当前图集 → 渲染真的换了图
 *   → IndexedDB 落盘 → 刷新页面后恢复
 *
 * 纯 Node 桩，但桩必须真到能骗过这段代码：
 *   ① IndexedDB 桩要**真能存取 Blob**（put / getAll / delete / clear 全套）；
 *   ② Blob 桩要能 `URL.createObjectURL` 出一个「可用」的 URL，且
 *      `new Image().src = url` 能真的解出像素（否则 Sheet 建不起来、渲染断言全废）；
 *   ③ Image 的 onload 必须**同步**触发（app.js 里是同步流程，见 3.0 那条红线）。
 *
 * 含反例自测（--selftest）：把尺寸校验去掉，断言必须检出「非 16 倍数被接受」。
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

/* --------------------------------------------------------- 真 PNG 编解码
 * 导入路径整条都依赖「Blob → URL → Image → 像素」，所以桩里必须能
 * 真的编出 PNG 再真的解回来。这里用最小实现（RGBA8，无滤波变体 0）。 */

function crc32(buf) {
	let c, crc = 0xffffffff;
	for (let i = 0; i < buf.length; i++) {
		c = (crc ^ buf[i]) & 0xff;
		for (let k = 0; k < 8; k++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
		crc = (crc >>> 8) ^ c;
	}
	return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
	const len = Buffer.alloc(4); len.writeUInt32BE(data.length, 0);
	const t = Buffer.from(type, 'ascii');
	const body = Buffer.concat([t, data]);
	const c = Buffer.alloc(4); c.writeUInt32BE(crc32(body), 0);
	return Buffer.concat([len, body, c]);
}

/* 造一张 RGBA8 的 PNG。fill(x,y) 返回 [r,g,b,a] */
function encodePng(w, h, fill) {
	const stride = w * 4;
	const raw = Buffer.alloc((stride + 1) * h);
	for (let y = 0; y < h; y++) {
		raw[y * (stride + 1)] = 0;
		for (let x = 0; x < w; x++) {
			const p = fill(x, y), o = y * (stride + 1) + 1 + x * 4;
			raw[o] = p[0]; raw[o + 1] = p[1]; raw[o + 2] = p[2]; raw[o + 3] = p[3];
		}
	}
	const ihdr = Buffer.alloc(13);
	ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
	ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
	return Buffer.concat([
		Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
		chunk('IHDR', ihdr),
		chunk('IDAT', zlib.deflateSync(raw)),
		chunk('IEND', Buffer.alloc(0))
	]);
}

function decodePng(buf) {
	let pos = 8, w = 0, h = 0, ct = 0, idat = [];
	while (pos < buf.length) {
		const len = buf.readUInt32BE(pos), type = buf.toString('ascii', pos + 4, pos + 8);
		const data = buf.slice(pos + 8, pos + 8 + len);
		if (type === 'IHDR') { w = data.readUInt32BE(0); h = data.readUInt32BE(4); ct = data[9]; }
		else if (type === 'IDAT') idat.push(data);
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
	}
	return { w, h, data: out };
}

/* 造一张「能被认出来」的楼层图集：帧 N 的颜色 = (N, N*2, N*3) 全不透明。
 * 这样渲染后读回像素就能确认「换的确实是这张图」。 */
function makeAtlasPng(w, h, seed) {
	return encodePng(w, h, (x, y) => {
		const idx = Math.floor(y / 16) * (w / 16) + Math.floor(x / 16);
		return [(idx * 7 + seed) & 0xff, (idx * 13 + seed) & 0xff, (idx * 29 + seed) & 0xff, 255];
	});
}

/* ----------------------------------------------------------- 桩：DOM */

function makeEl(id, tag) {
	const listeners = {};
	const el = {
		id, tagName: (tag || 'div').toUpperCase(),
		value: '', checked: false, textContent: '', innerHTML: '',
		children: [], style: {}, dataset: {}, disabled: false,
		classList: { _h: {}, add(c) { this._h[c] = 1; }, remove(c) { delete this._h[c]; }, contains(c) { return !!this._h[c]; }, toggle() { } },
		appendChild(c) { el.children.push(c); return c; },
		removeChild(c) { const i = el.children.indexOf(c); if (i >= 0) el.children.splice(i, 1); return c; },
		insertBefore(c) { el.children.push(c); return c; },
		addEventListener(t, f) { (listeners[t] = listeners[t] || []).push(f); },
		removeEventListener() { },
		dispatch(t, ev) { (listeners[t] || []).forEach(f => f.call(el, ev || {})); },
		getContext() { return CTX; },
		getBoundingClientRect() { return { left: 0, top: 0, width: 640, height: 480, right: 640, bottom: 480 }; },
		focus() { }, blur() { }, click() { el.dispatch('click', {}); },
		setAttribute(k, v) { el[k] = v; }, getAttribute(k) { return el[k]; },
		querySelector(sel) { return document.querySelector(sel); },
		querySelectorAll(sel) { return document.querySelectorAll(sel); }
	};
	el._options = [];
	return el;
}

const DRAWN = [];
function makeCtx(w, h, canvasEl) {
	const buf = new Uint8ClampedArray(w * h * 4);
	return {
		_buf: buf, _w: w, _h: h, canvas: canvasEl,
		save() { }, restore() { }, translate() { }, scale() { }, rotate() { }, clip() { },
		beginPath() { }, closePath() { }, setTransform() { }, rect() { }, setLineDash() { },
		clearRect() { }, fillRect() { }, strokeRect() { }, moveTo() { }, lineTo() { },
		stroke() { }, fill() { }, arc() { }, measureText() { return { width: 10 }; },
		fillText() { }, strokeText() { },
		createLinearGradient() { return { addColorStop() { } }; },
		drawImage(img, ...a) {
			DRAWN.push({ key: img && img.__key, args: a });
			if (!img || !img._png) return;
			let sx = 0, sy = 0, sw = img.width, sh = img.height, dx = 0, dy = 0;
			if (a.length === 2) { dx = a[0]; dy = a[1]; }
			else if (a.length === 4) { dx = a[0]; dy = a[1]; sw = a[2]; sh = a[3]; }
			else if (a.length === 8) { [sx, sy, sw, sh, dx, dy] = a; }
			for (let y = 0; y < sh && (dy + y) < h; y++)
				for (let x = 0; x < sw && (dx + x) < w; x++) {
					const so = ((sy + y) * img.width + (sx + x)) * 4, dof = ((dy + y) * w + (dx + x)) * 4;
					const p = img._png.data;
					if (p) { buf[dof] = p[so]; buf[dof + 1] = p[so + 1]; buf[dof + 2] = p[so + 2]; buf[dof + 3] = p[so + 3]; }
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

const IDS = [
	'cv', 'canvasWrap', 'verLabel', 'palette', 'curTile', 'stTile', 'stCell', 'stSize',
	'stMsg', 'checkList', 'chkFill', 'chkRing', 'selZoom', 'selStage', 'selSheet',
	'lyrNone', 'lyrTrap', 'lyrPlant', 'lyrItem', 'selTrap', 'selPlant', 'selItem', 'selHeap',
	'chkTrapVisible', 'chkTrapActive', 'layerHint',
	'chkGrid', 'chkFeat', 'chkWater', 'chkRoomSem', 'chkItems',
	'btnGen', 'btnCopy', 'btnCopyBody', 'btnCopyNotes', 'btnDl',
	'inClass', 'selParent', 'inPkg', 'codeBox', 'chkNotes',
	'inW', 'inH', 'btnApplySize',
	'btnSave', 'btnLoad', 'btnPng', 'btnUndo', 'btnRedo', 'btnCheck',
	// C.3 新增
	'btnImpSheet', 'btnImpFeat', 'btnImpItems', 'btnExtClear', 'extHint',
];
const els = {};
IDS.forEach(id => { els[id] = makeEl(id, /^(inp|chk|sel|file|btn)/.test(id) ? 'input' : 'div'); });
els.cv.width = 160; els.cv.height = 128;
els.cv.getContext = () => CTX;
els.chkGrid.checked = true; els.chkFeat.checked = true;
els.chkWater.checked = true; els.chkItems.checked = true; els.chkRoomSem.checked = true;
['selTrap', 'selPlant', 'selItem', 'selHeap', 'selSheet', 'selStage', 'selZoom', 'selParent']
	.forEach(id => { els[id].querySelectorAll = sel => (sel === 'option' ? els[id]._options : []); });

/* ---------------- 桩：IndexedDB（真能存取 Blob） ---------------- */

const DB = new Map();          // 后端存储：key → record
function makeReq() {
	const r = { onsuccess: null, onerror: null, result: undefined };
	setTimeout(() => { try { if (r.onsuccess) r.onsuccess(); } catch (e) { if (r.onerror) r.onerror(e); } }, 0);
	return r;
}
function makeStore() {
	return {
		put(rec) { DB.set(rec.key, rec); return makeReq(); },
		getAll() { const r = makeReq(); r.result = Array.from(DB.values()); return r; },
		delete(k) { DB.delete(k); return makeReq(); },
		clear() { DB.clear(); return makeReq(); }
	};
}
const IDB = {
	open() {
		const req = { onsuccess: null, onerror: null, onupgradeneeded: null, result: null };
		setTimeout(() => {
			const db = {
				objectStoreNames: { contains: () => true },
				createObjectStore() { },
				transaction() {
					const tx = { oncomplete: null, onerror: null, objectStore: () => makeStore() };
					setTimeout(() => { if (tx.oncomplete) tx.oncomplete(); }, 0);
					return tx;
				}
			};
			req.result = db;
			if (req.onsuccess) req.onsuccess();
		}, 0);
		return req;
	}
};

/* ---------------- 桩：Blob / URL / Image（能真解码） ---------------- */

let BLOB_SEQ = 0;
const BLOB_REG = new Map();    // url → Buffer（PNG 原始字节）
const BLOB_BY_BODY = new Map(); // 内容 → url（模拟「同一个 Blob 得到同一个 URL」）

function makeBlob(parts) {
	const body = parts.map(p => (typeof p === 'string' ? Buffer.from(p, 'utf8') : p)).reduce((a, b) => Buffer.concat([a, b]), Buffer.alloc(0));
	return { _buf: body, size: body.length, __isBlob: true };
}

const URLStub = {
	createObjectURL(b) {
		if (b && b._buf) {
			/* ⚠️ 不能只按「长度 + 前 32 字节」判重：编辑器里测试用的两张 256×256 图集
			 * 压缩后长度与前 32 字节**完全可能相同**（同尺寸、同编码参数、同滤波），
			 * 那样两张图会共用一个 URL ⇒ 后导入的图被静默当成前一张（实测踩到）。 */
			const sig = b._buf.length + ':' + require('crypto').createHash('md5').update(b._buf).digest('hex');
			if (BLOB_BY_BODY.has(sig)) {
				const u = BLOB_BY_BODY.get(sig);
				/* ⚠️ 缓存里那条可能已被 revoke（clearExt / 替换旧图集时会 revoke）。
				 * 此时必须**重新发号**，否则把死 URL 交回去 ⇒ Image.onerror ⇒ 导入静默失败。
				 * （实测踩到：清空外部图集后再导入同一张图，返回的 rec 是 null。） */
				if (BLOB_REG.has(u)) return u;
				BLOB_BY_BODY.delete(sig);
			}
			const u = 'blob:egopd/' + (++BLOB_SEQ);
			BLOB_REG.set(u, b._buf);
			BLOB_BY_BODY.set(sig, u);
			return u;
		}
		return 'blob:x';
	},
	revokeObjectURL(u) { BLOB_REG.delete(u); }
};

/* Image 桩：src 可以吃 dataURL，也可以吃 blob URL（从 BLOB_REG 取字节解 PNG） */
function makeImage() {
	const im = { width: 0, height: 0, onload: null, onerror: null, __key: '', _ok: false, _png: null };
	Object.defineProperty(im, 'src', {
		set(v) {
			im.__key = String(v);
			let buf = null;
			if (/^data:image\/png;base64,/.test(im.__key)) buf = Buffer.from(im.__key.split(',')[1], 'base64');
			else if (BLOB_REG.has(im.__key)) buf = BLOB_REG.get(im.__key);
			if (buf) {
				try {
					const png = decodePng(buf);
					im._png = png; im.width = png.w; im.height = png.h; im._ok = true;
				} catch (e) { im._ok = false; }
			}
			/* onload 必须同步（见文件头 ③） */
			if (im._ok) { if (im.onload) im.onload(); }
			else if (im.onerror) im.onerror();
		},
		get() { return im.__key; }
	});
	return im;
}

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
		if (sel.startsWith('input[name=')) {
			const m = /^input\[name=['"]?([^'"\]]+)['"]?\]$/.exec(sel);
			if (m && document._groups[m[1]]) return document._groups[m[1]];
		}
		return [];
	},
	_groups: { lyr: ['lyrNone', 'lyrTrap', 'lyrPlant', 'lyrItem'].map(id => els[id]) },
	createElement(tag) {
		const el = makeEl('__new_' + tag, tag);
		if (String(tag).toLowerCase() === 'canvas') {
			el.width = 0; el.height = 0;
			let c = null;
			el.getContext = () => {
				if (!c || c._w !== el.width || c._h !== el.height) c = makeCtx(el.width || 1, el.height || 1, el);
				return c;
			};
		}
		return el;
	},
	addEventListener() { }, removeEventListener() { },
	body: makeEl('body'), documentElement: makeEl('html'), title: ''
};

/* 记录「最近一次被 pickFile 用过的 input」，供测试注入文件 */
let LAST_FILE_INPUT = null;

const sandbox = {
	console, document,
	window: null,
	navigator: { userAgent: 'node' },
	localStorage: { _d: {}, getItem(k) { return this._d[k] || null; }, setItem(k, v) { this._d[k] = String(v); }, removeItem(k) { delete this._d[k]; } },
	indexedDB: IDB,
	requestAnimationFrame(f) { return setTimeout(f, 0); },
	cancelAnimationFrame() { },
	setTimeout, clearTimeout, setInterval, clearInterval,
	Image: makeImage,
	Blob: makeBlob,
	URL: URLStub,
	FileReader: function () { },
	alert() { }, prompt() { return null; }, confirm() { return true; },
	addEventListener() { }, removeEventListener() { }, dispatchEvent() { },
};
sandbox.window = sandbox;
sandbox.globalThis = sandbox;
sandbox.self = sandbox;

const ctx = vm.createContext(sandbox);

/* app.js 的 pickFile 会 document.createElement('input') 再 inp.click()。
 * 我们拦下这个 input，好在测试里塞 file。 */
const _origCreate = document.createElement;
document.createElement = function (tag) {
	const el = _origCreate.call(document, tag);
	if (String(tag).toLowerCase() === 'input') LAST_FILE_INPUT = el;
	return el;
};

function loadAll() {
	['assets.js', 'render.js', 'editor.js', 'codegen.js', 'app.js'].forEach(f => {
		vm.runInContext(fs.readFileSync(path.join(DIR, f), 'utf8'), ctx, { filename: f });
	});
}

/* 等 IndexedDB / 定时器那一串异步排空 */
function flush() {
	return new Promise(res => setTimeout(res, 30));
}

(async function main() {
	const SELFTEST = process.argv.includes('--selftest');

	loadAll();
	await flush();

	const R = sandbox.TE_RENDER, Ed = sandbox.TE_EDITOR, App = sandbox.TE_APP;

	console.log('\n=== A. 桩与接线 ===');
	ok(!!App, 'app.js 暴露 TE_APP 探针');
	ok(!!App.importAtlas, 'TE_APP.importAtlas 可用');
	eq(typeof App.sheets, 'function', 'TE_APP.sheets 是函数');
	const builtinSheets = App.sheets();
	eq(builtinSheets.length, 5, '初始有 5 张内置图集（原版 5 个区域；项目特图集走外部导入）');
	ok(builtinSheets.every(s => s.builtin === true), '初始全部是 builtin');
	eq(App.ext().length, 0, '初始无外部图集');
	eq(App.extHint(), '当前无外部图集', '提示文案为「当前无外部图集」');

	console.log('\n=== B. 导入一张楼层图集 ===');
	const pngA = makeAtlasPng(256, 256, 3);
	const fileA = { name: 'my_custom_tiles.png', type: 'image/png', _buf: pngA };
	const recA = await new Promise(res => App.importAtlas('sheet', fileA, res));

	ok(!!recA, 'importAtlas 返回了记录');
	eq(recA && recA.kind, 'sheet', 'kind = sheet');
	eq(recA && recA.key, 'ext:sheet:my_custom_tiles', 'key 带 ext:sha 前缀且去掉了 .png');
	const sheets2 = App.sheets();
	eq(sheets2.length, 6, '下拉注册表增至 6 张（5 内置 + 1 外部）');
	const extEntry = sheets2.filter(s => !s.builtin)[0];
	ok(!!extEntry, '新增项 builtin=false（⇒ UI 会带 ★）');
	eq(Ed.E.sheetKey, 'ext:sheet:my_custom_tiles', '导入后自动切为当前图集');

	await flush();
	await flush();
	eq(R.state.sheet ? 'ok' : 'null', 'ok', '渲染内核已收到图集（S.sheet 非空）');
	eq(R.state.sheet.w, 256, '内核里的图集宽度 = 256');
	eq(R.state.sheet.h, 256, '内核里的图集高度 = 256');

	console.log('\n=== C. 渲染真的换成了新图（像素级）===');
	Ed.setRoomSize(3, 3);
	Ed.MACROS.skeleton();
	Ed.set(1, 1, R.T.EMPTY);
	/* applySheet() 的链路是异步的（getImage → loadFeatures → loadWater → loadItems），
	 * 必须等它排空才读 S.sheet，否则读到的是 boot() 时那张内置图。 */
	await flush(); await flush(); await flush();
	/* 直接问内核：当前图集在帧 0 处的像素 */
	const px = R.state.sheet.raw(0, 0, 0);
	const want = decodePng(pngA).data;
	eq(px[0], want[0], '帧 0 的 R 通道来自新图');
	eq(px[1], want[1], '帧 0 的 G 通道来自新图');
	eq(px[2], want[2], '帧 0 的 B 通道来自新图');
	ok(!(px[0] === 83 && px[1] === 82 && px[2] === 78), '确认不是内置图集的下水道首帧');

	console.log('\n=== D. 尺寸校验（非 16 倍数必须拒绝）===');
	const pngBad = makeAtlasPng(16, 16, 1);
	/* 造一张 24×16（宽不是 16 的倍数但总长合法）*/
	const badPng = encodePng(24, 16, (x, y) => [200, 30, 30, 255]);
	const fileBad = { name: 'bad_size.png', type: 'image/png', _buf: badPng };
	const before = App.sheets().length;
	const recBad = await new Promise(res => App.importAtlas('sheet', fileBad, res));
	eq(recBad, null, '非 16 倍数被拒绝（返回 null）');
	eq(App.sheets().length, before, '注册表未增长');
	eq(App.ext().length, 1, '外部记录仍是 1 条（未被污染）');

	console.log('\n=== E. 同类图集只保留一张（导入第二张会替换第一张）===');
	const pngB = makeAtlasPng(256, 256, 9);
	const fileB = { name: 'second_tiles.png', type: 'image/png', _buf: pngB };
	await new Promise(res => App.importAtlas('sheet', fileB, res));
	await flush(); await flush(); await flush();
	const sheets3 = App.sheets();
	eq(sheets3.length, 6, '仍是 6 张（旧的被卸下、新的挂上）');
	const extras = sheets3.filter(s => !s.builtin);
	eq(extras.length, 1, '外部图集只有 1 张');
	eq(extras[0].key, 'ext:sheet:second_tiles', '留下的是后导入的那张');
	eq(App.ext().length, 1, 'EXT 记录也是 1 条');
	const px2 = R.state.sheet.raw(0, 0, 0);
	const wantB = decodePng(pngB).data;
	eq(px2[0], wantB[0], '内核已切到第二张图的像素');
	ok(!(px2[0] === want[0] && px2[1] === want[1]), '确认不是第一张图（替换真的生效）');

	console.log('\n=== F. 草叶图 / 道具图（覆盖内置那张）===');
	const featPng = encodePng(256, 128, (x, y) => [10, 200, 10, 255]);
	await new Promise(res => App.importAtlas('features', { name: 'my_features.png', type: 'image/png', _buf: featPng }, res));
	await flush();
	ok(/^blob:/.test(App.featuresSrc()), 'FEATURES_SRC 已指向外部 blob URL');
	eq(App.sheets().length, 6, '草叶图不入 SHEETS 下拉（它不是楼层图集）');
	eq(App.ext().length, 2, 'EXT 现有 2 条（sheet + features）');

	const itemsPng = encodePng(256, 800, (x, y) => [240, 160, 20, 255]);
	await new Promise(res => App.importAtlas('items', { name: 'my_items.png', type: 'image/png', _buf: itemsPng }, res));
	await flush();
	ok(/^blob:/.test(App.itemsSrc()), 'ITEMS_SRC 已指向外部 blob URL');
	eq(App.ext().length, 3, 'EXT 现有 3 条');

	const hint = App.extHint();
	ok(hint.indexOf('楼层图集 1 张') >= 0, '提示含「楼层图集 1 张」', hint);
	ok(hint.indexOf('草叶图 1 张') >= 0, '提示含「草叶图 1 张」', hint);
	ok(hint.indexOf('道具图 1 张') >= 0, '提示含「道具图 1 张」', hint);

	console.log('\n=== G. 持久化：IndexedDB 真的落了盘 ===');
	await flush();
	eq(DB.size, 3, 'IndexedDB 里有 3 条记录');
	const savedSheet = DB.get('ext:sheet:second_tiles');
	ok(!!savedSheet, '楼层图集记录已存（key 正确）');
	ok(!!(savedSheet && savedSheet.blob) && typeof savedSheet.blob !== 'string',
		'存的是 Blob 而不是字符串/dataURL（体积不膨胀）');
	eq(savedSheet && savedSheet.kind, 'sheet', 'kind 一并存下（恢复时要靠它分派）');
	ok(!!(savedSheet && savedSheet.label), 'label 一并存下（否则刷新后下拉是空名）');

	console.log('\n=== H. 代码生成里的图集标签用的是人类可读名 ===');
	const code = sandbox.TE_GEN.generate({
		pkg: 'p', rawClass: 'StandardRoom', sheetLabel: extras[0].label
	});
	ok(code.indexOf('second_tiles') >= 0, '生成代码里出现外部图集的名字');
	ok(code.indexOf('ext:sheet:') < 0, '生成代码里**不出现**内部 key（ext:sheet:）');

	console.log('\n=== I. 清空外部图集 ===');
	const tx0 = sandbox.TE_EDITOR.E;
	App.clearExt();
	await flush();
	await flush();
	eq(App.ext().length, 0, 'EXT 已清空');
	eq(App.sheets().length, 5, '下拉回到 5 张内置');
	ok(App.sheets().every(s => s.builtin), '剩下的全是内置');
	eq(App.extHint(), '当前无外部图集', '提示复位');
	ok(App.featuresSrc() === sandbox.TE_ASSETS.features, '草叶图回退到内置');
	ok(App.itemsSrc() === sandbox.TE_ASSETS.items, '道具图回退到内置');
	eq(DB.size, 0, 'IndexedDB 已清空');
	ok(tx0.sheetKey.indexOf('ext:') !== 0, '当前图集 key 已从 ext: 回退到内置');

	console.log('\n=== J. 刷新页面后能恢复（模拟第二次 boot）===');
	/* 先把一张图集重新导入并落盘，然后**重建整个沙箱**跑第二遍 loadAll。
	 * dbPut 的链路是 put → tx.oncomplete，需要两跳定时器，所以 flush 三次。 */
	await new Promise(res => App.importAtlas('sheet', fileA, res));
	await flush(); await flush(); await flush();
	eq(DB.size, 1, '重新导入后有 1 条持久化记录');
	const pk = App.sheets().filter(s => !s.builtin)[0].key;

	/* 第二遍：同一个 IndexedDB 后端、同一条路径 */
	const S2 = buildSandbox();
	['assets.js', 'render.js', 'editor.js', 'codegen.js', 'app.js'].forEach(f => {
		vm.runInContext(fs.readFileSync(path.join(DIR, f), 'utf8'), S2.ctx, { filename: f });
	});
	await flush(); await flush();
	const App2 = S2.sandbox.TE_APP;
	eq(App2.ext().length, 1, '刷新后外部图集被恢复（1 条）');
	eq(App2.sheets().length, 6, '下拉里重新出现 6 张');
	ok(App2.sheets().some(s => s.key === pk && !s.builtin), '恢复的 key 与刷新前一致');
	eq(App2.ext()[0].kind, 'sheet', '恢复出来的记录 kind 正确');
	ok(!!App2.ext()[0].label, '恢复出来的记录带 label');
	ok(App2.sheets().filter(s => !s.builtin)[0].label.indexOf('my_custom_tiles') >= 0,
		'恢复后下拉标签还是人类可读的名字');

	console.log('\n=== K. 反例自测 ===');
	if (SELFTEST) {
		/* 破坏尺寸校验：把 %16 检查去掉，看本脚本能否检出 */
		const p = path.join(DIR, 'app.js');
		const orig = fs.readFileSync(p, 'utf8');
		const broken = orig.replace(
			'if (im.width % 16 !== 0 || im.height % 16 !== 0) {',
			'if (false) {'
		);
		if (broken === orig) {
			console.log('  \u2717 反例注入失败：没找到尺寸校验那一行');
			failures++;
		} else {
			fs.writeFileSync(p, broken, 'utf8');
			const S3 = buildSandbox();
			['assets.js', 'render.js', 'editor.js', 'codegen.js', 'app.js'].forEach(f => {
				vm.runInContext(fs.readFileSync(path.join(DIR, f), 'utf8'), S3.ctx, { filename: f });
			});
			await flush();
			const A3 = S3.sandbox.TE_APP;
			const r = await new Promise(res => A3.importAtlas('sheet',
				{ name: 'bad.png', type: 'image/png', _buf: encodePng(24, 16, () => [1, 2, 3, 255]) }, res));
			fs.writeFileSync(p, orig, 'utf8');       // 还原
			ok(!!r, '反例被检出：去掉校验后 24×16 的图被错误接受');
			ok(A3.sheets().some(s => !s.builtin), '反例被检出：坏尺寸图进了下拉');
		}
	} else {
		console.log('  （跳过；加 --selftest 运行反例）');
	}

	console.log('\n' + (failures === 0
		? '\u2705 外部图集导入全部通过（' + passes + ' 项）'
		: '\u274c ' + failures + ' 项失败 / ' + passes + ' 项通过'));
	process.exit(failures === 0 ? 0 : 1);
})();

/* 造一个全新的沙箱（共享同一个 IndexedDB 后端和 BLOB_REG，模拟「刷新页面」）*/
function buildSandbox() {
	const ls = {};
	const sb = Object.assign({}, sandbox, {
		document,
		localStorage: { getItem(k) { return ls[k] || null; }, setItem(k, v) { ls[k] = String(v); }, removeItem(k) { delete ls[k]; } },
		window: null
	});
	sb.window = sb; sb.globalThis = sb; sb.self = sb;
	const c = vm.createContext(sb);
	return { sandbox: sb, ctx: c };
}
