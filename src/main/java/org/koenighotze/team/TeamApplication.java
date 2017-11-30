package org.koenighotze.team;

import io.vavr.jackson.datatype.VavrModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootApplication
public class TeamApplication {
    @Bean
    CommandLineRunner commandLineRunner(TeamInMemoryRepository teamRepository) {
        return evt -> {
            //@formatter:off
            teamRepository.save(new Team("1",
                    "Fortuna Düsseldorf",
                    "https://tmssl.akamaized.net//images/wappen/head/38.png?lm=1405514004",
                    "Friedhelm Funkel",
                    BigDecimal.valueOf(13000000),
                    LocalDate.of(1895, 5, 5)));
            teamRepository.save(new Team("2",
                    "1. FC Kaiserslautern",
                    "This is not a valid url",
                    "Norbert Meier",
                    BigDecimal.valueOf(15800000),
                    LocalDate.of(1900, 6, 2)));
            teamRepository.save(new Team("3",
                    "FC St Pauli",
                    "https://this.will.point.nowhere.de/",
                    "Olaf Janßen",
                    BigDecimal.valueOf(15000000),
                    LocalDate.of(1910, 5, 15)));
            teamRepository.save(new Team("4",
                    "Eintracht Braunschweig",
                    "http://localhost:8080/timeout",
                    "Torsten Lieberknecht",
                    BigDecimal.valueOf(20100000),
                    LocalDate.of(1895, 12, 15)));
            //@formatter:on
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(TeamApplication.class, args);
    }

    @Bean
    public VavrModule vavrModule() {
        return new VavrModule();
    }
}
