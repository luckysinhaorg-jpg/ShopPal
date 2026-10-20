package com.shoppal.alexa;

import com.shoppal.account.AuthService;
import com.shoppal.account.ShopAccount;
import com.shoppal.product.BusinessProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alexa")
public class AlexaAssistantController {

    private final SimulatedAlexaService alexaService;
    private final AuthService authService;

    public AlexaAssistantController(SimulatedAlexaService alexaService, AuthService authService) {
        this.alexaService = alexaService;
        this.authService = authService;
    }

    @PostMapping("/query")
    public ResponseEntity<?> handleQuery(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody QueryRequest request) {

        ShopAccount account = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            account = authService.resolveAccount(token).orElse(null);
        }

        // If not authenticated via header, check request fallback or return 401
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required to query shop assistant."));
        }

        var response = alexaService.processQuery(account, request.query());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sample-prompts")
    public ResponseEntity<?> getSamplePrompts(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        ShopAccount account = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            account = authService.resolveAccount(token).orElse(null);
        }

        BusinessProfile profile = account != null ? account.getBusinessProfile() : BusinessProfile.KIRANA;

        List<SamplePrompt> prompts = switch (profile) {
            case PHARMACY -> List.of(
                    new SamplePrompt("Which medicine batches are nearing expiry?", "Check medicine batches, expiry quarantine & FEFO rules", "expiry"),
                    new SamplePrompt("What stock needs restocking and why?", "Run 7-day demand forecast & recommended order quantities", "restock"),
                    new SamplePrompt("Give me an inventory summary", "View total medicines, sellable units, and capital exposure", "summary"),
                    new SamplePrompt("What's the weather today?", "Test how the assistant gracefully handles unsupported queries", "unsupported")
            );
            case FOOTWEAR -> List.of(
                    new SamplePrompt("What stock needs restocking and why?", "Run footwear lead-time forecast & reorder analysis", "restock"),
                    new SamplePrompt("Show return analysis and durability", "Inspect defect rates and evidence-based durability", "returns"),
                    new SamplePrompt("Give me an inventory summary", "View footwear units, wholesale cost exposure", "summary"),
                    new SamplePrompt("Order pizza for dinner", "Test how the assistant gracefully handles unsupported queries", "unsupported")
            );
            default -> List.of(
                    new SamplePrompt("What stock needs restocking and why?", "Run 7-day grocery demand forecast & lead-time analysis", "restock"),
                    new SamplePrompt("Which batches are nearing expiry?", "Check staple food freshness, quarantine & shelf life", "expiry"),
                    new SamplePrompt("Give me an inventory summary", "View Kirana grocery items, units, and capital exposure", "summary"),
                    new SamplePrompt("What is the stock price of Amazon?", "Test how the assistant gracefully handles unsupported queries", "unsupported")
            );
        };

        return ResponseEntity.ok(Map.of(
                "profile", profile.name(),
                "prompts", prompts,
                "disclaimer", "Simulated Alexa+ experience for Amazon Hackathon (Alexa+ track). Not an actual connection to Amazon Alexa+."
        ));
    }

    public record QueryRequest(String query) {}
    public record SamplePrompt(String prompt, String description, String category) {}
}
