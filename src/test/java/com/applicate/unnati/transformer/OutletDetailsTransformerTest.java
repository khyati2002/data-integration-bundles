package com.applicate.unnati.transformer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class OutletDetailsTransformerTest {

    private OutletDetailsTransformer transformer;

    @BeforeEach
    public void setUp() {
        transformer = new OutletDetailsTransformer();
    }

    @Test
    public void testResidentialAddressMapping() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("uid", "OUTLET123456");
        input.put("type", "LOYALTY");
        input.put("custname", "BABUL STORES");
        input.put("ownername", "BABUL STORES");
        input.put("email", "test@example.com");
        input.put("mobile", "9876543210");
        input.put("residential_address", "123 Main Street, Residential Area, City");
        input.put("outletlat", "26.424693999999999");
        input.put("outletlong", "90.973511000000002");
        input.put("outlettype", "Dual (FMCG + Tobacco)");
        input.put("channeltype", "Rural Wholesale");
        input.put("loyaltytype", "SWD Others");
        input.put("branch", "EGAU");
        input.put("district", "EDIS");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull(result);
        assertEquals("123 Main Street, Residential Area, City", result.get("residentialAddress"));
        assertNull(result.get("residential_address")); // Should not contain the original mapped field name
    }

    @Test
    public void testResidentialAddressMapping_NullValue() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("uid", "OUTLET123456");
        input.put("type", "LOYALTY");
        input.put("custname", "BABUL STORES");
        input.put("ownername", "BABUL STORES");
        input.put("residential_address", null);

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull(result);
        assertNull(result.get("residentialAddress")); // Should not be present when input is null
    }

    @Test
    public void testResidentialAddressMapping_EmptyValue() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("uid", "OUTLET123456");
        input.put("type", "LOYALTY");
        input.put("custname", "BABUL STORES");
        input.put("ownername", "BABUL STORES");
        input.put("residential_address", "");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull(result);
        assertNull(result.get("residentialAddress")); // Should not be present when input is empty
    }

    @Test
    public void testResidentialAddressMapping_WithSpecialCharacters() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("uid", "OUTLET123456");
        input.put("type", "LOYALTY");
        input.put("custname", "BABUL STORES");
        input.put("ownername", "BABUL STORES");
        input.put("residential_address", "456 Oak St, Apt #2B, New York, NY 10001");

        // Act
        Map<String, Object> result = transformer.transform(input);

        // Assert
        assertNotNull(result);
        assertEquals("456 Oak St, Apt #2B, New York, NY 10001", result.get("residentialAddress"));
    }
}