/* FlowOA 原型骨架：统一侧边栏 / 顶栏 / 标签页 */
(function () {
  var ICONS = {
    dashboard: '<path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/>',
    doc: '<path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>',
    list: '<path d="M3 5h18v2H3V5zm0 6h18v2H3v-2zm0 6h12v2H3v-2z"/>',
    flow: '<path d="M4 4h6v4H4V4zm10 0h6v4h-6V4zM4 16h6v4H4v-4zm10 0h6v4h-6v-4zM7 8h2v8H7V8zm8 0h2v8h-2V8z"/>',
    user: '<path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>',
    chart: '<path d="M5 21V9h4v12H5zm5 0V3h4v18h-4zm5 0v-8h4v8h-4z"/>',
    setting: '<path d="M19.14 12.94a7.9 7.9 0 0 0 .06-.94 7.9 7.9 0 0 0-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.61-.22l-2.39.96a7.3 7.3 0 0 0-1.63-.94l-.36-2.54a.5.5 0 0 0-.5-.42h-3.84a.5.5 0 0 0-.5.42l-.36 2.54c-.59.24-1.13.56-1.63.94l-2.39-.96a.5.5 0 0 0-.61.22L2.7 8.8a.5.5 0 0 0 .12.64l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94L2.82 14.5a.5.5 0 0 0-.12.64l1.92 3.32c.14.25.44.35.7.22l2.39-.96c.5.38 1.04.7 1.63.94l.36 2.54c.04.24.25.42.5.42h3.84c.25 0 .46-.18.5-.42l.36-2.54c.59-.24 1.13-.56 1.63-.94l2.39.96c.26.13.56.03.7-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58zM12 15.5A3.5 3.5 0 1 1 12 8.5a3.5 3.5 0 0 1 0 7z"/>',
    search: '<path d="M15.5 14h-.79l-.28-.27a6.5 6.5 0 1 0-.7.7l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0A4.5 4.5 0 1 1 14 9.5 4.5 4.5 0 0 1 9.5 14z"/>',
    bell: '<path d="M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6v-5a6 6 0 0 0-5-5.91V4a1 1 0 1 0-2 0v1.09A6 6 0 0 0 6 11v5l-2 2v1h16v-1l-2-2z"/>',
    full: '<path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>',
    build: '<path d="M22 19h-8.28a2 2 0 1 1 0-2H22v2zM12 13a5 5 0 1 0-5-5 5 5 0 0 0 5 5zm0-2a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/>',
    money: '<path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z"/>',
    check: '<path d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>'
  };

  var MENU = [
    { group: '工作台' },
    { key: 'dashboard', text: '工作台', icon: 'dashboard', href: 'dashboard.html' },
    { group: 'OA 办公' },
    { key: 'leave', text: '请假申请', sub: true, href: 'leave.html' },
    { key: 'todo', text: '待办中心', sub: true, href: 'todo.html', badge: 5 },
    { key: 'trace', text: '流程跟踪', sub: true, href: 'trace.html' },
    { key: 'employee', text: '员工档案', sub: true, href: 'employee.html' },
    { key: 'report', text: '统计看板', sub: true, href: 'report.html' },
    { group: '系统管理' },
    { key: 'sys-user', text: '用户管理', icon: 'user' },
    { key: 'sys-role', text: '角色管理', icon: 'setting' },
    { key: 'sys-menu', text: '菜单管理', icon: 'list' }
  ];

  function icon(name, cls) {
    return '<svg class="' + (cls || '') + '" viewBox="0 0 24 24" style="width:16px;height:16px;fill:currentColor">' + (ICONS[name] || '') + '</svg>';
  }

  function buildSidebar(active) {
    var html = '<div class="logo"><span class="dot"></span>Flow<b>OA</b></div><div class="menu">';
    MENU.forEach(function (m) {
      if (m.group) { html += '<div class="menu-group">' + m.group + '</div>'; return; }
      var cls = m.sub ? 'menu-sub' : 'menu-item';
      if (m.key === active) cls += ' active';
      var inner = (m.sub ? '' : icon(m.icon)) + '<span>' + m.text + '</span>' +
        (m.badge ? '<span class="badge">' + m.badge + '</span>' : '');
      html += '<a class="' + cls + '" href="' + (m.href || 'javascript:void(0)') + '">' + inner + '</a>';
    });
    html += '</div>';
    return html;
  }

  function buildNavbar(crumb) {
    return '' +
      '<div class="navbar-left">' +
        '<span class="hamburger">☰</span>' +
        '<span class="breadcrumb">首页 / <b>' + (crumb || '') + '</b></span>' +
      '</div>' +
      '<div class="navbar-right">' +
        '<svg class="nav-icon" viewBox="0 0 24 24" style="fill:#5a5e66">' + ICONS.search + '</svg>' +
        '<svg class="nav-icon" viewBox="0 0 24 24" style="fill:#5a5e66">' + ICONS.full + '</svg>' +
        '<span style="position:relative">' +
          '<svg class="nav-icon" viewBox="0 0 24 24" style="fill:#5a5e66">' + ICONS.bell + '</svg>' +
          '<i style="position:absolute;top:-2px;right:-2px;width:14px;height:14px;border-radius:50%;background:#F56C6C;color:#fff;font-size:10px;line-height:14px;text-align:center;font-style:normal">3</i>' +
        '</span>' +
        '<span class="avatar">石</span><span class="user-name">石天行</span>' +
      '</div>';
  }

  function buildTags(pageTag) {
    return '<div class="tags"><span class="tag">首页</span><span class="tag active">' + pageTag + '</span></div>';
  }

  window.Shell = {
    mount: function (active, breadcrumb, pageTag) {
      var aside = document.getElementById('sidebar');
      var nav = document.getElementById('navbar');
      var tags = document.getElementById('tags');
      if (aside) aside.outerHTML = '<aside class="sidebar">' + buildSidebar(active) + '</aside>';
      if (nav) nav.innerHTML = buildNavbar(breadcrumb);
      if (tags) tags.innerHTML = buildTags(pageTag || breadcrumb);
    }
  };
})();
