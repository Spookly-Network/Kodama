package net.spookly.kodama.brain.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import net.spookly.kodama.brain.dto.CreateInstanceRequest;
import net.spookly.kodama.brain.dto.InstanceDto;
import net.spookly.kodama.brain.service.InstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instances")
public class InstanceController {

    private final InstanceService instanceService;

    public InstanceController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @GetMapping
    public List<InstanceDto> listInstances() {
        return instanceService.listInstances();
    }

    @GetMapping("/{id}")
    public InstanceDto getInstance(@PathVariable UUID id) {
        return instanceService.getInstance(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstanceDto createInstance(@Valid @RequestBody CreateInstanceRequest request) {
        return instanceService.createInstance(request);
    }
}
