package com.example.mapme.view;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.example.mapme.R;
import com.example.mapme.model.AppService;
import com.example.mapme.presenter.AddObjectPresenter;
import com.example.mapme.view.overlays.PaintingSurface;
import com.example.mapme.widgets.CustomKmlFolder;
import com.example.mapme.widgets.CustomOverlay;

import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.HashMap;

import hu.supercluster.overpasser.adapter.OverpassQueryResult;

/**
 * AddObjectActivity - this abstract Activity provides all common methods for AddMarker, AddPolygon and AddPolyline Activities.
 */
public abstract class AddObjectActivity extends AppCompatActivity implements View.OnClickListener {

    public AddObjectPresenter presenter;
    protected MapView mMapView;
    private Marker userMarker;
    private ImageButton btnRotateLeft, btnRotateRight;
    private ImageButton painting, panning;
    private PaintingSurface paintingSurface;
    public AppService appService;
    protected boolean appServiceBound;
    private boolean serviceConnected = false;
    public String currentGeoObjectId;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new AddObjectPresenter(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent bindIntent = new Intent(AddObjectActivity.this, AppService.class);
        bindService(bindIntent, appServiceConnection, Context.BIND_AUTO_CREATE);
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(appServiceConnection);
        mMapView.onPause();
    }

    /**
     * Processes extras from intent and sets map position and user marker accordingly.
     */
    public void setMapPositionAndUserMarker() {
        IMapController mapController = mMapView.getController();
        // set map position
        Intent intent = getIntent();
        double mapCenterLatitude = intent.getDoubleExtra("mapCenterLatitude", 49.89873);
        double mapCenterLongitude = intent.getDoubleExtra("mapCenterLongitude", 10.90067);
        double zoomLevel = intent.getDoubleExtra("zoomLevel", 17.0);
        double userGeoPointLatitude = intent.getDoubleExtra("userGeoPointLatitude", 0);
        double userGeoPointLongitude = intent.getDoubleExtra("userGeoPointLongitude", 0);
        GeoPoint startPoint = new GeoPoint(mapCenterLatitude, mapCenterLongitude);
        mapController.setCenter(startPoint);
        mapController.setZoom(zoomLevel);
        // set user marker
        userMarker = new Marker(mMapView);
        userMarker.setIcon(getResources().getDrawable(R.drawable.position));
        userMarker.setPosition(new GeoPoint(userGeoPointLatitude, userGeoPointLongitude));
        mMapView.getOverlays().add(userMarker);
    }

