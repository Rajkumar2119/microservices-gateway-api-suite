package com.rajkumar.orders.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    private static final String ORDER_BODY = """
            {"items":[{"sku":"KB-MECH-01","quantity":2},{"sku":"MS-WL-02","quantity":1}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void catalogueIsPublicAndHidesInactiveProducts() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'KB-MECH-01')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.sku == 'OLD-CABLE')]").isEmpty());
    }

    @Test
    void placingAnOrderRequiresAToken() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(ORDER_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerPlacesOrderAndOnlyTheyOrAnAdminCanSeeIt() throws Exception {
        String orderNumber = placeOrder("alice", null);

        mockMvc.perform(get("/orders/{n}", orderNumber).with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(204.48))
                .andExpect(jsonPath("$.items", hasSize(2)));

        mockMvc.perform(get("/orders/{n}", orderNumber).with(user("mallory")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/orders/{n}", orderNumber).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer").value("alice"));

        mockMvc.perform(get("/orders").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").exists());
    }

    @Test
    void idempotencyKeyPreventsDuplicateOrders() throws Exception {
        String first = placeOrder("bob", "checkout-123");

        mockMvc.perform(post("/orders").with(user("bob"))
                        .header("Idempotency-Key", "checkout-123")
                        .contentType(MediaType.APPLICATION_JSON).content(ORDER_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(first));
    }

    @Test
    void inactiveProductsCannotBeOrdered() throws Exception {
        mockMvc.perform(post("/orders").with(user("carol"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"sku\":\"OLD-CABLE\",\"quantity\":1}]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invalidOrderBodyIsRejected() throws Exception {
        mockMvc.perform(post("/orders").with(user("carol"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"sku\":\"KB-MECH-01\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminsMoveOrdersThroughTheLifecycle() throws Exception {
        String orderNumber = placeOrder("dave", null);
        String confirm = "{\"status\":\"CONFIRMED\"}";

        mockMvc.perform(patch("/orders/{n}/status", orderNumber).with(user("dave"))
                        .contentType(MediaType.APPLICATION_JSON).content(confirm))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/orders/{n}/status", orderNumber).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(confirm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/orders/{n}/status", orderNumber).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/orders/admin/search").param("status", "CONFIRMED").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void customerCanCancelBeforeShipping() throws Exception {
        String orderNumber = placeOrder("erin", null);

        mockMvc.perform(post("/orders/{n}/cancel", orderNumber).with(user("erin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void onlyAdminsManageTheCatalogue() throws Exception {
        String product = "{\"sku\":\"HDMI-2M\",\"name\":\"HDMI Cable 2m\",\"priceGbp\":9.99}";

        mockMvc.perform(post("/products").with(user("frank"))
                        .contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("HDMI-2M"));

        mockMvc.perform(get("/products/HDMI-2M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceGbp").value(9.99));
    }

    private String placeOrder(String username, String idempotencyKey) throws Exception {
        var request = post("/orders").with(user(username))
                .contentType(MediaType.APPLICATION_JSON).content(ORDER_BODY);
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/orders/ORD-")))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("orderNumber").asText();
    }

    private static RequestPostProcessor user(String username) {
        return jwt().jwt(j -> j.subject(username).claim("roles", java.util.List.of("USER")))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("admin").claim("roles", java.util.List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
