package ucd.app.views.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ucd.app.R;
import ucd.app.entities.Complaint;
import ucd.app.entities.ComplaintPhoto;
import ucd.app.rest.ApiClient;
import ucd.app.rest.ApiService;
import ucd.app.views.adapters.InfoWindowAdapter;

import static ucd.app.views.activities.MainActivity.*;

public class PlaceFragment extends Fragment {

    private List<Complaint> complaints;
    private ProgressBar progressBar;
    private GoogleMap googleMap;
    private MapView googleMapView;

    public PlaceFragment() {
        // Required empty public constructor.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_place, container, false);
        final View infoView = inflater.inflate(R.layout.layout_marker, container, false);

        // Booting the service API and the progressBar.
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        this.progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        this.progressBar.setVisibility(View.VISIBLE);
        this.complaints = new ArrayList<>();

        if (complaints.isEmpty()) {
            apiService.fetchComplaints().enqueue(new Callback<List<Complaint>>() {

                @Override
                public void onResponse(Call<List<Complaint>> call, Response<List<Complaint>> response) {
                    complaints = response.body();
                    setupMarkers();

                    // Finish the loading.
                    progressBar.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onFailure(Call<List<Complaint>> call, Throwable throwable) {
                    Toast.makeText(rootView.getContext(), throwable.toString(), Toast.LENGTH_SHORT).show();

                    // Finish the loading.
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });
        }

        googleMapView = (MapView) rootView.findViewById(R.id.mapView);
        googleMapView.onCreate(savedInstanceState);
        MapsInitializer.initialize(rootView.getContext());
        googleMapView.onResume();

        googleMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {

                googleMap = mMap;
                if (ActivityCompat.checkSelfPermission(rootView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(rootView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                googleMap.setMyLocationEnabled(true);
                googleMap.setInfoWindowAdapter(new InfoWindowAdapter(infoView));
                googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick(Marker marker) {
                        showComplaintAlertActions(marker, infoView);
                    }
                });
                setupMarkers();

                if (location != null) {

                    // Zooming automatically to the location of the user.
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(12).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        googleMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        googleMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //googleMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        googleMapView.onLowMemory();
    }

    private void setupMarkers() {
        if (googleMap != null) {
            for (Complaint complaint : complaints) {
                Double latitude = Double.parseDouble(complaint.getLatitude());
                Double longitude = Double.parseDouble(complaint.getLongitude());

                String complaintPhotosBase64 = "";
                for (ComplaintPhoto complaintPhoto : complaint.getComplaintPhotos()) {
                    complaintPhotosBase64 += complaintPhoto.getPath() + complaintPhoto.getName() + complaintPhoto.getExtension() + ",";
                }

                Gson gson = new Gson();
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(latitude, longitude));
                markerOptions.title(gson.toJson(complaint));

                switch (complaint.getStatus()) {
                    case "STARTED":
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.complaint_started));
                        break;
                    case "INSPECTED":
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.complaint_inspected));
                        break;
                    case "CHECKED":
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.complaint_checked));
                        break;
                    case "DENOUNCED":
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.complaint_denounced));
                        break;
                    default:
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.complaint_default));
                }

                googleMap.addMarker(markerOptions);
            }
        }
    }


    private void showComplaintAlertActions(final Marker marker, final View infoView) {
        final Gson gson = new Gson();
        final Complaint complaint = gson.fromJson(marker.getTitle(), Complaint.class);

        if (!complaint.getStatus().equals("DENOUNCED")) {
            AlertDialog.Builder complaintActions = new AlertDialog.Builder(infoView.getContext());
            if (complaint.getDescription().isEmpty()) {
                complaintActions.setTitle(R.string.txt_empty_description);
            } else {
                complaintActions.setTitle(complaint.getDescription());
            }

            DialogInterface.OnClickListener inspectButton = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressBar.setVisibility(View.VISIBLE);
                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    apiService.inspectComplaints(complaint.getId(), loggedUser.getId()).enqueue(new Callback<Complaint>() {

                        @Override
                        public void onResponse(Call<Complaint> call, Response<Complaint> response) {
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.complaint_inspected));
                            marker.setTitle(gson.toJson(response.body()));
                            marker.hideInfoWindow();
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(infoView.getContext(), R.string.msn_inpect_success, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<Complaint> call, Throwable throwable) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(infoView.getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };
            DialogInterface.OnClickListener checkButton = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressBar.setVisibility(View.VISIBLE);
                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    apiService.checkComplaints(complaint.getId()).enqueue(new Callback<Complaint>() {

                        @Override
                        public void onResponse(Call<Complaint> call, Response<Complaint> response) {
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.complaint_checked));
                            marker.setTitle(gson.toJson(response.body()));
                            marker.hideInfoWindow();
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(infoView.getContext(), R.string.msn_check_success, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<Complaint> call, Throwable throwable) {
                            marker.hideInfoWindow();
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(infoView.getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };
            DialogInterface.OnClickListener denounceButton = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressBar.setVisibility(View.VISIBLE);
                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    apiService.denounceComplaints(complaint.getId()).enqueue(new Callback<Complaint>() {

                        @Override
                        public void onResponse(Call<Complaint> call, Response<Complaint> response) {
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.complaint_denounced));
                            marker.setTitle(gson.toJson(response.body()));
                            marker.hideInfoWindow();
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(infoView.getContext(), R.string.msn_report_success, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<Complaint> call, Throwable throwable) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(infoView.getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };

            switch (complaint.getStatus()) {
                case "STARTED":
                    if ((loggedUser.getInspector() == 1) && (!complaint.getUser().getId().equals(loggedUser.getId()))) {
                        complaintActions.setNeutralButton(R.string.btn_inspect, inspectButton);
                    }
                    complaintActions.setNegativeButton(R.string.btn_report, denounceButton);
                    break;
                case "INSPECTED":
                    if (complaint.getUser().getId().equals(loggedUser.getId())) {
                        complaintActions.setNeutralButton(R.string.btn_check, checkButton);
                    }
                    complaintActions.setNegativeButton(R.string.btn_report, denounceButton);
                    break;
                default:
                    complaintActions.setNegativeButton(R.string.btn_report, denounceButton);
            }

            complaintActions.show();
        }
    }
}