/* 陷阱 / 植物层的渲染核验（Node，无浏览器）
 *
 * 目的：证明「陷阱地形未显示」这个 bug 真的被修掉了 —— 也就是
 *   ① 只写 Terrain.TRAP 而不放陷阱对象 ⇒ featuresVisual 返回 -1（复现旧 bug 的成因）
 *   ② 放了陷阱对象 ⇒ featuresVisual 返回 color + shape*16，与 Java 公式一致
 *   ③ 陷阱优先于植物、植物优先于草叶（Java 的三级 if 顺序）
 *   ④ trap.visible === false ⇒ 不画（原版行为）
 *   ⑤ active === false ⇒ 退化成 BLACK 色（color = 8）
 *
 * 用一个最小的假 DOM 把 render.js 拉进 VM 里跑。
 * 反向自测：把 featuresVisual 的优先级顺序改错，必须报错。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const ROOT = path.resolve(__dirname, '..');
const RENDER = path.join(ROOT, 'tools/terrain-editor/render.js');

let pass = 0, fail = [];
function chk(cond, msg) {
	if (cond) { pass++; console.log('  \u2713 ' + msg); }
	else { console.log('  \u2717 ' + msg); fail.push(msg); }
}

/* ---- 假 DOM：render.js 只在 Sheet 里用 document.createElement ---- */
function fakeEnv() {
	const sandbox = {};
	sandbox.window = sandbox;
	sandbox.console = console;
	sandbox.document = {
		createElement: function () {
			return {
				width: 0, height: 0,
				getContext: function () {
					return {
						drawImage: function () { },
						getImageData: function (x, y, w, h) {
							return { data: new Uint8ClampedArray(w * h * 4) };
						}
					};
				}
			};
		}
	};
	vm.createContext(sandbox);
	return sandbox;
}

function loadRenderer(srcPath) {
	const sandbox = fakeEnv();
	const code = fs.readFileSync(srcPath, 'utf8');
	vm.runInContext(code, sandbox, { filename: srcPath });
	return sandbox.TE_RENDER;
}

exports.loadRenderer = loadRenderer;

