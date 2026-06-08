INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000011001, 1924700000000001000, '用户管理', 'C', '/system/user', 'system/user', 'system:user:manage', 'user', 1, 1, '2026-05-19 17:07:33.640', '2026-05-19 17:07:33.640', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000011002, 1924700000000001000, '角色管理', 'C', '/system/role', 'system/role', 'system:role:manage', 'peoples', 2, 1, '2026-05-19 17:07:33.640', '2026-05-19 17:07:33.640', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000011003, 1924700000000001000, '权限管理', 'C', '/system/permission', 'system/permission', 'system:permission:manage', 'permission', 3, 1, '2026-05-19 17:07:33.640', '2026-05-19 17:07:33.640', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000011004, 1924700000000001000, '菜单管理', 'C', '/system/menu', 'system/menu', 'system:menu:manage', 'menu', 4, 1, '2026-05-19 17:07:33.640', '2026-05-19 17:07:33.640', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021101, 1924700000000021001, '商品列表-查询', 'F', '/goods/product/list', 'goods/product/list', 'goods:product:list', 'goods', 1, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021102, 1924700000000021001, '商品列表-新增', 'F', '/goods/product/add', 'goods/product/add', 'goods:product:add', 'goods', 2, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021103, 1924700000000021001, '商品列表-编辑', 'F', '/goods/product/edit', 'goods/product/edit', 'goods:product:edit', 'goods', 3, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000192005, 1924700000000011004, '菜单状态修改', 'F', NULL, NULL, 'system:menu:changeStatus', NULL, 5, 1, '2026-05-21 18:30:34.564', '2026-05-21 18:30:34.564', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091009, 1924700000000011001, '批量删除用户', 'F', NULL, NULL, 'system:user:batchDelete', NULL, 5, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000012002, 1924700000000011005, '强制下线', 'F', '', '', 'system:online:forceLogout', '', 2, 1, '2026-05-23 18:26:42.782', '2026-05-23 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000012003, 1924700000000011005, 'Redis 面板查询', 'F', '', '', 'system:online:redis:list', '', 3, 1, '2026-05-23 18:26:42.782', '2026-05-23 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000012004, 1924700000000011005, '删除 Redis Key', 'F', '', '', 'system:online:redis:delete', '', 4, 1, '2026-05-23 18:26:42.782', '2026-05-23 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021104, 1924700000000021001, '商品列表-删除', 'F', '/goods/product/delete', 'goods/product/delete', 'goods:product:delete', 'goods', 4, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021105, 1924700000000021001, '商品列表-批量删除', 'F', '/goods/product/batchDelete', 'goods/product/batchDelete', 'goods:product:batchDelete', 'goods', 5, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021106, 1924700000000021001, '商品列表-上下架', 'F', '/goods/product/changeShelf', 'goods/product/changeShelf', 'goods:product:changeShelf', 'goods', 6, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021107, 1924700000000021001, '商品列表-批量上下架', 'F', '/goods/product/batchShelf', 'goods/product/batchShelf', 'goods:product:batchShelf', 'goods', 7, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021108, 1924700000000021001, '商品列表-导入', 'F', '/goods/product/import', 'goods/product/import', 'goods:product:import', 'goods', 8, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021109, 1924700000000021001, '商品列表-导出', 'F', '/goods/product/export', 'goods/product/export', 'goods:product:export', 'goods', 9, 1, '2026-05-26 20:46:16.678', '2026-05-26 20:46:16.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021201, 1924700000000021002, '商品分类-查询', 'F', '/goods/category/list', 'goods/category/list', 'goods:category:list', 'goods', 1, 1, '2026-05-26 20:46:26.661', '2026-05-26 20:46:26.661', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021202, 1924700000000021002, '商品分类-新增', 'F', '/goods/category/add', 'goods/category/add', 'goods:category:add', 'goods', 2, 1, '2026-05-26 20:46:26.661', '2026-05-26 20:46:26.661', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021203, 1924700000000021002, '商品分类-编辑', 'F', '/goods/category/edit', 'goods/category/edit', 'goods:category:edit', 'goods', 3, 1, '2026-05-26 20:46:26.661', '2026-05-26 20:46:26.661', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021204, 1924700000000021002, '商品分类-删除', 'F', '/goods/category/delete', 'goods/category/delete', 'goods:category:delete', 'goods', 4, 1, '2026-05-26 20:46:26.661', '2026-05-26 20:46:26.661', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021205, 1924700000000021002, '商品分类-批量删除', 'F', '/goods/category/batchDelete', 'goods/category/batchDelete', 'goods:category:batchDelete', 'goods', 5, 1, '2026-05-26 20:46:26.661', '2026-05-26 20:46:26.661', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021206, 1924700000000021002, '商品分类-状态修改', 'F', '/goods/category/changeStatus', 'goods/category/changeStatus', 'goods:category:changeStatus', 'goods', 6, 1, '2026-05-26 20:46:26.661', '2026-05-26 20:46:26.661', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021207, 1924700000000021002, '商品分类-批量状态', 'F', '/goods/category/batchStatus', 'goods/category/batchStatus', 'goods:category:batchStatus', 'goods', 7, 1, '2026-05-26 20:46:26.661', '2026-05-26 20:46:26.661', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021302, 1924700000000021003, '商品品牌-新增', 'F', '/goods/brand/add', 'goods/brand/add', 'goods:brand:add', 'goods', 2, 1, '2026-05-26 20:46:34.717', '2026-05-26 20:46:34.717', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021303, 1924700000000021003, '商品品牌-编辑', 'F', '/goods/brand/edit', 'goods/brand/edit', 'goods:brand:edit', 'goods', 3, 1, '2026-05-26 20:46:34.717', '2026-05-26 20:46:34.717', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021002, 1924700000000002000, '商品分类', 'C', '/goods/category', 'goods/category', 'goods:category:manage', 'category', 2, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021001, 1924700000000002000, '商品列表', 'C', '/goods/list', 'goods/list', 'goods:list:manage', 'shopping', 1, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021003, 1924700000000002000, '商品品牌', 'C', '/goods/brand', 'goods/brand', 'goods:brand:manage', 'brand', 3, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021301, 1924700000000021003, '商品品牌-查询', 'F', '/goods/brand/list', 'goods/brand/list', 'goods:brand:list', 'goods', 1, 1, '2026-05-26 20:46:34.717', '2026-05-26 20:46:34.717', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000012001, 1924700000000011005, '在线用户查询', 'F', '', '', 'system:online:list', '', 1, 1, '2026-05-23 18:26:42.782', '2026-05-23 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000010008, 1924700000000011002, '角色查询', 'F', '', '', 'system:role:list', '', 1, 1, '2026-05-20 18:26:42.782', '2026-05-20 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000002000, 1924700000000000000, '商品管理', 'M', NULL, NULL, 'product:manage', 'goods', 4, 1, '2026-05-19 17:07:24.176', '2026-05-19 17:07:24.176', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000005000, 1924700000000000000, '系统监控', 'M', NULL, NULL, 'monitor:manage', 'monitor', 7, 1, '2026-05-19 17:07:24.176', '2026-05-19 17:07:24.176', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000003000, 1924700000000000000, '库存管理', 'M', NULL, NULL, 'stock:manage', 'stock', 5, 1, '2026-05-19 17:07:24.176', '2026-05-19 17:07:24.176', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000001000, 1924700000000000000, '系统管理', 'M', NULL, NULL, 'system', 'system', 3, 1, '2026-05-19 17:07:24.176', '2026-05-19 17:07:24.176', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000000000, 0, '所有权限', 'M', NULL, NULL, '*:*:*', 'UnorderedListOutlined', 0, 1, '2026-05-19 17:07:14.678', '2026-05-19 17:07:14.678', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000011005, 1924700000000001000, '在线用户', 'C', '/system/online', 'system/online', 'system:online:list', 'online', 5, 1, '2026-05-19 17:07:33.640', '2026-05-19 17:07:33.640', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000011006, 1924700000000001000, '登录日志', 'C', '/system/loginlog', 'system/loginlog', 'system:loginlog:list', 'log', 6, 1, '2026-05-19 17:07:33.640', '2026-05-19 17:07:33.640', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000011007, 1924700000000001000, '操作日志', 'C', '/system/operlog', 'system/operlog', 'system:operlog:list', 'operlog', 7, 1, '2026-05-19 17:07:33.640', '2026-05-19 17:07:33.640', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000004000, 1924700000000000000, '订单管理', 'M', NULL, NULL, 'order:manage', 'order', 6, 1, '2026-05-19 17:07:24.176', '2026-05-19 17:07:24.176', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091004, 1924700000000011001, '删除用户', 'F', NULL, NULL, 'system:user:delete', NULL, 4, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091005, 1924700000000011001, '重置密码', 'F', NULL, NULL, 'system:user:resetPwd', NULL, 5, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091006, 1924700000000011001, '修改状态', 'F', NULL, NULL, 'system:user:changeStatus', NULL, 6, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000051001, 1924700000000005000, 'Redis监控', 'C', '/monitor/redis', 'monitor/redis', 'monitor:redis:view', 'redis', 1, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000051002, 1924700000000005000, '服务监控', 'C', '/monitor/server', 'monitor/server', 'monitor:server:view', 'server', 2, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000051003, 1924700000000005000, '接口监控', 'C', '/monitor/api', 'monitor/api', 'monitor:api:view', 'api', 3, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000095001, 1924700000000051001, '查看Redis监控', 'F', NULL, NULL, 'monitor:redis:view', NULL, 1, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000095002, 1924700000000051002, '查看服务监控', 'F', NULL, NULL, 'monitor:server:view', NULL, 1, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091012, 1924700000000011003, '权限新增', 'F', NULL, NULL, 'system:permission:add', NULL, 2, 1, '2026-05-21 17:16:40.524', '2026-05-21 17:16:40.524', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091013, 1924700000000011003, '权限编辑', 'F', NULL, NULL, 'system:permission:edit', NULL, 3, 1, '2026-05-21 17:16:40.524', '2026-05-21 17:16:40.524', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091014, 1924700000000011003, '权限删除', 'F', NULL, NULL, 'system:permission:delete', NULL, 4, 1, '2026-05-21 17:16:40.524', '2026-05-21 17:16:40.524', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091016, 1924700000000011003, '权限状态修改', 'F', NULL, NULL, 'system:permission:changeStatus', NULL, 6, 1, '2026-05-21 17:16:40.524', '2026-05-21 17:16:40.524', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000192002, 1924700000000011004, '菜单新增', 'F', NULL, NULL, 'system:menu:add', NULL, 2, 1, '2026-05-21 18:30:34.564', '2026-05-21 18:30:34.564', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000192003, 1924700000000011004, '菜单编辑', 'F', NULL, NULL, 'system:menu:edit', NULL, 3, 1, '2026-05-21 18:30:34.564', '2026-05-21 18:30:34.564', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000192004, 1924700000000011004, '菜单删除', 'F', NULL, NULL, 'system:menu:delete', NULL, 4, 1, '2026-05-21 18:30:34.564', '2026-05-21 18:30:34.564', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091001, 1924700000000011001, '用户查询', 'F', NULL, NULL, 'system:user:list', NULL, 1, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000192001, 1924700000000011004, '菜单查询', 'F', NULL, NULL, 'system:menu:list', NULL, 1, 1, '2026-05-21 18:30:34.564', '2026-05-21 18:30:34.564', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041001, 1924700000000004000, '订单列表', 'C', '/order/list', 'order/list', 'order:list:manage', 'order', 1, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000010002, 1924700000000011002, '新增角色', 'F', NULL, NULL, 'system:role:add', NULL, 2, 1, '2026-05-20 18:26:42.782', '2026-05-20 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000010003, 1924700000000011002, '编辑角色', 'F', NULL, NULL, 'system:role:edit', NULL, 3, 1, '2026-05-20 18:26:42.782', '2026-05-20 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000010004, 1924700000000011002, '删除角色', 'F', NULL, NULL, 'system:role:delete', NULL, 4, 1, '2026-05-20 18:26:42.782', '2026-05-20 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000010005, 1924700000000011002, '分配权限', 'F', NULL, NULL, 'system:role:assign', NULL, 5, 1, '2026-05-20 18:26:42.782', '2026-05-20 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000010006, 1924700000000011002, '批量删除角色', 'F', NULL, NULL, 'system:role:batchDelete', NULL, 6, 1, '2026-05-20 18:26:42.782', '2026-05-20 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2061777668529971202, 1924700000000000000, '门店销售', 'M', '/shop', 'Layout', 'shop:manage', NULL, 11, 1, '2026-06-02 19:51:16.161', '2026-06-02 19:51:16.161', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091003, 1924700000000011001, '修改用户', 'F', NULL, NULL, 'system:user:edit', NULL, 3, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000095003, 1924700000000051003, '查看接口监控', 'F', NULL, NULL, 'monitor:api:list', NULL, 1, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041003, 1924700000000004000, '退款管理', 'C', '/order/refund', 'order/refund', 'order:refund:manage', 'refund', 3, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924900000000010007, 1924700000000011002, '修改角色状态', 'F', NULL, NULL, 'system:role:changeStatus', NULL, 7, 1, '2026-05-20 18:26:42.782', '2026-05-20 18:26:42.782', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1925184200000000001, 1924700000000011001, '分配权限', 'F', NULL, NULL, 'system:user:assign', NULL, 7, 1, '2026-05-22 14:17:28.000', '2026-05-22 14:17:28.000', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000031001, 1924700000000003000, '库存列表', 'C', '/stock/list', 'stock/list', 'inventory:stock:list', 'inventory', 1, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000091011, 1924700000000011003, '权限查询', 'F', NULL, NULL, 'system:permission:list', NULL, 1, 1, '2026-05-21 17:16:40.524', '2026-05-21 17:16:40.524', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021304, 1924700000000021003, '商品品牌-删除', 'F', '/goods/brand/delete', 'goods/brand/delete', 'goods:brand:delete', 'goods', 4, 1, '2026-05-26 20:46:34.717', '2026-05-26 20:46:34.717', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021305, 1924700000000021003, '商品品牌-批量删除', 'F', '/goods/brand/batchDelete', 'goods/brand/batchDelete', 'goods:brand:batchDelete', 'goods', 5, 1, '2026-05-26 20:46:34.717', '2026-05-26 20:46:34.717', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000021306, 1924700000000021003, '商品品牌-状态修改', 'F', '/goods/brand/changeStatus', 'goods/brand/changeStatus', 'goods:brand:changeStatus', 'goods', 6, 1, '2026-05-26 20:46:34.717', '2026-05-26 20:46:34.717', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000060001, 1924700000000060000, '个人主页', 'C', '/profile/info', 'profile/info', 'profile:info', NULL, 1, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000060002, 1924700000000060000, '我的日志', 'C', '/profile/pwd', 'profile/pwd', 'profile:pwd', NULL, 2, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000060101, 1924700000000060100, 'AI SQL助手', 'C', '/ai/sql', 'ai/sql', 'ai:sql', NULL, 1, 1, '2026-05-27 20:36:07.059', '2026-05-27 20:36:07.059', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000060102, 1924700000000060100, 'AI运维助手', 'C', '/ai/ops', 'ai/ops', 'ai:ops', NULL, 2, 1, '2026-05-27 20:36:07.059', '2026-05-27 20:36:07.059', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000060100, 1924700000000000000, 'AI工具', 'M', '/ai', 'Layout', 'ai:index', 'ai', 10, 1, '2026-05-27 20:36:07.059', '2026-05-27 20:36:07.059', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041002, 1924700000000004000, '发货管理', 'C', '/order/delivery', 'order/delivery', 'order:deliver:manage', 'send', 2, 1, '2026-05-19 17:37:03.919', '2026-05-19 17:37:03.919', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2062103055994597377, 1924700000000011006, '日志列表', 'F', '/system/login/log/list', 'system:loginlog:list', 'system:loginlog:list', NULL, 0, 1, '2026-06-03 17:24:14.038', '2026-06-03 17:24:14.038', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2062103255836405761, 1924700000000011006, '导出', 'F', '/system/login/log/export', NULL, 'system:loginlog:export', NULL, 0, 1, '2026-06-03 17:25:01.684', '2026-06-03 17:25:01.684', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2062103456617738241, 1924700000000011006, '清空日志', 'F', '/system/login/log/clear', NULL, 'system:loginlog:clear', NULL, 0, 1, '2026-06-03 17:25:49.558', '2026-06-03 17:25:49.558', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000031002, 1924700000000003000, '入库管理', 'C', '/stock/in', 'stock/in', 'inventory:stockin:list', 'in', 2, 1, '2026-05-19 17:08:16.321', '2026-06-07 12:34:34.959', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000100000, 1924700000000000000, '首页', 'C', '/index', 'index', 'index:view', 'home', 1, 1, '2026-05-19 17:08:16.321', '2026-05-21 19:20:04.967', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000060000, 1924700000000000000, '个人中心', 'C', '/profile', 'profile', 'profile:view', 'user', 8, 1, '2026-05-19 17:08:16.321', '2026-05-19 17:08:16.321', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2061778081186570242, 1924700000000000000, '总览', 'C', '/shop/dashboard', '/shop/dashboard', 'shop:view', NULL, 0, 1, '2026-06-02 19:52:54.541', '2026-06-02 19:52:54.541', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2061777994368671745, 2061777668529971202, '售卖信息', 'C', '/shop/records', '/shop/records', 'shop:sale:list', NULL, 2, 1, '2026-06-02 19:52:33.851', '2026-06-02 19:52:33.851', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2062117964954431490, 1924700000000011007, '日志列表', 'F', '/system/operate/log/list', '/system/operate/log/list', 'system:operlog:list', NULL, 0, 1, '2026-06-03 18:23:28.583', '2026-06-03 18:23:28.583', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2062118164951429121, 1924700000000011007, '导出', 'F', '/system/operate/log/export', '/system/operate/log/export', 'system:operlog:export', NULL, 0, 1, '2026-06-03 18:24:16.266', '2026-06-03 18:24:16.266', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2061777915310235650, 2061777668529971202, '配件信息', 'C', '/shop/products', 'shop/Products', 'shop:product:list', NULL, 1, 1, '2026-06-02 19:52:14.995', '2026-06-02 19:52:14.995', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2063477861872787457, 1924700000000031001, '商品列表-查询', 'F', '/inventory/stock/list', '/inventory/stock/list', 'inventory:stock:list', NULL, 1, 1, '2026-06-07 12:27:15.181', '2026-06-07 12:27:15.181', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(2063478244963737601, 1924700000000031001, '库存列表-编辑', 'F', '/inventory/stock/edit', '/inventory/stock/edit', 'inventory:stock:edit', NULL, 2, 1, '2026-06-07 12:28:46.535', '2026-06-07 12:28:46.535', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000031003, 1924700000000003000, '出库管理', 'C', '/stock/out', 'stock/out', 'inventory:stockout:list', 'out', 3, 1, '2026-05-19 17:08:16.321', '2026-06-07 12:34:34.959', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000031004, 1924700000000003000, '库存预警', 'C', '/stock/warn', 'stock/warn', 'inventory:warn:list', 'warn', 4, 1, '2026-05-19 17:08:16.321', '2026-06-07 12:34:34.959', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000031005, 1924700000000003000, '库存流水', 'C', '/stock/flow', 'stock/flow', 'inventory:flow:list', 'log', 5, 1, '2026-05-19 17:08:16.321', '2026-06-07 12:34:34.959', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924500000000091001, 1924700000000031002, '新增入库', 'F', '', '', 'inventory:stockin:add', '', 1, 1, '2026-06-07 12:40:16.827', '2026-06-07 12:40:16.827', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924500000000091002, 1924700000000031003, '新增出库', 'F', '', '', 'inventory:stockout:add', '', 1, 1, '2026-06-07 12:40:16.827', '2026-06-07 12:40:16.827', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924500000000091003, 1924700000000031004, '调整阈值', 'F', '', '', 'inventory:warn:edit', '', 1, 1, '2026-06-07 12:40:16.827', '2026-06-07 12:40:16.827', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924500000000091004, 1924700000000031004, '补货入库', 'F', '', '', 'inventory:warn:receipt', '', 2, 1, '2026-06-07 12:40:16.827', '2026-06-07 12:40:16.827', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924500000000091005, 1924700000000031005, '导出流水', 'F', '', '', 'inventory:flow:export', '', 1, 1, '2026-06-07 12:40:16.827', '2026-06-07 12:40:16.827', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1923900000000031001, 1924700000000031002, '入库管理-查询', 'F', '', '', 'inventory:stockin:list', '', 0, 1, '2026-06-07 12:43:25.664', '2026-06-07 12:43:25.664', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1923900000000031002, 1924700000000031003, '出库管理-查询', 'F', '', '', 'inventory:stockout:list', '', 0, 1, '2026-06-07 12:43:25.664', '2026-06-07 12:43:25.664', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1923900000000031003, 1924700000000031004, '库存预警-查询', 'F', '', '', 'inventory:warn:list', '', 0, 1, '2026-06-07 12:43:25.664', '2026-06-07 12:43:25.664', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1923900000000031004, 1924700000000031005, '库存流水-查询', 'F', '', '', 'inventory:flow:list', '', 0, 1, '2026-06-07 12:43:25.664', '2026-06-07 12:43:25.664', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041101, 1924700000000041001, '查询', 'F', '', '', 'order:info:list', '', 0, 1, '2026-06-07 12:59:22.860', '2026-06-07 12:59:22.860', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041102, 1924700000000041001, '新增订单', 'F', '', '', 'order:info:add', '', 1, 1, '2026-06-07 12:59:22.860', '2026-06-07 12:59:22.860', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041103, 1924700000000041001, '支付', 'F', '', '', 'order:info:pay', '', 2, 1, '2026-06-07 12:59:22.860', '2026-06-07 12:59:22.860', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041104, 1924700000000041001, '取消订单', 'F', '', '', 'order:info:cancel', '', 3, 1, '2026-06-07 12:59:22.860', '2026-06-07 12:59:22.860', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041105, 1924700000000041001, '确认收货', 'F', '', '', 'order:info:receive', '', 4, 1, '2026-06-07 12:59:22.860', '2026-06-07 12:59:22.860', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041106, 1924700000000041001, '申请退款', 'F', '', '', 'order:refund:apply', '', 5, 1, '2026-06-07 12:59:22.860', '2026-06-07 12:59:22.860', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041201, 1924700000000041002, '查询', 'F', '', '', 'order:delivery:list', '', 0, 1, '2026-06-07 12:59:33.028', '2026-06-07 12:59:33.028', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041202, 1924700000000041002, '发货', 'F', '', '', 'order:delivery:delivery', '', 1, 1, '2026-06-07 12:59:33.028', '2026-06-07 12:59:33.028', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041301, 1924700000000041003, '查询', 'F', '', '', 'order:refund:list', '', 0, 1, '2026-06-07 12:59:43.809', '2026-06-07 12:59:43.809', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041302, 1924700000000041003, '审核通过', 'F', '', '', 'order:refund:approve', '', 1, 1, '2026-06-07 12:59:43.809', '2026-06-07 12:59:43.809', 0);
INSERT INTO public.sys_permission
(id, parent_id, perm_name, perm_type, "path", component, perm_code, icon, sort, status, create_time, update_time, is_deleted)
VALUES(1924700000000041303, 1924700000000041003, '审核拒绝', 'F', '', '', 'order:refund:reject', '', 2, 1, '2026-06-07 12:59:43.809', '2026-06-07 12:59:43.809', 0);