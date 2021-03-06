package ucd.app.views.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import ucd.app.R;
import ucd.app.entities.Complaint;
import ucd.app.entities.ComplaintPhoto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class InfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View infoWindow;

    public InfoWindowAdapter(View view) {
        infoWindow = view;
    }

    @Override
    public View getInfoWindow(final Marker marker) {
        if (marker.isInfoWindowShown()) {
            marker.hideInfoWindow();
            marker.showInfoWindow();
        }
        return null;
    }

    @Override
    public View getInfoContents(final Marker marker) {
        updateInfoWindow(marker);
        return infoWindow;
    }

    private void updateInfoWindow(final Marker marker) {
        final Gson gson = new Gson();
        final Complaint complaint = gson.fromJson(marker.getTitle(), Complaint.class);

        TextView complaintTitle = (TextView) infoWindow.findViewById(R.id.complaint_title);
        TextView complaintDescription = (TextView) infoWindow.findViewById(R.id.complaint_description);
        ImageView complaintImage1 = (ImageView) infoWindow.findViewById(R.id.complaint_image1);
        ImageView complaintImage2 = (ImageView) infoWindow.findViewById(R.id.complaint_image2);
        ImageView complaintImage3 = (ImageView) infoWindow.findViewById(R.id.complaint_image3);

        if (complaint.getStatus().equals("DENOUNCED")) {
            complaintTitle.setText(R.string.msn_complaint_reported);
            complaintDescription.setText(R.string.msn_complaint_denounced);
            complaintImage1.setImageBitmap(null);
            complaintImage2.setImageBitmap(null);
            complaintImage3.setImageBitmap(null);
        } else {
            complaintTitle.setText(R.string.complaint_title);
            complaintDescription.setText(complaint.getDescription());
            Picasso picasso = Picasso.with(infoWindow.getContext());
            List<String> complaintPhotos = new ArrayList<>();

            for (ComplaintPhoto complaintPhoto : complaint.getComplaintPhotos()) {
                complaintPhotos.add(complaintPhoto.getPath() + complaintPhoto.getName() + complaintPhoto.getExtension());
            }

            try {
                picasso.load(complaintPhotos.get(0)).into(complaintImage1, new Callback() {
                    @Override
                    public void onSuccess() {
                        if (marker.isInfoWindowShown()) {
                            marker.hideInfoWindow();
                            marker.showInfoWindow();
                        }
                    }

                    @Override
                    public void onError() {}
                });
            } catch (Exception exception) {
                complaintImage1.setImageBitmap(null);
            }

            try {
                picasso.load(complaintPhotos.get(1)).into(complaintImage2, new Callback() {
                    @Override
                    public void onSuccess() {
                        if (marker.isInfoWindowShown()) {
                            marker.hideInfoWindow();
                            marker.showInfoWindow();
                        }
                    }

                    @Override
                    public void onError() {}
                });
            } catch (Exception exception) {
                complaintImage2.setImageBitmap(null);
            }

            try {
                picasso.load(complaintPhotos.get(2)).into(complaintImage3, new Callback() {
                    @Override
                    public void onSuccess() {
                        if (marker.isInfoWindowShown()) {
                            marker.hideInfoWindow();
                            marker.showInfoWindow();
                        }
                    }

                    @Override
                    public void onError() {}
                });
            } catch (Exception exception) {
                complaintImage3.setImageBitmap(null);
            }
        }
    }
}