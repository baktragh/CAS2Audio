package com.baktra.cas2audio.features.goldies;

import java.util.List;
import java.util.Map;

public interface GoldiesRepositoryObserver {

    public void onSuccessfullRefresh();

    public void onFailedRefresh(Exception failureException);

    public void onDisplay(Map<TapeSystem,List<String>> map,List<TapeSystem> tapeSystems);


    public void onFailedRead(int reason,Exception failureException);
}
