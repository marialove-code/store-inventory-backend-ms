package com.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.SysOperationLog;
import com.inventory.service.SysOperationLogService;
import com.inventory.mapper.SysOperationLogMapper;
import org.springframework.stereotype.Service;

/**
* @author 95349
* @description 针对表【sys_operation_log(系统操作审计日志表)】的数据库操作Service实现
* @createDate 2026-05-08 10:31:31
*/
@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
    implements SysOperationLogService{

}




