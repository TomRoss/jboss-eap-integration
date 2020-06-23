/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.acme.jms.mdb.remote;

import org.jboss.ejb3.annotation.ResourceAdapter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;


import org.jboss.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: tomr
 * Date: 14/11/2013
 * Time: 08:47
 * To change this template use File | Settings | File Templates.
 */

@MessageDriven(name = "RemoteQueueMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "${remote.in.queue}"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "${max.session}"),
        @ActivationConfigProperty(propertyName = "useJNDI", propertyValue = "${use.jndi}"),
        @ActivationConfigProperty(propertyName = "hA", propertyValue = "${ha}"),
        @ActivationConfigProperty(propertyName = "rebalanceConnections", propertyValue = "${rebalance.connections}"),
        @ActivationConfigProperty(propertyName = "user", propertyValue = "${user.name}"),
        @ActivationConfigProperty(propertyName = "password", propertyValue = "${user.password}")
}, mappedName = "${remote.in.queue.fqn}")
@ResourceAdapter("${ra.bind.name}")
public class RemoteQueueMDB implements MessageListener {
    private final static Logger LOG = Logger.getLogger(RemoteQueueMDB.class);
    private static AtomicInteger mdbCnt = new AtomicInteger(0);
    private int msgCnt = 0;
    private int mdbID = 0;
    private TextMessage txtMsg = null;

    @Resource(name = "${remote.out.queue.fqn}")
    private Queue outQueue;

    @Resource(name = "${jca.connection.factory}")
    private QueueConnectionFactory qcf;

    @Resource
    private MessageDrivenContext ctx;

    private QueueConnection queueConnection = null;
    private QueueSession queueSession = null;
    private QueueSender queueSender = null;

    private String outQueueName = "outQueue";
    private long delay = 0;
    private boolean throwException = false;

    public RemoteQueueMDB() {

        String className = this.getClass().getName();

        /*if (className.equals("org.acme.jms.mdb.remote.RemoteQueueMDB")){

            mdbID = mdbCnt.getAndIncrement();

        }

        LOG.infof("MDB[%d] MDB class %s created",mdbID,className); */

    }


    /**
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    public void onMessage(Message message) {

        try {

            if (LOG.isDebugEnabled()) {

                LOG.debugf("MDB[%d] Message unique value = %s", mdbID, message.getStringProperty(JMSMessageProperties.UNIQUE_VALUE));

            }

            processMessage(message);

            if (message instanceof TextMessage) {

                txtMsg = (TextMessage) message;

                if (LOG.isDebugEnabled()) {

                    LOG.debugf("MDB[%d] Received Message[%s]: with text '%s'.", mdbID, txtMsg.toString(), txtMsg.getText());

                }

                if (delay != 0) {

                    doDelay(delay);

                }

                if (throwException) {

                    throw new RuntimeException("This is a dummy exception.");

                }

                queueConnection = qcf.createQueueConnection();

                queueSession = queueConnection.createQueueSession(true, Session.SESSION_TRANSACTED);

                if (outQueue == null) {

                    outQueue = queueSession.createQueue(outQueueName);

                    queueSender = queueSession.createSender(outQueue);

                } else {

                    queueSender = queueSession.createSender(outQueue);

                }

                queueSender.send(message);

                msgCnt++;

            } else {

                LOG.warnf("MDB[%d] Message of wrong type: %s", mdbID, message.getClass().getName());
            }

        } catch (JMSException jmsException) {

            ctx.setRollbackOnly();

            LOG.errorf(jmsException, "MDB[%d] Got error while executing onMessage() method.", mdbID);

            throw new RuntimeException(jmsException);

        } finally {

            cleanUp();

        }
    }

    @PreDestroy
    public void printStats() {
        LOG.infof("MDB[%d] Processed %d messages.", mdbID, msgCnt);
        LOG.infof("MDB[%d] Closing.", mdbID);

        if (LOG.isDebugEnabled()) {

            LOG.debugf("MDB[%d] MDB count is ", mdbID, mdbCnt.get());
        }

        mdbCnt.decrementAndGet();
    }

    @PostConstruct
    public void init() {
        LOG.infof("MDB[%d] created.", mdbID);

        mdbID = mdbCnt.getAndIncrement();

    }

    private void doDelay(long delay) {

        try {
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {

            LOG.warnf(interruptedException, "MDB[%d] This should not happen", mdbID);
        }
    }

    private void cleanUp() {
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

            if (LOG.isDebugEnabled()) {

                LOG.debugf("MDB[%d] JMS resources closed.", mdbID);
            }

        } catch (JMSException jmsException) {

            LOG.warnf(jmsException, "MDB[%d ] Caught JMSExeption while cleaning up JMS resources. This can be ignored.", mdbID);

        }
    }

    private void processMessage(Message message) throws JMSException {


        Enumeration<String> props = message.getPropertyNames();

        while (props.hasMoreElements()) {

            String name = props.nextElement();

            if (LOG.isTraceEnabled()) {
                LOG.tracef("MDB[%d] prop=%s ", mdbID, name);
            }

            if (name.equals(JMSMessageProperties.MESSAGE_CONSUMER_DELAY)) {
                delay = message.getLongProperty(JMSMessageProperties.MESSAGE_CONSUMER_DELAY);
            } else if (name.equals(JMSMessageProperties.MESSAGE_THROW_EXCEPTION)) {
                throwException = message.getBooleanProperty(JMSMessageProperties.MESSAGE_THROW_EXCEPTION);
            }

        }


    }

}
