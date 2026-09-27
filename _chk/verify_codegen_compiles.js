/* EGOPD 地形编辑器 —— 「生成的 Java 必须能真编译」核验
 *
 * 为什么需要它：verify_editor_layers_ui.js 只做字符串断言，只能证明「长得像」。
 * 真实世界里最容易出的事故是**签名/包名写错**（比如写成 new Sungrass() 而不是
 * new Sungrass.Seed()，或忘了 import Point）—— 字符串断言抓不到，javac 一抓就准。
 *
 * 做法：① 用编辑器内核对生成一份含陷阱+植物的房间 Java；
 *       ② 直接交给项目自带的 JDK 21 javac，classpath 用**本仓已编译产物**
 *          （core/build/classes/java/main + SPD-classes/build/classes/java/main）；
 *       ③ 断言零错误，并反汇编确认字节码里出现了 Level.setTrap / Level.plant。
 *
 * 反例自测（--selftest）：把 codegen 里的 `new ' + cls + '.Seed()'` 改成 `new ' + cls + '()`
 * ⇒ 本脚本必须报失败（证明它真的在编译，而不是在骗自己）。
 *
 * 前置：需要 core / SPD-classes 已编译过（用户侧 Gradle 跑过即可）。
 *       若目录不存在，脚本会打印 SKIP 并以 0 退出（不能因为环境缺失就误报失败）。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const { spawnSync } = require('child_process');

const ROOT = path.join(__dirname, '..');
const DIR = path.join(ROOT, 'tools', 'terrain-editor');
const PROBEDIR = path.join(__dirname, 'probe');
const OUTDIR = path.join(PROBEDIR, 'out');

const CORE = path.join(ROOT, 'core', 'build', 'classes', 'java', 'main');
const SPDC = path.join(ROOT, 'SPD-classes', 'build', 'classes', 'java', 'main');

let pass = 0, fail = 0;
function ok(c, m, extra) {
	if (c) { pass++; console.log('  \u2713 ' + m); }
	else { fail++; console.log('  \u2717 ' + m + (extra !== undefined ? '  \u2192 ' + extra : '')); }
}
function eq_(got, want, m) {
	ok(got === want, m, got === want ? undefined : { got: got, want: want });
}

/* ------------------------------------------------------------ 环境检查 */
if (!fs.existsSync(CORE) || !fs.existsSync(SPDC)) {
	console.log('\nSKIP: 缺少已编译产物，无法做 javac 核验');
	console.log('  需要：' + CORE);
	console.log('  需要：' + SPDC);
	console.log('  （先跑一次 Gradle 编译即可；本脚本不代跑编译。）\n');
	process.exit(0);
}

const JAVA_HOME = process.env.JAVA_HOME && fs.existsSync(path.join(process.env.JAVA_HOME, 'bin', 'javac.exe'))
	? process.env.JAVA_HOME
	: 'D:\\PD\\tools\\jdk-21.0.12.1+1';
const JAVAC = path.join(JAVA_HOME, 'bin', 'javac.exe');
const JAVAP = path.join(JAVA_HOME, 'bin', 'javap.exe');
if (!fs.existsSync(JAVAC)) {
	console.log('\nSKIP: 找不到 javac：' + JAVAC + '\n');
	process.exit(0);
}

/* ------------------------------------------------------- 载入编辑器内核 */
const sandbox = { console };
sandbox.window = sandbox;
sandbox.document = {
	createElement: () => ({
		width: 0, height: 0,
		getContext: () => ({ drawImage() { }, getImageData: (x, y, w, h) => ({ data: new Uint8ClampedArray(w * h * 4) }) })
	})
};
vm.createContext(sandbox);
const FILES = ['assets.js', 'render.js', 'editor.js', 'codegen.js'];
const SRC = {};
FILES.forEach(f => { SRC[f] = fs.readFileSync(path.join(DIR, f), 'utf8'); });
FILES.forEach(f => vm.runInContext(SRC[f], sandbox, { filename: f }));

const Ed = sandbox.TE_EDITOR, R = sandbox.TE_RENDER, G = sandbox.TE_GEN;

