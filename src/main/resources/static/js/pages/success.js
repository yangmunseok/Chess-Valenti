// success.html 에서 옮긴 페이지 스크립트

// 쿼리 파라미터 값이 결제 요청할 때 보낸 데이터와 동일한지 반드시 확인하세요.
// 클라이언트에서 결제 금액을 조작하는 행위를 방지할 수 있습니다.
const urlParams = new URLSearchParams(window.location.search);
const paymentKey = urlParams.get("paymentKey");
const orderId = urlParams.get("orderId");
const amount = urlParams.get("amount");

const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
const displayAmount = document.getElementById('display-amount');
const displayDate = document.getElementById('display-date');

async function confirm() {
  const requestData = {
    paymentKey: paymentKey,
    orderId: orderId,
    amount: amount,
  };

  console.log(requestData);

  const response = await fetch("/payment/confirm", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [csrfHeader]: csrfToken
    },
    body: JSON.stringify(requestData),
  });

  const json = await response.json();

  if (!response.ok) {
    // 결제 실패 비즈니스 로직을 구현하세요.
    window.location.href = `/html/fail.html?message=${json.message}&code=${json.code}`;
  }

  // 결제 성공 비즈니스 로직을 구현하세요.
  displayAmount.textContent = '₩' + json.balanceAmount;
  const date = new Date(json.approvedAt);
  const formatted = new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
  displayDate.textContent = formatted;
  console.log(json);
}

confirm();
