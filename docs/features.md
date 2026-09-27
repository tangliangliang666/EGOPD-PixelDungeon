# 非职业专属功能档案（拆迁办挑战 / 钻石彩蛋）

> 2026-09-05 从仓库根 AGENTS.md 5e 节拆分而来：本文档收录**不绑定特定职业**的自定义功能实现细节。
> 通用开发经验见 AGENTS.md；环指大师职业内容见 docs/ring-master.md；神谕见 docs/oracle-class-design.md。
> 原 5e 节内属于环指大师的「闭馆」技能/杰作/深度创伤已归入 docs/ring-master.md 5e 节。

## 1. 挑战系统接线配方（通用，可复用于新增任何自定义挑战）

**挑战系统结构**（`Challenges.java`，位掩码 + 两个对齐数组，**UI 完全数据驱动无需新图标**）：
- 新挑战 = ①新位掩码常量（`DEMOLITION_SQUAD=512`）；②`NAME_IDS` 尾部加小写 id（"demolition_squad"）；③`MASKS` 尾部加同常量（两数组下标对齐，顺序=选择界面显示顺序）；④`MAX_VALUE`/`MAX_CHALS` 同步 +1（`HeroSelectScene` 自动读 `MAX_CHALS` 渲染勾选框）；⑤文本键 `challenges.<id>`=名称 / `challenges.<id>_desc`=描述（**misc_zh.properties 与 misc.properties（en）都要加**，其余语言缺键回退）；⑥效果在 `Dungeon.isChallenged(常量)` 处接入。选择界面 WndChallenges 用 CheckBox（无图标需求）；开局 `Dungeon.init()` 先 `challenges = SPDSettings.challenges()` 再 `initHero`（initHero 内可安全判断）。
  - ⚠️ **位号必须「常规挑战占满最低连续位、特殊挑战占最高位」**：`HeroSelectScene` 的随机挑战是 `2^i`（`i < MAX_CHALS`）直接拼掩码，**只认位号不认数组下标**；`DEBUG_MODE` 这类占高位、不参与随机也不计数的挑战必须排在数组末尾。新增常规挑战时若最高位已被它占走，就把特殊挑战**整体上移一位**（位号只是常量，`isChallenged` 自动跟随），并同步 `MAX_VALUE = 2^总位数 − 1`。实例：「依旧果冻人」`JELLY_PERSON = 1024`（`DEBUG_MODE` 1024→2048，见本文档 2026-09-15 节）。
  - 挑战「是否计入常规数」由 `Challenges.activeChallenges()` 决定（影响 `1.25^n` 分数倍率、`validateChampion` 徽章、菜单计数）：纯视觉/调试类若不希望白送倍率，在里面照 `DEBUG_MODE` 那样 `continue` 跳过。

## 2. 「拆迁办」DEMOLITION_SQUAD（爆破流挑战）

- **「拆迁办」DEMOLITION_SQUAD**（爆破流）：①开局武器替换——`HeroClass.initHero` switch 后统一 `belongings.weapon = new FetalDoctor().identify()` + quickslot0（挑战下旧武器被字段覆盖丢弃）；②胎儿博士挑战特性——`FetalDoctor` 覆写 `STRReq(lvl)`（挑战下恒 10）与 `doUnequip`（挑战下返 false=**无法卸下**，替换/丢弃均被拦）；③商店武器→炸弹——`ShopRoom.generateItems` 中挑战下把近战/投掷/毒镖三件货（`w`/`m`/`TippedDart`）全部换成 `new Bomb()`。

## 3. 钻石矿墙（Terrain.MINE_DIAMOND）

- **钻石矿墙（Terrain.MINE_DIAMOND=39，2026-09-04 彩蛋，MC 钻石剑的前置掉落）**：采矿层（MiningLevel）可挖矿墙第三种（另两种=水晶 MINE_CRYSTAL/巨石 MINE_BOULDER），flags=WALL（SOLID|LOS_BLOCKING，比 crystal/boulder 的纯 SOLID 更硬——它们不像墙那样挡视线）。**帧号由用户在 `tiles_caves_crystal.png`/`tiles_caves_gnoll.png` 向下扩展两行绘制**：`FLAT_MINE_DIAMOND=256/ALT=257`、`RAISED_MINE_DIAMOND=258~261`（右/左开口 4 帧组，同 RAISED_WALL_DECO 结构）、`RAISED_MINE_DIAMOND_ALT=262~265`、`WALL_OVERHANG_MINE_DIAMOND=266~269`（4 缝合，同 WALL_OVERHANG_DECO）、`WALL_INTERNAL_MINE_DIAMOND=272~287`（16 缝合，同 WALL_INTERNAL_DECO）。接入点：①`DungeonTileSheet` wallStitcheable 数组加 MINE_DIAMOND（否则不按墙缝合、地面 tilemap 不渲染 raised）；②`getRaisedWallTile`/`stitchInternalWallTile`/`stitchWallOverhangTile` 加 MINE_DIAMOND 分支（internal/overhang 与 WALL_DECO 相同仅采矿层 branch==1 触发）；③`directFlatVisuals.put(MINE_DIAMOND, FLAT_MINE_DIAMOND)` + `commonAltVisuals` 两处（FLAT/RAISED 各有 ALT）；④**生成**：`MiningLevelPainter.setDiamonds(3)`（链式 setter 返回 `MiningLevelPainter` 自身、**须置于 setGold 之前**——setGold 返回 RegularPainter 会丢掉子类方法；但实际调用顺序是 `MiningLevel.painter()` 里先 `.setDiamonds(3).setGold(...)`），generateGold 末尾追加放置逻辑——**只从普通房间（跳过 MineSecretRoom——隐藏门内难找）的 `Terrain.WALL` 中选"至少一面 4 邻非墙"的墙**（即玩家可见/可挖的面，不会埋进完全封闭墙内），shuffle 后取前 N；⑤**挖掘**：`Hero.handle`（点击判定）与 `Hero.actMine`（落地分支）都加 `Terrain.MINE_DIAMOND`——actMine 分支掉 1 个 `Diamond` 到地面（`Dungeon.level.drop(...).sprite.drop()`，浅蓝 Splash 0x9FE7FF + SHATTER 音效）后地形变 `EMPTY_DECO`；⑥**道具**：`items/Diamond.java`（stackable、不可升级、恒已鉴定），图标 `ItemSpriteSheet.DIAMOND = QUEST+10`（items.png **30 行 11 列**，QUEST 区 xy(1,30) 起第 11 格），描述"闪闪发光的钻石，似乎并不是属于这个世界的矿物。"（`items.diamond.name/desc`）；⑦**墙体查看文本**：`MiningLevel.tileName/tileDesc` 加 `diamond_name/diamond_desc`（levels_zh.properties 与 levels.properties 英文均补，键 `levels.mininglevel.diamond_name/desc`）；⑧**矿点闪烁**：`CavesLevel.addCavesVisuals` 判定扩展为 `WALL_DECO || MINE_DIAMOND`（Vein 白色粒子提示矿点），且 `Vein.update` 的存活检查同样加 MINE_DIAMOND（否则挖掉后粒子不消失/或误杀）。

## 4. 钻石剑（DiamondSword）与铁匠兑换

- **钻石剑（四阶近战，2026-09-04 MC 彩蛋，钻石矿的奖励闭环）**：`items/weapon/melee/DiamondSword.java extends MeleeWeapon`——`tier=4`（力量需求沿用四阶默认）、图标 `ItemSpriteSheet.DIAMOND_SWORD = xy(12, 36)`（items.png **36 行 12 列**，尺寸 16×16，assignItemRect(16,16)）、**无武技/特效**（不覆写 duelistAbility/targetingPrompt，与 FetalDoctor 等自定义武器一致——决斗者拿它武技为空属预期现状）、**伤害公式覆写 `min(lvl)=5+lvl` / `max(lvl)=30+6*lvl`**（即 5+L~30+6L，MeleeWeapon 默认是 tier+lvl / 5(tier+1)+lvl(tier+1)，须显式覆写 min/max 如 FetalDoctor 模式）。**奖励机制（2026-09-04 改为"离开矿层入账 + 兑换窗口消耗"两段式，仿暗金→favor）**：①`Blacksmith.Quest` 新增静态 `diamondCount`（序列化键 `diamond_count`、reset 置 0、旧档缺键 getInt 返 0 天然安全）；②`Blacksmith.Quest.complete()`（唯一调用点 `MiningLevel.activateTransition` 确认离开矿层）里仿暗金 favor 逻辑——背包 `Diamond` detachAll 并 `diamondCount += quantity`（**不再直接发剑**）；③`Quest.rewardsAvailable()` 增加 `diamondCount >= 2` 分支（否则完成且人情花光后铁匠不再开窗口、Notes landmark 也被移除）；④`WndBlacksmith` 兑换窗口加"打造钻石剑"按钮（`Messages.get(this,"diamond_sword",2)`，enable 条件 `diamondCount>=2`），确认后 `diamondCount -= 2`、`new DiamondSword().identify()`、doPickUp（失败 drop hero.pos）；⑤窗口顶部在 `diamondCount>0` 时显示入账提示行（键 `windows.wndblacksmith.diamond_count`，布局注意按钮列表起始 y 用变量 pos 承接提示行）。文本键：`items.weapon.melee.diamondsword.{name/desc}`（zh+en，desc=用户给定"使用钻石制造的剑刃，相比金属武器来说更加锋利耐用"）、`windows.wndblacksmith.diamond_sword/_verify/_yes/_no` + `diamond_count`（zh+en，旧 `actors.mobs.npcs.blacksmith.diamond_sword_reward` 键已随直发逻辑删除）。钻石道具图标尺寸 12×13（`assignItemRect(DIAMOND, 12, 13)`）。

- **钻石剑专属附魔系统（2026-09-06，MC 附魔台彩蛋，含引擎改动）**：钻石剑**无法获得任何普通附魔**，封禁入口有两道：①`ScrollOfEnchantment.enchantable()` 增加 `!(item instanceof DiamondSword)`（附魔卷轴不可选，同文件 import DiamondSword）；②`DiamondSword` 覆写 `enchant(Enchantment)` 与无参 `enchant()` 均 `return this` 静默拦截——覆盖卷轴/诅咒/铁匠 smith 等一切 `Weapon.enchant(...)` 写入。**专属附魔入口**：`WndBlacksmith` 在"打造钻石剑"按钮后新增「_为钻石剑附魔(%d颗钻石)_」按钮——仅当 `findDiamondSword(hero)!=null && !cursed && canEnchantMore()` 时显示，enable=`diamondCount>=1`；`findDiamondSword`（静态，主手→副手→背包 `getItem(DiamondSword.class)` 优先序）；确认（WndOptions）后 `diamondCount-=1` → `sword.enchantOnce()` → 隐藏窗口 → `WndTitledMessage` 展示本次结果行（文本键 `windows.wndblacksmith.diamond_enchant/_verify/_yes/_no/_title/_result/_none`，zh+en）。**六种专属附魔**以剑自身字段存等级（Bundle 序列化键同名）：`sharpness 锋利0~5 / smite 亡灵杀手0~5 / baneArthropod 节肢杀手0~5 / fireAspect 火焰附加0~2 / knockback 击退0~2 / looting 抢夺0~3`；**锋利/亡灵杀手/节肢杀手三选一互斥**——判定统一收敛到 `conflictsWithOwned(type)`（以"剑上当前附魔、含本轮新增"为准），`canEnchantMore` 同源复用。**`enchantOnce()` 复刻 MC Java 版附魔台工序**：c=30、l=10（钻石）→ c'=c+1+randInt(⌊l/4⌋+1)×2 再乘 `1+0.15*(randFloat+randFloat-1)` 取整 ≈26~40；等级范围表与权重提为类常量 `ENCHANT_RANGES[][]`（MC wiki Java 版各等级 min~max 区间）与 `ENCHANT_WEIGHTS={10,5,5,5,2,2}`（锋利/亡灵/节肢/击退/火焰/抢夺）；**每轮流程 = 加权抽 1 项 → 应用 → 以 `Random.Int(50)<=c'` 判续（是则 c' 折半）→ 用最新 c' 经 `enchantPool` 重建候选池**——剔除"本次已抽 `pickedThisOp`"、`conflictsWithOwned` 冲突者（含刚获得/升级的，故同一轮附魔里绝不会锋利与亡灵杀手并存）、已达上限者，并按新 c' 经 `rolledLevelAt` 重算各类型可出现的等级（续抽轮等级会随折半的 c' 变低，与 MC 一致）。**2026-09-06 修复冲突漏洞**：旧实现候选池仅在附魔开始时按"剑上原有附魔"构建一次、续抽时只删已选项，未把本轮新获得的附魔纳入互斥 → 空剑一次附魔连抽两轮时可能同时拿到锋利与亡灵杀手；`restoreFromBundle` 另加互斥自愈——同组多个时按 锋利>亡灵杀手>节肢杀手 保留其一、其余清零（旧档冲突剑读档即修正，显示与 proc 的 else-if 实际生效顺序一致）。**MC 附魔流光显示（2026-09-06 晚，引擎级改动 `sprites/ItemSprite.java`）**：钻石剑拥有任一专属附魔时（`DiamondSword.hasEnchants()`），其图标（出现在背包/装备/快捷栏/地面/物品详情 WndInfoItem 等所有 `ItemSprite` 路径）会再叠加半透明的 MC 风格附魔光效——附魔贴图 `assets/sprites/enchanted_item_glint.png`（256×256 紫黑相间的噪声图，用户从 `D:\PD\enchanted_item.png` 移入；常量 `Assets.Sprites.ENCHANTED_GLINT`）；实现要点：①**新字段** `ItemSprite.viewItem`（`view(Item)`/`view(Heap)` 跟踪最近一次传入物品；chest/tomb 等非物品堆刷新时置 null；`revive` 也置 null）；②**流光 = ItemSprite 自有的第二张 quad**（顶点数组 `glintVertices` + `glintVB`/`glintBuffer`，仿引擎阴影绘制路径），在 `ItemSprite.draw()` 的 `super.draw()` 之后手动上传并绘制：**与图标共用同一 `matrix` 与 `camera()`，仅换绑 glint 贴图再 drawQuad**——这样无论图标处于哪个容器都严格重合（**重要教训**：`Window` 会为自身 `new Camera`，`ItemSprite.camera()` 沿 parent 链解析到窗口相机，而游离 Image（无 parent）只能回落到 `Camera.main`，导致背包里流光画到地图上、且呈整块方框——第一版即此 bug，故不可用"游离 Image 复制 x/y 再 draw"的做法）；③**双方向滚动**：`update()` 中每帧 `glintOffX/Y += SCROLL*Game.elapsed` 并 `% 256` 环绕，`updateGlintFrame()` 把 quad 的 UV 设为宽度 = `this.width`（图标宽度 16）的滑动窗口（位置恒为 0,0-w,h，随图标的 matrix 变换）——左→右 + 下→上同时循环，速度 16 px/s（≈16 秒一周期）；④生命周期：`refreshGlint()` 在三个 `view` 路径按 `glintOn = wantGlint()` 懒创建缓冲（首次需要时创建，且无论新建还是复用都立即 `updateGlintFrame()` 按当前图标帧重建顶点——缓冲在 kill/revive 后保留复用，避免沿用旧帧遮罩 UV）；**GPU 缓冲只在 `destroy()`（场景整组销毁、GL 线程）经 `releaseGlint()` 释放**（delete GPU 缓冲并置空，幂等；贴图由 TextureCache 托管勿 delete）。`kill()`/`revive()` **严禁碰 GL**：它们会在 Actor 线程被触发（拾取物品 `Heap.pickUp→Heap.destroy→sprite.kill`、GameScene 回收 heap 精灵 `recycle→revive`），该线程无 GL context，delete 缓冲即 `FATAL ERROR in native method` JVM abort——这两处只关 `glintOn` 标志，缓冲随精灵实例保留复用。**所有位置皆显示**：`ItemSprite` 是全引擎物品图标统一入口（背包 ItemButton/快捷栏 QuickSlotButton/WndInfoItem/地面堆 heap 等都构造 ItemSprite），一处 hook 覆盖全场景。**成长规则**（用户拍板）：剑上无此魔咒 → 赋予本次抽中等级；已有 → 等级 +1（不超上限）；不会选中与已有附魔冲突的魔咒。**战斗结算 `proc()`**（`super.proc` 之后、MagicImmune 失效同普通附魔）：增伤类取**护甲减免后伤害**的百分比——锋利对一切目标 +`5%×等级`；亡灵杀手仅对 `UNDEAD||DEMONIC`（范围同 WandOfPrismaticLight 增伤判定，含骷髅/食尸鬼/幽灵/死灵法师/矮人王/卫兵/武僧/盗贼/术士/裂魔等及其子类）`+8%×等级`；节肢杀手仅对 `instanceof Crab/Spinner/Swarm/Scorpio`（螃蟹族/蛛族/蝇群/蝎族——mod 无共同 Property，用 instanceof；YogScorpio 是 YogDzewa 内部类但 extends Scorpio 自然覆盖）`+8%×等级` 并 `Buff.affect(defender, Cripple.class, 5f)`。非增伤类**按附魔等级等效武器 buffedLvl 代入原版公式**：火焰附加=同等级烈焰（触发 `(lvl+1)/(lvl+3)`、点燃 8f、点燃后灼烧 `round(rand(1,3+depth/4)*0.67*powerMulti)`、焰粒子 `lvl+1` 簇）；击退=同等级弹性（触发 `(lvl+1)/(lvl+5)`、`WandOfBlastWave.throwChar(...,round(2*powerMulti),true,true,this)`——剑为近战故第 4 参恒 true）；抢夺=同等级幸运（触发 `(lvl+4)/(lvl+40)`、命中 `Buff.affect(Lucky.LuckProc)` 并 `setRingLevel(-10+round(5*powerMulti))`、未命中 detach 已有标记——Mob.die→genLoot 消费掉落，与 Lucky 附魔共用一套引擎）。**引擎改动**：`Lucky.LuckProc` 原私有 `ringLevel`（默认-5）补 `public void setRingLevel(int)`（同包内 Lucky.proc 仍直写字段，跨包 DiamondSword 走 setter）。**展示**：`statsInfo()` 覆写→无附魔回落 `super`（默认空）；有附魔则整段替换为需求格式 `这把武器具有附魔：` + 每魔咒独立一行 `\n\n_name+罗马数字_`（下划线=斜体、**1 级不显示罗马数字**、`toRoman` 仅 2→II/3→III/4→IV/5→V、名字不带空格直接拼缀如 `_锋利IV_`），无任何逐条说明文字。附魔按钮与结果文案键位于 `windows.wndblacksmith.diamond_enchant*`；**附魔名中文硬编码于 `enchantName()`/`enchantInfo()`**（沿用本 mod zh 优先、EN 兜底占位惯例）。**2026-09-06 深夜 遮罩裁剪流光（防"整块方框盖在图标上"）**：上述 quad 流光若不加裁剪，会在整个图标矩形上平铺噪声，细长物品周围露出大块光斑——因此给流光引入**双纹理遮罩 shader**：新增引擎类 `com.watabou.noosa.NoosaScriptGlint extends NoosaScript`（SPD-classes 模块，`:core:compileJava` 会连带编译 project 依赖）：片元 = `texture2D(uTex,vUV)*uColorM * texture2D(uMask,vMaskUV).a + uColorA`（alpha 同时乘到 rgb，保持预乘一致性）；采样器 `uMask` 固定绑定纹理单元 1（`use()` 覆写中 `glUniform1i(uMask.location(),1)`，aMaskUV enable），顶点格式扩为 **6 float/顶点：x,y,u,v,maskU,maskV**（新增 `drawMaskedQuad(Vertexbuffer)`：`aXY(2,6,0)/aUV(2,6,2)/aMaskUV(2,6,4)`），`bindMask(SmartTexture)` 把遮罩贴图绑到单元 1 并恢复单元 0 为激活态（引擎其余绘制都假定单元 0 激活，**必须恢复**）。`ItemSprite` 流光侧：`glintVertices` 24 float，`updateGlintFrame()` 里 maskU/V **直接拷贝图标 quad 的 UV**（读 `this.vertices` 槽 2,3 步长 4，已含 flip 语义）——于是**遮罩 = 图标贴图（items.png）当前帧的 alpha**，图标透明像素处流光被滤除、不透明处正常显示，与图标逐像素对齐且**无需任何人工绘制遮罩资源**；`draw()` 流光段改用 `NoosaScriptGlint.get()`：`bindMask(texture)`（单元1=图标贴图）→ `glintTex.bind()`（单元0=噪声）→ `camera/uModel/lighting/drawMaskedQuad`。**2026-09-06 深夜二修（"丢出后地面光效仍方形 + 拾取闪退"，同一次编译后反馈）**：①**方形根因**——引擎 `Texture.bind()` 用**单一静态 `bound_id`** 记录"当前绑定纹理"且**不区分纹理单元**；图标贴图 items.png 恰在流光绘制之前刚于单元 0 绑过（`super.draw()`），`bindMask()` 里的 `mask.bind()` 因 `bound_id==mask.id` 短路跳过 glBindTexture → **单元 1 从未真正绑上遮罩**，shader 采样默认空纹理（alpha=1）→ 遮罩恒失效、光效恒为整块方形。修复：`bindMask()` 改为 `Texture.activate(1)` 后用**原始 `Gdx.gl.glBindTexture(GL_TEXTURE_2D, mask.id)` 强制绑定**（`Texture.id` 为 public），`bound_id` 语义（代表单元 0 当前绑定）不受影响。②**闪退根因**——`ItemSprite.kill()`/`revive()` 内调用了 `releaseGlint()`（`Vertexbuffer.delete` = GL 调用），而拾取地面堆时 `Heap.pickUp→Heap.destroy→sprite.kill()` 与 GameScene 回收 heap 精灵 `recycle→revive()` 都发生在 **Actor 线程（无 GL context）** → `FATAL ERROR in native method`。修复：GL 释放只保留在 `destroy()`；`kill()`/`revive()` 仅置 `glintOn=false` 等 CPU 态，缓冲随精灵实例保留复用（与引擎 Image 主缓冲同生命周期模型）。

## 神器「爱慕」与召唤物（2026-09-05）

- 神器 `items/artifacts/Admiration.java`（爱慕）：充能上限 100（状态栏按百分比显示），**四档贴图随充能切换**（0-24/25-49/50-74/75-100，items.png 29 行 11~14 列）。未召唤时充能每回合 +2（`CHARGE_PER_TURN` 常量，可调）蓄满自动召唤「溶解之爱」；召唤后充能实时=溶解之爱的血量百分比。
- 召唤物 `actors/mobs/MeltingLove.java`（溶解之爱，`sprites/MeltingLoveSprite.java`，love.png 10 帧 16×16：待机1/2、移动3、攻击4-6、死亡7-10）：`extends DirectableAlly`（自动跟随英雄/攻击敌人），数值套蜜蜂公式，等效等级=神器等级×3+5；命中施加 20 回合腐蚀淤泥（`Ooze.set(20f)`）；击杀目标（经 `Mob.die` 中 cause instanceof MeltingLove 回调）在目标处生成友方粉色史莱姆。
- 等级成长：溶解之爱存活时按存活回合累积，每满 150+50n 回合（n=升级前等级）神器 +1 级（上限 levelCap=10），召唤物数值随之成长（保留 HP 百分比）。升级走 `Item.upgrade()`（配合 artifact 的 level 体系）。
- 诅咒与反噬：初始必定 `cursed=true` 但**不影响行为**（诅咒中按默认规则不可卸下）；祛除诅咒后若在溶解之爱存活时卸下 → 红色提示"溶解之爱焦躁不安地看着你的方向"并挂 10 回合 `BetrayalTimer`；期间重新装备（`activate`）取消；超时则 `MeltingLove.betray(hero)`（转为 ENEMY 攻击英雄）。
- 粉色史莱姆 `actors/mobs/PinkSlime.java` + `sprites/PinkSlimeSprite.java`：数值/动画帧配置完全套用史莱姆（pinkslime.png 与 slime.png 同为 128×32），仅阵营为 ALLY、EXP/掉率为 0；名称键 `actors.mobs.pinkslime.name`。
- 说明/注意：溶解之爱"无法装备武器/防具"由设计保证（无任何装备入口）；Ooze 显示名为"腐蚀淤泥"；若需更快蓄能改 `CHARGE_PER_TURN`。
- 修正（2026-09-05 二轮）：①待机动画降低帧 2 出现频率（拉长循环）；②击杀生成粉色史莱姆改在 `Mob.die` 的 `super.die` 之后回调（目标移除、格子空出），回调参数改为格坐标 `onKill(pos)`；③溶解之爱新增生命恢复：每 (15-n) 回合 +1 HP（n=神器等级，最小间隔 1）；④BetrayalTimer 补 name/desc（"爱慕的反噬倒计时"，显示剩余回合）；⑤溶解之爱 description() 补官方描述文本。
- 修正（2026-09-06，**崩溃 bug**：召唤/读档即崩 `NoSuchMethodException: Admiration$LoveSync.<init>()`）：`LoveSync`/`BetrayalTimer` 原为非静态内部类，而 `Buff.affect`/读档恢复走 `Reflection.newInstance` 反射**无参构造**——非静态内部类隐式带外层引用参数、无无参构造器 → 实例化失败 buff=null → NPE 崩游戏。已改为 `public static class`（二者均不引用外层 `Admiration.this`，只经 `findAdmirationInInventory` 查实例，语义不变）。**仓库惯例**：凡经 `Buff.affect`/存档反射重建的嵌套 Buff 必须 static（参照 `SkeletonKey.KeyReplacementTracker`、`UnstableSpellbook.ExploitHandler`）；ArtifactBuff 走 `passiveBuff()`+`attachTo` 直挂则维持非静态。
- 结局彩蛋（2026-09-06，仿枯萎玫瑰）：幸福结局结算画面（`scenes/SurfaceScene.java`，英雄带护符走上地表时由 `SewerLevel.activateTransition` 切入，`Badges.validateHappyEnd` 确认）原版只按等级择优显示玫瑰幽灵（枯萎玫瑰 `level()/2>=3`）/地灵/守卫三者之一作为"同伴"。现新增最高优先级分支：英雄携带爱慕神器（装备槽或背包，`belongings.getItem`）**且曾成功召唤过溶解之爱**（判据=英雄身上存在 `Admiration.LoveSync` buff——LoveSync 只在 `summonLove` 成功后挂载、爱慕仍在身上即常驻，是最可靠"曾拥有溶解之爱"的存档持久标志）→ 在英雄左侧同伴位渲染 `new MeltingLoveSprite()`（与幽灵同款 `PARALYSED` 定格、scale 2 摆放）。与玫瑰一致属"纪念式"展示：不检查溶解之爱此刻是否存活/在场（可能已死于途中）。爱慕优先级置于玫瑰幽灵之前，二者不会同时显示（画面同伴位唯一）。
- 修正（2026-09-08，**第二神器栏失效 bug**）：本 mod 装备模型为 KindofMisc 三槽（`artifact`/`misc`/`ring`，见 `KindofMisc.doEquip`）——神器可装备进 **`artifact`（第一神器栏）**，该槽被占时落入 **`misc` 槽（第二神器栏）**，两槽装备都会 `activate()` 挂 passive buff（读档 `Belongings.restoreFromBundle` 亦对 misc 调 `activate`）。但 `findAdmirationInInventory` 原**只查 artifact 槽 + 背包、漏掉 misc 槽** → 爱慕在第二栏时：`LoveSync` 找不到神器 → ①充能↔血量同步中断（召唤后充能恒卡 100，血条变化不再反映）；②升级计时（survivalTurns）不累计 → 无法升级；③溶解之爱一死，`AdmirationBuff` 见充能仍满 → **立刻原地重召**。修复：查找逻辑补 `belongings.misc instanceof Admiration` 分支（置于 artifact 之后、背包之前）；`BetrayalTimer` 的"重新装备则取消反噬"判定同步补 misc（正常装备路径已由 `activate()` 取消倒计时，此处为一致性兜底）。**仓库惯例**：凡"从英雄身上查找某装备/神器"的辅助方法，若物品可进 misc/ring 槽，须连同检查，勿只查主槽+背包。
- 新增（2026-09-10，**溶解之爱精灵图重制 + 「赠予」玩法**）：
  - **新贴图**：`Assets.Sprites.LOVENEW = "sprites/lovenew.png"`、`LOVEANGER = "sprites/loveanger.png"`（旧 `Sprites.LOVE` 已废弃删除）。
    - `lovenew.png`：单帧 **16×24**，每行 10 帧、共 4 行 = **两组**（组内 2 行 = 20 帧）。组内帧序：1 默认、2 待机回头、3 移动、4~6 攻击、7~10 死亡、11 尴尬、12 快乐、13 焦虑、14~15 黑化。常量 `LOVENEW_COLS=10`、`LOVENEW_GROUP_STEP=20`，组基址 `b = palette * 20`。**默认使用第一组（`PALETTE_A=0`）**。
    - `loveanger.png`：单帧 **20×24**，单行 6 帧：1 默认、2 移动、3~4 攻击、5~6 死亡（反噬形态，无表情/黑化帧）。
  - **动画配置**（`sprites/MeltingLoveSprite.java`）：待机参考 `GuardSprite` 的回头范式 `0,0,0,1,0,0,1,1`（fps 2、循环），移动单帧 `b+2`（fps 10），攻击 `b+3..b+5`，死亡 `b+6..b+9`。「尴尬 / 快乐」为定格式一次性表情（同帧重复 8 次、fps 4，播完 `onComplete` 自动回待机）；「焦虑」为循环持续表情（fps 2，直到 `clearAnxious()`）；「黑化」为两帧一次性过渡（`b+13..b+14`），播完 `onComplete` 自动 `configureAnger()` 换成 loveanger 配置。**贴图宽度 16→20 切换后需 `place(ch.pos)` 重新对齐格子**（移动中不动，交给引擎下一格处理）。
  - **形态持久化**：`MeltingLove` 新增字段 `imageGroup`（图像组）与 `angry`（是否已反噬），双双进 Bundle（`image_group` / `anger`，旧档缺键默认 0/false）；`MeltingLoveSprite.link()` 在精灵被任意时刻重建（读档/场景重载）时读取怪物身上的状态重建形态。神器侧 `Admiration.lovePalette` 同步进 Bundle（`love_palette`），`summonLove` 把该组号传给 `MeltingLove.summon(hero, cell, artLevel, palette)` → **嬗变切换后的形态在死亡重召后依然保持**。
  - **「赠予」动作**（`Admiration`，`AC_GIFT = "GIFT"`）：仅在 `isEquipped(hero)` 时出现在动作菜单（`actionName` = 「赠予」）。点击后经 `GameScene.selectItem(giftSelector)` 打开背包选择器（仅可选 `Potion` 或 `Scroll`）；若场上无存活的忠诚溶解之爱则提示并不进入选择。分支：
    - **嬗变卷轴**（`ScrollOfTransmutation`）→ 消耗，在溶解之爱身上播嬗变粒子（`Speck.CHANGE` + `Assets.Sounds.READ`），**组在 A/B 间往返切换**，文本「溶解之爱在触碰到这张卷轴时，发生了奇妙的变化！」。
    - 其它卷轴 → 返还、不生效，播**尴尬**，「溶解之爱读不懂这张卷轴...」。
    - **拒绝类药水**（`REFUSE_POTIONS`：力量药剂 / 根骨秘药 / 经验药水 / 神圣启示药水）→ 返还、不生效，播**快乐**，「溶解之爱喜欢你的礼物，但她觉得你比她更需要它。」
    - **负面药水**（`NEGATIVE_POTIONS`：剧毒瓦斯 / 燃烧之焰 / 麻痹瓦斯 / 冰霜 / 腐蚀瓦斯 / 瞬间冻结 / 遮蔽迷雾 / 风暴云 / 龙息）→ 返还、不生效，播**尴尬**，「溶解之爱似乎不太喜欢这份礼物...」。
    - 其它**正面药水** → 消耗并施加对应效果（`applyPotionEffect` 逐种映射：治愈/蜂蜜治愈→治疗；迅捷/隐身/飘浮/净化→`Buff.prolong`；羽落→`FeatherBuff`；解咒→`cleanse`；护盾→`Barrier`；奥术护甲→`ArcaneArmor`；大地护甲→`Barkskin`；疾跑→`Stamina`；其余统一治疗+回血），播**快乐**，「溶解之爱因你的礼物而感到心生爱意。」
  - **焦虑 / 反噬的精灵表现**：`doUnequip` 在进入 10 回合反噬倒计时时调 `love.beginAnxious()`（播焦虑）；`BetrayalTimer.act()` 每回合重申 `beginAnxious()`（移动中只置标志，`Sprite.idle()` 移动结束后接上，不打断跑动）；`detach()` 收尾调 `endAnxious()`。倒计时归零 `love.betray(hero)` → `angry=true` 并 `darkenThenAnger()`（播黑化两帧 → 自动换 loveanger 配置）。**关键**：黑化/换形态不改变 `palette`，故反噬形态切换**不影响**叛变前溶解之爱处于第一组还是第二组。
  - 修正（2026-09-11，**移动不转身 + 停止后不回待机 + 快捷栏赠予**）：
    - **移动不转身的根因**：引擎 `CharSprite.move(from,to)` 是"先 `turnTo()` 再 `play(run)`"，而 `flipHorizontal` 只在 `Image.frame()` 里被写进顶点（`Image.updateFrame`）。原移动动画**只有一帧**且循环 → 帧索引永不变化 → `MovieClip.updateAnimation()` 永不调用 `frame()` → 翻转不写进顶点；同时 `play(run)` 在 `curAnim==run`（循环中）时会直接早退，也刷新不了。修法：① 移动帧写成**两帧同姿势**（索引在 0↔1 间切换，从而周期性刷新顶点）；② 覆写 `move(from,to)`，在 `super` 之后若 `curAnim==run` 则 `play(run,true)` 强制重播以立即刷新顶点；③ `loveanger` 的 run 同样写成两帧。
    - **停止移动回待机**：本仓 `Mob`/`Char` 侧没有 run→idle 的收尾（对比英雄侧是 `Hero.ready()` 里的 `if (sprite.looping()) sprite.idle()`），故覆写 `onComplete(Tweener)`：移动补间结束（`tweener==motion` 且 `ch.isAlive()`）时 `idle()`。**必须在 `super` 之前调用**——super 的 `notifyAll()` 会唤醒行为线程、可能立刻发起下一次 `move()`，放在之后就把它覆盖了。死亡时（`!isAlive()`）不 idle，避免打断 die 动画丢掉 `MobSprite` 的淡出回调。
    - **快捷栏「赠予」**：覆写 `Admiration.defaultAction()`——装备期间返回 `AC_GIFT`，未装备沿用父类（默认动作为空，点按无反应，与改动前一致）。`Item.execute(Hero)` 每次现取该访问器（`ui/QuickSlotButton.java:102` 等），故把爱慕放进快捷栏 / 工具栏点一下即进入赠予流程。
## 新增普通武器三件（2026-09-07，用户自建非 EGO）

- **意志之刃（四阶近战，`melee/WillBlade.java extends MeleeWeapon`）**：图标 `ItemSpriteSheet.WILL_BLADE = xy(6,37)`（items.png 37 行 6 列，16×16），`tier=4`、**伤害公式默认**（min=tier+L、max=5(tier+1)+L(tier+1)，不覆写 min/max）。**效果（仿奥术聚酯临时等级）**：武器以"背包中升级卷轴（`ScrollOfUpgrade`）总数"获得等量临时等级——`scrollBonus()` 遍历 `Dungeon.hero.belongings.getBags()`（**含卷轴筒 ScrollHolder 等小背包内**）累加 `ScrollOfUpgrade.quantity()`；在 `buffedLvl()` 覆写里 `super.buffedLvl()+scrollBonus()`——**选 buffedLvl 而非 level()**：Item.level() 会连带影响 STRReq/售价（value 按 level 乘算，会造"囤卷轴卖高价"漏洞），buffedLvl 恰是引擎给"临时增益等级"预留的钩子（同 degrade 反向语义），只提升伤害（damageRoll 经 min()/max()→buffedLvl）与已鉴定时的伤害显示。无武技/附魔特效（与 DiamondSword 一致）。已注册生成池 **WEP_T4 尾部追加（权重 1）**。
- **磨损对剑（一阶近战，`melee/WornTwinSword.java extends Gloves`）**：图标 `ItemSpriteSheet.WORN_TWIN_SWORD = xy(7,36)`（36 行 7 列，14×15，assignItemRect(14,15)）。**直接继承拳套**：tier=1、`DLY=0.5`（2 倍攻速）、`min/max` 沿用拳套覆写 = **1+L ~ 5+L**（正是需求数值，无需再覆写），继承其决斗家 combo 武技；只换 image/hitSound（HIT_SLASH, pitch 1）。继承 `bones=false`（同拳套不落遗骨）。**未加入生成池**（按用户要求，获取途径后续另行设计）。
- **空酒瓶（一阶投掷，`missiles/EmptyBottle.java extends MissileWeapon`）**：图标 `ItemSpriteSheet.EMPTY_BOTTLE = xy(8,36)`（36 行 8 列，16×16）。覆写 `min(lvl)=10+L` / `max(lvl)=25+L`（高伤）；**耐久为 1**：覆写 `durabilityPerUse(int)=MAX_DURABILITY+0.001f`（每次投掷扣除全部耐久即碎，不落不粘；因 >100f 故无 has_broken 日志，静默碎裂）、`defaultQuantity()=1`（单只不堆叠）。**未加入生成池**（按用户要求）。
- 文本键（items_zh.properties 追加）：`items.weapon.melee.willblade.{name/desc/stats_desc}`、`items.weapon.melee.worntwinsword.*`、`items.weapon.missiles.emptybottle.*`（desc/stats_desc 均为用户给定文案）。

## 新增：烙印工坊 + 蜡翼（2026-09-08，用户自建，火系联动套装）

- **烙印工坊（三阶近战，`melee/BrandWorkshop.java extends MeleeWeapon`，非 EGO）**：图标 `ItemSpriteSheet.BRAND_WORKSHOP = xy(7,37)`（37 行 7 列，**14×14**——2026-09-07 用户补丁修正，原误填 16×16）。面板 **3+L ~ 16+4L**（覆写 min/max）。**效果**：命中附加本次伤害 **20% 的火焰伤害**（`proc` 内 `defender.damage(Math.round(damage*0.2f), this)` + 火焰粒子，**不附加燃烧**——刻意不 reignite）；若带烈焰附魔（Blazing）则常驻 **+60% 附魔强度**。**已加入生成池 WEP_T3 尾部（权重 1）**。
- **蜡翼（五阶 E.G.O，`melee/WaxWing.java extends MeleeWeapon`）**：图标 `ItemSpriteSheet.WAX_WING = xy(8,37)`（37 行 8 列，**15×15**——2026-09-07 用户补丁修正，原误填 16×16）。面板 **5+L ~ 24+6L**。**效果**：① 对燃烧目标命中时本次伤害上限按目标剩余燃烧回合数提升（引擎掷点在命中前 → `proc` 里按"随机 0~剩余回合 附加"等效近似，用户确认该口径，统计期望=抬高上限的 +回合/2）；② **持有（主/副手）被攻击时点燃攻击者**——在 `Char.defenseProc` 加蜡翼特判（仿圣宣/失乐园先例），复用静态 `WaxWing.ignite(Char)`（Buff.affect+Burning.reignite 8f+粒子，免疫跳过）；③ 每次命中点燃目标；④ 若带烈焰附魔则 **+200% 附魔强度**。
- **附魔强度加成实现**（关键决策）：`附魔强度`=本 mod 已有的 `Weapon.Enchantment.genericProcChanceMultiplier` 倍率（神的技艺/圣骑士体系同款语义，直接 `multi += 0.6f/2f`）。在 `Weapon.java` 的 `genericProcChanceMultiplier` 末尾新增武器级判定：`attacker instanceof Hero` 且 `attackingWeapon() instanceof BrandWorkshop/WaxWing` 且该武器 `enchantment instanceof Blazing` → 加成。范围与既有 Furioso 附魔强度 Buff 一致（技能/受击期间同样生效）。**仓库惯例**：附魔强度加成一律在 `genericProcChanceMultiplier` 累加，勿改各附魔类。
- **蜡翼注册（EGO 池语义拆分）**：`EGOWeapons.java` 新增 `TIER_5_CRAFT_ONLY`（合成限定数组）——`isEGO`/`tierOf(→5)` 包含之（故**可分解**：(5-1)×5=20 脑啡肽、有 E.G.O 标注），但**不进 `randomOfTier` 的重构随机池**、不进 Generator 掉落池。**仓库惯例**：EGO 若"仅合成获得、不参与重构随机"，放入专属 CRAFT_ONLY 数组，勿塞进 TIER_X 主数组。
- **特殊合成配方（入配方栏）**：`WaxWing.CraftRecipe`：烙印工坊（鉴定且未诅咒）+ 火龙吐息合剂（`PotionOfDragonsBreath`，用户确认即"烈焰吐息合剂"所指）+ 嬗变卷轴（`ScrollOfTransmutation`），**20 能量**（cost()=20）。注册于 `Recipe.java` 的 `threeIngredientRecipes` 尾部 + `QuickRecipe.java` 炼金书样例（三者各耗 1 份）。
- 文本键：`items.weapon.melee.brandworkshop.{name/desc/stats_desc}`、`items.weapon.melee.waxwing.*`（含 `\n` 换行转义）。

## 新增饰物：沉默的代价（2026-09-08，用户自建 Trinket）

- **饰物**（`items/trinkets/SilentPrice.java extends Trinket`，vanilla Trinket 体系：**背包持有即生效、无需装备**，0~3 级）。图标 `ItemSpriteSheet.SILENT_PRICE = xy(2,19)`（19 行 2 列，13×13）。升级走 Trinket 通用炼金配方 `Trinket.UpgradeTrinket`（能量 6+2×lvl），无需额外注册。
- **效果**：持有者伤害 / 免伤 / 精准 / 闪避按等级提升 A%（0..3 级 → 5/10/15/20）；但英雄每次就绪等待输入超过 B 秒（现实时间，0..3 级 → 5/4/3/2 秒）未操作时，自动执行一次等待跳过该回合。
- **四维加成注入点**（全部静态钩子，未持有时返回中性 1，无副作用）：
  - 伤害：`Char.damage()` 入口 `if (src instanceof Hero && src != this)` 乘 `heroDamageMultiplier()`（覆盖近战/远程/法术等一切以英雄为源的伤害，未走 Hero.damageRoll 以免漏投掷/法术）；
  - 免伤：`Hero.damage()` 与十戒同乘区 `dmg *= RingOfTenacity.damageMultiplier * SilentPrice.damageTakenMultiplier()`（乘 `1-A%`）；
  - 精准/闪避：`Char.hit()` 的 `acuRoll *= heroAccuracyMultiplier()`（攻击方为英雄）/ `defRoll *= heroEvasionMultiplier()`（防守方为英雄），仿 FerretTuft。
- **空闲自动等待（新机制，唯一非既有 hook 的实现；2026-09-08 修复驱动逻辑）**：空闲时钟本体放在 `SilentPrice`（静态 `idleAccum`，`tickIdle(float)` 累计/判定、`resetIdleClock()` 清零）；`GameScene.update()`（渲染线程）仅在英雄真正"就绪等待输入"的帧里驱动 `SilentPrice.tickIdle(Game.elapsed)`：条件为 `hero.ready && curAction==null && !resting && isAlive`（**不再排除瞄准/自定义格子监听**，见下）。累计 ≥B 秒 → `Dungeon.hero.spendAndNext(Actor.TICK)`（=busy + 花费整回合 + next()；其 next() 顺带清空 `Actor.current` 解除 processing，使既有通知逻辑在下一帧唤醒回合线程完成跳回合）。**"操作"的唯一定义是消耗回合时间的动作**：`Hero.spend()`（移动/攻击/使用道具/等待等回合时间消耗的统一入口）调用 `SilentPrice.resetIdleClock()` 清零 → 计时从"最后一次消耗回合的动作"重新起算。**打开背包/查看说明/检查等不消耗回合的界面行为不算操作，依旧计时并照常跳回合**（点开背包时同样会每 B 秒自动等待）。
  - **实现坑（已修）**：不可用 `!Actor.processing()` 当守卫——英雄就绪等待输入时回合线程正以 `Actor.current==hero` park（`act()` 返回 false 后未清 current 即 wait），`Actor.processing()` 恒为 true，若用它当守卫会让累计永远为 0、自动等待永不触发。
  - **瞄准照常计时（2026-09-08 调整）**：法杖瞄准、选择投掷目标等 `cellSelector` 自定义监听期间**同样照常计时**——瞄准不是消耗回合的操作，超时会被强制等待（会打断瞄准）；`cellSelector.listener == defaultCellListener` 守卫已移除。
- **测试入口**：`windows/WndDebug.java` 调试台新增 **「饰品」标签页**（`PKG_TRINKETS` 递归扫描 + `Trinket` 过滤，`trinketsTab = new ItemTab(Trinket.class, PKG_TRINKETS)`），点击即生成到背包（现 10 个标签页，两行排版）。
- **获取（2026-09-10 修复）**：已加入 `Generator.Category.TRINKET.classes` 尾部（`SilentPrice.class`）并同步 `defaultProbs` 追加权重 1（18 项对齐）→ 现在可经 TrinketCatalyst 三选一正常获得、可被蜕变卷轴换出；同时因 `Catalog.TRINKETS.addItems(Generator.Category.TRINKET.classes)`，图鉴/博物馆饰品区自动收录。**教训：新饰物仅写类文件 + 文本键不会进入任何随机池**，必须登记进该数组。
- 文本键 `items.trinkets.silentprice.{name/desc/stats_desc/typical_stats_desc}`；desc 为用户 lore（**不含"沉默的代价："前缀**，名称由 name 键显示）；stats_desc 用 `_%1$d%%_`/`_%2$d_` 占位符动态填 A%、B 秒。饰物恒视为已识别（从催化剂获得即 identify，故面板直接显示数值）。

# 2026-09-08 松脂消耗品：焦炭松脂 / 散装焦炭松脂 / 黄金松脂 / 散装黄金松脂
- **两类涂层 buff**（`actors/buffs`，图标帧 108/109，用户绘制）：基类 `ResinCoatingBuff extends FlavourBuff`（`bonusDamage(Char,int)` 附加元素伤害+粒子、`coat(...)` 互斥施加、`current(...)` 查询）：
  - `CharcoalResinBuff`（帧 108）：临时**烈焰**附魔覆盖原附魔，攻击附带 **10%** 原本伤害的火焰伤害。
  - `GoldenResinBuff`（帧 109）：临时**电击**附魔覆盖原附魔，攻击附带 **20%** 原本伤害的电击伤害。
- **战斗注入点**（仿神圣武器 HolyWepBuff 三处，`items/weapon/Weapon.java`）：
  - `proc()`：新分支先于 HolyWeapon —— 英雄装备该武器且带涂层 → `coating.enchant().proc(...)`（Blazing/Shocking 实例化，复用其点燃/电弧与触发率），随后 `coating.bonusDamage(defender, damage)`（百分比元素伤害）；**原附魔不触发**（覆盖语义）。
  - `glowing()`：涂层期间武器辉光取 `coating.enchant().glowing()`（烈焰橙/电击白）——也是 `Enchanting.show` 虚影的采样色，复现"附魔武器虚影"。
  - `hasEnchant()`：涂层期间仅以涂层附魔类判定（临时附魔覆盖原本附魔，禁用原附魔相关机制）。
- **四个消耗品**（`items/`，基类 `Resin`）：整块松脂使用消耗 **1 回合**，散装松脂**不消耗回合**（快速使用）；需先装备近战武器。贴图 `ItemSpriteSheet` 第 40 行第 6-9 格：xy(6,40)/(8,40) 16×16、散装 xy(7,40)/(9,40) 16×15。恒已识别（`isIdentified()=true`）；同类**可叠加**（基类 `stackable=true`，共用一栏，每次使用经 `Item.detach` 自动扣 1，炼金拆分按数量扣减不受影响）。
  - `CharcoalResin` 200 回合 / `LooseCharcoalResin` 75 回合 / `GoldenResin` 150 回合 / `LooseGoldenResin` 50 回合；焦炭系挂 `CharcoalResinBuff`、黄金系挂 `GoldenResinBuff`。
  - 使用视觉：`hero.sprite.operate` + `Enchanting.show(hero, weapon)` + `Sample READ`（同神圣武器）。
- **炼金配方**（`Recipe.SimpleRecipe`，注册于 `items/Recipe.java` `oneIngredientRecipes`，均不入掉落池、只可炼金获得）：
  - 火焰药水（液火药剂 `PotionOfLiquidFlame`）×1 + **4** 能量 → `CharcoalResin` ×1；
  - 雷鸣魔药（`ShockingBrew`）×1 + **4** 能量 → `GoldenResin` ×1；
  - `CharcoalResin` ×1 → `LooseCharcoalResin` ×2（**0** 能量拆分）；`GoldenResin` ×1 → `LooseGoldenResin` ×2（**0** 能量拆分）。
- **消息键**：物品 `items.{charcoalresin|loosecharcoalresin|goldenresin|loosegoldenresin}.{name/ac_use/desc}` + 通用 `items.resin.{no_weapon/apply}`；buff `actors.buffs.{charcoalresinbuff|goldenresinbuff}.{name/desc}`（desc 中 10%/20% 为裸 `%`，因无参调用不经过 format）。
- 已知取舍：涂层作用于英雄"当前装备武器"（换武器会转移）；投掷物/弓矢命中不触发 proc 分支故不生效；与神圣武器同时存在时涂层优先。

# 2026-09-08 检修批次（5 项：时序 / 格挡 / 分解 / 图标 / 蜡翼）
- **CENSORED 恐惧时序修复**（`items/weapon/melee/CensoredWeapon.java`）：proc 在 `Char.attack` 中早于 `enemy.damage`，而 `Char.damage` 入口会对带 Terror 的目标 `recover()`（-5 回合、≤0 即解除），导致同一次攻击立刻打掉刚挂上的恐惧。同 FrostShard 之法：将 20% 恐惧与固定淤泥包装为一次性 `FlavourBuff`（`actPriority=VFX_PRIO`）`attachTo(defender)`，真正施加延后到伤害结算之后。
- **防御武器格挡显示统一为范围**：机制层 `Hero.drRoll()` 本就是 `Random.NormalIntRange(0, weapon.defenseFactor)`（defenseFactor=上限）→ 所有武器天然"0~X 范围格挡"。差异只在文案：黑天鹅 `stats_desc`/`typical_stats_desc` 由"能格挡 X 点"改为"能格挡 0~X 点"（`assets/messages/items/items_zh.properties`）；凡作-血肉圆盾（CommonWorkC）复核已是 0~%d 范围，未改。
- **未鉴定武器可分解**（`items/Recipe.java` + `Enkephalin.java` + `LiquidMetal.java`）：
  - `Recipe.usableInRecipe`：对近战/可升级投掷武器取消 `cursedKnown` 硬性要求 → 未鉴定（诅咒未知）武器可入炼金釜；已确认诅咒的仍禁止（`(!cursedKnown || !cursed)`）。Wand 分支不受影响。
  - `Enkephalin.DecomposeRecipe`：去掉 `isIdentified()` → EGO 武器未鉴定也可分解为脑啡肽（仍禁已确认诅咒）。
  - `LiquidMetal.Recipe`：去掉 `cursedKnown` → 投掷武器未鉴定也可熔炼为液金（sampleOutput 原有 `levelKnown` 产量分支照常）。
- **存档/排行榜职业图标**（`ui/Icons.get(HeroClass)`）：环指大师 `SEAL`（占位）→ `MASTER_RING`（大师指环）；拇指·前二老板 `ARMOR_VALENCINA`（拇指大衣）→ `PALERMO_SWORD`（巴勒莫对剑，物品类 `PalermoSword`）。
- **蜡翼烈焰附魔强度解禁**（`Weapon.Enchantment.genericProcChanceMultiplier` +200% 分支，无需在蜡翼 proc 里重复结算）：原版 Blazing 的额外火焰附加伤害需要附魔强度>1（通常靠奥术之戒）才对燃烧目标结算；蜡翼带 Blazing 时 `genericProcChanceMultiplier` 加 2f（强度达 3×）→ `Blazing.proc` 自身即对已燃烧目标结算该附加。**2026-09-08 修订（用户纠正认知）**：此解禁仍**要求武器带烈焰附魔**，只是不再需要奥术之戒——早前在 `WaxWing.proc` 补的"未带 Blazing 也按 powerMulti=2f 附加"块违反该语义，已整体移除（proc 恢复为：燃烧目标 0~回合附加 + 每次命中点燃）。

# 2026-09-08 补充修订（蜡翼解禁语义纠正 + 人体派防嬗变）
- **蜡翼解禁语义纠正（用户澄清认知）**：上轮在 `WaxWing.proc` 加的"未带烈焰附魔也按 powerMulti=2f 附加"整块已删除——蜡翼的烈焰附加伤害解禁**仍然要求带烈焰附魔（Blazing）**，只是无需奥术之戒。机制落点本就在 `Weapon.Enchantment.genericProcChanceMultiplier` 的蜡翼+Blazing → `multi += 2f` 分支：带 Blazing 时强度 3×，`Blazing.proc` 的 `procChanceMultiplier`>1 使 powerMulti>1，对已燃烧目标自然结算附加火焰直伤（未燃目标先点燃、powerMulti-1 后仍可能有余量）。proc 区恢复为：燃烧目标 0~剩余回合附加 + 每次命中点燃；`Dungeon`/`Blazing` import 随删除块一并移除（仅类注释仍提及）。
- **人体派作品防嬗变（`items/scrolls/ScrollOfTransmutation.java`）**：`usableOnItem()` 开头新增 `item instanceof BodyArtWeapon → false`——环指大师"人体派作品"家族（`BodyArtWeapon` 及子类：试作-解体刀 / 劣作 ABC / 凡作 ABC / 良作 ABC / 名作 ABC / 生命作·胫骨）携带素材数值（weapon/bone/meat/blood）与额外附魔列表，嬗变 `changeWeapon` 会按 tier 随机化为普通武器导致作品永久消失，故在可选项阶段直接禁用（WndBag `itemSelectable=false → slot.enable(false)`，灰色不可点）。无需改 `changeItem`（其唯一调用方即本卷轴的 `onItemSelected`，被 UI 拦截后不会到达）。

# 2026-09-08 巴勒莫对剑转正（占位→正式 5 阶武器）＋加入掉落池
- **背景**：原 `PalermoSword extends Shortsword` 仅换图换名（占位）；拇指·前二老板的「巴勒莫剑术」（`PalermoFencing`）是**独立技能类**，按 `heroClass==VALENCINA` 分派、不依赖本武器类，故改武器数值不影响剑术。
- **重写 `items/weapon/melee/PalermoSword.java`**：改为 `extends MeleeWeapon`；`image=PALERMO_SWORD`（xy(12,33) 16×16 不变）、`hitSound=HIT_SLASH`、`tier=5`、`DLY=0.5f`（非常快）；覆写 `min(lvl)=5+lvl`、`max(lvl)=18+3*lvl`。无武技（同蜡翼类普通武器）。
- **叠层 buff `actors/buffs/PalermoSwordBuff.java`**（独立顶层类保存档安全，仿 AcceleratingFuture）：`extends Buff`，每次攻击 `gain()` +1 层（上限 `MAX_STACKS=5`），层间线性 `delayMultiplier(n)=1-0.1n`（满层 0.5，攻击延迟减半）；持续时间 `DURATION=3f` 回合，每次攻击**刷新**而非累加，3 回合不攻击即全部消失。图标帧 `BuffIndicator.PALERMO_SWORD=110`（用户绘制），`iconTextDisplay` 显示层数、到期渐隐。
- **结算注入（`actors/hero/Hero.java` 两处）**：
  - `attackDelay()` 装备武器分支尾部：持「连斩」且 `attackingWeapon() instanceof PalermoSword` → `wDelay *= PalermoSwordBuff.delayMultiplier(stacks)`（与上方加速弹/加速剑法/心-不光彩同列乘法系数）。
  - `onAttackComplete()` 在 `spend(attackDelay())` 后：`attackingWeapon() instanceof PalermoSword` → `PalermoSwordBuff.gain(this)`（每次挥击完成即叠层，不论命中；空挥不触发因 attackTarget==null 提前返回）。
- **掉落池（`items/Generator.java`）**：`WEP_T5.classes` 尾部追加 `PalermoSword.class`（第 13 个），`defaultProbs` 同步扩为 13 个 2——与 WillBlade/BrandWorkshop 尾部追加先例一致（旧档安全位）。非 EGO，不进 `EGOWeapons`。
- **消息键**：`items.weapon.melee.palermosword.{name=巴勒莫对剑 / desc=家传宝背景 / stats_desc=非常快的武器+每击-10%×5层效果}`（stats_desc 无参调用、裸 % 安全）；`actors.buffs.palermoswordbuff.{name=连斩 / desc=层数与机制说明}`（带参、% 用 %% 转义）。
- **加速叠加口径（用户问询总结）**：`Hero.attackDelay()` 装备武器时 `延迟 = 1 × delayFactor(DLY·改造) × Π(各临时来源系数)`——巴勒莫连斩每层 ×0.9…0.5（**来源内部线性**）；与加速弹 ×0.5、加速剑法（拇指 T1+2，仅 DLY>1 慢武器）×0.5、心-不光彩 ×(1−delayCut 20%~40%) 为**来源间乘法叠加**。巴勒莫 DLY0.5 属快武器不触发加速剑法门槛。「加速的未来」不进 attackDelay 乘区（只强化剑术斩击），与连斩互不影响。

# 2026-09-09 E.G.O投掷武器体系 + 首两把 E.G.O 投掷（葬花楔 / 新星之声）
- **登记表 `items/weapon/missiles/EGOThrowingWeapons.java`**（镜像近战 EGOWeapons）：`TIER_4={BuryingWedge}`、`TIER_5={NovaVoice}`；含 `isEGO / tierOf / randomOfTier / supportsTier(4|5)`。默认**不入 Generator 掉落池**（"脑啡肽+同阶投掷→重构"的合成定位）。
- **配方扩展（`items/Enkephalin.java`，同一对 Recipe 内按素材类别分流）**：分解 `testIngredients` 接受近战或投掷 E.G.O（沿用 09-08 规则：未鉴定可分解、已确认诅咒禁）→ `(阶-1)×5` 脑啡肽（用通用 `tierOf(Item)`）；重构素材=已鉴定非诅咒武器：近战分支→随机近战 E.G.O（旧逻辑不变），**投掷分支排除飞镖族与 E.G.O 投掷本体**、仅支持 4/5 阶 → 对应阶 E.G.O 投掷（每阶目前一把，产出确定）。`sampleOutput` 也按素材类别返回对应预览。
- **E.G.O 标注（`items/weapon/missiles/MissileWeapon.java` info() 尾部）**：`EGOThrowingWeapons.isEGO(this)` → 追加 `这件武器是_E.G.O武器_。`（镜像 MeleeWeapon）。
- **葬花楔（四阶 `missiles/BuryingWedge.java`）**：图标 `BURYING_WEDGE=xy(12,9)` 16×16（9 行 12 列）；面板套标枪（默认公式 8+L~20+4L）；命中卡入（sticky 默认 true=中矢）；投掷飞行角速度 **0**（`MissileSprite` 角速度表）。**污血（`actors/buffs/VileBlood.java`）**：命中卡入后于 `rangedHit` 覆写内按"目标身上全部葬花楔的最高 trueLevel"赋予/刷新（多把不同等级取最高）；**受击追加伤害结算点 = `actors/Char.java` 的 `Char.attack()` 中 `enemy.damage(...)` 之后**：有 VileBlood 且目标存活 → `enemy.damage(1+level, this)`（无视护甲魔法伤害；目标已被本击击杀则不追加）。图标帧复用 `BuffIndicator.CORRUPT`。
- **新星之声（五阶 `missiles/NovaVoice.java`）**：图标 `NOVA_VOICE=xy(13,9)` 13×13（9 行 13 列）；面板套震爆方石（默认 10+L~25+5L）；`sticky=false`；投掷飞行角速度 **360°/s**。**落地 AoE**＝复刻 ForceCube.onThrow（中心+8 邻格 pressCell、对范围内全体 `shoot`、blast+爆炸音）；**自动旋回**＝内嵌 `CircleBack` Buff（镜像 HeavyBoomerang 机制，`left=1`：掷出 1 回合后 MissileSprite 飞回手中；耐久耗尽或坠入深渊则不旋回）。`ondeath` 键随附。
- **消息键**：`items.weapon.missiles.buryingwedge.{name=葬花楔/desc/stats_desc}`、`items.weapon.missiles.novavoice.{name=新星之声/desc/stats_desc/ondeath}`、`actors.buffs.vileblood.{name=污血/desc}`（zh+en 双写）。

# 2026-09-09 27 层测试层（CircularTestLevel）+ 跳层测试道具（TestPortal）
> ⚠️ 2026-09-10 已改造：本层由「固定圆形单房间」改为**普通楼层式随机地形生成 + 新贴图 tiles_lob**，类名 `CircularTestLevel` → `LobTestLevel`（extends RegularLevel）。本节保留为历史记录，现行实现见下方「2026-09-10 27 层测试层改造」。
- **`levels/CircularTestLevel.java`（extends DeadEndLevel）**：27 层测试房。半径 10 圆形单房间（25×25 地图、圆心(12,12)，圆外留 2 圈墙），材质/配色/音乐均暂用下水道同款（TILES_SEWERS/WATER_SEWERS/SewerLevel.SEWER_TRACK_LIST）。圆心 LANDING_CELL=312 为传送落点。
- **用户拍板的三项设计**：① 纯死路无出口——不铺任何楼梯、transitions 为空（Hero 触发换层需 transition 格子被走到才生效，见 Hero.java:2130；故空 transitions+无楼梯地形=无法离开），只能再使用道具(重置本层)/死亡/重开；② 道具无限次使用不消耗；③ 道具不设掉落/商店/开局渠道，仅调试窗口（F2 / 「调试模式」挑战菜单）「杂项」卡可调出——TestPortal 放 items 根包被 WndDebug 浅层扫描自动收录，零额外注册。
- **`items/TestPortal.java`（extends Item，items 根包）**：AC_USE → Level.beforeTransition() + InterlevelScene.Mode.RETURN + returnDepth=27/returnBranch=0/returnPos=LANDING_CELL(正 cell 直落，规避空 transitions 下 pos<0 的 entrance 回退 NPE)；isIdentified=true、isUpgradable=false（Item 默认 true 须覆写）、图标暂用 CRYSTAL_KEY。
- **接线改动**：Dungeon.newLevel() switch 加 case 27 → CircularTestLevel（原 default DeadEnd 拦截）；Level.create() 资源发放条件加 `!(this instanceof DeadEndLevel)`（防 27 层空耗力量药水/升级卷轴 LimitedDrops 配额计数）。
- **DeadEnd 家族的红利**：newLevel() 对 `instanceof DeadEndLevel` 自动跳过 generatedLevels 登记与 deepestFloor 更新 → 27 层进出不污染最深记录/统计/无杀戮标志；loadLevel 走 bundle 反序列化（默认无参构造）→ 存档/继续兼容。
- 消息键（zh+en）：items.testportal.{name/ac_use/desc/warp}。全站 region=depth/5 类索引均已人工核对：SpectralWallParticle 有 clamp(type>5→5)、SecretRoom/Lab/Traps 等数组索引仅存于 RegularLevel 房间生成路径（本层不走）。括号配平/引用核验通过，未代跑编译（用户约定）。

# 2026-09-19 27 层草地变「五区蘑菇形」修复：草叶细节区域与楼层贴图对齐
- **现象**：27 层（`LobTestLevel`）已换用 `tiles_lob.png`（草地材质取自**第二区/监狱**），但地图上草的**草叶细节**却是**第五区/恶魔大厅的蘑菇形**，两者风格打架。
- **根因（能力边界问题，非图片配错）**：草叶细节（长草尖、草簇阴影）不在楼层图集里，而是来自**全局唯一**的 `environment/terrain_features.png`（`Assets.Environment.TERRAIN_FEATURES`，256×128，8 行）。该图集**每 16 个帧槽一组对应一个区域**，`TerrainFeaturesTilemap.getTileVisual()` 原本只按真实深度取段：
  ```java
  int stage = (Dungeon.depth-1)/5;                  // 27 → 5
  if (Dungeon.depth == 21 && level instanceof LastShopLevel) stage--;
  stage = Math.min(stage, 4);                        // 5 被夹成 4 = 恶魔大厅
  ```
  即 **`Math.min(...,4)` 会把深度 >25 的关卡一律夹到第五区**。27 层真实深度 27 ⇒ 永远取恶魔大厅那套（蘑菇形）草叶，叠在监狱风格的草皮上。
- **取证**：`_chk/grass_zone_mismatch.py` 逐帧主色对照 ⇒ `terrain_features.png` stage 4 的 HIGH_GRASS 槽 73 主色 `#994C00/#8C2F00/#A68521`（暖褐，蘑菇/枯叶色），而 stage 1（监狱）槽 25 是 `#67933D/#7A924C/#6A723D`（草绿）。`_chk/verify_lob_grass_stage.py` 进一步证明：`tiles_lob.png` 槽 250（HIGH_GRASS_UNDERHANG）与 `tiles_prison.png` 槽 250 的**不透明像素集合完全相同、RGB 逐像素全等**（63 个实心像素）⇒ tiles_lob 的草叶确实就是第二区那一套（用户描述属实）；槽 122 全帧差异只在**地块底色**（lob 换成暖褐底），草形同源。
- **修法（可复用的通用机制，不硬编码 27）**：`TerrainFeaturesTilemap` 新增公开接口 `FeaturesTexProvider { int featuresStage(); }`，并抽出静态方法 `featuresStage()`：
  - 关卡实现该接口 ⇒ 用它自己报的段号（再 `GameMath.gate(0, x, 4)` 夹一次，注意 **`gate` 返回 float，必须显式 `(int)` 转型**，否则 javac 报「从float转换到int可能会有损失」）；
  - 未实现 ⇒ **原样走原版逻辑**（`(depth-1)/5` + 21 层商店退档 + `min(...,4)`）。
  `LobTestLevel` 因此 `implements TestLevel, TerrainFeaturesTilemap.FeaturesTexProvider`，`featuresStage()` 返回 `(GEN_DEPTH - 1) / 5` —— **跟着 `GEN_DEPTH` 自动走**：`GEN_DEPTH=4` ⇒ stage 0；将来把 `GEN_DEPTH` 调到 7（监狱）就自动变 stage 1，无需另改一行。
- **改动面**：`tiles/TerrainFeaturesTilemap.java`（+接口 +`featuresStage()` +`Level`/`GameMath` import；`getTileVisual` 里 3 行内联计算 → 1 行 `int stage = featuresStage();`）、`levels/LobTestLevel.java`（+implements +`featuresStage()` 覆写 +import）。**`getTileVisual(int,int,boolean)` 签名未变**，`CustomTilemap.Tilemap` 抽象基类无需同步改（`TerrainFeaturesTilemap` 本身不是 `CustomTilemap`）。
- **回归验证**：原版所有深度映射逐一比对无变化（1~5→0、6~10→1、11~15→2、16~20→3、21 层商店→3、21~26→4）；`level == null`（`JournalScene` 里 `new TerrainFeaturesTilemap(...)` 只为重建纹理）走原版分支、`instanceof` 对 null 恒 false ⇒ 无 NPE。修复后 27 层槽位：HIGH_GRASS 73→**9**、FURROWED 75→**11**、GRASS 77→**13**（+1 的 alt 槽 10/12/14 同段）。
- **核验工具**：`_chk/verify_lob_grass_stage.py`（复刻 `featuresStage()` + 逐帧主色断言 + **反例自测**：关掉 provider 分支须复现 stage 4，否则判脚本失效）、`_chk/grass_zone_mismatch.py`（terrain_features 五段 vs tiles_lob/tiles_prison 主色对照）、`_chk/terrain_features_probe.py`（terrain_features.png 全 128 槽 dump）。
- 源码级核验：单文件 javac EXIT=0（`LobTestLevel.java` 0 行告警；`TerrainFeaturesTilemap.java` 仅余 1 条 `[this-escape]`，落在构造函数 `instance = this;`——`git diff -U0` 确认该行不在任何 hunk 内，属上游既有写法）、括号配平 25/25 与 49/49、`check_utf8_all.py` 1432 文件全通过。
- **⚠️ 2026-09-25 更正**：本节的落点结论（`featuresStage()` 返回 `(GEN_DEPTH - 1) / 5` ⇒ stage 0 第一区、槽 9）**已被推翻**，现改为固定报**第二区（监狱）**、槽 25——见下方 2026-09-25 节。原因：`GEN_DEPTH` 是**布局**借位，不代表本层草皮材质所属区；本节其余内容（现象、根因、接口机制、取证方法）仍然有效。

# 2026-09-25 27 层草叶细节区段更正：第一区（下水道）→ 第二区（监狱）
- **症状**：27 层（`LobTestLevel`）的草叶细节取的是**第一区（下水道）**那一套（主色 `#48763C/#59994A/#395E30`），与 `tiles_lob.png` 的**监狱**风格草皮仍然不搭——上一次修复（2026-09-19）只把「第五区蘑菇形」换掉了，却对到了**错的区**。
- **根因（把「布局借位」当成了「材质所属区」）**：09-19 把 `featuresStage()` 写成 `(GEN_DEPTH - 1) / 5`。但 `GEN_DEPTH = 4` 只是**生成期借用的布局深度**（房间池 / 水贴图 / BGM / 配色按下水道来），**与本层 `tilesTex()` 实际使用的草皮材质无关**。`tiles_lob.png` 的草尖取自**第二区（监狱）**：槽 250（HIGH_GRASS_UNDERHANG）与 `tiles_prison.png` 同一帧**逐像素相同**（63 个实心像素集合相同、RGB 全等），且其 top-3 主色 `#67933D/#7A924C/#6A723D` 与 `terrain_features.png` **第二区**槽 25 完全一致，而与**第一区**槽 9（`#48763C/#59994A/#395E30`）明显不同。
- **修法**：`LobTestLevel` 新增具名常量 `public static final int FEATURES_STAGE = 1;`（1 ⇒ 第二区 监狱），`featuresStage()` 直接 `return FEATURES_STAGE;`——**与 `GEN_DEPTH` 彻底解耦**（不要再写回 `(GEN_DEPTH - 1) / 5`）。判据修正为：**报的是「本层 `tilesTex()` 里的草皮材质取自哪个区」，而不是「生成期借用了哪个深度」**。
- **改动面**：仅 `levels/LobTestLevel.java`（+常量、方法体 1 行、类与方法 Javadoc）。`tiles/TerrainFeaturesTilemap.java` **无需改动**（`FeaturesTexProvider` 接口与 `GameMath.gate` 夹取逻辑原样保留）。
- **槽位变化**：HIGH_GRASS 9→**25**、FURROWED_GRASS 11→**27**、GRASS 13→**29**（alt 槽 +1；全部落在 `terrain_features.png` 的**图集行 1** = 第二区）。按真实深度算出来的错误值是 73/75/77（第五区）。
- **核验工具（已改判据）**：`_chk/verify_lob_grass_stage.py` 现分五层：① **源码一致性**——`FEATURES_STAGE == 1`，且 `featuresStage()` 必须直接 `return FEATURES_STAGE;`、不得再出现 `GEN_DEPTH`；② `tiles_lob` 槽 250 vs `tiles_prison` 槽 250 逐像素；③ **主色指纹**——第二区 top-3 == 草尖 top-3、第一区不等于它（从像素层反证「报第一区」是错的）；④ **反例自测**——不实现 provider 须复现 stage 4（旧 bug）；⑤ **回归守卫**——`(GEN_DEPTH-1)/5 = 0` 与正确的 1 不同，可作源码级判据。
- **脚本已双向反例自测**：把常量改成 `FEATURES_STAGE = 0`、或把方法体退回 `return (GEN_DEPTH - 1) / 5;`，两次均被判失败（exit 1、并打印对应断言），源文件已按 md5 校验还原。

# 2026-09-10 27 层测试层改造：普通楼层式地形生成 + 新贴图 tiles_lob
- **新增 `levels/TestLevel.java`（标记接口）**：实现它的楼层不计入 generatedLevels/deepestFloor（Dungeon.newLevel），也不参与物资配额与 feeling 抽取（Level.create）。区别于 DeadEndLevel（无生成逻辑的空层）——本接口用于"有完整生成流程但不该影响存档进度"的调试层。
- **新增 `levels/LobTestLevel.java`（extends RegularLevel implements TestLevel，替换并删除 CircularTestLevel）**：27 层测试层改为**与普通楼层一致的地形生成**（随机房间 + 走廊 + 门），贴图换为 `Assets.Environment.TILES_LOB`（新常量 → `environment/tiles_lob.png`，256×256，与其他 tiles_*.png 同规格，按 `tilesTex()` 路径懒加载无需预载注册）。
- **用户拍板三项**：① 无下行楼梯（生成后把 `Terrain.EXIT/UNLOCKED_EXIT` 抹成 EMPTY 并移除 REGULAR_EXIT transition，保留入口楼梯作落点，离开只能靠传送门/死亡）；② 地形+水/草/装饰、不刷怪不摆物品不放陷阱；③ 水贴图暂沿用下水道 water0.png。
- **关键坑（深度索引表）**：StandardRoom/ConnectionRoom/EntranceRoom/ExitRoom 的挑选表都是 `float[27][]` 且以 `Dungeon.depth` 为索引，**深度 27 必然 ArrayIndexOutOfBoundsException**；painter 的隐藏门概率 `depth/20f` 在 27 层还会变成 1.0（全部门隐藏）。解法：`create()` 生成期临时把 `Dungeon.depth` 换成 `GEN_DEPTH=4`（下水道区域；取 4 而非 1/2——深度 1/2 会额外放置冒险指南书页）并临时换随机种子（否则楼层种子固定、每次进入都是同一张图），`finally` 立即还原。GEN_DEPTH 一行即可切换区域风格（3~5 下水道/6~10 监狱/11~15 矿洞/16~20 都市/21~26 大厅）。
- **仅地形的实现**：覆写 `initRooms()` 只放 入口房+出口房+N 标准房，**不调 super.initRooms()**——刻意排除特殊房/密室/商店（它们自带战利品/怪物/陷阱，且 `SpecialRoom.createRoom()` 会推进本局特殊房队列，影响玩家之后真实楼层）；`createMobs()/createItems()` 空实现；painter 用 SewerPainter 且 `setTraps(0, null, null)`；保留下水道同款 water 0.30/grass 0.20 填充、WALL_DECO 滴水视觉、REGION_DECO 可燃与烧毁表现、同款配色与 BGM。
- **纯地形标准房白名单（关键，防房间 paint 混入实体）**：`createMobs()/createItems()` 空覆写挡不住「房间 paint() 阶段」注入的实体——`AquariumRoom`(食人鱼)、`SuspiciousChestRoom`(Mimic+战利品)、`BurnedRoom/MinefieldRoom`(燃烧/爆炸陷阱)、`StudyRoom/GrassyGraveRoom`(物品) 都会在 `builder.build()`→`paint()` 时直接 `mobs.add/setTrap/drop`。解法：`StandardRoom` 新增 `createRoom(boolean terrainOnly)` 与静态 `TERRAIN_ONLY_ROOMS` 白名单（管道/环形大厅/水桥/装饰块/圆盆地/装饰线/分段/柱厅/牢房格/雕像/条纹草/裂缝/平台共 13 种，逐一核验 paint 零实体注入），`LobTestLevel.initRooms()` 改用 `createRoom(true)`。白名单含 `RingRoom`——其 `placeCenterDetail` 的 `drop(findPrizeItem())` 在 itemsToSpawn 为空时 `findPrizeItem()` 返回 null、`Level.drop(null)` 走 dummy 分支不落物（TestLevel 已保证不 addItemToSpawn，故安全）。
- **落点改动**：`TestPortal` 的 returnPos 由旧的固定圆心 cell 改为 `LobTestLevel.LANDING_POS = -1`——常规生成必有入口 transition，`Dungeon.switchLevel` 对 `pos<0` 会自动回退到 `getTransition(null).cell()`（入口楼梯），比固定 cell 更稳（固定 cell 在新随机布局中可能是墙）。
- 括号配平/引用核验通过，未代跑编译（用户约定）。

# 2026-09-09 死亡证明（DeathCertificate）+ 999 层物品陈列室（MuseumLevel）
- **`items/DeathCertificate.java`（extends Item，items 根包）**：死亡证明。使用→记录当前楼层/位置（写 MuseumLevel 静态 pending 字段）→ Level.beforeTransition() + InterlevelScene.RETURN 跳 999（returnPos=LANDING_CELL 正 cell 直落，规避空 transitions 下 entrance 回退 NPE）；**一次性消耗品**（2026-09-09 游戏验证后由无限次改为使用即 detach，成功传送前从背包移除；陈列室内拒绝使用、主角死亡时不消耗）；在陈列室内再用会提示拒绝。图标 DEATH_CERTIFICATE=xy(14,37) 19×21（用户手绘）。消息键 items.deathcertificate.{name/ac_use/desc/warp/already_here}（zh+en）。
- **获得渠道（用户拍板）**：隐藏房整体替换——新增 `levels/rooms/secret/SecretDeathProofRoom.java`（paint 中央 drop 死亡证明、门 HIDDEN），`SecretRoom.createRoom()` 前插 `Random.Float()<0.05f` → 返回该房型；有意不注册进 ALL_SECRETS 常规池。
- **`levels/MuseumLevel.java`（extends DeadEndLevel，depth 999，Dungeon.newLevel() 加 case 999）**：32×32 地图四周 1 圈墙、内部 30×30 方形展厅；下水道材质/配色（用户未指定主题，暂用 Sewer 同款）；不铺楼梯 transitions 为空（纯死路）。
- **陈列数据源＝游戏图鉴 Catalog 实时读取**：equipment 顺序=图鉴 equipmentCatalogs（MELEE→ARMOR→THROWN→WANDS→RINGS→ARTIFACTS→TRINKETS），consumable 顺序=consumableCatalogs（POTIONS→SCROLLS→SEEDS→STONES→FOOD→EXOTIC_POTIONS→EXOTIC_SCROLLS→BOMBS→TIPPED_DARTS→BREWS_ELIXIRS→SPELLS）；每类反射 newInstance，投掷类自动"一组"（MissileWeapon 构造即 quantity=defaultQuantity()）；全部 identify() + cursed=false +0（用户拍板）。ARMOR 段用 ClassArmor 过滤自动排除六件英雄专属；~~ARTIFACTS 段排除 SkeletonKey（钥匙）~~（**2026-09-24 已取消此排除**——它本就是神器图鉴里的一格，先前把它和英雄专属盔甲一道过滤，导致陈列室唯独缺这一件神器；详见文末同名修复节）。杂项行＝用户点名固定清单：金币×1000/能量×20/露珠/魔能触媒/奥术刻笔/暗金/火把/蜂蜜罐/破碎的蜂蜜罐(ShatteredPot)/重生十字章/元素余烬/粘咕球/邪能碎片/液金×50/矮人徽记。
- **布局**：一行 30 格从左到右依序铺、行满换下一行并**间隔一行留白**（每行间空一行），陈列上限第 28 行，底部第 29/30 行留作行走区；英雄落点 LANDING_CELL=15+30×32=975（第30行第15列）。
- **拾取返程**：Hero.actPickUp 成功分支（`you_now_have` GLog 之后、curAction=null 之前）加 `if (Dungeon.level instanceof MuseumLevel) onExhibitPickedUp()`；MuseumLevel.onExhibitPickedUp() 快照遍历 heaps destroy 清空其余地面物 → Level.beforeTransition() → InterlevelScene.RETURN 回 returnDepth/returnBranch/returnPos（createItems 时由 pending 静态写入实例字段，storeInBundle/restoreFromBundle 持久化存档）。
- **扩展性结论（重要）**：陈列内容**自动跟随图鉴**——新增武器/盔甲/投掷/法杖/戒指/神器/饰品/常规消耗品只需登记进对应 Generator.Category 数组（或 Catalog.java 特列如 food/bombs 等），图鉴与陈列室同步自动更新，无需改 MuseumLevel；但合成限定类（蜡翼、葬花楔、新星之声等未入图鉴）不会出现；杂项行/专属盔甲排除为硬编码，新增杂项需手动加 put()。

# 2026-09-09 微笑的尸山（SmilingCorpseMountain）：同 Boss 三形态不同帧尺寸换皮（验证）
- **需求**：验证"同一 Boss 不同阶段用不同大小的帧图片"的可行性；AI 暂为空闲（挨打木桩）。
- **`actors/mobs/SmilingCorpseMountain.java`（extends Mob）**：HP/HT=3000、EXP=0、defenseSkill=0（必中靶）、`Property.BOSS`。HP 区间→形态：≥2000→形态3、≥1000→形态2、其余→形态1（`phase()`）。`onAdd()` 里强制 `state = PASSIVE`（空闲：不移动/不索敌/不还手——Mob.aggro 对 PASSIVE 有保护；可压过调试窗 spawn 强设的 WANDERING）+ `BossHealthBar.assignBoss`（幂等）。`damage()` 后与 `updateSpriteState()`（link/读档/buff 刷新自动触发）→ `sprite.setPhase(phase())` 同步外观。
- **`sprites/SmilingCorpseMountainSprite.java`（extends MobSprite）**：单精灵类内换 `texture` + 按新尺寸重建 `TextureFilm`/四态动画（参照 DM300Sprite 模式）：形态3=m3.png 64×48×5帧、形态2=m2.png 48×48×5帧、形态1=m1.png 32×24×6帧（帧网格尺寸各异可行）。各形态帧分配（用户确认）：帧1=idle 单帧循环、帧2=run 单帧循环、attack=帧2→3 两帧、die=帧1 单帧；多余帧预留不用。切形态顺序（**先 `play(idle)` 再 `place(ch.pos)`**，勿颠倒——见下坑）：
- **资源与键**：`assets/sprites/m1~m3.png`（用户绘制，已就位）；`Assets.Sprites` 新增 `SMILE_CORPSE_M1/M2/M3`；消息键 `actors.mobs.smilingcorpsemountain.{name/desc}`（zh+en，中文名"微笑的尸山"）。
- **生成方式（用户拍板暂不接线）**：未接入任何楼层刷怪池；从调试窗（WndDebug → Mob 标签页，自动扫描 actors.mobs 包，无需注册）选择生成→点地图放置即可观察。
- **设计结论**：同 Boss 多阶段不同帧尺寸可行——推荐"单精灵类内换贴图+重建帧网格"（帧切换走 film 重建而非混帧）；后续做真实 Boss AI 时移除 onAdd 的 PASSIVE 强制即可。

# 2026-09-09 新增四阶武器：し协会的暗杀刃（ShiAssassinsBlade，用户新武器，入 WEP_T4 池）

- **定位**：四阶通用近战，全局自然掉落（`Generator.WEP_T4` 尾部追加，权重 **1**）；非 E.G.O、无武技（不覆写 duelistAbility/targetingPrompt，与 DiamondSword/WillBlade 一致）。
- **`items/weapon/melee/ShiAssassinsBlade.java extends MeleeWeapon`**：图标 `ItemSpriteSheet.SHI_ASSASSINS_BLADE = xy(13, 32)`（items.png **32 行 13 列**，尺寸 **15×16**，`assignItemRect(15,16)`；该格原属 DOCUMENTS 组但仅用到 +0..+6，槽位空闲）。`tier=4`（力量需求沿用四阶默认），`hitSound=HIT_STAB`、pitch 0.9（同暗杀之刃手感）。
- **面板（覆写 min/max）**：`min(lvl)=4+4L` / `max(lvl)=24+2L`——最小成长 > 最大成长，掷骰区间随升级收窄：+0 为 4~24、+5 为 24~34、**+10 收敛为固定 44**（此后每击恒视为掷出最大伤害）。+11 起 min>max 反转（4+4L>24+2L），`Random.NormalIntRange` 对反转区间只会落在两边界附近、无崩溃风险，属超上限极端情形（常规 +10 封顶前不会遇到）。
- **效果①（匕首类偷袭加成）**：`damageRoll()` 覆写中仿暗杀之刃——`enemy instanceof Mob && Mob.surprisedBy(hero)` 时掷骰区间改为「`min + round(diff×0.50f)` ~ `max`」，偷袭伤害向最大值倾斜（含必中/偷袭结算，偷袭语义见 `Mob.surprisedBy`）。
- **效果②（掷出最大伤害爆发）**：掷出的基础伤害 == `max()` 时，最终伤害额外 **+44**（`MAX_DMG_BONUS=44`；在 augment 与 exStr 加成之后平加，恒为字面 44）。与偷袭加成联动：偷袭把区间整体抬高到 max 附近，触发爆发概率明显更高。实现为**自包含覆写**（不继承 AssassinsBlade，避免顺带引入其决斗家"潜行"武技与文本链）。
- 文本键（items_zh.properties 末尾追加，zh-only，同 WillBlade/BrandWorkshop 先例）：`items.weapon.melee.shiassassinsblade.{name/stats_desc/desc}`（stats_desc 三段效果描述含 `\n` 转义；desc=用户 lore）。

# 2026-09-10 新增两种武器附魔：欲望（Desire）与蜚蠊（Roach）+ buff 图标错位修正

- **本项目首次自定义武器附魔**（此前 `items/weapon/enchantments/` 13 种全为原版）。调研结论见 `docs/enchantment-implementation.md`。
- **`items/weapon/enchantments/Desire.java extends Weapon.Enchantment`**（粉色光效 `0xFF69B4`）：
  - 概率公式照烈焰范式 `(lvl+1)/(lvl+5) × procChanceMultiplier(attacker)`——**无附魔强度、+0 武器 = 20%**（lvl1 33.3%、lvl2 42.9%，渐近 100%）。
  - 两段式：`procChance < 1` 时命中即施加**残废 `Cripple`（固定 5 回合）**；`procChance >= 1`（附魔强度已把"必定残废"顶满）后，溢出的 `procChance-1` 转化为**扎根 `Roots`（固定 3 回合）**的概率，未命中仍施加残废。飞行单位对扎根免疫（`Roots.attachTo` 返回 false），直接落在残废分支。
  - 命中伴 `Splash.at(sprite.center(), 0xFFFF69B4, 5)` 粉色水花。
- **`items/weapon/enchantments/Roach.java extends Weapon.Enchantment`**（棕色光效 `0x8B5A2B`）+ 内部 buff **`Roach.AttackVermin`（攻击害虫）**：
  - 命中时读出 `AttackVermin` 存量、释放并清零，`return damage + bonus`（机制同恒动）。
  - `AttackVermin.act()` 每回合积累 `damage = min(10×power, damage + 0.2×power)`；`power` 由附魔 proc 用 `procChanceMultiplier(attacker)` 写入——**无附魔强度时每 5 回合 1 点、上限 10 点**，强度同时放大速度与上限。
  - 状态挂在**角色身上**（不是武器字段）：人体派作品 `extraEnchants` 只序列化类名，挂武器会丢档；`storeInBundle/restoreFromBundle` 存 `damage`/`power`，缺键兜底 `power=1`。
  - 图标 `BuffIndicator.ATTACK_VERMIN = 111`，**仅在存量 ≥1 时显示**（`icon()` 条件返回 `NONE`，跨越 0↔1 时 `BuffIndicator.refreshHero()`）；`tintIcon` 由浅褐渐深至深棕，`iconTextDisplay` 显示存量。
  - ⚠️ **不在 `act()` 里取附魔强度**：`genericProcChanceMultiplier` 会**消耗** `RunicSlashTracker`/`DirectedPowerTracker`（detach），逐回合调用会把天赋加成提前吃掉——因此强度只在命中 proc 时取一次并写回 buff。
- **注册**：`Weapon.Enchantment.common` 加 `Desire.class`/`Roach.class`（现 6 种，各 8.33%）；`Unstable.randomEnchants` 同步加入（两者均有 on-hit 效果）。**未登记 `EnchantArt.RECIPES`**——该技艺未命中配方时按天赋级别随机附魔，故两者仍可获得；若要定向配方需再指定两个配方物品。
- **本地化**：`items.weapon.enchantments.desire.{name,desc}`、`...roach.{name,desc}`、`...roach$attackvermin.{name,desc}`（zh+en 双份；`\n` 单反斜杠、`%1$d/%2$d`）。
- **🐞 buff 图标错位修正（本次一并修复）**：Pillow 逐帧扫描（小图 7px×18 列、大图 16px×16 列）证实**「瞄准心脏 + 焦炭松脂 + 黄金松脂 + 连斩」4 个图标实际画在帧 107/108/109/110**，而代码写的是 108/109/110/111（"107 决定跳过"的旧注释与贴图不符），整体多算 1 格。已在 `BuffIndicator` 统一 **-1**：`AIM_HEART_MARK=107`、`CHARCOAL_RESIN=108`、`GOLDEN_RESIN=109`、`PALERMO_SWORD=110`。修正后 0~110 已占，**新 buff 从 111 起取号**。
- 括号配平/引用核验通过，未代跑编译（用户约定）。

# 2026-09-11 新增 buff「泪剑」(TearSword)：环绕泪剑特效 + 测试道具

**本阶段只做特效，无任何数值/战斗效果**。可叠层，每层在角色周围环绕一柄泪剑，随移动跟随、带深蓝色拖尾。

## 需求 → 实现对照

| 需求 | 实现 |
| --- | --- |
| buffID 在「攻击害虫」下一位 | `BuffIndicator.TEAR_SWORD = 112`（紧接 `ATTACK_VERMIN = 111`） |
| 贴图 `tearsword` | `Assets.Sprites.TEARSWORD = "sprites/tearsword.png"`；实测 16×16、斜向单帧（柄左上 ~ (1,1)、尖右下 ~ (14,14)） |
| 逆时针旋转 45° 为正方向，朝右即此方向 | `angle = BASE_ANGLE = -45f`（noosa 屏幕 y 向下，**正角 = 顺时针**，故逆时针 45° 取负） |
| 朝左时水平翻转 | `angle = +45f` **且** `scale.x = -1f`（等价性见下） |
| 剑贴图上的 **(5,5) 像素**排在以**脚底**为圆心、**16px 半径**的**上半圆**上均分 | 圆心 `(cs.x + cs.width/2, cs.y + cs.height)`；`θ = π·(i+0.5)/n`，`x = cx + cosθ·16`、`y = cy − sinθ·16`；对齐基准同日由贴图中心 (8,8) 改为 **(5,5)**，见下节 |
| 1 层时在头顶 | `θ = π·0.5/1 = 90°` → 正上方 ✔（取"每份中点"而非端点，任何 n 都不会落在 0°/180° 的水平端与腿脚重叠） |
| 跟随移动 | 视觉每帧读角色贴图绝对坐标重定位 |
| **与朝向同侧的剑渲染在角色之下** | 朝左→左半边的剑移入 `behind` 组、挂 `GameScene.floorEmitters`（角色之下）；其余留在本组（`mobs`，非 Visual 成员被 `sortMobSprites()` 排到队尾 → 角色之上）。判定 = 剑心 x 与角色中轴比较，**正上方那柄归「之上」** |
| 深蓝色拖尾 | 自建 `TearSwordTrailParticle`（深蓝 `0x1E3A8A`，0.30s 淡出 + 1.6→0.6px 收缩），线段插值补点 |
| 测试道具，使用得 1 层 | `items/TearSwordTester.java`（`AC_GAIN` +1 层 / `AC_CLEAR` 清空），套用 `ItemSpriteSheet.LONGSWORD` |

## 新增文件

- **`effects/TearSwordVisual.java extends Group`**：持 `ArrayList<Image> swords`，`setStacks(int)` 增删剑贴图；
  `update()` 逐帧定位 + 设 `angle`/`scale.x` + 铺拖尾；`attached()` 判断角色贴图是否就绪；
  `visible = cs.visible` 与角色同显隐（脱离 FOV 时不画）。
- **`effects/particles/TearSwordTrailParticle.java extends PixelParticle`**：`reset(x,y)` 自己 `revive()` +
  复位颜色/尺寸/**`am=1f`**（`revive()` 不还原透明度）；`update()` 线性淡出并收缩。
- **`actors/buffs/TearSword.java extends Buff`**：`MAX_STACKS`（初版 8，**同日二次调整按需求下调为 3**）；`type = POSITIVE`；
  `icon() = TEAR_SWORD`、`iconTextDisplay() = 层数`、`desc()` 带 `%1$d/%2$d`；
  静态入口 `stacks(Char)` / `addStacks(Char,int)` / `setStacks(Char,int)`（`0` = 移除）；
  存读档只存 `stacks`（视觉字段不序列化）。
- **`items/TearSwordTester.java extends Item`**：位于 `items` 根包 → 被 `WndDebug`「杂项」页的**浅层扫描自动收录**，无需注册。

## 关键实现点

- **挂载层级（本次踩坑）**：`GameScene.effect(...)` / `floorEmittersAdd(...)` / `effectOverFog(...)` 形参都是 **`Visual`**，
  而 `Group`（→`Gizmo`）**不是** `Visual`，传不进去；`GameScene.mobs` / `effects` 又都是**私有实例字段**，外部够不着。
  正解 = `target.sprite.parent.add(visual)`（即往 `GameScene.mobs` 里加，与 `MasterpieceShow.ensureVisual` 同范式）。
  `GameScene.create()` 的 add 顺序：`floorEmitters` < `mobs` < `emitters` < `effects` < `fog`；
  `sortMobSprites()` 的比较器把 **mobs 组内非 Visual 成员排到队尾** → 与角色同组但**渲染在角色之上**。
  <br>**同日追加**：`GameScene.floorEmittersAdd` 的形参由 `Visual` **放宽为 `Gizmo`**（`Visual extends Gizmo`，
  纯放宽、既有调用点照旧编译），这样 `Group` 这类「非 Visual 的容器」也能挂到角色贴图**之下**——
  泪剑的「背身层」正依赖此点。
- **静态 `Image` 的镜像只能用 `scale.x = -1`**：`Image.flipHorizontal` 是裸 public 字段，只在 `frame()` → `updateFrame()`
  时被写进顶点；静态图构造完再赋值**完全无效**。（`CharSprite.turnTo()` 敢直接改该字段，是因为它每帧动画都会重新
  `frame()` 覆盖顶点——别把两种场景搞混。）
- **旋转+镜像的等价性**（已在纸面验算）：noosa 变换 = `T(x,y)·T(o)·R(angle)·S(scale)·T(-o)`，线性部分 = `R·S`。
  正向 `R(-45)`；朝左取 `R(+45)·S(-1,1)`，其矩阵 = `[-c,-s; -s,c]`，与 `M_h·R(-45)`（`M_h=diag(-1,1)`）**逐项相等** →
  确实是正向结果的水平镜像（镜像轴 = **过轴心的竖线**；轴心同日由贴图中心 (8,8) 改为 **(5,5)**，等价性不变——
  只需 `S` 与 `R` 的复合关系成立，与轴心取值无关）。
- **对齐基准（同日由贴图中心改为 (5,5)）**：不再用 `originToCenter()`（等价 `origin=(8,8)`），而是 `origin.set(5,5)`。
  `origin` 同时充当**平移量**与**旋转/缩放轴心**，所以定位必须写成 `x = 剑心x − 5`、`y = 剑心y − 5` 把这分量抵消，
  否则贴图会整体平移 (5,5)。可直接推出的两个后果：① 未旋转时，贴图内的 (8,8) 落在圆弧点 **+(3,3)**（右下）处，
  即整柄剑比旧版偏右下 3px；② 轴心落在**剑柄握把**上（实测 (5,5) 为深色握把像素，剑尖在 (14,14)），
  逆时针转 45° 后剑尖指向朝向、约在轴心前 12.7px、剑柄尾在轴心后 5.7px。
  轴心用**像素常量**而非 `width()/2`，故与 `scale.x = -1` 无耦合（别用 `width()`——那个会乘 `scale.x`，朝左时是负数）。
- **拖尾采样**：`CharSprite.move()` 用 `PosTweener` **平滑滑动**（每帧只挪几像素），故按 `TRAIL_INTERVAL = 0.05s`
  采样，并在「上次采样点 → 当前点」线段上按 `TRAIL_STEP = 4px` 补点（`TRAIL_MAX_STEPS = 6` 防瞬移爆量）→
  尾迹连成一条线而非孤立点；**站着不动不铺粒子**（也更省 draw call）。
- **`Group.recycle(Class)` 的坑**：它只返回一个 `!exists` 的槽位（或新建），**不会调 `reset()`** →
  自定义粒子必须在自己的 `reset(x,y)` 里 `revive()` + 复位 `am`/位置/颜色/尺寸。
- **生命周期**：`fx(true)`（`Buff.attachTo` / `Char.updateSpriteState()`）建视觉 → `act()` 每回合 `ensureVisual()`
  兜底重建（换层/切场景后旧视觉随场景销毁）→ `detach()` 里 `killAndErase()`。
  **入口约定写进 javadoc**：直接 `Buff.affect(ch, TearSword.class)` 拿到的是 **0 层**，会在首个 `act()` 自解；
  调试窗「状态」页已按 `Release`/`Karma` 的先例接好叠层分支（点一次 +1 层）。
- **图标帧号**：`BuffIcon` 用 `new TextureFilm(texture, 7 | 16, ...)` 算行列。已画帧 112——
  小图 `buffs.png`(128×64, 7px, 18 列) → **行 6 列 4**；大图 `large_buffs.png`(256×128, 16px, 16 列) → **行 7 列 0**。
  原图已备份至 `.workbuddy/tmp_icons/backup/`。

## 未做 / 待办

- 层数上限 **`MAX_STACKS`**（初版暂定 8，**同日二次调整按需求下调为 3**；无限叠会让剑互相重叠。要放开只改这一个常量）。
- 当前无任何数值效果；未接入任何获取渠道（只测试道具 / 调试窗）。
- 括号配平 + 资源帧校验（Pillow）通过，**未代跑编译**（用户约定）。

---

# 2026-09-11 追加调整：泪剑定位轴心 (5,5) + 环绕分层遮挡

同一特征的后续调整，**只动特效表现，仍未接任何数值**。

## 需求 → 实现

| 需求 | 实现 |
| --- | --- |
| 泪剑在轨道上的位置以 **(5,5) 像素**为中心（不再是贴图中心） | `TearSwordVisual.PIVOT_X/PIVOT_Y = 5f`；`sword.origin.set(5,5)`；定位 `x = px − 5`、`y = py − 5` |
| 与朝向**同侧**的泪剑渲染在角色**之下**（朝左→左侧那几柄被角色遮挡） | 新增 `behind` 组挂 `GameScene.floorEmitters`；判定 `facingLeft ? px < cx : px > cx` → `behind.add(sword)`，否则留本组（`mobs` → 角色之上）。`px == cx`（正上方那柄）归「之上」 |

## 实现要点

- **`origin` 是「平移量 + 转轴」二合一**（`updateMatrix()` = `T(x,y)·T(origin)·R(angle)·S(scale)·T(-origin)`），
  所以「以 (5,5) 为中心」= `origin.set(5,5)` **并且**定位时减掉 5 —— 两处必须成对改，只改一处会让贴图整体偏移或转轴错位。
- **(5,5) 落在剑柄握把上**（贴图实测 16×16、33 个不透明像素、柄尾 (1,1)、剑尖 (14,14)）：逆时针 45° 后
  剑尖指向朝向、位于轴心前约 **12.7px**，柄尾在轴心后约 **5.7px**。未旋转时贴图内的 (8,8) 落到圆弧点 **+(3,3)**（右下）处，
  即整柄剑比旧版偏右下 3px。
- **分层靠两个组**：本组（`this`）挂 `target.sprite.parent`（mobs，角色之上）；新增的 `behind` 组挂
  `GameScene.floorEmitters`（角色之下）。换层只需 `want.add(sword)` —— `Group.add()` 会**自动把节点从原组摘除**，
  一行即完成双向切换（`remove()` 不会 kill 节点，安全）。
- **`GameScene.floorEmittersAdd` 形参由 `Visual` 放宽为 `Gizmo`**：`Group` 不是 `Visual`，不收宽就挂不进去。
  唯一调用点 `BlessingMarker` 传的正是 `Visual`，纯放宽、调用点零改动。层序（`GameScene.create()`）：
  `levelVisuals` → **`floorEmitters`** → `heaps` → **`mobs`**（`hero` 在此）→ `raisedTerrain`/`walls`
  ⇒ `floorEmitters` 确实渲染在角色贴图之下。
- **`behind` 惰性创建，且不可复用已销毁的组**：`Group.destroy()` 会把 `members` 置 null（此后 `add()` 直接 NPE），
  故 `ensureBehind()` 在 `parent == null` 时**新建**而非复用；场景未就绪（`scene == null`，helper 静默不挂）时返回 null，
  本帧全部剑留在角色之上，下帧再试。
- **回收必须显式**：背身剑不是本组的成员，`killAndErase()` 不会连带清掉 → 新增 `dispose()` 遍历 `swords` 逐个
  `killAndErase()` 并 `behind.killAndErase()`；`kill()` / `destroy()` 都覆写为「先 `dispose()` 再 `super`」，
  保证「buff 结束」与「场景销毁」两条路径都不残留贴图。
- **遮挡幅度提示**：剑长（约 18px）与半径 16px 同量级，而角色贴图仅 12×15 → 重叠主要发生在**接近头顶的一两柄**
  （剑柄端探入头部/肩部数像素）；弧线两端（近水平位置）的剑基本不与角色相交，换层与否肉眼难辨。
  若要更明显的「环绕压身」，可减小 `RADIUS` 或换更大的贴图。

## 待确认

- 分层判定目前只看**剑心 x 相对角色中轴**；若希望「朝左时连正上方那柄也归下去」，把判据改成 `<=` / `>=` 即可。
- 括号配平通过（4 个新建 + `GameScene` / `TearSword` 两处改动），**未代跑编译**（用户约定）。

---

# 2026-09-11 追加调整（二）：泪剑上限 3 层 + 浮动 / 惯性 / 协同攻击

同特征的第三轮调整。**本轮仍只做特效、不产生任何数值/战斗效果**——协同伤害是下一轮才补上的
（见文末「追加调整（三）」；那一轮同时修掉了本轮埋下的一个计时器缺陷）。

## 需求 → 实现

| 需求 | 实现 |
| --- | --- |
| 层数上限限定为 **3 层** | `TearSword.MAX_STACKS = 8 → 3`。clamp 只在 `addStacks`/`setStacks` 两处，`iconTextDisplay()`、测试器文案、buff `desc` 的 `%2$d` 全部自动跟随，无需另改 |
| 每柄泪剑**小幅慢速浮动** | 在弧上基准点之外再叠一层正弦漂移：`ox = sin(t·1.1 + phase)·1.5`、`oy = cos(t·1.1·0.73 + phase·1.37)·1.5`（双频，避免走成规整的圆）；角度另叠 `sin(t·1.1·0.8 + phase)·5°` 的摆动。`phase = i·2.399`（黄金角）⇒ 三柄剑起伏互不同步 |
| 角色移动时**不立刻跟随，短暂延迟后追随** | 位置改为**指数缓动**：`s += (target − s)·(1 − e^(−12·dt))`。稳态滞后 ≈ 移速/12 ≈ 5px（角色约 64px/s），停步后收敛归位 |
| 近战攻击怪物时**所有泪剑协同攻击**（迅速戳刺怪物位置后回到原位） | `TearSwordVisual.strike(x,y)`：全部剑沿「角色脚底 → 目标」方向冲向目标，抵住片刻再收回；**逐柄错峰 0.035s**形成"依次戳刺"的协同感 |
| 触发时机 | `Hero.onAttackComplete()` 的 `attack(attackTarget)` **之后** → `TearSword.strikeAt(hero, attackTarget)`（顺序理由见下） |

## 实现要点

- **触发点是「近战」的充分条件**：`Hero.onAttackComplete()` 只由 `CharSprite.play(attack)` 播完触发
  （见 `CharSprite` 中 `if (anim == attack) … ch.onAttackComplete()`），而 `sprite.attack(...)` 在整个 Hero 里
  只有 `actAttack()` 的近战分支会调；`Hero.shoot()`（投掷）直接 `attack()` 走人、不播攻击动画，`actAttack()` 里
  被「逻辑工作室」接管的远程射击也在 `sprite.attack()` **之前** return。所以这一钩子天然排除远程/投掷。
  钩子位置与「巴勒莫对剑」叠层**同一处**（项目既有先例）。
- **取目标坐标与 `attack()` 的先后**：本轮把 `strikeAt` 放在 `attack()` **之前**（先取样才"扎在它原来站的地方"）；
  下一轮因要追加伤害而改成放在 `attack()` **之后**——原因与取舍见「追加调整（三）」。实测目标死亡时贴图只是
  原地播放死亡动画，坐标依然有效，所以两种顺序取样都成立。
- **突刺段不走惯性**：`amt > 0` 时位置**直接等于**定时曲线算出的点。这是一段脚本化动画，缓动会把方正的
  节奏糊掉。曲线（`strikeAmount`）三段拼接且**两端值均为 0**（冲刺 smoothstep 0→1 → 抵住恒 1 → 收回 smoothstep 1→0），
  所以进出突刺那一帧与「缓动跟随」的结果**连续、不会突跳**。总时长
  `STRIKE_OUT(0.10) + STRIKE_HOLD(0.06) + STRIKE_BACK(0.22) + STRIKE_STAGGER·(n−1)`；收回段刻意比冲刺长，
  读起来像「戳出去快、收回来稳」。
- **落点带越过与铺开**：沿冲刺方向多走 `STRIKE_REACH = 3px`（让剑"扎进去"），并沿**法线**方向按
  `(i − (n−1)/2)·STRIKE_SPREAD`（4.5px）铺开 ⇒ `n=1` 居中、`n=3` 为 −4.5 / 0 / +4.5，三柄剑不叠在同一个像素上。
- **分层判据改为「弧上基准点 + 离轨豁免」**：主判据仍用**不含浮动的 `bx`**（否则剑随浮动晃过中轴会来回换层而闪烁）；
  但若某柄剑离基准点超过 `STRIKE_FRONT_DIST = 10px`（只在突刺离轨时发生），一律拉回 `this`（角色之上），
  否则冲过去的那几柄会被角色/怪物贴图挡住、白做动画。10px 的阈值留有余量：走动的稳态滞后约 5px，
  叠加最大浮动 √2×1.5 ≈ 2.1px，合计约 7px，不会误判。
- **拖尾由「跟角色」改为「跟每柄剑自己」**：原先按角色位移铺粒子，现在剑会浮动/惯性/突刺，各自位移不同
  → 改为逐剑记录 `trailX/trailY`，每 `TRAIL_INTERVAL = 0.05s` 比对自己的位移，超过 `TRAIL_MIN_DIST = 2.5px`
  才在「上次采样点 → 当前点」线段上按 `TRAIL_STEP = 4px` 补点。于是：走路有尾迹（约 3.2px/采样）、
  突刺有粗尾迹（8~16px/采样 → 2~4 颗/次）、**站着不动的浮动几乎不铺粒子**（约 0.08px/采样，静默丢弃）。
  原 `lastCx/lastCy/resetTrail()` 随之删除。
- **首帧直接就位**：`Sword.placed` 首次为 false 时把位置**设为目标**而不是从 (0,0) 缓动过来
  （否则新建视觉的剑会从屏幕左上角飞进来）。
- **`strike()` 在层数为 0 时直接返回**；`setStacks(0)` 会把 `strikeTimer` 复位为 −1，防止"已经没有剑了还在计时"。

## 可调参数（都在 `TearSwordVisual` 顶部常量区）

| 常量 | 值 | 作用 |
| --- | --- | --- |
| `FLOAT_AMP` / `FLOAT_SPEED` / `FLOAT_WOBBLE` | 1.5px / 1.1rad·s⁻¹ / 5° | 浮动幅度、频率（周期≈5.7s）、角度摆动 |
| `FOLLOW_SPEED` | 12 | 惯性：**越小越"拖"**，稳态滞后 ≈ 移速/本值 |
| `STRIKE_OUT`/`HOLD`/`BACK`/`STAGGER` | 0.10 / 0.06 / 0.22 / 0.035 s | 突刺节奏与逐柄错峰 |
| `STRIKE_REACH` / `STRIKE_SPREAD` | 3 / 4.5 px | 越过目标与横向铺开 |
| `STRIKE_FRONT_DIST` | 10 px | 离轨多远就强制画在角色之上 |
| `TRAIL_MIN_DIST` | 2.5 px | 低于此位移不铺拖尾 |

## 待确认

- 若觉得浮动太"飘"，先降 `FLOAT_AMP`；若觉得惯性太"重"，升 `FOLLOW_SPEED`（滞后与它成反比）。
- 仅英雄触发；若要怪物持有时也生效，在 `Mob.onAttackComplete()` 里加同一行 `TearSword.strikeAt(this, enemy)` 即可
  （那边的目标字段叫 `enemy`）。此说明已写进 `TearSword` 的类 javadoc。
- 括号配平通过（改动 3 个文件：`TearSwordVisual` / `TearSword` / `Hero`），**未代跑编译**（用户约定）。

---

# 2026-09-11 追加调整（三）：修复「协同攻击不触发」+ 落实协同法术伤害

第四轮调整。修掉上一轮埋下的一个致命缺陷，并把协同攻击从「纯动画」变成真正的伤害来源。

## 缺陷定位：突刺计时器从未推进

**现象**：浮动正常，但近战挥击时泪剑完全不突刺。

**根因**在 `TearSwordVisual.update()`：

```java
//---- 协同攻击计时 ----
if (strikeTimer >= 0f && strikeTimer > strikeTotal( n )) { strikeTimer = -1f; }
final boolean striking = strikeTimer >= 0f;
```

`strike()` 起手把 `strikeTimer` 置 0，而**全类没有任何一处执行 `strikeTimer += dt`**
（`time += dt`、`trailTimer += dt` 都在，唯独漏了这一句）。后果是双重静默失败：

1. 计时器恒为 0 → `strikeAmount(0 − i·0.035)` 对每柄剑都返回 `0f`
   （函数开头就是 `if (t <= 0f) return 0f;`）→ `amt > 0f` 永远不成立 → **剑一动不动**；
2. 既然 `strikeTimer` 永不递增，`striking` 也就永远是 `true` → 突刺状态**永远退不出去**。

两个后果都不抛异常、不打日志，所以表现为「特效静默失效」。修复即补上逐帧推进，
并把结束判定并进同一个 `if`（顺带修掉第 2 条）：

```java
if (strikeTimer >= 0f) {
    strikeTimer += dt;
    if (strikeTimer > strikeTotal( n )) strikeTimer = -1f;
}
```

> **教训**：这套视觉的计时全部是**自己维护的秒表**（`time` / `trailTimer` / `strikeTimer`）。
> 每新增一个计时字段，务必同时确认「起手复位」与「逐帧推进」两处都写了（`Game.elapsed` 单位是秒）。

## 需求 → 实现

| 需求 | 实现 |
| --- | --- |
| 修复「协同攻击视觉不触发」 | 补 `strikeTimer += dt`（见上） |
| 每柄泪剑每次攻击造成 **1 点法术伤害** | 新增 `TearSword.DAMAGE_PER_SWORD = 1`；`strikeAt()` 在触发动画后追加 `层数 × 本值` 点伤害 |
| 保留后续调整空间 | 数值集中在一个常量；加成/随层数递增/暴击都加在 `strikeAt()` 里即可 |

## 实现要点

- **伤害来源 = `TearSword.MagicStrike` 标记类**（`target.damage(dmg, new MagicStrike())`）——
  一个与 `DimDusk.MagicStrike` 同构的**非 Buff 空类**。`Char.damage` 本身**不结算护甲**
  （护甲在 `Char.attack` 里），所以这条伤害天然「无视护甲」，与「法术伤害」的定位相符。
  不用 buff 实例当来源的理由见下条。
- **已登记进 `AntiMagic.RESISTS`**（2026-09-11 追加、当日修正为标记类）：
  `AntiMagic.RESISTS.add( TearSword.MagicStrike.class )`。
  本条伤害由此被当作**正规法术伤害**处理，一处登记买到三件事：
  ① `Char.damage` 里的 `if (isImmune(srcClass)) { damage = 0; }` ⇒ 带 `MagicImmune`（反魔法卷轴）
  或「魔法免疫」精英特性（`ChampionEnemy.AntiMagic`，它也是把 RESISTS 灌进 `immunities`）的目标**完全免伤**；
  ② `AntiMagic` 雕文的 `drRoll` 对其减伤（普通目标 `glyphLevel = -1` ⇒ `drRoll` 返 0、实则不减）；
  ③ 伤害浮字自动显示法术图标（`Char.damage` 的图标分支按 `RESISTS.contains(src.getClass())` 精确类匹配判定，
  来源正是 `MagicStrike` 实例；原先那条 `instanceof TearSword` 的特判已删除，不需要了）。
- **为什么登记「标记类」而不是 buff 本身**：`MagicImmune.attachTo()` 除了提供免疫，还会
  `for (Buff b : target.buffs()) { if (b.getClass().isAssignableFrom(immunity)) b.detach(); }`
  ——它把 `RESISTS` 里的条目当成「可被驱散的魔法效果」，**按类清除目标身上的 buff**
  （`Charm`/`Weakness`/`Hex` 这些负面 buff 正是靠这条清掉的）。
  若登记 `TearSword.class`，持有者一拿到魔法免疫（反魔法卷轴）身上的泪剑就会被驱散，
  且免疫期间 `Buff.attachTo` 的 `isImmune` 检查会让新增层数失败。换成非 Buff 的 `MagicStrike`
  之后，这条路径匹配不到泪剑（`TearSword.class.isAssignableFrom(MagicStrike.class)` 为 false），
  于是**免伤保留、buff 本身不受牵连**——两件事靠「来源与 buff 分离」解耦。
  ⚠️ 若日后要改回登记 `TearSword.class`（例如确实想让它被反魔法卷轴清掉），
  `Mob.die` 的英雄击杀白名单必须同步跟着改。
- **`addStacks`/`setStacks` 的 `b.target == null` 兜底仍然保留**：`Buff.append` 会把 `attachTo` 的
  返回值丢掉、照旧返回实例，所以目标免疫（或任何 attachTo 失败）时拿到的是未附着的「幽灵对象」；
  显式挡掉可避免在未附着的对象上改层数。现在泪剑已不再被自己的免疫拦下（`TearSword.class`
  不在 RESISTS 里），但这条兜底对其它 attachTo 失败路径依旧有效，属于零成本保险。
- **调用点从 `attack()` 之前移到之后**：泪剑伤害若先把目标打死，`attack()` 就会在尸体上白跑一遍
  （命中判定、武器附魔、`PalermoSwordBuff`/`Combo` 等 Tracker 全在它内部）。放到 `attack()` 之后，
  这次挥击先正常结算，泪剑伤害作为「追加」落在后面。副作用：若这一击本身已击杀目标，
  泪剑伤害会被 `Char.damage` 的 `!isAlive()` 兜底跳过（动画照播）。
- **已知小瑕疵**：泪剑伤害与主攻击伤害在同一帧结算，两个伤害浮字会叠在同一位置。要错开需把伤害延迟到
  突刺命中那一刻（0.10~0.16s 后），那要从视觉层回调游戏逻辑（`MissileSprite` 的 callback 是先例）；
  本轮刻意没做，保持「伤害在挥击结算处一次算清」这一简单模型。
- **击杀归因要补白名单**：`Mob.die()` 里有一段「是否算英雄击杀」的判定
  （`cause == Dungeon.hero || cause instanceof Weapon || …`，命中才结算指令击杀计数、环指大师素材掉落、
  小小银河/笑靥、BOSS 判定、神谕庇佑、致命势能/致命迅捷）。泪剑的 `cause` 是 `MagicStrike` 实例，不在原白名单里，
  故已加 `|| cause instanceof TearSword.MagicStrike`——与 `DimDusk.MagicStrike`/`FlameStrike` 的先例完全一致。
  **EXP 与杀敌统计不受影响**：它们在 `Mob.destroy()` 里结算（无 `cause` 参数），而 `Char.die()` 无条件调 `destroy()`。
  （对照：`Bleeding` 因为是"非英雄直接伤害"而没进白名单，只在环指大师职业下补发一次掉落，见 `Mob.die` 尾部。）

## 改动落点（本轮）

- `effects/TearSwordVisual.java`：补 `strikeTimer += dt`（唯一的功能性修复）。
- `actors/buffs/TearSword.java`：新增 `DAMAGE_PER_SWORD`；`strikeAt()` 追加伤害；javadoc 更新；`addStacks`/`setStacks` 补 `b.target == null` 兜底。
- `actors/hero/Hero.java`：`TearSword.strikeAt(...)` 移到 `attack(attackTarget)` 之后。
- `actors/mobs/Mob.java`：`die()` 的「英雄击杀」白名单加 `|| cause instanceof TearSword.MagicStrike`；新增 import。
- `items/armor/glyphs/AntiMagic.java`：`RESISTS.add( TearSword.MagicStrike.class )`；新增 import（见上「已登记进 AntiMagic.RESISTS」）。
- `actors/Char.java`：图标分支曾加 `instanceof TearSword`，**登记 RESISTS 后已删除该特判与 import**
  （图标改由 `RESISTS.contains(src.getClass())` 精确类匹配覆盖，不再需要特判）。
- `messages/actors/actors(_zh).properties`：`desc` 补上协同伤害描述（zh/en 双份）。

括号配平通过（`TearSwordVisual` / `TearSword` / `Hero` / `Char` / `Mob` / `AntiMagic`），**未代跑编译**（用户约定）。

---

# 2026-09-11 追加调整（四）：泪剑不被魔法免疫驱散 + 协同突刺的剑尖转向

两条独立需求：**①「魔法免疫」只免伤、不再驱散泪剑**；**②协同突刺要有「戳刺感」**——
突刺前先把剑尖转向怪物，收剑回位过程中同步转回原位角度。

## 需求 → 实现

| 需求 | 实现 |
| --- | --- |
| 泪剑不因持有者的魔法免疫而被驱散 | 伤害来源由 **buff 实例**改为**非 Buff 的 `TearSword.MagicStrike` 空标记类**；`AntiMagic.RESISTS` 改登记 `TearSword.MagicStrike.class`；`Mob.die` 击杀白名单同步改判 `cause instanceof TearSword.MagicStrike` |
| 免伤照旧（不回归） | 三者全部走 `src.getClass()`：`isImmune` 免伤、`AntiMagic.drRoll` 减伤、`RESISTS.contains` 法术图标 —— 登记标记类后**三条全部保留** |
| 突刺前把剑尖转向怪物 | 新增 `strikeRotate()`：冲刺段的**前 `STRIKE_TURN_PART`（= 40%）**就把剑尖转到位，之后才是明显位移 |
| 角度 = 指向怪物 | `angle = atan2(gy − cy, gx − cx) − (朝左 ? 135° : 45°)`；`(gx, gy)` 是**该剑自己的落点**（已含 `STRIKE_REACH`/`STRIKE_SPREAD`），故三柄剑的朝向略有分叉 |
| 收回时转回原位 | 转向曲线的收回段与位移**共用同一条 smoothstep 归零曲线** ⇒ 边收边转回 |
| 不绕远路 | 角度插值经 `angleDelta()` 归一化到 (−180, 180] 取最短路径 |

## 一、免伤与「不驱散」为什么能分开

`AntiMagic.RESISTS` 的消费点只有三处，其中**只有一处**关心「是不是 buff」：

| 消费点 | 判据 | 登记标记类后 |
| --- | --- | --- |
| `Char.damage`：`isImmune(srcClass)` ⇒ `damage = 0` | `src.getClass()` | ✔ 免疫照旧 |
| `Char.damage`：`RESISTS.contains(src.getClass())` ⇒ 法术图标 + 雕文 `drRoll` 减伤 | `src.getClass()` | ✔ 照旧 |
| `MagicImmune.attachTo()`：遍历 `target.buffs()` 按类 `detach()` | **`Buff` 子类之间的类匹配** | ✔ 不再命中泪剑 |

第三条是唯一的「按类驱散」路径，判据为 `b.getClass().isAssignableFrom(immunity)`；
`MagicStrike` 不是 `Buff`、与 `TearSword` 也无继承关系，因此匹配失败 ⇒ 泪剑留下。
顺带也解决了「免疫期间叠不上去」（`Buff.attachTo` 的 `isImmune` 同样只看 `TearSword.class`，它已不在 RESISTS 里）。

**行为等价性核查**：换来源前后，`src` 都**不是 `Char`/`Hero`**（原先 buff 实例、现在空标记类），
所以 `Char.damage` 里 `if (!(src instanceof Char))` 的「保护光环减伤」分支、`src instanceof Hero` 的
寂静代价加成、`isInvulnerable(src.getClass())`（只看 buff、与 src 类型无关）等**全部行为不变**；
差异只有 `src.getClass()` 的取值本身，而它影响的正是上表三处、已随登记同步。无回归。

## 二、突刺转向的角度推导（含镜像分支）

noosa 变换为 `T(x,y)·T(origin)·R(angle)·S(scale)·T(-origin)`；由 `glwrap/Matrix.rotate` 推得旋转作用于
列主序矩阵即 `p' = (x·cos − y·sin, x·sin + y·cos)`，也就是 **`angle` 正值为屏幕上的顺时针**。

贴图剑尖位于相对 origin 的 (9,9)，未旋转时方向角 45°：

- **朝右**（`scale.x = 1`）：旋转后剑尖方向 = `angle + 45°` ⇒ 要指向 θ 取 `angle = θ − 45°`；
- **朝左**（`scale.x = −1`，剑尖先被镜像到 135°）：方向 = `angle + 135°` ⇒ `angle = θ − 135°`。

**纸面 + 数值双重验算**：用真实矩阵对 8 个方向 × 2 个镜像分支共 12 个组合计算剑尖最终指向，
全部与目标方向零误差；且 `wobble = 0` 时朝右 `angle = −45°` ⇒ 剑尖 **0°**、朝左 `angle = 45°` ⇒ 剑尖 **180°**，
与原有「原位角度」完全自洽 —— 说明该式不是另起一套，而是原式的一般化。
**正左/正右的近战本来就已对准**，所以肉眼能看出明显转向的是**斜角方向**的怪物（θ = ±45°/±135°）。

## 三、转向与位移的相位关系

`strikeRotate(t)` 与 `strikeAmount(t)` 分段形状相同，唯一区别是冲刺段把「到位时间」压缩到
`STRIKE_OUT · STRIKE_TURN_PART`（0.10 × 0.4 = **0.04s**）。实测曲线：

| t | 0.00 | 0.02 | 0.04 | 0.06 | 0.10 | 0.18 | 0.28 | 0.38 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 位置进度 | 0 | .104 | .352 | .648 | 1 | .977 | .432 | 0 |
| 转向进度 | 0 | **.500** | **1** | 1 | 1 | .977 | .432 | 0 |

即：**转向在 0.04s 就已完成，此刻位移才走了约 35%** ⇒ 观感是「先摆正剑头，再戳出去」；
收回段两列数值完全相等 ⇒ 「边收边转回原位」。两者两端均为 0，进出突刺都不跳变。

## 改动落点

- `actors/buffs/TearSword.java`：新增 `public static class MagicStrike {}`（空标记类，含完整推导注释）；
  `strikeAt()` 伤害来源改为 `new MagicStrike()`；类注释「法术伤害的登记」段整体改写。
- `items/armor/glyphs/AntiMagic.java`：`RESISTS.add( TearSword.MagicStrike.class )`（原 `TearSword.class` 已替换）。
- `actors/mobs/Mob.java`：击杀白名单改判 `cause instanceof TearSword.MagicStrike`。
- `effects/TearSwordVisual.java`：新增 `STRIKE_TURN_PART` 常量、`strikeRotate()`、`angleDelta()`；
  `update()` 的角度计算改为「原位角度 ↔ 指向落点」按 `strikeRotate` 插值；类注释补「突刺时的剑尖朝向」与角度换算段。
- `messages/actors/actors(_zh).properties`：`desc` 补「突刺时剑尖转向目标」「可被魔法免疫抵挡，但不会因此驱散泪剑」。

括号配平 + 角度公式数值验算通过，**未代跑编译**（用户约定）。

## 可调参数

- `TearSwordVisual.STRIKE_TURN_PART = 0.4f` —— 冲刺段内完成转向的比例。调小 → 转得更急（更「甩」）；
  调大到 1.0 → 转向与位移完全同步（就退化成上一版那种「平移感」的前半）。
- 转向的**绝对速度**由 `STRIKE_OUT` 与 `STRIKE_TURN_PART` 共同决定，与剑长无关。

# 2026-09-11 前二老板（VALENCINA）皮肤「泪锋之剑」

## 需求
贴图 `sprites/valencina.png` 第 8 行（**索引 7**，原本为空白预留行）已画好一套皮肤；需在**角色选择界面**加一个名为「泪锋之剑」的开关，开启后正式游戏中**无论穿着什么护甲都显示该行外观**。

## 实现
| 位置 | 改动 |
| --- | --- |
| `SPDSettings` | 新增 `KEY_VALENCINA_SKIN="valencina_skin"` 与 `valencinaSkin()` / `valencinaSkin(boolean)`（全局布尔，默认 false） |
| `HeroSprite` | `public static final int VALENCINA_SKIN_TIER = 7`；`public static int skinTier(HeroClass, int armorTier)`——前二老板且开关开启时恒返 7；`updateArmor()` 拆为 `updateArmor(boolean allowSkin)`（`disguise()` 传 `false`）；`avatar(Hero)` 改用 `skinTier` |
| `HeroSelectScene` | 新增 `CheckBox skinCheck`（`scenes.heroselectscene.valencina_skin`），**仅选中 VALENCINA 时显示**；横屏居中于职业描述与开始按钮之间，竖屏放开始按钮上方；`updateFade()` 同步 alpha/active；勾选时刷新 `HeroBtn` 图标 |
| `StartScene` / `WndGameInProgress` / `WndRanking` | 存档槽图标与头像统一改用 `HeroSprite.skinTier(...)` |

## 机制要点
- `HeroSprite.tiers(cls)` 对非 RING_MASTER 职业复用 **ROGUE 的 film**：`new TextureFilm(texture, texture.width, FRAME_HEIGHT)` ⇒ `cols=1, rows=128/15=8`，索引 **0..7 全部有效**，UV 按 256×128 归一化。
- 行分配：**0-5 = 护甲 tier，6 = ClassArmor(tier 6)（同时被 `HeroSelectScene.HeroBtn` / `StartScene` 当作立绘行），7 = 「泪锋之剑」皮肤**。
- 因复用的是 ROGUE film，`valencina.png` **必须保持 256×128**；皮肤行走原本空白的行 7，所以**不需要改贴图高度**。
- 皮肤为全局设置（非存档字段）：开关变化后，进行中的存档与存档槽图标都会同步改变外观。

## 待验证
跑 `build-desktop.bat` 后确认：选 VALENCINA 出现勾选框（其它职业隐藏）→ 勾选后按钮图标变化 → 进游戏穿任意护甲外观恒为第 8 行 → 开关切换时旧存档外观同步。

# 2026-09-12 新增神器「泪锋的加护」(TearSwordBlessing)：双形态泪剑特效

> **本阶段只做特效，未接任何数值/战斗效果**（护盾/防御、绝望 buff、充能消耗都留待后续）。

## 需求
1. 新神器「泪锋的加护」，**两个形态「加护」/「绝望」**，贴图分别在 `xy(7,39)` 与 `xy(8,39)`；
2. 消耗充能获得泪剑（设计，未实现）；
3. **加护**：按泪剑数量随时间给护盾并加防御，累积受击若干次后减泪剑（设计，未实现）；
4. **绝望**：按泪剑数量获得「绝望」buff（武器攻击力**上限**降低、下限不变）并协同攻击造成伤害，
   累积攻击若干次后减泪剑（设计，未实现）；
5. 本阶段只做特效：**绝望形态 = 已实现的泪剑特效**；**加护形态 = 剑尖竖直向下、绕角色在一个略小的圆内旋转**（仍有随机浮动）；
6. 两形态可**自如切换**，切换时泪剑**旋转并改变位置**。

## 需求 → 实现对照
| 需求 | 实现 |
| --- | --- |
| 两个形态 + 两张贴图 | `ITEM: ARTIFACT_TEAR_BLESSING = xy(7,39)` / `ARTIFACT_TEAR_DESPAIR = xy(8,39)`，`assignItemRect(..., 14, 16)`（实测两格内容均为 14×16，左对齐）；`updateImage()` 按 `form` 切贴图 + `Item.updateQuickslot()` |
| 绝望 = 现有泪剑特效 | `TearSwordVisual.Form.DESPAIR`（默认）：沿脚底上半圆、剑尖朝朝向、可协同突刺 —— 原逻辑原样保留 |
| 加护 = 剑竖直向下 | 新增 `BLESS_ANGLE = 45f`：贴图原始剑尖指向右下 45°，再顺时针 45° 即**竖直向下**；与 `BASE_ANGLE = −45f` 恰好互为相反数，故朝左/朝右都落在 90°（下），见下方数值验算 |
| 加护 = 绕角色旋转 + 略小的圆 | 圆心改为角色**躯干中心**（`cs.y + cs.height/2`），轨道半径 `BLESS_RADIUS = 11f`（绝望为脚底 `RADIUS = 16f`）；角度 `a = orbit + 2π(i+0.5)/n` 整圈均分，`orbit += ORBIT_SPEED·dt` 整体绕行 |
| 依旧随机浮动 | 浮动 / 摆动 / 惯性缓动 / 拖尾各段**与形态无关**，两种形态共用（绕行线速度 ≈ 11.6 px/s < `TRAIL_MIN_DIST/间隔` 阈值 ⇒ 单纯旋转不喷拖尾粒子，不会糊成一圈） |
| 自如切换 + 切换时旋转并改变位置 | 菜单动作 `AC_SWITCH`（`TearSwordBlessing.execute`）→ `TearSword.setStacks(hero, N, form)` → `TearSwordVisual.setForm()`：**位置**交给原惯性缓动自然挪到新轨道；**角度**叠一整圈附加旋转并让基准角平滑过渡（净转 3/4 圈） |
| 暂不做游戏效果 | 装备期间由被动 buff 维持 `PREVIEW_STACKS = TearSword.MAX_STACKS`（3 柄）+ 当前形态；`chargeCap=100` 且直接 `charge=chargeCap`（不蓄能也不消耗，避免状态栏顶 0%） |

## 关键实现点

### 一、形态存在哪里（两层，缺一不可）
- 泪剑本体是通用 buff `TearSword`，形态存在它的 `form` 字段（`TearSwordVisual.Form`，存档写 **0/1**：缺键 → 0 → 绝望，正好是「单独使用该 buff 时的原表现」）。
- 但**层数归零即 detach**，形态会随之丢失；所以神器自己也存一份（`TearSwordBlessing.STORE_FORM`，同样 0/1），
  每次行动/切换都用 `TearSword.setStacks(ch, n, form)` 把神器的形态带进 buff。
- `TearSwordVisual.setForm()` 只在**已经有剑**时播切换动画；**没有剑**（视觉刚建）时直接落到目标形态，
  避免「先以旧形态露一帧再转过去」。`ensureVisual()` 在重建视觉时也会 `setForm(form)` 兜底。

### 二、剑尖朝向的数值验算（沿用已验证的模型）
模型（由「突刺转向」那轮实测确认）：**最终剑尖屏幕角 = (朝左 ? 135 : 45) + 旋转角**。

| 朝向 | 形态 | 旋转角 | 剑尖角 | 结论 |
| --- | --- | --- | --- | --- |
| 朝右 | 绝望 | −45° | 0° | 指向右（朝向） |
| 朝左 | 绝望 | +45° | 180° | 指向左（朝向） |
| 朝右 | 加护 | +45° | **90°** | 竖直向下 |
| 朝左 | 加护 | −45° | **90°** | 竖直向下 |

### 三、切换为什么「旋转且不跳变」
切换时同时改两样东西，且两者在切换那一帧都与「切换前」完全等价：
1. **基准角**平滑过渡：`baseNow = baseFrom + (baseTo − baseFrom)·smoothstep(t/SWITCH_TIME)`，起点取**当前实际值** `baseNow`；
2. **附加旋转**：`switchSpin = spinNow ± 360°`，按 `switchSpin·(1−smoothstep)` 归零 —— 起点处它恰是**整整一圈（≡ 0°）**。

于是总角度 `baseNow + spinNow` 连续；净转动 = 90°（基准角变化）∓ 360° = **270°（3/4 圈）**。
中途连按切换也不会跳：新起点在「当前已有旋转」之上再加一圈，数值验算连续误差 **0.000°**。

### 四、加护形态的分层判据改用「上下」
原判据按**左右**（`facingLeft ? bx<cx : bx>cx`）只适合上半圆；整圈环绕若沿用，每柄剑转到正上/正下时会跨过中轴、
在脑袋/脚下**闪一次层**。改为按**上远下近**（`by < ccy` → 沉到 `GameScene.floorEmitters`），换层只发生在圆的**左右两端**
（剑离身体最远处），看不出跳变。

### 五、协同突刺按形态拦截
`TearSword.strikeAt()` 开头新增 `if (b.form != DESPAIR) return;` —— 加护形态「守而不攻」，既不播突刺动画也不结算伤害
（想反悔只需删这一行）。`TearSwordVisual` 本身不判形态，只做动画。

### 六、其它踩坑
- **局部变量重名**：`update()` 里新加的切换进度若叫 `p`，会与下方拖尾循环里的 `TearSwordTrailParticle p` 冲突
  （Java 不允许内层块遮蔽方法内已有局部变量）→ 改名 `sw`。**这次是 `javac` 单文件核验抓到的**。
- 神器被动 buff 用**非静态内部类**（同 `Admiration.AdmirationBuff`）：`Bundle` 会跳过非静态内部类不予重建，
  改由 `Artifact.activate()` 在装备/读档时现造；因此「卸下神器即收剑」写在 `BlessingBuff.detach()` 里。

## 改动落点
- **新增** `items/artifacts/TearSwordBlessing.java`（神器本体：双形态、`AC_SWITCH`、被动维持、存档）。
- `effects/TearSwordVisual.java`：新增 `Form` 枚举、`BLESS_RADIUS/BLESS_ANGLE/ORBIT_SPEED/SWITCH_TIME/SWITCH_SPIN`、
  `form()/setForm()`，`update()` 增加绕行角与切换动画、轨道按形态取、分层分形态；类注释新增「两种形态」「分层」段。
- `actors/buffs/TearSword.java`：新增 `form` 字段、`form(Char)/setForm(Char,Form)/setStacks(Char,int,Form)/applyForm()`，
  `strikeAt` 加形态拦截，存档写 `form`（0/1）。
- `sprites/ItemSpriteSheet.java`：新增两个常量 + `assignItemRect(...,14,16)`；顺带修正 `TX_HEIGHT` 的过期注释（640→800）。
- `messages/items/items(_zh).properties`：`items.artifacts.tearswordblessing.*`（name / ac_switch / cursed /
  switch_blessing / switch_despair / desc）。
- `AGENTS.md`：items.png 高度说明更新为 **256×800（50 行）**、39 行 7/8 列登记为本神器，陷阱表「图标全部错位」同步为 800。

## 可调参数
- `TearSwordVisual.Y_OFFSET = -4f`（**两种形态共用**的整体垂直偏移，负值 = 上移；直接加在轨道圆心高度上）；
- `TearSwordVisual.BLESS_RADIUS = 11f`（加护轨道半径）、`ORBIT_SPEED = 1.05f`（绕行角速度，一圈约 6s）；
- `SWITCH_TIME = 0.42f`（切换时长）、`SWITCH_SPIN = 360f`（**必须为 360 的整数倍**，否则起点不再等价于「不额外旋转」，会跳角）；
- `TearSwordBlessing`：`CHARGE_PER_SWORD=1`（唤剑充能）、`EXP_PER_USE=3`（每次唤剑攒的神器经验）、
  `LEVEL_CAP=10`、`CHARGE_PER_HERO_LEVEL=3f`（每「英雄等级」的经验换几点充能）、`START_CHARGE=0`；
  `SHIELD_CAP_PER_LEVEL=3`（护盾上限 3L）、`SHIELD_PERIOD=3f`（护盾结算周期）。
- `TearSword`：`DECAY_PER_STACK=3`（两形态共用减层阈值）、`DESPAIR_DAMAGE_BONUS=2`（协同伤害 x×(L+2)）。

## 待验证
- **验证**：调试窗（F2）「神器」页发放 → 装备；此时**充能为 0、泪剑为 0**（同神偷袖章，充能只能靠获得经验），
  先用调试窗「怪物」页刷怪打死 → 有充能后菜单出现「唤出泪剑」→ 唤满 3 柄；
  加护形态看护盾条随时间上涨 + 挨打掉剑；切绝望看协同突刺伤害与武器上限下降；存读档后等级/充能/层数/护盾均保持。
- **仍待实现**：把本神器接入获取途径（目前只能从调试窗拿；`Generator.ARTIFACT` 池与 `HeroClass` 初始物都未接线）。

---

# 2026-09-12 追加调整（六）：泪锋的加护 —— 接数值（充能/升级 + 两形态效果）

用户拍板的数值：**设泪剑层数 x、神器等级 L**——
① 充能/升级方式对齐**神偷袖章**（`MasterThievesArmband`）：**获得经验充能、使用升级**；
② 加护：每 `x/3` 回合积攒 `L` 点护盾（上限 `3L`）、护甲**最低**防御 `+(x+L)`、受击 3 次 −1 层；
③ 绝望：协同突刺 `x×(L+2)` 法术伤害、武器攻击力**上限** `−(3x+L)`（下限不变）、造成伤害 3 次 −1 层；
④ 两形态**共用同一个减层计数**（加护挨 2 下 → 切绝望打 1 下 = 掉 1 层）。

## 充能与升级（同神偷袖章）
| 项 | 实现 |
| --- | --- |
| 获得经验充能 | 覆写 `Item.onHeroGainExp(percent, hero)`（`Hero.earnExp` 会对每个随身物品回调）→ `partialCharge += 3f × percent × RingOfEnergy.artifactChargeMultiplier(hero)`，满 1 转 1 点 `charge`；只在**装备中且未诅咒**时生效；经验药水路径不遍历物品 ⇒ 不给充能 |
| 充能上限 | `chargeCap = 5 + level()/2`，在覆写的 `upgrade()` 里按**升级后**的等级重算（同神偷袖章） |
| 使用升级 | 每次「唤出泪剑」`exp += 3`（`EXP_PER_USE`），`exp >= 10 + round(3.33 × level())` 即连续 `upgrade()`（`levelCap = 10`），提示 `level_up` + `Catalog.countUse` + `Talent.onArtifactUsed` |
| 唤剑 | 新增动作 `AC_SUMMON`（菜单/快捷栏默认动作）：耗 1 点充能 +1 层泪剑，占 1 回合（`hero.spendAndNext(1f)`）；满 3 柄或充能不足时按钮不出现 |

## 两形态效果（实时按「当前形态 + 当前层数 + 神器等级」计算，不在 buff 上缓存）
| 效果 | 落点（宿主钩子） |
| --- | --- |
| 护甲最低防御 `+(x+L)` | `Hero.drRoll()`：护甲那段的 `NormalIntRange(DRMin(), DRMax())`，把 `DRMin` 侧抬高（并 `armMax = max(armMax, armMin)`） |
| 武器上限 `−(3x+L)` | `KindOfWeapon.damageRoll(Char)`：`Hero.heroDamageIntRange(min, max − penalty)`，削到低于下限时取平；削的是 augment/力量加成**之前**的原始掷值 |
| 协同伤害 `x×(L+2)` | `TearSword.strikeAt()` 里 `dmg = stacks * (artifactLevel + DESPAIR_DAMAGE_BONUS)` |
| 受击计数 | `Hero.damage()` 的 `effectiveDamage <= 0` 之后，**且过滤 `src instanceof Char`**（否则 DOT 跳伤会瞬间刷爆计数） |
| 造成伤害计数 | `strikeAt()` 结算伤害后推进（放在 `damage()` 之外：被魔法免疫挡下也算「剑刺出去了」） |
| 减层 | `TearSword.countDecay()`：两形态共用一个 `decayCount`，满 `DECAY_PER_STACK=3` 减 1 层并清零，层数归零时 buff 自行 detach（计数随之消失） |

## 新增文件 / 改动落点
- **新增** `actors/buffs/TearShield.java extends ShieldBuff`：带 `cap`（=3L）的护盾，**无自然衰减**，`add(ch, amount, cap)` / `clear(ch)` 两个静态入口；
  图标用 `BuffIndicator.ARMOR` + 青蓝 tint（与屏障的蓝、奥术护甲的紫区分）；`cap` 存档。
- `items/artifacts/TearSwordBlessing.java`：重写为「充能/升级 + `AC_SUMMON` + 被动只同步形态与护盾」；
  删掉 `PREVIEW_STACKS`（层数改由唤剑/减层管理）；新增 `shieldProgress`（存 `shieldProgress` 键）与 `desc_worn` 状态行。
- `actors/buffs/TearSword.java`：新增 `artifactLevel(Char)`（**拉取式**：直接查 `belongings.artifact/misc` 里的神器等级，不靠神器每回合推）、
  `armorMinDrBonus` / `weaponMaxPenalty` / `onHitTaken` 三个静态查询 + `of()` 幽灵兜底 + `countDecay`；`strikeAt` 换伤害公式并计数；
  存档新增 `decay_count`；删掉 `DAMAGE_PER_SWORD`。
- `actors/hero/Hero.java`：`drRoll()` 护甲段 + `damage()` 受击计数（两处 mod 钩子）。
- `items/KindOfWeapon.java`：`damageRoll(Char)` 武器上限削减（+ import `TearSword`）。
- 文本：`items.artifacts.tearswordblessing.*` 增 `ac_summon/no_charge/full_swords/summon/level_up/full/desc_worn/desc_cursed`
  并把 `desc` 改为「充能升级 + 两形态 + 共用计数」；`actors.buffs.tearsword.desc` 精简为视觉描述，另加 `desc_blessing/desc_despair`
  在 `TearSword.desc()` 里按形态追加当前数值；**新增** `actors.buffs.tearshield.name/desc`（zh/en 双份）。
- `AGENTS.md` §4：新增两条通用经验（三个「对英雄生效」的宿主钩子；`onHeroGainExp` 物品级经验钩子）。

## 核验
- **编译级核验通过**（单文件 `javac`：`TearShield/TearSword/TearSwordBlessing/KindOfWeapon/Hero` 一次 EXIT=0；
  classpath = `SPD-classes/build/classes/java/main` + `core/build/classes/java/main` + `.gradle/caches` 全 jar，`-proc:none`、临时目录已删）。
  **抓到一处真错误**：`TearShield` 漏 `import ...actors.Char`。
- zh/en 键一致性脚本校验通过（`tearswordblessing.*` 14 键、`buffs.tear{sword,shield}.*` 6 键，无缺键/无重复）。**未跑 Gradle**（用户约定）。

---

# 2026-09-12 追加调整（七）：精简神器 / 泪剑文案

把描述文本里的「开发说明腔」删掉，只留玩家需要知道的机制；游戏 UI 已直接可见的数值不再在文本里重复。

## 改动
| 位置 | 改动 |
| --- | --- |
| `items.artifacts.tearswordblessing.desc`（zh/en） | 删去**充能机制的详细解释**（经验→充能→唤剑的完整链条、升级曲线、等级上限 10）与**动画行为描述**（「竖直向下，缓缓绕着你旋转」「排成半环」「爆发不再、伤害趋于一致」），压成一短句「获得经验可为它积蓄充能；充能足够时可在菜单中唤出泪剑（至多 3 柄）」+ 两个形态各一句话 |
| `items.artifacts.tearswordblessing.desc_worn` | **整键删除**（「当前：等级 %1$d，充能 %2$d/%3$d，泪剑 %4$d/%5$d 柄」——等级在物品标题、充能在神器状态栏百分比、层数在 buff 图标角标，UI 里都已直接可见） |
| `TearSwordBlessing.desc()` | 去掉 `desc_worn` 分支，只在**被诅咒且已装备**时追加 `desc_cursed` |
| `actors.buffs.tearsword.desc`（zh/en） | 删去**动画细节**（圆心上半圆/半径 16px/竖直向下/整圆绕行/浮动/惯性/深蓝尾迹/水平翻转）与「当前 %1$d/%2$d 层」（buff 图标角标已显示层数），压成「每层一柄剑 + 两形态各一句 + 魔法免疫」 |
| `TearSword.desc()` | 保留 `Messages.get(this, "desc", stacks, MAX_STACKS)` 实参（`String.format` 忽略未引用实参），**日后想把层数写回文案只改 properties 即可，不用动 Java** |

- `desc_blessing` / `desc_despair`（两形态的**实时数值**行）**保留**：护甲最低防御 +X、协同伤害、武器上限 −X 这些是 UI 看不到的信息。
- `desc_cursed` 保留（诅咒状态提示）。

## 神器描述文本位置（供手工修改）
- **中文**：`core/src/main/assets/messages/items/items_zh.properties` 第 **2710** 行 `items.artifacts.tearswordblessing.desc=`
- **英文**：`core/src/main/assets/messages/items/items.properties` 第 **2550** 行 `items.artifacts.tearswordblessing.desc=`
- 泪剑 buff：`.../actors/actors_zh.properties` 第 **2147** 行、`.../actors/actors.properties` 第 **1964** 行（`actors.buffs.tearsword.desc=`）
- 文案里 `_..._` 是**斜体**标记（SPD 富文本），`\n` 是换行（**单反斜杠**，properties 里不要写成 `\\n`）。

## 核验
- 单文件 `javac`（`TearSword` + `TearSwordBlessing`）**EXIT=0**；未跑 Gradle（用户约定）。
- zh/en 键一致性脚本校验通过：神器 13 键、`buffs.tear*` 6 键，无缺键、无重复；`desc_worn` 全库无残留引用。
- `String.format` 多余实参安全性实测确认（`String.format("abc",1,3)` → `abc`）。

---

# 2026-09-12 追加调整（八）：「泪锋的加护」加护形态修复 + 区间夹取规范化

用户反馈：加护形态**只有「护甲最低防御」生效**，**护盾不积攒**、**受击不减少泪剑**；并要求检查「修改最大最小值」的行为，保证区间不越界。

## 一、护盾不积攒：结算点从「神器被动 buff」搬到「泪剑自己的 act()」
**根因**：护盾原写在 `TearSwordBlessing.BlessingBuff.act()` 里。这是个**非静态内部类**，靠
`Artifact.activate()` → `passiveBuff()` 现造并 `attachTo` 才存活——它是否在跑，与「泪剑在不在转」
并不共享同一条链路（泪剑的视觉由 `TearSword.act()` 的 `ensureVisual()` 驱动）。
另外还有一个**静默归零**：护盾量 = `L`（神器等级）、上限 = `3L`，而神器**初始等级为 0**
（`START_CHARGE=0`，升 1 级要 10 点经验 = 至少 4 次唤剑），于是「L=0 ⇒ 每回合 +0 盾、上限 0」
完全等于没有这个效果——而同一时刻「护甲最低防御 = x + L = x」却是看得到的。

**修复**（`actors/buffs/TearSword.java`）：
- 护盾结算搬进 **`TearSword.act()`**：本 buff 只要还有剑就必然每回合 act（`ensureVisual()` 就挂在这里，
  所以「剑在转」本身就证明这段代码在跑），盾的积攒与剑的存续变成了**同一份生命期**，
  从结构上消灭「剑在转、盾不涨」。
- 新增字段 `shieldProgress`（存档键 `shield_progress`），并把 `SHIELD_PERIOD=3f` /
  `SHIELD_CAP_PER_LEVEL=3` 两个常量从神器搬到 `TearSword`。
- **L=0 按 1 算**（`Math.max(1, art.level())`）：否则神器升级之前这半形态完全是空的。
  要严格按字面的 `L`，把这个 `Math.max` 删掉即可。
- 仍然要求「神器装备中且未被诅咒」（`artifact()` + `art.cursed`），与旧行为一致。
- 神器的 `BlessingBuff.act()` 从此**只做形态同步**；`detach()` 仍负责卸下时收剑 + 清盾。

## 二、受击不减少泪剑：计数点从「减免结算之后」前移到「减免结算之前」
**根因**：`TearSword.onHitTaken()` 原放在 `Hero.damage()` 的 `if (effectiveDamage <= 0) return;`
**之后**——而 `effectiveDamage` 是「实掉的血 + 掉的盾」。于是两种很常见的情况都被吞掉：
① 伤害被护甲减到 0（高 DR / 满护甲挨打）；② 被 `TearShield`/屏障整口吃下（**攒了盾反而刷不掉剑，与设计正好相反**）。
若在试炼场（`Dungeon.depth > 15 && branch == 1`，那里 `dmg` 被直接置 0）测试，则永远不计数。

**修复**（`actors/hero/Hero.java`）：钩子前移到「最终 `dmg` 已算出、还没进 `super.damage`」处：
```java
if (dmg > 0 && src instanceof Char && src != this) TearSword.onHitTaken( this );
```
- `dmg > 0`：免疫 / 试炼场把伤害置 0 的路径不计（那些本来就不算「挨打」）；
- `src instanceof Char`：剔除流血/毒/燃烧这类每回合跳一次的 DOT（否则瞬间把剑刮光）；
- `src != this`：排除自伤（圣宣/雕文等）。
其余「挨一下就算一次」——满护甲 0 伤害、被盾吃掉，都算。加护形态门槛（`form == BLESSING`）保持不变。

## 三、区间夹取规范化（「降低最大值不低于最小值、增加最小值不高于最大值」）
两处「动别的物件的 min/max」的地方，全部改用 `TearSword` 上的具名工具，规则写死在一处：
| 工具 | 语义 |
| --- | --- |
| `TearSword.clampRaisedMin(min, max, raise)` | 抬高下端；抬后**绝不高于上端**（是否截断见下） |
| `TearSword.clampMaxToMin(min, max)` | 兜底：上端不低于下端（下端抬高后必调一次） |
| `TearSword.clampReducedMax(min, max, reduce)` | 压低上端；削后**绝不低于下端**（差值不足取平） |
| `TearSword.ARMOR_MIN_BONUS_RAISES_MAX`（默认 true） | `true` = 抬下端时把上端**一并抬到同值**（`+N` 足额兑现，护甲区间整体上移）；`false` = 把下端**截断在原上端**（上限不动，窄区间护甲会吃掉加成） |
**两处落点**：`Hero.drRoll()`（护甲最低防御，抬下端 + `clampMaxToMin` 兜底）、
`KindOfWeapon.damageRoll(Char)`（武器上限，`clampReducedMax`）。
**数值验算**（默认 `true`，全部合法 `min ≤ max`）：布甲 `0~1 +8 → 8~8`；皮甲 `2~5 +4 → 6~6`；
板甲 `5~12 +8 → 13~13`；武器 `1~5 −9 → 1~1`、`1~10 −3 → 1~7`。
置 `false` 时：`0~1 +8 → 1~1`、`2~5 +4 → 5~5`（加成被上限吞掉）。

## 核验
- 单文件 `javac`（`TearSword` / `TearShield` / `TearSwordBlessing` / `KindOfWeapon` / `Hero`）**EXIT=0**；未跑 Gradle（用户约定）。
- 数值验算：护盾节奏（x=1 每 3 回合、x=2 每 1.5、x=3 每回合；上限 3L 处封顶）、两形态共用计数
  （加护挨 2 下 → 切绝望打 1 下 = 掉 1 层）、两处夹取的边界组合，全部符合预期。

> **后续变动**：本节的 `ARMOR_MIN_BONUS_RAISES_MAX` 开关、`SHIELD_CAP_PER_LEVEL` 常量、
> 以及「L=0 按 1 算」的处理，均已被 **追加调整（九）** 取代（护甲定案截断、护盾改 `L+2` / 上限 `3L+6`）。

---

# 2026-09-12 追加调整（九）：护甲夹取定案「截断」+ 加护护盾公式改写

## 一、护甲最低防御 → 定案「截断」
**删除** `TearSword.ARMOR_MIN_BONUS_RAISES_MAX` 开关（不再保留「足额抬上端」分支），
`clampRaisedMin` 变为纯截断：

```java
public static int clampRaisedMin( int min, int max, int raise ) {
	if (raise <= 0) return min;
	return Math.max( min, Math.min( Math.max( min, max ), min + raise ) );
}
```

- 语义：抬后的**下端被夹在原有上端之内**，`护甲上限永远不被这条效果改动`；
  当加成量 > `上端 − 下端` 时，超出部分被上端吞掉（**加成不足额**）。
- 例：布甲 `0~1 +3 → 1~1`；皮甲 `0~5 +2 → 2~5`（足额）；板甲 `5~12 +8 → 13~13`
  ——注意**上端只有在「下端被抬到超过它」时才会被截断吞吃**，`+8` 这种量在宽区间上仍是全额生效。
- `clampMaxToMin` 保留但已退化为**幂等护栏**（截断后 `min ≤ max` 恒成立），`Hero.drRoll()` 两行结构不变。
- `clampReducedMax`（绝望形态削武器上限）**不受影响**，仍是「削到低于下限就取平」。

## 二、加护形态护盾公式改写
| | 旧（追加调整八） | 新（本次） |
| --- | --- | --- |
| 结算频率 | 每 `x/3` 回合一次 | 每 `3/x` 回合一次（**不变**，同一常量） |
| 单次护盾量 | `L` | **`L + 2`** |
| 护盾上限 | `3L` | **`3(L+2) = 3L + 6`** |
| L=0 时 | 特判按 1 算 | **按公式原值**（单次 2 点、上限 6 点） |

**常量改动**（`actors/buffs/TearSword.java`）：
- `SHIELD_PERIOD = 3f` —— 不变（3 柄每回合一次、1 柄每 3 回合一次）；
- **新增** `SHIELD_AMOUNT_BONUS = 2` —— 单次结算量 = `神器等级 + 本值`；
- `SHIELD_CAP_PER_LEVEL = 3` → **`SHIELD_CAP_TICKS = 3`** —— 上限 = 本值 × 单次结算量，
  即「最多攒三次」，等价于 `3(L+2)`。
- `act()` 内改为 `int perTick = lvl + SHIELD_AMOUNT_BONUS;`（`lvl = Math.max(0, art.level())`，
  **去掉了旧的 `Math.max(1, level)` 特判**——公式自带下限，不会再出现「一次结算 0 点」），
  结算时 `TearShield.add( target, perTick, SHIELD_CAP_TICKS * perTick )`。

**数值表**（单次 / 上限）：

| L | 0 | 1 | 2 | 3 | 5 | 10 |
| --- | --- | --- | --- | --- | --- | --- |
| 单次 | 2 | 3 | 4 | 5 | 7 | 12 |
| 上限 | 6 | 9 | 12 | 15 | 21 | 36 |

**节奏**：x=1 → 每 3 回合一次（攒满上限 9 次 ≈ 27 回合）；x=2 → 每 1.5 回合一次；x=3 → 每回合一次
（攒满 3 次 = 3 回合）。**剑越多攒得越快**，这一条与旧版一致。

**文案**：`actors.buffs.tearsword.desc_blessing` 由 1 个参数扩为 3 个
（`%1$d` 护甲加成、`%2$d` 单次护盾、`%3$d` 上限），zh/en 双份同步；
`TearSword.desc()` 里按 `perTick` 实时算出后传入，`TearShield` / `TearSwordBlessing` 的
「上限 = 3L」注释同步改为 `3(L+2)`。

## 核验
- 单文件 `javac`（`TearSword` / `TearShield` / `TearSwordBlessing` / `KindOfWeapon` / `Hero`）**EXIT=0**；
  未跑 Gradle（用户约定）。
- 全局检索确认 `ARMOR_MIN_BONUS_RAISES_MAX` / `SHIELD_CAP_PER_LEVEL` 已无任何引用残留。

---

# 2026-09-12 追加调整（十）：切换形态时清空所有泪盾

需求：切换泪剑形态（加护 ⇄ 绝望）时，把身上所有 `TearShield` 泪盾一并清空——加护攒下的盾**不跨形态延续**，
切回加护也要从零再攒。

## 落点（两处，缺一不可）
| 位置 | 覆盖的情况 |
| --- | --- |
| `TearSword.applyForm(Form)`：形态**真正变化**时 `TearShield.clear(target)` | 有泪剑 buff 的形态被改写——`setForm(Char,Form)`（玩家切换 / 读档同步）与 `setStacks(...,Form)`（唤剑带入新形态）两条路径都汇到这里 |
| `TearSwordBlessing.switchForm(hero)`：在 `TearSword.setForm` 之后显式 `TearShield.clear(hero)` | 「泪剑已耗尽（`TearSword` buff 已 detach）、但盾还留着」——此时 `setForm` 找不到 buff，代不了手 |

## 实现细节
- `setForm(Char, Form)` 不再自己改字段，改为**委托 `applyForm(f)`**：形态早退、视觉同步、清盾三件事只写一处。
- 清盾带 **`stacks > 0` 守卫**：buff 刚被创建时（0 层）形态字段总要从默认的 `DESPAIR` 变成神器要求的形态，
  那不算玩家切换形态；否则「泪剑耗尽 → 重新唤剑」会把玩家保留下来的盾**误清**。
- `BlessingBuff.act()` 每回合的兜底 `setForm` 同步：形态一致时 `applyForm` 早退，**不会**反复清盾。
- 泪剑单独消散（加护形态被受击计数耗光）**不动**泪盾——两者是独立资源，只有「卸下神器」「切换形态」才清。

## 文案
`actors.buffs.tearshield.desc` 末句由「卸下神器时护盾一并收回」改为「卸下神器或_切换形态_时，护盾一并清空」，
zh/en 双份；`TearSword` / `TearShield` 类注释同步（新增「切换形态 = 清空泪盾」小节）。

## 核验
单文件 `javac`（`TearSword` / `TearShield` / `TearSwordBlessing` / `KindOfWeapon` / `Hero`）**EXIT=0**；未跑 Gradle。

---

# 2026-09-12 三项改动：溶解之爱第三套图像 / 漆黑噤默等级同步修复 / 死亡证明层禁传送

## 一、溶解之爱：第三套图像接入 + 嬗变卷轴改为「三选一」

用户在原 `lovenew.png` 下方补画了第三套精灵，贴图由 **160×96（4 行 = 2 组）** 变为
**160×144（6 行 = 3 组）**；布局与既有两组完全一致（每组 2 行 = 20 帧，组内只用前 15 帧）。
已用 Pillow 逐格统计确认行 4-5 的 15 格均有像素（与行 0-3 同构）。

| 落点 | 改动 |
| --- | --- |
| `sprites/MeltingLoveSprite.java` | 新增 `PALETTE_C = 2` 与 `PALETTE_COUNT = 3`；新增 `static int normalizePalette(int)`（越界/旧档非法值一律落回 `PALETTE_A`）；`setPalette` 改用归一化；类注释改为「两张贴图、三种形态」 |
| `actors/mobs/MeltingLove.java` | `setPalette` / `summon(...)` / `restoreFromBundle` 全部过 `normalizePalette`；字段与访问器注释改为 0/1/2 |
| `items/artifacts/Admiration.java` | 「赠予嬗变卷轴」的切换由**两组往返**改为**三组随机换到另外一组**：`do { next = Random.Int(PALETTE_COUNT); } while (next == love.palette());`；`lovePalette` 读档同归一化；补 `import Random` |

- `configureLove()` 用 `b = palette * LOVENEW_GROUP_STEP` 取组起点，第 3 组 = 帧 40-54，**无需改动渲染逻辑**。
- 语义：**必定换到不同的一组**（原地不动不算切换），三组间无顺序偏好。

## 二、漆黑噤默：等级同步改用 `trueLevel()`（修复临时/加成等级被固化）

**根因**（`items/weapon/melee/MorphWeapon.morphInto`）：切换形态时用

```java
replacement.level( current.level() );   // ✗
```

而 `Weapon.level()` 的语义是**持久等级 + 加成**：

```java
	@Override
	public int level() {
		int level = super.level();
		if (curseInfusionBonus) level += 1 + level/6;   // 诅咒菱晶的加成
		return level;
	}
```

于是每次切换都把「当前含加成的等级」写进新形态的 `level` 字段（真实等级），
`curseInfusionBonus` 又被 `morphInto` 原样复制带走 → **加成被固化一次、再算一次**，逐次累积，
`level/6` 部分还会随等级放大，最终等级雪崩式膨胀。

**修复**：改用 `current.trueLevel()`（`Item.trueLevel()` = 不含任何加成、只由升级得到的纯等级）。
加成本身仍由 `curseInfusionBonus` / `masteryPotionBonus` / `enchantHardened` 的复制带走，
所以在哪个形态上都照常显示与生效。

## 三、死亡证明（999 层陈列室）：禁止一切传送类道具

999 层是死路布局（无楼梯、无重生点），「返程」依赖 `DeathCertificate` 记下的原点 + 拾起展品触发；
任何传送都会把英雄/物品挪到预期之外的位置，甚至打断返程流程。

**清单集中在 `levels/MuseumLevel.java`**（新增两个静态方法，日后增删只改这一处）：

```java
public static boolean isTeleportBlocked( Item item )   // instanceof 判定，子类同样命中
public static boolean blockTeleport( Item item )       // 不在该层 / 不在清单 → false；否则 GLog 提示并返回 true
```

覆盖清单：传送卷轴、秘卷·通道、念力结晶、转移结晶、返回晶柱、劳埃德信标、闪现符石、传送飞镖、逃脱棱晶、测试传送门。

**拦截落点**（均在「开始生效/消耗之前」，被拦下时**不消耗、不产生投射物、不进入瞄准流程**）：

| 文件 | 位置 | 覆盖对象 |
| --- | --- | --- |
| `items/Item.java` | `execute()` 的 `AC_THROW` 分支开头 | 闪现符石（Runestone）、传送飞镖等一切投掷型 |
| `items/scrolls/Scroll.java` | `execute()` 的 `AC_READ` 分支开头 | 传送卷轴、秘卷·通道 |
| `items/spells/Spell.java` | `execute()` 的 `AC_CAST` 分支开头 | 念力结晶、转移结晶、返回晶柱 |
| `items/artifacts/LloydsBeacon.java` | `execute()`（`super` 之后） | 劳埃德信标（设定/返回/传送敌人） |
| `items/quest/EscapeCrystal.java` | `execute()` 的 `AC_USE` 分支内 | 逃脱棱晶 |
| `items/TestPortal.java` | `execute()` 的 `AC_USE` 分支内 | 测试传送门 |

**文案**：`levels.museumlevel.no_teleport`（`%s` = 道具名），zh 加在 `levels_zh.properties`、
en 加在 `levels.properties`（key 规则 = 全类名去前缀 + 小写，见 `Messages.get`）。

## 核验
- 单文件 `javac`：任务一/二（`MorphWeapon` / `DarkSilence` / `Admiration` / `MeltingLove` / `MeltingLoveSprite`）**EXIT=0**；
  任务三（`MuseumLevel` / `Item` / `Scroll` / `Spell` / `LloydsBeacon` / `EscapeCrystal` / `TestPortal` / `DeathCertificate`）**EXIT=0**；未跑 Gradle。
- 投掷链核对：`Dart.execute` → `Item.execute(AC_THROW)`、`Runestone` 未覆写 `execute`，两条都汇到 `Item` 的拦截点。

## 四、附魔的武器等级影响：欲望不动、蜚蠊补上（用户拍板）

用户裁定：**「欲望」的公式里已经含等级（概率 `(等级+1)/(等级+5) × 附魔强度`），不需要改**；
**「蜚蠊」**按「速度与上限同步提升」方案补上武器等级。

`items/weapon/enchantments/Roach.java`：

| | 旧 | 新 |
| --- | --- | --- |
| 积累速度 | `0.2 × 附魔强度` /回合 | `0.2 × 附魔强度 × (1 + 武器等级/5)` |
| 积累上限 | `10 × 附魔强度` | `附魔强度 × (10 + 2 × 武器等级)` |

- 新增常量 `RATE_LEVEL_DIVISOR = 5f`（每 5 级速度翻倍）与 `CAP_LEVEL_STEP = 2`；
  新方法 `AttackVermin.rate()`，`cap()` 改为按等级算。
- `AttackVermin` 新增字段 `level`（存档键 `weapon_level`，旧档缺键 → 0，下次命中即被真实等级覆盖）；
  `release(...)` 签名改为 `release(float newPower, int weaponLevel)`，`proc` 里传 `weapon.buffedLvl()`。
- **速度与上限同步提升** ⇒ **攒满时间恒定 ~50 回合**，武器等级只改变「一次释放能存多少」：

| 武器等级 | 0 | 1 | 3 | 5 | 10 |
| --- | --- | --- | --- | --- | --- |
| 速度（/回合，强度 1） | 0.20 | 0.24 | 0.32 | 0.40 | 0.60 |
| 上限（强度 1） | 10 | 12 | 16 | 20 | 30 |
| 上限（强度 2） | 20 | 24 | 32 | 40 | 60 |

- 文案：`items.weapon.enchantments.roach$attackvermin.desc` 补一句「附魔强度以及_武器等级_越高……」（zh/en 双份）。
- 核验：`Roach` / `Desire` 单文件 `javac` **EXIT=0**；数值表用脚本验算，各等级攒满时间均为 50 回合。

## 五、泪锋的加护入池 + 0.2.2 打包（2026-09-12）

- **入池**：`items/Generator.java` 的 `ARTIFACT.classes` 按字母序插入 `TearSwordBlessing.class`
  （`TalismanOfForesight` 与 `TimekeepersHourglass` 之间），`ARTIFACT.defaultProbs` 同步在第 13 位追加 `1`
  → 两数组长度 **14 → 15（必须等长）**，此前只有 `Admiration` 一个自定义神器在池内。
  - 权重 `1` 的含义：`randomArtifact()` 取出后执行 `cat.probs[i]--` ⇒ **单局最多出现一件**，与 `Admiration`、原版神器一致；
    因此泪锋加护现在能正常从宝箱/商店等渠道刷出，不再只能靠调试窗获得。
  - 旧档兼容：`Generator.restoreFromBundle` 仅在 `probs.length == defaultProbs.length` 时回填存档权重；
    长度不符则保持 `defaultProbs.clone()` 并走既有的 ARTIFACT 迁移分支。池内权重除 0（隐身斗篷/圣典）外全为 1，
    逐索引推演迁移后映射与新数组一致（`probs[3]`/`probs[6]` 仍为 0，新位 `probs[12]` 得 1），旧档不会因本次插入丢神器。
- **版本**：`build.gradle` `appVersionCode 921 → 922`、`appVersionName 0.2.1 → 0.2.2`。
- **产物**：`android/build/outputs/apk/debug/EGOPD_0.2.2.APK`（37,142,627 B）。
  `aapt dump badging` 实测：`versionCode=922`、`versionName=0.2.2-INDEV`、
  `applicationId=com.mypd.mypixeldungeon.indev`、minSdk 21 / targetSdk 36。
  含本次改动的证据链：`:android:dexBuilderDebug` 为 executed（非 UP-TO-DATE）→
  `javap -c Generator$Category.class` 中 `TearSwordBlessing` 类字面量位于 `Admiration` 与 `TimekeepersHourglass` 之间 →
  `core-0.2.2.jar` 内该 class 与 `build/classes` 上的 md5 完全一致（`e0764c8f…`）。
- **打包踩坑**（同步进 AGENTS.md 第 1 节）：`JAVA_HOME` 未设 → 落到系统 JDK 1.8 → Gradle 9.4 直接拒跑
  （项目自带 JDK 21 在 `tools/jdk-21.0.12.1+1`）；桌面调试构建会长时间占用 `executionHistory.lock`，
  其它构建报"拒绝访问"；被中断的守护进程会继续跑并占住 `desugar_graph/…/graph.bin`，
  需 `gradlew --stop`（无效则强杀该 PID）+ 删 `android/build/intermediates/desugar_graph` 后重打。

# 2026-09-13 新角色「中指 长兄」(MIDDLE_FINGER) + 文本删除线标记（`~~`）

## 需求
1. 新增角色「中指长兄」，其中**「长兄」两字以删除线显示**（若难以实现可跳过）；
2. 内容**暂时整体套用战士**；
3. 角色精灵图用 `sprites/matthias.png`、选角立绘用 `splashes/matthias.jpg`；
4. 新增专属职业护甲**「中指外套」**，贴图 `xy(15,12)`、尺寸 16×16。

## 一、文本删除线标记（通用机制，`ui/RenderedTextBlock`）
**为什么不能用字体做**：删除线常用的组合字符 U+0336（COMBINING LONG STROKE OVERLAY）在 `pixel_font.ttf` 与
`droid_sans.ttf` 的 cmap 里**都没有字形**（已逐段核对）；中文又走 `droid_sans.ttf` 由 FreeType **逐字**生成，
组合符的零宽叠放也不保证正确。故改为「标记 + 画线」。

**实现**（与既有 `_` / `**` 高亮标记同构）：
- `~~` 与 `_` / `**` 一样是**开关标记**，自身不渲染；新增与 `words` **等长**的 `strikes`
  （`ArrayList<ColorBlock>`，SPACE/NEWLINE 位置放 `null`）。
- `~~` 未必被平台切分器切成独立 token（英文整句通常是一个 token），所以**先按 token 判定，
  再对 token 内部 `indexOf("~~")` 循环切分**，两种情形都能处理。
- 画线用 `ColorBlock(1,1,0xFFFFFFFF)`（白色实心，靠 `hardlight(color)` 上色 ⇒ 与文字同色）；
  在 `layout()` **最后**定位：`x = word.x`、`y = word.y + (word.height()-粗)/2`、`width = word.width()`、
  粗 = `max(1, zoom)`。放最后是为了跟随居中/右对齐产生的位移。
- `hardlight` / `resetColor` / `alpha` 三个方法同步作用于 `strikes`；`zoom()` 走 `layout()` 自动跟随。
- **不冲突**：全项目源文本中没有 `~~`；`GLog` 的青色前缀常量 `"~~ "` 在 `ui/GameLog` 里已先 `substring` 剥掉，
  不会进入 `RenderedTextBlock`。
- 用法：任意 `RenderedTextBlock`（含 `StyledButton` / `IconTitle` / `GLog` 面板）文本写 `前~~中~~后` 即可。

## 二、角色接线（全链路兜底清单）
| 位置 | 改动 |
| --- | --- |
| `HeroClass` | 枚举末尾加 `MIDDLE_FINGER(HeroSubClass.BERSERKER, HeroSubClass.GLADIATOR)`；`initHero` → `initMiddleFinger(hero)`；`masteryBadge`/`spritesheet`/`splashArt`/`isUnlocked` 各加 case |
| `initMiddleFinger` | **直接委托 `initWarrior(hero)`**（破旧短剑 + 三块投石 + 布甲破损纹章 + 战士鉴定三件套），待专属开局时只改此方法 |
| `HeroSubClass` | **未新增**：专精复用 `BERSERKER`/`GLADIATOR` ⇒ `Talent.initSubclassTalents` 走既有分支 |
| `Talent` | **未改**：`initClassTalents(cls)` 三处 switch 都无该 case ⇒ 落到 `default:` = 战士天赋；`HEROIC_ENERGY` 图标同走 default(26) |
| `HeroClass.armorAbilities` | **未改**：落到 `case WARRIOR: default:` = 战士三技能（跳跃/冲击波/忍耐） |
| `Assets` | `Sprites.MATTHIAS = "sprites/matthias.png"`、`Splashes.MATTHIAS = "splashes/matthias.jpg"` |
| `HeroSprite` | **未改**：matthias.png 为 256×128、帧 12×15、21 列 × 8 行，与战士同布局 ⇒ 直接复用 ROGUE film |
| `Badges.validateMastery` | 加 case → `MASTERY_WARRIOR`（占位） |
| `Icons.get(HeroClass)` | 加 case → `ARMOR_MIDDLE_FINGER`（存档槽/排行榜图标） |
| `WndHeroInfo` | `tabIcon` 与三段描述图标（SEAL / WORN_SHORTSWORD / SCROLL_ISAZ）各加 case |
| `RemainsItem` | 加 case → `SealShard`（占位）；**2026-09-18 起已换成专属遗物 `LedgerPage`「账簿残页」**（拇指那边同样换成了 `BrokenEye`「破损义眼」，详见文末 2026-09-18 一节） |
| `Blacksmith` / `Wandmaker` | 加 case → 复用战士台词 |
| `ClassArmor.upgrade` | 加 case → `MiddleFingerCoat`（顺带修复该行原有的乱码注释） |

## 三、职业护甲「中指外套」
- `ItemSpriteSheet`：新增 `ARMOR_MIDDLE_FINGER = ARMOR+14`（= `xy(15,12)`，第 12 行第 15 格）+ `assignItemRect(..., 16, 16)`。
  常量必须声明在 `static{}` 赋值块**之前**（静态初始化按源码顺序执行）。
- 新建 `items/armor/MiddleFingerCoat.java`（`extends ClassArmor`，只设 `image`），行为与拇指大衣完全一致。
- 文本：`items.armor.middlefingercoat.name/desc`（zh = 中指外套）；**顺带补齐** en 侧此前缺失的
  `indexfingersuit` / `ringrobe` / `thumbcoat` 三条名称与描述。
- 已核对 `items.png`（256×800）第 12 行第 15 格（x 224..239 / y 176..191）**已有 236 个不透明像素**，贴图就位。

## 四、已知副作用 / 注意
- `GamesInProgress.MAX_SLOTS = HeroClass.values().length`，新增职业后 **9 → 10**：存档槽多一格。
  `StartScene` 会自动压缩槽间距，但屏幕很矮且存档很多时可能略显拥挤（该行为对全部自定义职业一致）。
- `WndRanking.StatsTab` 用的是 `record.heroClass.name()`（上游代码，非本次改动）：查看**非当前英雄**的排行榜时，
  标题显示的是枚举名（如 `MIDDLE_FINGER`），自定义职业都会这样，如需可另改。
- 编辑 `items/armor/ClassArmor.java` 时该文件曾被以 **GBK** 落盘（全项目唯一非 UTF-8 的 java 文件），已重新编码为 UTF-8。

## 五、待验证
跑 `build-desktop.bat`：① 选角界面出现第 10 个按钮（中指 长兄），标题「中指长兄」的「长兄」带删除线；
② 选角立绘为 matthias.jpg、按钮图标来自 matthias.png 第 6 行；③ 进游戏外观与战士一致；
④ 调试窗生成「中指外套」（或走盔甲技能解锁流程）图标为 `xy(15,12)`；⑤ 其它职业标题不受 `~~` 影响。

# 2026-09-13（晚）复仇账簿 / 指虎 / 封印之剑系列改造 + 中指 长兄专属开局

## 需求
1. 新神器**复仇账簿**：贴图 `xy(1,41)` 13×16；**百分比充能**（上限 100）——**损失生命**按「损失量 ÷ 生命上限」的百分比等比例充能，**击杀怪物**按「怪物生命上限 ÷ 英雄生命上限」的百分比的**十分之一**充能；使用后获得**仇怨** buff（期间**力量 +12**、**每回合 −2% 充能**、**充能归零自动解除**）；buff 图标画在「泪剑」(帧 112) 的下一位。
2. 新武器**指虎**：一阶、攻击延迟 0.5、伤害 `1+L ~ 5+L`；贴图 `xy(2,41)` 15×10；描述「以钢刃连接在指节套上而成的指虎。」+ 效果「这是一件非常快的武器。」
3. **封印之剑系列改造**：参考漆黑噤默的形态切换（保留等级/附魔等），装备时点「解封」依次推进 封印之剑 → 一阶段解封之剑 → 二阶段解封之剑 → 莱瓦汀；**取消装备时恢复为封印之剑**；等阶由「套用一阶数据」**改为六阶**、力量需求 **22**。
4. 以上三件（复仇账簿 / 指虎 / 莱瓦汀）作为**中指 长兄**的初始物品，**取消**其原先套用战士的初始物。

## 一、复仇账簿（`items/artifacts/RevengeLedger.java`）
| 项 | 值 / 位置 |
| --- | --- |
| 贴图 / 图标 | `ItemSpriteSheet.ARTIFACT_REVENGE_LEDGER = xy(1,41)`，`assignItemRect(…,13,16)`；buff 图标 `BuffIndicator.RANCOR = 113` |
| 充能 | `chargeCap = 100`，`charge` 为整数百分比、`partialCharge` 存小数（跨事件累积到 1 才进位）；`Artifact.status()` 在 `chargeCap==100` 时自动按 `%d%%` 显示 |
| 受伤充能 | `Hero.damage()` 里在 `int effectiveDamage = preHP - postHP;` **之后**加钩子：`if (postHPOnly < preHPOnly) RevengeLedger.onHeroDamaged(this, preHPOnly - postHPOnly);`。**用不含护盾的 HP 快照**（`preHPOnly/postHPOnly`），所以被护盾整口吃下的伤害不给充能；DOT（流血/中毒）掉的血照算（需求即「损失血量时」） |
| 击杀充能 | `Mob.die()` 英雄击杀分支（与 `RingMasterLoot.onEnemyKilled` 同处）调 `RevengeLedger.onEnemyKilled(Dungeon.hero, this)`；**必须在 `super.die()` 之前**读 `mob.HT` |
| 仇怨 buff | 非静态内部类 `Rancor extends ArtifactBuff`（同 `OdinsEye.precognition` 写法）：`act()` 每回合 `charge -= 2`、归零即 `detach()`；`iconFadePercent() = (100 − charge)/100`（灰罩＝已耗充能）；存档由神器 `storeInBundle/restoreFromBundle` 手动存取 |
| 力量 +12 | `Hero.STR()` 加一行：`if (buff(RevengeLedger.Rancor.class) != null) strBonus += RevengeLedger.RANCOR_STR;` |
| 通用充能入口 | 覆写 `Artifact.charge(Hero, float)` 转发到 `gainCharge()`，使 `ArtifactRecharge` 之类的外部充能来源可用 |
| 被动 buff | `ledgerRecharge`（只 `spend(TICK)`）——`Artifact.activate()` 会无条件 `passiveBuff().attachTo(ch)`，**返回 null 会 NPE**，任何纯事件驱动型神器都必须给一个占位被动 buff |

## 二、指虎（`items/weapon/melee/KnuckleDuster.java`）
`extends Gloves`（拳套：tier 1、`DLY = 0.5f`、`1+L ~ 5+L`），数值完全吻合；仍显式覆写 `min/max` 自文档化。
贴图常量 `KNUCKLE_DUSTER = xy(2,41)` + `assignItemRect(…,15,10)`。**不入武器池**（同磨损对剑/空酒瓶）。
文本键 `items.weapon.melee.knuckleduster.{name,desc,stats_desc}`。

## 三、封印之剑系列（新增 `SealedSwordBase`，四个子类改为继承它）
- 四个形态仍是**各自独立的类**（`SealedSword` / `UnsealedSword` / `UnsealedSword2` / `Laevateinn`，保留调试窗独立条目），
  基类 `SealedSwordBase extends MeleeWeapon` 统一提供：
  - `tier = 6`；`STRReq(int lvl)` 覆写为 `STRReq(7, lvl)` —— 七阶公式的基础值恰好是 **22**，升级减免与「力量药剂」−2 一并继承；
  - `AC_UNSEAL`「解封」：`actions()` 在装备中且未全解时给出；`execute()` 调 `unsealInto(this, hero, 下一形态)` 后 `hero.spendAndNext(1f)`；
  - `unsealInto(...)` = 漆黑噤默 `MorphWeapon.morphInto` 的同一套「保留数据」逻辑（`trueLevel()` / `enchantment` / `cursed` /
    `cursedKnown` / `levelKnown` / `quantity` / `masteryPotionBonus` / `enchantHardened` / `curseInfusionBonus`，外加快捷栏与装备/背包两条替换路径）；
  - **卸下还原**：`doUnequip()` 先把武器槽与快捷栏里的引用整体换成新的 `SealedSword`，再让**那个新实例**走正常卸下流程 ——
    `KindOfWeapon.doUnequip` 靠 `belongings.weapon == this` 判断清哪个槽位，**必须先换引用**，否则槽位会残留旧实例。
    被诅咒而无法卸下时（`cursed && MagicImmune==null && (!lostInventory || keptThroughLostInventory)`）**提前跳过换引用**，
    原样交给 `super` 打印「无法卸下」。
- 子类不再覆写 `name()`（原名硬编码中文），改为走 `items.weapon.melee.<类名小写>.name/desc/stats_desc`，zh/en 双份。
- 基类文本键：`items.weapon.melee.sealedswordbase.{ac_unseal,need_equip,unsealed}`（`unsealed` 为 `%s`，接新形态名）。

## 四、中指 长兄专属开局（`HeroClass.initMiddleFinger`）
不再调用 `initWarrior`（战士的破旧短剑/投石/布甲破损纹章/鉴定三件套全部取消）：
- 武器槽：**指虎**（`.identify()` + 显式 `activate(hero)` 挂武技充能）；
- 神器槽：**复仇账簿**（`.identify()` + `activate(hero)`）；
- 背包：**莱瓦汀**（`.identify().collect(hero.belongings.backpack)`）；快捷栏 0/1/2 分别设为 指虎 / 复仇账簿 / 莱瓦汀。

## 五、待验证（`build-desktop.bat`）
① 选中指 长兄：开局武器为指虎、神器为复仇账簿、背包里有莱瓦汀，且**没有**战士那套初始物；
② 挨打/杀怪时复仇账簿的快捷栏百分比上升（被护盾吃下的伤害不涨）；
③ 满充能后点「燃起仇怨」：力量 +12、buff 图标为帧 113 且随充能变灰，每回合掉 2%，归零自动消失；
④ 装备莱瓦汀点「解封」——已是最后一阶故无此按钮；改装封印之剑后连点三次可依次升到莱瓦汀，
   期间等级/附魔保持；**卸下即变回封印之剑**；
⑤ 调试窗「神器」页出现 复仇账簿（贴图 41 行 1 列 13×16）、武器页出现 指虎（41 行 2 列 15×10）。

# 2026-09-14 封印之剑改造：锁定双手 + 决斗家式主副切换 + 快捷栏解封

## 需求
1. 初始携带**封印之剑**形态（不再直接给莱瓦汀）；
2. 调研决斗家的主副武器体系，让莱瓦汀系列**无法从武器栏被换下**，只能切换到**副手**；**副手形态即还原为封印之剑**；
3. 让莱瓦汀系列**在快捷栏点击即可切换形态**。

## 一、初始物品：莱瓦汀 → 封印之剑（`HeroClass.initMiddleFinger`）
背包里改发 `new SealedSword()`，快捷栏 2 仍指向它；其余（指虎/复仇账簿）不变。

## 二、锁定：卸不下来（`SealedSwordBase.doUnequip`）
只要剑还在**主手 `belongings.weapon` 或副手 `belongings.secondWep`**，`doUnequip(...)` 一律 `GLog.w` + **返回 false**。
这一处就挡住了全部「离手」路径，因为它们最终都会回到它：
- **取消装备**：`EquipableItem.execute(AC_UNEQUIP)`；
- **丢弃 / 投掷**：`EquipableItem.doDrop()` = `if (!isEquipped || doUnequip(hero,false,false)) super.doDrop()`；`cast()` 同理；
- **被别的武器顶替**：`KindOfWeapon.doEquip()`、`equipSecondary()` 都先要求「当前槽位武器 doUnequip 成功」；
- **解除武装陷阱**（唯一一处**绕过** doUnequip 的路径）：`DisarmingTrap` 是直接 `hero.belongings.weapon = null`，
  所以必须在陷阱侧单独拦一道（已加 `!(weapon instanceof SealedSwordBase)`）。
另加 `keptThroughLostInventory() → true`：安卡复活「失去全部物品」时这把剑也不会从手上消失。

## 三、决斗家式主副切换按钮（`SealedSwordBase.SwordSwap`）
- 载体是**英雄身上的 Buff**（`implements ActionIndicator.Action`），因为 `ActionIndicator` 是全局单槽，
  游戏里所有候选（狂暴、角斗士连击、武技充能、苦痛技艺…）都是这么做的（参考 `MeleeWeapon.Charger`、`ScorchingWound`）。
- 挂载点：`SealedSwordBase.activate(Char)` 里 `Buff.affect(ch, SwordSwap.class)` —— 主手/副手两条路都会走 `activate`
  （`Belongings.restoreFromBundle` 对 `secondWep` 也会 `activate`），读档后按钮自动回来。
- 图标复用 `HeroIcon.WEAPON_SWAP`（帧 109，决斗家交换按钮），颜色 `0x5500BB` 同色；`icon()` 默认 `NONE` ⇒ **不占 buff 条**。
- **抢占策略**：只在 `ActionIndicator.action == null` 时占位（`attachTo` 与每回合 `act()` 各一次机会），
  把按钮让给狂暴/连击这类职业技能，它们 `clearAction` 后再接管 —— 避免两个 Buff 每回合互相顶掉。
- `act()` 还会自检「手上是否还有这把剑」，没有就 `detach()` + `clearAction`。

## 四、副手即封印（`swapHands(Hero)`）
照 `MeleeWeapon.Charger.doAction()` 的**直接对调两个槽位引用**（**绝不**走 `doUnequip`，否则会被自我锁定挡住）：
- 剑从主手 → 副手时，若当前不是封印形态，先 `copyOf(this, SealedSword.class)` 造一个**同数据的封印态替身**再放进去；
- 替身落位后要把**快捷栏里指向旧实例的引用**一并换掉（`Dungeon.quickslot.setSlot/clearItem`），否则快捷栏会握着一个已离场的实例；
- `landed.activate(hero)` + 对调过去的另一把武器也补一次 `activate`；
- 视觉/音效照交换按钮：`sprite.operate()` + `Sounds.UNLOCK` + `Item.updateQuickslot()` + `ActionIndicator` + `AttackIndicator.updateState()`；
- **不花回合**（与决斗家交换一致）。

## 五、快捷栏点击（`defaultAction()` 重写）
| 剑所在位置 | `defaultAction()` | 点击效果 |
| --- | --- | --- |
| 主手 | `AC_UNSEAL` | 推进到下一形态（`spendAndNext(1f)`）；已全解则提示「封印已完全解开」 |
| 副手 | `AC_SWAP` | 换回主手（副手恒为封印态，所以这一步同时是「取回」） |
| 背包里 | `super` 为 null 时兜底 `AC_EQUIP` | 正常装备 |

`AC_UNSEAL` 同时保留在动作菜单里；`execute()` 会校验「必须在主手」，否则提示 `need_main_hand`。

## 六、本地化
- 新增 `sealedswordbase.{ac_swap,need_main_hand,bound,full,swapped}`（zh/en 双份）；
- 删除已无引用的 `sealedswordbase.need_equip`；
- 三处 `stats_desc` 全部改写为新机制（锁手 + 右下角按钮 + 副手封印 + 快捷栏解封）。

## 七、已知取舍
`ActionIndicator` 只有一个槽：中指 长兄的两个子职业里，**角斗士的连击**（`Combo.hit()` 每次命中抢占）
和**狂战士的狂暴**（`Berserk.act()` 持续抢占）会与「切换主副」按钮轮流占位。
当前策略是**让位给职业技能**，所以战斗中按钮可能变成连击/狂暴；脱战（连击计时归零、狂暴结束）后下一回合恢复。
若要两者并存，需要给 `ActionIndicator` 增加第二个槽位（改 `Toolbar`/`GameScene` 布局），属另一个量级的改动。

## 八、待验证（`build-desktop.bat`）
① 选中指 长兄：背包里是**封印之剑**（不是莱瓦汀），快捷栏 2 指向它；
② 点击快捷栏 2 装备 → 右下角出现「切换主副」按钮（交换图标，紫色）；
③ 点动作菜单/快捷栏「解封」依次升到一阶段 → 二阶段 → 莱瓦汀，等级/附魔保持；
④ 点右下角按钮：剑进副手并**变回封印之剑**，主手空出；再点一次回到主手；
⑤ 尝试取消装备/丢弃/投掷/换别的武器顶替：全部被拒并提示「被锁死在手中」；踩解除武装陷阱也不掉剑；
⑥ 角斗士/狂战士实战中确认连击、狂暴按钮仍能正常出现。

# 2026-09-14（补）手感修正：卸下提示文本 + 交换按钮显示主副手武器

## 需求
1. 把「无法卸下」的提示文本改成：**「你并不想放下这把珍贵的剑，哪怕只有一刻。」**
2. 查看决斗家切换武器按钮的**显示方式**，参照它的既有实现，让本按钮**同时显示主手与副手武器**。

## 一、提示文本（`doUnequip`）
- 键 `items.weapon.melee.sealedswordbase.bound` 由 `%s被锁死在手中，无法卸下。` 改为固定文案
  **「你并不想放下这把珍贵的剑，哪怕只有一刻。」**（英文：`You would not set down this precious sword, not even for a moment.`）；
- 因为不再带参数，调用处由 `Messages.get(SealedSwordBase.class, "bound", name())` 收敛为 `Messages.get(SealedSwordBase.class, "bound")`。

## 二、按钮双图标（`SwordSwap` 补 `primaryVisual()/secondaryVisual()`）
决斗家的交换按钮（`MeleeWeapon.Charger`）本来就是「主手大图标 + 副手小图标」两件套，照抄其实现：

| 钩子 | 内容 |
| --- | --- |
| `primaryVisual()` | 主手 `belongings.weapon` 的 `ItemSprite`；**主手空着**时退回 `new HeroIcon(this)`（交换图标）。照上游 `ico.width += 4`，重心左移给右下角的副手图标腾位 |
| `secondaryVisual()` | 副手 `belongings.secondWep` 的 `ItemSprite`，`scale = PixelScene.align(0.51f)`、`brightness(0.6f)`；副手空着时同样退回交换图标 |
| `indicatorColor()` | `0x5500BB`（与决斗家同色，未变） |

### 图标何时重建（关键）
`ActionIndicator` 只在被 `setAction()` / `refresh()` 时重建 `primaryVis/secondVis`，而图标是**重建时现读**手上的武器，
所以「手里的东西变了」必须主动报一次。新增每回合一次的比对：

```java
private Item shownMain, shownOff;          //上一次画上去的主/副手物品（不持久化）
private void refreshIfHandsChanged() {     //act() 里调用
    Item main = hero.belongings.weapon, off = hero.belongings.secondWep;
    if (main != shownMain || off != shownOff) {
        shownMain = main; shownOff = off;
        if (ActionIndicator.action == this) ActionIndicator.refresh(); // 被别人占着就别动
    }
}
```
配套的两处即时刷新：
- `swapHands()` 末尾：`ActionIndicator.refresh()`（两只手都换了）；
- `unsealInto()` 末尾：`ActionIndicator.refresh()`（主手形态换了新实例）。

另照 `Charger` 补 `fx(boolean on)`：`on` 时把 `shownMain/shownOff` 置 null 并在槽位空闲时重新 `setAction(this)`
（场景重建 / 英雄精灵刷新后按钮与图标都能回来）。

> 顺带简化：`swapHands()` 原来在末尾无条件 `ActionIndicator.setAction(swap)` 抢槽，现改为只 `refresh()`，
> 占不占位统一交给 `SwordSwap.act()` 的「空闲才占」策略，避免和狂暴/连击互相顶掉。

## 三、核验
- `SealedSwordBase.java`（含 4 个形态类）单文件 `javac` **EXIT=0**，产出 6 个 class；
- `SealedSwordBase.java` / `items_zh.properties` / `items.properties` 三文件**语法括号平衡 + UTF-8**；
- `bound` 键 zh/en 双份已更新，代码里已无第二处引用旧文案。

## 四、待验证（`build-desktop.bat`）
① 取消装备/丢弃/换武器顶替 → 提示「你并不想放下这把珍贵的剑，哪怕只有一刻。」；
② 装备封印之剑后右下角按钮 = **主手剑的大图标 + 右下角小副手图标**；主手空着一侧显示交换图标；
③ 换一把普通武器到另一只手 / 点按钮主副对调 / 解封换形态 → **按钮图标立即跟着变**，不需重启场景。

# 2026-09-14（补2）封印之剑解封形态的屏幕温度滤镜（HeatVignette）

## 需求
装备**一阶段解封之剑 / 二阶段解封之剑 / 莱瓦汀**时，屏幕分别出现**黄 / 橙 / 红**的半透明滤镜，代表温度升高；
**屏幕边缘最强、屏幕中央接近透明**；**不能遮挡游戏 UI**。

## 一、渲染层级：怎么做到「压住画面但不遮 UI」
noosa 的 `Group.draw()` **严格按 `add()` 顺序**逐成员绘制，而 `GameScene` 的 `create()` 恰好有一条天然分界线：

```
terrain → … → fog → spells → overFogEffects → statuses → healthIndicators → emoicons → cellSelector
                                  ｜  ← 滤镜插在这里（世界层最后）
                        menu → status → boss → resume → action → loot → attack → log → toolbar → inventory …
```
⇒ `HeatVignette` 紧跟在 `add(cellSelector)` 之后 `add()`，于是：**世界（地块/怪物/特效/雾）在它下面被着色，
状态栏 / 工具栏 / 动作按钮 / 日志 / 各类窗口全在它上面**。

它自带 `camera = PixelScene.uiCamera`（**不能**用默认的 `Camera.main`，否则滤镜会跟着英雄滚动），
并按 `scale = (uiCamera.width/64, uiCamera.height/64)` 把 64×64 的 quad 拉伸铺满整屏。

## 二、渐变是怎么来的（`effects/HeatVignette.java`）
- **一张全局共享的白色径向渐变贴图**：`TextureCache.create("egopd_heat_vignette", 64, 64)` → 直接往
  `texture.bitmap`（libGDX `Pixmap`，RGBA8888）里逐像素写 `A = f(半径)`、`RGB` 恒为白（**非预乘**）。
  - 归一化半径 `r = min(1, √(dx²+dy²))`（把正方形贴图拉伸到屏幕上 ⇒ 渐变变成**椭圆**，四条屏幕边缘落在最外圈）；
  - `r ≤ 0.2` 全透明，之后 smoothstep 升到 1；中心留 `CENTER_FLOOR = 0.05` 的底噪 ⇒「接近透明」而不是硬空洞。
  - 写入时机安全：`TextureCache.create` 只建对象（`id == -1`，其内部 `filter/wrap` 见未生成会跳过 GL 调用），
    真正上传发生在第一次 `bind()`，此时像素已写好；断上下文后 `TextureCache.reload()` 用同一个 Pixmap 重传。
- **三种颜色不占三张贴图**：靠 `hardlight(color)`（纯乘算，走 `uColorM`）现场染色；
  片元着色器是 `gl_FragColor = texture2D(uTex,vUV) * uColorM + uColorA`，再按
  `GL_SRC_ALPHA / GL_ONE_MINUS_SRC_ALPHA` 混合 ⇒ 得到「按渐变强度给画面染一层色」。

| 形态 | 色板 | 边缘最大不透明度 |
| --- | --- | --- |
| 封印之剑（stage 0） | — | 0（不显示） |
| 一阶段解封之剑 | `0xFFD24A` 黄 | 0.40 |
| 二阶段解封之剑 | `0xFF8A00` 橙 | 0.55 |
| 莱瓦汀 | `0xFF2A00` 红 | 0.70 |

## 三、状态从哪来：**拉取式**，零钩子
不通知、不挂 buff、不改物品。`HeatVignette.update()`（场景成员每帧被 `Group.update()` 调用）里
直接 `SealedSwordBase.swordInHands(Dungeon.hero)` 取当前**手上**那把剑的 `stage()` 作为目标热度，
再指数逼近（`FADE_TIME = 0.35s`）——所以：

- 装备 / 解封 / 主副对调 / 读档 / 换层重建场景 / 剑被移除，**全都自动正确**；
- 换形态是**渐变过渡**（黄→橙→红 连颜色带强度一起插值），不是硬切；
- 背包里的剑不算（`swordInHands` 只看 `belongings.weapon` / `secondWep`）；
- 场景重建时构造函数直接把 `heat` 对齐到目标值，**不会每次换层都闪一次淡入**。

## 四、核验
- `HeatVignette.java` + `GameScene.java` + 封印之剑系列 + `HeroClass` + `DisarmingTrap` 共 9 个文件
  单文件 `javac` **EXIT=0**（23 个 class）；`HeatVignette.java` 括号平衡 + UTF-8；
- 未代跑 Gradle。

## 五、待验证（`build-desktop.bat`）
① 装备封印之剑（stage 0）→ 无滤镜；解封到一阶段 → 屏幕四周泛黄，中央几乎看不见；二阶段 → 橙；莱瓦汀 → 红；
② 确认状态栏 / 工具栏 / 右下角动作按钮 / 日志 / 打开背包窗口**都不被染色**；
③ 点「切换主副」把剑送进副手（变回封印之剑）→ 滤镜淡出；换回主手 → 淡入；
④ 换层、读档后滤镜仍在（且不会闪淡入）。

# 2026-09-14（补3）场景特效强化：减弱滤镜 + 四周火焰 + 热浪扭曲 + 设置开关

## 需求
1. 整体**减弱**屏温滤镜浓度；
2. 屏幕**四周**添加火焰粒子；
3. 添加模拟**热浪的屏幕扭曲**；
4. 设置里加开关，使「**莱瓦汀场景特效**」可关闭。

## 一、滤镜浓度下调（`HeatVignette`）

| 参数 | 旧 | 新 | 说明 |
| --- | --- | --- | --- |
| `HEAT_ALPHA`（边缘不透明度） | 0 / 0.40 / 0.55 / 0.70 | **0 / 0.20 / 0.28 / 0.36** | 整体约减半 |
| `INNER_RADIUS`（中心留白圈） | 0.20 | **0.26** | 屏幕中央更清透 |
| `CENTER_FLOOR`（中心底噪） | 0.05 | **0.04** | — |

## 二、屏幕四周火焰（`effects/HeatFlames` + `effects/particles/HeatFlameParticle`）

- **布局**：底边 4 段最盛、左右各 2 段往内上方飘、顶边 3 段细小火星向下垂落，共 **11 条火线**；每条＝一个 `Emitter`（`camera = uiCamera`），位置用**归一化坐标**每帧换算 `uiCamera.width/height`，窗口尺寸变化自动跟随。
- **为什么在工具栏后面也看得见**：火苗升程取屏幕高的 `RISE_FRACTION = 18%`，远高于工具栏 —— 被挡住的部分自然隐没、露出的部分在栏顶之上连成火带；这同时兼容「翻转 UI」（工具栏在上时改由顶边那条被挡）。
- **密度随热度**：`intensity = (heat/3)^1.4` —— 黄只有零星几颗、橙渐旺、红连成一片；`interval = base / max(0.3, intensity)`，且只在变化超 ±15% 时才 `startDelayed` 重设。低于 `MIN_INTENSITY = 0.05` 直接熄火。
- **颜色**：新建 `FLAME_COLORS = {0xFFE070, 0xFFE070, 0xFFA828, 0xFF7A20}`（比色罩更亮饱和），`lightMode()` 加色混合才有亮芯。
- **每条线一个 `Factory`**：方向固化在匿名工厂里、颜色/速度在 `emit()` 时从外层现读 —— 用一个静态工厂配静态字段的话，几条方向不同的线会在同一帧互相覆盖。
- 挂在世界层最后（`heatVignette` **之后**、UI 之前）⇒ 火苗压在滤镜之上不被染色、又在所有 UI 之下不遮挡。

## 三、热浪扭曲（`NoosaScriptWarp`，跨 SPD-classes/core）

noosa **没有**后期处理管线，所以没有走 FBO，而是**换着色器**：

| 改动 | 内容 |
| --- | --- |
| 新建 `noosa/NoosaScriptWarp` | `extends NoosaScript`；顶点多输出 `vScreen = (uCamera*uModel*aXYZW).xy`；片元按屏幕位置算正弦抖动偏移 `vUV`（两组纵向波纹叠成横向抖动 + 一点纵向），再按「离屏幕中心越远越强」加权（`uWaveEdge = 0.7`）；光照项原样保留 |
| `NoosaScript.get()` | `enabled` 为真时返回 `NoosaScriptWarp.get()` |
| `NoosaScriptNoLighting.get()` | 同上（返回类型放宽为 `NoosaScript`）—— 地形/迷雾/水面本来传的就是恒等光照，观感不变 |
| `Game.render()` | 每帧多一句 `NoosaScriptWarp.resetCameraCache()` |
| `GameScene.draw()` | 覆写后**第一句** `HeatVignette.beginWorldPass()` 打开扭曲，再 `super.draw()` |

**「谁被扭曲」＝每帧开/关**：`GameScene.draw()` 的第一句 `HeatVignette.beginWorldPass()` 打开，`HeatVignette.draw()`（最后一个世界层元素）**第一行**关掉 ⇒ 世界层被折射，滤镜自身与其后所有 UI 保持原样；`destroy()` 再兜底关一次，免得残留状态扭到别的场景。

**⚠️ 为什么开关必须放 `draw()` 而不是 `update()`**：`Game.render()` 的顺序是 `resetCamera() → draw() → step()(update)` —— 若在 `update()` 里开关，下一帧开头那两句 `NoosaScript.get().resetCamera()` / `NoosaScriptNoLighting.get().resetCamera()` 会一并被换成扭曲脚本，**普通脚本的相机缓存就再也清不掉**（UI 层会用上过期矩阵）。放在 `draw()` 里则 `render()` 的相机重置总能落回普通脚本，同时世界层仍被完整夹住。

| 形态 | 最大像素偏移 `HEAT_WARP` |
| --- | --- |
| 封印之剑 / 一阶段 / 二阶段 / 莱瓦汀 | 0 / 0.9 / 1.4 / **2.0** px |

相位 `waveTime` 按 `2π` 回绕（`WAVE_PERIOD = 1.2s`），shader 里时间系数全取整数（1/2/3）⇒ 回绕处连续无跳变。

## 四、设置开关

| 位置 | 内容 |
| --- | --- |
| `SPDSettings` | `KEY_LAEVATEIN_FX = "laevateinn_fx"`，`laevateinnFX()` 默认 **true** |
| `WndSettings` 显示页 | 在「震屏」下方新增 `CheckBox`「莱瓦汀场景特效」 |
| 文本 | `windows.wndsettings$displaytab.laevateinn_fx`（zh：莱瓦汀场景特效 / en：Laevateinn Scene FX） |

关闭时 `HeatVignette.targetHeat()` 直接返回 0 ⇒ 滤镜、火焰、扭曲**一起平滑淡出**（0.35s），无需重启也无需任何钩子；设置开着但热度为 0 时整条链路回到原 shader，**零开销**。

## 五、核验
- 10 个改动文件 + 3 个新增文件：单文件 `javac` **EXIT=0**（79 class）；另单独编译 `NoosaScriptNoLighting` 的全部消费者（`Tilemap`/`FogOfWar`/`AlchemyScene`/`Archs`/`CustomTilemap`/`AmbitiousImpRoom`）**EXIT=0**（33 class）；
- 全部文件**括号平衡 + UTF-8**；还原 shader 字面量确认 `//\n` 分隔符唯一、两段各自括号平衡、`gl_FragColor` 唯一；
- 未代跑 Gradle。

## 六、待验证（`build-desktop.bat`）
① 滤镜明显变淡、中央更清透；② 屏幕四周出现火苗，黄稀疏→红旺盛；③ 边缘热浪扭曲、中央几乎不动，且**画面仍跟随镜头**（这是新脚本最容易踩的坑）；④ 工具栏/状态栏/日志/右下角按钮/打开的窗口**既不被染色也不被扭曲**；⑤ 设置→显示→关掉「莱瓦汀场景特效」→ 三者一起淡出，重开淡入。

## 七、调参入口
- 滤镜：`HeatVignette` 的 `HEAT_COLORS` / `HEAT_ALPHA` / `INNER_RADIUS` / `CENTER_FLOOR` / `FADE_TIME`；
- 火焰：`HeatFlames` 的 `RISE_FRACTION` / `BASE_LIFE` / `BASE_SIZE` / `SIZE_GAIN` / `MIN_INTENSITY` 与构造器里各条线的 `baseInterval`、`sizeScale`；颜色见 `HeatVignette.FLAME_COLORS`；
- 扭曲：`HeatVignette` 的 `HEAT_WARP` / `WARP_EDGE_BIAS` / `WAVE_PERIOD`，波形在 `NoosaScriptWarp.SHADER` 的片元里。

# 2026-09-14（补4）封印之剑开局挂副手 + 「仇怨」紫色粒子

## 需求
1. 初始携带的**封印之剑开局就装备在副手**（原先放在背包里）。
2. 处于复仇账簿的「**仇怨**」buff 下时，**从角色向四周散发紫色粒子**。

## 一、开局副手（`HeroClass.initMiddleFinger`）

| 位置 | 物品 |
| --- | --- |
| 主手 `belongings.weapon` | 指虎 |
| 神器槽 `belongings.artifact` | 复仇账簿 |
| **副手 `belongings.secondWep`** | **封印之剑** |

- 直接赋值 + 显式 `activate(hero)`，照主手武器/神器槽的既有写法。
- **刻意不走 `equipSecondary()`**：它会 `spendAndNext(timeToEquip)` 结算一个装备回合，
  还会触发 `Badges.validateDuelistUnlock()`（决斗家解锁徽章），都不适合开局。
- `activate` 会挂上 `SwordSwap`（右下角切换按钮）—— 开局即出现「主手指虎 + 副手封印之剑」的双图标按钮。
- **副手不影响战斗**：`Belongings.attackingWeapon()` 只看 `thrownWeapon` / `abilityWeapon` / `weapon()`（主手），
  副手只在各处 `instanceof` 类判定（HolyDecree / ParadiseLost / WaxWing / LingeringScent…）里被读到，本系列与之无关。
- 滤镜同理：`SealedSwordBase.swordInHands` 优先主手，主手指虎 → 热度 0，开局**没有**滤镜。
- 开局力量不足带来力量惩罚照旧（六阶 / 需求 22），属预期。

## 二、「仇怨」紫色粒子

新 `effects/particles/RancorParticle`（`extends PixelParticle`）：

| 项 | 值 |
| --- | --- |
| 工厂 | `RAGE`，`lightMode() = true` ⇒ 加色混合（紫光要「亮」，不是压暗画面） |
| 初速 | `speed.polar(Random.Float(PointF.PI2), Random.Float(12, 30))` ⇒ **整圈随机方向**，从角色向外辐射 |
| 浮力 | `speed.y -= Random.Float(3, 9)`（纯放射太机械，带一点上浮才像怨气在冒） |
| 寿命 | 0.7s |
| 大小 | `size(1 + 3p)`：出生 4px → 消亡 1px |
| 颜色 | `ColorMath.interpolate(0x3B0A6B, 0xCB86FF, p)`：深紫（尾）→ 亮紫（头） |

发射器挂在 `RevengeLedger.Rancor`（非静态内部类）上，**跟随角色贴图**：

```java
private transient Emitter aura;

private void ensureAura() {              // act() 每回合 / fx(true) 调用
    if (target == null || target.sprite == null) return;
    if (aura != null && aura.exists) return;
    aura = target.sprite.emitter();      // CharSprite.emitter() → GameScene.emitters 组，pos(贴图)
    if (aura != null) aura.pour(RancorParticle.RAGE, RANCOR_PARTICLE_INTERVAL);
}
private void freeAura() {                // fx(false) / detach() 调用
    if (aura != null) { aura.on = false; aura = null; } // 只停发射，在飞的粒子自然消亡后由 autoKill 自毁
}
@Override public void fx(boolean on) { if (on) ensureAura(); else freeAura(); }
```

- 间隔 `RANCOR_PARTICLE_INTERVAL = 0.1f`（约 10 颗/秒，同屏常驻 6~8 颗）。
- **重建判据是 `exists` 而不是 `parent != null`**：`Group.destroy()` 只递归 `destroy()` 子节点、
  **不清 `parent`** ⇒ 换层后旧发射器 `parent` 仍非空、但 `exists` 已是 false。
  而英雄贴图重建时 `CharSprite.link → Char.updateSpriteState()` 会把所有 buff 的 `fx(true)` 再跑一遍，
  正好走到 `ensureAura` 重建 —— 这是「buff 视觉跟随角色」的通用兜底链路。
- `detach()` 里也显式 `freeAura()`：`Buff.detach()` 只在 `target.sprite != null` 时才回调 `fx(false)`，
  读档期间（sprite 为 null）会漏掉。
- 不参与存档（`transient`，且 Bundle 只存显式 put 的字段），读档后由 `RevengeLedger.activate` 重挂 buff → `fx(true)`。
- `GameScene.create()` 里 `scene = this`（254 行）早于 `new HeroSprite()`（323 行），
  所以开局 `HeroSprite()` 构造 → `link()` → `updateSpriteState()` → `fx(true)` 时
  `GameScene.emitter()` 已经可用，粒子不会晚一回合才出现。

## 三、核验
- `RancorParticle.java`（新）/ `RevengeLedger.java` / `HeroClass.java` 单文件 `javac` **EXIT=0**（6 class）；
  三文件**括号平衡 + UTF-8**；另确认 `Emitter.on` / `Gizmo.exists` 均为 public。
- 未代跑 Gradle。

## 四、待验证（`build-desktop.bat`）
① 开局：主手指虎、副手封印之剑、右下角按钮显示两把武器（点它主副对调，剑进主手后可解封）；
② 背包里**没有**封印之剑（已装备在副手）；
③ 使用复仇账簿 → 角色四周持续飘出紫色粒子；充能归零 / 主动解除后粒子自然飘散消失；
④ 换层后粒子仍在（且不重复叠加）；⑤ 存档读档后粒子恢复。

## 五、调参入口
- 粒子外观：`RancorParticle` 的 `DEEP` / `BRIGHT` / `LIFESPAN` / `MIN_SPEED` / `MAX_SPEED`；
- 浓度：`RevengeLedger.RANCOR_PARTICLE_INTERVAL`。

# 2026-09-14（补5）中指长兄一层天赋（4 个）

## 需求
| 天赋 | +1 | +2 |
| --- | --- | --- |
| 健身一餐 | 进食后获得 **1 点临时力量**，持续 **75 回合** | 2 点 / 150 回合 |
| 仔细看好！ | 攻击命中获得 **1 层夸耀**，击杀改为 **2 层** | 命中 2 层、击杀 3 层 |
| 傲人肌肉 | 空手攻击视为拥有 **+0 武力戒指** | 视为 **+1 武力戒指** |
| 你犯规了 | **无法闪避**，但获得与闪避能力相同的**减伤**（参考磐岩附魔） | 除 +1 外，**等待**时获得**已失去体力 20%** 的护盾（最高 5 点、不可叠加、移动即解除） |

- 夸耀：最高 **8 层**，每层 **+5% 精准**；攻击**未命中**立刻清空全部层数。

## 一、天赋枚举与注册（`actors/hero/Talent.java`）

- 图标行：**talent_icons.png 第十一行**（512×176 = 32 列 × 11 行 = **352 帧**，第十一行 = 绝对索引 **320~351**）。
  T1 取前 4 格 → `FITNESS_MEAL(320)` / `WATCH_CLOSELY(321)` / `PROUD_MUSCLES(322)` / `YOU_CHEATED(323)`；
  第十一行 5~7 列（324~326，用户已绘制）留给设计稿 T2 的三个天赋。充能天赋 `icon()` 加
  `case MIDDLE_FINGER: result = 346;`（= 320+26，**待绘制**，返回空帧不会越界：352 > 346）。
- **`initClassTalents` 的三个 tier switch 都必须给 MIDDLE_FINGER 显式 case**：
  T1 填 4 个新天赋；**T2 / T3 写空 case**——否则会落进 `case WARRIOR: default:` 让中指长兄**白拿战士天赋**（铁胃/壮汉等）。
  T3 空 case 的另一层原因：目前 `HeroSubClass` 仍复用 BERSERKER/GLADIATOR 占位，
  `initSubclassTalents` 给的是狂战士/角斗士 T3，待转职设计完成后替换。
- T2 尚未实现 ⇒ `TalentsPane` 会因 `talents.get(i).isEmpty()` 跳过该层（面板只会显示一层）。

## 二、四个天赋的实现位置

| 天赋 | 汇聚点 | 改动 |
| --- | --- | --- |
| 健身一餐 | `Talent.onFoodEaten` + `Hero.STR()` | `TempStrength.apply(hero, points, 75/150)`；`STR()` 里 `strBonus += TempStrength.amountOf(this)` |
| 仔细看好！ | `Char.attack`（命中后 / 未命中）+ `Hero.attackSkill()` | 命中后 `Talent.onHeroAttackBrag(hero, enemy, !enemy.isAlive())`；未命中 `Talent.onHeroAttackMissed`；精准乘区 `accuracy *= brag.accuracyMultiplier()` |
| 傲人肌肉 | `RingOfForce.damageRoll(Hero)` | 空手时用**伪等级** `pointsInTalent-1`（+1→0、+2→1）走武力戒指的 `min/max(level, tier)` 公式；已有更高等级武力戒指时取高者 |
| 你犯规了 | `Hero.defenseSkill()` / `Hero.defenseProc()` / `Hero.act()` | ①闪避归零（照磐岩）；②`Talent.dodgeAsDamageReduction` 把命中率折算成减伤；③`resting` 时 `CheatGuard.refresh`、否则 `CheatGuard.clear` |

### 新增 buff 类（均为顶层类，参与存档反射）

| 类 | 基类 | 关键点 |
| --- | --- | --- |
| `actors/buffs/Brag`（夸耀） | `Buff` | **不覆写 `act()`**（沿用默认 `diactivate()`）⇒ 无时长、只靠命中/未命中增删；`stacks` 上限 8，`accuracyMultiplier() = 1 + 0.05×层` |
| `actors/buffs/TempStrength`（临时力量） | `FlavourBuff` | **覆盖式刷新**：`setTurns(turns)` = `spend(turns - cooldown())`（照 `AdrenalineSurge.reset`），不叠层 |
| `actors/buffs/CheatGuard`（犯规护盾） | `ShieldBuff` | **无自然衰减**（只在挨打时消耗）；`setShield()` 只抬高不降低 ⇒ 天然「不可叠加」 |

- buff 图标新帧：`BuffIndicator.BRAG=114` / `TEMP_STRENGTH=115` / `CHEAT_GUARD=116`
  （113 仇怨已占；buffs.png 7×7 有 162 帧、large_buffs.png 16×16 有 128 帧，均够用）。**三张图标待用户绘制**，绘制前显示为空帧。

### 关键实现细节

- **「你犯规了」照抄磐岩附魔的完整算法**（`Stone.proc`）：命中/闪避掷点乘区（Bless/Hex/Daze/ChampionEnemy/AscensionChallenge/FerretTuft）
  都补上，再 `hitChance = gate(0.25, (1+3×hitChance)/4, 1)`、`damage = ceil(damage × hitChance)`。
  取真实闪避值用独立开关 `Talent.cheatTestingEvasion()`（`Hero.defenseSkill` 里归零前判它），
  **不复用 `Stone.testingEvasion()`**，免得符文与天赋互相干扰。
- **护甲已带磐岩附魔时跳过换算**（`armor().hasGlyph(Stone.class, this)`）——那条路已在 `armor().proc()` 里算过同一笔，否则双重减免。
- **`Char.attack` 的两个钩子点**：命中钩子放在 `enemy.damage(effectiveDamage, this)` **之后**（才能读到「是否击杀」）；
  未命中钩子放在 else 分支 `onHeroAttackResolved` 之前。
- **`FloatingText.getHitReasonIcon` 必须加 `defRoll == 0` 早退**：闪避归零后 `defenseSkill()` 恒返 0，
  而下面 `arm.evasionFactor(...)` 那段拿它当分母 ⇒ **0/0 = NaN** 会污染命中原因（并可能破坏排序比较器的传递性）。
  那里原本只对磐岩附魔做了早退（`defRoll == 0 && arm.hasGlyph(Stone)`），本天赋照抄一条同样的早退（返 `HIT_ARM`）。
- **「等待」沿用工程的既有语义**：判定 `Hero.resting`（长按等待 / 休息），与环指大师 T1「眺望」一致；
  单击等待（`rest(false)`）不置 `resting`，因此不会给盾。
- 护盾钩子放在 `Hero.act()` 里与 `Talent.BARKSKIN` 同一处（无论走「等待」还是「行动」分支都会经过），
  非 resting 时 `clear()` ⇒ 移动/攻击/交互后立即解除。

## 三、文本

- `actors_zh.properties` / `actors.properties` 各加 4 组天赋文本（`actors.hero.talent.<枚举小写>.title/desc`）
  与 3 组 buff 文本（`actors.buffs.<类名小写>.name/desc`）。
- **arg-less 的 desc 可以写裸 `%`**（`Messages.get` 只在 `args.length > 0` 时才 `String.format`，见 Messages.java 135 行），
  但**带参 desc 的字面百分号必须写 `%%`**：夸耀 desc 传 `(层数, 加成)` 两个 int、临时力量传 `(点数, 剩余回合 String)`、
  犯规护盾传 `(当前护盾, 上限)` 两个 int。
- 临时力量如实计入装备需求（与真力量同路），文案已按此表述。

## 四、核验
- 单文件 `javac` **EXIT=0**（9 个 class：Brag / TempStrength / CheatGuard / Talent / Hero / Char / RingOfForce / BuffIndicator / FloatingText）。
- 9 个 .java 文件**括号平衡 + UTF-8**；两个 .properties **无重复键**（zh 1839 / en 1660），
  7 组新 desc 用 `java.util.Properties` + `String.format` 实跑通过（`%1$d/%2$d/%%` 渲染正确）。
- 未代跑 Gradle。

## 五、待验证（`build-desktop.bat`）
① 选角「中指长兄」升到 2 级 → 天赋面板出现**一层 4 个**新天赋（T2 不显示、不会出现战士天赋）；
② 进食 → 力量面板 +1/+2、buff 显示剩余回合，75/150 回合后消失；再吃刷新不叠层；
③ 连续命中敌人 → 夸耀层数上涨（击杀当次多 1 层），图标上显示层数；打空一次立刻清零；
④ 空手攻击伤害提升到武力戒指公式（+0 约 1~10）；装武力戒指时取较高者；
⑤ 被攻击**必定命中**但伤害明显被削（对照磐岩附魔的强度）；
⑥ 长按等待 → 获得不超过 5 点的护盾，移动或攻击后立刻消失；
⑦ 读档后夸耀层数 / 临时力量 / 护盾均正确恢复。

## 六、遗留
- 三个新 buff 图标（帧 114/115/116）与充能天赋图标（帧 346）**待绘制**。
- 中指长兄的**职业选择文案**（`actors.hero.heroclass.middle_finger*`，zh/en 一致）仍写着「暂时完整沿用战士的开局」，
  与现状（专属开局 + 一层天赋）已不符，待一并改写。
- T2 / T3 与两个转职分支未实现。

# 2026-09-14（补6）中指长兄 T1「你犯规了」+2 修正 + 图标换原版帧

## 一、问题：护盾被实现成了「护甲」

原实现把补盾放在 `Hero.act()` 里**每回合轮询**：

```java
if (hasTalent(YOU_CHEATED) && pointsInTalent(YOU_CHEATED) >= 2){
    if (resting) CheatGuard.refresh(this);
    else         CheatGuard.clear(this);
}
```

配合 `ShieldBuff.setShield()`「只抬高不降低」的语义 ⇒ **只要一直在等待，护盾每回合都被补满**。
于是挨打后下回合立刻回满，等效「等待期间常驻 5 点减伤」——手感与原版 `HoldFast`（坚守阵地，等待获得**护甲**）几乎一样，
这正是「护盾像护甲」的根因。另有第二个坑：**单击等待不走 `resting`**（`Toolbar` 单击 → `hero.rest(false)` → `resting = false`），
所以单击等待根本不给盾，只有长按休息才给。

> 澄清：护盾本身一直是真护盾（`CheatGuard extends ShieldBuff`，在 `Char.damage()` 里由
> `ShieldBuff.processDamage` 消耗，与 `Hero.drRoll()` 的护甲掷点是两条独立的路）。问题只在**补盾时机**。

## 二、修正：补盾 = 每次等待一次；解除 = 做出任何动作

| 项 | 位置 | 语义 |
| --- | --- | --- |
| 补盾 | `Hero.rest(boolean fullRest)`，与 `HOLD_FAST` / `PATIENT_STRIKE` / 余香同处 | **单击等待与长按休息都经过这里**，各补**一次**（`setShield` 只抬高 ⇒ 不可叠加） |
| 解除 | `Hero.act()` | 本回合若**执行了动作**（`curAction != null`：移动/攻击/交互/拾取/挖矿…）→ `CheatGuard.clear()` |

- `cheatActed` 必须在 `if (curAction == null) {...} else {...}` **之前取快照**：
  各 `actXxx()` 分支末尾会把 `curAction` 置回 `null`，之后判就不准了。
- **不再每回合 refresh**：护盾变成一次性资源——补上后被打掉就没了，要再挨打就得重新等待一次。
- 「等待/空闲」（`curAction == null`）时保留护盾 ⇒ 对应需求里的「**移动时**即解除」。
- 已知边界：`Toolbar` 的「等待/拾取」按钮在**脚下有物品**时走 `handle()` + `next()`（不经过 `rest()`，也不算 `curAction`），
  该分支既不补盾也不清盾——与工程里 `HOLD_FAST` 在那一处「即使没真的等待也触发」的既有约定保持不冲突。

## 三、图标：两个新帧未绘制，暂借原版同类帧

| buff | 常量（自绘后用） | 暂借帧 | 依据 |
| --- | --- | --- | --- |
| 夸耀 | `BRAG = 114` | —（**用户已绘制**，小图/大图均已画） | — |
| 临时力量 | `TEMP_STRENGTH = 115` | **`UPGRADE` = 帧 50** | 原版「增强」箭头，被 `AdrenalineSurge` / `PhysicalEmpower` / `EnhancedRings` / `Adrenaline` 共用，语义最接近「临时提升属性」 |
| 犯规护盾 | `CHEAT_GUARD = 116` | **`ARMOR` = 帧 20** | 原版护盾通用图标：`Barrier` / `TearShield` / 格挡 `Blocking` / `HoldFast` **全部**用它（`icon()` 里写的就是 `BuffIndicator.ARMOR`） |

- 两个常量值（115/116）**保留**作为自绘后的落点，注释已写明「改回本常量即可」。
- 各自保留 `tintIcon` 着色以便与同帧原版 buff 区分：临时力量暖橙 `(1, 0.45, 0.2)`、犯规护盾淡紫 `(0.85, 0.5, 1.1)`。
- 图集容量核对：`buffs.png` 128×64、cell 7 ⇒ **18 列 × 9 行 = 162 帧**；`large_buffs.png` 256×128、cell 16 ⇒ **16 列 × 8 行 = 128 帧**。
  帧 20 与帧 50 在大图里分别是 (4,1) 与 (2,3)，均在 128 帧范围内。

## 四、核验
- 单文件 `javac` **EXIT=0**（CheatGuard / TempStrength / Brag / Hero / BuffIndicator）。
- Pillow 复核帧占用：小图 114 = 49 alpha 像素（已画）、115/116 = 0（空）；大图 114 = 252（已画）、115/116 = 0（空）。
- 未代跑 Gradle。

## 五、待验证（`build-desktop.bat`）
① 单击等待 → 立刻出现护盾；② 长按休息时护盾**不再每回合回满**，挨打掉一点就少一点；
③ 移动/攻击后护盾消失；④ 护盾图标显示为原版灰色盾牌（淡紫着色）、临时力量显示为橙色上箭头。

# 2026-09-15（补7）中指长兄二层天赋（5 个）

设计稿：记录一餐 / 纹身铭刻 / 趁手玩具 / 永不遗忘 / 会很烫的！ —— 图标帧 324~328，用户已全部绘制。

## 一、总览

| 天赋 | 图标 | 效果 | 唯一汇聚点 | 实现方式 |
| --- | --- | --- | --- | --- |
| **记录一餐** | 324 | +1：进食只花 1 回合 + 账簿 8% 充能；+2：15% | `Food.eatingTime()` / `HornOfPlenty.doEatEffect()`（耗时）＋ `Talent.onFoodEaten()`（充能） | 耗时分支加 `RECORD_MEAL`；充能走新建 `RevengeLedger.chargeByTalent(hero, pct)` |
| **纹身铭刻** | 325 | +1：消耗奥术刻笔换 25% 充能；+2：50% | `RevengeLedger` 新增动作 `AC_TATTOO` | 动作只在「中指长兄 + 有此天赋 + 账簿已装备」时出现在账簿菜单；`execute` 里把刻笔 `detach` 掉再 `gainCharge` |
| **趁手玩具** | 326 | 主副切换默认 1 回合；有天赋且不在冷却时不消耗回合（+1 冷却 20 回合 / +2 10 回合） | `SealedSwordBase.swapHands(Hero)` | 末尾 `hero.spendAndNext(Talent.handyToySwapCost(hero))`；冷却用新建 `Talent.HandyToyCooldown`（FlavourBuff） |
| **永不遗忘** | 327 | +1：攻击命中中指长兄的敌人获得「报复对象」；+2：命中英雄或任意友方单位都算 | `Char.attack` 命中分支 → `Talent.onEnemyAttackLanded()` | 给攻击者 `Buff.affect(RevengeTarget)`（无时限、只打一次） |
| **会很烫的！** | 328 | +1：血量 < 25% 不受到燃烧伤害；+2：< 50% | `Hero.damage(int, Object)` 开头 | `src instanceof Burning && Talent.tooHotImmune(this)` → 直接 `return` |

**「报复对象」**（新顶层 buff `actors/buffs/RevengeTarget.java`）：受到来自中指长兄的伤害 **+20%**（`DAMAGE_BONUS = 1.2f`），无时限。

## 二、逐项要点

### 1. 记录一餐
- **耗时**：`Food.eatingTime()` 早已是「命中任一餐类天赋就 `TIME_TO_EAT - 2`（= 1 回合）」的写法，加一条 `RECORD_MEAL` 即可；**号角（`HornOfPlenty`）是独立的第二处判定**，必须同步加（这条规律见 AGENTS §6「新增/适配餐类天赋」行）。
- **充能**：`Talent.onFoodEaten()` 里新增分支 → `RevengeLedger.chargeByTalent(hero, 15/8)`。未装备或账簿被诅咒时静默跳过（与受伤/击杀充能一致，不给空提示）。

### 2. 纹身铭刻
- 动作挂在**账簿**上（不是刻笔上）：效果属于账簿，放这里既不碰原版 `Stylus` 的任何行为，又能在玩家使用账簿的地方被看见。
- 刻笔可能躺在**卷轴袋**里（`ScrollHolder` 接受刻笔），所以先用 `belongings.getItem(Stylus.class)` 找、再 `detach(backpack)` —— `Bag.iterator()`/`Item.detachAll` 会递归进嵌套包。
- **占 1 回合**（`hero.spendAndNext(1f)`）：需求没写耗时，按游戏里所有物品动作的惯例补上；若希望它是瞬发，改这一行即可。
- 顺带 `Catalog.countUse(Stylus.class)`，与 `Stylus.inscribe` 的统计口径一致。

### 3. 趁手玩具（含一次行为变更）
- **行为变更**：`swapHands()` **此前完全不消耗回合**（照决斗家交换按钮的写法），现改为默认 **1 回合**。需求里的「更换武器只消耗 1 回合」在原版本来就是默认值（`EquipableItem.timeToEquip` 返回 1f，全工程唯一消费点是 `hero.spendAndNext`）——所以那句话对应的代码无需改动，天赋的真正收益落在「切换主副免费」上。
- 回合结算放在 `swapHands` **最末尾**：对调、快捷栏/按钮图标重建、音效都做完才收走回合。两个入口（菜单 `AC_SWAP`／右下角按钮 `SwordSwap.doAction`）都经过它，各结算一次不会重复；`ActionIndicator.onClick → action.doAction()` 那两层**不会**替你扣回合。
- 冷却用 `HandyToyCooldown`，`Buff.affect(hero, ..., 19f / 9f)`（减 1 是本回合自身也占一格，照 `SwiftEquipCooldown` 的 19f 先例）。带 `BuffIndicator.TIME` 图标 + `iconFadePercent()`，玩家能看见「还差几回合」；触发时额外 `GLog` 一句 `free_swap`。
- 「解封」（`AC_UNSEAL`）本来就占 1 回合，本次不动。

### 4. 永不遗忘
- **挂点**：`Char.attack` 的命中分支，紧跟 T1「仔细看好！」的 `onHeroAttackBrag`——同样必须在 `enemy.damage()` 之后才代表「这一下真的打中了」；未命中走 else 分支不计入。
- 过滤链（廉价短路在前）：`Dungeon.hero.heroClass == MIDDLE_FINGER` → 有天赋 → `attacker.alignment == ENEMY` → 受害者是英雄（+2 时含 `Alignment.ALLY` 的友方，如幽灵/召唤物）→ 尚未被标记。
- 标记**无时限**（`RevengeTarget` 不覆写 `act()`，基类 `diactivate()` 把 `time` 顶到 `Float.MAX_VALUE`），同一个敌人只打一次，所以没有刷新/叠加问题。
- **反馈**：`RevengeTarget.announced = true`，`Char.add()` 会自动在敌人头顶弹一次名字，**不要再自己 `showStatus`**（会弹两遍）。
- **加成落点**：`Char.damage(int, Object)` 里 `if (buff(RevengeTarget.class) != null && Talent.isHeroDealtDamage(src, this)) damage *= 1.2f`。判定收敛在 `Talent.isHeroDealtDamage`：
  - `src instanceof Hero` → 近战、投掷武器、一切写 `enemy.damage(dmg, hero)` 的技能；
  - `src instanceof Wand` → **法杖的 src 是法杖实例而不是英雄**（每个法杖都写 `ch.damage(damageRoll(), this)`），所以额外认一类；安全，因为游戏里只有英雄会真正用法杖打出伤害；
  - **不算**间接伤害（点燃地面后的 `Burning` 烫伤、中毒、流血，src 是那些 buff 自己）。

### 5. 会很烫的！
- 放在 `Hero.damage` **最开头**（试炼场判定之后、`interrupt()` 之前）直接 `return`：不给伤害、不闪红、**也不打断行动**——残血时被一整场火每回合打断会非常难受。
- 只是「烧不动你」：烫伤层数、点燃地面、烧背包里的卷轴仍由 `Burning.act()` 照常走完。
- 阈值判定 `hero.HP * 100 < hero.HT * (25|50)`（严格小于）。

## 三、图标 / 文案索引

| 项 | 索引 | 状态 |
| --- | --- | --- |
| 记录一餐 / 纹身铭刻 / 趁手玩具 / 永不遗忘 / 会很烫的！ | talent_icons.png 第十一行 5~9 列 = 324~328 | 用户已绘制（Pillow 复核 320~328 满帧、346 已绘制） |
| 充能天赋（HEROIC_ENERGY） | 346 | 已绘制（本类 T1 时已接线） |
| `RevengeTarget` | 无图标（`BuffIndicator.NONE`） | 挂在敌人身上，buff 条只画英雄 buff ⇒ 没有展示位，靠 `announced` 弹字 |

文案（zh/en 各一份）：`actors.hero.talent.{record_meal,tattoo_engraving,handy_toy,never_forget,too_hot}.title/desc`、`actors.buffs.revengetarget.name/desc`、`items.artifacts.revengeledger.{ac_tattoo,no_stylus,tattoo_used}`、`items.weapon.melee.sealedswordbase.free_swap`。

## 四、核验
- 单文件 `javac` **EXIT=0**（RevengeTarget / RevengeLedger / Talent / Char / Hero / Food / HornOfPlenty / SealedSwordBase）。
- UTF-8 逐文件校验通过；大括号配平。
- Properties 校验：4 个文件均 0 重复键（actors_zh 1851 / actors 1672 / items_zh 2320 / items 2138）；新增键按真实参数类型跑 `String.format` 全通过（天赋 desc 无参 ⇒ 裸 `%` 安全；`revengetarget.desc` 1 个 int、`tattoo_used` 2 个 int ⇒ 字面百分号已写 `%%`）。
- 未代跑 Gradle。

## 五、待游戏内验证
① 进食（普通食物与号角各一次）耗时 1 回合且账簿涨充能；② 账簿菜单出现「刻入纹身」，无刻笔时给提示、有刻笔时消耗一支并涨 25%/50%（+2 为 50%）；③ 主副切换默认收 1 回合，天赋冷却外免费且 buff 条出现倒计时图标；④ 被怪打中后怪头顶弹「报复对象」，之后受中指长兄的近战/法杖伤害 +20%；⑤ 残血（<25%/<50%）站在火里掉血为 0 且行动不被打断。

# 2026-09-15（补8）复仇账簿等级/上限 + 莱瓦汀系列面板与火焰 + 形态门槛

设计稿：①账簿等级跟随英雄等级 ÷2（最高 10 级），每级充能上限 +10%（100% → 200%）；
②封印之剑无特殊效果、一阶段 +10% 火伤、二阶段 +30% 火伤并点燃目标、莱瓦汀面板改 7+L~40+8L 并 +50% 火伤 + 每回合点燃自身与 5×5 圆形地块；③解封门槛 = 账簿充能 > 25/50/75% **或** 当前生命 < 75/50/25%。

## 一、总览

### 1. 复仇账簿：等级与充能上限（`items/artifacts/RevengeLedger.java`）

| 项 | 值 | 入口 |
| --- | --- | --- |
| 账簿等级 | 英雄等级 ÷ 2（向下取整），封顶 **10** 级 | `levelForHero(Hero)` |
| 充能上限 | `100 + 10 × 等级`（%），满级 **200%** | `chargeCapFor(int level)` |
| 上限刷新时机 | 装备时（`activate`）、每回合（`ledgerRecharge.act`）、充能前（`gainCharge`） | `syncChargeCap()` |
| 状态条 | 恒按**充能点数**显示百分比（0%~200%） | 覆写 `status()` |

**没有覆写 `level()`**：`Artifact.visiblyUpgraded()` 里是 `level()*10/levelCap`，而神器 `levelCap` 默认 0 —— 若让 `level()` 返回非 0，这个除法直接变 `Infinity`（`Math.round` 给 `Integer.MAX_VALUE`），格子上的等级角标会炸掉。所以「账簿等级」是一个**独立的只读方法** `ledgerLevel()`，只在 `info()` 末尾加一行 `level_info`。

**充能刻度**：充能点数就是百分比点数（100 点 = 基础满格，满级可攒 200 点）。天赋加成（8%/15%/25%/50%）与受伤/击杀充能全部按点数照旧，所以**上限变大 = 能存更多仇怨**，而非把已有数值稀释。

**读档防裁充能**：`Artifact.restoreFromBundle` 里有 `if (chargeCap > 0) charge = Math.min(chargeCap, charge)`。上限现在随英雄等级变化，反序列化时字段还是初始值 ⇒ 高充能会被裁掉。做法：实例初始化与 `restoreFromBundle` 里**先把 `chargeCap` 设成 `CHARGE_CAP_MAX`**，再由 `syncChargeCap()` 修正（英雄可能还没重建好，那就不动，等 `activate()` 再对齐）。

### 2. 莱瓦汀系列：面板与火焰（`items/weapon/melee/*`）

| 形态 | 面板 | 火焰伤害 | 其他 |
| --- | --- | --- | --- |
| 封印之剑 | 6+L ~ 35+7L（不变） | 无 | 无特殊效果 |
| 一阶段解封之剑 | 6+L ~ 35+7L（不变） | 本次伤害 **10%** | — |
| 二阶段解封之剑 | 6+L ~ 35+7L（不变） | 本次伤害 **30%** | 命中**点燃目标** |
| 莱瓦汀 | **7+L ~ 40+8L** | 本次伤害 **50%** | 主手时**每回合**点燃自身 + 5×5 圆形地块 |

- **面板**：在 `Laevateinn` 里覆写 `min(int lvl)/max(int lvl)`，**不动 `tier`**（`tier` 还牵动力量需求公式、`value()` 定价、"六阶"标签与决斗家天赋比较；本系列力量需求已由 `STRReq(lvl)` 固定为 22 = 七阶曲线）。
- **附加伤害的唯一钩子**：覆写 `SealedSwordBase.proc(Char, Char, int)`（`Weapon.proc`）。调用点在 `Hero.attackProc` 内、主伤害 `enemy.damage()` **之前**，与「焦炭松脂」的附加伤害顺序完全一致。rider 的 `src` 用**武器本身**（照 `ResinCoatingBuff.bonusDamage`），因此不会被中指长兄 T2「报复对象」二次加成。
- **命中特效**：`defender.sprite.emitter().burst(FlameParticle.FACTORY, 4)`（与 `CharcoalResinBuff.onBonusDamageFX` 同款）。
- **每回合火场**：`Laevateinn.burnAura(Hero)`，由 `SealedSwordBase.SwordSwap.act()` 每回合驱动（该 buff 只要剑在手上就一定存在、每回合必然 act，天然当心跳）。
  - 自身：`Buff.affect(hero, Burning.class).reignite(hero)`（`reignite` 只刷新时长、不重挂，不会每回合弹状态名）。
  - 地块：5×5 外框内取圆形（`dx²+dy² ≤ 4`，13 格），跳过墙壁与水面。
  - **必须判 `Blob.volumeAt(cell, Fire.class) == 0` 才 `Blob.seed`**：`Blob.seed` 是 `cur[cell] += amount` 累加体积，每回合无脑撒火会让火势滚雪球、几回合烧穿整层（照 `Burning.act()` 点燃地面的写法）。

### 3. 形态门槛（`SealedSwordBase.canReachStage`）

| 目标形态 | 账簿充能 | 或 当前生命 |
| --- | --- | --- |
| 一阶段解封之剑 | > 25% | < 75% |
| 二阶段解封之剑 | > 50% | < 50% |
| 莱瓦汀 | > 75% | < 25% |

- 判定写在 **`unsealInto(...)` 开头**（所有"变成新形态"的唯一汇聚点），不满足就 `GLog.w` 并 `return false`，任何调用方都绕不过去。
- 充能按**点数**比（`RevengeLedger.chargePoints() > 25/50/75`），与"上限最高 200%"同一把尺子 ⇒ 账簿越强门槛越松。
- **门槛只作用于解封链，不作用于主副切换**：副手形态恒为封印之剑（stage 0），"切换"本身永远产生不了更高的形态，所以没有任何可拦之处；开局把剑换到主手后逐级解封才是唯一路径。
- 账簿**未装备 / 被诅咒**时读不到充能，此时只剩"残血"那条路（与 `chargeByTalent` 的静默跳过口径一致）。

## 二、文案 / 键索引

- `items.artifacts.revengeledger.level_info`（新增，2 参：等级 / 上限；含字面 `%` ⇒ 写 `%%`）
- `items.artifacts.revengeledger.desc`（改写：补等级与上限说明）
- `items.weapon.melee.{sealedsword,unsealedsword,unsealedsword2,laevateinn}.stats_desc`（补特殊效果与门槛；**无参** ⇒ 字面 `%` 必须写**裸 `%`**，写 `%%` 会原样显示）
- `items.weapon.melee.sealedswordbase.need_charge_or_hp`（新增，2 参：充能门槛 / 残血门槛）

## 三、核验

- 单文件 `javac` **EXIT=0**（RevengeLedger / SealedSwordBase / SealedSword / UnsealedSword / UnsealedSword2 / Laevateinn，另带 Talent + RevengeTarget 源码排除旧 class 干扰）。
- UTF-8 逐文件校验通过、大括号配平（137/149/4/4/5/27）。
- Properties：`items_zh` 2322 键 / `items` 2140 键，**0 重复键**；7 个目标键按真实参数类型跑 `String.format` **全部通过**（`level_info`/`need_charge_or_hp` 各 2 个 int）。
- 未代跑 Gradle。

## 四、待游戏内验证

① 账簿等级随英雄等级长（升到 4 级 → 账簿 2 级、上限 120%），状态条显示 `0%`~`200%` 区间、不出现 `x/y`；
② 读档后高充能（>100）不被裁掉；
③ 解封三档分别被门槛拦住（给提示），用残血或攒充能后可通过；
④ 一阶段/二阶段/莱瓦汀命中时目标身上炸火星、伤害按 10%/30%/50% 追加；二阶段目标着火；
⑤ 莱瓦汀在主手时每回合自身着火 + 周围 13 格铺火，且火势**不滚雪球**（连续多回合体积不暴涨）；换到副手后火场停止；
⑥ Laevateinn 面板显示 7+L ~ 40+8L。

### 修复：绝望形态协同突刺击杀 boss 后楼层不解锁（2026-09-15）

- **现象**：泪锋的加护「绝望」形态下，若近战挥击当场把 boss 打死，随后泪剑的协同突刺会对**尸体**再调一次
  `damage()`；boss 覆写（如 `Goo.damage()` 首行「`BossHealthBar` 未挂载 ⇒ `assignBoss()` + `Dungeon.level.seal()`」）
  于是把刚解锁的楼层重新锁上，boss 层出口打不开。
- **根因**：`TearSword.strikeAt` 由 `Hero.onAttackComplete` 在 `attack()` **之后**调用（设计如此：让命中判定/附魔/Tracker
  都跑在主伤害上），主伤害可能已经击杀目标。原代码注释写「`Char.damage` 自带 `!isAlive()` 兜底」——这是**误判**：
  那条 `if (!isAlive()) return;` 在 `Char.damage()` 基类里，boss 覆写 `damage()` 时首行就 assignBoss/seal，
  `super.damage()` 的守卫来得太晚。
- **修复**（`actors/buffs/TearSword.java` `strikeAt`）：伤害结算加存活前提
  `if (dmg > 0 && target.isAlive()) target.damage( dmg, new MagicStrike() );`。
  **动画（① 突刺表现）与减层计数（③ `countDecay`）保持无条件执行**——前者只是视觉，后者语义是「剑刺出去了就算一次」，
  且修复前这两种情形本来也会执行，因此**非 boss 情形零行为变化**。
- **同类审计**：自定义代码中另有多处 `.damage(` 的两个文件——`SealedSwordBase.proc` 的附带火焰伤害本就带
  `!defender.isAlive()` 守卫且结算在主伤害之前（无风险）；`DimDusk`（薄暝）早已按此修复。本次为最后一处遗漏。
- **核验**：`TearSword.java` 单文件 `javac -nowarn -proc:none -encoding UTF-8` **EXIT=0**；文件 UTF-8 可解码（无 GBK 污染）。
- **经验已固化**：`AGENTS.md` 第 6 节「多段伤害击杀 BOSS 后出口不解锁」条目改写为
  「判定必须在**调用方**：打之前判一次 + 每段之后各判一次；`Char.damage()` 的守卫拦不住覆写」，并记录全工程覆写清单。

## 六、神器售卖设置：泪锋的加护 / 爱慕（2026-09-15）

- **结论**：`TearSwordBlessing` 此前**不可出售**，根因是它在初始化块里设了 `unique = true`，撞上
  `Shopkeeper.canSell` 的第一道判定 `if (item.unique && !item.stackable) return false;`。
  `Admiration` 未设 `unique`，本就默认可售（全工程 6 个 `unique = true` 的神器＝专属：神偷袖章 / 圣典 /
  奥丁之眼 / 复仇账簿 / 指令终端 / 泪锋的加护，前五个恰是本作认定的「专属神器」）。
- **为什么不能直接把 `unique` 删掉**：本作中 `unique` 是**一标三用**——
  ① `Shopkeeper.canSell` → 禁止出售；② `ScrollOfTransmutation.usableOnItem` 的 `return !item.unique;`
  （注释原文 "all non-unique artifacts (no holy tome or cloak of shadows, basically)"）→ 禁止嬗变；
  ③ `Thief.steal` / `CrystalMimic.steal` 的 `!toSteal.unique && toSteal.level() < 1` → 禁止被偷/被吃。
  直接去掉标记，泪锋加护就会变成可被嬗变卷轴**永久重掷**、可被水晶拟态**永久吃掉**的普通物品。
- **改法（只放开「出售」这一个维度）**：
  - `items/Item.java`：新增可覆写谓词 `public boolean sellable() { return !unique || stackable; }`
    ——默认实现与原判定完全等价，因此全工程行为零变化。
  - `actors/mobs/npcs/Shopkeeper.java`：`canSell` 第 2 行改为 `if (!item.sellable()) return false;`。
    （`canSell` 另被 `Alchemize` 的炼金窗口复用，故炼金窗口的出售入口一并放开。）
  - `items/artifacts/TearSwordBlessing.java`：覆写 `sellable()` 返回 `true`（**保留** `unique = true`）。
  - `items/artifacts/Admiration.java`：同样显式覆写返回 `true`（本就可售，把语义固定在类上防回归）。
- **仍不可绕过的通则**：`canSell` 的其余三道照旧——`value() <= 0`、已封印护甲、**已装备且被诅咒**。
  爱慕初始 `cursed = true`，故**须先用祛除诅咒卷轴解咒并卸下**才能出售（与原版诅咒装备一致）。
- **售价**：走 `Artifact.value()`（`100 + 20*visiblyUpgraded()`，诅咒减半），商店收购价＝
  `value × 5 × (深度/5 + 1)`，即浅层未强化约 1000 金。
- **核验**：4 个文件一次性 `javac -nowarn -proc:none -encoding UTF-8` **EXIT=0**；字节码核验
  `Shopkeeper.canSell` 已 `invokevirtual Item.sellable:()Z`（不再直读 `Item.unique` 字段），
  两个神器类均含 `public boolean sellable()`。未代跑 Gradle。

---

# 2026-09-15（补11）修复：松脂涂层的附加伤害击杀未计入「英雄击杀」

承接（补10）审计结论：`ResinCoatingBuff.bonusDamage` 用 buff 自身当 src，落在 `Mob.die()` 英雄击杀
白名单之外 ⇒ 松脂打出最后一击时静默丢失账簿充能与一串击杀类效果。本次修复。

## 一、改动（2 处，均在 `actors/mobs/Mob.java`）

- 新增 `import ...actors.buffs.ResinCoatingBuff;`（按字母序插在 `Preparation` 与 `Sleep` 之间，第 50 行）。
- `Mob.die()` 第 909 行的英雄击杀白名单追加一条：

  ```java
  || cause instanceof ResinCoatingBuff
  ```

  注释写明「必须保留 buff 作源，才能让元素免疫/抗性判定生效」。

## 二、为什么是加白名单而不是换 src

- `ResinCoatingBuff.bonusDamage` 里 `defender.damage(bonus, this)` 的 src 必须是**涂层 buff 实例**，
  `Char.damage` 才拿得到它去走 `isImmune(火焰/电击)` 判定；换成武器实例会丢掉元素语义
  （金色松脂是电击，不是火焰）。
- 与 DimDusk（薄暝）/ TearSword（泪剑）同款做法：自定义投送点用**自带标记类型**，再到白名单登记。

## 三、边界确认（不擅自扩大原版口径）

| 投送点 | src 类型 | 是否计入英雄击杀 |
|---|---|---|
| 松脂涂层附加元素伤害 | `ResinCoatingBuff` 实例 | **本次补上** |
| `Blazing.proc` / `Shocking.proc`（涂层临时附魔） | `Weapon.Enchantment` 子类实例 | ✅ 原本就在名单 |
| 封印之剑系列火焰伤害 | 武器自身（`Weapon`） | ✅ 原本就在名单 |
| 法杖 `WandOfXxx`（`Wand extends Item`） | 法杖实例 | ❌ 原版就不计入 —— **不动** |
| 炸弹 / 卷轴 / 神器（`CapeOfThorns` 等） | 物品实例 | ❌ 原版就不计入 —— **不动** |
| `HolyWeapon.INSTANCE`（牧师圣言武器附加伤害） | `ClericSpell` 实例 | ❌ 原版就不计入 —— **不动** |

## 四、核验

- 单文件 `javac -nowarn -proc:none -encoding UTF-8`（`Mob` + `ResinCoatingBuff`）**EXIT=0**。
- `Grep` 回读确认两条改动落地：`Mob.java:50`（import）、`Mob.java:919`（白名单）。
- 全工程扫描 `damage(..., this)` / `damage(..., XXX.INSTANCE)`（`actors/buffs`、`items/weapon/enchantments`、
  `actors/hero/abilities`、`actors/hero/spells`、`items/` 全层）确认**无第二处同类缺口**。
- 未代跑 Gradle。

## 五、待游戏内验证

涂好焦炭/黄金松脂后，用**附加伤害**打出对怪物的最后一击：① 复仇账簿按（怪 HT ÷ 英雄 HT × 10%）涨充能；
② 指令任务击杀计数 +1；③ 环指大师素材掉落判定生效。

## 七、爱慕 / 泪锋的加护：可嬗变 + 泪锋诅咒惩罚（2026-09-15）

### 1. 两件神器放开「被嬗变」，并确认已在嬗变池内

- 新增可覆写谓词 `Item.transmutable()`（默认 `!unique`）；`ScrollOfTransmutation.usableOnItem` 的神器分支
  由 `return !item.unique;` 改为 `return item.transmutable();`。
  - `TearSwordBlessing`（`unique` 仍为 true）与 `Admiration` 各覆写返回 `true` ⇒ 都能被嬗变卷轴重掷；
  - 神偷袖章 / 圣典 / 奥丁之眼 / 复仇账簿 / 指令终端等专属物仍返回 false（`unique` 未动）。
- **「加入嬗变池」核查结论：本就在池内**。`Generator.Category.ARTIFACT.classes` 早已同时包含
  `Admiration`（索引 0）与 `TearSwordBlessing`（索引 12），权重都是 `1`；`ScrollOfTransmutation.changeArtifact`
  取的正是 `Generator.randomArtifact()`（同一个 ARTIFACT 池），所以两件神器既能当嬗变**对象**、也能当嬗变**产物**。
  权重 1 在 `randomArtifact()` 里会被 `probs[i]--` ⇒ **单局最多出现一件**，与原版神器一致。
- 被嬗变掉不留残骸：泪剑层数与泪盾由 `TearSwordBlessing.BlessingBuff.detach()`（卸下即触发）统一收回；
  在背包里被嬗变则本来就没有该神器挂着的 buff。

### 2. 爱慕被嬗变 → 溶解之爱进入反叛倒计时

- 新增 `Item.onTransmuted(Hero)` 钩子（默认空实现），唯一调用点＝`ScrollOfTransmutation.onItemSelected`
  里 `if (result != item)` 的**第一行**（已确认替换、尚未执行卸下/移除）。
- `Admiration` 覆写它 → 调用新抽出的私有方法 `startBetrayal(hero)`（原 `doUnequip` 里那段逻辑抽出，
  两个入口共用）：溶解之爱 `beginAnxious()` + 10 回合 `BetrayalTimer`。
- **去重**：`startBetrayal` 在「已在倒计时中」时只把 `left` 刷回 10、不再重复 `GLog`——这样嬗变路径
  「回调 → doUnequip」连调两次也只提示一次（装备中被诅咒的爱慕：回调触发于 `item.cursed = false` 之前，负责起头）。
- `onTransmuted` **刻意不看 `cursed`**：诅咒只是「摘不下来」，嬗变是直接把神器销毁；
  `doUnequip` 里的调用仍保留 `!cursed` 门（被诅咒时父类本来也拒绝卸下）。
- 正常卸下的行为不变；重新装备时 `activate()` 取消倒计时的逻辑也未动。

### 3. 泪锋的加护：诅咒时护甲最高防御减半

- `Hero.drRoll()` 里在**加护形态「抬下限」之前**插入一段（开关＝`TearSword.hasCursedArmorPenalty(this)`）：
  `armMax = TearSword.clampReducedMax( armMin, armMax, (armMax + 1) / 2 )` ⇒ 上端减半（向下取整），
  复用绝望形态削上端那套「差值不足就取平」的钳制，**绝不把下端一并拉低**，区间恒合法。
  例：`0~5 ⇒ 0~2`；`3~8 ⇒ 3~4`；`6~8 ⇒ 6~6`（上端不越过下端）。
- 与形态、泪剑层数无关：只要神器还装备在身上且被诅咒就生效（`TearSword.artifact()` 查 artifact / misc 两个槽）。
- 落点在**掷值**而非护甲物品的显示值——与绝望形态「武器上限 −(3x+L)」同一套做法
  （`KindOfWeapon.damageRoll`），护甲窗口里显示的仍是原始 X~Y。
- 文案（zh/en 双份）：`items.artifacts.tearswordblessing.desc_cursed` 补「使护甲的最高防御_减半_」；
  主 `desc` 末尾补「_诅咒_时，泪锋不再响应你，并令你护甲的最高防御减半。」
- 与「加护形态抬下限」**不互斥**：被诅咒时若泪剑仍在场（先唤剑后中诅咒），抬下限仍按截断语义在砍半后的
  区间内生效——本次只按需求做加法，未擅自让诅咒额外禁用抬下限。

### 4. 核验

- 6 个改动文件 `javac -encoding UTF-8` **EXIT=0**；`javap -c` 确认链路：
  `ScrollOfTransmutation.onItemSelected → Item.onTransmuted(Hero)`、
  `Hero.drRoll → TearSword.hasCursedArmorPenalty(Char) → clampReducedMax(III) → armorMinDrBonus`，
  两个神器类均含 `transmutable()`，`Admiration` 含 `onTransmuted(Hero)` / `startBetrayal(Hero)`。
- **单文件 javac 的新坑**：`core/build/classes` 是上次打包的旧产物，今天新增的类
  （`RevengeTarget`、`Talent.tooHotImmune` 等）不在其中 ⇒ `Hero.java` / `Talent.java` 单文件编译会报
  「找不到符号」。加 `-sourcepath core/src/main/java` 让 javac 顺带编译缺失的新类即可（输出目录用项目内路径最稳）。

## 八、泪锋的加护去 unique + 「神器充能」交互核查（2026-09-15）

### 1. 泪锋的加护：去掉 `unique`

- `items/artifacts/TearSwordBlessing.java`：初始化块删除 `unique = true;`（保留 `bones = false;`）。
  它不再是专属神器，完全按原版普通物品的待遇：

| `unique` 的消费者 | 去除后的变化 |
|---|---|
| `Shopkeeper.canSell` | **无变化**（`sellable()` 早已覆写为 true） |
| `ScrollOfTransmutation.usableOnItem` | **无变化**（`transmutable()` 早已覆写为 true） |
| `Thief.steal` / `CrystalMimic.steal` | **新风险**：两者只要求 `!unique && level() < 1`，取物走 `randomUnequipped()`（唯一门槛是 `LostInventory`），神器初始 0 级 ⇒ **崭新泪锋可被偷 / 被拟态吃掉**；升过 1 级后天然免疫 |
| `Burning` / `Frost` / `Heap` / `Bag.resurrect` / `Hero.important` / `DriedRose` 幽灵窗口 | **无影响**：前两者只吃 `Scroll`/`Potion`；`Heap` 显式跳过 `EquipableItem`；`Bag.resurrect()` 全工程**无人调用**（死代码）；`Hero.important` 只对 `Scroll`/`Potion` 生效；枯玫瑰窗口只收 `MeleeWeapon`/`Armor` |

- 两个谓词 `sellable()` / `transmutable()` **保留**：现在与父类默认实现等价，留着用于把语义钉在类上
  （防止日后有人为别的理由补回 `unique` 而误关商店 / 嬗变的口子）。类注释与两处 javadoc 已同步改写。
- 顺带核查：`RevengeLedger` 仍带 `unique = true`（且 `value()` 返回 0）⇒ 不可售、不可嬗变、不可被偷，
  与泪锋 / 爱慕的待遇不同。

### 2. 「神器充能」`ArtifactRecharge` 的机制

- 触发来源：`ScrollOfMysticalEnergy`（`set(30)`）、`WildEnergy`（`chargeArtifacts(4f)` + `extend(8)`）、
  武僧 `MonkEnergy.meditate`（`extend(8)`）、天赋 `MYSTICAL_CHARGE`（魔杖攻击 0.5/1/1.5 回合）、
  `Pasty`(+2f)、`CloakScrap`(+4f)、`MagesStaff` + 决斗家「武器充能」，另有 `MnemonicPrayer` 的延长。
- 每回合：遍历 `hero.buffs()` 里的 `Artifact.ArtifactBuff` → 跳过 `isCursed()`（= `cursed && 无魔法免疫`）
  → `charge(hero, min(1, left))` → `Artifact.charge(Hero, float)`。
  **父类实现是空方法** ⇒ 不覆写 `charge()` 就等于这个 buff 对那件神器 0 效果。
- `amount` 的单位 ≈「1 点/回合」，各神器在 `charge()` 里自乘系数
  （原版：`TalismanOfForesight`/`SandalsOfNature` 2、`CapeOfThorns`/`DriedRose` 4、
  `CloakOfShadows`/`HolyTome`/`TimekeepersHourglass`/`LloydsBeacon`/`OdinsEye` 0.25、
  `EtherealChains` 0.5、`SkeletonKey` 0.133、`MasterThievesArmband`/`UnstableSpellbook` 0.1）。
- `RingOfEnergy.artifactChargeMultiplier` **不在这条链路上**（全工程只出现在各神器 `act()` 的被动充能里），
  所以 `charge()` 里不乘它是与原版一致的。

### 3. 三件神器的逐件结论

| 神器 | 是否覆写 `charge(Hero,float)` | 神器充能的效果 | 备注 |
|---|---|---|---|
| 复仇账簿 `RevengeLedger` | ✅ `→ gainCharge(amount)` | 每回合 +1 点（=+1%），30 回合约 +30 点 | 单位与 `onHeroDamaged`/`onEnemyKilled`/`chargeByTalent(+8/15/25/50)` 同为「百分点」 |
| 泪锋的加护 | ❌ 未覆写 | **完全无效** | 唯一充能来源是 `onHeroGainExp`；`chargeCap` 仅 5~10 |
| 爱慕 `Admiration` | ❌ 未覆写 | 完全无效 | ⚠️ 不能直接接：召唤后 `charge` 被 `LoveSync` 复用为「溶解之爱血量百分比」 |

### 4. 核查发现的问题

1. **账簿双倍充能 ⟹ 仇怨永不结束**：`RevengeLedger` 是全工程唯一同时挂「被动 + 激活」两个 `ArtifactBuff`
   的自定义神器（`ledgerRecharge` + `Rancor`），而 `ArtifactRecharge` 对**每个** ArtifactBuff 各调一次
   `charge()` ⇒ 实得 **+2%/回合**，恰好抵消 `Rancor` 的 `RANCOR_DRAIN = 2%/回合`
   ⇒ **神器充能持续期间仇怨的充能不减、效果（力量 +12）无限延长**，直到 buff 结束。
   - 原版同构的 `OdinsEye`（`eyeRecharge` + `precognition`）没有这个问题——它的 `precognition`
     激活期间**不扣充能**（激活时一次性扣费）。
   - 修法（待定）：照 `ArtifactRecharge` 排除 `HornOfPlenty.hornRecharge` 的范式把激活态排除；
     最干净的做法是给 `ArtifactBuff` 加一个 `acceptsRecharge()` 谓词、`Rancor` 覆写为 `false`。
2. **泪锋的加护未接入神器充能**。若接入，系数要按它自己的刻度定：
   - 参照经验路径（一次英雄升级 ≈ 3 点充能），「一次 30 回合 buff ≈ 3 点」⇒ 系数 **0.1**（推荐）；
   - 若要「一次秘能卷轴直接灌满」，用 0.2 ~ 0.25。
3. **爱慕若要接入，必须限定「未召唤」**：`LoveSync` 每回合写 `adm.charge = 溶解之爱血量%`，
   任何无条件的 `charge()` 覆写都会被血条同步抹掉/污染。
   - 另注：爱慕初始 `cursed = true`，而 `ArtifactRecharge` 会整段跳过被诅咒的神器 ⇒ 接入后也须先解咒才吃得到。

## 九、泪锋的加护 / 爱慕 接入「神器充能」（2026-09-15）

上一节核查出的「两件神器未接入 `ArtifactRecharge`」现已落地。
（清查中发现的**复仇账簿双倍充能问题仍未修**，见上节第 4 条第 1 项。）

### 1. 泪锋的加护：对齐经验路径

`items/artifacts/TearSwordBlessing.java`：

- 新常量 `public static final float CHARGE_PER_RECHARGE_TURN = 0.1f;`
- 新增覆写 `public void charge(Hero target, float amount)`：
  `if (cursed || target.buff(MagicImmune.class) != null) return;` → `accumulateCharge(0.1f * amount)`。
- 把原 `onHeroGainExp` 里的「累加 / 进位 / 封顶 / 提示 / `updateQuickslot`」抽成私有
  `accumulateCharge(float gain)`，两条来源共用（逐字保留原行为）。

**系数的来历**：一次「英雄等级」的经验给 `CHARGE_PER_HERO_LEVEL = 3` 点；而充能 buff 满额 30 回合
（`amount` 累计 30）⇒ `3 / 30 = 0.1`，两者严格等价。系数与神偷袖章完全一致
（它的经验路径同为「3 点 / 英雄等级」、`chargeCap` 同为 `5 + level/2`，`charge()` 也是 `0.1f * amount`）。

| 来源 | 折算 | 一次「满额」的收益 |
|---|---|---|
| 获得经验 | `3 × 升级进度 × 环能倍率` | 一个英雄等级 = **3 点** |
| 神器充能 buff | `0.1 × amount` | 30 回合（满额）= **3 点** |

这条路径**不乘** `RingOfEnergy.artifactChargeMultiplier`（原版惯例：该倍率只进各神器自身的经验 /
被动充能；经验路径仍照乘）。

### 2. 爱慕：参考干枯玫瑰的双路

`items/artifacts/Admiration.java`：

- 新常量 `private static final float RECHARGE_CHARGE_PER_TURN = 4f;`
- 新增覆写 `public void charge(Hero target, float amount)`，按 `MeltingLove.findLoyalAlly()` 分路：

| 溶解之爱状态 | 行为 |
|---|---|
| 不在场（未召唤 / 已阵亡） | 正常蓄能：`partialCharge += 4f × amount`（一次满额 30 回合 ≈ 蓄满一条 100） |
| 在场（忠诚） | 充能**不进能量条**，改为**回复她的生命** `round((1 + 神器等级/3) × amount)`，头顶弹治疗数字 |
| 在场但已反噬（敌对） | 两条路都不做——不该给敌人回血 |

- **为什么在场时不能写 `charge`**：那一栏此刻已被 `LoveSync` 占用为「溶解之爱血量百分比」
  （每回合 `adm.charge = hpPct`，见 `Admiration.LoveSync.act()`），写进去下一回合就被抹掉。
- 治疗式 `round((1 + 等级/3) × amount)` 与 `DriedRose.charge()` 治疗幽灵的式子完全相同，
  蓄能式 `4f × amount` 也照玫瑰（它的 `chargeCap` 同为 100）。`DriedRose` 正是「神器 × 召唤物」的官方范本。
- 蓄满时**刻意不额外弹提示**：蓄满只意味着下一回合 `AdmirationBuff.act()` 会召唤，
  而 `summonLove()` 本就会提示「爱慕满溢，溶解之爱苏醒并追随着你。」，重复弹两遍反而吵。
- 仍受两条既有约束：① `ArtifactRecharge` 会整段跳过被诅咒的神器，而爱慕**初始必被诅咒**
  ⇒ 要先祛除诅咒才吃得到；② 神器未装备时 `AdmirationBuff` 不在身上，`charge()` 自然不会被调。

### 3. 核验

- 2 个文件 `javac -nowarn -proc:none -encoding UTF-8 -sourcepath core/src/main/java` **EXIT=0**。
- `javap -c` 确认链路：`TearSwordBlessing.charge` 内为 `ldc 0.1f → accumulateCharge(F)V`；
  `Admiration.charge` 内为 `Hero.buff → MeltingLove.findLoyalAlly → level()/Math.round/Math.min →
  CharSprite.showStatusWithIcon → refreshUI → MeltingLove.findAnyAlive`，且该方法内**无 `GLog` 调用**。
- 两文件 UTF-8 可解码、零替换字符。
- 文案：按本类既有风格（`Admiration` 全篇硬编码中文、不使用 `Messages` 键），本次**未新增文案键**。

# 2026-09-15 中指长兄 T2 文案瘦身 + 夸耀去滤镜 + 强化窗口整叠放入（0.2.3）

## 1. 中指长兄二层天赋：删除描述末尾的括号解释

`actors_zh.properties` / `actors.properties`（英文基准档同步），T2 四项天赋的 desc 尾部
`\n\n（……）` 括号说明整段删除（连带前导空行）：

| 天赋键 | 删除的括号内容 |
|---|---|
| `record_meal` | （未装备复仇账簿时不会充能；丰饶之角的进食同样受益） |
| `tattoo_engraving` | （在复仇账簿的菜单里选「刻入纹身」；需要先装备账簿，且未处于魔法免疫状态） |
| `handy_toy` | （冷却未走完时，切换照常消耗 1 回合） |
| `too_hot` | （只是烧不动你：身上照样会着火，背包里的卷轴也照样会被烧掉） |

`never_forget`（永不遗忘）desc 本就没有括号说明，未动。T1 天赋与 `brag` buff 文本均未改。

## 2. 夸耀（Brag）删除颜色滤镜

`actors/buffs/Brag.java`：删掉 `tintIcon(Image)` 覆写（原 `icon.hardlight(1f, 0.82f, 0.35f)` 橙黄滤镜）
与随之无用的 `com.watabou.noosa.Image` import，改为**不覆写 tintIcon**，图标以自绘原色显示。
`BuffIndicator.BRAG = 114` 帧号、`iconTextDisplay()`（层数）、`desc()` 均不变。

## 3. 大师指环「强化」窗口：材料整叠放入

`windows/WndBodyReinforce.java`（用户拍板选「整叠放入」方案）：

- **根因**：`Item.detach(Bag)` 在 `quantity > 1` 时走 `split(1)`，**只拆 1 份**，
  所以此前每次点选背包里的素材只会放进 1 个，3 格 × 1 份 = 一次最多 3 份，极繁琐。
- `itemSelector.onSelect` 改为 `item.detachAll(backpack)`：把**整个堆叠**搬进该格
  （`detachAll` 会 `quickslot.clearItem` 并递归进嵌套 bag，素材箱里的素材同样能取出）。
- `reinforce()` 累加四值时按 `Math.max(1, m.quantity())` **乘份数**：
  骨素材 ×23 ⇒ 骨值 +230、武器/肉/血值各 -46（照旧吃 `dimReturn` 递减与 A+ 等级上限）。
  `BodyArtWeapon` 不可堆叠，份数恒为 1，逻辑不变。
- 格子内堆叠数量由 `ItemSlot.updateText()` 的 `item.status()` 自动显示；点格子仍可把整叠取回背包，
  因此「整叠放入」不会误消耗（真正的消耗只发生在按下「强化」时）。
- 文案同步：窗口提示行 + 右侧选择器 `textPrompt()`（「……整叠放入右侧」）。
- **未改 `WndBodyCraft`（创作）**：用户只点名强化界面，创作仍是每次 1 份 / 9 格。如需一致可再改。

---

# 2026-09-15（补12）新 buff「残像纠缠」+ 配套测试道具

需求：持有该 buff 时角色呈「不稳定」状态——身上浮现一层**淡蓝紫色半透明**的自身图像副本「残像」，
偏移量在 **1 个像素之内**，**图层在角色图像之下**；移动时残像**延迟跟随**（角色动完，残像顿一下再跟上）。

## 一、三个新文件

| 文件 | 职责 |
|---|---|
| `sprites/AfterimageSprite.java` | 残像贴图（继承 `Image`）：复制角色当前帧 + 延迟追赶 + ≤1px 漂移 + 淡蓝紫染色 |
| `actors/buffs/Afterimage.java` | 「残像纠缠」buff：`fx(boolean)` 里创建/销毁残像，挂到 `floorEmitters` 层 |
| `items/AfterimageTester.java` | 测试道具「残像测试器」：使用即开关 buff（items 根包 ⇒ 调试窗「杂项」自动收录） |

## 二、四个技术要点

### 1. 图层：必须走 floorEmitters，不是 effect

`GameScene` 里各层的 `add` 顺序就是渲染顺序：`floorEmitters`（第 305 行 `add`）**早于** `mobs`（第 321 行）
⇒ 挂在 floorEmitters 层的残像渲染在角色**之下**。若用 `GameScene.effect(...)`（effects 层，mobs 之后添加）会盖在角色脸上。

### 2. 复制角色当前帧：`Image.copy()` + 每帧 `frame(target.frame())`

残像**不继承 `CharSprite`**，只继承 `Image`，每帧「抄」三样：`texture`（换装可能换贴图册）、
`flipHorizontal`、`frame()`（当前帧 UV 矩形）。这样天然跟随换装/换帧/朝向，不必复制角色的任何逻辑。

**坑**：`Image` 只在 `frame(...)` 被调用时才把 `flipHorizontal` 写进顶点 UV（见 `Image.updateFrame()`），
所以**必须每帧调 `frame()`**——好在 `updateVertices()` 复用顶点数组、只置 dirty 标记，开销极低。

### 3. 延迟跟随：位置缓动而非直接赋值

残像位置 `residueX/Y` 每帧以 `FOLLOW_RATE * Game.elapsed` 的比例向 `target.x/y` 收敛：
起步时残像先顿一下、跑动时恒定落后一小段、停下后慢慢贴回。

**额外加成**：`floorEmitters` 在 `mobs` **之前** update ⇒ 残像读到的是角色精灵**上一帧**的位置，
天然又多慢一帧，延迟感更明显（属特性，非 bug）。

### 4. 「不稳定」：≤1 像素的正弦漂移

在缓动位置之上叠加两个不同频率的正弦（相位随机，避免多残像同步抖动）：
`x = residueX + (sin(t*1.7)*0.7 + sin(t*3.3)*0.3) * 1px`，幅度 ≤1 像素、缓慢游移。

### 染色

`tint(0x93A7FF, 0.5f)`：`Visual.tint(color, strength)` 是「压暗 `1-strength` + 加色 `color*strength`」
⇒ 图像整体偏冷色并轻微提亮（真正的"**淡**"蓝紫），再配 `alpha(0.45f)` 半透明。
**不要用 `hardlight`**——它只做纯乘法，会把残像整体压暗。

## 三、踩到的坑

**`Buff.affect(ch, cls, duration)` 的泛型上限是 `FlavourBuff`** —— 带时长的重载只接受 `FlavourBuff` 子类。
`Afterimage` 继承 `Buff`（要覆写 `act()` 常驻）⇒ 必须用**无时长重载** `Buff.affect(ch, cls)`。
首编译即报：`推论变量 T 具有不兼容的上限，等式约束条件：Afterimage，上限：FlavourBuff`。

**`fx(true)` 要幂等且鲁棒** —— `Char.updateSpriteState()`（读档 / 场景重建 / 重新入场）会对每个 buff
再调一次 `fx(true)`；而 `GameScene.floorEmittersAdd` 在 `scene == null` 时**空转**（挂不上）。
因此判据写成「残像为空 **或** 残像没挂上（`parent == null`）」，否则首次挂载若空转，残像将永远不出现。

## 四、图标

`BuffIndicator.AFTERIMAGE = 117`（下一空帧）。帧 117 尚未绘制，`Afterimage.icon()` **暂借原版帧 50**
（UPGRADE，增强箭头）并用 `tintIcon` 染蓝紫——照 115/116 的既有惯例，注释已写在 `BuffIndicator` 中，
自绘完成后把 `icon()` 改回 `BuffIndicator.AFTERIMAGE` 即可。

## 五、核验

- 4 文件一次性 `javac -nowarn -proc:none -encoding UTF-8` **EXIT=0**（首编译因上述泛型坑报 1 错，改用无时长重载后通过）。
- UTF-8 可解码、括号配平（26 / 29 / 14 / 47）；properties `actors_zh` 1853 键、`actors` 1674 键、**0 重复键**、
  `afterimage` 2 键 zh/en 齐全、换行标记确认为**字面 `\n`**。
- 确认 `GameScene` 中 `floorEmitters` 先于 `mobs` 添加（305 < 321）；`WndDebug` 的 `PKG_ITEMS_ROOT`
  浅层扫描会收录本道具，无需注册。
- 未代跑 Gradle。

## 六、待游戏内验证

① 调试窗「杂项」调出「残像测试器」，使用后残像浮现、buff 条出现蓝紫图标；
② 残像**在角色之下**（走到怪物或其他角色旁边看遮挡关系）；
③ 静止时残像在 1 像素内轻微游移；
④ 移动时残像明显落后一小段、停下后慢慢贴回；
⑤ 再使用一次 → 残像消失。

---

# 2026-09-15（补13）残像纠缠·升级为「四残像 + 轨迹延迟回放 + 追上震荡」

需求（在补12 基础上的三处改动）：

1. 残像数量 **1 → 4**；
2. 跟随方式**不再是缓动追赶**，而是**完全复刻角色的移动轨迹与时机、只做时间延迟**
   （角色移动 →（微小间隔）→ 残像1 移动 →（同样的间隔）→ 残像2 移动 →…，
   每个残像的起止时间与速度都与角色一模一样）；
3. 每个残像**都比上一个多延迟一次**，**追上角色时小幅度震荡摇摆**；
4. 配色：由近及远 **紫红 → 蓝紫 → 蓝 → 深蓝**（四色递进，已与用户确认），仍然全部半透明。

## 一、核心改动：缓动 → 采样回放

| | 补12（旧） | 补13（新） |
|---|---|---|
| 跟随 | `residue += (target - residue) * FOLLOW_RATE * elapsed` 缓动追击 | 读「`delay` 秒之前」的**真实采样位置** |
| 表现 | 一直在「慢慢靠近」，起步顿、跑动恒定落后、停下慢慢贴 | 位移是角色轨迹的**时间平移**，起步/速度/停止**完全一致** |
| 数量 | 1 个 | 4 个（延迟 1×~4× `DELAY_STEP`） |
| 颜色 | 单一 `0x93A7FF` | `TINTS = {0xD957B0, 0x93A7FF, 0x4A90FF, 0x2743D8}` |

`AfterimageSprite.Trail`（新增的静态嵌套类）＝**角色轨迹环形缓冲**：
每帧记录一次 `{时刻, x, y, 朝向, 帧矩形, 纹理}`，容量 96 帧（约 1.6 秒 @60fps）。
四个残像**共用同一个 Trail 实例**（由 `Afterimage` buff 创建后传给每个残像），
各按 `Game.timeTotal - (i+1)×DELAY_STEP` 去查：
位置在相邻两个样本间**按时间线性插值**（帧率再低也是匀速），朝向/帧/纹理取「不晚于该时刻的样本」。

**为什么必须采样而不是缓动**：缓动只能做出「慢慢靠近」的手感，速度与节奏永远与角色对不上；
需求要的是「看起来就是角色本人晚了一会儿在走同一条路」，那只能把真实轨迹按时间轴平移。

## 二、延迟间隔的标定（关键数据）

`CharSprite.DEFAULT_MOVE_INTERVAL = 0.1f` 秒 / 格、一格 16 像素 ⇒ **约 160 像素/秒**。
所以 `DELAY_STEP` 直接决定「相邻残像的像素间距」：

| DELAY_STEP | 相邻间距 | 最远残像落后 | 观感 |
|---|---|---|---|
| 0.05 | 8px（半身位） | 0.2s ≈ 2.0 格 | 偏紧，四个残像略糊 |
| **0.07（采用）** | **≈11px** | **0.28s ≈ 2.8 格** | **四个残像分得清清楚楚** |
| 0.10 | 16px | 0.4s ≈ 4 格 | 拖尾过长 |

用户明确表示「残像落后较远是可以接受的视觉效果」，故按**能看清四个独立残像**取 0.07。

## 三、追上时的震荡

判据：回放位置与角色当前位置的间距 ≤ `CAUGHT_RANGE(0.5px)` ⇒ 视为追上。
**只在「由未追上 → 追上」的那一帧**触发一次（`caughtUp` 状态位），
震荡本身**不参与判定**（否则摆出去→判定为没追上→再触发，会永久反复触发）。
震荡＝幅度 `WOBBLE_AMPLITUDE(1.8px)`、`WOBBLE_TIME(0.5s)` 内线性衰减、角频率 `WOBBLE_FREQ(26rad/s)`
的正弦，x/y 用同相位不同频率（0.78 倍）以免走成一条直线；相位随残像随机，四个不会同步抖。

四个残像延迟不同 ⇒ **追上时刻天然错开**，正好呈现「一个接一个贴回来并各抖一下」。

静止时四个残像完全重合（只剩 ≤1px 的 `JITTER` 漂移）——半透明且叠在角色**之下**，所以看不出叠加。

## 四、踩到的两个坑（已入 AGENTS.md）

1. **`Game.switchScene()` 会把 `Game.timeTotal` 归零**（`Game.java:268`）。Trail 的时间戳随之倒流，
   若只判「间隔过大」就永远等不到重置，`newestAtOrBefore` 会一直返回**上个场景的陈旧样本**，
   残像会定在错误的位置上（约 1.6 秒后自愈）。修法：重置判据写成
   `now < last || now - last > RESET_GAP`。**顺带确认**：场景切换时 `scene.destroy()` →
   `Group.destroy()` 递归把每个成员的 `parent` 置 null ⇒「重建判据用 `parent == null`」是可靠的
   （切场景后 `fx(true)` 会带着全新 Trail 重建）。
2. **纹理必须先于帧矩形设置**：`Image.frame(RectF)` 要用 `frame.width() * texture.width` 换算显示尺寸。
   所以 Trail 把「纹理 + 帧矩形」作为**同一份样本**存下来，二者永远配套（换装换了贴图册也不会错位）。

## 五、核验

- `javac -nowarn -proc:none -encoding UTF-8`（AfterimageSprite / Afterimage / AfterimageTester）**EXIT=0**
- UTF-8 可解码、括号配平（47/47、35/35、14/14）
- `actors_zh.properties` 1853 键 / `actors.properties` 1674 键、**0 重复键**；`afterimage` 文案
  zh/en 双份已按新行为重写（四残像 / 四色 / 轨迹复刻 / 追上震荡），换行标记为字面 `\n`
- 测试道具 `AfterimageTester` 的 `desc()` 同步改写
- 未代跑 Gradle

## 六、待游戏内验证

① 使用「残像测试器」→ 应看到**四个**残像（紫红最近、深蓝最远）；
② 走一步：角色先动，随后紫红→蓝紫→蓝→深蓝**依次**按同样的速度走完同一段路；
③ 每个残像抵达你身边时**轻轻抖一下**再贴合；
④ 连续走动时拖尾最长约 2.8 格；
⑤ 上下楼/读档后残像仍在（并且不会跑到错误的位置上）；
⑥ 图层仍在角色**之下**、整体仍是半透明。

## 七、参数调整（首次实机对照后）

| 常量 | 原值 | 现值 | 理由 |
|---|---|---|---|
| `ALPHA` | 0.45 | **0.6** | 原值偏虚，抬高不透明度让残像"看得出形状"、静止重合时能分出层次 |
| `JITTER` | 1.0 → 1.5 → **1.0** | 先按"小幅提升"调到 1.5；用户实机对照后要求**回调**，故回到 1.0 |

取值依据都在 `AfterimageSprite` 的常量注释里。位置公式是两个系数 0.7 / 0.3 的正弦之和（系数和恰为 1），
所以 `JITTER` 数值即瞬时偏移上限 —— 1 像素远小于一个身位（16 像素），观感就是"浮在身上的重影在缓缓游移"。
文案 `actors.buffs.afterimage.desc`（zh/en）里的像素表述已同步（回调后为"一个像素 / a single pixel"）。
`TINT_STRENGTH(0.5)`、`DELAY_STEP(0.07)`、`WOBBLE_AMPLITUDE(1.8)` 均未动。

## 八、层数＝残像个数 + 受击冲击（补13 第二轮）

### 1. 层数

- `Afterimage.stacks`（1~`MAX_STACKS`＝4）。走本工程既有惯例（照 `HuntingTarget` / `Brag`）：
  公有 `stacks` 字段 ＋ 静态 `addStack` / `setStacks` / `stacksOn` ＋ 覆写 `iconTextDisplay()` 显示层数；
  `desc()` 改为 `Messages.get(this, "desc", stacks)`（文案因此变成**带参文本**，字面 `%` 需写 `%%`）。
- `AfterimageSprite.COUNT` 改名 **`MAX_COUNT`**：它现在只是**上限兼取色表长度**，实际个数由
  `Afterimage.count()` 决定（夹到 `1..MAX_STACKS`，再夹到 `TINTS.length` 防越界）。
  层数不足时取 `TINTS` 的**前 N 个**颜色 ⇒「最近的残像先出现」，蓝 / 深蓝随层数增长才浮现。
- `fx(true)` 的重建判据从「未建成」扩成「**未建成 或 数量 ≠ 层数 或 没挂上场景**」，
  配合 `refresh()`（`clearResidues()` + `fx(true)`）就能在层数变化时立刻重建。
- `stacks` 进存档（`storeInBundle`）；`restoreFromBundle` 里 `getInt` 缺键返 0 ⇒
  必须 `Math.max(1, Math.min(...))` 夹一次，否则旧存档读进来层数为 0、一个残像都不建。

### 2. 受击冲击（`FLINCH_*`）

- 入口 `Afterimage.flinch(Char, Object src)`，调用点在 **`Hero.damage()`**（该吃的伤害尚未扣，
  但已排除上方那些「这次根本不生效」的早退分支）。**刻意排除 `src instanceof Buff`**：
  毒 / 燃烧 / 流血等 DOT 都以 buff 为源，若也触发就会每回合抖一下，像在抽搐。
- 方向＝**远离伤害源**（`ch.sprite.x - src.sprite.x`）；近战 / 远程 / 技能都以 `Char` 为源，
  拿不到方向（陷阱、环境伤害）就随机甩一个 —— 总之挨打必有反馈。
- `AfterimageSprite.flinch(dirX, dirY)` 把单位方向按本残像相位偏转 **±12°**（`(phase-π)*0.07`），
  几个残像是「被震散」而不是整齐平移；重复受击＝重新开始一轮（覆盖、不叠加）。
- 曲线是**阻尼余弦**：`a = FLINCH_DISTANCE × (剩余/总时长) × cos(已过 × FLINCH_FREQ)`，起点即最大偏移
  ⇒ 路径为「甩出 → 越过平衡点 → 轻微反向 → 归位」，0.35 秒（≈21 帧）收完。
- 参数：`FLINCH_DISTANCE = 7f`（约半个身位，明显大于 `JITTER` 与 `WOBBLE_AMPLITUDE`）、
  `FLINCH_TIME = 0.35f`、`FLINCH_FREQ = 20f`（≈1.1 个周期）。
- **冲击与摇摆都只叠加在绘制坐标上，不参与「追上」判定**（`update()` 的 ③ 用基准 `px/py` 判，
  ④⑤ 才加偏移）——否则甩出去的瞬间会被自己判成「没追上」，状态位反复翻转、震荡退化成永久抖动。

### 3. 测试道具

`AfterimageTester` 由「开关」改为**层数循环器**：每次使用 +1 层（1→2→3→4），满层再用一次则移除；
`actionName` 动态显示「开启残像（1 层）／ 叠加残像（2 → 3 层）／ 关闭残像」。

### 4. 核验（源码级，未代跑 Gradle）

`javac`（AfterimageSprite / Afterimage / AfterimageTester / Hero）**EXIT=0**；UTF-8 与括号配平通过；
`actors_zh` 1853 键 / `actors` 1674 键、**0 重复键**；`afterimage.desc` 按 1/2/3/4 四个层数各实跑一次
`String.format` 全通过（参数为装箱 `Integer`）。

## 九、受击颤动放慢加大 + 受击也分层（补13 第三轮）

用户反馈两条：「放慢一些受击颤动的速度、增大幅度（现在弹出-弹回太小太快，看上去很『Q弹』）」、
「做出多层残像时的分层延迟效果」。

### 1. 节奏：把「Q 弹」换成「晃一大下」

| 常量 | 原值 | 现值 | 依据 |
|---|---|---|---|
| `FLINCH_DISTANCE` | 7f | **13f** | 7px 不足半个身位，太小；13px ≈ 一个身位（一格 16px），略小于一格所以不会看成「瞬移到别格」 |
| `FLINCH_TIME` | 0.35f | **0.8f** | 0.35s ≈ 21 帧收完，太快；0.8s 慢到能看清「甩出 → 回摆 → 收」 |
| `FLINCH_FREQ` | 20f | **9f** | 见下 |

**「Q 弹」的量化归因**（两版余弦都只有 3 个极值点，`极值个数 = ⌊时长 × 频率 ÷ π⌋ + 1`，
所以问题不在"周期数"，而在**整个动作的时间尺度与幅度尺度**）：

1. **最显眼的一半只有 5 帧**：从最大偏移回到平衡点耗时 `π/(2 × FREQ)` —— 旧值
   `π/40 ≈ 0.079s`（60fps 下 4.7 帧），快到人眼只捕捉到"颤了一下"；新值 `π/18 ≈ 0.175s`
   （10.5 帧），能看清"被甩出去"这个过程。
2. **幅度不足半个身位**：7px 相对一格 16px 太小，"被打飞"的语义立不住，落在眼里就是抖动。
3. **频率与时长是绑在一起改的**：余弦极值点出现在 `age × FREQ = kπ`，只拉长时长而保持 `FREQ = 20`
   则会得到 `0.8 × 20 = 16 rad` ⇒ 极值点 **6 个**（三组来回），比原来更弹簧。
   取 `0.8 × 9 = 7.2 rad` 才让极值点保持在 3 个（与旧值同量级）。

新曲线的形状：`+13px @0s → 0 @0.17s → −7.3px @0.35s → 0 @0.52s → +1.7px @0.70s → 0 @0.8s`。
即「甩出 → 越过平衡点 → **反向大摆一次** → 归位」；第 3 个正向尖峰落在时长的 87% 处，
线性阻尼只剩 `0.13` ⇒ 仅 1.7px、实际看不见。反向峰处阻尼剩 `0.56` ⇒ 回摆有 7px 以上，
清晰可见，不会像旧值（3.6px 且更早、更突）那样"刚离开就被抹平"。
阻尼仍是**线性衰减**（剩余 ÷ 总时长）⇒ 末尾必然归零，无突跳。

### 2. 受击分层：冲击波由内向外传

- 新增 `FLINCH_LAYER_DELAY = 0.09f`（秒/层）：第 i 个残像（0＝最内层）等 `i × 0.09` 秒才起步。
  **必须大于移动延迟 `DELAY_STEP`（0.07）**，否则传导节奏会和走路的拖尾节奏混在一起、看不出是「挨打传出去的一串」。
- 新增 `FLINCH_LAYER_SCALE = 0.12f`：第 i 个甩出距离 ＝ `FLINCH_DISTANCE × (1 + i × 0.12)`
  ⇒ 13 / 14.6 / 16.1 / 17.7px，外侧更远、有扩散感；彼此相差半格以内，不会散得看不出是一组。
- 实现：`AfterimageSprite` 新增 `rank`（构造函数里的 `index`）与 `kickAmp` 字段；
  `flinch()` 里算好方向偏转、幅度与**排队时长**，并把 `flinch` 置为 `flinchWait > 0 ? 0 : FLINCH_TIME`
  （最内层等待为 0、立即甩出）；`update()` 的 ④ 先扣 `flinchWait`，等归零才开始这一轮 `FLINCH_TIME` 的回弹。
  等待期间天然零偏移，不需要额外的「未开始」状态。
- **延迟留在贴图侧**：`Afterimage.flinch(...)` 只广播一个方向，不知道层数也不做延迟——
  否则残像的起步时刻会和它自己那条轨迹回放错位。

### 3. 60fps 模拟核验（脚本复刻 `update()` 的 ④ 段）

| rank | 起步 | 最大甩出 | 反向峰值 | 尾部小尖峰 | 收完 |
|---|---|---|---|---|---|
| 0（紫红·最近） | 0.00s | +12.7px @0.00s | −7.3px @0.32s | +2.0px @0.63s | ~0.77s |
| 1（蓝紫） | 0.08s | +14.3px | −8.1px @0.40s | +2.3px | ~0.85s |
| 2（蓝） | 0.17s | +15.8px | −9.0px @0.48s | +2.5px | ~0.93s |
| 3（深蓝·最远） | 0.27s | +17.3px | −9.9px @0.58s | +2.8px | ~1.03s |

（旧参数对照：四层**同时** 0.00s 起步、+6.7px、−3.6px @0.15s、0.32s 收完。）
四层整串动作约 1.07s：慢到能看清层次先后，又不至于拖到下一次挨打还没收完。
`javac`（AfterimageSprite / Afterimage / AfterimageTester）**EXIT=0**。未代跑 Gradle。

# 2026-09-15 「依旧果冻人」JELLY_PERSON（纯视觉挑战，含引擎级分层形变）

## 1. 挑战接线

- `Challenges.JELLY_PERSON = 1024`，数组顺序放在 `demolition_squad` 之后、`debug_mode` **之前**；
  **`DEBUG_MODE` 由 1024 上移为 2048**，`MAX_VALUE` 2047→**4095**，`MAX_CHALS` 10→**11**。
  - 原因：`HeroSelectScene` 的「随机挑战」是 `2^i`（`i < MAX_CHALS`）**直接拼位掩码**，
    只认位号连续，不认数组下标 ⇒ 常规挑战必须占满最低的连续位、特殊挑战（调试模式）必须占最高位。
    新挑战若沿用 1024 就会与调试模式撞位、随机挑战还会开出调试模式。
  - 副作用：旧存档/旧设置里 `1024` 这一位会被解读成新挑战（无害）；把新挑战**计入常规挑战数**
    （⇒ 参与 `1.25^n` 分数倍率、`Badges.validateChampion`、菜单挑战计数）。若不想计分，
    在 `Challenges.activeChallenges` 里照 `DEBUG_MODE` 那样 `continue` 跳过它即可。
- 文本键（`messages/misc/misc_zh.properties` + `misc.properties`，其余语言走 ResourceBundle 回退英文）：
  `challenges.jelly_person` =「依旧果冻人」/ "Still jelly"、`challenges.jelly_person_desc`。
  选择界面由 `WndChallenges` 数据驱动，**不需要新图标**。

## 2. 视觉实现（引擎级：`SPD-classes` 分层形变）

- **`com.watabou.noosa.JellyDraw`（新增）**：把一张贴图按高度切成 `SLICES` 条横带，
  每条整体横向平移、位移随高度按 `t²` 递增（`t = 0` 在**底边**＝脚底 ⇒ 底边完全不动），
  顶点写进**全局共享的客户端 `FloatBuffer`**（`Quad.createSet(SLICES)`），
  一次 `script.drawQuadSet(quads, SLICES)` 画完。相邻层向下多铺 `SEAM = 0.25f` 像素（UV 同步多取）防接缝，
  **最底层不铺**（否则采样越出帧外、串到贴图册相邻帧）。
  **UV 的 v 必须按 `1-t` 插值**（`vTop ↔ y=0`、`vBot ↔ y=height`），`t` 直接拿去插会上下颠倒（见本节的「修订」）。
- **`com.watabou.noosa.Image`（改动）**：新增 `jellyAmp`（当前幅度）等 5 个字段 +
  `jellyWobble(amp, dir, duration, freq)` / `jellyOffset()`，覆写 `update()` 递增 `jellyTime` 并在
  超时后把 `jellyAmp` 归零；`draw()` 里 `jellyAmp != 0` 时改走 `JellyDraw.draw(this)` 后 `return`。
  **`jellyAmp == 0` 时绘制路径与改动前逐字节一致**（唯一的常驻开销是一次 float 比较）。
- **三个不能踩的点**：①**不改 `Image.vertices`/`buffer`**——钻石剑附魔流光直接读 `vertices` 当遮罩 UV，
  且摆动结束后 Image 还要用自己那份原始顶点继续画；②**不用静态 `Vertexbuffer`**——
  `Game.switchScene()` 会 `Vertexbuffer.clear()` 把已注册缓冲的 GL id 全清掉；
  ③摆动方向/判定全部走**绘制层**，不碰 `x/y/scale/angle`（角色逻辑位置与命中判定不受影响）。

## 3. 游戏侧：`effects/JellyWobble`

- `enabled()` = `Dungeon.isChallenged(Challenges.JELLY_PERSON)`；两个触发点：
  - **单位受击** → `Char.damage()` 末尾（伤害浮字之后）：全工程伤害的汇聚点，英雄/友方/敌人/中立一视同仁。
    内部用 `isHit(src)` 只认**直接命中**（`src` 是 `Char` / `Weapon` / `Weapon.Enchantment` / `Item` / `Trap`）——
    燃烧、流血、中毒、腐蚀淤泥等每回合跳一次的持续伤害 `src` 是 buff 自身，被排除，
    否则着火的目标会变成一直抖的果冻。
  - **物品落地** → `ItemSprite.update()` 里投掷/掉落动画结束、`place(heap.pos)` 归位之后。
    覆盖一切「掉到地上」的路径（投掷、怪物掉落、踩草、商店找零…）；关卡生成时创建的 Heap 走
    `link()`（`dropInterval = 0`）不触发，所以**开局不会满地乱抖**。
- 参数：`AMP = 16f`（顶部最大横向偏移 16 像素＝一个多身位，实际带 ±15% 随机）、`DURATION = 0.9f` 秒、
  `FREQ = 8f` rad/s（时长×频率 ≈ 7.2 ⇒ 极值点 3 个，甩出去→回摆→过冲）。
  方向默认**背离攻击者**（攻击者在左就往右甩），拿不到攻击者（道具/陷阱）时随机。
- 已知取舍：地面上的钻石剑摆动时，其附魔流光用的是图标原始顶点，**流光不跟着晃**（只影响这一件道具）。

## 4. 修订（2026-09-15 二次调整）

- **修复「摆动时贴图上下颠倒」**：`JellyDraw` 里 UV 的 v 是拿 `t`（**从脚底起算**的高度比例）
  直接插值的，而局部 y 是**从顶边起算**（`y=0` 对应 `frame.top`），两者差一个 `1-t`
  ⇒ 贴到顶边的是 `frame.bottom`，整张图被垂直翻转。改成 `lerp(vTop, vBot, 1-t)`（`vTop=V` 帧顶、`vBot=` 帧底，
   `flipVertical` 时互换）后与 `Image.updateFrame()` 的映射一致。**根因是「t 的零点」与「y 的零点」不是同一端**，
   改任何「高度比例 → UV」的代码前先确认这一点。
- **幅度 6 → 16 像素**（`JellyWobble.AMP`），配合 `SLICES` 12 → 20：相邻层横向错位量
  ≈ `2·amp·t / SLICES`，幅度涨 2.7 倍而层数不加，峰值处轮廓会露出明显阶梯。
  中段（t=0.5）位移为 `0.25 × amp = 4px`，观感是「上半身被扇飞、脚底钉在地上」。

## 5. 修订二（2026-09-15 三次调整）：次数、竖直形变、死亡动画

- **摆动次数**：`DURATION` 0.9 → **1.4s**、`FREQ` 8 → **14 rad/s**
  ⇒ 来回次数 `DURATION×FREQ/(2π)` 从 1.15 次提到 **≈3.1 次**，尾巴上能看清三下余震。
  要再改只动这两个常量（次数对 `DURATION` 是线性的）。
- **竖直形变（不再只有横向）**：`Image` 新增 `jellySquashAmp` / `jellySquash()`，
  与横向摆动**差 1/4 周期**（cos → −sin）⇒ 先被甩出去、随后才一压一弹，而不是整体同步缩放（那只是呼吸）。
  `JellyDraw` 里以脚底为基准做 `y = h·(1 − t·(1+squash))`（`t=0` 处恒为 `h` ⇒ 脚底不动），
  并把压扁量按 `t` 折算成横向**鼓出**（`BULGE = 0.6`，近似保体积）。
  三个形变（横移 / 压缩 / 鼓出）的零点都在底边，所以「脚底不移动也不形变」始终成立。
  `JellyWobble.SQUASH = 0.3f`（±30%），死亡时随 `DEATH_BOOST = 1.3f` 放大到 ±39%。
- **死亡动画也形变**：`Char.die()` 里 `sprite.die()` **之前**调 `JellyWobble.death(this, src)`
  （放在 `destroy()` 之后、`if (src != Chasm.class)` 块内 ⇒ 掉进深渊（无死亡动画）不触发）。
  方向沿用**致命一击的来向**，于是死亡摆动会接在受击摆动后面同向甩出去，像被一击打散架。
  `Char.die()` 是全工程唯一的死亡汇聚点（`actors/` 下只有 `Hero` 覆写它，且无复活手段时仍 `super.die`）
  ⇒ 一处接线即覆盖所有怪物/英雄，**不要**去 28 个 `XxxSprite.die()` 里逐个加（大半还会 `super.die()`，会重复触发）。
- **`Char.die()` 里的调用要放在 `sprite.die()` 之前**：部分死亡动画会 `killAndErase()` 摘掉精灵，之后就晚了。

---

# 2026-09-16 复仇账簿「燃起仇怨」增加门槛与初始消耗

需求：每次**使用**（燃起仇怨）立刻消耗 20% 充能；充能不足 20% 则无法使用；这笔消耗与「随时间消耗」
（每回合 2%）**独立结算**。

## 一、改动清单（`items/artifacts/RevengeLedger.java`）

| 位置 | 改动 |
|---|---|
| 常量 | 新增 **`RANCOR_ACTIVATION_COST = 20`**（门槛与初始消耗**共用同一个常量**） |
| `actions()` | `charge > 0` → `charge >= RANCOR_ACTIVATION_COST \|\| activeBuff != null`（不够门槛就不列出动作，已激活仍列出以便主动解除） |
| `execute()` | `charge <= 0` → `charge < RANCOR_ACTIVATION_COST`，仍用 `no_charge` 键提示 |
| `startRancor()` | 挂 buff **之前**先 `charge = Math.max(0, charge - RANCOR_ACTIVATION_COST)` |
| `Rancor.act()` | 每回合 `-RANCOR_DRAIN` 不变（未改） |

## 二、四个设计要点

1. **门槛与消耗共用一个常量**。若两处各写一个数字，日后调门槛时极容易只改一处，出现「门槛写 30 却只扣 20」
   这种隐性白嫖。同一条规则的两面共用一个来源是唯一安全的写法。
2. **两笔消耗各自独立**：入场费在 `startRancor` 里一次性扣，持续性消耗在 `Rancor.act()` 里逐回合扣，
   互不冲抵。`Rancor.act()` 与既有的「充能归零即熄灭」判定都无需改动——20% 扣完若归零，下一次回合结算
   时（先扣 2% 再判 `charge <= 0`）自动熄灭。
3. **消息文本不带数字**（用户约定）：`GLog` 只给感受性措辞，具体数值一律写进神器描述。
   因此 `on_rancor` 保持**无参**（不改文案）、`no_charge` 只把语义由「一笔都没记下」改成「还太淡」，
   真正的「至少 20% / 立刻付出 20% / 每回合 2%」写在 `revengeledger.desc`（zh + en）。
4. **入口覆盖**：菜单、快捷栏、背包双击（`item.defaultAction()` → `Item.execute(hero)`）最终都汇到
   `execute(hero, AC_RANCOR)`，所以门槛只需拦这一处。

**边界情形**：充能恰好 20% 时可以使用，但扣完即为 0，仇怨会在下一次回合结算时立刻熄灭（只生效不到一回合）。
这是「门槛用尽」的自然结果，符合「充能越多越划算」的直觉；若要避免，把门槛判据改成「高于 20%」即可
（`charge > RANCOR_ACTIVATION_COST`）。

## 三、核验（源码级，未代跑 Gradle）

- `javac -nowarn -proc:none -encoding UTF-8`（`RevengeLedger.java`）**EXIT=0**。
- `javap -p -c` 确认 `startRancor` 首段即为 `iconst_0 / getfield charge / bipush 20 / isub / Math.max / putfield`，
  且**早于** `activeBuff()` 调用 —— 扣费确实在挂 buff 之前发生。
- properties：`items_zh` 2322 键 / `items` 2140 键、**0 重复键**；`revengeledger.*` **14 个键 zh/en 齐全**；
  `desc` 为无参文本，其中 `20%` / `2%` / `100%` 等字面 `%` 无需转义（`Messages.get` 只在 `args.length > 0` 时才 format）。
- 待游戏内验证：① 充能 19% 时菜单里没有「燃起仇怨」、快捷栏点击提示「还太淡」；② 充能 20% 时使用 → 立刻变 0；
  ③ 充能 100% 时使用 → 立刻变 80%，之后每回合 −2%；④ 已激活时再点＝解除，不再扣 20%。

---

# 2026-09-16 中指长兄三阶通用天赋：过人的毅力 / 野兽狂怒

两个**通用 T3**（两个转职分支都能点），各 3 点。图标占 `talent_icons.png` **第十一行 10/11 列 = 329/330**
（待绘制；第十一行 27 列 346 是充能天赋，已占用）。放在**职业天赋层**（`initClassTalents` 的 tier 3）而不是
子职业层，这样两个转职共用一份。

## 一、设计数值

| 天赋 | +1 | +2 | +3 |
|---|---|---|---|
| **过人的毅力** | 下限 5% | 下限 10% | 下限 15% |
| **野兽狂怒** | 血量 < 50% 时，造成与受到的伤害 **×1.1** | ×1.2 | ×1.4 |

**过人的毅力**（`Talent.perseveranceCap`）两段规则，`floorHP = max(1, ceil(HT × 下限%))`：

1. **HP 高于下限**：`dmg = min(dmg, HP − floorHP)` —— 这一下最多把血打到下限为止，**绝不出致命一击**；
2. **HP 已在下限之内**：`dmg = min(dmg, 1)` —— 仍会被打死，但单次永远只掉 1 点，**不会是「一下秒」**。

`HT=100` 时的实测形状（模拟脚本，一次 999 点致命伤）：

| 下限 | HP=100 | HP=50 | HP=21 | HP=11 | HP=6 | HP=5 | HP=4 |
|---|---|---|---|---|---|---|---|
| 5% | → 剩 5 | → 剩 5 | → 剩 5 | → 剩 5 | → 剩 5 | → 剩 4 | → 剩 3 |
| 10% | → 剩 10 | → 剩 10 | → 剩 10 | → 剩 10 | → 剩 5 | → 剩 4 | → 剩 3 |
| 15% | → 剩 15 | → 剩 15 | → 剩 15 | → 剩 10 | → 剩 5 | → 剩 4 | → 剩 3 |

## 二、四个接线要点

1. **下限夹取放在 `Hero.damage` 的 `super.damage` 之前、`dmg` 定稿处**（`RingOfTenacity` / `SilentPrice`
   两行之后）：那一刻护甲减伤与全部倍率都算完了，而**护盾与血量还一点没动**，所以夹的就是「这一下真正要
   打进去的总伤害」。**必须早于 `super.damage`**——血量归零是在 `super.damage` 里直接判死的，事后补血来不及。
2. **覆盖一切来源**：攻击、环境、陷阱、坠落、以及毒/燃烧/流血这类 DOT 都走同一个 `Hero.damage`，一处夹取全管住。
   `Hero.damage` 里更早的整段免疫（如「会很烫的！」直接 `return`）自然不受影响。
3. **「造成的伤害」用 `Talent.isHeroDealtDamage` 口径**（英雄本人为源 + 法杖），接在 `Char.damage` 里
   「报复对象 +20%」的**同一处**：那里的 `src` 覆盖面最广，近战/投掷/技能/法杖一次全管住，与项目既有约定一致。
4. **两份加成绝不会叠乘在同一笔伤害上**：`Char.damage` 里的 `src` 是出手方、`this` 是挨打方；`Hero.damage`
   里英雄只可能是挨打方。同一笔伤害只走其中一条。

## 三、判据细节（都是踩过的坑）

- **「低于 50%」取严格小于**：`hero.HP * 100 < hero.HT * 50`（整数比较），与既有的 `tooHotImmune` 同款写法，
  避免浮点误差；同理下限分支用 `HP > floorHP` 而不是再算一遍百分比——否则「向上取整后的下限」与「百分比比较」
  两套口径会在边界上打架（例：`HT=13`、5% ⇒ 下限 1 点，而 `HP=1` 明明是 7.7% > 5%，两套口径给出相反结论）。
- **下限向上取整**（`ceil`）：`HT=75`、5% ⇒ 4 点（5.3%）。宁可多留一点血，也不让下限在高生命上限 + 小百分比时
  被取整抹成 0；再用 `max(1, ...)` 兜底，保证任何 `HT` 下下限都 ≥ 1。
- **`floorHP` 分支保证 `HP − floorHP ≥ 1`**（因为只有 `HP > floorHP` 才走这一支），所以不会出现
  `min(dmg, 0)` 那种「无敌」的副作用。
- **新天赋对旧存档可加**：`Hero.storeInBundle` 不存天赋表（`Talent.storeTalentsInBundle` 只存**已投入点数 > 0**
  的项），`restoreTalentsFromBundle` 会先 `initClassTalents(hero)` 重建整张表再回填点数 ⇒ 往
  `initClassTalents` 里加新天赋，**旧存档也能立刻在天赋面板看到并加点**。

## 四、核验（源码级，未代跑 Gradle）

- `javac -nowarn -proc:none -encoding UTF-8`（`Talent.java` / `Hero.java` / `Char.java`）**EXIT=0，0 错误**；
  `javap` 确认 `perseveranceCap(Hero,int)` / `beastFuryMultiplier(Hero)` / `perseveranceFloorPercent(Hero)` 已生成。
- 枚举图标索引全表扫描：**无重复**；`329 → PERSEVERANCE`、`330 → BEAST_FURY`。
- properties：`actors_zh` 1857 键 / `actors` 1678 键、**0 重复键**；`perseverance.*` 与 `beast_fury.*`
  共 4 个键 zh/en 齐全，`_..._` 强调标记成对（描边标签需成对才触发）。
- 待游戏内验证：① 血量 50% 被打一下不掉到 50% 以下；② 血量贴住下限后每次只掉 1 点；③ 残血时用技能/法杖
  打出去也是 ×1.4；④ 毒/燃烧掉血同样受下限保护；⑤ 血量 > 50% 时野兽狂怒不生效。

---

# 2026-09-16 中指长兄第一个转职「忠义巡礼者」（怨恨标记 + 五式复仇技艺 + 加倍清算！）

> 需求原文（用户）：_忠义巡礼者_会使得任何攻击命中角色和友方单位的敌人获得 1 层「怨恨标记」（图标已绘制在夸耀 buff 的下一位）。忠义巡礼者可以对带有怨恨标记的敌人使用特殊的复仇技能。
> 追加设计：五式技能按目标身上的**标记层数**解锁（1~5 层），使用时扣掉等同层数；技能按钮需**常时显示**；另实现专精天赋「加倍清算！」（3 点）。

## 一、需求 → 实现对照

| 需求 | 实现落点 |
|---|---|
| 命中角色/友方单位的敌人 +1 层怨恨标记 | `VengeanceArts.onAttackLanded(attacker, victim)`，调用点＝`Char.attack` 命中分支（紧挨 T2「永不遗忘」的 `RevengeTarget`） |
| 标记本体（叠层负面 buff，挂敌人） | **新文件** `actors/buffs/GrudgeMark.java`（`MAX_STACKS = 10`，`announced = true`，`iconTextDisplay()` 显示层数） |
| 标记图标位置 | `BuffIndicator.GRUDGE_MARK = 115`（＝夸耀 `BRAG = 114` 的下一位）；`TEMP_STRENGTH` 从 115 **顺延到 118** |
| 技能按钮常时显示 | `VengeanceArts extends Buff implements ActionIndicator.Action`，`act()` 每回合**无条件抢回**槽位（与 `SwordSwap` 的让步策略相反） |
| 转职特色入口 | `TengusMask.choose` 里 `if (way == HeroSubClass.LOYAL_PILGRIM) Buff.affect(curUser, VengeanceArts.class);` |
| 五式复仇技能 | `VengeanceArts.Move` 枚举（`STOMP/KICK/PIERCE/RESTOMP/EXECUTION`，`cost = 1..5`），列表窗口 **新文件** `windows/WndVengeanceSkill.java` |
| 使用时扣层 | `GrudgeMark.consume(Char, int)`（不足返 0 ⇒ 整式中止，绝不空放、绝不扣成负数） |
| 专精天赋「加倍清算！」 | `Talent.DOUBLE_RECKONING(331, 3)`，全部加成**只**从 `VengeanceArts.reckoning(hero)` 一处取点 |
| 转职图标 | `HeroIcon.LOYAL_PILGRIM = 126`（hero_icons.png **倒数第二个位置**） |

## 二、五式复仇技艺（全部走"选目标 → 扣层 → 结算"三段）

| 层 | 招式 | 选靶距离要求 | 效果 |
|---|---|---|---|
| 1 | 踏碎 | **相邻**（脚下八格，与武器攻击距离无关） | 对**选中的那一个**敌人造成 `S-5 ~ S+15`（S＝`hero.STR()`），护甲照常减免；**不消耗回合**。冲击波特效照旧，但**不波及**相邻的其它敌人 |
| 2 | 踢飞 | 8 格内 | 跳到目标身边，对其一次攻击，并把**落点**周围八格敌人各击退 2 格；**已贴着目标则原地起脚、不移动** |
| 3 | 穿腹 | 武器攻击距离内 | 对范围内全部敌人各一次攻击，并各造成**等同于该次伤害**的 `Bleeding` |
| 4 | 仇怨重踏 | 武器攻击距离内 | 对范围内全部敌人各**三段**攻击，末段各击退 3 格 |
| 5 | 即刻处刑 | **相邻** | 主手非莱瓦汀则立刻换上；对目标三段（**50%/100%/150%**），本条**无视莱瓦汀力量惩罚** |

### 逐式实现要点

- **踏碎必须真的不消耗回合**：`doStomp` 里刻意**不调** `hero.busy()`、也**不调** `spendAndNext`（其余四式都在回调末尾
  `hero.spendAndNext(hero.attackDelay())`）。伤害算 `Hero.heroDamageIntRange(min,max)` 后**自己减 `enemy.drRoll()`**——
  普通 `attack()` 链不方便表达"固定区间 + 仍走护甲"。**只结算被选中的目标**（`useMoveOnTarget` 把 `enemy` 传进来），
  震屏 / `BlastWave` / `bloodBurstA` 全是纯演出。
- **踢飞＝照抄英勇之跃**：`leapCell(hero, enemy)` 用 `Ballistica(hero.pos, enemy.pos, STOP_TARGET | STOP_SOLID)` 取
  `collisionPos`，被占则沿 `route.path` 回退；返回 -1 报「没有空地」。落点确定后 `hero.sprite.jump(...)` →
  `hero.move(dest)` + `occupyCell` + `Dungeon.observe()` + `GameScene.updateFog()`（缺一不可，否则视野/占格不同步）。
  击退方向用 `Ballistica(ch.pos, ch.pos + i, MAGIC_BOLT)`（`i` = 八格偏移，即"背离英雄"）。
- **穿腹的流血量要抓「实际打掉多少」**：攻击前后各取一次 `ch.HP`，差值即 `dealt`；`dealt <= 0` 或目标已死就跳过
  （**打之前判活、每段之后各判一次**——boss 覆写 `damage()` 会对尸体再 `assignBoss()+seal()`）。
- **仇怨重踏必须先快照目标列表**：末段击退会改位置，边打边筛会漏人 ⇒ 先 `ArrayList<Char> targets = targetsInReach(hero)`。
- **即刻处刑的力量加成是「再加一份同分布掷值」**：`MeleeWeapon.damageRoll` 本就会给 `0 ~ (力量−需求)` 的加成，本式
  在 `hero.attack(enemy, multi, bonus, INFINITE_ACCURACY)` 的 `bonus` 参数里**再掷一次同区间**——所以超出 3 点力量时
  三段各得 `0~6` 而非 `0~3`。

## 三、「无视莱瓦汀力量惩罚」怎么做到（本批最绕的一处）

莱瓦汀的力量需求是 22，不足时的惩罚分散在三处，且**都在内部按力量差额算、从外面绕不过去**：
`Weapon.accuracyFactor()`（精准 ÷1.5ⁿ）、`Weapon.baseDelay()`（延迟 ×1.2ⁿ）、`Hero.canSurpriseAttack()`。

- 引入 **`VengeanceArts.ExecutionAftermath`**（`extends FlavourBuff`）作为**唯一判据**，三处统一经
  `SealedSwordBase.ignoresStrengthPenalty(Weapon, Char)` 读它（`Weapon.accuracyFactor` / `Weapon.baseDelay` 把
  力量惩罚项**整体归零**，`Hero.canSurpriseAttack` 的力量分支直接放行）。
- **取攻击延迟也必须在豁免态下取**：`executionDelay(hero)` 临时 `Buff.affect(hero, ExecutionAftermath.class, Actor.TICK)`
  → 读 `hero.attackDelay()` → 立刻 `temp.detach()`（同一个回合内挂上又撤掉，`act()` 根本跑不到，无残留）。
  **已存在余威时不动它**（刚用过即刻处刑的情况，直接沿用）。
- 天赋 +3 时余威**留下来**：`AFTERMATH_BASE = 3f` 起、每清掉 1 层标记再加 `AFTERMATH_PER_MARK = 5f`。
- `SealedSwordBase.forceIntoMainHand(hero, Laevateinn.class)`：**绕过解封门槛**把指定封印之剑直接塞主手
  （不走 `swapHands`，因为那条会把形态换回去）；失败（手上没有该系列）返回 null 并 `GLog` 提示。

## 四、专精天赋「加倍清算！」——单一取点，避免以后重复改五式代码

需求原文要求「避免之后重复修改复仇技能代码」，故设计为**所有加成只从 `reckoning(hero)` 一处读**：

```java
private static int reckoning( Hero hero ){       // 0 / 1 / 2 / 3
    return hero == null ? 0 : hero.pointsInTalent( Talent.DOUBLE_RECKONING );
}
```

| 点 | 招式 | 加成 | 实现位置 |
|---|---|---|---|
| +1 | 踏碎 | 目标获得 5 回合**易伤** | `doStomp` 内 `points >= 1 && enemy.isAlive()` → `Buff.prolong(enemy, Vulnerable.class, RECKONING_VULNERABLE)` |
| +1 | 踢飞 | 击退后 3 回合**麻痹** | `doKick` 击退循环内 `Buff.prolong(ch, Paralysis.class, RECKONING_PARALYSIS)` |
| +2 | 穿腹 | 流血 ×1.5；**免疫流血**的目标改为再补 0.5 倍伤害（合计 1.5 倍） | `doPierce` 里 `ch.isImmune(Bleeding.class)` 分支：能流血 → `Bleeding.set(dealt * 1.5f)`；不能 → `ch.damage(round(dealt * 0.5f), hero)` |
| +2 | 仇怨重踏 | 攻击后 10 回合**眩晕 / 恍惚 / 失明** | `doRestomp` 击退之后 `Buff.prolong(ch, Vertigo/Daze/Blindness.class, RECKONING_SEAL)` |
| +3 | 即刻处刑 | 3 回合起、每清 1 层 +5 回合的**处刑余威**；同时**清空目标剩余全部标记** | `doExecution` 末尾 `GrudgeMark.clearAll(enemy)` → `Buff.affect(hero, ExecutionAftermath.class, 3f + 5f * cleared)` |

所有数值常量集中在 `VengeanceArts` 顶部「数值常量（改平衡只动这里）」区：
`STOMP_MIN_BASE/PER_STR`、`STOMP_MAX_BASE/PER_STR`、`KICK_RANGE`、`KICK_KNOCKBACK`、`RESTOMP_HITS`、`RESTOMP_KNOCKBACK`、
`EXECUTION_MULTI[]`、`AFTERMATH_BASE/PER_MARK`、`RECKONING_VULNERABLE/PARALYSIS/SEAL/BLEED_MULTI`。

## 五、「按钮必须常时显示」的代价与做法（本批最值得记的一条）

角斗士的连击按钮只在攒够连击时才出现——连击在**自己**身上，一眼可见；本系统的"弹药"是**敌人**身上的标记，
从自己的状态栏完全看不出能不能出手，所以按钮从转职起就一直占着右下角。

- `act()` 每回合跑：**槽位不是自己就抢回来**（`ActionIndicator.setAction(this)`）；已经是自己则只在
  `highestMarksInView()` 变化时 `ActionIndicator.refresh()`（否则每回合重建按钮）。子职业变了就 `detach()` 自我回收。
- `secondaryVisual()` 画**视野内敌人的最高标记层数**（＝当前最高可用招式等级），颜色用 `CharSprite.NEGATIVE`；
  `indicatorColor()` 取 `0x9922CC`（与标记图标同色系）。
- **代价**：右下角只有一个槽（见 `AGENTS.md` §4），封印之剑的 `SwordSwap` 用的是"只在空槽时占位"的让步策略，
  所以忠义巡礼者会让它长期让位——**主副切换仍可从武器的动作菜单 `SealedSwordBase.AC_SWAP` 执行**（已在类 javadoc 注明）。
- **读档兜底**：走 `restoreFromBundle` 末尾 `ActionIndicator.setAction(this)`（`Hero.restoreFromBundle` 先恢复 buffs
  后恢复 `subClass`，`attachTo` 里依赖 `subClass` 的判断必然失败——同 `ArtTechniques`/`ScorchingWound` 的既有坑）。
- `revivePersists = true`：安卡复活后按钮仍在。

## 六、转职接入清单（本次实际改动的 13 个文件）

| # | 文件 | 改动 |
|---|---|---|
| 1 | `ui/HeroIcon.java` | `LOYAL_PILGRIM = 126`（附注：图片总共 128 帧，127 被 `NONE` 占，**126 是本图最后一个可用槽**） |
| 2 | `ui/BuffIndicator.java` | `GRUDGE_MARK = 115`；`TEMP_STRENGTH` 115 → **118** |
| 3 | `actors/buffs/TempStrength.java` | 注释同步（帧号 115 → 118） |
| 4 | `actors/hero/HeroSubClass.java` | 新增枚举常量 `LOYAL_PILGRIM(HeroIcon.LOYAL_PILGRIM)` |
| 5 | `actors/hero/HeroClass.java` | `MIDDLE_FINGER(BERSERKER, GLADIATOR)` → `MIDDLE_FINGER(LOYAL_PILGRIM, GLADIATOR)`（第二个专精「背叛家人者」未设计，仍占位角斗士） |
| 6 | `actors/hero/Talent.java` | `DOUBLE_RECKONING(331, 3)`；`initSubclassTalents` 的 **tier 3 switch** 加 `case LOYAL_PILGRIM:`（其余两层写空 case） |
| 7 | `items/TengusMask.java` | `choose()` 里施加 `VengeanceArts` |
| 8 | `actors/buffs/GrudgeMark.java` | **新文件**（180 行） |
| 9 | `actors/buffs/VengeanceArts.java` | **新文件**（774 行，含 `Move` 枚举与 `ExecutionAftermath`） |
| 10 | `windows/WndVengeanceSkill.java` | **新文件**（83 行：五式列表 + 消耗标签，`enable(arts.canUse(move))`） |
| 11 | `actors/Char.java` | 命中分支加 `VengeanceArts.onAttackLanded(this, enemy)` |
| 12 | `items/weapon/melee/SealedSwordBase.java` | 新增 `forceIntoMainHand(Hero, Class)` 与 `ignoresStrengthPenalty(Weapon, Char)` |
| 13 | `items/weapon/Weapon.java` + `actors/hero/Hero.java` | `accuracyFactor` / `baseDelay` 力量惩罚归零；`canSurpriseAttack` 放行 |

文本：`actors_zh` / `actors` 各 12 组键（`herosubclass.loyal_pilgrim*`、`talent.double_reckoning.*`、
`buffs.grudgemark.*`、`buffs.vengeancearts.*` 与 `$move.{stomp,kick,pierce,restomp,execution}.{name,desc}`、
`$executionaftermath.*`）；`windows_zh` / `windows` 各 2 键（`wndvengeanceskill.title/.cost`）。

## 七、核验（源码级，未代跑 Gradle）

- `javac -nowarn -proc:none -encoding UTF-8`（13 个文件全量）**EXIT=0，0 错误**（仅 unchecked 警告）。
- 括号/花括号/方括号配平全 0、无 U+FFFD 乱码：`VengeanceArts` 774 行、`GrudgeMark` 180 行、`WndVengeanceSkill` 83 行。
- properties 用**真实参数类型实跑 `String.format`**：`grudgemark.desc(Integer,Integer)`、`executionaftermath.desc(String)`、
  五式 `desc(Integer)`、`invoked(String)`、`need_marks(Integer)`、窗口 `cost(Integer)` 全部 zh/en 通过；0 重复键。
- 枚举/图标索引无重复：`HeroIcon.LOYAL_PILGRIM = 126`；`BuffIndicator` 中 `GRUDGE_MARK = 115` 与 `TEMP_STRENGTH = 118` 均唯一。
- **待游戏内验证**：① 转职界面出现「忠义巡礼者」且图标正确；② 敌人打中自己/召唤物后头顶弹「怨恨标记」并叠层；
  ③ 右下角按钮转职即常时显示，右上小字＝视野内最高层数；④ 五式的距离要求/消耗层数/回合数；⑤ 踏碎**确实不消耗回合**；
  ⑥ 踢飞跳位与击退方向正确；⑦ 穿腹流血量＝该次伤害；⑧ 即刻处刑三段换出莱瓦汀且力量不足时命中率不掉；
  ⑨ 加倍清算 +1/+2/+3 各项；⑩ 安卡复活后按钮仍在、读档后按钮仍在。
- **遗留**：`ExecutionAftermath` 暂借 `BuffIndicator.GRUDGE_MARK` 当图标（未单独绘制帧），后续需补一张专属帧；
  第二个专精「背叛家人者」的 hero 图标也得先给 hero_icons.png **向下加高一行**（126 已是末槽）。

---

# 2026-09-16（二）忠义巡礼者：修按钮冲突 + 踏碎/踢飞调参 + 两个新专精天赋

本轮是对上一节的修正与扩展：一处回归修复（切换武器按钮被挤掉）、两处手感调参、
以及两个新专精天赋「如数奉还」「永志不忘」，同时把怨恨标记从"永久"改成"20 回合"。

## 一、修复：转职后「切换主副」按钮消失

**现象**：转职忠义巡礼者后，右下角封印之剑的「切换主副」按钮不再出现。

**根因**（不是玄学，是两条相反的占位策略撞车）：

| 类 | `act()` 的占位策略 | 结果 |
|---|---|---|
| `VengeanceArts` | 每回合 `if (action != this) setAction(this)`——**无条件抢回** | 槽位永远归它 |
| `SealedSwordBase.SwordSwap` | `if (action == null) setAction(this)`——**只在空槽时占位** | 永远抢不到 |

`ActionIndicator` 全局只有**一个**槽位（`GameScene` 里只 new 了一个 `ActionIndicator`），
所以后者被判"死刑"——而且它不报错、不崩，只是安静地再也不占位。

**修法**：不能靠"让位"解决（技艺按钮必须常时显示，让来让去两边都闪），改成**把被挤掉的功能补进自己的窗口**：
`WndVengeanceSkill` 底部新增一条 **「切换主副（%1$s → %2$s）」** 按钮，只在
`VengeanceArts.canSwapWeapon()`（＝`SealedSwordBase.swordInHands(hero) != null`）成立时出现，
点击调 `arts.swapWeapon()` → `SealedSwordBase.swordInHands(hero).swapHands(hero)`。
于是这条入口跟着技艺列表一起常驻，回合代价 / 音画 / 快捷键全部沿用原实现（`swapHands` 内部自带）。

`VengeanceArts` 新增两个方法（`canSwapWeapon()` / `swapWeapon()`）与两个消息键
（`windows.wndvengeanceskill.swap` / `.swap_empty`）。

## 二、数值/手感调整

| 项 | 旧 | 新 |
|---|---|---|
| 踏碎伤害区间 | `3+S ~ 15+2S` | **`S-5 ~ S+15`**（`STOMP_MIN_BASE=-5`，`STOMP_MIN_PER_STR=1`，`STOMP_MAX_BASE=15`，`STOMP_MAX_PER_STR=1`） |
| 踢飞落点判定 | 只认 `leapCell`（英勇之跃弹道碰撞点），**已相邻时弹道退格至 0 ⇒ 恒返回 -1 ⇒ 报"没有空地"** | 三级放宽 `kickDest()`，见下 |

`kickDest(hero, enemy)` 的三级顺序：

1. **已经贴着目标**（`distance <= 1`）→ 直接返回 `hero.pos`：不跳、不移动、不检查「定身」，
   原地起脚直接结算（这条同时修掉了"站位太近反而放不出"的荒谬 bug）；
2. `leapCell` 的弹道碰撞点（保持原有英勇之跃手感）；
3. **兜底**：在目标周围八格里挑一格「`passable` 且 `Actor.findChar == null`」的空地，取离英雄最近的。

配套：`doKick` 拆成"结算体 `strike`（攻击 + 击退八格 + 加倍清算麻痹 + 收回合）+ 可选的前置跳跃"，
`dest == hero.pos` 时直接 `strike.call()`；`useMove` 里的「定身」检查也改成 `dest != hero.pos` 才判。

## 三、怨恨标记改为 20 回合（含"无限"扩展点）

- `GrudgeMark` 由 `extends Buff` 改为 **`extends FlavourBuff`**（`Buff.affect(ch, cls, dur)` 的泛型上限就是
  `FlavourBuff`，继承 `Buff` 传时长会编译失败），新增 `DURATION = 20f`。
- 叠加时用 **`Buff.prolong`（只延不缩）** 刷新计时，不用 `Buff.affect(..., dur)`（那是 `spend` 累加，
  反复挨打会把计时器叠到几十回合）。
- `act()` 覆写：条件满足时 `spend(DURATION); return true;`（不 detach＝永久），否则 `detach(); return true;`。
- 描述模板加第三个参数：`actors.buffs.grudgemark.desc(层数, 上限, 剩余回合文本)`，
  「无限」走独立键 `actors.buffs.grudgemark.forever`。

## 四、新专精天赋（T3，与「加倍清算！」同层）

| 枚举 | 图标（talent_icons.png 第 11 行） | 点数 | 中文 |
|---|---|---|---|
| `FULL_REPAYMENT` | 13 列 = **332** | 3 | 如数奉还 |
| `EVERLASTING_GRUDGE` | 14 列 = **333** | 3 | 永志不忘 |

`Talent.initSubclassTalents` 的 `case LOYAL_PILGRIM` 已放满三个槽位
（`DOUBLE_RECKONING` / `FULL_REPAYMENT` / `EVERLASTING_GRUDGE`）。

### 4.1 如数奉还（敌人挨得越重，标记叠得越多）

判定全在 `VengeanceArts.extraMarks(Hero, int damage)` 一处：

| 点数 | 规则 |
|---|---|
| +1 | `damage > 10` → 额外 +1 层 |
| +2 | `damage > 5` → 额外 +1 层 |
| +3 | `damage > 5` → +1 层；`damage > 10` → **改为 +2 层**（两条是"改为"，不叠加） |

**伤害取自哪**：`Char.attack` 命中分支里的 `effectiveDamage`（交给 `enemy.damage` 的那个值，
已含护甲减免 / 易伤 / 攻防 proc），作为新参数传给 `VengeanceArts.onAttackLanded(this, enemy, damage)`。
没有另做 HP 前后快照——那样会把护盾吸收、溢出击杀算成不同的数。

阈值常量：`REPAYMENT_HIGH_DAMAGE = 10`、`REPAYMENT_LOW_DAMAGE = 5`。

### 4.2 永志不忘（标记不散 / 击杀补充能 / 标记即视野）

| 点数 | 效果 | 实现位置 |
|---|---|---|
| +1 | 怨恨标记持续时间**无限** | `VengeanceArts.marksLastForever()` ← `GrudgeMark.act()` |
| +2 | 击杀带标记单位额外恢复 **5%** 复仇账簿充能 | `VengeanceArts.onEnemyKilled()` ← `Mob.die()` 英雄击杀分支（`super.die()` 之前） |
| +3 | 获得**所有**带标记单位的**灵视感知** | `Level.updateFieldOfView` 里英雄段的 `heroMindFov` |

- +1 的判定点读 `Dungeon.hero` 的天赋（英雄是单例），不需要把点数复制到每个敌人身上。
- +2 复用 `RevengeLedger.chargeByTalent(hero, 5f)`（与「记录一餐」+8%/15% 同一入口，5f＝5 个百分点）。
- +3 **不用**给英雄挂 `MindVision`（那会把整层怪都显出来），也**不自己**往 `hero.mindVisionEnemies` 里塞
  （那个列表每次 FOV 重算都会被 `clear()`），而是照 `TalismanOfForesight.CharAwareness` 的既有写法
  直接往 `heroMindFov` 写该怪所在格 + `NEIGHBOURS9`。

## 五、本轮改动文件

| # | 文件 | 改动 |
|---|---|---|
| 1 | `actors/buffs/VengeanceArts.java` | 踏碎常量、`kickDest`、`doKick` 重构、`onAttackLanded` 加 damage 参、新增 `extraMarks` / `marksLastForever` / `onEnemyKilled` / `canSwapWeapon` / `swapWeapon` / `talentPoints` |
| 2 | `actors/buffs/GrudgeMark.java` | 改 `extends FlavourBuff`、`DURATION=20f`、`gain` 用 `prolong`、`act()` 覆写、`desc` 加第 3 参、`turnText()` |
| 3 | `windows/WndVengeanceSkill.java` | 底部「切换主副」按钮 + `swapLabel()` |
| 4 | `actors/hero/Talent.java` | 新增 `FULL_REPAYMENT(332,3)` / `EVERLASTING_GRUDGE(333,3)`；`case LOYAL_PILGRIM` 补满三槽 |
| 5 | `actors/Char.java` | 命中钩子改为传 `effectiveDamage` |
| 6 | `actors/mobs/Mob.java` | 英雄击杀分支加 `VengeanceArts.onEnemyKilled(Dungeon.hero, this)` + import |
| 7 | `levels/Level.java` | `heroMindFov` 加「带标记单位灵视感知」段 + import |

文本：`actors_zh` / `actors` 各 +4 键（`talent.full_repayment.*`、`talent.everlasting_grudge.*`、`buffs.grudgemark.forever`），
改 4 键（`grudgemark.desc` 加第三参、`vengeancearts.desc` 补切换入口说明、`$move.stomp.desc` 改数值、`$move.kick.desc` 补"原地起脚"）；
`windows_zh` / `windows` 各 +2 键（`wndvengeanceskill.swap` / `.swap_empty`）。

## 六、核验（源码级，未代跑 Gradle）

- `javac -nowarn -proc:none -encoding UTF-8`（本轮 7 个文件 + `-sourcepath` 连带编译）**EXIT=0，0 错误**。
- properties：`actors_zh` 1892 键 / `actors` 1713 键 / `windows_zh` 394 键 / `windows` 393 键，**四份 0 重复键**；
  新增/改动键 zh/en 全在（**0 MISSING**）；带参文本按真实参数类型实跑 `String.format` 全过
  （`grudgemark.desc(Integer,Integer,String)`、窗口 `swap(String,String)` 等）。
- **待游戏内验证**：① 转职后打开技艺列表，底部出现「切换主副（当前主手 ⇄ 副手）」，点了确实换；
  ② 踏碎伤害落在 S-5 ~ S+15；③ **贴脸站在敌人旁边用踢飞**——不再报"没有空地"，原地起脚并把周围敌人击退；
  ④ 目标被围死时能从八格兜底落点跳过去；⑤ 怨恨标记 20 回合后消失、重复挨打只刷新不叠时长；
  ⑥ 如数奉还 +1/+2/+3 三档阈值（可配合高伤敌人测）；⑦ 永志不忘 +1 标记不再消失、+2 击杀有账簿时涨充能、
  +3 隔着墙/黑暗也能看到带标记的怪。
- **遗留**：仍有两处待补——`ExecutionAftermath` 的专属 buff 帧；
  `Talent` 新图标 332/333（`talent_icons.png` 第 11 行 13/14 列）尚未绘制，目前显示为图内空白。

---

# 2026-09-16（三）ActionIndicator 双槽位：切换武器按钮不再并入技艺窗口

> **本节的结论取代上面「2026-09-16（二）」里「把切换主副并入技艺列表」的做法**（那节保留为历史记录）。

## 一、需求

上一版为了解决「技艺按钮常驻 ⇒ 封印之剑的切换武器按钮被挤掉」，把切换入口做成了技艺窗口底部的一条按钮。
用户明确要求**改回两个独立按钮、同时显示**，而不是并进同一个窗口。

## 二、根因回顾

`ActionIndicator` 原本是**全局唯一槽位**：`action` 静态字段 + 一个 Tag。

| 类 | 原 `act()` 策略 | 结果 |
|---|---|---|
| `VengeanceArts`（复仇技艺，槽位 0） | 每回合无条件 `setAction(this)` | 槽位永远归它 |
| `SealedSwordBase.SwordSwap`（切换主副） | 只在 `action == null` 时占位 | 永远抢不到，**静默失效** |

## 三、实现：给 `ActionIndicator` 开第二个槽位

`ui/ActionIndicator.java`（重写）：

| 项 | 主槽（0） | 副槽（1） |
|---|---|---|
| 静态动作字段 | `action`（**原版语义不变**） | `secondAction`（新增） |
| 界面实例 | `instance` | `secondInstance`（新增） |
| 占位 / 回收 | `setAction` / `clearAction(a)` | `setSecondAction` / 同样走 `clearAction(a)` |
| 重建 | `refresh()` | `refreshSecond()`；`refreshAll()` = 两个一起 |
| 按键 | `SPDAction.TAG_ACTION`（X，原样） | `SPDAction.TAG_ACTION_2`（**新增，默认不绑键**） |

- 构造器 `ActionIndicator(int slot)`（无参重载 = 槽位 0），实例内用 `private final int slot` 区分，
  `update()`/`onClick()`/`hoverText()` 统一读 `current()`（＝本槽位的动作）。
- `destroy()` 只清自己那一个实例引用；`clearAction()`（无参）两个槽一起清（新开一局 / 重开场景用）。
- **向后兼容**：主槽的字段与方法语义与原来**完全一致**，原版 20+ 处 `ActionIndicator.setAction/clearAction/refresh` 一行未改。

`scenes/GameScene.java`（4 处）：

1. 字段 `private ActionIndicator action2;`；
2. `create()` 里 `action2 = new ActionIndicator(1); action2.camera = uiCamera; add(action2);`
3. `updateGame()` 的 `tagAction2` 三处（`updateTags` 分支、else-if 比较、`tagAppearing` 判定）；
4. `layoutTags()` 里主按钮之后、`resume` 之前插一段 `if (scene.tagAction2){ ... pos = scene.action2.top(); }`。

`items/weapon/melee/SealedSwordBase.java`：`SwordSwap` 全面改走副槽——`act()` / `attachTo` / `fx` 都用
`setSecondAction(this)`（先判 `secondAction != this`，避免每回合重建按钮）；`refreshIfHandsChanged()` 判
`secondAction == this` 后 `refreshSecond()`；形态切换 / `swapHands` / 解封处原先的 `ActionIndicator.refresh()`
统一换成 **`ActionIndicator.refreshAll()`**（两只手的武器都变了，两个按钮画的物品都不作数）。

`windows/WndVengeanceSkill.java`：**删掉**底部那条「切换主副」按钮与 `swapLabel()`，类注释同步改写；
`VengeanceArts` 里的 `canSwapWeapon()` / `swapWeapon()` 两个代劳方法一并删除（已无调用者）。

`SPDAction.java`：新增 `TAG_ACTION_2 = new SPDAction("tag_action_2")`，**不加默认绑定**。
（`GameAction.code` 是构造顺序下标，`WndKeyBindings` 只用它过滤 `NONE`（`code < 1`），
所以**中间插常量是安全的**；无默认绑定的动作会被排到键位列表末尾。）

## 四、按键冲突（要点）

`Button` 的按键监听是「`KeyBindings.getActionForKey(event) == keyAction()` 就触发」，
两个 Tag 若都用 `TAG_ACTION`，**按一次 X 会同时开技艺列表 + 换武器**。
故副槽必须用自己的 `GameAction`；不绑键时 `getActionForKey` 返回 `GameAction.NONE`（永不相等，
`null == NONE` 也为 false），既不误触发也画不出按键提示。

## 五、文本

- `windows.properties` / `windows_zh.properties`：`windows.wndkeybindings.tag_action_2`（`Swap Weapon Hands` / `切换武器主副`）——
  键位绑定列表里的名字。**只加 base 与 zh**：`I18NBundle` 的父链保证"语言文件缺键 → 回落英文 base"。
- `actors*.properties`：`vengeancearts.desc` 末段改写为「两个按钮在右下角同时显示，互不挤占」。
- 删除 `windows.wndvengeanceskill.swap` / `.swap_empty`（zh/en 各 2 条，已无引用）。

## 六、验证

- `javac -nowarn -proc:none -encoding UTF-8 -sourcepath core/src/main/java` 编 6 个文件（`SPDAction` /
  `ActionIndicator` / `GameScene` / `SealedSwordBase` / `WndVengeanceSkill` / `VengeanceArts`）**EXIT=0，0 错误**。
  （注意：编 `GameScene.java` 需要额外把 **gdx-controllers-core-2.2.4.jar** 加进 classpath，否则报
  「无法访问 `ControllerListener`」。）
- 括号配平：6 个文件 brace/paren/bracket **全 0**、无 U+FFFD。
- properties：`actors_zh` 1892 / `actors` 1713 / `windows_zh` 392 / `windows` 391，**四份 0 重复键、0 格式错误**。
- **待游戏内验证**：① 拿封印之剑后右下角**同时**出现技艺按钮与「切换主副」按钮（上下两行）；
  ② 点各自按钮互不干扰；③ 主副对调后面两个按钮的图标都跟着换；④ 按 X 只开技艺列表、不会顺手换武器；
  ⑤ 设置里的键位列表底部出现「切换武器主副」，可自行绑键。

## 七、已知取舍

- 竖列现在最多 5 个 Tag（攻击 / 拾取 / 技艺 / 切换 / 继续），比原版多 24px；极小屏（横屏虚拟高度 160）下
  5 个同时出现时会顶到顶部状态栏区域。实测极少同时出现，先记着。
- `ActionIndicator.SLOTS = 2` 只是给后来者看的"一共几个槽位"的常量，`GameScene` 目前是显式建两个实例。

---

# 2026-09-16（四）忠义巡礼者：踏碎改单体 + 踢飞描述去掉"英勇之跃"

继上一节把「切换主副」独立成副槽位之后的两处调整，都是**表现面/文案面**的收敛，不新增机制。

## 一、踏碎：范围伤害 → 单体伤害

| | 旧 | 新 |
|---|---|---|
| 结算对象 | 英雄周围**八格内的全部敌人** | **只有被选中的那一个**（`useMoveOnTarget` 传进来的 `enemy`） |
| 距离要求 | `distance(hero, enemy) <= 1`（不变） | 同上 |
| 特效 | `sprite.operate` + `BlastWave.blast(hero.pos)` + `PixelScene.shake` + `bloodBurstA` | **全部保留**（纯演出，不再带伤害） |

- 签名由 `doStomp(Hero, int)` 改为 **`doStomp(Hero, Char enemy, int)`**，`switch` 里的调用点同步改成
  `case STOMP: doStomp( hero, enemy, points ); break;`。
- 循环体展开成单目标结算：`int dmg = Hero.heroDamageIntRange(S-5, S+15); dmg -= enemy.drRoll();`
  → `dmg > 0` 走 `enemy.damage(dmg, hero)` + 血花，否则 `enemy.sprite.flash()`。
- 「加倍清算！」+1 的 5 回合易伤同样只对 `enemy` 生效（`doStomp` 内 `enemy.isAlive()` 判定）。
- 类 javadoc 的招式表、`Move.STOMP` 的枚举注释、`case STOMP` 的距离注释一并改写。
- **文案**：`actors.buffs.vengeancearts$move.stomp.desc`（zh/en）与转职长描述
  `actors.hero.herosubclass.loyal_pilgrim_desc`（zh/en）里的「周围八格内所有敌人 / every enemy in the eight
  cells around you」都改成「选中的目标 / the chosen target」。

> 仍保留的不变量：**踏碎不消耗回合**（不调 `hero.busy()` / `spendAndNext`）、伤害**不受武器攻击距离影响**。

## 二、踢飞描述：删掉「同英勇之跃」

- `actors.buffs.vengeancearts$move.kick.desc`（zh）：`_跳跃到它身边_（同英勇之跃），…` → `_跳跃到它身边_，…`
- `actors.properties` 对应键：`(as with Heroic Leap)` 整段删除。
- **代码注释里的 `HeroicLeap` 引用保留**（`kickDest` / `leapCell` 的 javadoc）——那是落点算法出处的实现注记，
  对后来者有指引价值；被删掉的只是**玩家可见的描述文字**。

## 三、验证

- `javac`（`VengeanceArts.java`，带 `-sourcepath`）**EXIT=0，0 错误**；括号配平 brace/paren/bracket **全 0**、无乱码。
- properties 四份 **0 重复键**（`actors_zh` 1892 / `actors` 1713 / `windows_zh` 393 / `windows` 392）；
  三个改动键按真实参数类型（`Integer`）实跑 `String.format` 全通过。
- **待游戏内验证**：① 站在两个敌人中间用踏碎，**只有选中的那个掉血**、旁边那个不受影响；
  ② 冲击波与震屏照旧；③ 仍不消耗回合；④ 技艺列表里踢飞描述不再出现「英勇之跃」。

---

# 2026-09-16（五）背叛家人者（第二个专精）+ 莱瓦汀系列改造

本批把中指长兄的**第二个转职分支**做实，并顺手把**莱瓦汀系列的成长方式**从"升级卷轴"改成"随英雄等级"。
一句话概括四件事：**剑不再吃卷轴、但跟着英雄长；火伤不再顶着近战图标；新分支用账簿充能换力量；三个天赋围绕莱瓦汀展开。**

## 一、莱瓦汀系列：不可用升级卷轴强化，但随英雄等级成长（照灵能弓）

需求原文：「使得莱瓦汀系列武器无法被升级卷轴选中并升级，但是会随角色等级而升级，参考女猎人的灵能弓；
并且使得莱瓦汀不会随着等级上升而降低力量需求。」

| 点 | 旧 | 新 |
|---|---|---|
| 卷轴强化 | `isUpgradable()` 继承默认 `true` | **`false`**（`ScrollOfUpgrade.usableOnItem` 直接返回它） |
| 等级来源 | 物品等级字段（默认 0） | **`Dungeon.hero.lvl / 5`**（`HERO_LEVELS_PER_UPGRADE`），照 `SpiritBow.level()` |
| 力量需求（无参） | `STRReq(level())` ⇒ **随等级一路下降** | `STRReq(7, 0)` 固定 **22**，**刻意忽略传入的 lvl** |

- 落点全在 `SealedSwordBase`（四个形态共用）：
  - `level()` / `buffedLvl()`：前者是"随英雄成长"的基础等级，后者再叠「拆开包装」的额外等级
    （`KindOfWeapon.min()/max()` 取的是 `buffedLvl()`，所以额外等级只影响掷值区间，**不写回真实等级**——
    否则每次形态切换都会被 `copyState` 固化一次，等级雪崩）。
  - `isUpgradable() = false`：这一处就把升级卷轴挡死，不需要改 `ScrollOfUpgrade`。
  - `STRReq(int lvl)`：**传死 0**。⚠️ 这是本批最容易踩的坑——`Weapon.STRReq()` 是 `STRReq(level())`，
    一旦 `level()` 开始随英雄等级上涨，"每 +1/+3/+6/+10 减 1"的常规减免就会让力量需求一路掉到个位数，
    等于白送。所以这里必须忽略形参，只保留「肌肉记忆合剂 -2」与「得心应手 -2/-4/-6」两条下调途径。
- **面板一览**（`L = 英雄等级 ÷ 5`；莱瓦汀比其余三形态高 1 点下限、5 点上限/级）：

| 英雄等级 | L | 封印 / 一阶段 / 二阶段（6+L ~ 35+7L） | 莱瓦汀（7+L ~ 40+8L） |
|---|---|---|---|
| 1 | 0 | 6 ~ 35 | 7 ~ 40 |
| 10 | 2 | 8 ~ 49 | 9 ~ 56 |
| 20 | 4 | 10 ~ 63 | 11 ~ 72 |
| 30 | 6 | 12 ~ 77 | 13 ~ 88 |

> 对照原版六阶满强化（+10）＝ 35+70 = **105**，所以这条成长线**永远越不过一把养满的普通六阶剑**；
> 它换来的是"不吃卷轴也一直能用"。想调慢/调快只改 `HERO_LEVELS_PER_UPGRADE`（默认 5）。
- **玩家可见文案**：新增 `items.weapon.melee.sealedswordbase.level_note`（zh/en），
  由 `SealedSwordBase.info()` 统一追加。**不逐形态改 `stats_desc`**——那句话四个形态完全一样，
  写四遍迟早"有的改了、有的没改"。

## 二、修复：附加火伤顶着"近战伤害"小图标

- 根因：`Char.damage` 的伤害浮字图标按 **src 的运行时类**逐条判定。本系列的附加火伤是
  `defender.damage(burn, this)`——**src 是武器本身**（照 `ResinCoatingBuff.bonusDamage` 的惯例），
  而主伤害的 src 是英雄本人。于是主伤害走物理图标（正确），附加火伤**落进默认分支**也画成物理图标（错误）。
- 修法：在 `Char.damage` 的图标表里补一条
  `if (src instanceof SealedSwordBase) icon = FloatingText.BURNING;`
  （紧挨着既有的 `DimDusk.FlameStrike` 那条）。
- **刻意不用 `Melting.isFireDamage(src)` 表**：那张表是**数值口径**（融化增伤用），列了烈焰法杖等
  "仍按魔法显示"的来源；两处判据分开维护，避免改一个显示把数值口径带偏（`Melting` 的 javadoc 已注明）。

## 三、新转职「背叛家人者」的接入清单

| 文件 | 改动 |
|---|---|
| `ui/HeroIcon.java` | 新增 `FAMILY_BETRAYER = 128`（`hero_icons.png` 已加高到 128×272＝8 列×17 行；127 仍是 `NONE`） |
| `actors/hero/HeroSubClass.java` | 新增枚举常量 `FAMILY_BETRAYER(HeroIcon.FAMILY_BETRAYER)` |
| `actors/hero/HeroClass.java` | `MIDDLE_FINGER` 第二槽位由 `GLADIATOR`（占位）换成 `FAMILY_BETRAYER` |
| `items/TengusMask.java` | **不需要挂常驻 buff**（特性全靠攻击钩子驱动、也没有右下角按钮）；留注释说明，免得后人以为漏了 |
| `actors/hero/Talent.java` | `initSubclassTalents` 新增 `case FAMILY_BETRAYER:`（三个槽位：得心应手 / 融化而死 / 拆开包装） |

## 四、分支特色：预支账簿充能，换一次「刚好挥得动」的力量

新增 `actors/buffs/FamilyBetrayal.java`（顶层 `FlavourBuff`）：

- **触发**：`Talent.onHeroAttackStarted`（`Char.attack` 里、**命中判定与 `damageRoll` 之前**）→
  非仇怨状态 + 手持莱瓦汀系列 + `STRReq() > hero.STR()` ⇒ 消耗 **5%**（`CHARGE_COST`）账簿充能，
  挂上补足 `STRReq() - hero.STR()` 点力量的 buff。
- **解除**：`Talent.onHeroAttackResolved`（命中与落空**两条路都会走到**；只有"目标无敌、攻击根本没打出去"
  那条早退路径不走，所以那条路上**压根不收费**，见本文「八、补记」）→ 立刻 `detach`。
  不放这里就会变成"常驻力量"，不再是"仅一次攻击"。
- 取数在 `Hero.STR()`（全工程唯一力量汇聚点，与同帧的 `TempStrength` 并列一行）。
- **三个刻意的边界**（都写进了 javadoc）：
  1. 力量本来就够 → **一分不花、不挂 buff**；
  2. 「仇怨」期间 → **完全不触发**（那边已给 +12 力量，再收钱是双重付费，也正是需求里"如果没有进入仇怨状态"的含义）；
  3. 充能不足 5% → `RevengeLedger.consumeCharge` **整笔拒绝**（不部分扣款，避免"剩 1% 也能白嫖一次"）。
- 为此给 `RevengeLedger` 补了两个静态入口：`consumeCharge(Hero, int)`（只加不减 → 反向操作）与
  `rancorActive(Hero)`。
- 视觉反馈：触发时从身上炸一簇 `RancorParticle.RAGE`（与"仇怨"同源），并在 buff 条上按 `UPGRADE` 帧
  暗红着色闪一下（未自绘图标 ⇒ 按 `BuffIndicator` 的【惯例】借帧，不预留帧号）。

## 五、三个专精天赋（T3，图标 334/335/336，用户已绘制）

| 天赋 | 图标 | 效果 | 唯一取点 |
|---|---|---|---|
| **得心应手** `EFFORTLESS_GRIP` | 334（下箭头） | 力量需求 **-2 / -4 / -6** | `SealedSwordBase.effortlessGripReduction` → `STRReq(int)` |
| **融化而死** `MELT_TO_DEATH` | 335（骷髅） | 系列攻击挂「融化」；带融化者**火焰伤害 +50/100/150%**；莱瓦汀火场内的敌人**直接进入融化** | `Melting.points()` |
| **拆开包装** `UNWRAP` | 336（上箭头） | +1 额外等级＝**形态序号**（封印 0 → 莱瓦汀 +3）；+2 另一手武器更高再 +1；+3 **取消解封条件限制** | `SealedSwordBase.unwrapLevelBonus` / `unwrapPoints` |

- 「拆开包装」的额外等级走 **`buffedLvl()`**：+3 是"每次解封都多一级"，所以莱瓦汀（stage 3）天然 +3，
  与用户给的示例一致；比较对象取**本剑当前的成长等级**。
- +3 的"取消限制"落在 `canReachStage()` 首行的提前 `return true`——**所有调用方都绕不过去**（`unsealInto` 也调它）。

## 六、融化（`Melting`）：一个状态 + 一张火焰来源表

- `actors/buffs/Melting.java`：`FlavourBuff`，**8 回合**、`Buff.prolong` 刷新（不叠加）、`announced`（首次弹名）、
  图标 `BuffIndicator.MELTING = 116`（用户已绘：黄骷髅＋红底）。
- **施加点两处**：① `SealedSwordBase.proc`（四个形态的命中，刻意放在"有没有火伤"判定**之前**——
  需求说的是"系列武器的攻击"，封印之剑也该上状态，只是它自己没火伤）；
  ② `Laevateinn.burnAura`（主手握莱瓦汀时，火场圈内的**敌人**每回合被拖进融化；只用距离判，与铺火同源）。
- **增伤唯一取点**：`Char.damage` 的 float 乘区里乘一次 `Melting.damageMultiplier(this, src)`——
  与"报复对象 +20%"同一位置，src 覆盖面最广（近战附加火伤 / 火场 / 点燃 / 松脂 / 烈焰法杖一次全管住）。
- `isFireDamage(src)` 是**本作火焰来源的唯一判定表**（`FIRE_SOURCES`，10 类），
  以后新增带火焰直伤的武器/法术记得补一条；**免疫火焰**的目标改为"一切伤害都按同比例提高"，
  否则这个状态对它们等于不存在。

## 七、核验（源码级，未代跑 Gradle）

- `javac`（13 个文件，带 `-sourcepath` + gdx/gdx-controllers jar）**EXIT=0，0 错误**（仅 unchecked 提示）。
- properties 四份：**0 重复键**；13 个新键 zh/en **全部成对**；`items...level_note` zh/en 就位。
- `java.util.Properties` **实装载** + 按真实参数类型 `String.format` **实跑**
  （`melting.desc` 传 `(int, String)`、`familybetrayal.desc` 传 `(int, String)`）全部通过，
  `\n` 转义与 `%%` 转义均正确。
- Pillow 逐帧核验：`talent_icons.png` 334/335/336 **已绘制**（337 起为空）、
  `hero_icons.png` 128 **已绘制**（127 透明）、`buffs.png`/`large_buffs.png` 帧 **116 已绘制**（117 起为空）。
- **待游戏内验证**：
  1. 转职界面第二项为「背叛家人者」且图标正确、天赋面板出现三个新天赋（不再白拿狂战士天赋）；
  2. 力量不足时挥剑会扣 5% 账簿充能、并**刚好**能挥动（精准/延迟不再受罚）；力量够了不扣；
     「仇怨」期间不扣；
  3. 升级卷轴选中这把剑时应被拒绝；
  4. 附加火伤的浮动数字应显示**火焰**小图标，主伤害仍是物理图标。

### 八、补记（同日复审：一处会白扣充能的漏洞）

复审 `Char.attack` 的控制流时发现：**「目标无敌」是一条不带收尾的早退路径**——
`enemy.isInvulnerable(getClass())` 为真时在第 407 行直接 `return false`，**不经过**
`Talent.onHeroAttackResolved`。而 `FamilyBetrayal` 的收费写在「攻击开始」（`onHeroAttackStarted`，
第 394 行）→ 结果是**玩家会白掉 5% 充能、这一击却根本没打出去**（对 DM-300 通电立柱护体、
各种 `isInvulnerable` 场景均会触发）。

- 修法：把 `enemy` 一并传进 `FamilyBetrayal.onAttackStarted(hero, enemy)`，**首行即判**
  `enemy == null || enemy.isInvulnerable(hero.getClass())` → 直接返回（判据与 `Char.attack`
  首段完全一致，不新增第二套规则）；既然没收费，也就不会挂 buff，自然无需摘除。
- 类注释同步写明「只有这条早退路径不收尾」，避免后来者照抄「命中与落空两条路都会走到」时漏掉它。
- 顺带统一 jargon：`SealedSwordBase.STRReq(int)` 的 javadoc 原写「力量药剂」，实际
  `masteryPotionBonus` 的唯一来源是**肌肉记忆合剂**（`PotionOfMastery`），改成正确物品名，
  与属性文本里的说法一致。
- 复审后重跑 `javac`（`FamilyBetrayal` / `Talent` / `SealedSwordBase` / `Char` 四文件，
  带 `-sourcepath` + gdx/gdx-controllers）**EXIT=0，0 错误**。
- 同时把两条通用教训写进 `AGENTS.md` §6：
  ①「照灵能弓覆写 `level()` ⇒ 力量需求跟着掉」（`Weapon.STRReq()` 是 `STRReq(level())`，必须传死 `(7,0)`）；
  ②「自定义武器附加火伤在飘字里画成近战图标」（`Char.damage` 图标块按 `src` 类型分支，需为自家武器补一行）；
  并把 §4「新增转职接入清单」里过时的帧数说明更新为 **8 列×17 行＝136 帧，129 起可用**。

### 九、平衡调整 + 文案风格统一（2026-09-16 深夜，出 v0.2.4）

#### 1. 预支充能 2% → **5%**（`FamilyBetrayal.CHARGE_COST`）
一次「预支力量」的代价从 2% 提到 5%，直接翻倍多。连带同步的位置共 4 处：
- 代码：`FamilyBetrayal.CHARGE_COST`（常量本身；javadoc 里全用 `{@value #CHARGE_COST}` 引用 ⇒ 自动跟随）、
  `Talent.onHeroAttackStarted` 的行内注释。
- 文本：`actors.hero.herosubclass.family_betrayer_short_desc` / `family_betrayer_desc`（zh + en 共 4 条）。
- 文档：本文第四节与「待游戏内验证」。
- **不受影响**：`RevengeLedger.RANCOR_DRAIN = 2`（那是「仇怨」每回合的**持续**消耗，与本条无关，别顺手一起改）。

#### 2. 描述文本风格统一：「只做功能描述，不写修饰句」
用户当晚把三条新天赋的**中文**改成「无铺垫、直接给功能」的写法，英文侧仍是旧样 ⇒ 按此基准把两侧拉平：

| 键 | 改动 |
|---|---|
| `effortless_grip.desc`（en） | 删掉开场句 *Wielding a blade well is a matter of familiarity, not of force.* |
| `melt_to_death.desc`（en） | 删掉开场句 *Whatever stands in Laevateinn's way ends up as a puddle.*，正文压成与 zh 同构 |
| `unwrap.desc`（en） | 删掉开场句 *Every layer of the seal that comes off leaves the blade a little keener.* 与结尾的实现细节脚注 |
| `effortless_grip.desc`（zh） | 首档标签 `+1：` 补成 `_+1：_`（另两档本来就是下划线包裹，不然样式不一致） |
| `melting.desc`（zh/en） | 删掉「这个单位正在莱瓦汀的烈焰里一点点化开：」/ *This creature is coming apart in Laevateinn's flames:* |
| `familybetrayal.desc`（zh/en） | 删掉「用账簿里的旧账换来的一次力量：」/ *Strength bought with old debts out of the ledger:* |
| `family_betrayer_desc`（en） | 删掉 *The one who turns his back on the family keeps no grudges on anyone's behalf…* 整段，以及 zh 里没有的「三个边界」段与「三个天赋」收尾段 |
| `family_betrayer_short_desc`（en） | 去掉 *burns the family ledger for power*，与 zh 对齐 |
| `everlasting_grudge.desc`（en） | 删掉开场句 *The Middle Finger never settles a debt on memory alone.* |

#### 3. 顺手揪出的真 bug：`/n/n` 不会换行
`grep -c "/n/" core/src/main/assets/messages/ -r` 在 `actors.properties` 命中 **9 处**，
全部是手掌/前二老板 T3 天赋（`wild_heart` / `shameful_heart` / `stubborn_heart` / `burst_finish` /
`lunge_stab` / `no_suspense` / `weakness_pierce` / `crush_bugs` / `glorious_triumph`），
写法是 `... Heart:/n/n_+1:_ ...`（**正斜杠**）。这类笔误**不报错**，只会在天赋面板里原样显示
`:/n/n_+1:_`，中文侧这 9 条本来就是正确的 `\n\n`。已全部改正，复查残留为 0；
并把「换行只认反斜杠 n」写进 `AGENTS.md` §2。

#### 4. 核验
- `javac`（`FamilyBetrayal` / `Talent`）**EXIT=0，0 错误**。
- `java.util.Properties` **实装载**四份文件通过（`\n` 转义合法、无非法格式串）；
  `String.format` 按真实参数类型实跑 `melting.desc` / `familybetrayal.desc` 通过
  （`_%1$d%%_` → `_150%_`，`剩余回合：5`）。
- 键数：zh-actors **1905** / en-actors **1726** / zh-items **2323** / en-items **2141**，0 重复键、0 缺键。
- 替换全部由带「命中数必须 == 1」断言的脚本执行（共 16 + 10 处），避免大文件手工替换漏改/误改。

#### 5. 版本
`build.gradle`：`appVersionCode 923 → 924`、`appVersionName '0.2.3' → '0.2.4'`
（沿用 `900 + minor*10 + patch` 编码：0.2.4 ⇒ 924）；`ios/robovm.properties` 同步（该文件为生成产物）。
产物归档 `EGOPD_0.2.4.APK`。

# 2026-09-17 中指长兄盔甲技能：槽位与图标接线（占位骨架）

## 一、起因与现状排查
用户要求：开始做中指 长兄的盔甲技能，**先「检查现有套用 + 把图标配置正确」**，设计要求随后再给。

- `HeroClass.armorAbilities()` 只有 6 个原版职业 + ORACLE / RING_MASTER / VALENCINA 四个显式 case，
  **MIDDLE_FINGER 落到 `case WARRIOR: default:`** ⇒ 中指 长兄的「选择盔甲技能」窗口与动作栏里显示的是
  战士的三个技能（英勇之跃 / 震荡波 / 坚忍），图标也就是**战士的帧 16 / 17 / 18**，这就是要修的「图标不对」。
- 图标来源链：`ArmorAbility.icon()`（基类默认返 `HeroIcon.NONE`=127）→ `new HeroIcon(ability)` →
  `hero_icons.png` 的 `TextureFilm`（16×16 分帧）。**图标只能由技能实例自己给出**，所以只要还在借用战士的实例，
  就不可能显示中指 长兄自己的图标 —— 必须先有属于本职业的技能类。
- 现图 `hero_icons.png` 为 **128×272（8 列 × 17 行 = 136 帧）**，逐帧扫描确认：
  126=忠义巡礼者、127=`NONE`（全透明）、128=背叛家人者均已画；**129~135 全透明可用**。

## 二、本次改动（只动图标与槽位，不改行为）
| 文件 | 改动 |
|---|---|
| 新增 `actors/hero/abilities/middlefinger/MiddleFingerAbilityOne/Two/Three.java` | 三个占位技能类，分别 `extends HeroicLeap / Shockwave / Endure`，**只覆写 `icon()`** |
| `ui/HeroIcon.java` | 新增 `MIDDLE_FINGER_ABILITY_1/2/3 = 129/130/131`（**第 17 行第 2/3/4 列**，帧号=(行-1)×8+(列-1)），注明图待绘制、名待定 |
| `actors/hero/HeroClass.java` | 新增 `case MIDDLE_FINGER:` 返回上述三个实例（+3 个 import），不再落 `default` 借战士 |
| `items/armor/MiddleFingerCoat.java` | javadoc 由「行为完全复用战士」改述为「已有独立槽位与图标，行为仍暂沿用战士」 |
| `actors/actors_zh.properties` / `actors.properties` | 各 +9 键：`actors.hero.abilities.middlefinger.<技能类名小写>.{name,short_desc,desc}`，文案明写「占位」 |

**为什么保留继承、而不是留空**：留空会让中指 长兄在等设计期间**丢掉现有可用的技能**（功能回归）；继承战士只是
行为的临时来源，`talents()` 也仍是战士的盔甲天赋，行为与天赋保持一致。设计要求到位后需**一次做完三件事**：
①重写 `activate()`（并解除对战士技能的继承）；②改名（文件名 / `HeroIcon` 常量名 / zh+en 文本键三处同步）；
③按设计写出 `talents()` 并补齐天赋三重注册。

## 三、核验
- `javac` 六文件（三个新技能 + `HeroClass` + `HeroIcon` + `MiddleFingerCoat`，`-sourcepath` + gdx/gdx-controllers jar）
  **EXIT=0、0 错误**，产物含 `MiddleFingerAbilityOne/Two/Three.class`；括号配平 0、无 U+FFFD。
- 文本：`actors_zh.properties` **1914** 键 / `actors.properties` **1735** 键（各 +9），**0 重复键**，18 个新键逐条实测存在。
- Pillow 逐帧：帧 129/130/131 当前**全透明**（预期：等用户绘制）。

## 四、待办（等盔甲技能设计要求）
1. 三个技能的名称与行为；图标需绘制到帧 **129 / 130 / 131**（第 17 行第 2/3/4 列）。
2. 每个技能的 3 个分支天赋：Talent 枚举（4 点档 + 图标）+ `icon()` 的 `HEROIC_ENERGY` 分支 case +
   职业/转职两套层级 switch 的 T4 case + 在对应 `talents()` 数组里声明。

## 五、新增通用经验（已记入 `AGENTS.md` §4「新增盔甲技能接入清单」）
- `armorAbilities()` 的 switch **有 `case WARRIOR: default:` 兜底 ⇒ 漏加 case 不报错**，只会静默把别的职业的
  技能与图标发给新职业（本次症状）。凡是「图标不是自己的」，先查这里。
- `Messages` 取键为「类名去掉 `com.shatteredpixel.shatteredpixeldungeon.` 前缀后全小写」，且**缺键会沿父类链回落**
  ⇒ 用「继承别的职业的技能」做占位时，会静默显示父类的名字，必须自带文本键才看得出是占位。
- `hero.armorAbility` 按**类名**存档 ⇒ 给已有存档的玩家**改名或删类会让存档读不回来**（占位类改名时机要留意）。

# 2026-09-17（二）中指长兄：两个文本修复 + 盔甲技能其一「咬紧牙关」/ 其二「永不遗忘」

> 承接上一节的占位骨架。本轮把「其三」之外的**两个技能**按设计要求定名并实装，并先修掉用户报的两个文本 bug。

## 〇、先修两个文本 bug

### 1. 「切换武器的冷却」buff 文本缺失

| 项 | 内容 |
|---|---|
| 症状 | T2「趁手玩具」免费切换主副武器后，buff 栏出现冷却图标，但**名称与描述都是空的** |
| 根因 | `Talent.HandyToyCooldown extends FlavourBuff`（`icon()` 返 `BuffIndicator.TIME` ⇒ 会进 buff 栏），但**文本键一个都没写** |
| 取键 | 嵌套类 ⇒ `Talent$HandyToyCooldown` 去前缀后全小写 = `actors.hero.talent$handytoycooldown.{name,desc}`（注意是 **`$`** 不是 `.`）|
| 修法 | zh/en 各补 2 键，句式照既有的「剩余 X 回合」。`FlavourBuff.desc()` 会传 `dispTurns()`（一个 `String`）⇒ 描述里留 `%s` |

### 2. 融化（`Melting`）buff 描述乱码

| 项 | 内容 |
|---|---|
| 症状 | 描述里「受到的伤害增加 X%」与「剩余回合」显示成 `%1$d%%` / `%2$s` 这样的**格式串原文** |
| 根因 | `Melting.desc()` 只传了 **1** 个实参（百分比），而文本需要 `%1$d` + `%2$s` **两个** ⇒ `String.format` 抛 `MissingFormatArgumentException`，被 `Messages.format` 兜住后**原样返回格式串**——这就是「乱码」的真身 |
| 修法 | 补上第二个实参 `dispTurns()` |

```java
public String desc(){
    return Messages.get( this, "desc",
            (int)(BONUS_PER_POINT * 100 * Math.max( 1, points() )),
            dispTurns() );
}
```

**通用教训（已记入 `AGENTS.md`）**：`Messages.get` 只在 **`args.length > 0`** 时才走 `String.format`（`Messages.java:135`）。
⇒ ①**不带参**的文本里写裸 `%`（如天赋描述里的 `_+100%_`）**是安全的**，不必写成 `%%`；②**一旦带参，每个 `%` 都必须能被实参消化**，否则**整串回退成原文**——不报错、不崩溃，只是显示乱码，属最难查的一类。

## 一、「咬紧牙关」（`GritTeeth`）—— 40 充能 / 0 血不死

### 1. 机制

- `baseChargeUse = 40f`。施放后给英雄挂 `GritTeethBuff`，基础 **5 回合**（`Talent.GRIT_TEETH_BASE_DURATION`）；重复施放走 `Buff.prolong`（**只延不缩**，并把「已被射线击穿」的标记清掉）。
- 免死的**唯一汇聚点**是 `Hero.isAlive()`：新增缓存字段 `gritTeeth`（写法照抄既有的 `berserk` 字段）——`HP <= 0` 时若 `gritTeeth.undying()` 为真就 `return true`；`HP > 0` 时两个缓存一起清空。挂在 `isAlive()` 而不是 `Hero.damage` 上，「不触发死亡」的所有下游（能否行动、死亡结算、任务失败）**自动一致**。
- **「解离射线」击穿免死**：`GritTeethBuff.checkRayBypass(this, src)`，插在 `Char.damage` 的 `if (HP < 0) HP = 0;` 与 `if (!isAlive()) die(src);` **之间**——只有这一刻才判得出「这一击是否致命」，也才来得及在 `isAlive()` 被问起之前把免死作废。射线来源收成**一张类表**（`RAY_SOURCES`），只放 `Eye.DeathGaze` 一条就够：邪能魔眼的射线与最终 boss `YogDzewa` 的死亡射线**都是** `new Eye.DeathGaze()`。
- **到期收尾**（`act()`，调度器只会调一次）：先 `bypassed = true` → 结算「绝境迫发」回血 → `detach()` → 判死。**顺序不能动**：`isAlive()` 拿着的是缓存引用，若先 `detach()` 再置位，读到的还是「免死」⇒ 英雄永远死不掉。

### 2. 三个 T4 天赋

| 天赋 | 图标索引 | 效果 | 取值点 |
|---|---|---|---|
| 野兽意志 `BEAST_WILL` | 337 | 持续时间 +3 / +5 / +10 回合（⇒ 8 / 10 / 15） | `Talent.gritTeethDuration(Hero)`，`activate()` 里取一次 |
| 濒亡狂怒 `NEAR_DEATH_FURY` | 338 | 0 血期间自己打出的伤害 +100% / +200% / +300% | `Talent.nearDeathFuryMultiplier(Hero)`，在 `Char.damage` 里**紧挨 `beastFuryMultiplier`、同一个 `isHeroDealtDamage` 判据内** |
| 绝境迫发 `DESPERATE_BURST` | 339 | 结束后回复 5% / 10% / 15% **最大生命**（向上取整，与「过人的毅力」同口径） | `Talent.gritTeethEndHeal(Hero)`，在 `GritTeethBuff.act()` 里取 |

## 二、「永不遗忘」（`NeverForget`）—— 70 充能 / 冲刺范围一击

### 1. 冲刺段（照抄「英勇之跃」`HeroicLeap`）

`targetingPrompt()` 给出提示语；`targetedPos()` 把光标吸附到真实落点（`STOP_SOLID|STOP_TARGET`）。`activate()` 沿 `Ballistica(STOP_TARGET|STOP_SOLID)` 走到碰撞点、再 `backTrace` 退到第一个没被别人占的格子，然后 `sprite.jump` → `move`/`occupyCell`/`observe`/`updateFog` → 回调里结算。**这一段与 `HeroicLeap` 逐行对齐**，只把落地后的动作换成本技能的范围一击。

### 2. 范围一击（`strike(Hero)`）

- 半径 `BASE_RADIUS(1) + pointsInTalent(MAKE_A_SCENE)`；判据是**欧氏距离 ≤ 半径 + 0.5**，所以：
  - 半径 1 ⇒ 恰好铺满 **3×3**（设计要求）
  - 「大闹一场吧」+1/+2/+3 ⇒ 半径 2/3/4 ⇒ **5×5 / 7×7 / 9×9 的圆形范围**（不是方块）
  - 范围**不判墙体遮挡**——设计要求就是「范围内的所有目标」。
- **「家人的心意」的生效前提是范围里真的有友方单位**（没有则整条天赋作废，`heartPoints` 取 0）。
- 每个目标各走一次 `hero.attack(ch, multi, 0f, 1f)` ⇒ 命中判定、护甲减伤、武器特效全部走原版那套，倍率只乘在 `damageRoll()` 的结果上。**副作用**：每个目标各触发一遍「攻击开始/命中/落空」钩子，与「对每个目标各发动一次攻击」的语义一致。

| 情形 | 基础 | 家人的心意 +1 | +2 | +3 |
|---|---|---|---|---|
| 范围内**无**友方 | 敌方 100% | —（天赋作废） | — | — |
| 范围内**有**友方 → 友方 | 100% | 200% | 300% | **必定击杀** |
| 范围内**有**友方 → 敌方 | 200% | 300% | 400% | 600% |

「必定击杀」照原版写法：`HP = 0` + 顺带 `detach` 蛮兵的 `BruteRage` + `die(hero)`（src 传英雄 ⇒ `Mob.die` 的击杀白名单认它，掉落/统计照常）。

### 3. 三个 T4 天赋

| 天赋 | 图标索引 | 效果 | 取值点 |
|---|---|---|---|
| 家人的心意 `FAMILY_HEART` | 340 | 见上表（**需范围内有友方**） | `strike()` 内的 `heartPoints` |
| 大闹一场吧 `MAKE_A_SCENE` | 341 | 范围 5×5 / 7×7 / 9×9 圆形 | `strike()` 的 `radius` |
| 我会牢记在心 `KEEP_IN_MIND` | 342 | 击杀了友方单位后获得 5 / 10 / 15 回合**神器充能** | `strike()` 末尾 ⇒ `Buff.affect(hero, ArtifactRecharge.class).set(5 * points)` |

> 「我会牢记在心」判「谁死了」必须在**攻击循环结束后**做（循环里读会读到「还没打」）：实现上先把友方引用收进 `allies`，循环走完再逐个 `!isAlive()`。

## 三、存档兼容：两个占位类改名

`hero.armorAbility` 是**按类名**存档的（`Bundle.put(Bundlable)`），还原时走 `Bundle` 的静态 `aliases` 表。本轮把占位类 `MiddleFingerAbilityOne` / `Two` 删掉、换成 `GritTeeth` / `NeverForget`，所以**必须在 `ShatteredPixelDungeon` 的构造函数里补两条别名**（紧挨已有的 `WornKey` 那条）：

```java
Bundle.addAlias( GritTeeth.class,   "...abilities.middlefinger.MiddleFingerAbilityOne" );
Bundle.addAlias( NeverForget.class, "...abilities.middlefinger.MiddleFingerAbilityTwo" );
```

漏了这一步，旧存档里那一格会读成 **null**（技能丢失）。`addAlias(Class<?> cl, String alias)` 就是把 `alias → cl.getName()` 写进静态表。

## 四、图标：借「怒气」帧 + 紫色滤镜

设计要求「咬紧牙关套用怒气图标 + 紫色滤镜」。为了**不再多占一帧**，给 `ArmorAbility` 加了通用的着色钩子：

| 文件 | 改动 |
|---|---|
| `actors/hero/abilities/ArmorAbility.java` | 新增 `public static final int NO_TINT = 0xFFFFFF` + `public int iconTint()`（默认 `NO_TINT` = 不加） |
| `ui/HeroIcon.java` | `HeroIcon(ArmorAbility)` 在 `frame()` 之后按通道归一化 `hardlight( r/255, g/255, b/255 )` |
| `GritTeeth.java` | `icon()` 返 `HeroIcon.BERSERK`(104)，`iconTint()` 返 `0xFF5AFF`（保留红蓝、压掉绿 = 紫） |
| `GritTeethBuff.java` | `icon()` 返 `BuffIndicator.BERSERK`，`tintIcon()` 走既有的 `hardlight(1f, 0.35f, 1f)` |

buff 侧的 `tintIcon` 是既有机制，技能侧的 `iconTint` 是本轮新增。**帧 129 因此没有调用方，但已随 `MIDDLE_FINGER_ABILITY_1` 占用，不要复用。**

## 五、天赋注册（T4 只有一处）

- 枚举：`Talent` 加 `BEAST_WILL(337,3)` / `NEAR_DEATH_FURY(338,3)` / `DESPERATE_BURST(339,3)` / `FAMILY_HEART(340,3)` / `MAKE_A_SCENE(341,3)` / `KEEP_IN_MIND(342,3)` —— `talent_icons.png` **第十一行**（320 起，每行 32 列）的第 18~23 列，**用户已绘制**；该职业的 `HEROIC_ENERGY`（第十一行第 27 列 = 346）此前已接好。
- **T4 天赋不需要任何层级 switch**：`Talent.initArmorTalents` 直接把 `ability.talents()` 塞进 `talents.get(3)`。⇒ 只要在 `GritTeeth.talents()` / `NeverForget.talents()` 里各声明「3 个分支天赋 + `HEROIC_ENERGY`」即可。
  （**这一条修正了 `AGENTS.md` §4 原来的写法**——它把「职业/转职两套 switch 的 T4 空 case」也算进了必需步骤，实际那两个 switch 只管到 `talents.get(2)`。）

## 六、本轮改动文件

| 文件 | 改动 |
|---|---|
| 新增 `actors/buffs/GritTeethBuff.java` | 顶层 `FlavourBuff`：免死标志 + 射线击穿 + 到期回血/判死 + store/restore |
| 新增 `actors/hero/abilities/middlefinger/GritTeeth.java` | 技能其一 |
| 新增 `actors/hero/abilities/middlefinger/NeverForget.java` | 技能其二 |
| 删除 `.../middlefinger/MiddleFingerAbilityOne.java`、`MiddleFingerAbilityTwo.java` | 占位类改名 |
| `.../middlefinger/MiddleFingerAbilityThree.java` | 仅更新 javadoc（仍是占位，行为沿用「坚忍」） |
| `actors/hero/HeroClass.java` | `armorAbilities()` 的 MIDDLE_FINGER case 改为 `{GritTeeth, NeverForget, MiddleFingerAbilityThree}` |
| `actors/buffs/Melting.java` | **修**：`desc()` 补第二个实参 `dispTurns()` |
| `actors/hero/Talent.java` | 6 个枚举常量 + 3 个静态取值方法（`HandyToyCooldown` 类本体早已存在，本轮只补它的文本） |
| `actors/Char.java` | `damage()` 插 `checkRayBypass`；乘区插 `nearDeathFuryMultiplier` |
| `actors/hero/Hero.java` | `isAlive()` 加 `gritTeeth` 缓存分支 |
| `actors/hero/abilities/ArmorAbility.java` | `NO_TINT` + `iconTint()` |
| `ui/HeroIcon.java` | `HeroIcon(ArmorAbility)` 施加 `iconTint()`；补 129/130/131 注释 |
| `ShatteredPixelDungeon.java` | 两条 `Bundle.addAlias`（占位类改名） |
| `actors/actors_zh.properties` / `actors.properties` | 本轮 29 个键 ×2 语言 |

## 七、核验（源码级，未代跑 Gradle）

- **javac**（`tools/jdk-21.0.12.1+1`）12 个改动文件：`-proc:none -encoding UTF-8 -nowarn -sourcepath core/src/main/java`，classpath 用 Windows 风格（`core/build/classes/java/main;SPD-classes/build/classes/java/main;<gdx-1.14.0.jar>`）⇒ **EXIT=0**，只有 unchecked 提示。
- **文本回归** `_chk/verify_armor.py`：29 个本轮键 × zh/en 双侧 ⇒ **重复键 0 / 缺失 0 / `/n` 误写 0 / 转义序列非法 0 / 带参键的占位符与 Java 实参逐一匹配**，`ALL PASS`。
  - 现键数：`actors_zh.properties` **1932**、`actors.properties` **1753**。
  - （**既有状态，非本轮引入**：zh 有 **179** 个键在 en 侧没有 —— 本作以 zh 为主、en 侧历来滞后。本轮新增键**都是双份**，上面那条 ALL PASS 就是这条的保证。）
- **乱码/配平**：12 个文件无 U+FFFD、`{}` 全配平（`Talent.java` 的 `(`/`)` 差 4 是 `//1)`~`//4)` 这种编号注释造成的假阳性，已逐行定位确认）。
- 逐条复核过的机制点：`Mob.die` 击杀白名单（`cause == Dungeon.hero` ⇒ 认）；`ArtifactRecharge.set(float)` 存在、用法与 `ScrollOfMysticalEnergy` 一致；`Buff.prolong` = `affect` + `postpone`（`postpone` 只延不缩）；`FlavourBuff.act()` 本体即 `detach(); return true;`（⇒ 证明 `act()` 只在到点响一次）；`ArmorAbility.desc()` 末尾拼的 `cost` 键走父类回落到 `abilities.armorability.cost`（不缺）；落地收尾 `target.die(this); if(!target.isAlive()) Dungeon.fail(this);` 与 `Berserk.act()` 逐字一致。

## 八、待办

1. 画图：`hero_icons.png` **帧 130**（永不忘记）、**帧 131**（其三占位）；帧 129 已随常量占用但无调用方（保留）。
2. ~~其三仍是占位（沿用「坚忍」+ 战士的盔甲天赋），设计到位后按 `MiddleFingerAbilityThree` 的 javadoc 三步走。~~
   → **当日已闭合**：见下一节（其三定名「即刻处刑[莱瓦汀]」并实装，占位类已删除）。
3. 游戏内实测（免死是否真的不触发死亡、射线是否真的击穿、冲刺落点与范围是否符合预期）——本轮只做源码级核验。

# 2026-09-17（三）中指长兄：盔甲技能其三「即刻处刑[莱瓦汀]」+ 三个专精天赋

> 承接上一节：其一「咬紧牙关」/ 其二「永不遗忘」同日实装，其三此前是**沿用战士「坚忍」的占位**。
> 本节把它按设计重写为 `InstantExecution`，占位类 `MiddleFingerAbilityThree` 已删除。至此中指长兄三个盔甲技能**全部实装**。

## 〇、设计需求（用户原文要点）

- **消耗充能 80%**。
- 中指长兄对敌人进行即刻处刑：进行**三次 30% 倍率**的攻击；**第三次攻击时击飞敌人 5 格**；随后**第四次攻击投出莱瓦汀追击**，并**跃至敌人身边**进行一次**基于力量值**的收尾攻击。
- 细节要求：**每次攻击都要播放攻击动画**；第四次「投出莱瓦汀追击」**不必改动背包内的武器状态**，直接实现「类似投掷武器的、投出**旋转的**莱瓦汀并击中敌人、并造成**基于莱瓦汀面板**的伤害」；收尾攻击数值为 **5+S ~ 15+3S**（S = 当前力量，**可计入仇怨状态的力量加成**），**最后一次攻击要有震击的特效**（类似此前复仇技能的实现）。
- 三个专精天赋：

  | 天赋 | +1 | +2 | +3 |
  |---|---|---|---|
  | 没有打开账簿的必要 | 立刻消耗复仇账簿**全部充能**，按消耗充能的 **0.5 倍**获得伤害加成 | **1 倍** | **1.5 倍** |
  | 好久没解放到这种程度了 | 莱瓦汀系列获得 **1 临时等级**，持续 **10 回合** | **2 级 / 15 回合** | **3 级 / 20 回合** |
  | 只属于我的传说之剑 | 血量 < **50%** ⇒ 消耗的充能**降低 20%** | 除 +1 外，血量 < **40%** ⇒ **额外降低 10%** | 除 +1、+2 外，血量 < **30%** ⇒ **额外降低 10%** |

## 一、连段（`InstantExecution`，五段）

| 段 | 内容 | 实装 |
|---|---|---|
| 起手 | 主手未持莱瓦汀则立刻换上 | `SealedSwordBase.forceIntoMainHand(hero, Laevateinn.class)`（**绕过解封门槛**，与「复仇技艺 · 即刻处刑」同一条路径；手上没有封印之剑系列时只给一条提示，连段退化为用当前武器打） |
| 1~3 | 三段攻击，每段 **30%** | `hero.attack(victim, 0.30f, ledgerBonus, Char.INFINITE_ACCURACY)`，每次先 `AttackIndicator.target()`；整段包在 `hero.sprite.attack(victim.pos, cb)` 里 ⇒ **攻击动画照常播** |
| 3+ | 第三段命中后**击飞 5 格** | `WandOfBlastWave.throwChar(victim, trajectory, 5, true, false, hero)`，弹道沿「长兄 → 目标」的直线（`Ballistica.MAGIC_BOLT`），附带一次震屏 |
| 4 | **投出莱瓦汀追击** | `hero.sprite.zap()` + `MissileSprite.reset(hero.sprite, victim.pos, blade, cb)`；命中时刻调 `hero.attack(victim, 1f, ledgerBonus, INFINITE_ACCURACY)` ⇒ **基于莱瓦汀面板**（`damageRoll()` 带上超出需求的力量，`proc()` 带上火焰附加与「融化」） |
| 5 | **跃至敌侧 + 收尾重击** | 弹道落点照「英勇之跃」往回退一格；落地后 `5+S ~ 15+3×S`（`Hero.heroDamageIntRange`，护甲照常减免）+ **震击**（`BlastWave.blast(hero.pos)` + `PixelScene.shake(1, 0.4f)`，写法逐字照「踏碎」） |

**三个实现要点**（已同步 `AGENTS.md` §4）：

1. **击退是异步的，必须插一拍**。`throwChar` 排的是一个 `Pushing` 视觉（`DELAY = 0.15f`），敌人的 `pos` 要等动画播完才更新；而 `Pushing.act()` 一上来就把自己 `Actor.remove` 了，所以 `pushingExistsForChar()` 在动画期间**恒为 false**，靠它或靠 `actor.actPriority` 都等不到落位。这里在击退与投刀之间插了 **`hero.sprite.operate(hero.pos, cb)`**（英雄贴图 `operate` = 8fps×4 帧 = **0.5 秒**，见 `HeroSprite` 第 98 行）当节拍，`cb` 里再读 `victim.pos` 起飞——顺带把「击飞 → 再度抬手 → 掷剑」的演出串得好看。
2. **投掷是纯演出**：喂给 `MissileSprite` 的就是武器实例本身（`ItemSprite.view(item)` 只读贴图），**不消耗、不卸装、不改任何武器状态**；角速度走 `MissileSprite` 的默认 **720°/秒** ⇒ 自然就是「旋转着飞出」。
3. **相邻要求**：换出来的莱瓦汀攻击距离是 1，所以起手就校验 `distance <= 1`（与「复仇技艺 · 即刻处刑」一致），否则 `too_far` 提示。

## 二、三个天赋的数值入口（全部集中在 `Talent`，各一个取点）

| 天赋枚举（绝对索引） | 入口方法 | 消费点 |
|---|---|---|
| `NO_LEDGER_NEEDED(343, 3)` | `Talent.ledgerDevourMultiplier(hero)`（0.5×点数）+ `InstantExecution.devourLedger(hero)`（真扣充能） | 技能起手，扣完 `GLog` 报出「吞了多少 / 换成多少」 |
| `OVERDUE_RELEASE(344, 3)` | `Talent.executionUnleashLevels(hero)` / `Talent.executionUnleashDuration(hero)` | `ExecutionUnleashed.apply(...)` 在技能起手施加 |
| `LEGENDARY_BLADE(345, 3)` | `Talent.instantExecutionChargeFactor(hero)` | `InstantExecution.chargeUse()` 覆写（**可负担性检查与真扣费共用同一个 `chargeUse`**，所以门槛与实扣永远一致） |

- **吞充能**用 `RevengeLedger.consumeCharge(hero, ledger.chargePoints())`——它本身就是「整笔付清、不够一分不扣」的语义，传「全部点数」即要么全额吞下、要么不动；未装备 / 已诅咒时 `RevengeLedger.find` 返回 null，天赋静默作废。
- **残血分档**用 `hero.HP * 100 < hero.HT * 门槛` 比较（避免整除取整把边界算错），**减免按百分点叠加**（0.8 / 0.7 / 0.6）。
- S 直接取 `hero.STR()` ⇒「仇怨」的 +12、T1「健身一餐」、背叛家人者的预支力量**全部自然计入**，不需要额外判定。

## 三、`ExecutionUnleashed`（莱瓦汀解放）——限时等级

- 消费点是 **`SealedSwordBase.buffedLvl()`**（`KindOfWeapon.min()/max()` 读的就是它），与「拆开包装」（`unwrapLevelBonus`）**并列相加**；**绝不写回 `level()`**——本系列的 `level()` 是「英雄等级 ÷ 5」现算的，写进真实等级会被 `copyState`（形态切换 / 主副对调）**固化一次**，切几次叠几层 ⇒ 等级雪崩。
- 时长走 `Buff.prolong`（= `affect` + `postpone`，**只延不缩**）⇒ 连续两次即刻处刑只是把计时器压回满，不滚雪球；等级取 `max(已有, 新)`。
- **顶层类**（`actors.buffs`，随存档序列化），`levels` 手动存取；图标按本作惯例**借原版帧**（`BuffIndicator.UPGRADE` 帧 50）+ **橙金滤镜**，与同样借该帧的「临时力量」「背叛之力」区分。
- `desc()` 覆写为 `Messages.get(this, "desc", levels, dispTurns())`——文本里有两个占位符，**不能**沿用 `FlavourBuff` 那个只传 `dispTurns()` 的默认实现（实参对不上会整串回退成原文 = 看起来是乱码）。

## 四、存档兼容、图标与命名

- **占位类改名 ⇒ 必须补别名**：`ShatteredPixelDungeon` 构造函数里加 `Bundle.addAlias(InstantExecution.class, "…middlefinger.MiddleFingerAbilityThree")`（同批已有 `GritTeeth ← MiddleFingerAbilityOne`、`NeverForget ← MiddleFingerAbilityTwo`）。
- 图标：`HeroIcon.MIDDLE_FINGER_ABILITY_3` = **帧 131**（待绘制）；**129 仍无调用方但不要复用**（其一借用怒气帧 104）。
- `HeroClass.armorAbilities()` 的 `case MIDDLE_FINGER` 现为 `{ GritTeeth, NeverForget, InstantExecution }`；`MiddleFingerCoat` 的类注释同步更新。
- **同名提示**：「复仇技艺」第 5 式也叫「即刻处刑」（键 `actors.buffs.vengeancearts$move.execution.*`）。两者键空间互不相干，本技能显示名带 `[莱瓦汀]` 后缀以示区分；若仍嫌混淆，改 `actors.hero.abilities.middlefinger.instantexecution.name` 一处即可。

## 五、本轮改动文件

| 文件 | 改动 |
|---|---|
| `actors/hero/abilities/middlefinger/InstantExecution.java` | **新增**（技能本体：五段连段 + 三个天赋消费点） |
| `actors/buffs/ExecutionUnleashed.java` | **新增**（限时等级 buff） |
| `actors/hero/abilities/middlefinger/MiddleFingerAbilityThree.java` | **删除**（占位类） |
| `actors/hero/Talent.java` | 枚举 343/344/345 + 4 个数值入口 + import |
| `items/weapon/melee/SealedSwordBase.java` | `buffedLvl()` 叠加 `ExecutionUnleashed.bonusFor` |
| `actors/hero/HeroClass.java` | `armorAbilities` 换用 `InstantExecution` |
| `ShatteredPixelDungeon.java` | 新增一条 `Bundle.addAlias` |
| `items/armor/MiddleFingerCoat.java` | 类注释同步（三技能均已实装） |
| `actors/actors_zh.properties` / `actors.properties` | 新增 17 键 ×2 语言，删除 3 个占位键 ×2 |

## 六、核验（源码级，未代跑 Gradle）

- **javac**（`tools/jdk-21.0.12.1+1`）7 个改动文件 ⇒ **EXIT=0**（只有 unchecked 提示）。
- **文本**：新增 `instantexecution.*`（9）、三个天赋 `title/desc`（6）、`executionunleashed.*`（2），删除 `middlefingerabilitythree.*`（3）。
  键数 `actors_zh.properties` **1932 → 1946**、`actors.properties` **1753 → 1767**（净 +14/语言，两侧一致）。
- **⚠️ 踩到一个真事故并已修**：写入脚本把 Python 源码里的 `\n` 当成**真换行**落盘了，一条 entry 断成多行 ⇒ 续行没有 `=`，会被解析成垃圾键（文本只剩第一段，**加载不报错**）。修复脚本 `_chk/fix_newline_escape.py` 把 12 处续行接回单行（**并逐键断言与设计文案逐字一致**）；写入脚本已补 `esc()` + `assert_no_stray()`，不会再犯。
- **`_chk/InstExecLocCheck.java`**（真 `Properties.load` + 按真实实参类型 `String.format`，**并断言每个实参都出现在结果里** ⇒ 能抓出「值被截断」）：19 个键 × 2 语言 ⇒ **ALL PASS**。
- **`_chk/verify_armor.py`**（上一轮的回归脚本，已去掉删除的占位键）：26 个键 × 2 语言 ⇒ **ALL PASS**。
- **乱码/配平**：7 个文件无 U+FFFD、花括号全配平。
- 逐条复核过的机制点：`Char.attack` 的伤害合成为 `dmg = damageRoll()*dmgMulti; dmg += dmgBonus`（⇒ 传进去的额外伤害**不被倍率缩放**，三段 30% 上就是「固定 +X」）；`FlavourBuff.dispTurns()` 是 `protected`（子类可读）；`Pushing.Effect.DELAY = 0.15f`；`HeroSprite.operate` = 8fps×4 帧；`Buff.affect(Char,Class,float)` = `affect` + `spend(duration)`；`HeroBelongings.attackingWeapon()` 正常返回 `weapon()`。

## 七、待办

1. 画图：`hero_icons.png` **帧 131**（即刻处刑[莱瓦汀]）。
2. 游戏内实测：三段倍率手感、击飞 5 格是否被墙 / 占位卡住（`throwChar` 自会截断）、投刀与跃击的时序是否自然、收尾震击的表现。
3. 下表两处**判断**需要用户拍板。

## 八、需要拍板的两处

1. **「没有打开账簿的必要」的加成落在哪几段**：当前按字面「技能获得伤害加成」实现为**连段每一段各加一份**（三段 30% + 投掷 + 收尾 = 共 5 段，各 `+充能点数×倍率`）。若想只作用于**收尾一击**、或改成**百分比**加成，改 `InstantExecution` 里 `ledgerBonus` 的传递点（一处）+ `Talent.ledgerDevourMultiplier`（一处）即可。
2. **「只属于我的传说之剑」的叠加口径**：当前按**百分点叠加**（0.8 / 0.7 / 0.6）。若要改成**连乘**（0.8 / 0.72 / 0.648），把 `Talent.LEGENDARY_REDUCTIONS` 的求和改成累乘即可。

## 九、修复：「击飞之后整段哑火」

> **⚠️ 本节方案已被取代**（2026-09-17 深夜）。这里把病根归到「贴图回调不牢靠」，于是加了「主路 + 真实时间保底」的双保险；用户实测后确认**保底虽不再卡死，但击飞之后仍然看不到投刀动画、也不跳跃**。真正的病根是**英雄 `busy()` 期间回合线程停摆 ⇒ `throwChar` 排的 `Pushing` Actor 在本回合内根本不会执行**（详见文末「十、重写」一节）。当时的双保险代码、`Gate`/`BeatTimer` 两个小类、以及 `...instantexecution.beat_fallback` 日志键**均已删除**。本节保留仅作踩坑记录。

**症状**：放技能 → 三段命中、敌人被击飞 5 格 → **之后再无下文**，英雄也不再行动（这一回合交不出去）。

**根因**：`hero.busy()` 会把英雄的 `ready` 置 false，而 `Hero.act()` 在 `curAction == null` 时只调 `ready()` 并 `return false` ⇒ **整个世界就此停住**，只等这段连段自己 `spendAndNext` 把回合交出去。也就是说连段里**任何一次回调失约都会死锁**——这正是「停止行动」的真身。
而本技能原先把「击退→投刀」「投刀→追击→收尾」全押在贴图回调上；贴图侧的 `CharSprite.animCallback` 是**单槽**的（`attack`/`operate`/`zap` 谁后调谁覆盖，见 `CharSprite.java:258-282`），一旦被覆盖或不触发，就再没人来交回合。

**改法**：每一段推进都改成「**主路 + 保底路**」，由一道**只放行一次的闸门**（`Gate implements Callback`）合并：

| 段 | 主路（演出） | 保底路（真实时间） | 保底秒数 |
|---|---|---|---|
| 起手一刀 | `hero.sprite.attack(cell, cb)` | `BeatTimer` | 0.5 |
| 击退 → 投刀 | `hero.sprite.operate(cell, cb)`（0.5 秒抬手） | `BeatTimer` | 0.8 |
| 投刀 → 命中/追击 | `MissileSprite.reset(..., cb)` | `BeatTimer` | 1.25 |
| 跃击 → 收尾 | `hero.sprite.jump(..., cb)` | `BeatTimer` | 1.25 |

`BeatTimer` 是一个挂在 `hero.sprite.parent` 上的透明 `Visual`（构造 `super(0,0,0,0)`，`update()` 里 `left -= Game.elapsed`），**不经过 `MovieClip`、不占 `animCallback`、不受动画状态影响**（`effects/Pushing.Effect` 就是这套写法）。正常对局主路先到、保底静默作废；失约时保底兜住，最多演出难看一点，但**不会再卡死**。

**顺带留的定位工具**：`armBeat` 里有一条「**保底真的救场时才打印**」的日志（键 `actors.hero.abilities.middlefinger.instantexecution.beat_fallback`，由 `LOG_FALLBACK` 开关控制），用来判断究竟哪一段的贴图回调失灵；定位完成后把 `LOG_FALLBACK` 置 false 即可（键可留可删）。

**核验**（源码级，未代跑 Gradle）：`javac` **EXIT=0**；`_chk/verify_armor.py` 26 键 × 2 语言 **ALL PASS**；键数 zh **1946 → 1947** / en **1767 → 1768**（各 +1，两侧一致）；无 U+FFFD、`{}`/`()` 全配平、properties 无断行 / 无重复键 / 无 `/n/` 误写。

**待办**：游戏内实测（重点看：三段手感、击飞是否被墙/占位截断、投刀与跃击时序、收尾震击，以及**是否还会出现「保底节拍」日志**——若出现，请把日志内容告知，可据此定位未触发的贴图回调）。

---

# 中指长兄 · 技能列表带技能名 + 「咬紧牙关」改用专属图标（2026-09-17 晚）

## 一、列表类文本统一带技能名

**需求**：选择盔甲技能的界面里，描述要能看出是哪一门技能。

**做法**：新增 `ArmorAbility.namedShortDesc()`，三个列表消费点全部改用它——

| 消费点 | 位置 |
|---|---|
| 选择盔甲技能界面的按钮 | `windows/WndChooseAbility.java`（`new RedButton(ability.namedShortDesc(), 6)`） |
| 英雄图鉴 · 技能页 | `windows/WndHeroInfo.java`（`ArmorAbilityInfoTab` 的 `abilityDescs[i]`） |
| 职业护甲物品描述 | `items/armor/ClassArmor.desc()` |

规则：`lower(short_desc)` **含** `lower(name)` ⇒ 原样返回（原版技能名字已写在句子里，不重复）；否则 ⇒ `"_" + titleCase(name) + "_\n" + short_desc`。

- **为什么不直接改文本**：原版 20 多个技能的 `short_desc` 都把名字写在句子里（「战士_苦痛坚忍_，跳过…」），逐条改写等于动原版文本；放在代码层判断可以做到「原版一字不动、本 mod 的技能补齐」。
- **英文必须按「不分大小写」判**：原版把名字写成句内变形（`endure` → `_Endures_`），逐字比较会全部漏判成「需要补名字」。
- **预演结果**（`_chk/check_named_short_desc.py`）：zh 侧只有 8 项会补一行名字 —— 中指三技能 + `oracle.furiosoreplica`、`ringmaster.decoy`、`valencina.disposal`、`valencina.aimheart`（英文侧再多 `mage.warpbeacon`、`ratmogrify`，因英文名与句内变形差一个字母）；其余全部原样。
- **按钮高度是安全的**：`RedButton`（`StyledButton`）先 `setSize`（触发一次布局 ⇒ 文本按 `maxWidth` 换行）再取 `reqHeight()`，所以文字里加 `\n` 不会溢出按钮（`WndMonkAbilities` 同写法）。

## 二、图标：三个技能各用专属帧

| 技能 | 之前 | 现在 |
|---|---|---|
| 其一 咬紧牙关 | 怒气帧 **104** + 紫滤镜（`HeroIcon.BERSERK` + `iconTint()`） | **帧 129** = `MIDDLE_FINGER_ABILITY_1`，**滤镜已撤** |
| 其二 永不遗忘 | 帧 130 | 不变（注释里「图尚未绘制」已改为「专属槽位」） |
| 其三 即刻处刑[莱瓦汀] | 帧 131 | 不变（同上） |

- 三帧**均已绘制**：核对 `hero_icons.png`（128×272，8 列 × 17 行）第 17 行第 2/3/4 列，各自 252/256 像素非空。核对脚本 `_chk/CropIcons.java`（整行放大）、`_chk/FrameCrop.java`（按帧号放大 12×，输出 `_chk/frame_<n>.png`）。
- `ArmorAbility.iconTint()` / `NO_TINT` 与 `HeroIcon(ArmorAbility)` 里的滤镜分支**当前没有调用方**（唯一使用者是咬紧牙关），作为通用钩子保留。
- `GritTeethBuff`（状态 buff）仍借用 `BuffIndicator.BERSERK`：buff 图标取自 `buffs.png`，与 `hero_icons.png` 不是同一张图集，要换需另画。

## 三、事故与修复：编辑工具写坏 `ClassArmor.java` 的中文

- **症状**：`javac` 报一串 `编码 UTF-8 的不可映射字符`，且**报错行包含根本没碰过的两行**（`case VALENCINA:` / `case MIDDLE_FINGER:` 的注释）。
- **真因（同一文件里两种编码混存）**：① 新写入的中文注释**整行按 GBK 落盘**；② **行尾汉字的最后一字节被写成 `?`** ⇒ `拇指大衣` 的 `衣`（`E8 A1 A3`）残留成 `E8 A1 3F`，`中指外套` 的 `套`（`E5 A5 97`）残留成 `E5 A5 3F`。两者都让文件不再是合法 UTF-8 ⇒ `options.encoding='UTF-8'` 直接编译失败。
- **修复**：`_chk/fix_classarmor_encoding.py` —— 按 `utf-8 + surrogateescape` **无损**读入（保留非法字节）→ 只替换出事的那三行（唯一性断言 + 打印新旧）→ 写回后再断言「可完整解码 / 无代理字符 / 关键内容在」。**旧办法「整文件按 GBK 解码再写 UTF-8」在混存文件上会报错或二次污染，已废弃。**
- **新增全仓体检**：`_chk/check_utf8_all.py`（默认扫 `core/src/main/java`，1424 个文件，全部合法 UTF-8；另报「汉字后紧跟 `?`」的可疑行——`.properties` 里繁体文本的合法问号会命中告警，属噪音）。
- **铁律**（已写入 `AGENTS.md` §6）：**每次编辑 `.java`/`.properties` 后立刻跑体检脚本**，别只看「编辑成功」。

## 四、本轮改动文件

`actors/hero/abilities/ArmorAbility.java`（新增 `namedShortDesc()`）、`windows/WndChooseAbility.java`、`windows/WndHeroInfo.java`、`items/armor/ClassArmor.java`（含编码修复）、`actors/hero/abilities/middlefinger/GritTeeth.java`（图标 129、撤滤镜）、`.../NeverForget.java`、`.../InstantExecution.java`（注释）、`items/armor/MiddleFingerCoat.java`、`ui/HeroIcon.java`（槽位注释）；新增脚本 `_chk/{check_utf8_all,fix_classarmor_encoding,check_named_short_desc}.py`、`_chk/{CropIcons,FrameCrop}.java`。

## 五、核验（源码级，未代跑 Gradle）

`javac`（9 文件）**EXIT=0**；`_chk/check_utf8_all.py` **全部合法 UTF-8**；`_chk/check_named_short_desc.py` **zh/en 均 ALL PASS**（中指三技能都补上名字）。

## 六、待办

- 游戏内实测：① 选择盔甲技能界面三个按钮是否「名字 + 描述」两行、有无溢出；② 咬紧牙关图标是否显示为新帧 129；③ 图鉴技能页与护甲描述的显示。
- 若还想让**状态 buff** 也换专属图标，需要另画 `buffs.png` 的帧（当前仍借怒气帧）。

# 2026-09-17（四）中指长兄：三处修正（过人的毅力只保血 / 怨恨标记补远程 / 傲人肌肉伪等级）

## 一、「过人的毅力」（T3）只减免血量，护盾照常吃伤害

- **旧口径**：在 `Hero.damage` 里对「已定稿的最终伤害」直接夹取 ⇒ 护盾也被一起削掉，
  等于「本该被盾吃下的那一份」凭空少了，盾反而更耐打，与「只保血量」的设计相反。
- **新口径**：护盾先按原样吃满（`absorbed = min(dmg, shielding())`），下限保护只夹
  「越过护盾、真正落进血量」的那部分，返回 `absorbed + capped` 交回 `Char.damage` 照常结算
  （护盾那一份一个字节都没动，两边口径完全一致）。
- **接口**：`Talent.perseveranceCap(Hero, int dmg, int shield)`——多一个 `shield` 形参；
  调用点 `Hero.damage` 传 `src instanceof Hunger ? 0 : shielding()`。
- 两条规则本身没变：HP 高于下限 ⇒ 最多打到刚好贴住下限；HP 已在下限内 ⇒ 单次最多掉 1 点血。
- 饥饿伤害绕过护盾（`ShieldBuff.processDamage` 首行就特判），所以按 0 传，与结算口径一致。

## 二、「忠义巡礼者」的怨恨标记补上远程攻击

- **病因**：标记原先只有一个钩子（`Char.attack` 的命中分支），而**远程 / 法术弹道压根不走 `Char.attack`**——
  萨满 / 术士 / DM100 / 元素 / 眼魔 / 尤格之爪·之拳 / 哨戒室哨兵都是
  `hit( this, enemy, true )` → `enemy.damage( dmg, new XxxBolt() )`，于是「只有近战能叠」。
- **改法**：在 `Char.hit(Char, Char, boolean)`（**3 参重载**，全仓 11 处调用方全是这类弹道）
  里加命中钩子 `VengeanceArts.onRangedAttackHit(...)`。近战 / 投掷走 4 参重载，
  两条路互不重叠 ⇒ 不会重复叠层；日后新增法杖怪自动纳入，不必逐个改 `zap()`。
- 两个入口共用一份判据 `VengeanceArts.grudgeEligible(...)`（敌人出手 + 受害者是英雄或友方 + 已转职），
  口径不会再各写一套。
- **已知边界**：3 参重载里拿不到伤害值（命中判定早于伤害结算），所以远程只叠**基础 1 层**；
  专精天赋「如数奉还」按伤害追加层数仍只作用于近战 / 投掷。要让远程也吃满，得把判档挪到伤害之后。
- 另注：**怪物扔投掷武器这条路口目前不存在**——`MissileWeapon.onThrow` 里的 `curUser` 是 `Hero`
  （`Item.curUser`），投掷武器只有英雄能用；而像蝎子那种「远程平砍」（`canAttack` 里用
  `Ballistica.PROJECTILE` 放行距离）走的仍是 `Char.attack`，本来就已经叠层。

## 三、「傲人肌肉」（T1）伪等级少算 1 级 ⇒ 仇怨的 +12 力量全丢

- **病因**：实现写成 `muscleLevel = pointsInTalent - 1`（+1 点 → 0、+2 点 → 1），
  但武力戒指进伤害公式的等级来自 `RingBuff.buffedLvl() → soloBuffedBonus()`，
  未诅咒时返回 **`buffedLvl()+1`** ⇒ **+0 戒指在这儿是等级 1**、+1 戒指是等级 2。
  于是 +1 点（伪等级 0）一头撞进 `min()/max()` 里那句
  `if (lvl <= 0) tier = 1; //tier is forced to 1 if cursed`（**原版行为，不是 mod 加的**）
  ⇒ `tier(hero.STR())` 被整体丢弃，伤害钉死 1~10、**力量一概不计**（比空手还弱，
  「仇怨」那 +12 点自然也就完全看不到）。
- **改法**：`muscleLevel = hero.pointsInTalent(Talent.PROUD_MUSCLES)` —— +1 点 = 等级 1 =真正的「+0 戒指」，
  +2 点 = 等级 2 = 真正的「+1 戒指」，与设计文案精确对齐。真戒指路径（`usingForce`）公式一字未动，
  原版的诅咒降级行为也不受影响（诅咒戒指 `soloBuffedBonus` 返回 ≤0，仍会被降级）。
- **数值影响**（tier 见 `RingOfForce.tier`，S = `hero.STR()`）：+1 点由「固定 1~10」变为
  `round(tier+1) ~ round(6(tier+1))`；+2 点由「+0 戒指」提升为「+1 戒指」。力量（含仇怨 +12）已计入。

## 四、核验（源码级，未代跑 Gradle）

`javac`（5 文件：`Talent` / `Hero` / `Char` / `VengeanceArts` / `RingOfForce`）**EXIT=0**；
`_chk/check_utf8_all.py` 全仓 **1424 个文件全部合法 UTF-8**；
`perseveranceCap` / `onAttackLanded` / `onRangedAttackHit` / `muscleLevel` 四个符号回读确认无遗漏调用点。

## 五、待办（游戏内实测）

- ① 带护盾挨打：盾该碎还是碎，血量仍受下限保护（盾吃完后只对血量夹取）；
- ② 被萨满 / 术士 / 眼魔等远程打中时，敌人身上是否出现怨恨标记（近战照旧）；
- ③ 空手 + 傲人肌肉 +1 / +2 时伤害是否随力量（尤其「仇怨」开启后）明显上涨。

---

# 中指长兄 · 即刻处刑[莱瓦汀] 重写：整段连段交给「渲染线程状态机」（2026-09-17 深夜）

## 〇、问题回放（用户实测第二轮）

用户实测报三件事：① 伤害**正常计算**；② **看不到丢出莱瓦汀的动画**；③ 击退目标后**长兄没有跳到目标周围**。
换句话说：三段伤害照打，但后面「投刀 → 跃击」两步在视觉上完全没发生。上一节（§九「双保险」）只解决了「卡死」，这三件事一件都没解决 ⇒ 说明病根判断错了。

## 一、真正的病根：英雄持有回合期间，回合线程是停的

链路（全部源码级求证）：

| 环节 | 事实 | 出处 |
|---|---|---|
| ① | `hero.busy()` 把 `ready` 置 false | `Hero.java:1145` |
| ② | `Hero.act()` 在 `curAction == null` 时只 `ready()` 再 `return false` | `Hero.java:1032` |
| ③ | `Actor.process()` 收到 false ⇒ 走进 `Thread.wait()` 挂起；但 `Actor.current` **仍指向英雄** ⇒ `Actor.processing()` 恒为 true | `Actor.java` |
| ④ | `GameScene.update()` 只在 `!Actor.processing()` 时才 `actorThread.notify()` | `GameScene.java` |

⇒ **英雄持有回合的整段时间里，回合线程不会醒，任何 Actor 都不会执行。**

这一条把三件症状一次解释清楚：

1. **击退根本没发生**。原实现用 `WandOfBlastWave.throwChar(enemy, …)`——它只做 `Actor.add(new Pushing(…))`，真正移动敌人的是 `Pushing.act()` 里挂上的 `Pushing.Effect`。回合线程停着 ⇒ 敌人**原地不动**（要等这一回合交出去以后才补上那一记击飞，看起来像「击退发生在最后」）。
2. **后面两步读到的是击飞前的位置**：投刀时目标还贴着脸 ⇒ `MissileSprite` 的 `from`/`to` 几乎重合、飞行时间≈0 ⇒ **动画一闪而过、根本看不见**。
3. `leapDest()` 一算「已经相邻（`distance <= 1`）」⇒ 直接返回 `hero.pos` ⇒ **不跳**，就地补刀。

**关键佐证**：`effects.Pushing.Effect` 是个 `Visual`（不是 Actor），它由**渲染线程**的 `update()` 驱动、并**自己**写 `ch.pos`。⇒ 位移这件事本来就可以、也应该在渲染线程完成，与回合线程无关。

## 二、改法：整段连段做成一个渲染线程状态机

`InstantExecution` 的三个内部构件：

| 类 | 角色 |
|---|---|
| `Execution extends Visual` | 五段连段的推进器：挂在 `hero.sprite.parent` 上，`phase`（演到第几拍）+ `left`（距下一拍的秒数，每帧 `-= Game.elapsed`） |
| `Knockback extends Visual` | 击飞演出：克隆 `Pushing.Effect`（`DELAY = 0.15f`、`speed = 2·位移/时长`、`acc = -speed/时长` 的匀减速滑行），演完回调里**直接写 `victim.pos`** |
| `NO_OP` | 静态空 `Callback`，给所有装饰性贴图动画使用 |

`Execution.step()` 的九拍（每次 `phase++`）：

| 拍 | 内容 | 下一拍间隔 |
|---|---|---|
| 0 / 1 | 挥砍（`hero.sprite.attack(victim.pos, NO_OP)`）→ 30% 斩击 | `BEAT_HIT = 0.30` 秒 |
| 2 | 第三段 + **击飞**（`knockBack()`） | `BEAT_KNOCKBACK = 0.55` 秒 |
| 3 | **投出莱瓦汀**（`hero.sprite.zap` + `MissileSprite.reset(…, blade, null)`），返回飞行秒数（下限 0.25 秒） | 飞行秒数 |
| 4 | 投刀命中（`hero.attack(victim, 1f, bonus, INFINITE_ACCURACY)`，此刻主手正是莱瓦汀）+ 算跃点 + **起跳**（`hero.sprite.jump(…, null)`） | 腾空秒数（距离×0.1） |
| 5 | **落地**（`hero.move` + `occupyCell` + `Dungeon.observe` + `GameScene.updateFog`）+ 收尾重击（`(5+S)~(15+3S) - drRoll()`）+ 震击 → `finish()` | — |

五个要点：

1. **全程不排任何 Actor**（`Execution`、`Knockback` 都是 `Visual`），所以英雄持有回合也照样演、照样推进。
2. **装饰性动画一律传空回调**：`animCallback` 是单槽（会被 `attack`/`operate`/`zap` 互相覆盖）；更要紧的是 `null` 会让 `CharSprite.onComplete` 自动 `idle()` 并调 `ch.onAttackComplete()`——**对英雄就是再打一次真攻击、并把回合提前结算掉**。传 `NO_OP` 就只播动画、不触发那条岔路。
3. **每一拍自己检查前置条件**（`done` / `victim.isAlive()` / `hero.sprite != null`），任一不成立立即 `finish()`。
4. **`finish()` 只走一次**：`done` 闸门 → `killAndErase()` → `hero.sprite.idle()`（挥砍与跳跃都不带回调、不会自己回待机）→ `endTurn(hero)`（`Invisibility.dispel()` + `hero.spendAndNext(Actor.TICK)`）。
5. **击退规则照抄 `throwChar`**（老板减半、大型单位要求开阔地、落点被占少推一格、离开 `OPEN_DOOR` 顺手 `Door.leave`、`heroFOV` 变化时 `Dungeon.observe`），只省掉碰撞伤害那一支。

## 三、顺带清掉的死代码

- 上一版的 `Gate` / `BeatTimer` / `LOG_FALLBACK` 全部删除，收敛为一个 `NO_OP` 常量。
- 文本键 `…instantexecution.beat_fallback` 从 `actors_zh.properties` / `actors.properties` **各删 1 条**（键数 1947 → **1946** / 1768 → **1767**）。

## 四、核验（源码级，未代跑 Gradle）

- `javac -Xlint:all`（`InstantExecution.java` 单文件 + `-sourcepath`）**EXIT=0**，且**本文件 0 告警**——日志里 269 行告警全部来自被顺带重编的既有源文件（如 `Char.java` 的 `lossy-conversions` / `rawtypes`）。
- `_chk/InstExecLocCheck.java`：19 键 × 2 语言 **ALL PASS**。
- `_chk/verify_armor.py`：26 键 × 2 语言 **ALL PASS**。
- `_chk/check_utf8_all.py`：1424 个文件**全部合法 UTF-8**，无「汉字+?」痕迹。
- 全仓 `grep beat_fallback|LOG_FALLBACK` = 0。

## 五、待办

- **游戏内实测**（重点：能否看到「3 次挥砍 → 莱瓦汀旋转飞出 → 长兄跃到敌人身边 → 收尾震击」；击飞是否在**本回合内**看得见地把敌人推开）。
- 数值口径两处仍待拍板（见「八、需要拍板的两处」）。

---

# 中指长兄 · 选人界面介绍重写 + 存档槽图标改为复仇账簿（2026-09-17 深夜二）

## 一、需求

用户：①重新撰写角色选择界面的职业介绍；②把存档界面图标改为复仇账簿的图标。

## 二、职业介绍：两个文本键、三处消费点

`HeroClass.title()` / `desc()` / `shortDesc()` 都走 `Messages.get(HeroClass.class, …)`，键在 `actors_zh` 与 `actors`：

| 文本 | 键 | 显示在哪 |
|---|---|---|
| `_desc_short` | `actors.hero.heroclass.middle_finger_desc_short` | **选人界面（横屏）** 直接显示（`HeroSelectScene:471`，`RenderedTextBlock` 宽 80；与开始按钮冲突时自动加宽） |
| `_desc` | `actors.hero.heroclass.middle_finger_desc` | 选人界面的信息按钮 → `WndHeroInfo`，`cls.desc().split("\n\n")` 逐段渲染（`WndHeroInfo:178`） |

（竖屏选人界面只把职业名写在开始按钮上，不显示描述。）

旧文案是 2026-09-13 留下的**占位**（「暂时完整沿用战士……专属机制仍在设计中」），而中指长兄的三技能 + 两专精早已实装，故整段重写：

- `_desc_short`（新）：以神器_复仇账簿_记下每一笔血债、点燃_仇怨_换取额外_力量_，并把锁死在左手的_封印之剑_逐级解封为_莱瓦汀_。
- `_desc`（新）三段：①复仇账簿 / 仇怨（燃起立即 `+12 力量`、此后每回合烧掉一点充能、燃尽自熄）；②左手封印之剑（六阶、无法卸下、只能在主副手间对调且落回副手即重新封印、握主手时逐级解封为一阶段 / 二阶段解封之剑与莱瓦汀、附带递增火焰伤害）；③开局携带（指虎 + 复仇账簿 + 封印之剑，布甲、水袋、绒布包）。
- **刻意不重复列专精与盔甲技能**：它们在同一扇 `WndHeroInfo` 里紧跟描述之后各自成条（`subClsDescs` / `abilityDescs`），写进 `_desc` 是重复。四个既有魔改职业（战士 / 神谕 / 环指 / 拇指）的描述也都是「核心机制 ＋ 开局携带（＋ 开局鉴定）」，此处对齐。
- **未写「开局鉴定的物品」段**：`initMiddleFinger` 里没有任何 `new ScrollOfX().identify()`，源码注释也明确写了「战士原有的……鉴定三件套一并取消」⇒ 他开局确实没有额外鉴定物品，**照实省去、不编**。
- 名字写法沿用 2026-09-17 新文本的惯例（正文写「中指长兄」，不带 `~~` 删除线；删除线只留在标题 `heroclass.middle_finger` 上）。

## 三、存档槽图标

`scenes/StartScene` 的存档列表里每个存档旁边的小图标 = `Icons.get(info.heroClass)`（`StartScene:243/250`）；`scenes/RankingsScene:277` 的排行榜共用同一个入口。故只改 `ui/Icons.get(HeroClass)` 一处：

```java
case MIDDLE_FINGER:
-   return new ItemSprite(ItemSpriteSheet.ARMOR_MIDDLE_FINGER);    // 中指外套
+   return new ItemSprite(ItemSpriteSheet.ARTIFACT_REVENGE_LEDGER); // 复仇账簿（xy(1,41)，13×16）
```

`ARMOR_MIDDLE_FINGER` 没有变成孤儿常量（`items/armor/MiddleFingerCoat.image` 与 `windows/WndHeroInfo` 的页签图标仍在用）。
**未动 `WndHeroInfo` 的页签图标**：那是一份平行 switch，本作拇指就刻意让页签用护甲图标（与存档图标不同），且用户只说了存档界面 ⇒ 留作可选项。

## 四、核验（源码级，未代跑 Gradle）

- `javac -Xlint:all`（`Icons.java` 单文件 + `-sourcepath`）**EXIT=0**，本文件 0 告警。
- 新增 `_chk/ClassDescCheck.java`：用真 `Properties.load` 读 zh/en 两份，断言四个 `heroclass.middle_finger*` 键存在且非空、`_` 与 `~~` 标记成对、无 U+FFFD、6 条旧占位文案已清干净；另加**全文件行级断行自检**（非空非注释行必须含 `=`）⇒ **全部通过**。
- `check_utf8_all.py` 1424 文件全过；`verify_armor.py` 26 键 ×2 ALL PASS；`InstExecLocCheck` 19 键 ×2 ALL PASS；两份 properties 键数不变（zh 1946 / en 1767）。

## 五、待办 / 可选

- **游戏内看**：选人界面横屏的短介绍会不会被开始按钮挤得换行（`HeroSelectScene` 会自动加宽 `maxWidth`，理论上没问题）；信息按钮里三段描述的排版；存档列表里的图标是否已是那本账簿。
- 可选一致性（未做，等用户点头）：① en 侧 `middle_finger_unlock` 仍写 "elder brother"，而标题与描述已用 "Big Brother"；② `WndHeroInfo` 的页签图标仍是中指外套；③ en 侧莱瓦汀有两种拼写（物品名 `Laevateinn`，2026-09-17 的技能描述写 `Laevatinn`），本轮描述统一用物品名的 `Laevateinn`，历史文本未动。

# 中指长兄 · 职业介绍改三段式 + 详情界面左侧图标改为账簿/封印之剑（2026-09-17 深夜三）

## 一、需求

1. 角色介绍**详情界面**的左侧图标：第一个换成_复仇账簿_，第二个换成_封印之剑_；
2. 介绍保持**三段式**：① 基础特色机制 ② 初始携带的物品 ③ 初始鉴定的物品。

## 二、「左侧图标」到底是哪一个

先把栅格确认清楚：`WndTabbed.layoutTabs()`（`windows/WndTabbed.java:143`）在本作是把页签**横向排在窗口底部**的（`tab.setPos(pos, height + ...)`），所以「左侧图标」**不是页签**，而是 `WndHeroInfo.HeroInfoTab` 里**贴在每段描述左边**的那排小图 —— `layout()` 里 `icons[i].x = (20-icons[i].width())/2`，正文从 `x = 20` 起。它与 `desc().split("\n\n")` **逐段一一配对**。

改动（`windows/WndHeroInfo.java`，`HeroInfoTab` 构造函数里的**第二个** switch）：

| 段 | 旧图标 | 新图标 |
|---|---|---|
| ① 核心机制 | `SEAL`（战士破损纹章） | `ARTIFACT_REVENGE_LEDGER`（复仇账簿，13×16） |
| ② 初始携带 | `WORN_SHORTSWORD`（破旧短剑） | `SEALED_SWORD`（封印之剑，15×16） |
| ③ 开局鉴定 | `SCROLL_ISAZ` | `SCROLL_ISAZ`（未动，仍是通用的「鉴定清单」标记） |

**底部页签图标没动**（构造函数**第一个** switch 的 `tabIcon = new ItemSprite(ARMOR_MIDDLE_FINGER, null)`）——与拇指「页签用护甲图标」的既有取舍一致，且用户只说了介绍详情界面。

> **⚠️ 新记的坑**：`HeroInfoTab.layout()` 是 `for (i < info.length)` 里访问 `icons[i]` ⇒ **描述段数必须 ≤ 图标数**。给某职业多加一段描述却忘了补图标，会直接数组越界崩溃。以后动 `_desc` 的段数，必须同步检查 `icons[]`。

## 三、三段文本（zh / en 同步）

原文是「① 账簿/仇怨机制 → ② 封印之剑机制 → ③ 开局携带」三段，与新段落语义错位。改法：

| 段 | 内容 | 备注 |
|---|---|---|
| ① | 账簿/仇怨（燃起 +12 力量、每回合烧充能、燃尽自熄）＋ 封印之剑解封链 | 原第 1、2 段**合并**（都属「特色机制」） |
| ② | 开局携带：指虎 + 复仇账簿 + 封印之剑 + 布甲 + 水袋 + 绒布包 | 句式对齐战士/拇指的「开局携带…」 |
| ③ | 开局鉴定：力量药剂 / 复仇卷轴 / 恐惧卷轴 | 新增，`_-_` 清单格式对齐原版战士/决斗家 |

`_desc_short`（选人界面横屏那段）未动。

## 四、开局鉴定三件套（源码侧）

`HeroClass.initMiddleFinger` 原本**没有任何 `identify()` 调用**（其注释还写着「战士原有的…鉴定三件套一并取消」），所以第三段文本必须配套补上，否则文本描写的是不存在的东西：

```java
//开局鉴定：力量药剂 / 复仇卷轴 / 恐惧卷轴
new PotionOfStrength().identify();
new ScrollOfRetribution().identify();
new ScrollOfTerror().identify();
```

新增 import：`items.scrolls.ScrollOfRetribution`、`items.scrolls.ScrollOfTerror`（`PotionOfStrength` 原本已导入）。方法顶部的注释也同步改写。

> **命名提示**：物品的正式中文名是 **力量药剂**（`items.potions.potionofstrength.name`），故文本用「力量药剂」而非口语的「力量药水」；「复仇卷轴」＝ `ScrollOfRetribution`（`items.scrolls.scrollofretribution.name` 本就叫这个，与自定义神器「复仇账簿」同字不同物，别混）。

## 五、核验（源码级，未代跑 Gradle）

- `javac -Xlint:all`（`WndHeroInfo.java` + `HeroClass.java`，带 `-sourcepath`）**EXIT=0**；`HeroClass.java` **0 告警**；`WndHeroInfo.java` 仅 1 条 `[this-escape]`（第 96 行 `add(heroInfo)`）—— **经 `git diff` 确认该行不在本次任何 hunk 内** ⇒ 上游既有写法，非本次引入，放行。
- `_chk/ClassDescCheck.java` 扩写：新增**三段式断言** —— 段数必须＝3；① 段含「复仇账簿 / 仇怨 / 封印之剑」（en：Revenge Ledger / Rancor / Sealed Sword）；② 段含「携带」（en：starts with）；③ 段恰好 3 条 `_-_` 且含力量/复仇/恐惧（en：Strength / Retribution / Terror）⇒ **全部通过**。
- `check_utf8_all.py` 1424 文件全过；`verify_armor.py` 26 键 ×2 ALL PASS；`InstExecLocCheck` 19 键 ×2 ALL PASS；两份 properties 键数不变（zh 1946 / en 1767）；`/n/` 误写 0；两个 `.java` 括号配平、无 U+FFFD。

## 六、待办 / 可选

- **游戏内看**：三段排版；账簿（13×16）与封印之剑（15×16）落在 20px 槽里是否居中协调。
- 第三段的图标仍是通用的 `SCROLL_ISAZ`（用户只点名了前两个）—— 想换成「力量药剂」或别的，说一声即可。

# 2026-09-17 「依旧果冻人」音效：格式与落点勘查（**已实施**，落地记录见本文件末尾「音效落地」节）

## 1. 结论：音效是 mp3，音乐是 ogg

| 用途 | 目录 | 格式 | 现状 |
|---|---|---|---|
| 短音效 | `core/src/main/assets/sounds/` | **.mp3** | 67 个，418B ~ 19.5KB，多数 <1.5s |
| 背景音乐 | `core/src/main/assets/music/` | **.ogg** | 31 个（流式播放） |

- **唯一真源是 `core/src/main/assets/`**：Android 由 `android/build.gradle` 的
  `sourceSets.main.assets.srcDirs = [ new File(project(':core').projectDir, "/src/main/assets") ]`
  直接指过去（进 APK 的 `assets/sounds/…`）；桌面由 `desktop/build.gradle` 的
  `processResources { from new File(project(':core').projectDir, "/src/main/assets") }` 倒进 jar
  **根目录**（jar 内路径是 `sounds/…`，**没有 `assets/` 前缀**）；desktop 自己的
  `src/main/assets` 只有 `fonts/`、`icons/`。⇒ **文件只放一份，不必往多处拷贝**。
- 采样率/码率**没有硬性统一**（现有库里 32k/44.1k/48k、mono/stereo、32~256kbps 都有）。
  新音效建议照主流：**44.1kHz / 单声道 / ≤64kbps / ≤1s / <10KB**。
- libGDX 的 `Sound` 是**整段解码进内存**（`Gdx.audio.newSound`），别放长音频；`.wav`/`.ogg` 也能加载
  （按文件头解码），但本库统一 mp3。

## 2. 新增一个音效的三步（漏掉第 2 步就「静默无声」）

1. 文件放到 `core/src/main/assets/sounds/<小写 snake_case>.mp3`；
2. `Assets.java` 的 `Sounds` 内部类加常量，**并且把常量加进同类末尾的 `all[]` 数组**：
   ```java
   public static final String JELLY_BOING = "sounds/jelly_boing.mp3";
   // …
   public static final String[] all = new String[]{ /* … */ MINE, JELLY_BOING };
   ```
3. 触发点调用 `Sample.INSTANCE.play( Assets.Sounds.JELLY_BOING );`

`ShatteredPixelDungeon.create()`（85-88 行）：先 `Sample.INSTANCE.enable(SPDSettings.soundFx())` 与
`Sample.INSTANCE.volume(SPDSettings.SFXVol()²/100f)`，再 `Sample.INSTANCE.load(Assets.Sounds.all)`
——后者只是**入队**；真正解码发生在 `Game.update() → Sample.INSTANCE.update()`，**每帧只取 1 个**
（67 个音效约 1 秒加载完）。

**漏加 `all[]` 的表现**：`Sample.play(id, …)` 的 `id` 就是资源路径字符串、同时是 `ids` 映射的 key，
查不到就**静默 `return -1`**（不抛异常、日志干净）⇒ 代码改了、声音没有。

## 3. `Sample` 的播放 API

| 方法 | 说明 |
|---|---|
| `play(id)` | 音量 1、音调 1 |
| `play(id, volume)` | 左右同量 |
| `play(id, volume, pitch)` | **pitch 是变速变调**，做同效果的随机化最省事 |
| `playDelayed(id, delay, volume, pitch)` | 延迟播放（按 `Game.elapsed` 计时） |

音量与开关由设置面板（`SPDSettings.soundFx()` / `SFXVol()` 0-10）在启动时灌进 `Sample`，
**新音效自动受控**，不需要自己判断。

## 4. 拟接入「依旧果冻人」的位置（等音频文件到位后实施）

三个触发点都在 `effects/JellyWobble`：`hit()`（`Char.damage` 末尾）、`death()`（`Char.die`）、
`land()`（`ItemSprite.update`）。三点都已被 `enabled()` 闸门包住 ⇒ 音效天然只在挑战开启时响。注意：

- **受击会高频触发**（一次 AOE 打中 5 个敌人＝ 5 次摆动），同帧叠加多个音效会很吵
  ⇒ 建议在 `JellyWobble` 里加**最小间隔**去抖（如 0.08s，用 `Game.elapsed` 累计），
  或复用「当前是否已在摆动」（`sprite.jellyAmp != 0`）来判断。
- `pitch` 建议取 `Random.Float(0.9f, 1.15f)`，同一次效果听感不重复。

## 5. 自检脚本

`python _chk/audio_format_check.py [目录]`：裸解析 MP3 帧头，逐文件打印采样率/声道/码率/时长/体积，
并提示「扩展名不是 mp3」「时长 > 2s」「采样率非 44100」等偏差。（现有库中 `bee`/`descend`/`drink`
是 MPEG2 帧，脚本报「未找到 MPEG1 Layer3 帧头」属正常，不影响播放。）

# 2026-09-17 「依旧果冻人」音效落地（独占播放 + 打断升调）

## 1. 素材：源文件是「假 mp3」，已转码

用户给的 `jelly.mp3`（143KB）**不是 mp3**：真身是 **MP4/M4A 容器 + AAC-LC 音轨、48kHz 立体声、时长 5.83s**
（`ftyp isom` + `mp4a`；`ffmpeg -f mp3 -i` 直接 `Conversion failed`）。

这件事**不是格式洁癖**：桌面后端（lwjgl3）是**按文件扩展名**挑解码器的
（`com.badlogic.gdx.backends.lwjgl3.audio` 的 `Mp3$Sound` / `Ogg$Sound` / `Wav$Sound`），
MP3 解码器读 MP4 容器一帧都解不出来；而 `Sample.load()` 把异常吞掉只留一行 log
⇒ 表现是「**装进去、没声音、日志干净**」。所以按项目惯例转成真 mp3 后入库：

```
ffmpeg -y -i jelly.mp4 -vn -ac 1 -ar 44100 -c:a libmp3lame -b:a 64k \
       core/src/main/assets/sounds/jelly.mp3
```

⇒ **44100Hz / 单声道 / 64kbps / 47,511B / 5.85s**（符合本项目音效规格）。
本机 `C:/Program Files\Kuyo\ffmpeg.exe`（gyan.dev 完整版，含 `libmp3lame`）即可，不需要额外下载。

## 2. 引擎侧：`Sample` 新增「独占音效」

需求是「播放期间停止背景音乐、播完恢复」，而 `Sound` **没有播放完成回调**（只有 `Music` 有），
所以只能计时。计时点必须是**每帧无条件执行**的地方：
`Game.update()` → `Sample.INSTANCE.update()`（与当前场景无关，切场景也照跑）。

`com/watabou/noosa/audio/Sample.java` 新增：

| 成员 | 作用 |
|---|---|
| `playExclusive(id, volume, pitch, duration)` | **从头**播放（先 `Sound.stop()` 掐掉同一 id 的旧实例，因为 `Sound.play` 是叠加式的）＋ **暂停 `Music`** ＋ 记账倒计时 |
| `exclusivePlaying()` | 「是否还在播」——调用方据此决定要不要升调重播 |
| `releaseExclusive()` | 收尾：结束独占并把音乐放回来 |

三个必须注意的点：

1. **倒计时必须写在 `update()` 里 `delayedSFX` 那段之前**——那段开头有
   `if (delayedSFX.isEmpty()) return;`，写在后面就永远走不到（＝音乐再也不回来）。
2. **`exclusiveOwnsMusic`**：只有「播放前音乐在放（`!Music.paused()`）」时才由我们负责恢复；
   窗口失焦之类的外部暂停不会被错误恢复。反过来，`update()` 里还有一句自愈：
   独占期间若发现音乐又“活”了（切后台回来时系统会 `resume`）就再按回去。
3. **`reset()` 里也要 `releaseExclusive()`**：Android 销毁重建活动会调 `reset()`+`register`，
   若当时仍占着 `Music` 的暂停位，新实例里背景音乐就永远起不来了。

`duration` 由调用方传（`Sound` 取不到时长）：**素材时长 ÷ pitch**，因为 pitch 会等比例加快播放。

## 3. 核心侧：`effects/JellyWobble`

音效挂在 `swing()` 里 ⇒ 与摆动**同生共死**（「不是直接命中」的持续伤害既不振也不响）：

- 常量：`SFX = Assets.Sounds.JELLY`、`SFX_SECONDS = 5.85f`（**换素材必须同步**）、
  `SFX_VOLUME = 0.45f`（**2026-09-18 由 1f 下调**，理由见下）、`PITCH_STEP = 1.122f`（每次约 +2 半音）、`PITCH_MAX = 2f`。
- `sfx()`：**没在播** ⇒ 音调复位 1 后从头播；**还在播** ⇒ 音调 `×PITCH_STEP`（`PITCH_MAX` 封顶）
  再从头播（引擎侧会先 `Sound.stop()`），于是连续挨打能听到音调一路往上爬。
- **`PITCH_MAX = 2f` 是有硬理由的**：Android `SoundPool` 的播放速率合法区间就是 0.5~2.0，
  再高会被系统截断 ⇒ 2.0 正好是平台上限（约 +12 半音）。
- `Assets.java`：`Sounds.JELLY = "sounds/jelly.mp3"` **并加进 `all[]`**（漏了就是静默无声）。

## 4. 已知取舍

- **受击是高频事件**：一次 AOE 打中 5 个敌人 ＝ 5 次触发 ＝ 音调瞬间顶到上限，且背景音乐
  会被顶掉近 6 秒（`SFX_SECONDS ÷ pitch`）。这是需求本身要的效果，去抖开关可以后续再加
  （在 `sfx()` 里加最小间隔即可）。
- 物品落地（`land()`）同样会响：扔东西、怪物掉落、踩草都会触发。
- **换音效素材要同步 `SFX_SECONDS`**，否则要么音乐提前回来、要么多静音一小段。

## 5. 校验

- `javac @argfile` 两侧 **EXIT=0**（SPD-classes：`Sample.java`；core：`JellyWobble.java` + `Assets.java`）。
- `python _chk/audio_format_check.py core/src/main/assets/sounds`：新文件列在清单里，`44100Hz mono 64kbps`。
- `python _chk/check_utf8_all.py`：1424 个文件全部合法 UTF-8。
- 版本号与打包见下节（0.2.5）。

# 2026-09-18 出 v0.2.5（「依旧果冻人」并入正式版）

`build.gradle`：`appVersionCode 924 → 925`、`appVersionName '0.2.4' → '0.2.5'`
（编码 `900 + minor*10 + patch`：0.2.5 ⇒ 925）。本轮无新增功能，只是把上一节的挑战 + 音效正式发版。

- 构建：`:android:assembleDebug`，**BUILD SUCCESSFUL in 21s**（`dexBuilderDebug` executed）。
- 产物：`android-debug.apk` **37,576,782 B** → 归档 `EGOPD_0.2.5.APK`（根目录，md5 `42ccb188…`）。
- 核验：`output-metadata.json` 与 `aapt dump badging` 双向确认
  `versionCode=925` / `versionName=0.2.5-INDEV` / `applicationId=com.mypd.mypixeldungeon.indev`；
  包内 `assets/sounds/jelly.mp3` **47,511 B**（与源文件同尺寸）；
  `classes*.dex` 中 `JellyWobble` / `playExclusive` / `exclusivePlaying` / `jelly.mp3` 字面量全部命中
  ⇒ 证明 APK 真的含本次改动。

# 2026-09-18 果冻音效音量下调（1f → 0.45f）

**现象**：用户反馈音效「比起其他音效太大了」。

**原因**：素材是一段 5 秒的音乐，录音电平本来就比原版「一击一响」的音效高；当初为了先听清楚写了 `SFX_VOLUME = 1f`，
而 1f 在 SPD 里等于「和其他音效齐平」⇒ 一段满电平的音乐压在一群短音效上，自然盖场。

**改法**：只动 `JellyWobble.SFX_VOLUME`：**1f → 0.45f**（约 **−6.9 dB**）× 设置里的音效音量。

**为什么改这一个常量就够**：`Sample.playExclusive(id, volume, pitch, duration)` 内部就是
`play(id, volume, pitch)`，volume 最终与设置音量相乘——和原版所有音效走**同一条路径**，
所以调这个常量等价于「让这一条音效相对其他音效更轻」，不需要动引擎侧。

**可调区间**：想更轻 → `0.3`；想回响一点 → `0.7`；`1.0` 就是与其他音效齐平（原始值）。
下调只影响音量，**不影响**独占占位时长（`duration` 仍按 `SFX_SECONDS / pitch` 算），
即「音乐被顶掉多久」与音量无关。

**校验**：`javac` 单文件 `JellyWobble.java` **EXIT=0**；`_chk/check_utf8_all.py` 1424 文件全 OK。
**未打包**——`EGOPD_0.2.5.APK` 内仍是 1f 的旧音量，下次打包才生效。

# 2026-09-18 四位角色专属 BGM（设置开关 + 引擎层曲目替换钩子）

用户提供四首角色专属 BGM，要求「在菜单中添加专属 BGM 开关，开启时用对应 BGM 替换游戏原本的 BGM」。
经确认的交互定案：**一个总开关**，用哪首由**当前英雄的职业**决定；**不是这四位职业时不出现该选项**。

## 1. 素材与转码

| 角色 | 职业枚举 | 源文件 | 入库名 | 时长 | 96kbps 后 |
|---|---|---|---|---|---|
| 食指 · 神谕代行者 | `ORACLE` | `食指.ogg.mp3`（7.61MB / 197kbps） | `hero_oracle.mp3` | 324.0s | 3.71 MB |
| 环指大师 | `RING_MASTER` | `环指.ogg.mp3`（4.84MB / 216kbps） | `hero_ring_master.mp3` | 187.5s | 2.15 MB |
| 中指 · 长兄 | `MIDDLE_FINGER` | `中指.ogg.mp3`（4.82MB / 213kbps） | `hero_middle_finger.mp3` | 189.8s | 2.17 MB |
| 拇指 · 前二老板 | `VALENCINA` | `拇指.ogg.mp3`（5.17MB / 217kbps） | `hero_valencina.mp3` | 199.6s | 2.28 MB |

- **源文件虽叫 `.ogg.mp3`，真身就是 mp3**（`ffmpeg -i` 报 `Audio: mp3 (mp3float), 44100 Hz, stereo`），
  不是上一次那种「MP4 装 mp3 扩展名」的假货，所以直接转码即可。
- 转码脚本 **`_chk/import_hero_bgm.py`**（`--bitrate` 可调、`--dry` 只看不写）：统一
  `44100Hz / 立体声 / libmp3lame 96k`，并打印每首的源体积→新体积与压缩率。
  **合计 22.44 MB → 10.31 MB**。
- **为什么是 mp3 而不是 ogg**：`Music` 里有一条「iOS 不能播 ogg ⇒ 把 `.ogg` 换成 `.mp3`」的替换
  ⇒ 原版音乐必须双份。自定义曲目直接给 mp3 就三端通吃（Android `MediaPlayer` / 桌面 lwjgl3 /
  robovm 都支持），也不必再维护 `.ogg` 那一份。
- **放 `core/src/main/assets/music/`**：Android（`android/build.gradle` 的 `sourceSets.main.assets.srcDirs`）、
  桌面（`desktop/build.gradle` 里 `from project(':core')/src/main/assets`）、iOS（`ios/robovm.xml`
  的 `<directory>../core/src/main/assets</directory>`）三端全都指向 core 这一份，**只放一处即可**。

## 2. 引擎层：`Music` 新增曲目替换钩子

**问题**：BGM 播放点散落在十几处——各 `Level.playLevelMusic()`（Sewer/Prison/Caves/City/Halls + 四个 BossLevel）、
`YogDzewa` / `DwarfKing` / `DM300` 的战斗中途变奏、`AmuletScene` 通关曲……逐个改既容易漏，
又等于把 mod 逻辑写进原版代码。

**做法**：在 `com.watabou.noosa.audio.Music` 里加一个可注册的钩子，**只在两个公共入口**拦截：

```java
public interface TrackOverride { String override( String requestedTrack ); }   // 返回 null = 不干预
private TrackOverride trackOverride;
public synchronized void setTrackOverride( TrackOverride override ) { ... }
private String overridden( String requested ) { ... }   // 与 requested 相同也返回 null
```

- `play(String assetName, boolean looping)`：命中时 `assetName = forced; looping = true;`
- `playTracks(String[] tracks, float[] chances, boolean shuffle)`：命中时
  `play( forced, true ); return;` ——整个随机列表被「该曲目单曲循环」取代。

四条必须留意的细节：

1. **幂等是硬要求**：`playTracks` 命中后内部还会再调一次 `play(forced, true)`，那颗钩子会被**再问一次**。
   所以 `overridden()` 对「返回值 == 请求值」也返回 `null`，否则就是无限递归。
2. **`lastPlayed` 记的是替换后的名字**（赋值发生在替换之后）⇒ 开关来回切时能被正确判成「换曲」；
   反过来，已经在放同一首时 `if (isPlaying() && lastPlayed.equals(assetName))` 会直接 `return`
   ⇒ **刷新不会把音乐打断重来**。
3. **`trackList` 保持 null 是安全的**：`enable(true)` 的重启路径会退到 `play(lastPlayed, looping)`，
   再走一遍钩子；`playTracks` 里残留的 `trackQueue` 因 `trackList == null` 不会被动用。
4. 钩子在 `enabled` 检查与 iOS 扩展名替换**之后**：音乐被静音时行为与原版一致。

## 3. 游戏层：`HeroBgm`

新类 [HeroBgm.java](file:///d:/PD/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/HeroBgm.java)
（根包，与 `Assets` / `SPDSettings` 同级），负责「谁该放什么」：

| 成员 | 作用 |
|---|---|
| `register()` | 在 `ShatteredPixelDungeon.create()` 里注册钩子（挂在 `Sample.INSTANCE.load` 之后） |
| `track()` | 钩子的实现：开关关 / 无英雄 / 职业不在四位之列 ⇒ `null`（不干预） |
| `trackFor(HeroClass)` / `hasBgm(HeroClass)` | 职业 → 曲目映射与「有没有专属曲」判定 |
| `activeClass()` | `Dungeon.hero != null ? hero.heroClass : null` |
| `refresh()` | 拨动开关后立刻生效：`Dungeon.level.playLevelMusic()` |

**「不干预」的判据一条链解决三件事**：标题画面 / 选人 / 结算 / 日志这些界面都会把 `Dungeon.hero`
置 **null**（`HeroSelectScene`、`RankingsScene`、`JournalScene`、`Rankings`…），于是它们既不会被替换，
也不会让设置里冒出这一项。`TitleScene` 的 `THEME_1/THEME_2` 因此保持原版。

**`refresh()` 的取巧点**：不去记「刚才放的是哪首」，而是让当前关卡**重走一遍选曲判定**——
`playLevelMusic()` 内部怎么分支都无所谓，请求最终都会经过钩子：开 → 换成专属曲，关 → 换回原版曲；
若结论相同，`Music.play` 自己会 `return`，不会打断。

`SPDSettings` 新增 `KEY_HERO_BGM = "hero_bgm"` / `heroBgm(boolean)` / `heroBgm()`，**默认 `true`**。

## 4. 设置界面

落在 **设置 → 音频**（`WndSettings.AudioTab`）最下方，新增分隔线与一个复选框
（文本键 `windows.wndsettings$audiotab.hero_bgm`＝`专属BGM` / `Character Theme`）：

- `onClick` → `SPDSettings.heroBgm(checked())` + `HeroBgm.refresh()`（**立即生效**，不必等下一层）。
- 整块被 `if (HeroBgm.hasBgm( HeroBgm.activeClass() ))` 包住 ⇒ 非专属角色连分隔线都不会画。
- `layout()` 里沿用既有的 `height` 累加范式（`sep4.y = height + GAP`），
  窄屏/宽屏两种排布都不用改。

## 5. 已知边界

- **BOSS 战中途拨开关**：`refresh()` 会把曲目按该层 `playLevelMusic()` 的**静态判定**重算，
  而运行中途的动态变奏（如 Yog 二阶段）不在其列；从专属曲切回时可能落到该层默认曲而非当前阶段曲。
  只影响「战斗中途动开关」这一种情形，可接受。
- **Android 的 `playNextTrack` 跑在独立线程**（原版如此）：`track()` 在那种时机被调用，
  读的是 `Dungeon.hero` 与 prefs，不涉及 UI，安全。
- 四首曲子**各自循环播放**（3~5 分钟），不做随机轮换。

## 6. 校验

- `javac` 两侧 **EXIT=0**：SPD-classes（`Music.java`）；core（`HeroBgm` + `Assets` + `SPDSettings` +
  `WndSettings` + `ShatteredPixelDungeon`，classpath 需含 `_chk/_javachk` 与 `gdx-controllers-core`）。
- `_chk/audio_format_check.py core/src/main/assets/music`：四个新文件均为
  **44100Hz / stereo / 96kbps**（脚本的「时长 > 2s」「建议单声道」提示是给音效定的规则，BGM 不适用）。
- `_chk/check_utf8_all.py`：**1425** 个文件全部合法 UTF-8。
- 文本键双语齐备（zh/en 各 +1）。
- **未打包**。

# 2026-09-18 食指·神谕代行者专属音效（3 组共 7 条）

**需求**：①Furioso-Replica 的结束语要「显示哪句就念哪句」；②赫尔墨斯的双蛇杖的攻击音换成技能1 的三条（每次随机一条）；
③Furioso-Replica 第九击加特殊攻击音（技能8 第三条）。另要求**检查音量，避免过大或过小**。

## 素材来源：边狱公司灰机 wiki

页面 `limbuscompany.huijiwiki.com/wiki/敌方单位/1347`（食指父辈-里恩）。**两个必须绕过的坑**：

1. **wiki 域被 Cloudflare 挑战**：`curl` 直连页面与 `api.php` 都是 **403**；但抓取通道带渲染，能正常读。
   取文件清单的稳妥姿势 = `…/index.php?title=敌方单位/1347&action=raw`（拿到完整 wikitext，含所有 `[[文件:xxx.ogg]]` 与分组）。
2. **真文件不在 wiki 域，在 `huiji-public.huijistatic.com`**（注意是 `-public`）；主域名 `huijistatic.com` 在本机出口稳定 **502**。
   URL 规则：`/limbuscompany/uploads/<md5[0]>/<md5[0:2]>/<真实文件名>`。
   **⚠️ 页面显示名 ≠ 真实文件名**：wikitext 里写 `indexfather_1_0.ogg`，真实是 `Indexfather_1_0.ogg`
   （ns6 文件命名空间**首字母自动大写**）⇒ 按显示名算 md5 必 404。权威做法：`api.php?action=query&prop=imageinfo&iiprop=url&titles=File:A|File:B`（`|` 写 `%7C`）。
   *坑中坑*：脚本生成的 URL 清单若是 Windows CRLF，`while read` 会让 URL 末尾带上 `\r` ⇒ **curl 全线 HTTP=000**（不是网络问题）。

## 入库流水线 `_chk/import_lcb_sfx.py`

原素材 7 条来自不同录制源，**均值散布 −14.4 ~ −20.1 dB（相差近 6 dB）**——技能1 那三条是**同一事件随机三选一**，
不归一会让每次攻击忽大忽小。脚本做四件事：

1. **裁尾**：`silencedetect` 定位**尾部**静音（−50dB/≥0.1s），裁到「静音起点 + 0.15s」（保留自然衰减尾）。开头静音不裁（本批都没有）。
   收益：`skill1_3` 由 3.13s → 1.87s，回到项目「≤2s」规格内，并减重。
2. 解码 44.1kHz / 单声道。
3. 增益 `gain = min(−20 − mean, −1.5 − peak)`：**双重约束**——均值拉到 −20 dB（与其他音效听感一致），同时峰值留安全余量。
4. **闭环修正**：有损编码会产生**过冲**（实测 `skill1_3` 的 WAV 峰值 −1.5dB、解码后却到 **−0.1dB**，有削峰风险）⇒
   编码后**以解码后实测为准**按差值追加衰减并重编码（`PEAK_LIMIT = −1.0`，最多 3 轮）。该文件自动跑了 2 轮，落到 **−1.3dB**。

**结果**：7 条全部 **44100Hz / mono / 64kbps**，均值 **−20.2 ~ −20.9 dB**（散布仅 0.7 dB），
峰值 **−1.3 ~ −5.7 dB**，合计 **96 KB**。（`skill8_3` 时长 2.32s 略超 2s 提示线，作为收尾重击音属有意为之。）

## 接线三处

| 位置 | 改动 |
|---|---|
| `Assets.java` | 新增 7 个常量，**并全部加进 `Sounds.all[]`**（漏加 ＝ 静默无声） |
| `FuriosoReplica` | `ENDING_TEXTS`（纯文本）→ **`ENDING_LINES`（文本+音效成对）**，`finish()` 里一次取出、同步显示+播放 |
| `HermesCaduceus` | 覆写 `hitSound(float)` 从三条里随机播 + 武技入口 `setHitSound` 同步给形态武器 |

**结束语为什么必须成对取**：若文本和语音各自独立随机，会出现「显示阿鼻叫唤、念的却是无我梦中」的错配。
故改用二维数组把两者绑死，对应关系取自素材来源（解放-I→无我梦中 / 解放-II→阿鼻叫唤 / 解放-III→支离灭裂）。

**第九击的音要手写**：本技能九次攻击全部走 `enemy.damage(dmg, hero)`，**不经过 `Char.attack`**，
所以不会像普通攻击那样经由 `hitSound` 自动出声，必须显式 `Sample.INSTANCE.play(INDEXFATHER_SKILL8_3)`。

## 双蛇杖那条「特殊设计」到底是什么

需求里提醒「该武器会随机出其他武器进行攻击，直接配置攻击音效很可能不起作用」。查证结果分两半：

- **普通近战攻击其实是安全的**。命中音链是 `Hero.hitSound(pitch)` → **`belongings.attackingWeapon().hitSound(pitch)`**（多态），
  而 `attackingWeapon()` 的 `weapon()` 分支返回的**就是双蛇杖本身**——随机出的形态武器（私有字段 `formInstance`）
  只被用来算伤害/命中/攻速/射程，**从不进 `thrownWeapon` / `abilityWeapon`**。所以「改字段」在普通攻击下**确实会生效**。
- **但有一个路径真的会失效**：`MeleeWeapon.beforeAbilityUsed` 会写 `hero.belongings.abilityWeapon = this`（`afterAbilityUsed` 复位），
  而双蛇杖的**武技正是委托给 `formInstance.duelistAbility(...)` 执行的** ⇒ 武技窗口内 `attackingWeapon()` 变成那把**形态武器**，
  它自己的命中音被播放，双蛇杖配置的音被整体绕过。

**采用的设计**：
1. **覆写 `hitSound(float)` 而不是给字段赋值** —— 字段只能填一条固定音，拿不到「每次随机」；覆写方法还顺带免疫上述多态问题。
2. **武技入口把音同步过去**：`formInstance.setHitSound(Random.element(HIT_SOUNDS))`。
   由于 `hitSound` 是 `items` 包的 **protected**、`items.weapon.melee` 跨包写不了（对照 `augment` 是 public 才写得动），
   在 `KindOfWeapon` 加了一个最小的 `public void setHitSound(String)`。

## 校验

- `javac` 单文件核验 `KindOfWeapon` / `HermesCaduceus` / `FuriosoReplica` / `Assets` **EXIT=0**。
- 交叉校验脚本：7 个常量 ↔ **实际文件存在** ↔ **已登记进 `all[]`**，**全部命中**。
- `_chk/audio_format_check.py`：7 条全部 44100Hz / mono / 64kbps。
- `_chk/check_utf8_all.py`：**1425** 文件全部合法 UTF-8。
- **未打包**。

---

# 2026-09-18 指令系统提示音（*哔哔* / _CLEAR_）

给指令系统（神谕代行者核心机制）的两条提示文本配同步音效。素材由用户提供，为两个 `.m4a`。

## 1. 素材与入库

| 素材 | 提示 | 裁后时长 | 入库名 |
|---|---|---|---|
| `bibi .m4a` | `*哔哔*` | 1.21s | `instruction_buzz.mp3` |
| `clear.m4a` | `_CLEAR_` | 2.02s | `instruction_clear.mp3` |

两条都是 **AAC / 48kHz / stereo**，走同一套流水线：裁尾 → 44.1kHz/mono → 响度闭环 → 64kbps mp3。

`_chk/import_lcb_sfx.py` 本次扩展了 `--src-dir` 与 `--map SRC=DST`（可重复）两个参数，
不再依赖硬编码的 `_lcb_audio` 目录与 `MAP`，可直接处理 Downloads 里的素材：

```bash
python _chk/import_lcb_sfx.py \
  --src-dir "C:/Users/14675/Downloads" \
  --map "bibi .m4a=instruction_buzz.mp3" --map "clear.m4a=instruction_clear.mp3"
```

**两条素材的电平走向相反**，正好说明为什么必须过归一：

| 入库文件 | 原始峰值 | 施加增益 | 归一后均值 | 归一后峰值 |
|---|---|---|---|---|
| `instruction_buzz` | 0.0 dB（已顶格） | **−1.5 dB**（被峰值约束卡住） | −20.9 dB | −1.8 dB |
| `instruction_clear` | −11.0 dB（录音偏轻） | **+7.4 dB** | −20.3 dB | −4.5 dB |

`buzz` 已满刻度，只能靠**峰值约束**定增益（否则削峰）；`clear` 要放大 7.4 dB 才够响。
最终两条均值都落在 −20 dB 附近，与项目现有音效（−18.9 ~ −22.6）同档。

## 2. 接线：三个显示点，一个播放口

`*哔哔*` / `_CLEAR_` 的显示是 **`GLog.c(...)` + `hero.sprite.showStatus(...)` 成对**出现的，共 3 处：

| 位置 | 触发 |
|---|---|
| `InstructionTimer.trigger()` | 指令计时器到点（150~200 回合，天赋可缩短） |
| `Instruction.triggerSpecial(...)` | 命运宠儿+3 特殊指令（背水一战封层 / 隐藏房间楼层） |
| `Instruction.complete(...)` | 指令完成 |

**做法**：在 `Instruction` 内集中两个播放口 `playBuzz()` / `playClear()`，三处调用。
好处是「哪些路径该响」一目了然，且去抖只需写一次。

**去抖（0.25s）**：`triggerSpecial` 可能在很接近的时刻被多条路径触发（背水一战封层与计时器同回合），
不去抖会听到叠音。⚠️ **时间戳必须用 `Game.timeTotal`（累计秒）——`Game.elapsed` 是单帧增量（≈0.016 秒），
拿来当时间戳会完全失效**（这是本次唯一一处容易踩错的 API 选择）。

去抖判据写成 `Game.timeTotal > lastAt && Game.timeTotal - lastAt < DEBOUNCE`：
前半个条件用于应对 Game 重启（`timeTotal` 归零而静态字段仍持旧值）时不会把首次提示误杀。

音效走 `Sample.play(...)`（普通音效通道），与 BGM 的 `Music` 相互独立，不会互相打断。

## 3. 校验

- `javac` 核验 `Assets` / `Instruction` / `InstructionTimer` **EXIT=0**。
- 交叉校验：2 个常量 ↔ 实际文件 ↔ `all[]`，全部命中。
- `_chk/audio_format_check.py`：两条 44100Hz / mono / 64kbps；`instruction_clear` 2.09s 略超 2.0s 线
  （与 `jelly` 5.90s、`skill8_3` 2.32s 同属已接受的特例——台词类素材裁太狠会切尾音）。
- `_chk/check_utf8_all.py`：1425 文件全合法。
- **未打包**。

---

# 2026-09-18 拇指·前二老板专属音效（6 组共 17 条）

素材来自灰机 wiki `敌方单位/1314`。三组用途：闪避嘲讽、预知眼过热、以及巴勒莫剑术 / 处置 / 加速弹的动作音。

## 1. 入库清单

| 用途 | 条数 | 入库名前缀 | 触发点 |
|---|---|---|---|
| 闪避成功嘲讽 | 1 | `valencina_dodge_taunt` | `Char.attack` 未命中分支 |
| 预知眼过热 | 1 | `valencina_overheat` | `OdinsEye.overheatPenalty()`（授予处） |
| 巴勒莫剑术语音 | 4 | `valencina_palermo_voice_1..4` | `PalermoFencing.cast()` |
| 巴勒莫剑术命中音 | 8 | `valencina_palermo_hit_1..8` | `PalermoFencing.hitSegment()` |
| 「处置」语音 | 2 | `valencina_disposal_voice_1..2` | `PalermoFencing.disposal()` |
| 加速弹换弹声 | 1 | `valencina_reload` | `AccelerationShot.consumeOnAttack()` |

合计 360 KB，全部 44100Hz / mono / 64kbps，均值 −20 ~ −22 dB。

**下载**：`9SV-BAT8-*` 以数字开头，不涉及大小写；`thumbfather_*` 必须按 ns6 规则**首字母大写**写成
`Thumbfather_*`，否则 md5 路径必 404（本批实测一次通过）。

## 2. 修了脚本的一个真缺陷：峰值顶格

`Thumbfather_3_1-1.ogg`（攻击音第 1 条）**素材本身录满刻度到 0.0 dB**，
`gain = min(−20 − mean, −1.5 − peak)` 被峰值约束卡死，均值只能到 **−23.5 dB**
——比同组其他七条（约 −20）**轻 3.5 dB**。而这八条是**随机八选一**，落差会被玩家听成「时大时小」。

解法：给 `_chk/import_lcb_sfx.py` 加**峰值顶格补救**：

1. 判定：`TARGET_MEAN − mean > PEAK_CEIL − peak + CHAIN_TRIGGER(0.3)` 即视为「被卡住」；
2. 补救：先过一遍 `alimiter=limit=L:level_out=0.84`（**自带补偿增益**，一次到位），
   L 从 0.75 起每轮 −0.1 迭代，直到 crest ≤ 18.5 + 1.0 dB；
3. 再走原有归一。

效果：`hit_1` −23.5 → **−22.0**，`overheat` −23.3 → **−21.0**，随机组散布 **3.3 dB → 2.0 dB**
（项目现有音效本身散布 3.7 dB，已更整齐）。报表中经补救的文件会标 `[限幅]`。

## 3. 接线：新建集中入口 `ValencinaSfx`

六处触发点分散在 5 个类里，全部收口到 `abilities/valencina/ValencinaSfx.java`：
分组数组 + 播放口 + 职业校验（`Dungeon.hero.heroClass == VALENCINA`）+ 去抖，调用方只写一句。

| 播放口 | 要点 |
|---|---|
| `playDodgeTaunt()` | ⚠️ **只在取到 `dodge_taunt_1`（「怎么，打不中吗？」）时播**。`Char.attack` 里原本 `Random.Int(5)` 直接拼键名，改为先存 `dodgeTaunt` 索引、显示后再判 `== 1` |
| `playOverheat()` | **过热授予的那一刻播一次**。⚠️ **不能挂 `OdinsEyeOverheat.act()`**——`FlavourBuff.act()` 整个生命周期只被调用一次（等到期时 detach），挂那里会变成在第 30 回合、过热即将结束时才响（2026-09-18 用户实测发现并修正） |
| `playPalermoVoice()` | 四选一 + **上一条未播完则跳过**（按各条实测时长判断，时间戳用 `Game.timeTotal`） |
| `playPalermoHit()` | 八选一，**取代**原版 `HIT_STRONG` |
| `playDisposalVoice(hero)` | 二选一，**文本与语音成对取用**（防「显示 A、念的却是 B」）+ `hero.sprite.showStatus` 头顶台词（暖米色 `0xE5CAA5`） |
| `playReload()` | **`Sample.playDelayed(id, 0.4f)` 延后播放**——否则与本次攻击音效糊在一起 |

**职业校验的意义**：奥丁之眼是通用神器，非拇指角色也能装备；`isValencina()` 保证这些语音只在拇指身上响。

**攻击音已覆盖三条路径**（同日补全）：`hitSegment()`（单次剑术）、`disposalStrike()`（处置 4 段斩击，
命中即播，不再用 `HIT_STRONG`）、`freeSwing()`（瞄准心脏突进的免费剑术）。三处同调 `playPalermoHit()`。

⚠️ **处置 4 段会叠音**：段间隔约 `attackDelay × 0.5`（通常 0.25~0.5s），而素材长 0.9~1.7s
⇒ 连击时同一条语音会重叠 2~4 层。这是「每段都响」的必然结果；若嫌吵，两个改法：
①在 `playPalermoHit()` 加最短间隔去抖；②只在第一段（`idx == 0`）播。

## 4. 校验

- `javac` 六个文件 **EXIT=0**。
- 交叉校验：17 个常量 ↔ 实际文件 ↔ `all[]`，**全部命中**。
- `_chk/audio_format_check.py`：全部 44100Hz / mono / 64kbps（5 条台词类超 2.0s，属已接受特例）。
- `_chk/check_utf8_all.py`：1427 文件全合法。

## 5. 同日后续修正：语音音量上调（用户反馈「偏小」）

**判据换了**：不再只看 `mean_volume`，改用 **EBU R128 的 LUFS（感知响度）**。
原因是人声内部有自然停顿，那些静音段会把 mean 拉低 —— 按音效目标（−20dB）归一后，
说话段实际电平明显低于音效，玩家听感就是「语音偏小」。脚本新增 `--target-mean` 参数。

| 对象 | LUFS | 说明 |
|---|---|---|
| 项目原版音效（crossbow / zap / death） | −21.5 ~ −24.0 | 归一目标 −20dB 对应的感知响度 |
| 拇指语音（上调前） | 约 −20 | 与原版音效同级 ⇒ 听感偏轻 |
| 拇指语音（上调后，−15dB 目标） | **−13.8 ~ −16.0** | 比原版音效响 5~7 LUFS |
| 神谕结束语（同步上调） | −15.4 ~ −16.4 | 3 条 `indexfather_liberation_*` |

**只上调语音类**：拇指 16 条（除 `valencina_reload`——换弹声非语音，保持 −20）+ 神谕 3 条结束语。
`indexfather_skill1_*` / `skill8_3` 是招式音效，同样保持 −20。

**一个容易误判的点**：`mean_volume` 散布 3.3dB，但 **LUFS 散布只有 2.25**
（`overheat` mean −18.5 看似偏轻，LUFS −15.44 其实与其他条接近）⇒ 归一时以 LUFS 为准更贴近听感。

**试过并否决的两条路**：
- `loudnorm=I=-15` 单遍动态模式：对本作这些 1~2 秒的短喊话反而把 mean 拉到 −20（窗口来不及适应）。
- `acompressor` 无补偿：整体电平掉 9dB 而 crest 只从 18.2 压到 16.9，压缩效率极低；
  带 `makeup=5`（⚠️**该参数是线性倍数不是 dB**）虽能把 mean 抬到 −13.4，但峰值同时顶格削波，
  且 `alimiter` 本质是限制器、**几乎不提升均值**（实测某素材只 +0.7dB）。

**真峰值体检**：上调后逐个测 `input_tp`，全部 < 0（−0.07 ~ −1.94 dBTP），无削波。
顺带发现历史遗留：`jelly` 的素材 TP 达 **+2.69 dBTP**（本身过载），只是靠代码侧 `SFX_VOLUME=0.45` 才没爆出来
—— 未处理，留待确认。

# 2026-09-18 「过人的毅力」豁免「自伤换成长」道具（蓄血圣杯 / 割腕）

## 一、问题

中指长兄 T3「过人的毅力」会把受到的伤害夹进**血量下限**（血量高于下限时打不到下限以下；血量已在下限之内时**单次最多 1 点**）。这本身是保命设计，但它把两件道具变成了**免费成长机**：

| 道具 | 动作 | 升级判据 | 被下限保护后的结果 |
|---|---|---|---|
| 蓄血圣杯 `ChaliceOfBlood` | 「血祭」`PRICK` | `prick()` 里 `hero.damage(...)` 之后 `if (!hero.isAlive())` 判死，活着就 `upgrade()` | 永远活着 ⇒ 每一下都成功 ⇒ **无条件刷满 10 级**，代价还被抹平 |
| 割腕 `WristSlit` | 「割腕」`CUT` | 同上（`cut()` → `damage` → 活着就 `upgrade()`） | 同上 ⇒ **想割几次割几次** |

两件道具的伤害源都是**自己**（`hero.damage(damage, this)`），所以不能靠「只认敌人伤害」这种粗暴判据解决。

## 二、做法：给「自伤换成长」打标记接口

新增 **`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/SelfHarmCost.java`** —— 一个空标记接口，javadoc 写明语义（「对持有者自己的伤害是换取道具升级/成长的代价」）与成因（为什么必须豁免）。

- `ChaliceOfBlood` / `WristSlit` 各自 `implements SelfHarmCost`（连同 import 与一行说明注释）。
- `Talent.perseveranceCap` 增加第 4 个形参 `Object src`，并在**函数最前面**（`dmg <= 0` 之后、`perseveranceFloorPercent` 判定**之前**）早退：

```java
//「自伤换成长」的代价不参与下限保护（蓄血圣杯的血祭 / 割腕的割腕）。
//放在 hero/天赋点数的判定之前：豁免与「是否点了过人的毅力」无关，点了也不打折。
if (src instanceof SelfHarmCost) return dmg;
```

- `Hero.damage` 的调用点把 `src` 一并传下去（`..., src instanceof Hunger ? 0 : shielding(), src )`），注释块补第 ⑤ 条说明。

**为什么放「天赋点数判定之前」**：豁免的是「这笔伤害的性质」，与持有者点没点该天赋无关。写在后面就变成「只有点了天赋才豁免」，语义反了（而且那时 `floorPct<=0` 也会先返回，等于永不触发）。

**只豁免下限保护**，不豁免通用减伤：护甲减伤、`RingOfTenacity`、`SilentPrice` 都在更早的乘区里结算，照常生效——这与圣杯自己 `prick_warn` 里写的「自伤可被_物理、魔法防御与伤害减免_降低」一致。附带好处：`prick_warn` 里显示的「耗尽生命力的概率」此前会因下限保护而**说谎**，现在恢复准确。

## 三、为什么用接口而不是 `instanceof`

「自伤换成长」是一条**通用语义**（而不是「这两个类特殊」）：以后再加同类道具，实现接口即可自动获得豁免，不必记得回来改天赋；同时读 `ChaliceOfBlood.java` 就能看到 `implements SelfHarmCost`，顺着接口找到成因说明。若写成 `src instanceof ChaliceOfBlood || src instanceof WristSlit`，新增道具时极易漏改（而漏改的表现是**玩家白嫖升级**，不会有任何报错）。

## 四、文本同步

`actors.hero.talent.perseverance.desc` 末尾追加例外（zh + en），否则玩家会以为点满就能白嫖：

- zh：`（…这类持续伤害。_例外_：_蓄血圣杯_的_血祭_与_割腕_的割腕是换取_成长_的代价，这两笔伤害_完全不受下限保护_）`
- en：`(… such as poison, burning and bleeding. _Exception_: the _Chalice of Blood_'s _prick_ and _Wrist Slit_'s cut are the price of growth, and _never receive this protection_)`

## 五、核验（源码级，未代跑 Gradle）

- `javac -Xlint:all`（`SelfHarmCost` / `ChaliceOfBlood` / `WristSlit` / `Talent` / `Hero`）**EXIT=0**；**本次改动行 0 告警**（日志里 `ChaliceOfBlood:211`、`Talent:558/834/1893`、`Hero:277/2304` 都是别的源文件的既有 `rawtypes` / `cast` / `this-escape`，与本次无关）。
- 新增 **`_chk/verify_selfharm_exempt.py`**，一次断言五处：①`SelfHarmCost` 是 interface 且点名实现者；②两件道具都 `implements SelfHarmCost`（且 import 齐）；③`perseveranceCap` 是 4 参且守卫**早于** `perseveranceFloorPercent`；④`Hero.java` 调用点第 4 实参是 `src`；⑤两份 `perseverance.desc` 写明例外且标记成对。**已做反例自测**（把旧的三参调用、旧文本喂给判据，确认会判 FAIL，证明不是空测）⇒ ALL PASS。
- `check_utf8_all` 1427 文件 OK；`verify_armor` 26×2、`InstExecLocCheck` 19×2、`ClassDescCheck` 全 PASS；`/n/` 误写 0；五个 `.java` 括号配平（`Talent.java` 的 `()` 差 4＝4 处 `//N)` 编号注释，既有现象）、无 U+FFFD。

## 六、顺带修掉一个**脆断言**（假红）

本轮跑 `ClassDescCheck` 时冒出一条 `FAIL：第 1 段未提到机制关键字: 仇怨`——**不是本次改动引起的**：是 `middle_finger_desc` 的首段被用户自己重写过（删掉「仇怨」，改成「记下自己_受到的伤害_与_造成的击杀_，用以充能_复仇账簿_」），而该脚本把 `仇怨` 硬编码成了必需词。

已放宽为「首段出现 `复仇账簿` **或** `封印之剑` 任一即可」——**文本断言只认稳定的概念名**（神器/武器是固定游戏对象，名字不随文案改动），**别认 buff 名、招式名这类用户会自己改的措辞**。

## 七、待办 / 待确认

- **zh / en 不同步（需用户拍板）**：`middle_finger_desc` 首段 zh 已重写为简版（不再提「仇怨 / 每回合烧充能 / 燃尽自熄 / 主手对调重封」这些细节），**en 侧仍是旧版**（`turning them into _Rancor_ he can stoke…` 那一大段）。要么把 en 按新 zh 精简同步，要么把 zh 的细节补回去。
- 同类「自伤也是代价」但**不构成刷等级循环**的两处（`ScrollOfPsionicBlast` 对自己、`WandOfTransfusion` 对自己）**刻意未豁免**——它们只是「用血换一次效果」，不会被下限保护变成无限循环。若希望它们也不吃下限保护，说一声即可（加个 `implements` 就行）。

# 2026-09-18 果冻音效素材重转（真峰值 +2.69 → −1.25 dBTP）

## 一、问题：上一版是「推过满刻度」的

上一轮只调了 `SFX_VOLUME`（1f → 0.45f），**素材本身的问题一直没动**：

| | 均值 | 最大 | LUFS | **真峰值** |
|---|---|---|---|---|
| 源 `Downloads/jelly.mp3`（5.83s / 48kHz 立体声 / 192k AAC） | — | −0.0dB | −5.74 | **+0.04 dBTP** |
| 上一版入库文件 | −7.4dB | 0.0dB | −6.16 | **+2.69 dBTP** |
| 本次重转 | −11.5dB | −1.3dB | −10.34 | **−1.25 dBTP** |

**+2.69 dBTP 意味着整段音乐被推过了 0 dBFS**（源素材本来就贴在满刻度上，再按均值归一就是硬削平顶）。靠 0.45 的音量压着只是「听不出破音」，波形已经平了。

## 二、做法：从源重走一遍，闭环以「解码后」为准

源目录 `_chk/_jelly_audio/`（同时留了 `jelly_src.m4a` 3:17 的完整原曲，本次用的是 Clipchamp 剪好的 5.83s 片段）：

1. 解码为 **44100Hz / 单声道**（立体声下混会让 LUFS 从 −5.74 掉到 −8.74，因为两声道内容相关）；
2. 以 `volume=GdB` → `libmp3lame -b:a 64k` 编码，**每轮都重新解回来量 `loudnorm` 的 `input_tp`**，用 `G += (−1.5 − tp)` 迭代（实测 2 轮落到 −1.25，落在 ±0.25 容差内）；
3. 只衰减 **−1.15 dB**，不做任何限幅 / 压缩——源素材 `LRA = 0.10 LU`，本来就已经是压平的墙，再压只会更糊。

**为什么不复用 `_chk/import_lcb_sfx.py`**：那个脚本的归一目标是「均值」，且带 `silencedetect` 裁尾与 `alimiter` 补救（针对短音效设计）。这段素材的正确目标是「真峰值留够余量」，均值随它去。

## 三、音量必须跟着改：`SFX_VOLUME` 0.45f → **0.73f**

改素材不改音量常量 = 音量会跟着素材一起变。**听感要保持不变**，所以按 LUFS 反算：

```
上一版听感 = 素材 −6.16 LUFS + 20log10(0.45) = −13.10 LUFS
本次素材  = −10.34 LUFS  ⇒  需要的音量 = 10^((−13.10 − (−10.34))/20) = 0.728 ≈ 0.73
```

**这是「换素材必同步的两个常量」之一**（另一个是 `SFX_SECONDS`，本次仍是 5.85s，未变）：

| 常量 | 含义 | 换素材时怎么定 |
|---|---|---|
| `JellyWobble.SFX_SECONDS` | 播完才恢复 BGM、音调才复位 | `ffmpeg -i` 读 Duration |
| `JellyWobble.SFX_VOLUME` | 抵消素材电平差 | 按 LUFS 反算，保持听感一致 |

## 四、核验

- `ffmpeg -af volumedetect`：`jelly.mp3` 均值 −11.5dB / 最大 −1.3dB（旧：−7.4 / 0.0）。
- `ffmpeg -af loudnorm`：`I = −10.34 LUFS` / `TP = −1.25 dBTP`（旧：−6.16 / **+2.69**）⇒ **不再削波**。
- `_chk/audio_format_check.py`：44100Hz / mono / 64kbps（时长 5.90s 超 2s，是既有的特例）。

---

# 2026-09-18 中指·长兄专属语音（7 条，含头顶淡紫台词）

素材取自灰机 wiki **「敌方单位/1327 中指父辈 - 马蒂亚斯」**（不是 1314 那个拇指前二老板）。转码走 `_chk/import_lcb_sfx.py --target-mean -15`（语音档，同拇指那批），入库名统一 `middlefinger_*`。

## 一、素材 → 触发点对照

| 入库名 | wiki 原文件名 | 台词 | 触发点（代码落点） |
|---|---|---|---|
| `middlefinger_unseal_1.mp3` | `9SV-BAT3-06_1.ogg` | 好吧，这种程度的话该撕掉一层包装纸了！ | 解封推进到**一阶段**（`SealedSwordBase.unsealInto`） |
| `middlefinger_unseal_2.mp3` | `9SV-BAT3-06_2.ogg` | 有意思……特色是这种手感啊！ | 推进到**二阶段**（同上） |
| `middlefinger_unseal_3.mp3` | `9SV-BAT3-06_3.ogg` | 竟然让我解放到这种地步……哈！ | 推进到**莱瓦汀**（同上） |
| `middlefinger_counter.mp3` | `9SV-BAT3-BR5-1.ogg` | 给我过来！ | 忠义巡礼者使用**复仇技艺**（`VengeanceArts.useMoveOnTarget`） |
| `middlefinger_ledger_full.mp3` | `9SV-BAT3-11.ogg` | 哈啊……看来没有打开账簿的必要了。 | **复仇账簿满充能**的回合开始（`RevengeLedger.ledgerRecharge.act`） |
| `middlefinger_hit.mp3` | `9SV-BAT3-13-01.ogg` | 你这……！ | **受击**（`Hero.damage`，**15% 概率**） |
| `middlefinger_execution.mp3` | `9SV-BAT3-BR8-4-01.ogg` | 正中靶心啊！ | **盔甲技能「即刻处刑[莱瓦汀]」连段收尾**（`InstantExecution.Execution` 第 5 拍） |

**「剑 阶段N → N+1」的编号口径**：wiki 的「阶段1~4」与我们的 `STAGES` 下标差 1——wiki 的**阶段4 = 莱瓦汀全集**，所以「阶段1→2」的台词就是「推进到我们的一阶段」（`UNSEAL[targetStage - 1]`）。判据：wiki 里 4 阶段的语音全挂着「…即刻处决[莱瓦汀]」的名，可见阶段 4 = 全解形态。

**只取了 7 条**：wiki 同组还有 `-01/-02` 变体与「穿腹[莱瓦汀]」两句（这会很烫的！/非常烫！），本轮**未**按需求选用（无变体编号的那些只用基名文件）。

## 二、统一规则（用户要求）

1. **语音 + 头顶文本成对**：每条都是 `text + sound + duration` 三元组（内部 `Line` 小类），取一次同时用 ⇒ 不会「显示 A、念的 B」。
2. **头顶文本颜色 = 淡紫 `0xCBA6F7`**（`MiddleFingerVoice.LINE_COLOR`）；拇指的台词是暖米色 `0xE5CAA5`，两者刻意不同——各角色有各自的家族色。
3. **上一条没播完就整条跳过**（语音与文本一起不播）：`Sound.play` 是叠加式的，连击/连续受击不拦就糊成一团。
   判据 `lastAt > 0 && Game.timeTotal > lastAt && Game.timeTotal - lastAt < lastDuration`——时间戳**必须是 `Game.timeTotal`**（累计秒），`Game.elapsed` 是每帧增量（≈0.016s），拿它当时间戳差值恒为负、去抖形同失效。

## 三、入口类 `MiddleFingerVoice`（新文件）

`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/middlefinger/MiddleFingerVoice.java`

结构与拇指的 `ValencinaSfx` 同构（放**该角色技能所在的包**，条件/去抖/素材只写一遍），五个播放口：
`playUnseal(int newStage)` / `playCounter()` / `playLedgerFull()` / `playHitTaken()` / `playExecution()`。

**入口类自己认人**：`isMiddleFinger()`（`Dungeon.hero.heroClass == HeroClass.MIDDLE_FINGER`）——受击那条挂在**所有角色共用**的 `Hero.damage` 上，不判职业就是人人都说「你这……！」。

**时长表是硬编码的**（`Sample` 不提供「还剩多久播完」的查询，音效是整段解码进内存的），来自解码实测：

| 素材 | 解码后时长 |
|---|---|
| unseal_1 / unseal_2 / unseal_3 | 4.371 / 3.490 / 3.416 s |
| counter / ledger_full | 2.093 / 4.923 s |
| hit / execution | 1.448 / 1.160 s |

⚠️ `_chk/audio_format_check.py` 报的时长是**按帧数估算**的（unseal_1 报 4.44，实测 4.371）——**偏大**，当阈值用只会多等一会儿；要精确值得 `ffmpeg -i` 读 Duration 或解成 wav 数采样点。

## 四、「挂在哪」的判据（本轮的五处各不相同，正好凑齐一套）

| 语义 | 落点 | 本例 |
|---|---|---|
| 「使用时」 | 技能/武器内部，动作成立的那一刻 | 复仇技艺（`useMoveOnTarget` 扣完标记之后） |
| 「技能演完才播」 | 连段的**最后一拍**，不是 `activate()` | 即刻处刑（`Execution` 的 `case 5`） |
| 「获得时」 | `Buff.affect` 的**授予处** | （对照拇指的预知眼过热，见 AGENTS.md §6） |
| 「回合开始」的持续判定 | buff 的 `act()` | 账簿满充能（`ledgerRecharge.act`） |
| 「通用事件过滤」 | 调用点自己判，入口类再判一次职业 | 受击（`Hero.damage`，与泪剑计数同一条件） |

## 五、满充能语音为什么要「一次一武装」

`RevengeLedger` 新增 `maxVoiceAnnounced` 字段（已进存档，旧档缺键读成 false ⇒ 满充能时补说一次，可接受）：

```java
if (charge >= chargeCap) {
    if (!maxVoiceAnnounced) { maxVoiceAnnounced = true; MiddleFingerVoice.playLedgerFull(); }
} else {
    maxVoiceAnnounced = false;   // 掉下上限就重新武装，下次满格还会说
}
```

放在 `act()`（回合开始）而不是 `gainCharge()` 到顶那一刻：伤血冲顶多半发生在**敌人的回合**里，那时出声会和挨打的音效糊在一起。掉下上限即复位 ⇒「攒满 → 说一句 → 花掉 → 再攒满」每循环各说一次。

## 六、核验（源码级，未代跑 Gradle）

- 下载 7 条 ogg：全部 `HTTP=200` / `MAGIC=OggS`（走 `huiji-public.huijistatic.com`，文件名首字母大写坑照旧）。
- 转码 LUFS 散布 **2.25dB**（−13.62 ~ −15.87），真峰值 **全部 < 0**（−0.70 ~ −2.39 dBTP），符合语音档判据。
- `Assets.java`：7 个常量 + **全部进 `all[]`**；交叉校验**固化成脚本 `_chk/verify_sfx_assets.py`**（本轮把此前每次都临时手写的那段解析固化下来，带「前缀过滤」与「过滤词没命中即报错」两句兜底，避免空跑还报 PASS）。断言「101 个常量 ↔ 101 个文件 ↔ 101 个 `all[]` 成员，无重复无缺漏」⇒ ALL PASS；**已做反例自测**（合成一份「常量没进 all[] + all[] 里有未知名」的假 Assets 喂给同一批判据，确认会判 FAIL）。
- `javac -Xlint:all`（`MiddleFingerVoice` / `InstantExecution` / `SealedSwordBase` / `VengeanceArts` / `RevengeLedger` / `JellyWobble` / `Assets` / `Hero`）**EXIT=0**；本批只有 4 条告警，**全在未改动行**（`Hero:2311` rawtypes、`Hero:278` this-escape、`SealedSwordBase:96` rawtypes、`RevengeLedger:273` static-via-instance —— 后者是既有 `ledger.updateQuickslot()`，只是被我上面新增的行顶到了 273）。
- `check_utf8_all.py`：1428 文件全合法 UTF-8；`audio_format_check.py` 新增 7 条均为 44100/mono/64kbps；`/n/` 误写 0。

## 七、待办 / 待确认

- **本轮改动未打包**：果冻素材重转 + 中指 7 条语音（连带 0.2.5 之后所有未进包的改动：音量下调、角色 BGM、神谕 7 条、指令 2 条、拇指 17 条及其修正）。
- 语音素材时长偏长（unseal_1 4.4s、ledger_full 4.9s）：**入库音效按项目惯例是整段解码进内存的**，这三条长的会多占约 100KB 内存，可接受；若想更省可在 `_chk` 里加「按静音段切分」再入库。
- 「穿腹[莱瓦汀]」两句语音（这会很烫的！/非常烫！）与 wiki 上的 `-01/-02` 变体**本次未取**，需要再说。

---

# 2026-09-18 封印之剑系列：可附魔 / 可强化 + 切形态保留强化符石

需求原文：「修复莱瓦汀系列武器无法被注魔卷轴/附魔符石/强化符石选中的问题，以及切换形态时不保留强化符石效果的问题。」

## 一、症状与病根（两件事，互不相关）

| # | 症状 | 病根 |
|---|---|---|
| ① | 注魔秘卷 / 附魔符石 / 强化符石 的选择列表里，封印之剑系列**整格变灰、点不动**，且没有任何报错 | 三件物品的可选判据**共用一处** `ScrollOfEnchantment.enchantable()`，它以 `isUpgradable()` 当"是不是一件正常吃升级资源的装备"的代称；而本系列为了"随英雄等级成长"刻意 `isUpgradable() = false` ⇒ 被**连坐**（`InventoryPane` 里 `selector.itemSelectable(...)` 为 false 即 `enable(false)`，只灰不报错） |
| ② | 用强化符石选了「伤害/速度」后，**一切形态（解封 / 落副手封回 / 即刻处刑强制换装）效果就没了** | 形态切换＝「`Reflection.newInstance` 造新实例 + 接替槽位」⇒ **没被显式搬运的字段静默归零**。`SealedSwordBase.copyState()` 漏搬 `Weapon.augment` |

调用面（改一处即覆盖三件物品）：`ScrollOfEnchantment.enchantable()` ← 卷轴自己的 `itemSelector.itemSelectable` + `StoneOfEnchantment.usableOnItem` + `StoneOfAugmentation.usableOnItem`（**全仓只有这三个调用方**，已用脚本钉住）。被装备在主/副手的那把也会出现在列表里——`WndBag.placeItems` 会把 `weapon`/`secondWep` 一并铺出来。

## 二、改法

| 文件 | 改动 |
|---|---|
| `items/scrolls/exotic/ScrollOfEnchantment.java` | `enchantable()` 加 `\|\| item instanceof SealedSwordBase`（与既有的 `SpiritBow` / `BodyArtWeapon` 并列）+ import |
| `items/weapon/melee/SealedSwordBase.java` | `copyState()` 加 `to.augment = from.augment;`；补 `copyState()` javadoc（**搬运面＝`Weapon.storeInBundle` 的键清单**）；类 javadoc 补"不吃升级卷轴 ≠ 不能附魔" |
| `items/weapon/melee/MorphWeapon.java` | `morphInto()` 同款补 `replacement.augment = current.augment;` |
| `items_zh.properties` / `items.properties` | `sealedswordbase.level_note` 补一句「注魔秘卷 / 附魔符石 / 强化符石仍然有效」 |

三条刻意的取舍：

- **不动 `isUpgradable()`**：附魔/强化改的是 `enchantment` / `augment`，与等级无关；把它改成 true 会让升级卷轴重新能选中它、毁掉"随英雄成长"的设计。**"不能升级"与"不能附魔"是两条独立的路。**
- **搬运口径向 `ScrollOfTransmutation.changeWeapon` 看齐**：那份实现**早就搬了 `n.augment = w.augment;`**，说明"搬运 augment"是本工程的既定口径，`copyState` / `morphInto` 是漏了。
- **顺手修 `MorphWeapon`**：漆黑噤默的形态切换是同一缺陷（近战走 `MeleeWeapon.damageRoll`、远程走 `MissileWeapon.damageRoll`，两者都读 `augment`）。⚠️ **这一处超出用户原话范围**，已在回复里点明，不想要可单独回退。

## 三、核验（源码级，未代跑 Gradle）

- `javac -Xlint:all`（3 个 .java）**EXIT=0**；告警全在既有行（`ScrollOfEnchantment` 的 `unchecked`/`static`、`SealedSwordBase:100` / `MorphWeapon:66` 的 `rawtypes` 数组声明），**本次改动行 0 告警**。
- 新增 `_chk/verify_sealed_enchant.py`：①`enchantable()` 放行封印之剑系列且不丢钻石剑排除；②`isUpgradable()` **仍返回 false**；③`ScrollOfUpgrade.usableOnItem` 仍以它为准（把意图钉死）；④三件物品仍都走 `enchantable`，且**全仓没有新的第四调用方**；⑤`copyState` / `morphInto` 都搬 `augment`，并按 `Weapon.storeInBundle` 的 8 个字段逐条断言搬运完整性。**已做反例自测**（旧 `enchantable` / 旧 `copyState` / 旧 `morphInto` / 把 `isUpgradable` 改成 true，四种写法都判 FAIL）。
- 新增 `_chk/SealedSwordLocCheck.java`：真 `Properties.load` 读 `items_zh` / `items`，22 键 ×2 语言——四形态 name/desc/stats_desc + 系列 10 条动作文本 + 改后的 `level_note`；断言 `level_note` 点名三件物品、保留「不能吃升级卷轴 / 22」，并做 `_` 标记奇偶、U+FFFD、`/n/` 三项体检。
- 回归：`check_utf8_all` 1428 文件 OK；`verify_armor` 26×2 / `InstExecLocCheck` 19×2 / `ClassDescCheck` / `verify_selfharm_exempt` / `verify_masterring_bind` 全 PASS。

## 四、待办 / 待确认

- **本轮未打包**（与前面所有未进包的改动一起）。
- 待游戏内验证：① 三件物品都能在列表里选中封印之剑系列（含已装备在主/副手的那把）；② 强化符石选「伤害」后**来回切几次形态**，面板的"伤害强化"文字与掷值区间都不掉；③ 附魔后切形态，附魔仍在。
- 待确认：`MorphWeapon` 那处是否保留（超出原话范围）。

# 2026-09-18 中指·复仇技艺打击音（wiki「技能0」三条）

## 一、问题：踏碎「只有语音、没有打击音」

五式复仇技艺里，踢飞 / 穿腹 / 仇怨重踏 / 即刻处刑 都播原版 `Assets.Sounds.HIT_STRONG`，**只有踏碎一声不响**。

根因不是「坏了」而是**一直缺**：原版的命中音全都挂在 `hero.attack(...)` 返回 `true` 的那个分支里，
而踏碎**不走 `hero.attack`**（直接 `Hero.heroDamageIntRange` → `enemy.damage`，无命中判定、必中），
没有任何一行代码播过它。**判据：先看这个技能走不走 `hero.attack`，不走的一律要自己补播放口。**

## 二、素材：wiki「技能0」列表的第 1、2、5 条

wiki 页「敌方单位/1327 中指父辈」的**音效**表里，`技能0` 一行共 11 个文件，按列表顺序取第 1、2、5 个：

| 入库名 | wiki 原文件 | 源（48kHz 立体声 vorbis） | 入库（44100/mono/64k） |
|---|---|---|---|
| `middlefinger_strike_1.mp3` | `middlefather_0_1.ogg` | 2.22s / −11.97 LUFS / **TP +1.40** | 1.93s / −20.73 LUFS / TP −3.24 |
| `middlefinger_strike_2.mp3` | `middlefather_0_3.ogg` | 1.91s / −10.52 LUFS / **TP +0.98** | 1.51s / −18.32 LUFS / TP −1.82 |
| `middlefinger_strike_3.mp3` | `middlefather_0_5.ogg` | 2.36s / −12.50 LUFS / **TP +0.59** | 1.41s / −18.46 LUFS / TP −1.49 |

⚠️ **「第 1、2、5 条」按列表位置计**，不是按文件名里的数字——这组编号本身有跳跃（**没有 `middlefather_0_2`**，
列表是 `_1 / _3 / _4-1 / _4-2 / _5 / _6 …`），按数字找会缺一条。

三条源素材**真峰值全是正的**（+0.59 ~ +1.40 dBTP，又是推过满刻度的那类），所以转码增益是负的
（−5.00 / −4.30 / −3.30 dB），走音效档 `--target-mean -20`（与语音档 −15 区分）。入库后真峰值全部 < 0。

## 三、落点：五式统一（`VengeanceArts`）

| 招式 | 代码位置 | 原来 | 现在 |
|---|---|---|---|
| 1 踏碎 | `doStomp`（伤害掷值之后、`enemy.damage` 之前） | **（无）** | `playStrike()` |
| 2 踢飞 | `doKick` 的 `strike` 回调 | `HIT_STRONG` | `playStrike()` |
| 3 穿腹 | `doPierce` 的逐个目标循环 | `HIT_STRONG` | `playStrike()` |
| 4 仇怨重踏 | `doRestomp` 的每段循环 | `HIT_STRONG` | `playStrike()` |
| 5 即刻处刑 | `doExecution` 的三段循环 | `HIT_STRONG` | `playStrike()` |

`VengeanceArts` 里 `Assets` / `Sample` 两个 import 因此**不再被使用，已一并移除**（避免后人以为这里还播原版音效）。

## 四、打击音与语音**分通道**（关键）

`MiddleFingerVoice.playStrike()` **不参与语音的「上一条没播完就跳过」去抖**：

```java
public static void playStrike(){
    if ( !isMiddleFinger() ) return;
    Sample.INSTANCE.play( Random.element( STRIKE ) );   // 没有 busy() 检查
}
```

原因：语音的去抖窗口是 **1~5 秒**（`unseal_1` 4.37s、`ledger_full` 4.92s），而复仇技艺是「喊一句台词 + 同时打出去」——
若打击音共用那条通道，**刚喊完「给我过来！」的那几秒里打击音会被整段吞掉**，等于白接。分开后两者互不干扰：
语音照旧只受自己的时长约束，打击音每次命中都响。

## 五、核验（源码级，未代跑 Gradle）

- 三条 ogg：全部 `HTTP=200` / `MAGIC=OggS`；入库后 `44100Hz / mono / 64kbps`，LUFS 散布 **2.41dB**
  （−18.32 ~ −20.73，落在项目现有音效 −18.9~−22.6 区间），**真峰值全部 < 0**。
- `Assets.java`：3 个常量 + 全部进 `all[]`；`verify_sfx_assets.py` = **ALL PASS（104 常量 ↔ 104 文件 ↔ 104 `all[]` 成员）**。
- `javac -proc:none -Xlint:all`（`VengeanceArts` / `MiddleFingerVoice` / `Assets`）**EXIT=0**，本批文件 **0 条告警**。
- `check_utf8_all.py`：1428 文件全合法 UTF-8。

## 六、待确认

- **叠音**：新素材 1.4~1.9 秒，比原版 `HIT_STRONG`（约 0.3 秒）**长 5 倍**。穿腹（多目标）、仇怨重踏（三段）、
  即刻处刑（三段）都是连击，每段都播 ⇒ 会叠 2~3 层（与拇指「处置」四段斩击同一类问题）。要避免的话两个旋钮：
  ①给打击音也加去抖（比如 0.35s 最短间隔）；②只在首段播。**本轮按「每段都响」实现**。
- 本轮（果冻重转、中指 7 条语音、中指 3 条打击音）及 0.2.5 之后的所有改动**仍未打包**。

# 2026-09-18 环指大师·击杀播报（6 条，含 2 条「提比娅」条件条目）

素材取自灰机 wiki **「敌方单位/1331 卡利斯托」**。需求：**全部**作为**击杀时**随机播报（10% 概率），
若已有语音正在播放则不播，头顶显示**米色**台词。转码走 `_chk/import_lcb_sfx.py --target-mean -15`（语音档）。

## 一、素材 → 内容对照

| 入库名 | wiki 原文件 | 台词 | 候选条件 |
|---|---|---|---|
| `ringmaster_kill_1.mp3` | `9SV-BAT15-01.ogg` | 让我们一起剖析，你身体中残存的美吧？ | 常驻 |
| `ringmaster_kill_2.mp3` | `9SV-BAT15-06-2.ogg` | 观众的积极参与……不错呢！ | 常驻 |
| `ringmaster_kill_3.mp3` | `9SV-BAT15-11.ogg` | 我将为你生动呈现，人体派的艺术！ | 常驻 |
| `ringmaster_kill_4.mp3` | `9SV-BAT15-10-01.ogg` | 你觉得如何？请快点……把你的感想告诉我吧！ | 常驻 |
| `ringmaster_kill_tibia_1.mp3` | `9SV-BAT15-04.ogg` | 你听见了吗？那由提比娅的一对肱骨与二十四根肋骨奏响的旋律！ | **装备提比娅** |
| `ringmaster_kill_tibia_2.mp3` | `9SV-BAT15-04-01.ogg` | 静脉和动脉，当你流出的两种不同颜色的血交融之时，啊啊……！真是艺术啊！ | **装备提比娅** |

⚠️ 第 4 条在 wiki 上的标题是「提比娅的旋律…**拼点失败时 2**」——同组还有一条不带 `2` 的「…拼点失败时」（台词是「那双眼睛……那副肉体……就是你了」），**只取了带 `2` 的那条**。
第 5、6 条在 wiki 上标的是「…使用时 强力的必杀技」（及 `2`），我方按需求**统一改作击杀播报**使用。

## 二、落点：`Mob.die` 的两处

```
Mob.die(cause)
├─ 英雄击杀白名单分支（cause == hero || Weapon || Enchantment || MagicStrike ‖ ResinCoating ‖ Electricity）
│     └─ RingMasterVoice.onEnemyKilled(Dungeon.hero);     ← 新增
└─ 流血致死补发分支（cause instanceof Bleeding && 职业是环指大师）
      └─ RingMasterVoice.onEnemyKilled(Dungeon.hero);     ← 新增
```

两个分支**互斥**（`Bleeding` 不在白名单里），所以一次死亡最多触发一次播报，不会有双重概率。

**这是「击杀播报」这类需求的唯一正确落点**：`Mob.die` 的击杀归属是**枚举式白名单**（`cause == Dungeon.hero ||
cause instanceof Weapon || cause instanceof Xxx`），自带伤害（火焰/流血/涂层/法术）各有各的特质类，
**没有一处统一的 `onHeroKilled` 钩子**。漏了某个 `cause` 类型，那种击杀就静默不播报。

## 三、入口类 `RingMasterVoice`（新文件）

`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/abilities/ringmaster/RingMasterVoice.java`

结构照搬 `MiddleFingerVoice`（`Line{text, sound, duration}` 三元组成对、`Game.timeTotal` 去抖、入口类自己认职业），
三处差异：

1. **只有一个播放口 `onEnemyKilled(hero)`**（本角色的触发点唯一），顺序：职业判定 → **10% 概率** → **`busy()` 跳过** → 随机取一条。
2. **条件条目**：`Line.tibiaOnly`，`pick()` 里按 `hero.belongings.weapon() instanceof LifeWorkTibia` 现算候选池
   ——**每次击杀都重算**，换武器的当场就生效（不做缓存）。
3. **配色 = 米色 `0xE5CAA5`**（wiki 台词统一色，与拇指同色）。

## 四、核验（源码级，未代跑 Gradle）

- 6 条 ogg：全部 `HTTP=200` / `MAGIC=OggS`；入库 `44100Hz / mono / 64kbps`。
- 源素材**全部顶格**（真峰值 > 0）⇒ 转码时 6 条全走 `[限幅]` 补救分支，增益 −0.90 ~ 0.00 dB。
- 入库后 **LUFS 散布仅 0.94 dB**（−14.65 ~ −15.59），**真峰值全部 < 0**（−1.36 ~ −2.10 dBTP）。
- 解码实测时长：4.60 / 3.81 / 3.34 / 5.59 / **6.51 / 9.05** 秒（后两条是长句）。
- `verify_sfx_assets.py` = **ALL PASS（110 常量 ↔ 110 文件 ↔ 110 `all[]` 成员）**。
- `javac -Xlint:all`（`RingMasterVoice` / `Mob` / `Assets`）**EXIT=0**，本批文件 **0 条告警**。
- `check_utf8_all.py`：1429 文件全合法 UTF-8。

## 五、待确认

- **时长**：`tibia_2` 长 **9.05 秒**、`tibia_1` 6.51 秒。去抖窗口等于素材时长 ⇒ 说完这两条之后，接下来 9 秒内的击杀
  都不会再播报（连杀时体感是「安静了一阵」）。若嫌长，两个选项：①裁掉句尾静音（脚本已按 −50dB/0.1s 裁过，这是真实语音长度）；
  ②给「随机池」加权，让长句出现率更低。**本轮按原样**。
- `Mob.die` 里 `Dungeon.hero` **未做 null 保护**（既有代码风格如此，`RingMasterLoot` 也是直接取字段）；
  `RingMasterVoice.onEnemyKilled` 内部自己判了 null，不会因此崩。

---

# 2026-09-18 拇指·前二老板与中指长兄的职业遗物（破损义眼 / 账簿残页）

两位职业此前都复用战士的纹章残蜡（`RemainsItem.get()` 里写着 `//占位复用战士遗物`），本轮换成专属遗物。

## 一、改动清单

| 文件 | 改动 |
| --- | --- |
| `sprites/ItemSpriteSheet.java` | 容器段新增 `BROKEN_EYE = xy(11,3)`（12×14）与 `LEDGER_PAGE = xy(12,3)`（11×13），并补两条 `assignItemRect`。两格紧邻既有的破损终端 `xy(9,3)` / 断裂骨刃 `xy(10,3)`，都是**用户已绘制**的贴图。 |
| `items/remains/BrokenEye.java`（新） | `extends RemainsItem`，`image = BROKEN_EYE`，`doEffect` 调 `StoneOfClairvoyance.reveal(hero.pos)`。 |
| `items/remains/LedgerPage.java`（新） | `extends RemainsItem`，`image = LEDGER_PAGE`，常量 `BONUS = 1` / `TURNS = 100`，`doEffect` 调 `TempStrength.apply(hero, BONUS, TURNS)` 并飘 `FloatingText.STRENGTH`。 |
| `items/remains/RemainsItem.java` | `case VALENCINA` → `new BrokenEye()`；`case MIDDLE_FINGER` → `new LedgerPage()`（占位 `SealShard` 已删）。 |
| `items/stones/StoneOfClairvoyance.java` | 新增公开静态入口 `reveal(int cell)`，**内部复用** `activate(cell)`——不另写第二份地图揭露逻辑。 |
| `journal/Catalog.java` | `MISC_CONSUMABLES` 注册 `BrokenBoneBlade` / `BrokenEye` / `LedgerPage`。 |
| `messages/items/items_zh.properties`、`items.properties` | 新增 `items.remains.brokeneye.*` / `items.remains.ledgerpage.*`（zh + en）。 |

## 二、效果

- **破损义眼**（拇指）：以使用者为中心触发一次**明示符石**——揭示周围 20 格地图并挖出其中的隐藏门/陷阱。
  音效与特效都由符石代码自己播放（`TELEPORT`，发现密室时补 `SECRET`），遗物使用后消散。
- **账簿残页**（中指）：获得 **1 点临时力量、持续 100 回合**。复用中指 T1「健身一餐」的 `TempStrength`：
  覆盖式刷新、计入伤害/装备需求等一切力量判定（汇聚点是 `Hero.STR()`），头顶飘一次力量图标。

## 三、顺手修的两处既有缺口

1. **`items.remains.brokenterminal.*` 的 en 侧一直是空的**（神谕代行者那轮只写了 zh）⇒ 英文语言下物品名会直接
   显示成键名。本轮补齐 `broken terminal` 的 name/desc，并把它写进 `_chk/RemainsLocCheck.java` 的回归断言。
2. **`BrokenBoneBlade` 没进图鉴**（2026-09-05 环指大师那轮漏了）⇒ `Catalog.countUses` 只对已注册的类计数，
   断刃在日志里永远不会出现。本轮一并补上。

## 四、坑与判据

- **常量必须写在 `assignItemRect` 所在的 `static{}` 之**前**：Java 静态初始化按源码顺序执行，常量写在块后就等于
  传 0 进 `assignItemRect`，**所有物品贴图整体错位**且不报错。`verify_remains_relics.py` 会断言这一点。
- **撞格**：`xy(col,row)` 相撞时后画的覆盖先画的，同样静默。脚本顺带扫全文件查重。
- **别绕开 `reveal` 直接调 `activate`**：`activate` 是 `protected`，遗物包里调不到；脚本断言 `Runestone.activate`
  仍是 `protected abstract`，防止有人图省事把它改成 `public`。
- **遗物描述不带「使用后消散」**：魔改新增的遗物（破损终端 / 断裂骨刃 / 本轮两件）都按用户给的原文写，
  与上游那批（纹章残蜡等）的句式不同，这是既有取舍，别去「统一」。

## 五、核验（源码级，未代跑 Gradle）

- `javac -proc:none -Xlint:all`（6 个 java 文件）**EXIT=0**；本批文件 0 条告警；
  `Catalog.java` 的 4 条 `[rawtypes]` 落在 `new Class[0]`（242/244/249/381 行，上游写法，`git diff` 确认不在 hunk 内）。
- `_chk/verify_remains_relics.py` = **ALL PASS**（30 条断言 × 6 文件，含 6 条**反例自测**）。
- `_chk/RemainsLocCheck.java` = **ALL PASS**（4 新键 + 8 回归键 × 2 语言；真 `Properties.load` + 真 `String.format`）。
- `_chk/verify_remains_sprites.py` = **ALL PASS**（items.png 逐像素解码：破损义眼 148 / 账簿残页 125 个不透明像素，
  且**无像素溢出到声明矩形之外**）。
- `_chk/check_utf8_all.py` = 1431 文件全合法 UTF-8。

---

# 2026-09-18 出 v0.3.0（新增 EGOPD 自家改动栏）

## 一、改动清单

| 文件 | 改动 |
| --- | --- |
| `build.gradle` | `appVersionCode 925 → 930`、`appVersionName '0.2.5' → '0.3.0'`（沿用编码 `900 + minor*10 + patch`：0.3.0 ⇒ 930）。 |
| `ui/changelist/EGOPD_Changes.java`（新） | EGOPD 自家改动栏的内容载体。当前只有一个 major 条目 `EGOPD v0.3.0` + 一句说明（**正文暂空**），后续版本按该文件内的注释格式追加条目。 |
| `scenes/ChangesScene.java` | 新增 `EGOPD_TAB = 0`；页签按钮 8 → 9 个并改为**两行排布**（面板高 `h-36` → `h-55`）；switch 由 `case 0..7` 扩到 `case 0..8`（EGOPD = 0，版本页整体顺延 1）；`lang_warn`（「改动详情仅提供英文版本」）对 EGOPD 页不再显示。 |

## 二、页签布局（为什么必须改成两行）

- 上游 8 个页签（3.X / 2.X / 1.X / 0.9 / 0.8 / 0.7 / 0.6 / 0.5-）**正好占满一行**：按 `前一个.right()-2` 递推，右端到 **141px**，而可用宽度只有约 **137px**，
  本来就已经溢出 4px ⇒ 塞不下第 9 个。
- 新布局：第 1 行 `EGOPD(32) + 3.X~0.9`（右端 100px）、第 2 行 `0.8~0.5-`（右端 73px），两行都远在 135px 内。
- 纵向为什么只加 19px 就够：页签按钮是 `addToBack()` 添加的，会被后面 `add()` 的列表面板**盖住**，所以只能排在 `list.bottom()` **以下**；
  而 `list.bottom()` 到可用区底部原本只有 19px（正好一行，选中态 19 高时已经贴死底边）。把面板高减 19 后，
  可用空间变 38px = 两行 × 19px，恰好放下；原 panel 底部留白 19px 也从「闲置」变成了第二行。
- 默认页签为 **EGOPD**（`changesSelected = 0`）；`case 1: default:` 承接未知值，行为等同上游（回落到 3.X）。

## 三、核验（源码级 + 打包后）

- `javac -proc:none -encoding UTF-8`（`ChangesScene` + `EGOPD_Changes`）**EXIT=0**；`check_utf8_all.py` 1432 文件全合法 UTF-8。
- 索引一致性脚本核对：`case 0..8` 连续；9 个按钮的 `changesSelected = N;`、`setRect(...changesSelected == N ? 19 : 15)` 与 case 一一对应（0~8 无缺漏）。
- 页签几何复刻：两行右端 100 / 73 ≤ 可用 135，无越界。
- `:android:assembleDebug` **BUILD SUCCESSFUL 23s**（`:core:compileJava`、`:android:packageDebug` 均 executed）→
  `versionCode=930` / `versionName=0.3.0-INDEV` / `application-label:'EGOPD'` /
  `application-icon-*` 仍全部指向 `mipmap-anydpi-v26/ic_launcher.xml`；`applicationId=com.mypd.mypixeldungeon.indev` **未变**（移动端存档不受影响）。
- dex 内可搜到 `EGOPD_Changes`（3 次）、`EGOPD v0.3.0` 与中文提示各 1 次 ⇒ 新类与新文本确实进包。
- **全量资产比对**：APK 内 533 条 `assets/*` 与工作区 `core/src/main/assets` **逐条 md5 一致（533/533）**。
- 产物归档 **`EGOPD_0.3.0.APK`**（49,366,899 B，md5 `052da48e…`）。
- `ios/robovm.properties` 由构建自动重写为 0.3.0/930（未跟踪的生成产物，无需手改）。

## 四、坑与判据

- **体积对比会被旧包的「空洞」骗**（本轮最大教训）：ZipFlinger 的增量打包会把变小的条目原地重写，旧的更长数据留在原位成为垃圾。
  实测 `EGOPD_0.2.5.APK` 有 **259 处、共 898,040 字节空洞**（最大一处 293KB 在 `classes3.dex` 前），本次重打包只剩 502 字节（仅 PNG 的 3 字节对齐缝）
  ⇒ 两包同样是 533 条 assets、**压缩总量只差 223 字节**，文件却小了 897KB（50,264,234 → 49,366,899）。
  **判断「内容变没变」只认逐条 md5，不认文件大小。**
- **本版与 0.2.5 的 assets 内容完全相同**：工作区 `git status` 显示 26 个已改 + 81 个新增素材，但逐条比对下来与 0.2.5 包内**无一处差异**
  （`sprites/items.png` 的 mtime 23:11 晚于旧包归档的 22:59，字节却没变）。⇒ 若期望这一版能看到素材层面的变化，需确认改动确实保存进了
  `core/src/main/assets/` 下的目标文件（根目录的 `精灵-*.png` 与 assets 内的同名文件**并不相同**）。
- **`assets/` 里混着工作源文件**：`.aseprite` 工程文件与中文名 PNG（`interfaces/精灵-0001.aseprite`、`sprites/精灵-0001.png`、
  `splashes/提比亚.png` 等 **9 个、共 1.14MB**）会被原样打进 APK。要瘦身或避免外泄源文件应从 assets 移出（**本轮未动**，留待用户决定）。
- **Python `zipfile` 的中文名陷阱**：非 UTF-8 标记的条目名会按 cp437 解码，比对前须 `name.encode('cp437').decode('utf-8')` 还原，否则误报「找不到源文件」。

# 2026-09-19 地形编辑器（可视化画房间 → 生成可编译的 paint()）

> 面向「新层级开发」的工具链：在浏览器里**直观画**房间地形，一键生成能直接编译的
> `StandardRoom` 子类 `paint()`。使用说明见 `docs/terrain-editor-guide.md`。

## 一、动机与范围

做新楼层时，房间地形原本只能手写 `Painter.fill/set` 坐标、改一次编译一次才知道长什么样。
本工具把「画 → 看 → 出码」这一环做成离线可用的网页，**不需要跑游戏**：
- 画布用的是**游戏真实图集 + 真实 4 层渲染算法**（含墙面自动缝合、草叶跨格拼接、水/深渊边缘过渡），所见即所得；
- 输出是**可编译**的 Java，不是伪码——已用真 JDK 编译验证（见第五节）。

## 二、文件清单

| 文件 | 行数/大小 | 职责 |
| --- | --- | --- |
| `tools/terrain-editor/index.html` | UI 骨架 | 双击即用，无外部依赖（图集 base64 内联） |
| `tools/terrain-editor/app.js` | ~1440 行 | UI 装配：画布交互、工具栏、覆盖层面板、**外部图集导入 + IndexedDB 持久化**、**区域随机面板**、**尺寸模糊化面板**、检查器渲染、导入导出 |
| `tools/terrain-editor/editor.js` | ~1030 行 | 编辑内核：网格、**陷阱/植物/道具三层**、**区域随机标记与拓扑合并**、**尺寸模糊化模型 + `safeRect()` 安全区**、撤销栈、7 种工具算法（+2 种区域工具）、宏命令、不变量检查 |
| `tools/terrain-editor/render.js` | ~1155 行 | 渲染引擎（水层 + 4 层地形 + 草叶/陷阱/植物 + **道具占位图** + **区域标记叠加层** + **安全区参考线/越界遮罩**，**手抄 Java**，帧号对齐） |
| `tools/terrain-editor/codegen.js` | ~945 行 | Java 代码生成器（矩形合并 + `setTrap`/`plant`/`drop` 三层 + **区域多重集打乱** + **尺寸模糊化：`sizeCatProbs()`+`setSize()` 成对生成 + 越界夹取** + 落地清单） |
| `tools/terrain-editor/assets.js` | 332,095 B | **生成物**，勿手改；6 张 `tiles_*.png` + `terrain_features.png` + 水帧 + **`items.png`** 的 base64 |

> `app.js` 在 C.3 涨了约 380 行（外部图集注册表 + IndexedDB + 导入导出 + 探针），
> 是**唯一**持有 "外部图集" 状态的文件 —— 它刻意把外部图集做成**一等公民**：
> 与内置图集共用同一个 `SHEETS` 注册表、同一套 key 查找，UI 与 codegen 都不需要区分来源
> （只有下拉标签多个 ★）。
>
> C.5 又把「区域随机」拆成**内核持数据、渲染只读、codegen 只算**三层：`E.randoms`
> 是编辑器内核里的纯元数据（形如 `{l,t,r,b}` 的矩形数组，**不占 `map[]`**），
> `render.js` 只负责把它画出来，`codegen.js` 只负责把它变成 `Random.shuffle`。
> 三者之间只靠 `setRegions(list)` 单向传数据 —— 这样「区域」永远不会意外混进地形快照。

## 三、关键技术决策

### 3.1 渲染引擎为什么要「手抄」

画布要显示**游戏里真实的样子**，而游戏的渲染是 4 个 tilemap 层按 z 序各算各的帧号
（`DungeonTerrainTilemap` → `TerrainFeaturesTilemap` → `RaisedTerrainTilemap` → `DungeonWallsTilemap`），
帧号公式散在 `DungeonTileSheet` 的十几个静态方法里（`getVisualWithAlts` / `stitchInternalWallTile` /
`stitchWallOverhangTile` / `getRaisedWallTile` / `getRaisedDoorTile` …）。
`render.js` 把这套算法**逐条重写**成 JS，常量表与 Java 侧一一对应。

**手抄就有抄错的风险**，所以配了**独立核验**（第五节 5.1）：纯 Python 再实现一遍同一套算法，
两边跑同一批地图、逐 (格 × 层) 比对帧号。

### 3.2 第三层覆盖物：道具（C.4，2026-09-19）

在陷阱/植物之后，加了**道具层** —— 71 种物品 × 7 种堆类型，定点摆放在地板上。

**物品与陷阱/植物的本质区别**：物品根本不在地图格里。它们存在 `Level.heaps`
（`SparseArray<Heap>`），每个 `Heap` 自带一个 `ItemSprite`、挂进 `GameScene` 的 **objects 组**，
**地图格本身仍是普通地板**。⇒ 画道具**不改变地形**，同一格可以既是草地又是宝箱。

| 维度 | 陷阱 / 植物 | 道具 |
| --- | --- | --- |
| 存储 | `Level.traps` / `Level.plants`（与 `map[]` 平行） | `Level.heaps`（与 `map[]` 平行） |
| 渲染来源 | `TerrainFeaturesTilemap`（tilemap 层） | `GameScene` 的 objects 组（`ItemSprite`） |
| 地形联动 | **必须**把地形写成 `TRAP` 系（否则「有对象没地板皮」） | **不许**改地形（改了就是错的） |
| 定点 API | `level.setTrap(new X(), pos)` / `level.plant(new X.Seed(), pos)` | `level.drop(Item, cell)` → `Heap` |
| 位置检查 | 放在 `WALL`/`WATER`/`CHASM` 上 ⇒ ⚠ | 只查 `WALL` ⇒ ⚠ |

**定点投放的唯一入口是 `Level.drop( Item, int cell )`**（返回 `Heap`，可链式
`level.drop(...).type = Heap.Type.CHEST`）。**`addItemToSpawn(Item)` 是错的** ——
它只把物品推进 `itemsToSpawn`，随后 `RegularLevel.createItems()` 通过 `randomDropCell()` 消费
⇒ **落点随机**，而且**编译完全能过**，属纯静默错误（详见第六之二节 2）。
上游先例证明 `paint()` 里 `drop` 是合法写法：`RingRoom.placeCenterDetail`、`MassGraveRoom`、
`RatChestChasmRoom`、`RatKingRoom`。

**占位图统一用「口粮」**（用户要求：不渲染真实图标）。帧号是**算出来的**：
`ItemSpriteSheet` 的 `SIZE=16`、`TX_WIDTH=256` ⇒ `WIDTH=16`；
`xy(x,y){x-=1;y-=1;return x+WIDTH*y;}` ⇒ `FOOD=xy(1,28)=432`、**`RATION=FOOD+5=437`**
⇒ col 5 / row 27 / 像素 `(80,432)`。`items.png` 在 `core/src/main/assets/sprites/`
（**不在** `environment/`），单独作为 `S.itemSheet` 加载。

两个反直觉点（都摔过）：
- **`Sheet.prototype.draw()` 对任何合法索引都返回 `true`**（哪怕整帧全透明）⇒
  `if (!drew)` **不是**空帧判据，必须用 `solidCount(idx) > 0`；
- 帧号是 **0-based** 的 `(item % WIDTH) * SIZE`，按 `(col-1)*16` 算会得到「437 是空帧」的**假结论**。

**UI**：覆盖层面板从三态扩成**四态**（不画层 / 陷阱层 / 植物层 / **道具层**），
新增「道具类型」（71 项 / 7 组）与「堆类型」（7 种）两个下拉；
道具标记画在**格子正中**（黄点=普通堆 / `#ffcc44`=宝箱 / `#e08adf`=墓穴·骸骨等特殊堆）。
JSON 工程文件升到 **`version: 3`**（v1 纯地形 / v2 加陷阱植物 / v3 加道具），
**三个版本都能读**（缺的层按空层）。

### 3.3 代码生成用「矩形合并」而非逐格展开

朴素做法是每格一条 `Painter.set`，一个 12×12 房间会产出近 200 行怪物。
生成器改为：**把同地形的最大矩形合并成一条 `Painter.fill`**，零星单格才退化成 `Painter.set`。

实测样例（12×12 含 5×4 草地 + 3×3 深渊 + 1 格宽水渠 + 3 个孤点）：

```
生成代码 67 行；Painter.fill 6 个，Painter.set 11 个   ← 共 17 条语句
```

合并算法是贪心整行扫描（先向右扩到本行末尾，再整行向下扩，要求每格同值且未被占用），
输出顺序按**地形面积从大到小**排 —— 读起来更接近人手写的 `paint()`。

### 3.4 坐标为什么是 `left + x` / `top + y`

`Room extends Rect`，`left/top/right/bottom` 是 `RegularPainter` 实际分配的房间矩形（**闭区间**）。
`paint()` 里只能用相对偏移，所以画布上的 `(x, y)` 直译成 `left + x, top + y`，
**画在哪就落在哪**，不需要使用者自己算 ±1。

生成器用 `Painter.fill(level, l, t, w, h, v)` 的**宽高重载**而不是 `l,t,r,b` 重载：
宽高从画布直接得来，省掉一次 ±1 换算，少一个出错点（`Rect` 的闭区间语义是上游最常踩的坑）。

### 3.5 两个下拉框的区别（呼应 §6 的草地陷阱）

画布预览受两个下拉框影响，且**只有第一个影响生成的代码**（代码只写 `Terrain` 常量）：

- **图集** → `Level.tilesTex()` 那张 256×256（6 张可选，含 `tiles_lob`）；
- **草叶区**（0–4）→ 草叶细节来自**另一张全局唯一的** `environment/terrain_features.png`，
  按「区」取帧。这正是 2026-09-19 修的「27 层草地显示成五区蘑菇形」那个 bug 的手工版
  —— 编辑器把它做成了可切换的预览选项，方便新楼层对齐草叶区。

### 3.6 陷阱 / 植物是**独立对象层**，不是地形（2026-09-19 新增）

做「覆盖层」编辑时挖出的结论，也是最值得记住的一条：

> **`Terrain.TRAP` 只是"这格下面有个陷阱"的占位符**，真正让陷阱存在的是
> `Level.traps`（`SparseArray<Trap>`，与 `map[]` **平行**的另一套）。
> 只写地形 ⇒ `TerrainFeaturesTilemap` 什么都不画，看起来就是一块普通地板。

`TerrainFeaturesTilemap.getTileVisual(pos, tile, flat)` 的判断顺序是：

```
① traps.get(pos)  命中 ⇒ (trap.visible ? (trap.active ? trap.color : Trap.BLACK) + trap.shape*16 : -1)
② plants.get(pos) 命中 ⇒ plant.image + 7*16
③ 才轮到 HIGH_GRASS(9+16*stage+alt) / FURROWED_GRASS(11+…) / GRASS(13+…) / EMBERS(9+16*5+alt)
```

编辑器把这条链**原样**搬进 `render.js` 的 `featuresVisual()`，并建了两张手抄表：

| 表 | 规模 | 内容 |
| --- | --- | --- |
| `TRAPS` | 33 项 | 类名 / 颜色（`RED=0…BLACK=8`）/ 形状（`DOTS=0…LARGE_DOT=6`）/ 中文名 |
| `PLANTS` | 13 项 | 类名 / `image` 0–12 / 中文名 |

帧号：陷阱 `color + shape*16`（`FrostTrap` = WHITE(6) + STARS(3)*16 = **54**），
植物 `image + 7*16`（`Sungrass` = 3 + 112 = **115**）。

**两个静默陷阱**：

- `TenguDartTrap extends PoisonDartTrap`、`GnollRockfallTrap extends RockfallTrap`
  —— 颜色/形状是**继承**来的，核验脚本必须解析继承（上溯到 5 层）才不会误报缺失。
- `visible=false`（未发现）时 `getTileVisual` 返回 **`-1`（不画）**，地形侧对应 `SECRET_TRAP`；
  `active=false` 时用 **`BLACK`（第 8 号色）** 而不是原色。

### 3.7 水体必须单独垫底（同批修的 bug）

**水在编辑器里曾经和深渊长得一模一样**（都是黑的）。原因：

```java
// DungeonTerrainTilemap
return super.needsRender(pos) && data[pos] != DungeonTileSheet.WATER;
```

纯水格**被 tilemap 跳过** —— 因为水不是 tilemap 画的，而是 `GameScene` 里一个单独的
`SkinnedBlock`（5 帧 `water0..water4`，每帧 **32×32 = 2×2 格**，`autoAdjust` 平铺全图）。
编辑器只抄了「跳过」没抄「垫底」，水格下面直接漏出黑色背景。

**修法**：`render()` 先铺 **layer 0**（32×32 平铺水帧）再走地形 pass；地形 pass 里
水格只在**是基础水帧**（`wv === F.WATER`，即缝合位 `r === 0`）时跳过，**缝合边缘帧照画**
（否则岸线缺一圈）。缝合位：上 +1 / 右 +2 / 下 +4 / 左 +8；地图边界外 `at()` 返 `-1`，**不可缝合**。

## 四、编辑器内建的正确性护栏

不是纯画图板，会把 `StandardRoom` 的常见踩坑点做成**即时提示**（每次改动后重算）：

| 级别 | 触发条件 | 对应上游坑 |
| --- | --- | --- |
| ⚠ | 外圈有格既不是墙也不是门 | `paint()` 默认把外圈当墙，刷成草会漏风 |
| ⚠ | 门出现在内圈 | **门是房间边界的语义**；`placeDoors` 只在外圈找门位，内圈的门不产生连接 |
| ⚠ | 可通行格被墙隔成孤岛 | 玩家走不过去 |
| ⚠ | 内圈全是 `CHASM` | 玩家无处落脚 |
| ⚠ | 陷阱被放在 `WALL` / `WATER` / `CHASM` 上 | 走不到（`WATER` 上更是永远触发不了） |
| ⚠ | 道具被放在 `WALL` 上 | 墙里的 `Heap` 玩家永远拿不到（道具**只查墙**，不像陷阱那样要求地面是 `TRAP` 系） |
| ℹ | 高草被墙完全包住 / 一个门都没有 | 确认是否刻意 |
| ℹ | 陷阱 / 植物 / 道具计数 | 提醒「它们是独立对象层，不是地形」，顺便确认没漏画 |

「锁定外圈」开关默认打开：外圈只允许放墙类地形，防手滑。

## 五、核验

### 5.1 渲染引擎帧号（`_chk/verify_terrain_editor_frames.py`）

纯 Python **独立重实现**同一套 4 层算法，再经 Node 加载 `render.js` 跑同一批地图，逐 (格 × 层) 比对。

```
构造 5 张测试地图，共 1710 格
  综合房间 depth=1 stage=0     24x20  depth=1   stage=0
  综合房间 depth=24 stage=4    24x20  depth=24  stage=4
  综合房间 depth=9 stage=1     24x20  depth=9   stage=1
  水/深渊缝合                   12x12  depth=1   stage=0
  全地形单格枚举                  42x3  depth=1   stage=0

比对 6840 个 (格 × 层) 组合：一致 6840，不一致 0
✅ 编辑器渲染引擎四层帧号与 Java 算法逐格一致

反例自测：把 render.js 的 RAISED_HIGH_GRASS 挪 1 帧，应报不一致…
  ✓ 反例被检出（22 处不一致）⇒ 判据有效
```

### 5.2 代码生成器反向解释（`_chk/verify_terrain_codegen.js`）

**不比对字符串**（比对字符串证明不了语义），而是**反向解释执行**生成的每条 `Painter.*` 调用、
重算出一张网格，与原网格逐格比对：

```
生成代码 67 行；Painter.fill 6 个，Painter.set 11 个
反向解释：识别 17 条语句
  ✓ 生成的每条 Painter 语句都能被解释
  ✓ 恰好 1 次整间铺墙 / 恰好 1 次内圈掏空
逐格比对 196 格：一致 196，不一致 0
  ✓ 语句数 17 < 40（矩形合并生效；不合并会逼近 196）
  ✓ 5×4 草地区块被合并为单条 fill
  ✓ 1 格宽水渠以列填充形式生成
  ✓ 外圈隐藏门 / 上锁门被生成
  ✓ 结尾保留门类型设置循环
  ✓ 反例被检出（1 处不一致）⇒ 判据有效
  ✓ 花括号配平 / 圆括号配平 / package / 类声明 / paint 签名 / 分号
✅ 代码生成器端到端核验全部通过
```

> **踩到的假红**：圆括号配平一开始报错，查明是**注释里的中文全角「）」**被朴素正则当成右括号。
> 修法是先剥注释再计数（21/21 平）。

### 5.3 房间检查器判据单测（`_chk/verify_terrain_checker.js`）

检查器的判据在浏览器里很难用眼睛发现错 —— **假报和漏报都像"正常"**。
因此每条判据配正例 + **反向构造**（把条件破坏掉，断言必须报）。14 条断言，覆盖：

- **外圈判定**：门在**四条边**上都算合法外圈内容（四向开门不报「破损」）；
  把底边/顶边的门换成草地 ⇒ 必须报「外圈破损」。
- **门位置**：内圈中央放门 ⇒ 必须报「门不在外圈」；门全在外圈 ⇒ 不报。
- **连通性 / 深渊**：一堵墙**横切整个内部宽度** ⇒ 报「不连通」；内圈全 `CHASM` ⇒ 报「无处落脚」。
- **图集覆盖**：`WATER` 帧为空 ⇒ **不报**（水不由 tilemap 画）；`GRASS` 帧为空 ⇒ **必须报**。
- **正常房间**：纯空房 + 四向开门 ⇒ 零提示。

### 5.4 真 JDK 编译（关键一环）

按项目约定 AI 不代跑 Gradle，但**单文件 javac 是允许的源码级核验**：

```bash
javac -proc:none -Xlint:all -encoding UTF-8 \
  -cp "core/build/classes/java/main;SPD-classes/build/classes/java/main;$GDX;$GDXCTRL" \
  -sourcepath "core/src/main/java" -d _chk/_javachk \
  _chk/_javachk/EgoTerrainEditorSampleRoom.java
# EXIT=0；本文件告警/错误 0 行
```

- 首次 javac 报 `类 RegionDecoPatchRoom 是公共的, 应在名为 RegionDecoPatchRoom.java 的文件中声明`
  —— 是**测试脚本的类名与文件名不匹配**（`Ed.E.roomClass` 残留了上一个测试的值），不是生成器问题；
  修法：样例脚本里把类名统一成 `EgoTerrainEditorSampleRoom`。
- 顺带发现生成器多写了一行 `import com.watabou.utils.Point;`（生成代码里从未使用）——已删除。
  **未使用的 import javac 不报错，只能靠「逐个 import 数出现次数」查出来。**

### 5.5 用真浏览器验证 UI（Edge 无头）

`agent-browser install` 下载 Chromium 会超时（Google storage 不可达），
改用系统自带 Edge 的无头模式截图即可验证 UI 真能跑起来：

```bash
"/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe" \
  --headless --disable-gpu --no-sandbox --window-size=1400,900 \
  --virtual-time-budget=6000 --screenshot=_chk/_te_final.png \
  "file:///D:/PD/tools/terrain-editor/index.html"
```

**这一步抓到了 3 个前面纯逻辑核验抓不到的真问题**：

| # | 现象 | 真因 | 修法 |
| --- | --- | --- | --- |
| 1 | 顶栏标签重复显示 | `TE_SHEETS` 的 label 本就是「27 层测试层 tiles_lob」，`applySheet()` 又拼了 `'图集 '` 前缀 | 去掉前缀 |
| 2 | 起步示范的草渲染成**红色电路板**似的花纹 | **渲染完全正确**：`GRASS` 在 `terrain_features.png` 层是一撮**稀疏草叶**（stage4 帧 77 仅 38 个实心像素、散点状），连成片时规律重复 ⇒ 像电路板；`HIGH_GRASS`（帧 73，115 像素）才是密草丛 | 示范改成「一排犁过的草 + 一簇高草 + 一格单草 + 一格余烬 + 2×2 水 + 3×3 深渊 + 一格基座」；默认图集 `lob`→`sewers`、草叶区 4→0 |
| 3 | 检查器误报「外圈有 2 格既不是墙也不是门」；又误报「水没画」 | 前者：外圈判定只给**左右两列**的门放行、**上下两行**忘放行；后者：`WATER` 不走 tilemap（`needsRender` 跳过纯 WATER 帧，水体是 `SkinnedBlock` 动画层），地基帧为空属设计如此 | 四条边统一 `ringOk = SOLID \|\| DOOR_TILES`；覆盖检查跳过 `WATER` 与门/出入口类，只查**地面类**地形 |

> **第 2 条最值得记**：看到"花屏"先别改渲染 —— 先用 `tileframe_dump.py` dump 那一帧，
> 确认它到底画了什么。这次 dump 出来是散点状的小草叶（合法的原版美术），
> 问题在**示范内容选得不好**，不在渲染引擎。

### 5.6 陷阱 / 植物表 vs Java 源（`_chk/verify_trap_tables.py`）

`TRAPS` / `PLANTS` 两张表是**手抄** `levels/traps/*.java` 与 `plants/*.java` 的。抄错一个颜色/形状，
画布上的陷阱就变成**另一种同样合法的陷阱贴图** —— 肉眼绝对分不出。所以：

1. **解析 Java 源**，含**继承上溯**（最多 5 层）——
   `GnollRockfallTrap extends RockfallTrap`、`TenguDartTrap extends PoisonDartTrap`
   的颜色/形状是继承来的，不解析继承就会误报缺失；
2. 与 JS 表逐项比：类名集合、颜色、形状、植物 `image`、帧号公式；
3. 拿**真图集**验：每个帧号在 `terrain_features.png` 上非空，
   且**同一形状的所有颜色帧像素数一致**
   （DOTS=108 / WAVES=136 / GRILL=128 / STARS=66 / DIAMOND=92 / CROSSHAIR=132 / LARGE_DOT=77）。
   这条是强结构判据 —— 某个颜色抄错时，它的像素数会跳到别的形状去。

```
✅ 全部通过：陷阱 33 项 / 植物 13 项与 Java 源逐项一致，帧号在真图集上非空
```

`_chk/verify_trap_render.js`（17 项）在假 DOM 里跑真的 `featuresVisual()`：裸 `Terrain.TRAP`
**不画任何东西**、`makeTrap` 帧号（`FrostTrap`=54 / `DisintegrationTrap`=85 / `ToxicTrap`=35）、
`visible:false` ⇒ `-1`、`active:false` ⇒ 用 `BLACK`、**三级优先级**（陷阱 > 植物 > 草）。

`_chk/selftest_trap_render.py` 就地改 `render.js`（改完还原）造 4 类破坏 —— 陷阱分支永不命中 /
`shape*16` 写成 `shape` / `visible` 反向 / `active` 反向 —— 断言核验**各自命中预期的那条断言**。

### 5.7 水体层（`_chk/verify_water_layer.js`，13 项）

`verify_water_layer.js` 自带一个纯 JS **PNG 解码器**（RGBA8 + 调色板，从 `assets.js` 的 dataURL 解），
所以能真正验像素而不只是验调用：

- 资产自检：水帧全部 **32×32**；`water0` 非透明像素平均色**偏绿**（实测 `rgb(62,106,85)`）；
- 水层以 **32×32** 平铺（不是 16）；
- 水的地形帧与深渊帧**不同**；
- **水层关掉时纯水格什么都不画**（复现旧 bug），打开时全图被覆盖；
- 水边出现**缝合帧**，且地图内部 `r === 0`。

### 5.8 覆盖层端到端：Node 桩 + 真浏览器

```bash
node _chk/verify_editor_layers_ui.js    # 80 项：DOM 桩（自带 PNG 解码 + 假 canvas 像素缓冲）
node _chk/verify_editor_browser.js      # 51 项：系统 Edge 无头（真 DOM / 真 MouseEvent）
node _chk/verify_atlas_import.js        # 59 项：外部图集导入（纯 Node 桩，--selftest 61 项）
python _chk/verify_terrain_browser.py   # 20 项：另一条独立的真浏览器链路
```

| | DOM 桩 | 真浏览器 |
| --- | --- | --- |
| 图集解码 | 自写 PNG 解码器 + 带像素缓冲的假 canvas | 浏览器原生 |
| 事件 | 自派发 + `f.call(el)` 保证 `this` 正确 | 真 `MouseEvent`，走 `cellFromEvent` 的坐标换算 |
| 抓什么 | 图层路由、撤销原子性、JSON 往返（含 **v2 老档兼容**）、codegen 字符串 | UI 是否真绑上、下拉项数、点击是否真落笔、代码面板是否真更新 |

**两次都是真浏览器先抓到问题**（桩完全没发现）：

| # | 现象 | 真因 |
| --- | --- | --- |
| 1 | 满屏「UI 没绑上」 | 注入过早 —— `index.html` 用 5 个 `<script src>` 按序加载，`--dump-dom` 在脚本跑完前就快照 |
| 2 | 代码面板「长度为 0」 | `#codeBox` 是 **`<textarea>`**，内容在 `.value` 里，读 `.textContent` 永远是空 |

**C.4 之后断言面继续扩大**（`verify_editor_browser.js` 51 项）：除陷阱/植物外，
新增道具点击（记录 `Food` / 堆类型 `HEAP` / ctor `new Food()`）、
**「落笔前后地形值不变」**（`before->after` 形式，因为探针值不能含 `=`）、
以及「同格重复落笔被去重跳过」；**C.3 再加 13 项**（H 段：`TE_APP` 探针存在、三个导入按钮 +
清空按钮存在、`#extHint` 初始文案、6 张内置图集全 `builtin`、外部 0 条、下拉 6 项对齐注册表、
`IndexedDB` 可用）。

> **H 段为什么只断言「入口存在 + 初始态」**：真浏览器**造不出 `File` 对象**（文件选择器没法自动化），
> 而探针是紧跟 `app.js` 同步执行的、此时异步的 IndexedDB 恢复必然还没跑完。
> ⇒ 导入/替换/持久化/恢复的**全部逻辑**交给 `verify_atlas_import.js`（它能自己造 File + Blob URL）。

> **真浏览器最终定格的做法（不再用 iframe + 轮询）**：`--dump-dom` **会忽略定时器与 `load` 事件**，
> `setTimeout` / `setInterval` / `window.onload` 里做的事它**永远观察不到**（`--virtual-time-budget`
> 也救不了）。唯一可靠的时机是**紧跟 `app.js` 之后同步执行** ⇒ 把 `_chk/te_probe.js`
> **注入进同一目录的 `index.html` 副本**（`replace('</body>', '<script>'+inj+'</script></body>')`）。
>
> 三个连带坑：① `<head>` 其实带属性（`<head data-page-node-id="…">`），
> `replace('<head>', ...)` **静默不生效**，必须 `re.subn(r'<head[^>]*>', ...)`；
> ② 探针脚本里**不能出现 `</` + `script>`** 字面量（注释里也不行）——
> 它被拼进 `<script>`，会**提前闭合标签**，整段不执行；
> ③ 探针输出的 `k=v` 值里**不能含 `=`**，否则外层切分崩掉。
>
> 另外 `file://` 是 **opaque origin**：跨源被屏蔽时错误只剩一句 `Script error.`（无行号），
> 排障要加 `--allow-file-access-from-files --disable-web-security` 才看得到真实消息。
>
> **⚠️ `spawnSync` 必须给 `timeout`**（2026-09-19 实际卡过 4 分钟+）：Edge 若被上一次的
> **残留实例**顶住，无头进程**永久挂起**，而 `spawnSync` 默认无限等待 ⇒ 整个套件卡死。
> 现固定 `timeout: 120000, killSignal: 'SIGKILL'`，并在 `r.error`/`r.signal` 时提示
> 「先 `taskkill /IM msedge.exe /F` 再重跑」。
>
> **⚠️ 「可用性初始化」绝不能挂在异步存储回调里**（C.3 踩的最深的坑）：
> `file://` 下 Edge 的 `indexedDB.open()` 可能**既不 `onsuccess` 也不 `onerror`** ⇒ 回调永挂。
> 原本 `boot()` 把 `applySheet()/fullRefresh()` 写在 `restoreExt` 的回调里，于是
> 画布 **0×0**、`Ed.E.w = 14`、**控制台一声不响**（定位靠临时 `_diag.html` 打印
> `cvSize` 与 `boot()` 返回值，用完即删）。修法：**boot 同步做完必需初始化**，
> `restoreExt` 只做**追加式**补充；`openDB` 加超时 + `onblocked`；`restoreExt` 加
> **300 ms 强制放行 + `done` 闩**保证回调**至多一次**。

### 5.9 外部图集导入（`_chk/verify_atlas_import.js`，59 项 / `--selftest` 61 项）

C.3：把「用户自己画的一套贴图」导进来预览，刷新不丢。

```bash
node _chk/verify_atlas_import.js              # 59 项
node _chk/verify_atlas_import.js --selftest   # 61 项（+2 条反例）
```

**为什么必须用桩而不是真浏览器**：浏览器里**没法自动化「从文件选择器挑一个 png」**。
所以这个桩做得相当重，桩件清单本身就是一段经验：

| 桩件 | 为什么需要 |
| --- | --- |
| **真 PNG 编/解码**（`crc32` / `chunk` / `encodePng` / `decodePng`） | 要断言「渲染**真的换成了新图的像素**」，只断言 URL 变了等于没验 |
| **真 DOM 桩** | `app.js` 会 `getElementById`，缺一个就抛 |
| **IndexedDB 桩**（底层一张 `Map`，实现 put/getAll/delete/clear） | 验「刷新页面后自动恢复」这条唯一路径 |
| **`Blob` + `URL.createObjectURL` + 同步解码的 `Image` 桩** | `app.js` 把 blob URL 交给 `Image`；桩必须同步 `onload`，否则测试要到处 `await` |

覆盖面 A→K：接线 / 导入楼盘图集 / **像素级确认真的换图** / 非 16 倍数被拒 /
**同类替换（只留一张）** / 草叶图与道具图覆盖内置 / **IndexedDB 真的落了 Blob** /
生成代码里是**人类可读名**（不含 `ext:sheet:`）/ 清空后全部回退 / **模拟刷新后恢复** /
反例自测（去掉 `% 16` 校验必须被抓到）。

> **三个产品 bug 就是这样被抓出来的**（详见 §6之二 1/5/6）：① `importAtlas` **从不删除**
> 同类的旧 IndexedDB 记录 ⇒ 刷新后旧图集先恢复、把新图集顶掉（且 DB 无限增长）；
> ② `render()` 缺空值保护（首帧崩）；③ 启动被 IndexedDB 卡死。
>
> **桩自己也有两个坑**（都固化进脚本注释）：① `createObjectURL` 的**去重键不能用
> 「长度 + 前 32 字节」** —— 两张同尺寸图集会撞车，表现为「§E 拿到的是第一张图」，
> 要用**完整 md5**；② `revokeObjectURL` 之后**必须让去重缓存失效** —— 否则重新导入
> 同一张图会拿到**已吊销的死 URL**，`Image.onerror` ⇒ 静默导入失败（§J 就这么红过）。

### 5.10 区域随机（`_chk/verify_random_regions.js`，108 项 / `--selftest` 按设计失败 2 项）

```bash
node _chk/verify_random_regions.js              # 108 项
node _chk/verify_random_regions.js --selftest   # 按设计失败 2 项（顺序断言）
```

这套验的不是「有没有生成 `shuffle`」，而是三个**肉眼绝对看不出来**的性质：

| 性质 | 不验会怎样 | 验法 |
| --- | --- | --- |
| **多重集不变** | 逐格 `Random.oneOf` 会毁掉作者定的配比（5 草 2 水 1 墙可能变一片汪洋），但代码「看起来也在打乱」 | 从生成的 `int[] r0 = new int[]{...}` 里**解析字面量**，排序后与原区域逐值对比 |
| **顺序不变量** | 排在填充之前会被**整片覆盖**，而且**完全不报错**、字符串层面毫无异常 | 断言区域段的行号在**每一条** `Painter.fill/set` 之后、门类型循环之前 |
| **覆盖层联动** | 只打乱地形 → 陷阱/植物/道具留在原地错位，编译照样能过 | 两条 `withLayers` 路径分别验：置换表初始化为恒等、`r0Old = r0.clone()`、写回公式、注释差异 |

覆盖面 A→K：接线 / **区域拓扑**（合并去重、整片删除、排序、越界夹取）/ 多重集 +
行优先顺序 / **顺序不变量** / `withLayers` 开关 / 无区域时**不许凭空生成** /
退化区域（1 格被跳过、整片同值仍生成）/ **JSON v4 往返 + v3 老档兼容** /
撤销重做含区域 / UI 路由 + 提示行文案与警示色 / 渲染调用断言（记账 ctx 数 `setLineDash`、`clip`）。

> **反例自测的注入方式必须真的破坏不变量**：`--selftest` 会把区域段**搬到骨架填充之前**
> （用 `indexOf` 定位发射点 + 锚点重排），而不是「删掉那行段落注释」——
> 后者对行号断言毫无影响，`--selftest` 会**假绿**。注入失败时脚本直接 `exit(2)` 报错。
> 详见 guide §6.5 陷阱 ⑩⑪⑫（另含「测试别硬写画布宽度」「退化样本要互不相邻」两条假红）。

> **第二层兜底在 `verify_codegen_compiles.js`**：两种 `withLayers` 变体一起交给真 `javac`，
> 并反汇编断言 `Random.shuffle:([I)V` —— `Random` 有 `int[]` / `T[]` / `(U[],V[])`
> **三个重载**，用错重载**编译能过但语义全错**，只有字节码层能验。

### 5.11 生成的 Java 真 `javac` 编译（`_chk/verify_codegen_compiles.js`，42 项）

字符串断言只能证明生成物「长得像」Java，而**最容易出的事故恰恰是签名/包名**：

```bash
node _chk/verify_codegen_compiles.js              # 正例：必须零错误（42 项）
node _chk/verify_codegen_compiles.js --selftest   # 反例：把 Seed 去掉，必须编译失败
```

生成物写成 `levels.rooms.standard` 包（与 `StandardRoom` 同包，否则 `extends StandardRoom` 解析不到），
classpath 用**本仓已编译产物**（`core/build/classes/java/main` + `SPD-classes/build/classes/java/main`）。
编译后再 `javap -c` 反汇编，断言字节码里真出现：

```
invokevirtual  Level.setTrap:(...levels/traps/Trap;I)...Trap;
invokevirtual  Level.plant:(...plants/Plant$Seed;I)...Plant;
new            ...plants/Firebloom$Seed        ← 内部类，不是 Firebloom
invokevirtual  Level.pointToCell:(...watabou/utils/Point;)I
invokevirtual  Level.drop:(...items/Item;I)...items/Heap;    ← 道具走这条，不是 addItemToSpawn
putfield       ...items/Heap.type:...items/Heap$Type;
getstatic      ...items/Heap$Type.CHEST
invokevirtual  ...items/DarkGold.quantity:(I)...items/DarkGold;
invokestatic   ...utils/Random.NormalIntRange:(II)I
invokestatic   ...utils/Random.shuffle:([I)V   ← 区域随机：必须是 int[] 重载
getstatic      ...Dungeon.depth:I              ← IronKey( Dungeon.depth )
```

> **道具为什么必须靠反汇编验**：`Level.drop` 的**返回值**才是关键 ——
> 堆类型靠 `level.drop(...).type = Heap.Type.CHEST` 链式设置，而字符串层面
> `addItemToSpawn` 与 `drop` 都只是「一个方法调用」。字节码能同时确认
> **签名是 `(Item,int)`、返回值是 `Heap`、随后真的 `putfield Heap.type`**。
> 另有一条**非注释行**断言：生成的代码里**不得**出现 `addItemToSpawn`
> （它只允许出现在警告注释里）。

反例自测实测到的编译错误正是预期的那条：
`不兼容的类型: Sungrass无法转换为Seed`。

> **Windows 上 javac 的中文诊断是 GBK 编码**，Node 默认按 UTF-8 解会得到 `�����ݵ�����`，
> 基于中文关键词的断言必然失手。解法：`spawnSync` 不设 `encoding`、拿 Buffer 后用
> `TextDecoder('gbk')` 解码（发现替换字符 `\uFFFD` 时再切 GBK）。
> 另注意 javac 的「错误:」与正文**不在同一行**（正文在下一行），断言要在**全文**里找。

### 5.12 道具表 vs Java 源（`_chk/verify_item_tables.py`，8 项 + 反例）

`render.js` 的 `ITEMS` 表是 **71 个类名 + 7 个分组 + 中文名**，**手抄 `items/` 全包**。
抄错一个类名，画布上照样画出「一个口粮」看不出差别，但**生成的代码编译不过**（类不存在）。
所以逐个对文件核验：

1. `items/<pkg>/<cls>.java` **真实存在**；
2. 是 `public class` 且**不是 abstract**（abstract 的 `Item` 造不出来）；
3. 无重复项；
4. 每一项都能产出 `ctor`（生成器必须写出 `new <cls>()`）。

`--selftest` 把假类名 `TotallyFakeItem` 注入表里，断言脚本必须报错（实测抓到 2 处）。

### 5.13 口粮占位图帧号的三层独立证明（`_chk/verify_item_placeholder.py`，16 项）

「占位图」这一层唯一的正确性依据是**帧号 437**，错了会**静默画成另一件物品**
（同样是合法贴图，肉眼看不出）。三层独立证明：

1. **公式层**：Java 的 `xy()` 公式算出的 437 与 `render.js` 里硬编码的 `RATION_FRAME` 一致；
2. **像素层**：从 `assets.js` 解出**真 `items.png`**（256×800），自写 PNG 解码器验帧 437
   **有 164 个不透明像素**，并取相邻空帧作**对照**；
3. **调用层**：在带像素缓冲的假 canvas 里跑 `render()`，断言它真的发出了
   `drawImage(src 80,432 → dst 32,32 / 64,64)`。

> 第二层的对照是必要的：**`Sheet.prototype.draw()` 对任何合法索引都返回 `true`**，
> 所以只看「调用了 draw」证明不了「这一帧有内容」。

### 5.15 尺寸模糊化的安全区算术与夹取（`_chk/verify_size_fuzz.js`，16 项反例自测）

C.6 的正确性支点是**同一套「安全区」算术被写了两遍**：`editor.js` 的 `safeRect()`（画参考线）
与 `codegen.js` 的 `fuzzSafeRight()`/`fuzzSafeBottom()`（夹取坐标）。两处只要漂移 1 格，
画面上「绿框里」的内容生成后就会**越界被裁**，而肉眼完全看不出来。六节断言：

- **A 算术交叉验证**：`ON` 与 `OFF` **两种状态都断言**。这节当场抓出真缺陷：关闭模糊化时
  `safeRect()` 返回了 `r=6` 而 `fuzzSafeRight()` 返回 13（旧注释还写着「两处同算法」）。契约是
  **关闭时必须双双退化成整块画布 `w-1`/`h-1`**，否则 UI 会在一块「尺寸固定」的房间上
  画出一圈误导性的红色危险区。
- **B 夹取**：生成物所有坐标都落在安全区内；`guarded=0/1/3` 三档必须**互不相同**（否则谓词是恒真的）。
- **C 类别选择**：`fuzzCategoryFor()` 把四个数坍缩成一个区间（`SizeCategory` 宽高共用一个
  `[min,max]`）。`on:false` 时 `fuzzImpossible()` **短路返 false** ⇒ 断言辅助函数必须显式带 `on:true`。
  覆盖 `5~13`/`9~13` 无解、`10~14`/`4~10` 有解。
- **D 生成代码接线**：`DECL_RE` 匹配 `@Override … sizeCatProbs(){` **声明块**（裸 `/sizeCatProbs/`
  是假绿陷阱——这个词在文件头注释与 `setSize` 注释里也出现），`CALL_RE` 匹配 `\n\t\tsetSize( 8, 10, 8, 10 );`。
  再跑两个反例：**只写 `setSize` 不覆写 `sizeCatProbs`** / **只覆写不调 `setSize`**，都必须被断出来。
- **E 检查器代码**：`warning`/`small`/`fixed` 三个短码各自单独 + 两两同时。
- **F JSON v5 往返**：缺键取默认、缺 `format` 抛错、超范围被夹、min/max 写反被交换。

> `fuzzIssues()` 返回的是**短码**不是中文句子（渲染层负责组文案），所以断言必须打在码上。

### 5.16 尺寸模糊化的面板端到端（`_chk/verify_size_fuzz_ui.js`，63 项 / `--selftest` 按设计失败 6 项）

复用 `verify_editor_layers_ui.js` 的 DOM 桩（~250 行），补上 C.6 的 6 个控件 id：

- **B 开机同步**；**C 面板→内核→代码+提示**（关闭时生成物**不得**出现 `sizeCatProbs`/`setSize`）；
- **D 三个预设按钮**（小 7~9 / 中 8~10 / 大 10~14）；**E 越界输入被夹且回写进输入框**；
- **F `#chkShowSafe`** → `R.showSafe()`，且**渲染侧那份 fuzz 镜像与编辑器一致**；
- **G JSON 往返**；**H 先载文件再动别的控件不冲掉 fuzz**；**I 生成代码仍有成对两处 + 9~13 报无解**。

这一节当场抓出一个真缺陷：`syncFuzzFromModel` 被定义在 `bindUI()` 内部却从 `boot()` 调用 ⇒
浏览器里会 `ReferenceError` 卡在首帧之前。修法是把 `applyFuzzFromUI` 与 `syncFuzzFromModel`
**提到模块作用域**（旁边留了「⚠️ 别把它们挪回 `bindUI()` 内部」的注释）。

> 桩有一个易漏点：真实 DOM 的 `textContent` 是**剥掉标签的 `innerHTML`**。桩必须用
> `defineProperty` 让 `textContent` 从 `innerHTML` 派生 —— 否则「用 `innerHTML` 写、用 `textContent` 读」
> 的探针会读到空串（本轮因此误报过 6 项失败）。

### 5.14 全仓体检

`python _chk/check_utf8_all.py` → 扫描 1432 个文件，全部合法 UTF-8、无「汉字+?」痕迹。

**18 套核验当前全绿**：帧号 6840/6840（+反例 22 处）、codegen 196 格全等、
检查器 14/14、陷阱 33/33+植物 13/13、陷阱渲染 17/17、陷阱渲染反例 4/4、水体 13/13、
覆盖层桩 80/80、外部图集导入 59/59（`--selftest` 61）、区域随机 108/108（`--selftest` 按设计失败 2）、
真 javac 49/49、道具表 8/8（+反例）、口粮占位 16/16、
**尺寸模糊化 全部通过（16 项反例自测）**、**尺寸模糊化面板 63/63（`--selftest` 按设计失败 6）**、
**真浏览器探针 51/51**、UTF-8 全通过（1432 文件）。

### 5.17 C.7 解耦核验（改名 / 图集移出内置 / 包名可配置）

三项范围（用户确认「只改名、不清功能」+「移出内置走外部导入」+「包名可配置 + 清单通用化」）各配了断言：

- **改名**：`verify_terrain_codegen.js` 断言生成的 Java、注册清单、内置图集清单**三处都不含 `EGOPD`**。
- **移出内置**：断言内置清单**恰好 5 张**且**不含 `tiles_lob`**。判据有效性已用「临时塞回 `lob` ⇒ 恰好 2 条失败」证明。
- **包名可配置**：断言「空 ⇒ 回退通用 SPD 路径」「纯空格 ⇒ trim 后仍回退」「自定义 ⇒ 原样采纳」。
- **清单通用化**：断言清单里**没有写死的 `35`**、**没有 `float[27]`**、用 `<深度数>` 占位、
  且给的是**判据**（长度须等于 `rooms.size()`）而不是具体值。
- **存档格式兼容（最易踩的坑）**：`format` 键从 `egopd-terrain-editor` 改为 `spd-terrain-editor`
  ⇒ 读侧必须走**白名单**同时收旧名，否则用户存量 `.json` 全部打不开。
  `verify_size_fuzz.js` F 节加了两条：**旧名能读入** + **无关名必须被拒**（反例）。
  另有三个套件的 fixture 刻意保留旧名，充当**向后兼容的活体回归**。

**数量耦合断言的过时**：删 `lob` 让内置图集 6→5，**4 处**写死 6 的断言（`verify_atlas_import.js` 6 处、
`verify_editor_browser.js` 2 处）同时变红。属**合法过时**，但须逐条确认「真的只是数量变了」。

## 六、修掉的脚本 bug

- **Node harness 用 `__dirname` 找 `render.js`**：harness 写在 `_chk/`、而 `render.js` 在
  `tools/terrain-editor/`，于是 `ENOENT: D:/PD/_chk/render.js`。
  改为**显式吃绝对路径**（`node _te_harness.js <render.js> <cases.json>`），
  反例自测也从「复制 harness 到临时目录」改成「复制坏的 render.js 到临时目录 + 传它的路径」。
- **`setSheets.__fake = true`** 是早期草稿的残留（曾试图绕过 Sheet 构造），实际 harness 直接
  往 `R.state` 塞数据、不建 Sheet，该行已删。
- **DOM 桩 `dispatch` 必须 `f.call(el, ev)`**：`app.js` 的事件回调普遍写 `this.checked` / `this.value`，
  直接 `f(ev)` 会让 `this` 变成 `undefined`，报 `Cannot read properties of undefined (reading 'checked')`。
- **`Image` 桩的 `onload` 必须同步触发**：真实浏览器里 dataURL 的 `onload` 是异步的，
  但 `app.js` 的 `boot()` 是**同步**跑完的 —— 桩里用 `setTimeout` 就永远等不到图集 ⇒
  第一次 `draw()` 时 `S.sheet` 还是 `null`，报 `Cannot read properties of null (reading 'draw')`。
- **`verify_trap_tables.py` 的类型错误**：`parse_trap_constants()` 返回的是**正则捕获的字符串**，
  直接比较会报 `'<=' not supported between 'int' and 'str'`；每处都要 `int(...)`。
- **`verify_water_layer.js` 的 `resize()` 陷阱**：`resize()` 会**保留旧格**，
  `resize(2,2)` 再 `resize(3,3)` 得到的不是全水 3×3。要先清 `state.w/h`。
- **`grep '\|'` 与中文标点**：核验脚本里统计 `Painter.fill` 数量时，
  中文全角「）」会被朴素正则当成右括号 —— 先剥注释再计数。
- **`R.ITEMS` 在探针里是 `undefined`**：`render.js` 用的是 `global.TE_RENDER = API`，
  **不是 `module.exports`** ⇒ `require()` 回来的是空对象。必须像其它套件一样走 `vm.runInContext` 沙箱。
- **`v2` 老档样本写错字段名**：真实 schema 需要 `format:'spd-terrain-editor'`
  （**C.7 之前是 `'egopd-terrain-editor'`**，读侧走白名单两者都收，见下），
  且网格字段叫 **`tiles`** 而不是 `map`。写错会报「不是地形编辑器文件」/ `undefined is not iterable`。
- **`verify_editor_browser.js` 的 `te_probe.js` 被误删**：探针是**必需的伴随文件**，
  删掉后跑核验直接 `ENOENT`。重建后要保证它覆盖面与断言一一对应。
- **注入 `<head>` 用 `replace('<head>', ...)` 静默失效**：真实标签是
  `<head data-page-node-id="…">` ⇒ 必须用 `re.subn(r'<head[^>]*>', ...)`。
- **探针注释里写了 `</` + `script>`**：探针被拼进 `<script>` 标签，这个字面量会**提前闭合标签**，
  整段脚本不执行（`<title>` 不变，看起来像"没注入"）—— 排查花了很久。

## 六之二、修掉的产品 bug（不是脚本错，是真会砸到用户的）

### 1. `render()` 缺空值保护 ⇒ 首帧必崩（**本可能砸到每一个用户**）

`app.js` 的 `boot()` 是**同步**跑到底的：`fullRefresh() → draw() → R.render()`。
而图集是 `new Image(); im.onload = ...` **异步**加载 ⇒ **首帧必定早于 `setSheets()`**。

原来 `render()` 无保护，`S.sheet.draw()` 直接抛
`TypeError: Cannot read properties of null (reading 'draw')`，**整个 `boot()` 就此中断** ——
表现是所有下拉框空白、画布停在 16×16、代码框全空（用户会以为"工具坏了"）。

**以前没炸纯属运气**：图集加载得够快。C.4 把 `items.png`（`assets.js` 从 216KB 涨到 332KB）
内联进来后**时序一变就必炸** —— 也可以说，这个 bug 一直存在，只是被加载速度掩盖了。

修法：`render()` 里 `ctx.scale` 之后直接

```js
if (!S.sheet) return;      // 图集未就绪：直接不画，等 onload 后的 refresh
```

并把 features pass 的判据从 `if (showFeatures)` 收紧成 `if (showFeatures && S.features)`。
⇒ 行为**与加载速度无关**。

> **教训**：任何"资源加载完之后画面才对"的实现都是脆的。首帧必须能在资源缺失时安全返回。
> 核验侧相应改成显式 `R.setSheets(fakeSheetImg, fakeSheetImg)`。

### 2. `addItemToSpawn` 不是定点投放（差点写进生成器）

道具层的核心是「放在**指定格**」。查证后确认：

- `Level.drop( Item, int cell )` —— **唯一定点投放入口**，返回 `Heap`
  （字节码 `Level.drop:(L…items/Item;I)L…items/Heap;`）⇒ 可链 `level.drop(...).type = Heap.Type.CHEST`；
- `addItemToSpawn( Item )` —— 只把物品推进 `itemsToSpawn` 队列，随后由
  `RegularLevel.createItems()` 通过 **`randomDropCell()`** 消费 ⇒ **落点是随机的**。
  而它**编译完全能过**，属于纯静默错误。

`Room.paint()` 里 `level.drop(...)` 上游有先例：`RingRoom.placeCenterDetail`、`MassGraveRoom`、
`RatChestChasmRoom`、`RatKingRoom`。⇒ 生成器固定用 `drop`，并在代码注释里写明「不要用 `addItemToSpawn`」。

### 3. 口粮占位帧号：第一版探针算错了（不是推导错）

推导：`SIZE=16`、`TX_WIDTH=256` ⇒ `WIDTH=16`；`xy(x,y){x-=1;y-=1;return x+WIDTH*y;}`
⇒ `FOOD=xy(1,28)=432`、`RATION=FOOD+5=**437**`（col 5 / row 27 / 像素 `(80,432)`）。

第一版探针按 `(col-1)*16` 算 ⇒ 报告「437 是空帧」。**是探针错了**：
`assignItemRect` 用的是 **0-based** 的 `(item % WIDTH) * SIZE`。重探后帧 437 有 **164 个不透明像素**。

另一个反直觉点：**`Sheet.prototype.draw()` 对任何合法索引都返回 `true`**（哪怕这一帧全透明），
所以 `if (!drew)` **不是**有效的空帧判据 —— 必须用 `solidCount(idx) > 0`。
编辑器就靠这条决定「能画口粮就画口粮，否则退化成橙黄十字」。

### 4. `Ration` 这个类不存在，真名是 `Food`

测试与 `boot()` 示范最初都写了 `Ration`。查证：`items/food/Food.java`，
`public class Food extends Item`，`image = ItemSpriteSheet.RATION`。
**类名与贴图常量是两回事** —— 表里因此是 71 项（`Food` 在列，`Ration` 不在）。

### 5. `importAtlas` 从不删除同类旧记录 ⇒ 刷新后新图集被顶掉（C.3）

导入第二张楼层图集时，`importAtlas` 把旧图从**内存注册表**里卸下（`unmountExt`），
但**忘了删 IndexedDB 里的那条记录**。当时看不出问题（画面确实换了新图），
但**一刷新**就现原形：`restoreExt` 把库里**两条**都恢复出来，
而**旧的排前面 ⇒ 旧的赢** —— 用户看到的就是「导入新图集后刷新，又变回旧图集了」。
顺带还有 DB 无限增长。

修法：在同一个循环里 `dbDel(r.key)`。核验 §E 就是钉这条（「留下的是后导入的那张」+
「确认不是第一张图」两条互补断言）。

### 6. 启动被 IndexedDB 卡死 ⇒ 画布 0×0、控制台无声（C.3，**最隐蔽的一个**）

`boot()` 把收尾工作（`applySheet()` / `fullRefresh()`）写在 `restoreExt` 的**回调里**。
逻辑上没问题 —— 但在 `file://` 这种 opaque origin 下，Edge 的 `indexedDB.open()`
**可能既不触发 `onsuccess` 也不触发 `onerror`**，回调**永远不执行**：
画布 `cvSize=16,16`、`Ed.E.w=14`（应为 12）、`boot()` 返回值是对象（看起来"跑完了"）。

**定位过程**（值得记）：临时造一个 `_diag.html`，在 `app.js` 之后**同步**打印
`cvSize` 与 `boot()` 的返回值 ⇒ 一眼看出「同步段跑完了、异步段没跑」。
**这个探针用完即删，不进仓库。**

修法三件套：

1. `openDB` 加 **250 ms 超时 + `onblocked`**；
2. `restoreExt` 加 **300 ms 强制放行 + `done` 闩** ⇒ 回调**至多执行一次**；
3. **`boot()` 的必需初始化改回同步**，`restoreExt` 只做**追加式**补充：

```js
rebuildSheetOptions(); applySheet(); fullRefresh();   // 同步，绝不依赖 IndexedDB
msg('编辑器就绪 —— …', 'var(--ok)');
restoreExt(function () {                              // 异步，恢复到了才重刷
    if (!EXT.length) return;
    rebuildSheetOptions(); applySheet(); refreshExtHint();
});
```

> **通用教训**：**「能让页面用起来」的那部分初始化，绝不能挂在异步存储的回调里。**
> 存储慢、存储坏、存储 API 静默失效 —— 任一种都不该让整个工具变砖。

### 7. 「探针」与「真实路径」各写一份 ⇒ 探针永远跟不上（C.5）

写 C.5 的核验时我把 `TE_APP.markRegion` 写成「压撤销 → 改状态 → `syncLayers()`」，
而拖框结束的真实路径是「压撤销 → 改状态 → `syncLayers()` → `refreshRegionHint()`」——
**探针少了最后一步**。于是测试里读到的是**没更新过的旧文案**（「当前无区域标记」），
看起来像提示行功能坏了，其实是探针与真实路径**漂移**了。

**修法**：把那段逻辑抽成一个具名函数 `markRegion()`，拖框分支与 `TE_APP.markRegion`
**都调它**（探针额外补一句 `draw()`）。这样「被测的东西」与「用户点的东西」是同一段代码，
探针再也不会落后于 UI。

> **通用教训**：给核验脚本用的探针**不要重写一遍主流程**。要么直接调用主流程用的函数，
> 要么让主流程调用探针（二选一，别各写一份）。**同一个动作有两份实现时，被测试的那份
> 一定不是用户点的那份。**

### 8. 假绿：反例自测「删注释」而不「改行为」（C.5）

`verify_random_regions.js --selftest` 本该把「区域段在填充之后」这条不变量破坏掉，
但最初的反例注入是**删掉那行段落注释** —— 而 D 节断言的是 `Random.shuffle(` **所在的行号**，
注释删不删跟它无关 ⇒ **`--selftest` 照样 108/108 全绿**（假绿：反例根本没生效）。

**修法**：反例必须真的**把区域段搬到填充之前**（`indexOf` 定位发射点 + 锚点重排），
并且**注入失败时直接 `exit(2)`** 而不是静默跳过。

> **通用教训**：反例自测本身也需要被怀疑 —— **「这段破坏会被我的断言看见吗？」**
> 如果注入代码没有「失败即报错」的兜底，它就是一个**只会输出绿色的假保险**。

## 七、后续可扩展方向（未做）

> 已完成：**道具生成 + 生成位置**（C.4，2026-09-19）—— 71 种物品 × 7 种堆类型，
> `level.drop` 定点投放，统一口粮占位图，三层 JSON（`version:3`）。
>
> 已完成：**用户自行导入图集**（C.3，2026-09-19）—— 楼层图集 / 草叶图 / 道具图三类，
> 尺寸校验（16 的倍数）、同类替换（只留一张）、IndexedDB 存 **Blob** 持久化、刷新自动恢复、
> 生成代码里写人类可读名。UI 在左栏「外部图集」，见指南 §2.6。
>
> 已完成：**区域随机排列**（C.5，2026-09-19）—— 框选矩形区域，生成时按**多重集**打乱
> 区域内的地形（配比不变、只换位置）；可勾选**连同覆盖层一起打乱**（陷阱/植物/道具用同一张
> 置换表联动）；区域拓扑保证**两片永不相交**（重叠迭代合并）；JSON 升到 **v4**（`randoms`）。
> UI 在左栏「区域随机」，见指南 §2.7。

- **模糊化**（房间尺寸在一定范围内变化）：需让用户选目标 `SizeCategory`、画安全区/上下界参考线，
  并标注「不支持复杂具体地形」。**约束**：`Room.setSize` 内部是
  `resize( NormalIntRange(minW,maxW)-1, NormalIntRange(minH,maxH)-1 )` —— 宽高**各自独立**、
  **正态分布**；取值范围来自 `SizeCategory`（`NORMAL(4,10,1)` / `LARGE(10,14,2)` / `GIANT(14,18,3)`），
  且 `StandardRoom.sizeCatProbs()` 默认 `{1,0,0}`（永远 NORMAL）。⇒ 在 `paint()` 里写死尺寸
  会在尺寸变化的房间上错位，复杂图案要锚定 min/max 边界或用中心相对坐标；
- **独立于 EGOPD**：去掉 EGOPD 专属包名默认值、`tiles_lob` / 27 层引用；
- **撤销/重做栈深度可配置**：当前栈深度是固定上限，在大画布（尤其整层模式）上偏低。
  落地要点：给一个「最多保留 N 步」输入框，**每步快照含 `randoms` + 三个覆盖层数组**
  ⇒ 内存随画布面积线性上升；**下调时必须立刻截断已有栈**，否则用户调小了设置却不释放内存。
  建议与「整层绘制模式」一起做（那时画布显著变大）。改差分/命令式撤销收益更大但改动面太宽，暂不做；
- **整层绘制模式**（用户明确要求）：用于特殊层的地形生成，**不设尺寸上限**。
  与房间模式的关键差异：坐标用**绝对格号**（整层没有 `left/top` 分配矩形）、
  产物是 `Level` 子类的 `build()` / `paint()`（自行 `setSize` + 铺图）而非 `StandardRoom.paint()`、
  骨架两条固定语句不再适用、检查器判据要整套替换（可通行区连通性 / 出入口可达 / 边界封闭）、
  画布需要支持超限滚动。样板参考 `levels/LobTestLevel.java`（`GEN_DEPTH` +
  `TerrainFeaturesTilemap.FeaturesTexProvider`）；
- 输出 `decorate()` 后处理片段（`RegionDecoPatchRoom` 那类「先铺地形、再装饰」的房间）；
- 直接输出 `chances[]` 的补丁行（当前只在注释里给清单）；
- 给 `Terrain` 常量生成 `switch` 骨架（如 `tileName` / `tileDesc` 的本地化分支）。
- **模糊化**（房间尺寸在一定范围内变化）：需让用户选目标 `SizeCategory`、画安全区/上下界参考线，
  并标注「不支持复杂具体地形」。**约束**：`Room.setSize` 内部是
  `resize( NormalIntRange(minW,maxW)-1, NormalIntRange(minH,maxH)-1 )` —— 宽高**各自独立**、
  **正态分布**；取值范围来自 `SizeCategory`（`NORMAL(4,10,1)` / `LARGE(10,14,2)` / `GIANT(14,18,3)`），
  且 `StandardRoom.sizeCatProbs()` 默认 `{1,0,0}`（永远 NORMAL）。⇒ 在 `paint()` 里写死尺寸
  会在尺寸变化的房间上错位，复杂图案要锚定 min/max 边界或用中心相对坐标；
- **独立于 EGOPD**：去掉 EGOPD 专属包名默认值、`tiles_lob` / 27 层引用；
- **整层绘制模式**（用户明确要求）：用于特殊层的地形生成，**不设尺寸上限**。
  与房间模式的关键差异：坐标用**绝对格号**（整层没有 `left/top` 分配矩形）、
  产物是 `Level` 子类的 `build()` / `paint()`（自行 `setSize` + 铺图）而非 `StandardRoom.paint()`、
  骨架两条固定语句不再适用、检查器判据要整套替换（可通行区连通性 / 出入口可达 / 边界封闭）、
  画布需要支持超限滚动。样板参考 `levels/LobTestLevel.java`（`GEN_DEPTH` +
  `TerrainFeaturesTilemap.FeaturesTexProvider`）；
- 输出 `decorate()` 后处理片段（`RegionDecoPatchRoom` 那类「先铺地形、再装饰」的房间）；
- 直接输出 `chances[]` 的补丁行（当前只在注释里给清单）；
- 给 `Terrain` 常量生成 `switch` 骨架（如 `tileName` / `tileDesc` 的本地化分支）。

---

# 2026-09-20 修复：蜕变卷轴变出的天赋在别职业身上静默失效

## 一、症状与根因

用户报告：中指长兄的 T3「过人的毅力」在本职业身上正常，但**别的职业用蜕变卷轴（`ScrollOfMetamorphosis`）
变出这条天赋后完全不生效**；拇指的 T2「加速弹药」同理。

根因不是逻辑写错，而是**判据用错了替身**：

- 蜕变卷轴会把**别职业**的天赋真正写进本英雄的天赋表 —— `ui/TalentButton` 的 METAMORPH_REPLACE 分支
  执行 `newTier.put(talent, tier.get(replacing))` 后 `Dungeon.hero.talents.set(tier-1, newTier)`，
  并记进 `Dungeon.hero.metamorphedTalents`。所以「战士身上有过人的毅力」是**完全合法**的状态。
- 而 `Talent` 里那一排 `if (hero.heroClass != HeroClass.MIDDLE_FINGER) return …;`（拇指侧是
  `!= HeroClass.VALENCINA`）把整段效果**短路**掉了。它的原意只是「不是这个职业就别忙了」的廉价短路，
  对蜕变出来的天赋却变成**静默失效**：点满了、界面上有、就是零效果，不报错、无日志提示，极难排查。

## 二、改动清单（5 个文件）

| 文件 | 改动 |
| --- | --- |
| `actors/hero/Talent.java` | 枚举体顶部新增【判据红线】说明块；**删掉 23 处职业判定**（见下）；原 `isMiddleFingerDamage` 整段重写为 **`isHeroDealtDamage`**（`src == Dungeon.hero`，与职业无关，javadoc 里写明为什么）。 |
| `actors/Char.java` | `Char.damage` 里两处调用改名（`Talent.isHeroDealtDamage`），注释同步。 |
| `actors/mobs/npcs/Shopkeeper.java` | `canBuyAccelerationRounds()` 去掉 `heroClass == VALENCINA`（连同死 import）。 |
| `items/AccelerationRound.java` | 液金配方 `testIngredients` 去掉 `heroClass != VALENCINA`（连同死 import）。 |
| `actors/buffs/TremblingScorch.java` | `onParalysisLifted` 的 `heroValid` 只留 `subClass == WAR_HERO`（子职业本身就是职业独有，不必再叠一层替身）。 |

`Talent.java` 去职业判定的 23 处：`onFoodEaten` 的「狩猎一餐 / 记录一餐」两段、攻击钩子
（`onHeroAttackStarted` / `onHeroAttackLanded` / `onHeroAttackResolved`，家族背叛者两条钩子改由被调方
自守 `subClass`）、`onHeroAttackBrag` / `onHeroAttackMissed` / `onEnemyAttackLanded`、`handyToySwapCost`、
`tooHotImmune`、`perseveranceFloorPercent` / `perseveranceCap`、`beastFuryMultiplier`、
`gritTeethDuration` / `gritTeethEndHeal` / `nearDeathFuryMultiplier`、`ledgerDevourMultiplier` /
`executionUnleashLevels` / `executionUnleashDuration` / `instantExecutionChargeFactor`、
`onPrecognitionActivated` / `onHeroDodgedEnemyAttack` / `valencinaSmoke`、`onAttackProc` 的「狩猎目标」段。

**没动**的（同一次审计里确认「机制本身就是该职业独有」，蜕变卷轴给不了、谈不上失效）：
`AcceleratingFuture.gain` / `Char.trackAcceleratingFuture`（拇指核心机制「加速的未来」）、
`ValencinaTracker`、`InstructionTarget.updateMark`（神谕指令系统）、`Talent.icon()`（同一天赋按职业取不同图）、
`initClassTalents`、台词音效与 `RingMasterArt`；以及**上游**那批 `heroClass != CLERIC` —— 那是刻意给
Cleric 走的**另一套效果分支**（`RECALL_INSCRIPTION` / `DIVINE_SENSE` / `CLEANSE` / `BLESS` /
`SHIELD_OF_LIGHT`），不是「有没有天赋」的替身，别一起拆。

## 三、判据红线（已写进代码枚举体顶部 + AGENTS.md §6）

> 天赋的生效判据**只能**是「这英雄身上有没有这门天赋的点数」：`hero.hasTalent(X)` 或
> `hero.pointsInTalent(X) > 0`。职业判定只在该机制**本身就是该职业独有**时才用。

## 四、核验（源码级，未代跑 Gradle）

- `javac -proc:none -Xlint:all`（5 文件）**EXIT=0**；按 `difflib` 逐行比对，本批**改动行与全部告警行零交集**
  （`Talent.java` 3 条、`Char.java` 37 条、`Shopkeeper.java` 1 条、`AccelerationRound.java` 1 条全部在改动行之外，
  都是上游既有写法）。
- 新增 `_chk/verify_metamorph_talents.py`：**73 条断言 + 6 条反例自测**，`ALL PASS`。
  它是**双向**的 —— 既断言「天赋函数体内不得出现 `heroClass`」，也断言「该保留的职业判定仍在、且出现次数正确」
  （核心机制 5 处、上游 Cleric 分支 3 处等），防止以后有人**过度修正**把职业独有机制一起拆掉；
  `--list` 可列出当前所有残留 `heroClass` 及其所属方法。
- `_chk/check_utf8_all.py`：1432 文件全合法 UTF-8。

---

# 2026-09-20 神谕代行者三处修复（解放复活保底 / 拼好饭充能 / 与你再会回能）

## 一、剑刃解放：复活后按「业」重挂保底解放

**症状**：解放 buff 在死亡时被清除是预期行为，但复活后**解放 buff 再也不出现** ——
神谕代行者 T2「剑刃解放 +2」那条「最低层数随业积累提升（业 90 时保底 9 层）」的效果整段失效，
攻击附带伤害与受击减伤一并消失，且不报错、无提示。

**根因**：解放 buff（`Release`）不是 `revivePersists`，死亡后由 `Hero.live()` 统一 detach；
而保底层数的**唯一载体**是另一个 buff「业」（`Karma`，`revivePersists = true`，复活保留），
两者必须**同时存在**才成立（`effectiveStacks()` = `max(自身层数, karmaFloor)`）。
buff 被摘掉后 `Hero` 的减伤块（`if (buff(Release.class) != null)`）与 `Talent.onAttackProc`
都直接短路，保底自然无从生效。

**改动**：

| 文件 | 改动 |
| --- | --- |
| `actors/buffs/Release.java` | 抽出静态判据 `karmaFloor(Hero)`（「未点满 2 点 / 无业 ⇒ 0」，业每 10 点 1 层、上限 `MAX_STACKS`），`effectiveStacks()` 改为统一走它；新增静态入口 `onHeroRevive(Hero)`：已有 buff 直接返回，`karmaFloor > 0` 才 `Buff.affect` 重挂。 |
| `actors/hero/Hero.java` | `live()` 末尾（detach 循环与 `Regeneration`/`Hunger` 之后）补一句 `Release.onHeroRevive( this )`。 |

**为什么放 `live()`**：`live()` 同时被 `Dungeon.init()`（新开局）与 `Hero.resurrect()`（十字架复活）调用 ——
新开局时英雄尚无天赋、`karmaFloor` 恒为 0，是天然的空操作，不会平白发 buff。

**为什么不用 `revivePersists = true`**：那会连「死亡清空解放层数」这一条设计意图一起丢掉；
用户明确要求保留清除、只补保底。

## 二、拼好饭：充能消耗 15% → 30%

`PinHaoFan.baseChargeUse` 由 `15f` 改为 `30f`（强度过高）。
心-命运（20f）与 Furioso-Replica（90f）**未动**，回归脚本专门断言这两个数字防误伤。
技能描述里的消耗数字来自 `ArmorAbility.desc()` 的 `Messages.get(this, "cost", (int)baseChargeUse)`，
用的是基类兜底键 `actors.hero.abilities.armorability.cost`（`充能消耗：_%d_`），
所以**改常量即可，无需改任何文本**。

## 三、Furioso-Replica「与你再会」：击杀计数顺序

**症状**：技能结束时的充能返还有时不生效（全部九次攻击都打出去时**完全拿不到返还**）。

**根因**：击杀计数写在了造成伤害**之前** ——

```java
final int newKills = !enemy.isAlive() ? kills + 1 : kills;   // ← 此刻伤害还没打出去
if (dmg > 0){ enemy.damage(dmg, hero); }
```

而 `selectTarget()` 只返回 `ch.isAlive()` 的目标，所以这句的 `isAlive()` 恒为 `true`、
**击杀数永远是 0**。返还公式是 `(击杀数 + 未使用次数) × 3%/点`，于是：
场上有 9 个以上目标（九次攻击全部用满）时未使用次数 = 0，返还就是 **0%**；
目标较少时靠「未使用次数」还能拿到一部分，看起来像是「有时好使」。

**改动**（`actors/hero/abilities/oracle/FuriosoReplica.java`）：把伤害提到计数之前，并用
`wasAlive && !enemy.isAlive()` 显式表达「打之前活着、打之后死了」：

```java
boolean wasAlive = enemy.isAlive();
if (dmg > 0){ enemy.damage(dmg, hero); }
final int newKills = (wasAlive && !enemy.isAlive()) ? kills + 1 : kills;
```

击退仍被 `if (enemy.isAlive())` 保护（尸体不参与击退），未使用次数
`unused = MAX_ATTACKS - hitsDone` 与封顶 100 的逻辑均未改动。

## 四、核验（源码级，未代跑 Gradle）

- `javac -proc:none -Xlint:all`（4 文件：`Release` / `Hero` / `PinHaoFan` / `FuriosoReplica`）**EXIT=0**；
  6 条诊断（`Release:149` 的 `[this-escape]`、`Hero:2315/278`、三处 `armor.updateQuickslot()` 的 `[static]`）
  **全部落在改动行之外**，都是上游既有写法（`Release:149` 是被我插入的 30 行顶下去的原 `ReleaseParticle` 构造）。
- 新增 `_chk/verify_oracle_fixes.py`：**35 条断言 + 6 条反例自测，ALL PASS**。
  除三处修复外还含两条**防过度修正**的断言：心-命运 20f / Furioso 90f 不许被误改、
  击退的 `isAlive()` 保护不许被删；以及延续 2026-09-20 的判据红线（三个方法体内不得出现 `heroClass`）。
- `_chk/check_utf8_all.py`：1432 文件全合法 UTF-8。

## 五、顺带发现（**未改**，待你定夺）

`剑刃解放` 的天赋文本写着「_+1：_ 解放最高叠加到_6层_；_+2：_ …最高叠加到_9层_」，
但 `Release.gainStack()` 只有 `if (stacks < MAX_STACKS)`（`MAX_STACKS = 9`），**没有按天赋点数取上限** ——
也就是说 +1 时同样能叠到 9 层，与文本不符。这不在本次需求范围内，没有动。

---

# 2026-09-20 水仙十字圣剑（NarcissusCrossSword，二阶三形态武器）

用户自建武器：**形态由英雄等级自动决定、玩家无法手动切换**，两个特殊形态都**彻底阻断经验值获取**。

## 一、需求 → 实现对照

| 形态 | 进入条件 | 图标 | 面板 | 效果 |
|---|---|---|---|---|
| 常态 | 其余等级 | `xy(3,41)` | `2+L ~ 15+3L` | 无特殊效果 |
| 芒性 | 英雄**恰好 1 级** | `xy(4,41)` | `6+L ~ 35+7L` | 最低深层数每 1 层 +20% 精准/闪避；经验→等额**不衰减护盾** + `经验÷5` 回合神器充能/充能/祝福；受击免伤 + 对视野内全体造成一次伤害 + 12 回合致盲，60 回合冷却；白色光效覆盖附魔光效 |
| 荒性 | 英雄达到 **30 级** | `xy(5,41)` | `6+L ~ 35+7L` | 经验→等额**回合数祝福**；攻击时对视野内全体同时造成伤害 + 12 回合灵视，60 回合冷却；蓝紫光效覆盖附魔光效 |

三张图标均 **14×14**（`assignItemRect(...,14,14)`）。常态 `stats_desc` 写明「1 级→芒性 / 30 级→荒性」的切换条件。

## 二、关键实现点

### 一、为什么「一形态一个类」而不是「一个类 + form 字段」

`Item.image` 是在实例**构造时**写进 `ItemSprite` 的，之后不会自动重读 —— 用 form 字段切形态，
快捷栏 / 装备栏 / 动作按钮上的图标与文字不会跟着变，得自己去把每一个 `ItemSprite` 找出来刷新。
沿用本作已有的「换实例」写法（`MorphWeapon` / `SealedSwordBase`）可以让这些引用**一次全部重建**。

搬运**直接复用 `MorphWeapon.morphInto(current, hero, targetClass)`**（它接受任意 `Weapon`、返回 boolean），
而不是自己再写一份状态搬运：它已经处理了等级（取 `trueLevel()` 而非 `level()`，避免诅咒菱晶加成被反复固化）、
附魔 / 诅咒 / 鉴定 / 数量 / `masteryPotionBonus` / `enchantHardened` / `curseInfusionBonus` / `augment`，
以及快捷栏改指、槽位原地替换、按钮重建与头顶虚影。**自己写只会多出一个「漏搬字段」的漏点。**

### 二、经验阻断只留一个入口

在 `Hero.earnExp` **最开头**接 `NarcissusCrossSwordBase.interceptExp(this, exp)`，命中即 `return`。
这一刀等于连坐掐掉同方法内的：5 件神器的充能、背包物品的 `onHeroGainExp` 回调、3 个天赋计数器 ——
这正是「无法再获得经验值」的应有之义。作为替代，芒性自己发放护盾 + 充能 + 祝福，荒性发放祝福。

配套一处：`Mob.die` 里弹「+经验」浮字的判断要加上 `&& !NarcissusCrossSwordBase.blocksExpNow(Dungeon.hero)`，
否则怪死时会弹一个**永远不会到账**的加经验浮字。

### 三、非衰减护盾必须自定义

原版 `ShieldBuff` 的实现是「`act()` 里同时 `spend(TICK)` + 掉血」，`Barrier` 更是按回合衰减。
要做「只被伤害消耗、不随时间衰减」，只能自己写一个 `NarcissusShield extends ShieldBuff`，
把 `act()` 缩成**只 `spend(TICK)`**（不掉血）。直接用 `Barrier` 会随时间漏光。

### 四、「闪避加成」只能挂引擎侧

武器没有原生的「闪避系数」钩子 —— `KindOfWeapon.defenseFactor` 给的是**减伤区间**而不是闪避。
所以芒性的闪避加成落在 `Hero.defenseSkill` 里紧跟「闪避戒指」那句之后
（`evasion *= NarcissusCrossSwordBase.heldEvasionMultiplier(this)`）。
精准侧则有现成钩子（`Weapon.accuracyFactor`），由 `NarcissusSwordMang` 自行覆写，不必动引擎。

### 五、形态看护 buff 与读档安全

**不在 `activate()` 里直接变形**：`activate` 会在装备流程与 `Belongings.restoreFromBundle` 中途被调用，
此刻替换 `belongings.weapon` 的实例会把「正在恢复的那件东西」从脚下抽走。
做法是在 `activate()` 里只挂一个常驻的 `FormKeeper extends Buff`，真正的变形留给它下一次 `act()` ——
延迟一拍既安全又不丢同步（英雄的下一个回合相位必然 act 一次）。`FormKeeper` 标 `revivePersists = true`：
安卡复活后剑仍在手上，看护不该被 `Hero.live()` 摘掉。

### 六、掉落池接线

入 `Generator.WEP_T2`：`classes` 追加 `NarcissusCrossSword.class`（**常态类**，芒性/荒性由等级自动变形），
`defaultProbs` 同步从 11 项扩到 12 项。`Generator` 取物是 `Random.chances(cat.probs)` 拿下标再
`cat.classes[i]`，**两数组长度必须相等** —— 少了会在 `probs[i]--` / `classes[i]` 处越界，
多了则末位武器永远抽不到。按用户决定：**不打 E.G.O 标签**（不登记 `EGOWeapons`），
理由是这样一把独立故事的圣剑不属神谕的 E.G.O 体系，且打标签后会被分解成脑啡肽、被重构随机产出，
与「只在 1 级 / 30 级才有形态意义」的设计相冲。

## 三、新增 / 改动文件

**新增**（6 个 Java + 1 个核验脚本）：

- `items/weapon/melee/NarcissusCrossSwordBase.java` —— 抽象基类：三形态常量、`formFor` / `held` / `syncForm`、
  最深层数倍率、经验拦截、视野内全体打击、冷却，以及内嵌 `FormKeeper`。
- `items/weapon/melee/NarcissusCrossSword.java` / `NarcissusSwordMang.java` / `NarcissusSwordHuang.java` —— 三个形态。
- `actors/buffs/NarcissusShield.java`（非衰减护盾）/ `actors/buffs/NarcissusCrossCooldown.java`（60 回合冷却）。
- `_chk/NarcissusLocCheck.java`（文本实装载）/ `_chk/verify_narcissus_sprites.py`（贴图逐像素）。

**改动**：

- `sprites/ItemSpriteSheet.java` —— 3 个图标常量（写在 `assignItemRect` 所在 `static{}` **之前**）。
- `actors/hero/Hero.java` —— `earnExp`（拦经验）/ `defenseProc`（受击反击）/ `defenseSkill`（闪避倍率）三处挂钩。
- `actors/mobs/Mob.java` —— `destroy()` 里经验浮字加阻断判断。
- `items/Generator.java` —— `WEP_T2`（import + classes + defaultProbs）。
- `messages/items/items_zh.properties` / `items.properties` —— 11 键 ×2 语言。
- `messages/actors/actors_zh.properties` / `actors.properties` —— 2 个 buff 的 name/desc ×2 语言。

## 四、核验（源码级，未代跑 Gradle）

- `javac -proc:none -Xlint:all`（10 文件）**EXIT=0、0 错误**；`Generator.java` 6 条、`Hero.java` 2 条诊断
  **全部落在改动行之外**（`Generator:326/338/746/748/961`、`Hero` 的 `earnExp` 签名 `rawtypes` 与构造期 `this-escape`），
  均为上游既有写法。
- `_chk/NarcissusLocCheck.java`：**43 条断言 ALL PASS**（真 `Properties.load` + 真 `String.format` 跑两条带参文本）。
- `_chk/verify_narcissus_sprites.py`：三格均非空（各 98 个不透明像素）、**溢出 0**、
  `xy` 无撞格、三格配色互不相同（常态金棕 / 芒性白黄 / 荒性蓝紫）。
- `_chk/check_utf8_all.py`：1438 文件全合法 UTF-8。

## 五、待办 / 待确认

- **面板与效果数值未经实机验证**：三形态面板、60 回合冷却、12 回合致盲/灵视、每层 20% 加成
  都只做了源码级核验，需要实际进游戏跑一遍（F2 →「武器」标签可直接生成三个形态）。
- 「芒性」会把英雄**永久锁在 1 级**（不再获得经验 ⇒ 不会升到 30 级），这是需求本身的推论，
  不是缺陷；要脱离形态只有把手上的剑换掉。

---
# 2026-09-20 解放叠加上限对齐天赋文本 + 「自伤换成长」击穿「咬紧牙关」免死

## 一、解放（Release）叠加上限按天赋点数取

**症状**：天赋文本写「_+1：_ …解放最高叠加到_6层_」，但实际 **+1 时同样能叠到 9 层**——文本与代码口径漂移。

**根因**：`Release.gainStack()` 只有 `if (stacks < MAX_STACKS)`（`MAX_STACKS = 9`），**没有按天赋点数取上限**。

**改动**（`actors/buffs/Release.java`）：

| 项 | 说明 |
| --- | --- |
| `MAX_STACKS_1 = 6`（新常量） | +1 时的上限；`MAX_STACKS = 9` 保持为 +2 上限兼全局上限 |
| `growthCap(Hero)`（新静态方法） | `+2 ⇒ 9` / `+1 ⇒ 6` / `0 点 ⇒ 0`，与 `Talent.BLADE_RELEASE` 的描述严格对齐 |
| `gainStack()` | 改走 `growthCap( ownerHero() )`，不再写死 `MAX_STACKS` |
| `debugGainStack()`（新方法） | **调试专用**：无视天赋上限叠到 9 层。`WndDebug` 的「附加 buff」按钮改调它——否则调试台不要求英雄真点出 T2，叠层入口会被新上限锁死 |

「业的保底层数」（`karmaFloor`，仅 +2 生效）与「复活重挂」（`onHeroRevive`）口径未变。

## 二、自伤换成长击穿「咬紧牙关」的免死

**症状**：中指长兄开盔甲技能「咬紧牙关」（0 血不死）后，**蓄血圣杯的血祭 / 割腕的割腕同样可以无条件刷等级**——血量打到 0 也不死，而这两件道具的升级判据只是「挨完这一下还活着」。

**根因**：「免死」判据在 `Hero.isAlive()`（`HP <= 0` 时看 `gritTeeth.undying()`），原本只有**解离射线**能击穿它（`checkRayBypass`）；「自伤换成长」这一类根本没被考虑，于是「挨完还活着 ⇒ 升级成功」再次成立——与 2026-09-18「过人的毅力」那次是同一种病：**保命机制抹平了升级代价**。

**改动**：

| 文件 | 改动 |
| --- | --- |
| `actors/buffs/GritTeethBuff.java` | `checkRayBypass` 改名 **`checkBypass`**，判据扩成 `!isRay(src) && !isSelfHarm(src)`；新增 `isSelfHarm(Object) = src instanceof SelfHarmCost`（与 `Talent.perseveranceCap` **共用同一个标记接口**，不另开名单） |
| `actors/Char.java` | 调用点改名 + 注释同步（位置不变：`if (HP<0) HP=0;` 与 `if (!isAlive()) die(src);` 之间） |
| `items/SelfHarmCost.java` | javadoc 写明**两处**取用点（下限保护 / 免死），并声明两处共用同一判据 |
| `ChaliceOfBlood` / `WristSlit` | 类注释同步（保命机制有两套，都不能抹平升级代价） |
| `messages/actors/actors_zh.properties`、`actors.properties` | 技能 `short_desc` / `desc` 与 buff `desc` 都补上「血祭、割腕这类自伤换成长同样可以击穿」 |

**为什么「只有致命一击才击穿」**：`checkBypass` 里 `if (ch.HP > 0) return;` 保留了原有语义——射线/自伤**没打死你**的时候不会提前作废这次免死。所以开着咬紧牙关血祭、只要没被这一下打死，照样是「挨完还活着 ⇒ 升级成功」，这是设计内的正常玩法。

## 三、核验（源码级，未代跑 Gradle）

- `javac -proc:none -Xlint:all`（7 文件）**EXIT=0**；诊断全部落在改动行之外（`Release` 的 `[this-escape]` 是 `ReleaseParticle` 构造、`GritTeethBuff` 3 条 `rawtypes` 是 `RAY_SOURCES`、`ChaliceOfBlood:212` 的 `[cast]`、`WndDebug` 3 条、`Char` 的 `new Class[0]` 系列，均为上游既有写法）。
- 新增 `_chk/verify_grit_selfharm.py`：**67 条断言 + 13 条反例自测，ALL PASS**（跨 8 个文件；含「常量 ↔ 天赋文本口径一致」与「调试台不能退回 gainStack」两条防漂移断言）。
- 新增 `_chk/GritLocCheck.java`：真 `Properties.load` + 真 `String.format`，**ALL PASS**。除非空 / `_` 奇偶 / U+FFFD / `/n/` 外，专查两条只有真解析才看得出的问题：**描述必须解析出两段**（转义写成真换行 ⇒ 游戏里只剩第一段）、**buff desc 的 `%s` 必须能被 format 吃掉**。
- `_chk/check_utf8_all.py`：1438 文件全合法 UTF-8。

## 四、踩坑：heredoc 会把转义还原成真换行

`.properties` 里多段文本必须写成**字面量转义**（反斜杠 + n）。用 `bash <<'EOF'` 把 Python 源码喂进去时，这一层转义会把双反斜杠还原成**真换行**——落盘后 entry 断成两截，游戏里表现成「描述只剩第一段」，**不报错**。本轮第一次改写就中了这个坑（`gritteeth.desc` / `gritteethbuff.desc` 各断一次）。对策：这类改写一律**先把脚本写进文件**（`_chk/fix_grit_text_escape.py`）再跑，脚本内含**幂等**的「合并续行 + 回写字面量转义」修复与复核。

## 五、顺带发现（**未改**）

`actors.properties`（英文）比 `actors_zh.properties` 少 **179 个键**（含 `blade_release.title/desc`、`neverforget.cast`、oracle / ringmaster 的整组技能文本、`acceleratingfuture` 等 buff 文本）——本作英文文本长期滞后，英文语言下这些条目会直接显示成键名。不在本次需求范围内，没有动。

# 2026-09-20 原版神器强化形态五件（血宴圣杯 / 一生炖菜 / 他人之锁 / 9章2节 / 迫近之日）

## 一、需求

把 **5 件原版神器**各做一个「强化形态」：**保留原版形式**（原版的功能文本、等级/充能机制照旧），只**附加一条新效果**；做成**独立的新神器**（不改原版），并且**只作为强化形态存在、不进任何掉落池**——唯一获取途径是炼金：**原版神器 + 60 脑啡肽，12 点炼金能量**，合成后**保留原神器的等级/充能与全部状态**。配方要出现在**炼金指南 UI**（日志 → 炼金页）里。

| 强化形态 | 基底（原版） | 新增效果 | 贴图 |
| --- | --- | --- | --- |
| 血宴圣杯 | 蓄血圣杯 | 记录自己与敌人受到的**每一跳流血**为「血宴」；血祭时可按 **1:1** 抵掉本次升级伤害（抵得完 ⇒ **零伤升级**）。与武器**渴望**同持时：渴望把流血转成的护盾在衰减时按 **1/5** 回血 | `xy(1..3, 42)`，12×15，3 帧 |
| 一生炖菜 | 丰饶之角 | 食用时**每消耗 1 充能额外回 3 血** | `xy(4..7, 42)`，15×16，4 帧 |
| 他人之锁 | 灵魂锁链 | 牵引敌人时对**敌我双方**施加残废 + 虚弱（自己 3 回合 / 敌人 20 回合） | `xy(8, 42)`，15×16 |
| 9章2节 | 无序魔典 | 阅读时**点燃视野内所有敌人**；对阅读前**已在燃烧**的敌人额外 5 回合残废 | `xy(9, 42)`，13×16 |
| 迫近之日 | 先知护符 | 探查到**隐藏陷阱**额外累积 10 点升级进度；探查到**敌人**时施加 10 回合恐惧 | `xy(10, 42)`，16×16 |

42 行此前**整行空闲**，本批占用 1~10 列。

## 二、核心设计：父类开钩子，子类只覆写

五件强化形态**一律 `extends` 原版神器、只覆写钩子**，原版逻辑与**功能文本**都不复制：

- **文本靠 `Messages.get` 的父类链继承**：新类只需写 `name` / `desc` / `desc_enhanced`，原版那批键（`desc_1/2/3`、`prick_warn`、`onprick`、`ac_snack`、`desc_hint`、`desc_cursed`…）自动沿父类链取到 ⇒ 不必抄、也不会与上游漂移。
- **形象刷新抽成虚方法**：原版把贴图**硬编码在多处**，覆盖某处就漏另一处。本轮先做了一次「提虚方法」重构：
  - `ChaliceOfBlood.updateImage(int lvl)`（原版 2 处：`upgrade()` / `restoreFromBundle()`）
  - `HornOfPlenty.updateImage()`（原版 **4** 处：`doEatEffect` / `charge` / `restoreFromBundle` / `hornRecharge.gainCharge`）
  子类只覆写这一处，**四处调用点一起换图**。
- **其余钩子**：`ChaliceOfBlood.extraPrickHP(hero)`（喂死亡概率窗口）、`ChaliceOfBlood.mitigatePrickDamage(hero, dmg)`、`EtherealChains.onEnemyPulled(hero, enemy)`、`TalismanOfForesight.extraTrapExp(oldValue)` / `onEnemyScryed(ch, dist)`。

## 三、三个「位置决定成败」的接点

1. **`mitigatePrickDamage` 必须在「至少 1 点」保底之后调用**。`prick()` 里先有 `if (damage <= 0) damage = 1;`，钩子放在它**之后**才允许把伤害削到 0 ⇒ 才做得到「血宴抵完 ⇒ 零伤升级」。放在之前会被保底重新抬回 1 点。
2. **`extraPrickHP` 是死亡概率窗口的唯一入口**。血祭弹出的「生死确认」窗按 `hero.HP + shielding()` 算必死概率；不把血宴算进去就是在骗玩家（明明不会死，提示却写着必死）。所以两者必须**同时**接。
3. **`onEnemyPulled` 只在「真的拉动了怪物」之后触发**。`chainEnemy` 有多个提前 return，且位移是**异步**完成的（`Pushing` 回调里才落位）⇒ 覆写整个方法既容易漏判，又要重写动画与回调。父类在回调末尾（`artifactProc` 之后）加了一个默认空实现的钩子，只覆盖「拉动成功」这一路（把自己拉过去的 `chainLocation` 不会走到）。

另有两个「看着能覆写、其实不行」的坑：

- **`TalismanOfForesight.scry` 是匿名字段初始化**（`public CellSelector.Listener scry = new ...(){}`）。**Java 字段不参与多态**：子类再声明一个同名 `scry` 只会**遮蔽**父类那个，而父类 `execute()` 里读到的仍是父类自己的实例 ⇒ 覆写根本不生效。所以改为在「揭到隐藏陷阱」「探查到敌人」两处各开一个 protected 钩子。
- **「本来就烧着」必须在点燃之前判定**（9章2节）。`igniteAllInView()` 先取 mob 快照、先记 `wasBurning`，再统一点燃；若边烧边判，本轮先被点着的敌人立刻满足「已燃烧」，人人白吃一次残废。

## 四、炼金配方：一个通用基类 + 四处登记

`items/artifacts/ArtifactEnhanceRecipe<S extends Artifact, R extends Artifact>`：

- 常量 `ENKEPHALIN_NEEDED = 60` / `ENERGY_COST = 12`；子类只声明三件事：`acceptedArtifact()` / `createEnhanced()` / `transferState()`。
- `testIngredients` 用 **`it.getClass() == acceptedArtifact()` 严格比较**（不是 `instanceof`）⇒ **已强化品不再被接受**，否则玩家会白扔 60 脑啡肽换一件一模一样的东西。
- `brew` 拿到的是釜里**真实的那件神器实例**（不是副本），直接读状态拷到新实例上；`transferState` 搬 `level/exp/chargeCap/charge/partialCharge/cooldown` + 诅咒与鉴定三标志，**外加各神器自己的持久字段**（号角 `storedFoodEnergy`、魔典随机 `scrolls` 清单）。不搬 ⇒ 玩家养到 +10 的神器在合成瞬间被静默清零。
- **神器原本进不了炼金釜**：入釜选择器走 `Recipe.usableInRecipe`，它对 `EquipableItem` 只放行近战/可升级投掷武器 ⇒ 神器被直接挡在釜外，表现为「配方怎么都不生效」。已在那里放行 `item instanceof Artifact`。
- 登记两处：`Recipe.twoIngredientRecipes`（实际配方）+ `ui/QuickRecipe.java` 的 **`case 8`**（炼金指南预览）。
  页签索引来源是 `journal/Document.java` 的 `ALCHEMY_GUIDE.pagesStates` 插入顺序 ⇒ **第 8 页（0 基）就是 `Spells`**，`_chk/verify_artifact_enhance.py` 会把这条顺序一起断言。

## 五、渴望联动：`ThirstBloodBarrier`（含**已知取舍**）

`Char.damage` 里「渴望把流血转成护盾」那条路改挂 `actors/buffs/ThirstBloodBarrier extends Barrier`（**上游有先例**：`DwarfKing.DKBarrior extends Barrier`）。它比 `Barrier` 只多一件事：每次衰减时问一句「英雄身上有没有装备血宴圣杯」，装了就把衰减量的 1/5 折算回血（不足 1 点的零头攒在 `healPool` 里，否则 1/5 取整永远被抹成 0）。

两个实现要点：`act()` 必须在 `super.act()` **之前**存下 `target`（护盾耗尽会自我 `detach()` 把 `target` 置空）；`Barrier` 的衰减是「攒够 1 点才 `absorbDamage(1)`」，所以每 tick 的 `lost` 是 0 或 1。

> **⚠️ 已知代价（有意取舍，改动前先读 `ThirstBloodBarrier` 类注释的「已知代价」一节）**
> `Char.buff(Class)` 是**精确类匹配**（`b.getClass() == c`），**不是** `isInstance`。所以英雄同时持有普通 `Barrier` 时（例如先被流血转出护盾、又喝了护盾药剂）：`Buff.affect(hero, Barrier.class)` 会**另建一份**（buff 栏出现**两个 ARMOR 图标**），且所有 `hero.buff(Barrier.class)` 判定都**看不见**本类（`Dewdrop`/`Waterskin` 的护盾限量、`Blocking`/`NarcissusShield.fx` 的「还有没有护盾」、`Barrier.fx` 摘 SHIELDED）。
> 这些后果都**轻微**（只多显示一个图标 / 多给一点护盾），**不会崩、也不会让回血失效**。**伤害吸收不受影响**：`ShieldBuff.processDamage` 走 `target.buffs(ShieldBuff.class)`，那是 `isInstance`，两类会被一起算进伤害池。
> 选这条路是为了**「只有渴望造出的护盾会回血」这一语义精确 + 改动局部**；代价是不建子类、改在 `Barrier.act()` 开钩子并给「渴望充入的护盾余量」记账（那会把血宴逻辑塞进全游戏共用的热路径，且伤害分摊后分不清哪份是渴望的份额）。

血宴的累积取在 `Bleeding.act()`（流血伤害的唯一结算处），插在 `target.damage(...)` **之后**，且只统计**英雄自身 + 敌人**（友方/中立不计）。

## 六、一个被顺手修掉的重复播报

抽 `mitigatePrickDamage` 钩子时，父类 `prick()` 已经按「减免了多少」统一播报了（键 `prick_mitigated`），而子类里又 `GLog.p` 了一次 ⇒ 弹**两条一模一样**的消息。已改为**父类唯一出口、钩子内只算不打**，并把该键从血宴专属的 `feast_block` 改名为中性的 `prick_mitigated`（父类不该跟「血宴」这个具体名字耦合）。回归里加了断言：`mitigatePrickDamage` 体内不得出现 `GLog` / `prick_mitigated`。

## 七、改动落点

| 文件 | 改动 |
| --- | --- |
| `sprites/ItemSpriteSheet.java` | 10 个常量（42 行 1~10 列）+ 13 条 `assignItemRect`（常量声明均在该 `static{}` **之前**） |
| `items/artifacts/ArtifactEnhanceRecipe.java` | **新增**，通用强化配方基类 |
| `items/artifacts/{BloodFeastChalice,LifelongStew,ChainOfOthers,ChapterNineVerseTwo,ApproachingDay}.java` | **新增**，5 件强化形态（含内嵌 `CraftRecipe`） |
| `items/artifacts/ChaliceOfBlood.java` | `min/maxPrickDmg` 提 `protected`；新增 `extraPrickHP` / `mitigatePrickDamage` / `updateImage(int)`；`prick()` 接三处；死亡概率计入额外抵挡量 |
| `items/artifacts/HornOfPlenty.java` | 4 处硬编码贴图抽成 `updateImage()`；`storedFoodEnergy` 提 `protected` |
| `items/artifacts/EtherealChains.java` | 新增 `onEnemyPulled` 钩子（回调末尾、`artifactProc` 之后） |
| `items/artifacts/TalismanOfForesight.java` | 新增 `extraTrapExp` / `onEnemyScryed` 钩子（因 `scry` 是匿名字段，不能覆写） |
| `items/artifacts/UnstableSpellbook.java` | `scrolls` 提 `protected`（合成时搬运随机清单） |
| `actors/buffs/ThirstBloodBarrier.java` | **新增**，渴望护盾的专属子类（衰减回血 1/5 + `healPool` 零头池） |
| `actors/buffs/Bleeding.java` | `act()` 接 `BloodFeastChalice.onBleedTick`（`target.damage` 之后） |
| `actors/Char.java` | 渴望特判改挂 `ThirstBloodBarrier` |
| `items/Recipe.java` | `usableInRecipe` 放行 `Artifact`；`twoIngredientRecipes` 登记 5 条；顺手删掉一个死 import `EGOWeapons` |
| `ui/QuickRecipe.java` | `case 8`（Spells 页）追加 5 条预览 |
| `messages/items/items_zh.properties`、`items.properties` | 16 键 × zh/en |

## 八、核验（源码级，未代跑 Gradle）

- `javac -proc:none -Xlint:all`（**17 文件**）**EXIT=0、0 错误**（日志里「错误」二字出现 0 次）。本批文件相关的告警全部落在**未改动的行/上游既有写法**上：`ChaliceOfBlood:253` 的 `[cast]`（`charge()` 里 upstream 的 `(Hero) target`）、`HornOfPlenty:242~254` 的 `[lossy-conversions]`（`gainFoodValue` 复合赋值）、`HornOfPlenty:65` / `UnstableSpellbook:72` / `QuickRecipe:161,247` 的 `[this-escape]`（构造函数）、`UnstableSpellbook:85` 的 `[rawtypes]`（`ArrayList<Class>` 是上游既有写法，本次只改了访问修饰符）、`Char.java` 的 `rawtypes` 系列（`new Class[]{...}`）。
- 新增 `_chk/verify_artifact_enhance.py`：**115 条断言 + 反例自测，ALL PASS**。跨 A 父类钩子 / B 强化形态类 / C 炼金配方 / D 联动接线 / E 文本 五组；顺序敏感处一律 `body.index()` **比位置**（保底 vs 减免、`target.damage` vs `onBleedTick`、`wasBurning` vs `reignite`、`super.act()` vs 取 `target`、`artifactProc` vs 钩子）；把 `Char.buff` 的**精确类匹配**前提与「已知代价」注释也钉成断言，防后人当 bug「修」。
- 新增 `_chk/verify_artifact_sprites.py`：纯 Python 解码 `items.png`（无 PIL），13 帧**非空 / 无溢出 / 同组多帧两两不同**全绿；并硬断言「常量声明行必须早于使用它的 `static{}`」（顺序陷阱，否则**所有物品图标整体错位且不报错**），含反例自测。xy 撞格只判本批的 42 行（**上游内部类各自 `xy(1,1)` 给自己的另一张图集编址，全表扫描会有假阳性**）。
- 新增 `_chk/ArtifactLocCheck.java`：真 `Properties.load` + 真 `String.format`，**ALL PASS**（16 键 ×2 语言；分段必须解析出两段、血宴两条带参文本的 `%d` 必须能被 format 吃掉、5 个中文名硬断言、5 个原版神器 `name` 回归）。
- 新增 `_chk/check_unused_imports.py`（可复用）：javac **不报**多余 import，本工具自行统计。**首版判据有 bug 并被实测抓出 9 个假阳性**：本仓注释里大量使用 markdown 强调 `//**文字**`，其子串 `/*` 会让「先正则去 `/*...*/`、再去 `//`」的写法**从该行一路吞到文件末尾**（`Char.java` 里 `/*` 出现 3 次而 `*/` 只 2 次）⇒ 全都误判成「多余」。已改为**单趟左到右状态扫描**（进 `//` 就整行吃掉，字符串/文本块原样保留），带反例自测。
- `_chk/check_utf8_all.py`：1445 文件全合法 UTF-8、无「汉字+?」痕迹。

## 九、待人工验证

能力范围外（游戏窗口无输出可采集）：F2 调试台生成 5 件强化神器看图标与描述；炼金台按「原版神器 + 60 脑啡肽」验证消耗/能量/状态保留；血祭时验证血宴抵扣与零伤升级；与渴望同持后受流血，观察护盾衰减回血与**是否出现两个 ARMOR 图标**（见第五节已知取舍）。
# 2026-09-20 赫尔墨斯的双蛇杖禁止入遗骸 + 传火大剑继承加强为「等级/5 + 附魔」

## 一、赫尔墨斯的双蛇杖：`bones = false`

**症状**：神谕代行者死亡后，双蛇杖出现在英雄遗骸里，下一局**别的角色**拾取遗骸即可获得这件专属武器。

**根因**：`Item.bones` 默认为 `false`，但 **`EquipableItem` 把它置为 `true`** ⇒ **所有武器/护甲/神器默认都会进遗骸**（`Bones.pickItem` 按 `item.bones` 筛候选；`Bones.get` 取出后还会降到 +3 并强制诅咒）。双蛇杖从没显式关掉这个开关，于是和普通武器一样进了遗骸。

**对策**：`HermesCaduceus` 的实例初始化块里显式 `bones = false;`（与既有专属件同款：`LogicStudio` / `SealedSwordBase` / `DarkSilence` / `MasterRing` / `InstructionTerminal`）。

## 二、传火大剑跨局继承：从「白板 +0」加强为「等级/5（向下取整）+ 附魔」

原实现只发一把 +0 白板（设置里只存一个布尔标记 `firelink_next_run`），本次按需求同时继承**强化等级**与**附魔**：

| 环节 | 落点 | 说明 |
|---|---|---|
| 记录等级 | `FirelinkGreatsword.markNextRun()` → `SPDSettings.firelinkLevel(int)` | 取原剑 **`trueLevel()`**（纯升级等级） |
| 记录附魔 | 同处 → `SPDSettings.firelinkEnchant(String)` | 存附魔**类全名** `enchantment.getClass().getName()`；无附魔存空串 |
| 发放 | `grantAtRunStart()`（`Dungeon.init()` 在 `initHero` 之后调用） | 读三键 → **立即清零** → 造剑 → `upgrade(srcLevel / INHERIT_LEVEL_DIVISOR)` → 反射重建附魔 → `identify()` → 收进背包 |

### 一、为什么取 `trueLevel()` 而不是 `level()`
`Weapon.level()` 在 `curseInfusionBonus` 为真时返回 `level + 1 + level/6`；而继承品**不带**该加成 ⇒ 用 `level()` 相当于白送 1~2 级。本仓已有同类铁律（形态切换同步等级一律用 `trueLevel()`）：**只要把自身等级当成另一表达式的基准，就必须区分二者**。

### 二、为什么用「类名反射」而不是序列化对象
设置里只有 `int/boolean/String`（`GameSettings.put/getInt/getString/getBoolean`），存不下 `Bundlable`。附魔类本身是**无参可构造的 public static 类**（`Weapon.Enchantment` 子类），`Reflection.forName(name)` + `isAssignableFrom(Weapon.Enchantment.class)` + `Reflection.newInstance(cls)` 即可无损还原（已实测 `Blazing` 的 `getName() → forName → newInstance` 往返一致）。任何一步失败都**静默退回白板剑**，不抛异常、不影响开局。

### 三、向后兼容
旧版只写过 `firelink_next_run`；新增的 `firelink_level` / `firelink_enchant` 缺键时分别取 `0` / `""` ⇒ 老档（或更新后第一次继承）拿到的仍是 +0 白板剑，行为不变。发放后两键清零，不残留到再下一局。

## 三、改动落点

| 文件 | 改动 |
|---|---|
| `items/weapon/melee/HermesCaduceus.java` | 实例初始化块 `bones = false;` + 类注释说明 |
| `items/weapon/melee/FirelinkGreatsword.java` | 新增 `INHERIT_LEVEL_DIVISOR = 5` / `equippedInstance(Hero)` / `enchantFromName(String)`；`markNextRun()` 存等级+附魔；`grantAtRunStart()` 按「等级/5 + 附魔」发放；`equippedBy()` 改为委托 `equippedInstance()` |
| `SPDSettings.java` | 新增 `firelink_level` / `firelink_enchant` 两键 + 助手（CRLF 文件，改后仍保持 CRLF） |
| `messages/items/items_zh.properties`、`items.properties` | `firelinkgreatsword.stats_desc` 补「继承 1/5 等级 + 附魔」；英文侧同时补上原本缺失的继承说明 |

## 四、核验（源码级，未代跑 Gradle）

- `javac -proc:none -Xlint:all`（3 文件：`HermesCaduceus` / `FirelinkGreatsword` / `SPDSettings`）**EXIT=0、0 错误**；`FirelinkGreatsword`、`SPDSettings` **0 告警**，`HermesCaduceus` 只剩两条**改动前既有**告警（`:66` `[rawtypes]` 的 `new Class[][]`、`:349` `[lossy-conversions]` 的 `delay *= Math.pow(...)`，均落在未改动行，只因新插 5 行而整体下移）。
- 新增 `_chk/verify_hermes_firelink.py`：**66 条断言 + 反例自测，ALL PASS**（Hermes 2 / Firelink+SPDSettings 26 / 文本 zh+en 14 / 断行自检 2 / 反例 5）。顺序敏感处一律 `body.index()` 比位置（先读后清、清零早于造剑、附魔早于 `identify()`）；「不得再出现旧写法」先 `strip_comments`。
  - **写脚本时踩的两个假红**（修脚本、未放宽断言）：① 赫尔墨斯的**注释里**写着「`EquipableItem` 默认 `bones = true`」，全文搜 `bones = true` 会假红 ⇒ 必须先剥注释；② 反例自测里旧文件取不到 `enchantFromName` 方法体时**提前 return**，后续断言键整体缺失、`.get()` 返 `None` ⇒ 改为「方法体缺失即置空串，每条断言各自判 False」，并引入 `before(a,b)` 兜住 `index()` 的 `ValueError`。
- 新增 `_chk/FirelinkLocCheck.java`：真 `Properties.load` 读两份 items + 真跑继承公式边界值（0/1/4/5/6/9/10/12/24/25/30 ⇒ 0/0/0/1/1/1/2/2/4/5/6）+ 真跑附魔重建链（`Blazing` 往返一致、假类名返 null、非附魔类被 `isAssignableFrom` 拦掉），**ALL PASS**。
  - **顺带发现**：`com.watabou.utils.Reflection.forName` 内部走 gdx `ClassReflection.forName`，**会引用 gdx-controllers 的类** ⇒ 单跑这个 Java 核验脚本必须把 `gdx-controllers-core` 加进 classpath（与 `egopd-source-verify` 第 1 步同款要求）；它对「类不存在」是**捕获后返回 null**（会往 stderr 打一段堆栈），所以 `enchantFromName` 里判 `cls == null` 足以兜住。
  - **真装载的段数判据要写对**：`\n\n` 解析成两个换行 ⇒ `split("\n")` 切出「段1 / 空串 / 段2」**三块**，断言必须是「非空块恰好 2 个 + 中间块是空行」，写成「段数 == 2」会假红。
- `_chk/check_utf8_all.py`：1445 文件全合法 UTF-8。

## 五、待人工验证

能力范围外（游戏窗口无输出可采集）：① 非神谕角色死亡后确认遗骸里**没有**双蛇杖，而神谕角色死亡后遗骸里仍有专属遗物 `BrokenTerminal`（未受影响）；② 传火大剑 +N 通关后下一局开局确认拿到 剑(+(N/5)) 且**附魔一致**（先在原剑上贴一张附魔卷轴更容易看出来）；③ `+4` 剑（N/5=0）确认仍是白板剑；④ 连续两局验证「第二局不再返还」（键已清零）。

# 2026-09-20 出 v0.3.1（Bug 修复 + 中指长兄割腕穿透无敌 + EGO 特化神器）

## 一、本版内容

- **版本号**：`appVersionCode 930 → 931`、`appVersionName 0.3.0 → 0.3.1`（根 `build.gradle` 的 `ext`，唯一处；`android/desktop/ios` 都从这里取）。
- **更新日志**：`ui/changelist/EGOPD_Changes.java` 新增 major 条目 `EGOPD v0.3.1`（排在 v0.3.0 **之前**），下挂 3 条：

  | 按钮 | 图标 | 要点 |
  | --- | --- | --- |
  | Bug 修复 | `Icons.get(Icons.SHPX)` | 概括性一条 |
  | 中指长兄：割腕穿透无敌 | `ItemSprite(WRIST_SLIT)` | 无敌期间割腕/蓄血圣杯不再能白送成长，自伤换成长行为**绕过无敌免死判定** |
  | EGO 特化神器 | `ItemSprite(ARTIFACT_BLOOD_FEAST_CHALICE1)` | 5 件强化形态；炼金台「原版神器 + 60 脑啡肽」（12 点能量）合成，保留等级/充能/状态 |

  新增 import：`sprites.ItemSprite` / `sprites.ItemSpriteSheet` / `ui.Icons`。
- `ChangesScene` **无需改动**：EGOPD 页（`EGOPD_TAB = 0`）在 v0.3.0 时已建好并适配过布局（两行 9 个页签按钮），本版**未新增页签** ⇒ case 序号 / `changesSelected` / `setRect` 三处都无需同步。

## 二、核验（源码级 + 打包后四道）

- `_chk/check_utf8_all.py`：**1445 文件**全合法 UTF-8。
- 单文件 `javac -proc:none -Xlint:all`（`EGOPD_Changes.java`）：**EXIT=0，0 错误 0 告警**（javac 无任何输出）。
- `_chk/patch_release_031.py`：**30 条断言 ALL PASS** —— 含「v0.3.1 条目唯一且排在 v0.3.0 之前」「恰 3 条 `addButton`」「括号配平」「无 `/n/` 误写」「三个图标常量在 `ItemSpriteSheet`/`Icons` 里真实存在」「5 个中文名确实在 `items_zh.properties`」，另有改动前**双向断言**（先验旧值 930/`0.3.0` 存在再替换）。
- **打包**：`:android:assembleDebug` → **BUILD SUCCESSFUL in 29s**；`:core:compileJava` 与 `:android:packageDebug` 均 **executed**（非 UP-TO-DATE）⇒ 改动确实进包。
- **① 元数据**（`aapt dump badging`）：`versionCode='931'`、`versionName='0.3.1-INDEV'`、`application-label='EGOPD'`、`application-icon-480/640` 均指向 `res/mipmap-anydpi-v26/ic_launcher.xml`。
- **② 新类/新文本进包**（dex 字面量计数）：`EGOPD_Changes` ×3、`EGOPD v0.3.1` ×1、`中指长兄：割腕穿透无敌` ×1、`EGO 特化神器` ×2、`绕过无敌的免死判定` ×1、`原版神器 + 60 脑啡肽` ×1。
- **③ 全量资产比对**（新脚本 `_chk/verify_apk_assets_031.py`）：APK 内 **533** 个 `assets/*` 与工作区 `core/src/main/assets` **md5 逐条一致**，无单边、无不一致。脚本按 skill 备忘先做 **cp437 → utf-8 中文条目名还原**再比。
- **④ 体积变化可解释**：APK **47.10 MB（49,383,020 B）**；对比上一版 `EGOPD_0.3.0.APK`（49,366,899 B）差 **+16,121 B（+0.033%）** —— 与「只多出一段 changelist 代码 + 常量池」相符（上一版几乎无增量打包空洞，故本次不必量空洞）。
- **归档**：`EGOPD_0.3.1.APK`（仓库根），md5 `dc2f86213da64b97e12933dc9f17045a`，与构建产物一致，`aapt` 复核头部正常。

## 三、待人工验证

- 游戏内「改动」界面 → EGOPD 页签：确认 v0.3.1 三条按钮的**图标与文本渲染正常**（`_强调_` 生效、无串行、无把两段并成一段）。
- 中指长兄盔甲技能无敌期间，割腕 / 蓄血圣杯应照常结算自伤与成长（不再被无敌吞掉）。
- **先卸载再安装**：同名同 versionCode 覆盖安装时，MIUI/EMUI 等会继续显示**缓存的**桌面图标与名称。

# 2026-09-21 标题滚动背景改为深红色调（横幅不动）

## 需求
把标题界面的滚动背景整体改成深红色调；横幅 `banners.png` 保持不变。

## 做法：为什么只改调色板就够了
4 张图层全是 **8 位索引 PNG（PLTE + tRNS）** ⇒ 改色 = **只重写 PLTE**，
IDAT（像素索引）**逐字节保留** ⇒ 尺寸 / 透明度 / 结构**零风险**，而且没有量化误差。
深红化用**双色调（duotone）**：取亮度后映射到「暗红 (18,2,4) → 亮红 (232,64,46)」色阶，
`gamma=1.0`、`mix=1.0`。这样原图的明暗结构与剪影全部保留，只把色相统一压到深红。

## 落点
- **改色**：`core/src/main/assets/splashes/title/` 下 4 张 ——
  `archs.png`、`back_clusters.png`、`mid_mixed.png`、`front_small.png`
- **未改**：`core/src/main/assets/interfaces/banners.png`（横幅。它是 **RGBA** 不是索引图，
  调色板改色在物理上就碰不到它；另有 mtime 证据：其 mtime 为 2026-09-04，早于本次操作）
- **未改任何代码**：`TitleBackground.java` 的 4 个 `TextureFilm` 帧尺寸与 4 个 `INIT_*_CHANCES`
  数组长度**一个都没动** ⇒ 帧网格仍成立

## 帧网格核对（改色后仍成立）
| PNG | 帧尺寸 | 网格 | 可用帧 | 权重数组需 |
|---|---|---|---|---|
| `archs.png` | 333×100 | 3×2 | 6 | 6 |
| `back_clusters.png` | 450×250 | 1×2 | 2 | 2 |
| `mid_mixed.png` | 273×242 | 7×4 | 28 | 24 |
| `front_small.png` | 112×116 | 9×4 | 36 | 20 |

## 参数与复现
```
python _chk/recolor_title_bg.py --check                  # 只看调色板统计
python _chk/recolor_title_bg.py --apply [--dark R,G,B] [--light R,G,B] [--gamma G] [--mix M]
python _chk/recolor_title_bg.py --preview                # 生成上排原图/下排新图的对照 PNG
python _chk/verify_title_bg.py                           # 39 项结构核验
python _chk/verify_title_bg.py --restore                 # 一键还原原图
```
`--mix 0.6` 可退化成「只染 60%」的轻微染色；`--dark/--light` 调深红色阶两端。

## 核验结果（39 项全通过）
尺寸 / 位深 / 色型 / 交错不变；**IDAT 逐字节不变**；**tRNS 逐字节不变**；
PLTE 项数不变且内容确实变了；逐项 alpha 不变（全透明/半透明/不透明的项数一致）；
可见像素均色 **(49,40,25) → (43,9,9)**（R/G = R/B ≈ 4.78，且整体仍然很暗）；
帧网格成立；横幅未改动。

## 影响面（重要）
这套滚动背景是 **10 个界面共用**的，不只标题界面：
`TitleScene` / `WelcomeScene` / `StartScene`（存档槽）/ `RankingsScene`（排行榜）/
`JournalScene`（日志）/ `NewsScene`（新闻）/ `ChangesScene`（改动）/ `AboutScene`（关于）/
`SupporterScene`（支持）/ `SurfaceScene`（地表结算）
⇒ **这 10 个界面的背景一起变红。**

## 备份与还原
原图备份在 `_chk/_bak_title_bg/`，**故意不放在 assets 里**（否则会被打进 APK 撑大包体）。
一键还原：`python _chk/verify_title_bg.py --restore`。
`banners.png` 未进备份（它没被改），其 sha256 已记在 `_chk/_bak_title_bg/RECOLORED.txt`。

## 未做 / 待人工验证
- **未启动游戏**，只做了源码级与字节级核验。
- 需**重新构建**才能在游戏里看到效果：`desktop/build/resources/main/` 与
  `android/build/intermediates/**/merge*Assets/` 里仍是上一次构建的旧图。
- 观感（深红是否够味、是否与 UI 撞色）只能人工在游戏里判断；
  不满意可调 `--dark/--light/--gamma/--mix` 重跑（脚本幂等，且有备份保护）。
- 横幅本身**未动**，按需求保持原样。

# 2026-09-21 标题火把火焰改为「黑金」（黑色为主 + 掺杂金黄）

## 需求
把标题界面的燃烧火焰效果改成「掺杂部分金黄色的黑色火焰」。

## 改前是什么颜色（可能出乎意料）
上游 SPD 的标题火把是**绿焰**（不是橙焰）：亮部均色 `(225,255,186)`、核心 `(0,234,69)`。
两张图与 `Fireball.java` 经 git 核对**均为上游原样未改**。

## 为什么是逐像素变换而不是改调色板
这两张是 **RGBA（color type 6）**，没有 PLTE ⇒ 只能变换 RGB 并**保持 alpha 逐像素不变**。
无损性依然成立：尺寸 / 位深 / 色型 / 交错不变、alpha 完全不动、无缩放重采样。

## 映射：两段各自独立的色阶
```
L ≤ thr : 黑 (10,8,10) → 余烬 (70,30,8)      大片低 alpha 的外焰 → 暗影/烟熏
L >  thr : 暗金 (206,146,30) → 亮金 (255,224,116)   最亮的焰心 → 金黄
```
分界点 `thr` 由「要让多少比例的像素变金」反推（`--gold-quantile`，默认 0.28，**alpha 加权**）。
必须按分位数而不能用固定阈值：两张图的亮度分布差很多
（tall 只有 3.5% 像素在 0.90 以上；short 有 9.2%，且 51.8% 落在 0.75–0.90），
固定阈值会让 short 几乎全变金，达不到「掺杂**部分**金黄」。

**踩过的坑（已修，记下来免得重犯）**：第一版用「余烬 → 金」一条连续色阶跨过阈值，
结果绝大多数「金区」像素落在阈值刚过一侧、插值到余烬附近，金区均色只有 `(141,90,20)`
——是暗琥珀不是金黄。改成金区**从真正的金起步**（阈值处直接等于 `gold_dim`）才读得出金色。
代价是阈值处有一步跳变，但它正好落在焰心亮部边界上，像素画风格里反而更利落。

## 落点
- **改色**：`core/src/main/assets/effects/fireball-tall.png`（横屏 61×61×24）、
  `core/src/main/assets/effects/fireball-short.png`（竖屏 47×47×24）
- **未改任何代码**：贴图路径是 `Fireball.java` 里的**硬编码字符串**，文件名不变即可，
  4 个帧网格与 `MovieClip.Animation` 帧表一个都没动
- **未改**：`effects/fireball.png` —— 注意 `Assets.FIREBALL = "effects/fireball.png"` 在**本作是死常量**
  （0 引用、文件也不存在），别被它误导

## 结果
| 贴图 | 可见均色 | 均 alpha | 不透明像素 | 「金」像素占可见 |
|---|---|---|---|---|
| `fireball-tall.png` | (49,237,91) → **(68,37,13)** | 49 → 49 | 4588 → 4588 | 0% → **8.2%** |
| `fireball-short.png` | (111,246,120) → **(67,34,11)** | 58 → 58 | 3500 → 3500 | 0% → **5.8%** |

金区均色 `(224,174,62)` / `(204,153,49)`（真金黄），黑区均色 `(54,24,9)` / `(57,25,8)`（近黑带暖）。
**可见像素占比、均 alpha、不透明像素数三项与改前完全相同** ⇒ 透明轮廓一点没动。

体积反而变小：`88688 → 75728 B`（85.4%）、`49725 → 40670 B`（81.8%）——色数变少，zlib 压得更好。

## 参数与复现
```
python _chk/recolor_fireball.py --check
python _chk/recolor_fireball.py --apply [--gold-quantile Q] [--black R,G,B] [--ember R,G,B] \
                                       [--gold-dim R,G,B] [--gold R,G,B]
python _chk/recolor_fireball.py --preview      # 生成原图/新图逐帧对照
python _chk/verify_fireball.py                 # 17 项核验
python _chk/verify_fireball.py --restore       # 一键还原原图
```
想要更「金」用 `--gold-quantile 0.45`（金区升到约 14~20% 像素）；
想让黑的部分更「有形」可把 `--black` 提亮到 `30,26,30`（纯黑在深色背景上会显得像一块阴影）。

## ⚠️ 观感前提（重要）
原图**大部分像素是低 alpha**（tall 中等亮度带的平均 alpha 只有 23~46，只有 3.5% 达 255）。
所以「黑色」在深色标题背景上主要表现成**把背景压暗的阴影**，而不是一块纯黑；
**真正「看得见」的形是那 6~8% 的金黄焰心**。这正是黑焰该有的烟熏感，但据此调参时要有预期。

## 影响面
`Fireball` 类**不是标题界面独占**：`WelcomeScene:240` 也 `new Fireball()`
⇒ **开场界面（冷启动那屏）的火把会一起变黑金**，两个界面共用同一套贴图。

## 备份与还原
原图备份在 `_chk/_bak_fireball/`（**故意不放 assets 里**，否则会被打进 APK）。
一键还原：`python _chk/verify_fireball.py --restore`。
`Fireball.java` 未进备份（它没被改），其 sha256 记在核验输出里。

## 未做 / 待人工验证
- **未启动游戏**，只做了源码级与字节级核验。
- 需**重新构建**才能在游戏里看到：`desktop/build/resources/main/effect…` 与
  `android/build/intermediates/**/merge*Assets/` 里仍是上一次构建的旧图。
- 观感（黑金比例是否合适、在深色背景上是否够「有形」）只能人工在游戏里判断；
  不满意可调 `--gold-quantile / --black / --gold-dim / --gold` 重跑（脚本幂等、有备份保护）。

# 2026-09-21 标题火把火焰改为「整体亮红色」（并回退此前的黑金方案）

## 需求
黑金方案效果不够好 ⇒ 先**回退到上游原版**，再把火焰改成整体亮红色。

## 回退（先做，且留了证据）
`python _chk/verify_fireball.py --restore` 从 `_chk/_bak_fireball/` 恢复两张原图，
恢复后 `git status` 对这两个文件**报「未修改」**（即逐字节回到上游原版），
且与备份 sha256 一致（`fireball-tall.png` `74cb3960…`、`fireball-short.png` `d46f5840…`）。

## 做法：新增 ramp（整条亮度色阶）模式
脚本原来只有 blackgold（阈值分段：大部分压黑 + 少数变金）一种形状，
这次加了 `--mode ramp`：**整条亮度色阶**，暗端 → 亮端贯穿全图、单一色相。
以后再换配色（红/蓝/紫…）直接给两端色即可，不用改代码。

```
L : 暗端 (96,6,8) → 亮端 (255,74,52)      --mode ramp --dark R,G,B --bright R,G,B [--gamma G]
```

暗端刻意**不是纯黑**：纯黑会让半透明外焰在深色背景上变成一团读不出颜色的黑影，
「整体亮红」就不成立。给成暗红后，全图（含最暗的 1/4）都仍明确是红色。

**⚠️ 中途发现并修掉的两个自身缺陷（都记下来免得重犯）**
1. **阈值判据不一致**：`apply_lut` 用 `int(亮度×255)` 截断后查表，而「金区/黑区」分区却用
   浮点亮度判 ⇒ 阈值附近几百个像素两边落在不同分支。表现是「只改了暗色参数，金色区却被动了几百个像素」。
   现已统一走**整数亮度索引**（`lum_index()`），红色方案下金区与暗色参数因此**严格无关**（已实测 0 像素）。
2. **核验脚本分区标签反了**：`pairs` 按降序排（算分位数需要），却又拿 `[:q]` 当「最暗」⇒
   「暗部仍是红」这条断言实际测的是**亮部**。已改为另取升序副本。

## 结果
| 贴图 | 整体均色 | 最暗 1/4 | 最亮 1/4 | 均 alpha | 不透明像素 |
|---|---|---|---|---|---|
| `fireball-tall.png` | (49,237,91) → **(198,50,36)** | (0,189,58) → (169,37,28) | (143,255,135) → (225,61,44) | 49 → 49 | 4588 → 4588 |
| `fireball-short.png` | (111,246,120) → **(215,57,41)** | (10,218,70) → (182,43,32) | (200,255,170) → (239,67,47) | 58 → 58 | 3500 → 3500 |

**alpha 逐像素完全不变**，可见占比与不透明像素数与改前完全相同 ⇒ 火焰形状/轮廓一点没动，只换了颜色。
体积：`88688 → 82970 B`（93.6%）、`49725 → 45473 B`（91.4%）。

## 参数与复现
```
python _chk/recolor_fireball.py --check  --mode ramp
python _chk/recolor_fireball.py --apply  --mode ramp [--dark R,G,B] [--bright R,G,B] [--gamma G]
python _chk/recolor_fireball.py --preview            # 原图/新图逐帧对照
python _chk/verify_fireball.py                       # 19 项核验（按 RECOLORED.txt 里的模式断言）
python _chk/verify_fireball.py --restore             # 一键还原原版
```
想更红更烈：`--bright 255,60,40 --gamma 1.4`（gamma>1 压暗中间调 ⇒ 暗部更沉、对比更强）。
想更亮更均匀：`--gamma 0.8`。

## 影响面
`Fireball` 类**不是标题界面独占**：`WelcomeScene:240` 也 `new Fireball()`
⇒ **开场界面的火把一起变红**，两个界面共用同一套贴图。横幅与滚动背景本次未动。

## 备份与还原
原图备份 `_chk/_bak_fireball/`（**故意不放 assets 里**，否则会被打进 APK）。
`python _chk/verify_fireball.py --restore` 一键还原。`Fireball.java` 未动（sha256 见核验输出）。

## 未做 / 待人工验证
- **未启动游戏**，只做源码级与字节级核验。
- 需**重新构建**才能在游戏里看到效果。
- 红色浓淡（是否够亮/够红）只能人工在游戏里判断；调参重跑即可（脚本幂等、有备份保护）。

# 2026-09-21 标题横幅改新版面（闪烁帧保留但置空）

## 需求
用户手工重排了 `interfaces/banners.png`：
```
移动端标题 (0,0)-(169,94)      169×94     （旧 139×100）
PC 端标题  (176,0)-(427,65)    251×65     （旧 240×57）
闪烁帧保留以供后续开发，但**置为空**
BOSS_SLAIN / GAME_OVER 保持不变
```
要求据此配置，以便检查视觉效果。

## 做法
只改 `effects/BannerSprites.java` 的 4 个 `uvRect`（本项目对该文件的第一处修改，7 增 4 删）：

| Type | 新 uvRect | 尺寸 | 说明 |
|---|---|---|---|
| `TITLE_PORT` | `(0, 0, 169, 94)` | 169×94 | 移动端标题 |
| `TITLE_LAND` | `(176, 0, 427, 65)` | 251×65 | PC 端标题 |
| `TITLE_GLOW_PORT` | `(253, 66, 422, 160)` | 169×94 | **空帧**（尺寸刻意与对应标题帧一致） |
| `TITLE_GLOW_LAND` | `(253, 66, 504, 131)` | 251×65 | **空帧** |

**空帧是怎么置空的**：不删枚举值、不动 `TitleScene:125` / `WelcomeScene:127` 的代码，
而是把 uvRect 指向图内一块**实测全透明**的区域（`(253,66)` 起，来自「最大空白矩形」算法算出的
259×190 空白区）。这样闪烁帧仍会参与 alpha 动画与 `Blending.setLightMode()`，只是画不出东西 ——
以后画好闪烁素材，把这两个 uvRect 改到真实位置即可。尺寸刻意取成**与对应标题帧相同**，
这样将来直接叠在标题上时定位天然对齐。

## 实测发现（两个都要你定夺）

### ① 横幅变宽 ⇒ 在常见机型上**会被裁**
`TitleScene:112` 是 `title.x = left + (w - 横幅宽)/2`，所以**相机宽 ≥ 横幅宽才不被裁**。
新版比旧版宽（竖 139→169、横 240→251），而各模式最小相机宽只有 135 / 240：

| 机型（估算 density） | 相机宽 | 竖屏横幅 169 |
|---|---|---|
| 1080×2400 @2.75 | ≈154 | ⚠️ 被裁 15 px |
| 1440×3200 @3.50 | ≈160 | ⚠️ 被裁 9 px |
| 720×1280 @2.00 | ≈144 | ⚠️ 被裁 25 px |

旧版 139 宽的横幅在这些机型上都放得下（144 > 139）⇒ **这是新版面引入的新问题**。
横屏同理：251 > 240，最小宽度下会被裁 11 px（常见 16:9 横屏相机更宽，一般不触发）。
要彻底避免，竖屏画面宽需 ≤ ~144（取上述最小相机宽的下界）。

### ② 两块标题画面的底边比帧多 1 行
实测画面包围盒：移动端 y=0..94、PC 端 y=0..65，而帧只到 93 / 64；那一行是**满宽且 alpha 255**。
按用户给的原值配置（原值即规格），若要补上就把 y1 各 +1：
`TITLE_PORT (0,0,169,95)`、`TITLE_LAND (176,0,427,66)`。

## 核验与预览工具
```
python _chk/analyze_banners.py            # 帧内容包围盒 / 逐边 1px 切断检测 / 最大空白矩形
python _chk/verify_banners.py             # 18 项：矩形值、空帧必须真透明、BOSS/GAME_OVER 未误伤、适配报告
python _chk/render_title_preview.py --devices   # 渲染模拟标题界面预览图（供人工看效果）
```
`verify_banners.py` 的关键断言是 **「空帧指向的区域在图内必须全透明」** ——
这是「空帧」这个约定的全部依据；素材一旦挪动导致那块不再为空，空帧就会画出错位的碎片，
而那种错误在真机上表现为「标题旁边多了几片莫名其妙的图案」，很难反查。

## 核验结果（18 项全通过）
矩形值与约定逐项一致；两个空帧区域实测 **0 个非透明像素**；
两个标题帧内分别有 8308 / 8668 个非透明像素；`BOSS_SLAIN` `(0,157,127,225)` 与
`GAME_OVER` `(128,157,256,192)` **保持上游原值**且区域内仍有画面（3861 / 2830 像素）；
六个矩形全部在图内；`BannerSprites.java` 合法 UTF-8；备份存在；闪烁帧仍有两个消费方。

## 备份
`_chk/_bak_title_assets/banners.png` —— 这是**手工资产且没有 git 版本可回退**
（git 里的 HEAD 版本是 mod 早先替换过的自定义图，不是本次手工改的版本），所以必须备份。

## 未做 / 待人工验证
- **未启动游戏**，只做源码级与字节级核验。
- 预览图 `_chk/_title_preview_*.png` 是**模拟**：横幅裁切与按钮坐标精确，
  但背景是**静态近似**（真机是 6 层视差滚动），按钮里的中文无法渲染（用编号 + 图例代替）。
- 需**重新构建**才能在游戏里看到。

# 2026-09-22 新增三阶武器：次元撕裂者（DimensionalRipper）——「转移」+「空间撕裂」窗口

## 需求
- 三阶武器，**基础数值整套照抄三阶原版弯刀（`Scimitar`）**。
- 贴图 `xy(6, 41)`（用户已绘）。
- 特殊效果：自带「转移」——命中时有概率**随机传送目标或自己**；任一方被传送后的 **3 回合内**可用
  「空间撕裂」瞬移到目标身旁并追加一次攻击。

## 关键决策（用户 2026-09-22 拍板）

| 项 | 取值 | 说明 / 原版对照 |
|---|---|---|
| 触发概率 | **+0 = 1/8，随强化等级趋近 1/2** | 原版两条诅咒都是**定值**（转移 1/12、定相 1/20）；本武器是唯一会随等级成长的，见文末「续」 |
| 传送对象 | **50% 目标 / 50% 自己** | 单次判定只传一个，**不做「传不动就换另一方」的回退**（回退会破坏 50/50） |
| 开窗条件 | **只有这把武器自己造成的传送**才开窗 | 别的传送（传送卷轴等）不开 |
| 窗口目标 | **锁定触发那一次被攻击的敌人** | 也是「空间撕裂」唯一的落点依据 |
| 是否真诅咒 | **否** | 不碰 `enchantment` ⇒ 可正常卸下 / 正常附魔，解咒洗不掉转移 |
| 掉落 | 入 `WEP_T3`，prob = 2 | 与同阶原版武器同权 |

## 落点

| 文件 | 改动 |
|---|---|
| `items/weapon/melee/DimensionalRipper.java` | **新增**。`extends Scimitar`（面板整套继承）；`proc()` 覆写转移；`warp()` 静态工具；`equipped(Hero)` 查主/副手；`doUnequip` 收尾关窗 |
| `actors/buffs/RiftWindow.java` | **新增**。`Buff implements ActionIndicator.Action`，占**副槽**；自管 3 回合窗口 + 按钮 |
| `sprites/ItemSpriteSheet.java` | `DIMENSIONAL_RIPPER = xy(6,41)` + `assignItemRect(..., 13, 15)` |
| `items/Generator.java` | `WEP_T3.classes` 追加 `DimensionalRipper.class`；`defaultProbs` 同步补 1 个 `2`（**12/12 等长**） |
| `items_zh` / `items` / `actors_zh` / `actors` 四份 properties | 8 键 × 2 语言 |

## 实现要点

### 一、「转移」走 `Weapon.proc`

```java
public int proc(Char attacker, Char defender, int damage) {
    damage = super.proc(attacker, defender, damage);   // ① 先走完原版附魔 / 护盾结算
    if (attacker.buff(MagicImmune.class) == null && defender.isAlive() && damage > 0
            && Random.Float() < procChance(buffedLvl()) * Weapon.Enchantment.genericProcChanceMultiplier(attacker)) {
        ...
```

三个触发前判定照抄原版 `Displacing`：魔法免疫跳过 / 目标存活 / **真造成了伤害**。
概率走唯一的 `procChance(buffedLvl())` 入口（等级成长见文末「续」），再乘
`genericProcChanceMultiplier`（奥术之环、狂暴等加成同样生效）。

### 二、`warp()` 的四道前置

IMMOVABLE（雕像 / 炮台）→ `isImmune(...)` → `ScrollOfTeleportation.teleportChar`（原版随机落点，自带特效）
→ 被传走的怪物 `state` 从 `HUNTING` 退回 `WANDERING`（否则下一回合直线跑回来＝等于没传）。

> ⚠️ `HUNTING` / `WANDERING` 是**每个怪物各自一份的 `AiState` 实例字段**，只能经实例比较
> （`mob.state == mob.HUNTING`）；写成 `Mob.HUNTING` 编译不过。

### 三、「空间撕裂」窗口（`RiftWindow`）

- **时长**：`WINDOW_TURNS = 3`，语义＝**触发当回合（余下的行动）+ 其后 2 个回合**。
  靠 `Buff` 自己按回合记账（`act()` 里 `turnsLeft--` → `spend(TICK)`）。
- **`timeToNow()` 才是关键那行**：`Buff.affect → append → attachTo → Char.add(buff) → Actor.add(buff)`
  （`actor.time += now`）⇒ 新挂载的 buff 本就调度在 `now`、当回合即 `act`，所以对新挂载它是**空操作**；
  真正起作用的是**重复触发**——此时 buff 已存在、`time` 已被 `spend(TICK)` 推到 `now+1`，
  不拉回来就白送一格（窗口变 4 回合）。
  （**第一版 javadoc 写成「少了就多送一回合」，对新建场景不准确，已修正。**）
- **占副槽**：`ActionIndicator.setSecondAction`（槽位 1，配 `SPDAction.TAG_ACTION_2`、默认不绑键），
  与主槽的职业按钮**并存**；每回合经 `keepSlot()` 重占（可能被封印之剑的 `SwordSwap` 挤掉）；
  `detach()` 里 `clearAction(this)` 只清自己。
- **三个失效判据**（`stillValid()`）：武器仍在手上（主 / 副手都查）+ 没换层 + 目标还活着。
  `attachTo` 也按它把关 ⇒ 读档恢复出来的空窗口不会挂出假按钮。
- **落点用 `appear` 而非 `teleportToLocation`**：后者要过一遍可达性判定，而这里要的正是**无视距离**
  （本作 `ArtTechniques` 的「即刻解剖」同一条路）。
- **一次性**：`doAction()` 末尾 `detach()` ⇒ 用掉即关窗。顺序是 `hero.busy()` → `appear` →
  `hero.sprite.attack(...)`，回调里 `hero.attack(victim, 1f, 0f, Char.INFINITE_ACCURACY)` +
  `hero.spendAndNext(hero.attackDelay())`（照 `ArtTechniques.DISSECT`）。
- **刻意不实现存档**：窗口锁定的 `Char` 引用无法随存档搬运 ⇒ 不重写
  `storeInBundle` / `restoreFromBundle`，读档后 `enemy` 为 null、`turnsLeft` 为 0，第一次 `act()` 自行关闭。

### 四、已知代价（有意保留）

副槽目前只有两个候补：本窗口与封印之剑的 `SwordSwap`（常驻占用）。
同时满足「一手次元撕裂者 + 一手封印之剑」且窗口恰好开着时，两者会争同一个槽位
（需同时持有两件终局级物品，极罕见）；本 buff 每回合重占，最坏只是按钮偶尔闪一下，不影响判定本身。

## 核验（未跑 Gradle）

- 单文件 `javac -proc:none -Xlint:all`（4 个文件）：**EXIT=0、0 错误**；
  `Generator.java` 的 6 条告警（327 / 339 / 749 / 751 / 964 行）全落在**未改动的上游裸泛型 `Class` 用法**上。
- `check_utf8_all.py` 1451 文件 OK；`check_unused_imports.py` ALL PASS。
- **新增 `_chk/verify_dimensional_ripper.py`：66 条断言 + 10 条反例自测 ALL PASS**
  （A 数值照抄弯刀 / B 转移 proc / C 窗口 / D 接线 / E 文本）。含两条跨文件一致性判据：
  **文本里的「1/8」「3 回合」必须与源码常量一致**（防改代码不改文本）。
- 全量回归：既有 20 个核验脚本重跑，均 ALL PASS。

### 核验过程本身踩的 2 个坑（已写进 skill）

1. **同一文件多处编辑必须串行**：本轮一次性并发发出 4 条同文件 Edit，只有 1 条真正落盘、
   另 3 条**静默丢失**，而工具每次都报「成功」——这是本项目第 4 次踩这个坑。
2. **「全局行数差」类结构断言必被后续改动打破**：`_chk/verify_wandmaker_intro.py` 原断言
   「相对改动前副本恰好 −4 行」，被本轮给 `actors*.properties` 各加 3 行（1 行注释 + 2 个新键）打破
   （实得 −1）。已改为**按键名判**：值可以改、**键不许整条消失**，另把 6 条按职业旧键
   钉成「必须彻底移除」，既抗后续改动、又仍能抓出误删。

## 未做 / 待人工验证

- **未启动游戏**，只做源码级与字节级核验。需用户 `:core:compileJava`（或打包）后：
  F2 → WndDebug「武器」标签生成「次元撕裂者」→ 装备并攻击，确认
  ① 是否随机传送（目标 / 自己）；② 右下角**副槽**是否出现「空间撕裂」按钮（带剩余回合数）；
  ③ 按下是否真的瞬移到目标身旁并追加一击。
- 未验证与另一个副槽使用者（封印之剑）的共存表现（理论分析见上）。

# 2026-09-22 次元撕裂者（续）：命中与传送的粒子特效（RiftParticle）

## 需求
- 给次元撕裂者加「**命中时**」与「**传送时**」两处粒子特效。
- 配色**从白到深紫共六种**，且要取自武器贴图本身——忽略贴图里的半透明黑与棕色部分，
  应当**恰好剩 6 种色调**。

## 取色：直接从 `items.png` 那一格采
`xy(6,41)` 那格共 **9 种颜色**，按上面的规则去掉 3 种后正好 6 色：

| # | 色值 | 角色 |
|---|---|---|
| 1 | `#FFFFFF` | 纯白高光 |
| 2 | `#F1B8FC` | 淡紫白 |
| 3 | `#D887F0` | 亮兰紫（刀身主色） |
| 4 | `#AE52DD` | 中紫 |
| 5 | `#6A42BA` | 深紫 |
| 6 | `#4638A8` | 靛紫（最深） |

被排除的 3 种：`#000000`（alpha＝102，像素画外描边）、`#2E0301` 与 `#561F20`（刀柄棕色暗部）。

**可复算的判据**（核验脚本就按这条跑）：六种紫都满足 `B ≥ R`，两处棕色都是 `R > B`，
描边则是 `alpha < 255` —— 三条判据正好把 9 切成 **6 + 1 + 2**。

## 落点

| 文件 | 改动 |
|---|---|
| `effects/particles/RiftParticle.java` | **新增**。`extends PixelParticle`；`COLORS` 六色；三个 Factory |
| `items/weapon/melee/DimensionalRipper.java` | 命中粒子（`proc`）、传送粒子（`warp`）、公共入口 `teleportFx`；**撤掉原来的 `Speck` 白色光点** |
| `actors/buffs/RiftWindow.java` | `doAction` 里**先记出发点**，`appear` 之后调 `DimensionalRipper.teleportFx` |

## 三个 Factory = 一套「传送」的视觉词汇

| Factory | 挂在哪 | 行为 |
|---|---|---|
| `HIT` | 命中（`proc`，`damage > 0`） | 贴着目标向四周炸开一小蓬，射程近 |
| `SCATTER` | 传送**起点** | 向外炸散、更远更快 ⇒ 读作「人被打散」 |
| `IMPLODE` | 传送**终点** | 从四周围拢回中心 ⇒ 读作「人重新拼起来」 |

三者都开 `lightMode()`（叠加混合）——紫色在加色模式下才有「裂隙发光」的味道。

## 三个实现要点

1. **按发射序号循环取色**：`base = index % COLORS.length`。`Emitter` 每次 `start` 都把发射计数
   从 0 重数，所以一蓬 8 颗会按 白 → 淡紫 → … → 靛紫 **轮着来**；**只要喷够 6 颗，六色必定全出场**。
   比随机取色稳——随机有一整蓬全是深紫的可能。
2. **不插值**：全程只 `color(COLORS[...])`，代码里没有 `ColorMath`。插值会混出贴图之外的中间色，
   就不再是「这六种」了。观感上的「冷却」靠**整档下沉**：`update()` 里 `am < 0.5` 时色阶 +1 档。
3. **IMPLODE 的速度＝位移 ÷ 存活期**：`speed.set(cx-x, cy-y); speed.scale(1f/lifespan);`
   ⇒ 粒子**正好**在寿终那一帧抵达落点，不会冲过头再从中心飞出去。
   因此终点用的是 `CellEmitter.center(pos)`（汇聚到格心**一个点**），而不是 `get`（撒满整格）。
4. **粒子数取 ≥ 6**（命中 6、传送 8）：既然按序号循环取色，喷满 6 颗才把六色各走一遍。
   初版命中只喷 4 颗，离线模拟一跑就发现**最深两档紫永远看不到**（4 颗只覆盖 5 色），故改成 6。
   核验脚本据此断言 `HIT_PARTICLES ≥ 6` **且** `WARP_PARTICLES ≥ 6`。

## 顺带补的原版缺口
`ScrollOfTeleportation.appear()` 只在**非英雄**传送时往起点撒 3 颗白色光点
（`if (heroFOV[ch.pos] && ch != Dungeon.hero)`），**终点一颗都不给**。
于是英雄自己传送时「起点没有、终点也没有」；`teleportFx` 两端都管，英雄的传送终于有了像样的出入场。

## 核验

- 单文件 `javac -Xlint:all`（RiftParticle / DimensionalRipper / RiftWindow）：**EXIT=0、0 告警**。
- `_chk/verify_dimensional_ripper.py` 扩到 **108 条断言 + 17 条反例自测，ALL PASS**。
  新增 F 组的硬判据：**与 `items.png` 逐色比对**、该格 9 色构成、明度从白到深紫单调、
  `index % 6` 循环取色、不得插值、IMPLODE 抵达判据、三处挂点的顺序（`damage>0` 在 `hitFx` 前、
  `teleportChar` 在 `teleportFx` 前、记出发点在 `appear` 前）。
- 全量回归：26 个脚本重跑，除**两条与本次无关的既有失败**（旧 APK 的 md5 比对、地形浏览器陈旧阈值）外全 OK。
- **离线预览**（临时工具 `_chk/_rift_fx_preview.py`，`_` 前缀未入库）：从 `RiftParticle.java` **解析**
  `COLORS`（唯一权威来源，不复制常量），按同一套公式积分粒子轨迹，画成
  `_chk/_out/rift_fx_preview.png`（贴图格放大 ×12 ＋ 6 个色块 ＋ 三种 Factory 在 t=0.15/0.30s 的两帧）。
  同时打印两条数值自检作硬证据：**六色覆盖 6/6**、**IMPLODE 寿终残差 = 0.000000**（确实落在中心）。

### 本轮核验脚本自身踩的 2 个坑（已写进 skill）

1. **反例文案是「先求值」再传进断言的**：`"#%02X%02X%02X" % 0xFFFFFF` 直接抛 `TypeError`，
   异常发生在 `check()` **之前** ⇒ **断言本身被掩盖**，看起来像脚本崩了而不是判据失败。
   已统一走 `hexa()`（int / 元组两种形态都吃）。
2. **`(r,g,b)` 元组与 `0xRRGGBB` 比集合**：`set([(255,255,255)]) == set([0xFFFFFF])` 是 **False**，
   而两边打印出来都是 `#FFFFFF` ⇒ 判据**静默假红**（报「缺 6 色 / 多 6 色」，看着像全错）。
   已加 `pack()` 统一归一化；两条都补了反例自测。

## 待人工验证

- 需 `:core:compileJava` 后进游戏看三件事：① 每次命中的紫色小爆开；
  ② 传送时起点炸散 + 终点汇聚（英雄自己传送也应有）；③「空间撕裂」瞬移同样有出入场粒子。


# 2026-09-22 次元撕裂者（续二）：转移概率随强化等级成长

## 需求
- 把「转移」触发概率从**定值 1/8** 改成**随强化等级提升**：`+0` 时为 1/8，随等级**趋向 1/2**。

## 公式与取值
```java
public static final float PROC_CHANCE_BASE  = 1/8f;   // +0
public static final float PROC_CHANCE_MAX   = 1/2f;   // 渐近上限（永不到达）
public static final float PROC_CHANCE_DECAY = 7/8f;   // 每级吃掉 1/8 的差距

public static float procChance(int lvl) {
    float gap = (PROC_CHANCE_MAX - PROC_CHANCE_BASE) * (float) Math.pow(PROC_CHANCE_DECAY, lvl);
    return Math.max(0f, PROC_CHANCE_MAX - gap);
}
```

读作「**与上限的差距每升一级乘 7/8**」，即 `p = 1/2 − 3/8 × (7/8)^L`。

| 等级 | +0 | +1 | +3 | +6 | +10 | +15 | +20 | +30 | +60 |
|---|---|---|---|---|---|---|---|---|---|
| 概率 | 12.5% | 17.2% | 24.9% | 33.2% | 40.1% | 44.9% | 47.4% | 49.3% | 49.99% |

逐级增幅 **4.69 → 4.10 → 3.59 → …** 个百分点，**单调递减**。

## 为什么用指数而不是线性
- **低强化的每一级最值钱**（+0→+1 一下多 4.7 个百分点），正反馈鼓励前期强化；
- 高强化自然**饱和**，这才叫「趋向 1/2」；线性写成 `1/8 + 3/8×(L/10)` 会在 +10 处硬顶到 1/2，
  之后强化毫无收益，「趋向」的语义也没了；
- 1/2 是**渐近线**：`+100` 也只有 49.99994%，**永远达不到**（核验脚本按这条硬断言）。

## 落点

| 文件 | 改动 |
|---|---|
| `items/weapon/melee/DimensionalRipper.java` | 常量 `PROC_CHANCE` → `PROC_CHANCE_BASE` / `_MAX` / `_DECAY` + 唯一入口 `procChance(int)`；`proc()` 改喂 `procChance(buffedLvl())` |
| `items/weapon/melee/DimensionalRipper.java` | **覆写 `statsInfo()`**：把当前概率算出来填进 `stats_desc`（否则面板永远显示写死的 +0 值） |
| `items_zh` / `items` | `stats_desc` 加 `%d%%` 占位符 + 成长说明（起点值 / 上限值仍写明，与常量联动校验） |

## 两个要点

1. **等级一律取 `buffedLvl()`**，与 `BattleAxe`、`DaCapo`、`BlackSwan` 等原版武器「按等级算数值」
   的惯例一致（`Item.buffedLvl()` 还会额外吃掉 `Degrade` 的减级）。负等级（被诅咒的武器）
   会让差距大于 3/8 ⇒ 概率**低于** 1/8，由 `Math.max(0f, …)` 兜底截到 0。
2. **面板文本必须动态算**：本版本武器的文本入口是 `MeleeWeapon.statsInfo()`，默认实现是
   `Messages.get(this, "stats_desc")` —— **不覆写就永远显示文本里写死的那个数**，强化了也看不出概率在涨。
   照 `BlackSwan` 的范式覆写并传参。⚠️ `stats_desc` 里的字面百分号必须写成 `%%`，
   否则 `String.format` 抛异常、`Messages.format` 兜底**整串回退成资源键名**（面板直接显示一串键名）。
   显示口径用 `Math.round`：`+0` 的 12.5% 显示成 **13%**（截断会显示 12%）。

## 核验

- 单文件 `javac -Xlint:all`（DimensionalRipper / RiftParticle / RiftWindow）：**EXIT=0、0 告警**。
- `_chk/verify_dimensional_ripper.py` 扩到 **130 条断言 + 22 条反例自测，ALL PASS**。
  新增 G 组：**按源码三常量复算整条曲线** —— `+0` 恰为 1/8、`+0→+30` 严格递增、任何等级 `< 1/2`、
  增幅单调递减、`+60/+100` 贴近 1/2、四个实用等级落在设计区间、面板用 `Math.round`。
  B 组加「旧裸常量 `PROC_CHANCE` 已彻底退场」与「喂的是 `buffedLvl()`」两条；
  E 组加「`%d%%` 占位符存在」与「覆写 `statsInfo()` 并传参」两条。
- **`jshell` 实证格式化**：`String.format(Locale.CHINA, 文本, 12)` → `_12%_`、传 40 → `_40%_`，
  确认**没有**走「整串回退」的兜底分支。
- 全量回归：27 个脚本重跑，除**两条与本次无关的既有失败**（旧 APK 的 md5 比对、地形浏览器陈旧阈值）外全 OK。

## 待人工验证
- `:core:compileJava` 后打开武器面板，确认描述里的百分比**随强化等级变化**
  （+0 显示 13%、+10 显示 40%、+15 显示 45%）；再升几级，确认数字同步上涨。
- 手感上确认高强化时触发明显更频繁（+10 ≈ 40% 意味着**约每 2~3 次命中**就传送一次）。
# 2026-09-23 封印之剑系列「不可出售」+ 场景特效坐标速查

## 一、需求与根因

需求：封印之剑系列（封印之剑 / 一阶段 / 二阶段 / 莱瓦汀）目前能在商店被卖掉，需要禁止。

根因是两层，缺一不可：

1. `Item.sellable()` 的默认规则＝`!unique || stackable`，而本系列**没有**标 `unique`
   （它是普通六阶武器，标了会顺带被禁掉锻造/附魔等一系列东西）⇒ 默认就是可售的。
2. `doUnequip()` 那层「锁死在双手」的保护**挡不住出售** —— 商店的收购窗口是 `WndBag`，
   它在布局里把 `belongings.weapon`（主手）与 `belongings.secondWep`（副手）**一起** `placeItem`，
   所以剑只要挂在副手，就会出现在收购列表里、被当成普通六阶武器换钱。

## 二、改动（1 个文件）

`items/weapon/melee/SealedSwordBase.java` 新增覆写：

```java
@Override
public boolean sellable() {
    return false;
}
```

- 覆写的是**谓词**，不是去加 `unique` —— `Item` 的注释已把这条约定写死：
  「要单独放开/关掉某一项出口，就覆写 `sellable()` / `transmutable()`，**别动** `unique`」。
- 放基类一处即覆盖四个形态，与 `isUpgradable()` 同一个理由（同一件事写四遍迟早会漏一个）。
- `Shopkeeper.canSell` 是全仓**唯一**消费 `sellable()` 的地方（实测 1 处），所以这一处即封锁整条出售路径。

## 三、封印之剑场景特效坐标速查（滤镜 / 火焰粒子 / 屏幕扭曲）

> 设计与历史见本文件「2026-09-14（补2）」（滤镜）与「（补3）」（减弱 + 四周火焰 + 热浪扭曲）。
> 下表是**按行号定位的调参入口**，只想微调观感时照这张表改即可；行号会随编辑漂移，动手前用 `grep -n` 复核。

### ① 温度滤镜（屏温色罩）— `core/src/main/java/.../effects/HeatVignette.java`

| 行 | 成员 | 作用 / 怎么调 |
|---|---|---|
| 73 | `TEXTURE_KEY` | 渐变贴图的全局缓存键（全局只一张，颜色靠染色） |
| 76 | `TEXTURE_SIZE` | 渐变分辨率 64×64，仅一次性生成 |
| 79 | `INNER_RADIUS` | 中心留白圈半径，越大中央越清透 |
| 82 | `CENTER_FLOOR` | 中心底噪（中心仍留的一丁点色度） |
| 89 | `HEAT_COLORS` | **色罩颜色**，下标＝形态序号 0封印/1黄/2橙/3红 |
| 92 | `FLAME_COLORS` | **火焰粒子颜色**，比色罩更亮更饱和 |
| 95 | `HEAT_ALPHA` | **边缘不透明度** 0 / 0.20 / 0.28 / 0.36 |
| 98 | `HEAT_WARP` | **边缘最大像素偏移** 0 / 0.9 / 1.4 / 2.0 |
| 101 | `WARP_EDGE_BIAS` | 扭曲的近中心衰减：0＝全屏一致、1＝只在边缘明显 |
| 104 | `WAVE_PERIOD` | 热浪循环周期 1.2s（shader 里时间系数只能取整数，保证回绕连续） |
| 109 | `FADE_TIME` | 冷热切换的平滑时长 0.35s |
| 112 | `MIN_HEAT` | 低于它视为无特效 |
| 123-139 | 构造函数 | 用 `uiCamera`、贴合屏幕、初始直接对齐（避免换层闪淡入） |
| 141-157 | `update()` | 每帧指数逼近 `targetHeat()`（所以换形态是渐变而非硬切） |
| 160-164 | `draw()` | **第一行关掉扭曲** ⇒ 世界层终点 / UI 起点 |
| 179-201 | `heat()` / `flameColor()` / `targetHeat()` | 对外接口；`targetHeat()` 读手持剑的 `stage()`，受设置开关约束 |
| 209-218 | `applyHeat()` | 把热度换算成颜色 + alpha 套到本 Image 上 |
| 230-241 | `beginWorldPass()` | **推扭曲参数**（由 `GameScene.draw()` 第一句调用） |
| 258-275 | `sample()` / `lerpColor()` | 色板与强度表的线性插值 |
| 301-328 | `makeTexture()` | **生成 64×64 白色径向渐变**（`smoothstep` 从中心 0 升到边缘 1） |

### ② 屏幕四周火焰（火苗）— `core/src/main/java/.../effects/HeatFlames.java`

| 行 | 成员 | 作用 / 怎么调 |
|---|---|---|
| 53 | `MIN_INTENSITY` | 低于它一条火线都不点 |
| 56 | `RISE_FRACTION` | 火苗漂出距离占屏幕高的比例 18% |
| 59 | `BASE_LIFE` | 基准寿命 0.9s |
| 62-63 | `BASE_SIZE` / `SIZE_GAIN` | 粒子基准边长 3.2px、随强度增长 2.4px |
| 66-90 | `Line` 内部类 | 一条火线：归一化位置/范围 + 漂移方向 + 缩放 + 基准间隔 |
| 99-114 | 构造函数 | **布局**：底边 4 段最盛、左右各 2 段、顶边 3 段，共 11 条火线 |
| 117-127 | `line(...)` | 建一条火线（`autoKill=false`、`camera=uiCamera`） |
| 133-149 | `factoryFor(...)` | 每条线一个匿名工厂：方向固化、颜色/速度在 `emit()` 那一刻现读、`lightMode()=true` |
| 152-191 | `update()` | 每帧按热度刷新颜色/大小/速度与各线发射间隔；**160 行**是 `pow(heat/3, 1.4)` 强度曲线 |

### ③ 火焰粒子本体 — `core/src/main/java/.../effects/particles/HeatFlameParticle.java`

| 行 | 成员 | 作用 / 怎么调 |
|---|---|---|
| 36 | `extends PixelParticle.Shrinking` | 会一边飘一边缩小的像素方块 |
| 40 | `lifespan` 默认值 | 0.9s |
| 50-66 | `reset(...)` | 颜色/加速度/大小/寿命全由发射方喂；62 行加横向抖动防「栅栏感」 |
| 68-81 | `update()` | alpha 曲线：出生 15% 快速亮起 → 中段实心 → 末期 35% 淡出 |

### ④ 屏幕扭曲（热浪折射 shader）— `SPD-classes/src/main/java/com/watabou/noosa/NoosaScriptWarp.java`

| 行 | 成员 | 作用 / 怎么调 |
|---|---|---|
| 46 | `extends NoosaScript` | 只比默认脚本多一段「算抖动 → 偏移 vUV」，光照完全保留 |
| 49 | `enabled` | **「谁被扭曲」的唯一开关**，由核心侧每帧驱动 |
| 59-66 | 构造函数 | 取 4 个 uniform：时间 / 幅度 / 分辨率 / 边缘偏置 |
| 68-71 | `get()` | `Script.use(...)` 取全局唯一实例 |
| 80-84 | `resetCameraCache()` | `Game.render()` 每帧调用（不调则画面不再跟随镜头） |
| 96-102 | `upload(time, ampPixels, edgeBias)` | 上传一次扭曲参数，每帧一次即可 |
| 109-153 | `SHADER` | **波形本体**：顶点 112-123 输出 `vScreen`；片元 129-153，抖动公式在 **145-151**，`gl_FragColor` 在 152 |

### ⑤ 挂载与驱动

| 位置 | 内容 |
|---|---|
| `scenes/GameScene.java:54-55` | import 两个特效类 |
| `scenes/GameScene.java:207` / `210` | 成员 `heatVignette` / `heatFlames` |
| `scenes/GameScene.java:386-391` | `add()`：滤镜在前、火苗紧随其后；**都在世界层最后、所有 UI 之前** |
| `scenes/GameScene.java:817-820` | 覆写 `draw()`：**第一句** `HeatVignette.beginWorldPass()` |
| `noosa/NoosaScript.java:187-188`、`NoosaScriptNoLighting.java:41-42` | `enabled` 为真时 `get()` 返回扭曲脚本 |
| `noosa/Game.java:165` | 每帧 `NoosaScriptWarp.resetCameraCache()` |
| `SPDSettings.java:298-306` | `KEY_LAEVATEIN_FX="laevateinn_fx"` + getter/setter（默认 true） |
| `windows/WndSettings.java:230` / `318-326` / `367-368` | 显示页复选框 `chkHeatFx`「莱瓦汀场景特效」 |
| `windows_zh.properties:322` / `windows.properties:322` | 该开关的文本键 |
| `items/weapon/melee/Laevateinn.java:103-130` | **另一套「世界内火场」**（`burnAura`：每回合给自身与 5x5 圆形内的地块 `Blob.seed(Fire)`）——那是地图上真实燃烧的火，不属于屏幕特效；它的每回合心跳借 `SealedSwordBase.SwordSwap.act()` |

## 四、核验

- 新增 `_chk/verify_sealed_sellable.py`：**35 条断言 + 3 组反例自测 ALL PASS**。
  反例覆盖三条「看起来也行但其实是错的」写法：① 方法体改回 `return true`；② 整段删掉覆写
  （改回继承 `Item` 默认值）；③ 拿 `unique = true` 当杠杆 —— 三条都被判据抓出。
- **字节码级实证**（`javap -c`）：新编译的 `SealedSwordBase.sellable()` 是 `iconst_0; ireturn`
  （＝返回 false）；四个形态类**均无**自己的 `sellable()`（继承基类的 false）；
  对照组 `Item.sellable()` 仍是 `!unique || stackable`；`TearSwordBlessing` / `Admiration`
  仍返回 `true`（未被误伤）。
- 单文件 `javac -proc:none -Xlint:all`（SealedSwordBase + HeatVignette）**EXIT=0、0 错误**；
  3 条告警全部落在**未改动行**（`SealedSwordBase:100` 的 `new Class[]` rawtypes、
  `HeatVignette:138/215` 的 `this-escape`），均属改动前既有。
- 顺手修掉一处**失效的 javadoc 链接**：`HeatVignette` 类注释里写着 `{@link #pushWarp()}`，
  而该方法早已改名，改为 `{@link #beginWorldPass()}`。
- `check_utf8_all.py` 1452 文件 OK；改动文件换行符（两处均 LF）与括号配平未破坏。

## 五、待人工验证

① 商店收购列表里（含副手那把）不再出现封印之剑系列任何形态；
② 同一界面里普通武器、以及「泪剑祝福 / 爱慕」这类**刻意放开出售**的神器照旧可卖；
③ 解封切换形态后（一阶段/二阶段/莱瓦汀）同样不可售。

# 2026-09-23 「考验」系统（TRIALS）——与挑战平行的独立位掩码

## 1. 定位与约定

- **考验＝开局可选修饰符**，与挑战**完全独立、可同时开**：自己的位段、自己的设置键、自己的窗口、
  自己的存档字段，**任何一方都不读也不写对方的位掩码**。某条考验若与某条挑战效果重合，
  就在**同一个散点**写 `Dungeon.isChallenged(X) || Dungeon.isTrialled(Y)`；**绝不让考验去 set 挑战的位**
  （否则 `WndChallenges` 会把考验显示成挑战、`activeChallenges()` 误计数、`1.25^n` 倍率误叠加）。
- 十条考验＝卡巴拉之树十质点，**顺序＝树上从上到下＝选择界面显示顺序**：
  `KETER / HOKMA / BINAH / CHESED / GEBURA / TIPHERETH / NETZACH / HOD / YESOD / MALKUTH`。
- **骨架先行**：数据 + 界面 + 存档三层已通。**具体效果逐条实现** —— 目前 **HOKMA（序号 1）**、
  **BINAH（序号 2）**、**CHESED（序号 3）**、**GEBURA（序号 4）** 已实现（分别见下文 §6 / §7 / §8），
  其余六条仍是**占位**（`trials.<id>_desc` 为说明性文本，无代码效果）。
  写文本时名称刻意保持全大写（不走 `Messages.titleCase`）。
- **实现某条考验的约定**：不去 `set` 挑战的位、也不改挑战的代码；效果写成 `Trials` 上的静态钩子
  （如 `modifyMoveSpeed` / `modifyHeroMoveDelay` / `modifyHeroAttackDelay` / `modifyGeneratedItem` /
  `modifyMobHT` / `bindMobPassives` / `interceptLethalDamage`），
  由框架里的**汇聚点**调用，钩子内部第一行统一 `if (!Dungeon.isTrialled(该条))` 放行原值。
- **三类规则的区分（实现前先想清楚）**：**全局规则**（HOKMA）夹在「一切结算完之后」的唯一出口，
  用 `final` 封口让编译器保证没有第二个出口；**事件型规则**（BINAH、CHESED 的生命上限）只在某个
  **动作发生的那一刻**生效（生成、入场…），收口在触发该动作的唯一入口，且**不得改变随机流**；
  **挡路型规则**（GEBURA）则钉在**已有流程的某一行之间**，靠「位置」而不是「新入口」生效（见 §8）。
  三者都不要写成「逐条拦截所有来源」。

## 2. 接线清单

- **新文件**：`Trials.java`（逐项同构于 `Challenges.java`：位常量 / `NAME_IDS` / `MASKS` / `MAX_VALUE` /
  `MAX_TRIALS` / `activeTrials()` / 图标尺寸表 `ICON_FRAME`·`ICON_COLS`·`ICON_W`·`ICON_H`）、
  `windows/WndTrials.java`（**2026-09-25 起改为「生命之树」排版**，`WIDTH` 120 与 `WndChallenges` 齐平）、
  `windows/WndTrialInfo.java`（质点详情小窗，继承 `WndTitledMessage`）、
  `assets/interfaces/tree.png`。
- **存档四处**：① `SPDSettings`（`KEY_TRIALS="trials"` + `trials(int)/trials()`，范围 `0..Trials.MAX_VALUE`）；
  ② `Dungeon`（字段 `:189`／`init()` `:244`／`isTrialled()` `:304`／存档键 `:633` + 存 `:661` + 读 `:770`／
  `preview()` `:890`）；③ `GamesInProgress.Info.trials`；④ `Rankings`（`TRIALS` 键存/还原）。
  旧档/旧记录缺键时 `getInt` 返 0，**天然安全**。
- **界面七处**：选人界面按钮（`HeroSelectScene`，**刻意不套 `Badge.VICTORY` 门控**，方便随时进游戏验证）、
  局内左上计数（`MenuPane`，新计数占 `btnJournal.left()-21` 那一格 —— 挑战占 `-14`、深度占 `-7`）、
  局内菜单（`WndGame`）、存档槽详情（`WndGameInProgress`）、通关结算（`WndVictoryCongrats`）、
  排行详情第 6 页签（`WndRanking` 的 `pages[5]`/`icons[5]`）。
- **图标**：`Assets.Interfaces.TRIALS = "interfaces/tree.png"`；`Icons` 新增 `TRIAL_GREY / TRIAL_COLOR / TRIAL_COUNT`
  三个常量，**已接 `icons.png` 上的专属格**（2026-09-23 实拍）：
  | 常量 | `uvRectBySize(x, y, w, h)` | 实测内容 | 用途 |
  |---|---|---|---|
  | `TRIAL_GREY` | `208, 32, 16, 16` | 满格 16×16（灰绿「质点树」） | 选人界面按钮（未开考验时） |
  | `TRIAL_COLOR` | `224, 32, 16, 16` | 满格 16×16（金色同款树） | 选人按钮（已开）/ 局内菜单 / 结算页 / 排行页签 |
  | `TRIAL_COUNT` | `160, 88, 7, 7` | 5×6，落在格内 `+1,+1` | 局内左上「考验 N」计数 |
  其中 `160,88` 正好在 `CHAL_COUNT`（`160,80,7,7`）**正下方**，两者不重叠。
  ⚠️ **`TRIAL_COUNT` 必须与 `CHAL_COUNT` 同尺寸（7×7）**——原画是 8×8，导致计数数字比挑战的**低 1px**、
  图标间距从 7px 变 8px（详见 §4 的「改 rect 会连带改布局」②）。收成 7×7 后字形（`+1..+5 / +1..+6`）一像素不切。
  `208,32` / `224,32` 原本就是 `PASTE(192,32,13,13)` 与 `SHUFFLE(240,32,15,12)` 之间的两个**空格**。
  坐标语义 = `uvRectBySize(left, top, w, h)` ＝ `uvRect(left, top, left+w, top+h)`，**y 自图片顶部往下数**
  （`Image.frame(RectF)` 把 `width/height` 设成 `w/h` 的真实像素数 ⇒ **rect 直接决定这个 Image 的尺寸**）。
- **文本**：`misc_zh.properties` 与 `misc.properties` 加 `trials.<id>` / `trials.<id>_desc`（10×2）；
  `windows_zh.properties` 与 `windows.properties` 加 `windows.wndtrials.title` + `windows.wndgame.trials` +
  `windows.wndgameinprogress.trials` + `windows.wndvictorycongrats.trials`。
  窗口标题走 `Messages.get(this,"title")` ⇒ 键名是 **`windows.wndtrials.title`**。

## 3. 三个坑（都已踩/已修）

- **`WndRanking` 建页签的循环原本是 `break`**：`if (pages[i] == null) break;` ⇒
  **「只开了考验、没开挑战」时第 6 页签永远不会被建**（`pages[4]` 为 null 直接退出）。已改 `continue`。
- **`WndRanking` 的页签内容会被窗口相机裁掉**：内容高 > `HEIGHT(=144)` 的部分**看不见**（不是溢出到窗口外，
  是被裁）。挑战页签 12 条 × 16px ＝ 192 早已超出；考验页签因此用**行高 14、行间无间隔**（10×14＝140）才塞得进。
- **`WndTrials` 的名称不能走 `Messages.titleCase`**：`Messages` 只在 `lang == ENGLISH` 时才改大小写，
  于是英文下 `KETER` 会被压成 `Keter`。直接 `Messages.get(Trials.class, id)` 即可。

## 4. 美术资源

- `tree.png`：**160×48 ＝ 3 行 × 10 帧（每帧 16×16）**，列序＝`Trials.NAME_IDS`（KETER→MALKUTH）；
  **帧内图标的实际像素尺寸各不相同**（其余像素透明），画面起点恒为 `(0,0)`。
  三行＝三种状态（2026-09-25 用户口径）：**第 1 行彩色＝已开启**、**第 2 行浅灰＝尚未解锁**、
  **第 3 行深灰＝已解锁但未开启**（初始态）；判据收口在 `Trials.iconRow(index, enabled)`，
  「是否解锁」另留 `Trials.isUnlocked(index)`（**目前恒为 true**）。
  ⚠️ **三行包围盒逐格完全一致**（`_chk/verify_tree_rows_bbox.py` 实测 0/10 不一致）⇒ 尺寸表三行共用，
  换状态只改取样矩形的 y 偏移。
  **2026-09-24 十帧全部绘齐**，逐帧包围盒（0 基帧号）＝
  `13×16 / 15×13 / 15×14 / 15×14 / 13×16 / 15×15 / 15×15 / 15×14 / 15×15 / 15×16`。
  ⚠️ **2026-09-25 订正**：TIPHERETH（第 5 帧）右侧曾残了一道**极薄的不透明像素**，实测包围盒被算成
  16×15；用户擦掉后回填为 **15×15**，整树外包宽随之 **104.27 → 103.27**（左右余量 7.86 → 8.36）。
- **尺寸表必须与实测包围盒一致**：重画一帧后要回填 `Trials.ICON_W/ICON_H`
  —— 这两个数**就是** `Image.width()/height()`（`uvRectBySize(x,y,w,h)` ⇒ `frame.width()*texture.width`），
  写大了图标会带透明边、写小了直接被裁。用 `_chk/analyze_tree_png.py` 实测、
  `_chk/verify_tree_trials_icons.py` 双端核对（它解析 `Trials.java` 与 PNG；声明尺寸小于实测 ⇒ FAIL，
  声明大于实测 ⇒ 带透明边被居中推偏）。
  ⚠️ **源图一改就要重跑**：2026-09-25 擦掉 TIPHERETH 那道像素后，该脚本当场报「帧 5：声明 16x15 ≠ 实测 15x15」。
  该脚本自身还藏过一条**过期判据**（整图高按「单行 16」写死，而 tree.png 2026-09-24 起就是 3 行 48 高）⇒
  永久报红 1 条，已修成 `h == ICON_ROWS * ICON_FRAME`。**陈旧判据会把真回归埋掉，见到就修，别习惯性忽略**。
  ⚠️ 「`WndTrials` 里图标在 16×16 槽内**居中**绘制」是**旧逐条列表版**（2026-09-25 前）的做法，
  已随列表一起退役；现在按**包围盒取帧 + 只改显示尺寸**，见 §4 的「生命之树」一节。
- **已绘制**（2026-09-23）：`icons.png` 上的三枚专属格 —— 见 §2 图标表的坐标与实测尺寸。
  ⚠️ **改 rect 会连带改布局**：`rect` 的 `w/h` 就是 `Image.width()/height()`，凡是按「图标自然尺寸」排版的地方
  都会跟着动。本次实测到的三处：
  ① `HeroSelectScene` 的选项按钮**高度硬编码 16**（`btn.setRect(..., 16)`）⇒ 16×16 的树**上下贴边**
  （挑战图标 12 高、留 2px），且 `leftJustify` 下 `text.x = icon.x + icon.width() + 1` ⇒ 标签比「挑战」那行**右移 1px**；
  ② `MenuPane` 的计数文字用 `trialsIcon.y + trialsIcon.height()`、横向按 `(icon.width() - text.width())/2` 居中
  ⇒ 框高/框宽差 1px 就等于数字**低 1px**（横向还会因 `align()` 取整把 7px 间距变成 8px）。
  **2026-09-23 已按此修**：`TRIAL_COUNT` 由 `160,88,8,8` 收成 `160,88,7,7`（字形 `+1..+5 / +1..+6` 一像素不切），
  §2 里对应的 `Icons.java` 注释、本表与 `_chk/verify_trial_icons.py` 的期望值同步更新；
  ③ `WndVictoryCongrats` 的行高 `max(icon, text)`、`WndGame`(`BTN_HEIGHT=20`)/`WndGameInProgress`(18)、
  `WndRanking`(`tabHeight=25`) 都在容差内，无副作用。
  ⚠️ **铁律**：凡是要与别的图标**并排或上下对齐**的计数类图标，rect 尺寸必须与参照图标**逐像素同规格**。
  `icons.png` 上把格子画大一格看着无害，但 `Image.frame(RectF)` 会把它变成 `Image.width()/height()`，
  再经布局公式传导成可见的错位。改尺寸只有两条正路：**改 rect 让两枚同规格**，或**改 usage 让布局不依赖图标尺寸**；
  **别指望 `icon.scale` 能救布局耦合**：`Visual.width()` = `width × scale.x`，布局若在设 scale **之前**
  读过 `width()`（如 `IconTitle.layout()`），改 scale 只会让图标压到文字上。缩放本身**不糊**
  （`SmartTexture` 默认就是 NEAREST），但**非整数倍会让少数行列宽一像素**；要用就按下一节那四条约束来。

### 考验选择界面改为「生命之树」排版（2026-09-25）

- **新布局**：十个质点按卡巴拉生命之树排三列，列间**严格等距蜂窝**（列间距 `h = a·√3/2`，相邻列错开 `a/2`）；
  中列在 `y = 0 / 2a / 3a / 4a`（`a` 处是**空掉的 Da'at 位**，纯空白），左右两列在 `0.5a / 1.5a / 2.5a`；
  **22 条路径**全画（14 条长 `a`、7 条长 `√3·a`、1 条 `KETER—TIPHERETH` 长 `2a`）。
- **几何全在 `WndTrials` 的常量区**（`A` / `H` / `ICON_SCALE` / `WIDTH` / `NODE_DX` / `NODE_DY` / `EDGES`）：
  树是纯静态几何、**不依赖任何手绘像素**，改 `A` 或 `ICON_SCALE` 会自己重排。
  ⚠️ 红线两条，`verify_trials_tree_ui.py` 都有断言：
  ① `a·√3 + 最大图标显示宽 ≤ WIDTH`（否则树**横向**溢出）；
  ② `4a + 最大图标显示高 ≤ 内容高 − 标题 − 上下留白`（否则窗口**纵向**顶出屏幕）。
- **三轮尺寸**（同一套几何，只动 `a` / `ICON_SCALE` / `WIDTH`）：

  | 轮次 | `A` | `ICON_SCALE` | 图标显示 | 树外包 | 内容 | 外框 | 左右各留 |
  |---|---|---|---|---|---|---|---|
  | 首版 2026-09-25 | 70 | 0.70 | 9~11 px | 132.24 × 291 | 140 × 315 | 152 × 327 | 3.88 |
  | 二版（用户「图标过小，需要翻倍」） | 70 | 1.40 | 18~22 px | 143.24 × 302 | 148 × 326 | 160 × 338 | 2.38 |
  | **三版·当前**（用户「占据整个屏幕，上下沿看不到，参考普通挑战栏」） | **47.5** | **1.40** | **18~22 px** | **103.27 × 212** | **120 × 236** | **132 × 248** | **8.36** |

  - 二版在机上**上下沿被顶出屏幕**（外框高 338）。参照物取 `WndChallenges`：它有 **13 条**挑战
    ⇒ 内容高 `16 + 13×16 + 12×1` = **236**、外框 132 × 248（**已确认在该机上完整可见**）。
  - 于是把 `A` 从 70 收到 **47.5** —— 这是**高度**卡出的上限（`4a + 22 ≤ 212`），
    内容高恰为 236 ⇒ **与挑战窗逐像素同规格**；而 `ICON_SCALE` 仍是 1.40，**图标一个像素没动**。
  - 宽度**不是**瓶颈：`a ≤ (120 − 21)/√3 = 57.1`。所以「翻倍图标 + 挑战窗大小的窗口」**可以兼得**，
    代价只是节点间距从 70 收到 47.5（斜向间隙仍有 **17.45px**、纵向 **25.5px**，反而比二版更宽松）。
  - 再想放大图标：`A` 已被高度锁死，只能**同步加高窗口**（每 +4 高才换来 +1 图标显示尺寸）；
    或改整数倍 2.0（图标 26~32），那时 `a ≥ 图标高 + 间隙` 会把窗口推得更高。
- **图标「只改显示尺寸、不重采样」**（用户 2026-09-25 口径）：取样矩形恒取原生 13~16px
  （`Trials.ICON_W/ICON_H`，`uvRectBySize`），只改 `Image.scale` ⇒ **贴图一个字节都不动**，
  缩放由 NEAREST（`SmartTexture` 的默认过滤；它**不是** LINEAR）在 draw 时完成。
  ⚠️ 已知代价：**非整数倍会让少数行列宽一像素**（0.70 是抽稀掉几列、1.40 是把几列补宽），用户已确认接受；
  要绝对干净只能用**整数倍**（1.0 / 2.0）。
  **单一出口**：显示尺寸只在 `WndTrials.scaleToDisplay(Image,int)` 设一次，树上质点与详情窗图标共用它，
  避免两处 `scale.set` 各写一遍走样（`verify_trials_tree_ui.py` 断言全仓 `icon.scale.set(` 恰好 1 处）。
- **详情窗图标**：与树上那枚**同一显示尺寸**（点谁看谁，大小连着）。
  ⚠️ 调用顺序不能反 —— `IconTitle.layout()` 读的是 `width() = 原生宽 × scale`，
  必须在 `new WndTrialInfo(...)` **之前**设好，否则标题位置会压在图标上。
- **路径层**：整层一次画进运行时 `Pixmap`（`TextureCache.create(KEY, w, h)` 后往 `tx.bitmap` 写像素，
  与 `effects/HeatVignette` 同一套；**不需要手动上传**，首次 `bind()` 才生成）。
  35% 黑、线端按「最大图标半高 + 2px」= **13px** 退让；最短的 `a` 边（47.5）仍可见 **21.5px**，不会看成虚线。
  ⚠️ 用 `Pixmap.Blending.None` **直接写入**而不是 SourceOver —— 否则两条线交叉处会叠两次、比别处更黑。
- **交互**：质点即热点（`Button` 无外观、命中区 26×26 —— 必须 **≥ 最大图标显示尺寸 22**，不然点图标边缘会漏；
  质点最近距离 = `A` = 47.5 > 26，热点不会互相压住）⇒ 开 `WndTrialInfo`
  详情窗（左上角**与树上同显示尺寸**的图标 + 名称 + 描述 + 开关）；**开关放在详情窗里，而且必须由
  `WndTrialInfo` 自己在 `super(...)` 之后构造**（⚠️ 见下一条红线）、调用方只传文案 / 初值 / 回调；
  勾选只改本窗口的「待提交」位掩码，
  仍是**关窗时**（`WndTrials.onBackPressed`）一次性写回 `SPDSettings`。
  开关一动、树上那枚图标立刻换行 ⇒ 图标行始终跟着**待提交值**走（`Trials.iconRow` 特意做成纯函数、不读设置）。
- ⚠️ **红线：详情窗里的开关不能在调用方 new**（2026-09-25 修，`verify_trials_tree_ui.py` 有断言）。
  `Button` 的 `PointerArea` 是在**构造时**就把自己注册进全局 `PointerEvent` 监听表的，而那张表是
  `new Signal<>(true)`（**stackMode**：`add()` 走 `addFirst`、`dispatch()` 从队首遍历、**首个返回 true 就
  `return`**）⇒ **后注册者优先，且第一个命中的会吞掉其余所有人**。而 `Window` 的构造会加一个**覆盖全屏**
  的 blocker（点窗口外即关窗），它照样拦截：`Gizmo.isActive()` 只看 `active && parent.isActive()`，
  **不看 `visible`**，而 blocker 正是 `visible = false` 的那个。
  原先 `openDetail` 是「先 `new CheckBox(...)` 再 `new WndTrialInfo(...)`」⇒ 开关比该 blocker **早注册**，
  每次点击都被 blocker 抢先命中 —— 症状就是**开关点上去毫无反应**（用户 2026-09-25 报）。
  现在开关由 `WndTrialInfo` 自己的构造体（`super(...)` 之后）建，参数是
  `toggleLabel / checked / toggleActive / ToggleListener`；`WndTrials` 里**连 `CheckBox` 这个词都不许有**。
  同族坑见 §6「UI 组件构造即崩」（都是**构造顺序**）。
- ⚠️ **窗口底色是深棕木纹**（`chrome.png` 的 `WINDOW` 九宫格，内部约 `#4E3A2D`），文本默认色为白、
  标题/名称/高亮走 `Window.TITLE_COLOR`（黄）—— 出预览图时别照抄「浅色窗口」的印象。
- ⚠️ **旧的逐条勾选列表已整体退役**（`CheckBox` 排在每行左侧那套）⇒ §3 与 §4 里
  「行高 14 / 图标在 16×16 槽内居中 / `ICON_SLOT`」这些结论对当前界面**不再适用**。

## 5. 核验

- `_chk/TrialsLocCheck.java` —— 真 `Properties.load` + **用真实类名反推键名**（`Trials`/`WndTrials` 去前缀小写），
  **311 条断言 ALL PASS**：常量骨架自洽（10 条、位＝`1<<i`、`MAX_VALUE=1023`）、图标表在帧内且**十帧**声明＝实测尺寸、
  20 个文本键 zh+en 非空 / 无 U+FFFD / 无 `/n/` / `_` 成对、10 个名称硬断言、挑战邻键回归。
  **2026-09-24 判据升级**（中文 `binah`/`chesed`/`gebura_desc` 改单段后，旧判据「必须解析出真换行」作废）：
  ① **`[TRUNC]` 文件级截断守卫** —— 每个 properties 的每一非空、非注释行都必须含 `=` 或 `:`
  （「字面 `\n` 被写成真换行 ⇒ entry 截断」的普适判据；单段文案同样受保护，覆盖面比逐键断言更大）；
  ② **`[SHAPE]`** —— 同一个键的 zh/en 段落结构必须一致（同为单段或同为多段）；
  ③ **`[RULE]`** —— GEBURA 的「僵直 / stagger」说明在 zh 与 en 必须**同时出现或同时不出现**。
  新判据的反例自测见 `_chk/TrialTextGuardProbe.java`（用真 Java 代码跑「真换行截断」「zh 单段 / en 多段」两组反例）。
- `_chk/verify_tree_trials_icons.py` —— 图案与常量互查（含 `--selftest`，证明「声明须包住实测」不是恒真断言）；
  ① 整图尺寸 ② 逐帧不越格 ③ 声明须包住实测 ④ **已绘制帧声明＝实测（十帧逐一相等）** ⑤ 未绘制帧仅 NOTE。
- `_chk/verify_trials_tree_ui.py` —— **生命之树界面静态回归**（**112 条断言** + **12 组**反例自测）：
  ① 几何（坐标表逐项对规格、路径集合与长度分布 `{a:14, √3a:7, 2a:1}`、无孤点、树宽 ≤ `WIDTH`）；
  ①b **窗口尺寸**（内容必须与 `WndChallenges` **逐像素同规格**；两边尺寸都**现算**、不抄死数字：
  挑战窗高 = `TTL + n×BTN + (n−1)×GAP`，`n` 由 `Challenges.NAME_IDS` 数出来（现为 13）；
  树高必须**正好吃满**剩余空间；`A` 的上限必须由**高度**决定而不是宽度）；
  ② 图标（`ICON_SCALE` 必须 = 1.40、显示尺寸由 `round(原生×ICON_SCALE)` 现算、
  **不得出现 `icon.resize` 与任何 `TextureFilter`/`Pixmap.Filter`/`bitmap.resize`/`texture.filter`/lanczos**、
  换行只改取样矩形 y、**全仓 `icon.scale.set(` 恰好 1 处**（单一出口 `scaleToDisplay`）、
  `scaleToDisplay` 必须 1 声明 + 2 调用、**命中区 `TOUCH` ≥ 最大图标显示尺寸**、
  详情窗里 `scaleToDisplay(icon,index)` 必须**早于** `new WndTrialInfo(`）；
  ③ 行映射（三行常量 0/1/2、`iconRow` 是纯函数、`isUnlocked` 恒真、行为真值表）；
  ④ 交互（`SPDSettings.trials(` 全仓**恰好 1 处**且在 `onBackPressed` 的 `editable` 守卫里、
  **`WndTrials` 里连 `CheckBox` 这个词都不许有**（开关只能由详情窗自己造）、
  **`WndTrialInfo` 里 `new CheckBox(` 必须出现在 `super(...)` 之后** —— 构造顺序＝PointerArea 注册顺序，
  早注册就会被窗口那个全屏 blocker 抢先命中、开关永远点不动；
  初值走 `checked(boolean)`（直接写字段不换勾选图标）、可否切换走 `toggleActive`、切换回调走 `onToggle`）；
  ⑤ 文案键 `windows.wndtrials.enable` 双语齐备且与 `Messages.get(this,"enable")` 拼写一致；
  ⑥ `tree.png` 三行包围盒逐格一致。
  ⚠️ 判据写法两条：查「有没有重采样」要查**调用**（`TextureFilter` 等），**别写 `'nearest' not in src`**
  —— 类注释里就写着「取样走 NEAREST」，那样写加了注释必挂；用 `body_of()` 取方法体时必须给 `sig=`
  限定到**声明**（调用点可能在声明之前，如 `scaleToDisplay` / `openDetail`，否则取到的是别的方法体）。
  `--selftest` 用 **12 组**反例（少一条路径 / Binah 放错列 / 改用 resize / **窗口宽回到 148** /
  **`A` 放大到 60 撑破高度** / iconRow 读设置 / 多一处实时写回 / **开关构造抢在 `super` 之前** /
  **开关初值直接写字段** / **调用方自己 `new CheckBox`** / 键名拼错 / `strip_comments` 被 `//**` 吞）
  证明判据不恒真。
- `_chk/verify_trial_icons.py` —— **`icons.png` 三枚专属格的坐标核验**（含 `--selftest`）：① 三处 rect 等于预期值；
  ② PNG 尺寸 256×128 且被 16 整除；③ 三格内**非空 + 内容不越界**；④ 灰/彩**满格**（包围盒 == rect）；
  ⑤ 与其余 83 个 rect **两两不相交**（防止误占别人的格）；⑥ 不与 `CHAL_COUNT` 重叠；
  ⑦ **同规格守卫**：`TRIAL_COUNT` 与 `CHAL_COUNT` 必须**等尺寸**且**字形相对偏移一致** ——
  这条才是「计数数字对齐」的真正保证，**只查「不重叠」是查不出来的**（8×8 与 7×7 也不重叠，但数字会错位 1px）。
  `--selftest` 用两组假数据证明重叠判定既不空转也不过敏，再用三组假数据证明同规格守卫抓得住
  「8×8 错配」与「字形偏移 +2」，同时不把正常的 7×7/7×7 误判为错配。
- `_chk/crop_icons_regions.py` —— 把 `icons.png` 指定区域**裁出来放大成预览 PNG**（`_chk/_icons_preview/`），
  带洋红网格线标出拟用的矩形边界。配置坐标前先用它**看一眼**：本次就是靠它确认「灰/彩两枚同款树、
  满格无留白」「计数那枚是 5×6 深色描边 + 淡黄阶梯」。纯 Python 写 PNG（无 PIL）。
- `_chk/analyze_icons_region.py` —— 按 `(x,y,w,h)` dump 该区域的逐行 ASCII 与包围盒，用于精确定位字形。

## 6. HOKMA（智慧，序号 1）的落地 —— 三类「最终延迟」夹取（2026-09-23）

### 需求

> 「角色的**最终移动延迟不会小于 1**（即最低为 1）；**怪物的最终移动延迟不会大于 1**（即最高为 1）；
> 且角色的**最终攻击延迟不会小于 0.5**。以上这些调整均**优先于所有加速/减速的 buff 和各类效果**。」

| 对象 | 约束 | 等价写法 | 常量 |
|---|---|---|---|
| 英雄移动 | 最终延迟 **≥ 1**（英雄不可能比「每回合走一格」更快） | `speed ≤ 1` | `HERO_MOVE_DELAY_MIN = 1f` |
| 敌方移动 | 最终延迟 **≤ 1**（敌人不可能比「每回合走一格」更慢） | `speed ≥ 1` | `ENEMY_MOVE_DELAY_MAX = 1f` |
| 英雄攻击 | 最终延迟 **≥ 0.5**（攻击最快每 0.5 回合一次） | —— | `HERO_ATTACK_DELAY_MIN = 0.5f` |

「**最终**」＝夹取发生在**一切 buff / 装备 / 天赋 / 护甲刻文 / 挑战修正叠加完之后**，
所以它压过所有加速与减速来源，**也压过原版挑战**（如 `AscensionChallenge.modifyHeroSpeed` /
`enemySpeedModifier` 的速度修正）。这就是「优先于所有加速/减速」的落地方式：
**不去按来源逐条拦截，而是在所有来源的下游放一个唯一的夹子。**

### 两个岔口（用户 2026-09-23 拍板）

- **战士天赋「手起刀落」（`LethalMomentumTracker`）的免费击杀一击 → 压住、严格按规则**：
  那记 `attackDelayRaw()` 里的 `return 0` 也受 0.5 下限约束（该击只省一半时间）。
  **天赋本身不改**（那是战士的固有强度），只把「本次攻击延迟 0」这一**结果**夹到 0.5。
  同理「夺命余势」（`LETHAL_HASTE` / `GreaterHaste`）的免费瞬移各要花 1 回合。
  两条天赋在 HOKMA 生效时会**在面板上追加一行小字**说明（见下「文本策略」）。
- **「怪物」的范围 → 仅敌方单位**（`alignment == Alignment.ENEMY`）。英雄的盟友（分身、玫瑰幽魂、
  灵鹰等）与中立 NPC（幽灵、王鼠）**不受影响**。判据与原版 `AscensionChallenge.enemySpeedModifier`
  完全一致 —— 复用同一套「谁算怪物」的定义，避免 mod 里再出现第二套名单。

### 核心设计：单一终局（single terminal）+ `final`

移动延迟在全作有**几十处** `1/speed()` / `delay/speed()` 的算法，但**速度值本身只有一个出口**：
`Char.speed()`。所以只在 `Char.speed()` 夹一次，就覆盖了**全部**移动延迟站点 ——
包括**将来新增**的站点，不需要逐处去改（也就不会漏）。

为了让子类**无法绕过**这个出口，把 `Char.speed()` 改成 **`final`**，原实现改名为 `speedRaw()`：

```java
// Char.java
public float speedRaw() { /* …原 speed() 的实现… */ }   // 子类覆写这里
public final float speed(){
    return Trials.modifyMoveSpeed( this, speedRaw() );  // 唯一出口，HOKMA 夹在最外层
}
```

```java
// Trials.java
public static float modifyMoveSpeed( Char ch, float speed ){
    if (ch == null || !Dungeon.isTrialled( HOKMA )) return speed;
    if (ch == Dungeon.hero)                       //英雄：延迟 ≥ 1 ⇔ 速度 ≤ 1
        return Math.min( speed, 1f / HERO_MOVE_DELAY_MIN );
    if (ch.alignment == Char.Alignment.ENEMY)     //敌方：延迟 ≤ 1 ⇔ 速度 ≥ 1
        return Math.max( speed, 1f / ENEMY_MOVE_DELAY_MAX );
    return speed;                                 //盟友 / 中立 NPC 原样放行
}
```

**连锁改动：11 处 `speed()` 覆写全部改名 `speedRaw()`**（`super.speed()` 一并改 `super.speedRaw()`），
否则 `final` 会让它们编译不过 —— 这份清单正好就是「**谁在偷偷改速度**」的完整答案：
`Hero`（另有戒指 / 护甲 / `Momentum` / `AscensionChallenge` 等修正）、`Mob`（接 `enemySpeedModifier`）、
`CrystalGuardian`、`DM300`、`npcs/Ghost`、`npcs/RatKing`、`npcs/Thief`、
`abilities/cleric/PowerOfMany`、`abilities/rogue/ShadowClone`、`items/artifacts/DriedRose`（幽灵公主本体）。
→ 它们**故意只覆写 `speedRaw()`**：自己的修正依然生效，但**出口只有一个**，HOKMA 的夹子在它们之上。

### ⚠️ 只夹速度**不够**：`0 / speed` 恒为 0

```java
// Hero.move
float delay = 1;
if (buff(GreaterHaste.class) != null) delay = 0;   //天赋「夺命余势」= 免费移动
…
spend( delay / speed() );
```

`delay = 0` 时 **`0 / speed` 无论 speed 被夹成多少都还是 0** —— 封顶速度**拦不住免费移动**。
攻击延迟同理：它**根本不经过 `speed()`**。所以这两条路必须**各自单独收口**：

```java
// Hero.move —— 最终值在 spend 处再夹一次（全作其余移动站点分子恒为 1，已被 Char.speed() 覆盖）
spend( Trials.modifyHeroMoveDelay( delay / speed() ) );

// Hero.attackDelay —— 唯一出口，同样 final；夹在 attackDelayRaw() 之外
public final float attackDelay(){
    return Trials.modifyHeroAttackDelay( attackDelayRaw() );
}
protected float attackDelayRaw(){
    if (buff(Talent.LethalMomentumTracker.class) != null){
        buff(Talent.LethalMomentumTracker.class).detach();
        return 0;             // ← 这记「手起刀落」必须在 raw 里，才会被外面的 0.5 夹住
    }
    …
}
```

**「内 / 外」的区别就是要害**：若 `attackDelay()` 不做 `final`、或把 LethalMomentum 的 `return 0`
挪到夹取之后，HOKMA 就会被天赋**击穿**（这正是用户拍板要「严格按规则」的地方）。

### 一个特例：`CrystalGuardian` 的碎水晶追击

水晶守卫「击碎水晶后追加一次移动」自己写 `spend(1/super.speed())`，**绕过了 `speed()`**。
它必须**显式经过夹子**，但又**不能改调 `speed()`** —— 因为 `speed()` 会把它自己那记
「封闭空间内 `/4`」的惩罚**再乘一遍**（其 `speedRaw()` 里已算过 `/4`）。所以写：

```java
spend( 1 / Trials.modifyMoveSpeed( this, super.speedRaw() ) );   // 过夹子，且不重复施加 /4
```

⚠️ 这是一种容易漏的**「绕过出口的自算延迟」**：改速度汇聚点时，除主出口外还要 `grep` 一遍
`1/speed()`、`1 / …\.speed()` 这类**自算延迟**的站点，看有没有谁绕过了出口。

### 文本策略：只在 HOKMA 生效时追加说明

三条限制会改变两个天赋在**本局**的实际表现，故给它们加了 `.hokma` 键，由 `Talent.desc()`
在 `Dungeon.isTrialled(Trials.HOKMA)` 时**追加**一行：

```java
private static String hokmaDelayNote( Talent talent ){
    if (talent == null || !Dungeon.isTrialled( Trials.HOKMA )) return "";
    String note = Messages.get( Talent.class, talent.name() + ".hokma" );
    if (note.equals( Messages.NO_TEXT_FOUND )) return "";   //没有本键就什么都不加
    return "\n\n" + note;
}
```

- 键缺失时**静默跳过**（`NO_TEXT_FOUND` 判定），所以只给「真的会被规则改变」的天赋写键即可；
  普通局（未开 HOKMA）文本**一字不变**。
- 本次写了两个：`actors.hero.talent.lethal_momentum.hokma`（0.5 回合）、
  `actors.hero.talent.lethal_haste.hokma`（1 回合）。
- `trials.hokma_desc` 改为三段实际效果描述（含「压过一切加速/减速」与「怪物＝敌方单位」），中英双份同步。

### 落点清单（13 个文件）

- `Trials.java` —— 三常量 + 三个静态钩子（`modifyMoveSpeed` / `modifyHeroMoveDelay` /
  `modifyHeroAttackDelay`，全部 `Dungeon.isTrialled(HOKMA)` 门控）+ 类注释改为「HOKMA 已实现」。
- `actors/Char.java` —— `speed()` 拆成 `final speed()` + `speedRaw()`。
- `actors/hero/Hero.java` —— `speedRaw()` 改名；`attackDelay()` 拆成 `final attackDelay()` + `attackDelayRaw()`；
  `move()` 的 `spend(…)` 过 `modifyHeroMoveDelay`；删掉一处真未用的 import。
- `actors/hero/Talent.java` —— `desc()` 追加 `hokmaDelayNote(…)`。
- 9 个子类 `speed()`→`speedRaw()`（`Mob` / `CrystalGuardian` / `DM300` / `Ghost` / `RatKing` / `Thief` /
  `PowerOfMany` / `ShadowClone` / `DriedRose`）+ `CrystalGuardian` 追击那记 spend 过夹子。
- 文本 4 个：`misc_zh`/`misc.properties` 的 `trials.hokma_desc`、`actors_zh`/`actors.properties` 的两个 `.hokma`。

### 核验

- `_chk/verify_hokma_delay.py` —— **三层核验**（含 `--selftest`）：① 源码结构（`HOKMA == 2`、三常量取值、
  三个钩子都带 `Dungeon.isTrialled` 门控、`Char.speed()` 是**唯一 final** 出口且 `speedRaw` 定义数 = 11、
  `Hero.attackDelay` 是 final 且门控、`Hero.move` 的 spend 已过夹子）；② `javap -p` 签名；
  ③ `javap -c` 字节码（`Math.min` / `Math.max` 真在、**英雄分支在敌方分支之前**、`1.0f` 上下限各出现、
  `HOKMA` 位以 `iconst_2` 出现 —— ⚠️ 编译期常量会被**内联进字节码**，所以断言要找内联后的
  `iconst_2` + `Dungeon.isTrialled` 调用，**不能**去找 `Trials.HOKMA` 字段名）。
- `_chk/TrialsLocCheck.java` —— 断言由 207 → **241 条**：新增 §⑦ 用真 `Properties.load` 校验 `HOKMA == 2`、
  `NAME_IDS[1] == "hokma"`、三常量（1f/1f/0.5f）、`trials.hokma_desc` **含真换行**（证明 `\n` 是字面转义、
  而非 entry 断成两截）、文本提到 0.5 与 1、两个 `.hokma` 注记存在且原 `.desc` 未丢。
- `_chk/patch_speed_terminal.py` / `_chk/patch_hokma_text.py` —— 两个**幂等**补丁脚本（重跑不重复改），
  断言用「比位置」而非「比存在」。
- 顺带修：`_chk/check_unused_imports.py` 原把「只在 javadoc `{@link …}` 里出现的类」误报成未用，
  已把 `@link/@linkplain/@see/@throws/@exception/@value` 的引用也算作「已用」（并加进 `--selftest`）。

## 7. BINAH（理解，序号 2）的落地 —— 「生成时」等级规则（2026-09-23）

### 需求原文

> 道具自然生成时不会带有正等级，若有等级的道具生成时带有诅咒，则等级变为负数。
> 任务 NPC 给予的道具等级变为 0。
> （备注：影响的是道具的生成时行为，而不是全局行为。若诅咒道具生成时为 0，则等级不变，还是 0。
> 任务 NPC 包括 1 区的幽灵、2 区的制杖人给予的法杖、3 区的铁匠的锻造随机生成的装备、4 区的小恶魔任务给予的戒指）

### ⚠️ 判定顺序（用户补充澄清 —— 顺序反了整个考验会退化）

> 「如果先确定道具不会带有正等级、随后再进行等级反转，就不会出现负等级的装备，
> 所以是先判定是否因诅咒反转等级，再判定正等级归零」

两步读的是**同一个**「生成时掷出的等级」：

```
① if (cursed) lvl = -lvl;      // 因诅咒反转
② if (lvl > 0) lvl = 0;        // 正等级归零
```

① 的结果**永不 > 0**，所以 ② 只会命中「未被诅咒」的那些。若把 ② 提到 ① 之前，
等级会先被清零、① 再反转 0 仍是 0 ⇒ **负等级永远不会出现**，整条考验退化成「所有道具都是 +0」。

### 收口点：`Item.random()`（`final` 封口）

「自然生成」在全作只有一个出口 —— `Item.random()`。所有子类都在这一个方法里先 `level(n)` 掷等级、
再掷诅咒（`Weapon` / `Armor` / `MissileWeapon` 各 75/20/5% 出 +0/+1/+2、30% 诅咒；
`Wand` / `Ring` 为 66.67/26.67/6.67%；`Artifact` 恒 +0 但 30% 诅咒），最后由 `Generator` 的六个分支取走。

与 HOKMA 同款处理：`Item.random()` 改 `final`，方法体转调 `Trials.modifyGeneratedItem(randomRaw())`，
8 个子类的 `public Item random()` 一律改名 `protected Item randomRaw()`。
好处是**编译期**保证没有第二个出口：将来谁再覆写 `random()` 直接编译不过。

覆盖范围（都属于「自然生成」，所以自动生效，无需逐个接线）：
地面掉落 / 商店货架 / 各类宝箱与房间道具 / `Generator.randomWeapon|randomArmor|randomMissile|randomArtifact` /
**雕像 `Statue` 的武器** / **装甲兽 `ArmoredBrute` 的护甲** / 秘密房间与水晶箱。

**不在范围内**（这正是需求里「生成时行为、不是全局行为」的含义）：
铁匠重铸（动的是玩家自己的道具，走 `first.upgrade()`）、升级卷轴、附魔与刻文、
角色开局自带装备（`Belongings`）、**前一名冒险者的遗骸（`Bones`）**，
以及 `Statue` 生成后自己做的 `weapon.cursed = false`。

### 任务 NPC 的四处「压平」

四处赠礼都在 `Generator.random()` **之后**由 NPC 自己加等级，`modifyGeneratedItem` 拦不到，
所以另加一个钩子 `Trials.flattenQuestReward(Item)`（等级 → 0、**不动诅咒**），在各自加完等级之后调用：

| 区 | NPC | 自家加等级 | 落点要点 |
|---|---|---|---|
| 1 下水道 | 幽灵 `Ghost` | `upgrade(itemLevel)` +0~+3（武器与护甲各一次） | 武器是 `Generator` 生成、护甲是 `new` 出来的，两个都要压 |
| 2 监狱 | 制杖人 `Wandmaker` | `upgrade()` +1（两根候选杖各一次） | 压平放在 `cursed = false; upgrade();` 之后 |
| 3 洞穴 | 铁匠 `Blacksmith` | `level(rewardLevel)` +0~+3（5 件奖励） | 压平放进 `generateRewards()` 的 `for` 循环 |
| 4 矮人都市 | 小恶魔 `Imp` | `upgrade(2)` +2 | 压平放在 `cursed = true` 之后 |

**为什么是「事后压平」而不是「跳过 upgrade」**：`Ring.upgrade()` 内含 `Random.Int(3)`（祛咒掷点）。
跳过 `upgrade(2)` 会少消耗随机数、挪动之后的随机流；事后压平则完全不碰 RNG。
制杖人那两根还有第二个理由：`Wand.random()` 若掷出诅咒，BINAH 会让它变成负等级，
而 NPC 随后会 `cursed = false` —— 若只对「掷出的等级」做归零，会留下**无诅咒的负等级法杖**；
事后压平一步到位。

### 特殊生成：祭坛与墓室的「事后诅咒」（2026-09-23 追加）

用户点名追问的「祭坛地形」（`levels/rooms/special/SacrificeRoom.java`）与同型的**墓室石棺**
（`CryptRoom.java`）是另一条路：**道具确实是自然生成的，但诅咒是房间事后补的**。

> 触发方式（顺带纠正一处常见误解）：祭坛不是「把道具放上去」，而是把**怪物（或英雄自己）**送到火堆旁送死
> —— `actors/blobs/SacrificialFire.sacrifice()` 按死者 EXP 扣掉火的 `volume`，归零时掉落武器。
> 另外火熄灭时若 `prize` 为 null（老存档没把 prize 存进 bundle），第 205 行会在**运行期**重新调一次
> `SacrificeRoom.prize()` —— 同一条路径，所以本次修复对两条路径都生效。

房间赠品逻辑（武器版；墓室是护甲版，逐行同型）：

```java
Weapon prize = Generator.randomWeapon( (Dungeon.depth / 5) + 1);  // ← 走 Item.random 收口，BINAH 生效
...
if (!prize.cursed){                  // ← 建在生成**之后**的分支
    prize.upgrade();                 // ← 白送 +1，BINAH 拦不到
    if (!prize.hasGoodEnchant()) prize.enchant(curse);
}
prize.cursed = prize.cursedKnown = true;   // ← 强制诅咒，也在生成**之后**
```

**为什么只覆盖了一半**：`prize.cursed = true` 写在生成之后，所以 BINAH 规则② 在生成出口读到的
`cursed` 仍是 `false` —— 祭坛「必定诅咒」这个特征**帮不上忙**。逐分支：

| 生成时 roll | BINAH 出口值 | `!cursed` | 房间加工 | 修复前最终 | 修复后最终 |
|---|---|---|---|---|---|
| 未中诅咒（**70%**） | 0（正等级已抹平） | true → 进分支 | `upgrade()` +1 + 强制诅咒 | **+1 诅咒** ✗ 违反规则① | **−1 诅咒** ✓ |
| 中诅咒（**30%**） | −n（n = 0/1/2） | false → 跳分支 | 强制诅咒（本就是） | 0 / −1 / −2 | 0 / −1 / −2（不变） |

⇒ 修复前祭坛武器约七成是 **+1 诅咒武器**，直接违反「生成时不会带有正等级」；
而那三成生成时就掷中诅咒的，规则② 反而是生效的。

**修法**：新增第三个钩子 `Trials.curseReverseLevel(Item)`（**只对正等级取负**，0 与负数原样保留），
在房间**完成自身的等级调整之后**（放在 `prize.cursed = true` 之后）调用一次。

⚠️ **为什么不能复用现成的两个钩子**（本机制最容易踩的坑）：

- `flattenQuestReward` 是「非 0 一律归 0」，会把那 30% 里 BINAH 规则② **已经算好的 −1/−2 一起抹成 0**
  ⇒ 反而违反规则②；
- 二次调用 `modifyGeneratedItem` 会再取一次负（−1 → +1 → 0）⇒ **双重取负**。

所以必须是「只处理正等级」的第三种语义。`curseReverseLevel` 同样只写 `level(int)`、不碰 `Random`
（字节码层已证明无 `com/watabou/utils/Random`），**随机流依旧不变**。

**范围界定（刻意不扩，按用户决定）**：同目录另有三个「**永不诅咒**」房间
`PoolRoom` / `SentryRoom` / `TrapsRoom` —— 它们显式 `prize.cursed = false`，规则② 永不触发，
不属于用户所说的「必定诅咒」房间；它们同样有事后 `prize.upgrade()`（33% 概率），理论上也会绕过规则①，
**本次不处理**。`verify_binah_gen.py` ⑤ 里有一条断言把「它们**不**接钩子」钉住，防止将来被误加。
`ShopRoom` 则显式 `level(0)`，无问题。

### ⚠️ 随机流不变量（本次最要紧的约束）

改动**只读** `trueLevel()` / `cursed`、**只写** `level(int)`，全程不碰 `Random`
（`level(int)` 走 `updateQuickslot()` / `Wand.updateLevel()`，两者都没有随机数）
⇒ **同一 seed 的关卡布局与不开本考验时逐字节一致**。
这一条不是靠读源码推的：`_chk/BinahGenProbe.java` ⑥ 在同一 seed 下取 200 个随机数，
一轮中间穿插钩子调用、一轮不穿插，实测**逐位相同**。

### 两个「读等级」的坑

- 读必须用 `trueLevel()`（原始存储字段），而**不是** `level()`：`Wand` 覆写了 `level()`，
  会把 `curseInfusionBonus`（+1+level/6）与 `resinBonus` 算进去，拿到的是「显示等级」而非「掷出的等级」。
- 写用 `level(int)` 是**安全的**（上游 `Ghost` / `Blacksmith` 本来就这么写）：
  `Wand.level(int)` 会转调 `updateLevel()` 重算 `maxCharges`、`RingOfMight.level(int)` 会转调
  `updateTargetHT()`。探针实测：+2 法杖压平后 `maxCharges` 由 5 回到 3（= `initialCharges()`），`curCharges` 同步被夹。

### 落点清单（16 个文件；另加 2 个文本）

- `Trials.java` —— `BINAH` 段说明 + **三个**钩子 `modifyGeneratedItem(Item)` / `flattenQuestReward(Item)` /
  `curseReverseLevel(Item)`（都 `Dungeon.isTrialled(BINAH)` 门控）+ 类注释进度改为「HOKMA、BINAH 已实现」。
- `items/Item.java` —— `random()` 改 `final` 并转调 `Trials`；新增 `protected Item randomRaw()` 默认实现。
- 8 个子类 `public Item random()` → `protected Item randomRaw()`：`Armor` / `Artifact` / `Bomb` / `Gold` /
  `Ring` / `Wand` / `MissileWeapon` / `Weapon`（**只改签名一行，方法体一字未动**，见核验 ④）。
- 4 个 NPC：`Wandmaker`（2 处）/ `Ghost`（2 处）/ `Blacksmith`（循环内 1 处）/ `Imp`（1 处）+ 各自 `import Trials`。
- 2 个特殊房间（2026-09-23 追加）：`levels/rooms/special/SacrificeRoom.java`（祭坛）与 `CryptRoom.java`
  （墓室石棺）+ 各自 `import Trials`，在 `prize.cursed = true` 之后调 `Trials.curseReverseLevel( prize )`。
- 文本 2 个（不计入上面的 16）：`misc_zh.properties` / `misc.properties` 的 `trials.binah_desc`。

### 附带影响（上游行为，未改，先记着）

- `Statue` 生成武器后会自行 `weapon.cursed = false` ⇒ 若那次生成掷出诅咒，BINAH 先把等级取负、
  `Statue` 再去掉诅咒，玩家可能捡到**无诅咒的负等级武器**。实测无崩溃
  （`ItemSlot` 对 `buffedLvl < 0` 本就走 `DEGRADED` 配色）。
- 调试窗 `WndDebug` 生成的物品走 `Reflection.newInstance(clazz)` 后**不调** `random()`
  ⇒ 不受 BINAH 影响（正好方便做对照测试）。

### 核验

- `_chk/verify_binah_gen.py` —— **五层核验**（含 `--selftest`）：
  ① 源码结构（`BINAH == 4`、`Item.random()` final、8 个子类各恰好 1 个 `randomRaw`、全仓无残留
  `public Item random()` 与 `super.random()`、**两步顺序比位置**、读 `trueLevel()`、四处 NPC 压平在加等级**之后**、
  `Generator` 不重复调钩子）；② `javap -p` 签名；
  ③ `javap -c` 字节码（`Item.random()` 真的先 `randomRaw` 再 `Trials`、**`ineg` 偏移 < `iconst_0` 偏移**、
  `iconst_4` 内联的 BINAH 位、三个钩子方法体都不含 `com/watabou/utils/Random`）；
  ④ 备份差异层（8 个子类与 `_chk/_bak_binah/` 相比**只有签名 1 行不同**）；
  ⑤ 房间赠品层（2026-09-23 追加）—— 两个房间的 `import Trials`、调用点位于「生成 → 事后 `upgrade()` →
  强制诅咒 → 取负」**顺序之后**、取负之后再无升级/改等级、`curseReverseLevel` 只对**正等级**取负、
  调用点唯一（只有这两个房间）、三个「永不诅咒」房间**不**接钩子；外加该钩子的字节码门控 /
  `ineg` / 条件分支 / 无 `Random`。
  脚本内另带 `strip_comments`（保字符偏移的单趟状态机）—— 因为**注释里写的伪代码会被 `in` 匹配到**，
  本次初版就被自己的注释骗出两条假 FAIL（说明里的 `if (lvl > 0) lvl = 0;`、注释里的 `updateQuickslot`），
  已用 `--selftest` 守住。
- `_chk/BinahGenProbe.java` —— **行为真值表探针**（脱离游戏真跑，**133 条断言**）：
  5 类道具（武器/护甲/法杖/投掷/戒指）× 8 格真值表；诅咒 +0 → 0（用户点名的那格）；
  法杖 `maxCharges` 重算；「生成时规则、非全局规则」（钩子跑完再 `upgrade(2)` → +2）；
  四处 NPC 压平且**诅咒保持**；关掉 BINAH 时全部为空操作；**同 seed 200 个随机数逐位相同**；
  ⑦ 房间取负真值表（+0→0、+1→−1、**−1/−2/−3 原样保留**，含「−1 不得被二次取负成 +1」这条陷阱回归，
  并与 `flattenQuestReward`「把 −2 抹成 0」作对照）。
- `_chk/TrialsLocCheck.java` —— 断言 241 → **261 条**：新增 §⑧ 校验 `BINAH == 4`、`NAME_IDS[2] == "binah"`、
  **反射读 `Item.random()` 的 `ACC_FINAL`**（只有真编译产物答得出来，与源码层断言互为交叉验证）、
  `Item.randomRaw()` 存在、`trials.binah_desc` 解析出真换行 / 提到 0 与负等级 / 不再是占位文案。
- `_chk/patch_binah_random.py` / `_chk/patch_binah_text.py` —— 两个**幂等**补丁脚本。
  ⚠️ 前者改成**字节级**读写：本工程 Java 源是 **CRLF**，用文本模式 `io.open(..., newline='\n')`
  会把整个文件改写成 LF、git diff 立刻变成「每行都改了」（本次已踩，脚本末尾已加 CRLF 计数断言兜底）。

---

# 2026-09-23 §8. CHESED（慈悲，序号 3） + GEBURA（严厉，序号 4）—— 两条只作用于**敌方单位**的规则

## 8.1 需求原文

- **CHESED**：所有敌方单位的生命值 **+25%**，并且**每 5 回合恢复 10% 的血量**。
  - 补充（用户原话）：回血判定**在怪物血量不满时才开始计时**；**回血数值不要使用硬编码**，后续随时可能调整。
- **GEBURA**：所有敌方单位**即将死亡时进入无敌状态，一定回合内不会倒下**；回合数 = **怪物可以给予的经验数量**
  （例如给予 5 经验的怪物死亡时拥有 5 回合的无敌）；无敌回合结束时则死亡。
  - 补充（用户原话）：**判断死亡并给予无敌的时机要靠后**，避免影响原版的「死亡后复活 / 战续」怪物的行为
    （点名：**豺狼暴徒**、**矮人尸群**）。

用户经 `AskUserQuestion` 拍板的三点：① GEBURA **含 Boss**；② 无敌期间血量**停在 0**（空血条仍不倒）；
③ 回合数取**怪物固有经验值 `Mob.EXP`**（不是「实际结算给英雄的经验」—— 后者在英雄等级超过该单位上限时为 0，
会让这条考验在后期直接失效）。

## 8.2 为什么不能套用 HOKMA / BINAH 的收口（三类规则）

- **全局规则**（HOKMA）＝ 夹在「一切结算完之后」的唯一出口，用 `final` 封口。
- **事件型规则**（BINAH、CHESED 的生命上限）＝ 只在**某个动作发生的那一刻**生效，收口在唯一入口。
- **挡路型规则**（GEBURA）＝ **没有**现成的动作入口，必须让原版的既有流程**走到某一行**才判 ⇒ 钉在
  `Char.damage` 的**某两行之间**，靠「位置」而不是「新入口」生效。

⇒ 三处收口各不相同：CHESED 在 `Mob.onAdd()`（入场事件）＋ 一只常驻 buff；GEBURA 在 `Char.damage` 的致死闸门。

## 8.3 CHESED 收口点一：`Mob.onAdd()`（生命上限 +25%）

原版就在 `Mob.onAdd()` 里用一个 `firstAdded` 守卫给 AscensionChallenge 改血：

```java
if (firstAdded) {
    float percent = HP / (float) HT;
    HT = Math.round(HT * AscensionChallenge.statModifier(this));
    HT = Trials.modifyMobHT( this, HT );      // ← CHESED：夹在挑战倍率之后、HP 回算之前
    HP = Math.round(HT * percent);
    firstAdded = false;
}
Trials.bindMobPassives( this );               // ← CHESED：常驻重挂（必须在守卫之外）
```

- **为什么夹在这两行之间**：夹在挑战倍率**之后** ⇒ 两条规则互不干扰（乘法可交换，但「先挑战后考验」与原版
  `statModifier` 的语义一致，便于将来加日志）；夹在 `HP = round(HT*percent)` **之前** ⇒ 怪物仍以**满血**入场。
  顺序反了会在界面上表现为「入场不是满血」。
- **存档安全**：存档里存的是**放大过的** HT，而 `restoreFromBundle` 会把 `firstAdded` 置 false ⇒ **读档不会二次放大**
  （完全沿用原版对挑战的处理方式，没有新加存档字段）。
- ⚠️ **局限（原版收口自带的，与原版挑战待遇一致）**：少数单位的 HT 是**入场之后**才定的 —— `Bee.spawn()`、
  Boss 换阶段重算、`Swarm` 分裂等 ⇒ 这些拿不到 +25%。刻意不为它们另开出口（否则等于给这两类单位开小灶、
  也破坏了「唯一收口」这条不变量）。

## 8.4 CHESED 收口点二：常驻计时 buff `ChesedMend`（每 5 回合回 10%）

- **为什么是 buff 而不是写进 `Mob.act()`**：约 63 个 `act()` 覆写里只有 44 个调 `super.act()`；
  `CrystalSpire` / `Masterpiece` / `MobSpawner` / `Sheep` 等**不调** ⇒ 写在 `Mob.act()` 里会**整类漏掉**。
  buff 有自己的回合，谁都漏不掉；另一个好处是它**随存档保存**（`Char.storeInBundle` 存的就是 buffs 列表），
  读档后继续沿用原来的计时。
- **重挂点**：`Trials.bindMobPassives(this)`，必须在 `firstAdded` 守卫**之外** —— 读档时 `firstAdded` 已是 false，
  放里面会**漏挂**。幂等（`Buff.affect` 已有该 buff 时返回既有实例）。
- **计时语义（严格照需求「不满血才开始计时」）**：
  ```java
  if (mob == null || !Dungeon.isTrialled(CHESED) || mob.alignment != ENEMY
          || !mob.isAlive() || mob.HP >= mob.HT) { turns = 0; ... }   // 满血 ⇒ 归零，不累积
  if (++turns >= Trials.CHESED_MEND_TURNS) { turns = 0; ... }         // 连续 5 回合不满血才回一次
  ```
  `alignment` **每次 `act()` 现判**（不是挂 buff 时判一次）⇒ 被腐蚀 / 被招募 / 被魅惑成友方后**立刻停回血**，
  变回敌方再继续。
- **回血量不写死**（用户点名要求）：
  `heal = min( HT - HP, max( 1, round( HT * Trials.CHESED_MEND_PERCENT ) ) )` —— 口径全在
  `Trials.CHESED_HP_MULT / CHESED_MEND_TURNS / CHESED_MEND_PERCENT` 三个常量里，后续要调**只改这一处**；
  `max(1, ...)` 保底（极小血量单位不至于回 0）、`min(HT-HP, ...)` 不溢出。
- ⚠️ **buff 的 `type` 只能是 POSITIVE / NEUTRAL**：`Mob.Sleeping` 把 NEGATIVE 的 buff 当成「被打醒」的
  信号 ⇒ 标成 NEGATIVE 会让**全图怪物一入场就醒来并警戒**（灾难性副作用）。

## 8.5 GEBURA 收口点：`Char.damage` 的致死闸门（「靠后」的由来）

插入位置固定为：

```java
if (HP < 0) HP = 0;                                  // (a) 伤害已打进血量
GritTeethBuff.checkBypass( this, src );              // (b) 「咬紧牙关」的免死击穿检查
Trials.interceptLethalDamage( this, src );           // (c) GEBURA：闸门就钉在这一行
if (!isAlive()) { die( src ); }                      // (d) 宣布死亡
```
（闸门一共**三个**调用点：这一处 ＋ `Brute.BruteRage.act()` / `ArmoredBrute.ArmoredRage.act()` 里战续
耗尽的那两处，见 §8.5.2。）

**为什么必须是这一行**（＝用户说的「靠后」）：

1. **最晚的可撤回点**：伤害已算完、护盾已结算、HP 已归零，只差宣布死亡 ⇒ 此刻才判得出「这一击是否致命」。
2. **在一切死亡副作用之前**：此时子类的 `die()` 还没跑 ⇒ `DM300` 不会在**还活着**的时候
   `bossSlain() + unseal() + 掉碎片`、矮人国王不会提前把王座战利品丢出来、装甲雕像不会原地掉装备。
   子类 `die()`（loot / 经验 / 统计 / 剧情善后）**全程只跑一次**，且跑在真正该死的那一刻
   （由 buff 计时结束时调 `die(原始来源)`）。
3. **原版两种「战续」各有各的接法**（用户口径：**豺狼暴徒接、矮人尸群不接**）：
   - **`Ghoul`（矮人尸群）**：在 `die()` 里、调 `super.die()` **之前**就 `return` 转入倒地待复活 ⇒ 由
     `Mob.deathIsDeferred()` 显式**让路**。`Ghoul` 覆写该判据，且判据与它 `die()` 里的分支**同源**
     （`GhoulLifeLink.searchForHost(this) != null`）；那个方法是**纯查询**（无副作用、不碰 `Random`），
     所以「先问一次、`die()` 里再问一次」不会有任何行为差异。
     ⚠️ 若把让路判据改成「自己另判一套」，两边会各判一次、行为会漂移 —— 必须同源。
   - **`Brute` / `ArmoredBrute`（豺狼暴徒 / 装甲暴徒）**：它们覆写 `isAlive()`（狂暴护盾没耗尽就始终算
     「活着」）⇒ 战续**期间**闸门**根本问不到它**，狂暴流程与护盾时长分毫不动。但**战续耗尽那一刻**是
     `BruteRage.act()`（装甲暴徒为 `ArmoredRage.act()`，它**覆写了 `act()`**）自己调 `die(null)` 的，
     绕开了 `Char.damage` ⇒ 为它在**紧贴那句 `die(null)` 之前**另接同一个钩子。于是豺狼暴徒＝
     **「狂暴扛完 → 再吃 GEBURA 无敌」两次续命**（见 §8.5.2）。

## 8.6 GEBURA 的「无敌 + 不倒」怎么实现（复用原版，不新写判定）

- **不倒**：覆写 `Mob.isAlive()` 返回 `super.isAlive() || geburaGrace` ⇒ 紧随其后的 `if (!isAlive()) die(src)`
  **不执行**，血量**停在 0**（空血条）。`isActive()` 也随之变真（`isAlive()` 的实现之一）⇒ 单位仍在回合调度里、
  **会照常行动** —— ⚠️ 但**刚被打成濒死的那一回合是例外**：它会僵直一回合、不动
  （2026-09-24 削弱，见 §8.12）。玩家文案已写明这一点，免得玩家以为「打空了就安全了」。
- **无敌**：覆写 `Mob.isInvulnerable(Class)` ⇒ `Char.damage` / `Char.attack` 里**原版就有的**无敌分支会吃掉伤害
  并弹「无敌」浮字（文案键 `actors.char.invulnerable` **已存在**，无需新增），英雄的盟友也会自动不去打它
  （`Mob.chooseEnemy` 本来就跳过无敌目标）—— **一行判定都不用新写**。
- ⚠️ **`isAlive()` 里不能现查 `buff(...)`**：原版在 `Char.deathMarked` 上注明 `isAlive()` 会在**绘制期**被调用
  （性能 + 线程约束）⇒ 无敌状态缓存成 `Mob` 上的布尔字段 **`geburaGrace`**，由 `GeburaGrace` 的
  `attachTo` / `detach` 维护。读档时 buff 随 `Char.restoreFromBundle` 重新 `attach` ⇒ 缓存**自动重建**，
  不必单独存档（`Mob.storeInBundle` 里确实**没有**它）。
- **计时结束即死**：`GeburaGrace.act()` 里 `--turnsLeft <= 0` 时 —— ⚠️ **顺序要紧**：`detach()` 会清掉
  `geburaGrace`，而 `isAlive()` 依赖它 ⇒ 必须**先问活着、后摘自己**，反过来问就永远是 false、怪物会赖着不死。
  然后用**原始致死来源**调 `die(src)` ⇒ 击杀归属（经验、掉落、环指大师素材、复仇账簿充能等）与原版一致。
  跨存档时 `lethalSource`（transient）还原不了，只能按 `lethalSourceClass` 还原「**英雄本人击杀**」这一类 ——
  足以覆盖最关键的「英雄击杀」专属结算。
- **豁免是一次性的**：`Mob.geburaUsed` 门闩。它**不必存档**（用掉豁免的结局一定是「计时结束后真死」，
  而死者不会被写进存档）。
- **其他例外（都保持原版，不豁免、不延迟）**：**落坑**（`Chasm` 直接调 `die()`）、剧情演出的直接终结都
  **不走闸门**；`Dungeon.level.pit[pos]` 上的致死不豁免（站在深渊上白送 N 回合太亏）；
  `EXP <= 0` 的单位不豁免（0 回合＝本来就不该给）。

#### 8.5.1 ⚠️ 本考验的**作用范围**：「被打死」＋被点名的战续结束，不含「被机制直接终结」

这是**刻意的划界**，不是漏做。闸门本体只钉在 `Char.damage` 一处，另为**战续耗尽**（`BruteRage` /
`ArmoredRage`，用户点名）开了两个调用点；除此之外一切**绕过伤害**、由代码直接调 `die()` 的终结
一律照原版立即生效。全库该类站点清点如下
（`grep -rn "\.die(" core/src --include=*.java` 后逐个分类），**都不接钩子**：

| 站点 | 性质 | 为什么不能接 |
|---|---|---|
| `Char.attack` 两处 `enemy.HP = 0; → enemy.die(this)`（`Preparation.canKO` 潜行处决 / `Talent.COMBINED_LETHALITY`） | 处决类**必杀**机制 | 「必杀」的语义就是不受「濒死无敌」影响；同一类还有 `DeathMark`、`NeverForget` |
| `Chasm` 落坑、`SmokeBomb`、`DwarfKing` 阶段清场、`Necromancer` / `SpectralNecromancer` 献祭召唤物、 `RotHeart`、`Ghoul` 的 `GhoulLifeLink` 超时、`Berserk` / `GritTeethBuff`、剧情演出 | 脚本 / 剧情 / 机制终结 | 与「战续」同类：都**没有**「这一击是否致命」这个语义，硬接会把机制改成别的机制 |
| ~~`Brute` / `ArmoredBrute` 的 `target.die(null)`（`BruteRage` / `ArmoredRage` 到期）~~ | 战续**结束** | **已改为接钩子**（用户 2026-09-23 追加要求）⇒ 见下方 §8.5.2 |

⇒ 一句话：**GEBURA 让怪物「多挨几下打」，不让它「免疫机制」；唯一的例外是「战续结束」这一刻，
因为它本来就是「这一击最终落地」的时刻。** 若将来要把某一处纳入，做法是在该站点
`die()` 之前插同一个 `Trials.interceptLethalDamage(ch, src)`（钩子本身与来源无关，见探针 §③
「无敌与伤害来源无关：任何 src 类别都免疫」），但**必须先确认它不属于上表的「必杀/剧情」两类**。

#### 8.5.2 战续结束也要吃 GEBURA：`Brute` / `ArmoredBrute` 的第二、三个调用点

**需求**（2026-09-23 用户追加）：豺狼暴徒的**战续额外血量（狂暴护盾）耗尽之后**，依旧可以触发 GEBURA
的无敌；**矮人尸群保持不触发**。

**为什么不能只靠 `Char.damage` 那一处**：战续**期间**这只怪靠覆写 `isAlive()` 撑着（护盾 > 0 就算活着），
所以任何致死伤害在闸门处问 `ch.isAlive()` 都得到 true ⇒ **闸门不介入**（这正是「狂暴流程与护盾时长分毫
不动」的原因，也是要保留的性质）。真正的死亡发生在 **`BruteRage.act()` 里那句 `target.die(null)`** ——
它是 buff 自己的回合推到尽头后主动调用的，**不经过 `Char.damage`**。

**改法**：把同一个钩子接在**该路径最靠后的位置**（紧贴原版那句 `die(null)` 之前），并按返回值决定是否落地：

```java
if (shielding() <= 0){
    //接管时跳过 die(null)：血停在 0、进入无敌，等计时结束由 GeburaGrace 用原始来源调 die()
    if (!Trials.interceptLethalDamage( target, null )){
        target.die(null);
    }
}
```

三个必须讲清的点：

1. **闸门改成返回 `boolean`（是否接管）**，而不是让调用方「回头再看一眼 `isAlive()`」。
   `Char.damage` 那侧不看返回值（紧随其后的 `if (!isAlive()) die(src)` 本来就是原版的落地语句）；
   `BruteRage` 这侧**必须看** —— 它没有那句兜底。用返回值还避开了与 `Char.deathMarked`（「血量归零但
   仍算活着」）之类的纠缠：**每一处提前返回都精确对应「原版那句 `die()` 该照常执行」**。
   验证：闸门里 `return true` 有且仅有 1 个（真接管），其余 **7** 处提前返回全是 `return false`。
2. **无敌期间不会「一边无敌一边回盾」**：`BruteRage` 继承的 `ShieldBuff.absorbDamage` 在护盾归零时会
   `detach()`（`detachesAtZero` 默认 true），所以走到那句 `die(null)` 时 buff **已经自行摘掉**。
   即便没摘，`Brute.isAlive()` 也会因 `geburaGrace` 为真而在 `super.isAlive()` 处短路、不会再走到
   `triggerEnrage`。
3. **装甲暴徒必须单独接**：`ArmoredRage extends Brute.BruteRage` 但**覆写了 `act()`**（消耗 `3*TICK`、掉甲），
   所以**不会**继承到 `BruteRage` 里那个调用点。它同属「最顶级的豺狼暴徒」（游戏内描述原文），
   同一个战续机制必须同样待遇，否则同族两级怪会出现「一个吃无敌一个不吃」。核验脚本对这一点有专门的
   反向断言（若将来有人删掉 `ArmoredRage.act()` 的覆写，会立刻 FAIL 提醒复核）。
4. **致死来源仍传 `null`**：原版战续结束本来就是 `die(null)`（不归属任何一次攻击），沿用同一口径 ⇒
   **不臆造击杀归属**、经验/掉落/统计与原版逐字一致。探针对此有硬断言。
5. **关掉 GEBURA 时与原版逐字节一致**：`interceptLethalDamage` 在未开启时第一行就 `return false`，
   于是 `target.die(null)` 照常执行 —— 探针用**真 `Brute` 类**做了这条反向断言。

## 8.7 为什么 GEBURA / CHESED 都不碰随机流

`Trials` 的四个钩子（`modifyMobHT` / `bindMobPassives` / `geburaGraceTurns` / `interceptLethalDamage`）与两个
buff 的关键方法体（`act` / `attachTo` / `detach` / `storeInBundle`）里**没有一次 `Random` 调用** ⇒
**同一 seed 的关卡布局与不开这两条考验时逐字节一致**（探针 §⑦ 实测「同 seed 200 个随机数逐位相同」）。
这条是硬验收标准：考验只改效果，不改生成。

## 8.8 落点清单（8 个文件 + 2 份文本）

| 文件 | 改动 |
|---|---|
| `Trials.java` | 三个 CHESED 常量 + 四个静态钩子（`interceptLethalDamage` 返回 `boolean`）；进度注释更新为「HOKMA/BINAH/CHESED/GEBURA 已实现」 |
| `actors/mobs/Mob.java` | `onAdd()` 里夹 `modifyMobHT`、守卫之外重挂 `bindMobPassives`；新增 `public boolean geburaGrace` / `geburaUsed`；新增/覆写 `isAlive()` / `isInvulnerable(Class)` / `deathIsDeferred(Object)` |
| `actors/Char.java` | 致死闸门插入 `Trials.interceptLethalDamage( this, src );`（`checkBypass` 之后、`if(!isAlive())` 之前；返回值刻意不接） |
| `actors/mobs/Ghoul.java` | 新增 `deathIsDeferred()`（与 `die()` 分支**同源**）；`die()` 改为先问 `deathIsDeferred(cause)` |
| `actors/mobs/Brute.java` | 新增 `import Trials`；`BruteRage.act()` 里那句 `target.die(null)` 改为「先问闸门，未被接管才落地」（战续结束收口，见 §8.5.2） |
| `actors/mobs/ArmoredBrute.java` | 同上（`ArmoredRage` **覆写了 `act()`**，必须单独接） |
| `actors/buffs/ChesedMend.java`（新） | 常驻 POSITIVE buff：满血清计时、每 5 回合按 HT 现算回 10% |
| `actors/buffs/GeburaGrace.java`（新） | 濒死无敌 buff：维护 `geburaGrace` 缓存、数满回合后 `die(原始来源)`、`turns_left`/`source_class` 存档 |
| `misc_zh.properties` / `misc.properties` | `trials.chesed_desc` / `trials.gebura_desc` 从占位换成正式文案（字面 `\n`、`_` 成对） |

## 8.9 核验（三层证据链）

- **`_chk/verify_chesed_gebura.py` —— 五层核验**（含 `--selftest`），当前 **129 条断言全绿**：
  ① **源码结构**：`CHESED == 8` / `GEBURA == 16`、三个常量值、**三个调用点的「全仓唯一清单」逐字吻合**
  （`Char.java` + `ArmoredBrute.java` + `Brute.java`，多一处少一处都是回归）、`interceptLethalDamage` 签名
  是 `public static boolean` 且 **`return true` 恰好 1 个 / `return false` 恰好 7 个**、
  `onAdd` 四步顺序比位置、`Char.damage` 四处顺序比位置、**`BruteRage`/`ArmoredRage` 里 `die(null)` 被
  `if (!interceptLethalDamage(...))` 包住**且钩子在 `die` 之前、`Mob` 三钩子语义、`Ghoul` 让路同源、
  两个 buff 的语义与「不写死回血数值」、四个钩子不碰 `Random`、GEBURA 只认 ENEMY / 一次性 / 让路 / 深渊例外；
  ② **`javap -p` 签名**（`Mob` 的 `public boolean geburaGrace` 字段、三个方法、两个 buff 继承 `Buff`、
  `BruteRage extends ShieldBuff` / `ArmoredRage extends Brute$BruteRage` 继承链未改）；
  ③ **`javap -c` 字节码**：`checkBypass` 偏移 < GEBURA 偏移 < **其后第一条** `isAlive` 死亡判定 ⇒
  「靠后」属实；`Mob.isAlive()` 用 `ifne` **短路**（不是 `ior`）且读 `geburaGrace`；
  `GeburaGrace.act()` 的 `isAlive → detach → die` 偏移递增；`Brute$BruteRage.act()`(@23→@28) 与
  `ArmoredBrute$ArmoredRage.act()`(@21→@26) 里**闸门都先于 die**；相关方法体**无** `com/watabou/utils/Random`；
  ④ **存档层**：两个 buff 的往返字段、`bundle.contains(...)` 旧档守卫、两个瞬态标记**不得**进存档、
  `Mob.restoreFromBundle` 置 `firstAdded = false`；
  ⑤ **行为层**：读探针输出，要求「失败 = 0」且六段全覆盖。
  `--selftest` 用**合成源码**证明「比位置」与「无 Random」两个判据**不恒真**（顺序颠倒 / 锚点缺失 /
  偷用 `Random` 都能被打出来，且字符串里的 `"Random.Int(3)"` 不被误伤）。
- **`_chk/ChesedGeburaProbe.java` —— 行为真值表探针**（脱离游戏真跑真类，**71 条断言**）：
  ① +25% 真值（8→10、40→50、10→13 四舍五入、1→1、ALLY 与关闭时无操作）；
  ② 满血不计时 / 第 5 回合回 10% / 回完重新数 / **满血打断计时** / 不溢出 / 关闭与友方无操作；
  ③ 致死闸门（介入后 `isAlive` / `isActive` / `isInvulnerable` 为真、血停在 0、`geburaUsed` 置位）；
  ④ 数满 `EXP` 回合才倒（EXP=5→第 5 回合、EXP=7→第 7 回合、带**原始致死来源**、一次性、读档重建缓存与剩余回合）；
  ⑤ 门控与战续让路（关闭 / ALLY / EXP=0 不介入；**Brute 同型 stub** 不经闸门；**Ghoul 孤身豁免** vs
  **身边有同类则让路**）；⑥ **真 `Brute` / `ArmoredBrute` 类**端到端（见 §8.5.2）；
  ⑦ 不消耗随机数。
  §⑥ 的具体断言：狂暴中闸门返回 false 且**不消耗豁免** → 护盾耗尽时 buff 已自行摘除 →
  **未被立即 `die(null)`**、`geburaUsed` 置位、`GeburaGrace` 已挂、`isAlive` 为真、血停在 0、免疫伤害 →
  数满 EXP=8 个回合才倒且**致死来源仍是 `null`** → 关闭 GEBURA 时「立即 `die(null)`、无无敌」的原版保真 →
  装甲暴徒（覆写了 `act()`）同样被接管。
  脱离游戏的办法：`Unsafe.allocateInstance` 造一个只有 `pit[]` 与宽高的空关卡，避开 `Gdx.files` 贴图初始化；
  `ProbeBrute` 用反射把 `hasRaged` 置位，绕开真 `triggerEnrage` 里的 `sprite.showStatusWithIcon`。
- **`_chk/TrialsLocCheck.java`** —— 断言 261 → **302 条**：新增 §⑨ 校验两位号、`NAME_IDS[3]/[4]`、三个常量值、
  **反射**读 `Mob.geburaGrace` 是 `public boolean` 且 `Mob.deathIsDeferred(Object)` 存在（**只有真编译产物
  答得出来**，与源码层断言互为交叉验证）、两条 desc 解析出真换行 / 数值齐全 / 不再是占位文案。
- **`_chk/fix_chesed_gebura_text.py`** —— **幂等**文本补丁，**字节级**读写 + CRLF 计数断言
  （`.properties` 是 CRLF，用文本模式写会把整文件变 LF）。

## 8.10 ⚠️ CHESED 的**副作用**：矮人国王二阶段卡死（2026-09-24 修）

**现象**（用户报）：四条新考验全开时，第四个 Boss 矮人国王**二阶段卡死、不继续出怪**。

**根因（纯算术，不是随机、不是时序）**：二阶段的「王座屏障」是**一套写死绝对量的刻度**，三者靠恒等式咬合——

| 项 | 原版写法（常规 / 强化挑战） | HT=300 时的取值 |
|---|---|---|
| 屏障满值 | `setShield( HT )` | 300 |
| 每击杀一名仆从削减 | `HT / 12`（强化 `HT / 18`） | 25 |
| 第二波阈值 | `shielding() <= 200`（强化 300） | 200 = 300×2/3 |
| 第三波阈值 | `shielding() <= 100`（强化 150） | 100 = 300×1/3 |

⇒「第 4 次击杀放第二波、第 8 次放第三波、第 12 次打空进三阶段」这个节奏，**唯一前提是 HT 能被 12（强化 18）整除**。
CHESED 给敌方生命上限 +25%（`Trials.CHESED_HP_MULT`）把 HT 变成 **375**，整除被破坏，而二阶段的国王
**除 `KingDamager` 外免疫一切伤害**（`DwarfKing.isInvulnerable` 在 phase 2 恒真）、波次又被绝对阈值卡住 ⇒ **死局**：

- 12 次击杀只能削掉 `12×(375/12) = 372` ⇒ 屏障**永远剩 3 点**，三阶段永远进不去；
- 第 4 次击杀后屏障剩 **251 > 200** ⇒ **第二波永远不出**，玩家无解可打（`DKBarrior` 的 `incShield()` 与
  `Barrier.act()` 的衰减正好互相抵消 ⇒ 也不会靠回合数自然磨掉）。

**修法**（`actors/mobs/DwarfKing.java`，把两条刻度改成**从 HT 现算**）：

| 新刻度 | 算式 | HT=300 / 450（原版） | HT=375（CHESED） | HT=563（CHESED×强化） |
|---|---|---|---|---|
| 击杀次数 | `barrierKills()` = 强化 `18` / 常规 `12` | 12 / 18 | 12 | 18 |
| 削减量 | `barrierChip()` = **`ceil(HT / kills)`** | 25 / 25 | 32 | 32 |
| 波次阈值 | `barrierThreshold(n)` = `HT × n / 3` | 200/100 · 300/150 | 250 / 125 | 375 / 187 |

两条不变量由此对**任意 HT** 成立：① `kills × chip ≥ HT` ⇒ kills 次击杀必定打空；
② 阈值恰好落在「第 kills/3、第 2×kills/3 次击杀」上。**HT = 300 / 450 时新算式与原字面量逐个相同**
（25 / 200 / 100 / 300 / 150）⇒ **不开本考验时分毫不变**。

- 落点：`DwarfKing.java` 一处 —— 新增三个 private 助手（`barrierKills` / `barrierChip` / `barrierThreshold`）、
  四处波次阈值改走 `barrierThreshold( 2 | 1 )`（`summonsMade` 守卫原样保留）、
  `KingDamager.detach()` 与 `Summoning` 被挡分支的削减量改走 `king.barrierChip()`
  （原来两处各自带一遍 `isChallenged(...) ? 18 : 12`）。**不新增字段、不碰 `Random`**。
- 核验：`_chk/verify_dwarfking_chesed.py`（**33 条断言**）＋ `_chk/DwarfKingBarrierProbe.java`（**31 条断言**）：
  - 结构层 —— 无残留绝对 `shielding()` 阈值、四处阈值 num↔`summonsMade` 配对、`barrierChip()` 恰好被调 3 处、
    旧的 `HT/18`、`HT/12` 裸算式彻底退场、三条助手不含 `Random`；
  - **算术模拟层** —— 把波次状态机复刻成纯函数，`HT∈[120,1200]×2 模式`（2162 例）全部能进三阶段、
    屏障收尾为 0、不多出怪；真实可达 HT（250/300/375/450/563/600）**恰好 kills 次击杀**、波次@4/8（常规）与 @6/12（强化）；
    **旧写法 @HT=375 判卡死**、@HT=300/450 判通关（证明模拟器不恒真也不恒假）；
  - **探针层**（脱离游戏真跑真类、反射调 private）—— 三个算式在 HT=300/450 上与原字面量逐个相同；
    `barrierKills()` 读的是**调用时**的挑战位；`HT∈[1,2000]×2` 的不变量零反例；
    **真跑 `act()`** 用 `summonsMade` 是否推进判断走了哪条分支：常规 shield=247 ≤ 250 放行 / 251 > 250 拦住、
    shield=126 > 125 拦住，强化 shield=370 ≤ 375 放行 / 380 > 375 拦住、187 放行 / 188 拦住 —— 两侧都验；
    穿插算式与 `act()` 后同 seed 随机序列逐位相同。
    探针只驱动 phase 2，且**避开喊话点**（`summonsMade == 0/4/6/12`）—— 那几处会 `sprite.centerEmitter()` +
    `Sample.INSTANCE.play(...)`，脱离 Gdx 必 NPE；非强化挑战的第三波分支**无条件**碰精灵，故只验「拦住」那一侧。
  - `--selftest` 四个反例：阈值改回绝对字面量 / 削减量改回向下取整 / 阈值幅度写成 1/4 / 少改一处 ⇒ 必须判失败。

**⚠️ 通用红线（写给以后的考验）**：凡是**改敌方 HT** 的新效果（乘倍率、加减常数、封顶），都要回查一遍
**用绝对数字表达 HT 刻度**的 Boss。本节修完 DwarfKing 之后**又按这条红线把全仓复查了一遍**，
结论与处置见下一节 **§8.11**（`CrystalSpire` 的 `2*HT/3f` 本来就是比例式、安全；另有 `YogDzewa`、
`DwarfKing` 一阶段转二阶段、`SmilingCorpseMountain` 三处绝对刻度一并收口）。

## 8.11 全仓「绝对 HT 刻度」复查与收口（2026-09-24）

用户要求：修完矮人国王之后，**顺便通查其余「有阶段转换」的 Boss**，凡阶段依赖**写死绝对生命值**的，
一律改成「从 HT 现算」。复查方式：`grep -rn "HP [<>=]{1,2} [0-9]"` + `grep "HT - [0-9]|[0-9]{3}"` +
逐个读 `Property.BOSS` 单位的 `damage()` / `act()` / 分段函数，最后按下面这张表定处置。

**判据**：改动必须满足「**在不改 HT 的原版数值下，新算式与原写死值逐个相同**」⇒ 不开考验时分毫不变。

| 单位 | 原写死的绝对刻度 | HT 基数 | 会不会卡死 | 处置 |
|---|---|---|---|---|
| `DwarfKing` 二阶段屏障 | 削减 `HT/12`（强化 18）、阈值 200/100（强化 300/150） | 300 / 450 | **会（真死局）** | 见 §8.10，已改 |
| `DwarfKing` 一阶段转二阶段 | 入场血量 `50`（强化 `100`） | 300 / 450 | 不会（血一定能掉到 50） | **已改**：普通 `HT/6`、强化 `HT×2/9` |
| `YogDzewa` | 阶段推进量 `300`、末段下限 `100`、光束伤害刻度 `400` | 1000 | 不会（四个阈值仍严格递减） | **已改**：`HT×3/10`、`HT/10`、`HT×2/5` |
| `SmilingCorpseMountain` | 换皮阈值 `2000` / `1000` | 3000 | 不会（纯外观） | **已改**：`HT×2/3`、`HT/3` |
| `DwarfKing` 三阶段「奄奄一息」台词 | `HP < 20` | 300 / 450 | 不会（只是台词） | **刻意保留**，理由见下 |
| `GnollGeomancer` `RockArmor` | `setShield(25)` | 150 | 不会 | **刻意保留**：固定 25 点护甲，本就不是 HT 刻度 |
| `Goo` / `CrystalSpire` / `YogFist` / `Brute` / `ArmoredBrute` / `MeltingLove` / `CrystalGuardian` | —— | —— | 不会 | 本来就是相对量（`HP*2 <= HT`、`2*HT/3f`、`HT/2`），无需改 |
| `DM300` | —— | 300 / 400 | 不会 | 本来就是 HT 派生（`HT/4*(3-p)`、`HT/3*(2-p)`），无需改 |
| `Tengu` | —— | 200 / 250 | 不会 | 本来就是 HT 派生（`HT/8`、`HT/2`），无需改 |

**YogDzewa 的具体账**（最接近矮人国王的一处）：它把「每推进一个阶段要打掉多少血」写死成 300，
末段生命下限写死成 100，光束数里的伤害刻度写死成 400 —— 三者恰好是 HT=1000 的 3/10、1/10、2/5。
CHESED 把 HT 抬到 1250 后，四段掉血量从 `300/300/300/100`（24%/24%/24%/10%）漂成
`300/300/300/350`（24%/24%/24%/28%）——**末段被拉长 3.5 倍**，虽然不会卡死，但节奏与设计意图脱钩。
改后为 `375/375/375/125`（30%/30%/30%/10%），等比结构恢复。

**为什么两处绝对量刻意保留**：

- `DwarfKing` 的 `HP < 20`「奄奄一息」喊话：**原版在两个难度下用的是同一个常数**，没有任何**单一比例**
  能同时还原（20 在 HT=300 与 450 上并非同比例，硬凑就得给普通/强化各配一个任意分母），
  且它不参与任何阶段推进/无敌/出怪判定 ⇒ 改了只会**破坏原版行为**却换不到任何鲁棒性。
- `GnollGeomancer` 的 `RockArmor.setShield(25)`：固定 25 点岩石护甲与 HT 无关，**本就不是 HT 刻度**。

- 落点（3 个文件，均**不新增字段、不碰 `Random`**）：
  - `YogDzewa.java` —— 新增 4 个常量（`PHASE_STEP_NUM/DEN`、`BEAM_STEP_NUM/DEN`）与 3 个 private 助手
    （`phaseStep()` / `finalPhaseFloor()` / `beamStep()`），5 处调用点全部改走助手；
  - `DwarfKing.java` —— 新增 `phase2EntryHP()`，`damage()` 里那对三元的判据与赋值各改一处，
    并给保留的 `20` 补了一段「为什么刻意保留」的注释；
  - `SmilingCorpseMountain.java` —— `phase()` 两个阈值改 `HT×2/3` / `HT/3`，类注释与 javadoc 同步改口径。
- 核验（两层，共 **75 条断言**）：
  - `_chk/verify_boss_phase_scales.py`（**49 条 + 20 条反例自测**）：① 结构层（新刻度在、旧绝对数字
    剥注释后彻底退场、助手定义/调用次数、助手体不含 `Random`）；② 算术层（HT=1000 时 YogDzewa 三个
    算式与原写死值逐个相同、HT=1250 时四段窗口为 375/375/375/125 而旧写法是 300/300/300/350、
    `HT∈[120,3000]` 下限严格递减、SCM 在 0..3000 每个 HP 上新旧判据完全一致）；③ 阶段机仿真
    （`HT∈[600,2000]` 都能走完 3 次推进到 phase 4、阶段数恒为 4）。
  - `_chk/BossPhaseScaleProbe.java`（**26 条断言**，脱离游戏真 `new` 出三个 Boss、反射调 private）：
    真方法返回值与手算一致；`phase2EntryHP()` 读的是**调用时**的挑战位（同一实例切位立即变）；
    `HT∈[200,3000]` 共 2801 个取值不变量零反例；SCM 的 `phase()` 逐点等价。
  - 两处「刻意保留」也各有一条断言钉住（防「热心改成一律 HT 化」）。
- 顺带更新：`Trials.java` 的 CHESED 注释块里那段「全仓已扫过」的结论**换成这次的完整清单**
  （原结论只扫了「绝对数字」一种形态，漏了 YogDzewa 那类「绝对步长」）。

## 8.12 GEBURA 削弱（2026-09-24）：锁血那一击的**僵直一回合**

**需求**（用户原文）：「触发 GEBURA 的锁血后，怪物等待一回合不行动，随后才正常行动
（即刚触发锁血的那回合不会立刻行动）」。

**为什么该削**：原实现里被锁血的怪「血量停在 0、免疫伤害」但**照常出手** ⇒ 玩家把怪打进濒死后
反而立刻吃它一整套攻击，体感是「打空血＝自杀」。现在它要先愣一回合（无敌期间总出手次数 −1）。

**落点（三个，代码本身约 10 行，其余是解释性注释）**：

| 落点 | 改动 |
|---|---|
| `Mob.java` 字段区 | 新增第三个 GEBURA 瞬态标记 `public boolean geburaStagger`（**不进存档**） |
| `Mob.java` 新方法 | `public boolean consumeGeburaStagger()` —— 唯一消费点：没标记返 false；有则**先清标记**再返 true |
| `Mob.java` `act()` | 在 `paralysed` 分支**之后**插入 `if (consumeGeburaStagger()) { spend( TICK ); return true; }` |
| `Trials.interceptLethalDamage` | 发下豁免的同一处置上 `mob.geburaStagger = true;`（顺序：门闩 → 僵直 → 挂 buff） |

**四个必须讲清的点**：

1. **为什么是「下一次行动机会」而不是「扣掉这一刻的出手」**：闸门是在**别人的回合**里被调到的
   （英雄的攻击结算 / `BruteRage` 的战续结算），那一刻的「这一回合」并不属于它。
2. **为什么不改成动时间轴**（例如 `Actor.delayChar`）：那会把 `GeburaGrace` 的**计时一起推后**
   ⇒ 无敌期间照样出手、总出手次数不变，**等于没削**。记成「下一次行动机会空过」既符合用户口径，
   也不受调度顺序影响（`Actor.now` 是「当前行动者的时间」，不是单调递增的墙钟）。
3. **形态刻意抄 `paralysed` / `FeintConfusion`**（`Mob.act()` 里已有的两处「本回合不行动」）：
   都是 `spend( TICK )` 后直接 `return true`。差别只有两点 —— 标记是**一次性**的；
   **不动 `enemySeen`**（它只是愣住了，不是失去目标；`paralysed` 那句 `enemySeen = false` 是瘫痪自己的语义）。
   位置放在 `paralysed` **之后**：瘫痪期间本来就不能行动，僵直记在账上，等它真能动的那一回合再兑现。
   ⚠️ 若挪到 `chooseEnemy()` / `state.act()` **之后**，「这一回合不行动」就形同虚设 —— 字节码层专门钉了这条。
4. **僵直与豁免是两件事**：豁免能跨存档（`GeburaGrace` 存 `turns_left` / `source_class`），
   僵直是**一次性瞬态、刻意不进存档**（同一帧内挂上、下次行动就消费掉；真读档时那一次僵直早已兑现，
   留着反而会平白多愣一回合）。豁免计时**照常走**，所以「少打一下」就是削弱本身。
5. **作用边界**：消费点在 `Mob.act()` 里 ⇒ **自己写 `act()` 却不调 `super.act()` 的单位绕过它**，
   不会僵直。完整清单见下。

**作用边界（谁绕过 `Mob.act()`）** —— 这是**上游自带的同类边界**，不是本次引入的缺陷：
原版 `YogDzewa.act()` 自己重写了一遍 char/mob 流程、里面**压根没有 `paralysed` 那一句** ⇒ 它连瘫痪都不吃。
2026-09-24 与用户确认口径：**接受为既定边界，不为它开特判**（开特判＝在剧本化 Boss 代码里插一句
只为一回合服务的逻辑，还会把「唯一消费点」变成两处）。全仓清单已钉进核验脚本的第 ⑥ 层：

| 覆写 `act()` 且**不调** `super.act()` 的单位 | 能不能吃到 GEBURA |
|---|---|
| `YogDzewa`（Mob，**ENEMY，EXP=50**） | **能吃到豁免、吃不到僵直** ← 全库唯一的真边界 |
| `Pylon`（Mob，入场 NEUTRAL、`activate()` 后转 ENEMY、EXP 取 `Mob` 默认 1） | 理论上有 1 回合豁免；只 1 回合、且 NEUTRAL 期间本就无敌，影响可忽略 |
| `CrystalSpire`（NEUTRAL，EXP=20）／ `Masterpiece`（NEUTRAL，EXP=0）／ `DecoyDoll`（ALLY，EXP=0） | 否（闸门第一关「只认 ENEMY」或 `EXP<=0` 就挡掉） |
| `Char`（`act()` 原型基类）／ `MobSpawner`／ `Pushing`／ `Swap`（都是 `Actor`） | 否（不是 `Char`，谈不上 `EXP`） |
| `npcs/Sheep` ／ `npcs/VaultLaser` ／ `npcs/VaultSentry`（都 extends `NPC`） | 否（`NPC` 父类统一 `NEUTRAL` + `EXP=0`） |

> 正面样本：转阶段 Boss `DM300` **调了** `super.act()` ⇒ 它吃得到僵直。
> 所以边界不是「凡是 Boss 都没份」，而是精确的「有没有经过 `Mob.act()`」。
> ⚠️ 分类是**人肉核对**的：阵营 / `EXP` 大量来自**父类实例块**（`NPC`）与**运行时改写**（`Pylon.activate()`），
> 靠正则推只会推出错的结论。

**核验**（并入 CHESED/GEBURA 那套三层证据链，见 §8.9）：

- `_chk/verify_chesed_gebura.py`：源码层（字段可见性、`consumeGeburaStagger` 的「先清后返回」顺序、
  消费点在 `paralysed` 之后与 `chooseEnemy()`/`state.act()` **之前**、`Trials` 置位且**只置一处**、
  不碰 `Random`、**不进存档**）+ 字节码层（`consumeGeburaStagger`(@36) < `chooseEnemy`(@57) <
  `AiState.act`(@97)；消费调用之后紧跟一条 `spend`；`putfield` 清标记(@7) 早于 `iconst_1`(@8)）
  + **第 ⑥ 边界层**（12 个 `act()` 绕过者清单**逐字吻合**；`Mob` / `DM300` 落在「调了 `super.act()`」
  一侧 ⇒ 消费点可达、且有正面样本；`YogDzewa` 确为 ENEMY + EXP=50 ⇒ 边界属实）。
  现 **155 条断言全绿**；`--selftest` 新增三组反例（「消费点后置」「忘了清标记」「只认顶格 `act()`、
  不被匿名内部类骗」必须被抓住）。
- `_chk/ChesedGeburaProbe.java` §⑧（**12 条**；整份探针 **83 条 ALL PASS**）：置位、**问一次即清**、
  之后永远 false、关闭/盟友/`EXP=0` 不置位、战续路径同样置位且同为一次性、
  僵直不影响豁免（空过一回合后无敌仍在、血仍停在 0）。
- `_chk/TrialsLocCheck.java`：**308 条 ALL PASS**（新增反射读 `Mob.geburaStagger` 是 `public boolean`、
  `consumeGeburaStagger()` 是 public 无参 boolean —— 只有真编译产物答得出来；当时还断言「两条 desc 必须写明
  「僵直 / stagger」，且旧口径「仍会照常行动 / keep taking their turns as usual.」已退场」——
  ⚠️ 「必须写明」这条**同日稍晚已作废**，见下方「玩家文案」的补记）。

**玩家文案**（`_chk/fix_gebura_stagger_text.py`，字节级 + 幂等 + CRLF 计数不变）：
zh 第 2 段改为「…并且照常行动——唯独刚被打成濒死的那一回合它会_僵直不动_：愣过一个回合之后才恢复正常。」；
en 同步（`-- except for the very turn they are stricken down…`）。
⚠️ 该脚本踩了个小坑：**`trials.gebura_desc=` 这个键名自带一个下划线**，拿整行去数 `_` 会永远奇数（假 FAIL）
⇒ 只数「值」部分，并逐段（`\n\n` 分隔）各查一次成对。

> ⚠️ **补记（同日稍晚，v0.3.4 打包之后）**：用户**手改**了考验文案 —— 中文 `binah`/`chesed`/`gebura_desc`
> 全部改成**单段精简版**，其中 `gebura_desc` **去掉了「僵直」这句**。按用户指示「以中文为基准、不要改动中文」，
> 英文侧已由 `_chk/fix_trials_en_align_zh.py` 同步为**同结构的单段 1:1 英译**。
> 后果：①「僵直」机制在游戏内文案里**不再有说明**（**代码效果完好**，`Mob.geburaStagger` 那条链没动）；
> ② v0.3.4 APK 里仍是旧版多段文案 ⇒ **本仓库源码领先于已发布的 v0.3.4**（下次发版会带上新文案）。
> 核验脚本随之把旧判据「`desc` 必须解析出真换行」换成更普适的两条：`[TRUNC]` 文件级截断守卫
> （每个 properties 的非空非注释行都必须含 `=` 或 `:`）+ `[SHAPE]` zh/en 段落结构一致；
> `[RULE]` 改为「僵直 / stagger 必须两侧**同时出现或同时不出现**」。反例自测 `_chk/TrialTextGuardProbe.java`。

> 未做：本版**不升版本号、不加改动栏**（用户未要求发版）。若要出包，按 `egopd-release-package` 的流程
> 在 `ui/changelist/EGOPD_Changes.java` 里补一条「考验 GEBURA：锁血那一回合僵直不动」。

# 2026-09-24 挑战「水仙追迹」开局发放改为**放背包**

**需求**（用户）：开局获取的水仙十字圣剑改为**放进背包**，不再替换初始武器装备。

**改动**（`actors/hero/HeroClass.java`，`initHero()` 里的 `NARCISSUS_TRACING` 分支）：

| 项 | 旧写法（v0.3.2） | 新写法（v0.3.3） |
|---|---|---|
| 圣剑 | `sword.identify(); hero.belongings.weapon = sword; sword.activate( hero );` | `new NarcissusCrossSword().identify().collect();` |
| 快捷栏 | `Dungeon.quickslot.setSlot(0, sword)` | **不动**（槽位留给该职业自己的初始快捷项） |
| 财富戒指 | `wealth.identify(); wealth.collect();` | `new RingOfWealth().identify().collect();`（不变） |
| 与「拆迁办」的关系 | 同为「替换初始武器」，靠**代码顺序**让水仙追迹胜出 | 互不干扰（一个改 `belongings.weapon`、一个只 `collect()`），顺序不再构成语义 |

**两条容易踩的点**：

- **不调 `activate()`**：形态看护 buff（`NarcissusCrossSwordBase.FormKeeper`）由**装备**流程挂上
  （`KindOfWeapon.doEquip` → `activate()`）。剑此刻还在背包里，提前挂会因 `held() == null`
  在英雄下一回合自我回收 —— 纯空转。玩家把剑装上主手后，看护 buff 会在下一次 `act()` 依
  「英雄等级 ↔ 形态」把常态变形成「芒性」（开局 1 级），与原行为一致。
- **不占快捷栏**：剑在背包里就不是「已装备」，占 0 号槽会把该职业自己的初始快捷项挤走；
  旧写法还连带把下方「水袋进快捷栏」那段的落点顶到 1 号槽。

**文本同步**（`misc.properties` / `misc_zh.properties` 的 `challenges.narcissus_tracing_desc`）：
「已装备在主手」→「放在背包里，装不装上主手随你」；英文 `(equipped)` → `in your backpack`。
补丁 `_chk/fix_narcissus_backpack_text.py`（幂等、字节级、断言 CRLF 计数不变、旧片段必须退场）。

**核验**：

- `_chk/verify_challenge_narcissus.py` —— ③ 段整体重写：发放块里必须**不含**（剥注释后）
  `belongings.weapon` / `activate(` / `quickslot`，且必须有两条 `.identify().collect()`；
  「块必须在拆迁办之后」这条顺序断言**撤掉**（已无语义），改断「两条发放块都在 `initHero` 里」。
- `_chk/ChallengesLocCheck.java`（真 `Properties.load` + 真 `String.format`）—— 新增 4 条：zh/en
  都写明「放在背包里」，且旧说法「已装备在主手」/`(equipped)` 已彻底退场。共 **93 条断言 ALL PASS**。

# 2026-09-24 出 v0.3.3（矮人国王二阶段卡死修复 + 全仓 Boss 阶段刻度复查 + 水仙追迹圣剑改放背包）

## 一、本版内容

- **版本号**：`appVersionCode 932 → 933`、`appVersionName 0.3.2 → 0.3.3`（根 `build.gradle` 的 `ext`，唯一处）。
- **更新日志**：`ui/changelist/EGOPD_Changes.java` 新增 major 条目 `EGOPD v0.3.3`（排在 v0.3.2 **之前**），下挂 3 条：

  | 按钮 | 图标 | 要点 |
  | --- | --- | --- |
  | 修复：矮人国王二阶段卡死 | `Icons.get(Icons.CHALLENGE_COLOR)` | 详见 §8.10 —— CHESED 的 `HT×1.25` 破坏「屏障恰为 12 的倍数」的整除算术 |
  | 全仓复查：其余 Boss 的阶段刻度 | `Icons.get(Icons.SHPX)` | 详见 §8.11 —— YogDzewa / 矮人国王一→二 / 微笑的尸山 三处绝对刻度改 HT 派生 |
  | 水仙追迹：圣剑改为放背包 | `ItemSprite(ItemSpriteSheet.NARCISSUS_CROSS_SWORD)` | 详见下一节 —— 不再替换初始武器、不再占快捷栏 |

  补丁 `_chk/add_v033_changelog.py`（幂等、**按目标文件换行风格** 取 `sep`、缩进对齐 v0.3.2 的「语句 2 tab / 续行 4 tab」）。`ChangesScene` **无需改动**（未新增页签）。
- **§8.11 与「水仙追迹」两节正文**见上文（本版改动全在那两节里，此处只记发版流程与核验）。

## 二、核验（源码级 + 打包后四道）

- `_chk/check_utf8_all.py`：**1456 文件**全合法 UTF-8；`_chk/check_unused_imports.py` ALL PASS。
- 单文件 `javac`：6 个改动 `.java` 全部 **EXIT=0**，告警只有既有的 rawtypes/unchecked（`YogDzewa` 12 条、`DwarfKing` 3 条），**均不在改动块内**。
- `_chk/verify_boss_phase_scales.py` **49 条 + 20 条反例自测** ALL PASS；`_chk/BossPhaseScaleProbe.java` **26 条断言** ALL PASS（真 `new` 实例 + 反射调 private）；`_chk/verify_challenge_narcissus.py` 全绿；`_chk/ChallengesLocCheck.java` **93 条** ALL PASS。
- **打包**：`:android:assembleDebug` → **BUILD SUCCESSFUL in 46s**；`:core:compileJava` 与 `:android:packageDebug` 均 **executed**（非 UP-TO-DATE）⇒ 改动确实进包。
  ⚠️ 必须带环境变量，否则报 `Gradle requires JVM 17 or later ... currently configured to use JVM 8`（`./gradlew` 走的默认 JVM 是 8）：
  `JAVA_HOME=D:/PD/tools/jdk-21.0.12.1+1`、`GRADLE_USER_HOME=D:/PD/.gradle`。
- **① 元数据**（`aapt dump badging`）：`versionCode='933'`、`versionName='0.3.3-INDEV'`、`application-label='EGOPD'`、`application-icon-480/640` 均指向 `res/mipmap-anydpi-v26/ic_launcher.xml`。
- **② 新类/新文本进包**（dex 字面量计数）：`EGOPD_Changes` ×3、`EGOPD v0.3.3` ×1、`修复：矮人国王二阶段卡死` ×1、`全仓复查：其余 Boss 的阶段刻度` ×1、`水仙追迹：圣剑改为放背包` ×1；**新方法名 `phaseStep` / `beamStep` / `phase2EntryHP` 各 ×1**（证明三个 HT 派生助手真的编进去了）、`NARCISSUS_CROSS_SWORD` ×1。
- **③ 全量资产比对**（新脚本 `_chk/verify_apk_assets_033.py`）：APK 内 **541** 个 `assets/*` 与工作区 `core/src/main/assets` **md5 逐条一致**，无单边、无不一致；关键文本抽验 4 条全 OK（zh 含「放在背包里」且「已装备在主手」已退场；en 反之）。
- **④ 体积变化可解释**（新脚本 `_chk/apk_size_diff_033.py`）：APK **49,441,252 B**，对比 `EGOPD_0.3.2.APK`（49,476,010 B）**−34,758 B（−0.070%）**。逐层归因：

  | 顶层 | 新条目 | 旧条目 | 压缩字节差 |
  | --- | --- | --- | --- |
  | (root) | 7 | 7 | **+2,185**（`classes2.dex` +2,187 / `AndroidManifest.xml` −2） |
  | META-INF | 17 | 17 | +193（MANIFEST.MF +108 / CERT.SF +88 / CERT.RSA −3） |
  | assets | 541 | 538 | **−37,553** |
  | com / kotlin / lib / res | 14/8/8/60 | 同 | 0 |

  assets 那 −37,553 再拆开＝ **−45,749**（删掉 `assets/interfaces/Image_1789805146883_302 (1).png`）**+3,464**（4 张新 `*_old.png`）**+4,734**（`icons.png`/`status_pane`/`talent_button`/`menu_pane`/`toolbar` 重绘）**−2**（两条文本）。
  等式闭合：`Σ条目压缩差(−35,175) + 本地头/扩展字段(+184) + 中央目录与尾部(+233) = −34,758` ✓。
  两包**空洞均为 0**（本次没有「条目变小原地重写」⇒ 文件大小可信，不必按 §4.4 掏空洞）。
- **归档**：`EGOPD_0.3.3.APK`（仓库根），md5 `b30d2bc091e0bec8a95d6c8c99d47739`，与构建产物一致，`aapt` 复核头部正常。

## 三、资产侧顺带发现（**非本次会话改动**，仅记录以免下次误判）

对比 `EGOPD_0.3.2.APK` 与新包，工作区 `core/src/main/assets/interfaces/` 在两次打包之间发生过这些变化，全部照原样进了 v0.3.3：

1. `Image_1789805146883_302 (1).png`（45,749 B 压缩）**已从工作区删除** —— 聊天/下载命名的临时图，删掉是好事（v0.3.2 的包里含它）。
2. 新增 4 个 **`*_old.png` 备份**（`menu_pane_old` / `status_pane_old` / `talent_button_old` / `toolbar_old`），mtime 仍是检出日的 `08-23 18:48`，与被重绘的同名主体文件（`09-24 08:2x`）成对 ⇒ 用户在沿用自己既有的「改名备份原版、新图占原名」约定（早先 `banners_old.png` 就是这么来的）。
3. ⚠️ **备份图会一起进包**：`assets/**/*_old.png` 共 7 个、压缩后 **49,504 B（占包 0.10%）**，其中 `banners_old.png` 一张就 44,448 B。若这些只是留档，建议移到 `core/src/main/assets/` **之外**（如 `_chk/_bak_assets/`），可直接瘦 48 KB 左右。

## 四、待人工验证

- 游戏内「改动」界面 → EGOPD 页签：确认 v0.3.3 三条按钮的**图标与文本渲染正常**（无串行、无把两段并成一段）。
- **水仙追迹**：开该挑战开局，圣剑与财富戒指应**出现在背包里**、主手仍是本职业初始武器、快捷栏 0 号槽没被抢。
- **矮人国王**：开 CHESED 后打二阶段，血量应能正常削到 0 并出第二波（不再卡死）。
- **YogDzewa / 微笑的尸山**：不开任何考验时行为应与旧版**逐点一致**（HT 派生公式在原 HT 上等于原写死值）。
- **先卸载再安装**（同名同 versionCode 覆盖安装时 MIUI/EMUI 等会继续显示缓存的桌面图标与名称）。


# 2026-09-24 出 v0.3.4（考验 GEBURA 削弱：锁血那一回合「僵直」）

## 一、本版内容

- **版本号**：`appVersionCode 933 → 934`、`appVersionName 0.3.3 → 0.3.4`（根 `build.gradle` 的 `ext`，唯一处）。
- **改动本体**见上文 **§8.12**：`Mob` 新增瞬态标记 `geburaStagger` + `consumeGeburaStagger()`，
  `Mob.act()` 在 `paralysed` **之后**消费它，`Trials.interceptLethalDamage` 在发下豁免的同一处置位。
- **更新日志**：`ui/changelist/EGOPD_Changes.java` 新增 major 条目 `EGOPD v0.3.4`（排在 v0.3.3 **之前**），
  下挂 **1 条**按钮：

  | 按钮 | 图标 | 要点 |
  | --- | --- | --- |
  | 考验 GEBURA 削弱：锁血那一回合「僵直」 | `Icons.get(Icons.CHALLENGE_COLOR)` | 四段正文：反直觉手感 → 一回合僵直 → 覆盖范围与 YogDzewa 例外 → 不碰随机数 |

  本版只此一条，直接手工编辑写入（未另写补丁脚本）。`ChangesScene` **无需改动**（未新增页签）。

## 二、核验（源码级 + 打包后四道）

- `_chk/check_utf8_all.py`：**1456 文件**全合法 UTF-8；`_chk/check_unused_imports.py` ALL PASS。
- 单文件 `javac`（`Trials.java` / `Mob.java` / `EGOPD_Changes.java`）：**EXIT=0**；仅 `Mob.java:952`
  一条**既有**的 `[rawtypes]`（`isInvulnerable( Class effect )` 的裸 `Class` 形参，**不在改动块内**）。
- `_chk/verify_chesed_gebura.py`（现为**六层**）：**155 条断言全绿** + `--selftest` 五组反例全过。
  本版新增 **第 ⑥ 边界层**（5 条）：`act()` 绕过者清单逐字吻合（12 个）、`Mob`/`DM300` 落在「调了
  `super.act()`」一侧（消费点可达 + 正面样本）、`YogDzewa` 确为 ENEMY + EXP=50（边界属实）；
  `--selftest` 相应加了「只认顶格 `act()`、不被匿名内部类骗」一组。
- **打包**：`:android:assembleDebug` → **BUILD SUCCESSFUL in 1m**；`:core:compileJava` 与
  `:android:packageDebug` 均 **executed**（非 UP-TO-DATE）⇒ 改动确实进包。
  环境变量 `JAVA_HOME=D:/PD/tools/jdk-21.0.12.1+1`、`GRADLE_USER_HOME=D:/PD/.gradle`（缺了会报 JVM 8）。
- **① 元数据**（`aapt dump badging`）：`versionCode='934'`、`versionName='0.3.4-INDEV'`、
  `application-label='EGOPD'`、`application-icon-480/640` 均指向 `res/mipmap-anydpi-v26/ic_launcher.xml`。
- **② 新类/新文本进包**（dex 字面量计数）：`EGOPD_Changes` ×3、`EGOPD v0.3.4` ×1、
  `考验 GEBURA 削弱` ×1、`锁血那一回合` ×1、`僵直` ×2、`亚戈·德泽瓦` ×2；
  **新方法/新字段名 `consumeGeburaStagger` ×1、`geburaStagger` ×1**（证明僵直那条链真的编进去了）。
- **③ 全量资产比对**（`_chk/verify_apk_assets_034.py`）：APK 内 **541** 个 `assets/*` 与工作区
  `core/src/main/assets` **md5 逐条一致**，无单边、无不一致；本版抽验改为「僵直」文案 4 条全 OK
  （zh 含「僵直不动」且「并且仍会照常行动」已退场；en 反之）。
- **④ 体积变化可解释**（`_chk/apk_size_diff_034.py`）：APK **49,442,270 B**，对比 `EGOPD_0.3.3.APK`
  （49,441,252 B）**+1,018 B（+0.002%）**。逐层归因：

  | 顶层 | 新条目 | 旧条目 | 压缩字节差 |
  | --- | --- | --- | --- |
  | (root) | 7 | 7 | **+895**（`classes2.dex` +893 / `AndroidManifest.xml` +2） |
  | META-INF | 17 | 17 | +2（CERT.SF +1 / CERT.RSA +1） |
  | assets | 541 | 541 | **+120**（`misc.properties` +66 / `misc_zh.properties` +54） |
  | com / kotlin / lib / res | 14/8/8/60 | 同 | 0 |

  新增/删除条目均为 **0**；两包**空洞均为 0**（本次全是「条目原地变大」，无重写空洞 ⇒ 文件大小可信）。
- **归档**：`EGOPD_0.3.4.APK`（仓库根），md5 `bc0235d742fc35fc403e688bb792a731`，与构建产物一致，
  `aapt` 复核头部正常。

## 三、待人工验证

- 开 GEBURA 打任意怪：把它打进**濒死那一刻它应当当回合不动**（愣一下），下一回合才恢复行动；
  无敌计时照常走 ⇒ 无敌期总出手次数比旧版**少 1**。
- 顺带确认**关掉 GEBURA** 时行为与旧版一致（`interceptLethalDamage` 第一行就 `return false`）。
- 游戏内「改动」界面 → EGOPD 页签：确认 v0.3.4 那条按钮的图标与四段文本渲染正常（无串行、无并段）。
- **先卸载再安装**（同名同 versionCode 覆盖安装时 MIUI/EMUI 等会继续显示缓存的桌面图标与名称）。

# 2026-09-24 修两个 Bug：背叛家人者漏免攻击延迟惩罚 / 死亡证明陈列室缺「骷髅钥匙」

## 一、Bug A：背叛家人者免力量惩罚时漏掉了攻击延迟的 ×1.2ⁿ

### 现象与根因（是**时序**问题，不是算术）
「中指长兄 → 转职「背叛家人者」」在力量不足时挥莱瓦汀系列（`SealedSwordBase`）会自动预支一笔
复仇账簿充能（`FamilyBetrayal.CHARGE_COST = 5%`）换一次力量补足，本该**精准 + 攻击延迟**两处惩罚一起免。
实际只免了精准。

力量惩罚有两条**独立出口**，都读 `STRReq() - owner.STR()`：

| 出口 | 位置 | 惩罚 |
| --- | --- | --- |
| 精准 | `Weapon.accuracyFactor` | 力量差 n ⇒ `÷1.5ⁿ` |
| 攻击延迟 | `Weapon.baseDelay` | 力量差 n ⇒ `×1.2ⁿ` |

（`SealedSwordBase.ignoresStrengthPenalty` 为真时两者都跳过。）

旧实现把「摘掉补足」放在 `Talent.onHeroAttackResolved`。但该方法跑在 `Char.attack` 的**尾巴**上，
而攻击延迟要等它**之后**才被算出来：

```
Char.attack()
  → Talent.onHeroAttackStarted         (挂补足)
  → 命中 / damageRoll 结算
  → Talent.onHeroAttackResolved        (旧：在此摘补足 ← 太早)
  → Hero.onAttackComplete
  → spend( attackDelay() )             (此时才读 Hero.STR() 算延迟)
```

⇒ 摘早了，`attackDelay()` 读到的是**没补足**的 `Hero.STR()`，`baseDelay` 的 `×1.2ⁿ` 就漏了下来。

### 改法（消费点下移到 `Hero.spend(float)`）
- `actors/hero/Hero.java`：`spend(float)` 在 `super.spend(time)` + `SilentPrice.resetIdleClock()`
  之后调 `FamilyBetrayal.consumeAfterAttack(this)`。
- `actors/hero/Talent.java`：`onHeroAttackResolved` 里那句摘除**删除**（换成指向新消费点的注释）；
  `onHeroAttackStarted` 保留。
- `actors/buffs/FamilyBetrayal.java`：
  - `onAttackStarted` 在算 `need` **之前**先 `consumeAfterAttack(hero)` 摘一次**残留**——兜
    「buff 已挂、却走到早退路径、一次都没 spend」的情形；残留会垫着力量让 `need` 恒为 0 ⇒
    变成「付一次钱、永久挥得动」。
  - `consumeAfterAttack` / 类注释「生命周期」改写，指明消费点＝`Hero.spend(float)`。
  - `CHARGE_COST = 5` / `DURATION = 1f` 与 `amountOf` / `apply` 均不变。

### 为什么这是正确且最小的落点
- `Hero.spend(float)` 是**英雄所有回合成本的唯一出口**：平砍（`onAttackComplete → spend(attackDelay())`）、
  连击 / 武技（`spendAndNext(hero.attackDelay())`）、buff 驱动的一击**全部经此** ⇒ 一次改动覆盖全部攻击路径。
- 仍然**只维持一次攻击**：补足在攻击开始挂、在本击成本结算完摘。
- `spend()` 是热路径，但 `consumeAfterAttack` 没挂该 buff 时是**纯空操作**（一次 `hero.buff()` 查表）。
- **不新增字段、不碰 `Random`**。

## 二、Bug B：死亡证明（999 层陈列室）唯独缺「骷髅钥匙」

### 根因
`levels/MuseumLevel.java` 的 `skip(Class)` 原判据同时过滤了 `ClassArmor` 与 `SkeletonKey`
（`ClassArmor.class.isAssignableFrom(c) || SkeletonKey.class == c`）。但 `SkeletonKey`
**本就注册在 `Generator.Category.ARTIFACT.classes`**（神器图鉴里有一格）
⇒ 陈列室里唯一缺的就是它。

### 改法
- `skip(Class)` 收窄为**只**过滤英雄专属盔甲：`return ClassArmor.class.isAssignableFrom(c);`
- 删除因此不再使用的 `import …artifacts.SkeletonKey;`
- 更新 `createItems` 段注释与 `skip` 的 javadoc。

### 真机核验（不是只看源码）
`_chk/MuseumLocCheck.java` 反射调用 `skip(Class)` 并复刻 `createItems` 循环，逐类 `Reflection.newInstance`：
陈列数 **292**（容量 30×14＝420），`SkeletonKey` **在列**，被过滤的只剩 **6 件 `ClassArmor`**。
（无 GL 上下文时贴图类 `newInstance` 会 `NoClassDefFoundError`——按栈里是否含
`ItemSpriteSheet` / `TextureCache` / `Pixmap` 归类为「GL 依赖失败」，不计入真失败；真失败＝0。）

## 三、核验（源码级，用户约定不代跑 Gradle）
- `_chk/verify_2026-09-24.py`：**96 条断言全绿**。含 79 条静态源码断言（配「备份源喂反例」自测）、
  `javac` 告警**类别**对照（本次插行使行号位移，故按「文件 + 类别」集比对，既有 5 条告警逐字相同）、
  `javap` 字节码证据（`Hero.spend` 内 `FamilyBetrayal.consumeAfterAttack` 确在 `Char.spend` 之后；
  `onHeroAttackResolved` 已不含 `FamilyBetrayal`；`onHeroAttackStarted` 仍在调；`Hero.STR()` 仍加 `amountOf`）、
  以及真机 `MuseumLocCheck`。
- 单文件 `javac`（4 文件）EXIT=0；`_chk/check_utf8_all.py` **1456 文件 OK**；
  `_chk/check_unused_imports.py` 4 文件 **ALL PASS**。
- 补丁脚本 `_chk/patch_2026-09-24.py`（字节级 + 幂等 + 行尾归一），改前副本留 `_chk/_bak_2026-09-24/`。
- **未升版本、未打包**（用户只要求修这两处 bug；发版另议）。

## 四、待人工验证
- 用背叛家人者拿力量不足的莱瓦汀砍一刀：**精准与挥砍延迟两处惩罚应同时消失**，且只在这一击生效
  （下一击若力量仍不足会再扣一次充能）。
- 进 999 层陈列室：确认**骷髅钥匙**出现在神器那一排（拾取 / 返程流程不受影响）。

---

# 2026-09-24 新道具「洛伊德护符」+ 全仓「回血收口」到 `Char.heal(int)`（禁疗 `HealBlock`）

## 一、需求原文（用户）

> 实现一个道具：**洛伊德护符**，其效果为：**投掷生效**，投掷到任何单位上时，产生白色烟雾特效，
> 使得目标获得 **50 回合**的「禁疗」buff：**持续期间获得的所有血量恢复被强制置 0**
> （需要能够覆盖包括 **CHESED 考验效果**在内的回血效果）。禁疗 buff 持续期间，目标身上会持续散发
> 白色烟雾粒子效果。如果目标属于**宝箱怪**（宝箱怪 / 黄金宝箱怪 / 水晶宝箱怪 / 黑檀宝箱怪），
> 则额外使目标**暴露**（退出伪装状态），获得 **20 回合**的**麻痹、虚弱和致盲**。
> 描述：曾经用于捕获不死人的道具，在50回合内，能令投中的目标无法恢复生命值。\n\n
> 在命中宝箱怪时，尤其有效。\n\n白教的主神洛伊德早已无人信仰，但是捕获不死人的战斗从未停止。
>
> 炼金釜配方：**治疗药水 ×1 ＋ 液金 ×20 ＋ 3 炼金能量 → 洛伊德护符 ×6**；
> 顺带把**焦炭松脂 / 黄金松脂**系列也加入合成配方（此前没有）；
> 并修一个炼金釜 bug：**点击箭头快速套用配方时闪退**。
>
> （后续追加）贴图 `xy(7,41)`，**14×16**；**原版神器加强统一 60 → 30 脑啡肽**。

## 二、设计论证：为什么「禁疗」不能逐路径打补丁，必须先做**收口**

需求里最硬的一句是「**所有**血量恢复被强制置 0（含 CHESED 考验的回血）」。玩家要的是**语义全覆盖**。

本作的回血路径非常散：药水 / 圣草 / 食物 / 吸血附魔 / 再生 / 各类 buff / 法杖 / 法术 / 天赋 /
神器 / 怪物自我回血 / 考验 buff……**逐路径加一个 `if (禁疗) return;` 是必漏的写法**：漏一处就
「禁疗了但还在回血」，而且**将来新增的任何回血都会漏**（没人记得再补一遍）。

所以本次先做一件事：**把「把 HP 往上抬」收口到唯一入口 `Char.heal(int)`**，禁疗只在那一个地方判。

### 2.1 唯一入口 `Char.heal(int)`

```java
public int heal( int amount ) {
    if (amount <= 0 || !isAlive()) return 0;
    //「禁疗」：所有来源的回血一律置 0（含 CHESED 考验的 ChesedMend）
    if (HealBlock.blocks( this )) return 0;
    int before = HP;
    HP = Math.min( HT, HP + amount );
    return HP - before;          // ← 返回「实际回复量」
}
```

三处要点：

1. **判据唯一**：`HealBlock.blocks(ch)` 就是 `ch.buff(HealBlock.class) != null`。全仓
   `HealBlock.blocks(` 调用点**恰好 1 个**（就是这里）——核验脚本把这条钉成了硬断言。
2. **返回值 = 实际回复量**（不是名义量）：带浮字的站点据此守卫，**禁疗时不再弹假的 `+N` 浮字**；
   顺带修掉「血量接近满时浮字虚高」的老毛病里我们**新写**的那部分。
3. **不改 HT 钳制语义**：`Math.min(HT, ...)` 原样保留；`HP = HT`（生成初始化 / 复活）、
   `Math.min(HP, HT)`（下压钳制）这些**不是回血**，本次一律不动（见 §五 白名单）。

### 2.2 收口范围：**66 条替换 / 50 个文件**

用一次性补丁脚本 `_chk/patch_heal_route.py` 落地（字节级 IO、幂等 marker、位置断言 + 位移修正、
每条 op 的计数断言、按文件各自保留行尾）。改动前的整文件副本留在 `_chk/_bak_heal_route/`（50 个）。

覆盖面（按来源分类）：

| 来源 | 文件（节选） |
|---|---|
| **考验 CHESED** | `actors/buffs/ChesedMend.java`（3 处，每 5 回合回 10%） |
| 药水 / 丹药 | `WaterOfHealth`、`ElixirOfAquaticRejuvenation` |
| 食物 | `FrozenCarpaccio`、`Pasty`、`PhantomMeat`、`SupplyRation`、`LifelongStew` |
| 植物 | `plants/Sungrass` |
| 常驻 buff | `Regeneration`、`Healing`、`WellFed`、`MagicalSleep`、`LifeRegen`、`ThirstBloodBarrier`、`GritTeethBuff` |
| 吸血 / 武器 | `Vampiric`、`Mimicry`、`ParadiseLost`、`HermesCaduceus` |
| 法杖 | `WandOfTransfusion`、`WandOfWarding`、`WandOfLivingEarth`、`CursedWand` |
| 法术 / 技能 | `BlessSpell`、`LayOnHands`、`HallowedGround`、`ElementalStrike`、`ElementalBlast`、`Challenge`、`PinHaoFan` |
| 天赋 | `actors/hero/Talent.java`（2 处，含 `HEARTY_MEAL` 等） |
| 神器 / 遗物 | `ChaliceOfBlood`、`DriedRose`（2 处）、`Admiration`、`TornPage` |
| 怪物自我回血 | `Bat`、`Goo`（3 处）、`Necromancer`（3 处）、`Succubus`、`RotLasher`、`MeltingLove`、`YogFist`、`CrystalGuardian`、`RingfingerAutomaton` |
| 其它 | `Dewdrop`、`Corruption`（腐化友军的满血重置）、`Metabolism`（盔甲诅咒：把食物转成 HP）、`Mob`（天赋 `SOUL_EATER` 的进食回血）、`AimHeartMark`（3 处） |

> ⚠️ **`TornPage` 等 3 处浮字沿用上游的「名义值」**（`TornPage` 显示 `toHeal`、
> `CrystalGuardian` / `Succubus` 显示硬编码 `"5"`）——这是**上游原有写法**（改前就是这样），
> 本次只加 `> 0` 守卫（禁疗时不弹假浮字），**不动上游的展示口径**。

## 三、`HealBlock`（禁疗 buff）

`actors/buffs/HealBlock.java`，**顶层类**（随存档序列化 + 反射重建 ⇒ 不能做非静态内部类，
本项目已因这个崩过一次：`Admiration.LoveSync`）。

- `DURATION = 50f`、`type = NEGATIVE`、`announced = true`。
  - ⚠️ **NEGATIVE 在这里是刻意的、且不踩红线**：那条「buff 别标 NEGATIVE，否则全图怪立刻醒来」的
    坑针对的是**入场即挂**的被动 buff（`Trials.bindMobPassives` 那一路）。本 buff 只在**被护符命中**时
    施加，不会在刷怪时出现。
  - 副产物：宝箱怪身上的 NEGATIVE buff 会触发 `Mimic.add()` 的自动暴露逻辑（见 §四）。
- `apply(ch, duration)` 走 `Buff.prolong`：**只延不缩、不叠加**（连续两发不变成 100 回合）。
- `blocks(ch)` 是 `static` 工具：这样「禁疗」的全部语义只在 `HealBlock` ＋ `Char.heal` 两处，
  以后要收窄成「只禁某类回血」也只有这两个地方要动。
- **持续粒子**：`fx(boolean)` 挂/摘发射器。⚠️ `sprite.emitter()` 返回的是**共享池对象**，
  必须**缓存引用**才能关掉（`private transient Emitter smoke`）——关的时候要 `smoke.on = false`。
  瞬态字段，**不进存档**。
- **图标**：暂无自绘，借 `BuffIndicator.HERB_HEALING`（圣草治疗帧）+ `tintIcon` 暗红着色区分。
  按 `BuffIndicator` 的惯例，未自绘的 buff 不预留帧号常量。

## 四、`LloydTalisman`（护符本体）

`items/LloydTalisman.java extends Item`：`stackable = true`、`defaultAction = AC_THROW`（投掷生效）、
`isUpgradable() = false`、`isIdentified() = true`（炼金产物，效果固定，与松脂系列一致）。

### 4.1 `onThrow` 的**顺序就是语义**（核验脚本按「比位置」钉死）

```
① Actor.findChar(cell)              ← 找落点单位
② 未命中/已死 → super.onThrow(cell)  ← 照常落地、可捡回，然后 return
③ sprite.emitter().burst(BURST, 12) ← 目标身上炸一簇白烟
④ CellEmitter.get(cell).burst(FACTORY, 8) + Sounds.PUFF   ← 落点也起一团 + 音效
⑤ ch instanceof Mimic → mimic.stopHiding() + alignment=ENEMY
⑥ HealBlock.apply(ch, 50)
⑦ 若是宝箱怪：Paralysis / Weakness / Blindness 各 20 回合
   （结尾**不调** super.onThrow ⇒ 命中即消耗，与原版药水 shatter 同款处理）
```

**为什么 ⑤ 必须在 ⑥ 之前**：`Mimic.add()` 对 NEGATIVE buff 会自动执行
「`alignment = ENEMY` ＋ `stopHiding()`」。若先挂 buff 再手动暴露，就会**多播一次揭示特效**
（GLog + 音效 + 星光）。先暴露 ⇒ 后面加 buff 时 alignment 已不是 NEUTRAL，自动分支自然跳过。

**命中即消耗**：`Item.cast()` 已把护符从背包 `detach`（栈 >1 时给的是数量 1 的副本），
命中分支不再落地就等于「用掉了」。投空（没砸到单位）才走 `super.onThrow` 落地，**可以捡回来**。

### 4.2 图集与配方

- `ItemSpriteSheet.LLOYD_TALISMAN = xy(7, 41)`（**常量声明必须早于 `assignItemRect` 所在的
  `static{}`**，否则静态初始化顺序会让所有物品图标整体错位且不报错）+ `assignItemRect(14, 16)`
  —— **恰好**是包围盒（14×16 就是 `Image.width()/height()`，写错会连带改布局）。
- `CraftRecipe extends Recipe.SimpleRecipe`：`inputs = { PotionOfHealing, LiquidMetal }`、
  `inQuantity = { 1, 20 }`、`cost = 3`、`output = LloydTalisman`、`outQuantity = 6`。
- 登记进 `items/Recipe.java` 的 `twoIngredientRecipes`（**实际合成匹配**）；
  同时补进 `ui/QuickRecipe.java` 的 `case 6`（**炼金指南/日志页展示**，见 §七）。

### 4.3 文本键（zh + en 各 2 个文件）

| 键 | 位置 |
|---|---|
| `items.lloydtalisman.name` / `.desc` | `items_zh.properties` / `items.properties` |
| `actors.buffs.healblock.name` / `.desc` | `actors_zh.properties` / `actors.properties` |

`healblock.desc` 带 `%s` 占位（剩余回合），由 `HealBlock.desc()` 用 `dispTurns()` 填入。

## 五、**刻意不动**的 9 条「抬血站点」（收口白名单）

`_chk/verify_lloyd_healblock.py` 的 ② 层每次重扫全仓抬血写法
（`HP += x` / `HP = HP + x` / `HP = Math.min(..., HP + x)` / `HP = Math.max(HP, 下限)`），
断言结果**恰好等于**这份白名单 —— 这是防「以后有人顺手直写 `HP +=` 把禁疗架空」的回归护栏。

| 文件 | 站点 | 为什么不是回血 |
|---|---|---|
| `actors/Char.java` | `HP = Math.min(HT, HP + amount)` | **`heal()` 本体**（收口点自己） |
| `actors/buffs/Blessing.java` | `target.HP += htBonus` | HT 增加时按增量回算当前 HP（上限重算） |
| `actors/buffs/PrismaticGuard.java` | `HP += 0.1f` | **自持 float `HP` 字段**（护盾量，不是生命） |
| `actors/hero/Hero.java` | `HP += Math.max(HT - curHT, 0)` | `updateHT(boostHP)`：HT 涨了 HP 补差额 |
| `actors/hero/abilities/rogue/ShadowClone.java` | `HP += hpBonus` | 分身 HT 增量回算 |
| `items/wands/WandOfWarding.java` | `HP += 19` / `HP += 30` | 守卫塔按档位重设 HP（阶梯生成，不是回血） |
| `actors/mobs/YogDzewa.java` | `HP = Math.max(HP, HT - phaseStep()*phase)` | ★ **设计选择**（见下） |
| `actors/mobs/YogDzewa.java` | `HP = Math.max(HP, finalPhaseFloor())` | ★ 同上 |

★ **YogDzewa 两条是「设计选择」而不是「它不是回血」**：那是打 Boss 时把血**垫回**该阶段的
HT 派生下限（防止一刀削穿阶段），属于「Boss 阶段刻度」的一部分
（见 `_chk/verify_boss_phase_scales.py`）。本作**刻意**让它不受禁疗影响。
若以后要「连 Boss 阶段下限也禁疗」，改白名单检查那一处是**唯一**入口。

> 另外还有一整类「生成初始化 / 阶段血」写法 `HP = HT`、`HP = HT/2`、`HP = Math.round(HT*pct)`
> （`Mob.spawn`、`Succubus`、`DecoyDoll`、`RingfingerAutomaton`、`SmokeBomb`、`DriedRose`、
> `Tengu`、`YogFist`、`Hero.resurrect` 等）与下压钳制 `Math.min(HP, HT)`（`Goo`、`MeltingLove`
> 的满血重算）。这些**本来就不该被禁疗拦**（否则 Boss 换阶段、分身生成、满血重算会全部失效），
> 核验的 ③ 层把它们逐条断言为「仍在原地」，**防的正是「用力过猛」**。

## 六、炼金釜「点箭头闪退」（顺带修）

`scenes/AlchemyScene.java` 两处越界：

1. **快速配方套用**（点箭头把配方原料自动填进 3 个格子）的 while 没有收敛上界 ⇒
   原料数量 > 3 时 `inputs[3]` 直接 `ArrayIndexOutOfBoundsException` 闪退。
   修法：`while (!found.isEmpty() && needed > 0 && curslot < inputs.length)`（**循环自己收敛到
   炼金釜的 3 格**），外加堆叠原料的 `detachAll`。
2. `combines` 数组填充循环加上 `i < combines.length`（=3）的**双重上界**：本作新增配方后，
   某些原料组合可能同时满足 3 条以上，光靠 `recipes.size()` 会越界。

## 七、松脂系列：其实**早就登记在 `Recipe` 表**，只是指南页看不到

需要澄清一处与用户说法不符的地方：**焦炭松脂 / 黄金松脂的配方此前已在 `Recipe.java`（208~211 行）
登记**（`CharcoalResin.FromPotion`、`LooseCharcoalResin.SplitRecipe`、`GoldenResin.FromPotion`、
`LooseGoldenResin.SplitRecipe`）—— 也就是说**炼金釜本身能合出来**。

看不到的原因是：**炼金指南 / 日志的配方页不读 `Recipe` 表，而是读 `ui/QuickRecipe.getRecipes(pageIdx)`**
（`windows/WndJournal.java:418` 调用）。`QuickRecipe` 只决定「指南里展示哪些配方」。
本次把 4 条松脂 + 1 条洛伊德护符补进 **`case 6`**（液金 / 松脂那一页），
指南页从此能看到它们（配 6 个 import：`CharcoalResin` / `GoldenResin` / `LooseCharcoalResin` /
`LooseGoldenResin` / `LloydTalisman` / `PotionOfHealing` / `PotionOfLiquidFlame`）。

## 八、原版神器加强：脑啡肽 **60 → 30**

- `items/artifacts/ArtifactEnhanceRecipe.java`：`ENKEPHALIN_NEEDED = 30`。
- `ui/QuickRecipe.java`：五处 `.quantity(60)` 全部改 30（并断言无 60 残留）。
- `items/Recipe.java`：残留的旧注释「原版神器 + 60脑啡肽」同步改为 30。

## 九、核验（源码级；用户约定不代跑 Gradle）

- **`_chk/verify_lloyd_healblock.py`：83 条断言全绿 + 4 组反例自测（`--selftest`）**。四层：
  ① 结构层（护符 / 禁疗 / `Char.heal` / 图集 / 粒子 / 配方 / 指南页 / 文本键 / 炼金釜修复，
  **顺序敏感处一律比位置**、**断言前先剥注释**）；
  ② **收口层**（全仓抬血写法扫描 ≡ 9 条白名单，多一个/少一个都 FAIL；并断言
  `HealBlock.blocks(` 全仓恰好 1 个调用点、`.heal(` ≥ 50 处、`int healed` 守卫 ≥ 30 处、
  `ChesedMend` 单独点名）；
  ③ 排除层（HT 钳制 / 生成初始化 / 阶段血仍在原地）；
  ④ 反例自测（删掉 `heal()` 里的禁疗判定、把 `stopHiding()` 挪到挂 buff 之后、
  凭空造一个 `HP += 5` 站点、注释里写 `HP += 999` —— 四种改坏法都必须被抓出来，
  证明判据不恒真）。
- **单文件 `javac`**（13 个关键文件）**EXIT=0**。告警逐条归因：
  - `WhiteSmokeParticle.java:63 [this-escape]` ≡ 原版 `SmokeParticle.java:49`（同款
    「构造函数里调 `color(...)`」，上游惯用写法，**用原版类对照编译证实同一条**）；
  - `LloydTalisman.java:145 [rawtypes]/[unchecked]` ≡ 原版 `CharcoalResin.java:59`
    （`inputs = new Class[]{...}`，全仓所有配方类同款）；
  - `Char.java` 的 39 条告警全在 572 / 1396 / 1669~1781 行，**没有一条落在 `heal()`（1064~1074）内**；
  - `QuickRecipe.java:168 / 254 [this-escape]` 不在本次 diff 的任何 hunk 内（上游既有）。
- `_chk/check_utf8_all.py`：**1459 文件 OK**。
- `_chk/check_unused_imports.py`：5 文件（`LloydTalisman` 14 / `HealBlock` 6 /
  `WhiteSmokeParticle` 4 / `QuickRecipe` 92 / `Char` 163 条 import）**ALL PASS**。
- 补丁脚本：`_chk/patch_heal_route.py`（回血收口）、`_chk/append_lloyd_text.py`（文本键）、
  `_chk/patch_quickrecipe_resin.py`（指南页）；改前副本留 `_chk/_bak_heal_route/`（50 文件）。
- **未升版本、未打包**（用户未要求）。

### 9.1 过程中修掉的两处「补丁自身的问题」

1. **`Metabolism.java` 少一个右花括号**：op 的旧块末行 `}` 实为外层 `if` 的收尾，被一起替换掉 ⇒
   花括号收支 −1，`javac` 报 3 条「需要';'」。逐文件括号审计后发现只有这一处异常，字节补回
   `\t\t\t}`。（**教训**：补丁脚本要带括号收支审计，光看「脚本打印 OK」不够。）
2. **4 处替换把更深一级的行少给了一级缩进**（`Healing` / `ThirstBloodBarrier` / `Metabolism` /
   `LifelongStew`），已手修；并**用字节级 diff 全量核对了 66 条 op 的增删行缩进**。

## 十、待人工验证

1. 对普通怪投洛伊德护符：应出**白色烟雾**，且 50 回合内**喝治疗药水 / 吃食物 / 踩圣草 / 吸血
   全部只弹不加血**（不再出现假的 `+N` 浮字）；目标身上持续冒白烟。
2. **CHESED 考验**（第 3 号）下投中一只怪：它的每 5 回合回血应被完全掐掉（这是需求点名的那条）。
3. 对**四种宝箱怪**（普通 / 黄金 / 水晶 / 黑檀）投中：应**立刻现形转为敌对**，且带 20 回合
   **麻痹 + 虚弱 + 致盲**；**不应多播一次揭示特效**（顺序对了才只有一次）。
4. 投空（砸地上）：不生效，护符**掉在地上可捡回**；投中（用掉）：护符消失、不落地。
5. 炼金釜：治疗药水 + 20 液金 + 3 能量 → 6 个护符；**点箭头快速套用配方不再闪退**；
   指南第 6 页能看到 4 条松脂 + 洛伊德护符。
6. 原版神器加强：消耗 **30** 脑啡肽（不是 60）。

---

## 【同日 晚三】白烟粒子缩小 · 禁疗描述精简 · 改动栏改「极简」 · 版本与打包暂缓

> 用户当次原话：「烟雾的特效粒子大小有些过大了，需要缩小，大约为原先尺寸的一半（即面积四分之一）；
> 随后请你更新更新日志，并更新版本、打包APK和.jar桌面端。注意今后的日志需要采取极简的风格，
> 仅用单句描述进行了什么增改，而不需要详细解释和过度阐述。禁疗buff的描述内容也需要精简，
> 只保留"无法恢复生命值"即可。」
> 随后补充：「更新版本、打包APK和.jar桌面端请稍后，需要添加一个新道具后再进行这些步骤，
> 请你先完成其他要求」。

### 11.1 白烟粒子尺寸减半（面积 1/4）

`effects/particles/WhiteSmokeParticle.java` 的唯一尺寸出口：

```java
// 改前
size( 13 - p * 7 );          // 13.0 → 6.0
// 改后
size( 6.5f - p * 3.5f );     // 6.5 → 3.0，恰好一半 ⇒ 面积只有原先的 1/4
```

`PixelParticle.size(float)` 直接把宽高一起设为该值，所以**线性尺寸减半＝面积四分之一**，
一次改完两条需求。受影响的是**两个工厂共用**的 `update()`：护符命中的 `BURST`（12 发）与
禁疗期间的 `FACTORY`（`pour(…, 0.09f)` 持续缭绕）——两处都自动跟着变小，无需分别调参。
粒子**数量 / 寿命 / 速度**均未动，所以只是「颗粒更细」，密度手感不变。

### 11.2 禁疗描述精简为一句话

`actors.buffs.HealBlock.desc()` 原本把 `dispTurns()` 传进模板（`%s` 显示剩余回合）。现改为：

```java
return Messages.get( this, "desc" );      // 文本里不再有 %s ⇒ 不传参
```

| 语言 | 新文案 |
|---|---|
| zh（`actors_zh.properties`） | `无法恢复生命值` |
| en（`actors.properties`） | `Cannot recover health` |

⚠️ **副作用（有意接受）**：buff 描述里**不再显示剩余回合**。这是「只保留『无法恢复生命值』」
的字面执行结果；若日后想恢复剩余回合，改回 `dispTurns()` 并补 `%s` 即可（探针里有一条
「双语 desc 均无 %s」的断言，届时需同步放行）。

### 11.3 改动栏（`EGOPD_Changes.java`）改为「极简」风格 —— 新约定

用户明示：**今后的日志只写单句，说清「改了什么」，不解释、不展开**。已落到三处：

1. **类注释**新增一段 `<b>行文风格（2026-09-24 起）：极简。</b>`——一句话说清约束，
   并指明「详尽原理属于 `docs/` 档案，不写进改动栏」。
2. **`AGENTS.md` §1「出 EGOPD 新版本的固定三步」**补一节，写明极简风格与「正文不出现 `\n` 分段」。
3. **机械判据**：`_chk/verify_lloyd_healblock.py` 新增断言 —— v0.3.5 及以后的条目正文
   **不得含 `\n` 转义**（＝只有一段），把「风格」变成可回归检查的东西，而不是靠人自觉。

### 11.4 v0.3.5 条目（6 条，全部单句）

| # | 标题 | 正文（原文照录） |
|---|---|---|
| 1 | 新道具：洛伊德护符 | 投掷命中后使目标 50 回合无法恢复生命值；命中宝箱怪时额外使其暴露，并获得 20 回合麻痹、虚弱与致盲。 |
| 2 | 禁疗：所有回血来源失效 | 全仓回血统一收口后，禁疗期间药水、食物、吸血、再生与 CHESED 考验的回血一律归零。 |
| 3 | 修复：炼金釜快速配方闪退 | 修复炼金釜点击箭头套用快速配方时的闪退。 |
| 4 | 炼金指南：松脂系列与洛伊德护符 | 把焦炭松脂、黄金松脂系列与洛伊德护符补进炼金指南页。 |
| 5 | 神器强化：脑啡肽下调 | 原版神器强化形态所需脑啡肽由 60 下调为 30。 |
| 6 | 白烟粒子缩小 | 洛伊德护符与禁疗的白色烟雾粒子缩小至原来的一半。 |

### 11.5 版本号与打包：**暂缓**

用户要先加一个新道具，再一起走「改版本号 → `:android:assembleDebug` → 拷 `EGOPD_x.y.z.APK`」。
因此本轮：

- `build.gradle` 的 `ext` **保持 934 / `0.3.4` 不动**（一度改成 935/0.3.5，按用户补充已改回）。
- `EGOPD_Changes.java` 的 **v0.3.5 段落已先行写好**（上述 6 条）⇒ 源码暂时「领先于」版本号，
  下个新道具落码后把版本号升到 935/`0.3.5` 即可，改动栏不用再动。
- `AGENTS.md` §1 的版本现状一句同样留在 934/0.3.4，等升版时一并改。

### 11.6 顺带做的两处事实性更正（脑啡肽 60 → 30 的残留）

上一轮把配方从 60 改到 30 时，有两处「描述性数字」没跟着走，本次补上：

- `ui/changelist/EGOPD_Changes.java` 的 **v0.3.1 旧条目**：`原版神器 + 60 脑啡肽` → `+ 30 脑啡肽`
  （旧版本条目本身不改写，只把**当前已失效的数值**纠正为现值，否则玩家照着合会多备一倍）。
- `items/artifacts/ArtifactEnhanceRecipe.java` 的 javadoc：`白扔 60 脑啡肽` → `白扔 30 脑啡肽`。

`ENERGY_COST = 12` 未动（用户只提脑啡肽数量）。

### 11.7 核验

| 项 | 结果 |
|---|---|
| `verify_lloyd_healblock.py` | **90 条断言全过**（本轮 +3）；`--selftest` 5 组反例全捕获 |
| 单文件 `javac`（4 个改动文件） | **EXIT=0**（仅 `WhiteSmokeParticle:63` 的 this-escape 警告 ≡ 原版 `SmokeParticle:49`，既有写法） |
| `check_utf8_all.py` | 1459 文件 OK |
| `check_unused_imports.py`（4 文件） | ALL PASS |
| 行尾 | `EGOPD_Changes.java` 纯 CRLF；其余改动文件纯 LF（均保持原样） |

### 11.8 过程教训：同一文件的两处编辑**又**并行丢了一次

本轮在一条消息里对 `_chk/verify_lloyd_healblock.py` 发了两条 `Edit`（改 `desc()` 断言 + 改粒子断言），
结果**只有后一条生效、前一条静默消失**（工具两条都报 success）。这与项目里已登记四次的
「同一文件多处编辑必须串行」是同一个坑，**这是第五次**——已把计数在 `MEMORY.md` 里更新为 5 次。

---

## 【同日 晚四】新道具「神圣卡」＋ 禁疗描述恢复剩余回合 ＋ 出 v0.3.5（APK + 桌面 JAR）

> 用户当次原话：「需要你回复禁疗buff的剩余回合数显示。接下来增加以下道具，随后继续版本更新相关任务，
> 包括打包APK和JAR：神圣卡：贴图位置xy(8,41)，大小15×15，配方为任意卷轴+13炼金能量；描述为：
> 绘制着十字架的纯白卡牌，其中蕴含神圣的能量，能为你抵挡下一次伤害。效果：使用花费一回合，
> 并且获得"神圣屏障"buff，该buff有特定图标，绘制在"融化"buff的下一位；效果持续时间无限，不可叠加；
> 该效果为给予一个一次性的无敌效果，可以响应敌人攻击、陷阱伤害，但不响应角色自身buff的伤害；
> 抵挡一次伤害后即消失。」

### 12.1 禁疗描述：恢复剩余回合

上一轮按「只保留『无法恢复生命值』」把 `%s` 一起去掉了，用户本轮要求**恢复**：

| | 改后 |
|---|---|
| zh | `无法恢复生命值\n\n剩余时长：%s回合` |
| en | `Cannot recover health\n\nTurns left: %s` |

`HealBlock.desc()` 同步改回 `Messages.get( this, "desc", dispTurns() )`。
`verify_lloyd_healblock.py` 里那三条「无 `%s`」断言已**反向**改回（这一处的口径以用户当次要求为准）。

### 12.2 新道具「神圣卡」—— 落码清单

| 位置 | 内容 |
|---|---|
| `items/HolyCard.java`（新） | 道具本体 + 内部类 `CraftRecipe` |
| `actors/buffs/HolyBarrier.java`（新） | 「神圣屏障」buff + **唯一判据** `blocks(Char, Object)` + `trigger(Char)` |
| `sprites/ItemSpriteSheet.java` | `HOLY_CARD = xy(8, 41)` ＋ `assignItemRect(HOLY_CARD, 15, 15)` |
| `ui/BuffIndicator.java` | `HOLY_BARRIER = 117`（`MELTING = 116` 的**下一位**） |
| `items/Recipe.java` | `oneIngredientRecipes` 登记 `new HolyCard.CraftRecipe()` |
| `ui/QuickRecipe.java` | 炼金指南第 6 页加一条（原料用 `Scroll.PlaceHolder` 表示「任意卷轴」） |
| `actors/hero/Hero.java` | `damage()` 里两行 hook（唯一调用点） |
| 文本 4 份 | `items.holycard.*`（zh/en, 5 键）＋ `actors.buffs.holybarrier.*`（zh/en, 3 键） |

**道具行为**：`AC_USE` 对自己使用 → **花一回合**（`hero.spendAndNext(1f)`）→ 挂 `HolyBarrier`
（无回合数、不衰减、不叠层）→ 挡下下一次伤害后消失。使用与抵挡都复用既有 `WhiteSmokeParticle`
（纯白卡牌的视觉语言，不另画素材）。

**配方**：`任意卷轴 ×1 + 13 炼金能量 → 神圣卡 ×1`。刻意**不用** `Recipe.SimpleRecipe`——
它按 `ingredient.getClass() == inputs[i]` 精确匹配，表达不了「任意卷轴」；改成自己实现
`Recipe`，判据 `ingredients.get(0) instanceof Scroll`（普通 / 异域卷轴都吃）。

**不可叠加**：屏障在场时再使用一张卡**整段不生效**——不花回合、不扣卡，只提示一句
（`items.holycard.already`）。这样「不可叠加」不会退化成「白扔一张卡」。

### 12.3 「一次伤害」的边界：黑名单式，两条例外

判定只有一条，收在 `HolyBarrier.blocks(Char, Object)`，**全仓只有 `Hero.damage` 一个调用点**：

```
挡：一切外部伤害（敌人攻击 / 陷阱 / 爆炸 / 坠落 / 毒气 / 电击 …）—— 判据是「没命中下面两条」
① 不挡：src instanceof Buff        —— 自己身上的 DOT（毒 / 燃烧 / 流血 / 腐蚀 / 饥饿）
② 不挡：src instanceof SelfHarmCost —— 蓄血圣杯的血祭 / 割腕（「自伤换成长」）
```

- **①** 是用户点名的边界（「不响应角色自身buff的伤害」）。若不排除，站在自己的毒里就会白吃一张卡。
- **②** 是本作既有红线的**第三个取用点**：那两笔伤害是「挨完还活着就升级」的**代价**，
  被屏障挡掉就等于「一张卡换一次免费升级」。与 `Talent.perseveranceCap`、`GritTeethBuff.checkBypass`
  **共用同一个 `SelfHarmCost` 判据**——按接口自己的约定，**不新开 instanceof 名单**。

⚠️ **需要用户拍板的边界**：按「黑名单」语义，**环境持续伤害**（毒气 `ToxicGas`、`Electricity`、
`StormCloud`）的 `src` 是 **Blob 而不是 Buff**，所以**会**吃掉屏障。这与道具描述「抵挡**下一次**伤害」
自洽（玩家站进毒气 = 吃了这一次），但若用户想要「只挡敌人攻击与陷阱」，把 `blocks()` 改成
白名单式（`src instanceof Char || src instanceof Trap`）即可，一行的事。

### 12.4 图标帧 117

`large_buffs.png`（256×128，16px 格）与 `buffs.png`（128×64，7px 格）的**帧 117** 用户已绘；
探针真解 PNG 复核：帧 117 非空、**与帧 116（融化）不是同一张图**、帧 118 仍为空（没有误占下一格）。

### 12.5 出 v0.3.5（935）

- `build.gradle`：`appVersionCode 934→935`、`appVersionName '0.3.4'→'0.3.5'`。
- `EGOPD_Changes.java`：v0.3.5 段落现共 **7 条**（原 6 条 + 「新道具：神圣卡」，仍为单句）。
- 打包 `:android:assembleDebug` → 归档 `EGOPD_0.3.5.APK`；桌面端 `:desktop:release` → `desktop-0.3.5.jar`。
- `AGENTS.md` §1 的版本现状一句同步为 935 / `0.3.5`。

### 12.6 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_holy_card.py`（新） | **58 条断言全过**；`--selftest` 5 组反例全捕获 |
| `_chk/verify_lloyd_healblock.py` | 90 条全过（3 条描述口径已反向同步） |
| 单文件 `javac`（8 文件） | **EXIT=0**（仅既有告警：Hero 281/2363、QuickRecipe 169/255） |
| `check_utf8_all.py` | 1461 文件 OK |
| `check_unused_imports.py`（4 文件） | ALL PASS |

**反例自测抓到的一次自身错误**：初版 `--selftest` 的 (e) 用 `body.replace('return;','',1)` 去模拟
「删掉不可叠加的早退」——删掉的其实是更前面那句 `!hero.isAlive()` 的 return，于是反例根本没被改坏、
判据显示「恒真」。改成「定位 `Messages.get(this,"already")` 之后再删第一个 `return;`」才对。
**教训**：反例自测不许用「删第一个匹配」，要**锚定到具体那一句**。

### 12.7 顺带发现的一处既有缺口（未处理，待用户决定）

`.properties` 里 **松脂系列的英文文本整段缺失**：zh 有 `items.resin.*`（2 键）与
`items.charcoalresin.*` / `items.loosecharcoalresin.*` / `items.goldenresin.*` /
`items.loosegoldenresin.*`（各 3 键）共 **14 个键**，`items.properties` 里**一个都没有**
（`grep -in "charcoal\|goldenresin"` 只命中原版的 charcoal potion）。英文环境下这些道具会显示
**原始键名**。这不是本次改动引入的，属上一轮松脂工作的遗漏；补的话是纯增量、风险为零，
但需要用户认可的英文文案，故本轮先记下来。

---

# 2026-09-24 「趣味挑战」——挑战系统内新增一层**分类**（不新增位段）

## 13.1 需求原文

> 新建一类挑战「趣味挑战」，将原先挑战中的**拆迁办、依旧果冻人、水仙追迹、调试模式**四个挑战移入趣味挑战当中。
> 趣味挑战不需要设置特殊挑战图标和排版，套用原先挑战即可。趣味挑战的灰化按钮、点亮按钮、游戏内右上角小图标
> 分别在 `icons.png` 的 `(128,48)`、`(144,48)`、`(160,48)` 处。

用户经 `AskUserQuestion` 拍板的一条关键口径：
**「不计入常规挑战，但仍计入得分倍率（按照各自的设定）」** —— 即

| 维度 | 趣味挑战是否计入 | 实现处 |
|---|---|---|
| 常规挑战数（随机池 / 局内计数 / `CHAMPION_*` 徽章） | **否** | `Challenges.activeChallenges()` 只数常规 |
| 得分倍率 `(1.25)^n` | **是**（按各自设定；`DEBUG_MODE` / `NARCISSUS_TRACING` 仍把倍率置 0） | `Challenges.scoredChallenges()` 保持**旧口径**（除 `DEBUG_MODE` 外全计） |

## 13.2 设计：不新增位段，只加一层「分类」

四条趣味挑战**继续用原来的位**（`DEMOLITION_SQUAD=512` / `JELLY_PERSON=1024` / `DEBUG_MODE=2048` /
`NARCISSUS_TRACING=4096`），仍存在同一个 `challenges` 位掩码里
（`Dungeon.challenges` / `SPDSettings.challenges()` / `info.challenges`），
**不新增字段、不改存档格式**。新增的只是 `Challenges` 里的一个类别判据：

```java
public static final int FUN_MASK = DEMOLITION_SQUAD | JELLY_PERSON | NARCISSUS_TRACING | DEBUG_MODE;

public static boolean isFun( int mask ){ return (mask & FUN_MASK) != 0; }   // 位与 ⇒ 单条/组合都成立
public static boolean isRegular( int mask ){ return !isFun( mask ); }
```

三个配套约定（都留了核验断言）：

1. **`nameId(mask)` 反查文案键**：从 `MASKS` / `NAME_IDS` 两个**等长**数组反查，取代「按下标配对」——
   以后再加挑战不会因为数组错位而取错文案。
2. **写回各清各的位**：`WndChallenges` 写回 `SPDSettings.challenges() & ~regularMask()`，
   `WndFunChallenges` 写回 `& ~funMask()` ⇒ **打开任一个窗口都不会把另一类的勾选抹掉**。
3. **`MAX_CHALS` 12 → 9**：它表达的是「常规挑战数」，跟着分类一起改。

## 13.3 落点

| 文件 | 改动 |
|---|---|
| `Challenges.java` | `FUN_MASK`；`isFun` / `isRegular`；`regularMasks()` / `funMasks()`；`regularMask()` / `funMask()`；`nameId(int)`；`activeChallenges(int)` 只数常规；`activeFun()` / `activeFun(int)`；`scoredChallenges()` / `scoredChallenges(int)`；`MAX_CHALS = 9` |
| `ui/Icons.java` | 枚举 `FUN_GREY` / `FUN_COLOR` / `FUN_COUNT`；rect 分别为 `uvRectBySize(128,48,14,16)` / `(144,48,14,16)` / `(160,48,7,7)` |
| `windows/WndChallenges.java` | 只列常规（`regularMasks()` + `nameId()`），写回**保留趣味位** |
| `windows/WndFunChallenges.java`（新） | 套用挑战的排版，只列趣味（`funMasks()`），写回**保留常规位** |
| `scenes/HeroSelectScene.java` | 新增 `funButton`；⚠️ 图标判据必须用**带参** `Challenges.activeFun(SPDSettings.challenges())`——选中界面里 `Dungeon.challenges` 恒为 0 |
| `ui/MenuPane.java` | 局内计数 `Icons.FUN_COUNT`，栏位偏移 `-28`（挑战 -14 / 考验 -21 / 趣味 -28） |
| `windows/WndGame.java` | 局内菜单按钮；顺带把常规挑战按钮的判据改成 `Challenges.activeChallenges(Dungeon.challenges) > 0`（否则只开趣味挑战时会弹空窗） |
| `windows/WndGameInProgress.java` | `hasChals` / `hasFun` 两个判据 + 两个按钮（`GAP -= 2` ×3） |
| `windows/WndRanking.java` | 页签扩到 **7** 个（尾位 `FUN_COLOR`）；挑战页只列常规（9 行），新增 `FunChallengesTab` 只列趣味（4 行） |
| `Rankings.java` | `chalMultiplier` 走 `scoredChallenges()` |
| `windows(_zh).properties` | `wndfunchallenges.title` / `wndgame.funchallenges` / `wndgameinprogress.funchallenges`（zh + en 各一） |
| `icons.png` | 三个新格（用户已绘）：灰化按钮 `(128,48)`、点亮按钮 `(144,48)`、小图标 `(160,48)` |

**顺带修掉一个既有裁剪 bug**：拆分前挑战页有 13 行（207px）塞进 `HEIGHT = 144` 的窗口 ⇒ 底部被裁；
拆成「常规 9 行（143px）+ 趣味 4 行」后两边都塞得下（沿用「一类一页签」的既有范式，而不是把趣味堆在挑战页下面）。

## 13.4 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_fun_icons.py`（新） | 三格 rect ↔ `icons.png` 实测像素：非空、满格灰/彩、与其余 86 个 rect 无重叠、与 `CHAL_COUNT` / `TRIAL_COUNT` 同规格（7×7，字形 dx=1/dy=1） |
| `_chk/verify_fun_challenges.py`（新） | **81 条**跨文件接线断言（A~K）+ 12 条反例自测 |
| `_chk/ChallengesLocCheck.java` | 扩到 **167 条**（含趣味语义 + `scoredChallenges` + 新文案键） |
| `_chk/compare_count_icons.py`（新） | 三个计数图标逐像素比对：互不相同、`FUN_COUNT` 非透明像素 28（另两个 30）⇒ 视觉可区分且对齐正确 |
| 单文件 `javac` | **EXIT=0**（仅既有告警） |
| `check_utf8_all.py` / `check_unused_imports.py` | OK / ALL PASS |

---

# 2026-09-24 修 Bug：GEBURA 濒死无敌「延后死亡」⇒ 幸运附魔 / 财富戒指的**额外掉落判定**落空

## 14.1 需求原文

> 检查并修复考验当中的 GEBURA 存在的一项问题：**怪物无敌延后死亡导致幸运附魔、财富戒指的额外掉落判定不生效**。

追加口径（用户实测）：

> GEBURA 下财富戒指**增加原本基础掉落掉率**的功能可能是生效的，但是它的**额外掉落生成**不生效。

## 14.2 根因

`Mob.rollToDropLoot()` 的三段结构是「基础掉落 → 财富戒指额外掉落 → 幸运附魔额外掉落」。
其中**后两段读的都是「致死那一击那一刻」的状态**：

| 段 | 判据 | 状态来源 | 在 vanilla 里为什么读得到 |
|---|---|---|---|
| 基础掉落 | `Random.Float() < lootChance()` | `lootChance()` 含 `1.2^财富等级` | 与死亡同刻 ⇒ 不受影响（**用户观察到的「基础掉率仍生效」与之一致**） |
| 财富额外掉落 | `Ring.getBuffedBonus(hero, Wealth.class) > 0` | **英雄侧**的 `Wealth` RingBuff 等级 | 与死亡同刻 |
| 幸运额外掉落 | `buff(Lucky.LuckProc.class) != null` | **怪物侧**的 `LuckProc` | 与死亡同刻 |

vanilla 的「同刻」是因为**攻击 → 伤害 → `die()` 全在同一个调用栈里**（`Char.attack` → `Char.damage`
→ `Mob.die` → `Mob.rollToDropLoot`）。而 GEBURA 把真正倒下**推迟了 `EXP` 个回合**
（`GeburaGrace.act()` 数满之后才调 `die(原始来源)`）⇒ 推迟到的那一刻：

- **`LuckProc` 必然已经不在**：它是**只活到下一个 buff tick 的瞬态标记** ——
  `LuckProc.act()` 的第一句就是 `detach()`（它本来就是靠「同一调用栈」才有机会被读到）。
  闸门在 `Char.damage` 里挂上 `GeburaGrace` 之后就再也没有机会回看它 ⇒
  **幸运附魔的额外掉落、以及环指大师素材的「幸运再判一次」（`RingMasterLoot` 的 `luckyProc` 实参）双双落空**。
  这是本条 Bug 的**主因**，行为探针 §① 有直接实证（挂上 → 跑一次 `act()` → 已自行摘掉）。
- **财富等级是英雄侧凭据**：延后期间它同样可能与「致死那一刻」不一致（复活 / 卸下等）。
  它不像 `LuckProc` 那样**必然**消失，所以表现为「有时仍在、有时已失效」，与用户「可能是生效的」的
  措辞吻合；但**判定口径本身是错的**（拿「真正倒下那一刻」的凭据判「致死那一击」的掉落），
  所以一并按同一原则收口。

> 一句话原则：**掉落的额外判定必须按「致死那一击那一刻」的凭据算，物品的落地仍在真正死亡那一刻。**

## 14.3 修法：闸门抄一份凭据，死亡时「live 优先、取不到才回落」

不把结算提前到锁血那一刻（那会让物品在「怪物还没倒」时就掉在地上，且要防重复结算）；
只在闸门里**抄两份凭据**存到 Mob 上，等真正倒下时供那两段判定回落：

```java
// Trials.interceptLethalDamage（mob.geburaUsed = true; 之后、Buff.affect(GeburaGrace) 之前）
mob.geburaLuckyProc = mob.buff( Lucky.LuckProc.class );                    // 抄**实例**，掉落强度一并留存
mob.geburaWealthBonus = (Dungeon.hero == null) ? 0
        : Ring.getBuffedBonus( Dungeon.hero, RingOfWealth.Wealth.class );  // 抄等级读数
```

消费端一律 **live 优先、取不到才回落**（⇒ **未开启本考验时行为逐字不变**）：

```java
// Mob.die()
boolean luckyProc = buff(Lucky.LuckProc.class) != null || geburaLuckyProc != null;

// Mob.rollToDropLoot() · 财富块
int wealthBonus = Ring.getBuffedBonus(Dungeon.hero, RingOfWealth.Wealth.class);
if (wealthBonus <= 0) wealthBonus = geburaWealthBonus;
if (wealthBonus > 0) { ... RingOfWealth.tryForBonusDrop(Dungeon.hero, rolls, wealthBonus) ... }

// Mob.rollToDropLoot() · 幸运块
Lucky.LuckProc luck = buff(Lucky.LuckProc.class);
if (luck == null) luck = geburaLuckyProc;
if (luck != null){ geburaLuckyProc = null; Dungeon.level.drop(luck.genLoot(), pos).sprite.drop(); Lucky.showFlare(sprite); }
```

三个必须讲清的点：

1. **等级必须「传进来」，不能只放宽外层守卫**：`tryForBonusDrop` 内部还会自查一次
   `getBuffedBonus`，只改外层的 `if (等级 > 0)` 的话回落值等于白抄（内部照样 `return null`）。
   因此 `RingOfWealth` 新增三参重载 `tryForBonusDrop(Char, int tries, int bonus)`，
   二参重载转调它并把 live 等级原样传进去（**升级点最小、原调用方一字未改**）。
2. **已 `detach` 的 `LuckProc` 仍可消费**：`Buff.detach()` 是 `target.remove(this)`，
   而 `Char.remove(Buff)` **不会把 `buff.target` 置空** ⇒ 二次 `detach` 幂等、`genLoot()` 可安全调用
   （探针 §④ 有实证）。所以留存「实例」是安全的，且**掉落强度（`ringLevel`）连带留存**，
   不必为它另开字段或加 getter。
3. **两份留存都刻意不进存档**：豁免用掉的结局一定是「计时结束后真死」，而死者不会被写进存档
   （与 `geburaUsed` / `geburaStagger` 同一理由）。

## 14.4 落点

| 文件 | 改动 |
|---|---|
| `actors/mobs/Mob.java` | 新增 `public Lucky.LuckProc geburaLuckyProc` / `public int geburaWealthBonus`（**不进存档**）；`die()` 的 `luckyProc` 布尔把留存算进去；`rollToDropLoot()` 财富块（回落 + 三参调用）与幸运块（回落 + 用完即放） |
| `Trials.java` | `interceptLethalDamage` 里两行抄写（**不含任何 `Random` 调用** ⇒ 「不改随机流」的硬验收仍成立） |
| `items/rings/RingOfWealth.java` | 新增三参重载 `tryForBonusDrop(Char, int, int)`（体内**不再自查**等级）；二参重载转调 |

## 14.5 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_gebura_drop.py`（新） | **37 条**断言（A~H 八组：字段/不进存档、闸门抄写位置与顺序、两处消费、重载、可留存性、门控前提、行为层）+ **36 条反例自测**（0 条抓不到错） |
| `_chk/GeburaDropProbe.java`（新，行为探针） | **26 条断言全过**：① `LuckProc` 跑一次 `act()` 即自行摘掉（根因实证）② 闸门抄下的**就是那一击身上那个实例** ③ 计时走完后 live 没了、留存仍在 ④ 已 detach 的实例二次 detach 幂等且 `genLoot()` 可产出 ⑤ 关掉 GEBURA 时两字段恒为初值、live 的 `LuckProc` 未被动过 ⑥ 同 seed 200 个随机数逐位相同 ⑦ 抄下的财富等级 == 致死那一刻读数；摘掉 Wealth buff 后 live 归 0 而留存仍是旧值，且三参重载按**实参**放行 |
| `_chk/verify_chesed_gebura.py` | 全部通过（既有 155 条口径未被本次改动破坏；含「闸门不碰 Random」与 12 个 `act()` 绕过者清单） |
| 单文件 `javac`（3 文件） | **EXIT=0**（仅既有 `rawtypes` 告警 `Mob.java:970`） |
| `check_utf8_all.py` / `check_unused_imports.py` | OK / ALL PASS |
| 行尾 | `Mob.java` / `Trials.java` 保持 LF，`RingOfWealth.java` 保持 CRLF |

## 14.6 待人工验证

1. 开 GEBURA，用**幸运附魔武器**击杀任意怪：它进入濒死无敌、计时结束时倒下，
   **这一击的幸运额外掉落应当出现**（修复前：不出现）。
2. 开 GEBURA + 戴**财富戒指**，连杀若干怪：财富的**额外掉落（彩色闪光那一下）**在濒死无敌的怪身上
   同样会出现（修复前：主因是判定口径错，表现为迟迟不出）。
3. 关掉 GEBURA：与旧版逐字一致（`interceptLethalDamage` 第一行就 `return false` ⇒ 两份留存都是初值）。
4. 环指大师职业 + 幸运武器 + GEBURA：素材的「幸运再判一次」应当恢复。

---

# 2026-09-26 「带着标记倒下」：两处「英雄击杀」判据改为「携带 buff 的怪死亡」＋ 水晶矿洞层的虚空地形与「魔法乱流送回本层」

## 15.1 需求原文

> 现在需要你先修复两个相似的问题：拇指角色的盔甲技能"瞄准心脏"的"荣耀凯旋"在锁血时不触发；食指角色的击杀指令目标的相关指令完成要求以及天赋，在锁血时不触发；这些问题都是由于需要判定玩家击杀目标而导致的，由于它们的共性（需要击杀的目标带有对应buff），将触发条件改为"携带buff的怪物死亡"应该就可以正常避免，请你按照这个思路进行修复。随后请你改动水晶任务矿洞层的地形生成，使其中正常生成深渊（或者虚空）地形（替代地板而不是墙体）；如果玩家掉落其中，不会抵达别的层，而是传送回本层并受到坠落伤害判定，提示"一股魔法乱流把你送回了这一层某处"

## 15.2 根因：GEBURA 锁血把「死亡」推迟了

`Trials.interceptLethalDamage` 在「这一击致死」时不让它死，而是挂 `GeburaGrace`（濒死无敌 `EXP` 回合），
计时结束由 `GeburaGrace.act()` 调 `die(lethalSource)` 真正倒下。于是任何「这一击把它打死了」的判据都必然落空：

| 缺陷 | 旧判据 | 落空原因 |
|---|---|---|
| 拇指 盔甲技能「瞄准心脏」天赋「荣耀凯旋」 | `Char.attack` 里紧接 `enemy.damage(...)` 的 `!enemy.isAlive()` | 那一击进的是 `GeburaGrace`（`Mob.isAlive()` 被覆写为 `super.isAlive() \|\| geburaGrace`）⇒ `isAlive()` 仍为 true |
| 食指「击杀指令目标」的完成与天赋「神谕庇佑」 | `Mob.die` 英雄击杀分支里的 `buff(InstructionTarget.class) != null` | 真正倒下时 `cause` 是 `GeburaGrace` 记下的 `lethalSource`，不是 `Dungeon.hero`／武器；且标记 buff（`AimHeartMark` 只有 10 回合）很可能已过期 |

**用户给的方向**：两者的共性＝「要击杀的目标身上带着对应 buff」⇒ 判据改成「**携带该 buff 的怪死亡**」，
与「谁打死的」「打完立刻死没死」都解耦。

## 15.3 Part A：判据收口到 `Mob.die()` 开头

三条约束决定了收口点与写法：

1. **必须在 `super.die(cause)` 之前**。`Mob.die → super.die`（`Char.die`）`→ destroy()`（虚分派到 `Mob.destroy()`
   → `super.destroy()` → `Actor.remove(this)` → `Char.onRemove()` → 逐个 `buff.detach()`）
   ⇒ 之后 buff 全被摘掉，再判就读不到。（`PalermoFencing` 里那句同型判据本来就是这个错——`enemy.die()` 返回时
   buff 已经没了，是**死代码**。）
2. **锁血期间 live 的 buff 可能已过期** ⇒ 闸门在「致死那一击」抄一份携带状态到 `Mob` 上，
   死亡时 **live 优先、取不到才回落**（与 2026-09-24 的 `geburaLuckyProc` / `geburaWealthBonus` 同一范式）。
3. **与「英雄击杀」分支互不重叠**：新判据只跟 buff 走，旧分支只跟 `cause` 走。

```java
// Mob.die(Object cause) 的 alignment == Alignment.ENEMY 块开头
boolean aimMarked   = buff(AimHeartMark.class) != null || geburaAimMarked;
boolean instrTarget = buff(InstructionTarget.class) != null || geburaInstrTarget;
geburaAimMarked  = false;   // 一次性：抄存的携带状态随这次真实死亡消费掉
geburaInstrTarget = false;
if (aimMarked){ AimHeartMark.onKillByHero( Dungeon.hero ); }
if (instrTarget){ Talent.onOracleBlessing( Dungeon.hero ); Instruction.onTargetKilled( Dungeon.hero ); }
```

`Trials.interceptLethalDamage` 侧只加两行**纯读取**（不碰 `Random`）：

```java
mob.geburaAimMarked   = mob.buff( AimHeartMark.class ) != null;
mob.geburaInstrTarget = mob.buff( InstructionTarget.class ) != null;
```

被撤掉的旧判据：`Char.attack` 的 `aimMarkedBeforeHit` 局部变量与攻击后收尾块、
`PalermoFencing.tryExecute` 的 `onKillByHero`（死代码）、`Mob.die` 英雄击杀分支里的 `InstructionTarget` 块。
全仓 `AimHeartMark.onKillByHero` / `Instruction.onTargetKilled` 的调用点**各只剩 1 处，都在 `Mob.die`**。

⚠️ 两个留存字段**刻意不进存档**（`storeInBundle` / `restoreFromBundle` 都不碰）：推迟用掉之后一定是真死，
死者不会被写进存档。

## 15.4 Part B：水晶矿洞层的虚空地形 + 「魔法乱流把你送回本层」

**地形**（`MiningLevel.carveVoid`，从三个矿洞房间的 `paint()` 里调）：

- **替代地板、不替代墙体**：只把 `EMPTY` / `EMPTY_DECO` 换成 `Terrain.CHASM`；`WALL` / `MINE_CRYSTAL` 一格不动。
- **不封死任何人**：只在「深内区」（房间内圈再各缩 2 格）里挖 ⇒ 外面永远留一整圈地板；
  圆盘与房间自己放的关键单位（水晶守卫／水晶尖塔）保持 `radius+1` 以上切比雪夫距离
  ⇒ 它的落脚点与八邻格必定是地板。半径 1~3，最外圈 50% 保留（边缘不规则，照 `FissureRoom`）；
  `heaps` 上还有东西的格不挖。
- ⚠️ **只写 `map[]`（`Painter.set`）** 是安全的，因为 `Level.create()` 的顺序是
  `while(!build())` → **`buildFlagMaps()`** → `cleanWalls()`：`paint()`（连带 `decorate()`）全在 `build()` 里，
  画完之后 flags 会统一从 `map[]` 重算。**反过来，`create()` 之后再改地形就必须走 `Level.set(...)` /
  `updateCellFlags(...)`**，否则得到「看着是深渊、却踩得上去」的幽灵格（`pit[]` / `passable[]` 没跟上）。
- 画师 `MiningLevelPainter.decorate`：先给「房间里的虚空」拍快照，`super.decorate()`（`CavesPainter`
  会在**房间之间（原为墙体）**填深渊）之后只还原**不在快照里**的那批 ⇒ 原版「矿洞层绝不允许深渊」的口径
  只在房间之间保留。

**落坑不换层**：

- `Level` 新增 `public boolean handlesChasmFall(){ return false; }`（实例方法，按当前层多态分派）；
  `MiningLevel` 覆写为 `Blacksmith.Quest.Type() == Blacksmith.Quest.CRYSTAL`（只有水晶任务才生效）。
- `Chasm.heroFall` 在最前面加一条早返回分支：本层自理时调 `heroRiftReturn()` 后 `return`。
  ⚠️ **必须早于 `Level.beforeTransition()`** —— 那里面会给「离开这一层」的指令记账（`onFloorExit`）、
  还会把英雄的零碎回合凑整，对「其实没离开」的情况都不该发生。
- `heroRiftReturn()`：落脚点用 `Level.randomRespawnCell(hero)`（与传送卷轴同源，天然避开深渊／不可站立格／
  已有角色；额外躲开密室）→ `ScrollOfTeleportation.appear(hero, cell)`（精灵跟随／淡入／粒子／音效全齐）
  → `Dungeon.observe()` + `GameScene.updateFog()` → `GLog.i( Messages.get(Chasm.class, "rift_return") )`
  → `heroLand()`（**原版落坑的同一套落地结算**：残废 + 流血 + 伤害；喝过羽落秘药照常免伤）。
  取不到落脚点时保持原状、不做无谓位移。

文本（三语）：`levels.features.chasm.rift_return` ＝ `一股魔法乱流把你送回了这一层某处` /
`一股魔法亂流把你送回了這一層某處` / `A blast of magical turbulence warps you back to somewhere on this floor.`

## 15.5 落点

| 文件 | 改动 |
|---|---|
| `actors/mobs/Mob.java` | 新增 `public boolean geburaAimMarked` / `geburaInstrTarget`（**不进存档**）；`die()` 开头（`super.die` 之前）的「带着标记倒下」判据块；英雄击杀分支里的旧 `InstructionTarget` 块删除 |
| `Trials.java` | `interceptLethalDamage` 里两行抄写（纯读取，**无 `Random`**） |
| `actors/Char.java` | 删掉 `aimMarkedBeforeHit` 局部变量与攻击后的 `onKillByHero` 收尾块 |
| `items/weapon/melee/PalermoFencing.java` | 删掉 `tryExecute` 里那句死代码与随之无用的 import |
| `levels/Level.java` | 新增 `handlesChasmFall()`（默认 false） |
| `levels/features/Chasm.java` | `heroFall` 的早返回分支 + `heroRiftReturn()`（新）+ `ScrollOfTeleportation` import |
| `levels/MiningLevel.java` | 新增 `carveVoid(Level, Room, int)` + 覆写 `handlesChasmFall()` |
| `levels/rooms/quest/MineSmallRoom.java` | CRYSTAL 分支末尾 `carveVoid( level, this, -1 )` |
| `levels/rooms/quest/MineLargeRoom.java` | `carveVoid( level, this, level.pointToCell(p) )`（保护水晶守卫） |
| `levels/rooms/quest/MineGiantRoom.java` | `carveVoid( level, this, level.pointToCell(p) )`（保护水晶尖塔） |
| `levels/painters/MiningLevelPainter.java` | `decorate` 先快照房间虚空、`super.decorate` 后只还原房间之间那批 |
| `assets/messages/levels/levels{,_zh,_zh-hant}.properties` | `levels.features.chasm.rift_return` 三语 |

## 15.6 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_marked_kill_routes.py`（新） | **78 条**断言（A~I 九组：字段／不进存档／消费、闸门抄写的位置与纯读取、旧判据撤除与全仓调用点唯一、`handlesChasmFall` 默认与覆写、`heroFall` 早返回与 `heroRiftReturn`、`carveVoid` 地形规则、房间调用点与画师、三语文本、行为层）+ **78 条反例自测（0 条抓不到错）** |
| `_chk/MarkedKillProbe.java`（新，行为探针） | **23 条断言全过**（0 条环境跳过）：① 带 `AimHeartMark` 的 `Rat` 以 **`Chasm.class`（非英雄击杀）** 为 `cause` 死亡 ⇒ 荣耀凯旋真的回血，且 `die()` 走完后 buff 已被 `onRemove` 摘掉（证明钩子跑在摘 buff **之前**）② 带 `InstructionTarget` ⇒ 神谕庇佑给盾，对照组不给 ③ 不挂 live、只置 `geburaAimMarked` ⇒ 回落分支可达且用掉即清 ④ 负向对照什么都不发生 ⑤ 真调闸门 ⇒ 两字段被抄下、`geburaGrace` 为真；关掉考验则返回 false 且字段保持初值 ⑥ 同 seed 200 个随机数逐位相同（穿插 20 次闸门调用） |
| 单文件 `javac`（11 文件，`-Xlint:all`） | **EXIT=0**（0 错误；43 条告警全是既有的 `Char.java` rawtypes/lossy 等，无一落在改动行） |
| `check_utf8_all.py` / `check_unused_imports.py` | OK（1472 文件）/ ALL PASS |
| 行尾 | `Mob.java` / `Trials.java` / `PalermoFencing.java` 保持 LF；`Char/Level/Chasm/MiningLevel/Mine*Room/MiningLevelPainter` 保持 CRLF |

⚠️ 探针环境三处必踩（已写进探针注释）：`new MiningLevel()` 会经 `RegularLevel.<clinit>` 读 `Game.version`
（脱游戏为 null ⇒ `ExceptionInInitializerError`，先赋个版本号）；`Level.mobs` 只在 `Level.create()` 里 new
（手工补 `new HashSet<Mob>()`）；脱游戏没有 `HeroSprite` ⇒ 把 `hero.lvl` 抬到 `> maxLvl` 让 `exp == 0`，
跳过 `destroy()` 里那句 `hero.sprite.showStatusWithIcon(...)`。

## 15.7 待人工验证

1. 拇指点满「荣耀凯旋」+ 学「瞄准心脏」：对带标记的怪，**在 GEBURA 考验下**把它打死 ⇒ 怪进濒死无敌，
   计时结束真倒下那一刻**应当回血**（修复前不回）。不带标记的怪不该回血。
2. 食指「击杀指令目标」（`TASK_KILL_TARGET`）任务：在 GEBURA 考验下击杀指令目标 ⇒
   指令**应当判完成**、神谕庇佑给盾（修复前不完成）。
3. 关掉 GEBURA：与旧版逐字一致（两个留存字段恒为初值）。
4. 水晶任务矿洞层：房间里应当看到**地板被挖成虚空**（墙体与矿脉完好），外圈仍有一整圈地板可绕行；
   水晶守卫／尖塔所在格及其八邻格是地板。
5. 踩进虚空：**不换层**、被传送回本层某处、扣坠落伤害并显示「一股魔法乱流把你送回了这一层某处」；
   喝过羽落秘药时不掉血。矮人任务（`GNOLL`）层的矿洞**不应**出现虚空，落坑行为与原版一致。

---

# 2026-09-26（补）矿洞虚空地形两条收紧 ＋ 新调试道具「矿洞跃迁符」

> 上一条「带着标记倒下 ＋ 虚空地形」见本文件的 `2026-09-26` 章节。这一条是它落地后的**实测补记**：
> 用行为探针真建关卡跑 `carveVoid` 之后，发现两处与注释宣称不符（或不够好）的地方，收紧；
> 同时为「反复看地形」加了一个调试道具。

## 一、`MiningLevel.carveVoid` 的两条收紧

### 1. 圆盘之前是**没有夹取**的 ⇒ 「外面留一整圈地板」其实不成立

原写法是「圆心在深内区（房间内圈再各缩 2 格）、半径 1~3 的切比雪夫圆盘」，
但**圆盘本身没有被夹在深内区里**。圆心落在深内区角落时，圆盘会往房间内墙方向溢出 `radius` 格：
20×20 探针房 20 次采样里有 **146 格**深渊落在深内区之外，最远直接贴到房间内墙那一圈。

后果是玩家**沿墙走一步就可能掉下去**（水晶任务下只是被送回本层 + 摔伤，但仍然很别扭），
而注释里写的「不封死任何人」并没有依据。

**修法**：逐格加一句夹取，整块圆盘落进深内区：

```java
if (x < left || x > right || y < top || y > bottom) continue;
```

夹掉的部分**不参与随机数消耗**（放在 `Random.Int(2)` 之前），所以「同 seed 逐格可复现」依旧成立。

### 2. 外圈 50% 抽取会留下**孤零零的单格陷阱**

外圈（`d == radius`）按 50% 概率保留是照 `FissureRoom` 的边缘不规则写法。但 `FissureRoom` 是在房间里拉长线，
这里是个圆盘 ⇒ 「相邻两格都被抽掉」的角落格会变成一个**不属于任何连通块**的单格深渊。
20 次采样里 17 次出现多块（最多 4 个孤立角格）。

**修法**：分两趟。第一趟挖实心核（`d < radius`）并把外圈候选存进 `fringe`；
第二趟只有「四邻里至少一个也是深渊」的外圈格才真挖：

```java
for (int cell : fringe){
    if (map[cell - 1] == Terrain.CHASM || map[cell + 1] == Terrain.CHASM
            || map[cell - w] == Terrain.CHASM || map[cell + w] == Terrain.CHASM){
        Painter.set( level, cell, Terrain.CHASM );
    }
}
```

`±1` / `±w` 一定在数组内：地图形状保证深内区外面还有「房间内圈 + 边界墙」。
`Random.Int(2)` 的**消耗顺序与格序完全没变**，同 seed 仍逐格可复现。

### 3. ⚠️ 一条**不是** bug 的现象：矿脉会把虚空切成几块

`MineSmallRoom.paint` 是**先撒矿脉再挖虚空**，而挖虚空只吃 `EMPTY` / `EMPTY_DECO`
⇒ 留在原地的矿脉（`MINE_CRYSTAL`）就成了虚空里的**踏脚石**，一块圆盘因此可能被切成几块。

这是「只吃地板、不动矿脉」这条规则的正确结果，**不是**回归。所以探针把「单块性」拆成两段证：
无阻挡的干净地板上必须恰好 1 块；带矿脉/墙体的场景只证「只吃地板」与「不越界不贴墙」。

## 二、行为探针 `_chk/MiningVoidProbe.java`（26 条断言）

静态结构断言证不了下面这些事，所以真建关卡跑（`Game.version` / `Level.mobs·heaps·blobs` 要手工补，
否则一开头就 NPE）：

| 段 | 证什么 |
|---|---|
| ① | `handlesChasmFall()` 只对 `CRYSTAL` 为真；`GNOLL` / `FUNGI` / 未开始 / 基类 `Level` 全 false |
| ② | 只吃地板 ／ 贴内墙那圈永远是地板 ／ 整块夹在深内区 ／ 干净地板上恰好 1 块 ／ 与内墙至少隔 1 格 |
| ③ | `protectedCell` 及其八邻格一次都没被挖，同时仍挖出了虚空（不是「因保护而干脆不挖」） |
| ④ | 深内区全铺墙体 ⇒ `map[]` 逐格不变（不产生半截挖掘） |
| ⑤ | 深内区为空 ⇒ 早退不动地形；深内区仅 2×2 ⇒ 夹紧后仍挖得出且不越界 |
| ⑥ | ⭐ **「只写 `map[]` 安全」的前提实证**：不调 `buildFlagMaps()` 时深渊格 `pit[]` 全 false（幽灵格风险真实存在），调完之后逐格变 true、`passable[]` 变 false、普通地板一格没被误标 |
| ⑦ | 同 seed 两次 `carveVoid` 的 `map[]` 逐格相同 |
| ⑧ | 从房间内环出发能走遍所有非墙非深渊格 ⇒ 虚空不会把房间切成孤岛 |

探针输出接进 `_chk/verify_marked_kill_routes.py` 的 **J 组**（`_miningvoid.out`，含反例自测）；
该脚本 F 组同期补了 F16（夹取）/ F17（孤立格回收）两条结构判据。

## 三、新调试道具「矿洞跃迁符」`items/CrystalMineWarp.java`

需求原话：*「创作一个道具，能快速传送到水晶任务矿洞层，方便测试地形生成。」*

放在 **`items` 根包** ⇒ F2 调试窗「杂项」与调试控制台 `ScanProvider.shallow("杂项", Item.class, PKG + "items")`
都会自动收录，**不需要注册**；同时它**不进任何掉落池 / 商店 / 开局奖励**
（判据里钉死「全仓除本文件外零引用」）。与既有的 `TestPortal` 同属「调试道具」家族：
`isIdentified() = true`、`isUpgradable() = false`、无限次使用不消耗。

### 三个动作

| 动作 | 行为 |
|---|---|
| `WARP`（跃迁，默认动作） | 进矿洞；目标深度在 11→12→13→14 之间轮换，**保留**该层地形（同深度可复现 ⇒ 适合改完代码做前后对照） |
| `REROLL`（重 roll） | 同样轮换深度，但先 `Dungeon.generatedLevels.remove(...)` 清掉生成记录 ⇒ 逼引擎走 `newLevel()` 重新生成，每按一次都是新地形 |
| `RETURN`（返回） | 回到出发前那一层的**原坐标**；只在矿层里出现 —— 矿层唯一的出口楼梯要「镐子 ＋ 任务进度」，不给回程会把人卡在矿里 |

### 三处必须踩准的地方

1. **虚空地形只在 `Blacksmith.Quest.Type() == CRYSTAL` 下生成**（房间 `paint()` 与 `handlesChasmFall()` 都读它）
   ⇒ 道具会**临时把任务种类顶成水晶**，并在「返回」时按离开前的值还原。
   `Blacksmith.Quest.setType(int)` 是本次新加的钩子：只开放这一个 setter，`type` 字段仍是 `private static`。
2. **目标深度必须显式给 11~14**：`Dungeon.newLevel()` 里只有 `branch == 1 && depth ∈ [11,14]` 才产出 `MiningLevel`，
   否则掉到 `DeadEndLevel`。所以哪怕玩家正站在 7 层（监狱）用符，也照样落到真正的矿层。
3. **跳层走既有的 `InterlevelScene.Mode.RETURN`**（与 `TestPortal` / 传送卷轴 / 灯芯草同一条路），
   落点 `returnPos = -1` 交给 `Dungeon.switchLevel` 回退到该层入口（矿层入口就是 `MineEntrance` 那格楼梯），
   并先调 `Level.beforeTransition()`。
   **没有**去伪造 `LevelTransition`——那条路要求目标层存在对应类型的过渡点，而「从任意层跳进任务分支」并不满足这个前提。

### 已知副作用（都是刻意的）

- 「重 roll」会洗掉该矿层已探索的地图（调试道具，属预期行为）；
- 任务种类会被顶成水晶、离开时还原；但若玩家**没按返回**就退出，任务种类会保留为水晶。

### 核验

`_chk/verify_crystal_mine_warp.py`（32 条断言 ＋ 反例自测），覆盖：道具本体与调试契约、
`actions()` 里「返回」的门控、跃迁/重 roll 路径的七个必踩点、返回路径的还原与落点、
`setType` 钩子的只读性、三份 `items*.properties` 文本键、以及「两个调试入口仍是 items 根包浅层扫描 ＋ 零掉落渠道」。

---

# 2026-09-26（续）考验 NETZACH（胜利）＋ HOD（荣耀）：命中 / 闪避按区递增 ＋ 距离淡出 / 150 回合精英化

## 16.1 需求原文

> 希望这两个考验的命中 / 闪避属性加成按照区域提升，例如一区 +10%，五区则 +50%（描述文本同步修改）

两条规则的完整口径（含上面这条「按区递增」的追加要求）：

| 考验 | 序号 | 规则 |
|---|---|---|
| **NETZACH**（胜利） | 6 | 所有**敌方单位**的**闪避**按区递增（一区 +10% …… 五区 +50%）；距离英雄**超过 2 格**半透明、**超过 4 格**完全不可见（纯视觉） |
| **HOD**（荣耀） | 7 | 所有**敌方单位**的**命中**按区递增（同口径）；某个敌方单位**存活超过 150 回合**后获得一次**随机精英效果**（不可叠加） |

## 16.2 定位：两条规则各自落在哪个「收口类别」

§6 / §7 / §8 已经把考验规则归纳成三类落地方式（**数值出口收口** / **事件型规则** / **挡路型规则**）。
本轮两条都属**数值出口收口**（改属性的唯一取数点），HOD 另挂一条**常驻 buff 计时**（与 CHESED 的
`ChesedMend` 同型）。所以两次实现都没有新增「入口」，只改了既有的计算点与一个渲染点。

## 16.3 共用口径：区域（1~5）与按区递增倍率

「区域」＝地牢的**五章**结构，口径直接取 `Dungeon.scalingDepth()`（不是 `depth`：升天时两者不同）：

```java
public static final float REGION_BONUS_STEP = 0.10f;
public static final int MAX_REGION          = 5;

public static int currentRegion(){                      // 深度 1~5 ⇒ 1 区 …… 21~25 ⇒ 5 区
    return Math.min( MAX_REGION, Math.max( 1, (Dungeon.scalingDepth() - 1) / 5 + 1 ) );
}
public static float regionBonusMultiplier(){            // 一区 1.10 …… 五区 1.50
    return 1f + REGION_BONUS_STEP * currentRegion();
}
```

- **唯一口径**：HOD 的命中与 NETZACH 的闪避都读这一个方法，改数值只动 `REGION_BONUS_STEP` 一处。
- **升天封顶**：`scalingDepth() == 26`（升天层）被 `MAX_REGION` 夹到五区，不会算出 1.60。
- **不加随机数**：纯算术，同种子逐位一致。

## 16.4 NETZACH（胜利）

### 收口点一（数值）：`Trials.finalEvasion` / `finalAccuracy`

⚠️ **全作有三处在算「命中 vs 闪避」，各自复刻了一整套乘区**（它们各自的 copy-pasta 注释也承认了）：

| 站点 | 用途 |
|---|---|
| `Char.hit` | 真正的命中判定 |
| `Talent.dodgeAsDamageReduction` | 把「本可闪避的概率」折算成减伤 |
| `Stone.proc`（磐岩附魔） | 同上 |

只改 `Char.hit` 会让**换算出的概率与真实命中率漂移**。因此把「读属性 + 乘考验倍率」收进两个方法，
三处一律改调它们（这就是为什么 HOD 的命中与 NETZACH 的闪避共用同一对收口）：

```java
public static float finalAccuracy( Char attacker, Char defender ){
    if (attacker == null) return 0f;
    return attacker.attackSkill( defender ) * accuracyFactor( attacker );   // HOD
}
public static float finalEvasion( Char defender, Char attacker ){
    if (defender == null) return 0f;
    return defender.defenseSkill( attacker ) * evasionFactor( defender );   // NETZACH
}
```

两个倍率函数都带同一组门控：**非 `Alignment.ENEMY` 返回 1**、**本考验未开返回 1**（与 HOKMA / CHESED 同判据）。
⇒ 收口点对英雄、对未开考验的局**逐字节等价于原版**。

### 收口点二（视觉）：`CharSprite.draw()`

距离用 `Dungeon.level.distance`（切比雪夫，与原版其余「格数」口径一致）：`≤2` 全不透明 / `3~4` 半透明（`0.5`）/ `>4` 全透明。

做法是**只在绘制这一帧**把 `am` / `aa` 临时乘上系数、`super.draw()` 之后立刻写回：

```java
float fade = Trials.enemyFade( ch );
float amBak = am, aaBak = aa;
if (fade < 1f) { am *= fade; aa *= fade; }
... // renderShadow + super.draw()
if (fade < 1f) { am = amBak; aa = aaBak; }
```

**为什么不写在 `update()` / `resetColor()`**：真正的隐形走 `AlphaTweener`（它会**持续写** alpha）、
受击闪光走 `hardlight`（**只碰颜色不碰 alpha**）⇒ 只有「只影响这一帧、不落任何持久状态」才不会与这两者打架。
`fade == 1f`（未开考验 / 不是敌方 / 贴身 / 无英雄）时两个 `if` 都不执行，零影响。

**「看不见」是给玩家的难度，不是给系统的隐身**：不改命中判定、不写 `invisible`、不碰 AI。

### 血条：`CharHealthIndicator`

`visible` 追加 `&& Trials.enemyFade( target ) > 0f` —— 否则完全淡出的单位会被一条**浮空血条**暴露位置。

## 16.5 HOD（荣耀）

### 收口点一（数值）

与 NETZACH 共用 `finalAccuracy` / `finalEvasion`（见 16.4），HOD 只负责命中侧倍率 `accuracyFactor`。

### 收口点二（计时）：常驻 buff `HodGlory`

需求是「**任意敌方单位存活超过 150 回合**」⇒ 计时器交给 `actors/buffs/HodGlory`：

- **常驻 buff**（不限期），由 `Trials.bindMobPassives` 在 `Mob.onAdd()` 里幂等重挂（与 `ChesedMend` 同型）。
  用 buff 而不是写进 `Mob.act()`，是因为**一部分单位的 `act()` 覆写不调 `super.act()`**
  （`CrystalSpire` / `Masterpiece` / `MobSpawner` / `Sheep` 等），而 buff 有自己的回合，谁都漏不掉；
  顺带它随存档保存（`Char.storeInBundle` 存的就是 buffs 列表），读档后接着原来的计时。
- **计时口径严格照「存活」**：只有在「是敌方阵营 ＋ 活着 ＋ 本考验仍开着」时才 `++turns`；
  一旦不再适用就**清零**（不再算存活），被腐蚀 / 被招募 / 被魅惑成友方后立刻停表，变回敌方再从 0 重数。
  ⇒ 每次现判 `alignment`，而不是挂 buff 时判一次。
- **数满自摘**：`++turns > HOD_ELITE_TURNS` 时调 `Trials.grantHodElite(mob)` 然后 `detach()`。
  这个 buff 的使命就是「数到 N 发一次精英」，发完即止、绝不再重掷 —— 「不可叠加」的一半保障在这里。
- ⚠️ **`type` 只能是 POSITIVE**（与 `ChesedMend` 同一条红线）：`Mob.Sleeping` 把 `type == NEGATIVE`
  的 buff 当成「被打醒」的信号 ⇒ 标成 NEGATIVE 会让**全图怪物一入场就醒来并进入警戒**。

### 收口点三（晋升）：`Trials.grantHodElite`

```java
public static boolean grantHodElite( Mob mob ){
    if (mob == null || !Dungeon.isTrialled( HOD )) return false;
    if (mob.alignment != Char.Alignment.ENEMY) return false;
    if (!mob.buffs( ChampionEnemy.class ).isEmpty()) return false;      // 不可叠加
    ChampionEnemy champ = Buff.affect( mob, ChampionEnemy.randomChampionClass() );
    if (champ != null && mob.sprite != null)
        mob.sprite.showStatus( CharSprite.POSITIVE, Messages.titleCase( champ.name() ) );
    return true;
}
```

- **「随机精英」复用原版冠军系统**的六种效果：为此把 `ChampionEnemy.rollForChampion` 里的
  `Random.Int(6)` 分支抽成 `public static Class<? extends ChampionEnemy> randomChampionClass()`，
  两个调用点共用同一张映射。抽出是**纯重构**：`rollForChampion` 的随机流位置一字未变。
- ⚠️ **必须用 `buffs(ChampionEnemy.class)` 而不是 `buff(...)`**：后者的实现是 `b.getClass() == c`
  的**精确类匹配**，对抽象基类永远返回 `null` ⇒ 会写出「明明有精英却判成没有、再发一个」的叠加 bug。
- **「不可叠加」＝ 身上已有任一 `ChampionEnemy`**（挑战 `CHAMPION_ENEMIES` 刷出来的、或本考验早先发过的）。
- **反馈**：晋升时用精英自己的名字弹状态字（原版文本键已存在，如 `actors.buffs.championenemy$blazing.name`）。
- ⚠️ **这里会消耗一次 `Random.Int(6)`**（运行时事件）。与 BINAH「不动随机流」的口径**并不冲突**：
  那条红线针对**关卡生成那一刻**（保证同种子布局逐字节一致），而本效果发生在生成之后，
  与怪物掉落 / 攻击掷点同属运行时随机 —— 不存在「开了本考验、布局就变」的问题。
- **挂载顺序**：`bindMobPassives` 里 HOD 那条**必须排在 CHESED 的 `mob.EXP <= 0` 提前返回之前**
  —— 需求是「任意敌方单位」，与「给不给经验」无关（`EXP == 0` 的怪也要数表）。

## 16.6 文本（zh ＋ en）

`trials.netzach_desc` / `trials.hod_desc` 从占位文案换成正式描述，两侧同步：

- zh：`所有_敌方单位_的闪避属性随所在_区域_提升：_一区 +10%_，每深入一区再 +10%，至_五区 +50%_。\n\n_距离英雄超过 2 格_的敌方单位会变得半透明，_超过 4 格_时_完全不可见_。`
- 换行是**字面 `\n`**（两个字符），分段用 `\n\n` —— 与全仓 `_zh.properties` 一致。

（`misc.properties` 只有 en + zh 两份，无 `_zh-hant`。）

## 16.7 落点

| 文件 | 改动 |
|---|---|
| `Trials.java` | 新增区域口径（`REGION_BONUS_STEP` / `MAX_REGION` / `currentRegion` / `regionBonusMultiplier`）；NETZACH 三常量 ＋ `evasionFactor` / `enemyFade`；HOD 常量 ＋ `accuracyFactor` / `finalAccuracy` / `finalEvasion` / `grantHodElite`；`bindMobPassives` 泛化（HOD 计时器优先挂载，不再被 CHESED 的 EXP 早返回挡住）；类注释进度更新 ＋ `HodGlory` import |
| `actors/buffs/HodGlory.java` | **新建**：POSITIVE 常驻 buff，`turns` 进存档，数满晋升并自摘 |
| `actors/buffs/ChampionEnemy.java` | 抽出 `public static randomChampionClass()`（`rollForChampion` 与 `grantHodElite` 共用） |
| `actors/Char.java` | `hit()` 改调 `Trials.finalAccuracy` / `finalEvasion` |
| `actors/hero/Talent.java` | `dodgeAsDamageReduction` 改调同两个收口 |
| `items/armor/glyphs/Stone.java` | `proc()` 改调同两个收口 ＋ `Trials` import |
| `sprites/CharSprite.java` | `draw()` 里按 `enemyFade` 临时乘 / 还原 `am`·`aa` ＋ `Trials` import |
| `ui/CharHealthIndicator.java` | `visible` 追加 `&& Trials.enemyFade( target ) > 0f` ＋ `Trials` import |
| `assets/messages/misc/misc{,_zh}.properties` | `trials.netzach_desc` / `trials.hod_desc` 换成正式描述 |

## 16.8 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_hod_netzach.py`（新） | **140 条**断言 ＋ **17 条反例自测**，五层：① 源码结构（常量、`callers_of` 调用点唯一、`seq_ok` 顺序、`body_has_random` 无随机）② `javap -p` 签名 ③ `javap -c` 字节码（无 `Random`；`grantHodElite → detach` 顺序；`Char.hit` 的取数发生在 `INFINITE_EVASION` 之前；`draw` 里取系数早于 `super.draw`、`am`/`aa` 还原晚于 `super.draw`）④ 文本层（zh ＋ en 键在、非占位、CRLF 完整、字面 `\n`）⑤ 行为层（探针输出全绿、七段都跑到） |
| `_chk/HodNetzachProbe.java`（新，行为探针） | **52 条断言全过**（0 条环境跳过）：① 区域→倍率映射（含升天封顶）② 命中 / 闪避倍率门控 ③ `finalAccuracy` / `finalEvasion` 取数 ④ `enemyFade` 距离阈值（`≤2→1` / `3~4→0.5` / `>4→0`）⑤ `grantHodElite` 不可叠加 ⑥ `HodGlory` 计时边界（第 150 回合不发 / 第 151 回合发 ＋ 自摘；不适用时清零）⑦ `randomChampionClass` 恰好覆盖 6 种 |
| 单文件 `javac`（`-Xlint:all`，8 文件） | **EXIT=0**（0 错误；告警全是既有的本仓 rawtypes / lossy 等，无一落在改动行） |
| 回归 | 在范围内的核验脚本全绿：`verify_chesed_gebura` 156/0、`verify_hokma_delay` 36/0、`verify_binah_gen` 116/0、`verify_gebura_drop` 37/0、`verify_marked_kill_routes` 87/0、`verify_holy_card` 58/0、`verify_fun_challenges` 81/0、`verify_trials_tree_ui` 112、`verify_tree_trials_icons`、`verify_dwarfking_chesed` 27/0、`verify_boss_phase_scales` 49/0 |
| 行尾 | `Trials.java` / `HodGlory.java` 保持 LF；`ChampionEnemy/Char/Talent/Stone/CharSprite/CharHealthIndicator` 保持 CRLF；`misc{,_zh}.properties` 保持 CRLF 且无裸 LF |

⚠️ 探针环境三处必踩（已写进 `HodNetzachProbe` 注释）：`Game.version` 必须先赋值（脱游戏为 `null` ⇒ NPE）；
`Mob` 子类必须 `HP = HT > 0` **且** `enemySeen = true`（否则 `Mob.defenseSkill` 经 `surprisedBy` 返回 0、
`HodGlory.act` 因 `isAlive()` 为假而跳过）；手工 new 的 `Level` 要自带 `width` / `height` / `length`。

## 16.9 待人工验证

1. 开 NETZACH：一区敌方闪避 +10% ⇒ 英雄打空的比例略升；五区 +50% ⇒ 明显更常打空。
   描述文本（考验界面）应为正式文案而非占位。
2. 开 NETZACH：把怪拉到 **2 格内**完全不透明、**3~4 格**半透明、**>4 格**看不见（血条一并消失）；
   走到远处再靠近，透明度应随距离实时恢复。
3. 开 HOD：敌方命中按区递增（一区 +10% ⇒ 五区 +50%）。
4. 开 HOD：让一只怪**存活超过 150 回合**（如贴着它绕圈不打）⇒ 它应弹出一个**精英名字**的状态字并获得
   对应效果；再等下去**不会**再发第二个（已有精英的怪永不叠加）。
5. 同时开 NETZACH ＋ HOD：两个倍率叠加（闪避与命中各自独立生效）。
6. 关掉两条考验：命中 / 闪避 / 绘制 / 血条与旧版逐字一致。

---

# 2026-09-26（续二）考验 YESOD（根基）：禁自然鉴定 ＋ 商店补一张鉴定卷轴 ＋ 地面物品一律「未知」；NETZACH 淡出阈值收紧到 1 / 3 格

## 17.1 需求原文

> 请对 NETZACH 进行一些修改：变淡 / 完全不可见的距离从 2 格 / 4 格之外变为 1 格 / 3 格之外（同步描述文本）。
> 随后请实现 YESOD 考验。描述：物品无法自然鉴定（鉴定卷轴和感知符石的鉴定不受影响），商店中额外出售
> 一张鉴定卷轴。地面上的所有物品均显示为未知贴图和描述。
>
> 备注：前半段效果中，被禁止的鉴定包括武器、护甲、戒指、法杖的随使用过程自然鉴定；**不**包括卷轴、
> 药水等消耗品的使用时、投掷时鉴定，也**不**包括卷轴的拆分为符石时鉴定，但是**包括**卷轴和药水的
> 直接分解成炼金能量的附加鉴定；后半段效果中，未知的贴图位置为 xy(16,2)，大小 10×15（形状为问号），
> 使用检视功能查看时显示描述为「你不知道这里是什么。」效果仅影响地面上的物品，不影响宝箱和骷髅堆等
> 容器，不影响背包中物品贴图的显示和查看。

三处边界由用户在实现前逐条确认：

| 待定项 | 用户选择 |
|---|---|
| 商店货架（`Heap.Type.FOR_SALE`）是否也变问号 | **也变问号，但价格正常显示** |
| 地面的金币堆是否变问号 | **也变**（`GOLD` 就是普通 `HEAP`，无需特判） |
| 「自然鉴定」的范围 | **只禁四类装备**（武器 / 护甲 / 戒指 / 法杖）；三个「直觉」天赋与神器装备时的自动鉴定**保留** |

## 17.2 定位：这是「三条互相独立的收口线」，不是一条

YESOD 的三段效果落点完全不同，任一段漏掉都**不会报错**（只是效果不全），所以要按收口线逐个封：

| 效果 | 收口线 | 唯一入口 |
|---|---|---|
| 禁自然鉴定 | **闸门式**（在既有判定处插 `else if`） | `Trials.naturalIdDisabled()` |
| 炼金分解附带的鉴定 | **闸门式** | `WndEnergizeItem.energize()`（全作唯一「分解成炼金能量」入口） |
| 地面物品显示为未知 | **显示出口收口**（贴图 / 名字 / 描述三处共用同一判据） | `Trials.hidesGroundHeap( Heap )` |
| 商店补一张鉴定卷轴 | **生成期注入** | `ShopRoom.placeItems()`（**不是** `generateItems()`） |

**故意不碰**（反向判据，写进核验脚本）：

- `Item.identify()`：全作鉴定**总闸**，连英雄初始装备（`HeroClass.initHero` 里几十处）都走它。做成总闸会把
  「开局自带的武器本该是已知的」一起拦掉（而且开局时 `Dungeon.trials` 已经生效）⇒ **绝不能**在这里加闸门。
- `Scroll.ScrollToStone`（卷轴拆符石）、`ScrollOfIdentify` / `ScrollOfDivination` / `StoneOfIntuition`；
- 三个「直觉」天赋（`Talent`）与 `Artifact` 装备时的自动鉴定；
- `ShopRoom.spacesNeeded()`：它读 `itemsToSpawn.size()` 来算**房间最小尺寸** ⇒ 额外那张卷轴若写进
  `generateItems()`，开关这个考验就会改变整层房间布局。

## 17.3 NETZACH：淡出阈值 2 / 4 → 1 / 3

只动两个常量与一条描述，`enemyFade()` 的判定顺序一字未改（`≤NEAR` ⇒ 1.0 / `≤FAR` ⇒ 0.5 / 其余 ⇒ 0）：

```java
public static final int NETZACH_FADE_NEAR = 1;   // 原 2：≤1 格完全不透明
public static final int NETZACH_FADE_FAR  = 3;   // 原 4：>3 格完全不可见
```

⇒ 中间带从「3~4 格」变成「2~3 格」。描述里的「超过 2 格 / 超过 4 格」（en：`more than 2 tiles` /
`more than 4 tiles`）同步改为 1 / 3。**「看不见」仍是纯视觉**：不写 `invisible`、不碰命中与 AI。

## 17.4 YESOD 收口点一：四类装备的自然鉴定

原版在四个地方「数够次数就自动鉴定」，形态完全一致（先判遗忘碎片，再 `identify()`）：

| 站点 | 行 |
|---|---|
| `items/weapon/Weapon.java` | `proc()`（L222） |
| `items/armor/Armor.java` | `proc()`（L553） |
| `items/rings/Ring.java` | `onHeroGainExp()`（L341） |
| `items/wands/Wand.java` | `wandUsed()`（L469） |

⚠️ **护甲那处不在 `absorb()` 里**：原版把「数满即鉴定」写在 `Armor.proc()`（它覆写 `Item.proc`），
`javap -p Armor` 里**根本没有** `absorb` 方法 —— 找站点时别按直觉搜签名。

四处统一插一个 `else if`，与遗忘碎片 `ShardOfOblivion.passiveIDDisabled()` 同口径（数满后只 `setIDReady()`）：

```java
} else if (Trials.naturalIdDisabled()){
    if (usesLeftToID > -1){
        GLog.p( Messages.get( Trials.class, "id_blocked" ), name() );   // ← 只差这一句提示
    }
    setIDReady();
} else {
    identify(); ...
}
```

**为什么是「再加一个 `else if`」而不是改 `setIDReady()` 或 `identify()`**：`setIDReady()` 是「数够了但还没鉴定」
的**共享状态标记**，`identify()` 是所有鉴定路径的共用出口（卷轴 / 符石 / 任务奖励都走）。在这两处动手会连带
影响本考验**不该**管的东西；只在「数满」这一条分支上加判别，作用域最窄。`ShardOfOblivion` 的分支**保持在前**
（它的优先级更高：碎片是「转为待鉴定」，本考验是「永不鉴定」）。

## 17.5 YESOD 收口点二：炼金分解附带的鉴定

`WndEnergizeItem.energize()` 是全作**唯一**的「分解成炼金能量」入口，它顺带做了一次鉴定（场景内是
`AlchemyScene.showIdentify(item)`，场景外是 `item.identify()`）。两处各加一个守卫：

```java
if (!item.isIdentified() && !Trials.naturalIdDisabled()){   // L161：不再弹「鉴定」提示
    ((AlchemyScene) ShatteredPixelDungeon.scene()).showIdentify(item);
}
...
if (!Trials.naturalIdDisabled()){                           // L171：场景外也不再自行鉴定
    item.identify();
}
```

**能量照给**：`Dungeon.energy += item.energyVal();` 在守卫**之前**，分解的收益一点不少 —— 变的只是
「不再顺手告诉你这是啥」。这与「卷轴的直接使用 / 投掷鉴定不受影响」并不冲突：用户区分的是
**分解**这条路径，而不是卷轴这个品类。

## 17.6 YESOD 收口点三：地面物品一律显示为「未知」

三处显示出口读同一个判据，缺一处就会出现「图标是问号、名字却照旧」这类半吊子状态：

| 出口 | 文件 | 做法 |
|---|---|---|
| 贴图 | `sprites/ItemSprite.java` L268 | `view( heap )` 的 `case HEAP: case FOR_SALE:` 里，藏则 `view( ItemSpriteSheet.UNKNOWN_ITEM, null )`，否则照旧 `view( heap.peek() )` |
| 名字（列表 / 检视标题） | `items/Heap.java` L373 | `title()` 开头：`type == HEAP && hidesGroundHeap(this)` ⇒ 返回未知文本（**排在 `switch(type)` 之前**） |
| 描述（检视窗） | `windows/WndInfoItem.java` L52 | `WndInfoItem( Heap )` 的未知分支：`IconTitle(heap)` ＋ 未知文本，**排在「普通堆取 `peek()`」之前** |

判据本身（`Trials.hidesGroundHeap`）只认两种堆型：

```java
if (heap == null || heap.isEmpty()) return false;
if (!Dungeon.isTrialled( YESOD )) return false;
return heap.type == Heap.Type.HEAP || heap.type == Heap.Type.FOR_SALE;
```

⇒ 宝箱 / 上锁宝箱 / 水晶箱 / 墓穴 / 骷髅堆 / 遗骸（`CHEST`、`LOCKED_CHEST`、`CRYSTAL_CHEST`、`TOMB`、
`SKELETON`、`REMAINS`）**全部不受影响**；背包、掉落窗、炼金窗里的物品也都不走这三处出口。

### 贴图：`UNKNOWN_ITEM = xy(16, 2)`，取样矩形 10×15

`ItemSpriteSheet.java` L111 加常量、L126 加矩形：

```java
public static final int UNKNOWN_ITEM    =  xy(16, 2);
assignItemRect(UNKNOWN_ITEM,    10, 15);
```

实测 `items.png`（256×800）第 16 列第 2 行：非透明像素 121 个，包围盒 **正好 x 0..9 / y 0..14 = 10×15**，
**没有透明外边距** ⇒ 10×15 的矩形与图形自然尺寸**逐像素相等**，显示时不会被推偏、也不会被裁。
（为什么必须精确：`assignItemRect` 的 `w/h` 就是 `Image.width()/height()`，与所有按自然尺寸排版的地方耦合。）

**问号图标不会误触发附魔流光**：`view(Heap)` 在 `switch` 之前就把 `viewItem` 置 `null`，我们走的是
`view(UNKNOWN_ITEM, null)` 这条两参重载，`viewItem` 保持 `null`（核心素材本来也没有流光帧）。

### 两个「刻意保留」的细节

1. **金币也是 `Type.HEAP`** ⇒ 一并变问号，无需为它写任何特判。
2. **商店货架的价签仍显示物品名**：`Heap.title()` 的隐名分支只对 `Type.HEAP` 生效；`FOR_SALE` 保留
   `items.heap.for_sale` = 「**%2$s：%1$d金币**」（`%2$s` 就是物品名）。这正是用户选的「货架变问号、但价格
   正常显示」—— SPD 的价格标签字符串天然把物品名写在里面，想连名字一起藏得另改这条本地化文本。
   （`WndTradeItem extends WndInfoItem`，购买窗走 `super(heap)` ⇒ 标题是那条价签、图标是问号、
   正文是未知文本，**购买 / 偷窃按钮照常**，交易流程不受影响。）

## 17.7 商店额外出售一张鉴定卷轴

`ShopRoom.placeItems()` L138（**不是** `generateItems()`）：

```java
if (itemsToSpawn == null) itemsToSpawn = generateItems();
if (Dungeon.isTrialled( Trials.YESOD )){
    itemsToSpawn.add( new ScrollOfIdentify() );
}
```

⚠️ **必须写在 `placeItems()`**：`spacesNeeded()` 读 `itemsToSpawn.size()` 决定房间最小尺寸，而它在
**房间尺寸计算**阶段就被调用 ⇒ 卷轴若加进 `generateItems()`，开关这个考验就会**改变整层布局**
（商店变大 / 与邻房挤压）。放在 `placeItems()` 里，房间尺寸与考验**完全无关**（核验脚本专门钉住
「`spacesNeeded()` 的字节码里不出现 `Trials`」）。

## 17.8 文本（zh ＋ en）

`assets/messages/misc/misc{,_zh}.properties` 三处：`trials.yesod_desc` 从占位换成正式描述，
新增 `trials.yesod_unknown`（检视描述**与**列表标题共用同一条，避免两处口径漂移）与
`trials.id_blocked`（`GLog` 提示，带 `%s` 占位符），并同步改 `trials.netzach_desc` 的 1 格 / 3 格。

- zh：`物品_无法自然鉴定_：武器、护甲、戒指、法杖在_使用过程中_的自动鉴定，以及卷轴与药水_分解为炼金能量_时附带的那次鉴定，都_不会发生_。…`
- 换行是**字面 `\n`**（两个字符），分段用 `\n\n`；两份都保持 CRLF。

## 17.9 落点

| 文件 | 改动 |
|---|---|
| `Trials.java` | `NETZACH_FADE_NEAR 2→1`、`NETZACH_FADE_FAR 4→3`；新增 YESOD 段（类注释里的范围边界与三条收口线 ＋ `naturalIdDisabled()` L659 / `hidesGroundHeap()` L671 / `unknownGroundText()` L678）＋ `Heap` import |
| `sprites/ItemSpriteSheet.java` | `UNKNOWN_ITEM = xy(16,2)`（L111）＋ `assignItemRect(UNKNOWN_ITEM, 10, 15)`（L126） |
| `sprites/ItemSprite.java` | `view( Heap )` 的 `case HEAP: case FOR_SALE:` 加未知贴图分支 ＋ `Trials` import |
| `items/Heap.java` | `title()` 开头加隐名分支（只对 `Type.HEAP`）＋ `Trials` import |
| `windows/WndInfoItem.java` | `WndInfoItem( Heap )` 加未知分支（排在 `fillFields(heap.peek())` 之前）＋ `Trials` import |
| `items/weapon/Weapon.java` | `proc()` 加 `else if (Trials.naturalIdDisabled())`（L222） |
| `items/armor/Armor.java` | `proc()` 加同款分支（L553） |
| `items/rings/Ring.java` | `onHeroGainExp()` 加同款分支（L341） |
| `items/wands/Wand.java` | `wandUsed()` 加同款分支（L469） |
| `windows/WndEnergizeItem.java` | `energize()` 两处守卫（L161 / L171），能量照给 |
| `levels/rooms/special/ShopRoom.java` | `placeItems()` 里 YESOD 时补一张 `ScrollOfIdentify`（L138） |
| `assets/messages/misc/misc{,_zh}.properties` | `trials.yesod_desc` 换正式文案；新增 `trials.yesod_unknown` / `trials.id_blocked`；`trials.netzach_desc` 同步 1 / 3 格 |
| `_chk/patch_yesod_text.py` | **新**：幂等字节级文本批改器（断言每条键恰好命中一次、CRLF 不变、正文只有字面 `\n`） |
| `_chk/YesodProbe.java` | **新**：行为探针（37 断言） |
| `_chk/_build_yesod.sh` | **新**：11 文件单文件 `javac` ＋ 探针运行（并同步刷 `_chk/_javachk`） |
| `_chk/verify_yesod.py` | **新**：六层回归核验（131 断言 ＋ 15 条反例自测） |

## 17.10 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_yesod.py`（新） | **131 条**断言全过 ＋ **23 条反例自测**全过，**0 条 SKIP**。六层：① 源码结构（阈值常量、`callers_of` 引用清单唯一、`seq_ok` 顺序、守卫块里**有** `setIDReady()` 而**无** `identify()`、商店三方法各自该不该含 `YESOD`）② `javap -p` 签名 ③ `javap -c` 字节码（掩码**内联值**、4 站点「取数早于 `identify()`」、`WndEnergizeItem` 的 2 处守卫罩住 `showIdentify`、`Item.identify` 反向无 `Trials`）④ 文本层 ⑤ `items.png` 实测包围盒 ⑥ 行为探针 |
| `_chk/YesodProbe.java`（新，行为探针） | **37 条断言全过 / 0 FAIL**（2 段因 headless 无 `Gdx` 偏好而软跳过，以 `[INFO]` 留痕，**不静默**）：① 贴图常量与取样矩形 ② `naturalIdDisabled()` 门控 ③ `hidesGroundHeap` 真值表（含 6 种容器全 false）④ `Heap.title()`（软段）⑤ `unknownGroundText()`（软段）⑥ NETZACH 淡出阈值 1 / 3 ⑦ `Weapon.proc` 数满后的实跑（YESOD 开：只 `setIDReady()`、`levelKnown` 仍 false；关：确实走进 `identify()`） |
| 单文件 `javac`（`-Xlint:all`，11 文件） | **EXIT=0**（`_chk/_ie_yesod.log`） |
| `_chk/patch_yesod_text.py` | 幂等；en CRLF 285→287、zh 286→288；重跑自动 `[SKIP]` |
| `check_utf8_all.py` | 1474 文件全绿 |
| `check_unused_imports.py` | ALL PASS |
| 回归 | `verify_hod_netzach.py`（阈值已同步为 1 / 3）、`verify_trials_tree_ui.py`（101/0）、`verify_tree_trials_icons.py`（FAIL 0）全绿 |
| 行尾 | `Trials.java` 保持 LF；其余 `.java` 保持 CRLF；`misc{,_zh}.properties` 保持 CRLF 且无裸 LF |

⚠️ **三条核验踩坑（已写回脚本注释，勿重犯）**

1. **常量会被内联**：`Trials.YESOD` 是编译期常量（= 256）⇒ `naturalIdDisabled()` 的字节码是
   `sipush 256` ＋ `invokestatic Dungeon.isTrialled`，**没有** `getstatic Trials.YESOD`。判据要落在
   「内联值 == 源码读出的值」（见 `mask_value` / `has_int_const`）。
2. **`first_invoke(body, 'identify')` 会判反**：它先撞上前面那句 `ldc // String identify_ready`
   （字符串常量，不是调用）⇒ 必须用带冒号的 `'identify:()'`。
3. **`javap` 的 classpath 要顺延到构建产物**：`items/Item.class` 只存在于 `core/build/classes`
   （它被隐式引用，没被显式编进 `_chk/_javachk2`）⇒ 少了它就报「找不到类」，反向判据会静默变成
   `None` 而假 FAIL。另外 `callers_of` 数的是**引用**不是**定义**，所以清单里**不含**定义处 `Trials.java`。

## 17.11 待人工验证

1. 开 YESOD ⇒ 捡到未鉴定武器，一直用到「用够了」：日志应出现「自然鉴定已被_根基考验_禁用…」而**不是**
   直接鉴定；戒指按升级经验、法杖按挥动次数同理。
2. 开 YESOD ⇒ 打怪 / 升级后，武器 / 护甲 / 戒指 / 法杖的等级**始终**是未知（`?`），直到用鉴定卷轴 /
   占卜卷轴 / 感知符石。
3. 开 YESOD ⇒ 用**鉴定卷轴**、**占卜卷轴**、**感知符石**鉴定仍照常生效；**卷轴拆符石**照常；
   卷轴的**使用 / 投掷**鉴定照常。
4. 开 YESOD ⇒ 把一瓶未鉴定药水 / 一张卷轴**分解为炼金能量**：能量照给，但**不再**附带鉴定。
5. 开 YESOD ⇒ 地面掉落物一律「?」图标；长按检视显示「你不知道这里是什么。」；**金币堆**也变问号。
6. 开 YESOD ⇒ **商店**：货架图标是问号、价签仍是「物品名：价格金币」、购买 / 偷窃按钮照常可用；
   且**商店里多出一张鉴定卷轴**（关掉考验后恢复原样）。
7. 开 YESOD ⇒ **容器**（宝箱 / 上锁宝箱 / 水晶箱 / 墓穴 / 骷髅堆）图标与检视**完全不受影响**；
   背包里物品的贴图与检视也完全不受影响。
8. 开 NETZACH ⇒ 怪在 **1 格内**完全不透明、**2~3 格**半透明、**>3 格**看不见（血条一并消失）；
   考验界面描述文本应为 1 格 / 3 格。
9. 关掉两条考验 ⇒ 鉴定、商店、地面显示、淡出与旧版逐字一致。

---

# 2026-09-26（续三）四项二层天赋效果改写：变换无常 / 狩猎一餐 / 骨骸编织 / 纹身铭刻

## 18.1 需求原文

> 神谕代行者的二层天赋「变换无常」效果改为：+1 时重复判定闪避 1 次，+2 时重复判定闪避 2 次。
> （备注：即，进行是否被敌方命中的比大小判定时，额外随机闪避值一定的次数，取最高值来判断是否闪避成功）
> 拇指的二层天赋「狩猎一餐」从恢复武技充能改为恢复奥丁之眼充能；环指的二层天赋「骨骸编织」取消原本的
> 2~8 护甲值，效果改为获得 (0~角色等级) 的护甲（即判定减伤时，减伤数值增加 0~角色等级）；
> 中指的二层天赋「纹身铭刻」效果增强：在 +1/+2 时，消耗刻笔还会分别获得 25%/50% 当前生命上限的
> 「生命强化」（一种增加生命上限的 buff）。

一处边界由用户在实现前确认：

| 待定项 | 用户选择 |
|---|---|
| 「生命强化」的持续时长口径 | **复用既有「生命强化」**（`ElixirOfMight.HTBoost`，5 次升级后消失）；重复铭刻**覆盖式刷新**，不叠加 |

## 18.2 变换无常：为什么必须把判定搬出「闪避乘区」

旧口径是「闪避**数值** +10%/+20%」，落在 `Hero.defenseSkill()` 的乘区里。新口径是「掷点额外重复 1/2 次取
**最高值**」——**这是比较运算，不是线性缩放**：`max(R, R')` 的分布无法用 `R × k` 等价表达（乘区只能整体
平移/拉伸同一条分布）。所以这次改动**不是改一个系数，而是换收口点**：

| | 旧 | 新 |
|---|---|---|
| 收口点 | `Hero.defenseSkill()` 返回值 ×1.1 / ×1.2 | `Char.hit()` 里的 `defRoll` 掷点处 |
| 机制 | 闪避数值提升 | 掷点重复取最高 |
| 结果 | 单次掷点分布整体变大 | 单次掷点分布右偏（取 order statistic 的最大值） |

实现要点（`Char.java`）：

1. 把原来内联的掷点段**原样抽成** `private static float evasionRoll( Char defender, float defStat )`——
   九条既有乘区（Bless ×1.25 / Hex ×0.8 / Daze ×0.5 / 冠军敌人逐条 `evasionAndAccuracyFactor()` /
   `AscensionChallenge.statModifier` / BLESS 天赋 +3%·+5% / `FerretTuft` / 沉默的代价）一条不动地搬进去，
   **乘区只写一份**。核验脚本据此钉住「全文件 `Random.Float( defStat )` 恰好出现 1 次」防复制漂移
   （同类教训：`Talent.dodgeAsDamageReduction` 与 `Stone.proc` 各自复刻过一遍闭式版本）。
2. `hit()` 里改为：
   ```java
   float defRoll = evasionRoll( defender, defStat );
   if (defender instanceof Hero){
       int rerolls = ((Hero) defender).pointsInTalent( Talent.SHIFTING_FATE );
       for (int i = 0; i < rerolls; i++){
           defRoll = Math.max( defRoll, evasionRoll( defender, defStat ) );
       }
   }
   ```
   层数**直接取天赋点数**（+1 = 额外 1 次、+2 = 额外 2 次，共掷 2/3 次），`instanceof Hero` 门控使怪物
   不会误吃到英雄天赋。
3. `Hero.defenseSkill()` 里的旧分支**删除**，原地留一段说明注释（「比大小取最高值无法用闪避乘区等价表达，
   故本方法里没有分支，是刻意为之而不是漏改」）——避免后人当成漏改又加回去（那会变成**乘区 ＋ 重掷双重加成**）。

⚠️ 两个刻意保留的已知边界（都写进了 `evasionRoll` 的 javadoc）：

- `Talent.dodgeAsDamageReduction` 与 `Stone.proc` 里存在**闭式复刻**（把单次闪避掷点换成期望值算减伤）。
  闭式表达不了「取最高值」⇒ 靠蜕变卷轴让非神谕角色同时拿到「变换无常」与中指 T1「你犯规了」时，其减伤
  换算仍按**单次**掷点估算（**偏保守**，不会崩）。要精确化就得把闭式改成多次采样。
- `defStat` 仍由 `Trials.finalEvasion( defender, attacker )` 派生（考验 / 预知眼的 `INFINITE_EVASION`
  短路在此之前返回），所以本次改动**没有连累**考验或奥丁之眼的无限闪避。

## 18.3 狩猎一餐：奥丁之眼原本没有「外部补充能」入口

`OdinsEye.charge()` 是**自动充能**用的，自带 `0.25` 换算率（每次自动回能只加 1/4 点）。若直接复用它发
「1 点」，玩家会拿到 0.25 点。故新增一个**语义不同**的入口：

```java
public static void restoreCharge( Hero hero, float amount )   // 外部「发放」充能，1:1 计入
```

守卫链（全部**静默跳过**，不弹窗不报错）：`hero == null || amount <= 0f` → 魔免
（`hero.buff(MagicImmune.class) != null`，与神器自身的自动充能口径一致）→ 装备位查找
（`belongings.artifact` 找不到就找 `belongings.misc`；两处都没有 ⇒ 未装备，跳过）→ 被诅咒 → 已满
（`charge >= chargeCap`）。之后 `partialCharge += amount` 并 `while` 进位（支持 1.25 这种小数结转），
封顶时 `charge = chargeCap; partialCharge = 0;`，末尾 `Item.updateQuickslot()` 刷新快捷栏图标。

`Talent.onFoodEaten` 的 `HUNTING_MEAL` 分支由「武技充能」改为：
```java
float eyeCharge = hero.pointsInTalent(HUNTING_MEAL) == 2 ? 1.25f : 1f;   // +1 = 1 点 / +2 = 1.25 点
OdinsEye.restoreCharge( hero, eyeCharge );
```
⚠️ 同一方法里 **LOYALIST T2「专注一餐」（`FOCUSED_MEAL`）仍要充武技**（`MeleeWeapon.Charger`），所以核验
判据必须**收敛到 `HUNTING_MEAL` 那一段**，「整个方法里不许出现 Charger」这种写法会永远 FAIL。

## 18.4 骨骸编织：只改随机区间，不动结算链路

> ⚠️ **本节已被同日「续四」二次返工推翻（见 §19），仅作历史记录**：当时只把区间 `2~8` → `0~角色等级`，
> 但**仍沿用**「消耗素材时掷一次、把结果存进 buff」的方案 ⇒ 数值被冻结在消耗那一刻，且在图标上表现为
> 「层数」。现在 buff **不存任何数值**、没有层数，减伤时**实时**按角色等级取上界。

`BodyArtMaterial.execute()` 的 `AC_WEAVE` 分支（**当时**写法）：

```java
int armor = Random.IntRange( 0, hero.lvl );   // 旧：Random.IntRange( 2, 8 )
```

`IntRange` **两端闭区间**（故 0 与 `lvl` 都取得到）；`hero.lvl` 就是「角色等级」。
`armorLevel` 照旧写入 `BoneWeaving` buff（+1 持续 50 回合、+2 持续 100 回合的分支未动），结算仍在
`Char.drRoll()`：`dr += Random.NormalIntRange( 0 , buff(BoneWeaving.class).armorLevel );`——**当时没改结算**，
区间为 0 时该式自然得 0，不需要额外守卫。展示层（`iconTextDisplay()` / `desc()`）当时是动态读
`armorLevel`，只有 `BoneWeaving` 的 javadoc 需要同步（注释也是文档）。

> 口径提醒（当时）：`armorLevel` 与 `Barkskin` 的 `level` 定位相同 —— buff 上记的是**减伤上界**，
> 每次受击再在 `[0, armorLevel]` 内随机。**该字段现已整条拆除**，改由 `BoneWeaving.maxArmor(Char)`
> 实时给出上界；`Barkskin` 那种「buff 自带数值」的写法被判定为**不适合本天赋**（见 §19）。

## 18.5 纹身铭刻：给 `ElixirOfMight.HTBoost` 开一条「显式数值」通道

要发的是「**当前生命上限的 25%/50%**」这个**具体数值**，而 `HTBoost.boost()` 原本只有固定公式
（`left × boost(15 + 5×lvl) / 5`，随升级递减——这是艾利克斯的语义）。两种解法：

| 方案 | 评价 |
|---|---|
| 新造一个 buff 类 | ✗ 界面上出现两个都叫「生命强化」的图标，玩家分不清，也会与既有「5 次升级后消失」的认知冲突 |
| **给 `HTBoost` 加一条显式值通道** | ✓ 复用同一个 buff 与生命周期；数值先由外部算好再写入，**不自我滚雪球** |

实现：

```java
private int explicitBonus = 0;              // 新增字段；0 = 未启用，走原公式
public void reset(){ left = 5; explicitBonus = 0; }        // reset 也要清，否则 reset 后仍吃显式值
public void setExplicitBonus( int amount ){ left = 5; explicitBonus = Math.max(0, amount); }  // 覆盖式，不叠加
public int boost(){
    if (explicitBonus > 0) return explicitBonus;
    return Math.round(left*boost(15 + 5*((Hero)target).lvl)/5f);
}
```

`storeInBundle` / `restoreFromBundle` 补 `EXPLICIT_BONUS`（`bundle.getInt` 缺键返回 0 ⇒ **老存档安全**，
读进来仍是公式形态）。因为 `explicitBonus > 0` 时 `boost()` 返回**定值**，它不随 `left` 递减，5 次升级后
`left` 归零 → `detach()` → 加成整体消失——正好就是用户选的「复用既有生命强化」口径。

`RevengeLedger.engrave()`（消耗奥术刻笔时）在原有「充能 25%/50%」之后追加：

```java
int htPercent = Talent.tattooHealthBoostPercent(hero);      // 未点天赋 0；+1 → 25；+2 → 50
if (htPercent > 0){
    int htGain = Math.round(hero.HT * (htPercent / 100f));  // ⚠️ 先按当前 HT 算好，再写入
    ElixirOfMight.HTBoost boost = Buff.affect(hero, ElixirOfMight.HTBoost.class);
    boost.setExplicitBonus(htGain);
    hero.updateHT(true);                                    // 与艾利克斯同款：上限提升的同时补足等量生命
    if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(htGain), FloatingText.HEALING);
    GLog.p(Messages.get(this, "tattoo_ht", htPercent, htGain));
}
```

两个易错点：① 必须先按**当前** `HT` 算 `htGain` 再写入（顺序反了会拿「加上加成的 HT」去算百分比 ⇒ 滚雪球）；
② 数字写的是「净增点」，所以 `HT` 之外的反馈走 `GLog` 文本键（未带数字的规则只约束 `GLog` 的**前缀行文**，
此处是带占位符的完整句，符合 `tattoo_used` 的既有写法）。`hero.spendAndNext(1f)`（刻入占 1 回合）保持不动。

## 18.6 文本（zh ＋ en 双份）

| 键 | 改动 |
|---|---|
| `actors.hero.talent.shifting_fate.desc` | zh 改为「判定是否被命中时，闪避值额外随机 _1次_ / _2次_，取其中 _最高_ 的一次参与比大小」；en 同步（`extra time` / `highest`） |
| `actors.hero.talent.shifting_fate.title` / `bone_weaving.title` | **en 侧原先缺失**（只有 zh），本次补上 `Shifting Fate` / `Bone Weaving` |
| `actors.hero.talent.bone_weaving.desc` | 「_2~8_」→「_0~当前角色等级_」（两个层级标签都改）；en 同步 `0 up to her hero level` |
| `actors.hero.talent.tattoo_engraving.desc` | 追加「并获得 _25%_ / _50%_ 当前生命上限的 _生命强化_」；en 同步 `max health boost` |
| `actors.hero.talent.hunting_meal.desc` | 「为已装备的 _奥丁之眼_ 恢复 _1点_ / _1.25点_ 充能」（原为武技充能）；en 同步 `Odin's Eye` |
| `items.artifacts.revengeledger.tattoo_ht`（**新增**） | zh ＋ en 双份，两个 `%d` 占位符（百分比 / 净增点） |

> `*_zh-hant.properties` **不维护** EGOPD 自定义内容（实测：345 个上游天赋键、自定义键 0 个），故本次
> 无需补 hant 版本——与既有自定义天赋 / 神器的处理一致。

## 18.7 落点

| 文件 | 改动 |
|---|---|
| `actors/Char.java` | 抽出 `private static float evasionRoll(Char,float)`；`hit()` 加「重掷取最高」段；`drRoll()` 注释同步 |
| `actors/hero/Hero.java` | 删掉 `defenseSkill()` 里 `SHIFTING_FATE` 的闪避乘区分支，留说明注释 |
| `actors/hero/Talent.java` | `onFoodEaten()` 的 `HUNTING_MEAL` 改调 `OdinsEye.restoreCharge`；新增 `tattooHealthBoostPercent(Hero)`；import `OdinsEye` |
| `items/artifacts/OdinsEye.java` | 新增 `public static void restoreCharge(Hero,float)`（约 22 行，含完整守卫链） |
| `items/BodyArtMaterial.java` | `AC_WEAVE` 护甲区间 `IntRange(2,8)` → `IntRange(0, hero.lvl)` |
| `actors/buffs/BoneWeaving.java` | javadoc 同步为「0~角色等级」 |
| `items/potions/elixirs/ElixirOfMight.java` | `HTBoost` 新增 `explicitBonus` ＋ `setExplicitBonus()` ＋ `reset()` 清理 ＋ 存档字段 |
| `items/artifacts/RevengeLedger.java` | `engrave()` 追加生命强化发放；import `FloatingText` / `ElixirOfMight` / `CharSprite` |
| `messages/actors/actors{,_zh}.properties` | 四条 desc/title（含补 en 的两个 title） |
| `messages/items/items{,_zh}.properties` | 新增 `revengeledger.tattoo_ht` |

版本号 / APK **未动**（用户本次只要求改天赋＋同步文本）。

## 18.8 核验

| 项 | 结果 |
|---|---|
| 单文件 `javac`（`-Xlint:all`，8 文件） | **EXIT=0**（`_chk/_talent_javac.log`，277 行警告全是既有的 `rawtypes` / `this-escape` / `lossy-conversions`，逐条回查确认**无一新引入**） |
| `_chk/verify_talent_rework.py`（新） | **51 条**断言全过 ＋ **51 条反例自测**全过，**0 条 SKIP / 0 条抓不到错**。五组：A 变换无常（11）／B 狩猎一餐（11）／C 骨骸编织（7）／D 纹身铭刻（14）／E 文本 zh·en（8） |
| `check_utf8_all.py` | 1474 文件全绿 |
| `check_unused_imports.py` | ALL PASS（`Char.java` 163 条 import 含新引用的 `Trial` 系） |
| 行尾 | 4 个**上游** `.java`（`Char` / `Hero` / `Talent` / `ElixirOfMight`）保持 CRLF；4 个**自建** `.java`（`OdinsEye` / `RevengeLedger` / `BodyArtMaterial` / `BoneWeaving`，均未入 git）**本来**就是纯 LF，`Edit`/`Write` 保留原行尾未动；4 个 `.properties` 保持 LF。逐文件断言「`\r\n` 与裸 `\n` 不同时出现」⇒ **每个文件内部都无混存**（⚠️ 「`.java` 必须 CRLF」是错的判据，自建文件是一等公民） |

⚠️ **三条核验踩坑（已写回脚本注释，勿重犯）**

1. **反例锚点必须落在目标文件里**：写「给 Hero.java 插入 `SHIFTING_FATE`」时本想拿 `INFINITE_EVASION`
   当锚点，但它其实声明在 `Char.java`（`public static int INFINITE_EVASION = 1_000_000;`）⇒ 反例命中 0 次、
   自测被 SKIP。改锚 `public int defenseSkill( Char enemy ) {` 后生效。
2. **同表/同型语句会让 `count=1` 的变异只改到「前一处」**：`tattooChargePercent` 与
   `tattooHealthBoostPercent` 返回**同一个表达式**（表是刻意共用的）、`MagicImmune` 守卫在 `OdinsEye`
   里出现两次（`charge()` / `restoreCharge()` 各一）⇒ 这两条反例必须 `count=2`，否则自测误报
   「判据抓不到错」。
3. **判据范围别放大**：`onFoodEaten` 里 `FOCUSED_MEAL` 本来就要 `MeleeWeapon.Charger`，所以
   「武技充能已删净」这条要收敛到 `if (hero.hasTalent(HUNTING_MEAL)){` 那一段（用大括号配平取主体），
   对整个方法做子串否定会**永远 FAIL**。

## 18.9 待人工验证

1. 神谕代行者点满「变换无常」(+2)：开调试窗把某只怪的命中拉到必中，观察 MISS 频率应明显高于改动前
   （等效「掷 3 次取最高」）；换成「无限闪避」类效果（考验 / 预知眼）时仍应**必闪**（`INFINITE_EVASION` 短路）。
2. 神谕代行者 +2 时，界面描述必须是「额外随机 _2次_」；旧档里的历史描述不要残留（看的是 properties，直接生效）。
3. 前二老板点「狩猎一餐」：**装备**奥丁之眼进食 ⇒ 充能 +1 点（+2 天赋 +1.25 点，连吃两次应看到进位）；
   **未装备** ⇒ 进食正常、无报错、无充能变化；**背包里有一枚但没装备** ⇒ 也应无变化。
4. 前二老板在**魔免**（如魔法免疫药水）状态下进食 ⇒ 不给充能。
5. 环指大师点「骨骸编织」：低等级（1 级）时护甲加成应能低到 0~1，高等级时能高到接近等级数，且**升级后
   立刻变强、不必重新编织**；buff 图标上显示的应是**剩余回合数**（不是护甲值）；**+1 只认骨质素材、
   +2 才认肉质素材**、时长 50 / 100 回合不变（见 §19 二次返工）。
6. 中指长兄点「纹身铭刻」：+1 消耗奥术刻笔 ⇒ 弹出「+X」绿色漂浮字，生命上限与**当前**生命同步上涨；
   连续铭刻多次 ⇒ 数值按**当前** HT 重算（不叠加、不滚雪球），生命强化时长刷新为 5 次升级。
7. +2 时百分比应为 50%；**未点该天赋**时消耗刻笔只给充能、不给生命强化（也不能出现 0 点加成的空提示）。
8. 读一次存档再看：生命强化的显式数值应**原样保留**（`EXPLICIT_BONUS`），不是回落成公式值；老存档
   （改动前存的）读取后不应报错。

---

# 2026-09-26（续四）环指 T2「骨骸编织」二次返工：去掉「层数」，改为**实时**按角色等级取护甲上界

## 19.1 需求原文，与上一版错在哪

> 「之前的环指 T2『骨骸编织』方法有误，实现为 2~8 之间的固定值了，希望你取消骨骸编织的 buff 层数，
> 使得它本身作为一个没有层数的、『0~角色等级』的护盾 buff 存在，可以参考女猎人的『树肤韧甲』天赋。」

上一版（§18.4）只把区间从 `2~8` 换成 `0~角色等级`，**方案本身没动**：仍是「消耗素材时掷一次，
把结果写进 `BoneWeaving.armorLevel`，图标再把这个字段当数字显示」。两个后果：

| 后果 | 说明 |
|---|---|
| 数值被**冻结** | 掷点发生在消耗那一刻 ⇒ 之后升级不会变强，得重新编织一次才更新；「0~角色等级」实际是「0~**消耗时**的等级」 |
| 图标上出现「**层数**」 | `armorLevel` 被 `iconTextDisplay()` 渲染成图标角标，玩家看到的是「一个带层数的 buff」，与「无层数护盾」的定位矛盾 |

## 19.2 新方案：buff 只记「存在 ＋ 还剩多久」，护甲**实时**算

### ① `BoneWeaving`：删字段、删层数显示、只留时长

```java
// 删：public int armorLevel = 0;        及其 storeInBundle/restoreFromBundle 里的 ARMOR_LEVEL
// 删：iconTextDisplay() 覆写            ← 关键，原因见下
// 增：护甲上界（实时读数，单一出口）
public static int maxArmor( Char ch ) {
    return ch instanceof Hero ? ((Hero) ch).lvl : 0;
}
```

⚠️ **`FlavourBuff` 的默认 `iconTextDisplay()` 不是空串，而是 `Integer.toString((int)visualcooldown())`**
（`FlavourBuff.java:46`）⇒ 想「没有层数」的正确做法是**删掉整个覆写**（走默认＝显示剩余回合数，
与 `Bless` 一致），而不是 `return ""`（那是 `Buff` 的默认，图标会退化成只剩灰色进度弧）。
本次口径经用户确认：**显示剩余回合数**。`maxDuration`（进度弧基准）保留；`desc()` 改为实时读
`maxArmor( target )`。

### ② `Char.drRoll()`：上界实时取，`null` 守卫保留

```java
// 骨骸编织：护甲加成 0~角色等级。上界每次结算实时读取（不在消耗素材时冻结）……
if (buff(BoneWeaving.class) != null){
    dr += Random.NormalIntRange( 0 , BoneWeaving.maxArmor(this) );
}
```

⚠️ **不能**写成无条件的一行（让 `maxArmor` 在无 buff 时返回 0 就完事）—— 那样**没有这个 buff 的角色
也会多消耗两次 `Random.Float()`**，等于挪动全局随机流，同 seed 的其它判定全部漂移。守卫就是
「零影响」的保证，核验里单独有一条钉它。

### ③ `BodyArtMaterial.execute()`：不再掷点

```java
int points = hero.pointsInTalent( Talent.BONE_WEAVING );
float duration = points == 1 ? 50f : 100f;
BoneWeaving bw = Buff.prolong( hero, BoneWeaving.class, duration );
bw.maxDuration = Math.max( bw.maxDuration, duration );   // ⚠️ 不是直接赋值，见 §19.4
detach( hero.belongings.backpack );
hero.spendAndNext( 1f );
GLog.p( "骨骸编织：素材编入躯体，你的护甲得到强化。" );   // 顺手还旧账：GLog 不带数字（AGENTS §2）
```

`import com.watabou.utils.Random;` 随之成为未使用 import，已删除（否则 `check_unused_imports.py` 报）。

## 19.3 与 `Barkskin`（女猎人「树肤」）的异同

| | `Barkskin` | 本次的 `BoneWeaving` |
|---|---|---|
| 是否在 buff 上存数值 | ✅ 存 `level`，且**每 `interval` 回合衰减 1 点** | ❌ 不存；上界每次现算 |
| 图标角标 | `level`（当前护甲） | 剩余回合数（`FlavourBuff` 默认） |
| 减伤写法 | `dr += Random.NormalIntRange(0, Barkskin.currentLevel(this))` | `dr += Random.NormalIntRange(0, BoneWeaving.maxArmor(this))` —— **同一套** |
| 多来源叠加 | 只取最强的那个（`currentLevel` 取 max） | 单实例，续期只延长时长 |
| 上界来源 | 施加时算好（`Hero.java:1162` 的 `hero.lvl × points / 2`） | 每次结算现算（`hero.lvl`） |

> 结论：**借鉴的是「减伤怎么算」，不是「数值存在哪」**。本天赋要求「随等级实时变化 ＋ 没有层数」，
> 于是把 `Barkskin` 的 `level` 换成「静态方法现算」，连随机分布（`NormalIntRange`）都保持一致。

## 19.4 续期语义：`postpone` 只会变长 ⇒ `maxDuration` 必须 `Math.max`

`Buff.prolong` → `Actor.postpone(float time)`（`Actor.java:79`）：

```java
protected void postpone( float time ) {
    if (this.time < now + time) { this.time = now + time; /* …整点取整… */ }
}
```

即「**只会被推到更晚**，不会被更短的覆盖」。于是先用 +2（100 回合）再用 +1（50 回合）时，buff 剩余
仍是 100 —— 若 `maxDuration` 直接赋 50，进度弧会算出 `1 - 100/50 = -1` → 夹到 0 → **图标显示成已过期**。
`Math.max(旧值, 新值)` 与 `postpone` 语义对齐。

## 19.5 落点

| 文件 | 改动 |
|---|---|
| `actors/buffs/BoneWeaving.java` | 删 `armorLevel` / `ARMOR_LEVEL` / `iconTextDisplay()`；增 `public static int maxArmor(Char)`；`desc()` 实时读数；javadoc 重写 |
| `actors/Char.java` | `drRoll()`：`…buff(BoneWeaving.class).armorLevel` → `BoneWeaving.maxArmor(this)`（`null` 守卫原样保留） |
| `items/BodyArtMaterial.java` | `execute()` 删掷点与写值、`maxDuration` 改 `Math.max`、`GLog` 去数字；删 `Random` import；类 javadoc 同步 |
| `messages/actors/actors_zh.properties` | `actors.hero.talent.bone_weaving.desc`：写明「不叠加层数」＋「上界随角色等级实时变化」 |
| `messages/actors/actors.properties` | 同上（en：`(no stacks)` / `recalculated live`） |

## 19.6 核验

- `_chk/verify_talent_rework.py` **C 组整体重写**：判据重点从「区间是多少」变成「**数值有没有被存起来**」，
  共 9 条 —— C1 无 `armorLevel`／C2 无 `Random` 依赖／C3 时长未动／C4 `Math.max`／C5 上界走
  `maxArmor(this)`／C6 `null` 守卫在且掷点位于守卫内／C7 **无 `iconTextDisplay()` 覆写**／
  C8 `public static` ＋ `instanceof Hero`／C9 `desc()` 实时读数且存档只剩 `max_duration`。
  **全脚本 53 条 ＋ 53 反例，0 SKIP / 0 抓不到错。**
- 单文件 `javac -Xlint:all`（`Char` ＋ `BoneWeaving` ＋ `BodyArtMaterial` ＋ 5 个素材子类）⇒ **EXIT=0**，
  226 行警告逐条回查，**无一落在本轮改的三个文件上**（全是 `Char.java` 既有的 `rawtypes` / `lossy-conversions`）。
- `armorLevel` 在 `core/src/main/java` 下**已 0 命中**；`check_utf8_all.py` 1474 全绿；
  `check_unused_imports.py` ALL PASS；行尾 `Char.java` CRLF、两个自建件纯 LF、两个 `.properties` 纯 LF，**混存 0**。

## 19.7 待人工验证

1. 环指大师 +1 编织骨质素材：**1 级时护甲增益应常在 0~1**（旧版有 2 点保底）。
2. **升级后不必重新编织**：编织后连升几级，护甲上界应立刻跟着涨（旧版停在消耗时的等级）。
3. buff 图标（英雄信息窗的大图标模式）角标应显示**剩余回合数**、逐回合递减，而不是固定护甲值。
4. 先用 +2 编一次（100 回合），再用 +1 编一次（50 回合）：**剩余回合不应缩短**，进度弧也不应显示成已过期。
5. **+1 只认骨质素材、+2 才认肉质素材**不变；`编织` 的 `GLog` 不应再出现具体数字（AGENTS §2）。
6. 没点这个天赋 / 不是环指大师时：减伤与随机数消耗应与改动前**逐字节一致**（守卫保证不掷多余随机数）。

---

# 2026-09-26（续五）考验 NETZACH 浮层淡出：怪物头顶血条 ＋ 状态标记（睡眠 / 警觉 / 搜索 / 迷失）

> 需求原文：「现在需要你检查 NETZACH 考验的隐形效果，希望将怪物头顶的血条、以及右上角的标识符号
> （标记睡眠状态等）一并变为半透明或完全透明。」

## 20.1 问题：NETZACH 原来只淡化了「精灵本体」

上一版（§16.4）把淡化做在 `CharSprite.draw()` 里 —— 每帧在绘制前 `am *= fade`、画完立刻还原。
但**挂在怪身上 / 旁边的这几样都不继承它**：它们不是精灵的子节点，而是各自
`GameScene.add(...)` 挂到**场景**上的独立节点。

| 浮层 | 类 | 挂载方式 | 「看不见的怪」被它暴露成什么 |
|---|---|---|---|
| 血条（常驻） | `ui/CharHealthIndicator` | 构造器里 `GameScene.add(this)` | 怪物头顶一条浮空血条 |
| 血条（瞄准中） | `ui/TargetHealthIndicator` | `GameScene` 里 `add(new TargetHealthIndicator())`（单例） | 同上，而且它专门标出「你正瞄着谁」 |
| 状态标记 | `effects/EmoIcon`（`Sleep` / `Alert` / `Investigate` / `Lost`） | 构造器里 `GameScene.add(this)` | 睡眠 Zzz / 警觉 ! / 搜索 ? / 迷失 —— 精确定位 |

⇒ `>3 格`「完全看不见的怪」仍能被一条血条 + 一个 Zzz 图标指名道姓。

## 20.2 血条：`HealthBar` 自己**没有** alpha，必须写进三个色块

⚠️ 继承链是 `HealthBar extends Component extends Group extends Gizmo` —— 它在 `Visual`
**之外**，所以既没有 `am` / `aa` 字段、也没有 `alpha(float)` 方法；而 `Group.draw()` 只是
遍历成员逐个 `draw()`，**不会把父节点的 alpha 往下传**。

真正被画出来的是它内部三个 `ColorBlock`（`Bg` / `Shld` / `Hp`）：

```java
// ui/HealthBar.java（新增）
public void setAlpha( float value ){
    if (value < 0f) value = 0f;
    if (value > 1f) value = 1f;
    Bg.alpha( value );
    Shld.alpha( value );
    Hp.alpha( value );
}
```

`ColorBlock extends Image extends Visual` ⇒ 有 `alpha(v)`（= `am = v; aa = 0`）；颜色是烘在
`TextureCache.createSolid` 的 texture 里、由 `am` 当乘子 ⇒ 改 `am` 就是整体半透明。
`setAlpha` **不碰几何、不碰 `visible`**（那是 `layout()` 与 `update()` 的职责）。

两条血条的统一口径：

```java
float fade = Trials.enemyFade( target );
setAlpha( fade );
```

## 20.3 两条血条的 `visible` 处理**刻意不同**

| 类 | `visible` | 理由 |
|---|---|---|
| `CharHealthIndicator`（常驻） | `= (HP<HT \|\| shield>0) && fade > 0f` | 它的 `visible` **只服务自己**（全作没有第二个消费者）⇒ 全透明时连可见性一起关，最干净 |
| `TargetHealthIndicator`（瞄准中） | `= true`（**原样不动**） | ⚠️ 它的 `visible` 已经被**玩法逻辑**当语义读走了 |

⚠️ **`TargetHealthIndicator.instance.isVisible()` 不是「血条在不在屏幕上」**：
`items/trinkets/ChaoticCenser`（混沌香炉）拿它当「英雄当前有没有锁定目标」在读
（`if (instance != null && instance.isVisible())` ⇒ 取 `instance.target()` 去放毒气）。
若为了「藏血条」把它置 false，香炉就在 NETZACH 下**静默失效** —— 那是玩法改动，
违反 NETZACH「纯视觉、不碰任何游戏内状态」的设计（见 §16.4）。
`alpha` 压到 0 已经是「完全透明」，玩家同样看不到，而行为与改动前**逐位相同**。

⇒ 教训：**「`visible` 这个字段未必是显示开关」**，改 UI 显隐前先 grep 谁在读它。

## 20.4 状态标记：`EmoIcon` 在 `draw()` 里乘、画完还原

`EmoIcon extends Image extends Visual` ⇒ 直接有 `am` / `aa`。做法与 `CharSprite.draw()` 同构：

```java
// effects/EmoIcon.java（新增覆写）
@Override
public void draw() {
    float fade = (owner != null) ? Trials.enemyFade( owner.ch ) : 1f;
    float amBak = am, aaBak = aa;
    if (fade < 1f) { am *= fade; aa *= fade; }
    super.draw();
    if (fade < 1f) { am = amBak; aa = aaBak; }
}
```

- **为什么在 `draw()` 而不是 `update()`**：`update()` 只负责摆动缩放与定位（`owner.x + owner.width() - center.x`，
  即「怪物右上角」）；写持久 alpha 会与「真正的隐形走 `AlphaTweener`」打架 —— 与 §16.4 给 `CharSprite`
  的理由完全一致。`fade == 0` 时自然变成「画了但看不见」，不需要额外分支。
- **为什么只覆写基类**：`Sleep` / `Alert` / `Investigate` / `Lost` 四个子类**都不各自覆写** `draw()`，
  继承同一份 ⇒ 不会出现「改了基类漏了子类」。核验里专门 `javap -p` 四个 `EmoIcon$Xxx` 钉死这一点
  （且先断言「找得到这个类」，否则「不覆写」会退化成假通过）。
- `owner.ch` 对英雄时 `Trials.enemyFade(Dungeon.hero)` 立即返回 1 ⇒ 英雄自己的睡眠标记零影响。

## 20.5 仍未收口的浮层（刻意留着）

同属「挂在场景上、不继承精灵 alpha」，但本轮**没动**（改动面大，且都不是「状态标记」语义）：

| 未收口 | 出现条件 |
|---|---|
| `ShieldHalo shield` | `State.SHIELDED`（护盾） |
| `Flare aura` | `State.AURA`（部分单位自带光环） |
| `IceBlock` / `DarkBlock` / `GlowBlock` | `FROZEN` / `DARKENED` / `ILLUMINATED` |
| `TorchHalo light` | 光照 |
| `Emitter` 系列（`burning` / `chilled` / `marked` / `levitation` / `healing` / `hearts`） | 对应各状态粒子 |
| `FloatingText` | 伤害 / 治疗 / 升级漂浮字 |

⇒ 现状：开 NETZACH 时 `2~3 格`的半透明怪若顶着护盾 / 冰封，那层光环仍是**全亮**的。
要收口建议**统一走一个「挂到精灵时登记」的小工具**（登记表里存 `(Visual, Char)`，
在 `GameScene` 的绘制阶段统一乘系数），而不是逐个 `draw()` 手写 —— 否则以后每加一个
sprite 子效果都要记得来补一处。

## 20.6 落点

| 文件 | 改动 |
|---|---|
| `ui/HealthBar.java` | 新增 `setAlpha(float)`（夹取 0~1 ＋ 写三个 `ColorBlock`） |
| `ui/CharHealthIndicator.java` | `update()`：取 `fade` → `setAlpha(fade)` → `visible = … && fade > 0f` |
| `ui/TargetHealthIndicator.java` | `update()`：取 `fade` → `setAlpha(fade)`；`visible` **保持 `true` 不动**（香炉依赖） |
| `effects/EmoIcon.java` | 新增 `draw()` 覆写（本帧乘 `am`/`aa`、`super.draw()` 后还原）＋ `Trials` import |
| `Trials.java` | NETZACH 注释补「浮层收口点」清单 ＋「仍未收口」清单（**只改注释**，常量与 `enemyFade()` 一字未动） |
| `_chk/HodNetzachProbe.java` | 修上一轮遗留：④ 的距离阈值断言仍是旧的 `≤2 / 3~4 / >4` |
| `_chk/verify_hod_netzach.py` | `FADE_CALLERS` 由 2 个扩到 4 个；血条断言改成新形态 |
| `_chk/verify_netzach_overlay.py`（新） | 本轮专用核验（见 §19.7） |
| `_chk/_build_netzach_overlay.sh`（新） | 把这 5 个文件编进 `_chk/_javachk2`（供 `javap` 层核验） |

**没有**改动的：`CharSprite.draw()`（本体淡化照旧）、NETZACH 三个常量、`enemyFade()` 判定、
任何文本（`.properties` 无新增 / 无改动）。

## 20.7 核验

- `_chk/verify_netzach_overlay.py`（新）：**88 条断言全过、0 SKIP、0 假通过**（其中含 16 条反例自测）。
  五层：
  ① 源码结构（`setAlpha` 的三个色块 ＋ 不碰 `visible`/几何；两条血条的顺序；`EmoIcon.draw` 的
  「取系数 → 乘 → `super.draw()` → 还原」；4 个子类仍在；**剥注释后**的引用清单唯一性）
  ② `javap -p` 签名 ③ `javap -c` 字节码（`setAlpha` 恰好 3 次 `ColorBlock.alpha` 且顺序 Bg→Shld→Hp
  ＋ 零 `putfield`；两条血条「取数早于 `setAlpha`」；`EmoIcon.draw` 的 `am/aa` 各写两次且**还原晚于
  `super.draw()`**；`TargetHealthIndicator` 里没有 `fcmp` ⇒ 确实没拿 `fade` 去算 `visible`；
  四个 `EmoIcon$Xxx` 都不覆写 `draw`）④ 文本层（描述没被带坏）⑤ 探针输出层。
- `_chk/_build_netzach_overlay.sh`（新）：`javac -Xlint:all` 编 5 个文件 **EXIT=0**（只剩上游既有的
  `[this-escape]` 警告）。
- **本轮顺手修掉一处「假绿」**：上一轮把 `NETZACH_FADE_NEAR/FAR` 从 2/4 收紧到 1/3 时，
  `_chk/HodNetzachProbe.java` ④ 的断言忘了同步（仍是「距离 2 ⇒ 不透明」「距离 4 ⇒ 半透明」）；
  而 `verify_hod_netzach.py` 只是**读** `_chk/_hodnetzach.out`，那份输出文件的时间戳（18:41）
  早于 `Trials.java` 的改动（21:38）⇒ 一直显示绿。重跑立刻暴露 3 条 FAIL，
  改断言（`≤1 ⇒ 1.0` / `2~3 ⇒ 0.5` / `≥4 ⇒ 0`）后 **52 条全过**。
  ⇒ 教训见 `docs/handbook/pitfalls.md`：「核验脚本读产物快照」必须配套时间戳或每次重跑。
- 回归全绿：`verify_yesod`(131)、`verify_hod_netzach`、`verify_chesed_gebura`、`verify_hokma_delay`、
  `verify_binah_gen`、`verify_gebura_drop`(37)、`verify_fun_challenges`(81)、`verify_holy_card`、
  `verify_trials_tree_ui`、`verify_tree_trials_icons`、`verify_boss_phase_scales`(49)、
  `verify_marked_kill_routes`(87)、`verify_dwarfking_chesed`(27)；
  `_chk/YesodProbe` 37/0；`check_utf8_all` 1474 文件 OK；`check_unused_imports` ALL PASS。

## 20.8 待人工验证

1. 开 NETZACH，把一只怪拉到 **2~3 格**：怪半透明 ⇒ **头顶血条也应是半透明**（先打它一下让血条出现）。
2. 拉到 **>3 格**：怪消失 ⇒ **血条也消失**；用睡眠卷轴 / 让怪警觉 / 搜索 / 迷失时，
   **右上角的标记同样不可见**。
3. 用远程武器 / 法术瞄准 >3 格的怪：瞄准血条不可见（alpha=0）。
4. 带**混沌香炉**（`ChaoticCenser`）开 NETZACH：锁定 >3 格的怪时香炉应照旧会放毒气
   （这条正是不动 `TargetHealthIndicator.visible` 的原因）。
5. **不开** NETZACH 时逐项对照：血条、状态标记、瞄准血条应与改动前**完全一致**（`enemyFade` 恒 1 ⇒
   `setAlpha(1)` 即原样）。

---

# 2026-09-26（续六）考验 YESOD：容器贴图也变问号 ＋ 战利品指示器跟随 ＋ 出 v0.3.6

> 需求原文：「现在请你再进行一处改动：各类宝箱和骷髅堆的贴图也会被替换为问号（不用改动描述，
> 我已经手动改过），随后更新版本并打包APK」。
>
> 经 `AskUserQuestion` 追加确认三条：① 未知贴图覆盖 **`Heap.Type` 全部 6 类容器**；
> ② 「不用改动描述」指的是**考验描述**（`trials.yesod_desc`，由用户自行维护），**检视描述**仍走
> YESOD 惯例的「你不知道这里是什么。」；③ 屏幕左下角的**战利品指示器**也一起变问号。

## 21.1 判据本体：从「枚举两种类型」改成「非空堆一律」

上一版（§17.4）的 `hidesGroundHeap` 显式排除了容器：

```java
return heap.type == Heap.Type.HEAP || heap.type == Heap.Type.FOR_SALE;   // 旧
```

现在反过来 —— 判据里**不再出现任何 `Heap.Type`**：

```java
// Trials.java
public static boolean hidesGroundHeap( Heap heap ){
    if (heap == null || heap.isEmpty()) return false;
    return Dungeon.isTrialled( YESOD );
}
```

为什么不写成「枚举 6 类容器」：那样**将来每漏一种就静默漏一种**（`Heap.Type` 加新成员时不会报错，
只是那种容器照旧显形、把答案写在脸上）。写成「非空堆一律」把口径收成**一条不变量**。
安全性来自 `Heap.open()` —— 它在打开时把 `type` 改回 `Type.HEAP`，所以「已开箱的堆」本来就是普通掉落，
不需要在判据里单独照顾。

## 21.2 三个老出口 ＋ 一个新出口

| 出口 | 落点 | 本轮改了什么 |
|---|---|---|
| 地图贴图 | `sprites/ItemSprite.view(Heap)` | 闸门**提到 `switch` 之前**并罩住整个 `switch` —— 六类容器的贴图只在 `switch` 里被设置，只有这样才真正覆盖它们 |
| 名字 / 标题 | `items/Heap.title()` | `type == Type.HEAP` → `type != Type.FOR_SALE`（容器也隐名；货架仍提前放行价签） |
| 检视描述 | `windows/WndInfoItem(Heap)` | **无需改动** —— 它本来就读同一个判据（只是现在容器也落进这个分支了） |
| **战利品指示器** | `ui/LootIndicator.update()` | 原本是 `heap.type == CHEST ? ItemSlot.CHEST : …` 一串三元，**等于把刚藏起来的答案重新写在 HUD 上**；现在这个判断排在最前，命中时改取新增的 `ItemSlot.UNKNOWN` |

`ItemSlot.UNKNOWN` 与既有的 `CHEST` / `LOCKED_CHEST` / `SKELETON` 等「虚拟物品」同一套写法：

```java
public static final Item UNKNOWN = new Item() {
    public int image() { return ItemSpriteSheet.UNKNOWN_ITEM; }
    public String name() { return Trials.unknownGroundText(); }
};
```

⇒ 图标与文案都与地图**同源**（同一个 `UNKNOWN_ITEM` 格位、同一条 `trials.yesod_unknown`），
不会出现「地图上是问号、HUD 上是另一个问号」这种两份实现各自漂移的隐患。

## 21.3 为什么四个出口必须同源

`hidesGroundHeap` 是**一个判据的四个出口**。漏任何一个都会得到「半吊子」表现：

- 只改贴图 ⇒ 检视窗里还写着「宝箱」；
- 只改贴图与描述 ⇒ 左下角 HUD 仍在点名「脚下是个宝箱」。

所以 `_chk/verify_yesod.py` 里的 `callers_of('Trials.hidesGroundHeap')` 是**硬判据**：
引用清单必须**恰好**等于「贴图 + 名字 + 描述 + 战利品指示器」四个文件，**多一处少一处都是回归**。

## 21.4 核验

| 层 | 钉住了什么 |
|---|---|
| ① 源码结构 | `hidesGroundHeap` 里**不出现任何 `Heap.Type` / `type ==` / `switch`**；`view(Heap)` 的六个容器 `case` 全排在闸门之后；`LootIndicator` 的问号分支排在三元链最前；`ItemSlot.UNKNOWN` 的两个取值同源 |
| ② `javap -p` | `ItemSlot.UNKNOWN` 字段类型为 `items.Item` |
| ③ `javap -c` | `hidesGroundHeap` 字节码里**没有 `Heap$Type`**；`Heap.title()` 只比对 `Heap$Type.FOR_SALE`（`HEAP` 已退场）；六个容器 `getstatic` 全在闸门之后；`LootIndicator.update` 的 6 个类型图标也全在闸门之后；`ItemSlot$7` 的 `image()` / `name()` 分别取 `UNKNOWN_ITEM` 与 `unknownGroundText()` |
| ④ 文本 | `trials.yesod_unknown` 逐字；**zh/en 对「容器」的口径一致**（见 §21.5） |
| ⑤ PNG | `items.png` 上 `xy(16,2)` 的实测包围盒 = 10×15 @ (0,0)（与抽样矩形逐像素相等） |
| ⑥ 探针 | `YesodProbe` ③ 扩成「`Heap.Type` **全部 8 种取值都中**」＋断言枚举了一个不落；④ 断容器的名字也变未知 |

新写的助手 `after_offset(ins, gate, token)`：问「token 的**首个**引用是否落在闸门之后」，
**缺项返回 False**（「缺项也算通过」是这类顺序判据最常见的假绿来源）。layer 与 selftest 共用同一份实现。

- `_chk/verify_yesod.py`：**158 条 OK / 0 FAIL / 0 SKIP**；`--selftest` **27 条**全过。
- `_chk/YesodProbe.java`：**39 / 0**。
- `javac -Xlint:all`：13 文件（`_build_yesod.sh`，新增 `ui/ItemSlot` / `ui/LootIndicator`）与
  5 文件（`_build_yesod_container.sh`）**EXIT=0**。
- `_chk/check_utf8_all.py` 1474 文件 OK；`_chk/check_unused_imports.py` ALL PASS。
- 全量回归 14 个核验脚本全绿。

## 21.5 顺手抓到一处「只改一半」：英文考验描述

`trials.yesod_desc` 的**中文**版由用户手工重写过（删掉了「_宝箱、骷髅堆等容器_与_背包内_
的显示不受影响。」这半句），但**英文**版当时没跟着删 —— 英文环境下面板会显示一句
与实际行为**相反**的说明：「_Containers (chests, skull piles and the like)_ and the
_inventory_ are unaffected.」。

按用户确认，把这半句也改成 `_Items in the inventory_ are unaffected.`
（`_chk/patch_yesod_desc_en.py`：字节级、幂等、CRLF 计数不变、备份留在 `_chk/_bak_yesoddescen/`）。

⇒ 由此在核验里加了一条**跨语言口径断言**：`zh 提到「容器」` 与 `en 提到 Container/chest`
必须**同真同假**。它专抓「只改了半边」的文案漂移 —— 而不是逐字比对（描述归用户维护，
逐字比对会在用户每次润色时假红）。

## 21.6 版本与打包（v0.3.6）

- 根 `build.gradle`：`appVersionCode 935 → 936`、`appVersionName '0.3.5' → '0.3.6'`。
- `ui/changelist/EGOPD_Changes.java`：新增 **v0.3.6 段（10 条，每条一句话）**，
  覆盖 0.3.5 打包之后的全部内容 —— 三条新考验（NETZACH / HOD / YESOD）、容器问号、
  NETZACH 浮层淡出、趣味挑战、GEBURA 额外掉落修复、标记击杀判据、矿洞虚空地形、四项二层天赋改写。
- `AGENTS.md` §1 版本现状一句同步为 936 / `0.3.6`。
- 打包 `:android:assembleDebug`（38s，`:core:compileJava` 与 `:android:packageDebug` 均 executed）
  → 归档 `EGOPD_0.3.6.APK`。

### 四道核验

| # | 项 | 结果 |
|---|---|---|
| ① | `aapt dump badging` | `versionCode='936'` / `versionName='0.3.6-INDEV'` / `application-label:'EGOPD'` / 图标指向 `mipmap-anydpi-v26/ic_launcher.xml` |
| ② | dex 内新内容 | `EGOPD_Changes` ×3、`容器也变问号` ×1、`hidesGroundHeap` ×1 |
| ③ | 资产全量比对 | APK 内 **542** 个 assets 与工作区 **逐个 md5 全等**（仅 APK 0 / 仅工作区 0 / 不一致 0）；13 条关键文本抽验全中 |
| ④ | 体积变化 | 49,528,642 B（+69,606）；增量可解释：`classes2.dex` +13,200、`terrain_features.png` +11,248、`tree.png` +4,193、`items_zh.properties` +2,074、`items.png` +1,660、新增 `tiles_lob_3.png` 一条；**新包与旧包空洞均为 0** |

## 21.7 请实机验证

1. 开 YESOD：三类宝箱、骷髅堆、坟墓、英雄遗骸**全变问号**，检视显示「？」＋ 未知文本。
2. 站在任意一堆（含金币、散落物）上：左下角战利品指示器也是问号。
3. 商店货架：贴图问号、**价格照常显示**（价签那条 `items.heap.for_sale` 提前放行）。
4. 打开一个宝箱后再看：箱内物品仍是问号（`open()` 把 `type` 改回 `HEAP`），拾取后恢复原贴图。
5. **不开** YESOD：一切与改动前完全一致。
6. 英文环境：考验面板 YESOD 的最后一句应为「Items in the inventory are unaffected.」

## 21.8 已知边界（本轮没碰，供后续决定）

- **名字与描述仍是原生文本**：容器的 `items.heap.chest` / `skeleton` 等**一字未动**（用户明示
  「只换贴图」）。因为 `WndInfoItem` 走的是未知分支、`Heap.title()` 也返回未知文本，这些原生文本
  在 YESOD 下**根本不会被显示**；它们只在不开考验时出现。
- **`LootIndicator` 只覆盖英雄脚下那一格**：这是它原本的职责范围（相邻格不显示战利品提示），
  不是本轮引入的缺口。
- **问号贴图 10×15 比容器原贴图 16×16 小**：`CellSelector.overlapsPoint` 用精灵尺寸做命中判定，
  理论上点选面积会小一点；实际点击走的是「格子 → `Dungeon.level.heaps.get(cell)`」那条路，
  所以不影响拾取/开箱操作。
---

# 2026-09-27 考验 HOD（荣耀）精英化阈值调整：150 → 普通 300 / Boss 450

> 本章取代 §16.1 表格 / §16.5 里「某个敌方单位**存活超过 150 回合**」的旧口径。
> §16 按流水账惯例**保留原文不改**（与 §17 收紧 NETZACH 淡出阈值时的处理一致）。

## 22.1 需求原文

> 修改 HOD 挑战的数值设置，使得普通怪物获得精英效果所需时间改为 300 回合、boss 怪物获得精英效果所需时间改为 450 回合。

## 22.2 落点

| 文件 | 改动 |
|---|---|
| `Trials.java` | `HOD_ELITE_TURNS` **150 → 300**（普通）；新增 `HOD_ELITE_TURNS_BOSS = 450`（Boss）；新增唯一取数收口 `hodEliteTurns( Mob )` |
| `actors/buffs/HodGlory.java` | 判据 `++turns > Trials.HOD_ELITE_TURNS` → `++turns > Trials.hodEliteTurns( mob )`；类注释口径同步 |
| `assets/messages/misc/misc{,_zh}.properties` | `trials.hod_desc` 同步为「普通 300 / Boss 450」 |

```java
public static final int HOD_ELITE_TURNS      = 300;   // 普通
public static final int HOD_ELITE_TURNS_BOSS = 450;   // Boss

public static int hodEliteTurns( Mob mob ){
    if (mob != null && mob.properties().contains( Char.Property.BOSS )) return HOD_ELITE_TURNS_BOSS;
    return HOD_ELITE_TURNS;
}
```

## 22.3 Boss 判据＝`Char.Property.BOSS`

SPD **没有** `Mob.boss` 字段，boss 身份由 `Char.Property.BOSS` 表达（`Char.properties()` 返回 `HashSet<Char.Property>`）。

- **走 450**：`Goo` / `Tengu` / `DM300` / `DwarfKing` / `YogDzewa` / `YogFist` / `CrystalSpire` /
  `FungalCore` / `GnollGeomancer` / `SmilingCorpseMountain`。
- **仍走 300**：`MINIBOSS` 一族（`CrystalGuardian` / `FetidRat` / `GreatCrab` / `GnollTrickster` /
  `GnollSapper` / `FungalSentry` / `Pylon` / `RotHeart` / `RotLasher` / `DemonSpawner` / 元素体），
  以及全部普通怪与召唤物。

⚠️ **MINIBOSS 有意排除在 450 之外**（需求只说「boss 怪物」）。若日后要一并纳入，**只改 `hodEliteTurns` 一处**。

## 22.4 为什么收成一个方法

阈值现在有两条分支（300 / 450）。若把 `if (Boss) … else …` 直接写进 `HodGlory.act()`，将来任何别处要读这个阈值
（例如考验界面显示「还需 N 回合」）就会**长出第二份分支** ⇒ 两处悄悄漂移。收口后唯一取数点是
`Trials.hodEliteTurns(mob)`，`HodGlory` 只调它。

计时口径**未变**：仍只在「是敌方阵营 ＋ 活着 ＋ 本考验仍开着」时 `++turns`；不适用即清零；数满晋升
（`grantHodElite`）后自摘。

⚠️ 两个阈值都是 `public static final int` **编译期常量**，会被**内联进调用点** ⇒ 改完必须重新编译调用方。
`_chk/HodNetzachProbe` 这类探针若不重编（只跑 `run` 而不跑 `all`），会继续用旧值，表现为「断言数与预期不符」
或直接 `找不到符号`。

## 22.5 核验

| 项 | 结果 |
|---|---|
| `_chk/HodNetzachProbe.java` | **60 条断言全过**。⑥ 段新增：`HOD_ELITE_TURNS == 300` / `HOD_ELITE_TURNS_BOSS == 450` / `hodEliteTurns(普通怪) == 300` / `hodEliteTurns(Boss) == 450`；普通怪「300 不发、301 发」；Boss「数到 300 不发、450 不发、451 才发」。新增 `ProbeMob.markBoss()`（在子类内部 `properties.add( Char.Property.BOSS )`，绕开 protected 字段的跨类可见性） |
| `_chk/verify_hod_netzach.py` | **145 条 [OK]，0 失败**。常量断言改 300 ＋ 新增 450；新增 `hodEliteTurns` 方法体顺序断言；`HodGlory.act` 的 `seq_ok` 锚点改为 `++turns > Trials.hodEliteTurns( mob )`；文本层要素补 `300` / `450` |
| 单文件 `javac -Xlint:all`（8 文件） | **EXIT=0** |
| `check_utf8_all.py` | 1474 文件全绿 |
| 行尾 | `Trials.java` / `HodGlory.java` 保持 LF；`misc{,_zh}.properties` 保持纯 CRLF 且无裸 LF |

⚠️ **核验陷阱**（本次踩到）：`seq_ok` 的 token **不能互为前缀**。`HOD_ELITE_TURNS` 是 `HOD_ELITE_TURNS_BOSS`
的子串，于是两者在方法体里命中**同一个偏移**（`[92, 121, 121]`），而 `seq_ok` 要求严格递增 ⇒ 判失败。
改用不互为前缀的锚点（`return HOD_ELITE_TURNS_BOSS;` / `return HOD_ELITE_TURNS;`）即可。

## 22.6 待人工验证

1. 开 HOD，让一只**普通怪**存活 **301 回合**（贴着绕圈不打）⇒ 弹出精英名字的状态字。
2. 开 HOD，在 Boss 层让 **Boss** 数到 **300~450 回合** ⇒ **不应**发精英；超过 **450 回合** ⇒ 才发。
3. 考验界面 HOD 描述应为「……存活超过 300 回合……；Boss 单位则需存活超过 450 回合」。
