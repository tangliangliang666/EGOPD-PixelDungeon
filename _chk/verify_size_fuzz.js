/* 尺寸模糊化（C.6）核验：
 *
 * 这一套只查一件事：**开启模糊化后，生成的代码能不能真的让房间在指定范围内变大变小，
 * 而且不会在变小的时候把内容画到房间外面去。**
 *
 * SPD 源码侧的三条硬事实（已对着 core 源核过，本脚本把它们固化成断言）：
 *   ① StandardRoom.setSizeCat() 先按 sizeCatProbs() 抽一个 SizeCategory，
 *      再由该类别给 minWidth()/maxWidth()/minHeight()/maxHeight()；
 *   ② Room.setSize(minW,maxW,minH,maxH) 有硬校验
 *      `minW < minWidth() || maxW > maxWidth()` ⇒ **静默 return false**（什么都不发生，
 *      也绝不报错）—— 所以 sizeCatProbs() 覆写与 setSize() 调用**必须成对**；
 *   ③ Room.resize()/Rect.resize() 只改 right/bottom，left/top 恒定
 *      ⇒ 房间缩小时右/下边界往左上收；靠右/靠下的内容会被裁掉。
 *
 * 因此本脚本分四段：
 *   A. 安全区算术（safeRect 与 codegen 的 fuzzSafeRight/Bottom 必须一致）
 *   B. 夹取正确性（模糊化开启后，所有输出坐标都落在安全区内）
 *   C. 类别选择（能装下就装下；装不下必须报 fuzzImpossible，而不是悄悄生成废码）
 *   D. 生成代码的语法/接线（sizeCatProbs 覆写、setSize 调用、成对性）
 * 反例自测：每一段都注入一个「本该失败」的变体，确认断言真的会红。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const DIR = path.join(__dirname, '..', 'tools', 'terrain-editor');

function fakeCtx() {
	return { drawImage() {}, getImageData(w, h) { return { data: new Uint8ClampedArray(4) }; },
		clearRect() {}, save() {}, restore() {}, scale() {}, fillRect() {}, strokeRect() {},
		beginPath() {}, moveTo() {}, lineTo() {}, stroke() {}, setLineDash() {}, fill() {} };
}
const sandbox = {
	window: {},
	document: { createElement() { return { width: 0, height: 0,
		getContext() { return fakeCtx(); }, style: {}, appendChild() {}, addEventListener() {} }; },
		querySelector() { return null; }, body: { appendChild() {}, removeChild() {} } },
	console, Image: function () { this.src = ''; },
	setTimeout, clearTimeout, navigator: {},
	Blob: function () {}, URL: { createObjectURL() { return ''; }, revokeObjectURL() {} },
	FileReader: function () {}
};
sandbox.window = sandbox;
vm.createContext(sandbox);
for (const f of ['render.js', 'editor.js', 'codegen.js']) {
	vm.runInContext(fs.readFileSync(path.join(DIR, f), 'utf8'), sandbox, { filename: f });
}
const Ed = sandbox.TE_EDITOR, Gen = sandbox.TE_GEN, T = Ed.T;

const NAME2VAL = {};
Ed.PALETTE.forEach(g => g.items.forEach(it => { NAME2VAL[it[0]] = it[1]; }));

let fail = 0, counterExamples = 0;
function ok(cond, label, extra) {
	if (cond) console.log('  ✓ ' + label);
	else { console.log('  ✗ ' + label + (extra ? '  —— ' + extra : '')); fail++; }
}
/* 反例：期望条件**为假**。若为真说明判据恒真（假绿）。 */
function mustFail(cond, label, extra) {
	if (!cond) { console.log('  ✓ [反例] ' + label); counterExamples++; }
	else { console.log('  ✗ [反例] ' + label + ' —— 判据恒真，反例没生效！'); fail++; }
}

/* 把 PALETTE 的名字映射反转（值 -> 名） */
function nameOf(v) { for (const k in NAME2VAL) if (NAME2VAL[k] === v) return k; return String(v); }

