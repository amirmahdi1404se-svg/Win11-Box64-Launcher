let currentDir = "/storage/emulated/0";

function toggleStartMenu(e) {
    e.stopPropagation();
    document.getElementById("start-menu").classList.toggle("hidden");
}

function closeStartMenu() {
    document.getElementById("start-menu").classList.add("hidden");
}

function closeWindow(id) {
    document.getElementById(id).classList.add("hidden");
}

function openX11() {
    if (window.Win11Native) {
        Win11Native.openX11Display();
    } else {
        alert("در حال اتصال به سرور گرافیکی Termux:X11...");
    }
}

function openExplorer(path) {
    currentDir = path;
    document.getElementById("explorer-window").classList.remove("hidden");
    document.getElementById("current-path").innerText = path;
    loadFiles(path);
}

function goBackDir() {
    if (currentDir === "/storage/emulated/0" || currentDir === "/") return;
    let parent = currentDir.substring(0, currentDir.lastIndexOf("/"));
    if (!parent) parent = "/storage/emulated/0";
    openExplorer(parent);
}

function loadFiles(path) {
    const container = document.getElementById("file-list");
    container.innerHTML = "";

    let files = [];
    if (window.Win11Native) {
        const raw = Win11Native.listFiles(path);
        files = JSON.parse(raw);
    } else {
        // حالت پیش‌نمایش در مرورگر
        files = [
            { name: "Games", path: path + "/Games", isDir: true },
            { name: "setup.exe", path: path + "/setup.exe", isDir: false },
            { name: "gta_sa.exe", path: path + "/gta_sa.exe", isDir: false }
        ];
    }

    // مرتب‌سازی: اول پوشه‌ها، بعد فایل‌ها
    files.sort((a, b) => b.isDir - a.isDir || a.name.localeCompare(b.name));

    files.forEach(file => {
        const isExe = !file.isDir && file.name.toLowerCase().endsWith(".exe");
        const div = document.createElement("div");
        div.className = "file-item" + (isExe ? " exe-badge" : "");

        let icon = "📄";
        if (file.isDir) icon = "📁";
        else if (isExe) icon = "🪟";

        div.innerHTML = `<div class="f-icon">${icon}</div><div class="f-name">${file.name}</div>`;

        div.onclick = () => {
            if (file.isDir) {
                openExplorer(file.path);
            } else if (isExe) {
                if (window.Win11Native) {
                    Win11Native.runExe(file.path);
                } else {
                    alert("اجرای فایل ویندوزی: " + file.name);
                }
            }
        };

        container.appendChild(div);
    });
}

// قابلیت جابه‌جایی پنجره (Drag Window)
const win = document.getElementById("explorer-window");
const header = document.getElementById("explorer-header");
let isDragging = false, offsetX, offsetY;

header.addEventListener("mousedown", e => {
    isDragging = true;
    offsetX = e.clientX - win.offsetLeft;
    offsetY = e.clientY - win.offsetTop;
});
document.addEventListener("mousemove", e => {
    if (!isDragging) return;
    win.style.left = (e.clientX - offsetX) + "px";
    win.style.top = (e.clientY - offsetY) + "px";
});
document.addEventListener("mouseup", () => isDragging = false);

// ساعت زنده تسک‌بار
setInterval(() => {
    const now = new Date();
    document.getElementById("clock").innerText = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}, 1000);

