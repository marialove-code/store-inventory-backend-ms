package com.inventory.modules.system.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.modules.system.log.entity.SysOperationLog;
import com.inventory.modules.system.log.service.SysOperationLogService;
import com.inventory.modules.system.log.mapper.SysOperationLogMapper;
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