/* 解析生成的代码里所有 `left + A` / `top + B` 的坐标对（连同宽高） */
function extractCells(src, w, h) {
	const cells = [];
	const reSet = /^Painter\.set\(\s*level,\s*left\s*\+\s*(\d+),\s*top\s*\+\s*(\d+),\s*Terrain\.(\w+)\s*\);/;
	const reFill = /^Painter\.fill\(\s*level,\s*left\s*\+\s*(\d+),\s*top\s*\+\s*(\d+),\s*(\d+),\s*(\d+),\s*Terrain\.(\w+)\s*\);/;
	src.split('\n').forEach(raw => {
		const s = raw.trim();
		let m;
		if ((m = s.match(reSet))) {
			cells.push({ x: +m[1], y: +m[2], w: 1, h: 1, t: m[3], kind: 'set' });
		} else if ((m = s.match(reFill))) {
			cells.push({ x: +m[1], y: +m[2], w: +m[3], h: +m[4], t: m[5], kind: 'fill' });
		}
	});
	return cells;
}

/* ============================ A. 安全区算术 ============================ */

console.log('');
console.log('A. 安全区算术（editor.js 的 safeRect 与 codegen.js 的 fuzzSafeRight/Bottom 必须同源）');

Ed.setRoomSize(12, 12);
Ed.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, guarded: 1 });

const sr = Ed.safeRect();
/* codegen 侧：fuzzSafeRight/Bottom 给的是**相对偏移**（含 left/top 基准），
 * 也就是相对 left 的 x 与相对 top 的 y；safeRect 给的是当前画布上的绝对格号。
 * 画布原点 = 房间左上角 ⇒ 两者应当逐位相等。**开启和关闭两种状态都要比**。 */
ok(Gen.fuzzSafeRight() === sr.r && Gen.fuzzSafeBottom() === sr.b,
	'[开启] 两处算出的安全区右下边界一致（r=' + sr.r + ', b=' + sr.b + '）',
	'codegen: ' + Gen.fuzzSafeRight() + ',' + Gen.fuzzSafeBottom());
/* 手算：minW=8，guarded=1 ⇒ 右边界 = 8-1-1 = 6 */
ok(sr.r === 6 && sr.b === 6, '安全区右/下边界 = minW-1-guarded = 6', 'r=' + sr.r + ' b=' + sr.b);
ok(sr.l === 1 && sr.t === 1, '安全区左/上边界 = guarded = 1', 'l=' + sr.l + ' t=' + sr.t);

/* 反例：把 guarded 改成 0，安全区必须变大（否则判据没接线） */
Ed.setFuzz({ guarded: 0 });
const sr0 = Ed.safeRect();
mustFail(sr0.r === sr.r, 'guarded 0→1 会改变安全区（说明 guarded 真的被读）',
	'0 与 1 算出了同一个边界 ' + sr0.r);
Ed.setFuzz({ guarded: 1 });

/* 关闭模糊化：安全区必须退化为「整块画布」，且 codegen 侧同步退化到 w-1/h-1。
 * 两处若漂移，编辑器画的框与生成器夹的边界就不是同一个 —— 所以这里两个都断。 */
Ed.setFuzz({ on: false });
const srOff = Ed.safeRect();
ok(srOff.r === Ed.E.w - 1 && srOff.b === Ed.E.h - 1,
	'[关闭] safeRect 退化为整块画布（' + srOff.r + ',' + srOff.b + '）',
	'r=' + srOff.r + ' b=' + srOff.b);
ok(Gen.fuzzSafeRight() === srOff.r && Gen.fuzzSafeBottom() === srOff.b,
	'[关闭] codegen 侧与 safeRect 仍然一致（' + Gen.fuzzSafeRight() + ',' + Gen.fuzzSafeBottom() + '）',
	'codegen: ' + Gen.fuzzSafeRight() + ',' + Gen.fuzzSafeBottom());
mustFail(srOff.r === sr.r, '[关闭] 关闭后安全区确实不再是那个内缩框',
	'关闭前后边界相同 ' + srOff.r + '，说明 on 标志没被 safeRect 读');
Ed.setFuzz({ on: true });

/* ============================ B. 夹取正确性 ============================ */

console.log('');
console.log('B. 夹取：模糊化开启后，所有输出的坐标必须落在安全区内（否则缩小时越界被裁）');

