// Pokemon Dawn & Dusk — Web Simulator & Pokédex Explorer

const TYPE_COLORS = {
    normal: '#A8A878',
    fire: '#F08030',
    water: '#6890F0',
    grass: '#78C850',
    electric: '#F8D030',
    ice: '#98D8D8',
    fighting: '#C03028',
    poison: '#A040A0',
    ground: '#E0C068',
    flying: '#A890F0',
    psychic: '#F85888',
    bug: '#A8B820',
    rock: '#B8A038',
    ghost: '#705898',
    dragon: '#7038F8',
    steel: '#B8B8D0',
    fairy: '#EE99AC'
};

const POKEMON_DATA = [
    { id: 1, name: 'Bulbasaur', type: 'grass', rarity: 'common', catchRate: 0.65 },
    { id: 4, name: 'Charmander', type: 'fire', rarity: 'common', catchRate: 0.65 },
    { id: 7, name: 'Squirtle', type: 'water', rarity: 'common', catchRate: 0.65 },
    { id: 10, name: 'Caterpie', type: 'bug', rarity: 'common', catchRate: 0.85 },
    { id: 16, name: 'Pidgey', type: 'normal', rarity: 'common', catchRate: 0.85 },
    { id: 19, name: 'Rattata', type: 'normal', rarity: 'common', catchRate: 0.85 },
    { id: 25, name: 'Pikachu', type: 'electric', rarity: 'uncommon', catchRate: 0.55 },
    { id: 37, name: 'Vulpix', type: 'fire', rarity: 'common', catchRate: 0.65 },
    { id: 39, name: 'Jigglypuff', type: 'normal', rarity: 'common', catchRate: 0.70 },
    { id: 52, name: 'Meowth', type: 'normal', rarity: 'common', catchRate: 0.75 },
    { id: 54, name: 'Psyduck', type: 'water', rarity: 'common', catchRate: 0.70 },
    { id: 58, name: 'Growlithe', type: 'fire', rarity: 'common', catchRate: 0.65 },
    { id: 63, name: 'Abra', type: 'psychic', rarity: 'uncommon', catchRate: 0.50 },
    { id: 66, name: 'Machop', type: 'fighting', rarity: 'common', catchRate: 0.70 },
    { id: 74, name: 'Geodude', type: 'rock', rarity: 'common', catchRate: 0.75 },
    { id: 92, name: 'Gastly', type: 'ghost', rarity: 'common', catchRate: 0.70 },
    { id: 94, name: 'Gengar', type: 'ghost', rarity: 'rare', catchRate: 0.20 },
    { id: 95, name: 'Onix', type: 'rock', rarity: 'uncommon', catchRate: 0.40 },
    { id: 129, name: 'Magikarp', type: 'water', rarity: 'common', catchRate: 0.90 },
    { id: 130, name: 'Gyarados', type: 'water', rarity: 'rare', catchRate: 0.15 },
    { id: 131, name: 'Lapras', type: 'water', rarity: 'rare', catchRate: 0.20 },
    { id: 133, name: 'Eevee', type: 'normal', rarity: 'uncommon', catchRate: 0.45 },
    { id: 143, name: 'Snorlax', type: 'normal', rarity: 'rare', catchRate: 0.15 },
    { id: 144, name: 'Articuno', type: 'ice', rarity: 'legendary', catchRate: 0.08 },
    { id: 145, name: 'Zapdos', type: 'electric', rarity: 'legendary', catchRate: 0.08 },
    { id: 146, name: 'Moltres', type: 'fire', rarity: 'legendary', catchRate: 0.08 },
    { id: 149, name: 'Dragonite', type: 'dragon', rarity: 'rare', catchRate: 0.15 },
    { id: 150, name: 'Mewtwo', type: 'psychic', rarity: 'legendary', catchRate: 0.05 },
    { id: 151, name: 'Mew', type: 'psychic', rarity: 'legendary', catchRate: 0.05 }
];

