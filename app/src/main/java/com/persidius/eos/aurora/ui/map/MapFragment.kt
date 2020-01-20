package com.persidius.eos.aurora.ui.map

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.databinding.FragmentMapBinding


class MapFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    private lateinit var viewModel: MapViewModel
    private var sessionId: Int? = null

    private var mapView: MapView? = null
    private val mapToken = "pk.eyJ1IjoiYWxleG5pY3VsYSIsImEiOiJjazVsNG5tejAwMWZzM3FxaTZqZW1qeWIzIn0.a0suQh0bx3TQRT4oGIuNdA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(activity?.applicationContext!!, mapToken)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = activity?.findViewById(R.id.mapView) as MapView?
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val binding = DataBindingUtil.inflate<FragmentMapBinding>(inflater, R.layout.fragment_map, container, false)
        binding.lifecycleOwner = this
        binding.model = MapViewModel()
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
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
