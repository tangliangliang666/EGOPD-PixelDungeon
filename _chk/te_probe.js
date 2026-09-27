/* 地形编辑器 · 浏览器探针（被 verify_editor_browser.js 注入到真 index.html 里执行）
 *
 * 契约：在页面里**同步**跑一遍，把结果写成 `k=v` 行填进 <pre id="TE_PROBE">，
 *       外层 verify_editor_browser.js 读 iframe 的 DOM 取回这些键值对。
 *
 * ⚠️ 三个必须遵守的点（都是踩过的坑）：
 *   ① 本文件会被**整段拼进** `<script>` 里（不是 <script src>），
 *      所以正文里**绝不能出现 `</` + `script>` 这个字面量** —— 它会把宿主标签提前闭合，
 *      后面的代码全被当成 HTML 文本，探针静默不执行、title 也不变（极难排查）。
 *      需要提及时请写「script 结束标签」这种描述性说法。
 *   ② 必须等 TE_EDITOR / TE_GEN 就绪 —— 由外层把它放在 app.js **之后**同步执行，
 *      此处不必自己等（boot() 是同步跑到底的）。
 *   ③ `#codeBox` 是 <textarea>，读 `.value` 而不是 innerHTML；
 *      检查器结果在 `#checkList` 的 <li> 里，也要读 textContent。
 *
 * 本探针只做「读状态 + 派发真鼠标事件」，不做任何断言 ——
 * 断言全部在外层 verify_editor_browser.js，这样探针改坏了外层立刻能看出来。
 */
