// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.log4j.aws;

import static net.sf.kdgcommons.test.StringAsserts.*;

import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import net.sf.kdgcommons.lang.StringUtil;

import com.kdgregory.log4j.aws.internal.shared.DefaultThreadFactory;
import com.kdgregory.log4j.aws.internal.shared.LogMessage;
import com.kdgregory.log4j.aws.internal.sns.SNSWriterConfig;
import com.kdgregory.log4j.testhelpers.HeaderFooterLayout;
import com.kdgregory.log4j.testhelpers.InlineThreadFactory;
import com.kdgregory.log4j.testhelpers.aws.ThrowingWriterFactory;
import com.kdgregory.log4j.testhelpers.aws.sns.MockSNSWriter;
import com.kdgregory.log4j.testhelpers.aws.sns.MockSNSWriterFactory;
import com.kdgregory.log4j.testhelpers.aws.sns.TestableSNSAppender;


public class TestSNSAppender
{
    private Logger logger;
    private TestableSNSAppender appender;


    private void initialize(String propsName)
    throws Exception
    {
        URL config = ClassLoader.getSystemResource(propsName);
        PropertyConfigurator.configure(config);

        logger = Logger.getLogger(getClass());

        Logger rootLogger = Logger.getRootLogger();
        appender = (TestableSNSAppender)rootLogger.getAppender("default");

        appender.setThreadFactory(new InlineThreadFactory());
        appender.setWriterFactory(new MockSNSWriterFactory());
    }


//----------------------------------------------------------------------------
//  Tests
//----------------------------------------------------------------------------

    @Test
    public void testConfigurationByName() throws Exception
    {
        initialize("TestSNSAppender/testConfigurationByName.properties");

        assertEquals("topicName",     "example",    appender.getTopicName());
        assertEquals("topicArn",      null,         appender.getTopicArn());
        assertEquals("batch delay",   1L,           appender.getBatchDelay());
    }


    @Test
    public void testConfigurationByArn() throws Exception
    {
        initialize("TestSNSAppender/testConfigurationByArn.properties");

        assertEquals("topicName",     null,         appender.getTopicName());
        assertEquals("topicArn",      "example",    appender.getTopicArn());
        assertEquals("batch delay",   1L,           appender.getBatchDelay());
    }


    @Test
    public void testAppend() throws Exception
    {
        initialize("TestSNSAppender/testAppend.properties");
        MockSNSWriterFactory writerFactory = appender.getWriterFactory();

        assertNull("before messages, writer is null",                   appender.getWriter());

        logger.debug("first message");

        MockSNSWriter writer = appender.getWriter();

        assertNotNull("after message 1, writer is initialized",         writer);
        assertEquals("after message 1, calls to writer factory",        1,                  writerFactory.invocationCount);
        assertRegex("topic name",                                       "name-[0-9]{8}",    writer.config.topicName);
        assertRegex("topic ARN",                                        "arn-[0-9]{8}",     writer.config.topicArn);
        assertEquals("last message appended",                           "first message",    writer.lastMessage.getMessage());
        assertEquals("number of messages in writer queue",              1,                  writer.messages.size());
        assertEquals("first message in queue",                          "first message",    writer.messages.get(0).getMessage());

        logger.debug("second message");

        assertEquals("last message appended",                           "second message",   writer.lastMessage.getMessage());
        assertEquals("number of messages in writer queue",              2,                  writer.messages.size());
        assertEquals("first message in queue",                          "first message",    writer.messages.get(0).getMessage());
        assertEquals("second message in queue",                         "second message",   writer.messages.get(1).getMessage());
    }


    @Test(expected=IllegalStateException.class)
    public void testThrowsIfAppenderClosed() throws Exception
    {
        initialize("TestSNSAppender/testAppend.properties");

        // write the first message to initialize the appender
        logger.debug("should not throw");

        appender.close();

        // second message should throw
        logger.error("blah blah blah");
    }


    @Test
    public void testWriteHeaderAndFooter() throws Exception
    {
        initialize("TestSNSAppender/testWriteHeaderAndFooter.properties");

        logger.debug("message");

        // must retrieve writer before we shut down
        MockSNSWriter writer = appender.getWriter();
        LogManager.shutdown();

        assertEquals("number of messages written to log",   3,                          writer.messages.size());
        assertEquals("header is first",                     HeaderFooterLayout.HEADER,  writer.getMessage(0));
        assertEquals("message is second",                   "message",                  writer.getMessage(1));
        assertEquals("footer is last",                      HeaderFooterLayout.FOOTER,  writer.getMessage(2));
    }


    @Test
    public void testMaximumMessageSize() throws Exception
    {
        final int snsMaximumMessageSize     = 262144;       // from http://docs.aws.amazon.com/sns/latest/api/API_Publish.html
        final int layoutOverhead            = 1;            // newline after message

        final String undersizeMessage       = StringUtil.repeat('A', snsMaximumMessageSize - 1 - layoutOverhead);
        final String okMessage              = undersizeMessage + "A";
        final String oversizeMessage        = undersizeMessage + "\u00A1";

        initialize("TestSNSAppender/testAppend.properties");

        logger.debug("this message triggers writer configuration");

        assertFalse("under max size",          appender.isMessageTooLarge(LogMessage.create(undersizeMessage)));
        assertFalse("at max size",             appender.isMessageTooLarge(LogMessage.create(okMessage)));
        assertFalse("over max size",           appender.isMessageTooLarge(LogMessage.create(oversizeMessage)));
    }


    @Test
    public void testUncaughtExceptionHandling() throws Exception
    {
        initialize("TestSNSAppender/testUncaughtExceptionHandling.properties");

        // note that we will be running the writer on a separate thread

        appender.setThreadFactory(new DefaultThreadFactory());
        appender.setWriterFactory(new ThrowingWriterFactory<SNSWriterConfig>());

        logger.debug("this should trigger writer creation");

        assertNull("writer has not yet thrown",         appender.getLastWriterException());

        logger.debug("this should trigger writer throwage");

        // without getting really clever, the best way to wait for the throw to be reported is to sit and spin
        for (int ii = 0 ; (ii < 10) && (appender.getLastWriterException() == null) ; ii++)
        {
            Thread.sleep(10);
        }

        assertNull("writer has been reset",         appender.getWriter());
        assertEquals("last writer exception class", IllegalStateException.class, appender.getLastWriterException().getClass());
    }
}