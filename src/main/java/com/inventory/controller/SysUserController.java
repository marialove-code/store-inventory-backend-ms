package com.inventory.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.Result;
import com.inventory.entity.SysUser;
import com.inventory.entity.SysUserListVO;
import com.inventory.entity.SysUserSimpleVO;
import com.inventory.service.SysUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sysUser")
public class SysUserController {
    @Resource
    private SysUserService sysUserService;

    /**
     * 用户列表接口（默认全量 + 条件搜索 二合一）
     * @param keyword 搜索关键词（模糊匹配用户名/昵称/手机号）
     * @param status 状态筛选（正常/禁用）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页用户列表
     */
    /**
     * 用户列表 + 条件搜索 + 分页 三合一接口
     * 前端进入页面、点击搜索，都调用这一个接口
     */
    @GetMapping("/list")
    public Result<Page<SysUserListVO>> list(
            // 搜索关键词：用户名/昵称/手机号 三选一模糊搜索
            @RequestParam(required = false) String keyword,
            // 状态筛选：1正常 0禁用（可以不传）
            @RequestParam(required = false) Integer status,
            // 页码：默认第1页
            @RequestParam(defaultValue = "1") Long pageNum,
            // 每页条数：默认10条
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        // ====================== 1. 创建分页对象 ======================
        // new Page<>(当前页码, 每页条数)
        // 作用：告诉MyBatis-Plus要查第几页、一页多少条
        Page<SysUser> page = new Page<>(pageNum, pageSize);

        // ====================== 2. 构建查询条件（重点） ======================
        // Wrappers.lambdaQuery()：创建Lambda查询构造器
        // 好处：不用写字符串，直接用实体类的字段，安全、不易出错
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();

        // ====================== 固定必传条件 ======================
        // wrapper.eq(字段名, 值) → 对应SQL：WHERE is_deleted = 0
        // 作用：只查询【未被逻辑删除】的用户（必须加）
        wrapper.eq(SysUser::getIsDeleted, 0);

        // ====================== 动态搜索条件 ======================
        // 判断：如果前端传了搜索关键词keyword，才拼接模糊查询
        if (StrUtil.isNotBlank(keyword)) {
            // wrapper.and(...) → 对应SQL：AND ( ... )
            // 作用：把下面三个OR条件包在一起，避免逻辑混乱
            wrapper.and(w -> w
                    // w.like(字段, 值) → 对应SQL：user_name LIKE '%关键词%'
                    // 用户名 模糊查询
                    .like(SysUser::getUserName, keyword)
                    // or() → 对应SQL：OR
                    .or()
                    // 昵称 模糊查询
                    .like(SysUser::getNickName, keyword)
                    .or()
                    // 手机号 模糊查询
                    .like(SysUser::getPhone, keyword)
            );
        }

        // 判断：如果前端传了状态status，才拼接状态筛选
        if (status != null) {
            // 精确匹配：账号状态 = 传入的status
            wrapper.eq(SysUser::getStatus, status);
        }

        // 排序：按创建时间 倒序（最新的排在最前面）
        // orderByDesc(字段) → 对应SQL：ORDER BY create_time DESC
        wrapper.orderByDesc(SysUser::getCreateTime);

        // ====================== 3. 执行查询 ======================
        // sysUserService.page(分页对象, 查询条件)
        // 作用：MyBatis-Plus自动执行分页SQL，返回带总条数的分页结果
        Page<SysUser> userPage = sysUserService.page(page, wrapper);

        // ====================== 4. 转换成VO（脱敏、隐藏密码） ======================
        // 新建一个和原分页信息一样的VO分页对象（页码、条数、总数不变）
        Page<SysUserListVO> voPage = new Page<>(
                userPage.getCurrent(),   // 当前页码
                userPage.getSize(),      // 每页条数
                userPage.getTotal()      // 总条数
        );

        // 把查询出来的SysUser列表 → 转换成SysUserListVO列表（去掉密码等敏感字段）
        voPage.setRecords(
                userPage.getRecords().stream()
                        // BeanUtil.copyProperties：复制相同名字的字段
                        .map(user -> BeanUtil.copyProperties(user, SysUserListVO.class))
                        // 转成List集合
                        .collect(Collectors.toList())
        );

        // ====================== 5. 返回统一格式结果 ======================
        // Result.success(数据)：按照项目统一格式返回，前端能正常解析
        return Result.success(voPage);
    }

