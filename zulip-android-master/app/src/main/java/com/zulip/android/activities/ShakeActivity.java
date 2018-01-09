package com.zulip.android.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Person;
import com.zulip.android.networking.AsyncLocationSending;
import com.zulip.android.networking.AsyncScoreSending;
import com.zulip.android.networking.ZulipAsyncPushTask;

import org.apache.commons.lang.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import com.google.android.gms.location.LocationServices;

public class ShakeActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private static final int LOCATION = 1;
    private static final int REQUEST_MAP_CHOOSE_PERSON = 1;

    private SensorManager sensorManager;
    private boolean isRunning = false;
    private float acelVal;  // current acceleration value and gravity.
    private float acelLast; // last acceleration value and gravity.
    private float shake;    // acceleration value differ from gravity.
    private Handler locationSenderHandler;
    private Runnable locationSenderRunnable;
    private LocationManager locationManager;
    private ZulipLocationManager locationListener;
    private ImageView backgroundView;
    private ListView nearbyListView;
    private List<Person> personList;
    private Map<String, Float> distanceMap;
    private ImageButton mapButton;
    private String provider;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shake);

        personList = new ArrayList<>();
        distanceMap = new LinkedHashMap<>();

        mapButton = (ImageButton) findViewById(R.id.mapButton);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ShakeActivity.this, NearbyListMapsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("NEARBYLIST", (Serializable) personList);
                intent.putExtras(bundle);
                startActivityForResult(intent, REQUEST_MAP_CHOOSE_PERSON);

            }
        });

        backgroundView = (ImageView) findViewById(R.id.background);
        backgroundView.setVisibility(View.VISIBLE);
        nearbyListView = (ListView) findViewById(R.id.nearbyList);

        // initial locationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = ZulipLocationManager.getInstance();

        //google api client
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        acelVal = SensorManager.GRAVITY_EARTH;
        acelLast = SensorManager.GRAVITY_EARTH;
        shake = 0.00f;

        // initial location sender
        locationSenderHandler = new Handler();
        final String timeStamp = ((Long) System.currentTimeMillis()).toString();
        locationSenderRunnable = new Runnable() {
            @Override
            public void run() {
                ZulipApp app = (ZulipApp) getApplicationContext();
                AsyncLocationSending task = new AsyncLocationSending(app);
                task.setTimeStamp(timeStamp);
                task.setCallback(new ZulipAsyncPushTask.AsyncTaskCompleteListener() {
                    @Override
                    public void onTaskComplete(String result, JSONObject object) {

                        backgroundView.setVisibility(View.INVISIBLE);


                        Person person;

                        try {
                            object = new JSONObject(result);
                            Iterator<String> iterator = object.keys();
                            while (iterator.hasNext()) {
                                String key = iterator.next();

                                JSONArray tuple = new JSONArray(object.getString(key));

                                person = new Person(tuple.getString(0), key);
                                person.setLatitude(Float.parseFloat(tuple.getString(2)));
                                person.setLongitude(Float.parseFloat(tuple.getString(3)));
                                if (tuple.length() > 4) {
                                    person.setScore(tuple.getString(4));
                                    person.setComment(java.net.URLDecoder.decode(tuple.getString(5), "UTF-8"));
                                }
                                if (!personList.contains(person)) {
                                    personList.add(person);
                                    distanceMap.put(person.getEmail(), 1000*Float.parseFloat(tuple.getString(1)));
                                }else{
                                    personList.remove(person);
                                    personList.add(person);
                                    distanceMap.put(person.getEmail(), 1000*Float.parseFloat(tuple.getString(1)));
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        NearbyListAdapter nearbyListAdapter = new NearbyListAdapter(ShakeActivity.this, personList, distanceMap);
                        nearbyListView.setAdapter((nearbyListAdapter));
                        nearbyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                final Person choosedPerson = (Person) nearbyListView.getItemAtPosition(position);
                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
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

                                AlertDialog.Builder builder = new AlertDialog.Builder(ShakeActivity.this);
                                builder.setMessage("Do you want to chat with " + choosedPerson.getName() + " ?").setPositiveButton("Yes", dialogClickListener)
                                        .setNegativeButton("No", dialogClickListener).show();


                            }
                        });
                        nearbyListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                                Person person = (Person) nearbyListView.getItemAtPosition(position);
                                LayoutInflater inflater = LayoutInflater.from(ShakeActivity.this);
                                final View scoreView = inflater.inflate(R.layout.choose_person_scorelayout, null);
                                String score = person.getScore();
                                String comment = person.getComment();
                                if (score != null)
                                    ((TextView) scoreView.findViewById(R.id.score)).setText("評分: " + score);
                                else
                                    ((TextView) scoreView.findViewById(R.id.score)).setText("評分: 目前沒有任何評分");

                                if (score != null)
                                    ((TextView) scoreView.findViewById(R.id.comment)).setText("評語: " + comment);
                                else
                                    ((TextView) scoreView.findViewById(R.id.comment)).setText("評語: 目前沒有任何評語");


                                new AlertDialog.Builder(ShakeActivity.this)
                                        .setTitle(person.getName()+"的評語")
                                        .setView(scoreView)
                                        .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                return;
                                            }
                                        })
                                        .show();
                                return true;
                            }
                        });

                    }

                    @Override
                    public void onTaskFailure(String result) {
                        Toast.makeText(ShakeActivity.this, "fail", Toast.LENGTH_SHORT).show();
                        System.out.println(result);
                    }

                });

                if (!isRunning)
                    return;
                task.execute();

                locationSenderHandler.postDelayed(this,5 * 1000);
            }
        };


        checkPermission();
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
    }

    @Override
    protected void onDestroy() {
        isRunning = false;
        locationManager.removeUpdates(locationListener);
        googleApiClient.disconnect();
        locationSenderHandler.removeCallbacks(locationSenderRunnable);
        super.onDestroy();
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged( SensorEvent sensorEvent ) {

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            acelLast = acelVal;
            acelVal = (float) Math.sqrt( (double) ( x * x + y * y + z * z ) );
            float delta = acelVal - acelLast;
            shake = shake * 0.9f + delta;

            // If shaking, get location and send to server for start tracking.
            if( shake > 12 ) {
                locationSenderHandler.removeCallbacks(locationSenderRunnable);
                locationSenderHandler.post(locationSenderRunnable);
            }
        }
        @Override
        public void onAccuracyChanged( Sensor sensor, int i ) {}
    };

    public void checkPermission() {
        int permission = ActivityCompat.checkSelfPermission(ShakeActivity.this, ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(ShakeActivity.this, new String[]{ACCESS_FINE_LOCATION}, LOCATION);
        else {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);

            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
            provider = locationManager.getBestProvider(criteria, true);
            isRunning = true;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case LOCATION:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "You need to provide Location Access for us.", Toast.LENGTH_SHORT).show();
                    isRunning = false;

                    setResult(RESULT_CANCELED);
                    finish();
                }
                else
                    isRunning = true;
                break;

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MAP_CHOOSE_PERSON && resultCode == RESULT_OK){
            Person person = (Person) data.getBundleExtra("PERSON").getSerializable("PERSON");

            Intent returnIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable("PERSON", (Serializable) person);
            returnIntent.putExtra("PERSON", bundle);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        checkPermission();
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location!= null)
            ZulipLocationManager.getInstance().setCurrentLocation(location);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        ZulipLocationManager.getInstance().onLocationChanged(location);
        System.out.println("locationIs: "+ Double.toString(location.getLongitude()));
    }
}
