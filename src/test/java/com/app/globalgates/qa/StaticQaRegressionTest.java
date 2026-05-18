package com.app.globalgates.qa;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StaticQaRegressionTest {

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Test
    void communityMainReportSendsReportDtoShapeAndAwaitsResponse() throws IOException {
        String script = read("src/main/resources/static/js/community/event.js");

        assertThat(script).contains("reporterId");
        assertThat(script).contains("targetId");
        assertThat(script).contains("targetType: \"post\"");
        assertThat(script).contains("if (!res.ok)");
    }

    @Test
    void chatServiceAcceptsEmptySuccessfulResponses() throws IOException {
        String script = read("src/main/resources/static/js/chat/service.js");

        assertThat(script).contains("const text = await response.text()");
        assertThat(script).contains("text ? JSON.parse(text) : null");
    }

    @Test
    void exploreNewsListRendersAllAdminNewsFields() throws IOException {
        String script = read("src/main/resources/static/js/explore/layout.js");

        assertThat(script).contains("news.newsCategory");
        assertThat(script).contains("news.newsType");
        assertThat(script).contains("news.newsContent");
        assertThat(script).contains("news.newsSourceUrl");
    }

    @Test
    void bookmarkPageCanMoveNewsBookmarksToFolders() throws IOException {
        String service = read("src/main/resources/static/js/bookmark/service.js");
        String event = read("src/main/resources/static/js/bookmark/event.js");

        assertThat(service).contains("getByMemberAndNews");
        assertThat(service).contains("addNews");
        assertThat(service).contains("moveNewsFolder");
        assertThat(event).contains("activeShareBookmarkType");
        assertThat(event).contains("BookmarkService.moveNewsFolder");
    }

    @Test
    void mypageProductRecommendationUsesAiCategoryUiInsteadOfTagFallback() throws IOException {
        String html = read("src/main/resources/templates/mypage/mypage.html");
        String css = read("src/main/resources/static/css/mypage/mypage.css");
        String event = read("src/main/resources/static/js/mypage/event.js");

        assertThat(html).contains("AI 추천 카테고리");
        assertThat(html).contains("상품명이나 설명을 입력하면 추천해드려요");
        assertThat(css).contains("Recommend-Chip");
        assertThat(css).contains("Product-Category-AI-Recommendation");
        assertThat(event).contains("data-recommended-category");
        assertThat(event).contains("applyRecommendedCategory");
        assertThat(event).contains("if (selectedTags.length >= 5)");
        assertThat(event).contains("태그는 최대 5개까지 추가할 수 있어요");
        assertThat(event).contains("addTag(trimmedCategoryName, trimmedCategoryName)");
        assertThat(event).contains("addTag(ck, ck)");
        assertThat(html.indexOf("id=\"tagList\""))
                .isLessThan(html.indexOf("id=\"productCategoryRecommendation\""));
    }
}
