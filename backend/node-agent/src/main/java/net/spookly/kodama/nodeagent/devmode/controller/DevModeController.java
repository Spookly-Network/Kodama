package net.spookly.kodama.nodeagent.devmode.controller;

import net.spookly.kodama.nodeagent.devmode.dto.DevModeStatusResponse;
import net.spookly.kodama.nodeagent.devmode.dto.DevModeUpdateRequest;
import net.spookly.kodama.nodeagent.devmode.service.DevModeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/node/dev-mode")
public class DevModeController {

    private static final Logger logger = LoggerFactory.getLogger(DevModeController.class);

    private final DevModeService devModeService;

    public DevModeController(DevModeService devModeService) {
        this.devModeService = devModeService;
    }

    @GetMapping
    public DevModeStatusResponse getStatus() {
        return DevModeStatusResponse.fromCurrent(devModeService.isDevModeEnabled());
    }

    @PostMapping
    public DevModeStatusResponse update(@RequestBody(required = false) DevModeUpdateRequest request) {
        if (request == null || request.devMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "devMode is required");
        }
        boolean enabled = request.devMode();
        boolean previous = devModeService.setDevMode(enabled);
        if (previous != enabled) {
            logger.info("Dev-mode updated. previous={}, current={}", previous, enabled);
        } else {
            logger.info("Dev-mode unchanged. current={}", enabled);
        }
        return DevModeStatusResponse.fromUpdate(previous, enabled);
    }
}
