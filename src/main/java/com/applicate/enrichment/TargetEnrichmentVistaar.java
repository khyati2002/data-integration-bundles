package com.applicate.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.Targets;
import com.salescode.dim.jooq.impl.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;

public class TargetEnrichmentVistaar extends AbstractEnrichment<Targets> {

    @Override
    public OperationResult.StepResult apply(Targets cdm) {

        final UserService userService = (UserService) ServiceLocator.lookup(User.class);

        try {
            String loginId = cdm.getUserValueStr();
            if (StringUtils.isEmpty(loginId)) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "userValueStr is null or empty");
            }

            User user = userService.findByLoginId(loginId);
            if(user == null){
                return new OperationResult.StepResult(OperationResult.Status.ERROR, StringUtils.format("User not found in database for this LoginId"));
            }

            Set<String> designation = user.getDesignation();
            if (designation != null && designation.stream().anyMatch("psr"::equalsIgnoreCase)) {
                Integer monthNumber = null;
                if (cdm.getExtendedAttributes() == null || !cdm.getExtendedAttributes().hasNonNull("month")) {
                    throw new IllegalArgumentException("No 'month' found in extendedAttributes.");
                }

                String monthName = cdm.getExtendedAttributes().get("month").toString();
                monthName = monthName.replace("\"", "");
                switch (monthName.toUpperCase()) {
                    case "JANUARY": case "JAN": monthNumber = 1; break;
                    case "FEBRUARY": case "FEB": monthNumber = 2; break;
                    case "MARCH": case "MAR": monthNumber = 3; break;
                    case "APRIL": case "APR": monthNumber = 4; break;
                    case "MAY": monthNumber = 5; break;
                    case "JUNE": case "JUN": monthNumber = 6; break;
                    case "JULY": case "JUL": monthNumber = 7; break;
                    case "AUGUST": case "AUG": monthNumber = 8; break;
                    case "SEPTEMBER": case "SEP": case "SEPT": monthNumber = 9; break;
                    case "OCTOBER": case "OCT": monthNumber = 10; break;
                    case "NOVEMBER": case "NOV": monthNumber = 11; break;
                    case "DECEMBER": case "DEC": monthNumber = 12; break;
                    default: monthNumber = 0; break;
                }
                if (monthNumber == 0) {
                    throw new IllegalArgumentException("No valid month has been passed.");
                }

                if (cdm.getExtendedAttributes() == null || !cdm.getExtendedAttributes().hasNonNull("year")) {
                    throw new IllegalArgumentException("No 'year' found in extendedAttributes.");
                }
                Integer year = Integer.parseInt(cdm.getExtendedAttributes().get("year").asText());

                YearMonth yearMonth = YearMonth.of(year, monthNumber);
                LocalDate firstOfMonth = yearMonth.atDay(1);
                LocalDate lastOfMonth = yearMonth.atEndOfMonth();
                LocalDateTime startDate = firstOfMonth.atStartOfDay();
                LocalDateTime endDate = lastOfMonth.atTime(23, 59, 59);

                cdm.setStartDate(startDate);
                cdm.setEndDate(endDate);
            }
        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Enrichment failed: " + e.getMessage());
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
    }
}