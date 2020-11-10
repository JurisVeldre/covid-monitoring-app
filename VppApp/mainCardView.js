import React, { useEffect, useState } from 'react';
import { StyleSheet, View, SafeAreaView, Dimensions, Alert } from 'react-native';
import {Button, Text, Icon} from 'native-base'
import CardView from 'react-native-cardview';
import io from 'socket.io-client'
import BluetoothBeacons from './bluetoothBeacons'

const windowHeight = Dimensions.get('window').height;

const TempUUIDArray = [
  {
    major: 0,
    roomName: 'Auditorium 14',
    readings: {
      pplCount: 5,
      co2: 1120,
      temp: 21,
      humidity: 20
    },
  },
  {
    major: 1,
    roomName: 'Auditorium 12',
    readings: {
      pplCount: 20,
      co2: 2254,
      temp: 26,
      humidity: 40
    }
  }  
]

export default MainCardView = () => {
  const [socket, setSocket] = useState()
  const [readings, setReadings] = useState()
  const [beacons, setBeacons] = useState()
  const [roomName, setRoomName] = useState()

  useEffect(() => {
    // setSocket(io("IP_ADDRESS:PORT"))
    // socket.on((newReadings) => {

    // })
    
  }, [])

  const searchArr = (major, array) => {
    for (var i=0; i < array.length; i++) {
      if (array[i].major === major) {
        return array[i];
      }
    }
  }

  const updateBeacons = (beacons) => {
    console.log(beacons)
    setRoomName(beacons ? searchArr(beacons.major, TempUUIDArray).roomName : null)
    setReadings(beacons ? searchArr(beacons.major, TempUUIDArray).readings : null)  
  }

  const showPeopleCountAlert = () => {
    Alert.alert("How many people are in the room with you",
                "Only count the other people",
                [
                  {
                    text: 'Quite a lot (20+)',
                    onPress: () => sendPplCount("20+")
                  },
                  {
                    text: 'Kinda many (10-20)',
                    onPress: () => sendPplCount("10-20")
                  },
                  {
                    text: 'Almost noone (1-10)',
                    onPress: () => sendPplCount("1-10")
                  },
                ], 
                {
                  cancelable: true
                }
              )
  }

  const sendPplCount = (peopleCount) => {
    console.log(`socket.emit('peopleCount', ${peopleCount})`)
  }

    return (
      <SafeAreaView style={styles.safeAreaView}>
        <BluetoothBeacons setBeacons={(beacons) => updateBeacons(beacons)}/>
        <View style={styles.container}>
          <View style={styles.labelView}>
            <View style={styles.topLabel}>
              <Text style={styles.labelText}>{roomName ? 'Connected' : 'Not Connected'}</Text>
            </View>
            <View style={styles.bottomLabel}>
            <Text style={styles.labelText}>{roomName ? roomName : '...'}</Text>
            </View>
          </View>
          <View flexDirection="row">
            <CardView
              cardElevation={5}
              cardMaxElevation={5}
              cornerRadius={5}
              style={styles.card}
            >
              <View style={styles.cardView}>
                <Text style={styles.cardTitle}>People count in room:</Text>
                <Text style={styles.cardText} >{readings ? readings.pplCount : "..."}</Text>
              </View>
            </CardView>
            <CardView
              cardElevation={5}
              cardMaxElevation={5}
              cornerRadius={5}
              style={styles.card}
            >
              <View style={styles.cardView}>
                <Text style={styles.cardTitle}>Co2 in room:</Text>
                <Text style={styles.cardText}>{readings ? readings.co2 : "..."}ppm</Text>
              </View>
            </CardView>
          </View>
          <View flexDirection="row">
            <CardView
              cardElevation={5}
              cardMaxElevation={5}
              cornerRadius={5}
              style={[styles.card, {flexDirection:'row'}]}
            >
              <View style={styles.cardView}>
                <Text style={styles.cardTitle}>Room temperature:</Text>
                <Text style={styles.cardText}>{readings ? readings.temp : "..."}°C</Text>
              </View>
            </CardView>
            <CardView
              cardElevation={3}
              cardMaxElevation={3}
              cornerRadius={5}
              style={styles.card}
            >
              <View style={styles.cardView}>
                <Text style={styles.cardTitle}>Humidity in room:</Text>
                <Text style={styles.cardText}>{readings ? readings.humidity : "..."}%</Text>
              </View>
            </CardView>
          </View>
          <Button 
            warning 
            block
            disabled={!readings}
            style={styles.pplCountButton}
            onPress={showPeopleCountAlert}>
            <Text>Report count</Text>
          </Button>
        </View>
      </SafeAreaView>
    );
  }

const styles = StyleSheet.create({
  safeAreaView: {
    flex: 1
  },
  container: {
    flex: 1,
    backgroundColor: "#fbfff2"
  },
  pplCountButton: {
    margin: 24,
  },
  labelView: {
    height:windowHeight/4 - 25, 
    justifyContent:'center'
  },
  card: {
    backgroundColor: '#DDE9D1',
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'center',
    flex: 1,
    margin: 5
  },
  cardView: {
    height: windowHeight/4
  },
  cardTitle: {
    textAlign: 'center',
    margin: 10,
  },
  topLabel: {
    marginRight:25, 
    marginLeft:25, 
    marginBottom: 10, 
    borderWidth:1,
    borderRadius:5,
    backgroundColor: "#DDE9D1",
  },
  bottomLabel: {
    marginRight:25, 
    marginLeft:25, 
    marginTop: 10, 
    borderWidth:1, 
    borderRadius:5, 
    backgroundColor: "#DDE9D1"
  },
  labelText: {
    textAlign: 'center'
  },
  cardText: {
    textAlign:'center', 
    fontWeight:"bold", 
    fontSize:40,
  },
});