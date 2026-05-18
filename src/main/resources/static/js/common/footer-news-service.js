const footerNewsService = (() => {
    const getLatestNews = async () => {
        const response = await fetch("/api/main/news/latest");
        console.log(response);
        return await response.json();
    };

    return { getLatestNews };
})();
