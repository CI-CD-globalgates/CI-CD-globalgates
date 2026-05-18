(function () {
    let currentIndex = 0;
    let autoSlideTimer = null;
    let slides = [];

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function formatRelativeTime(datetime) {
        if (!datetime) return "";

        const now = new Date();
        const date = new Date(String(datetime).replace(" ", "T"));
        if (Number.isNaN(date.getTime())) return "";

        let gap = Math.floor((now.getTime() - date.getTime()) / 1000 / 60);
        if (gap < 1) return "방금 전";
        if (gap < 60) return `${gap}분 전`;

        gap = Math.floor(gap / 60);
        if (gap < 24) return `${gap}시간 전`;

        gap = Math.floor(gap / 24);
        return `${gap}일 전`;
    }

    function splitNewsContent(content) {
        return String(content ?? "")
            .split(/\n+/)
            .map((line) => line.trim())
            .filter(Boolean)
            .slice(0, 20);
    }

    function flattenSlides(newsList) {
        return newsList.flatMap((news) => {
            const summaries = splitNewsContent(news.newsContent);

            if (summaries.length === 0) {
                return [{
                    id: news.id,
                    title: news.newsTitle,
                    summary: "",
                    createdDatetime: news.publishedAt || news.createdDatetime,
                }];
            }

            return summaries.map((summary) => ({
                id: news.id,
                title: news.newsTitle,
                summary,
                createdDatetime: news.publishedAt || news.createdDatetime,
            }));
        });
    }

    function createNewsItem(item) {
        const created = escapeHtml(formatRelativeTime(item.createdDatetime));

        return `
            <a class="footerNewsItem" href="/news/detail/${item.id}">
                <div class="footerNewsMeta">
                    <span class="footerNewsBadge">주요 뉴스</span>
                    <span class="footerNewsTime">${created}</span>
                </div>
                <strong class="footerNewsHeadline">${escapeHtml(item.title)}</strong>
                <p class="footerNewsSummary">${escapeHtml(item.summary)}</p>
            </a>
        `;
    }

    function stopAutoSlide() {
        if (!autoSlideTimer) return;
        clearInterval(autoSlideTimer);
        autoSlideTimer = null;
    }

    function updateTrackPosition() {
        const track = document.getElementById("footerNewsTrack");
        if (track) {
            track.style.width = `${100}%`;
            track.style.transform = `translateX(-${currentIndex * 100}%)`;
        }
    }

    function moveTo(index) {
        if (slides.length === 0) return;
        currentIndex = (index + slides.length) % slides.length;
        updateTrackPosition();
    }

    function startAutoSlide() {
        stopAutoSlide();
        if (slides.length <= 1) return;

        autoSlideTimer = setInterval(() => {
            moveTo(currentIndex + 1);
        }, 3500);
    }

    function showState(message) {
        const state = document.getElementById("footerNewsState");
        const viewport = document.getElementById("footerNewsViewport");
        const controls = document.getElementById("footerNewsControls");

        if (state) {
            state.textContent = message;
            state.classList.remove("off");
        }
        if (viewport) viewport.classList.add("off");
        if (controls) controls.classList.add("off");

        stopAutoSlide();
    }

    function bindControls() {
        const prevButton = document.getElementById("footerNewsPrev");
        const nextButton = document.getElementById("footerNewsNext");
        const viewport = document.getElementById("footerNewsViewport");

        if (prevButton) {
            prevButton.addEventListener("click", () => {
                moveTo(currentIndex - 1);
                startAutoSlide();
            });
        }

        if (nextButton) {
            nextButton.addEventListener("click", () => {
                moveTo(currentIndex + 1);
                startAutoSlide();
            });
        }

        if (viewport) {
            viewport.addEventListener("mouseenter", stopAutoSlide);
            viewport.addEventListener("mouseleave", startAutoSlide);
        }
    }

    function renderNews(newsList) {
        const state = document.getElementById("footerNewsState");
        const viewport = document.getElementById("footerNewsViewport");
        const track = document.getElementById("footerNewsTrack");
        const controls = document.getElementById("footerNewsControls");

        if (!state || !viewport || !track || !controls) return;

        slides = Array.isArray(newsList) ? flattenSlides(newsList) : [];

        if (slides.length === 0) {
            showState("뉴스가 없습니다.");
            return;
        }

        currentIndex = 0;
        track.innerHTML = slides.map(createNewsItem).join("");
        track.style.width = `${slides.length * 100}%`;

        state.classList.add("off");
        viewport.classList.remove("off");
        controls.classList.toggle("off", slides.length <= 1);

        updateTrackPosition();
        startAutoSlide();
    }

    async function loadNews() {
        if (typeof footerNewsService === "undefined") {
            showState("뉴스를 불러오지 못했습니다.");
            return;
        }

        try {
            renderNews(await footerNewsService.getLatestNews());
        } catch (e) {
            showState("뉴스를 불러오지 못했습니다.");
        }
    }

    function init() {
        if (!document.getElementById("footerNewsBanner")) return;
        bindControls();
        loadNews();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
