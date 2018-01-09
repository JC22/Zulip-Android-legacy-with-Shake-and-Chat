package com.zulip.android.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.internal.zzf;
import com.zulip.android.R;
import com.zulip.android.models.Person;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NearbyListMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;
    private CameraUpdate allMarkerCameraUpdate;
    private Map<Marker, Person> personMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_list_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        personMap = new LinkedHashMap<>();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        List<Person> personList = (List<Person>) getIntent().getExtras().getSerializable("NEARBYLIST");
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Person person : personList){

            Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(person.getLatitude(), person.getLongitude())).title(person.getName()));
            builder.include(marker.getPosition());

            personMap.put(marker, person);
        }
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.20); // offset from edges of the map 10% of screen
        try {
            allMarkerCameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 0);
        }
        catch (IllegalStateException e){
            allMarkerCameraUpdate = null;
        }

        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(ZulipLocationManager.getInstance().getCurrentLocation().getLatitude(), ZulipLocationManager.getInstance().getCurrentLocation().getLongitude())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 1000, null);
                return true;
            }
        });
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnInfoWindowClickListener(this);
        if (allMarkerCameraUpdate == null)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(ZulipLocationManager.getInstance().getCurrentLocation().getLatitude(), ZulipLocationManager.getInstance().getCurrentLocation().getLongitude())));
        else
            mMap.moveCamera(allMarkerCameraUpdate);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 1000, null);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final Person choosedPerson = personMap.get(marker);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        Intent returnIntent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("PERSON", (Serializable) choosedPerson);
                        returnIntent.putExtra("PERSON", bundle);
                        setResult(RESULT_OK, returnIntent);
                        finish();

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(NearbyListMapsActivity.this);
        builder.setMessage("Do you want to chat with " + choosedPerson.getName() + " ?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

}
