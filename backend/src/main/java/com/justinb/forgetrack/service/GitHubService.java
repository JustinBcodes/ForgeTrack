package com.justinb.forgetrack.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GitHubService {

    private final RestClient client;

    public GitHubService(RestClient.Builder builder, @Value("${app.github-token:}") String token) {
        RestClient.Builder configured = builder.baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "ForgeTrack")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        if (!token.isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        this.client = configured.build();
    }

    public PullRequestData getPullRequest(String repository, int number) {
        return client.get()
                .uri("/repos/{repository}/pulls/{number}", repository, number)
                .retrieve()
                .body(PullRequestData.class);
    }

    public record PullRequestData(String title, @JsonProperty("html_url") String htmlUrl, String state,
                                  boolean merged) {
        public String displayState() {
            return merged ? "merged" : state;
        }
    }
}