    /**
     * Enables rotation using icons or multitouch.
     */
    public void enableRotation() {
        btnRotateLeft = findViewById(R.id.btnRotateLeft);
        btnRotateRight = findViewById(R.id.btnRotateRight);
        btnRotateRight.setOnClickListener(this);
        btnRotateLeft.setOnClickListener(this);
        mMapView = findViewById(R.id.map);
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(mMapView);
        mRotationGestureOverlay.setEnabled(true);
        mMapView.setMultiTouchControls(true);
        mMapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis() + " onScroll " + event.getX() + "," + event.getY());
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis() + " onZoom " + event.getZoomLevel());
                return true;
            }
        });
        mMapView.getOverlayManager().add(mRotationGestureOverlay);
    }

    /**
     * Enables painting by initialising painting surface and setting mode.
     */
    public void enablePainting(PaintingSurface.Mode mode) {
        panning = findViewById(R.id.enablePanning);
        panning.setOnClickListener(this);
        panning.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        painting = findViewById(R.id.enablePainting);
        painting.setOnClickListener(this);
        paintingSurface = findViewById(R.id.paintingSurface);
        paintingSurface.init(this, mMapView);
        paintingSurface.setMode(mode);
    }

    /**
     * Disables painting.
     */
    public void disablePainting() {
        panning = findViewById(R.id.enablePanning);
        panning.setVisibility(View.GONE);
        painting = findViewById(R.id.enablePainting);
        painting.setVisibility(View.GONE);
    }

    /**
     * OnClick listener for rotate and painting/panning icons.
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.enablePanning:
                paintingSurface.setVisibility(View.GONE);
                panning.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                painting.setBackgroundColor(Color.TRANSPARENT);
                break;
            case R.id.enablePainting:
                paintingSurface.setVisibility(View.VISIBLE);
                painting.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                panning.setBackgroundColor(Color.TRANSPARENT);
                break;
            case R.id.btnRotateLeft: {
                float angle = mMapView.getMapOrientation() + 10;
                if (angle > 360)
                    angle = 360 - angle;
                mMapView.setMapOrientation(angle);
            }
            break;
            case R.id.btnRotateRight: {
                float angle = mMapView.getMapOrientation() - 10;
                if (angle < 0)
                    angle += 360f;
                mMapView.setMapOrientation(angle);
            }
            break;
        }
    }

    /**
     * AppService Connection.
     */
    public ServiceConnection appServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AppService.LocalBinder binder = (AppService.LocalBinder) service;
            appService = binder.getService();
            appServiceBound = true;
            appService.registerListener(presenter);
            serviceConnected = true;
            presenter.dataChanged();
            Log.d("info", "Service bound to AddObjectActivity");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            appServiceBound = false;
            Log.d("info", "Service unbound to AddObjectActivity");
        }
    };

    /**
     * Updates user position on map.
     *
     * @param userGeoPoint
     */
    public void updateUserPosition(GeoPoint userGeoPoint) {
        userMarker.setPosition(userGeoPoint);
        mMapView.invalidate();
        Log.d("info", "Updating user position");
    }

    /**
     * Shows info dialog to edit geoObject.
     *
     * @param view
     * @param id
     */
    public void showInfoEditObject(View view, String id) {
        currentGeoObjectId = id;
        AlertDialog.Builder infoDialog = new AlertDialog.Builder(AddObjectActivity.this);
        infoDialog.setTitle("GeoObject (Id: " + id + ")");
        infoDialog.setMessage("The GeoObject has already been saved to the database. You can edit the information about the GeoObject.");
        infoDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        infoDialog.setNeutralButton("Edit",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startEditObjectActivity();
                    }
                });
        infoDialog.show();
    }

    /**
     * Shows info dialog to add reference or edit geoObject.
     *
     * @param view
     * @param objectId
     */
    public void showInfoAddReferenceOrEditObject(View view, final String objectId, final OverlayWithIW geometry) {
        currentGeoObjectId = objectId;
        AlertDialog.Builder infoDialog = new AlertDialog.Builder(AddObjectActivity.this);
        infoDialog.setTitle("GeoObject (Id: " + objectId + ")");
        infoDialog.setMessage("The GeoObject has already been saved to the database. You can either add a reference to an existing object or edit the information about the GeoObject.");
        infoDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        infoDialog.setNeutralButton("Edit",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startEditObjectActivity();
                    }
                });
        infoDialog.setPositiveButton("Add Reference",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        presenter.computeOverpassResult(geometry, objectId);
                    }
                });
        infoDialog.show();
    }

    /**
     * Shows info dialog when reference was added.
     *
     * @param view
     * @param id
     */
    public void showInfoReferenceAdded(View view, String id) {
        currentGeoObjectId = id;
        AlertDialog.Builder infoDialog = new AlertDialog.Builder(AddObjectActivity.this);
        infoDialog.setTitle("GeoObject (Id: " + id + ")");
        infoDialog.setMessage("The reference was added to the GeoObject. You can now edit the information about the GeoObject.");
        infoDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        infoDialog.setNeutralButton("Edit",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startEditObjectActivity();
                    }
                });
        infoDialog.show();
    }

    /**
     * Shows info dialog when reference was added.
     *
     * @param id
     */
    public void showInfoEmptyOverpassResult(String id) {
        currentGeoObjectId = id;
        AlertDialog.Builder infoDialog = new AlertDialog.Builder(AddObjectActivity.this);
        infoDialog.setTitle("GeoObject (Id: " + id + ")");
        infoDialog.setMessage("There were no objects to reference found nearby.");
        infoDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        infoDialog.setNeutralButton("Edit GeoObject",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startEditObjectActivity();
                    }
                });
        infoDialog.show();
    }


    /**
     * Opens new EditInformationActivity for last saved geoObject.
     */
    public void startEditObjectActivity() {
        Intent intent = new Intent(this, EditInformationActivity.class);
        intent.putExtra("id", currentGeoObjectId);
        startActivity(intent);
    }

    /**
     * Calls appService to save geoObject to database.
     *
     * @param geometry
     * @return
     */
    public String saveToDatabase(OverlayWithIW geometry) {
        return presenter.saveToDatabase(geometry);
    }

    /**
     * Go back to previous activity.
     *
     * @param view
     */
    public void back(View view) {
        this.finish();
    }

    public void addAdditionalLayer(HashMap<String, String> objects) {
        mMapView.getOverlays().clear();
        KmlDocument kmlDocument = new KmlDocument();

        if (!objects.isEmpty()) {
            for (String key : objects.keySet()) {
                kmlDocument.parseGeoJSON(objects.get(key));
                Drawable defaultMarker = getResources().getDrawable(R.drawable.pin);
                Bitmap defaultBitmap = ((BitmapDrawable) defaultMarker).getBitmap();
                Style defaultStyle = new Style(defaultBitmap, 0x901010AA, 3.0f, 0x20AA1010);
                CustomKmlFolder cKmlFolder = new CustomKmlFolder();
                cKmlFolder.mItems = kmlDocument.mKmlRoot.mItems;
                CustomOverlay myOverLay = cKmlFolder.buildOverlay(mMapView, defaultStyle, null, kmlDocument, key);
                mMapView.getOverlays().add(myOverLay);
            }
            mMapView.invalidate();
            Log.d("info", "Additional layer was added");
        } else {
            Log.d("info", "Additional layer could not be added");
        }
    }

    public void addLayerWithOverpassResult(OverpassQueryResult result, int numberOfElements, final String objectId) {
        for (int i = 0; i < numberOfElements; i++) {
            final OverpassQueryResult.Element e = result.elements.get(i);
            GeoPoint geoPoint = new GeoPoint(e.lat, e.lon);

            Marker marker = new Marker(mMapView);
            marker.setPosition(geoPoint);
            marker.setTitle(e.tags.name);
            marker.setTextIcon(e.tags.name);
            //marker.setIcon(getDrawable(R.drawable.pin));
            marker.setOnMarkerClickListener(
                    new Marker.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker, MapView mapView) {
                            // add reference to database
                            HashMap<String, String> properties = new HashMap<>();
                            properties.put("reference", String.valueOf(e.id));
                            presenter.addObjectProperties(objectId, properties);
                            // show info
                            showInfoReferenceAdded(mMapView, objectId);
                            return false;
                        }
                    });
            mMapView.getOverlays().add(marker);
            marker.showInfoWindow();
            Log.d("info", "Layer with OverpassQueryResult was added");
        }
        // enable panning
        paintingSurface.setVisibility(View.GONE);
        panning.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        painting.setBackgroundColor(Color.TRANSPARENT);
    }
}