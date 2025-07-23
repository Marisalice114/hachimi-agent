package com.hachimi.hachimiagent.tools;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

public class WebScrapingTool {
    @Tool(description = "Scrape content from a web page")
    public String scrapeWebPage(@ToolParam(description = "url of the page to scrape") String url) {

        try {
            Document elements = Jsoup.connect(url).get();
            return elements.html();
        } catch (Exception e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}