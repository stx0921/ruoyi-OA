package com.ruoyi.oa.flow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.ruoyi.oa.constant.OaConstants;

/**
 * 流程定义部署器
 *
 * <p>为什么不用 Flowable 的自动扫描（{@code flowable.check-process-definitions}）：
 * 那套机制靠 {@code classpath*:/processes/**.bpmn20.xml} 通配匹配，在 Spring Boot
 * fat jar 里无法列出嵌套 jar（ruoyi-oa-3.9.2.jar）内的目录，启动日志会提示
 * "No deployment resources were found for autodeployment"。</p>
 *
 * <p>改为显式按精确 classpath 路径部署，配合 {@code enableDuplicateFiltering()}：
 * 资源内容没变则跳过，改了才生成新版本，运行中的老流程实例仍走旧版本。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class OaProcessDeployer implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(OaProcessDeployer.class);

    /** 流程定义文件在 classpath 下的路径 */
    private static final String LEAVE_BPMN = "processes/leave_approval.bpmn20.xml";

    @Autowired
    private RepositoryService repositoryService;

    @Override
    public void run(ApplicationArguments args)
    {
        deployLeaveProcess();
    }

    private void deployLeaveProcess()
    {
        try
        {
            Deployment deployment = repositoryService.createDeployment()
                    .name("请假审批流程")
                    .category("OA")
                    .addClasspathResource(LEAVE_BPMN)
                    .enableDuplicateFiltering()
                    .deploy();
            long count = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(OaConstants.PROCESS_KEY_LEAVE).count();
            log.info("请假审批流程部署完成，deploymentId={}，当前定义版本数={}",
                    deployment == null ? "skipped(unchanged)" : deployment.getId(), count);
        }
        catch (Exception e)
        {
            // 部署失败不应阻断整个应用启动，但必须让人看见
            log.error("请假审批流程部署失败，BPMN 可能存在语法或表达式错误：{}", e.getMessage(), e);
        }
    }
}
