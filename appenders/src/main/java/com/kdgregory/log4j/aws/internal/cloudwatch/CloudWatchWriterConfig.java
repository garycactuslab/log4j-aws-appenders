// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.log4j.aws.internal.cloudwatch;

import com.kdgregory.log4j.aws.internal.shared.MessageQueue.DiscardAction;

/**
 * Holds configuration that is passed to the writer factory.
 */
public class CloudWatchWriterConfig {
	public String logGroup;
	public String logStream;
	public String accessKey;
	public String secretKey;
	public String region;
	public long batchDelay;
	public int discardThreshold;
	public DiscardAction discardAction;

	public CloudWatchWriterConfig(String actualLogGroup, String actualLogStream, long batchDelay, int discardThreshold, DiscardAction discardAction) {
		this(actualLogGroup, actualLogStream, batchDelay, discardThreshold, discardAction, null, null, null);
	}

	public CloudWatchWriterConfig(String actualLogGroup, String actualLogStream, long batchDelay, int discardThreshold, DiscardAction discardAction, String accessKey, String secretKey, String region) {
		this.logGroup = actualLogGroup;
		this.logStream = actualLogStream;
		this.batchDelay = batchDelay;
		this.discardThreshold = discardThreshold;
		this.discardAction = discardAction;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.region = region;
	}
}
