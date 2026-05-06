let currentPage = 1;
const pagination = document.querySelector('.pagination');
setCurrentPage(pagination.dataset.page);

function setCurrentPage(page) {
  if (page < 1) {
    page = 1;
  }
  currentPage = page;
  let buttons = pagination.getElementsByTagName("button");

  if (buttons.length - 2 < page) {
    currentPage = buttons.length - 2;
    page = buttons.length - 2;
    buttons[buttons.length - 1].disabled = true;
  }
  if (page < 11) {
    buttons[0].disabled = true;
  }

  for (let i = 1; i < buttons.length - 1; i++) {
    const btn = buttons[i];
    const num = parseInt(btn.textContent);
    const bottom = Math.floor(num / 10) * 10 + 1;
    btn.style.display = 'block';
    if (num < bottom || bottom > page + 10) {
      btn.style.display = 'none';
    }
  }
}