package code;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 重设备份数据
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-05-11 17:47
 **/
@Slf4j
public class LogTest {
    public static void main(String[] args) throws Exception {
        Logger log1 = log;
        log1.info("1");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = loggerContext.getLogger("ROOT");
        RollingFileAppender<ILoggingEvent> fileInfo = (RollingFileAppender) root.getAppender("file_info");
        log1.info(fileInfo.getClass().getName());

        TimeBasedRollingPolicy rollingPolicy = (TimeBasedRollingPolicy) fileInfo.getRollingPolicy();
        TimeBasedRollingPolicy<ILoggingEvent> triggeringPolicy = (TimeBasedRollingPolicy) fileInfo.getTriggeringPolicy();

        /**重设备份数据*/
        rollingPolicy.setMaxHistory(10);
        triggeringPolicy.setMaxHistory(10);
        rollingPolicy.getTimeBasedFileNamingAndTriggeringPolicy().getArchiveRemover().setMaxHistory(10);
        triggeringPolicy.getTimeBasedFileNamingAndTriggeringPolicy().getArchiveRemover().setMaxHistory(10);

        log1.info("{}", rollingPolicy.getMaxHistory());
        log1.info("{}", triggeringPolicy.getMaxHistory());


        log1.info("{}", rollingPolicy.getMaxHistory());
        log1.info("{}", triggeringPolicy.getMaxHistory());

        for (int i = 0; i < 60; i++) {
            log1.info("1");
            Thread.sleep(5000);
        }


        log1.info("{}", rollingPolicy.getMaxHistory());
        log1.info("{}", triggeringPolicy.getMaxHistory());

        for (int i = 0; i < 6; i++) {
            log1.info("1");
            Thread.sleep(30000);
        }

    }
}