    // ======================================
    // 1. 新增【注册接口】（和 add 类似，单独写一个更规范）
    // 前端请求地址：POST /sysUser/register
    // ======================================
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid SysUser user) {

        // 1. 查询用户名是否已经存在（传统非lambda写法）
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", user.getUserName());
        long count = sysUserService.count(queryWrapper);

        // 2. 传统 if-else 判断，后期超级好扩展
        if (count > 0) {
            // 用户名已存在
            return Result.fail("用户名已存在");
        } else {
            // 用户名不存在 → 执行注册
            // 密码加密
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            user.setPassword(encoder.encode(user.getPassword()));

            // 保存用户
            sysUserService.save(user);

            return Result.success();
        }
    }

    // ======================================
    // 2. 新增【登录接口】
    // 前端请求地址：POST /sysUser/login
    // 接收字段：userName + password
    // ======================================
    @PostMapping("/login")
    public Result<SysUserSimpleVO> login(
            @RequestBody Map<String, String> params,
            HttpSession session
    ) {
        String userName = params.get("userName");
        String password = params.get("password");

        // 1. 非空校验
        if (StrUtil.isBlank(userName) || StrUtil.isBlank(password)) {
            return Result.fail("用户名或密码不能为空");
        }

        // 2. 查询用户（只查未删除、正常状态）
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUserName, userName);
        wrapper.eq(SysUser::getIsDeleted, 0); // 未删除
        wrapper.eq(SysUser::getStatus, 1);    // 正常状态

        SysUser user = sysUserService.getOne(wrapper);

        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        // 3. 密码校验
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }

        // 4. 保存登录态（后端用，前端拿不到）
        session.setAttribute("loginUser", user);

        // 5. 脱敏返回
        SysUserSimpleVO userVO = BeanUtil.copyProperties(user, SysUserSimpleVO.class);

        // ====================== ✅ 核心兜底 ======================
        // 如果 nickName 为空，就把 userName 赋值给 nickName
        if (StrUtil.isBlank(userVO.getNickName())) {
            userVO.setNickName(user.getUserName());
        }

        // 6. 返回
        return Result.success(userVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        // 清除登录态
        session.removeAttribute("loginUser");
        return Result.success();
    }

    /**
     * 修改用户状态（正常/禁用）
     * @param id 用户ID
     * @param status 目标状态（1-正常 0-禁用）
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        // 1. 参数校验
        if (id == null || (status != 0 && status != 1)) {
            return Result.fail("参数错误");
        }

        // 2. 查询用户是否存在
        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 3. 更新状态
        user.setStatus(status);
        sysUserService.updateById(user);

        return Result.success();
    }

    /**
     * 重置用户密码（默认密码：123456）
     * @param id 用户ID
     */
    @PutMapping("/{id}/resetPassword")
    public Result<Void> resetPassword(@PathVariable Long id) {
        // 1. 参数校验
        if (id == null) {
            return Result.fail("参数错误");
        }

        // 2. 查询用户是否存在
        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 3. 加密默认密码并更新
        String defaultPassword = "123456";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(defaultPassword));
        sysUserService.updateById(user);

        return Result.success();
    }

    /**
     * 单个用户逻辑删除
     * @param id 用户ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        // 1. 参数校验
        if (id == null) {
            return Result.fail("参数错误");
        }

        // 2. 查询用户是否存在
        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 3. 逻辑删除（更新 is_deleted=1）
        user.setIsDeleted(1);
        sysUserService.updateById(user);

        return Result.success();
    }

    /**
     * 批量用户逻辑删除
     * @param ids 用户ID列表
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        // 1. 参数校验
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的用户");
        }

        // 2. 批量更新 is_deleted=1
        LambdaUpdateWrapper<SysUser> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.in(SysUser::getId, ids)
                .set(SysUser::getIsDeleted, 1);
        sysUserService.update(updateWrapper);

        return Result.success();
    }

    /**
     * 根据ID查询用户详情（个人信息页用）
     */
    @GetMapping("/user/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        if (id == null) {
            return Result.fail("参数错误");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户信息（个人信息保存）
     * 支持：nickName, sex, age, phone, email, avatar, password
     * 修改后自动刷新登录态
     */
    @PutMapping("/user/{id}")
    public Result<Void> updateUser(
            @PathVariable Long id,
            @RequestBody SysUser user,
            HttpSession session
    ) {
        // 1. 校验用户是否存在
        SysUser exist = sysUserService.getById(id);
        if (exist == null || exist.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 2. 只更新允许的字段
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getId, id);

        if (StrUtil.isNotBlank(user.getNickName())) {
            wrapper.set(SysUser::getNickName, user.getNickName());
        }
        if (user.getSex() != null) {
            wrapper.set(SysUser::getSex, user.getSex());
        }
        if (user.getAge() != null) {
            wrapper.set(SysUser::getAge, user.getAge());
        }
        if (StrUtil.isNotBlank(user.getPhone())) {
            wrapper.set(SysUser::getPhone, user.getPhone());
        }
        if (StrUtil.isNotBlank(user.getEmail())) {
            wrapper.set(SysUser::getEmail, user.getEmail());
        }
        if (StrUtil.isNotBlank(user.getAvatar())) {
            wrapper.set(SysUser::getAvatar, user.getAvatar());
        }

        // 3. 如果传了密码，加密更新
        if (StrUtil.isNotBlank(user.getPassword())) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            wrapper.set(SysUser::getPassword, encoder.encode(user.getPassword()));
        }

        sysUserService.update(wrapper);

        // ✅ 关键：从数据库重新查询最新数据，再更新 Session
        SysUser newInfo = sysUserService.getById(id);
        newInfo.setPassword(null); // 不把密码存进 Session
        session.setAttribute("loginUser", newInfo);

        return Result.success();
    }
}

