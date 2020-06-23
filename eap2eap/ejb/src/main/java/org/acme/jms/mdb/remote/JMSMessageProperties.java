package org.acme.jms.mdb.remote;

/**
 * Created by tomr on 07/07/15.
 */
public interface JMSMessageProperties {

    public static final String TOTAL_MESSAGE_COUNT = "TotalMessageCount";
    public static final String UNIQUE_VALUE = "UniqueValue";
    public static final String PRODUCER_NAME = "ProducerName";
    public static final String PRODUCER_HOST = "ProducerHost";
    public static final String MESSAGE_CONSUMER_DELAY = "ConsumerDelay";
    public static final String MESSAGE_THROW_EXCEPTION = "ThrowException";
    public static final String JMSX_GROUP_ID = "JMSXGroupID";
    public static final String JMSX_DELIVERY_COUNT = "JMSXDeliveryCount";

}
