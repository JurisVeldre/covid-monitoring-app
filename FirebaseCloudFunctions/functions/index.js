const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { user } = require('firebase-functions/lib/providers/auth');
admin.initializeApp();

const db = admin.database()

exports.updateRooms = functions.database.ref('/users/{userID}').onWrite((change, context) => {
  const newRoomID = change.after.val().roomId
  const oldRoomID = change.before.val().roomId
  
  const removePersonRef = db.ref(`rooms/${oldRoomID}`).child('pplCount')
  const addPersonRef = db.ref(`rooms/${newRoomID}`).child('pplCount')

  const removePerson = removePersonRef.transaction(count => {
    return count - 1
  })

  const addPerson = addPersonRef.transaction(count => {
    return count + 1
  })

  return Promise.all([addPerson, removePerson])
});

exports.parseCountReports = functions.database.ref('/rooms/{roomID}/reports/{userID}').onWrite((change, context) => {
  const oldReport = change.before.val().count
  const roomId = context.params.roomID
  const uid = context.params.userID

  const reportRef = db.ref(`rooms/${roomId}/reports`)
  const pplCountRef = db.ref(`rooms/${roomId}`).child('pplCount')

  var reportCount = 0
  var pplCountSum = 0
  var previousSum = 0
  var previousCount = 0

  console.log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
  console.log("uid: ", uid)

  return reportRef.once('value', (snapshot) => {
    for (const [key, value] of Object.entries(snapshot.val())) {
      console.log("===================")
      if (key !== uid) {
        previousSum += value.count
      }
      console.log("key: ", key)
      console.log("value: ", value)
      reportCount++
      previousCount++
      pplCountSum += value.count
      console.log("===================")
    }
    previousSum += oldReport
  }).then(() => {
    return pplCountRef.transaction((count) => {
      console.log("count b4 oldAvg: ", count)
      const oldAvg = previousSum / previousCount
      console.log("oldAvg: ", oldAvg)
      count =- oldAvg 
      console.log("count after oldAvg: ", count)
      count =+ (pplCountSum / reportCount)
      console.log("count after newAvg: ", count)
      console.log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
      return count 
    })
  })
})