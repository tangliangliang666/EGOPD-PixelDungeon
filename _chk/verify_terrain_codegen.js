/* 地形编辑器端到端核验：
 *   1) 在 Node 里加载 editor.js + codegen.js（render.js 需要假 canvas，一并垫上）
 *   2) 造一张有代表性的房间（墙 / 草 / 高草 / 水 / 深渊 / 门 / 基座 / 雕像）
 *   3) 跑 codegen 生成 Java
 *   4) **反向解释执行**生成出来的 Painter.* 调用，重算出网格，与原网格逐格比对
 *      —— 这是唯一能证明「生成代码语义正确」的办法，光看字符串没有意义
 *   5) 反例自测：故意把生成的某一行改掉，反向解释必须报不一致
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
	console,
	Image: function () { this.src = ''; },
	setTimeout, clearTimeout,
	navigator: {},
	Blob: function () {}, URL: { createObjectURL() { return ''; }, revokeObjectURL() {} },
	FileReader: function () {}
};
sandbox.window = sandbox;
vm.createContext(sandbox);

/* assets.js 也要加载 —— 它提供 window.TE_SHEETS（内置图集清单），
 * C.7 的「工具不绑定任何 mod」断言要读它。它是纯数据文件，无需 canvas。 */
for (const f of ['assets.js', 'render.js', 'editor.js', 'codegen.js']) {
	vm.runInContext(fs.readFileSync(path.join(DIR, f), 'utf8'), sandbox, { filename: f });
}
const Ed = sandbox.TE_EDITOR, Gen = sandbox.TE_GEN, R = sandbox.TE_RENDER, T = Ed.T;

/* ---- 地形名 <-> 常量 双向表（用于反向解释生成的 Terrain.XXX） ---- */
const NAME2VAL = {};
Ed.PALETTE.forEach(g => g.items.forEach(it => { NAME2VAL[it[0]] = it[1]; }));

let fail = 0;
function ok(cond, label, extra) {
	if (cond) { console.log('  ✓ ' + label); }
	else { console.log('  ✗ ' + label + (extra ? '  —— ' + extra : '')); fail++; }
}
/* 本套件其余部分习惯写 ok(...)，但 C.7 那几条断言用 eq 读起来更准
 * （要断言的正是「取出来的值等于什么」）。这里补一个薄封装。 */
function eq(got, want, label, extra) {
	ok(got === want, label,
		'期望 ' + JSON.stringify(want) + '，实得 ' + JSON.stringify(got) +
		(extra ? '  —— ' + extra : ''));
}

/* =============================== 1. 造房间 =============================== */

Ed.setRoomSize(12, 12);                       // 内部 12x12，含墙 14x14
const w = Ed.E.w, h = Ed.E.h;

function put(x, y, v) { Ed.set(x, y, v); }

// 外圈：默认墙 + 四向门
Ed.MACROS.skeleton();
Ed.MACROS.fourDoors();
// 一扇隐藏门 + 一扇上锁门（验证外圈非墙内容也被生成）
put(3, 0, T.SECRET_DOOR);
put(10, h - 1, T.LOCKED_DOOR);

// 内部：一块 5x4 草地（应被合并成一个矩形 fill）
for (let y = 2; y <= 5; y++) for (let x = 2; x <= 6; x++) put(x, y, T.GRASS);
// 一条 1 格宽的竖直水渠（应生成 fill(...,1,4,WATER)）
for (let y = 8; y <= 11; y++) put(11, y, T.WATER);
// 一条横向高草带（4 格，应合并成 4x1）
for (let x = 8; x <= 11; x++) put(x, 2, T.HIGH_GRASS);
// 右下角一块 3x3 深渊
for (let y = 8; y <= 10; y++) for (let x = 7; x <= 9; x++) put(x, y, T.CHASM);
// 零散单格：基座、雕像、陷阱（验证退化到 Painter.set）
put(4, 9, T.PEDESTAL);
put(5, 9, T.STATUE);
put(6, 9, T.TRAP);
// 一格犁过的草孤点
put(12, 6, T.FURROWED_GRASS);
// 一格余烬孤点
put(2, 11, T.EMBERS);

