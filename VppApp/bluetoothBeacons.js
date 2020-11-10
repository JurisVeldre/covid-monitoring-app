import React, { useEffect, useState } from 'react'
import Beacons from 'react-native-beacons-manager'
import { PermissionsAndroid, DeviceEventEmitter } from 'react-native';

const BluetoothBeacons = ({setBeacons}) => {
  const [beacons, setListBeacons] = useState([])
  const [firstTime, setFirstTime] = useState(true)

  useEffect(() => {
    if (firstTime) {
      requestCourseLocationPermission()
      addListener()
      Beacons.detectIBeacons()
      setFirstTime(false)
    }
  }, []);

  const addListener = () => {
    DeviceEventEmitter.addListener('beaconsDidRange', (data) => {
      if (!arraysEqual(beacons, data.beacons) || data.beacons.length === 0) {
        checkRoom(data.beacons)
        setListBeacons(data.beacons)
      }
    })
  }

  const checkRoom = (beacons) => {
    //beacons is array
    var shortestDistance = 100
    var bestBeacon
    beacons.forEach(beacon => { 
        if (beacon.distance < shortestDistance) {
          shortestDistance = beacon.distance
          bestBeacon = beacon
        }
    })
    setBeacons(bestBeacon)
      //   [{"distance": 1.099394345972351, "major": 0, "minor": 0, "proximity": "near", "rssi": -75, "uuid": "ebf26430-fcb4-4370-9ff2-2236bf9567e9"}, {"distance": 1.3915545315304618, "major": 0, "minor": 1, "proximity": "near", "rssi": -76, "uuid": "f9a5ed19-ce00-495b-b715-cda1478ec9a1"}, {"distance": 1.0215479253994402, "major": 0, "minor": 0, "proximity": "near", "rssi": -70, "uuid": "7623f849-2d5a-4bbf-9d45-3efcddceff3b"}]
      // })
  }

  const objectsEqual = (o1, o2) => typeof o1 === 'object' && Object.keys(o1).length > 0 ? Object.keys(o1).length === Object.keys(o2).length && Object.keys(o1).every(p => objectsEqual(o1[p], o2[p])) : o1 === o2;

  const arraysEqual = (a1, a2) => a1.length === a2.length && a1.every((o, idx) => objectsEqual(o, a2[idx]));

  const requestCourseLocationPermission = async () => {
    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
        {
          title: "Bluetooth beacons permission",
          message:
            "This app needs access to your location " +
            "so you can take connect to room beacons.",
          buttonNegative: "Cancel",
          buttonPositive: "OK"
        }
      );
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        console.log("starting ranging")
        rangeBeacons()
      } else {
        console.log("location permission denied");
      }
    } catch (err) {
      console.warn(err);
    }
  };

  const rangeBeacons = async () => {
    try {
      await Beacons.startRangingBeaconsInRegion('REGION1')
      console.log(`Beacons ranging started succesfully!`)
    } catch (err) {
      console.log(`Beacons ranging not started, error: ${err}`)
    }
  };
  return (
    <></>
  );
}

export default BluetoothBeacons;