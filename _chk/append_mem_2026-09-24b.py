# -*- coding: utf-8 -*-
"""向 .workbuddy/memory/2026-09-24.md 追加「修两个 Bug」小节的收尾记录。幂等：小节标题已存在则跳过。"""
import os

LOG = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   '.workbuddy', 'memory', '2026-09-24.md')
KEY = '## 追加（同日晚四）：修两个 Bug'

BODY = """
---

## 追加（同日晚四）：修两个 Bug —— 背叛家人者漏免攻击延迟 / 死亡证明陈列室缺骷髅钥匙

用户原话：「修复中指长兄角色的背叛家人者分支，在分支特性触发、自动消耗复仇账簿充能免除莱瓦汀系列武器
使用的力量惩罚时，只消除了精准惩罚但没有消除攻击延迟惩罚的bug；需要检查道具『死亡证明』中特殊层内的
道具生成，目前其中缺少了神器『骷髅钥匙』。」

### 1. Bug A：消费点从 `onHeroAttackResolved` 下移到 `Hero.spend(float)`

- **根因是时序，不是算术**。力量惩罚有两条独立出口，都读 `STRReq() - owner.STR()`：
  `Weapon.accuracyFactor`（精准 `÷1.5ⁿ`）与 `Weapon.baseDelay`（延迟 `×1.2ⁿ`），
  均受 `SealedSwordBase.ignoresStrengthPenalty` 门控。
  精准在**命中判定前**算 ⇒ 那时补足还在，免掉了；攻击延迟要等 `Char.attack` 尾巴之后
  （`Talent.onHeroAttackResolved` → `Hero.onAttackComplete → spend(attackDelay())`）才算 ⇒
  旧代码在 `onHeroAttackResolved` 就把补足摘了 ⇒ `Hero.STR()` 已回退 ⇒ `×1.2ⁿ` 漏下来。
  用户观察到的「只免精准」正是这条链。
- **落点 3 文件**：
  - `actors/hero/Hero.java`：`spend(float)` 在 `super.spend(time)` + `SilentPrice.resetIdleClock()` **之后**
    调 `FamilyBetrayal.consumeAfterAttack(this)`。
  - `actors/hero/Talent.java`：`onHeroAttackResolved` 里那句摘除**删除**（留注释指向新消费点）；
    `onHeroAttackStarted` 保留。
  - `actors/buffs/FamilyBetrayal.java`：`onAttackStarted` 在算 `need` **之前**先 `consumeAfterAttack(hero)`
    摘一次**残留**（兜「buff 已挂、却走到早退路径、一次都没 spend」⇒ 否则残留垫着力量令 `need` 恒 0，
    变成「付一次钱、永久挥得动」）；类注释「生命周期」与 `consumeAfterAttack` 注释改写。`CHARGE_COST=5`
    / `DURATION=1f` / `amountOf` / `apply` 均未动。
- **为什么落点是对的**：`Hero.spend(float)` 是英雄**所有回合成本的唯一出口**（平砍 `onAttackComplete→
  spend(attackDelay())`、连击/武技 `spendAndNext(hero.attackDelay())`、buff 驱动一击）⇒ 一次覆盖全部
  攻击路径；仍只维持**一击**；没挂该 buff 时是**空操作**（一次 `hero.buff()` 查表），放热路径无副作用；
  不新增字段、不碰 `Random`。

### 2. Bug B：`MuseumLevel.skip(Class)` 收窄

- 原判据同时过滤 `ClassArmor` 与 `SkeletonKey`；而 `SkeletonKey` **本就注册在
  `Generator.Category.ARTIFACT.classes`**（神器图鉴有它一格）⇒ 陈列室唯独缺它。
- 改为**只**过滤英雄专属盔甲：`return ClassArmor.class.isAssignableFrom(c);`；删掉因此不再用的
  `import …artifacts.SkeletonKey;`；更新 `createItems` 段注释与 `skip` javadoc。
- 真机核验 `_chk/MuseumLocCheck.java`（反射调 `skip` + 复刻 `createItems` 循环逐类 `newInstance`）：
  陈列 **292**（容量 30×14＝420）、`SkeletonKey` **在列**、只过滤 **6 件 ClassArmor**（其余 `newInstance`
  失败按栈里是否含 `ItemSpriteSheet`/`TextureCache`/`Pixmap` 归为「GL 依赖失败」，真失败＝0）。

### 3. 核验与文档

- `_chk/verify_2026-09-24.py`：**96 条全绿**（79 静态源码断言 + 备份源反例自测 + `javac` 告警**类别**对照
  + `javap` 字节码证据 + 真机 MuseumLocCheck）。
- `javac`（4 文件）EXIT=0；`check_utf8_all.py` **1456 OK**；`check_unused_imports.py` **ALL PASS**。
- 补丁脚本 `_chk/patch_2026-09-24.py`（字节级 + 幂等 + 行尾归一），改前副本在 `_chk/_bak_2026-09-24/`。
- `docs/features.md`：文末新增同名整节（`_chk/append_features_2026-09-24b.py`，幂等），
  并**修正 2026-09-09 段里已过期的**「ARTIFACTS 段排除 SkeletonKey（钥匙）」那句（加删除线 + 注明已取消）。
- **未升版本、未打包**（用户只要求修这两处 bug）。

### 4. 环境事实（顺手澄清一处易错点）

- **版本**：`build.gradle` `ext` 仍 `appVersionCode=934 / appVersionName='0.3.4'`（本次未动）。
- ⚠️ **两个「包名」不是一回事**：`appPackageName = 'com.mypd.mypixeldungeon'`（Android
  `applicationId`，打包后 `.indev` 后缀）；而 **Java 源码包名仍是上游的
  `com.shatteredpixel.shatteredpixeldungeon`**。本轮核验初期按 `com.mypd.mypixeldungeon.indev`
  去拼源码路径 ⇒ 扑空（`No such file or directory`）。改源码 / 跑核验请一律用
  `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/`。
"""


def main():
    with open(LOG, 'rb') as f:
        raw = f.read()
    if KEY.encode('utf-8') in raw:
        print('SKIP: 小节已存在，未改动')
        return
    text = raw.decode('utf-8').rstrip('\n')
    out = text + '\n' + BODY.rstrip('\n') + '\n'
    with open(LOG, 'wb') as f:
        f.write(out.encode('utf-8'))
    print('OK: 追加 %d 字节' % (len(out.encode('utf-8')) - len(raw)))


if __name__ == '__main__':
    main()