const ORIG = Array.from(Ed.E.map);

/* =============================== 2. 生成 =============================== */

const code = Gen.generate({
	pkg: 'com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard',
	rawClass: 'StandardRoom',
	sheetLabel: '测试图集'
});

console.log('生成代码 ' + code.split('\n').length + ' 行；' +
	'Painter.fill 调用 ' + (code.match(/Painter\.fill\(/g) || []).length + ' 个，' +
	'Painter.set 调用 ' + (code.match(/Painter\.set\(/g) || []).length + ' 个');
console.log('');

/* =============================== 3. 反向解释 =============================== */

/* 语法上接受生成器会产出的三种调用：
 *   Painter.fill( level, this, Terrain.X );
 *   Painter.fill( level, this, 1, Terrain.X );
 *   Painter.fill( level, left+a, top+b, w, h, Terrain.X );
 *   Painter.set( level, left+a, top+b, Terrain.X );
 *   Painter.fill( level, left+a, top+b, w, 1, Terrain.X );
 *   Painter.fill( level, left+a, top+b, 1, h, Terrain.X );
 * 外加 for(Room.Door door : connected.values()) 两行（忽略）。
 */
function interpret(src) {
	const grid = new Int32Array(w * h).fill(-1);      // -1 = 未写
	const lines = src.split('\n');
	const log = [];

	function setRange(l, t, ww, hh, v) {
		for (let y = t; y < t + hh; y++)
			for (let x = l; x < l + ww; x++) {
				if (x < 0 || y < 0 || x >= w || y >= h) throw new Error('越界写入 ' + x + ',' + y);
				grid[y * w + x] = v;
			}
	}

	for (const raw of lines) {
		const s = raw.trim();
		if (!s.startsWith('Painter.')) continue;

		let m;
		// 整间铺墙：Painter.fill( level, this, Terrain.WALL );
		if ((m = s.match(/^Painter\.fill\(\s*level,\s*this,\s*Terrain\.(\w+)\s*\);/))) {
			setRange(0, 0, w, h, NAME2VAL[m[1]]);
			log.push('whole:' + m[1]);
			continue;
		}
		// 掏空内圈：Painter.fill( level, this, 1, Terrain.EMPTY );
		if ((m = s.match(/^Painter\.fill\(\s*level,\s*this,\s*1\s*,\s*Terrain\.(\w+)\s*\);/))) {
			setRange(1, 1, w - 2, h - 2, NAME2VAL[m[1]]);
			log.push('inner:' + m[1]);
			continue;
		}
		// 带坐标的 fill：Painter.fill( level, left + A, top + B, WW, HH, Terrain.X );
		if ((m = s.match(/^Painter\.fill\(\s*level,\s*left\s*\+\s*(\d+),\s*top\s*\+\s*(\d+),\s*(\d+),\s*(\d+),\s*Terrain\.(\w+)\s*\);/))) {
			setRange(+m[1], +m[2], +m[3], +m[4], NAME2VAL[m[5]]);
			log.push('fill:' + m[1] + ',' + m[2] + ' ' + m[3] + 'x' + m[4] + ' ' + m[5]);
			continue;
		}
		// 带坐标的 fill 宽度句式（生成器不产出，但保留以防）
		if ((m = s.match(/^Painter\.fill\(\s*level,\s*left\s*\+\s*(\d+),\s*top\s*\+\s*(\d+),\s*(\d+),\s*1,\s*Terrain\.(\w+)\s*\);/))) {
			setRange(+m[1], +m[2], +m[3], 1, NAME2VAL[m[4]]);
			continue;
		}
		// 带坐标的 set：Painter.set( level, left + A, top + B, Terrain.X );
		if ((m = s.match(/^Painter\.set\(\s*level,\s*left\s*\+\s*(\d+),\s*top\s*\+\s*(\d+),\s*Terrain\.(\w+)\s*\);/))) {
			setRange(+m[1], +m[2], 1, 1, NAME2VAL[m[3]]);
			log.push('set:' + m[1] + ',' + m[2] + ' ' + m[3]);
			continue;
		}
		// 其它 Painter 调用：报出来，别静默放过
		log.push('UNHANDLED: ' + s);
	}
	return { grid, log };
}

const interp = interpret(code);

const unhandled = interp.log.filter(l => l.startsWith('UNHANDLED'));
console.log('反向解释：识别 ' + interp.log.length + ' 条语句');
ok(unhandled.length === 0, '生成的每条 Painter 语句都能被解释', unhandled.slice(0, 3).join(' | '));
ok(interp.log.filter(l => l.startsWith('whole:')).length === 1, '恰好 1 次整间铺墙');
ok(interp.log.filter(l => l.startsWith('inner:')).length === 1, '恰好 1 次内圈掏空');

/* 逐格比对 */
let diff = [];
for (let i = 0; i < w * h; i++) {
	if (interp.grid[i] !== ORIG[i])
		diff.push({ p: i, x: i % w, y: Math.floor(i / w), got: interp.grid[i], exp: ORIG[i] });
}
console.log('');
console.log('逐格比对 ' + (w * h) + ' 格：一致 ' + (w * h - diff.length) + '，不一致 ' + diff.length);
if (diff.length) {
	const name = v => { for (const k in NAME2VAL) if (NAME2VAL[k] === v) return k; return String(v); };
	diff.slice(0, 15).forEach(d =>
		console.log('  (' + d.x + ',' + d.y + ') 生成=' + name(d.got) + ' 原图=' + name(d.exp)));
}
ok(diff.length === 0, '生成代码重算出的网格与原网格逐格一致');

/* =============================== 4. 矩形合并质量 =============================== */

const fills = (code.match(/Painter\.fill\(/g) || []).length;
const sets = (code.match(/Painter\.set\(/g) || []).length;
ok(fills + sets < 40, '语句数 ' + (fills + sets) + ' < 40（矩形合并生效；不合并会逼近 196）',
	'fill=' + fills + ' set=' + sets);

/* 5x4 草地必须被合并成 1 个 fill；检查源码里存在 "5, 4, Terrain.GRASS" */
ok(/Painter\.fill\( level, left \+ 2, top \+ 2, 5, 4, Terrain\.GRASS \);/.test(code),
	'5×4 草地区块被合并为单条 fill');
ok(/Terrain\.WATER \);/.test(code) && /, 1, ?\d+, Terrain\.WATER \);|Painter\.fill\( level, left \+ 11, top \+ 8, 1, 4, Terrain\.WATER \);/.test(code),
	'1 格宽水渠以列填充形式生成');
ok(/Terrain\.SECRET_DOOR \);/.test(code), '外圈隐藏门被生成');
ok(/Terrain\.LOCKED_DOOR \);/.test(code), '外圈上锁门被生成');
ok(/door\.set\( Room\.Door\.Type\.REGULAR \);/.test(code), '结尾保留门类型设置循环');

