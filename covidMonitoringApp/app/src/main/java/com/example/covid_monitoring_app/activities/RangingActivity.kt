package com.example.covid_monitoring_app.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ScaleDrawable
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
import androidx.core.graphics.drawable.toBitmap
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.covid_monitoring_app.R
import com.example.covid_monitoring_app.Singleton
import com.example.covid_monitoring_app.objects.PeopleCountObject
import com.example.covid_monitoring_app.objects.ReadingObject
import com.example.covid_monitoring_app.objects.RoomIDObject
import com.example.covid_monitoring_app.objects.RoomObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.altbeacon.beacon.*
import org.json.JSONObject
import java.util.zip.Inflater


class RangingActivity : AppCompatActivity(), BeaconConsumer {
    private val REQUEST_LOCATION_PERMISSION = 2018
    private var beaconManager: BeaconManager? = null
    private var region = Region("myRangingUniqueId", null, null, null)
    private var noneCount = 0
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var closestBeacon: Beacon? = null
    private var refreshCounter = 0
    private var roomList = mutableListOf<RoomIDObject>()
    private var shouldStopPosting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        database = Firebase.database.reference
        setContentView(R.layout.activity_readings)
        findViewById<Button>(R.id.reportCountButton).setOnClickListener { displayPeopleCountAlert() }
        getRoomIdList()
        setupToolbar()
        checkLocationPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager!!.stopRangingBeaconsInRegion(region)
        beaconManager!!.removeAllMonitorNotifiers()
        beaconManager!!.unbind(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        showLogoutDialog()
        return true
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout?")
            .setMessage("You're about to logout, are you sure?")
            .setPositiveButton("Yes"
            ) { _, _ -> logout() }
            .setNegativeButton(getString(R.string.cancel)
            ) { _, _ -> }
            .show()
    }

    private fun logout() {
        setRoomNull()
        finish()
    }

    private fun setRoomNull() {
        val uid = auth.currentUser?.uid.toString()
        val roomObject =
            RoomObject("null")
        database
            .child("users")
            .child(uid)
            .setValue(roomObject)
            .addOnCompleteListener {
            auth.signOut()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.logout_button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun getRoomIdList() {
        database.ref.child("roomIDs")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val a = RoomIDObject(it.key.toString(), it.child("name").value.toString())
                        roomList.add(a)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.i(TAG, "error: " + error.message)
                }
            })
    }

    private fun getData() {
        val url = "http://0514485d9f81.ngrok.io/institutions/5fc0e64967ab055ee9422dd3/rooms/5fc10cd967ab055ee9422de2/api"

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

    private fun checkRoomList(closestBeacon: Beacon?): Boolean {
        var contains = false
        roomList.forEach {
            if (it.id == closestBeacon?.id1.toString()) contains = true
        }
        return contains
    }


    private fun parseBeacons(beacons: (Array<Beacon>)) {
        val closestBeacon = beacons.minBy { it.distance }
        Log.i(TAG, "Closest beacon: $closestBeacon")
        noneCount = 0
        if (closestBeacon == null || checkRoomList(closestBeacon)) {
            if (closestBeacon != this.closestBeacon) {
                sendLocation(closestBeacon)
                this.closestBeacon = closestBeacon
                getData()
                refreshCounter = 0
            }
            if (refreshCounter > 59) {
                Toast.makeText(
                    this,
                    "Refreshing data",
                    Toast.LENGTH_LONG
                ).show()
                getData()
                refreshCounter = 0
            }
            refreshCounter++
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
            if (!shouldStopPosting) {
                if (noneCount >= 9 && closestBeacon != null) {
                    parseBeacons(emptyArray())
                } else {
                    if (beacons.isNotEmpty()) {
                        parseBeacons(beacons.toTypedArray())
                    } else {
                        noneCount++
                    }
                }
            }
        }
        try {
            beaconManager!!.startRangingBeaconsInRegion(region)
        } catch (e: RemoteException) {
        }
    }
    companion object {
        private const val TAG = "RangingActivity"
    }
}