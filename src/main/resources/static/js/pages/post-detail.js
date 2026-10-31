// post-detail.html 에서 옮긴 페이지 스크립트

document.addEventListener('DOMContentLoaded', function () {
  const submitBtn = document.getElementById('submit-comment');
  if (submitBtn) {
    submitBtn.addEventListener('click', function (e) {
      if (!isLoggedIn) {
        e.preventDefault();
        document.getElementById('login-confirm-modal').style.display = 'flex';
        return;
      }
      const postId = this.dataset.id;
      const content = document.getElementById('comment-input').value;
      const csrfToken = document.querySelector('meta[name="_csrf"]').content;
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

      if (!content.trim()) {
        alert('댓글 내용을 입력해주세요.');
        return;
      }

      fetch(`/api/posts/${postId}/comments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          [csrfHeader]: csrfToken
        },
        body: JSON.stringify({content: content})
      }).then(res => {
        if (res.ok) {
          alert('댓글이 등록되었습니다.');
          location.reload();
        } else if (res.status === 403) {
          alert('로그인이 필요한 서비스입니다.');
        } else {
          alert('댓글 등록에 실패했습니다.');
        }
      }).catch(err => {
        console.error(err);
        alert('오류가 발생했습니다.');
      });
    });
  }
});

function toggleReplyForm(commentId) {
  if (!isLoggedIn) {
    document.getElementById('login-confirm-modal').style.display = 'flex';
    return;
  }
  const form = document.getElementById(`reply-form-${commentId}`);
  if (form.style.display === 'none') {
    form.style.display = 'block';
  } else {
    form.style.display = 'none';
  }
}

function submitReply(parentId, postId) {
  const content = document.getElementById(`reply-input-${parentId}`).value;
  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

  if (!content.trim()) {
    alert('답글 내용을 입력해주세요.');
    return;
  }

  fetch(`/api/posts/${postId}/comments`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [csrfHeader]: csrfToken
    },
    body: JSON.stringify({content: content, parentId: parentId})
  }).then(res => {
    if (res.ok) {
      alert('답글이 등록되었습니다.');
      location.reload();
    } else {
      alert('답글 등록에 실패했습니다.');
    }
  }).catch(err => {
    console.error(err);
    alert('오류가 발생했습니다.');
  });
}

function deleteComment(commentId) {
  if (!confirm('정말로 이 댓글을 삭제하시겠습니까?')) {
    return;
  }

  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

  fetch(`/api/comments/${commentId}`, {
    method: 'DELETE',
    headers: {
      [csrfHeader]: csrfToken
    }
  }).then(res => {
    if (res.ok) {
      alert('댓글이 삭제되었습니다.');
      location.reload();
    } else {
      alert('댓글 삭제에 실패했습니다.');
    }
  }).catch(err => {
    console.error(err);
    alert('오류가 발생했습니다.');
  });
}