/* =============================== 5. 反例自测 =============================== */

console.log('');
console.log('反例自测：把生成的某一格改成别的 Terrain，反向解释必须报不一致…');
const broken = code.replace('Painter.set( level, left + 4, top + 9, Terrain.PEDESTAL );',
	'Painter.set( level, left + 4, top + 9, Terrain.EMPTY );');
const didBreak = broken !== code;
ok(didBreak, '反例替换命中目标行');
if (didBreak) {
	const bi = interpret(broken);
	let bd = 0;
	for (let i = 0; i < w * h; i++) if (bi.grid[i] !== ORIG[i]) bd++;
	ok(bd > 0, '反例被检出（' + bd + ' 处不一致）⇒ 判据有效', bd === 0 ? '判据恒真，必须修脚本' : '');
}

/* =============================== 6. 语法粗检 =============================== */

console.log('');
const brace = (code.match(/\{/g) || []).length - (code.match(/\}/g) || []).length;
ok(brace === 0, '花括号配平（差 ' + brace + '）');

/* 括号检查必须先剥掉注释 —— 注释里的中文全角「）」会被朴素正则误当成右括号 */
const codeNoComment = code.split('\n')
	.map(l => l.replace(/^\s*\/\/.*$/, '')          // 整行注释
		.replace(/\s\/\/[^"']*$/, ''))              // 行尾注释（生成器不产出字符串，安全）
	.join('\n');
const oP = (codeNoComment.match(/\(/g) || []).length;
const cP = (codeNoComment.match(/\)/g) || []).length;
ok(oP === cP, '圆括号配平（' + oP + '/' + cP + '，已剔除注释）',
	'若不剔除注释，中文全角「）」会被误计');
ok(/^package [\w.]+;$/m.test(code), 'package 语句存在');
ok(/^public class \w+ extends \w+ \{$/m.test(code), '类声明形式正确');
ok(/^\t@Override$/m.test(code) && /\tpublic void paint\( Level level \) \{/.test(code),
	'paint 方法签名正确');

/* 每行 Painter 语句必须以分号结尾 */
const badSemi = code.split('\n').filter(l =>
	l.trim().startsWith('Painter.') && !l.trim().endsWith(';'));
ok(badSemi.length === 0, '所有 Painter 语句以分号结尾', badSemi.slice(0, 2).join(' | '));

/* ============================================================
 * C.7 解耦：本工具不绑定任何 mod
 *
 * 三条都必须成立，否则「拿去给别的 SPD 项目用」就会带上本项目的痕迹：
 *   ① 包名可配置（空/纯空格 => 回退通用 SPD 路径；给了就用给的）
 *   ② 注册清单**不含**写死的项目数字（35 项 / float[27]）
 *   ③ 生成物与清单里**没有** EGOPD 字样
 * ============================================================ */

const rePkg = /^package ([\w.]+);$/m;

/* ①a 空包名 -> 通用兜底 */
const codeEmptyPkg = Gen.generate({ pkg: '', rawClass: 'StandardRoom' });
eq(rePkg.exec(codeEmptyPkg)[1],
	'com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard',
	'包名为空时回退到通用 SPD 路径');

/* ①b 纯空格也必须回退（`opts.pkg || DEFAULT` 会被 '   ' 骗过去） */
const codeBlankPkg = Gen.generate({ pkg: '   ', rawClass: 'StandardRoom' });
eq(rePkg.exec(codeBlankPkg)[1],
	'com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard',
	'包名只写空格时也回退（trim 后才判空）',
	'用 `opts.pkg || X` 判空会让纯空格通过，包名那一行就变成空白');

/* ①c 自定义包名必须被采纳 */
const codeMyPkg = Gen.generate({ pkg: 'com.example.mymod.levels.rooms', rawClass: 'StandardRoom' });
eq(rePkg.exec(codeMyPkg)[1], 'com.example.mymod.levels.rooms',
	'自定义包名被原样采纳（换个项目不用改代码）');

/* ② 注册清单不含写死的项目数字 */
const notes = Gen.registrationNotes();
ok(!/35/.test(notes), '清单里没有写死的「35 项」',
	'写死项目数字 => 换个项目生成出来就是错的');
ok(!/float\[27\]/.test(notes), '清单里没有写死的 float[27]');
ok(/<深度数>/.test(notes), '清单用 <深度数> 占位取代写死数字');
ok(/rooms\.size\(\)/.test(notes), '清单给的是**判据**（长度须等于 rooms.size()）而不是具体值');

/* ③ 生成物与清单都不得出现 EGOPD 字样 */
ok(!/EGOPD/.test(code), '生成的 Java 代码里没有 EGOPD 字样');
ok(!/EGOPD/.test(notes), '注册清单里没有 EGOPD 字样');
const builtinSheetJson = JSON.stringify(sandbox.TE_SHEETS || []);
ok(!/EGOPD/.test(builtinSheetJson), '内置图集清单里没有 EGOPD 字样');

/* ④ 内置图集只收原版 5 个区域（项目特有图集走外部导入） */
ok(!/tiles_lob/.test(builtinSheetJson),
	'内置图集清单不含 tiles_lob（项目特有图集走外部导入）',
	'lob 是本项目 27 层的图集，内置它等于把工具绑死在 EGOPD 上');
eq((sandbox.TE_SHEETS || []).length, 5, '内置图集恰好 5 张（原版 5 个区域）');

console.log('');
if (fail) { console.log('❌ 共 ' + fail + ' 项未通过'); process.exit(1); }
console.log('✅ 代码生成器端到端核验全部通过');