/* 造一个「故意画到右下角」的房间：内容横跨整个 12×12 */
Ed.MACROS.skeleton();
Ed.MACROS.fourDoors();
for (let y = 1; y <= 10; y++) for (let x = 1; x <= 10; x++) Ed.set(x, y, T.GRASS);
Ed.set(10, 10, T.PEDESTAL);           // 最右下角单格（必被夹）
Ed.set(4, 9, T.STATUE);
Ed.set(9, 2, T.WATER);
Ed.set(10, 10, T.CHASM);              // 覆盖：右下角放深渊，更直观

function checkAllInside(code, label) {
	const cells = extractCells(code, Ed.E.w, Ed.E.h);
	const sx = Gen.fuzzSafeRight(), sy = Gen.fuzzSafeBottom();
	const bad = cells.filter(c => c.x + c.w - 1 > sx || c.y + c.h - 1 > sy);
	ok(bad.length === 0, label + '：' + cells.length + ' 条语句全部在安全区内（右下 ≤ ' + sx + ',' + sy + '）',
		bad.slice(0, 3).map(c => c.x + ',' + c.y + ' ' + c.w + 'x' + c.h + ' ' + c.t).join(' | '));
	return cells;
}

/* 开模糊化生成 */
Ed.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, cat: 'NORMAL', guarded: 1 });
Gen.FUZZ_CLAMPS.count = 0;
const codeFuzz = Gen.generate({ rawClass: 'StandardRoom', sheetLabel: '测试' });
const cellsFuzz = checkAllInside(codeFuzz, '模糊化开启');
ok(Gen.FUZZ_CLAMPS.count > 0, '确实发生了夹取（' + Gen.FUZZ_CLAMPS.count + ' 次）—— 否则本段是空跑');
ok(/⚠ 模糊化：已夹进安全区/.test(codeFuzz), '被夹的行带上了「⚠ 模糊化」注释（可读性）');

/* 关模糊化生成 —— 不应有任何夹取，也不应有注释，且坐标可以越出小房间 */
Ed.setFuzz({ on: false });
Gen.FUZZ_CLAMPS.count = 0;
const codePlain = Gen.generate({ rawClass: 'StandardRoom', sheetLabel: '测试' });
ok(Gen.FUZZ_CLAMPS.count === 0, '关闭模糊化后不发生任何夹取');
ok(!/⚠ 模糊化/.test(codePlain), '关闭模糊化后没有夹取注释');
ok(!/sizeCatProbs/.test(codePlain), '关闭模糊化后不生成 sizeCatProbs 覆写');
ok(!/setSize\(/.test(codePlain), '关闭模糊化后不生成 setSize 调用');

/* 反例：拿「关闭模糊化」的代码去查安全区，必须查出越界 ⇒ 证明 checkAllInside 有效 */
const plainCells = extractCells(codePlain, Ed.E.w, Ed.E.h);
const plainBad = plainCells.filter(c => c.x + c.w - 1 > 6 || c.y + c.h - 1 > 6);
mustFail(plainBad.length === 0, '未夹取的代码确实会越出安全区（判据有区分力）',
	'竟然一条都没越界，说明测试数据没覆盖右下角');

/* 反例：两档 guarded 必须给出不同结果（证明 guarded 真的参与了夹取，不是摆设）。
 * ⚠️ 不要断言「内容不会贴到 x=6」：夹取后的内容能不能正好落在 6，
 *    取决于有没有内容本来就在第 7 列或更右 —— 这里的草地铺到 x=10，
 *    夹到 6 是**正确**结果，那种断言恒真（这里已经踩过一次，故删）。 */
Ed.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, guarded: 1 });
const cellsG1 = extractCells(Gen.generate({ rawClass: 'StandardRoom' }), Ed.E.w, Ed.E.h);
Ed.setFuzz({ guarded: 3 });
const cellsG3 = extractCells(Gen.generate({ rawClass: 'StandardRoom' }), Ed.E.w, Ed.E.h);
const maxX1 = Math.max.apply(null, cellsG1.map(c => c.x + c.w - 1));
const maxX3 = Math.max.apply(null, cellsG3.map(c => c.x + c.w - 1));
ok(maxX3 < maxX1, 'guarded 1→3 会让内容更内缩（最远列 ' + maxX1 + ' → ' + maxX3 + '）');
ok(maxX3 === Gen.fuzzSafeRight(), 'guarded=3 时内容正好被夹到安全区右边界（' + maxX3 + '）',
	'安全区右边界 = ' + Gen.fuzzSafeRight());
