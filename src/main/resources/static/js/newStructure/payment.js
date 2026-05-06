main();

async function main() {
// ------  결제위젯 초기화 ------
  const clientKey = "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
  const tossPayments = TossPayments(clientKey);
// 회원 결제
  const customerKey = "Dn2mbgVZwjNEbOxxEAPjd";
  const widgets = tossPayments.widgets({
    customerKey,
  });
// ------ 주문의 결제 금액 설정 ------
  await widgets.setAmount({
    currency: "KRW",
    value: 1000,
  });

  await Promise.all([
    // ------  결제 UI 렌더링 ------
    widgets.renderPaymentMethods({
      selector: "#payment-method",
      variantKey: "DEFAULT",
    }),
    // ------  이용약관 UI 렌더링 ------
    widgets.renderAgreement(
        {selector: "#toss-agreement", variantKey: "AGREEMENT"}),
  ]);

  document.querySelectorAll('.donate-btn').forEach((btn) => {
    console.log("event added.");
    btn.addEventListener('click', async (e) => {
      const raw = e.currentTarget.dataset.donation;
      const donation = raw ? parseInt(raw, 10) : NaN;
      if (isNaN(donation)) {
        await widgets.setAmount({
          currency: "KRW",
          value: parseInt(document.getElementById('pay-amount').value, 10),
        });
        console.log(document.getElementById('pay-amount').value)
      } else {
        await widgets.setAmount({
          currency: "KRW",
          value: donation,
        });
        console.log(donation)
      }
    });
  });

// ------ '결제하기' 버튼 누르면 결제창 띄우기 ------
  const button = document.getElementById('payment-button')
  button.addEventListener("click", async function (e) {
    const email = e.currentTarget.dataset.email;
    const username = e.currentTarget.dataset.username;
    const orderId = crypto.randomUUID();
    await widgets.requestPayment({
      orderId,
      orderName: "Valenti 멤버십",
      successUrl: window.location.origin + "/payment/success",
      failUrl: window.location.origin + "/payment/fail",
      customerEmail: email,
      customerName: username,
    });
    //customerMobilePhone: "01012341234",
  });

  document.querySelector('.loading-layout').style.display = 'none';
  document.querySelector('.container').style.display = 'block';
}
