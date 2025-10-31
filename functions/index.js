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

    await snap.ref.update({
      otp: otp,
      otpEntered: null,
      otpVerified: false,
      otpInvalid: false, // Initialize as not invalid
    });

    console.log(`OTP ${otp} created for order ${context.params.orderId}`);
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

    const correctOtp = after.otp;
    const enteredOtp = after.otpEntered;

    const isCorrect = enteredOtp === correctOtp;

    if (isCorrect) {
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