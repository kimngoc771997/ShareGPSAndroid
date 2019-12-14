package com.example.sharegps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private Location location;
    private TextView txtdinhvi;
    private TextView txtDangKy;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // = 5 seconds

    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private Button btnGPS;
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    private DatabaseReference mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnGPS = findViewById(R.id.button);
        txtdinhvi = findViewById(R.id.txtDinhvi);
        txtDangKy = findViewById(R.id.txtDangKy);

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }

        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();

        btnGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (googleApiClient != null && btnGPS.getText().equals("Bật định vị")) {
                    btnGPS.setText("Tắt định vị");
                    googleApiClient.connect();
                }
                else if (btnGPS.getText().equals("Tắt định vị")) {
                    final SharedPreferences shareGPS = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
                    String maTruyCap = shareGPS.getString("maTruyCap", "");
                    btnGPS.setText("Bật định vị");
                    txtdinhvi.setText("Vị trí");
                    googleApiClient.disconnect();
                    mData.child(maTruyCap).removeValue();
                }

            }
        });
        txtDangKy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDangKy();
            }
        });
        mData = FirebaseDatabase.getInstance().getReference();
    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//
//        if (googleApiClient != null) {
//            googleApiClient.connect();
//        }
//    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!checkPlayServices()) {
            txtdinhvi.setText("Bạn cần cài đặt Dịch vụ Google Play để sử dụng ứng dụng");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop location updates
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }

            return false;
        }

        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (btnGPS.getText().equals("Tắt định vị")) {
            Map<String, Object> childUpdates = new HashMap<>();
            final SharedPreferences shareGPS = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
            String maTruyCap = shareGPS.getString("maTruyCap", "");
            if (location != null) {
                txtdinhvi.setText("Vĩ độ : " + location.getLatitude() + "\nKinh độ : " + location.getLongitude());
                childUpdates.put("id", Integer.parseInt(maTruyCap.substring(3,8)));
                childUpdates.put("angle", 50);
                childUpdates.put("idBusInfo", maTruyCap.substring(3,8));
                childUpdates.put("Latitude", location.getLatitude());
                childUpdates.put("Longitude", location.getLongitude());
                if(!childUpdates.isEmpty()){
                    mData.child(maTruyCap).setValue(childUpdates);
                }
            }
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (btnGPS.getText().equals("Tắt định vị")) {
            Map<String, Object> childUpdates = new HashMap<>();
            final SharedPreferences shareGPS = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
            if (location != null) {
                txtdinhvi.setText("Vĩ độ : " + location.getLatitude() + "\nKinh độ : " + location.getLongitude());
                childUpdates.put("Latitude", location.getLatitude());
                childUpdates.put("Longitude", location.getLongitude());
                if(!childUpdates.isEmpty()){
                    String maTruyCap = shareGPS.getString("maTruyCap", "");
                    mData.child(maTruyCap).updateChildren(childUpdates);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }
                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                    }
                }

                break;
        }
    }
    private void dialogDangKy(){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_custom);
        final SharedPreferences shareGPS = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);

        final TextView txtmaTruyCap = dialog.findViewById(R.id.txtMaTruyCap);
        Button btnThoat = dialog.findViewById(R.id.btnCancel);
        Button btnDangKy = dialog.findViewById(R.id.btnDangKy);
        String maTruyCap = shareGPS.getString("maTruyCap", "");
        txtmaTruyCap.setText(maTruyCap);
        btnThoat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btnDangKy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit = shareGPS.edit();
                edit.putString("maTruyCap",txtmaTruyCap.getText().toString().trim());
                edit.apply();
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
