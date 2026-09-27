# PD 项目约定（EGOPD）

> 权威＝`AGENTS.md`(§1~§6)＋`docs/handbook/`(全量)＋`docs/features.md`（**禁整读**，按 `docs/INDEX.md` 定点读）。本文件＝红线速查；查旧条 `python _chk/docfind.py <词>`。
> 专档（均在 `docs/`）：ring-master｜oracle-implementation｜weapon-creation-guide｜terrain-editor-guide｜android-apk-branding-guide｜hermes-caduceus-design｜hero-level-progression｜negative-item-levels｜disposal-port-kit
> skill（前缀均 `egopd-`）：source-verify｜release-package｜apk-icon-name｜terrain-editor｜terrain-tileset｜sfx-import｜trials-tree-ui｜new-weapon｜npc-sprite-text｜port-old-mod-content

## 工作流红线
- 编译/打包**用户**跑；唯一例外 `:android:assembleDebug`（先查 `executionHistory.lock`，idle daemon 不算占锁），**必带** `JAVA_HOME=D:/PD/tools/jdk-21.0.12.1+1`＋`GRADLE_USER_HOME=D:/PD/.gradle`。
- 版本＝根 `build.gradle` ext（`900+minor*10+patch`；当前 **936=v0.3.6**）；历程只写 `ui/changelist/EGOPD_Changes.java`（**一句话一条、正文不出 `\n`**）。
- 同文件多处编辑**串行**；改完 Grep 回读。
- 改完 `.java`/`.properties` **立刻** `python _chk/check_utf8_all.py`；别整文件 GBK→UTF-8。
- **行尾＝单文件内不混存**：上游 `.java`/`.gradle`＝CRLF，`docs/`·`_chk/*.sh`＝LF；`git ls-files --eol` 看穿。批改走字节级 IO，断言＝「无裸 LF ＋ 行尾增量＝各条替换行数差之和」。
- zh/en 双份（`*_zh-hant.properties` **不维护**自定义内容）；改一处必查另一处；换行只认字面 `\n`；别 heredoc 传含转义 Python。
- `desc` 从 `_+1：_` 起笔、层级标签首档也带下划线；数值只写文本、`GLog` 不带数字；列表类技能走 `ArmorAbility.namedShortDesc()`。
- 补丁脚本**幂等**＋比位置断言＋留 `_chk/_bak_*/`；核验脚本先剥注释（**保偏移** `strip_comments`）；锚点先 grep 确认在**本文件**、`[SKIP]` 当失败看。

## 静默陷阱（详版 `docs/handbook/pitfalls.md`）
- **计数类图标与自然尺寸耦合**：`icons.png` rect 的 `w/h` 就是 `Image.width/height`；同组 rect 必须逐像素同规格；`Trials.ICON_W/H[i]`＝`tree.png` 帧包围盒**精确值**。（`verify_trial_icons.py`）
- **buff `type` 别标 NEGATIVE** ⇒ 全图怪醒来。
- **最终延迟夹不住**：`Char.speed()`→`final`＋`speedRaw()`；「免费移动」漏 ⇒ 在 `spend` 再夹；`final attackDelay()`＋`attackDelayRaw()`；还须 grep 自算延迟站点（`CrystalGuardian`）。（`verify_hokma_delay.py`）
- **改敌方 HT 后 Boss 卡死**：`DwarfKing` 屏障已改 `barrierChip()`/`barrierThreshold()` 为 HT 派生。**凡改敌方 HT，先 grep 绝对数字回查「表达 HT 刻度」的单位**。（`verify_boss_phase_scales.py`）
- **浮层不继承精灵 alpha**：`CharSprite.draw()` 管不到挂**场景**的独立节点（血条、`EmoIcon`）。收口＝`HealthBar.setAlpha()`（⚠️ `extends Component`、无 `am`/`aa`，要写进三个 `ColorBlock`）＋`EmoIcon.draw()` 覆写。未收口：`ShieldHalo`/`IceBlock`/`TorchHalo`/`Emitter`/`FloatingText`。（`verify_netzach_overlay.py`）
- **UI 的 `visible` 未必是显示开关**：`TargetHealthIndicator.isVisible()` 被 `ChaoticCenser` 当「锁定目标」读 ⇒ 置 false 香炉静默失效；改显隐前先 grep 谁读它。
- **「隐藏/一律如何」别按类型枚举**：写死枚举 ⇒ `Heap.Type` 加成员就**静默漏一种**；改不变量后体内不出现 `Heap.Type`。**四个出口都要收口**＝`ItemSprite.view(Heap)`（闸门提到 `switch` 前）／`Heap.title()`（`!= FOR_SALE`）／`WndInfoItem(Heap)`／`LootIndicator.update()`（`callers_of('hidesGroundHeap')` **恰 4 文件**）。（`verify_yesod.py`）
- **核验两坑**：① 只**读**产物快照会假绿 ⇒ 挂 mtime 或先重跑探针；② `callers_of` 用 `token in read()` 被**注释**污染 ⇒ 先过保偏移 `strip_comments`。

