-- 默认超级管理员
INSERT INTO public.sys_user
(id, user_name, "password", nick_name, phone, email, avatar, sex, age, status, sort, remark, is_deleted, create_time, update_time)
VALUES (
    1,
    'super_admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '超级管理员',
    NULL,
    NULL,
    NULL,
    0,
    NULL,
    1,
    0,
    '系统默认超管账号',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
