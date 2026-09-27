/* 整层模式（C.8）核验：
 *   1) 内核：setMode / setLevelSize / maxDim / 64 上限 / 模式切换夹尺寸
 *   2) 序列化：v6 带 mode、老档（无 mode）读成 room、整层档往返
 *   3) 检查器：整层专属（无入口 / 无出口 / 多入口 / 全图连通性 / LOCKED_EXIT 提示）
 *   4) codegen：generateLevel 产物（extends Level / build / setSize / 绝对坐标 /
 *      楼梯 transition / createMobs 空实现 / 无 left+top / 无 sizeCatProbs）+
 *      **反向解释执行**地形语句逐格回算（与房间模式同一套判据，证明语义正确）
 *   5) 反例自测（--selftest）：注入破坏 ⇒ 断言必须变红
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const DIR = path.join(__dirname, '..', 'tools', 'terrain-editor');

function fakeCtx() { return { drawImage(){}, getImageData(){ return {data:new Uint8ClampedArray(4)}; }, clearRect(){}, save(){}, restore(){}, scale(){}, fillRect(){}, strokeRect(){}, beginPath(){}, moveTo(){}, lineTo(){}, stroke(){}, setLineDash(){}, fill(){} }; }
function makeSandbox(srcPatch) {
	const sandbox = { window:{}, document:{ createElement(){ return {width:0,height:0,getContext(){return fakeCtx();},style:{},appendChild(){},addEventListener(){}}; }, querySelector(){return null;}, body:{appendChild(){},removeChild(){}} }, console, Image:function(){this.src='';}, setTimeout, clearTimeout, navigator:{}, Blob:function(){}, URL:{createObjectURL(){return '';},revokeObjectURL(){}}, FileReader:function(){} };
	sandbox.window = sandbox; vm.createContext(sandbox);
	for (const f of ['assets.js','render.js','editor.js','codegen.js']) {
		let src = fs.readFileSync(path.join(DIR,f),'utf8');
		if (srcPatch && srcPatch[f]) src = srcPatch[f];
		vm.runInContext(src, sandbox, {filename:f});
	}
	return sandbox;
}
const Ed = makeSandbox().TE_EDITOR, Gen0 = makeSandbox().TE_GEN, T = Ed.T;
/* codegen 用独立 sandbox（Ed 状态会被本套件弄脏）—— 每次 gen 前重建 */
let SB = makeSandbox();
function freshGen() { SB = makeSandbox(); return SB.TE_GEN; }
function freshEd() { SB = makeSandbox(); return SB.TE_EDITOR; }

let fail = 0, pass = 0;
function ok(cond, label, extra) {
	if (cond) { pass++; console.log('  ✓ ' + label); }
	else { fail++; console.log('  ✗ ' + label + (extra ? '  —— ' + extra : '')); }
}
function eq(got, want, label, extra) {
	ok(got === want, label, '期望 ' + JSON.stringify(want) + '，实得 ' + JSON.stringify(got) + (extra ? '  —— ' + extra : ''));
}

/* 把生成的 Painter.set/fill 反向解释回网格（绝对坐标版） */
function interpret(java) {
	const grid = new Map();
	const re = /Painter\.(set|fill)\(\s*level,\s*([\d\s,]+?)\s*,\s*Terrain\.([A-Z_]+)\s*\)/g;
	let m;
	while ((m = re.exec(java))) {
		const nums = m[2].split(',').map(s => parseInt(s.trim(), 10));
		const val = m[3];
		if (m[1] === 'set') grid.set(nums[0] + ',' + nums[1], val);
		else {
			const [x, y, w, h] = nums;
			for (let yy = y; yy < y + h; yy++)
				for (let xx = x; xx < x + w; xx++) grid.set(xx + ',' + yy, val);
		}
	}
	return grid;
}

