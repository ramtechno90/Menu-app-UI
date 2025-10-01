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
    const expiry = admin.firestore.Timestamp.fromDate(
      new Date(Date.now() + 15 * 60 * 1000) // 15 min validity
    );

    await snap.ref.update({
      otp: otp,               // store OTP (visible only to customer)
      otpEntered: null,       // staff will enter this later
      otpVerified: false,     // default
      otpExpiry: expiry       // optional expiry timestamp
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
    const expiry = after.otpExpiry.toDate();

    let verified = false;

    if (Date.now() <= expiry.getTime() && enteredOtp === correctOtp) {
      verified = true;
    }

    await change.after.ref.update({ otpVerified: verified });
    console.log(
      `Order ${context.params.orderId}: entered=${enteredOtp}, verified=${verified}`
    );

    return null;
  });