// inquiry.html 에서 옮긴 페이지 스크립트

const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

var quill = new Quill('#editor-container', {
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline'],
      ['link', 'image', 'code-block'],
      [{'list': 'ordered'}, {'list': 'bullet'}]
    ]
  },
  placeholder: '문의 내용을 상세히 작성해주시면 빠른 처리가 가능합니다.',
  theme: 'snow'
});

document.getElementById('save-post').addEventListener('click', function () {
  const title = document.getElementById('post-title').value;
  const content = quill.root.innerHTML; // HTML 형태로 내용 가져오기
  const category = document.getElementById('inquiry-category').value;
  if (!title) {
    alert('제목을 입력해주세요.');
    return;
  }

  console.log("발행 데이터:", {title, content});
  fetch("/api/inquiries",
      {
        method: 'post', headers: {
          "Content-Type": "application/json",
          [csrfHeader]: csrfToken
        }, body: JSON.stringify({title, content, category})
      }).then(
      (res) => {
        alert('문의가 성공적으로 제출되었습니다.');
        location.reload();
      });
});
