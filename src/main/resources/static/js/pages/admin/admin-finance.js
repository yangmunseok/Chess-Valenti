// admin/admin-finance.html 에서 옮긴 페이지 스크립트

const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
document.getElementById('tab-payments').display = 'block';

function switchTab(event, tabId) {
  document.querySelectorAll(".tab-btn-sm").forEach(btn => btn.classList.remove("active"))
  document.querySelectorAll(".tab-content").forEach(content => content.classList.remove("active"))
  event.currentTarget.classList.add("active");
  document.getElementById(tabId).classList.add("active");
}

function requestRefund(id) {
  fetch("/payment/refund/request", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [csrfHeader]: csrfToken
    },
    body: JSON.stringify(id)
  }).then(() => location.reload());
}

function approveRefund(id) {
  fetch("/payment/refund/accept", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [csrfHeader]: csrfToken
    },
    body: JSON.stringify(id)
  }).then(() => location.reload());
}

function rejectRefund(id) {
  fetch("/payment/refund/reject", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [csrfHeader]: csrfToken
    },
    body: JSON.stringify(id)
  }).then(() => location.reload());
}
