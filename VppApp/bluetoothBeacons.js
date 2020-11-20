import React, { useEffect, useRef, useState } from 'react'
import Beacons from 'react-native-beacons-manager'
import { PermissionsAndroid, DeviceEventEmitter } from 'react-native';

const BluetoothBeacons = ({setBeacons}) => {
  const beacons = useRef([])
  const [firstTime, setFirstTime] = useState(true)
  const beaconScanCount = useRef(null)

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
      if (beaconScanCount.current >= 10) {
        beaconScanCount.current = 0
        checkRoom()
      } else {
        beaconScanCount.current = beaconScanCount.current + 1
        beacons.current = beacons.current.concat(data.beacons)
        console.log(beaconScanCount.current)
      }
    })
  }

  const checkRoom = () => {
    console.log(beacons.current)
    var result = beacons.current.reduce((unique, o) => {
      if(!unique.some(obj => obj.uuid === o.uuid)) {
        unique.push(o);
      }
      return unique;
    },[]);

    var shortestDistance = 100
    var bestBeacon
    result.forEach(beacon => { 
        if (beacon.distance < shortestDistance) {
          shortestDistance = beacon.distance
          bestBeacon = beacon
        }
    })
    
    beacons.current = []
    setBeacons(bestBeacon)
  }

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