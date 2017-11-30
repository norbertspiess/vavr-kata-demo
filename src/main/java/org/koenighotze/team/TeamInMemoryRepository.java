package org.koenighotze.team;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TeamInMemoryRepository {
    private final Map<String, Team> data = new ConcurrentHashMap<>();

    public void save(Team team) {
        data.put(team.getId(), team);
    }

    public List<Team> findAll() {
        return List.ofAll(data.values());
    }

    public Option<Team> findById(String id) {
        return Option.of(data.get(id));
    }

    public void deleteAll() {
        data.clear();
    }
}