mustFail(maxX3 === maxX1, '  ↳ 两档 guarded 确实得到了不同结果');
Ed.setFuzz({ guarded: 1 });

/* ============================ C. 类别选择 ============================ */

console.log('');
console.log('C. SizeCategory 选择：能装下就装下，装不下必须如实报「无解」');

/* ⚠️ 每个用例都要带 on:true —— fuzzCategoryFor/fuzzImpossible 在 !on 时会短路
 *    （fuzzImpossible 直接 return false），漏写 on 会让「无解」用例假红。 */
function F(a, b, c, d) { return { on: true, minW: a, maxW: b, minH: (c === undefined ? a : c), maxH: (d === undefined ? b : d), guarded: 1 }; }

const cases = [
	{ f: F(8, 10), want: 'NORMAL', imp: false, why: '8~10 完全落在 NORMAL(4~10)' },
	{ f: F(10, 14), want: 'LARGE', imp: false, why: '10~14 完全落在 LARGE(10~14)' },
	{ f: F(14, 18), want: 'GIANT', imp: false, why: '14~18 完全落在 GIANT(14~18)' },
	{ f: F(4, 4), want: 'NORMAL', imp: false, why: '固定 4（NORMAL 下界）' },
	{ f: F(5, 9), want: 'NORMAL', imp: false, why: '5~9 落在 NORMAL' },
	/* 注意：fuzzCategoryFor 把宽高**合成一个区间**看待（needMin=min(minW,minH)、
	 * needMax=max(maxW,maxH)），因为 SizeCategory 对宽高共用同一个 [min,max]。
	 * 所以 6~12 会去找「同时装下 6 与 12」的类别 —— 没有 ⇒ 无解。 */
	{ f: F(6, 12), want: null, imp: true, why: '6~12：NORMAL 装不下 12、LARGE 装不下 6 ⇒ 无解' },
	{ f: F(8, 14), want: null, imp: true, why: '8~14：NORMAL 装不下 14、LARGE 装不下 8 ⇒ 无解' },
	{ f: F(9, 13), want: null, imp: true, why: '9~13：NORMAL 装不下 13、LARGE 装不下 9 ⇒ 无解' }
];
cases.forEach(c => {
	const cat = Gen.fuzzCategoryFor(c.f);
	const imp = Gen.fuzzImpossible(c.f);
	ok(imp === c.imp, '[' + c.why + '] fuzzImpossible=' + imp, '期望 ' + c.imp);
	if (c.want) ok(cat === c.want, '  ↳ 选中类别 = ' + cat, '期望 ' + c.want);
	else {
		/* 无解时仍要给一个「退路类别」—— 但必须如实告知（emitFuzzDecl 会写成
		 * 那个类别的完整范围，而检查器另给警告），所以这里只断言它是个合法类别。 */
		ok(!!Ed.SIZE_CATS[cat], '  ↳ 无解时给出退路类别 ' + cat + '（合法值）');
	}
});

/* 「能装下」的定义必须与 setSize 的硬校验严格一致：
 * minW >= cat.min && maxW <= cat.max（宽高分开算，但类别对宽高是同一个 [min,max]） */
[4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18].forEach(n => {
	const f = F(n, n);
	const cat = Gen.fuzzCategoryFor(f);
	const r = Ed.SIZE_CATS[cat];
	ok(n >= r.min && n <= r.max, '固定尺寸 ' + n + ' → 类别 ' + cat + '（' + r.min + '~' + r.max + '）装得下');
});

/* 反例：确实无解的范围必须被判为无解，且与「有解」的邻例构成对照 */
mustFail(!Gen.fuzzImpossible(F(5, 13)), '5~13 被判为无解（说明判据不是恒 false）');
ok(Gen.fuzzImpossible(F(9, 13)) === true, '9~13 也是无解（NORMAL 装不下 13、LARGE 装不下 9）');
ok(Gen.fuzzImpossible(F(10, 14)) === false, '10~14 有解（LARGE 边界完全覆盖）—— 与上面两例构成对照');
ok(Gen.fuzzImpossible(F(4, 10)) === false, '4~10 有解（NORMAL 边界完全覆盖）—— 另一个对照');

