const container = document.getElementById('container');
const registerBtn = document.getElementById('register');
const loginBtn = document.getElementById('login');
const mobileRegisterBtn = document.getElementById('mobile-register');
const mobileLoginBtn = document.getElementById('mobile-login');

const setActiveState = (mode) => {
  if (!container) return;
  const goRegister = mode === 'register';
  container.classList.toggle('active', goRegister);

  const updateMobileButton = (btn, isActive) => {
    if (!btn) return;
    btn.classList.toggle('active', isActive);
  };

  updateMobileButton(mobileRegisterBtn, goRegister);
  updateMobileButton(mobileLoginBtn, !goRegister);

};

const attachToggle = (btn, mode) => {
  if (!btn) return;
  btn.addEventListener('click', () => setActiveState(mode));
};

attachToggle(registerBtn, 'register');
attachToggle(loginBtn, 'login');
attachToggle(mobileRegisterBtn, 'register');
attachToggle(mobileLoginBtn, 'login');

const initialState = container?.classList.contains('active') ? 'register' : 'login';
setActiveState(initialState);
