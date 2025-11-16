const apiOutput = document.getElementById('api-output');
const loginForm = document.getElementById('login-form');
const forgotBtn = document.getElementById('forgot-password');

function log(message) {
    apiOutput.textContent = message;
}

// TODO: implement client-side routing between login, dashboards, and detail pages.

loginForm.addEventListener('submit', (event) => {
    event.preventDefault();
    const formData = new FormData(loginForm);
    const payload = Object.fromEntries(formData.entries());
    log('TODO: send login request to backend.\n' + JSON.stringify(payload, null, 2));
});

forgotBtn.addEventListener('click', () => {
    // TODO: call backend forgot-password flow
    log('TODO: trigger forgotten password workflow.');
});

// TODO: add role-based dashboard rendering after login (customer/teller/admin).
