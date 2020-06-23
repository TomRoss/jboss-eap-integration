**Very simple example of MDB listening on a remote destination hosted in master/slave setup.**

**Description**
 - This is a simple example of a MDB that listens on a remote destiation hosted by a remote master slave setup. The project uses Artemis Resource Adadapter to connect to remote queues and exteranl JNDI context to lookup all requred resoruce in remote EAP instances.

**Requirements**

 - Three JBoss EAP 7.1.1 intances or later

**Steps to deploy the example**

- create MDB hosting instance

  - `cp $JBOSS_HOME`
  - `cp -rp standalone standalone-mdb-host`
  - `cp ${project root directory}/ejb/src/main/resources/META-INF/configs/eap/mdb-host/standalone-full-ha.xml standalone-mdb-host/configuration`
  - `cp ${project root directory}/ejb/src/main/resources/META-INF/configs/eap/mdb-host/mdb.properties $JBOSS_HOME/bin`
  - start the instance
    - `$JBOSS_HOME/standalone.sh -Djboss.server.base.dir=$JBOSS\_HOME/standalone-mdb-host --server-config=standalone-full-ha.xml --properties=mdb.properties`

- create remote destination hosting instances, master and salve
  - `cd $JBOSS_HOME`
  - `cp -rp standalone standalone-master`
  - `cp -rp standalone standalone-slave`
  - `cp ${project root directory}/ejb/src/main/resources/META-INF/configs/eap/master/standalone-full-ha.xml standalone-master/configuration`
  - `cp ${project root directory}/ejb/src/main/resources/META-INF/configs/eap/slave/standalone-full-ha.xml standalone-slave/configuration`
  - start master instance
    - `./standalone.sh -Djboss.server.base.dir=$JBOSS_HOME/standalone-master --server-config=standalone-full-ha.xml -Djboss.socket.binding.port-offset=100`
  - start slave instace
    - `./standalone.sh -Djboss.server.base.dir=$JBOSS_HOME/standalone-slave --server-config=standalone-full-ha.xml -Djboss.socket.binding.port-offset=200`
- build and deploy the proejct
  - `cd ${project root directory}`
  - `mvn clean package wildfly:deploy`


- in the `$JBOSS_HOME/standalone-mdb-host/log` there should be an app.log. If the message is consumed the there should be a message:

`
 2017-10-30 09:38:27,181 INFO  org.jboss.as.jms.mdb.remote.RemoteQueueMDB: MDB[30] Received Message[ActiveMQMessage[ID:12d0a4de-bd56-11e7-8268-4fd4e7a9069b]:PERSISTENT/ClientMessageImpl[messageID=2147485611, durable=true, address=jms.queue.inQueue,userID=12d0a4de-bd56-11e7-8268-4fd4e7a9069b,properties=TypedProperties[ProducerHost=pagz,__AMQ_CID=12b8d71b-bd56-11e7-8268-4fd4e7a9069b,ThrowException=false,TotalMessageCount=1,ProducerName=pool-1-thread-1,UniqueValue=1509356303859,ConsumerDelay=0]]]: with text 'This is text message '1' out of '1'. Sent from host 'pagz'.'.
`

TODO

* add EAP configuriton description and jboss-cli scripts to create all the configuration components