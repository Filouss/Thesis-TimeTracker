package cz.cvut.fel.thesis.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures a GitHub-focused {@link WebClient} with OAuth2 integration.
 */
@Configuration
public class GitHubClientConfig {

    @Bean
    public WebClient githubWebClient(
            ClientRegistrationRepository clientRegistrations,
            OAuth2AuthorizedClientRepository authorizedClients
    ) {
        var oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations,authorizedClients);
        oauth.setDefaultClientRegistrationId("github");

        return WebClient.builder()
                .baseUrl("https://api.github.com")
                .apply(oauth.oauth2Configuration())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "timetracker-local")
                .build();
    }
}
