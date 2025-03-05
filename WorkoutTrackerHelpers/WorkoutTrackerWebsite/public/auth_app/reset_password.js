// Import Firebase libraries
import { initializeApp } from 'https://www.gstatic.com/firebasejs/9.20.0/firebase-app.js';
import { getAuth, confirmPasswordReset } from 'https://www.gstatic.com/firebasejs/9.20.0/firebase-auth.js';

// Function to get URL parameters
function getParameterByName(name) {
    const url = window.location.href;
    name = name.replace(/[\[\]]/g, '\\$&');
    const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)');
    const results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

// Initialize Firebase
const firebaseConfig = {
    apiKey: "",
    authDomain: "",
    projectId: "",
    storageBucket: "",
    messagingSenderId: "",
    appId: ""
};
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

// Extract actionCode from URL and populate the hidden input
const actionCode = getParameterByName('oobCode');
const email = getParameterByName('email');
const lang = getParameterByName('lang') || 'en';

document.getElementById('oobCode').value = actionCode;

document.getElementById('email-info').innerHTML = `For <strong>${email}</strong>`;

// Validate password function
function validatePassword(password) {
    const passwordPattern = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    return passwordPattern.test(password);
}

// Handle form submission
document.getElementById('reset-password-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const newPassword = document.getElementById('new-password').value;
    const messageElement = document.getElementById('message');

    if (!validatePassword(newPassword)) {
        messageElement.innerText = 'Password must be at least 8 characters long and include a mix of letters, numbers, and special characters.';
        messageElement.classList.remove('success');
        return;
    }

    try {
        // Save the new password.
        await confirmPasswordReset(auth, actionCode, newPassword);

        // Show success message
        messageElement.innerText = 'Password reset successful.';
        messageElement.classList.add('success');
        window.location.href = "https://workout-wrecker.web.app/auth_app/reset-password-complete.html";

    } catch (error) {
        // Handle errors here
        messageElement.innerText = 'Error resetting password: ' + error.message;
        messageElement.classList.remove('success');
    }
});

// Toggle password visibility
const showPasswordCheckbox = document.getElementById('show-password');
const passwordField = document.getElementById('new-password');

showPasswordCheckbox.addEventListener('change', () => {
    const type = showPasswordCheckbox.checked ? 'text' : 'password';
    passwordField.setAttribute('type', type);
});
