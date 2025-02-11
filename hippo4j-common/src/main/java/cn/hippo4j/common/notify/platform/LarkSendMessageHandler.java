package cn.hippo4j.common.notify.platform;

import cn.hippo4j.common.notify.NotifyConfigDTO;
import cn.hippo4j.common.notify.NotifyPlatformEnum;
import cn.hippo4j.common.notify.SendMessageHandler;
import cn.hippo4j.common.notify.request.AlarmNotifyRequest;
import cn.hippo4j.common.notify.request.ChangeParameterNotifyRequest;
import cn.hippo4j.common.toolkit.StringUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.Collectors;

import static cn.hippo4j.common.notify.platform.LarkAlarmConstants.*;

/**
 * Send lark notification message.
 *
 * @author imyzt
 * @date 2021/11/22 21:12
 */
@Slf4j
@AllArgsConstructor
public class LarkSendMessageHandler implements SendMessageHandler<AlarmNotifyRequest, ChangeParameterNotifyRequest> {

    @Override
    public String getType() {
        return NotifyPlatformEnum.LARK.name();
    }

    @Override
    @SneakyThrows
    public void sendAlarmMessage(NotifyConfigDTO notifyConfig, AlarmNotifyRequest alarmNotifyRequest) {
        String afterReceives = getReceives(alarmNotifyRequest.getReceives());
        String larkAlarmJson = LARK_ALARM_JSON_STR;

        String text = String.format(larkAlarmJson,
                // 环境
                alarmNotifyRequest.getActive(),
                // 线程池ID
                alarmNotifyRequest.getThreadPoolId(),
                // 应用名称
                alarmNotifyRequest.getAppName(),
                // 实例信息
                alarmNotifyRequest.getIdentify(),
                // 报警类型
                alarmNotifyRequest.getNotifyTypeEnum(),
                // 核心线程数
                alarmNotifyRequest.getCorePoolSize(),
                // 最大线程数
                alarmNotifyRequest.getMaximumPoolSize(),
                // 当前线程数
                alarmNotifyRequest.getPoolSize(),
                // 活跃线程数
                alarmNotifyRequest.getActiveCount(),
                // 最大任务数
                alarmNotifyRequest.getLargestPoolSize(),
                // 线程池任务总量
                alarmNotifyRequest.getCompletedTaskCount(),
                // 队列类型名称
                alarmNotifyRequest.getClass().getSimpleName(),
                // 队列容量
                alarmNotifyRequest.getCapacity(),
                // 队列元素个数
                alarmNotifyRequest.getQueueSize(),
                // 队列剩余个数
                alarmNotifyRequest.getRemainingCapacity(),
                // 拒绝策略名称
                alarmNotifyRequest.getRejectedExecutionHandlerName(),
                // 拒绝策略次数
                alarmNotifyRequest.getRejectCountNum(),
                // 告警手机号
                afterReceives,
                // 当前时间
                DateUtil.now(),
                // 报警频率
                alarmNotifyRequest.getInterval()
        );

        execute(notifyConfig.getSecretKey(), text);
    }

    @Override
    @SneakyThrows
    public void sendChangeMessage(NotifyConfigDTO notifyConfig, ChangeParameterNotifyRequest changeParameterNotifyRequest) {
        String threadPoolId = changeParameterNotifyRequest.getThreadPoolId();
        String afterReceives = getReceives(notifyConfig.getReceives());
        String larkNoticeJson = LARK_NOTICE_JSON_STR;

        /**
         * hesitant e.g. ➲  ➜  ⇨  ➪
         */
        String text = String.format(larkNoticeJson,
                // 环境
                changeParameterNotifyRequest.getActive(),
                // 线程池名称
                threadPoolId,
                // 应用名称
                changeParameterNotifyRequest.getAppName(),
                // 实例信息
                changeParameterNotifyRequest.getIdentify(),
                // 核心线程数
                changeParameterNotifyRequest.getBeforeCorePoolSize() + "  ➲  " + changeParameterNotifyRequest.getNowCorePoolSize(),
                // 最大线程数
                changeParameterNotifyRequest.getBeforeMaximumPoolSize() + "  ➲  " + changeParameterNotifyRequest.getNowMaximumPoolSize(),
                // 核心线程超时
                changeParameterNotifyRequest.getBeforeAllowsCoreThreadTimeOut() + "  ➲  " + changeParameterNotifyRequest.getNowAllowsCoreThreadTimeOut(),
                // 线程存活时间
                changeParameterNotifyRequest.getBeforeKeepAliveTime() + "  ➲  " + changeParameterNotifyRequest.getNowKeepAliveTime(),
                // 阻塞队列
                changeParameterNotifyRequest.getBlockingQueueName(),
                // 阻塞队列容量
                changeParameterNotifyRequest.getBeforeQueueCapacity() + "  ➲  " + changeParameterNotifyRequest.getNowQueueCapacity(),
                // 拒绝策略
                changeParameterNotifyRequest.getBeforeRejectedName(),
                changeParameterNotifyRequest.getNowRejectedName(),
                // 告警手机号
                afterReceives,
                // 当前时间
                DateUtil.now()
        );

        execute(notifyConfig.getSecretKey(), text);
    }

    private String getReceives(String receives) {
        if (StringUtil.isBlank(receives)) {
            return "";
        }
        return Arrays.stream(receives.split(","))
                .map(receive -> StrUtil.startWith(receive, LARK_OPENID_PREFIX) ?
                        String.format(LARK_AT_FORMAT_OPENID, receive) : String.format(LARK_AT_FORMAT_USERNAME, receive))
                .collect(Collectors.joining(" "));
    }

    private void execute(String secretKey, String text) {
        String serverUrl = LARK_BOT_URL + secretKey;

        try {
            HttpRequest.post(serverUrl).body(text).execute();
        } catch (Exception ex) {
            log.error("Lark failed to send message", ex);
        }
    }

}
