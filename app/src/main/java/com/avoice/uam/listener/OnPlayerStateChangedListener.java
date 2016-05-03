package com.avoice.uam.listener;

import com.avoice.uam.util.Config;

public interface OnPlayerStateChangedListener {
    void onPlayerStateChanged(Config.State newState);
    void onPlayerBufferingPercentChanged(int percent);
}
