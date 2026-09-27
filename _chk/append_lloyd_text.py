# -*- coding: utf-8 -*-
"""给洛伊德护符 + 禁疗 buff 追加 zh/en 文本键（纯 LF 文件，追加到尾部）。"""
import os, sys

ROOT = r"D:\PD\core\src\main\assets\messages"

BLOCKS = [
    ("items/items_zh.properties", """
# 2026-09-20: 洛伊德护符（投掷 → 50回合禁疗；命中宝箱怪额外揭穿伪装 + 20回合麻痹/虚弱/致盲）
items.lloydtalisman.name=洛伊德护符
items.lloydtalisman.desc=曾经用于捕获不死人的道具，在50回合内，能令投中的目标无法恢复生命值。\\n\\n在命中宝箱怪时，尤其有效。\\n\\n白教的主神洛伊德早已无人信仰，但是捕获不死人的战斗从未停止。
"""),
    ("items/items.properties", """
# 2026-09-20: Lloyd's Talisman (thrown -> 50 turns of heal block; mimics are exposed and get 20 turns of paralysis/weakness/blindness)
items.lloydtalisman.name=Lloyd's Talisman
items.lloydtalisman.desc=An item once used to capture the undead. For 50 turns, the target it strikes cannot recover health.\\n\\nEspecially effective against mimics.\\n\\nLloyd, chief god of the Way of White, has long since lost every worshipper -- but the battle to capture the undead has never stopped.
"""),
    ("actors/actors_zh.properties", """
# 2026-09-20: 「禁疗」（洛伊德护符）—— 一切回血被强制置 0
actors.buffs.healblock.name=禁疗
actors.buffs.healblock.desc=无法恢复生命值。\\n\\n期间获得的一切血量恢复都会被强制置 0——治疗药剂、圣草、食物、吸血、再生，乃至 CHESED 考验的回血，一视同仁。\\n\\n剩余时长：%s回合
"""),
    ("actors/actors.properties", """
# 2026-09-20: "heal block" (Lloyd's Talisman) -- every source of healing is forced to 0
actors.buffs.healblock.name=heal block
actors.buffs.healblock.desc=Cannot recover health.\\n\\nEverything that would restore health is forced to 0 -- potions, sungrass, food, lifesteal, regeneration, and even the CHESED trial's mending.\\n\\nTurns left: %s
"""),
]


def main():
    dry = "--dry" in sys.argv
    for rel, block in BLOCKS:
        path = os.path.join(ROOT, rel.replace("/", os.sep))
        raw = open(path, "rb").read()
        crlf = raw.count(b"\r\n")
        bare = raw.replace(b"\r\n", b"")
        if crlf or bare.count(b"\r") or bare.count(b"\n") != raw.count(b"\n"):
            print("[FAIL] not pure LF: " + rel)
            sys.exit(1)
        if not raw.endswith(b"\n"):
            print("[FAIL] no trailing newline: " + rel)
            sys.exit(1)
        marker = block.strip().split("\n")[1].strip()
        if marker in raw.decode("utf-8"):
            print("  [skip] already present: %s :: %s" % (rel, marker))
            continue
        add = block.strip("\n") + "\n"
        before = raw.count(b"\n")
        if dry:
            print("  [dry] %-34s +%d line(s)" % (rel, add.count("\n")))
            continue
        open(path, "wb").write(raw + add.encode("utf-8"))
        print("  [ok] %-34s lines %d -> %d" % (rel, before, raw.count(b"\n") + add.count("\n")))


if __name__ == "__main__":
    main()
