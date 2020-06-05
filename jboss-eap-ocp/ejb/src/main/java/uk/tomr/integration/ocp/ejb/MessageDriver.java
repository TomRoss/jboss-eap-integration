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

package uk.tomr.integration.ocp.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
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

import uk.tomr.integration.ocp.utils.JMSMessageProperties;
import org.jboss.logging.Logger;

@Singleton
@Startup
public class MessageDriver {
   private static final Logger LOG = Logger.getLogger(MessageDriver.class);

   @Resource(lookup = "${jca.connection.factory}")
   private QueueConnectionFactory qcf;
   private QueueConnection queueConnection = null;
   private QueueSession queueSession = null;
   @Resource(lookup = "${remote.in.queue.fqn}")
   private Queue inQueue;
   private QueueSender queueSender = null;
   private TextMessage textMessage = null;
   private final String queueName = "jms/amq/queue/inQueue";

   @Resource(lookup = "${external.context}")
   private Context externalCtx;


   private Context ctx;


   @EJB(lookup = "java:global/jboss-eap-ocp-ear/jboss-eap-ocp-ejb/MessageProcessorBean")
   private MessageProcessorLocal ejb;

   @Resource(name = "DefaultManagedExecutorService")
   private ManagedExecutorService mes;

   @PostConstruct
   public void init(){

      LOG.infof("<<< Starting up message generator >>>");
   }

   @Schedule(second = "*/25", minute = "*", hour = "*", info = "MyTimer", persistent = false)
   public void sendMessages(){

      mes.submit(new SendMessageTask());

   }

   @PreDestroy
   public void cleanUp(){

      LOG.infof("<<< Shutting down message generator >>>");
      mes.shutdownNow();

   }


   class SendMessageTask implements Runnable{

      public void run(){

         for (int i = 0; i < 200; i++){

            String message = "Sending message " + i + ".";

            ejb.sendMessage(message);

            LOG.infof("Message '%s' sent",message);

            message = null;

            //delay(500);

         }
      }

   }

   private void delay(long delay){

      try {
         Thread.sleep(delay);
      } catch (InterruptedException interruptedException) {

         LOG.warnf(interruptedException," This shouldn't happen");
      }
   }
}
