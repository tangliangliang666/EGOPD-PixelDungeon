# `_chk/` 脚本索引（生成物）

> 由 `python _chk/build_doc_index.py --apply` 生成。**改完代码先在这里挑回归脚本**，别整个目录乱翻。
> 「自测」列＝脚本自带反例自测模式（会故意喂反例、验证断言本身有效，而不只是全绿）。

| 脚本 | 分类 | 自测 | 用途（取自文件头部说明） |
|---|---|---|---|
| `ArtifactLocCheck.java` | 其它 |  | （无说明） |
| `BinahGenProbe.java` | 其它 |  | 考验 BINAH（理解）「生成时等级规则」的行为真值表探针（纯读，不改游戏代码） |
| `BossPhaseScaleProbe.java` | 其它 |  | （无说明） |
| `ChallengesLocCheck.java` | 其它 |  | （无说明） |
| `ChesedGeburaProbe.java` | 其它 |  | 考验 CHESED（慈悲）/ GEBURA（严厉）的行为探针（真跑游戏类，不改任何游戏文件） |
| `ClassDescCheck.java` | 其它 |  | 中指长兄四个键必须存在且非空 |
| `CropIcons.java` | 其它 |  | （无说明） |
| `DwarfKingBarrierProbe.java` | 其它 |  | 矮人国王二阶段「王座屏障」刻度的行为探针（真跑游戏类，不改任何游戏文件） |
| `FirelinkLocCheck.java` | 其它 |  | （无说明） |
| `FrameCrop.java` | 其它 |  | （无说明） |
| `GeburaDropProbe.java` | 其它 |  | GEBURA 掉落修复的行为探针（真跑游戏类，不改任何游戏文件） |
| `GritLocCheck.java` | 其它 |  | （无说明） |
| `HodNetzachProbe.java` | 其它 |  | 考验 HOD（荣耀）/ NETZACH（胜利）的行为探针：真调真方法，打印实测值 + 断言。 |
| `InstExecLocCheck.java` | 其它 |  | InstantExecution（无参的都是 Messages.get(this, key)，只有 devour 传 2 个 int） |
| `LabelIcons.java` | 其它 |  | （无说明） |
| `LocCheck.java` | 其它 |  | 新键：逐条实取 |
| `MarkedKillProbe.java` | 其它 |  | 「带着标记倒下」击杀判定的行为探针（真跑游戏类，不改任何游戏文件） |
| `MiningVoidProbe.java` | 其它 |  | 水晶任务矿洞层「虚空地形」的行为探针（真跑游戏类，不改任何游戏文件） |
| `MuseumLocCheck.java` | 其它 |  | （无说明） |
| `NarcissusLocCheck.java` | 其它 |  | （无说明） |
| `NegLevelProbe.java` | 其它 |  | 负数道具等级调研探针（纯读，不改任何游戏代码）：真跑构造道具压到负等级，读派生数值 + 存档往返实证，30 项断言 |
| `RemainsLocCheck.java` | 其它 |  | （无说明） |
| `SealedSwordLocCheck.java` | 其它 |  | —— 系列基类：动作与提示（只有 need_charge_or_hp / unsealed / swapped 带参）—— |
| `TrialBadgesProbe.java` | 其它 |  | 与 Trials.MASKS 同序：卡巴拉之树自上而下 |
| `TrialTextGuardProbe.java` | 其它 |  | （无说明） |
| `TrialsLocCheck.java` | 其它 |  | （无说明） |
| `WandmakerLocCheck.java` | 其它 |  | （无说明） |
| `YesodProbe.java` | 其它 |  | 考验 YESOD（根基）的行为探针：真调真方法，打印实测值 + 断言。 |
| `_count_ln.py` | 临时/日志残留 |  | （无说明） |
| `_dump_holycard.py` | 临时/日志残留 |  | 临时只读工具：核对「神圣卡 / 神圣屏障」的两处贴图是否就位。 |
| `_dupe_itemsprite_consts.py` | 临时/日志残留 |  | 解析 ItemSpriteSheet.java 里的公开常量，做「重复值」体检。 |
| `_ie_filter.py` | 临时/日志残留 |  | 过滤 javac 日志（GBK）中属于本次改动文件的行 |
| `_preview_row41.py` | 临时/日志残留 |  | 临时工具：把 items.png 的某一行渲染成放大预览 PNG（纯 Python，无 PIL）。 |
| `_rift_fx_preview.py` | 临时/日志残留 |  | 离线渲染「次元撕裂者」粒子特效预览图（纯 Python，无 PIL）。 |
| `_scan_row41.py` | 临时/日志残留 |  | 临时工具：扫描 items.png 第 41 行的 16x16 格占用情况，找出空位。 |
| `_te_harness.js` | 临时/日志残留 |  | 在 Node 里跑：把 render.js 的纯计算部分接上假 canvas，比对帧号 |
| `_zoom_items_row.py` | 临时/日志残留 |  | 把 items.png 的指定行放大导出，肉眼核验容器格子里画的是什么。 |
| `add_hod_elite_turns_chapter.py` | 其它 |  | 把 _chk/_hod_elite_turns_chapter.md 追加到 docs/features.md 末尾（幂等）。 |
| `add_hod_netzach_chapter.py` | 其它 |  | 把 HOD/NETZACH 章节追加到 docs/features.md（幂等、字节级、保 LF）。 |
| `add_marked_kill_docs.py` | 其它 |  | 把 2026-09-26「带着标记倒下 + 矿洞虚空」的章节追加到 docs/features.md（幂等）。 |
| `add_minewarp_docs.py` | 其它 |  | 把「矿洞虚空地形两条收紧 + 新调试道具矿洞跃迁符」追加到 docs/features.md（幂等）。 |
| `add_netzach_overlay_chapter.py` | 其它 |  | 把 NETZACH 浮层淡出章节追加到 docs/features.md（幂等、字节级、保 LF）。 |
| `add_trial_badges_text.py` | 其它 |  | 新增 10 条「考验通关成就」的徽章文本（zh + en）。 |
| `add_v033_changelog.py` | 其它 |  | 一次性补丁（幂等）：往 ui/changelist/EGOPD_Changes.java 插入 v0.3.3 改动栏。 |
| `add_yesod_chapter.py` | 其它 |  | 把 YESOD（根基）/ NETZACH 阈值章节追加到 docs/features.md（幂等、字节级、保 LF）。 |
| `add_yesod_container_chapter.py` | 其它 |  | 把 YESOD 容器问号章节追加到 docs/features.md（幂等、字节级、保 LF）。 |
| `align_text.py` | 其它 |  | 一次性文案对齐：2% -> 5%、剔除修饰句、zh/en 内容对齐。每处替换断言恰好命中 1 次。 |
| `align_text2.py` | 其它 |  | 补丁 2：修 /n/n 换行笔误（9 处）+ 去掉 everlasting_grudge 英文的修饰性开头。 |
| `analyze_banners.py` | 其它 |  | 分析用户手工改过的 banners.png：核对新帧范围、找可用的空白区。 |
| `analyze_icon_center.py` | 其它 |  | 诊断：tree.png 里每个图标的「实体核心」相对我裁出的包围盒中心偏了多少。 |
| `analyze_icons_region.py` | 其它 |  | 分析 icons.png 上指定矩形区域的实际内容（包围盒 / 是否越界 / 是否为空） |
| `analyze_tree_png.py` | 其它 |  | 分析 tree.png：尺寸、16x16 帧网格、逐帧非透明像素包围盒 |
| `apk_size_diff_033.py` | 其它 |  | EGOPD 发布核验 ④（v0.3.3）：新旧包体积变化必须能解释。 |
| `apk_size_diff_034.py` | 其它 |  | EGOPD 发布核验 ④（v0.3.4）：新旧包体积变化必须能解释。 |
| `apk_size_diff_035.py` | 其它 |  | EGOPD 发布核验 ④（v0.3.5）：新旧包体积变化必须能解释。 |
| `apk_size_diff_036.py` | 其它 |  | EGOPD 发布核验 ④（v0.3.6）：新旧包体积变化必须能解释。 |
| `append_features_2026-09-24b.py` | 其它 |  | 向 docs/features.md 追加 2026-09-24 修两个 Bug 的整节。幂等：标题已存在则跳过。 |
| `append_lloyd_text.py` | 其它 |  | 给洛伊德护符 + 禁疗 buff 追加 zh/en 文本键（纯 LF 文件，追加到尾部）。 |
| `append_mem_2026-09-24b.py` | 其它 |  | 向 .workbuddy/memory/2026-09-24.md 追加「修两个 Bug」小节的收尾记录。幂等：小节标题已存在则跳过。 |
| `append_memory_toggle_fix.py` | 其它 | 是 | 追加今日工作日志（幂等：已含标记就跳过）。 |
| `append_memory_window_size.py` | 其它 | 是 | ① 追加今日日志；② 订正 MEMORY.md 里那条过期的「考验选择界面」索引（还停在 0.70/140）。 |
| `armor_ability_text.py` | 其它 |  | 一次性补齐「咬紧牙关 / 永不遗忘」两个盔甲技能的 zh+en 文案，并顺手修掉两处文本缺失。 |
| `audio_format_check.py` | 其它 |  | 音频资源格式自检（EGOPD）。 |
| `build_doc_index.py` | 文档索引工具 |  | 生成/刷新项目的文档目录索引（幂等，可反复跑）。 |
| `check_icon_placement.py` | 检查/体检 |  | 量化：每个考验图标在 tree.png 的 16x16 格里到底放在哪，以及按不同口径居中的偏差。 |
| `check_named_short_desc.py` | 检查/体检 |  | 预演 ArmorAbility.namedShortDesc()：逐技能判断列表里会不会多出一行「技能名」。 |
| `check_unused_imports.py` | 检查/体检 | 是 | 未使用 import 自检（EGOPD）。 |
| `check_utf8_all.py` | 检查/体检 |  | 全仓源码编码体检：揪出「Edit 工具写坏」的两类痕迹。 |
| `compare_count_icons.py` | 其它 |  | 对比三枚计数图标（CHAL / TRIAL / FUN）的 7×7 字形是否逐像素相同，并出一张放大预览图。 |
| `crop_fun_icons.py` | 其它 |  | 把趣味挑战三枚图标裁出来、×8 放大拼成一排，供人眼确认（只读 icons.png）。 |
| `crop_icons_regions.py` | 其它 |  | 把 icons.png 的若干区域裁出来放大成预览 PNG，便于人眼确认「图标画在哪、有没有越界、和邻居有没有粘连」 |
| `doc_fix.py` | 其它 |  | docs/features.md：2% -> 5%（预支充能），并给解除钩子补上「无敌早退」的例外说明。 |
| `docfind.py` | 文档检索工具 |  | 跨语料文档检索：一条命令得到「文件:行号」，直接喂给 read 的 offset/limit。 |
| `dump.py` | 其它 |  | hero_icons.png: 128x256, 8 cols x 16 rows, 16px cells |
| `dumpTalent.py` | 其它 |  | 第 11 行（row index 10）=> 帧 10*cols .. 10*cols+cols-1 |
| `dumpbuff.py` | 其它 |  | buffs.png: 128x64, 7px cells -> 18 cols x 9 rows = 162 frames |
| `filter_javac_log.py` | 其它 |  | javac 日志是 GBK，直接 cat 是乱码。用法： |
| `filter_killfix_log.py` | 其它 |  | 从 javac 日志里按文件统计诊断条数（本批改动文件的告警要看清）。 |
| `fix_agents_budget_note.py` | 一次性修补 |  | 修正 AGENTS.md §0 里关于「指令预算截断方向」的错误表述（幂等）。 |
| `fix_boss_phase_scales.py` | 一次性修补 |  | 一次性补丁（幂等，字节级 IO）：把「用绝对生命值表达 HT 刻度」的阶段机制改为由 HT 反推。 |
| `fix_challenge_narcissus_text.py` | 一次性修补 |  | 插入新挑战「水仙追迹」的名称与描述（zh + en 各一份）。 |
| `fix_chesed_gebura_text.py` | 一次性修补 |  | 把 CHESED / GEBURA 的占位文案换成正式描述（zh + en 各一份）。 |
| `fix_classarmor_encoding.py` | 一次性修补 |  | 修复 ClassArmor.java：Edit 工具写坏的三处（两处注释末字被写成 E8 A1 3F，一处新注释被写成 GBK）。 |
| `fix_dwarfking_chesed.py` | 一次性修补 |  | 修复「考验 CHESED 导致矮人国王二阶段卡死」。 |
| `fix_features_dup.py` | 一次性修补 |  | 去掉 docs/features.md 里**重复追加**的批次块（同名 `# 标题` 只保留第一份）。 |
| `fix_gebura_stagger_text.py` | 一次性修补 |  | GEBURA 削弱（2026-09-24）：玩家文案里「无敌期间照常行动」→「除了刚被打成濒死的那一回合会僵直」。 |
| `fix_grit_text_escape.py` | 一次性修补 |  | 修复 gritteeth 系列文本在落盘时被「真换行」污染的问题（2026-09-20）。 |
| `fix_log_2026-09-23.py` | 一次性修补 |  | 行内 `python -c "..."` 的**反引号被 bash 当成命令替换**，把带反引号的片段整段删掉了。 |
| `fix_metamorph_talent_gate.py` | 一次性修补 |  | 一次性补丁（2026-09-20）：天赋判据去职业化 |
| `fix_narcissus_backpack_text.py` | 一次性修补 |  | 一次性补丁（幂等）：把「水仙追迹」挑战描述里的「圣剑已装备在主手」改成「圣剑放在背包里」。 |
| `fix_newline_escape.py` | 一次性修补 |  | 修复（2026-09-17）：instant_exec_text.py 把 Python 源码里的 ` |
| `fix_title_doc.py` | 一次性修补 |  | 修正 docs/title-scene-structure.md 里的过期表述与占位符（幂等）。 |
| `fix_tree_icon_note.py` | 一次性修补 |  | 一次性补丁：把 verify_tree_trials_icons.py 的「未绘制帧」相关说明更新为「十帧已全部绘齐」。 |
| `fix_trials_en_align_zh.py` | 一次性修补 |  | 一次性补丁（2026-09-24）：把英文 trials.binah/chesed/gebura_desc 对齐到用户改定的**中文基准**。 |
| `gen_probe_room.js` | 其它 |  | （无说明） |
| `gen_sample_room.js` | 其它 |  | （无说明） |
| `grass_frame_audit.py` | 其它 |  | 核验「高草」到底由哪些帧的哪些行像素组成（供「新增一种草皮」指南做绘制依据）。 |
| `grass_sandwich.py` | 其它 |  | 具体实例：角色站在「上下都是高草」的地块中，逐图层拆解 + 真实贴图渲染。 |
| `grass_zone_mismatch.py` | 其它 |  | 草叶细节「区域错配」核验（2026-09-19） |
| `hero_level_curve.py` | 其它 |  | EGOPD 调研工具：原版（SPD-classes 核心 + 本 mod 未改动的 Hero 部分）英雄 1~30 级基础数值曲线。 |
| `icon_adaptive_recolor.py` | 其它 |  | 把「legacy 扁平图」的重上色结果，同步到「自适应图标」的 foreground 分图层。 |
| `icon_palette_map.py` | 其它 |  | 反推「原版图标 → 新版图标」的调色映射。 |
| `import_hero_bgm.py` | 素材入库/转码 |  | 把四位角色专属 BGM 从素材（Downloads 下的中文名文件）转码为项目规格， |
| `import_lcb_sfx.py` | 素材入库/转码 |  | 边狱 wiki 音效入库流水线（EGOPD）。 |
| `instant_exec_text.py` | 其它 |  | 为「即刻处刑[莱瓦汀]」（InstantExecution）盔甲技能写入 zh / en 文本， |
| `loc_check.py` | 其它 |  | 核验本地化文本：重复键 / 新增键齐全 / 带参文本的 % 转义是否合法。 |
| `measure_fun_icons.py` | 其它 |  | 测量 icons.png 上「趣味挑战」三枚图标的实际不透明包围盒。 |
| `measure_icon_center.py` | 其它 |  | 核验：草稿渲染里，图标到底有没有「偏左/偏上」。 |
| `package_debug_console.py` | 打包分发 |  | 打包 debug-console 为可分发的 zip。 |
| `patch_2026-09-24.py` | 一次性文档/源码批改 |  | ① 「背叛家人者」预支账簿充能只免了精准惩罚、没免攻击延迟惩罚。 |
| `patch_banners_frames.py` | 一次性文档/源码批改 |  | 按用户手工改过的新版面配置 BannerSprites.java（并备份 banners.png）。 |
| `patch_binah_random.py` | 一次性文档/源码批改 |  | BINAH（理解）实现 · 第 1 步：把「自然生成」收口到 Item.random()。 |
| `patch_binah_text.py` | 一次性文档/源码批改 |  | BINAH（理解）实现 · 第 3 步：写入考验效果文本（trials.binah_desc，zh + en）。 |
| `patch_changelist_lobstage.py` | 一次性文档/源码批改 |  | 向 EGOPD_Changes.java 的 v0.3.5 条目追加一条「测试层草叶细节改取第二区」（幂等，字节级 IO）。 |
| `patch_doc_crossrefs.py` | 一次性文档/源码批改 |  | 把「活文档」里指向 AGENTS.md 旧节号的交叉引用改到重构后的实际位置。 |
| `patch_docs_031.py` | 一次性文档/源码批改 |  | 追加 v0.3.1 发布记录到 docs/features.md 与今日工作日志。 |
| `patch_docs_2026-09-20c.py` | 一次性文档/源码批改 |  | 文档同步（2026-09-20c）：AGENTS.md 陷阱行 + weapon-creation-guide + hermes-caduceus-design + features.md |
| `patch_docs_2026-09-23.py` | 一次性文档/源码批改 |  | 2026-09-23：把「封印之剑不可售」+「场景特效坐标速查」追加进 docs/features.md， |
| `patch_docs_toggle_fix.py` | 一次性文档/源码批改 | 是 | 同步 docs/features.md：① TIPHERETH 图标订正带来的数字；② 开关点不动的红线与修复。 |
| `patch_features_trials_tree.py` | 一次性文档/源码批改 | 是 | 把「考验界面改为生命之树排版」同步进 docs/features.md。 |
| `patch_features_window_size.py` | 一次性文档/源码批改 | 是 | features.md：把「生命之树」那节从「图标翻倍」改写到「窗口对齐挑战窗」（2026-09-25 第三轮）。 |
| `patch_heal_route.py` | 一次性文档/源码批改 |  | 一次性补丁：把全仓「真·回血」站点收口到 Char.heal(int) |
| `patch_hermes_firelink.py` | 一次性文档/源码批改 |  | 一次性补丁：2026-09-20 |
| `patch_hod_elite_turns.py` | 一次性文档/源码批改 |  | HOD（荣耀）精英化阈值调整：150 → 普通 300 / Boss 450。 |
| `patch_hod_netzach_text.py` | 一次性文档/源码批改 |  | 把考验 NETZACH / HOD 的描述从「占位」改成正式文案（中英各一份）。 |
| `patch_hokma_text.py` | 一次性文档/源码批改 |  | 幂等补丁：写入考验「HOKMA」相关的属性文本。 |
| `patch_icon_double.py` | 一次性文档/源码批改 |  | 把 WndTrials 的图标显示尺寸翻倍（0.70 → 1.40），并让窗口宽 140 → 148 以容纳。 |
| `patch_icon_script_rows.py` | 一次性文档/源码批改 |  | 修 verify_tree_trials_icons.py 里一条**过期判据**：整图高按「单行 16」写死。 |
| `patch_pitfall_toggle_fix.py` | 一次性文档/源码批改 |  | · AGENTS.md §6「高频静默陷阱（精简表）」—— 加一行（症状 / 要害）； |
| `patch_pitfalls_2026-09-23.py` | 一次性文档/源码批改 |  | 2026-09-23：把「专属武器锁在手上 ≠ 不可售」这条陷阱同时补进 |
| `patch_quickrecipe_resin.py` | 一次性文档/源码批改 |  | QuickRecipe 指南第 6 页（液金 / 松脂）补入：焦炭松脂系列、黄金松脂系列、洛伊德护符。 |
| `patch_release_031.py` | 一次性文档/源码批改 |  | 出 EGOPD v0.3.1：改版本号 + 写 EGOPD_Changes 更新日志。 |
| `patch_render_window_size.py` | 一次性文档/源码批改 |  | 预览脚本改造：从「图标翻倍」主题切到「窗口尺寸对齐挑战窗」主题。 |
| `patch_skill_toggle_fix.py` | 一次性文档/源码批改 | 是 | · TIPHERETH 图标订正带来的数字（104.27→103.27、56.6→57.1、ICON_W[5] 16→15）； |
| `patch_skill_window_size.py` | 一次性文档/源码批改 |  | 更新 egopd-trials-tree-ui skill：尺寸对齐挑战窗（第三轮），并订正「竖屏虚拟高≥540够用」那条错误结论。 |
| `patch_speed_terminal.py` | 一次性文档/源码批改 |  | 幂等补丁：把「移动速度」改成单一出口，让考验的延迟夹取无法被绕过。 |
| `patch_tiphereth_icon.py` | 一次性文档/源码批改 |  | 把 TIPHERETH（第 5 帧）的声明尺寸由 16x15 订正为 15x15。 |
| `patch_toggle_fix.py` | 一次性文档/源码批改 |  | 修复「开启考验」开关点击无反应 —— WndTrials.java 一侧。 |
| `patch_trials_window_size.py` | 一次性文档/源码批改 |  | 把 WndTrials 的窗口尺寸对齐「原版挑战窗」WndChallenges。 |
| `patch_verify_toggle_fix.py` | 一次性文档/源码批改 |  | 同步 verify_trials_tree_ui.py：把「开关必须由详情窗自己在 super 之后构造」钉进去。 |
| `patch_verify_window_size.py` | 一次性文档/源码批改 |  | 给 verify_trials_tree_ui.py 补上「窗口尺寸必须与原版挑战窗逐像素同规格」这条判据。 |
| `patch_wandmaker_intro.py` | 一次性文档/源码批改 |  | 老杖匠开场白：按职业六选一 → 不分职业一句（占位待填）。 |
| `patch_wndtrials_enable.py` | 一次性文档/源码批改 |  | 给 windows.properties / windows_zh.properties 追加「开启该考验」这条开关文案。 |
| `patch_yesod_desc_en.py` | 一次性文档/源码批改 |  | 把「容器不受影响」这半句从 YESOD 考验描述的**英文**版里删掉。 |
| `patch_yesod_text.py` | 一次性文档/源码批改 |  | YESOD（根基）与 NETZACH 阈值改动的文本补丁（zh + en）。 |
| `png_palette_dump.py` | 其它 |  | 4 位/2 位/1 位调色板 PNG 解码 + 主色统计（SPD 的 launcher 图标 foreground/background 就是 4-bit 索引 PNG）。 |
| `png_probe.py` | 其它 |  | 极简 PNG 解码探针（无第三方依赖），用于检查 items.png 某个 16px 格子里是否已有像素。 |
| `polish_agents_split.py` | 文档重构 |  | AGENTS.md 重构后的排版收尾（一次性，幂等）。 |
| `preview_tree_icons.py` | 其它 |  | 生成 tree.png 十帧的标注验收图（_chk/_tree_trials_icons_preview.png） |
| `recolor_fireball.py` | 其它 |  | 把标题火把的火焰改成「黑色为主 + 掺杂金黄色」。 |
| `recolor_title_bg.py` | 其它 |  | 把标题滚动背景的 4 张图改成深红色调（横幅 banners.png 不动）。 |
| `record_banners_layout.py` | 其它 |  | 把「横幅改新版面」记入文档（幂等）：features.md 追加批次 + 标题文档 §15.1 更新为当前状态。 |
| `record_fireball_recolor.py` | 其它 |  | 把「标题火把火焰改黑金」这次操作记入文档（幂等）。 |
| `record_fireball_red.py` | 其它 |  | 把「火焰改为整体亮红色（含先回退黑金方案）」记入文档（幂等）。 |
| `record_title_bg_recolor.py` | 其它 |  | 把「标题滚动背景改深红」这次操作记入文档（幂等）。 |
| `render_title_preview.py` | 其它 |  | 渲染标题界面的**模拟预览图**，供人工检查视觉效果（不进游戏）。 |
| `render_tree_draft.py` | 其它 |  | 把「生命之树 · 考验选择界面」草稿渲染成位图（与 show_widget 的 SVG 同一套几何）。 |
| `render_trials_final_mockup.py` | 其它 |  | 最终方案预览：0.70 等比缩放后塞进 140 内容宽的考验窗口 + 详情小窗。 |
| `render_trials_ingame.py` | 其它 |  | 按**实机绘制口径**渲染新版考验界面（生命之树）。 |
| `render_trials_mockup.py` | 其它 |  | 把「考验选择界面」的定稿示意（生命之树窗口 + 详情小窗）渲染成位图。 |
| `render_width_options.py` | 其它 |  | 窗口宽度方案对比：140 内容宽到底装不装得下这棵树？ |
| `revert_noexp_docs.py` | 其它 |  | 回退 2026-09-26 那一轮写进文档的两处表格行与一整章。 |
| `revert_noexp_trial_exempt.py` | 其它 |  | 回退「矿洞任务层小怪不吃 CHESED 回血 / GEBURA 豁免」这一轮改动（2026-09-26 傍晚）。 |
| `revert_verify_chesed_gebura.py` | 其它 |  | 回退 _chk/verify_chesed_gebura.py 里 2026-09-26 那一轮「grantNoExp」相关的核验。 |
| `selftest_trap_render.py` | 其它 |  | 反向自测：把 render.js 的陷阱优先级故意改错，verify_trap_render.js 必须报错。 |
| `split_agents_handbook.py` | 文档重构 |  | 把 AGENTS.md 按「常驻红线 + 目录索引」重构：长尾原文逐字迁入 docs/handbook/。 |
| `te_probe.js` | 其它 |  | （无说明） |
| `terrain_editor_assets.py` | 其它 |  | 地形编辑器 · 贴图资产提取（2026-09-19） |
| `terrain_editor_screenshot.py` | 其它 |  | 用系统 Edge 无头截屏工具栏（纯 Node 逻辑核验抓不到 UI 层问题） |
| `terrain_features_probe.py` | 其它 |  | terrain_features.png 帧槽探针（2026-09-19） |
| `terrain_tileset_check.py` | 其它 |  | 新地形图集自检工具：按「地形角色」核验每个 16x16 帧画得对不对（供 docs/terrain-creation-guide.md 配套使用）。 |
| `tileframe_dump.py` | 其它 |  | 纯 Python 读 PNG(无 PIL) 并把 spd 图集的指定 16x16 帧打成 ASCII，用于核验贴图分层结构。 |
| `tilemap_occlusion.py` | 其它 |  | 把角色画成纯品红(255,0,255)，再逐像素判定"是否被上层草叶覆盖"，输出精确的遮挡掩码。 |
| `tilemap_render_png.py` | 其它 |  | 用 SPD 真实贴图复刻分层 tilemap 渲染并输出 PNG，用于肉眼核验「两格高的草」的成因。 |
| `tilemap_sim.py` | 其它 |  | 复刻 SPD 分层 tilemap 的渲染，验证「两格高的草」是怎么拼出来的。 |
| `title_layout_calc.py` | 其它 |  | 标题界面（TitleScene）布局复算器：不启动游戏就能预览排版与溢出。 |
| `to_crlf.py` | 其它 |  | 把指定文本文件统一成 CRLF（幂等）；顺带报告是否含裸 LF 与是否 UTF-8 合法。 |
| `verify_2026-09-24.py` | 核验（回归断言） |  | 核验：2026-09-24 |
| `verify_apk_assets_031.py` | 核验（回归断言） |  | EGOPD 发布核验 ③：APK 内 assets/* 与工作区 core/src/main/assets 全量逐条 md5 比对。 |
| `verify_apk_assets_033.py` | 核验（回归断言） |  | EGOPD 发布核验 ③（v0.3.3）：APK 内 assets/* 与工作区 core/src/main/assets 全量逐条 md5 比对。 |
| `verify_apk_assets_034.py` | 核验（回归断言） |  | EGOPD 发布核验 ③（v0.3.4）：APK 内 assets/* 与工作区 core/src/main/assets 全量逐条 md5 比对。 |
| `verify_apk_assets_035.py` | 核验（回归断言） |  | EGOPD 发布核验 ③（v0.3.5）：APK 内 assets/* 与工作区 core/src/main/assets 全量逐条 md5 比对。 |
| `verify_apk_assets_036.py` | 核验（回归断言） |  | EGOPD 发布核验 ③（v0.3.6）：APK 内 assets/* 与工作区 core/src/main/assets 全量逐条 md5 比对。 |
| `verify_armor.py` | 核验（回归断言） |  | 本轮（咬紧牙关 / 永不遗忘 + 6 个天赋 + 2 个文本修复）的文本回归核验。 |
| `verify_artifact_enhance.py` | 核验（回归断言） | 是 | 原版神器强化形态（5 件）接线 + 文本 + 配方核验（2026-09-20）。 |
| `verify_artifact_sprites.py` | 核验（回归断言） | 是 | 原版神器强化形态（5 件 × 13 帧）贴图核验（2026-09-20）。 |
| `verify_atlas_import.js` | 核验（回归断言） | 是 | （无说明） |
| `verify_banners.py` | 核验（回归断言） |  | 新版横幅版面（banners.png + BannerSprites.java）的核验（回归断言）。 |
| `verify_binah_gen.py` | 核验（回归断言） | 是 | 核验考验「BINAH（理解）」的生成时等级规则实现。 |
| `verify_boss_phase_scales.py` | 核验（回归断言） | 是 | 核验：把「用绝对生命值表达 HT 刻度」的阶段机制改为由 HT 反推（2026-09-24）。 |
| `verify_challenge_narcissus.py` | 核验（回归断言） | 是 | 核验新挑战「水仙追迹」的**结构**实现（源码级）。 |
| `verify_chesed_gebura.py` | 核验（回归断言） | 是 | 核验考验「CHESED（慈悲）」与「GEBURA（严厉）」的实现。 |
| `verify_codegen_compiles.js` | 核验（回归断言） | 是 | （无说明） |
| `verify_console_fonts_and_mobs.py` | 核验（回归断言） | 是 | 核验 SPD 调试控制台的「宿主适配层」四个必须兜底的点。 |
| `verify_crystal_mine_warp.py` | 核验（回归断言） | 是 | 「矿洞跃迁符」（items/CrystalMineWarp.java）· 源码级核验（2026-09-26） |
| `verify_dimensional_ripper.py` | 核验（回归断言） | 是 | 次元撕裂者（三阶自定义武器）接线 + 文本 + 粒子 + 概率成长核验（2026-09-22）。 |
| `verify_doc_split.py` | 核验（回归断言） |  | 核验 AGENTS.md 的「常驻核心 + handbook 长尾」重构没有丢字（回归断言）。 |
| `verify_dropin_zip.py` | 核验（回归断言） |  | drop-in 压缩包独立可用性验证。 |
| `verify_dwarfking_chesed.py` | 核验（回归断言） | 是 | 核验：矮人国王二阶段「王座屏障」刻度（修 CHESED 引发的二阶段卡死）。 |
| `verify_editor_browser.js` | 核验（回归断言） |  | （无说明） |
| `verify_editor_layers_ui.js` | 核验（回归断言） | 是 | （无说明） |
| `verify_fireball.py` | 核验（回归断言） |  | 火焰改色后的核验（回归断言）。 |
| `verify_fun_challenges.py` | 核验（回归断言） | 是 | 「趣味挑战」分类的跨文件接线核验（EGOPD，2026-09-24）。 |
| `verify_fun_icons.py` | 核验（回归断言） | 是 | 核验「趣味挑战」三枚图标：Icons.java 里的 rect 是否与 icons.png 上真实绘制的像素对齐、是否与别的图标格冲突 |
| `verify_gebura_drop.py` | 核验（回归断言） | 是 | 「GEBURA 濒死无敌导致幸运附魔 / 财富戒指的**额外掉落判定**落空」修复的核验（EGOPD，2026-09-24）。 |
| `verify_grit_selfharm.py` | 核验（回归断言） |  | 回归核验（2026-09-20）：解放按天赋取上限 + 「自伤换成长」击穿「咬紧牙关」免死。 |
| `verify_hermes_firelink.py` | 核验（回归断言） |  | 核验：2026-09-20 |
| `verify_hod_netzach.py` | 核验（回归断言） | 是 | 核验考验「HOD（荣耀）」与「NETZACH（胜利）」的实现。 |
| `verify_hokma_delay.py` | 核验（回归断言） | 是 | 核验考验「HOKMA（智慧）」的延迟上下限实现。 |
| `verify_holy_card.py` | 核验（回归断言） | 是 | -*- coding: utf-8 -*- |
| `verify_item_placeholder.py` | 核验（回归断言） |  | 核验「道具占位图 = items.png 的口粮那一格」这条链路的每一环。 |
| `verify_item_tables.py` | 核验（回归断言） | 是 | 道具表核验（EGOPD 地形编辑器） |
| `verify_level_mode.js` | 核验（回归断言） | 是 | （无说明） |
| `verify_lloyd_healblock.py` | 核验（回归断言） | 是 | -*- coding: utf-8 -*- |
| `verify_lob_grass_stage.py` | 核验（回归断言） |  | 27 层草叶细节区域核验（2026-09-19 建；2026-09-25 改判据为「第二区」） |
| `verify_marked_kill_routes.py` | 核验（回归断言） | 是 | 「带着标记倒下」击杀判定 + 水晶矿洞层虚空地形 的核验（EGOPD，2026-09-26）。 |
| `verify_masterring_bind.py` | 核验（回归断言） |  | 大师指环「绑定后无法重新绑定」修复的跨文件不变量核验（2026-09-18）。 |
| `verify_metamorph_talents.py` | 核验（回归断言） |  | 蜕变卷轴天赋判据回归（2026-09-20 起） |
| `verify_mushrooms.py` | 核验（回归断言） | 是 | 可食用蘑菇（移植自 Easily Sprouted PD 的 items/food 蘑菇组）核验脚本。 |
| `verify_narcissus_sprites.py` | 核验（回归断言） |  | 水仙十字圣剑三形态贴图核验（2026-09-20）。 |
| `verify_netzach_overlay.py` | 核验（回归断言） | 是 | 核验考验「NETZACH（胜利）」的**浮层淡出**改动：怪头顶的血条与状态标记也要一起变淡/消失。 |
| `verify_oracle_fixes.py` | 核验（回归断言） |  | 神谕代行者三处修复的回归核验（纯源码静态断言，不跑 Gradle）。 |
| `verify_random_regions.js` | 核验（回归断言） | 是 | （无说明） |
| `verify_remains_relics.py` | 核验（回归断言） |  | 两位职业遗物「破损义眼 / 账簿残页」的接线核验（2026-09-18）。 |
| `verify_remains_sprites.py` | 核验（回归断言） |  | 用纯 Python 解码 items.png（无 PIL），逐个 dump 遗物所在矩形， |
| `verify_sealed_enchant.py` | 核验（回归断言） |  | 封印之剑系列「可附魔 / 可强化」修复的跨文件不变量核验（2026-09-18）。 |
| `verify_sealed_sellable.py` | 核验（回归断言） |  | 封印之剑系列「不可出售」回归核验（2026-09-23）。 |
| `verify_selfharm_exempt.py` | 核验（回归断言） |  | 核验「自伤换成长」豁免链是否完整（2026-09-18 中指长兄 T3「过人的毅力」）。 |
| `verify_sfx_assets.py` | 核验（回归断言） |  | 音效资源交叉校验（EGOPD）：Assets.Sounds 常量 ↔ 真实文件 ↔ all[] 注册。 |
| `verify_size_fuzz.js` | 核验（回归断言） |  | （无说明） |
| `verify_size_fuzz_ui.js` | 核验（回归断言） | 是 | （无说明） |
| `verify_sprite_frames.py` | 核验（回归断言） |  | 纯 Python（无 PIL）解码 PNG 核验精灵图帧布局：尺寸须被帧宽/帧高整除、每帧须有非透明像素、并统计半透明像素与重复帧。 |
| `verify_talent_rework.py` | 核验（回归断言） | 是 | 四项二层天赋改写 · 源码级核验（2026-09-26） |
| `verify_terrain_browser.py` | 核验（回归断言） | 是 | 地形编辑器 · 真实浏览器端到端核验（无头 Edge） |
| `verify_terrain_checker.js` | 核验（回归断言） |  | （无说明） |
| `verify_terrain_codegen.js` | 核验（回归断言） |  | （无说明） |
| `verify_terrain_editor_frames.py` | 核验（回归断言） |  | 地形编辑器 · 渲染帧号交叉核验（2026-09-19） |
| `verify_title_bg.py` | 核验（回归断言） |  | 滚动背景改色后的结构核验（回归断言；改了 TitleBackground 或这几张图都该跑）。 |
| `verify_trap_render.js` | 核验（回归断言） |  | （无说明） |
| `verify_trap_tables.py` | 核验（回归断言） | 是 | 核验地形编辑器里的「陷阱 / 植物」查表是否与 Java 源逐项一致。 |
| `verify_tree_rows_bbox.py` | 核验（回归断言） |  | 三行图标（tree.png 160x48）的包围盒是否逐格完全一致？ |
| `verify_tree_trials_icons.py` | 核验（回归断言） | 是 | 考验图标（tree.png）与 Trials.java 的尺寸表互查（2026-09-23） |
| `verify_trial_icons.py` | 核验（回归断言） | 是 | 核验「考验系统」三枚图标：Icons.java 里的 rect 是否与 icons.png 上真实绘制的像素对齐、是否与别的图标格冲突 |
| `verify_trials_tree_ui.py` | 核验（回归断言） | 是 | 考验「生命之树」界面（WndTrials / WndTrialInfo / Trials 图标行）静态回归。 |
| `verify_ui_ctor_order.py` | 核验（回归断言） | 是 | Component 构造顺序坑 · 静态回归。 |
| `verify_wandmaker_intro.py` | 核验（回归断言） |  | 老杖匠开场白「按职业六选一 → 不分职业一句」的源码级核验。 |
| `verify_water_layer.js` | 核验（回归断言） |  | 只支持 colorType 6 (RGBA8) 与 3 (调色板)，够用 |
| `verify_wnddebug_restore.py` | 核验（回归断言） | 是 | WndDebug 还原核验（EGOPD）。 |
| `verify_yesod.py` | 核验（回归断言） | 是 | 核验考验「YESOD（根基）」与 NETZACH 淡出阈值的改动。 |
| `zoom_icon_center.py` | 其它 |  | 放大现有树渲染图，并在每个节点中心画十字，用来判断图标是否真的居中。 |
| `ProbeFuzzRoom.java` | 其它 |  | （无说明） |
| `ProbeLayerRoom.java` | 其它 |  | 1) 骨架：整间铺墙，再把内部掏空 —— 与上游每个 StandardRoom 完全一致 |
| `ProbeLayerRoomLayered.java` | 其它 |  | 1) 骨架：整间铺墙，再把内部掏空 —— 与上游每个 StandardRoom 完全一致 |
| `TrialsLocCheck.java` | 其它 |  | （无说明） |
| `render.js` | 其它 |  | ---------------------------------------------------------------- 常量 |

共 255 个脚本。
