// 공용 모달 전용 service — 가입 커뮤니티 조회 + 커뮤니티 게시 endpoint 호출 + 토스트
const postModalService = (() => {

    const getMyCommunities = async (page = 1) => {
        const res = await fetch(`/api/communities/my/${page}`);
        return await res.json();
    };

    const writeCommunityPost = async (communityId, formData) => {
        await fetch(`/api/communities/${communityId}/posts`, { method: "POST", body: formData });
    };

    // 일반 글 작성/수정 — 페이지 무관 공용 endpoint (커뮤니티 아닌 개인 글)
    const writePost = async (formData) => {
        await fetch('/api/main/posts/write', { method: 'POST', body: formData });
    };

    const updatePost = async (postId, formData) => {
        await fetch(`/api/main/posts/update/${postId}`, { method: 'POST', body: formData });
    };

    // 게시 후 자동 토스트 — main의 .notification-toast 클래스 재사용
    const showToast = (message) => {
        const existing = document.querySelector(".notification-toast");
        if (existing) existing.remove();
        const toast = document.createElement("div");
        toast.className = "notification-toast";
        toast.textContent = message;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    };

    return { getMyCommunities, writeCommunityPost, writePost, updatePost, showToast };
})();