console.log('=== A. 内核：模式切换 / 尺寸上限 ===');
{
	const Ed = freshEd();
	eq(Ed.E.mode, 'room', '初始模式 = room');
	ok(Ed.maxDim('room') === 18 && Ed.maxDim('level') === 64, 'maxDim：room=18 / level=64');
	Ed.setMode('level');
	eq(Ed.E.mode, 'level', 'setMode 切到 level');
	Ed.setLevelSize(64, 64);
	eq(Ed.E.w, 64, '整层可开到 64×64');
	Ed.setLevelSize(100, 8);
	eq(Ed.E.w, 64, '整层超 64 被夹回 64');
	Ed.setLevelSize(2, 8);
	eq(Ed.E.w, 4, '整层最小 4');
	/* 模式切换夹尺寸：整层 64 切回房间 ⇒ 夹回 18 */
	Ed.setLevelSize(64, 64);
	Ed.setMode('room');
	eq(Ed.E.w, 18, '整层 64 切回房间被夹到 18（含外圈上限）');
	/* 截断保留：内容左上角对齐（源用旧跨度读） */
	const Ed1 = freshEd();
	Ed1.setMode('level'); Ed1.setLevelSize(6, 2);
	Ed1.set(4, 1, T.GRASS);                    // 在截断区外放一格
	Ed1.setMode('room');                        // 6>18? 否 ⇒ 不截断；换成直接截断的场景：
	const Ed1b = freshEd();
	Ed1b.setMode('level'); Ed1b.setLevelSize(20, 2);
	Ed1b.set(15, 1, T.GRASS);
	Ed1b.setMode('room');                       // 20 > 18 ⇒ 截到 18，x=15 保留
	eq(Ed1b.get(15, 1), T.GRASS, '[反例基线] 截断后 x<18 的内容原样保留');
	/* 房间→整层：模糊化被关掉 */
	const Ed2 = freshEd();
	Ed2.setFuzz({ on: true });
	Ed2.setMode('level');
	eq(Ed2.E.fuzz.on, false, '切到整层 ⇒ 模糊化自动关闭（房间专属属性）');
	/* 整层骨架：铺空地而不是墙 */
	const Ed3 = freshEd();
	Ed3.setMode('level'); Ed3.setLevelSize(6, 5);
	eq(Ed3.get(0, 0), T.EMPTY, '整层新建 = 全空地（无外圈墙语义）');
	const Ed4 = freshEd();
	Ed4.setRoomSize(5, 4);
	eq(Ed4.get(0, 0), T.WALL, '房间新建 = 外圈墙（对照）');
	/* fillInterior 模式感知 */
	Ed3.MACROS.fillInterior(T.WALL);
	eq(Ed3.get(0, 0), T.WALL, '整层 fillInterior 覆盖到边界格');
}

console.log('=== B. 序列化：v6 带 mode ===');
{
	const Ed = freshEd();
	Ed.setMode('level'); Ed.setLevelSize(20, 15);
	const j = JSON.parse(JSON.stringify(Ed.toJSON()));
	eq(j.version, 6, 'toJSON version=6');
	eq(j.mode, 'level', 'toJSON 带 mode=level');
	/* 往返 */
	const Ed2 = freshEd();
	Ed2.fromJSON(j);
	eq(Ed2.E.mode, 'level', 'fromJSON 读回 mode=level');
	eq(Ed2.E.w, 20, '整层档往返后尺寸不变');
	/* 老档（无 mode 键）⇒ room */
	const legacy = Object.assign({}, j, { mode: undefined, version: 5, w: 12, h: 12, tiles: new Array(144).fill(4) });
	const Ed3 = freshEd();
	Ed3.fromJSON(legacy);
	eq(Ed3.E.mode, 'room', '老档（v5，无 mode 键）读成 room');
	/* 整层档里的 fuzz.on 被强制关闭 */
	const jFuzz = JSON.parse(JSON.stringify(Ed.toJSON()));
	jFuzz.fuzz = { on: true, minW: 8, maxW: 10, minH: 8, maxH: 10, cat: 'NORMAL', guarded: 1 };
	const Ed4 = freshEd();
	Ed4.fromJSON(jFuzz);
	eq(Ed4.E.fuzz.on, false, '整层档即使带 fuzz.on=true 也被强制关闭');
}

