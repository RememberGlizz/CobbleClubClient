/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vexsoftware.votifier.fabric.event.VoteListener
 *  com.vexsoftware.votifier.model.Vote
 */
package com.cobbleclub.server.service;

import com.cobbleclub.server.service.VoteRewardService;
import com.vexsoftware.votifier.fabric.event.VoteListener;
import com.vexsoftware.votifier.model.Vote;

final class NuVotifierVoteBridge {
    private static boolean registered;

    private NuVotifierVoteBridge() {
    }

    static synchronized void register() {
        if (registered) {
            return;
        }
        VoteListener.EVENT.register(NuVotifierVoteBridge::onVote);
        registered = true;
    }

    private static void onVote(Vote vote) {
        if (vote == null) {
            return;
        }
        VoteRewardService.receiveNuVotifierVote(vote.getUsername(), vote.getServiceName(), vote.getTimeStamp());
    }
}

