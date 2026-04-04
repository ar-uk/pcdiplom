
package com.example.pcbuilding.build;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
@CrossOrigin
public class BuildController {

    private final BuildRepository buildRepository;

    @GetMapping
    public List<Build> getAll() {
        return buildRepository.findAll();
    }

    @PostMapping
    public Build create(@RequestBody Build build) {
        return buildRepository.save(build);
    }

    @PutMapping("/{id}")
    public Build update(@PathVariable Long id, @RequestBody Build updated) {
        Build build = buildRepository.findById(id).orElseThrow();
        build.setCpu(updated.getCpu());
        build.setGpu(updated.getGpu());
        build.setRam(updated.getRam());
        build.setMotherboard(updated.getMotherboard());
        return buildRepository.save(build);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        buildRepository.deleteById(id);
    }
}
