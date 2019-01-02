package org.hisp.dhis.amqp;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.qpid.jms.JmsQueue;
import org.apache.qpid.jms.JmsTopic;
import org.springframework.util.Assert;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AmqpClient
{
    private final Connection connection;

    public AmqpClient( Connection connection )
    {
        Assert.notNull( connection, "connection is a required dependency of AmqpClient." );
        this.connection = connection;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public Session createSession() throws JMSException
    {
        return createSession( false );
    }

    public Session createSession( boolean transacted ) throws JMSException
    {
        return connection.createSession( transacted, Session.AUTO_ACKNOWLEDGE );
    }

    public Topic createTopic( String topic ) throws JMSException
    {
        Session session = createSession();
        Topic sessionTopic = session.createTopic( topic );
        session.close();

        return sessionTopic;
    }

    public Queue createQueue( String queue ) throws JMSException
    {
        Session session = createSession();
        Queue sessionQueue = session.createQueue( queue );
        session.close();

        return sessionQueue;
    }

    public void sendQueue( String queue, String message )
    {
        send( new JmsQueue( queue ), message );
    }

    public void sendTopic( String topic, String message )
    {
        send( new JmsTopic( topic ), message );
    }

    public void send( Destination destination, String message )
    {
        try
        {
            Session session = createSession();
            MessageProducer producer = session.createProducer( destination );
            TextMessage textMessage = session.createTextMessage( message );
            producer.send( textMessage );
            session.close();
        }
        catch ( JMSException ex )
        {
            ex.printStackTrace();
        }
    }

    public void close()
    {
        if ( connection == null )
        {
            return;
        }

        try
        {
            connection.close();
        }
        catch ( JMSException ex )
        {
            ex.printStackTrace();
        }
    }
}