/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acme.jms.mdb.remote.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.NamingException;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;

import javax.enterprise.concurrent.ManagedExecutorService;

import org.acme.jms.mdb.remote.JMSMessageProperties;
import org.jboss.logging.Logger;

@Singleton
@Startup
public class MessageDriver {
   private static final Logger LOG = Logger.getLogger(MessageDriver.class);
   private final int messageNum = 50;
   @Resource(lookup = "${jca.connection.factory}")
   private QueueConnectionFactory qcf;
   private QueueConnection queueConnection = null;
   private QueueSession queueSession = null;
   @Resource(lookup = "${remote.in.queue.fqn}")
   private Queue inQueue;
   private QueueSender queueSender = null;
   private TextMessage textMessage = null;
   private final String queueName = "jms/queue/inQueue";

   @Resource(lookup = "${external.context}")
   private Context externalCtx;


   private Context ctx;


   @EJB(lookup = "java:global/jboss-as-remote-mdb-ear/jboss-as-remote-mdb-ejb/MessageProcessorBean")
   private MessageProcessorLocal ejb;

   @Resource(name = "DefaultManagedExecutorService")
   private ManagedExecutorService mes;

   @PostConstruct
   public void init(){

      LOG.info("MessageDriver bean created");

   }

   @Schedule(second = "*/10", minute = "*", hour = "*", info = "MyTimer", persistent = false)
   public void sendMessages(){

      if (LOG.isDebugEnabled()){

         LOG.debugf("Submitting sender task.");

      }

      mes.submit(new SendMessageTask());

      if (LOG.isDebugEnabled()){

         LOG.debugf("Sender task completed");

      }

   }

   //@Schedule(second = "*", minute = "*/5", hour = "*", info = "MyTimer", persistent = false)
   private void sendMessage(){
      int msgCnt = 0;

      try {

         queueConnection = qcf.createQueueConnection("quickuser","quick123+");

         queueSession = queueConnection.createQueueSession(true, Session.SESSION_TRANSACTED);

         queueSender = queueSession.createSender(inQueue);



         textMessage = queueSession.createTextMessage("Message from MessageDriver");

         textMessage.setBooleanProperty(JMSMessageProperties.MESSAGE_THROW_EXCEPTION, false);

         textMessage.setLongProperty(JMSMessageProperties.MESSAGE_CONSUMER_DELAY,0);

         queueSender.send(textMessage);

         Thread.sleep(2000);

         textMessage = null;

         msgCnt++;


      } catch (JMSException jmsEception) {

         LOG.errorf(jmsEception,"Cought exception while trying to send a message");

      } catch (InterruptedException interruptedException){

         LOG.warnf(interruptedException,"Shouldn't happen.");

      } finally {

         jmsCleanUp();

      }
   }

   @PreDestroy
   public void cleanUp(){

      LOG.info("MessageDriver bean destroyed");

   }

   private void jmsCleanUp(){

      try {

         if (queueSender != null){

            queueSender.close();
         }

         if (queueSession != null){

            queueSession.close();
         }

         if (queueConnection != null){

            queueConnection.close();
         }

      } catch (JMSException jmsException) {

         LOG.warnf(jmsException,"Error while cleaning up JMS resources.");

      }
   }

   class SendMessageTask implements Runnable{

      public void run(){
         long startTime = 0;
         long finishTime = 0;

         startTime = System.currentTimeMillis();

         for (int i = 0; i < messageNum; i++){

            String message = "Sending message " + i + ".";

            ejb.sendMessage(message);

            if (LOG.isDebugEnabled()){
               LOG.debugf("Message '%s' sent",message);
            }

            message = null;

         }

         finishTime = System.currentTimeMillis();

         LOG.infof("Sent %d messages to %s in %d milliseconds.",messageNum,"inQueue",(finishTime-startTime));
      }

   }
}
