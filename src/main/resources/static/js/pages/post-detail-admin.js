// post-detail.html 에서 옮긴 페이지 스크립트

const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

var quill = new Quill('#reply-editor', {
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline'],
      ['link', 'code-block'],
      [{'list': 'ordered'}, {'list': 'bullet'}]
    ]
  },
  placeholder: '답변 내용을 입력해주세요.',
  theme: 'snow'
});

document.getElementById('submit-reply').addEventListener('click', function () {
  const id = this.dataset.id;
  const answer = quill.root.innerHTML;

  if (quill.getText().trim().length === 0) {
    alert('답변 내용을 입력해주세요.');
    return;
  }

  fetch(`/api/admin/inquiries/${id}/answer`, {
    method: 'POST',
    headers: {
      "Content-Type": "application/json",
      [csrfHeader]: csrfToken
    },
    body: JSON.stringify({answer})
  }).then(res => {
    if (res.ok) {
      alert('답변이 등록되었습니다.');
      location.reload();
    } else {
      alert('답변 등록에 실패했습니다.');
    }
  }).catch(err => {
    console.error(err);
    alert('오류가 발생했습니다.');
  });
});
