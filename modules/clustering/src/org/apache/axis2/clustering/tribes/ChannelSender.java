/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.clustering.tribes;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.MessageSender;
import org.apache.catalina.tribes.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ChannelSender implements MessageSender {

    private Log log = LogFactory.getLog(ChannelSender.class);
    private Channel channel;

    public long sendToGroup(ClusteringCommand msg) throws ClusteringFault {
        if (channel == null) {
            return 0;
        }
        long timeToSend = 0;

        // Keep retrying, since at the point of trying to send the msg, a member may leave the group
        // causing a view change. All nodes in a view should get the msg
        //TODO: Sometimes Tribes ncorrectly detects that a member has left a group
        while (true) {
            if (channel.getMembers().length > 0) {
                try {
                    long start = System.currentTimeMillis();
                    channel.send(channel.getMembers(), msg, Channel.SEND_OPTIONS_USE_ACK);
                    timeToSend = System.currentTimeMillis() - start;
                    log.debug("Sent " + msg + " to group");
                    break;
                } catch (ChannelException e) {
                    String message = "Error sending command message : " + msg +
                                     ". Reason " + e.getMessage();
                    log.warn(message);
                }
            } else {
                break;
            }
        }
        return timeToSend;
    }

    public void sendToSelf(ClusteringCommand msg) throws ClusteringFault {
        if (channel == null) {
            return;
        }
        try {
            channel.send(new Member[]{channel.getLocalMember(true)},
                         msg,
                         Channel.SEND_OPTIONS_USE_ACK);
            log.debug("Sent " + msg + " to self");
        } catch (ChannelException e) {
            throw new ClusteringFault(e);
        }
    }

    public long sendToMember(ClusteringCommand cmd, Member member) throws ClusteringFault {
        long timeToSend = 0;
        try {
            if (member.isReady()) {
                long start = System.currentTimeMillis();
                channel.send(new Member[]{member}, cmd, Channel.SEND_OPTIONS_USE_ACK);
                timeToSend = System.currentTimeMillis() - start;
                log.debug("Sent " + cmd + " to " + TribesUtil.getHost(member));
            }
        } catch (ChannelException e) {
            String message = "Could not send message to " + TribesUtil.getHost(member) +
                             ". Reason " + e.getMessage();
            log.warn(message);
        }
        return timeToSend;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
