(function () {
    async function loadRecommendations(memberId) {
        try {
            const friends = await recommendationService.getRecommendations(memberId);

            if (!friends || friends.length === 0) {
                recommendationLayout.showEmptyState("추천할 회원이 없습니다.");
                return;
            }

            recommendationLayout.showRecommendationList(friends);
        } catch (e) {
            console.log("추천 로드 실패", e);
            recommendationLayout.showEmptyState("추천을 불러오지 못했습니다.");
        }
    }

    function init() {
        const suggestionList = document.getElementById("suggestionList");
        const sidebar = document.getElementById("trendSidebar");
        if (!suggestionList || !sidebar) return;

        const memberId = sidebar.dataset.memberId;
        if (!memberId) {
            recommendationLayout.showEmptyState("추천할 회원이 없습니다.");
            return;
        }

        loadRecommendations(memberId);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
