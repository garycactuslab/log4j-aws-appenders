// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.log4j.testhelpers;

import java.lang.reflect.Field;

import com.kdgregory.log4j.aws.internal.shared.AbstractLogWriter;
import com.kdgregory.log4j.aws.internal.shared.LogWriter;


/**
 *  Various utility methods to assist with tests.
 */
public class Utils
{
    /**
     *  Uses reflection to extract a field from an AbstractLogWriter subclass.
     */
    public static <T> T getField(LogWriter writer, String fieldName, Class<T> fieldKlass)
    throws RuntimeException
    {
        // note: will only work with the regular CloudWatchLogWriter
        try
        {
            Field field = AbstractLogWriter.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T)field.get(writer);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
