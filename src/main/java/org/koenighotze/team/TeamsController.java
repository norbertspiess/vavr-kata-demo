package org.koenighotze.team;

import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.vavr.CheckedFunction1.lift;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/teams")
@Slf4j
public class TeamsController {

    private final TeamInMemoryRepository teamRepository;

    @Autowired
    public TeamsController(TeamInMemoryRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @GetMapping
    public List<Team> getAllTeams() {
        return teamRepository.findAll()
                .map(this::hideManagementData);
    }

    @GetMapping("/{id}")
    public HttpEntity<Team> findTeam(@PathVariable String id) {
        return teamRepository.findById(id)
                .map(ResponseEntity::ok)
                .getOrElse(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/logo", produces = APPLICATION_OCTET_STREAM_VALUE)
    public HttpEntity<InputStreamResource> fetchLogo(@PathVariable String id) {
        return teamRepository.findById(id)
                .map(this::fetchLogoForTeam)
                .getOrElse(() -> {
                    log.warn("Logo fetch aborted. Team not found.");
                    return logoFetchNotFoundResponse();
                });
    }

    private HttpEntity<InputStreamResource> fetchLogoForTeam(Team team) {
        return readLogoFromTeamWithTimeout(team.getLogoUrl())
                .map(result -> result
                        .map(t -> logoFetchSuccessful(t))
                        .getOrElse(() -> logoFetchNotFoundResponse()))
                .recover(InterruptedException.class, e -> {
                    log.warn("Logo fetch aborted due to timeout", e);
                    return logoFetchTimedoutResponse();
                })
                .recover(TimeoutException.class, e -> {
                    log.warn("Logo fetch aborted due to timeout", e);
                    return logoFetchTimedoutResponse();
                })
                .recover(ExecutionException.class, e -> {
                    log.warn("Logo fetch failed to to internal error", e.getCause());
                    return logoFetchFailed();
                })
                .getOrElse(() -> {
                    log.warn("Logo fetch failed to to internal error");
                    return logoFetchFailed();
                });
    }

    private static HttpEntity<InputStreamResource> logoFetchFailed() {
        return new ResponseEntity<>(BAD_REQUEST);
    }

    private static HttpEntity<InputStreamResource> logoFetchNotFoundResponse() {
        return new ResponseEntity<>(NOT_FOUND);
    }

    private static HttpEntity<InputStreamResource> logoFetchSuccessful(ByteArrayOutputStream logo) {
        return ok(new InputStreamResource(new ByteArrayInputStream(logo.toByteArray())));
    }

    private static HttpEntity<InputStreamResource> logoFetchTimedoutResponse() {
        return new ResponseEntity<>(REQUEST_TIMEOUT);
    }

    private Try<Option<ByteArrayOutputStream>> readLogoFromTeamWithTimeout(String logo) {
        return Try.of(() -> CompletableFuture.supplyAsync(() ->
                lift(this::readLogoFromTeam).apply(logo))
                .get(3000, MILLISECONDS));
    }

    private ByteArrayOutputStream readLogoFromTeam(String logo) throws IOException {
        BufferedImage image = ImageIO.read(new URL(logo));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        return os;
    }

    private Team hideManagementData(Team team) {
        return new Team(team.getId(), team.getName(), team.getLogoUrl(), null, null, team.getFoundedOn());
    }
}