(function () {
	var out = [];
	function kv(k, v) { out.push(k + '=' + v); }

	var Ed = window.TE_EDITOR, R = window.TE_RENDER, Gen = window.TE_GEN;
	var doc = document;
	function $(sel) { return doc.querySelector(sel); }

	/* ---------------------------------------------- A. UI 装配 */
	kv('A1 sectLayer存在', !!$('#sectLayer'));
	var selTrap = $('#selTrap'), selPlant = $('#selPlant');
	kv('A2 selTrap选项', selTrap ? selTrap.options.length : -1);
	kv('A3 selPlant选项', selPlant ? selPlant.options.length : -1);
	kv('A4 chkWater勾选', $('#chkWater') ? $('#chkWater').checked : 'no-el');
	kv('A5 lyrTrap默认选中', $('#lyrTrap') ? $('#lyrTrap').checked : 'no-el');
	kv('A6 陷阱首项', selTrap && selTrap.options[0] ? selTrap.options[0].value : '');
	kv('A7 植物首项', selPlant && selPlant.options[0] ? selPlant.options[0].value : '');

	/* 本轮新增：道具层的控件必须也在 */
	var selItem = $('#selItem'), selHeap = $('#selHeap');
	kv('A8 selItem选项', selItem ? selItem.options.length : -1);
	kv('A9 selItem分组', selItem ? selItem.querySelectorAll('optgroup').length : -1);
	kv('A10 selHeap选项', selHeap ? selHeap.options.length : -1);
	kv('A11 chkItems勾选', $('#chkItems') ? $('#chkItems').checked : 'no-el');
	kv('A12 lyrItem存在', !!$('#lyrItem'));
	kv('A13 layerHint存在', !!$('#layerHint'));
	kv('A14 旧trapHint已移除', !$('#trapHint'));
	kv('A15 陷阱首项启用', selTrap && selTrap.options[0] ? !selTrap.options[0].disabled : false);

	/* ---------------------------------------------- 真鼠标事件助手
	 * 直接派发 mousedown/mouseup 到 canvas，走 app.js 的完整事件路由。
	 * 坐标换算：canvas 用 CSS 尺寸显示，但内部按 16px/格 × zoom 绘制，
	 * 所以要先算 boundingRect，再按 rect 宽高折算到画布逻辑坐标。 */
	var cv = $('#cv');
	function clickCell(cx, cy) {
		var r = cv.getBoundingClientRect();
		/* canvas 的逻辑宽度 / 显示宽度 = 每 CSS 像素对应多少逻辑像素 */
		var sx = cv.width / r.width, sy = cv.height / r.height;
		/* 目标格中心在画布逻辑坐标里的位置（16px/格） */
		var lx = cx * 16 + 8, ly = cy * 16 + 8;
		var clientX = r.left + lx / sx, clientY = r.top + ly / sy;
		['mousedown', 'mouseup'].forEach(function (type) {
			var ev = new MouseEvent(type, {
				bubbles: true, cancelable: true, view: window,
				button: 0, buttons: 1, clientX: clientX, clientY: clientY
			});
			cv.dispatchEvent(ev);
		});
	}
	function pos(x, y) { return y * Ed.E.w + x; }

	/* ---------------------------------------------- B. 陷阱层落笔 */
	/* 先切回陷阱层并选 FrostTrap，然后点 (2,2) */
	var lyrTrap = $('#lyrTrap');
	if (lyrTrap) {
		lyrTrap.checked = true;
		lyrTrap.dispatchEvent(new Event('change', { bubbles: true }));
	}
	if (selTrap) {
		selTrap.value = 'FrostTrap';
		selTrap.dispatchEvent(new Event('change', { bubbles: true }));
	}
	clickCell(2, 2);
	var t22 = Ed.E.traps && Ed.E.traps[pos(2, 2)];
	kv('B1 traps[2,2]', t22 ? t22.cls : 'null');
	kv('B2 map[2,2]', Ed.E.map[pos(2, 2)] + '(TRAP=' + R.T.TRAP + ')');
	kv('B3 trapFrame[2,2]', R.trapFrame(pos(2, 2)));

	/* ---------------------------------------------- C. 植物层落笔 */
	var lyrPlant = $('#lyrPlant');
	if (lyrPlant) {
		lyrPlant.checked = true;
		lyrPlant.dispatchEvent(new Event('change', { bubbles: true }));
	}
	if (selPlant) {
		selPlant.value = 'Sungrass';
		selPlant.dispatchEvent(new Event('change', { bubbles: true }));
	}
	clickCell(5, 3);
	var p53 = Ed.E.plants && Ed.E.plants[pos(5, 3)];
	kv('C1 plants[5,3]', p53 ? p53.cls : 'null');
	kv('C2 map[5,3]', Ed.E.map[pos(5, 3)] + '(GRASS=' + R.T.GRASS + ')');
	kv('C3 plantFrame[5,3]', R.plantFrame(pos(5, 3)));

	/* ---------------------------------------------- D. 未发现陷阱 */
	if (lyrTrap) {
		lyrTrap.checked = true;
		lyrTrap.dispatchEvent(new Event('change', { bubbles: true }));
	}
	if (selTrap) {
		selTrap.value = 'AlarmTrap';
		selTrap.dispatchEvent(new Event('change', { bubbles: true }));
	}
	var chkVis = $('#chkTrapVisible');
	if (chkVis && chkVis.checked) {
		chkVis.checked = false;
		chkVis.dispatchEvent(new Event('change', { bubbles: true }));
	}
	clickCell(7, 2);
	kv('D1 map[7,2]', Ed.E.map[pos(7, 2)] + '(SECRET_TRAP=' + R.T.SECRET_TRAP + ')');
	kv('D2 featuresVisual', R.featuresVisual(pos(7, 2)) + '(期望-1)');
	if (chkVis) {
		chkVis.checked = true;
		chkVis.dispatchEvent(new Event('change', { bubbles: true }));
	}

	/* ---------------------------------------------- G. 道具层落笔（本轮新增）
	 * 用真鼠标事件验证「画笔落道具 ⇒ E.items 有值、而地形**不变**」 */
	var lyrItem = $('#lyrItem');
	if (lyrItem) {
		lyrItem.checked = true;
		lyrItem.dispatchEvent(new Event('change', { bubbles: true }));
	}
	if (selItem) {
		selItem.value = 'Food';
		selItem.dispatchEvent(new Event('change', { bubbles: true }));
	}
	if (selHeap) {
		selHeap.value = 'HEAP';
		selHeap.dispatchEvent(new Event('change', { bubbles: true }));
	}
	/* 挑一个已知是空地的格子，记录落笔前的地形 */
	var beforeTile = Ed.E.map[pos(9, 6)];
	clickCell(9, 6);
	var i96 = Ed.E.items && Ed.E.items[pos(9, 6)];
	kv('G1 items[9,6]', i96 ? i96.cls : 'null');
	kv('G2 items[9,6]堆型', i96 ? i96.heap : 'null');
	kv('G3 items[9,6]ctor', i96 ? i96.ctor : 'null');
	/* ⚠️ 值里**不能出现 '='** —— 外层按第一个 '=' 切分 k/v，多一个等号会把值截断。
	 *    所以这里只输出 true/false，把「前后都是几」另起一个键。 */
	kv('G4 地形未被改动', (Ed.E.map[pos(9, 6)] === beforeTile) + '');
	kv('G4b 落笔前后地形值', beforeTile + '->' + Ed.E.map[pos(9, 6)]);
	kv('G5 道具层非空数', Ed.E.items ? Ed.E.items.filter(Boolean).length : -1);

	/* 再点一次同格：应当被「同类同堆型跳过」逻辑挡掉，数量不增 */
	var cntBefore = Ed.E.items.filter(Boolean).length;
	clickCell(9, 6);
	kv('G6 重复落笔被跳过', (Ed.E.items.filter(Boolean).length === cntBefore) + '');

	/* ---------------------------------------------- E. 代码面板 */
	/* app.js 在每次 fullRefresh 里重算代码；这里手动触发一次保证是最新的 */
	if (typeof window.TE_APP === 'object' && window.TE_APP && window.TE_APP.refreshCode) {
		try { window.TE_APP.refreshCode(); } catch (e) { /* 非致命 */ }
	}
	var box = $('#codeBox');
	var code = box ? (box.value || '') : '';
	kv('E1 code长度', code.length);
	kv('E2 含FrostTrap', /FrostTrap/.test(code) + '');
	kv('E3 含setTrap', /level\.setTrap\(/.test(code) + '');
	kv('E4 含Sungrass.Seed', /Sungrass\.Seed/.test(code) + '');
	kv('E5 含Pointimport', /import com\.watabou\.utils\.Point;/.test(code) + '');
	kv('E6 含AlarmTrap', /AlarmTrap/.test(code) + '');
	/* 本轮新增：道具层必须翻成 level.drop */
	kv('E7 含levelDrop', /level\.drop\(/.test(code) + '');
	kv('E8 含newFood', /new Food\(\)/.test(code) + '');
	kv('E9 不含addItemToSpawn', !/addItemToSpawn/.test(
		code.split('\n').filter(function (l) { return !/^\s*\/\//.test(l); }).join('\n')) + '');

	/* ---------------------------------------------- F. 房间检查器 */
	var cl = $('#checkList');
	kv('F1 检查器文本', cl ? (cl.textContent || '').replace(/[\r\n]+/g, ' ') : '');

	/* ---------------------------------------------- H. 外部图集（C.3）
	 * 真浏览器里能验的：入口按钮在不在、TE_APP 探针在不在、
	 * 内置图集登记成 6 张且都 builtin、初始没有外部图集、提示文案。
	 *
	 * 不在这里**真的导入**一张图：真浏览器无法凭空造 File 走文件选择器
	 * （那需要 DataTransfer 注入，且 file:// 下行为不一致）。
	 * 导入逻辑本身由 _chk/verify_atlas_import.js 用 IndexedDB/Blob 桩覆盖，
	 * 那条链是纯逻辑、不依赖浏览器实现细节。 */
	var A = window.TE_APP;
	kv('H1 TE_APP探针存在', (!!A) + '');
	kv('H2 探针含importAtlas', (!!(A && A.importAtlas)) + '');
	kv('H3 btnImpSheet存在', !!$('#btnImpSheet'));
	kv('H4 btnImpFeat存在', !!$('#btnImpFeat'));
	kv('H5 btnImpItems存在', !!$('#btnImpItems'));
	kv('H6 btnExtClear存在', !!$('#btnExtClear'));
	kv('H7 extHint存在', !!$('#extHint'));
	kv('H8 extHint文案', $('#extHint') ? $('#extHint').textContent : '');
	var sheets = A ? A.sheets() : [];
	kv('H9 内置图集数', sheets.length);
	kv('H10 全部builtin', sheets.every(function (s) { return s.builtin === true; }) + '');
	kv('H11 初始无外部图集', A ? A.ext().length : -1);
	kv('H12 图集下拉项数', $('#selSheet') ? $('#selSheet').options.length : -1);
	kv('H13 IndexedDB可用', (typeof window.indexedDB === 'object' || typeof window.indexedDB === 'function') + '');

	/* ---------------------------------------------- 输出
	 * ⚠️ 外层 verify_editor_browser.js 是从 **iframe 的 document.title** 取结果的
	 *    （`/<title>(RESULT::[^<]*)<\/title>/`），因为 --dump-dom 只给出顶层文档，
	 *    拿不到 iframe 内部的文本节点。所以这里必须把结果写进 title，
	 *    分隔符是 ' | '，另外再往 body 里塞一份 <pre> 方便人工排查。 */
	var payload = 'RESULT::' + out.join(' | ');
	try { doc.title = payload; } catch (e) { /* 忽略 */ }

	var pre = doc.createElement('pre');
	pre.id = 'TE_PROBE';
	pre.textContent = out.join('\n');
	(doc.body || doc.documentElement).appendChild(pre);
})();
