const faqFormTemplate = document.getElementById('faq-form-template');

const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

document.getElementById('faq-add-btn').addEventListener("click", () => {

  const content = faqFormTemplate.content.cloneNode(true);
  const faqContainer = document.getElementById('faq-container');
  const faqItem = content.firstElementChild;
  faqItem.querySelector('.save-faq-btn').onclick = function (e) {
    saveFaq(e);
  }
  faqItem.querySelector('.delete-faq-btn').onclick = function (e) {
    deleteFaq(e);
  }
  faqContainer.insertBefore(content,
      faqContainer.children[faqContainer.children.length - 1]);
});
Array.from(document.getElementsByClassName('save-faq-btn')).forEach(btn => {
  btn.addEventListener('click', e => saveFaq(e));
})
Array.from(document.getElementsByClassName('delete-faq-btn')).forEach(btn => {
  btn.addEventListener('click', e => deleteFaq(e));
})

function saveFaq(e) {
  const faqItem = e.currentTarget.closest('.faq-edit-item');
  const id = faqItem.dataset.id;
  const title = faqItem.querySelector('.admin-input-flat').value;
  const content = faqItem.querySelector('.reply-textarea').value;
  console.log(title)
  console.log(content)

  if (id != null) {
    fetch(`/admin/api/posts/${id}`,
        {
          method: 'PATCH', headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
          }, body: JSON.stringify({title, content, postType: "FAQ"})
        }).then(
        (res) => {
          alert('글이 성공적으로 기록되었습니다.')
          const json = res.json().then((json) => console.log(json))
        })
    //update
  } else {
    //create
    fetch("/admin/api/posts",
        {
          method: 'post', headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
          }, body: JSON.stringify({title, content, postType: "FAQ"})
        }).then(
        (res) => {
          alert('글이 성공적으로 기록되었습니다.')
          const json = res.json().then((json) => console.log(json))
        })
  }
  console.log('saveFaq(' + id + ') invoked.')
}

function deleteFaq(e) {
  const faqItem = e.currentTarget.closest('.faq-edit-item');
  const id = faqItem.dataset.id;

  fetch(`/admin/api/posts/${id}`,
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
}