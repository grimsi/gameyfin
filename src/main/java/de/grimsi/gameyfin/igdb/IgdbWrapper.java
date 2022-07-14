package de.grimsi.gameyfin.igdb;

import com.google.protobuf.InvalidProtocolBufferException;
import com.igdb.proto.Game;
import de.grimsi.gameyfin.igdb.dto.TwitchOAuthTokenDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IgdbWrapper {

    @Value("${gameyfin.igdb.api.client-id}")
    private String clientId;

    @Value("${gameyfin.igdb.api.client-secret}")
    private String clientSecret;

    @Value("${gameyfin.igdb.config.preferred-platform}")
    private int preferredPlatform;

    @Autowired
    private WebClient.Builder webclientBuilder;

    private WebClient twitchApiClient;

    private WebClient igdbApiClient;

    private TwitchOAuthTokenDto accessToken;

    @PostConstruct
    public void init() {
        twitchApiClient = webclientBuilder.build();
        authenticate();
        initIgdbClient();
    }

    public void authenticate() {
        log.info("Authenticating on Twitch API...");

        URI url = UriComponentsBuilder
                .fromHttpUrl("https://id.twitch.tv/oauth2/token?client_id={client_id}&client_secret={client_secret}&grant_type=client_credentials")
                .buildAndExpand(clientId, clientSecret)
                .toUri();

        this.accessToken = twitchApiClient
                .post()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(TwitchOAuthTokenDto.class)
                .block();

        log.info("Successfully authenticated.");
    }

    public Game findGameByTitle(String title) {
        return searchForGameByTitle(title).orElseThrow(() -> new RuntimeException("Could not find game with title: \"%s\"".formatted(title)));
    }

    public Optional<Game> getGameById(Long id) {
        byte[] gameBytes = igdbApiClient.post()
                        .uri("games.pb")
                        .bodyValue("fields *; where id = %d;".formatted(id))
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();

        try {
            return Optional.ofNullable(Game.parseFrom(gameBytes));
        } catch (InvalidProtocolBufferException e) {
            return Optional.empty();
        }
    }

    private void initIgdbClient() {
        if (accessToken == null) {
            authenticate();
        }

        igdbApiClient = webclientBuilder
                .baseUrl("https://api.igdb.com/v4/")
                .defaultHeader("Client-ID", clientId)
                .defaultHeader("Authorization", "Bearer %s".formatted(accessToken.getAccessToken()))
                .build();
    }

    private Optional<Game> searchForGameByTitle(String searchTerm) {
        List<Game> games = new ArrayList<>();

        igdbApiClient.post()
                .uri("games.pb")
                .bodyValue("fields *; search \"%s\";".formatted(searchTerm))
                .retrieve()
                .bodyToFlux(Game.class)
                .doOnNext(games::add)
                .blockLast();

        if (games.isEmpty()) return Optional.empty();

        // First check if there are any matches with the exact search term
        // If no exact match has been found, check if there are matches where the name ends with the search term
        // This will filter out most DLCs and similiar stuff, but will detect a game even when your search term is not exactly the title
        // If that also returns nothing, just return the first search result
        //
        // Example: Searching for "Rainbow Six Siege" will result in returning "Tom Clancy's Rainbow Six Siege" (the game we want)
        //          If we just used the first result from IGDB we would get something like "Tom Clancy's Rainbow Six Siege Demon Veil" as a result

        Optional<Game> srExactTitleMatch = games.stream().filter(s -> s.getName().equals(searchTerm)).findFirst();
        if (srExactTitleMatch.isPresent()) return srExactTitleMatch;

        Optional<Game> srTitleEndsWithMatch = games.stream().filter(s -> s.getName().endsWith(searchTerm)).findFirst();
        if (srTitleEndsWithMatch.isPresent()) return srTitleEndsWithMatch;

        return Optional.of(games.get(0));
    }
}
