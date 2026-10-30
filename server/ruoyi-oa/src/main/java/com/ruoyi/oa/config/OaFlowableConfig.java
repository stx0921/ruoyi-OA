package com.ruoyi.oa.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable 流程引擎配置
 *
 * <p>这里刻意只做最小定制：数据源与事务管理器直接复用若依已有的
 * {@code dynamicDataSource}，用户/部门体系也复用若依，因此关闭 Flowable 自带的 IDM 引擎。
 * 其余能力交给 flowable-spring-boot-starter 的自动装配，降低二次封装成本，便于后续排障。</p>
 *
 * @author ruoyi-oa
 */
@Configuration
public class OaFlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration>
{
    @Override
    public void configure(SpringProcessEngineConfiguration configuration)
    {
        // 用户体系复用若依（sys_user / sys_dept），不再启用 Flowable IDM
        configuration.setDisableIdmEngine(true);
        // 异步执行器默认关闭：无定时边界事件/异步任务时不占线程，后续按需在 yml 打开
        configuration.setAsyncExecutorActivate(false);
        // 流程图字体，避免生成的 PNG 中文/框线错位
        configuration.setActivityFontName("宋体");
        configuration.setLabelFontName("宋体");
        configuration.setAnnotationFontName("宋体");
    }
}
