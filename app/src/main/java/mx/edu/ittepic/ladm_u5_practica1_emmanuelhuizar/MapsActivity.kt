package mx.edu.ittepic.ladm_u5_practica1_emmanuelhuizar

import android.annotation.SuppressLint
import androidx.core.widget.addTextChangedListener
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    var baseRemota=FirebaseFirestore.getInstance()
    var posicion=ArrayList<Data>()
    lateinit var UbicacionCliente: FusedLocationProviderClient
    lateinit var locacion: LocationManager
    var dataLista=ArrayList<String>()
    var listaID=ArrayList<String>()

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        editText2.addTextChangedListener {
            if(editText2.text.toString()==""){
                button.setText("Todos")
            }else{
                button.setText("Buscar")
                editText2.setHint("Ingresa nombre del producto")
            }
        }

        button.setOnClickListener {
            actulizar(editText2.text.toString())
            mMap.clear()
            editText2.setText("")
        }

        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),1)
        }
        UbicacionCliente=LocationServices.getFusedLocationProviderClient(this)
        baseRemota.collection("tecnologico").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if(firebaseFirestoreException!=null){
                return@addSnapshotListener
            }

            posicion.clear()
            for (document in querySnapshot!!){
                var data=Data()
                data.nombre=document.getString("nombre").toString()
                data.posicion1=document.getGeoPoint("posicion1")!!
                data.posicion2=document.getGeoPoint("posicion2")!!
                posicion.add(data)
            }
        }
        locacion = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var oyente = Oyente(this)
        locacion.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,01f,oyente)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.minZoomLevel
        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        mMap.uiSettings.isZoomControlsEnabled=true
        mMap.uiSettings.isMyLocationButtonEnabled=true
        mMap.isMyLocationEnabled=true
        val Tec=LatLng(21.479839, -104.865482)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(Tec,17f))
        /*UbicacionCliente.lastLocation.addOnSuccessListener {it->
            if(it!=null){
                val posicionActual=LatLng(it.latitude,it.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual,17f))
            }
        }*/

    }

    private fun actulizar(s: String) {
        var dialogo= Dialog(this)
        dialogo.setContentView(R.layout.buscarlocacion)

        var lista = dialogo.findViewById<ListView>(R.id.lista)


        baseRemota.collection("tecnologico").orderBy("nombre").startAt(s).endAt(s+'\uf8ff')
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException!=null){
                    Toast.makeText(this,"No se pude realizar busqueda",Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for(document in querySnapshot!!){
                    var cadena = "Localizacion: "+document.getString("nombre")
                    dataLista.add(cadena)
                    listaID.add(document.id)
                }
                if(dataLista.size==0){
                    dataLista.add("No hay pedidos por entregar")
                }
                var adaptador = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,dataLista)
                lista.adapter=adaptador
            }
        lista.setOnItemClickListener { parent, view, position, id ->
            if(listaID.size==0){
                return@setOnItemClickListener
            }
            baseRemota.collection("tecnologico").document(listaID[position]).get()
                .addOnSuccessListener {
                    var nombre= it.getString("nombre")
                    var p = it.getGeoPoint("posicion1")!!.latitude
                    var p2 = it.getGeoPoint("posicion1")!!.longitude
                    var p3 = it.getGeoPoint("posicion2")!!.latitude
                    var p4 = it.getGeoPoint("posicion2")!!.longitude

                    var pf1 = (p3-p)/2
                    var pf2 = (p4-p2)/2

                    p=p+pf1
                    p2=p2+pf2

                    val casa = LatLng(p,p2)
                    mMap.addMarker(MarkerOptions().position(casa).title(nombre)).showInfoWindow()
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(casa,18f))

                }
                .addOnFailureListener {
                    Toast.makeText(this,"Error no hay conexion de red",Toast.LENGTH_LONG).show()
                }
            dialogo.dismiss()
        }
        dialogo.show()
    }

}



class Oyente(puntero:MapsActivity): LocationListener {
    var p=puntero
    override fun onLocationChanged(location: Location) {
        p.setTitle("Aun no llegas a alguna locacion del ITT")
        var geoPosicionGPS= GeoPoint(location.latitude, location.longitude)

        for(item in p.posicion){
            if(item.estoyen(geoPosicionGPS)){
                p.setTitle("Estas en: ${item.nombre}")
            }
        }
    }



    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }
}
