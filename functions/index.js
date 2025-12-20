const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

/**
 * Generates a 4-digit numeric OTP.
 */
function generateOtp() {
  return Math.floor(1000 + Math.random() * 9000).toString();
}

/**
 * 1. addOtpOnOrderCreate
 * Trigger: Firestore onCreate (orders/{orderId})
 * Purpose: Generates a secure OTP for the order and stores it in a private subcollection.
 */
exports.addOtpOnOrderCreate = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    try {
        const otp = generateOtp();
        const userId = snap.data().userId;
        const expiry = admin.firestore.Timestamp.fromDate(
          new Date(Date.now() + 15 * 60 * 1000) // 15 min validity
        );

        // Store OTP in a private sub-collection restricted to the user
        await snap.ref.collection("private").doc("data").set({
          otp: otp,
          userId: userId,
          createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Update public document to initialize OTP fields
        await snap.ref.update({
          otpEntered: null,
          otpVerified: false,
          otpInvalid: false,
          otpExpiry: expiry,
        });

        console.log(`OTP generated for order ${context.params.orderId}`);
    } catch (error) {
        console.error("Error generating OTP:", error);
    }
  });


/**
 * 2. verifyOtp
 * Trigger: Firestore onUpdate (orders/{orderId})
 * Purpose: Verifies the OTP entered by the delivery staff.
 */
exports.verifyOtp = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    // Only run if staff entered a new OTP (and it's not null)
    if (before.otpEntered === after.otpEntered || !after.otpEntered) {
      return null;
    }

    const enteredOtp = after.otpEntered;
    const expiry = after.otpExpiry ? after.otpExpiry.toDate() : new Date();

    try {
        // Fetch the correct OTP from the private collection
        const privateSnap = await change.after.ref.collection("private").doc("data").get();

        if (!privateSnap.exists) {
            console.error(`Private data not found for order ${context.params.orderId}`);
            await change.after.ref.update({
                otpVerified: false,
                otpInvalid: true,
                otpEntered: null, // Reset to allow retry
            });
            return null;
        }

        const correctOtp = privateSnap.data().otp;
        const isExpired = Date.now() > expiry.getTime();
        const isCorrect = enteredOtp === correctOtp;

        if (!isExpired && isCorrect) {
          // Success
          await change.after.ref.update({
            otpVerified: true,
            otpInvalid: false,
            status: "DELIVERED", // Automatically mark as delivered
          });
          console.log(`Order ${context.params.orderId}: Verified successfully.`);
        } else {
          // Failure
          await change.after.ref.update({
            otpVerified: false,
            otpInvalid: true,
            otpEntered: null, // Reset for re-entry
          });
          console.log(`Order ${context.params.orderId}: Verification failed. Correct=${isCorrect}, Expired=${isExpired}`);
        }
    } catch (error) {
        console.error("Error verifying OTP:", error);
    }
    return null;
  });

/**
 * 3. createDeliveryStaffAccount
 * Trigger: HTTPS Callable
 * Purpose: Allows Admins to create new delivery staff accounts securely.
 *          Creates a Firebase Auth user and a Firestore document.
 */
exports.createDeliveryStaffAccount = functions.https.onCall(async (data, context) => {
    // Check if the requester is an Admin
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'The function must be called while authenticated.');
    }

    // Check if the user is an admin in Firestore
    const adminDoc = await db.collection('admins').doc(context.auth.uid).get();
    const isAdminEmail = context.auth.token.email === 'admin@pizzaparadize.com';

    if (!adminDoc.exists && !isAdminEmail) {
        throw new functions.https.HttpsError('permission-denied', 'Only admins can create staff accounts.');
    }

    const { email, password, name, phone } = data;

    if (!email || !password || !name) {
        throw new functions.https.HttpsError('invalid-argument', 'Email, password, and name are required.');
    }

    try {
        // 1. Create Authentication User
        const userRecord = await admin.auth().createUser({
            email: email,
            password: password,
            displayName: name,
            disabled: false
        });

        // 2. Create Firestore Profile
        await db.collection('delivery_staff').doc(userRecord.uid).set({
            name: name,
            email: email,
            phone: phone || "",
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        console.log(`Created delivery staff: ${email} (${userRecord.uid})`);
        return { success: true, uid: userRecord.uid };

    } catch (error) {
        console.error("Error creating staff account:", error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});
