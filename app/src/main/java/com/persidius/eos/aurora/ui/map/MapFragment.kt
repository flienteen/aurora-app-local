package com.persidius.eos.aurora.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.database.Database
import com.persidius.eos.aurora.database.entities.Task
import com.persidius.eos.aurora.databinding.FragmentMapBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class MapFragment : Fragment() {

    private val mapToken = "pk.eyJ1IjoiYWxleG5pY3VsYSIsImEiOiJjazVsNG5tejAwMWZzM3FxaTZqZW1qeWIzIn0.a0suQh0bx3TQRT4oGIuNdA"

    private var listener: OnFragmentInteractionListener? = null

    private lateinit var viewModel: MapViewModel
    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private val searchRadius = 5.0 // 5.0 degrees is roughly 600km

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(activity?.applicationContext!!, mapToken)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val binding = DataBindingUtil.inflate<FragmentMapBinding>(inflater, R.layout.fragment_map, container, false)
        binding.lifecycleOwner = this
        binding.model = MapViewModel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = activity?.findViewById(R.id.mapView) as MapView
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { mapBoxMap ->
            this.map = mapBoxMap
            this.map.setStyle(Style.MAPBOX_STREETS) {
            }
            showNearbyLocations()
        }
    }


    private fun showNearbyLocations() {
        val activity = activity as MainActivity
        searchTasksByLocation(activity.lat, activity.lng, searchRadius)
    }

    @SuppressLint("CheckResult")
    private fun searchTasksByLocation(lat: Double, lng: Double, radius: Double) {
        Log.d("SearchByLocation", "[Lat: $lat, Lng: $lng, Radius: $radius]")
        Database.task.searchByLocation(lat, lng, radius)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ results ->
                Log.d("SearchByLocation", "${results.size} results")
                val latLngs = tasksToLatLngs(results)
                latLngs.forEach { latLng ->
                    map.addMarker(MarkerOptions().position(latLng))
                }
                zoomLatLngs(latLngs)
            }, { t -> Log.e("SearchByLocation", "Search for task by location errored", t) })
    }

    private fun zoomLatLngs(latLngs: List<LatLng>) {
        val latLngBounds = LatLngBounds.Builder().includes(latLngs).build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 50))
    }

    private fun tasksToLatLngs(tasks: List<Task>): List<LatLng> {
        val latLngs = mutableListOf<LatLng>()
        tasks.forEach { task ->
            if (task.posLat != null && task.posLng != null) {
                latLngs.add(LatLng(task.posLat, task.posLng))
            }
        }
        return latLngs
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.task_map_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
}
