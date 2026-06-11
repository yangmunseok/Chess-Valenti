import {currentNode} from "./state.js";
import {executeMove} from "./game.js";

const movesBody = document.getElementById('explorer-moves-body');
const gamesBody = document.getElementById('top-games-body');

export async function updateOpeningExplorer() {
  const explorerPanel = document.getElementById('opening-explorer');
  if (!explorerPanel || !explorerPanel.classList.contains('active')) return;

  const playPath = currentNode.playPath;
  const url = playPath 
    ? `https://explorer.lichess.org/masters?play=${encodeURIComponent(playPath)}`
    : `https://explorer.lichess.org/masters`;

  try {
    const res = await fetch(url, {
      headers: {
        'Accept': 'application/json'
      }
    });

    if (res.status === 401) {
      movesBody.innerHTML = '<tr><td colspan="3" style="text-align:center; padding:20px; color:var(--danger);">API 인증이 필요합니다. (401 Unauthorized)</td></tr>';
      return;
    }

    if (!res.ok) throw new Error("Explorer data not found");
    const data = await res.json();
    renderExplorerData(data);
  } catch (e) {
    console.warn("Lichess Explorer API failed:", e);
    movesBody.innerHTML = '<tr><td colspan="3" style="text-align:center; padding:20px; color:var(--text-secondary);">No data available for this position.</td></tr>';
    gamesBody.innerHTML = '';
  }
}


function renderExplorerData(data) {
  // 1. Render Moves Table
  movesBody.innerHTML = '';
  if (data.moves && data.moves.length > 0) {
    data.moves.slice(0, 12).forEach(move => {
      const tr = document.createElement('tr');
      const total = move.white + move.draws + move.black;
      
      const whitePct = Math.round((move.white / total) * 100);
      const drawPct = Math.round((move.draws / total) * 100);
      const blackPct = 100 - whitePct - drawPct;

      tr.innerHTML = `
        <td><span class="explorer-move-san" data-uci="${move.uci}">${move.san}</span></td>
        <td class="explorer-games-count">${total.toLocaleString()}</td>
        <td>
          <div class="mini-result-bar">
            <div class="white" style="width: ${whitePct}%" title="White wins: ${whitePct}%"></div>
            <div class="draw" style="width: ${drawPct}%" title="Draws: ${drawPct}%"></div>
            <div class="black" style="width: ${blackPct}%" title="Black wins: ${blackPct}%"></div>
          </div>
        </td>
      `;
      
      tr.querySelector('.explorer-move-san').onclick = () => {
        const from = move.uci.substring(0, 2);
        const to = move.uci.substring(2, 4);
        const promotion = move.uci.length > 4 ? move.uci.substring(4, 5) : null;
        executeMove(from, to, promotion);
      };
      
      movesBody.appendChild(tr);
    });
  } else {
    movesBody.innerHTML = '<tr><td colspan="3" style="text-align:center; padding:20px; color:var(--text-secondary);">No master moves found.</td></tr>';
  }

  // 2. Render Top Games
  gamesBody.innerHTML = '';
  if (data.topGames && data.topGames.length > 0) {
    data.topGames.slice(0, 5).forEach(game => {
      const item = document.createElement('div');
      item.className = `top-game-item winner-${game.winner || 'draw'}`;
      item.innerHTML = `
        <div class="top-game-players">
          <span><strong>${game.white.name}</strong> (${game.white.rating})</span>
          <span>vs</span>
          <span><strong>${game.black.name}</strong> (${game.black.rating})</span>
        </div>
        <div class="top-game-info">
          <span>${game.year} • ${game.month ? game.month + '월' : ''}</span>
          <span>${game.winner ? (game.winner === 'white' ? '1-0' : '0-1') : '½-½'}</span>
        </div>
      `;
      gamesBody.appendChild(item);
    });
  } else {
    gamesBody.innerHTML = '<p style="font-size:11px; color:var(--text-secondary); text-align:center; padding:10px;">No top games recorded.</p>';
  }
}
