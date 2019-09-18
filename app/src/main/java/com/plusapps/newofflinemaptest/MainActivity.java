package com.plusapps.newofflinemaptest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.plusapps.newofflinemaptest.mapsforge.MapsForgeTileProvider;
import com.plusapps.newofflinemaptest.mapsforge.MapsForgeTileSource;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;

import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.tileprovider.util.StorageUtils;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//***************************************************************************************
// 1. 오프라인 지도 생성
// 2. 오프라인 지도 rendering
// 3. location 처리
// 4. 권한 처리
// 5. 라이프사이클
//***************************************************************************************
public class MainActivity extends AppCompatActivity implements PlusLocationListener {


    //***************************************************************************************
    //
    // 오프라인 지도 생성
    //
    //***************************************************************************************
    private MapView mOfflineMap;
    MapsForgeTileSource fromFiles = null;
    MapsForgeTileProvider forge = null;
    AlertDialog alertDialog = null;


    private void createOfflineMap() {

        mOfflineMap = createMfMapView();

        Set<File> mapfiles = findMapFiles();
        //do a simple scan of local storage for .map files.
        File[] maps = new File[mapfiles.size()];
        maps = mapfiles.toArray(maps);
        if (maps == null || maps.length == 0) {
            //show a warning that no map files were found
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    this);

            // set title
            alertDialogBuilder.setTitle("No Mapsforge files found");

            // set dialog message
            alertDialogBuilder
                    .setMessage("In order to render map tiles, you'll need to either create or obtain mapsforge .map files. See https://github.com/mapsforge/mapsforge for more info. Store them in "
                            + Configuration.getInstance().getOsmdroidBasePath().getAbsolutePath())
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (alertDialog != null) alertDialog.dismiss();
                        }
                    });


            // create alert dialog
            alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

        } else {
            Toast.makeText(this, "Loaded " + maps.length + " map files", Toast.LENGTH_LONG).show();

            //this creates the forge provider and tile sources

            //protip: when changing themes, you should also change the tile source name to prevent cached tiles

            //null is ok here, uses the default rendering theme if it's not set
            XmlRenderTheme theme = null;
//            try {
//                theme = new AssetsRenderTheme(this.getApplicationContext(), "renderthemes/", "rendertheme-v4.xml");
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }

            fromFiles = MapsForgeTileSource.createFromFiles(maps, theme, "rendertheme-v4");
            forge = new MapsForgeTileProvider(
                    new SimpleRegisterReceiver(this),
                    fromFiles, null);


            mOfflineMap.setTileProvider(forge);


            //now for a magic trick
            //since we have no idea what will be on the
            //user's device and what geographic area it is, this will attempt to center the map
            //on whatever the map data provides
            //mOfflineMap.getController().setZoom(fromFiles.getMinimumZoomLevel());
            mOfflineMap.getController().setZoom(20.0);
            //mOfflineMap.zoomToBoundingBox(fromFiles.getBoundsOsmdroid(), true);
        }
    }

    private MapView createMfMapView() {

        final RelativeLayout mapContainer = (RelativeLayout) findViewById(R.id.offline_map_container);

        try {

            MapView offlineMap = new MapView(this);

            mapContainer.addView(offlineMap, 0);

            return offlineMap;

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }


    /**
     * simple function to scan for paths that match /something/osmdroid/*.map to find mapforge database files
     *
     * @return
     */
    protected static Set<File> findMapFiles() {
        Set<File> maps = new HashSet<>();
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();
        for (int i = 0; i < storageList.size(); i++) {
            File f = new File(storageList.get(i).path + File.separator + "osmdroid" + File.separator);
            if (f.exists()) {
                maps.addAll(scan(f));
            }
        }
        return maps;
    }

    static private Collection<? extends File> scan(File f) {
        List<File> ret = new ArrayList<>();
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().toLowerCase().endsWith(".map"))
                    return true;
                return false;
            }
        });
        if (files != null) {
            Collections.addAll(ret, files);
        }
        return ret;
    }




    //***************************************************************************************
    //
    // 오프라인 지도 rendering
    //
    //***************************************************************************************





    private void showCurrentLocation(Location location) {

        if (mOfflineMap == null) {
            return;
        }

        GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        //TODO mOfflineMap이 null인 경우가 가끔 발생. 처리하세요.
//        if(mOfflineMap == null) {
//            return;
//        }

        IMapController mapController = mOfflineMap.getController();
        //mOfflineMap.getController().setZoom(15.0);
        mapController.setCenter(startPoint);
    }


    private void initAndroidGraphicFactory() {

        AndroidGraphicFactory.createInstance(getApplication());
    }



    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }



    //***************************************************************************************
    //
    // location 처리 
    //
    //***************************************************************************************

    void initLocationFinder() {
        //Fused location provider를 사용하는 경우
        PlusFusedLocationFinder fusedLocationFinder = new PlusFusedLocationFinder(this, this);
        fusedLocationFinder.getIndoorLocation();
    }

    @Override
    public void onLocationCatched(Location location) {

    }

    @Override
    public void onFirstLocationCatched(Location location) {
        showCurrentLocation(location);
    }




    //***************************************************************************************
    //
    // 권한 처리
    //
    //***************************************************************************************


    private static final int REQUEST_CODE_REQUEST_APP_PERMISSIONS = 213;

    private void checkAppPermissions() {

        if ( ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED


        ) {
            //사용자가 권한 설정을 거부했는지 체크
            //거부한 경우 shouldShowRequestPermissionRationale는 true 반환
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) && ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION

            ) && ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            )
            ) {

                Toast.makeText(
                        this, "앱 실행을 위해서는 모든 권한을 설정해야 합니다",
                        Toast.LENGTH_LONG
                ).show();
                finish();

            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        REQUEST_CODE_REQUEST_APP_PERMISSIONS
                );

            }
        } else {


        }
    }

    //***************************************************************************************
    //
    // 라이프사이클
    //
    //***************************************************************************************



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAppPermissions();
        initAndroidGraphicFactory();
        initLocationFinder();
        createOfflineMap();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (alertDialog != null) alertDialog.dismiss();
        alertDialog = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (alertDialog != null) {
            alertDialog.hide();
            alertDialog.dismiss();
            alertDialog = null;
        }
        if (fromFiles != null)
            fromFiles.dispose();
        if (forge != null)
            forge.detach();
        AndroidGraphicFactory.clearResourceMemoryCache();
    }




}
