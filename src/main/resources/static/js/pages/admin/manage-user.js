// admin/manage-user.html 에서 옮긴 페이지 스크립트

const form = document.querySelector('.admin-form');
const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
const id = document.querySelector('meta[name="user_id"]').content;
console.log(id)
const banBtn = document.getElementById('user-ban-btn');
const unbanBtn = document.getElementById('user-unban-btn');
if (banBtn != null) {
  banBtn.addEventListener('click', (e) => {
    console.log('click')
    fetch(`/api/admin/users/${id}`, {
      method: 'PATCH', headers: {
        "Content-Type": "application/json",
        [csrfHeader]: csrfToken
      }, body: JSON.stringify({ban: true})
    }).then(
        (res) => {
          alert('유저를 벤하였습니다.')
          const json = res.json().then((json) => console.log(json))
        })
  })
}

if (unbanBtn != null) {
  unbanBtn.addEventListener('click', (e) => {
    fetch(`/api/admin/users/${id}`, {
      method: 'PATCH', headers: {
        "Content-Type": "application/json",
        [csrfHeader]: csrfToken
      }, body: JSON.stringify({ban: false})
    }).then(
        (res) => {
          alert('유저의 계정정지를 해제하였습니다.')
          const json = res.json().then((json) => console.log(json))
        })

  })
}

document.getElementById('user-role-btn').addEventListener('click', (e) => {
  const role = document.querySelector('.admin-select').value
  fetch(`/api/admin/users/${id}`, {
    method: 'PATCH', headers: {
      "Content-Type": "application/json",
      [csrfHeader]: csrfToken
    }, body: JSON.stringify({role})
  }).then(
      (res) => {
        alert(`유저의 권한을 ${role}로 변경하였습니다.`)
        const json = res.json().then((json) => console.log(json))
      })
})
