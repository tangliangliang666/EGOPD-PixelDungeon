/* EGOPD 地形编辑器 —— 「真实浏览器端到端」核验（系统 Edge 无头模式）
 *
 * 与 verify_editor_layers_ui.js 的分工：
 *   - verify_editor_layers_ui.js：纯 Node + DOM 桩，快、可断言细节，但**桩不等于浏览器**；
 *   - 本脚本：把 _chk/te_probe.js 注入**真 index.html**（真 DOM / 真事件 / 真图集解码），
 *     断言 UI 真的绑上了、点击真的落到图层上、代码面板真的更新了。
 * 两者都要跑：桩用来测边界，浏览器用来防「桩里对、浏览器里错」。
 *
 * 为什么用系统 Edge 而不是 agent-browser：后者要下 Chromium，本机实测下载超时。
 *
 * 用法：node _chk/verify_editor_browser.js
 * 退出码：0 全过 / 1 有失败 / 2 环境缺失（找不到 Edge）
 */
'use strict';
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const ROOT = path.join(__dirname, '..');
const DIR = path.join(ROOT, 'tools', 'terrain-editor');
const PROBE = path.join(__dirname, 'te_probe.js');
const TMP = path.join(DIR, '_probe_auto.html');

const EDGE_CANDIDATES = [
	'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
	'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
];
const EDGE = EDGE_CANDIDATES.find(p => fs.existsSync(p));
if (!EDGE) {
	console.log('\nSKIP: 找不到系统 Edge（尝试过：\n  ' + EDGE_CANDIDATES.join('\n  ') + '\n）\n');
	process.exit(2);
}

/* 组装注入页
 *
 * ⚠️ 为什么不用 iframe + 轮询（原实现的坑，2026-09-19 改）：
 *   ① 这个 Edge 版本的 `--dump-dom` 在页面 load 后**立刻**序列化顶层文档，
 *      `setTimeout(run, 40)` 这类轮询回调根本没机会跑（--virtual-time-budget 对它无效），
 *      所以原来那套 `run()` 一旦第一次判断「还没就绪」就永远拿不到结果。
 *   ② iframe 里的 title 也不会被顶层 <title> 反映出来。
 *
 *    改为：把探针**直接注入** index.html 的副本（放在同目录，保证 <script src> 相对路径不变），
 *    紧跟在 app.js 之后**同步**执行。boot() 本身同步跑到底，所以此刻 DOM 已定型。
 */
const inj = fs.readFileSync(PROBE, 'utf8');
const html = fs.readFileSync(path.join(DIR, 'index.html'), 'utf8');
const page = html.replace(
	'</body>',
	'<script>\n' + inj + '\n</script>\n</body>'
);
fs.writeFileSync(TMP, page);

let pass = 0, fail = 0;
function ok(c, m, extra) {
	if (c) { pass++; console.log('  \u2713 ' + m); }
	else { fail++; console.log('  \u2717 ' + m + (extra !== undefined ? '  \u2192 ' + extra : '')); }
}

const r = spawnSync(EDGE, [
	'--headless=new', '--disable-gpu', '--allow-file-access-from-files',
	'--virtual-time-budget=25000', '--dump-dom',
	'file:///' + TMP.replace(/\\/g, '/'),
], { encoding: 'utf8', windowsHide: true, maxBuffer: 64 * 1024 * 1024,
     // ⚠️ 必须给上限：Edge 若被前一次残留实例顶住，无头进程会**永久挂起**，
	 // 而 spawnSync 默认无限等待 ⇒ 整个套件卡死（2026-09-19 实际踩过 4 分钟+）。
	 timeout: 120000, killSignal: 'SIGKILL' });

fs.rmSync(TMP, { force: true });

if (r.error || r.signal) {
	console.log('\n\u274c 无头浏览器没能正常结束（error=' + (r.error && r.error.code) +
		' signal=' + r.signal + '）');
	console.log('  多半是上一次的 Edge 实例还在：先 taskkill /IM msedge.exe /F 再重跑。');
	process.exit(1);
}

const dom = r.stdout || '';
const m = /<title>(RESULT::[^<]*)<\/title>/.exec(dom);
if (!m) {
	console.log('\n\u274c 没能取回探针结果（标题未变成 RESULT::）');
	console.log('  前 300 字：' + dom.slice(0, 300));
	process.exit(1);
}

