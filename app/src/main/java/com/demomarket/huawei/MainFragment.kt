package com.demomarket.huawei

import android.Manifest
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.LocationServices
import com.huawei.hms.maps.*
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.site.api.SearchResultListener
import com.huawei.hms.site.api.SearchServiceFactory
import com.huawei.hms.site.api.model.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.roundToInt

class MainFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
        val placeList: MutableList<String> =
            mutableListOf("Shopping", "School", "Restaurant", "Police Station", "Health")
        private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private var hmap: HuaweiMap? = null
        private var mMarker: Marker? = null
        private var lat: Double = 0.0
        private var lng: Double = 0.0

        private var PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        private var PERMISSION_ALL = 1
        //  var hwLocationType: HwLocationType? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestPermissions(PERMISSIONS, PERMISSION_ALL)

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }

        mFusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(activity)
        getLastLocation()

        MapsInitializer.setApiKey("CgB6e3x9zTEbOc2zeKDBJ6VkletweXCTBEV3pi53kMsOPuyhP8s8szPIp5zgeeB0JfeWaptPegsCEbluva4rOMiq")
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this);

        //spinner
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, placeList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        placeSpinner.adapter = adapter

    }

    private fun getLastLocation() {
        try {
            val lastLocation =
                mFusedLocationProviderClient.lastLocation
            lastLocation.addOnSuccessListener(OnSuccessListener { location ->
                if (location == null) {
                    Log.i(ContentValues.TAG, "getLastLocation onSuccess location is null")
                    return@OnSuccessListener
                }
                lat = location.latitude
                lng = location.longitude
                val latLng = LatLng(lat, lng)
                hmap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
                mMarker = hmap!!.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lng))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_where_to_vote_24))
                        .title("You are here!")
                )
                Log.i(
                    ContentValues.TAG,
                    "getLastLocation onSuccess location[Longitude,Latitude]:${location.longitude},${location.latitude}"
                )
                return@OnSuccessListener
            }).addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "getLastLocation onFailure:${e.message}")
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "getLastLocation exception:${e.message}")
        }
    }

    fun getSites(hwLocationType: HwLocationType) {
        val searchService = SearchServiceFactory.create(
            activity,
            "CgB6e3x9zTEbOc2zeKDBJ6VkletweXCTBEV3pi53kMsOPuyhP8s8szPIp5zgeeB0JfeWaptPegsCEbluva4rOMiq"
        )
        val request = NearbySearchRequest()
        val location = Coordinate(lat, lng)
        request.location = location
        request.query = "Istanbul"
        request.radius = 5000
        request.hwPoiType = hwLocationType
        request.language = "en"
        request.pageIndex = 1
        request.pageSize = 5

        val resultListener: SearchResultListener<NearbySearchResponse?> =
            object : SearchResultListener<NearbySearchResponse?> {
                override fun onSearchResult(results: NearbySearchResponse?) {
                    if (results == null || results.totalCount <= 0) {
                        return
                    }
                    val sites: MutableList<Site> = results.sites
                    if (!(sites.size !== 0)) {
                        return
                    }
                    for (site in sites) {
                        Log.i(
                            "TAG",
                            java.lang.String.format(
                                "siteId: '%s', name: %s\r\n",
                                site.siteId,
                                site.name
                            )
                        )
                        mMarker = hmap!!.addMarker(
                            MarkerOptions()
                                .position(LatLng(site.location.lat, site.location.lng))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_location_on_24))
                                .title(site.name + " " + String.format("%.1f", site.distance / 1000.0)+ "km away")
                        )
                        val currentLocation = LatLng(site.location.lat, site.location.lng)
                        hmap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 11.0f))
                    }
                }

                override fun onSearchError(status: SearchStatus) {
                    Log.i(
                        "TAG",
                        "Error site : " + status.errorCode
                            .toString() + " " + status.errorMessage
                    )
                }
            }
        searchService.nearbySearch(request, resultListener)
    }

    override fun onMapReady(map: HuaweiMap?) {
        hmap = map;
        placeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                when (p0?.getItemAtPosition(p2).toString()) {
                    "Shopping" -> getSites(HwLocationType.SHOPPING)
                    "School" -> getSites(HwLocationType.SCHOOL)
                    "Restaurant" -> getSites(HwLocationType.RESTAURANT)
                    "Police Station" -> getSites(HwLocationType.POLICE_STATION)
                    "Health" -> getSites(HwLocationType.HEALTH_CARE)
                    else -> {
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                getSites(HwLocationType.SHOPPING)
                Toast.makeText(
                    requireContext(),
                    "Please Select a Place Name",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}