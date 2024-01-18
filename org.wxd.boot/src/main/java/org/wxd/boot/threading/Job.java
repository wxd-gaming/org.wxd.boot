package org.wxd.boot.threading;

import org.wxd.boot.assist.IAssistMonitor;

/** 取消 */
public interface Job extends IAssistMonitor {

    boolean cancel();

}