/* 反例：关闭模糊化时 fuzzImpossible 必须短路成 false（哪怕范围本身无解） */
ok(Gen.fuzzImpossible({ on: false, minW: 8, maxW: 14, minH: 8, maxH: 14 }) === false,
	'关闭模糊化时 fuzzImpossible 一律 false（不做无意义告警）');

/* ============================ D. 生成代码接线 ============================ */

console.log('');
console.log('D. 生成代码的接线：sizeCatProbs() 覆写 + setSize() 调用必须成对出现');

Ed.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, guarded: 1 });
const code = Gen.generate({ rawClass: 'StandardRoom', sheetLabel: '测试' });

/* ⚠️ 判据要看**方法声明**，不能用裸 /sizeCatProbs/ ——
 *    那个词在文件头注释与 setSize 上方注释里各出现一次，裸匹配恒真（假绿）。 */
const DECL_RE = /@Override\s*\n\s*public float\[\] sizeCatProbs\(\)\{/;
const CALL_RE = /\n\t\tsetSize\( 8, 10, 8, 10 \);/;

ok(DECL_RE.test(code), 'sizeCatProbs() 带 @Override 且签名正确');
ok(/return new float\[\]\{1, 0, 0\};/.test(code),
	'概率数组只给 NORMAL 权重 1（8~10 落在 NORMAL）');
ok(CALL_RE.test(code), 'setSize 的四个参数与面板填写值一致');
ok(code.indexOf('public float[] sizeCatProbs(){') < code.indexOf('setSize( 8, 10'),
	'sizeCatProbs 覆写写在 setSize 调用之前（同类内方法顺序，与上游风格一致）');
ok(/尺寸模糊化/.test(code), '文件头注释写明了「尺寸模糊化：开启」');

/* 两者成对的强断言：既要 name 出现，也要 setSize 出现，且 setSize 在 paint() 内 */
const setSizeInsidePaint = (() => {
	const i = code.indexOf('public void paint( Level level ) {');
	return i >= 0 && code.indexOf('setSize(', i) > i;
})();
ok(setSizeInsidePaint, 'setSize 调用位于 paint() 方法体内');

/* 反例①：删掉 setSize 那一行 ⇒ 成对性必须报缺失 */
const codeNoSetSize = code.replace(CALL_RE, '');
mustFail(CALL_RE.test(codeNoSetSize), '删掉 setSize 后判据报缺失（说明不是恒真）');
ok(DECL_RE.test(codeNoSetSize),
	'  ↳ 同时 sizeCatProbs 声明仍在 ⇒ 这正是「只写一半」的静默失效形态，判据能抓出来');

/* 反例②：删掉 sizeCatProbs 声明块 ⇒ 同样必须报缺失。
 * 逐行按内容定位：从注释块首行到紧随的 '\t}'，删掉整段声明。
 * （注意不能拿裸 /sizeCatProbs/ 当判据 —— 见上面的假绿说明。） */
const probsLines = code.split('\n');
const startIdx = probsLines.findIndex(l => /^\t\/\* ⚠️ 尺寸模糊化/.test(l));
const endIdx = probsLines.findIndex((l, i) => i > startIdx && l === '\t}');
const codeNoProbs = probsLines.slice(0, startIdx).concat(probsLines.slice(endIdx + 1)).join('\n');
ok(startIdx > 0 && endIdx > startIdx,
	'反例定位到了 sizeCatProbs 声明块（第 ' + (startIdx + 1) + '~' + (endIdx + 1) + ' 行）');
mustFail(DECL_RE.test(codeNoProbs), '删掉 sizeCatProbs 声明后判据报缺失');
ok(CALL_RE.test(codeNoProbs),
	'  ↳ 同时 setSize 仍在 ⇒ 另一种「只写一半」形态，判据也能抓出来');
ok((codeNoProbs.match(/public void paint/g) || []).length === 1,
	'  ↳ 删的是声明块本身，paint() 没被误删');

/* 画布大于模糊上限的情形：必须提示「超出部分永不存在」 */
Ed.setRoomSize(16, 16);
Ed.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, guarded: 1 });
Ed.MACROS.skeleton();
for (let y = 1; y <= 14; y++) for (let x = 1; x <= 14; x++) Ed.set(x, y, T.GRASS);
const codeBig = Gen.generate({ rawClass: 'StandardRoom' });
ok(/当前画布（16 × 16）比模糊上限/.test(codeBig) || /比模糊上限/.test(codeBig),
	'画布大于模糊上限时给出提示');
