/* 地形编辑器「房间检查器」单元测试
 *
 * 为什么需要：检查器里有几条判据在浏览器里很难用眼睛发现错（假报/漏报都像"正常"）。
 * 已踩过两个真实 bug：
 *   ① 外圈破损判定只给左右两列的门放行、上下两行忘放行 ⇒ 四向开门被误报；
 *   ② 图集覆盖检查没跳过 WATER ⇒ 误报「水没画」（水根本不由 tilemap 画）。
 * 这两条各有专门用例，且都带「反向构造」（把条件破坏掉，断言必须报）。
 *
 * 用法：node _chk/verify_terrain_checker.js
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const DIR = path.join(__dirname, '..', 'tools', 'terrain-editor');

/* 假 canvas：render.js 的 Sheet 需要 getImageData 才能跑；这里只要不报错即可。
 * 图集覆盖那条用例会单独把 solidCount 打桩，不依赖真实像素。 */
function fakeCtx() {
	return { drawImage() {}, getImageData(w, h) { return { data: new Uint8ClampedArray(w * h * 4) }; },
		clearRect() {}, save() {}, restore() {}, scale() {}, fillRect() {}, strokeRect() {},
		beginPath() {}, moveTo() {}, lineTo() {}, stroke() {}, setLineDash() {}, fill() {} };
}
const sandbox = {
	window: {},
	document: { createElement() { return { width: 0, height: 0,
		getContext() { return fakeCtx(); }, style: {}, appendChild() {}, addEventListener() {} }; },
		querySelector() { return null; }, body: { appendChild() {}, removeChild() {} } },
	console, Image: function () {}, setTimeout, clearTimeout, navigator: {},
	Blob: function () {}, URL: { createObjectURL() { return ''; }, revokeObjectURL() {} },
	FileReader: function () {}
};
sandbox.window = sandbox;
vm.createContext(sandbox);
for (const f of ['render.js', 'editor.js', 'codegen.js']) {
	vm.runInContext(fs.readFileSync(path.join(DIR, f), 'utf8'), sandbox, { filename: f });
}
const Ed = sandbox.TE_EDITOR, R = sandbox.TE_RENDER, T = Ed.T;

let fail = 0;
function ok(cond, label, extra) {
	if (cond) console.log('  ✓ ' + label);
	else { console.log('  ✗ ' + label + (extra ? '  —— ' + extra : '')); fail++; }
}
function reset(w, h) {
	Ed.setRoomSize(w, h);
	Ed.MACROS.skeleton();
}
function texts() { return Ed.check().map(m => m.text); }
function has(sub) { return texts().some(t => t.indexOf(sub) >= 0); }

console.log('=== 1. 外圈判定：门在四条边上都不该算「破损」 ===');

// 四向开门：四条边各一扇门 —— 早期 bug 会把上下两条边误报
reset(10, 10);
Ed.MACROS.fourDoors();
ok(!has('外圈有'), '四向开门（含上下两边的门）不报「外圈破损」',
	texts().join(' | '));
ok(!has('不在外圈'), '四向开门也不报「门不在外圈」');

// 反面：把底边中点那扇门改成草地 ⇒ 必须报外圈破损
reset(10, 10);
Ed.MACROS.fourDoors();
Ed.set(5, 11, T.GRASS);                       // h=12，底边 = 第 11 行
ok(has('外圈有'), '底边门被换成草地 ⇒ 报「外圈破损」（反向构造有效）');

// 反面：顶边同理
reset(10, 10);
Ed.MACROS.fourDoors();
Ed.set(5, 0, T.GRASS);
ok(has('外圈有'), '顶边门被换成草地 ⇒ 报「外圈破损」');

console.log('');
console.log('=== 2. 门位置：内圈的门必须报 ===');

reset(8, 8);
// 内圈中间放一扇门（不是开在外圈上）
Ed.set(4, 4, T.DOOR);
ok(has('不在外圈'), '内圈中央放门 ⇒ 报「门不在外圈」');

reset(8, 8);
Ed.MACROS.fourDoors();
ok(!has('不在外圈'), '门全在外圈 ⇒ 不报「门不在外圈」（反向构造有效）');

console.log('');
console.log('=== 3. 连通性与深渊 ===');

reset(8, 8);
// 用一堵墙把内圈横切成上下两半。
// ⚠️ 必须**铺满整个内部宽度**（x 到 interiorW()）：只铺到 6 会剩右侧 x=7 一条缝，
//    两半仍然连通 —— 这正是本测试第一版写错的地方（不是检查器漏报）。
for (let x = 1; x <= Ed.interiorW(); x++) Ed.set(x, 4, T.WALL);
ok(has('不连通'), '一堵墙横切整个内部 ⇒ 报「可通行格不连通」',
	texts().join(' | '));

reset(8, 8);
Ed.MACROS.fillInterior(T.CHASM);
ok(has('全是深渊'), '内圈全深渊 ⇒ 报「玩家无处落脚」');

reset(8, 8);
Ed.MACROS.fourDoors();
ok(!has('全是深渊') && !has('不连通'), '正常空房 ⇒ 不误报深渊/不连通');

console.log('');
console.log('=== 4. 图集覆盖：WATER 必须被跳过 ===');

// 打桩 solidCount：让所有帧都「有内容」，除了被点名的那几个
const realSolid = R.solidCount;
const realBase = R.baseVisualFor;
let solidStub = () => 256;
R.solidCount = (idx) => solidStub(idx);
R.baseVisualFor = (t) => (t === T.WATER ? 0 : 100 + (t | 0));

reset(8, 8);
Ed.MACROS.fourDoors();
Ed.set(3, 3, T.WATER); Ed.set(4, 3, T.WATER);
ok(!has('没有画这些地形'), 'WATER 帧为空 ⇒ 不报（水不由 tilemap 画）',
	texts().join(' | '));

// 反面：GRASS 帧为空 ⇒ 必须报
solidStub = (idx) => (idx === 100 + T.GRASS ? 0 : 256);
reset(8, 8);
Ed.MACROS.fourDoors();
Ed.set(3, 3, T.GRASS);
ok(has('没有画这些地形') && has('草地'), 'GRASS 帧为空 ⇒ 报「图集没画草地」（反向构造有效）',
	texts().join(' | '));

R.solidCount = realSolid;
R.baseVisualFor = realBase;

console.log('');
console.log('=== 5. 正常房间不产生任何提示 ===');

reset(12, 12);
Ed.MACROS.fourDoors();
ok(Ed.check().length === 0, '纯空房 + 四向开门 ⇒ 检查器零提示',
	texts().join(' | '));

console.log('');
if (fail) { console.log('❌ 共 ' + fail + ' 项未通过'); process.exit(1); }
console.log('✅ 房间检查器判据全部通过');
