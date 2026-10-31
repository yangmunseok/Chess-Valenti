// 엔진 평가: 평가 토글이 켜져 있으면 현재 국면을 서버(Stockfish)에 SSE 로 물어 점수와 평가 막대를 그린다.
import {currentNode} from "./state.js";

let elements = null;
let evaluationStream = null;

export function initEvaluation() {
  elements = {
    toggle: document.querySelector('.toggle'),
    evaluation: document.querySelector('.engine-evaluation'),
    evalFill: document.getElementById('eval-fill'),
  };
  elements.toggle?.addEventListener('change', renderEvaluation);
}

function updateEvalBar(score, isMate = false) {
  if (!elements.evalFill) {
    return;
  }

  let percent;
  if (isMate) {
    percent = score > 0 ? 100 : 0;
  } else {
    // Clamped linear scale: +/- 10.0 -> 100%/0%
    percent = Math.max(0, Math.min(100, 50 + (score / 10) * 50));
  }

  elements.evalFill.style.height = percent + '%';
}

function closeEvaluationStream() {
  if (evaluationStream) {
    evaluationStream.close();
    evaluationStream = null;
  }
}

function renderEvaluationData(data) {
  const {evaluation} = elements;
  evaluation.style.display = 'inline-block';

  if (data.pvs && data.pvs.length > 0) {
    if (data.pvs[0].hasOwnProperty("cp") && data.pvs[0].cp !== null) {
      const score = data.pvs[0].cp / 100;
      const displayScore = (score > 0 ? "+" : "") + score.toFixed(2);
      evaluation.textContent = displayScore;
      updateEvalBar(score);
    } else if (data.pvs[0].hasOwnProperty("mate") && data.pvs[0].mate
        !== null) {
      const mate = data.pvs[0].mate;
      evaluation.textContent = '#' + mate;
      updateEvalBar(mate, true);
    }
  }
}

export function renderEvaluation() {
  if (!elements?.evaluation) {
    return;
  }
  const {toggle, evaluation} = elements;
  closeEvaluationStream();

  if (currentNode.eval !== "") {
    evaluation.textContent = currentNode.eval;
    // Parse stored eval if available to update bar
    const storedEval = currentNode.eval;
    if (storedEval.startsWith('#')) {
      updateEvalBar(parseInt(storedEval.substring(1)), true);
    } else if (storedEval !== "Mate!") {
      updateEvalBar(parseFloat(storedEval));
    }
    return;
  }

  if (currentNode.move.endsWith('#')) {
    evaluation.textContent = 'Mate!';
    const winnerIsWhite = (currentNode.ply % 2 !== 0);
    updateEvalBar(winnerIsWhite ? 1 : -1, true);
    return;
  }

  if (toggle?.checked) {
    const streamFen = currentNode.fen;
    let receivedEvaluation = false;
    evaluationStream = new EventSource(
        `/api/evaluation/stream?fen=${encodeURIComponent(streamFen)}`
    );

    evaluationStream.onmessage = (event) => {
      if (streamFen !== currentNode.fen) {
        closeEvaluationStream();
        return;
      }
      receivedEvaluation = true;
      renderEvaluationData(JSON.parse(event.data));
    };

    evaluationStream.addEventListener('done', () => {
      closeEvaluationStream();
    });

    evaluationStream.onerror = (e) => {
      console.warn("Stockfish eval failed:", e);
      closeEvaluationStream();
      if (!receivedEvaluation) {
        evaluation.style.display = 'none';
      }
    };
  } else {
    evaluation.style.display = 'none';
  }
}
