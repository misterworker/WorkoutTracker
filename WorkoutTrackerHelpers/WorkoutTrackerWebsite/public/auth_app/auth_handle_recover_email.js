import { checkActionCode, applyActionCode } from "https://www.gstatic.com/firebasejs/9.20.0/firebase-auth.js";

export function handleRecoverEmail(auth, actionCode, lang) {
  // Localize the UI to the selected language as determined by the lang parameter.

  // Try to apply the email recovery code.
  checkActionCode(auth, actionCode).then((info) => {
    // Get the restored email address.
    const restoredEmail = info.data.email;

    // Apply the email recovery code.
    applyActionCode(auth, actionCode).then(() => {
      // Email address has been successfully recovered.
      alert(`Email recovered to ${restoredEmail}.`);
    }).catch((error) => {
      // Error occurred during email recovery.
      alert('Error recovering email: ' + error.message);
    });
  }).catch((error) => {
    // Invalid or expired action code.
    alert('Invalid or expired action code.');
  });
}
