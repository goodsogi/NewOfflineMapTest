package com.plusapps.newofflinemaptest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * FusedLocationApi 사용(실내에서도 위치 데이터 가져올 수 있음)
 * 현재위치 마커가 이상하게 표시안됨. PRIORITY_LOW_POWER로 지정하지 않았기 때문인 듯
 * <setPriority>
 * PRIORITY_BALANCED_POWER_ACCURACY: "block" level, 100미터 오차
 * PRIORITY_HIGH_ACCURACY: "gps"
 * PRIORITY_LOW_POWER: "city" level, 10킬로미터 오차
 * PRIORITY_NO_POWER: 배터리를 소모하지 않는, 가장 정확한 level
 *
 * @author jeff
 */
public class PlusFusedLocationFinder implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    //1분으로 지정해도 금방 location 가져옴. 첫 위치는 빨리 가져오고 그다음은 setInterval에 지정된 값(근사값)으로 갱신
    private static final long INTERVAL_INDOOR = 1000 * 60 * 60; //10초보다 작으면 실내에서 위치를 못 가져오는 것 같음
    private static final long INTERVAL_INDOOR_FASTEST = 1000 * 60;
    private static final long INTERVAL_OUTDOOR = 1000 * 2;
    private static final long INTERVAL_OUTDOOR_FASTEST = 1000 * 1;

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private PlusLocationListener mListener;
    private Location mCurrentLocation;
    private boolean mIsFirstLocationCatched;

    public PlusFusedLocationFinder(Context context, PlusLocationListener listener) {
        mContext = context;
        mListener = listener;
        mCurrentLocation = null;
        mIsFirstLocationCatched = true;
    }

    public void getIndoorLocation() {

        buildGoogleApiClient();

        createLocationRequestIndoor();

        connectGoogleApi();
    }

    public void getOutdoorLocation() {

        buildGoogleApiClient();

        createLocationRequestOutdoor();

        connectGoogleApi();
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    public Location getCurrentLocation() {
        return mCurrentLocation;
    }

    @Override
    public void onLocationChanged(Location location) {

        mCurrentLocation = location;

        if (mIsFirstLocationCatched) {
            mIsFirstLocationCatched = false;
            mListener.onFirstLocationCatched(location);
            return;
        }

        mListener.onLocationCatched(location);
    }

    public void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            return;
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        //googleapiclient의 disconnect는 안하는 것이 좋을 듯
        mGoogleApiClient.disconnect();
    }



    /**
     * 실내 위치정보 요청
     * setInterval은 1분 추천(가장 빠른 주기)
     * PRIORITY_BALANCED_POWER_ACCURACY는 오차 100미터(block level accuracy)
     * onPause에서 Location request를 제거하는 것 추천. 아니면 더 긴 갱신간격과 더 낮은 정확도의 location request로 바꾸는 것이 좋음
     */
    private void createLocationRequestIndoor() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.

        //interval을 1초로 잡으니께 실내에서 onLocationChanged가 호출이 안된당께. 그래서 10초로 잡으써
        mLocationRequest.setInterval(INTERVAL_INDOOR);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(INTERVAL_INDOOR_FASTEST);

        //오차 100미터 정확도
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //이거 설정하면 현재 위치 찾는데 시간 넘 오래 걸림
        // mLocationRequest.setSmallestDisplacement(1); //위치 갱신하는 최소 거리, 야가 설정되면 안 움직이면 업데이트 안 하니더, 실내에서는 하는데여. 이여상하네
    }

    /**
     * 야외 위치정보 요청
     * setInterval은 1분 추천(가장 빠른 주기)
     * PRIORITY_BALANCED_POWER_ACCURACY는 오차 100미터(block level accuracy)
     * onPause에서 Location request를 제거하는 것 추천. 아니면 더 긴 갱신간격과 더 낮은 정확도의 location request로 바꾸는 것이 좋음
     */
    private void createLocationRequestOutdoor() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.

        mLocationRequest.setInterval(INTERVAL_OUTDOOR);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(INTERVAL_OUTDOOR_FASTEST);

        //gps 우선
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //이거 설정하면 현재 위치 찾는데 시간 넘 오래 걸림
        // mLocationRequest.setSmallestDisplacement(1);
    }

    private void connectGoogleApi() {
        mGoogleApiClient.connect();
    }

    private void startLocationUpdates() {

        //mGoogleApiClient가 이상하게 connected 안된 경우 발생. 처리 필요!
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

}
