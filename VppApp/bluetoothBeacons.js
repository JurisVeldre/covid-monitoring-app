import { Body, Container, Header, Left, ListItem, Right, Title, Text } from 'native-base'
import React, { useEffect, useState } from 'react'
import Beacons from 'react-native-beacons-manager'
import { FlatList, StyleSheet, PermissionsAndroid, DeviceEventEmitter } from 'react-native';
import {objCompare, arraysEqual} from './objCompare'
const BluetoothBeacons = () => {

  useEffect(() => {
    // Tells the library to detect iBeacons
    Beacons.detectIBeacons()
    requestCameraPermission()

    DeviceEventEmitter.addListener('beaconsDidRange', (data) => {
      if (data.beacons !== beacons && data.beacons !== []) {
        setBeacons(data.beacons)
        console.log('Setting beacons to', data.beacons)
      }
    })
  }, []);

  const [beacons, setBeacons] = useState([])

  const requestCameraPermission = async () => {
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
  const renderItem = ({ item }) => (
    <ListItem>
      <Body>
        <Text>{item.uuid}</Text>
        <Text note>{item.proximity}</Text>
      </Body>
    </ListItem>
  );

  const extractKey = item => item.uuid

  return (
    <Container>
      <Header>
        <Left/>
        <Body>
          <Title>Beacon List</Title>
        </Body>
        <Right/>
      </Header>
    <FlatList
      data={beacons}
      renderItem={renderItem}
      keyExtractor={extractKey}/>
    </Container>
  );
}

const styles = StyleSheet.create({
});

export default BluetoothBeacons;