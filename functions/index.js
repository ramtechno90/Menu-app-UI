const {
  onDocumentCreated,
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize the Admin SDK
admin.initializeApp();

/**
 * Generates a 4-digit OTP.
 * @return {string} The generated OTP.
 */
function generateOtp() {
  return Math.floor(1000 + Math.random() * 9000).toString();
}

// Trigger when a new order document is created to add an OTP.
exports.addOtpOnOrderCreate = onDocumentCreated("orders/{orderId}", async (event) => {
  const otp = generateOtp();
  const { orderId } = event.params;

  logger.log(`Generated OTP ${otp} for order ${orderId}`);

  // Update the document with the new OTP and reset related fields.
  return event.data.ref.update({
    otp: otp,
    otpEntered: null,
    otpVerified: false,
    otpInvalid: false,
  });
});

// Trigger to verify the OTP when a staff member enters it.
exports.verifyOtp = onDocumentUpdated("orders/{orderId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();

  // Exit if the OTP entered by the staff hasn't changed.
  if (before.otpEntered === after.otpEntered || after.otpEntered === null) {
    return null;
  }

  const { orderId } = event.params;
  const correctOtp = after.otp;
  const enteredOtp = after.otpEntered;
  const isCorrect = enteredOtp === correctOtp;

  if (isCorrect) {
    logger.log(`OTP for order ${orderId} is correct. Marking as verified.`);
    // If correct, update the order status to DELIVERED.
    return event.data.after.ref.update({
      otpVerified: true,
      otpInvalid: false,
      status: "DELIVERED",
    });
  } else {
    logger.warn(`Incorrect OTP entered for order ${orderId}.`);
    // If incorrect, mark it as invalid and reset the entered OTP.
    return event.data.after.ref.update({
      otpVerified: false,
      otpInvalid: true,
      otpEntered: null, // Reset for re-entry
    });
  }
});

// Sends a push notification when a new order is created.
exports.sendNewOrderNotification = onDocumentCreated("orders/{orderId}", async (event) => {
  const order = event.data.data();
  const { orderId } = event.params;
  const customerName = order.customerName;

  logger.log(`New order received: ${orderId} from ${customerName}. Preparing notification.`);

  const payload = {
    notification: {
      title: "New Order Received!",
      body: `A new order has been placed by ${customerName}.`,
    },
    topic: "new_orders", // Targeting the 'new_orders' FCM topic
  };

  try {
    await admin.messaging().send(payload);
    logger.log("Notification sent successfully.");
  } catch (error) {
    logger.error("Error sending notification:", error);
  }
});