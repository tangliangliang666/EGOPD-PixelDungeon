/* 生成一份含「陷阱 + 植物」的房间 Java，交给 javac 真编译 —— 这是最强的核验：
 * 能证明 level.setTrap(Trap,int) / level.plant(Plant.Seed,int) 的签名与调用姿势都对，
 * 而纯字符串断言只能证明「长得像」。
 * 用法：node _chk/gen_probe_room.js
 */
const fs = require('fs'), path = require('path'), vm = require('vm');
const DIR = path.join(__dirname, '..', 'tools', 'terrain-editor');
const OUT = path.join(__dirname, 'probe', 'ProbeLayerRoom.java');

const sandbox = { console };
sandbox.window = sandbox;
sandbox.document = {
	createElement: () => ({
		width: 0, height: 0,
		getContext: () => ({ drawImage() { }, getImageData: (x, y, w, h) => ({ data: new Uint8ClampedArray(w * h * 4) }) })
	})
};
vm.createContext(sandbox);
['assets.js', 'render.js', 'editor.js', 'codegen.js'].forEach(f =>
	vm.runInContext(fs.readFileSync(path.join(DIR, f), 'utf8'), sandbox, { filename: f }));

const Ed = sandbox.TE_EDITOR, R = sandbox.TE_RENDER, G = sandbox.TE_GEN;
Ed.setRoomSize(10, 8);

/* 刻意覆盖各种分支：普通陷阱 / 未发现陷阱 / 大植物 / 小植物 / 同类多格（去重 import） */
Ed.setPlant(2, 2, R.makePlant('Firebloom'));
Ed.setTrap(3, 3, R.makeTrap('FrostTrap'));
Ed.setPlant(5, 5, R.makePlant('Sungrass'));
Ed.setTrap(5, 6, R.makeTrap('AlarmTrap', { visible: false }));
Ed.setTrap(1, 1, R.makeTrap('PoisonDartTrap'));
Ed.setPlant(7, 2, R.makePlant('BlandfruitBush'));
Ed.setTrap(7, 5, R.makeTrap('FrostTrap'));           // 与 (3,3) 同类 ⇒ import 不能重复
Ed.setPlant(8, 6, R.makePlant('Firebloom'));         // 同理

Ed.E.roomClass = 'ProbeLayerRoom';
/* 包名必须与 StandardRoom 同包（levels.rooms.standard），否则 extends StandardRoom 解析不到 */
const java = G.generate({
	pkg: 'com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard',
	rawClass: 'StandardRoom'
});
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, java);
console.log('已写出 ' + OUT + '（' + java.split('\n').length + ' 行）');
