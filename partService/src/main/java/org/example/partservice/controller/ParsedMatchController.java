package org.example.partservice.controller;

import org.example.partservice.model.ReferenceCpuMatch;
import org.example.partservice.model.ReferenceGpuMatch;
import org.example.partservice.model.ReferenceMotherboard;
import org.example.partservice.model.ReferenceMotherboardMatch;
import org.example.partservice.model.ReferenceCpu;
import org.example.partservice.model.ReferenceGpu;
import org.example.partservice.repository.ReferenceCpuMatchRepository;
import org.example.partservice.repository.ReferenceGpuMatchRepository;
import org.example.partservice.repository.ReferenceCpuRepository;
import org.example.partservice.repository.ReferenceGpuRepository;
import org.example.partservice.repository.ReferenceMotherboardMatchRepository;
import org.example.partservice.repository.ReferenceMotherboardRepository;
import org.example.partservice.service.ProductMatcherService;
import org.example.partservice.service.ProductMatcherService.MatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reference/match/parsed")
public class ParsedMatchController {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProductMatcherService matcherService;

    @Autowired
    private ReferenceCpuRepository cpuRepo;

    @Autowired
    private ReferenceGpuRepository gpuRepo;

    @Autowired
    private ReferenceCpuMatchRepository cpuMatchRepo;

    @Autowired
    private ReferenceGpuMatchRepository gpuMatchRepo;

    @Autowired
    private ReferenceMotherboardRepository motherboardRepo;

    @Autowired
    private ReferenceMotherboardMatchRepository motherboardMatchRepo;

    private static class ParsedRow {
        Long id;
        String name;
        String url;
        java.math.BigDecimal priceKzt;
    }