const kv = {};
m[1].replace(/^RESULT::/, '').split(' | ').forEach(seg => {
	const i = seg.indexOf('=');
	if (i > 0) kv[seg.slice(0, i).trim()] = seg.slice(i + 1).trim();
});

console.log('\n=== 真实浏览器（Edge 无头）端到端核验 ===\n');

console.log('A. UI 装配');
ok(kv['A1 sectLayer存在'] === 'true', '左侧「覆盖层」面板存在', kv['A1 sectLayer存在']);
ok(kv['A2 selTrap选项'] === '33', '陷阱下拉有 33 项', kv['A2 selTrap选项']);
ok(kv['A3 selPlant选项'] === '13', '植物下拉有 13 项', kv['A3 selPlant选项']);
ok(kv['A4 chkWater勾选'] === 'true', '水体层默认勾选', kv['A4 chkWater勾选']);
ok(kv['A5 lyrTrap默认选中'] === 'true', '默认停在陷阱层', kv['A5 lyrTrap默认选中']);
ok(kv['A6 陷阱首项'] === 'AlarmTrap', '陷阱首项 = AlarmTrap', kv['A6 陷阱首项']);
ok(kv['A7 植物首项'] === 'Rotberry', '植物首项 = Rotberry（image 0）', kv['A7 植物首项']);
/* 本轮新增：道具层的控件也要在真浏览器里装好 */
ok(parseInt(kv['A8 selItem选项'], 10) >= 60, '道具下拉有 ≥60 项', kv['A8 selItem选项']);
ok(parseInt(kv['A9 selItem分组'], 10) >= 6, '道具下拉分了 ≥6 组', kv['A9 selItem分组']);
ok(kv['A10 selHeap选项'] === '7', '堆型下拉有 7 项', kv['A10 selHeap选项']);
ok(kv['A11 chkItems勾选'] === 'true', '道具层默认勾选', kv['A11 chkItems勾选']);
ok(kv['A12 lyrItem存在'] === 'true', '「放道具」单选框存在', kv['A12 lyrItem存在']);
ok(kv['A13 layerHint存在'] === 'true', '#layerHint 存在', kv['A13 layerHint存在']);
ok(kv['A14 旧trapHint已移除'] === 'true', '旧的 #trapHint 已彻底移除', kv['A14 旧trapHint已移除']);

console.log('\nB. 真鼠标事件在陷阱层落笔');
ok(kv['B1 traps[2,2]'] === 'FrostTrap', '点 (2,2) ⇒ E.traps 记录 FrostTrap', kv['B1 traps[2,2]']);
ok(kv['B2 map[2,2]'] === '18(TRAP=18)', '地形被置为 Terrain.TRAP(18)', kv['B2 map[2,2]']);
ok(kv['B3 trapFrame[2,2]'] === '54', '帧号 54 = WHITE(6)+STARS(3)*16', kv['B3 trapFrame[2,2]']);

console.log('\nC. 切到植物层后落笔');
ok(kv['C1 plants[5,3]'] === 'Sungrass', '点 (5,3) ⇒ E.plants 记录 Sungrass', kv['C1 plants[5,3]']);
ok(kv['C2 map[5,3]'] === '2(GRASS=2)', '地形补成 Terrain.GRASS(2)', kv['C2 map[5,3]']);
ok(kv['C3 plantFrame[5,3]'] === '115', '帧号 115 = image3 + 7*16', kv['C3 plantFrame[5,3]']);

console.log('\nD. 未发现陷阱');
ok(kv['D1 map[7,2]'] === '17(SECRET_TRAP=17)', '地形写 SECRET_TRAP(17)', kv['D1 map[7,2]']);
ok(kv['D2 featuresVisual'] === '-1(期望-1)', 'featuresVisual 返回 -1（不画）', kv['D2 featuresVisual']);

console.log('\nE. 代码面板（#codeBox 是 textarea ⇒ 读 .value）');
ok(parseInt(kv['E1 code长度'], 10) > 1000, '生成结果非空', kv['E1 code长度']);
ok(kv['E2 含FrostTrap'] === 'true', '含 FrostTrap');
ok(kv['E3 含setTrap'] === 'true', '含 level.setTrap');
ok(kv['E4 含Sungrass.Seed'] === 'true', '含 Sungrass.Seed');
ok(kv['E5 含Pointimport'] === 'true', '含 Point import');
ok(kv['E6 含AlarmTrap'] === 'true', '含 AlarmTrap');

