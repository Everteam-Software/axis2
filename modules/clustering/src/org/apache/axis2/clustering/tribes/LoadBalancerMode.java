/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */
package org.apache.axis2.clustering.tribes;

import org.apache.axis2.clustering.LoadBalanceEventHandler;
import org.apache.axis2.clustering.control.wka.MemberJoinedCommand;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.RemoteProcessException;
import org.apache.catalina.tribes.group.RpcChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a member running in load balance mode
 */
public class LoadBalancerMode implements OperationMode {

    private static final Log log = LogFactory.getLog(LoadBalancerMode.class);

    private byte[] loadBalancerDomain;
    private Map<String, LoadBalanceEventHandler> lbEventHandlers;
    private List<MembershipManager> membershipManagers = new ArrayList<MembershipManager>();
    private MembershipManager primaryMembershipManager;

    public LoadBalancerMode(byte[] loadBalancerDomain,
                            Map<String, LoadBalanceEventHandler> lbEventHandlers,
                            MembershipManager primaryMembershipManager) {
        this.loadBalancerDomain = loadBalancerDomain;
        this.lbEventHandlers = lbEventHandlers;
        this.primaryMembershipManager = primaryMembershipManager;
    }

    public void addInterceptors(Channel channel) {
        LoadBalancerInterceptor lbInterceptor =
                new LoadBalancerInterceptor(loadBalancerDomain);
        lbInterceptor.setOptionFlag(TribesConstants.MEMBERSHIP_MSG_OPTION);
        channel.addInterceptor(lbInterceptor);
        if (log.isDebugEnabled()) {
            log.debug("Added Load Balancer Interceptor");
        }
    }

    public void init(Channel channel) {
        // Have multiple RPC channels with multiple RPC request handlers for each domain
        // This is needed only when this member is running as a load balancer
        for (Object o : lbEventHandlers.keySet()) {
            String domain = (String) o;
            final MembershipManager membershipManager = new MembershipManager();
            membershipManager.setDomain(domain.getBytes());
            membershipManager.setLoadBalanceEventHandler(lbEventHandlers.get(domain));

            MembershipListener membershipListener = new MembershipListener() {
                public void memberAdded(org.apache.catalina.tribes.Member member) {
                    membershipManager.memberAdded(member);
                }

                public void memberDisappeared(org.apache.catalina.tribes.Member member) {
                    membershipManager.memberDisappeared(member);
                }
            };
            channel.addMembershipListener(membershipListener);
            membershipManagers.add(membershipManager);
        }
    }

    public List<MembershipManager> getMembershipManagers() {
        return membershipManagers;
    }

    public void notifyMemberJoin(final Member member) {

        if (TribesUtil.isInDomain(member, loadBalancerDomain)) {  // A peer load balancer has joined

            // Notify all members in the LB group
            primaryMembershipManager.sendMemberJoinedToAll(member);

            // Send the MEMBER_LISTS of all the groups to the the new LB member
            for (MembershipManager manager : membershipManagers) {
                manager.sendMemberList(member);
            }
        } else { // An application member has joined.

            // Need to notify all members in the group of the new app member
            Thread th = new Thread() {
                public void run() {
                    for (MembershipManager manager : membershipManagers) {
                        if (TribesUtil.isInDomain(member, manager.getDomain())) {

                            // Send MEMBER_JOINED to the group of the new member
                            manager.sendMemberJoinedToAll(member);

                            // Send MEMBER_JOINED to the load balancer group
                            sendMemberJoinedToLoadBalancerGroup(manager.getRpcMembershipChannel(),
                                                                member);
                            break;
                        }
                    }
                }

                /**
                 * Send MEMBER_JOINED to the load balancer group
                 * @param rpcChannel The RpcChannel corresponding to the member's group
                 * @param member  The member who joined
                 */
                private void sendMemberJoinedToLoadBalancerGroup(RpcChannel rpcChannel,
                                                                 Member member) {
                    MemberJoinedCommand cmd = new MemberJoinedCommand();
                    cmd.setMember(member);
                    try {
                        rpcChannel.send(primaryMembershipManager.getMembers(),
                                        cmd,
                                        RpcChannel.ALL_REPLY,
                                        Channel.SEND_OPTIONS_ASYNCHRONOUS,
                                        10000);
                    } catch (ChannelException e) {
                        String errMsg = "Could not send MEMBER_JOINED[" +
                                        TribesUtil.getName(member) +
                                        "] to all load balancer members ";
                        log.error(errMsg, e);
                        throw new RemoteProcessException(errMsg, e);
                    }
                }
            };
            th.start();
        }
    }
}
