package org.acme.jms.mdb.remote.ejb;

import org.acme.jms.mdb.remote.JMSMessageProperties;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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

            if (LOG.isDebugEnabled()){

                LOG.debugf("Sending message %s.",message.toString());

            }


            queueConnection = qcf.createQueueConnection();

            queueSession = queueConnection.createQueueSession(true, Session.SESSION_TRANSACTED);

            textMessage = queueSession.createTextMessage(message);

            textMessage.setBooleanProperty(JMSMessageProperties.MESSAGE_THROW_EXCEPTION, false);

            textMessage.setLongProperty(JMSMessageProperties.MESSAGE_CONSUMER_DELAY,0);

            queueSender = queueSession.createSender(outQueue);

            queueSender.send(textMessage);

            if (LOG.isDebugEnabled()){

                LOG.debugf("Message %s sent.",textMessage.toString());

            }


        } catch (JMSException jmsException) {

            ejbContext.setRollbackOnly();

            LOG.errorf(jmsException,"Caught JMS Exception while processing message %s.",message.toString());

        } finally {

            jmsCleanUp();

        }
    }

    private void jmsCleanUp(){

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

            if (LOG.isDebugEnabled()){

                LOG.debugf("JMS resources closed");

            }

        } catch (JMSException jmsException){

            LOG.warnf(jmsException,"Caught JMS Exception cleaning up JMS resources");
        }
    }

    @PostConstruct
    public void init(){

        LOG.info("Message processor bean created");

    }

    @PreDestroy
    public void cleanUp(){

        LOG.info("Message processor bean destroyed");

    }
}
