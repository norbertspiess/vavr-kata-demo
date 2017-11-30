package org.koenighotze.team;

import io.vavr.collection.List;
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
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.springframework.http.HttpStatus.*;

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
    public HttpEntity<List<Team>> getAllTeams() {
        List<Team> teams = teamRepository.findAll()
                .map(this::hideManagementData);
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{id}")
    public HttpEntity<Team> findTeam(@PathVariable String id) {
        return teamRepository.findById(id)
                .map(ResponseEntity::ok)
                .getOrElse(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/logo")
    public HttpEntity<InputStreamResource> fetchLogo(@PathVariable String id) {
        return teamRepository.findById(id)
                .map(this::fetchLogoForTeam)
                .getOrElse(TeamsController::logoFetchNotFoundResponse);
    }

    private HttpEntity<InputStreamResource> fetchLogoForTeam(Team team) {
        try {
            ByteArrayOutputStream logo = readLogoFromTeamWithTimeout(team.getLogoUrl());

            return logoFetchSuccessful(logo);
        } catch (InterruptedException | TimeoutException e) {
            log.warn("Logo fetch aborted due to timeout", e);
            return logoFetchTimedoutResponse();
        } catch (ExecutionException e) {
            log.warn("Logo fetch failed to to internal error", e.getCause());
            return logoFetchFailed();
        }
    }

    private static HttpEntity<InputStreamResource> logoFetchFailed() {
        return new ResponseEntity<>(BAD_REQUEST);
    }

    private static HttpEntity<InputStreamResource> logoFetchNotFoundResponse() {
        return new ResponseEntity<>(NOT_FOUND);
    }

    private static HttpEntity<InputStreamResource> logoFetchSuccessful(ByteArrayOutputStream logo) {
        return ResponseEntity.ok(new InputStreamResource(new ByteArrayInputStream(logo.toByteArray())));

    }

    private static HttpEntity<InputStreamResource> logoFetchTimedoutResponse() {
        return new ResponseEntity<>(REQUEST_TIMEOUT);
    }

    private ByteArrayOutputStream readLogoFromTeamWithTimeout(String logo) throws InterruptedException, ExecutionException, TimeoutException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return readLogoFromTeam(logo);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).get(3000, MILLISECONDS);
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
