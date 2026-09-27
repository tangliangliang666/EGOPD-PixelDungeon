#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
打包 debug-console 为可分发的 zip。

做三件事：
  1. （可选，加 --sync）把 :core 侧的真实适配层同步进 debug-console/example-spd-adapter/
  2. 剔除 build/ 等产物
  3. 打成 debug-console-<版本>.zip（顶层目录 debug-console/）

⚠️ 默认**不**同步 example-spd-adapter/
  该目录是给「别的项目」看的**通用参考实现**，其中 SpdDebugConsole.isAvailable()
  故意不含任何挑战门控（门控属于宿主游戏策略，不属于库）。
  而 :core 里的真实适配层**带** DEBUG_MODE 门控 —— 一旦盲目同步，就会把示例
  换成 EGOPD 专属版本，误导其它使用者。故同步改为显式 opt-in。

用法：
  python _chk/package_debug_console.py [版本号] [--sync]   # 版本号默认 1.0.0
"""

import os
import shutil
import sys
import zipfile

ROOT = r"D:\PD"
MODULE = os.path.join(ROOT, "debug-console")
ADAPTER_SRC = os.path.join(ROOT, "core", "src", "main", "java",
                           "com", "shatteredpixel", "shatteredpixeldungeon", "debug")

# never ship these
EXCLUDE_DIRS = {"build", ".gradle", "out", ".idea"}


def sync_adapter():
    """Copy the real SPD adapter into the module so the zip is self-describing.

    Off by default — see the module docstring. Even when enabled, SpdDebugConsole.java
    is skipped, because the example copy is intentionally gate-free.
    """
    dst = os.path.join(MODULE, "example-spd-adapter")
    os.makedirs(dst, exist_ok=True)
    n = 0
    for f in sorted(os.listdir(ADAPTER_SRC)):
        if f.endswith(".java") and f != "SpdDebugConsole.java":
            shutil.copy2(os.path.join(ADAPTER_SRC, f), os.path.join(dst, f))
            n += 1
    return n


def package(version):
    out = os.path.join(ROOT, f"debug-console-{version}.zip")
    if os.path.exists(out):
        os.remove(out)

    count = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for root, dirs, files in os.walk(MODULE):
            dirs[:] = sorted(d for d in dirs if d not in EXCLUDE_DIRS)
            for f in sorted(files):
                p = os.path.join(root, f)
                rel = os.path.relpath(p, os.path.dirname(MODULE))
                z.write(p, rel.replace(os.sep, "/"))
                count += 1
    return out, count


def main():
    argv = [a for a in sys.argv[1:] if not a.startswith("--")]
    flags = {a for a in sys.argv[1:] if a.startswith("--")}
    version = argv[0] if argv else "1.0.0"

    if not os.path.isdir(ADAPTER_SRC):
        print("找不到适配层源码:", ADAPTER_SRC)
        return 1

    if "--sync" in flags:
        n = sync_adapter()
        print(f"[1/2] 同步适配层 {n} 个文件 -> example-spd-adapter/（已跳过 SpdDebugConsole.java）")
    else:
        print("[1/2] 跳过适配层同步（示例副本保持通用去门控版；要覆盖请加 --sync）")

    out, count = package(version)
    print(f"[2/2] 打包完成: {out}")
    print(f"      条目数 {count}，大小 {os.path.getsize(out)} 字节")
    print()
    print("验证独立可用性（解压到别的项目编译+运行）：")
    print(f"      python _chk/verify_dropin_zip.py {os.path.basename(out)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
