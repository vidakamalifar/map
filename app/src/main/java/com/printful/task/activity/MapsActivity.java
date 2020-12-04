package com.printful.task.activity;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.printful.task.R;
import com.printful.task.model.User;
import com.printful.task.network.TCPCommunicatorNetwork;
import com.printful.task.network.TCPListener;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, TCPListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int DEFAULT_ZOOM = 17;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    //MAP
    private GoogleMap map;
    private CameraPosition cameraPosition;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;

    private Location lastKnownLocation;
    private final LatLng defaultLocation = new LatLng(24.7046886, 141.3106085);

    private boolean locationPermissionGranted;

    private Marker marker;
    private String address;

    //TCP
    private TCPCommunicatorNetwork tcpClient;
    private Handler UIHandler = new Handler();
    private boolean isFirstLoad = true;
    private String message;
    private String updateMessage1;

    //USER
    private ArrayList<User> userArrayList = new ArrayList<>();
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        setContentView(R.layout.activity_maps);

        ConnectToServer();

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        // [START maps_current_place_map_fragment]
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void ConnectToServer() {
        tcpClient = TCPCommunicatorNetwork.getInstance();
        TCPCommunicatorNetwork.addListener(this);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        tcpClient.init(getString(R.string.server_url),
                6111);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getUserList();
            }
        }, 2000);
    }

    private void getUserList() {
        JSONObject obj = new JSONObject();

        try {
            obj.put(getString(R.string.authorize), getString(R.string.email));
        } catch (Exception e) {
            e.printStackTrace();
        }
        TCPCommunicatorNetwork.writeToSocket("AUTHORIZE kamalifar.vida@gmail.com", UIHandler, this);
    }

    @Override
    public void onTCPMessageReceived(String newMessage) {

        final String theMessage = newMessage;
        if (theMessage.startsWith(getString(R.string.USERLIST))) {
            message = theMessage.replace(getString(R.string.USERLIST), "");
            String[] rawUser = message.split(";");

            for (int i = 0; i < rawUser.length; i++) {
                String[] singleUser = rawUser[i].split(",");

                user = new User(singleUser[0], singleUser[1], singleUser[2], singleUser[3], singleUser[4]);
                userArrayList.add(user);

            }

        } else if (theMessage.startsWith(getString(R.string.UPDATE))) {
            updateMessage1 = theMessage.replace(getString(R.string.UPDATE), "");
            if (!updateMessage1.isEmpty()) {
                String[] userList = message.split(";");

                for (int i = 0; i < userList.length; i++) {
                    String[] updateUser = updateMessage1.split(",");

                    if (userArrayList.get(i).getId().equals(updateUser[0])) {
                        userArrayList.get(i).setLatitude(updateUser[1]);
                        userArrayList.get(i).setLongitude(updateUser[2]);
                    }
                }
            }
        }
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                showUserLocation();
            }
        });
    }

    @Override
    public void onTCPConnectionStatusChanged(boolean isConnectedNow) {
        if (isConnectedNow) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Log.d(TAG, "run: ");
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFirstLoad) {
            TCPCommunicatorNetwork.closeStreams();
            ConnectToServer();
        } else
            isFirstLoad = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.user_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView tvName = infoWindow.findViewById(R.id.tvUserName);
                TextView tvAddress = infoWindow.findViewById(R.id.tvUserAddress);
                final ShapeableImageView ivUser = infoWindow.findViewById(R.id.ivUser);
                tvName.setText(marker.getTitle());
                tvAddress.setText(marker.getSnippet());

                Glide.with(MapsActivity.this).load(userArrayList.get((Integer) marker.getTag()).getImage()).placeholder(android.R.drawable.progress_indeterminate_horizontal).error(android.R.drawable.stat_notify_error).into(ivUser);

                return infoWindow;
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
//                                showUserLocation();


                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void showUserLocation() {
        if (map != null) {
            if (marker != null) {
                for (int i = 0; i < userArrayList.size(); i++) {
                    final LatLng latlng = new LatLng(Double.parseDouble(userArrayList.get(i).getLatitude()),
                            Double.parseDouble(userArrayList.get(i).getLongitude()));
                    marker.setPosition(latlng);
                }
            } else {
                for (int i = 0; i < userArrayList.size(); i++) {
                    address = getAddress(Double.parseDouble(userArrayList.get(i).getLatitude()), Double.parseDouble(userArrayList.get(i).getLongitude()));
                    marker = map.addMarker(new MarkerOptions()
                            .title(userArrayList.get(i).getName())
                            .position(new LatLng(Double.parseDouble(userArrayList.get(i).getLatitude()),
                                    Double.parseDouble(userArrayList.get(i).getLongitude())))
                            .snippet(address));

                    marker.setTag(i);
                }
            }

        }
    }

    private String getAddress(double latitude, double longitude) {
        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }

        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        return address;
    }
}