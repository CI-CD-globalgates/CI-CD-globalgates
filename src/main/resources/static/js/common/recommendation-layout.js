const recommendationLayout = (() => {
    const createRecommendationItem = (friend) => {
        const profileSrc = friend.memberProfileFileName || "/images/profile/default_image.png";
        const handle = friend.memberHandle || "";
        const name = friend.memberName || friend.memberNickname || "";
        const bio = friend.memberBio || "";

        return `
              <div class="suggestionItem trend-item" data-profile-id="${friend.id}">
                  <div class="suggestionAvatar">
                      <img class="suggestionAvatarImg" src="${profileSrc}" alt="" onerror="this.src='/images/profile/default_image.png'">
                  </div>
                  <div class="suggestionProfile">
                      <a class="suggestionName" href="/mypage/${friend.id}">${name}</a>
                      <span class="sidebar-user-handle">${handle}</span>
                  </div>
                  <button class="connect-btn-sm default" data-member-id="${friend.id}">Connect</button>
              </div>`;
    };

    const showRecommendationList = (friends) => {
        const suggestionList = document.getElementById("suggestionList");
        if (!suggestionList) return;

        suggestionList.innerHTML = friends.map(createRecommendationItem).join("");
    };

    const showEmptyState = (message) => {
        const suggestionList = document.getElementById("suggestionList");
        if (!suggestionList) return;

        suggestionList.innerHTML = `
              <div class="suggestionItem suggestionItem--empty">
                  <div class="suggestionProfile">
                      <span class="suggestionName">${message}</span>
                      <span class="sidebar-user-handle">새로운 사업자를 연결해보세요.</span>
                  </div>
              </div>`;
    };

    return {
        showRecommendationList: showRecommendationList,
        showEmptyState: showEmptyState
    };
})();
