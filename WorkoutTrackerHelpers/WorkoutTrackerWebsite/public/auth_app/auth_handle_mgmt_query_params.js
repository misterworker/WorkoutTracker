import { initializeApp } from 'https://www.gstatic.com/firebasejs/9.20.0/firebase-app.js';
import { getAuth, verifyPasswordResetCode } from 'https://www.gstatic.com/firebasejs/9.20.0/firebase-auth.js';
import { handleRecoverEmail } from '/auth_app/auth_handle_recover_email.js';
import { handleVerifyEmail } from '/auth_app/auth_handle_verify_email.js';

function getParameterByName(name) {
    const url = window.location.href;
    name = name.replace(/[\[\]]/g, '\\$&');
    const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)');
    const results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
  }
  
document.addEventListener('DOMContentLoaded', async () => {
  // Get the action to complete.
  const mode = getParameterByName('mode');
  // Get the one-time code from the query parameter.
  const actionCode = getParameterByName('oobCode');
  // (Optional) Get the continue URL from the query parameter if available.
  const continueUrl = getParameterByName('continueUrl');
  // (Optional) Get the language code if available.
  const lang = getParameterByName('lang') || 'en';

  // Configure the Firebase SDK.
  const config = {
    apiKey: "",
    authDomain: "",
    projectId: "",
    storageBucket: "",
    messagingSenderId: "",
    appId: ""
};
  const app = initializeApp(config);
  const auth = getAuth(app);

  // Handle the user management action.
  switch (mode) {
    case 'resetPassword':
      try {
          const email = await verifyPasswordResetCode(auth, actionCode);
          // Redirect to reset password page with email included in the URL
          window.location.href = `https://workout-wrecker.web.app/auth_app/reset-password.html?oobCode=${actionCode}&continueUrl=${encodeURIComponent(continueUrl)}&lang=${lang}&email=${encodeURIComponent(email)}`;
      } catch (error) {
          console.error('Error verifying action code:', error);
          // Handle error (e.g., display an error message)
      }
      break;
    case 'recoverEmail':
      // Display email recovery handler and UI.
      handleRecoverEmail(auth, actionCode, lang);
      break;
    case 'verifyEmail':
      // Display email verification handler and UI.
      handleVerifyEmail(auth, actionCode, "https://workout-wrecker.web.app/auth_app/verify-email.html", lang);
      break;
    default:
      // Error: invalid mode.
  }
}, false);
