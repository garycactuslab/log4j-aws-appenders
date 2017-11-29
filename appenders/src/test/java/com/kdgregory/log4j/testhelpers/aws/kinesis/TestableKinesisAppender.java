// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.log4j.testhelpers.aws.kinesis;

import com.kdgregory.log4j.aws.KinesisAppender;
import com.kdgregory.log4j.aws.internal.kinesis.KinesisWriterConfig;
import com.kdgregory.log4j.aws.internal.shared.LogWriter;
import com.kdgregory.log4j.aws.internal.shared.MessageQueue;
import com.kdgregory.log4j.aws.internal.shared.ThreadFactory;
import com.kdgregory.log4j.aws.internal.shared.WriterFactory;
import com.kdgregory.log4j.testhelpers.Utils;


/**
 *  This class provides visibility into the protected variables held by
 *  KinesisAppender and AbstractAppender.
 */
public class TestableKinesisAppender
extends KinesisAppender
{

    public void setThreadFactory(ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
    }


    public void setWriterFactory(WriterFactory<KinesisWriterConfig> writerFactory)
    {
        this.writerFactory = writerFactory;
    }


    public WriterFactory<KinesisWriterConfig> getWriterFactory()
    {
        return writerFactory;
    }


    public LogWriter getWriter()
    {
        return writer;
    }


    public MockKinesisWriter getMockWriter()
    {
        return (MockKinesisWriter)writer;
    }


    public MessageQueue getMessageQueue()
    {
        // note: only works with the actual KinesisLogWriter
        return Utils.getField(writer, "messageQueue", MessageQueue.class);
    }


    public Throwable getLastWriterException()
    {
        return lastWriterException;
    }
}
