import { applyActionCode, checkActionCode } from "https://www.gstatic.com/firebasejs/9.20.0/firebase-auth.js";

export async function handleVerifyEmail(auth, actionCode, continueUrl, lang) {
  try {
    // Check the action code to get the email information.
    const info = await checkActionCode(auth, actionCode);
    const email = info.data.email;

    // Try to apply the verification code.
    await applyActionCode(auth, actionCode);
    // Email address has been successfully verified.
    alert('Email verified successfully.');

    // Redirect user back to the app if continue URL is available.
    if (continueUrl) {
      window.location.href = `${continueUrl}?email=${encodeURIComponent(email)}`;
    }
  } catch (error) {
    // Handle errors from both checkActionCode and applyActionCode.
    alert('Error verifying email: ' + error.message);
  }
}