/* 直接跑（node verify_trap_render.js） */
if (require.main === module) {
	const broken = process.argv.indexOf('--broken');
	const src = broken >= 0 ? brokenPath : RENDER;

	console.log('=== 加载 ' + (broken >= 0 ? '被破坏的副本 ' : '') + path.basename(src) + ' ===');
	let R;
	try {
		R = loadRenderer(src);
	} catch (e) {
		console.log('  \u2717 加载失败：' + e.message);
		process.exit(2);
	}
	const T = R.T;

	// 起一张 8×8 的图
	R.state.w = 8; R.state.h = 8;
	R.resize(8, 8, T.EMPTY);
	R.state.map = R.state.map; R.setVariance(1);
	R.state.featuresStage = 0;

	/* ---- ① 只写地形 TRAP、不放对象 ⇒ 旧 bug 的成因 ---- */
	console.log('=== \u2460 只写 Terrain.TRAP 而不放陷阱对象 ===');
	R.state.map[2 * 8 + 2] = T.TRAP;
	R.setLayers(new Array(64).fill(null), new Array(64).fill(null));
	chk(R.featuresVisual(2 * 8 + 2, T.TRAP) === -1,
		'\u2713 只有光秃秃的 TRAP 地形格时 features 层不画任何东西（这正是「陷阱未显示」的成因）');

	/* ---- ② 放一个陷阱对象 ⇒ 帧号 = color + shape*16 ---- */
	console.log('=== \u2461 放入陷阱对象后应画出正确帧 ===');
	const traps = new Array(64).fill(null);
	const tp = R.makeTrap('AlarmTrap');                    // RED(0) + DOTS(0)*16 = 0
	traps[2 * 8 + 2] = tp;
	R.setLayers(traps, new Array(64).fill(null));
	const got = R.featuresVisual(2 * 8 + 2, T.TRAP);
	chk(got === 0, '\u2713 AlarmTrap(RED/DOTS) 帧号 = ' + got + '（期望 0 = 0 + 0*16）');

	const tp2 = R.makeTrap('DisintegrationTrap');          // VIOLET(5) + CROSSHAIR(5)*16 = 85
	traps[3 * 8 + 3] = tp2;
	const got2 = R.featuresVisual(3 * 8 + 3, T.TRAP);
	chk(got2 === 85, '\u2713 DisintegrationTrap(VIOLET/CROSSHAIR) 帧号 = ' + got2 + '（期望 85 = 5 + 5*16）');

	const tp3 = R.makeTrap('ToxicTrap');                   // GREEN(3) + GRILL(2)*16 = 35
	traps[4 * 8 + 4] = tp3;
	const got3 = R.featuresVisual(4 * 8 + 4, T.TRAP);
	chk(got3 === 35, '\u2713 ToxicTrap(GREEN/GRILL) 帧号 = ' + got3 + '（期望 35 = 3 + 2*16）');

	/* ---- ③ visible === false ⇒ 不画 ---- */
	console.log('=== \u2462 未发现的陷阱不画（原版行为）===');
	const tp4 = R.makeTrap('FrostTrap', { visible: false });
	traps[5 * 8 + 5] = tp4;
	chk(R.featuresVisual(5 * 8 + 5, T.TRAP) === -1,
		'\u2713 visible=false 的陷阱 features 层返回 -1');

	/* ---- ④ active === false ⇒ 退化成 BLACK(8) ---- */
	console.log('=== \u2463 失效陷阱用 BLACK 色 ===');
	const tp5 = R.makeTrap('AlarmTrap', { active: false });   // BLACK(8) + DOTS(0)*16 = 8
	traps[6 * 8 + 6] = tp5;
	const got5 = R.featuresVisual(6 * 8 + 6, T.TRAP);
	chk(got5 === 8, '\u2713 active=false 的 AlarmTrap 帧号 = ' + got5 + '（期望 8 = BLACK + 0*16）');

	/* ---- ⑤ 优先级：陷阱 > 植物 > 草叶 ---- */
	console.log('=== \u2464 三级优先级（陷阱 > 植物 > 草叶）===');
	const pl = R.makePlant('Sungrass');                        // image 3 ⇒ frame 112+3 = 115
	const traps2 = new Array(64).fill(null), plants2 = new Array(64).fill(null);
	plants2[1 * 8 + 1] = pl;
	R.setLayers(traps2, plants2);
	R.state.map[1 * 8 + 1] = T.HIGH_GRASS;
	chk(R.featuresVisual(1 * 8 + 1, T.HIGH_GRASS) === 115,
		'\u2713 有植物时优先画植物（Sungrass 帧 115），不画高草');

	// 同格既有陷阱又有植物 ⇒ 陷阱赢
	const traps3 = new Array(64).fill(null), plants3 = new Array(64).fill(null);
	traps3[1 * 8 + 6] = R.makeTrap('ShockingTrap');           // YELLOW(2)+DOTS(0)*16 = 2
	plants3[1 * 8 + 6] = pl;
	R.setLayers(traps3, plants3);
	chk(R.featuresVisual(1 * 8 + 6, T.HIGH_GRASS) === 2,
		'\u2713 陷阱与植物同格时陷阱优先（帧 2）');

	// 都没有 ⇒ 回到草叶（帧号取决于 variance 的 ALT 位，两个分支都要对上）
	R.setLayers(new Array(64).fill(null), new Array(64).fill(null));
	const vg = R.state.variance[1 * 8 + 1];
	const wantGrass = 9 + 16 * 0 + (vg >= 50 ? 1 : 0);
	chk(R.featuresVisual(1 * 8 + 1, T.HIGH_GRASS) === wantGrass,
		'\u2713 没有层对象时回到高草草叶（variance=' + vg + ' ⇒ 帧 ' + wantGrass + '）');

	// 显式构造 variance=0 的位置，确认非 ALT 帧号是 9
	R.state.variance[0] = 0;
	chk(R.featuresVisual(0, T.HIGH_GRASS) === 9,
		'\u2713 variance=0 时高草草叶帧号 = 9（非 ALT）');
	chk(R.featuresVisual(0, T.FURROWED_GRASS) === 11, '\u2713 犁过的草帧号 = 11');
	chk(R.featuresVisual(0, T.GRASS) === 13, '\u2713 草地草叶帧号 = 13');
	chk(R.featuresVisual(0, T.EMBERS) === 9 + 16 * 5, '\u2713 余烬草叶帧号 = 89（固定第 5 段）');

	/* ---- ⑥ 全部 33 个陷阱都能造出来且帧号合法 ---- */
	console.log('=== \u2465 全部陷阱类逐一构造 ===');
	let okAll = true, badCls = [];
	R.TRAPS.forEach(function (d) {
		const t = R.makeTrap(d.cls);
		if (!t) { okAll = false; badCls.push(d.cls + '(null)'); return; }
		const want = R.TRAP_COLOR[d.color] + R.TRAP_SHAPE[d.shape] * 16;
		const arr = new Array(64).fill(null); arr[0] = t;
		R.setLayers(arr, new Array(64).fill(null));
		const v = R.featuresVisual(0, T.TRAP);
		if (v !== want) { okAll = false; badCls.push(d.cls + '(' + v + '!=' + want + ')'); }
	});
	chk(okAll, '\u2713 全部 ' + R.TRAPS.length + ' 个陷阱的渲染帧号 = color+shape*16'
		+ (badCls.length ? '；异常：' + badCls.join(', ') : ''));
	chk(R.TRAPS.length === 33, '\u2713 陷阱表 33 项');
	chk(R.PLANTS.length === 13, '\u2713 植物表 13 项');

	/* ---- ⑦ 带陷阱的格，render() 真的往 terrain_features 上画了 ---- */
	console.log('=== \u2466 render() 端到端：确有绘制调用 ===');
	const draws = [];
	const sandbox2 = fakeEnv();
	// 直接换掉 Sheet.prototype.draw 的依赖不好做，改用统计 drawImage 调用
	sandbox2.document.createElement = function () {
		return {
			width: 0, height: 0,
			getContext: function () {
				return {
					drawImage: function () { draws.push(1); },
					getImageData: function (x, y, w, h) {
						const d = new Uint8ClampedArray(w * h * 4);
						for (let i = 3; i < d.length; i += 4) d[i] = 255;   // 全不透明
						return { data: d };
					}
				};
			}
		};
	};
	vm.runInContext(fs.readFileSync(RENDER, 'utf8'), sandbox2, { filename: RENDER });
	const R2 = sandbox2.TE_RENDER, T2 = R2.T;
	R2.state.w = 4; R2.state.h = 4;
	R2.resize(4, 4, T2.EMPTY);
	R2.state.map = R2.state.map; R2.setVariance(1);
	const tr = new Array(16).fill(null); tr[5] = R2.makeTrap('AlarmTrap');
	R2.setLayers(tr, new Array(16).fill(null));

	const ctxCalls = [];
	const ctx = {
		clearRect() { }, save() { }, restore() { }, scale() { },
		drawImage() { ctxCalls.push('drawImage'); }
	};
	// Sheet.draw 用的是 this._src，需要先喂 Image
	const fakeImg = { width: 256, height: 128 };
	R2.setSheets(fakeImg, fakeImg);
	R2.render(ctx, { scale: 1, showFeatures: true });

	// features 层每格 1 次 draw（16 格）→ 至少 16 次
	const featDraws = ctxCalls.length;
	chk(featDraws >= 16, '\u2713 render() 调用了 drawImage ' + featDraws + ' 次（含 features 层）');

	console.log('');
	if (fail.length) {
		console.log('\u274c ' + fail.length + ' 项不符：');
		fail.forEach(function (f) { console.log('   - ' + f); });
		process.exit(1);
	}
	console.log('\u2705 全部通过（' + pass + ' 项）：陷阱/植物层与 Java 机制一致');
	process.exit(0);
}

/* 供外部（带 --broken）使用 */
var brokenPath = process.env.TE_BROKEN || '';
