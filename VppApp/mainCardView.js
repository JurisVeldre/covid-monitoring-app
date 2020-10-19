import React from 'react';
import { StyleSheet, View, SafeAreaView, Dimensions } from 'react-native';
import {Icon, Text} from 'native-base'
import CardView from 'react-native-cardview';

const windowHeight = Dimensions.get('window').height;

export default MainCardView = () => {
    return (
      <SafeAreaView style={styles.safeAreaView}>
        <View style={styles.container}>
          <View style={{height:windowHeight/3 - 25, justifyContent:'center'}}>
            <View style={styles.topLabel}>
              <Text style={styles.labelText}>Connected</Text>
            </View>
            <View style={styles.bottomLabel}>
            <Text style={styles.labelText}>Auditorium 14</Text>
            </View>
          </View>
          <View flexDirection="row">
            <CardView
              cardElevation={5}
              cardMaxElevation={5}
              cornerRadius={5}
              style={styles.card}
            >
              <View style={{height:windowHeight/3 - 15}}>
                <Text style={styles.cardTitle}>People count in room:</Text>
                <Text style={styles.cardText} >25</Text>
              </View>
            </CardView>
            <CardView
              cardElevation={5}
              cardMaxElevation={5}
              cornerRadius={5}
              style={styles.card}
            >
              <View style={{height: windowHeight/3 - 15}}>
                <Text style={styles.cardTitle}>Co2 in room:</Text>
                <Text style={styles.cardText}>1104 ppm</Text>
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
              <View style={{height:windowHeight/3 - 15}}>
                <Text style={styles.cardTitle}>Room temperature:</Text>
                <Text style={styles.cardText}>25 °C</Text>
              </View>
            </CardView>
            <CardView
              cardElevation={3}
              cardMaxElevation={3}
              cornerRadius={5}
              style={styles.card}
            >
              <View style={{height: windowHeight/3 - 15}}>
                <Text style={styles.cardTitle}>Humidity in room:</Text>
                <Text style={styles.cardText}>53%</Text>
              </View>
            </CardView>
          </View>
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
    backgroundColor: "#DDE9D1"
  },
  card: {
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'center',
    flex: 1,
    margin: 5
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
    backgroundColor: "#B2CD97",
  },
  bottomLabel: {
    marginRight:25, 
    marginLeft:25, 
    marginTop: 10, 
    borderWidth:1, 
    borderRadius:5, 
    backgroundColor: "#B2CD97"
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