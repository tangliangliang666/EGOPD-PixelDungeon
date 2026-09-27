/* 用编辑器内核生成一个真实房间 .java，供 javac 编译验证 */
const fs=require('fs'),path=require('path'),vm=require('vm');
const DIR='tools/terrain-editor';
function fc(){return {drawImage(){},getImageData(){return{data:new Uint8ClampedArray(4)}},clearRect(){},save(){},restore(){},scale(){},fillRect(){},strokeRect(){},beginPath(){},moveTo(){},lineTo(){},stroke(){},setLineDash(){},fill(){}};}
const sb={window:{},document:{createElement(){return{width:0,height:0,getContext(){return fc()},style:{},appendChild(){},addEventListener(){}}},querySelector(){return null},body:{appendChild(){},removeChild(){}}},console,Image:function(){},setTimeout,clearTimeout,navigator:{},Blob:function(){},URL:{createObjectURL(){return''},revokeObjectURL(){}},FileReader:function(){}};
sb.window=sb;vm.createContext(sb);
for(const f of ['render.js','editor.js','codegen.js']) vm.runInContext(fs.readFileSync(path.join(DIR,f),'utf8'),sb,{filename:f});
const Ed=sb.TE_EDITOR,Gen=sb.TE_GEN,T=Ed.T;

Ed.setRoomSize(12,12);
Ed.MACROS.skeleton(); Ed.MACROS.fourDoors();
Ed.set(3,0,T.SECRET_DOOR); Ed.set(10,13,T.LOCKED_DOOR);
for(let y=2;y<=5;y++)for(let x=2;x<=6;x++) Ed.set(x,y,T.GRASS);
for(let y=8;y<=11;y++) Ed.set(11,y,T.WATER);
for(let x=8;x<=11;x++) Ed.set(x,2,T.HIGH_GRASS);
for(let y=8;y<=10;y++)for(let x=7;x<=9;x++) Ed.set(x,y,T.CHASM);
Ed.set(4,9,T.PEDESTAL); Ed.set(5,9,T.STATUE); Ed.set(6,9,T.TRAP);
Ed.set(12,6,T.FURROWED_GRASS); Ed.set(2,11,T.EMBERS);

const code=Gen.generate({pkg:'com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard',rawClass:'StandardRoom',sheetLabel:'tiles_lob'}).replace(/^public class \w+ extends/m,'public class EgoTerrainEditorSampleRoom extends').replace(/^ \* 由 EGOPD.*$/m,' * 由 EGOPD 地形编辑器生成（核验用样例，勿入库）。');
const out='_chk/_javachk/EgoTerrainEditorSampleRoom.java';
fs.mkdirSync(path.dirname(out),{recursive:true});
fs.writeFileSync(out,code,'utf8');
console.log(code);