console.log('\nF. 房间检查器');
ok(/陷阱/.test(kv['F1 检查器文本'] || ''), '检查器报出了陷阱计数', kv['F1 检查器文本']);

console.log('\nG. 真鼠标事件在道具层落笔（本轮新增）');
ok(kv['G1 items[9,6]'] === 'Food', '点 (9,6) ⇒ E.items 记录 Food', kv['G1 items[9,6]']);
ok(kv['G2 items[9,6]堆型'] === 'HEAP', '默认堆型 = HEAP（普通地面）', kv['G2 items[9,6]堆型']);
ok(kv['G3 items[9,6]ctor'] === 'new Food()', 'ctor = new Food()（可直接编译）', kv['G3 items[9,6]ctor']);
/* 这是道具层最核心的性质：道具**不改地形**（游戏里它进 Level.heaps，不碰地图格） */
ok(kv['G4 地形未被改动'] === 'true', '**放道具完全不改地形**', kv['G4 地形未被改动']);
ok(parseInt(kv['G5 道具层非空数'], 10) >= 1, '道具层非空数 ≥1', kv['G5 道具层非空数']);
ok(kv['G6 重复落笔被跳过'] === 'true', '同格重复落笔被跳过（不产生重复道具）', kv['G6 重复落笔被跳过']);

console.log('\nE. 面板里的道具 codegen（本轮新增）');
ok(kv['E7 含levelDrop'] === 'true', '生成结果含 level.drop（精确落点）', kv['E7 含levelDrop']);
ok(kv['E8 含newFood'] === 'true', '生成结果含 new Food()', kv['E8 含newFood']);
ok(kv['E9 不含addItemToSpawn'] === 'true',
	'非注释行里不含 addItemToSpawn（那条路会被 createItems() 随机重排）',
	kv['E9 不含addItemToSpawn']);

console.log('\nH. 外部图集入口（C.3）');
ok(kv['H1 TE_APP探针存在'] === 'true', 'app.js 暴露了 TE_APP 探针', kv['H1 TE_APP探针存在']);
ok(kv['H2 探针含importAtlas'] === 'true', '探针含 importAtlas', kv['H2 探针含importAtlas']);
ok(kv['H3 btnImpSheet存在'] === 'true', '「导入楼层图集」按钮存在', kv['H3 btnImpSheet存在']);
ok(kv['H4 btnImpFeat存在'] === 'true', '「导入草叶图」按钮存在', kv['H4 btnImpFeat存在']);
ok(kv['H5 btnImpItems存在'] === 'true', '「导入道具图」按钮存在', kv['H5 btnImpItems存在']);
ok(kv['H6 btnExtClear存在'] === 'true', '「清空外部图集」按钮存在', kv['H6 btnExtClear存在']);
ok(kv['H7 extHint存在'] === 'true', '#extHint 存在', kv['H7 extHint存在']);
ok(/无外部图集/.test(kv['H8 extHint文案'] || ''), '初始提示为「当前无外部图集」', kv['H8 extHint文案']);
ok(kv['H9 内置图集数'] === '5', '内置图集 5 张（原版 5 区域；C.7 起项目特有图集走外部导入）', kv['H9 内置图集数']);
ok(kv['H10 全部builtin'] === 'true', '初始全部标记为 builtin', kv['H10 全部builtin']);
ok(kv['H11 初始无外部图集'] === '0', '初始外部图集 0 条', kv['H11 初始无外部图集']);
ok(kv['H12 图集下拉项数'] === '5', '图集下拉 5 项（与注册表一致）', kv['H12 图集下拉项数']);
ok(kv['H13 IndexedDB可用'] === 'true',
	'**浏览器支持 IndexedDB**（否则导入的图集刷新就丢）', kv['H13 IndexedDB可用']);

console.log('\n' + (fail ? '\u274c 失败 ' + fail + ' 项（通过 ' + pass + '）'
	: '\u2705 全部通过（' + pass + ' 项）') + '\n');
process.exit(fail ? 1 : 0);