## 专题要点
- **负数道具等级**：`Item.level` 纯 int、存档 `level<0→degrade`。A 显式（法杖/戒指/神器/饰品）／B 静默夹 0（`STRReq`/投掷/字形植物）／C 负向传递（近战负伤害、死杖、戒指反转、防具 `DRMax` 非单调）；下标处 `ChaoticCenser:161` 有守卫。**归档 `degrade(n)`、设值 `level(int)`**。（`docs/negative-item-levels.md`）
- **考验 TRIALS**：与挑战**平行独立**的位掩码，**绝不 set 挑战的位**；效果写在 `isChallenged(X) || isTrialled(Y)`。十条＝卡巴拉质点（全大写）。已实现 HOKMA／BINAH／CHESED／GEBURA／NETZACH（闪避按区递增＋距离淡出 ≤1/2~3/>3，**血条与状态标记一并淡出**）／HOD（命中按区递增；存活>300 获精英、**Boss 450**，收口 `Trials.hodEliteTurns(Mob)`）／YESOD（禁四类装备自然鉴定＋分解附带鉴定＋**地面堆一律「？」含六类容器**＋商店补鉴定卷轴）。「按区」口径＝`Dungeon.scalingDepth()`（**不是** `depth`）。（§8/§16/§17/§20/§21）
- **趣味挑战**：挑战内新增**分类**（不新增位段/不改存档）：`Challenges.FUN_MASK`＋`isFun/isRegular`、`regularMasks()/funMasks()`、**`nameId(mask)` 反查**；窗口**各清各的位**；计数走 `activeChallenges()/activeFun()`（⚠️ 选人界面 `Dungeon.challenges` 恒 0 ⇒ 用**带参**版）。（`verify_fun_challenges.py`）
- **GEBURA「锁血」**：濒死进 `N=EXP` 回合无敌（`Mob.geburaGrace`＋`GeburaGrace`），那回合额外「僵直」不动（`Mob.geburaStagger`，在 `paralysed` **之后**消费，不进存档）。⚠️ 自写 `act()` 不调 `super.act()` 者绕过（全仓唯一＝`YogDzewa`）。⚠️ 掉落：锁血那刻抄 `geburaLuckyProc`/`geburaWealthBonus`，死亡时 **live 优先、取不到才回落**，回落值**当参数**传进 `tryForBonusDrop`。（`verify_chesed_gebura.py`｜`verify_gebura_drop.py`）
- **依赖「英雄击杀」的判据**：改「**携带 buff 的怪死亡**」，收口 `Mob.die` ENEMY 块**开头**（**必须早于 `super.die`**，之后 `Char.onRemove` 会 `detach()` 全部 buff）。闸门抄 `geburaAimMarked`/`geburaInstrTarget`。（`verify_marked_kill_routes.py`）
- **矿洞虚空地形**：`Level.handlesChasmFall()` 覆写＋`Chasm.heroFall` 早返回；`carveVoid` 只吃 `EMPTY`/`EMPTY_DECO`→`CHASM`，不变量＝**圆盘夹在深内区**、**外圈 50% 抽完收回孤立格**，不改 `Random.Int(2)` 顺序。⚠️ **`create()` 后改地形必须走 `Level.set`/`updateCellFlags`**。（`MiningVoidProbe.java`）
- **事件型规则（BINAH 范式）**：唯一入口＋**不动随机流**。`Item.random()` 改 `final`、子类留 `randomRaw()`；**判定顺序＝语义**；收口＝`Trials.flattenQuestReward`（任务 NPC 事后 `upgrade`）、`Trials.curseReverseLevel`。（`verify_binah_gen.py`）
- **一次性无敌（`HolyBarrier`）**：判据 `blocks(Char,Object)`＝**黑名单**（排 `src instanceof Buff` 自身 DOT 与 `SelfHarmCost`）；唯一调用点在 `Hero.damage` 靠前处（早于 `flinch`/`perseveranceCap`）。⚠️ Blob 型环境伤害（毒气/电击/雷云）会吃掉屏障。（`verify_holy_card.py`）
- **「任意 X」炼金配方**：不能用 `Recipe.SimpleRecipe`（按 `getClass()==inputs[i]` 精确匹配）⇒ 自实现 `Recipe`（`instanceof`）；`findRecipes` **按原料件数分桶**（1/2/3）。指南页用 `X.PlaceHolder`。
- **考验选择界面＝生命之树**：`WndTrials` 内容 **120×236**（＝`WndChallenges` 逐像素同规格）。⚠️ **尺寸别猜屏幕高**（`Window.resize()` 不夹屏幕）⇒ 对齐已知能完整显示的窗口。`A=47.5`、`ICON_SCALE=1.40`；缩放只改显示尺寸（`icon.scale.set(dw/W,dh/H)`）；三行 `tree.png` 只改 `frame()` 的 y；⚠️ **开关由详情窗在 `super` 之后构造**。（`verify_trials_tree_ui.py`｜skill `egopd-trials-tree-ui`）
- **蘑菇 7 种**（Easily-Sprouted-PD）：`items.png` 第 45 行 `xy(1,45)~xy(7,45)`；基类 `MushroomFood`（**Boss 层禁食须在 `super.execute` 前 return**）＋6 子类＋buff `BerryRegeneration`（`POSITIVE`、走 `heal()`）。**未接获取途径**。（`verify_mushrooms.py`｜skill `egopd-port-old-mod-content`）
- **调试道具「矿洞跃迁符」**：① 虚空只在 `Blacksmith.Quest.Type()==CRYSTAL` 下生成 ⇒ 临时顶替再还原；② 深度必须显式 11~14；③ 走 `InterlevelScene.Mode.RETURN`＋`Level.beforeTransition()`。范式＝`TestPortal`。（`verify_crystal_mine_warp.py`）
- **其余专档**：调试控制台 `windows/WndDebug`；按等级切形态武器（`MorphWeapon.morphInto()`，**别在 `activate()` 变形** ⇒ `FormKeeper extends Buff`）；原版神器强化 5 件（`ArtifactEnhanceRecipe`＋`QuickRecipe` case 8）；遗物落四处（`ItemSpriteSheet` 常量+rect、`Catalog.MISC_CONSUMABLES`、zh+en 文本、`RemainsItem.get`）；地形编辑器（`StandardRoom.chances` 每行长度＝`rooms.size()`）。
- **`_chk/` 工具**：`check_utf8_all.py`（必跑）、`check_unused_imports.py`、`verify_sprite_frames.py`、`docfind.py`、`build_doc_index.py --apply`、`_build_marked_kill.sh`。