// Generate remaining 151 entries
const FULL_151 = [];
for (let i = 1; i <= 151; i++) {
    const existing = POKEMON_DATA.find(p => p.id === i);
    if (existing) {
        FULL_151.push(existing);
    } else {
        const types = Object.keys(TYPE_COLORS);
        const randType = types[i % types.length];
        FULL_151.push({
            id: i,
            name: `Pokémon #${i}`,
            type: randType,
            rarity: i > 143 ? 'legendary' : (i % 7 === 0 ? 'rare' : (i % 3 === 0 ? 'uncommon' : 'common')),
            catchRate: 0.5
        });
    }
}

// ----------------------------------------------------
// Pokédex Grid Rendering & Search
// ----------------------------------------------------
const dexGrid = document.getElementById('pokedex-grid');
const dexSearch = document.getElementById('dex-search');

function renderPokedex(list) {
    dexGrid.innerHTML = '';
    list.forEach(p => {
        const card = document.createElement('div');
        card.className = 'dex-card';
        const typeColor = TYPE_COLORS[p.type] || '#A8A878';
        card.innerHTML = `
            <div class="dex-num">#${String(p.id).padStart(3, '0')}</div>
            <img class="dex-sprite" src="https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${p.id}.png" alt="${p.name}" loading="lazy" onerror="this.src='https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png'">
            <div class="dex-name">${p.name}</div>
            <div class="dex-type" style="background: ${typeColor}; color: #fff;">${p.type}</div>
        `;
        card.addEventListener('click', () => {
            startEncounterWith(p);
            document.getElementById('demo').scrollIntoView({ behavior: 'smooth' });
        });
        dexGrid.appendChild(card);
    });
}

dexSearch.addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase().trim();
    const filtered = FULL_151.filter(p => p.name.toLowerCase().includes(q) || String(p.id) === q || String(p.id).padStart(3, '0') === q);
    renderPokedex(filtered);
});

renderPokedex(FULL_151);

// ----------------------------------------------------
// Interactive Encounter Canvas Mini-Game
// ----------------------------------------------------
const canvas = document.getElementById('encounterCanvas');
const ctx = canvas.getContext('2d');
const overlay = document.getElementById('game-overlay');
const overlayTitle = document.getElementById('overlay-title');
const overlayMessage = document.getElementById('overlay-message');
const btnRestart = document.getElementById('btn-restart');
const targetNameEl = document.getElementById('target-name');
const targetRarityEl = document.getElementById('target-rarity');
const ballsCountEl = document.getElementById('balls-count');

let currentCreature = POKEMON_DATA[0];
let pokeballs = 10;
let phase = 'aiming'; // 'aiming', 'flying', 'shaking', 'resolved'
let creatureImg = new Image();
let ringRadius = 60;
let ringContracting = true;
let shakeCount = 0;
let shakeTime = 0;

// Projectile & Touch
let touchStart = { x: 0, y: 0, time: 0 };
let currentTouch = { x: 0, y: 0 };
let isDragging = false;
let ballPos = { x: 240, y: 480 };
let ballFlight = { startX: 240, startY: 480, endX: 240, endY: 200, progress: 0 };

function startEncounterWith(pokemon) {
    currentCreature = pokemon;
    targetNameEl.textContent = `Wild ${pokemon.name}`;
    targetRarityEl.textContent = pokemon.rarity.toUpperCase();
    targetRarityEl.className = `badge-rarity ${pokemon.rarity}`;
    creatureImg.src = `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.id}.png`;
    phase = 'aiming';
    ballPos = { x: canvas.width / 2, y: canvas.height - 80 };
    overlay.classList.add('hidden');
}

startEncounterWith(POKEMON_DATA[0]);

btnRestart.addEventListener('click', () => {
    const next = FULL_151[Math.floor(Math.random() * FULL_151.length)];
    startEncounterWith(next);
});

// Canvas Touch / Mouse Handlers
canvas.addEventListener('mousedown', (e) => onPointerDown(getCanvasPos(e)));
canvas.addEventListener('mousemove', (e) => onPointerMove(getCanvasPos(e)));
window.addEventListener('mouseup', (e) => onPointerUp(getCanvasPos(e)));

