package com.example.covid_monitoring_app.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import kotlin.properties.Delegates
import kotlin.reflect.KProperty


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
    private var shouldSendNotif: Boolean by Delegates.observable(false) { _, _, newValue ->
        if (newValue) sendNotif()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        database = Firebase.database.reference
        setContentView(R.layout.activity_readings)
        findViewById<Button>(R.id.reportCountButton)
            .setOnClickListener { if (closestBeacon == null) showNoBeaconToast() else displayPeopleCountAlert() }
        getRoomIdList()
        setupToolbar()
        checkLocationPermission()
        showLocationPopup()
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

    override fun onBackPressed() {
        showLogoutDialog()
    }

    private fun showLocationPopup() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled)
            AlertDialog.Builder(this)
                .setTitle("Activate bluetooth")
                .setMessage("For this app to work bluetooth must be turned on")
                .setPositiveButton("Turn bluetooth on") { _, _ ->
                    mBluetoothAdapter.enable()
                    if (!gpsStatus)
                        AlertDialog.Builder(this)
                            .setTitle("GPS setting!")
                            .setMessage("GPS is not enabled, Do you want to go to settings menu? ")
                            .setPositiveButton("Setting") { _, _ ->
                                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                            .show();
                }
                .setNegativeButton(
                    getString(R.string.cancel)
                ) { _, _ -> }
                .show()
    }

    private fun showNoBeaconToast() {
        Toast.makeText(baseContext, "No room detected to report people in",
            Toast.LENGTH_SHORT).show()
    }

    private fun sendNotif() {
        val intent = Intent(this, this.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val title = "High risk for infection detected!"
        val text = "Please leave the room and let air circulate."
        val channelId = "Default"

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.danger_icon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "High Channel", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = builder.build()
        manager.notify(0, notification)
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
        supportActionBar?.title = "No Room Found"
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
        val url = "http://65dc018f42ed.ngrok.io/api/34653d34-d92b-40cf-9bb0-245f33abaec5"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                updateUI(parseJson(response.getJSONObject("measurements"), response.getJSONObject("beacon_data").getString("people_count").toInt()), getRoomName(response))
                shouldSendNotif = response.getBoolean("co2_limit") || response.getBoolean("humidity_limit")
            },
            Response.ErrorListener { error ->
                Log.i(TAG, error.toString())
            }
        )

        Singleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    private fun parseJson(jsonObj: JSONObject, peopleCount: Int): ReadingObject {
        return ReadingObject(
            atmosphericPressure = jsonObj.getInt("atmosphericpressure"),
            co2 = jsonObj.getString("co2"),
            humidity = jsonObj.getString("humidity"),
            temperature = jsonObj.getDouble("temperature"),
            peopleCount = peopleCount
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

        pplLabel.text = readings.peopleCount.toString() + " People"
        co2Label.text = readings.co2 +  " ppm"
        temperatureLabel.text = readings.temperature.toString() + " °C"
        humidityLabel.text = readings.humidity + "%"
        pressureLabel.text = readings.atmosphericPressure.toString() + " Pa"
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

    private fun resetUI() {
        val pplLabel = findViewById<TextView>(R.id.peopleCountLabel)
        val co2Label = findViewById<TextView>(R.id.co2CountLabel)
        val temperatureLabel = findViewById<TextView>(R.id.temperatureCountLabel)
        val humidityLabel = findViewById<TextView>(R.id.moistureCountLabel)
        val pressureLabel = findViewById<TextView>(R.id.pressureCountLabel)

        pplLabel.text = "..."
        co2Label.text = "..."
        temperatureLabel.text = "..."
        humidityLabel.text = "..."
        pressureLabel.text = "..."
        supportActionBar?.title = "..."
    }


    private fun parseBeacons(beacons: (Array<Beacon>)) {
        val closestBeacon = beacons.minBy { it.distance }
        Log.i(TAG, "Closest beacon: $closestBeacon")
        noneCount = 0
        if (!checkRoomList(closestBeacon)) {
            resetUI()
            sendLocation(null)
            this.closestBeacon = null
        }
        if (closestBeacon == null || checkRoomList(closestBeacon)) {
            if (closestBeacon != this.closestBeacon) {
                sendLocation(closestBeacon)
                this.closestBeacon = closestBeacon
                if (closestBeacon != null) getData() else resetUI()
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
        database.child("users")
            .child(userId.toString())
            .child("roomId")
            .setValue(closestBeacon?.id1.toString())
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