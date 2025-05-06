package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.applicate.services.channelkart.transformers.impl.JoltTransformer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

public class DSAbsentOutletActivityITCL extends JoltTransformer {


    @Override
    public Object transform(Map<String, Object> stringObjectMap) {
                Map<String, Object> result = (Map<String, Object>) super.transform(stringObjectMap);
                String referenceNumber = result.get("referenceNumber").toString()+ LocalDate.now(ZoneId.of(DateToClientTimeZoneStringConverter.getTimeZone() )).toString();
                result.put("referenceNumber", referenceNumber);
                result.put("id", referenceNumber);
                return result;
    }
}
