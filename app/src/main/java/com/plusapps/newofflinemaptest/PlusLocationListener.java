package com.plusapps.newofflinemaptest;

import android.location.Location;

/**
 * Created by bagjeong-gyu on 2017. 1. 9..
 */
public interface PlusLocationListener {

    void onLocationCatched(Location location);

    void onFirstLocationCatched(Location location);
}
