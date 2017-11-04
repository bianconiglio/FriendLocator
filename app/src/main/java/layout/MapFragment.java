package layout;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import aliona.mah.se.friendlocator.R;
import aliona.mah.se.friendlocator.beans.Member;
import aliona.mah.se.friendlocator.interfaces.MapFragmentCallback;
import aliona.mah.se.friendlocator.beans.Group;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {
    private final static String TAG = MapFragment.class.getName();
    private static final String GROUP = "map_group";
    private final int MY_PERMISSIONS_REQUEST_GPS = 5555;
    private boolean mPermissionGranted = false;
    private MapView mMapView;
    private GoogleMap mMap;
    private Group mGroup;
    private ArrayList<Member> members;
    private LatLng mMyPositon;
    private MapFragmentCallback callback;


    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance(Group group) {
        MapFragment frag = new MapFragment();
        Bundle args = new Bundle();
        args.putParcelable(GROUP, group);
        frag.setArguments(args);
        return frag;
    }

    public void setGroup(Group group) {
        mGroup = group;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mGroup = args.getParcelable(GROUP);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "ON CREATE VIEW");

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        // TODO: call with fragment manager and remove when the fragment is destroyed
        mMapView = view.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "ON ATTACH");
        super.onAttach(context);
        if (context instanceof MapFragmentCallback) {
            callback = (MapFragmentCallback) context;
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "ON RESUME");
        super.onResume();
        mMapView.onResume();
    }

    public void updateLocations() {
        addMarkers();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.i(TAG, "MAP IS READY");

        addSelf();

        Log.i(TAG, "ABOUT TO ADD MARKERS");
        addMarkers();

    }

    private void addSelf() {
        if (ActivityCompat.checkSelfPermission(this.getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_GPS);
        }
        if (mPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    private void addMarkers() {

        if (mMap == null) {
            return;
        }

        mMap.clear();

        mMyPositon = callback.requestLocationUpdate();
        members = callback.requestMembersUpdate(mGroup.getGroupName());

        Log.d(TAG, "ADDING MARKERS " + mMyPositon.toString());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mMyPositon));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(8.0f)); // should be between min = 2.0 and max = 21.0

        if (members == null || members.size() == 0) {
            Log.d(TAG, "LOCATIONS ARE NULL OR EMPTY");
            return;
        }

        for (Member member : members) {
            Log.d(TAG, "ADDING TO MAP");
            Log.d(TAG, member.getMemberName() + " " + member.getLongitude());
            double longitude, latitude;

            if (member.getLatitude() == null || member.getLongitude() == null) {
                continue;
            }

            try {
                longitude = Double.parseDouble(member.getLongitude());
                latitude = Double.parseDouble(member.getLatitude());
            } catch (NumberFormatException locationNonAvailable) {
                Log.d(TAG, locationNonAvailable.toString());
                continue;
            }

            LatLng memberPosition = new LatLng(latitude, longitude);

            Marker marker = mMap.addMarker( new MarkerOptions()
                    .position(memberPosition)
                    .title(member.getMemberName())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin_circle_black_48dp))
                    .snippet("Group:" + mGroup.getGroupName()
            ));

            Log.d(TAG, "ADD TO MAP SUCCESS");
            marker.showInfoWindow();
        }
    }

    /**
     * Method that is called after the user has decided whether to grant permission to access device location or not.
     * @param requestCode -- the final int identifying the request.
     * @param permissions -- the type of permission that user was asked for
     * @param grantResults -- the results, i.e. user's decision
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_GPS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissionGranted = true;
                } else {
                    mPermissionGranted = false;
                }
                return;
            }
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "ON PAUSE");
        super.onPause();
        if (mMap != null) {
            mMap.clear();
        }
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ON DESTROY");
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