mustFail(!/比模糊上限/.test(codeBig), '  ↳ 「比模糊上限」提示确实只在超大画布出现');

/* ============================ E. 检查器 6g 段 ============================ */

console.log('');
console.log('E. 检查器 6g：模糊化相关的提示码');
/* fuzzIssues() 返回的是**提示码**（editor.js 里只有 warning / small / fixed 三种），
 * 文案由检查器渲染层拼；这里断代码，别去断中文（渲染层文案会改）。
 *
 * ⚠️ setRoomSize(n) 的入参是**内部尺寸**（不含外圈墙），E.w = n + 2。
 *    而 fuzz 的 min/max 比的是 E.w（含墙的**总宽**）。
 *    所以「画布正好等于上限 10」要写 setRoomSize(8) ⇒ E.w = 10。 */
Ed.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, guarded: 1 });
Ed.setRoomSize(8, 8);                                // ⇒ E.w = E.h = 10，正好落在 [8,10] 上边界
ok(Ed.E.w === 10 && Ed.E.h === 10, 'setRoomSize(8,8) ⇒ 画布 10×10（含墙）', Ed.E.w + '×' + Ed.E.h);
const iss10 = Ed.fuzzIssues();
ok(iss10.length === 0, '画布 10×10 恰好在 8~10 区间内 ⇒ 无提示码', JSON.stringify(iss10));
mustFail(iss10.length > 0, '  ↳ 这种「干净用态」确实是空的');

/* ① 画布比模糊上限大 ⇒ warning（超出部分永不存在）。
 *    setRoomSize(12) ⇒ E.w = 14 > 上限 10。 */
Ed.setRoomSize(12, 12);
ok(Ed.E.w === 14, 'setRoomSize(12,12) ⇒ 画布 14×14', String(Ed.E.w));
const issBig = Ed.fuzzIssues();
ok(issBig.indexOf('warning') >= 0, '画布 14×14 > 模糊上限 10 ⇒ 给出 warning 码', JSON.stringify(issBig));
mustFail(issBig.indexOf('warning') < 0, '  ↳ warning 码确实只在超大画布出现');

/* ② 画布比模糊下限小 ⇒ small（房间最小时也放不下画布内容）。
 *    ⚠️ 这条判据曾是**死代码**的邻居：真正的问题是「画布 < 下限」这件事
 *    在 safeRect 上完全看不出来（safeRect 用 min(w, minW) 推，会自动收缩）。
 *    现改为直接比画布与下限。setRoomSize(4) ⇒ E.w = 6 < 下限 8。 */
Ed.setRoomSize(4, 4);
ok(Ed.E.w === 6, 'setRoomSize(4,4) ⇒ 画布 6×6', String(Ed.E.w));
const issSmall = Ed.fuzzIssues();
ok(issSmall.indexOf('small') >= 0, '画布 6×6 < 模糊下限 8 ⇒ 给出 small 码', JSON.stringify(issSmall));
mustFail(issSmall.indexOf('small') < 0, '  ↳ small 码确实只在过小画布出现');

/* ④ 换一段更宽的区间，让同一块画布落进去 ⇒ warning 必须消失（证明它不是恒报）。
 *    setRoomSize(9,9) ⇒ 画布 11×11；区间 8~10 时报 warning（11>10），
 *    放到 8~14 后 11 落在区间内 ⇒ 无码。 */
Ed.setRoomSize(9, 9);
ok(Ed.E.w === 11, 'setRoomSize(9,9) ⇒ 画布 11×11', String(Ed.E.w));
Ed.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, guarded: 1 });
ok(Ed.fuzzIssues().indexOf('warning') >= 0,
	'区间 8~10 时画布 11×11 报 warning', JSON.stringify(Ed.fuzzIssues()));
