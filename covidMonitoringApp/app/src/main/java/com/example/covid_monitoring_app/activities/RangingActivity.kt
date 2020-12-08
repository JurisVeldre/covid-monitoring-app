package com.example.covid_monitoring_app.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.covid_monitoring_app.R
import com.example.covid_monitoring_app.Singleton
import com.example.covid_monitoring_app.objects.PeopleCountObject
import com.example.covid_monitoring_app.objects.ReadingObject
import com.example.covid_monitoring_app.objects.RoomObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.altbeacon.beacon.*
import org.json.JSONObject
import java.util.zip.Inflater


class RangingActivity : AppCompatActivity(), BeaconConsumer {
    private val REQUEST_LOCATION_PERMISSION = 2018
    private var beaconManager: BeaconManager? = null
    private var noneCount = 0
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var closestBeacon: Beacon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        database = Firebase.database.reference
        setContentView(R.layout.activity_readings)
        findViewById<Button>(R.id.reportCountButton).setOnClickListener { displayPeopleCountAlert() }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.logout_button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        checkLocationPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager!!.unbind(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }

    private fun sendGet() {
        val url = "http://a6e58215657a.ngrok.io/institutions/5fc0e64967ab055ee9422dd3/rooms/5fc10cd967ab055ee9422de2/api"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                updateUI(parseJson(response.getJSONObject("measurements")), getRoomName(response))
            },
            Response.ErrorListener { error ->
                Log.i(TAG, error.toString())
            }
        )

        // Access the RequestQueue through your singleton class.
        Singleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    private fun parseJson(jsonObj: JSONObject): ReadingObject {
        return ReadingObject(
            atmosphericPressure = jsonObj.getInt("atmosphericpressure"),
            co2 = jsonObj.getString("co2"),
            humidity = jsonObj.getString("humidity"),
            temperature = jsonObj.getDouble("temperature")
        )
    }

    private fun getRoomName(response: JSONObject): String {
        return response.getJSONObject("room_data").getString("name")
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(readings: ReadingObject, roomName: String) {
        val pplLabel = findViewById<TextView>(R.id.peopleCountLabel)
        val co2Label = findViewById<TextView>(R.id.co2CountLabel)
        val temperatureLabel = findViewById<TextView>(R.id.temperatureCountLabel)
        val humidityLabel = findViewById<TextView>(R.id.moistureCountLabel)
        val pressureLabel = findViewById<TextView>(R.id.pressureCountLabel)

        co2Label.text = readings.co2 + "ppm"
        temperatureLabel.text = readings.temperature.toString() + " °C"
        humidityLabel.text = readings.humidity + "%"
        pressureLabel.text = readings.atmosphericPressure.toString() + "Pa"
        supportActionBar?.title = roomName
    }

    private fun displayPeopleCountAlert() {
        val pickerDialog = LayoutInflater
            .from(this)
            .inflate(R.layout.picker_dialog, null)
        val picker = pickerDialog.findViewById<NumberPicker>(R.id.picker)
        picker.maxValue = 100
        picker.minValue = 0

        val dialog = AlertDialog.Builder(this).create()
        pickerDialog
            .findViewById<Button>(R.id.sendButton)
            .setOnClickListener {
                reportPeopleCountInRoom(picker.value)
                dialog.dismiss()
        }
        dialog.setTitle("Report people count")
        dialog.setMessage("Please, input how many people are in the room with you")
        dialog.setView(pickerDialog)
        dialog.show()
    }

    private fun reportPeopleCountInRoom(count: Int) {
        val userId = auth.currentUser?.uid
        val peopleCountObject = PeopleCountObject(count)
        database
            .child("rooms")
            .child(closestBeacon?.id1.toString())
            .child("reports")
            .child(userId.toString())
            .setValue(peopleCountObject)
    }

    private fun checkLocationPermission() {
        if (isAboveMarshmallow()) {
            when {
                isLocationPermissionEnabled() -> initBLE()
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) -> displayRationale()
                else -> requestLocationPermission()
            }
        } else {
            initBLE()
        }
    }

    private fun displayRationale() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.location_permission_disabled))
            .setPositiveButton(getString(R.string.ok)
            ) { _, _ -> requestLocationPermission() }
            .setNegativeButton(getString(R.string.cancel)
            ) { _, _ -> }
            .show()
    }

    private fun initBLE() {
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout(" m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        beaconManager!!.bind(this)
    }

    private fun isLocationPermissionEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_LOCATION_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (permissions.size != 1 || grantResults.size != 1) {
                    throw RuntimeException("Error on requesting location permission.")
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initBLE()
                } else {
                    Toast.makeText(this,
                        R.string.location_permission_not_granted,
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isAboveMarshmallow(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun parseBeacons(beacons: (Array<Beacon>)) {
        val closestBeacon = beacons.minBy { it.distance }
        Log.i(TAG, "Closest beacon: $closestBeacon")
        noneCount = 0
        if (closestBeacon != this.closestBeacon) {
            if (this.closestBeacon != null) { sendLocation(closestBeacon) }
            this.closestBeacon = closestBeacon
        }
    }

    private fun sendLocation(closestBeacon: Beacon?) {
        val userId = auth.currentUser?.uid
        val roomObject =
            RoomObject(roomId = closestBeacon?.id1.toString())
        database.child("users").child(userId.toString()).setValue(roomObject)
    }

    override fun onBeaconServiceConnect() {
        beaconManager!!.removeAllRangeNotifiers()
        beaconManager!!.addRangeNotifier { beacons, _ ->
            if (noneCount >= 9) {
                parseBeacons(emptyArray<Beacon>())
            } else {
                if (beacons.isNotEmpty()) {
                    parseBeacons(beacons.toTypedArray())
                    Log.i(TAG, "Beacons i see: $beacons")
                } else {
                    noneCount++
                }
            }
        }
        try {
            beaconManager!!.startRangingBeaconsInRegion(
                Region("myRangingUniqueId", null, null, null)
            )
        } catch (e: RemoteException) {
        }
    }
    companion object {
        protected const val TAG = "RangingActivity"
    }
}