// admin/notice-post.html 에서 옮긴 페이지 스크립트

const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
const postId = document.querySelector('meta[name="post_id"]') ? document.querySelector(
    'meta[name="post_id"]').content : null
// 에디터 설정
var quill = new Quill('#editor-container', {
  modules: {
    toolbar: [
      [{header: [1, 2, false]}],
      ['bold', 'italic', 'underline', 'strike'],
      ['blockquote', 'code-block'],
      [{'list': 'ordered'}, {'list': 'bullet'}],
      ['link', 'image'],
      ['clean']
    ]
  },
  placeholder: '내용을 상세히 입력해주세요...',
  theme: 'snow'
});

function toggleStudyFields() {
  const postType = document.getElementById('post-type').value;
  const studyFields = document.getElementById('study-only-fields');
  if (postType === 'STUDY') {
    studyFields.style.display = 'block';
  } else {
    studyFields.style.display = 'none';
  }
}

// 초기 로드 시 실행
toggleStudyFields();

// 제목 입력 시 에러 상태 해제
document.getElementById('post-title').addEventListener('input', function() {
  this.classList.remove('error');
  document.getElementById('title-error').classList.remove('show');
});

// 발행 버튼 클릭 이벤트
document.getElementById('save-post').addEventListener('click', function () {
  const titleInput = document.getElementById('post-title');
  const title = titleInput.value;
  const postType = document.getElementById('post-type').value;
  const videoUrl = document.getElementById('video-url').value;
  const difficulty = document.getElementById('difficulty').value;
  const introduction = document.getElementById('introduction').value;
  const imageFile = document.getElementById('image-file').files[0];
  const content = quill.root.innerHTML; // HTML 형태로 내용 가져오기

  if (!title) {
    alert('제목을 입력해주세요.');
    titleInput.focus();
    return;
  }

  const formData = new FormData();
  formData.append('title', title);
  formData.append('content', content);
  formData.append('postType', postType);
  formData.append('videoUrl', videoUrl);
  formData.append('difficulty', difficulty);
  formData.append('introduction', introduction);
  if (imageFile) {
    formData.append('imageFile', imageFile);
  }

  const url = postId === null ? "/api/admin/posts" : `/api/admin/posts/${postId}`;
  const method = postId === null ? 'POST' : 'PATCH';

  fetch(url, {
    method: method,
    headers: {
      [csrfHeader]: csrfToken
      // Note: Do NOT set Content-Type header for FormData; the browser will set it with the correct boundary
    },
    body: formData
  }).then(res => {
    if (res.ok) {
      alert('글이 성공적으로 기록되었습니다.');
      location.href = '/admin/contents';
    } else if (res.status === 409) {
      const errorMsg = document.getElementById('title-error');
      titleInput.classList.add('error');
      errorMsg.classList.add('show');
      titleInput.focus();
    } else {
      alert('오류가 발생했습니다.');
    }
  });
});

// 삭제 버튼 클릭 이벤트
if (document.getElementById('delete-post') !== null) {
  document.getElementById('delete-post').addEventListener('click', function () {
    console.log('delete post.')
    fetch(`/api/admin/posts/${postId}`,
        {
          method: 'DELETE', headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
          }
        }).then(
        (res) => {
          alert('글이 성공적으로 삭제되었습니다.')
          const json = res.json().then((json) => console.log(json))
        })
  })
}
