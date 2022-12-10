package io.github.hidou7.tolog.classic;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class DBHelper {

    public static final short PROPERTIES_EXIST = 0x01;
    public static final short EXCEPTION_EXISTS = 0x02;

    public static short computeReferenceMask(ILoggingEvent event) {
        short mask = 0;

        int mdcPropSize = 0;
        if (event.getMDCPropertyMap() != null) {
            mdcPropSize = event.getMDCPropertyMap().keySet().size();
        }
        int contextPropSize = 0;
        if (event.getLoggerContextVO().getPropertyMap() != null) {
            contextPropSize = event.getLoggerContextVO().getPropertyMap().size();
        }

        if (mdcPropSize > 0 || contextPropSize > 0) {
            mask = PROPERTIES_EXIST;
        }
        if (event.getThrowableProxy() != null) {
            mask |= EXCEPTION_EXISTS;
        }
        return mask;
    }
}