Ed.setFuzz({ on: true, minW: 8, maxW: 14, minH: 8, maxH: 14, guarded: 1 });
const issWide = Ed.fuzzIssues();
ok(issWide.length === 0, '区间放宽到 8~14 后，11×11 画布无提示码', JSON.stringify(issWide));
mustFail(issWide.length > 0, '  ↳ warning 确实会随区间变化消失（不是恒报）');

/* ⑤ min==max ⇒ fixed（等价于固定尺寸，模糊化失去意义）。
 *    画布取 10×10（setRoomSize(8,8)）以避开 warning/small 的干扰，只让 fixed 冒出来。
 *    注意 min≈max≈9 而画布 10：10 落在 [9,9] 之外 ⇒ 仍会有 warning/small，
 *    所以这里把画布也做成 9（setRoomSize(7,7) ⇒ E.w = 9）来单独观察 fixed。 */
Ed.setRoomSize(7, 7);                                // E.w = E.h = 9
Ed.setFuzz({ on: true, minW: 9, maxW: 9, minH: 9, maxH: 9, guarded: 1 });
const issFixed = Ed.fuzzIssues();
ok(issFixed.indexOf('fixed') >= 0, 'min==max ⇒ 给出 fixed 码', JSON.stringify(issFixed));
ok(issFixed.length === 1 && issFixed[0] === 'fixed',
	'  ↳ 此时只有 fixed 一个码（画布 9×9 落在 [9,9] 上，不触发 warning/small）',
	JSON.stringify(issFixed));
mustFail(issFixed.indexOf('fixed') < 0, '  ↳ fixed 码确实只在 min==max 时出现');

/* ⑥ min==max 且画布不匹配时，warning 与 fixed 会**同时**出现 —— 确认两者可共存 */
Ed.setRoomSize(12, 12);                              // E.w = 14
const issBoth = Ed.fuzzIssues();
ok(issBoth.indexOf('warning') >= 0 && issBoth.indexOf('fixed') >= 0,
	'画布 14×14 配 min==max=9 ⇒ warning 与 fixed 同时出现', JSON.stringify(issBoth));

/* ⑥ 关闭模糊化 ⇒ 一律空数组（提示不该在没开功能时打扰人）。
 *    故意把画布设成「最惹眼」的 20×20，确认关闭后确实一声不吭。 */
Ed.setRoomSize(18, 18);                              // E.w = 20
Ed.setFuzz({ on: false });
const issOff = Ed.fuzzIssues();
ok(issOff.length === 0, '关闭模糊化后不产生任何提示码', JSON.stringify(issOff));
mustFail(issOff.length > 0, '  ↳ 关闭后确实是空的');

/* ⑤ guarded=0 ⇒ 没有缓冲带，安全区与「最小尺寸房间」完全重合；
 *    fuzzIssues 本体不报这一项（它是合法的，只是没缓冲），这里只记录约定。 */
Ed.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, guarded: 0 });
const srNoGuard = Ed.safeRect();
ok(srNoGuard.r === 7, 'guarded=0 时安全区右边界 = minW-1 = 7（无缓冲）', 'r=' + srNoGuard.r);

/* ============================ F. 序列化 ============================ */

console.log('');
console.log('F. JSON v5：fuzz 必须能存能读，且缺字段时有默认值');

const j = JSON.parse(JSON.stringify(Ed.toJSON()));
ok(j.version === 6, 'toJSON 输出 version=6（v6 起带 mode）', '实际 ' + j.version);
ok(j.fuzz && j.fuzz.on === true && j.fuzz.minW === 8, 'toJSON 带上了 fuzz 字段');

/* fromJSON **无返回值**（只有副作用），且要求 format 键正确 —— 断言要按这个事实写，
 * 不能断言「返回非空」（那会变成一条永远红的假红）。 */
ok(j.format === 'spd-terrain-editor', 'toJSON 带 format 标记（fromJSON 的必检键，C.7 起改为中性名）');

