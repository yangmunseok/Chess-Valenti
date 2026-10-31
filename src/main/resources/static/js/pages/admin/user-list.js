// admin/user-list.html 에서 옮긴 페이지 스크립트

function closeModal() {
  document.getElementById('stop-modal').classList.remove('active');
}

// 오버레이 클릭 시 닫기
window.onclick = function (event) {
  const modal = document.getElementById('stop-modal');
  if (event.target == modal) {
    closeModal();
  }
}

// 전역 변수로 현재 조작 중인 행을 저장 (예시용)
let currentTargetRow = null;

function openStopModal(button) {
  // 버튼이 속한 tr(행)을 찾아서 저장
  currentTargetRow = button.closest('tr');

  const modal = document.getElementById('stop-modal');
  modal.classList.add('active');
}

function confirmStop() {
  console.log(currentTargetRow);
  if (!currentTargetRow) {
    return;
  }

  // 1. 상태 컬럼(td)을 찾아 내용 변경
  const statusCell = currentTargetRow.querySelector('.status-cell'); // HTML에 클래스 추가 필요
  statusCell.innerHTML = `
      <span class="status-indicator stopped"></span>
      <span class="badge-stopped">활동 정지</span>
  `;

  // 2. 제어 버튼을 '정지 해제'로 변경 (선택 사항)
  const actionCell = currentTargetRow.querySelector('.action-cell');
  actionCell.innerHTML = `
      <button class="btn-action-resume" onclick="resumeUser(this)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 6L9 17l-5-5"/></svg>
          정지 해제
      </button>
  `;

  // 3. 행 스타일 변경
  currentTargetRow.classList.add('user-stopped');

  alert("해당 사용자의 활동이 정지되었습니다.");
  closeModal();
}
