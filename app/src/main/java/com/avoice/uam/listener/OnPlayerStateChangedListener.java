package com.avoice.uam.listener;

import com.avoice.uam.util.Config;

public interface OnPlayerStateChangedListener {
    void onPlayerStateChange(Config.State newState);
}
