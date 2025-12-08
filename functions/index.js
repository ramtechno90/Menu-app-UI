const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

// Generate 4-digit OTP
function generateOtp() {
  return Math.floor(1000 + Math.random() * 9000).toString();
}

// Trigger when a new order document is created
exports.addOtpOnOrderCreate = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const otp = generateOtp();
    const userId = snap.data().userId;
    const expiry = admin.firestore.Timestamp.fromDate(
      new Date(Date.now() + 15 * 60 * 1000) // 15 min validity
    );

    // Store OTP in a private sub-collection restricted to the user
    await snap.ref.collection("private").doc("data").set({
      otp: otp,
      userId: userId
    });

    // Update public document without the OTP
    await snap.ref.update({
      otpEntered: null,
      otpVerified: false,
      otpInvalid: false, // Initialize as not invalid
      otpExpiry: expiry,
    });

    console.log(`OTP created for order ${context.params.orderId}`);
  });


// Trigger to verify OTP when staff enters it
exports.verifyOtp = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    // Only run if staff entered a new OTP
    if (before.otpEntered === after.otpEntered || after.otpEntered === null) {
      return null;
    }

    const enteredOtp = after.otpEntered;
    const expiry = after.otpExpiry.toDate();

    // Fetch the correct OTP from the private collection
    const privateSnap = await change.after.ref.collection("private").doc("data").get();

    if (!privateSnap.exists) {
        console.error(`Private data not found for order ${context.params.orderId}`);
        // Consider failing verification or handling error
        await change.after.ref.update({
            otpVerified: false,
            otpInvalid: true,
            otpEntered: null,
        });
        return null;
    }

    const correctOtp = privateSnap.data().otp;
    const isExpired = Date.now() > expiry.getTime();
    const isCorrect = enteredOtp === correctOtp;

    if (!isExpired && isCorrect) {
      // Correct OTP
      await change.after.ref.update({
        otpVerified: true,
        otpInvalid: false,
        status: "DELIVERED",
      });
      console.log(
        `Order ${context.params.orderId}: entered=${enteredOtp}, verified=true`
      );
    } else {
      // Incorrect or expired OTP
      await change.after.ref.update({
        otpVerified: false,
        otpInvalid: true,
        otpEntered: null, // Reset for re-entry
      });
      console.log(
        `Order ${context.params.orderId}: entered=${enteredOtp}, verified=false (expired: ${isExpired})`
      );
    }

    return null;
  });
