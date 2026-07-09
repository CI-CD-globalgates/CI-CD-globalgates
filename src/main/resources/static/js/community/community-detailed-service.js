const CommunityDetailService = (() => {

    // ─── 커뮤니티 게시글 ───
    const createPost = async (communityId, formData) => {
        const response = await fetch(`/api/communities/${communityId}/posts`, {
            method: "POST",
            body: formData   // multipart (제목, 내용, 파일)
        });
        if (!response.ok) throw new Error("게시글 작성 실패");
        return await response.json();
    };

    const getPosts = async (communityId, page, type = "latest") => {
        const params = new URLSearchParams({ type });
        const response = await fetch(`/api/communities/${communityId}/posts/${page}?${params}`);
        if (!response.ok) throw new Error("게시글 목록 조회 실패");
        return await response.json();
    };

    // ─── 검색 (explore 패턴 동일) ───
    const searchPosts = async (communityId, page, keyword, type = "latest") => {
        const params = new URLSearchParams({ keyword, type });
        const response = await fetch(`/api/communities/${communityId}/search/${page}?${params}`);
        if (!response.ok) throw new Error("검색 실패");
        return await response.json();
    };

    // ─── 미디어 탭 ───
    const getMedia = async (communityId, page) => {
        const response = await fetch(`/api/communities/${communityId}/media/${page}`);
        if (!response.ok) throw new Error("미디어 조회 실패");
        return await response.json();
    };

    // 공용 답글 모달이 호출하는 답글 게시 — main 패턴 동일 (/api/main/posts/{postId}/replies).
    const writeReply = async (postId, formData) => {
        await fetch(`/api/main/posts/${postId}/replies`, {
            method: "POST",
            body: formData
        });
    };

    const getPost = async (postId, memberId) => {
        const response = await fetch(`/api/main/posts/${postId}?memberId=${memberId}`);
        return await response.json();
    };

    // 공용 작성 모달의 부가 기능 — 모두 main 패턴 그대로 (전역).
    const getMyProducts = async (memberId, callback) => {
        const response = await fetch(`/api/main/products/members/${memberId}`);
        const data = await response.json();
        if (callback) return callback(data);
        return data;
    };
    const getPostTemps = async (memberId, callback) => {
        const response = await fetch(`/api/main/post-temps/${memberId}`);
        const data = await response.json();
        if (callback) return callback(data);
        return data;
    };
    const savePostTemp = async (memberId, postTempContent, location, tags) => {
        await fetch("/api/main/post-temps", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ memberId, postTempContent, postTempLocation: location, postTempTags: tags })
        });
    };
    const loadPostTemp = async (id) => {
        const response = await fetch(`/api/main/post-temps/${id}/load`, { method: "POST" });
        return await response.json();
    };
    const deletePostTemps = async (ids) => {
        await fetch("/api/main/post-temps/delete", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(ids)
        });
    };
    const searchMentionMembers = async (keyword, memberId) => {
        const response = await fetch(`/api/main/mentions/search?keyword=${encodeURIComponent(keyword)}&memberId=${memberId}`);
        return await response.json();
    };

    return {
        createPost, getPosts, searchPosts, getMedia,
        writeReply, getPost,
        getMyProducts, getPostTemps, savePostTemp, loadPostTemp, deletePostTemps,
        searchMentionMembers
    };
})();
