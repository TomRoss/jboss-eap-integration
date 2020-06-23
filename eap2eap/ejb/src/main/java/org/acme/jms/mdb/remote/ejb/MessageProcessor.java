package org.acme.jms.mdb.remote.ejb;

import javax.jms.Message;

/**
 * Created by tomr on 25/08/15.
 */
public interface MessageProcessor {


    public void sendMessage(String message);

}
