// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.log4j.testhelpers.aws.cloudwatch;

import com.kdgregory.log4j.aws.CloudWatchAppender;
import com.kdgregory.log4j.aws.internal.cloudwatch.CloudWatchWriterConfig;
import com.kdgregory.log4j.aws.internal.shared.LogMessage;
import com.kdgregory.log4j.aws.internal.shared.LogWriter;
import com.kdgregory.log4j.aws.internal.shared.MessageQueue;
import com.kdgregory.log4j.aws.internal.shared.ThreadFactory;
import com.kdgregory.log4j.aws.internal.shared.WriterFactory;
import com.kdgregory.log4j.testhelpers.Utils;


/**
 *  This class provides visibility into the protected variables held by
 *  CloudWatchAppender and AbstractAppender.
 */
public class TestableCloudWatchAppender
extends CloudWatchAppender
{

    public void setThreadFactory(ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
    }


    public void setWriterFactory(WriterFactory<CloudWatchWriterConfig> writerFactory)
    {
        this.writerFactory = writerFactory;
    }


    public WriterFactory<CloudWatchWriterConfig> getWriterFactory()
    {
        return writerFactory;
    }


    public LogWriter getWriter()
    {
        return writer;
    }


    public MockCloudWatchWriter getMockWriter()
    {
        return (MockCloudWatchWriter)writer;
    }


    public MessageQueue getMessageQueue()
    {
        // note: only works with the actual CloudWatchLogWriter
        return Utils.getField(writer, "messageQueue", MessageQueue.class);
    }


    public Throwable getLastWriterException()
    {
        return lastWriterException;
    }

    public void updateLastRotationTimestamp(long offset)
    {
        lastRotationTimestamp += offset;
    }


    public long getLastRotationTimestamp()
    {
        return lastRotationTimestamp;
    }


    @Override
    public boolean isMessageTooLarge(LogMessage message)
    {
        return super.isMessageTooLarge(message);
    }
}
