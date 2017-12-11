// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.log4j.aws.internal.cloudwatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.helpers.LogLog;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.LogGroup;
import com.amazonaws.services.logs.model.LogStream;
import com.amazonaws.services.logs.model.OperationAbortedException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.ResourceAlreadyExistsException;
import com.kdgregory.log4j.aws.internal.shared.AbstractLogWriter;
import com.kdgregory.log4j.aws.internal.shared.LogMessage;
import com.kdgregory.log4j.aws.internal.shared.Utils;

public class CloudWatchLogWriter extends AbstractLogWriter {
	private String groupName;
	private String streamName;
	private String accessKey;
	private String secretKey;
	private String region;

	protected AWSLogs client;

	public CloudWatchLogWriter(CloudWatchWriterConfig config) {
		super(config.batchDelay, config.discardThreshold, config.discardAction);
		this.groupName = config.logGroup;
		this.streamName = config.logStream;
		this.accessKey = config.accessKey;
		this.secretKey = config.secretKey;
		this.region = config.region;
	}

	// ----------------------------------------------------------------------------
	// Hooks for superclass
	// ----------------------------------------------------------------------------

	@Override
	protected void createAWSClient() {
		if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
			BasicAWSCredentials cred = new BasicAWSCredentials(accessKey, secretKey);
			client = new AWSLogsClient(cred);
		} else {
			client = new AWSLogsClient();
		}
		
		if (StringUtils.isNotBlank(region)) {
			client.setRegion(Region.getRegion(Regions.fromName(region)));
		}
	}

	@Override
	protected boolean ensureDestinationAvailable() {
		try {
			LogGroup logGroup = findLogGroup();
			if (logGroup == null) {
				LogLog.debug("creating log group: " + groupName);
				createLogGroup();
			}

			LogStream logStream = findLogStream();
			if (logStream == null) {
				createLogStream();
			}

			return true;
		} catch (Exception ex) {
			LogLog.error("unable to configure log group/stream", ex);
			return false;
		}
	}

	@Override
	protected List<LogMessage> processBatch(List<LogMessage> currentBatch) {
		Collections.sort(currentBatch);
		return attemptToSend(currentBatch);
	}

	@Override
	protected int effectiveSize(LogMessage message) {
		return message.size() + CloudWatchConstants.MESSAGE_OVERHEAD;
	}

	@Override
	protected boolean withinServiceLimits(int batchBytes, int numMessages) {
		return (batchBytes < CloudWatchConstants.MAX_BATCH_BYTES) && (numMessages < CloudWatchConstants.MAX_BATCH_COUNT);
	}

	// ----------------------------------------------------------------------------
	// Internals
	// ----------------------------------------------------------------------------

	private List<LogMessage> attemptToSend(List<LogMessage> batch) {
		if (batch.isEmpty())
			return batch;

		PutLogEventsRequest request = new PutLogEventsRequest().withLogGroupName(groupName).withLogStreamName(streamName).withLogEvents(constructLogEvents(batch));

		// sending is all-or-nothing with CloudWatch; we'll return the entire
		// batch
		// if there's an exception

		try {
			LogStream stream = findLogStream();
			request.setSequenceToken(stream.getUploadSequenceToken());
			client.putLogEvents(request);
			return Collections.emptyList();
		} catch (Exception ex) {
			LogLog.error("failed to send batch", ex);
			return batch;
		}
	}

	private List<InputLogEvent> constructLogEvents(List<LogMessage> batch) {
		List<InputLogEvent> result = new ArrayList<InputLogEvent>(batch.size());
		for (LogMessage msg : batch) {
			InputLogEvent event = new InputLogEvent().withTimestamp(msg.getTimestamp()).withMessage(msg.getMessage());
			result.add(event);
		}
		return result;
	}

	private LogGroup findLogGroup() {
		DescribeLogGroupsRequest request = new DescribeLogGroupsRequest().withLogGroupNamePrefix(groupName);
		DescribeLogGroupsResult result = client.describeLogGroups(request);
		for (LogGroup group : result.getLogGroups()) {
			if (group.getLogGroupName().equals(groupName))
				return group;
		}
		return null;
	}

	private void createLogGroup() {
		while (true) {
			try {
				CreateLogGroupRequest request = new CreateLogGroupRequest().withLogGroupName(groupName);
				client.createLogGroup(request);
				for (int ii = 0; ii < 300; ii++) {
					if (findLogGroup() != null)
						return;
					else
						Utils.sleepQuietly(100);
				}
				throw new RuntimeException("unable to create log group after 30 seconds; aborting");
			} catch (ResourceAlreadyExistsException ex) {
				// somebody else created it
				return;
			} catch (OperationAbortedException ex) {
				// someone else is trying to create it, wait and try again
				Utils.sleepQuietly(250);
			}
		}
	}

	private LogStream findLogStream() {
		DescribeLogStreamsRequest request = new DescribeLogStreamsRequest().withLogGroupName(groupName).withLogStreamNamePrefix(streamName);
		DescribeLogStreamsResult result = client.describeLogStreams(request);
		for (LogStream stream : result.getLogStreams()) {
			if (stream.getLogStreamName().equals(streamName))
				return stream;
		}
		return null;
	}

	private void createLogStream() {
		try {
			CreateLogStreamRequest request = new CreateLogStreamRequest().withLogGroupName(groupName).withLogStreamName(streamName);
			client.createLogStream(request);

			for (int ii = 0; ii < 300; ii++) {
				if (findLogStream() != null)
					return;
				else
					Utils.sleepQuietly(100);
			}
			throw new RuntimeException("unable to create log strean after 30 seconds; aborting");
		} catch (ResourceAlreadyExistsException ex) {
			// somebody else created it
			return;
		}
	}
}