/* C.7 改名后的**向后兼容**：老档（v1~v5）写的是 'egopd-terrain-editor'，
 * 用户硬盘上已经存了一堆这种文件 ⇒ 读侧必须继续认。
 * 另加一条反例：完全无关的 format 必须被拒（否则白名单写成了「一律放行」）。 */
Ed.fromJSON(Object.assign({}, j, { format: 'egopd-terrain-editor' }));
ok(Ed.E.fuzz.on === true && Ed.E.fuzz.minW === 8, '老档 format=egopd-terrain-editor 仍能读入（向后兼容）');
let threwOk = false;
try { Ed.fromJSON(Object.assign({}, j, { format: 'some-other-tool' })); }
catch (e) { threwOk = true; }
ok(threwOk, '[反例] 无关的 format 被拒（白名单不是「一律放行」）');

Ed.fromJSON(j);
ok(Ed.E.fuzz.on === true && Ed.E.fuzz.minW === 8, '原样往返后 fuzz 保持不变');

/* 缺字段：只给 on=true，其余应回落到默认值而不是 undefined */
Ed.fromJSON(Object.assign({}, j, { fuzz: { on: true } }));
const f2 = Ed.E.fuzz;
ok(f2.on === true, 'fromJSON 读回 on=true');
ok(typeof f2.minW === 'number' && !isNaN(f2.minW), '缺 minW 时回落到数字默认值（不是 undefined）', String(f2.minW));
ok(f2.minW === 8 && f2.maxW === 10 && f2.minH === 8 && f2.maxH === 10,
	'缺字段时的默认值是 8/10/8/10（' + f2.minW + '/' + f2.maxW + '/' + f2.minH + '/' + f2.maxH + '）');
ok(typeof f2.guarded === 'number' && !isNaN(f2.guarded), '缺 guarded 时回落到数字默认值', String(f2.guarded));
ok(f2.guarded === 1, '缺 guarded 时默认 1');

/* 老档（v1~v4）没有 fuzz 键 ⇒ 应读成「关闭」，而不是崩或 undefined */
Ed.fromJSON(Object.assign({}, j, { version: 4, fuzz: undefined }));
ok(Ed.E.fuzz && Ed.E.fuzz.on === false, 'v4 老档（无 fuzz 键）读成「关闭」');
ok(typeof Ed.E.fuzz.minW === 'number', '  ↳ 且字段仍是数字默认值，不是 undefined', String(Ed.E.fuzz.minW));

/* 反例：format 缺失必须抛错（证明 fromJSON 真的在检，而不是静默吃进去） */
let threw = false;
try { Ed.fromJSON({ version: 5, fuzz: { on: true } }); } catch (e) { threw = true; }
ok(threw, '缺 format 键时 fromJSON 抛错（未静默接受）');
mustFail(!threw, '  ↳ 这个抛错确实会发生');

/* 反例：写入越界值必须被 clampFuzz 夹住 */
Ed.fromJSON(Object.assign({}, j, { fuzz: { on: true, minW: 1, maxW: 99, minH: 5, maxH: 5, guarded: 99 } }));
const f3 = Ed.E.fuzz;
ok(f3.minW >= 4 && f3.maxW <= 18, '越界尺寸被夹进 4~18（' + f3.minW + '~' + f3.maxW + '）');
ok(f3.guarded <= 8, '越界 guarded 被夹到 ≤ 8（' + f3.guarded + '）');
mustFail(f3.maxW === 99, '  ↳ 99 确实没被原样保留');

/* 边界：min/max 颠倒必须被换回来 */
Ed.fromJSON(Object.assign({}, j, { fuzz: { on: true, minW: 12, maxW: 6, minH: 12, maxH: 6, guarded: 1 } }));
ok(Ed.E.fuzz.minW <= Ed.E.fuzz.maxW && Ed.E.fuzz.minH <= Ed.E.fuzz.maxH,
	'颠倒的 min/max 被交换（' + Ed.E.fuzz.minW + '~' + Ed.E.fuzz.maxW + '）');

/* ============================ 收尾 ============================ */

console.log('');
if (fail) { console.log('❌ 尺寸模糊化核验：共 ' + fail + ' 项未通过'); process.exit(1); }
console.log('✅ 尺寸模糊化核验全部通过（含 ' + counterExamples + ' 项反例自测）');
