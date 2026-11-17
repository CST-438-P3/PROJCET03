package edu.csumb.api.shelter;

import edu.csumb.api.shelter.service.ShelterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /shelters and GET /shelters/{shelterId} from openapi.yaml, for any signed-in user. */
@RestController
@RequestMapping("/shelters")
public class ShelterController {

    private final ShelterService shelterService;

    /** Creates the controller with the service that does the work. */
    public ShelterController(ShelterService shelterService) {
        this.shelterService = shelterService;
    }

    /** Returns one page of shelters, sorted by name. */
    @GetMapping
    public ShelterPage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return shelterService.list(page, size);
    }

    /** Returns one shelter. */
    @GetMapping("/{shelterId}")
    public ShelterResponse get(@PathVariable long shelterId) {
        return shelterService.get(shelterId);
    }
}
