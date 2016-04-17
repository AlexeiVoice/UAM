package com.avoice.uam.interfaces;

import com.avoice.uam.util.Config;

public interface OnPlayerStateChangedListener {
    void onPlayerStateChange(Config.State newState);
}
