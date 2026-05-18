const recommendationService = (() => {
    const getRecommendations = async (memberId) => {
        const response = await fetch(`/ai/follow/recommend/${memberId}`);

        if (!response.ok) {
            throw new Error("Fetch error");
        }
        console.log(response);
        return await response.json();
    };

    return {
        getRecommendations: getRecommendations
    };
})();
