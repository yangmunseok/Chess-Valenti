// admin/admin-support.html 에서 옮긴 페이지 스크립트

const mode = document.querySelector('.admin-tabs').dataset.current
console.log(mode);

if (mode === 'inquiry' || mode === null) {
  document.getElementById('tab-inquiry').classList.add('active');
  document.getElementById('tab-inquiry-content').classList.add('active');
} else if (mode === 'faq') {
  document.getElementById('tab-faq').classList.add('active');
  document.getElementById('tab-faq-content').classList.add('active');
} else if (mode === 'notice') {
  document.getElementById('tab-notice').classList.add('active');
  document.getElementById('tab-notice-content').classList.add('active');
}

function showAnswerModal(btn) {
  const id = btn.dataset.id;

  document.querySelector(".support-answer-section").style.display = "flex";
}

function hideAnswerModal() {
  document.querySelector(".support-answer-section").style.display = "none";
}

function switchTab(event, tabId) {
  // 1. 모든 탭 버튼에서 active 제거
  const tabs = document.querySelectorAll('.tab-btn');
  tabs.forEach(tab => tab.classList.remove('active'));

  // 2. 모든 컨텐츠 영역 숨기기
  const contents = document.querySelectorAll('.tab-content');
  contents.forEach(content => content.classList.remove('active'));

  // 3. 클릭된 버튼과 해당 컨텐츠만 활성화
  event.currentTarget.classList.add('active');
  document.getElementById(tabId).classList.add('active');
}

function deleteInquiry(id) {
  if (!confirm('정말로 이 문의를 삭제하시겠습니까?')) {
    return;
  }

  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

  fetch(`/api/admin/inquiries/${id}`, {
    method: 'DELETE',
    headers: {
      [csrfHeader]: csrfToken
    }
  }).then(res => {
    if (res.ok) {
      alert('성공적으로 삭제되었습니다.');
      location.reload();
    } else {
      alert('삭제에 실패했습니다.');
    }
  }).catch(err => {
    console.error(err);
    alert('오류가 발생했습니다.');
  });
}
