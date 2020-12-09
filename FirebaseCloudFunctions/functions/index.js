const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { user } = require('firebase-functions/lib/providers/auth');
admin.initializeApp();

const db = admin.database()

const removeReport = (uid, oldRoomID) => {
  const reportRef = db.ref(`rooms/${oldRoomID}/reports/${uid}`)
  return reportRef.remove()
}

exports.updateRooms = functions.database.ref('/users/{userID}').onWrite((change, context) => {
  const newRoomID = change.after.val().roomId
  const oldRoomID = change.before.val().roomId

  const removePersonRef = db.ref(`rooms/${oldRoomID}`).child('pplCount')
  const addPersonRef = db.ref(`rooms/${newRoomID}`).child('pplCount')

  const removeUserReport = removeReport(context.params.userID, oldRoomID)
  const removePerson = removePersonRef.transaction(count => {
    return count - 1
  })

  const addPerson = addPersonRef.transaction(count => {
    return count + 1
  })

  return Promise.all([addPerson, removePerson, removeUserReport])
});

exports.parseCountReports = functions.database.ref('/rooms/{roomID}/reports/{userID}').onWrite((change, context) => {
  const oldReport = change.before.val() !== null ? change.before.val().count : 0
  const roomId = context.params.roomID
  const uid = context.params.userID

  const reportRef = db.ref(`rooms/${roomId}/reports`)
  const pplCountRef = db.ref(`rooms/${roomId}`).child('pplCount')

  var reportCount = 0
  var pplCountSum = 0
  var previousSum = 0
  var previousCount = 0

  return reportRef.once('value', (snapshot) => {
    if (snapshot.exists()) {
      for (const [key, value] of Object.entries(snapshot.val())) {
        if (key !== uid) {
          previousSum += value.count
        }
        reportCount++
        previousCount++
        pplCountSum += value.count
      }
      previousSum = oldReport ? previousSum + oldReport : previousSum
    } else {
      previousSum = oldReport ? oldReport : previousSum
      previousCount = 1
      pplCountSum = 0
      reportCount = 1
    }
  }).then(() => {
    return pplCountRef.transaction((count) => {
      const oldAvg = previousSum / previousCount
      count = count - oldAvg 
      const newAvg = pplCountSum / reportCount
      count += newAvg 
      return count 
    })
  })
})