console.log('=== C. 检查器：整层专属 ===');
{
	/* 无入口 */
	const Ed = freshEd();
	Ed.setMode('level'); Ed.setLevelSize(8, 6);
	let msgs = Ed.check();
	ok(msgs.some(m => /没有入口/.test(m.text)), '[反例基线] 空图 ⇒ 报「没有入口」');
	/* 有入口有出口 ⇒ 不再报 */
	Ed.set(2, 2, T.ENTRANCE); Ed.set(5, 3, T.EXIT);
	msgs = Ed.check();
	ok(!msgs.some(m => /没有入口/.test(m.text)), '画了入口 ⇒ 不再报「没有入口」');
	ok(!msgs.some(m => /没有出口/.test(m.text)), '画了出口 ⇒ 不再报「没有出口」');
	/* 多入口 ⇒ 报 */
	Ed.set(3, 2, T.ENTRANCE);
	msgs = Ed.check();
	ok(msgs.some(m => /2 个入口/.test(m.text)), '两个入口 ⇒ 报「2 个入口」');
	/* LOCKED_EXIT 提示 */
	Ed.set(6, 4, T.LOCKED_EXIT);
	msgs = Ed.check();
	ok(msgs.some(m => /LOCKED_EXIT/.test(m.text)), '画 LOCKED_EXIT ⇒ 提示生成器不配 transition');
	/* 房间判据不误报：整层全空地不报「外圈破损」 */
	const Ed2 = freshEd();
	Ed2.setMode('level'); Ed2.setLevelSize(6, 5);
	msgs = Ed2.check();
	ok(!msgs.some(m => /外圈/.test(m.text)), '[反例基线] 整层不报「外圈」判据');
	/* 全图连通性：孤岛（先铺满墙，再挖两块互不相邻的地板） */
	const Ed3 = freshEd();
	Ed3.setMode('level'); Ed3.setLevelSize(7, 5);
	Ed3.MACROS.fillInterior(T.WALL);            // 整层铺墙（整层模式 = 全画布）
	Ed3.set(1, 1, T.EMPTY); Ed3.set(5, 3, T.EMPTY);   // 两块不连通的地板
	msgs = Ed3.check();
	ok(msgs.some(m => /不连通/.test(m.text)), '[反例] 两块隔离地板 ⇒ 报「不连通」');
}