canvas.addEventListener('touchstart', (e) => {
    e.preventDefault();
    if (e.touches.length > 0) onPointerDown(getCanvasPos(e.touches[0]));
}, { passive: false });

canvas.addEventListener('touchmove', (e) => {
    e.preventDefault();
    if (e.touches.length > 0) onPointerMove(getCanvasPos(e.touches[0]));
}, { passive: false });

window.addEventListener('touchend', (e) => {
    onPointerUp(currentTouch);
});

function getCanvasPos(e) {
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    return {
        x: (e.clientX - rect.left) * scaleX,
        y: (e.clientY - rect.top) * scaleY
    };
}

function onPointerDown(pos) {
    if (phase !== 'aiming' || pokeballs <= 0) return;
    touchStart = { x: pos.x, y: pos.y, time: performance.now() };
    currentTouch = { x: pos.x, y: pos.y };
    isDragging = true;
}

function onPointerMove(pos) {
    if (!isDragging) return;
    currentTouch = pos;
}

function onPointerUp(pos) {
    if (!isDragging || phase !== 'aiming') return;
    isDragging = false;

    const dx = pos.x - touchStart.x;
    const dy = pos.y - touchStart.y;
    const duration = Math.max(50, performance.now() - touchStart.time);

    // Only throw if dragged upwards
    if (dy > -30) {
        ballPos = { x: canvas.width / 2, y: canvas.height - 80 };
        return;
    }

    pokeballs--;
    ballsCountEl.textContent = `x${pokeballs}`;

    const targetCenter = { x: canvas.width / 2, y: 200 };
    const landingX = touchStart.x + dx * 1.5;
    const landingY = touchStart.y + dy * 1.5;

    ballFlight = {
        startX: canvas.width / 2,
        startY: canvas.height - 80,
        endX: landingX,
        endY: landingY,
        progress: 0
    };
    phase = 'flying';
}

// Game Loop
function updateGame() {
    // Ring Animation
    if (ringContracting) {
        ringRadius -= 0.8;
        if (ringRadius <= 15) ringContracting = false;
    } else {
        ringRadius = 60;
        ringContracting = true;
    }

    if (phase === 'flying') {
        ballFlight.progress += 0.05;
        if (ballFlight.progress >= 1) {
            ballFlight.progress = 1;
            checkHitOutcome();
        }
    }

    drawGame();
    requestAnimationFrame(updateGame);
}

function checkHitOutcome() {
    const targetCenter = { x: canvas.width / 2, y: 200 };
    const dist = Math.hypot(ballFlight.endX - targetCenter.x, ballFlight.endY - targetCenter.y);

    if (dist > 75) {
        // Miss
        setTimeout(() => {
            phase = 'aiming';
            ballPos = { x: canvas.width / 2, y: canvas.height - 80 };
        }, 600);
    } else {
        // Hit -> Shaking phase
        phase = 'shaking';
        shakeCount = 0;
        shakeTime = 0;
        runShakeSequence();
    }
}

function runShakeSequence() {
    if (shakeCount < 3) {
        shakeCount++;
        setTimeout(runShakeSequence, 700);
    } else {
        // Final resolve
        const chance = currentCreature.catchRate || 0.65;
        const caught = Math.random() < chance;
        if (caught) {
            overlayTitle.textContent = "🎉 Gotcha!";
            overlayTitle.style.color = "#ffb300";
            overlayMessage.textContent = `${currentCreature.name} was successfully caught and added to your Pokédex!`;
            overlay.classList.remove('hidden');
        } else {
            overlayTitle.textContent = "💨 Broke Free!";
            overlayTitle.style.color = "#e53935";
            overlayMessage.textContent = `Aargh! ${currentCreature.name} broke free from the Poké Ball!`;
            overlay.classList.remove('hidden');
        }
        phase = 'resolved';
    }
}

