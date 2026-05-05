package com.electoral.results_service.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.electoral.results_service.dto.*;
import com.electoral.results_service.dto.HistoryResponse.HistoryEntry;
import com.electoral.results_service.dto.ComparisonResponse.CandidateTimeline;
import com.electoral.results_service.dto.TrendsResponse.CandidateTrend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ResultsService resultsService;

    private static final Map<Long, String[]> ELECTION_META = Map.of(
        1L, new String[]{"Presidencial 2022",     "PRESIDENTIAL", "2022-05-29"},
        2L, new String[]{"Senado 2022",            "SENATE",       "2022-03-13"},
        3L, new String[]{"Alcaldía Bogotá 2023",   "LOCAL",        "2023-10-29"},
        4L, new String[]{"Gobernación Antioquia",  "LOCAL",        "2023-10-29"},
        5L, new String[]{"Consulta Popular 2023",  "REFERENDUM",   "2023-03-12"}
    );


    /**
     * @param type
     * @param candidate
     */
    public HistoryResponse getHistory(String type, String candidate) {
        log.info("getHistory | type={} candidate={}", type, candidate);

        List<HistoryEntry> entries = new ArrayList<>();

        for (Map.Entry<Long, String[]> meta : ELECTION_META.entrySet()) {
            Long   id       = meta.getKey();
            String name     = meta.getValue()[0];
            String eType    = meta.getValue()[1];
            String date     = meta.getValue()[2];

            // filtro por tipo
            if (type != null && !eType.equalsIgnoreCase(type)) continue;

            ResultsResponse r = safeGetResults(id);
            if (r == null) continue;

            // filtro por candidato
            if (candidate != null) {
                boolean hasCand = r.getCandidates().stream()
                    .anyMatch(c -> c.getName().toLowerCase().contains(candidate.toLowerCase()));
                if (!hasCand) continue;
            }

            CandidateResult winner = topCandidate(r.getCandidates());
            double winnerPct = r.getTotalVotes() > 0
                ? (winner.getVotes() * 100.0 / r.getTotalVotes()) : 0;

            entries.add(HistoryEntry.builder()
                .electionId(id)
                .electionName(name)
                .electionType(eType)
                .date(date)
                .totalVotes(r.getTotalVotes())
                .winner(winner.getName())
                .winnerPct(Math.round(winnerPct * 100.0) / 100.0)
                .build());
        }

        entries.sort(Comparator.comparing(HistoryEntry::getDate).reversed());

        long totalVotes = entries.stream().mapToLong(HistoryEntry::getTotalVotes).sum();

        return HistoryResponse.builder()
            .totalElections(entries.size())
            .totalVotesCast(totalVotes)
            .elections(entries)
            .build();
    }

    /**
     * @param electionIds lista de IDs a comparar (mínimo 2)
     */
    public ComparisonResponse getComparison(List<Long> electionIds) {
        log.info("getComparison | ids={}", electionIds);

        if (electionIds == null || electionIds.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos 2 electionIds.");
        }

        List<String>        names   = new ArrayList<>();
        List<ResultsResponse> results = new ArrayList<>();

        for (Long id : electionIds) {
            ResultsResponse r = safeGetResults(id);
            if (r == null) continue;
            String[] meta = ELECTION_META.getOrDefault(id, new String[]{"Elección " + id, "?", "?"});
            names.add(meta[0]);
            results.add(r);
        }

        // Recolectar todos los candidatos presentes en cualquier elección
        Set<String> allNames = results.stream()
            .flatMap(r -> r.getCandidates().stream())
            .map(CandidateResult::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CandidateTimeline> timelines = allNames.stream().map(cName -> {
            List<Integer> votes = new ArrayList<>();
            List<Double>  pcts  = new ArrayList<>();

            for (ResultsResponse r : results) {
                Optional<CandidateResult> found = r.getCandidates().stream()
                    .filter(c -> c.getName().equals(cName))
                    .findFirst();

                int v = found.map(CandidateResult::getVotes).orElse(0);
                double pct = r.getTotalVotes() > 0 ? (v * 100.0 / r.getTotalVotes()) : 0;
                votes.add(v);
                pcts.add(Math.round(pct * 100.0) / 100.0);
            }

            return CandidateTimeline.builder()
                .name(cName)
                .votes(votes)
                .percentages(pcts)
                .build();
        }).collect(Collectors.toList());

        // Ordenar por promedio de votos descendente
        timelines.sort(Comparator.comparingDouble(
            t -> -t.getVotes().stream().mapToInt(Integer::intValue).average().orElse(0)));

        return ComparisonResponse.builder()
            .electionNames(names)
            .candidates(timelines)
            .build();
    }

    /**
     * @param electionIds IDs en orden cronológico para calcular tendencia
     */
    public TrendsResponse getTrends(List<Long> electionIds) {
        log.info("getTrends | ids={}", electionIds);

        if (electionIds == null || electionIds.isEmpty()) {
            // default: todas las elecciones en orden
            electionIds = new ArrayList<>(ELECTION_META.keySet());
            electionIds.sort(Comparator.comparing(
                id -> ELECTION_META.getOrDefault(id, new String[]{"","","0"})[2]));
        }

        List<String>  labels    = new ArrayList<>();
        List<Integer> totals    = new ArrayList<>();
        List<ResultsResponse> results = new ArrayList<>();

        for (Long id : electionIds) {
            ResultsResponse r = safeGetResults(id);
            if (r == null) continue;
            String[] meta = ELECTION_META.getOrDefault(id, new String[]{"Elección " + id, "?", "?"});
            labels.add(meta[0]);
            totals.add(r.getTotalVotes());
            results.add(r);
        }

        // Tendencia general de participación
        String participationTrend = calcTrend(totals.stream()
            .map(Integer::doubleValue).collect(Collectors.toList()));

        // Tendencia por candidato
        Set<String> allNames = results.stream()
            .flatMap(r -> r.getCandidates().stream())
            .map(CandidateResult::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CandidateTrend> candidateTrends = allNames.stream().map(cName -> {
            List<Double> pcts = new ArrayList<>();
            for (ResultsResponse r : results) {
                int v = r.getCandidates().stream()
                    .filter(c -> c.getName().equals(cName))
                    .mapToInt(CandidateResult::getVotes).sum();
                double pct = r.getTotalVotes() > 0 ? (v * 100.0 / r.getTotalVotes()) : 0;
                pcts.add(Math.round(pct * 100.0) / 100.0);
            }
            return CandidateTrend.builder()
                .name(cName)
                .percentages(pcts)
                .trend(calcTrend(pcts))
                .build();
        }).collect(Collectors.toList());

        return TrendsResponse.builder()
            .labels(labels)
            .totalVotesPerElection(totals)
            .participationTrend(participationTrend)
            .candidateTrends(candidateTrends)
            .build();
    }


    private ResultsResponse safeGetResults(Long id) {
        try {
            return resultsService.getResults(id);
        } catch (Exception e) {
            log.warn("No se encontraron resultados para electionId={}: {}", id, e.getMessage());
            return null;
        }
    }

    private CandidateResult topCandidate(List<CandidateResult> candidates) {
        return candidates.stream()
            .max(Comparator.comparingInt(CandidateResult::getVotes))
            .orElse(new CandidateResult("N/A", 0));
    }

    private String calcTrend(List<Double> values) {
        if (values.size() < 2) return "STABLE";
        double first = values.get(0);
        double last  = values.get(values.size() - 1);
        if (first == 0) return "STABLE";
        double changePct = (last - first) / first * 100;
        if (changePct > 5)  return "RISING";
        if (changePct < -5) return "FALLING";
        return "STABLE";
    }
}