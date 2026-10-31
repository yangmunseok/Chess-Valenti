// admin/admin-contents.html 에서 옮긴 페이지 스크립트

const mode = document.querySelector('.admin-tabs').dataset.current
console.log(mode);

if (mode === 'notice' || mode === null || mode === '') {
  document.getElementById('tab-notice').classList.add('active');
  document.getElementById('tab-notice-content').classList.add('active');
} else if (mode === 'faq') {
  document.getElementById('tab-faq').classList.add('active');
  document.getElementById('tab-faq-content').classList.add('active');
} else if (mode === 'study') {
  document.getElementById('tab-study').classList.add('active');
  document.getElementById('tab-study-content').classList.add('active');
}

function deletePost(id) {
  if (!confirm('정말로 이 글을 삭제하시겠습니까?')) {
    return;
  }

  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

  fetch(`/api/admin/posts/${id}`, {
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