function drawGame() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Dynamic Dawn & Dusk Background
    const bgGrad = ctx.createLinearGradient(0, 0, 0, canvas.height);
    bgGrad.addColorStop(0, '#3b1c6e');
    bgGrad.addColorStop(0.5, '#1e1042');
    bgGrad.addColorStop(1, '#0d0826');
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Target Platform
    const targetX = canvas.width / 2;
    const targetY = 200;

    ctx.beginPath();
    ctx.ellipse(targetX, targetY + 60, 90, 24, 0, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(255, 179, 0, 0.2)';
    ctx.fill();

    // Creature Aura
    ctx.beginPath();
    ctx.arc(targetX, targetY, 65, 0, Math.PI * 2);
    const auraGrad = ctx.createRadialGradient(targetX, targetY, 20, targetX, targetY, 70);
    auraGrad.addColorStop(0, 'rgba(255, 179, 0, 0.4)');
    auraGrad.addColorStop(1, 'rgba(255, 179, 0, 0)');
    ctx.fillStyle = auraGrad;
    ctx.fill();

    // Target Creature
    if (phase !== 'shaking' && phase !== 'resolved') {
        if (creatureImg.complete && creatureImg.naturalWidth > 0) {
            ctx.drawImage(creatureImg, targetX - 70, targetY - 70, 140, 140);
        } else {
            ctx.beginPath();
            ctx.arc(targetX, targetY, 40, 0, Math.PI * 2);
            ctx.fillStyle = '#ff8f00';
            ctx.fill();
        }

        // Aiming Rings
        if (phase === 'aiming') {
            ctx.beginPath();
            ctx.arc(targetX, targetY, 65, 0, Math.PI * 2);
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.4)';
            ctx.lineWidth = 2;
            ctx.stroke();

            ctx.beginPath();
            ctx.arc(targetX, targetY, ringRadius, 0, Math.PI * 2);
            ctx.strokeStyle = ringRadius < 30 ? '#76FF03' : '#ffb300';
            ctx.lineWidth = 3;
            ctx.stroke();
        }
    }

    // Draw Poké Ball
    if (phase === 'aiming') {
        const curPos = isDragging ? currentTouch : { x: canvas.width / 2, y: canvas.height - 80 };
        drawPokeball(curPos.x, curPos.y, 28, 0);
    } else if (phase === 'flying') {
        const p = ballFlight.progress;
        const curX = ballFlight.startX + (ballFlight.endX - ballFlight.startX) * p;
        const curY = ballFlight.startY + (ballFlight.endY - ballFlight.startY) * p - Math.sin(p * Math.PI) * 140;
        const scale = 28 * (1 - p * 0.35);
        drawPokeball(curX, curY, scale, p * 720);
    } else if (phase === 'shaking') {
        const shakeOffset = Math.sin(performance.now() * 0.02) * 15;
        drawPokeball(targetX, targetY, 20, shakeOffset);
    }
}

function drawPokeball(x, y, radius, rotationDeg) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate((rotationDeg * Math.PI) / 180);

    // Top Red Half
    ctx.beginPath();
    ctx.arc(0, 0, radius, Math.PI, 0, false);
    ctx.fillStyle = '#e53935';
    ctx.fill();

    // Bottom White Half
    ctx.beginPath();
    ctx.arc(0, 0, radius, 0, Math.PI, false);
    ctx.fillStyle = '#f5f5f5';
    ctx.fill();

    // Black Divider Band
    ctx.beginPath();
    ctx.moveTo(-radius, 0);
    ctx.lineTo(radius, 0);
    ctx.lineWidth = radius * 0.2;
    ctx.strokeStyle = '#0d0826';
    ctx.stroke();

    // Center Outer Button
    ctx.beginPath();
    ctx.arc(0, 0, radius * 0.35, 0, Math.PI * 2);
    ctx.fillStyle = '#0d0826';
    ctx.fill();

    // Center Inner Button
    ctx.beginPath();
    ctx.arc(0, 0, radius * 0.22, 0, Math.PI * 2);
    ctx.fillStyle = '#ffffff';
    ctx.fill();

    ctx.restore();
}

requestAnimationFrame(updateGame);