    private RowMapper<ParsedRow> parsedRowMapper = new RowMapper<ParsedRow>() {
        @Override
        public ParsedRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParsedRow r = new ParsedRow();
            r.id = rs.getLong("id");
            r.name = rs.getString("name");
            try { r.url = rs.getString("url"); } catch (SQLException ex) { r.url = null; }
            try { r.priceKzt = rs.getBigDecimal("price_kzt"); } catch (SQLException ex) { r.priceKzt = null; }
            return r;
        }
    };

    @PostMapping("/cpu")
    @Transactional
    public ResponseEntity<?> matchParsedCpus(@RequestParam(required = false) Long parsedId) {
        String sql = parsedId == null ? "select id, name, url, price_kzt from parsed_cpu" : "select id, name, url, price_kzt from parsed_cpu where id = ?";
        List<ParsedRow> rows = parsedId == null ? jdbc.query(sql, parsedRowMapper) : jdbc.query(sql, new Object[]{parsedId}, parsedRowMapper);

        List<Long> saved = new ArrayList<>();
        for (ParsedRow r : rows) {
            if (r.name == null || r.name.isBlank()) continue;
            Optional<MatchResult> match = matcherService.matchProduct(r.name);
            if (match.isPresent() && "CPU".equalsIgnoreCase(match.get().type)) {
                MatchResult mr = match.get();
                Optional<ReferenceCpu> refOpt = cpuRepo.findById(mr.referenceId);
                if (refOpt.isPresent()) {
                    ReferenceCpu ref = refOpt.get();
                    ReferenceCpuMatch m = new ReferenceCpuMatch();
                    m.setOpendbId(ref.getOpendbId());
                    m.setName(ref.getName());
                    m.setManufacturer(ref.getManufacturer());
                    m.setSeries(ref.getSeries());
                    m.setVariant(ref.getVariant());
                    m.setCores(ref.getCores());
                    m.setThreads(ref.getThreads());
                    m.setBaseClockGhz(ref.getBaseClockGhz());
                    m.setBoostClockGhz(ref.getBoostClockGhz());
                    m.setSocket(ref.getSocket());
                    m.setMicroarchitecture(ref.getMicroarchitecture());
                    m.setTdp(ref.getTdp());
                    m.setRawJson(ref.getRawJson());
                    m.setParsedPartId(r.id);
                    m.setSourceLink(r.url);
                    m.setPriceKzt(r.priceKzt);

                    // If a match for this opendbId+parsedPartId already exists, update it instead of inserting
                    Optional<ReferenceCpuMatch> existing = cpuMatchRepo.findByOpendbIdAndParsedPartId(ref.getOpendbId(), r.id);
                    if (existing.isPresent()) {
                        ReferenceCpuMatch ex = existing.get();
                        ex.setName(m.getName());
                        ex.setManufacturer(m.getManufacturer());
                        ex.setSeries(m.getSeries());
                        ex.setVariant(m.getVariant());
                        ex.setCores(m.getCores());
                        ex.setThreads(m.getThreads());
                        ex.setBaseClockGhz(m.getBaseClockGhz());
                        ex.setBoostClockGhz(m.getBoostClockGhz());
                        ex.setSocket(m.getSocket());
                        ex.setMicroarchitecture(m.getMicroarchitecture());
                        ex.setTdp(m.getTdp());
                        ex.setRawJson(m.getRawJson());
                        // update parsed link info
                        ex.setParsedPartId(m.getParsedPartId());
                        ex.setSourceLink(m.getSourceLink());
                        ex.setPriceKzt(m.getPriceKzt());
                        cpuMatchRepo.save(ex);
                    } else {
                        cpuMatchRepo.save(m);
                    }
                    saved.add(r.id);
                }
            }
        }

        return ResponseEntity.ok("Processed " + rows.size() + " parsed_cpu rows, saved matches for " + saved.size());
    }

    @PostMapping("/gpu")
    @Transactional
    public ResponseEntity<?> matchParsedGpus(@RequestParam(required = false) Long parsedId) {
        String sql = parsedId == null ? "select id, name, url, price_kzt from parsed_video_card" : "select id, name, url, price_kzt from parsed_video_card where id = ?";
        List<ParsedRow> rows = parsedId == null ? jdbc.query(sql, parsedRowMapper) : jdbc.query(sql, new Object[]{parsedId}, parsedRowMapper);

        List<Long> saved = new ArrayList<>();
        for (ParsedRow r : rows) {
            if (r.name == null || r.name.isBlank()) continue;
            Optional<MatchResult> match = matcherService.matchProduct(r.name);
            if (match.isPresent() && "GPU".equalsIgnoreCase(match.get().type)) {
                MatchResult mr = match.get();
                Optional<ReferenceGpu> refOpt = gpuRepo.findById(mr.referenceId);
                if (refOpt.isPresent()) {
                    ReferenceGpu ref = refOpt.get();
                    ReferenceGpuMatch m = new ReferenceGpuMatch();
                    m.setOpendbId(ref.getOpendbId());
                    m.setName(ref.getName());
                    m.setManufacturer(ref.getManufacturer());
                    m.setChipset(ref.getChipset());
                    m.setMemoryGb(ref.getMemoryGb());
                    m.setMemoryType(ref.getMemoryType());
                    m.setBaseClockMhz(ref.getBaseClockMhz() != null ? ref.getBaseClockMhz().intValue() : null);
                    m.setBoostClockMhz(ref.getBoostClockMhz() != null ? ref.getBoostClockMhz().intValue() : null);
                    m.setCoreCount(ref.getCoreCount());
                    m.setInterfaceName(ref.getInterface_());
                    m.setTdp(ref.getTdp());
                    m.setReleaseYear(ref.getReleaseYear());
                    m.setRawJson(ref.getRawJson());
                    m.setParsedPartId(r.id);
                    m.setSourceLink(r.url);
                    m.setPriceKzt(r.priceKzt);

                    // Update existing match if opendbId+parsedPartId already present
                    Optional<ReferenceGpuMatch> existing = gpuMatchRepo.findByOpendbIdAndParsedPartId(ref.getOpendbId(), r.id);
                    if (existing.isPresent()) {
                        ReferenceGpuMatch ex = existing.get();
                        ex.setName(m.getName());
                        ex.setManufacturer(m.getManufacturer());
                        ex.setChipset(m.getChipset());
                        ex.setMemoryGb(m.getMemoryGb());
                        ex.setMemoryType(m.getMemoryType());
                        ex.setBaseClockMhz(m.getBaseClockMhz());
                        ex.setBoostClockMhz(m.getBoostClockMhz());
                        ex.setCoreCount(m.getCoreCount());
                        ex.setInterfaceName(m.getInterfaceName());
                        ex.setTdp(m.getTdp());
                        ex.setReleaseYear(m.getReleaseYear());
                        ex.setRawJson(m.getRawJson());
                        ex.setParsedPartId(m.getParsedPartId());
                        ex.setSourceLink(m.getSourceLink());
                        ex.setPriceKzt(m.getPriceKzt());
                        gpuMatchRepo.save(ex);
                    } else {
                        gpuMatchRepo.save(m);
                    }
                    saved.add(r.id);
                }
            }
        }

        return ResponseEntity.ok("Processed " + rows.size() + " parsed_video_card rows, saved matches for " + saved.size());
    }

    @PostMapping("/motherboard")
    @Transactional
    public ResponseEntity<?> matchParsedMotherboards(@RequestParam(required = false) Long parsedId) {
        String sql = parsedId == null ? "select id, name, url, price_kzt from parsed_motherboard" : "select id, name, url, price_kzt from parsed_motherboard where id = ?";
        List<ParsedRow> rows = parsedId == null ? jdbc.query(sql, parsedRowMapper) : jdbc.query(sql, new Object[]{parsedId}, parsedRowMapper);

        List<Long> saved = new ArrayList<>();
        for (ParsedRow r : rows) {
            if (r.name == null || r.name.isBlank()) continue;
            Optional<MatchResult> match = matcherService.matchProduct(r.name);
            if (match.isPresent() && "MOTHERBOARD".equalsIgnoreCase(match.get().type)) {
                MatchResult mr = match.get();
                Optional<ReferenceMotherboard> refOpt = motherboardRepo.findById(mr.referenceId);
                if (refOpt.isPresent()) {
                    ReferenceMotherboard ref = refOpt.get();
                    ReferenceMotherboardMatch m = new ReferenceMotherboardMatch();
                    m.setOpendbId(ref.getOpendbId());
                    m.setName(ref.getName());
                    m.setManufacturer(ref.getManufacturer());
                    m.setSeries(ref.getSeries());
                    m.setVariant(ref.getVariant());
                    m.setSocket(ref.getSocket());
                    m.setFormFactor(ref.getFormFactor());
                    m.setChipset(ref.getChipset());
                    m.setMemoryMaxGb(ref.getMemoryMaxGb());
                    m.setMemoryRamType(ref.getMemoryRamType());
                    m.setMemorySlots(ref.getMemorySlots());
                    m.setRawJson(ref.getRawJson());
                    m.setParsedPartId(r.id);
                    m.setSourceLink(r.url);
                    m.setPriceKzt(r.priceKzt);

                    Optional<ReferenceMotherboardMatch> existing = motherboardMatchRepo.findByOpendbIdAndParsedPartId(ref.getOpendbId(), r.id);
                    if (existing.isPresent()) {
                        ReferenceMotherboardMatch ex = existing.get();
                        ex.setName(m.getName());
                        ex.setManufacturer(m.getManufacturer());
                        ex.setSeries(m.getSeries());
                        ex.setVariant(m.getVariant());
                        ex.setSocket(m.getSocket());
                        ex.setFormFactor(m.getFormFactor());
                        ex.setChipset(m.getChipset());
                        ex.setMemoryMaxGb(m.getMemoryMaxGb());
                        ex.setMemoryRamType(m.getMemoryRamType());
                        ex.setMemorySlots(m.getMemorySlots());
                        ex.setRawJson(m.getRawJson());
                        ex.setParsedPartId(m.getParsedPartId());
                        ex.setSourceLink(m.getSourceLink());
                        ex.setPriceKzt(m.getPriceKzt());
                        motherboardMatchRepo.save(ex);
                    } else {
                        motherboardMatchRepo.save(m);
                    }
                    saved.add(r.id);
                }
            }
        }

        return ResponseEntity.ok("Processed " + rows.size() + " parsed_motherboard rows, saved matches for " + saved.size());
    }
}
