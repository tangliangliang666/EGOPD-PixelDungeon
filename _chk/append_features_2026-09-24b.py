# -*- coding: utf-8 -*-
"""向 docs/features.md 追加 2026-09-24 修两个 Bug 的整节。幂等：标题已存在则跳过。
docs/*.md 为纯 LF：读字节、去尾部多余空行、以 LF 追加、写回。"""
import os

DOC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'docs', 'features.md')
TITLE = '# 2026-09-24 修两个 Bug：背叛家人者漏免攻击延迟惩罚 / 死亡证明陈列室缺「骷髅钥匙」'

BODY = TITLE + """

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
"""


def main():
    with open(DOC, 'rb') as f:
        raw = f.read()
    # 行尾断言：docs/*.md 应为纯 LF
    assert raw.count(b'\r\n') == 0, 'features.md 出现 CRLF，与预期（纯 LF）不符'
    if TITLE.encode('utf-8') in raw:
        print('SKIP: 标题已存在，未改动')
        return
    # 去掉尾部多余空行，统一以 "\n\n" 分节
    text = raw.decode('utf-8').rstrip('\n')
    out = text + '\n\n' + BODY.rstrip('\n') + '\n'
    with open(DOC, 'wb') as f:
        f.write(out.encode('utf-8'))
    print('OK: 追加 %d 字节' % (len(out.encode('utf-8')) - len(raw)))


if __name__ == '__main__':
    main()
