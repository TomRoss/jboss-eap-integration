package uk.tomr.integration.ocp.ejb;

import uk.tomr.integration.ocp.utils.JMSMessageProperties;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.jms.*;
import javax.naming.InitialContext;

/**
 * Created by tomr on 25/08/15.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MessageProcessorBean implements MessageProcessorLocal {
    private static final Logger LOG = Logger.getLogger(MessageProcessorBean.class);

    @Resource(lookup = "${external.context}")
    private InitialContext exteranlcontext;

    @Resource(lookup = "${remote.in.queue.fqn}")
    private Queue outQueue;

    @Resource(lookup = "${jca.connection.factory}")
    private QueueConnectionFactory qcf;
    private QueueConnection queueConnection = null;
    private QueueSession queueSession = null;
    private QueueSender queueSender = null;
    @Resource
    private EJBContext ejbContext;
    private TextMessage textMessage = null;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void sendMessage(String message) {

        try {

            LOG.infof("Sending message %s.",message.toString());

            queueConnection = qcf.createQueueConnection();

            queueSession = queueConnection.createQueueSession(true, Session.SESSION_TRANSACTED);

            textMessage = queueSession.createTextMessage(message);

            textMessage.setBooleanProperty(JMSMessageProperties.MESSAGE_THROW_EXCEPTION, false);

            textMessage.setLongProperty(JMSMessageProperties.MESSAGE_CONSUMER_DELAY,0);

            queueSender = queueSession.createSender(outQueue);

            queueSender.send(textMessage);

            LOG.infof("Message %s sent.",textMessage.toString());

        } catch (JMSException jmsException) {

            ejbContext.setRollbackOnly();

            LOG.errorf(jmsException,"Caught JMS Exception while processing message %s.",message.toString());

        } finally {

            cleanUp();

        }
    }

    private void cleanUp(){

        try {
            if (queueSender != null) {

                queueSender.close();
            }

            if (queueSession != null) {

                queueSession.close();
            }

            if (queueConnection != null) {

                queueConnection.close();
            }

        } catch (JMSException jmsException){

            LOG.warnf(jmsException,"Caught JMS Exception cleaning up JMS resources");
        }
    }
}