/* 反例自测：把植物那行改成错误的 new Sungrass() */
const SELFTEST = process.argv.includes('--selftest');
if (SELFTEST) {
	/* 在 codegen.js 源码里做一次外科手术式替换（用后即弃的 sandbox 源码副本，
	 * 不碰磁盘上的真文件 —— 与 selftest_trap_render.py 的「改文件再还原」不同，
	 * 这里可以直接改源码字符串再 vm 执行，更安全。 */
	const broken = SRC['codegen.js'].replace(
		'new \' + o.cls + \'.Seed()',
		'new \' + o.cls + \'()');
	if (broken === SRC['codegen.js']) {
		console.log('!! 反例自测：未能改动 codegen.js（找不到目标片段）');
		process.exit(2);
	}
	/* 重建 sandbox（editor 已被上面的 generate 污染过状态也无妨，重新载一遍） */
	const sb2 = { console };
	sb2.window = sb2;
	sb2.document = sandbox.document;
	vm.createContext(sb2);
	['assets.js', 'render.js', 'editor.js'].forEach(f => vm.runInContext(SRC[f], sb2, { filename: f }));
	vm.runInContext(broken, sb2, { filename: 'codegen.broken.js' });
	Object.assign(sandbox, { TE_GEN: sb2.TE_GEN, TE_EDITOR: sb2.TE_EDITOR, TE_RENDER: sb2.TE_RENDER });
	console.log('\n[反例自测] 已把 `new X.Seed()` 破坏成 `new X()`，期望下面编译失败\n');
}

/* --------------------------------------------------------------- 生成 */
const Ed2 = sandbox.TE_EDITOR, R2 = sandbox.TE_RENDER, G2 = sandbox.TE_GEN;
Ed2.setRoomSize(10, 8);
Ed2.setPlant(2, 2, R2.makePlant('Firebloom'));
Ed2.setTrap(3, 3, R2.makeTrap('FrostTrap'));
Ed2.setPlant(5, 5, R2.makePlant('Sungrass'));
Ed2.setTrap(5, 6, R2.makeTrap('AlarmTrap', { visible: false }));
Ed2.setTrap(1, 1, R2.makeTrap('PoisonDartTrap'));
Ed2.setPlant(7, 2, R2.makePlant('BlandfruitBush'));
Ed2.setTrap(7, 5, R2.makeTrap('FrostTrap'));       // 重复类 ⇒ 验 import 去重
Ed2.setPlant(8, 6, R2.makePlant('Firebloom'));     // 重复类
/* 道具层：把「需要额外 import / 带参构造 / 堆型链式赋值」三种情况都放进来，
 * 因为它们是本层唯一会编译失败的地方（Random / Dungeon / Heap 不会自动出现）。 */
Ed2.setItem(1, 3, R2.makeItem('Food'));
Ed2.setItem(6, 1, R2.makeItem('Food'));                       // 重复类 ⇒ 验 import 去重
Ed2.setItem(4, 4, R2.makeItem('ScrollOfUpgrade', { heap: 'CHEST' }));
Ed2.setItem(8, 4, R2.makeItem('IronKey'));                    // ctor 带 Dungeon.depth
Ed2.setItem(2, 6, R2.makeItem('DarkGold'));                   // ctor 带 Random.NormalIntRange
Ed2.setItem(6, 6, R2.makeItem('GoldenKey'));                  // 另一种按深度构造
/* 区域随机（C.5）：这个段落是本层唯一会用到 Point / Random.shuffle(int[]) 的地方，
 * 而且是**唯一会写回 Painter.set( level, cell, val )** 的地方 ——
 * 漏 import Point、把 shuffle 用错重载（写成 shuffle(Object[])）都只有 javac 抓得到。 */
Ed2.clearRegions();
Ed2.addRegion(Ed2.rectOf(1, 1, 3, 3));               // 9 格，withLayers 关（纯地形 shuffle）
Ed2.setTrap(2, 2, R2.makeTrap('FrostTrap'));         // 区域内放覆盖层，供 withLayers=true 那份用
Ed2.addRegion(Ed2.rectOf(5, 4, 8, 6));               // 不相邻的第二片（4×3 = 12 格）
Ed2.E.roomClass = 'ProbeLayerRoom';

const PKG = 'com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard';
const java = G2.generate({ pkg: PKG, rawClass: 'StandardRoom' });

