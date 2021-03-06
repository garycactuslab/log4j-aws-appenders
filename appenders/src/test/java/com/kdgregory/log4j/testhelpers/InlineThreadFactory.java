// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.log4j.testhelpers;

import java.lang.Thread.UncaughtExceptionHandler;

import com.kdgregory.log4j.aws.internal.shared.LogWriter;
import com.kdgregory.log4j.aws.internal.shared.ThreadFactory;


/**
 *  A {@link ThreadFactory} used for testing: it executes the writer in the current
 *  thread. The writer's run() method should return immediately to avoid deadlock.
 */
public class InlineThreadFactory implements ThreadFactory
{
    @Override
    public void startLoggingThread(LogWriter writer, UncaughtExceptionHandler exceptionHandler)
    {
        writer.run();
    }
}