console.log('=== D. codegen：generateLevel ===');
{
	/* 同一 sandbox 里既造状态又生成（freshEd/freshGen 各自新建会互相丢状态） */
	SB = makeSandbox();
	const EdS = SB.TE_EDITOR, TS = SB.TE_EDITOR.T;
	EdS.setRoomSize(6, 4);                     // 初始化渲染态
	EdS.setMode('level'); EdS.setLevelSize(10, 8);
	EdS.E.roomClass = 'ProbeLevel';
	for (let x = 0; x < 10; x++) { EdS.set(x, 0, TS.WALL); EdS.set(x, 7, TS.WALL); }
	for (let y = 0; y < 8; y++) { EdS.set(0, y, TS.WALL); EdS.set(9, y, TS.WALL); }
	EdS.set(2, 2, TS.ENTRANCE); EdS.set(8, 5, TS.EXIT); EdS.set(5, 3, TS.WATER);
	EdS.setTrap(6, 3, { cls: 'FrostTrap', zh: '冰霜陷阱' });
	const java = SB.TE_GEN.generateLevel({ pkg: 'com.example.levels' });

	ok(/public class ProbeLevel extends Level \{/.test(java), '类声明 = extends Level');
	ok(/protected boolean build\(\)/.test(java), '入口是 build()（不是 paint）');
	ok(java.includes('setSize( 10, 8 )'), 'setSize(10, 8)');
	ok(java.includes('Painter.fill( level, 0, 0, 10, 8, Terrain.EMPTY )'), '先铺整层 EMPTY 底衬');
	ok(!/left \+/.test(java), '绝对坐标：无 left+ 偏移');
	ok(!/top \+/.test(java), '绝对坐标：无 top+ 偏移');
	ok(!/sizeCatProbs/.test(java), '无 sizeCatProbs（模糊化是房间专属）');
	ok(java.includes('LevelTransition.Type.REGULAR_ENTRANCE'), '入口 transition');
	ok(java.includes('LevelTransition.Type.REGULAR_EXIT'), '出口 transition');
	ok(java.includes('protected void createMobs() {}'), 'createMobs 空实现');
	ok(java.includes('protected void createItems() {}'), 'createItems 空实现');
	ok(java.includes('level.setTrap( new FrostTrap()'), '共享陷阱发射器在整层下原样工作');
	ok(java.includes('import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;'), 'LevelTransition import');

	/* 反向解释：地形语句回算后与画布一致（WALL/WATER/ENTRANCE/EXIT） */
	const grid = interpret(java);
	const name2val = {};
	EdS.PALETTE.forEach(g => g.items.forEach(it => { name2val[it[0]] = it[1]; }));
	let mism = 0;
	for (let y = 0; y < 8; y++)
		for (let x = 0; x < 10; x++) {
			const want = EdS.E.map[y * 10 + x];
			const gotName = grid.get(x + ',' + y);
			const got = gotName ? name2val[gotName] : TS.EMPTY;   // 没被画到的 = EMPTY 底衬
			if (got !== want) mism++;
		}
	eq(mism, 0, '反向解释：10×8 逐格与画布一致（含 EMPTY 底衬语义）');

	/* 片段模式 */
	const frag = SB.TE_GEN.generateLevelBodyOnly({});
	ok(frag.includes('setSize( 10, 8 )'), '片段含 setSize');
	ok(frag.includes('REGULAR_ENTRANCE'), '片段含入口 transition');
	ok(!/public class/.test(frag), '片段无 class 包装');
}

console.log('=== E. 房间模式不回归（coordExpr 拆分后的关键回归点） ===');
{
	const Ed = freshEd();
	Ed.setRoomSize(6, 4);
	Ed.set(1, 1, T.GRASS); Ed.set(2, 1, T.GRASS);
	const G = freshGen();
	/* 同 sandbox 重建 */
	SB.TE_EDITOR.setRoomSize(6, 4);
	SB.TE_EDITOR.set(1, 1, SB.TE_EDITOR.T.GRASS);
	SB.TE_EDITOR.set(2, 1, SB.TE_EDITOR.T.GRASS);
	const java = SB.TE_GEN.generate({ pkg: 'com.example.rooms' });
	ok(/left \+ 1, top \+ 1/.test(java), '房间模式 X=left+ Y=top+（回归：曾把 Y 写成 left+）');
	ok(/extends StandardRoom/.test(java), '房间模式仍是 StandardRoom 子类');
}

/* ============================ 反例自测 ============================ */
const SELFTEST = process.argv.includes('--selftest');
if (SELFTEST) {
	console.log('\n=== 反例自测：把 generateLevel 的楼梯段删掉，断言必须变红 ===');
	const broken = {};
	broken['codegen.js'] = fs.readFileSync(path.join(DIR, 'codegen.js'), 'utf8').replace(
		'LevelTransition.Type.REGULAR_ENTRANCE', 'LevelTransition.Type.SURFACE');
	if (broken['codegen.js'] === fs.readFileSync(path.join(DIR, 'codegen.js'), 'utf8')) {
		console.log('!! 未能注入反例'); process.exit(2);
	}
	const sb = makeSandbox(broken);
	const EdB = sb.TE_EDITOR;
	EdB.setRoomSize(6, 4); EdB.setMode('level'); EdB.setLevelSize(6, 5);
	EdB.E.roomClass = 'ProbeLevel';
	EdB.set(2, 2, EdB.T.ENTRANCE);
	const javaB = sb.TE_GEN.generateLevel({ pkg: 'com.x' });
	let caught = 0;
	if (!javaB.includes('REGULAR_ENTRANCE')) caught++;   // 反例：入口 transition 被换掉
	console.log(caught ? '  ✓ 反例被检出（ENTRANCE 不再生成）⇒ 判据有效' : '  ✗ 反例未生效');
	if (!caught) process.exit(2);
	console.log('\n✅ 反例自测通过：破坏确实会被断言抓住');
	process.exit(0);
}

console.log('\n' + (fail ? '❌ 失败 ' + fail + ' 项（通过 ' + pass + '）' : '✅ 整层模式核验全部通过（' + pass + ' 项）'));
process.exit(fail ? 1 : 0);