fs.mkdirSync(PROBEDIR, { recursive: true });
const JAVAFILE = path.join(PROBEDIR, 'ProbeLayerRoom.java');
fs.writeFileSync(JAVAFILE, java);

console.log('=== A. 生成物自检 ===');
ok(/new Sungrass\.Seed\(\)/.test(java), '植物写的是 new X.Seed()');
ok(/level\.setTrap\( new FrostTrap\(\), /.test(java), '陷阱写的是 level.setTrap( new X(), pos )');
const trapImports = (java.match(/^import .*\.levels\.traps\..*;$/gm) || []);
ok(trapImports.length === new Set(trapImports).size, '陷阱 import 无重复（同类多格只 import 一次）',
	trapImports.join(' | '));
const plantImports = (java.match(/^import .*\.plants\..*;$/gm) || []);
ok(plantImports.length === new Set(plantImports).size, '植物 import 无重复');

console.log('\n=== A2. 道具层生成物自检 ===');
ok(/level\.drop\( new Food\(\), /.test(java), '道具写的是 level.drop( new X(), pos )');
/* 只在**非注释**行里断言，因为生成物的注释里**故意**写着「别用 addItemToSpawn」的警告 */
const javaCode = java.split('\n').filter(l => !/^\s*\/\//.test(l)).join('\n');
ok(!/addItemToSpawn/.test(javaCode),
	'**关键**：不用 addItemToSpawn（它被 createItems() 随机重排，位置不可控）');
ok(/\.type = Heap\.Type\.CHEST/.test(java), 'CHEST 堆型用链式 .type = Heap.Type.CHEST');
const itemImports = (java.match(/^import com\.shatteredpixel\.shatteredpixeldungeon\.items\.[\w.]+;$/gm) || []);
ok(itemImports.length === new Set(itemImports).size, '道具 import 无重复（同类多格只 import 一次）',
	itemImports.join(' | '));
ok(/^import com\.shatteredpixel\.shatteredpixeldungeon\.items\.Heap;$/m.test(java),
	'用到堆型 ⇒ 自动 import Heap（漏了必然编译失败）');
ok(/^import com\.watabou\.utils\.Random;$/m.test(java), 'ctor 用 Random ⇒ 自动 import Random');
ok(/^import com\.shatteredpixel\.shatteredpixeldungeon\.Dungeon;$/m.test(java),
	'ctor 用 Dungeon ⇒ 自动 import Dungeon');

console.log('\n=== A3. 区域随机生成物自检（C.5）===');
ok(/^import com\.watabou\.utils\.Point;$/m.test(java),
	'区域段用 Point ⇒ 自动 import Point（漏了必然编译失败）');
ok(/^import com\.watabou\.utils\.Random;$/m.test(java), '区域段用 Random ⇒ 自动 import Random');
ok(/Random\.shuffle\( r0 \);/.test(java), 'withLayers=false 那份：直接 shuffle int[] 地形数组');
ok(/int\[\] r0Pos = new int\[\]/.test(java), '生成了 Point → cell 的位置数组 r0Pos');
ok(/Painter\.set\( level, r0Pos\[k\], r0\[k\] \);/.test(java), '写回用的是 Painter.set(level, cell, val)');
/* **关键签名**：Random.shuffle 在项目里有 3 个重载（int[] / T[] / (U[],V[])）。
 * 若把 int[] 写成对象数组，编译能过但语义全错（打乱的是引用不是格子值）；
 * 若误用 shuffle(List) 则直接编译失败。字节码里必须命中 (I[])? 这句留给 C 节反汇编。 */
const regArr = /int\[\] r0 = new int\[\]\{([\s\S]*?)\};/.exec(java);
ok(!!regArr, '生成了区域值数组 int[] r0 = new int[]{...}');
if (regArr) {
	const n0 = regArr[1].replace(/\s/g, '').split(',').filter(s => s).length;
	eq_(n0, 9, '第一片（3×3）值数组长度 = 9');
}
const regArr1 = /int\[\] r1 = new int\[\]\{([\s\S]*?)\};/.exec(java);
ok(!!regArr1, '第二片也生成（不相邻区域各自成段）');
if (regArr1) {
	const n1 = regArr1[1].replace(/\s/g, '').split(',').filter(s => s).length;
	eq_(n1, 12, '第二片（4×3）值数组长度 = 12');
}

/* withLayers=true 的变体必须也**能编译**（置换表 + clone + 回写是另一条代码路径，
 * 只断言字符串不够：早先 r0Old / r0Ord 的类型写错时字符串看着完全正常） */
const javaLayered = G2.generate({ pkg: PKG, rawClass: 'StandardRoom', withLayers: true });
const JAVAFILE2 = path.join(PROBEDIR, 'ProbeLayerRoomLayered.java');
fs.writeFileSync(JAVAFILE2, javaLayered.replace(/class ProbeLayerRoom/g, 'class ProbeLayerRoomLayered'));
ok(/int\[\] r0Ord = new int\[/.test(javaLayered), 'withLayers=true 变体含置换表 r0Ord');
ok(/int\[\] r0Old = r0\.clone\(\);/.test(javaLayered), 'withLayers=true 变体含 r0Old = r0.clone()');
console.log('  （withLayers=true 变体已写出，交给下面 javac 一起编）');

/* 尺寸模糊化（C.6）的变体也必须能真编译 —— 这一段是唯二会产出
 * **类级别声明**（@Override sizeCatProbs）的地方，而且它引用了 StandardRoom 的
 * SizeCategory 语义：若把 sizeCatProbs 的返回类型写错（比如 float[] 写成 int[]）、
 * 或 setSize 签名对不上（Room.setSize(int,int,int,int)），字符串断言完全看不出来，
 * 只有 javac 会红。 */
Ed2.setFuzz({ on: true, minW: 8, maxW: 10, minH: 8, maxH: 8, guarded: 1 });
Ed2.E.roomClass = 'ProbeFuzzRoom';
const javaFuzz = G2.generate({ pkg: PKG, rawClass: 'StandardRoom' });
const JAVAFILE3 = path.join(PROBEDIR, 'ProbeFuzzRoom.java');
fs.writeFileSync(JAVAFILE3, javaFuzz);
Ed2.setFuzz({ on: false });                       // 复位，别影响后续
ok(/public float\[\] sizeCatProbs\(\)\{/.test(javaFuzz), '模糊化变体覆写了 sizeCatProbs()（返回 float[]）');
ok(/setSize\( 8, 10, 8, 8 \);/.test(javaFuzz), '模糊化变体在 paint() 内调用 setSize( 8, 10, 8, 8 )');
ok(/new float\[\]\{1, 0, 0\}/.test(javaFuzz), '模糊化变体只给 NORMAL 权重 1（8~10 落在 NORMAL）');
console.log('  （模糊化变体已写出，交给下面 javac 一起编）');

console.log('\n=== B. javac 真编译（classpath = 本仓已编译产物）===');
fs.rmSync(OUTDIR, { recursive: true, force: true });
fs.mkdirSync(OUTDIR, { recursive: true });

/* ⚠️ javac 在 Windows 上把中文诊断按**系统 ANSI 代码页（GBK）**写到 stderr，
 * Node 默认按 UTF-8 解 ⇒ 拿到的是乱码（「不兼容的类型」会变成 `�����ݵ�����`），
 * 于是基于中文关键词的断言必然失手。两种解法：
 *   ① `-J-Dfile.encoding=UTF-8` + `-encoding UTF-8`（对 javac 的诊断输出不总生效）
 *   ② 拿到 Buffer 后用 GBK 解回来（最稳，不依赖 JVM 参数）
 * 这里选 ②：spawnSync 不设 encoding，拿 Buffer 手动解码，并同时试 UTF-8 兜底。 */
const ICONV = (() => {
	try { return new TextDecoder('gbk'); } catch (e) { return null; }
})();
function decodeOut(buf) {
	if (!buf) return '';
	const s = buf.toString('utf8');
	/* 若 UTF-8 解出来带替换字符，说明原始字节不是 UTF-8 ⇒ 用 GBK 再解一次 */
	if (s.indexOf('\uFFFD') >= 0 && ICONV) {
		try { return ICONV.decode(buf); } catch (e) { /* 用 UTF-8 那份 */ }
	}
	return s;
}

const cp = CORE + ';' + SPDC;
const r = spawnSync(JAVAC, ['-encoding', 'UTF-8', '-cp', cp, '-d', OUTDIR, JAVAFILE, JAVAFILE2, JAVAFILE3],
	{ windowsHide: true });
const out = decodeOut(r.stdout) + decodeOut(r.stderr);
const errLines = out.split('\n').filter(l => /错误|error:/.test(l));

if (SELFTEST) {
	/* 反例自测期望：编译**必须失败** */
	const compiled = r.status === 0 && fs.existsSync(path.join(OUTDIR, 'com', 'shatteredpixel', 'shatteredpixeldungeon',
		'levels', 'rooms', 'standard', 'ProbeLayerRoom.class'));
	ok(!compiled, '反例自测：把 Seed 去掉后 javac 应当报错（证明本脚本真的在编译）',
		compiled ? '竟然编译成功了' : undefined);
	if (!compiled) {
		/* javac 中文诊断形如「不兼容的类型: Sungrass无法转换为Seed」，
		 * 注意「错误:」与正文**不在同一行**（正文在下一行），所以要在**全文**里找。 */
		const seedErr = /不兼容的类型|无法转换为|incompatible types/.test(out);
		ok(seedErr, '报错内容与「Seed 类型不符」相关', out.split('\n').slice(0, 3).join(' / '));
	}
	console.log('\n' + (fail ? '\u2705 反例自测通过：编译确实会失败（' + fail + ' 项断言按预期失败）'
		: '\u274c 反例自测失败：本核验脚本没有真正在编译'));
	process.exit(fail ? 0 : 1);
}

ok(r.status === 0, 'javac 退出码 0（零错误零警告级错误）', r.status);
ok(errLines.length === 0, 'javac 输出无「错误」行', errLines.slice(0, 5).join(' / '));
const CLS = path.join(OUTDIR, 'com', 'shatteredpixel', 'shatteredpixeldungeon', 'levels', 'rooms', 'standard', 'ProbeLayerRoom.class');
ok(fs.existsSync(CLS), '产出了 ProbeLayerRoom.class');
const CLS2 = path.join(OUTDIR, 'com', 'shatteredpixel', 'shatteredpixeldungeon', 'levels', 'rooms', 'standard', 'ProbeLayerRoomLayered.class');
ok(fs.existsSync(CLS2), '产出了 ProbeLayerRoomLayered.class（withLayers=true 那条路径也真能编译）');
const CLS3 = path.join(OUTDIR, 'com', 'shatteredpixel', 'shatteredpixeldungeon', 'levels', 'rooms', 'standard', 'ProbeFuzzRoom.class');
ok(fs.existsSync(CLS3), '产出了 ProbeFuzzRoom.class（尺寸模糊化那条路径也真能编译）');

console.log('\n=== C. 反汇编确认字节码里的真调用 ===');
if (fs.existsSync(CLS)) {
	const jr = spawnSync(JAVAP, ['-p', '-c', CLS], { encoding: 'utf8', windowsHide: true });
	const bc = jr.stdout || '';
	ok(/Level\.setTrap:\(Lcom\/shatteredpixel\/shatteredpixeldungeon\/levels\/traps\/Trap;I\)/.test(bc),
		'字节码含 Level.setTrap(Trap,int)');
	ok(/Level\.plant:\(Lcom\/shatteredpixel\/shatteredpixeldungeon\/plants\/Plant\$Seed;I\)/.test(bc),
		'字节码含 Level.plant(Plant$Seed,int)');
	ok(/plants\/Firebloom\$Seed/.test(bc), '字节码含 Firebloom$Seed（内部类，不是 Firebloom）');
	ok(/levels\/traps\/FrostTrap"\."<init>"/.test(bc.replace(/\/\//g, '/')) || /levels\/traps\/FrostTrap/.test(bc),
		'字节码含 FrostTrap.<init>');
	ok(/Level\.pointToCell:\(Lcom\/watabou\/utils\/Point;\)I/.test(bc), '字节码含 Level.pointToCell(Point)');

	/* —— 道具层：证明真的调到了 Level.drop，且堆型 / 带参构造都落进字节码 —— */
	ok(/Level\.drop:\(Lcom\/shatteredpixel\/shatteredpixeldungeon\/items\/Item;I\)Lcom\/shatteredpixel\/shatteredpixeldungeon\/items\/Heap;/.test(bc),
		'字节码含 Level.drop(Item,int) → Heap（上游精确落点的唯一入口）');
	ok(/items\/Heap\.type:Lcom\/shatteredpixel\/shatteredpixeldungeon\/items\/Heap\$Type;/.test(bc),
		'字节码含 putfield Heap.type');
	ok(/Heap\$Type\.CHEST/.test(bc), '字节码含 Heap$Type.CHEST 常量');
	ok(/items\/food\/Food"\."<init>":\(\)V/.test(bc.replace(/\/\//g, '/')) || /items\/food\/Food/.test(bc),
		'字节码含 Food.<init>');
	ok(/items\/keys\/IronKey\.\"<init>\":\(I\)V/.test(bc) || /IronKey\.\"<init>\":\(I\)V/.test(bc),
		'字节码含 IronKey.<init>(int)（按深度构造，不是无参）');
	ok(/Dungeon\.depth:I/.test(bc), '字节码含 getstatic Dungeon.depth（钥匙按当前深度）');
	ok(/Random\.NormalIntRange:\(II\)I/.test(bc), '字节码含 Random.NormalIntRange（暗金随机量）');
	ok(/DarkGold\.quantity/.test(bc), '字节码含 DarkGold.quantity');

	/* —— 区域随机（C.5）：证明调到了正确的 shuffle 重载与 Painter.set —— */
	ok(/Random\.shuffle:\(\[I\)V/.test(bc),
		'字节码含 Random.shuffle(int[])（**必须**是这个重载，不是 shuffle(Object[])）');
	if (fs.existsSync(CLS2)) {
		const jr2 = spawnSync(JAVAP, ['-p', '-c', CLS2], { encoding: 'utf8', windowsHide: true });
		const bc2 = jr2.stdout || '';
		ok(/Random\.shuffle:\(\[I\)V/.test(bc2),
			'（withLayers）置换表也是 shuffle(int[]) —— 用的是同一条正确重载');
		ok(/Level\.pointToCell:\(Lcom\/watabou\/utils\/Point;\)I/.test(bc2),
			'（withLayers）字节码含 Level.pointToCell(Point)');
	}

	/* —— 尺寸模糊化（C.6）：证明 sizeCatProbs 真的覆盖了父类、setSize 真的是
	 *    Room.setSize(int,int,int,int)→boolean。这两条是「只写一半会静默失效」
	 *    的源头：setSize 返回 **Z（boolean）** 而不是 void，正是它「静默返回 false」
	 *    的形态；断言里体现这一点，日后有人以为是 void 就会立刻显形。 */
	if (fs.existsSync(CLS3)) {
		const jr3 = spawnSync(JAVAP, ['-p', '-c', CLS3], { encoding: 'utf8', windowsHide: true });
		const bc3 = jr3.stdout || '';
		ok(/public float\[\] sizeCatProbs\(\);/.test(bc3),
			'字节码含 public float[] sizeCatProbs()（类型确实是 float[]，覆写成立）');
		/* 生成代码里写的是裸 `setSize(...)`（继承自 Room），所以是 invokevirtual "setSize:(IIII)Z"，
		 * 不带 Room. 前缀 —— 别去匹配 Room.setSize。
		 * 返回 Z 是关键：它**会**静默返回 false，这就是「只写一半」失效的原因。 */
		ok(/Method setSize:\(IIII\)Z/.test(bc3),
			'字节码含 setSize:(IIII)Z（四个 int → boolean，静默失败的那道返回）');
		ok(/iconst_8|bipush\s+8/.test(bc3), '字节码里带上了下界常量 8');
	} else {
		ok(false, '没有 ProbeFuzzRoom.class，跳过模糊化反汇编');
	}
} else {
	ok(false, '没有 class 文件，跳过反汇编');
}

console.log('\n' + (fail ? '\u274c 失败 ' + fail + ' 项（通过 ' + pass + '）' : '\u2705 全部通过（' + pass + ' 项）') + '\n');
process.exit(fail ? 1 : 0);
